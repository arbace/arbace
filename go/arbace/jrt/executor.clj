;; jrt: executors over goroutines (C2G-SPEC §8.4; JAVA-SURFACE.md decision 4; doc/go/
;; JRT-NOTES.md, phase 2a): Executor, ExecutorService, ThreadFactory, Future, CountDownLatch,
;; Semaphore (the pools and FutureTask are translated since JC5). Every worker is a
;; jrt thread made by the pool's ThreadFactory, as Java's.
(in-ns 'go.arbace.jrt)

(go/file "executor.go"
  :imports [[strconv "strconv"] [sync "sync"] [time "time"]])

;; ---------------------------------------------------------------------------------------
;; The interfaces

(go/type ThreadFactory "ThreadFactory is java.util.concurrent.ThreadFactory.\n"
  (interface Object_I
    (Is_ThreadFactory [])
    (NewThread_Runnable__Thread ^Thread_I [^Runnable r])))

(go/type Executor "Executor is java.util.concurrent.Executor.\n"
  (interface Object_I
    (Is_Executor [])
    (Execute_Runnable__V [^Runnable r])))

(go/type ExecutorService "ExecutorService is java.util.concurrent.ExecutorService (the members jrt has).\n"
  (interface Executor
    (Is_ExecutorService [])
    (Submit_Callable__Future ^Future [^Callable task])
    (Submit_Runnable__Future ^Future [^Runnable task])
    (Submit_Runnable_O__Future ^Future [^Runnable task ^any result])
    (Shutdown__V [])
    (IsShutdown__Z ^bool [])
    (IsTerminated__Z ^bool [])
    (AwaitTermination_J_TimeUnit__Z ^bool [^int64 timeout ^{:tag (* TimeUnit)} unit])
    (Close__V [])))

(go/type Future "Future is java.util.concurrent.Future.\n"
  (interface Object_I
    (Is_Future [])
    (Get__O ^any [])
    (Get_J_TimeUnit__O ^any [^int64 timeout ^{:tag (* TimeUnit)} unit])
    (Cancel_Z__Z ^bool [^bool mayInterruptIfRunning])
    (IsCancelled__Z ^bool [])
    (IsDone__Z ^bool [])))

(go/var ThreadFactory_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ThreadFactory" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.ThreadFactory"))))
(go/var Executor_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.Executor" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.Executor"))))
(go/var ExecutorService_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ExecutorService" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract)
                     :Interfaces (lit (slice (* Class)) Executor_class) :Go "arbace/jrt.ExecutorService"))))
(go/var Future_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.Future" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.Future"))))

(go/func ThreadFactory_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ThreadFactory x)] (dynNominal x ThreadFactory_class ok)))
(go/func Executor_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Executor x)] (dynNominal x Executor_class ok)))
(go/func ExecutorService_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ExecutorService x)] (dynNominal x ExecutorService_class ok)))
(go/func Future_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Future x)] (dynNominal x Future_class ok)))
(go/func Future_Cast ^Future [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Future x)]
    (when (not (dynNominal x Future_class ok)) (panic (ClassCast x Future_class)))
    v))
(go/func ExecutorService_Cast ^ExecutorService [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ExecutorService x)]
    (when (not (dynNominal x ExecutorService_class ok)) (panic (ClassCast x ExecutorService_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; Executors, ThreadPoolExecutor, ScheduledThreadPoolExecutor and FutureTask are translated
;; from jdk26u (doc/go/JRT-NOTES.md, "Concurrency", JC5); jrt keeps the interfaces, which its
;; ForkJoinPool implements and the translated classes implement.

;; ---------------------------------------------------------------------------------------
;; CountDownLatch

(go/type CountDownLatch
  "CountDownLatch is java.util.concurrent.CountDownLatch: the count under mu; done is closed
when it reaches 0.\n"
  (struct Object ^{:tag sync/Mutex} mu ^int64 count ^{:tag (chan (struct))} done))

(go/var CountDownLatch_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.CountDownLatch" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class :Go "arbace/jrt.CountDownLatch"))))

(go/func CountDownLatch_New_I ^{:tag (* CountDownLatch)} [^int32 count]
  (when (< count 0)
    (panic (IllegalArgumentException_New_String (Str "count < 0"))))
  (let [l (addr (lit CountDownLatch :count (conv int64 count) :done (make (chan (struct)))))]
    (when (== count 0)
      (close (.-done l)))
    l))

(go/method Await__V [^{:tag (* CountDownLatch)} l]
  (when (== (block (.-done l) (CurrentThread) true (lit time/Time)) wokenInterrupted)
    (panic (InterruptedException_New))))

(go/method Await_J_TimeUnit__Z ^bool [^{:tag (* CountDownLatch)} l ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (let [me (CurrentThread)]
    (when (interruptedNow me)
      (panic (InterruptedException_New)))
    (switch (block (.-done l) me true (deadlineOf (unitNanos timeout unit)))
      (case [wokenInterrupted] (panic (InterruptedException_New)))
      (case [wokenTimeout]
        (select
          (case (<! (.-done l)) (return true))
          (default (return false)))))
    true))

(go/method CountDown__V [^{:tag (* CountDownLatch)} l]
  (.Lock (.-mu l))
  (when (> (.-count l) 0)
    (dec! (.-count l))
    (when (== (.-count l) 0)
      (close (.-done l))))
  (.Unlock (.-mu l)))

(go/method GetCount__J ^int64 [^{:tag (* CountDownLatch)} l]
  (.Lock (.-mu l))
  (defer (.Unlock (.-mu l)))
  (.-count l))

(go/method ToString__String ^{:tag (* String)} [^{:tag (* CountDownLatch)} l]
  (Concat (Object_toString l) (Str (+ "[Count = " (strconv/FormatInt (.GetCount__J l) 10) "]"))))
(go/method Ref ^any [^{:tag (* CountDownLatch)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* CountDownLatch)} t] CountDownLatch_class)
(go/method Clone__O ^any [^{:tag (* CountDownLatch)} t] (panic (CloneNotSupported t)))
(go/func CountDownLatch_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* CountDownLatch) x)] ok))

;; ---------------------------------------------------------------------------------------
;; Semaphore

(go/type Semaphore
  "Semaphore is java.util.concurrent.Semaphore (leaf), as the locks are: the permits under mu
(they may be negative, as Java's); a thread that must wait takes the gate, which every release
opens, and tries again. waiting counts the threads waiting (getQueueLength). Fairness is not
kept (a fair semaphore behaves as a non-fair one), as for ReentrantLock.\n"
  (struct Object ^{:tag sync/Mutex} mu ^int32 permits ^int32 waiting ^gate g ^bool fair))

(go/var Semaphore_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.Semaphore" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.Semaphore"))))

(go/func Semaphore_New_I ^{:tag (* Semaphore)} [^int32 permits]
  (addr (lit Semaphore :permits permits)))
(go/func Semaphore_New_I_Z ^{:tag (* Semaphore)} [^int32 permits ^bool fair]
  (addr (lit Semaphore :permits permits :fair fair)))

(go/func semArg [^int32 n]
  (when (< n 0)
    (panic (IllegalArgumentException_New))))

(go/method take
  "take takes n permits when there are enough (mu held).\n"
  ^bool [^{:tag (* Semaphore)} s ^int32 n]
  (when (>= (- (.-permits s) n) 0)
    (set! (.-permits s) (- (.-permits s) n))
    (return true))
  false)

(go/method acquire
  "acquire takes n permits for me: false when the deadline passed; InterruptedException when
interruptible and interrupted (the status consumed). Uninterruptible, an interrupt leaves the
status set, as Java's.\n"
  ^bool [^{:tag (* Semaphore)} s ^int32 n ^{:tag (* Thread)} me ^bool interruptible ^{:tag time/Time} deadline]
  (when (and interruptible (interruptedNow me))
    (panic (InterruptedException_New)))
  (.Lock (.-mu s))
  (while true
    (when (.take s n)
      (.Unlock (.-mu s))
      (return true))
    (let [ch (.wait (addr (.-g s)))]
      (inc! (.-waiting s))
      (.Unlock (.-mu s))
      (let [w (block ch me interruptible deadline)]
        (.Lock (.-mu s))
        (dec! (.-waiting s))
        (switch w
          (case [wokenInterrupted] (.Unlock (.-mu s)) (panic (InterruptedException_New)))
          (case [wokenTimeout] (.Unlock (.-mu s)) (return false)))))))

(go/method Acquire__V "Acquire__V is acquire().\n" [^{:tag (* Semaphore)} s]
  (.acquire s 1 (CurrentThread) true (lit time/Time)))
(go/method Acquire_I__V [^{:tag (* Semaphore)} s ^int32 n]
  (semArg n)
  (.acquire s n (CurrentThread) true (lit time/Time)))
(go/method AcquireUninterruptibly__V [^{:tag (* Semaphore)} s]
  (.acquire s 1 (CurrentThread) false (lit time/Time)))
(go/method AcquireUninterruptibly_I__V [^{:tag (* Semaphore)} s ^int32 n]
  (semArg n)
  (.acquire s n (CurrentThread) false (lit time/Time)))

(go/method TryAcquire_I__Z "TryAcquire_I__Z is tryAcquire(int): the permits when there are enough now.\n"
  ^bool [^{:tag (* Semaphore)} s ^int32 n]
  (semArg n)
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (.take s n))
(go/method TryAcquire__Z ^bool [^{:tag (* Semaphore)} s] (.TryAcquire_I__Z s 1))

(go/method TryAcquire_I_J_TimeUnit__Z
  "TryAcquire_I_J_TimeUnit__Z is tryAcquire(int, long, TimeUnit): interruptible; a timeout of
zero or less tries once.\n"
  ^bool [^{:tag (* Semaphore)} s ^int32 n ^int64 d ^{:tag (* TimeUnit)} unit]
  (semArg n)
  (let [nanos (.ToNanos_J__J (NN unit) d)
        me (CurrentThread)]
    (when (<= nanos 0)
      (when (interruptedNow me)
        (panic (InterruptedException_New)))
      (return (.TryAcquire_I__Z s n)))
    (.acquire s n me true (deadlineOf nanos))))
(go/method TryAcquire_J_TimeUnit__Z ^bool [^{:tag (* Semaphore)} s ^int64 d ^{:tag (* TimeUnit)} unit]
  (.TryAcquire_I_J_TimeUnit__Z s 1 d unit))

(go/method Release_I__V
  "Release_I__V is release(int): Error \"Maximum permit count exceeded\" on overflow, as Java's.\n"
  [^{:tag (* Semaphore)} s ^int32 n]
  (semArg n)
  (.Lock (.-mu s))
  (let [next (+ (.-permits s) n)]
    (when (< next (.-permits s))
      (.Unlock (.-mu s))
      (panic (Error_New_String (Str "Maximum permit count exceeded"))))
    (set! (.-permits s) next))
  (.open (addr (.-g s)))
  (.Unlock (.-mu s)))
(go/method Release__V [^{:tag (* Semaphore)} s] (.Release_I__V s 1))

(go/method AvailablePermits__I ^int32 [^{:tag (* Semaphore)} s]
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (.-permits s))

(go/method DrainPermits__I "DrainPermits__I is drainPermits(): the permits, then none (also when negative, as Java's).\n"
  ^int32 [^{:tag (* Semaphore)} s]
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (let [n (.-permits s)]
    (set! (.-permits s) 0)
    n))

(go/method IsFair__Z ^bool [^{:tag (* Semaphore)} s] (.-fair s))
(go/method HasQueuedThreads__Z ^bool [^{:tag (* Semaphore)} s] (> (.GetQueueLength__I s) 0))
(go/method GetQueueLength__I ^int32 [^{:tag (* Semaphore)} s]
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (.-waiting s))

(go/method ToString__String ^{:tag (* String)} [^{:tag (* Semaphore)} s]
  (Concat (Object_toString s) (Str (+ "[Permits = " (strconv/FormatInt (conv int64 (.AvailablePermits__I s)) 10) "]"))))
(go/method Is_Serializable [^{:tag (* Semaphore)} s])
(go/method Ref ^any [^{:tag (* Semaphore)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Semaphore)} t] Semaphore_class)
(go/method Clone__O ^any [^{:tag (* Semaphore)} t] (panic (CloneNotSupported t)))
(go/func Semaphore_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Semaphore) x)] ok))

(go/func init []
  (set! (.-IsInstance (.Info ThreadFactory_class)) ThreadFactory_InstanceOf)
  (set! (.-IsInstance (.Info Executor_class)) Executor_InstanceOf)
  (set! (.-IsInstance (.Info ExecutorService_class)) ExecutorService_InstanceOf)
  (set! (.-IsInstance (.Info Future_class)) Future_InstanceOf)
  (set! (.-IsInstance (.Info CountDownLatch_class)) CountDownLatch_InstanceOf)
  (set! (.-IsInstance (.Info Semaphore_class)) Semaphore_InstanceOf))
