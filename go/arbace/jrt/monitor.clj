;; jrt: monitors (C2G-SPEC §8.1): a thin lock in the header word, inflated to a monitor of
;; jrt's table on contention, on recursion overflow and on wait, deflated when free.
(in-ns 'go.arbace.jrt)

(go/file "monitor.go"
  :imports [[sync "sync"] [atomic "sync/atomic"] [time "time"]])

;; The lock word is the header's high 32 bits (object.go):
;;   bits 0-1   state: 0 unlocked, 1 thin, 2 inflated
;;   thin:      bits 2-9 the recursion count beyond the first entry, bits 10-31 the owner's
;;              thread number (1 .. 2^22-1)
;;   inflated:  bits 2-31 the monitor's index in jrt's table

(go/const
  [^{:tag uint32 :val 3} lockStateMask 3]
  [^{:tag uint32 :val 1} lockThin 1]
  [^{:tag uint32 :val 2} lockInflated 2]
  [^{:tag uint32 :val 255} thinCountMax 255]
  [^{:tag uint64 :val 4194303} thinOwnerMax 4194303])

(go/func lockWord ^uint32 [^uint64 w] (conv uint32 (>> w 32)))
(go/func withLock ^uint64 [^uint64 w ^uint32 l] (bit-or (bit-and w 0xffffffff) (<< (conv uint64 l) 32)))

(go/type monitor
  "monitor is an inflated lock: owner and count under mu, entry for the threads waiting to
enter, waiters for wait/notify. refs counts the threads that hold a reference to it without
owning it (entering, waiting): a monitor is deflated only with no owner and no refs.\n"
  (struct ^{:tag sync/Mutex} mu
          ^{:tag sync/Cond} entry
          ^uint64 owner
          ^int64 count
          ^int32 refs
          ^{:tag (slice (* waiter))} waiters
          ^uint32 idx
          ^{:tag (* Object)} obj))

(go/type waiter (struct ^{:tag (chan (struct))} ch))

(go/var ^{:tag sync/Mutex} monMu)
(go/var ^{:tag (map uint32 (* monitor))} monitors (make (map uint32 (* monitor))))
(go/var ^{:tag (slice uint32)} monFree)
(go/var ^uint32 monNext 1)

(go/var ^{:doc "Inflated counts the inflations, for tests and measurements.\n"
          :tag atomic/Int64} Inflated)

(go/func newMonitor
  "newMonitor creates a monitor for o in the table (monMu held).\n"
  ^{:tag (* monitor)} [^{:tag (* Object)} o ^uint64 owner ^int64 count]
  (let [^uint32 idx 0]
    (if (> (len monFree) 0)
      (do
        (set! idx (aget monFree (- (len monFree) 1)))
        (set! monFree (subslice monFree _ (- (len monFree) 1))))
      (do
        (set! idx monNext)
        (inc! monNext)))
    (let [m (addr (lit monitor :owner owner :count count :idx idx :obj o))]
      (set! (.-L (.-entry m)) (addr (.-mu m)))
      (aset monitors idx m)
      m)))

(go/func freeMonitor [^{:tag (* monitor)} m]
  (delete monitors (.-idx m))
  (set! monFree (append monFree (.-idx m))))

(go/method inflate
  "inflate turns the header word w (thin, or unlocked when owner is the caller) into an
inflated lock owned by owner with count recursions, and returns the monitor, with one ref
taken when ref; nil when the header changed meanwhile.\n"
  ^{:tag (* monitor)} [^{:tag (* Object)} o ^uint64 w ^uint64 owner ^int64 count ^bool ref]
  (.Lock monMu)
  (let [m (newMonitor o owner count)]
    (when ref
      (set! (.-refs m) 1))
    (when (not (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (withLock w (bit-or (<< (.-idx m) 2) lockInflated))))
      (freeMonitor m)
      (.Unlock monMu)
      (return nil))
    (.Unlock monMu)
    (.Add Inflated 1)
    m))

(go/method lookupMonitor
  "lookupMonitor returns the inflated monitor of o's header word w with one ref taken, or nil
when the header changed meanwhile.\n"
  ^{:tag (* monitor)} [^{:tag (* Object)} o ^uint64 w]
  (.Lock monMu)
  (when (!= (atomic/LoadUint64 (addr (.-hdr o))) w)
    (.Unlock monMu)
    (return nil))
  (let [m (aget monitors (>> (lockWord w) 2))]
    (.Lock (.-mu m))
    (inc! (.-refs m))
    (.Unlock (.-mu m))
    (.Unlock monMu)
    m))

(go/method enter "enter takes the monitor for me, then drops the ref the caller took.\n"
  [^{:tag (* monitor)} m ^uint64 me]
  (.Lock (.-mu m))
  (while (and (!= (.-owner m) 0) (!= (.-owner m) me))
    (.Wait (.-entry m)))
  (if (== (.-owner m) me)
    (inc! (.-count m))
    (do
      (set! (.-owner m) me)
      (set! (.-count m) 0)))
  (dec! (.-refs m))
  (.Unlock (.-mu m)))

(go/method tryDeflate
  "tryDeflate returns a free monitor without refs or waiters to the table and the header to
the unlocked state (the identity hash kept).\n"
  [^{:tag (* Object)} o ^{:tag (* monitor)} m]
  (.Lock monMu)
  (.Lock (.-mu m))
  (when (and (== (.-owner m) 0) (== (.-refs m) 0) (== (len (.-waiters m)) 0))
    (while true
      (let [w (atomic/LoadUint64 (addr (.-hdr o)))]
        (when (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (withLock w 0))
          (break))))
    (freeMonitor m))
  (.Unlock (.-mu m))
  (.Unlock monMu))

(go/method monitorEnter [^{:tag (* Object)} o]
  (let [me (currentThreadID)]
    (while true
      (let [w (atomic/LoadUint64 (addr (.-hdr o)))
            l (lockWord w)]
        (switch (bit-and l lockStateMask)
          (case [0]
            (if (<= me thinOwnerMax)
              (when (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (withLock w (bit-or (<< (conv uint32 me) 10) lockThin)))
                (return))
              (when (!= (.inflate o w me 0 false) nil)
                (return))))
          (case [lockThin]
            (let [owner (conv uint64 (>> l 10))
                  count (bit-and (>> l 2) thinCountMax)]
              (cond
                (and (== owner me) (< count thinCountMax))
                (when (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (withLock w (+ l 4)))
                  (return))
                (== owner me)
                (when (!= (.inflate o w me (+ (conv int64 count) 1) false) nil)
                  (return))
                :else
                (let [m (.inflate o w owner (conv int64 count) true)]
                  (when (!= m nil)
                    (.enter m me)
                    (return))))))
          (default
            (let [m (.lookupMonitor o w)]
              (when (!= m nil)
                (.enter m me)
                (return)))))))))

(go/func illegalMonitorState ^Throwable_I []
  (IllegalMonitorStateException_New_String (Str "current thread is not owner")))

(go/method monitorExit [^{:tag (* Object)} o]
  (let [me (currentThreadID)]
    (while true
      (let [w (atomic/LoadUint64 (addr (.-hdr o)))
            l (lockWord w)]
        (switch (bit-and l lockStateMask)
          (case [lockThin]
            (when (!= (conv uint64 (>> l 10)) me)
              (panic (illegalMonitorState)))
            (let [^uint32 nl 0]
              (when (> (bit-and (>> l 2) thinCountMax) 0)
                (set! nl (- l 4)))
              (when (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (withLock w nl))
                (return))))
          (case [lockInflated]
            (.Lock monMu)
            (let [m (aget monitors (>> l 2))]
              (.Unlock monMu)
              (when (or (== m nil) (!= (.-obj m) o))
                (continue))
              (.Lock (.-mu m))
              (when (!= (.-owner m) me)
                (.Unlock (.-mu m))
                (panic (illegalMonitorState)))
              (when (> (.-count m) 0)
                (dec! (.-count m))
                (.Unlock (.-mu m))
                (return))
              (set! (.-owner m) 0)
              (when (> (.-refs m) 0)
                (.Signal (.-entry m))
                (.Unlock (.-mu m))
                (return))
              (.Unlock (.-mu m))
              (.tryDeflate o m)
              (return)))
          (default
            (panic (illegalMonitorState))))))))

(go/method ownedMonitor
  "ownedMonitor returns o's monitor, inflating a thin lock, with one ref taken; it throws
IllegalMonitorStateException unless the current thread owns the lock.\n"
  ^{:tag (* monitor)} [^{:tag (* Object)} o ^uint64 me]
  (while true
    (let [w (atomic/LoadUint64 (addr (.-hdr o)))
          l (lockWord w)]
      (switch (bit-and l lockStateMask)
        (case [lockThin]
          (when (!= (conv uint64 (>> l 10)) me)
            (panic (illegalMonitorState)))
          (let [m (.inflate o w me (conv int64 (bit-and (>> l 2) thinCountMax)) true)]
            (when (!= m nil)
              (return m))))
        (case [lockInflated]
          (let [m (.lookupMonitor o w)]
            (when (!= m nil)
              (.Lock (.-mu m))
              (when (!= (.-owner m) me)
                (dec! (.-refs m))
                (.Unlock (.-mu m))
                (panic (illegalMonitorState)))
              (.Unlock (.-mu m))
              (return m))))
        (default
          (panic (illegalMonitorState)))))))

(go/var ^{:doc "interruptChan returns the current thread's interrupt channel for Object.wait: nil (never
ready) until phase 2's Thread sets it; a receive means the thread was interrupted.\n"}
  interruptChan
  (fn ^{:tag (chan :recv (struct))} [] nil))

(go/method monitorWait [^{:tag (* Object)} o ^{:tag time/Duration} d]
  (let [me (currentThreadID)
        m (.ownedMonitor o me)
        wt (addr (lit waiter :ch (make (chan (struct)))))]
    (.Lock (.-mu m))
    (let [saved (.-count m)]
      (set! (.-owner m) 0)
      (set! (.-count m) 0)
      (set! (.-waiters m) (append (.-waiters m) wt))
      (.Signal (.-entry m))
      (.Unlock (.-mu m))
      (let [interrupted false]
        (if (> d 0)
          (let [t (time/NewTimer d)]
            (select
              (case (<! (.-ch wt)))
              (case (<! (.-C t)))
              (case (<! (interruptChan)) (set! interrupted true)))
            (.Stop t))
          (select
            (case (<! (.-ch wt)))
            (case (<! (interruptChan)) (set! interrupted true))))
        (.Lock (.-mu m))
        (range [i x (.-waiters m)]
          (when (== x wt)
            (set! (.-waiters m) (append (subslice (.-waiters m) _ i) (spread (subslice (.-waiters m) (+ i 1)))))
            (break)))
        (while (!= (.-owner m) 0)
          (.Wait (.-entry m)))
        (set! (.-owner m) me)
        (set! (.-count m) saved)
        (dec! (.-refs m))
        (.Unlock (.-mu m))
        (when interrupted
          (panic (InterruptedException_New)))))))

(go/method monitorNotify [^{:tag (* Object)} o ^bool all]
  (let [me (currentThreadID)
        w (atomic/LoadUint64 (addr (.-hdr o)))
        l (lockWord w)]
    (switch (bit-and l lockStateMask)
      (case [lockThin]
        (when (!= (conv uint64 (>> l 10)) me)
          (panic (illegalMonitorState))))
      (case [lockInflated]
        (.Lock monMu)
        (let [m (aget monitors (>> l 2))]
          (.Unlock monMu)
          (when (or (== m nil) (!= (.-obj m) o))
            (panic (illegalMonitorState)))
          (.Lock (.-mu m))
          (when (!= (.-owner m) me)
            (.Unlock (.-mu m))
            (panic (illegalMonitorState)))
          (while (> (len (.-waiters m)) 0)
            (close (.-ch (aget (.-waiters m) 0)))
            (set! (.-waiters m) (subslice (.-waiters m) 1))
            (when (not all)
              (break)))
          (.Unlock (.-mu m))))
      (default
        (panic (illegalMonitorState))))))

;; ---------------------------------------------------------------------------------------
;; The API c2g's output calls (§8.1)

(go/func MonitorEnter
  "MonitorEnter is monitorenter: (locking x ...), synchronized methods. NullPointerException
on null.\n"
  [^any x]
  (.monitorEnter (.Self_Object (asObject x))))

(go/func MonitorExit
  "MonitorExit is monitorexit; IllegalMonitorStateException when the current thread does not
own x's monitor.\n"
  [^any x]
  (.monitorExit (.Self_Object (asObject x))))

(go/func Wait "Wait is Object.wait().\n" [^any x]
  (.monitorWait (.Self_Object (asObject x)) 0))

(go/func WaitTimeout
  "WaitTimeout is Object.wait(long, int): millis 0 and nanos 0 wait forever.\n"
  [^any x ^int64 millis ^int32 nanos]
  (let [o (.Self_Object (asObject x))]
    (when (< millis 0)
      (panic (IllegalArgumentException_New_String (Str "timeout value is negative"))))
    (when (or (< nanos 0) (> nanos 999999))
      (panic (IllegalArgumentException_New_String (Str "nanosecond timeout value out of range"))))
    (let [d (conv time/Duration 0)]
      (if (> millis 9223372036854)
        (set! d (conv time/Duration 9223372036854775807))
        (set! d (+ (* (conv time/Duration millis) time/Millisecond) (conv time/Duration nanos))))
      (.monitorWait o d))))

(go/func Notify "Notify is Object.notify().\n" [^any x]
  (.monitorNotify (.Self_Object (asObject x)) false))

(go/func NotifyAll "NotifyAll is Object.notifyAll().\n" [^any x]
  (.monitorNotify (.Self_Object (asObject x)) true))

(go/func HoldsLock "HoldsLock is Thread.holdsLock(x): whether the current thread owns x's monitor.\n"
  ^bool [^any x]
  (let [o (.Self_Object (asObject x))
        me (currentThreadID)
        w (atomic/LoadUint64 (addr (.-hdr o)))
        l (lockWord w)]
    (switch (bit-and l lockStateMask)
      (case [lockThin]
        (return (== (conv uint64 (>> l 10)) me)))
      (case [lockInflated]
        (.Lock monMu)
        (let [m (aget monitors (>> l 2))]
          (.Unlock monMu)
          (when (or (== m nil) (!= (.-obj m) o))
            (return false))
          (.Lock (.-mu m))
          (let [r (== (.-owner m) me)]
            (.Unlock (.-mu m))
            (return r)))))
    false))
