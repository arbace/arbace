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
    (IsDone__Z ^bool [])
    (ResultNow__O ^any [])
    (ExceptionNow__Throwable ^Throwable_I [])
    (State__Future_State ^{:tag (* Future_State)} [])))

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
(go/func Executor_Cast ^Executor [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Executor x)]
    (when (not (dynNominal x Executor_class ok)) (panic (ClassCast x Executor_class)))
    v))
(go/func ThreadFactory_Cast ^ThreadFactory [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ThreadFactory x)]
    (when (not (dynNominal x ThreadFactory_class ok)) (panic (ClassCast x ThreadFactory_class)))
    v))
(go/func ExecutorService_Cast ^ExecutorService [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ExecutorService x)]
    (when (not (dynNominal x ExecutorService_class ok)) (panic (ClassCast x ExecutorService_class)))
    v))

;; ---- Future's default methods (JDK 19) and Future.State

(go/type Future_State "Future_State is the enum java.util.concurrent.Future.State.\n" (struct Enum))

(go/var Future_State_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.Future$State" :Kind KindEnum
                     :Modifiers (bit-or AccPublic AccStatic AccFinal AccEnum) :Super Enum_class
                     :Declaring Future_class :Simple "State" :Go "arbace/jrt.Future_State"))))

(go/func newFutureState ^{:tag (* Future_State)} [^string n ^int32 o]
  (let [t (addr (lit Future_State))]
    (.Ctor_String_I (.-Enum t) t (Intern n) o)
    t))

(go/var
  [^{:tag (* Future_State) :doc "Future_State_RUNNING is Future.State.RUNNING.\n"} Future_State_RUNNING (newFutureState "RUNNING" 0)]
  [^{:tag (* Future_State)} Future_State_SUCCESS (newFutureState "SUCCESS" 1)]
  [^{:tag (* Future_State)} Future_State_FAILED (newFutureState "FAILED" 2)]
  [^{:tag (* Future_State)} Future_State_CANCELLED (newFutureState "CANCELLED" 3)])

(go/func Future_State_Values__Future_State1 ^{:tag (* RefArray)} []
  (RefArrayOf Future_State_class Future_State_RUNNING Future_State_SUCCESS Future_State_FAILED Future_State_CANCELLED))

(go/func Future_State_ValueOf_String__Future_State ^{:tag (* Future_State)} [^{:tag (* String)} n]
  (assert (* Future_State) (Enum_ValueOf_Class_String__Enum Future_State_class n)))

(go/method Ref ^any [^{:tag (* Future_State)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Future_State)} t] Future_State_class)
(go/method Clone__O ^any [^{:tag (* Future_State)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Future_State)} t] (.Impl_ToString__String t t))
(go/method CompareTo_Enum__I ^int32 [^{:tag (* Future_State)} t ^Enum_I o] (.Impl_CompareTo_Enum__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* Future_State)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* Future_State)} t] (.Impl_GetDeclaringClass__Class t t))
(go/func Future_State_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Future_State) x)] ok))

(go/func futureGet
  "futureGet is f.get() for Future's defaults: its result, its exception (ExecutionException,
CancellationException: exc), whether an interrupt was taken (Java's loops retry get then and
set the status again at the end).\n"
  [^Future f] :results [^any v ^Throwable_I exc ^bool interrupted]
  (while true
    (set! exc (runCatching (fn [] (set! v (.Get__O f)))))
    (if (and (!= exc nil) (InterruptedException_InstanceOf exc))
      (set! interrupted true)
      (return))))

(go/func Future_ResultNow__O
  "Future_ResultNow__O is Future's default resultNow(): the result of a task done normally, else
IllegalStateException.\n"
  ^any [^Future this]
  (when (not (.IsDone__Z this))
    (panic (IllegalStateException_New_String (Str "Task has not completed"))))
  (let [(values v exc interrupted) (futureGet this)]
    (when interrupted
      (.Interrupt__V (Thread_CurrentThread__Thread)))
    (when (== exc nil)
      (return v))
    (when (ExecutionException_InstanceOf exc)
      (panic (IllegalStateException_New_String (Str "Task completed with exception"))))
    (when (CancellationException_InstanceOf exc)
      (panic (IllegalStateException_New_String (Str "Task was cancelled"))))
    (panic exc)))

(go/func Future_ExceptionNow__Throwable
  "Future_ExceptionNow__Throwable is Future's default exceptionNow(): the exception of a task
that failed, else IllegalStateException.\n"
  ^Throwable_I [^Future this]
  (when (not (.IsDone__Z this))
    (panic (IllegalStateException_New_String (Str "Task has not completed"))))
  (when (.IsCancelled__Z this)
    (panic (IllegalStateException_New_String (Str "Task was cancelled"))))
  (let [(values _ exc interrupted) (futureGet this)]
    (when interrupted
      (.Interrupt__V (Thread_CurrentThread__Thread)))
    (when (== exc nil)
      (panic (IllegalStateException_New_String (Str "Task completed with a result"))))
    (when (ExecutionException_InstanceOf exc)
      (return (.GetCause__Throwable exc)))
    (panic exc)))

(go/func Future_State__Future_State
  "Future_State__Future_State is Future's default state().\n"
  ^{:tag (* Future_State)} [^Future this]
  (when (not (.IsDone__Z this))
    (return Future_State_RUNNING))
  (when (.IsCancelled__Z this)
    (return Future_State_CANCELLED))
  (let [(values _ exc interrupted) (futureGet this)]
    (when interrupted
      (.Interrupt__V (Thread_CurrentThread__Thread)))
    (when (== exc nil)
      (return Future_State_SUCCESS))
    (when (ExecutionException_InstanceOf exc)
      (return Future_State_FAILED))
    (panic exc)))

(go/func ExecutorService_Close__V
  "ExecutorService_Close__V is ExecutorService's default close() (JDK 19): shutdown, then
waiting for termination a day at a time; an interrupt while waiting is kept and set again at
the end. The JDK's also calls shutdownNow at the first interrupt, which jrt's interface cannot
name (its result is a List, a translated type): here the executor's tasks run on.\n"
  [^ExecutorService this]
  (let [terminated (.IsTerminated__Z this)]
    (when terminated
      (return))
    (.Shutdown__V this)
    (let [interrupted false]
      (while (not terminated)
        (let [exc (runCatching (fn [] (set! terminated (.AwaitTermination_J_TimeUnit__Z this 1 TimeUnit_DAYS))))]
          (when (!= exc nil)
            (if (InterruptedException_InstanceOf exc)
              (set! interrupted true)
              (panic exc)))))
      (when interrupted
        (.Interrupt__V (Thread_CurrentThread__Thread))))))

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
  (set! (.-IsInstance (.Info Future_State_class)) Future_State_InstanceOf)
  (set! (.-Enum (.Info Future_State_class)) Future_State_Values__Future_State1)
  (set! (.-IsInstance (.Info CountDownLatch_class)) CountDownLatch_InstanceOf)
  (set! (.-IsInstance (.Info Semaphore_class)) Semaphore_InstanceOf))
