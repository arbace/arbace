;; jrt: java.util.concurrent.locks (C2G-SPEC §8.4; doc/go/JRT-NOTES.md, phase 2a): Lock and
;; Condition, ReentrantLock with its conditions, ReentrantReadWriteLock, LockSupport. Each
;; lock's state is under a sync.Mutex; a blocked thread waits on a gate (a channel closed at
;; the next release) together with its interrupt token and a timer, which gives Java's
;; interruptible and timed acquisitions.
(in-ns 'go.arbace.jrt)

(go/file "locks.go"
  :imports [[sync "sync"] [time "time"]])

;; ---------------------------------------------------------------------------------------
;; Waiting

(go/type gate
  "gate is a broadcast: waiters take the channel, the next open closes it (the owner's mutex
held for both).\n"
  (struct ^{:tag (chan (struct))} ch))

(go/method wait ^{:tag (chan (struct))} [^{:tag (* gate)} g]
  (when (== (.-ch g) nil)
    (set! (.-ch g) (make (chan (struct)))))
  (.-ch g))

(go/method open [^{:tag (* gate)} g]
  (when (!= (.-ch g) nil)
    (close (.-ch g))
    (set! (.-ch g) nil)))

(go/const
  [^{:tag int :val 0} woken 0]
  [^{:tag int :val 1} wokenInterrupted 1]
  [^{:tag int :val 2} wokenTimeout 2])

(go/func block
  "block waits for ch, me's interrupt token (when interruptible) or the deadline (when not
zero); which one woke it.\n"
  ^int [^{:tag (chan (struct))} ch ^{:tag (* Thread)} me ^bool interruptible ^{:tag time/Time} deadline]
  (let [^{:tag (chan (struct))} intr nil
        ^{:tag (chan :recv time/Time)} tc nil]
    (when interruptible
      (set! intr (.-intr me)))
    (when (not (.IsZero deadline))
      (let [d (time/Until deadline)]
        (when (<= d 0)
          (return wokenTimeout))
        (let [tm (time/NewTimer d)]
          (defer (.Stop tm))
          (set! tc (.-C tm)))))
    (select
      (case (<! ch) (return woken))
      (case (<! intr) (return wokenInterrupted))
      (case (<! tc) (return wokenTimeout)))))

(go/func deadlineOf "deadlineOf is the deadline nanos from now; zero for none (nanos < 0).\n"
  ^{:tag time/Time} [^int64 nanos]
  (when (< nanos 0)
    (return (lit time/Time)))
  (.Add (time/Now) (conv time/Duration nanos)))

(go/func unitNanos ^int64 [^int64 d ^{:tag (* TimeUnit)} unit]
  (let [n (.ToNanos_J__J (NN unit) d)]
    (when (< n 0)
      (return 0))
    n))

;; ---------------------------------------------------------------------------------------
;; Lock and Condition (interfaces)

(go/type Lock
  "Lock is java.util.concurrent.locks.Lock.\n"
  (interface Object_I
    (Is_Lock [])
    (Lock__V [])
    (LockInterruptibly__V [])
    (TryLock__Z ^bool [])
    (TryLock_J_TimeUnit__Z ^bool [^int64 time ^{:tag (* TimeUnit)} unit])
    (Unlock__V [])
    (NewCondition__Condition ^Condition [])))

(go/var Lock_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.Lock" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.Lock"))))

(go/type Condition
  "Condition is java.util.concurrent.locks.Condition.\n"
  (interface Object_I
    (Is_Condition [])
    (Await__V [])
    (AwaitUninterruptibly__V [])
    (AwaitNanos_J__J ^int64 [^int64 nanos])
    (Await_J_TimeUnit__Z ^bool [^int64 time ^{:tag (* TimeUnit)} unit])
    (Signal__V [])
    (SignalAll__V [])))

(go/var Condition_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.Condition" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.Condition"))))

(go/func Lock_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Lock x)] (dynNominal x Lock_class ok)))
(go/func Lock_Cast ^Lock [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Lock x)]
    (when (not (dynNominal x Lock_class ok)) (panic (ClassCast x Lock_class)))
    v))
(go/func Condition_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Condition x)] (dynNominal x Condition_class ok)))
(go/func Condition_Cast ^Condition [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Condition x)]
    (when (not (dynNominal x Condition_class ok)) (panic (ClassCast x Condition_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; ReentrantLock

(go/type ReentrantLock
  "ReentrantLock is java.util.concurrent.locks.ReentrantLock (leaf): the owner and hold count
under mu; threads blocked in lock wait on the gate. Fairness is not kept (a fair lock
behaves as a non-fair one).\n"
  (struct Object
          ^{:tag sync/Mutex} mu
          ^{:tag (* Thread)} owner
          ^int32 holds
          ^gate g
          ^bool fair))

(go/var ReentrantLock_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.ReentrantLock" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Lock_class Serializable_class)
                     :Go "arbace/jrt.ReentrantLock"))))

(go/func ReentrantLock_New ^{:tag (* ReentrantLock)} [] (addr (lit ReentrantLock)))
(go/func ReentrantLock_New_Z ^{:tag (* ReentrantLock)} [^bool fair] (addr (lit ReentrantLock :fair fair)))

(go/method acquire
  "acquire takes l for me: false when the deadline passed; InterruptedException when
interruptible and interrupted (the status consumed).\n"
  ^bool [^{:tag (* ReentrantLock)} l ^{:tag (* Thread)} me ^bool interruptible ^{:tag time/Time} deadline]
  (when (and interruptible (interruptedNow me))
    (panic (InterruptedException_New)))
  (.Lock (.-mu l))
  (while true
    (cond
      (== (.-owner l) nil)
      (do
        (set! (.-owner l) me)
        (set! (.-holds l) 1)
        (.Unlock (.-mu l))
        (return true))
      (== (.-owner l) me)
      (do
        (when (== (.-holds l) 2147483647)
          (.Unlock (.-mu l))
          (panic (Error_New_String (Str "Maximum lock count exceeded"))))
        (inc! (.-holds l))
        (.Unlock (.-mu l))
        (return true)))
    (let [ch (.wait (addr (.-g l)))]
      (.Unlock (.-mu l))
      (switch (block ch me interruptible deadline)
        (case [wokenInterrupted] (panic (InterruptedException_New)))
        (case [wokenTimeout] (return false)))
      (.Lock (.-mu l)))))

(go/method Lock__V "Lock__V is lock().\n" [^{:tag (* ReentrantLock)} l]
  (.acquire l (CurrentThread) false (lit time/Time)))

(go/method LockInterruptibly__V [^{:tag (* ReentrantLock)} l]
  (.acquire l (CurrentThread) true (lit time/Time)))

(go/method TryLock__Z "TryLock__Z is tryLock(): the lock when it is free or held by the current thread.\n"
  ^bool [^{:tag (* ReentrantLock)} l]
  (let [me (CurrentThread)]
    (.Lock (.-mu l))
    (defer (.Unlock (.-mu l)))
    (cond
      (== (.-owner l) nil) (do (set! (.-owner l) me) (set! (.-holds l) 1) (return true))
      (== (.-owner l) me) (do (inc! (.-holds l)) (return true)))
    false))

(go/method TryLock_J_TimeUnit__Z ^bool [^{:tag (* ReentrantLock)} l ^int64 d ^{:tag (* TimeUnit)} unit]
  (.acquire l (CurrentThread) true (deadlineOf (unitNanos d unit))))

(go/method Unlock__V
  "Unlock__V is unlock(): IllegalMonitorStateException when the current thread does not hold
the lock.\n"
  [^{:tag (* ReentrantLock)} l]
  (let [me (CurrentThread)]
    (.Lock (.-mu l))
    (when (!= (.-owner l) me)
      (.Unlock (.-mu l))
      (panic (IllegalMonitorStateException_New)))
    (dec! (.-holds l))
    (when (== (.-holds l) 0)
      (set! (.-owner l) nil)
      (.open (addr (.-g l))))
    (.Unlock (.-mu l))))

(go/method IsLocked__Z ^bool [^{:tag (* ReentrantLock)} l]
  (.Lock (.-mu l))
  (defer (.Unlock (.-mu l)))
  (!= (.-owner l) nil))

(go/method IsHeldByCurrentThread__Z ^bool [^{:tag (* ReentrantLock)} l]
  (let [me (CurrentThread)]
    (.Lock (.-mu l))
    (defer (.Unlock (.-mu l)))
    (== (.-owner l) me)))

(go/method GetHoldCount__I ^int32 [^{:tag (* ReentrantLock)} l]
  (let [me (CurrentThread)]
    (.Lock (.-mu l))
    (defer (.Unlock (.-mu l)))
    (when (!= (.-owner l) me)
      (return 0))
    (.-holds l)))

(go/method IsFair__Z ^bool [^{:tag (* ReentrantLock)} l] (.-fair l))

(go/method NewCondition__Condition "NewCondition__Condition is newCondition(): a ConditionObject of l.\n"
  ^Condition [^{:tag (* ReentrantLock)} l]
  (addr (lit ReentrantLock_ConditionObject :l l)))

(go/method condOf ^{:tag (* ReentrantLock_ConditionObject)} [^{:tag (* ReentrantLock)} l ^Condition c]
  (when (== c nil)
    (panic (NPE)))
  (let [(values co ok) (assert (* ReentrantLock_ConditionObject) c)]
    (when (or (not ok) (!= (.-l co) l))
      (panic (IllegalArgumentException_New_String (Str "not owner"))))
    (when (!= (.-owner l) (CurrentThread))
      (panic (IllegalMonitorStateException_New)))
    co))

(go/method HasWaiters_Condition__Z "HasWaiters_Condition__Z is hasWaiters(Condition).\n"
  ^bool [^{:tag (* ReentrantLock)} l ^Condition c]
  (.Lock (.-mu l))
  (defer (.Unlock (.-mu l)))
  (> (len (.-waiters (.condOf l c))) 0))

(go/method GetWaitQueueLength_Condition__I ^int32 [^{:tag (* ReentrantLock)} l ^Condition c]
  (.Lock (.-mu l))
  (defer (.Unlock (.-mu l)))
  (conv int32 (len (.-waiters (.condOf l c)))))

(go/method ToString__String
  "ToString__String is ReentrantLock.toString: [Unlocked] or [Locked by thread NAME].\n"
  ^{:tag (* String)} [^{:tag (* ReentrantLock)} l]
  (.Lock (.-mu l))
  (let [o (.-owner l)]
    (.Unlock (.-mu l))
    (let [s (Concat (Object_toString l) (Str "[Unlocked]"))]
      (when (!= o nil)
        (set! s (Concat (Object_toString l) (Str "[Locked by thread ") (.Load (.-name o)) (Str "]"))))
      s)))

(go/method Is_Lock [^{:tag (* ReentrantLock)} l])
(go/method Is_Serializable [^{:tag (* ReentrantLock)} l])
(go/method Ref ^any [^{:tag (* ReentrantLock)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReentrantLock)} t] ReentrantLock_class)
(go/method Clone__O ^any [^{:tag (* ReentrantLock)} t] (panic (CloneNotSupported t)))
(go/func ReentrantLock_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* ReentrantLock) x)] ok))

;; ReentrantLock's conditions

(go/type condWaiter (struct ^{:tag (chan (struct))} ch))

(go/type ReentrantLock_ConditionObject
  "ReentrantLock_ConditionObject is a Condition of a ReentrantLock: its waiters, first in,
first out, under the lock's mu.\n"
  (struct Object ^{:tag (* ReentrantLock)} l ^{:tag (slice (* condWaiter))} waiters))

(go/var ReentrantLock_ConditionObject_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject"
                     :Kind KindClass :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Condition_class Serializable_class)
                     :Simple "ConditionObject" :Go "arbace/jrt.ReentrantLock_ConditionObject"))))

(go/method await
  "await is Condition.await's body: releases the lock fully, waits for a signal, the interrupt
token (when interruptible) or the deadline, takes the lock again with its hold count, and
then throws InterruptedException when it was interrupted. A signal that races with the
interrupt or the timeout wins (the interrupt status is set again), so no signal is lost.
Whether it was signalled (before the deadline).\n"
  ^bool [^{:tag (* ReentrantLock_ConditionObject)} c ^bool interruptible ^{:tag time/Time} deadline]
  (let [l (.-l c)
        me (CurrentThread)]
    (when (and interruptible (interruptedNow me))
      (panic (InterruptedException_New)))
    (.Lock (.-mu l))
    (when (!= (.-owner l) me)
      (.Unlock (.-mu l))
      (panic (IllegalMonitorStateException_New)))
    (let [saved (.-holds l)
          w (addr (lit condWaiter :ch (make (chan (struct)))))]
      (set! (.-waiters c) (append (.-waiters c) w))
      (set! (.-owner l) nil)
      (set! (.-holds l) 0)
      (.open (addr (.-g l)))
      (.Unlock (.-mu l))
      (let [why (block (.-ch w) me interruptible deadline)]
        (.Lock (.-mu l))
        (when (!= why woken)
          (let [found false]
            (range [i x (.-waiters c)]
              (when (== x w)
                (set! (.-waiters c) (append (subslice (.-waiters c) _ i) (spread (subslice (.-waiters c) (+ i 1)))))
                (set! found true)
                (break)))
            (when (not found)
              ;; signalled meanwhile: the signal counts
              (when (== why wokenInterrupted)
                (.Impl_Interrupt__V me me))
              (set! why woken))))
        ;; take the lock again, uninterruptibly
        (while (!= (.-owner l) nil)
          (let [ch (.wait (addr (.-g l)))]
            (.Unlock (.-mu l))
            (<! ch)
            (.Lock (.-mu l))))
        (set! (.-owner l) me)
        (set! (.-holds l) saved)
        (.Unlock (.-mu l))
        (when (== why wokenInterrupted)
          (panic (InterruptedException_New)))
        (== why woken)))))

(go/method Await__V [^{:tag (* ReentrantLock_ConditionObject)} c]
  (.await c true (lit time/Time)))

(go/method AwaitUninterruptibly__V [^{:tag (* ReentrantLock_ConditionObject)} c]
  (.await c false (lit time/Time)))

(go/method AwaitNanos_J__J "AwaitNanos_J__J is awaitNanos: the nanoseconds left (<= 0 when it timed out).\n"
  ^int64 [^{:tag (* ReentrantLock_ConditionObject)} c ^int64 nanos]
  (let [dl (.Add (time/Now) (conv time/Duration (max nanos 0)))]
    (.await c true dl)
    (conv int64 (time/Until dl))))

(go/method Await_J_TimeUnit__Z ^bool [^{:tag (* ReentrantLock_ConditionObject)} c ^int64 d ^{:tag (* TimeUnit)} unit]
  (let [dl (.Add (time/Now) (conv time/Duration (unitNanos d unit)))]
    (.await c true dl)))

(go/method signal [^{:tag (* ReentrantLock_ConditionObject)} c ^bool all]
  (let [l (.-l c)]
    (.Lock (.-mu l))
    (defer (.Unlock (.-mu l)))
    (when (!= (.-owner l) (CurrentThread))
      (panic (IllegalMonitorStateException_New)))
    (while (> (len (.-waiters c)) 0)
      (close (.-ch (aget (.-waiters c) 0)))
      (set! (.-waiters c) (subslice (.-waiters c) 1))
      (when (not all)
        (break)))))

(go/method Signal__V [^{:tag (* ReentrantLock_ConditionObject)} c] (.signal c false))
(go/method SignalAll__V [^{:tag (* ReentrantLock_ConditionObject)} c] (.signal c true))
(go/method Is_Condition [^{:tag (* ReentrantLock_ConditionObject)} c])
(go/method Is_Serializable [^{:tag (* ReentrantLock_ConditionObject)} c])
(go/method Ref ^any [^{:tag (* ReentrantLock_ConditionObject)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReentrantLock_ConditionObject)} t] ReentrantLock_ConditionObject_class)
(go/method Clone__O ^any [^{:tag (* ReentrantLock_ConditionObject)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ReentrantLock_ConditionObject)} t] (Object_toString t))

;; ---------------------------------------------------------------------------------------
;; ReentrantReadWriteLock (MultiFn, Ref, LockingTransaction): reentrant read and write
;; locks; the writer may take the read lock (downgrading); a waiting writer blocks new
;; readers that hold no read lock yet (Java's non-fair policy against writer starvation).

(go/type ReentrantReadWriteLock
  "ReentrantReadWriteLock is java.util.concurrent.locks.ReentrantReadWriteLock (leaf).\n"
  (struct Object
          ^{:tag sync/Mutex} mu
          ^{:tag (* Thread)} writer
          ^int32 wholds
          ^{:tag (map (* Thread) int32)} readers
          ^int32 nreaders
          ^int32 waitingWriters
          ^gate g
          ^{:tag (* ReentrantReadWriteLock_ReadLock)} rl
          ^{:tag (* ReentrantReadWriteLock_WriteLock)} wl))

(go/var ReentrantReadWriteLock_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.ReentrantReadWriteLock" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.ReentrantReadWriteLock"))))

(go/type ReentrantReadWriteLock_ReadLock (struct Object ^{:tag (* ReentrantReadWriteLock)} rw))
(go/type ReentrantReadWriteLock_WriteLock (struct Object ^{:tag (* ReentrantReadWriteLock)} rw))

(go/var ReentrantReadWriteLock_ReadLock_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccStatic) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Lock_class Serializable_class)
                     :Declaring ReentrantReadWriteLock_class :Simple "ReadLock"
                     :Go "arbace/jrt.ReentrantReadWriteLock_ReadLock"))))

(go/var ReentrantReadWriteLock_WriteLock_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccStatic) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Lock_class Serializable_class)
                     :Declaring ReentrantReadWriteLock_class :Simple "WriteLock"
                     :Go "arbace/jrt.ReentrantReadWriteLock_WriteLock"))))

(go/func ReentrantReadWriteLock_New ^{:tag (* ReentrantReadWriteLock)} []
  (let [rw (addr (lit ReentrantReadWriteLock :readers (make (map (* Thread) int32))))]
    (set! (.-rl rw) (addr (lit ReentrantReadWriteLock_ReadLock :rw rw)))
    (set! (.-wl rw) (addr (lit ReentrantReadWriteLock_WriteLock :rw rw)))
    rw))

(go/func ReentrantReadWriteLock_New_Z ^{:tag (* ReentrantReadWriteLock)} [^bool fair]
  (ReentrantReadWriteLock_New))

(go/method ReadLock__ReentrantReadWriteLock_ReadLock ^{:tag (* ReentrantReadWriteLock_ReadLock)}
  [^{:tag (* ReentrantReadWriteLock)} rw] (.-rl rw))
(go/method WriteLock__ReentrantReadWriteLock_WriteLock ^{:tag (* ReentrantReadWriteLock_WriteLock)}
  [^{:tag (* ReentrantReadWriteLock)} rw] (.-wl rw))

(go/method canRead ^bool [^{:tag (* ReentrantReadWriteLock)} rw ^{:tag (* Thread)} me]
  (cond
    (== (.-writer rw) me) (return true)
    (!= (.-writer rw) nil) (return false)
    (> (aget (.-readers rw) me) 0) (return true))
  (== (.-waitingWriters rw) 0))

(go/method acquireRead ^bool [^{:tag (* ReentrantReadWriteLock)} rw ^{:tag (* Thread)} me ^bool interruptible
                              ^bool try ^{:tag time/Time} deadline]
  (when (and interruptible (interruptedNow me))
    (panic (InterruptedException_New)))
  (.Lock (.-mu rw))
  (while (not (.canRead rw me))
    (when try
      (.Unlock (.-mu rw))
      (return false))
    (let [ch (.wait (addr (.-g rw)))]
      (.Unlock (.-mu rw))
      (switch (block ch me interruptible deadline)
        (case [wokenInterrupted] (panic (InterruptedException_New)))
        (case [wokenTimeout] (return false)))
      (.Lock (.-mu rw))))
  (aset (.-readers rw) me (+ (aget (.-readers rw) me) 1))
  (inc! (.-nreaders rw))
  (.Unlock (.-mu rw))
  true)

(go/method releaseRead [^{:tag (* ReentrantReadWriteLock)} rw ^{:tag (* Thread)} me]
  (.Lock (.-mu rw))
  (defer (.Unlock (.-mu rw)))
  (let [n (aget (.-readers rw) me)]
    (when (== n 0)
      (panic (IllegalMonitorStateException_New_String
               (Str "attempt to unlock read lock, not locked by current thread"))))
    (if (== n 1)
      (delete (.-readers rw) me)
      (aset (.-readers rw) me (- n 1)))
    (dec! (.-nreaders rw))
    (when (== (.-nreaders rw) 0)
      (.open (addr (.-g rw))))))

(go/method acquireWrite ^bool [^{:tag (* ReentrantReadWriteLock)} rw ^{:tag (* Thread)} me ^bool interruptible
                               ^bool try ^{:tag time/Time} deadline]
  (when (and interruptible (interruptedNow me))
    (panic (InterruptedException_New)))
  (.Lock (.-mu rw))
  (when (== (.-writer rw) me)
    (inc! (.-wholds rw))
    (.Unlock (.-mu rw))
    (return true))
  (while (or (!= (.-writer rw) nil) (> (.-nreaders rw) 0))
    (when try
      (.Unlock (.-mu rw))
      (return false))
    (let [ch (.wait (addr (.-g rw)))]
      (inc! (.-waitingWriters rw))
      (.Unlock (.-mu rw))
      (let [why (block ch me interruptible deadline)]
        (.Lock (.-mu rw))
        (dec! (.-waitingWriters rw))
        (when (!= why woken)
          ;; readers held back by this writer may go on
          (.open (addr (.-g rw)))
          (.Unlock (.-mu rw))
          (when (== why wokenInterrupted)
            (panic (InterruptedException_New)))
          (return false)))))
  (set! (.-writer rw) me)
  (set! (.-wholds rw) 1)
  (.Unlock (.-mu rw))
  true)

(go/method releaseWrite [^{:tag (* ReentrantReadWriteLock)} rw ^{:tag (* Thread)} me]
  (.Lock (.-mu rw))
  (defer (.Unlock (.-mu rw)))
  (when (!= (.-writer rw) me)
    (panic (IllegalMonitorStateException_New)))
  (dec! (.-wholds rw))
  (when (== (.-wholds rw) 0)
    (set! (.-writer rw) nil)
    (.open (addr (.-g rw)))))

(go/method IsWriteLocked__Z ^bool [^{:tag (* ReentrantReadWriteLock)} rw]
  (.Lock (.-mu rw))
  (defer (.Unlock (.-mu rw)))
  (!= (.-writer rw) nil))

(go/method IsWriteLockedByCurrentThread__Z ^bool [^{:tag (* ReentrantReadWriteLock)} rw]
  (let [me (CurrentThread)]
    (.Lock (.-mu rw))
    (defer (.Unlock (.-mu rw)))
    (== (.-writer rw) me)))

(go/method GetReadLockCount__I ^int32 [^{:tag (* ReentrantReadWriteLock)} rw]
  (.Lock (.-mu rw))
  (defer (.Unlock (.-mu rw)))
  (.-nreaders rw))

(go/method GetReadHoldCount__I ^int32 [^{:tag (* ReentrantReadWriteLock)} rw]
  (let [me (CurrentThread)]
    (.Lock (.-mu rw))
    (defer (.Unlock (.-mu rw)))
    (aget (.-readers rw) me)))

(go/method GetWriteHoldCount__I ^int32 [^{:tag (* ReentrantReadWriteLock)} rw]
  (let [me (CurrentThread)]
    (.Lock (.-mu rw))
    (defer (.Unlock (.-mu rw)))
    (when (!= (.-writer rw) me)
      (return 0))
    (.-wholds rw)))

(go/method Is_Serializable [^{:tag (* ReentrantReadWriteLock)} l])
(go/method Ref ^any [^{:tag (* ReentrantReadWriteLock)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReentrantReadWriteLock)} t] ReentrantReadWriteLock_class)
(go/method Clone__O ^any [^{:tag (* ReentrantReadWriteLock)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ReentrantReadWriteLock)} t] (Object_toString t))

(go/method Lock__V [^{:tag (* ReentrantReadWriteLock_ReadLock)} l]
  (.acquireRead (.-rw l) (CurrentThread) false false (lit time/Time)))
(go/method LockInterruptibly__V [^{:tag (* ReentrantReadWriteLock_ReadLock)} l]
  (.acquireRead (.-rw l) (CurrentThread) true false (lit time/Time)))
(go/method TryLock__Z ^bool [^{:tag (* ReentrantReadWriteLock_ReadLock)} l]
  (.acquireRead (.-rw l) (CurrentThread) false true (lit time/Time)))
(go/method TryLock_J_TimeUnit__Z ^bool [^{:tag (* ReentrantReadWriteLock_ReadLock)} l ^int64 d ^{:tag (* TimeUnit)} unit]
  (.acquireRead (.-rw l) (CurrentThread) true false (deadlineOf (unitNanos d unit))))
(go/method Unlock__V [^{:tag (* ReentrantReadWriteLock_ReadLock)} l]
  (.releaseRead (.-rw l) (CurrentThread)))
(go/method NewCondition__Condition ^Condition [^{:tag (* ReentrantReadWriteLock_ReadLock)} l]
  (panic (UnsupportedOperationException_New)))
(go/method Is_Lock [^{:tag (* ReentrantReadWriteLock_ReadLock)} l])
(go/method Is_Serializable [^{:tag (* ReentrantReadWriteLock_ReadLock)} l])
(go/method Ref ^any [^{:tag (* ReentrantReadWriteLock_ReadLock)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReentrantReadWriteLock_ReadLock)} t] ReentrantReadWriteLock_ReadLock_class)
(go/method Clone__O ^any [^{:tag (* ReentrantReadWriteLock_ReadLock)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ReentrantReadWriteLock_ReadLock)} t] (Object_toString t))

(go/method Lock__V [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.acquireWrite (.-rw l) (CurrentThread) false false (lit time/Time)))
(go/method LockInterruptibly__V [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.acquireWrite (.-rw l) (CurrentThread) true false (lit time/Time)))
(go/method TryLock__Z ^bool [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.acquireWrite (.-rw l) (CurrentThread) false true (lit time/Time)))
(go/method TryLock_J_TimeUnit__Z ^bool [^{:tag (* ReentrantReadWriteLock_WriteLock)} l ^int64 d ^{:tag (* TimeUnit)} unit]
  (.acquireWrite (.-rw l) (CurrentThread) true false (deadlineOf (unitNanos d unit))))
(go/method Unlock__V [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.releaseWrite (.-rw l) (CurrentThread)))
(go/method NewCondition__Condition "NewCondition__Condition: not supported for the write lock in jrt (unused).\n"
  ^Condition [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (panic (UnsupportedOperationException_New)))
(go/method IsHeldByCurrentThread__Z ^bool [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.IsWriteLockedByCurrentThread__Z (.-rw l)))
(go/method GetHoldCount__I ^int32 [^{:tag (* ReentrantReadWriteLock_WriteLock)} l]
  (.GetWriteHoldCount__I (.-rw l)))
(go/method Is_Lock [^{:tag (* ReentrantReadWriteLock_WriteLock)} l])
(go/method Is_Serializable [^{:tag (* ReentrantReadWriteLock_WriteLock)} l])
(go/method Ref ^any [^{:tag (* ReentrantReadWriteLock_WriteLock)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReentrantReadWriteLock_WriteLock)} t] ReentrantReadWriteLock_WriteLock_class)
(go/method Clone__O ^any [^{:tag (* ReentrantReadWriteLock_WriteLock)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ReentrantReadWriteLock_WriteLock)} t] (Object_toString t))

;; ---------------------------------------------------------------------------------------
;; LockSupport: a permit per thread (Thread.park, a channel of one)

(go/func parkFor
  "parkFor blocks the current thread until its permit is available (taking it), it is
interrupted (the status stays set, as Java's) or d passes (d < 0: no limit); it may return
spuriously, as Java's park.\n"
  [^{:tag time/Duration} d]
  (let [me (CurrentThread)
        ^{:tag (chan :recv time/Time)} tc nil]
    (when (> (len (.-intr me)) 0)
      (return))
    (when (>= d 0)
      (when (== d 0)
        (return))
      (let [tm (time/NewTimer d)]
        (defer (.Stop tm))
        (set! tc (.-C tm))))
    (select
      (case (<! (.-park me)))
      (case (<! (.-intr me)) (.Impl_Interrupt__V me me))
      (case (<! tc)))))

(go/func LockSupport_Park__V [] (parkFor -1))
(go/func LockSupport_Park_O__V "LockSupport_Park_O__V is LockSupport.park(blocker): the blocker is not kept.\n"
  [^any blocker] (parkFor -1))
(go/func LockSupport_ParkNanos_J__V [^int64 nanos]
  (when (> nanos 0) (parkFor (conv time/Duration nanos))))
(go/func LockSupport_ParkNanos_O_J__V [^any blocker ^int64 nanos]
  (when (> nanos 0) (parkFor (conv time/Duration nanos))))
(go/func LockSupport_ParkUntil_J__V "LockSupport_ParkUntil_J__V: the deadline in epoch milliseconds.\n"
  [^int64 deadline]
  (let [d (- deadline (System_CurrentTimeMillis__J))]
    (when (> d 0) (parkFor (* (conv time/Duration d) time/Millisecond)))))
(go/func LockSupport_Unpark_Thread__V "LockSupport_Unpark_Thread__V is LockSupport.unpark: gives the permit.\n"
  [^Thread_I t]
  (when (== t nil)
    (return))
  (select
    (case (>! (.-park (.Self_Thread t)) (lit (struct))))
    (default)))

(go/func init []
  (set! (.-IsInstance (.Info Lock_class)) Lock_InstanceOf)
  (set! (.-IsInstance (.Info Condition_class)) Condition_InstanceOf)
  (set! (.-IsInstance (.Info ReentrantLock_class)) ReentrantLock_InstanceOf)
  (set! (.-IsInstance (.Info ReentrantLock_ConditionObject_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* ReentrantLock_ConditionObject) x)] ok)))
  (set! (.-IsInstance (.Info ReentrantReadWriteLock_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* ReentrantReadWriteLock) x)] ok)))
  (set! (.-IsInstance (.Info ReentrantReadWriteLock_ReadLock_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* ReentrantReadWriteLock_ReadLock) x)] ok)))
  (set! (.-IsInstance (.Info ReentrantReadWriteLock_WriteLock_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* ReentrantReadWriteLock_WriteLock) x)] ok))))
