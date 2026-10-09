;; jrt: executors over goroutines (C2G-SPEC §8.4; JAVA-SURFACE.md decision 4; doc/go/
;; JRT-NOTES.md, phase 2a): Executor, ExecutorService, ThreadFactory, Future, the pools of
;; Executors (cached, fixed, thread per task), FutureTask, CountDownLatch, Semaphore. Every worker is a
;; jrt thread made by the pool's ThreadFactory, as Java's.
(in-ns 'go.arbace.jrt)

(go/file "executor.go"
  :imports [[strconv "strconv"] [sync "sync"] [atomic "sync/atomic"] [time "time"]])

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
;; The default thread factory: pool-N-thread-M, non-daemon, as Java's

(go/var ^{:tag atomic/Int64} poolNumbers)

(go/type defaultThreadFactory (struct Object ^string prefix ^{:tag atomic/Int64} n))

(go/var defaultThreadFactory_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.Executors$DefaultThreadFactory" :Kind KindClass
                     :Modifiers AccStatic :Super Object_class
                     :Interfaces (lit (slice (* Class)) ThreadFactory_class)
                     :Go "arbace/jrt.defaultThreadFactory"))))

(go/func Executors_DefaultThreadFactory__ThreadFactory "Executors_DefaultThreadFactory__ThreadFactory is Executors.defaultThreadFactory().\n"
  ^ThreadFactory []
  (addr (lit defaultThreadFactory :prefix (+ "pool-" (strconv/FormatInt (.Add poolNumbers 1) 10) "-thread-"))))

(go/method NewThread_Runnable__Thread ^Thread_I [^{:tag (* defaultThreadFactory)} f ^Runnable r]
  (let [t (Thread_New_Runnable_String r (Str (+ (.-prefix f) (strconv/FormatInt (.Add (.-n f) 1) 10))))]
    (.Store (.-daemon t) false)
    t))
(go/method Is_ThreadFactory [^{:tag (* defaultThreadFactory)} f])
(go/method Ref ^any [^{:tag (* defaultThreadFactory)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* defaultThreadFactory)} t] defaultThreadFactory_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* defaultThreadFactory)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* defaultThreadFactory)} t] (panic (CloneNotSupported t)))

;; ---------------------------------------------------------------------------------------
;; The pools

(go/type threadPool
  "threadPool is the ExecutorService of Executors' factories: workers made by the factory,
at most max (0: no limit), idle ones kept keepAlive (0: forever; < 0: a worker runs one task,
the thread-per-task executor); tasks queue when every worker is busy and the pool is full.\n"
  (struct Object
          ^{:tag sync/Mutex} mu
          ^ThreadFactory factory
          ^int max
          ^{:tag time/Duration} keepAlive
          ^int workers
          ^{:tag (slice (* poolWorker))} idle
          ^{:tag (slice Runnable)} queue
          ^bool shutdown
          ^bool terminated
          ^{:tag (chan (struct))} done
          ^string kind))

(go/var threadPool_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ThreadPoolExecutor" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) ExecutorService_class)
                     :Go "arbace/jrt.threadPool"))))

(go/func newPool ^{:tag (* threadPool)} [^ThreadFactory f ^int max ^{:tag time/Duration} keepAlive ^string kind]
  (when (== f nil)
    (panic (NPE)))
  (addr (lit threadPool :factory f :max max :keepAlive keepAlive :done (make (chan (struct))) :kind kind)))

(go/func Executors_NewCachedThreadPool__ExecutorService ^ExecutorService []
  (newPool (Executors_DefaultThreadFactory__ThreadFactory) 0 (* 60 time/Second) "cached"))
(go/func Executors_NewCachedThreadPool_ThreadFactory__ExecutorService
  "Executors_NewCachedThreadPool_ThreadFactory__ExecutorService is newCachedThreadPool(factory):
no limit, idle workers kept 60 s.\n"
  ^ExecutorService [^ThreadFactory f]
  (newPool f 0 (* 60 time/Second) "cached"))
(go/func Executors_NewFixedThreadPool_I__ExecutorService ^ExecutorService [^int32 n]
  (Executors_NewFixedThreadPool_I_ThreadFactory__ExecutorService n (Executors_DefaultThreadFactory__ThreadFactory)))
(go/func Executors_NewFixedThreadPool_I_ThreadFactory__ExecutorService
  "Executors_NewFixedThreadPool_I_ThreadFactory__ExecutorService is newFixedThreadPool(n, factory):
at most n workers, kept forever, an unbounded queue.\n"
  ^ExecutorService [^int32 n ^ThreadFactory f]
  (when (<= n 0)
    (panic (IllegalArgumentException_New)))
  (newPool f (conv int n) 0 "fixed"))
(go/func Executors_NewSingleThreadExecutor__ExecutorService ^ExecutorService []
  (newPool (Executors_DefaultThreadFactory__ThreadFactory) 1 0 "fixed"))
(go/func Executors_NewThreadPerTaskExecutor_ThreadFactory__ExecutorService
  "Executors_NewThreadPerTaskExecutor_ThreadFactory__ExecutorService is newThreadPerTaskExecutor:
a new thread of the factory per task (virtual threads: goroutines, as every thread here).\n"
  ^ExecutorService [^ThreadFactory f]
  (newPool f 0 -1 "per-task"))
(go/func Executors_NewVirtualThreadPerTaskExecutor__ExecutorService ^ExecutorService []
  (newPool (.Factory__ThreadFactory (Thread_OfVirtual__Thread_Builder_OfVirtual)) 0 -1 "per-task"))

(go/type poolWorker
  "poolWorker is a worker's Runnable: its first task, then the queue's, then idle until a task
is handed over on ch (nil: the pool shut down).\n"
  (struct Object ^{:tag (* threadPool)} p ^Runnable first ^{:tag (chan Runnable)} ch))

(go/var poolWorker_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ThreadPoolExecutor$Worker" :Kind KindClass
                     :Modifiers (bit-or AccPrivate AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Runnable_class)
                     :Go "arbace/jrt.poolWorker"))))

(go/method rejected [^{:tag (* threadPool)} p ^Runnable r]
  (panic (RejectedExecutionException_New_String
           (Concat (Str "Task ") (StrOfObj r) (Str " rejected from ") (.ToString__String p)))))

(go/method Execute_Runnable__V "Execute_Runnable__V is execute: to an idle worker, a new one, or the queue.\n"
  [^{:tag (* threadPool)} p ^Runnable r]
  (when (== r nil)
    (panic (NPE)))
  (.Lock (.-mu p))
  (when (.-shutdown p)
    (.Unlock (.-mu p))
    (.rejected p r))
  (when (> (len (.-idle p)) 0)
    (let [w (aget (.-idle p) (- (len (.-idle p)) 1))]
      (set! (.-idle p) (subslice (.-idle p) _ (- (len (.-idle p)) 1)))
      (>! (.-ch w) r)
      (.Unlock (.-mu p))
      (return)))
  (when (or (== (.-max p) 0) (< (.-workers p) (.-max p)))
    (inc! (.-workers p))
    (.Unlock (.-mu p))
    (.startWorker p r)
    (return))
  (set! (.-queue p) (append (.-queue p) r))
  (.Unlock (.-mu p)))

(go/method startWorker [^{:tag (* threadPool)} p ^Runnable first]
  (let [w (addr (lit poolWorker :p p :first first :ch (make (chan Runnable) 1)))
        ^Thread_I t nil
        exc (runCatching (fn [] (set! t (.NewThread_Runnable__Thread (.-factory p) w))))]
    (when (or (!= exc nil) (== t nil))
      (.Lock (.-mu p))
      (dec! (.-workers p))
      (.checkTerminated p)
      (.Unlock (.-mu p))
      (when (!= exc nil)
        (panic exc))
      (.rejected p first))
    (.Start__V t)))

(go/method checkTerminated "checkTerminated ends the pool when it is shut down without workers (mu held).\n"
  [^{:tag (* threadPool)} p]
  (when (and (.-shutdown p) (== (.-workers p) 0) (not (.-terminated p)))
    (set! (.-terminated p) true)
    (close (.-done p))))

(go/method Run__V "Run__V is the worker's loop.\n" [^{:tag (* poolWorker)} w]
  (let [p (.-p w)
        task (.-first w)
        normal false]
    (set! (.-first w) nil)
    ;; a task that throws ends its worker (the exception goes to the thread's handlers);
    ;; a replacement takes the queue
    (defer ((fn []
              (when (not normal)
                (.Lock (.-mu p))
                (dec! (.-workers p))
                (if (and (> (len (.-queue p)) 0) (not (.-shutdown p)))
                  (let [r (aget (.-queue p) 0)]
                    (set! (.-queue p) (subslice (.-queue p) 1))
                    (inc! (.-workers p))
                    (.Unlock (.-mu p))
                    (go (.startWorker p r)))
                  (do
                    (.checkTerminated p)
                    (.Unlock (.-mu p))))))))
    (while true
      (when (!= task nil)
        (.Run__V task)
        (set! task nil))
      (.Lock (.-mu p))
      (when (> (len (.-queue p)) 0)
        (set! task (aget (.-queue p) 0))
        (set! (.-queue p) (subslice (.-queue p) 1))
        (.Unlock (.-mu p))
        (continue))
      (when (or (.-shutdown p) (< (.-keepAlive p) 0))
        (dec! (.-workers p))
        (.checkTerminated p)
        (.Unlock (.-mu p))
        (set! normal true)
        (return))
      (set! (.-idle p) (append (.-idle p) w))
      (.Unlock (.-mu p))
      (if (== (.-keepAlive p) 0)
        (set! task (<! (.-ch w)))
        (let [tm (time/NewTimer (.-keepAlive p))]
          (select
            (case [r (<! (.-ch w))] (set! task r))
            (case (<! (.-C tm))
              (.Lock (.-mu p))
              (let [found false]
                (range [i x (.-idle p)]
                  (when (== x w)
                    (set! (.-idle p) (append (subslice (.-idle p) _ i) (spread (subslice (.-idle p) (+ i 1)))))
                    (set! found true)
                    (break)))
                (if found
                  (do
                    (dec! (.-workers p))
                    (.checkTerminated p)
                    (.Unlock (.-mu p))
                    (set! normal true)
                    (return))
                  (do
                    ;; a task is being handed over
                    (.Unlock (.-mu p))
                    (set! task (<! (.-ch w))))))))
          (.Stop tm)))
      (when (== task nil)
        ;; woken by shutdown: the loop ends at the shutdown check
        (continue)))))

(go/method Is_Runnable [^{:tag (* poolWorker)} w])
(go/method Ref ^any [^{:tag (* poolWorker)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* poolWorker)} t] poolWorker_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* poolWorker)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* poolWorker)} t] (panic (CloneNotSupported t)))

(go/method Submit_Callable__Future ^Future [^{:tag (* threadPool)} p ^Callable task]
  (when (== task nil) (panic (NPE)))
  (let [f (addr (lit FutureTask :callable task :done (make (chan (struct)))))]
    (.Execute_Runnable__V p f)
    f))
(go/method Submit_Runnable__Future ^Future [^{:tag (* threadPool)} p ^Runnable task]
  (.Submit_Runnable_O__Future p task nil))
(go/method Submit_Runnable_O__Future ^Future [^{:tag (* threadPool)} p ^Runnable task ^any result]
  (when (== task nil) (panic (NPE)))
  (let [f (addr (lit FutureTask :runnable task :result result :done (make (chan (struct)))))]
    (.Execute_Runnable__V p f)
    f))

(go/method Shutdown__V "Shutdown__V is shutdown(): no new tasks; queued ones still run.\n"
  [^{:tag (* threadPool)} p]
  (.Lock (.-mu p))
  (when (not (.-shutdown p))
    (set! (.-shutdown p) true)
    (range [_ w (.-idle p)]
      (>! (.-ch w) nil))
    (set! (.-idle p) nil)
    (.checkTerminated p))
  (.Unlock (.-mu p)))

(go/method IsShutdown__Z ^bool [^{:tag (* threadPool)} p]
  (.Lock (.-mu p))
  (defer (.Unlock (.-mu p)))
  (.-shutdown p))

(go/method IsTerminated__Z ^bool [^{:tag (* threadPool)} p]
  (.Lock (.-mu p))
  (defer (.Unlock (.-mu p)))
  (.-terminated p))

(go/method AwaitTermination_J_TimeUnit__Z ^bool [^{:tag (* threadPool)} p ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (let [me (CurrentThread)
        dl (deadlineOf (unitNanos timeout unit))]
    (switch (block (.-done p) me true dl)
      (case [wokenInterrupted] (panic (InterruptedException_New)))
      (case [wokenTimeout]
        (select
          (case (<! (.-done p)) (return true))
          (default (return false)))))
    true))

(go/method Close__V "Close__V is close(): shutdown, then wait for the termination.\n" [^{:tag (* threadPool)} p]
  (.Shutdown__V p)
  (<! (.-done p)))

(go/method Is_Executor [^{:tag (* threadPool)} p])
(go/method Is_ExecutorService [^{:tag (* threadPool)} p])
(go/method Ref ^any [^{:tag (* threadPool)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* threadPool)} t] threadPool_class)
(go/method Clone__O ^any [^{:tag (* threadPool)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* threadPool)} p]
  (.Lock (.-mu p))
  (let [state "Running"]
    (cond
      (.-terminated p) (set! state "Terminated")
      (.-shutdown p) (set! state "Shutting down"))
    (let [s (+ "[" state ", pool size = " (strconv/Itoa (.-workers p)) ", active threads = "
               (strconv/Itoa (- (.-workers p) (len (.-idle p)))) ", queued tasks = "
               (strconv/Itoa (len (.-queue p))) "]")]
      (.Unlock (.-mu p))
      (Concat (Object_toString p) (Str s)))))

;; ---------------------------------------------------------------------------------------
;; FutureTask

(go/const
  [^{:tag int32 :val 0} futureNew 0]
  [^{:tag int32 :val 1} futureNormal 1]
  [^{:tag int32 :val 2} futureExceptional 2]
  [^{:tag int32 :val 3} futureCancelled 3])

(go/type FutureTask
  "FutureTask is java.util.concurrent.FutureTask: a Callable's (or a Runnable's) run, its
result or exception, and cancellation. Java's states, simplified: running counts as new, as
in Java (cancel succeeds on a running task).\n"
  (struct Object
          ^{:tag sync/Mutex} mu
          ^Callable callable
          ^Runnable runnable
          ^int32 state
          ^bool started
          ^any result
          ^Throwable_I exc
          ^{:tag (* Thread)} runner
          ^{:tag (chan (struct))} done))

(go/var FutureTask_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.FutureTask" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Runnable_class Future_class)
                     :Go "arbace/jrt.FutureTask"))))

(go/func FutureTask_New_Callable ^{:tag (* FutureTask)} [^Callable c]
  (addr (lit FutureTask :callable (nnIface c) :done (make (chan (struct))))))
(go/func FutureTask_New_Runnable_O ^{:tag (* FutureTask)} [^Runnable r ^any result]
  (addr (lit FutureTask :runnable (nnIface r) :result result :done (make (chan (struct))))))

(go/method Run__V [^{:tag (* FutureTask)} f]
  (.Lock (.-mu f))
  (when (or (!= (.-state f) futureNew) (.-started f))
    (.Unlock (.-mu f))
    (return))
  (set! (.-started f) true)
  (set! (.-runner f) (CurrentThread))
  (.Unlock (.-mu f))
  (let [^any v nil
        exc (runCatching (fn []
                           (if (!= (.-callable f) nil)
                             (set! v (.Call__O (.-callable f)))
                             (do
                               (.Run__V (.-runnable f))
                               (set! v (.-result f))))))]
    (.Lock (.-mu f))
    (set! (.-runner f) nil)
    (when (== (.-state f) futureNew)
      (if (!= exc nil)
        (do
          (set! (.-exc f) exc)
          (set! (.-state f) futureExceptional))
        (do
          (set! (.-result f) v)
          (set! (.-state f) futureNormal)))
      (close (.-done f)))
    (.Unlock (.-mu f))))

(go/method Cancel_Z__Z ^bool [^{:tag (* FutureTask)} f ^bool mayInterrupt]
  (.Lock (.-mu f))
  (defer (.Unlock (.-mu f)))
  (when (!= (.-state f) futureNew)
    (return false))
  (set! (.-state f) futureCancelled)
  (when (and mayInterrupt (!= (.-runner f) nil))
    (.Interrupt__V (.-self (.-runner f))))
  (close (.-done f))
  true)

(go/method IsCancelled__Z ^bool [^{:tag (* FutureTask)} f]
  (.Lock (.-mu f))
  (defer (.Unlock (.-mu f)))
  (== (.-state f) futureCancelled))

(go/method IsDone__Z ^bool [^{:tag (* FutureTask)} f]
  (.Lock (.-mu f))
  (defer (.Unlock (.-mu f)))
  (!= (.-state f) futureNew))

(go/method report ^any [^{:tag (* FutureTask)} f]
  (.Lock (.-mu f))
  (defer (.Unlock (.-mu f)))
  (switch (.-state f)
    (case [futureNormal] (return (.-result f)))
    (case [futureExceptional] (panic (ExecutionException_New_Throwable (.-exc f))))
    (default (panic (CancellationException_New)))))

(go/method Get__O "Get__O is get(): waits; ExecutionException, CancellationException, InterruptedException.\n"
  ^any [^{:tag (* FutureTask)} f]
  (when (== (block (.-done f) (CurrentThread) true (lit time/Time)) wokenInterrupted)
    (panic (InterruptedException_New)))
  (.report f))

(go/method Get_J_TimeUnit__O ^any [^{:tag (* FutureTask)} f ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (switch (block (.-done f) (CurrentThread) true (deadlineOf (unitNanos timeout unit)))
    (case [wokenInterrupted] (panic (InterruptedException_New)))
    (case [wokenTimeout]
      (select
        (case (<! (.-done f)))
        (default (panic (TimeoutException_New))))))
  (.report f))

(go/method Is_Runnable [^{:tag (* FutureTask)} f])
(go/method Is_Future [^{:tag (* FutureTask)} f])
(go/method Ref ^any [^{:tag (* FutureTask)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* FutureTask)} t] FutureTask_class)
(go/method Clone__O ^any [^{:tag (* FutureTask)} t] (panic (CloneNotSupported t)))
(go/method ToString__String "ToString__String is FutureTask.toString: [Completed normally] and the like.\n"
  ^{:tag (* String)} [^{:tag (* FutureTask)} f]
  (.Lock (.-mu f))
  (let [s "[Not completed]"]
    (switch (.-state f)
      (case [futureNormal] (set! s "[Completed normally]"))
      (case [futureExceptional] (set! s (+ "[Completed exceptionally: " (.String (StrOfObj (.-exc f))) "]")))
      (case [futureCancelled] (set! s "[Cancelled]")))
    (.Unlock (.-mu f))
    (Concat (Object_toString f) (Str s))))

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
  (set! (.-IsInstance (.Info Semaphore_class)) Semaphore_InstanceOf)
  (set! (.-IsInstance (.Info FutureTask_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* FutureTask) x)] ok)))
  (set! (.-IsInstance (.Info threadPool_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* threadPool) x)] ok))))
