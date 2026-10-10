;; jrt's tests: monitors (C2G-SPEC §8.1): reentrancy, mutual exclusion, inflation and
;; deflation, wait and notify, the owner checks.
(in-ns 'go.arbace.jrt)

(go/file "monitor_test.go"
  :imports [[runtime "runtime"] [sync "sync"] [atomic "sync/atomic"] [testing "testing"] [time "time"]])

(go/func monitorCount ^int []
  (.Lock monMu)
  (let [n (len monitors)]
    (.Unlock monMu)
    n))

(go/func TestMonitorReentrant [^{:tag (* testing/T)} t]
  (let [o (Object_New)]
    ;; deep recursion overflows the thin lock's count and inflates
    (for [i 0] (< i 1000) (inc! i)
      (MonitorEnter o))
    (when (not (HoldsLock o))
      (.Error t "holdsLock"))
    (for [i 0] (< i 1000) (inc! i)
      (MonitorExit o))
    (when (HoldsLock o)
      (.Error t "held after the last exit"))
    (when (!= (res (fn ^string [] (MonitorExit o) "x")) "!java.lang.IllegalMonitorStateException: current thread is not owner")
      (.Error t "exit of an unowned monitor"))
    (when (!= (res (fn ^string [] (MonitorEnter nil) "x")) "!java.lang.NullPointerException")
      (.Error t "monitorenter null"))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))

(go/func TestMonitorExclusion
  "Goroutines incrementing a plain counter under one monitor lose no update (and the race
detector, bin/jrt test --race, sees the happens-before edges).\n"
  [^{:tag (* testing/T)} t]
  (let [o (Object_New)
        h (IdentityHash o)
        counter 0
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        before (.Load Inflated)]
    (for [g 0] (< g 8) (inc! g)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (for [i 0] (< i 200) (inc! i)
               (MonitorEnter o)
               (MonitorEnter o)
               (set! counter (+ counter 1))
               (MonitorExit o)
               (MonitorExit o))))))
    (.Wait wg)
    (when (!= counter 1600)
      (.Errorf t "counter %d" counter))
    (when (!= (IdentityHash o) h)
      (.Error t "identity hash lost through inflation"))
    (.Logf t "inflations: %d" (- (.Load Inflated) before))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))

(go/func TestWaitNotify [^{:tag (* testing/T)} t]
  (let [o (Object_New)
        ^{:tag (slice int)} queue nil
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        ^atomic/Int64 consumed (zero atomic/Int64)]
    ;; consumers wait until the queue has an item
    (for [c 0] (< c 4) (inc! c)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (for [k 0] (< k 25) (inc! k)
               (MonitorEnter o)
               (while (== (len queue) 0)
                 (Wait o))
               (.Add consumed (conv int64 (aget queue 0)))
               (set! queue (subslice queue 1))
               (MonitorExit o))))))
    (for [i 1] (<= i 100) (inc! i)
      (MonitorEnter o)
      (set! queue (append queue i))
      (Notify o)
      (MonitorExit o))
    (.Wait wg)
    (when (!= (.Load consumed) 5050)
      (.Errorf t "consumed %d" (.Load consumed))))
  ;; notifyAll wakes every waiter
  (let [o (Object_New)
        ready false
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        ^atomic/Int32 woken (zero atomic/Int32)]
    (for [c 0] (< c 5) (inc! c)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (MonitorEnter o)
             (while (not ready)
               (Wait o))
             (.Add woken 1)
             (MonitorExit o)))))
    (time/Sleep (* 20 time/Millisecond))
    (MonitorEnter o)
    (set! ready true)
    (NotifyAll o)
    (MonitorExit o)
    (.Wait wg)
    (when (!= (.Load woken) 5)
      (.Errorf t "woken %d" (.Load woken))))
  ;; a timed wait returns, and the monitor is held again with its recursion count
  (let [o (Object_New)
        start (time/Now)]
    (MonitorEnter o)
    (MonitorEnter o)
    (WaitTimeout o 30 0)
    (when (< (time/Since start) (* 30 time/Millisecond))
      (.Error t "wait(30) returned early"))
    (MonitorExit o)
    (when (not (HoldsLock o))
      (.Error t "the recursion count was not restored"))
    (MonitorExit o)
    (when (!= (res (fn ^string [] (Notify (Str "x")) "x")) "!java.lang.IllegalMonitorStateException: current thread is not owner")
      (.Error t "notify without the monitor"))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))

(go/func monFreeDuplicates
  "monFreeDuplicates counts the indexes the free list holds twice or that a live monitor
holds.\n"
  ^int []
  (.Lock monMu)
  (defer (.Unlock monMu))
  (let [seen (make (map uint32 bool))
        n 0]
    (range [_ i monFree]
      (let [(values _ live) (aget monitors i)]
        (when (or (aget seen i) live)
          (inc! n)))
      (aset seen i true))
    n))

(go/func TestMonitorStaleDeflate
  "A monitorExit that frees an inflated monitor calls tryDeflate after releasing the
monitor's mutex; meanwhile other threads may enter, exit and deflate it, and lock the object
again, thin or inflated with a new monitor at the recycled index. The late tryDeflate must
leave all that alone: it used to reset the header (dropping another thread's lock) and free
the index a second time, so that two monitors shared an index, the table lost the live one,
and lookupMonitor's nil dereference left monMu locked: every thread then blocked on it (the
deadlock in test.generative on Go, 2026-10-10).\n"
  [^{:tag (* testing/T)} t]
  (let [o (Object_New)
        ob (header o)
        h (IdentityHash o)
        m (.inflate ob (atomic/LoadUint64 (addr (.-hdr ob))) 0 0 false)]
    (.tryDeflate ob m)
    (when (!= (monitorCount) 0)
      (.Fatal t "not deflated"))
    ;; locked thin again by this thread: the stale deflate must keep the lock
    (MonitorEnter o)
    (.tryDeflate ob m)
    (when (not (HoldsLock o))
      (.Error t "a stale tryDeflate dropped a thin lock"))
    (MonitorExit o)
    ;; inflated again at the recycled index: the stale deflate must keep the new monitor
    (let [m2 (.inflate ob (atomic/LoadUint64 (addr (.-hdr ob))) 0 0 false)]
      (when (!= (.-idx m2) (.-idx m))
        (.Logf t "index not recycled: %d, %d" (.-idx m) (.-idx m2)))
      (.tryDeflate ob m)
      (when (!= (monitorAt (.-idx m2)) m2)
        (.Error t "a stale tryDeflate freed the live monitor"))
      (when (!= (bit-and (lockWord (atomic/LoadUint64 (addr (.-hdr ob)))) lockStateMask) lockInflated)
        (.Error t "a stale tryDeflate reset an inflated header"))
      (MonitorEnter o)
      (MonitorExit o))
    (when (!= (IdentityHash o) h)
      (.Error t "identity hash lost"))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))
    (when (!= (monFreeDuplicates) 0)
      (.Errorf t "%d indexes freed twice" (monFreeDuplicates)))))

(go/func TestMonitorContention
  "Many goroutines contending for a few monitors, short critical sections: inflation and
deflation race with entries and late tryDeflates (see TestMonitorStaleDeflate).\n"
  [^{:tag (* testing/T)} t]
  (let [^{:tag (slice any)} objs (make (slice any) 3)
        ^{:tag (slice int)} counters (make (slice int) 3)
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        n 20000
        before (.Load Inflated)]
    (when (testing/Short)
      (set! n 2000))
    (for [i 0] (< i 3) (inc! i)
      (aset objs i (Object_New)))
    (for [g 0] (< g 24) (inc! g)
      (.Add wg 1)
      (let [g g]
        (go ((fn []
               (defer (.Done wg))
               (for [i 0] (< i n) (inc! i)
                 (let [k (% (+ i g) 3)
                       o (aget objs k)]
                   (MonitorEnter o)
                   (aset counters k (+ (aget counters k) 1))
                   (MonitorExit o)
                   (when (== (% i 16) 0)
                     (runtime/Gosched)))))))))
    (.Wait wg)
    (when (!= (+ (aget counters 0) (aget counters 1) (aget counters 2)) (* 24 n))
      (.Errorf t "counters %v" counters))
    (.Logf t "inflations: %d" (- (.Load Inflated) before))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))
    (when (!= (monFreeDuplicates) 0)
      (.Errorf t "%d indexes freed twice" (monFreeDuplicates)))))
