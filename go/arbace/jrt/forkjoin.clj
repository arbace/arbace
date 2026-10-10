;; jrt: java.util.concurrent.ForkJoinTask and ForkJoinPool (added with c2g, doc/go/C2G-NOTES.md;
;; a pool since B1a step 5 phase 2B, doc/go/JRT-NOTES.md): the translated RecursiveTask extends
;; ForkJoinTask, BigInteger's Toom-Cook multiplication runs its RecursiveOp tasks through fork,
;; join and invoke, and arbace.core.reducers' fold runs its tasks in a pool.
;;
;; The pool is not the JDK's work-stealing pool (JAVA-SURFACE.md: cut) but has its semantics: a
;; pool of parallelism P runs a task given to it from outside (invoke, execute, submit) in a
;; worker thread of its own, and a task forked in a worker in a new worker thread while fewer
;; than P - 1 forked tasks run, else runs it in the forking thread at once.
;; A task runs once: whoever claims it first (a worker, a joiner, invoke) runs it, the others
;; wait for it. Workers are jrt threads (goroutines) that know their pool, so inForkJoinPool and
;; getPool answer as in a ForkJoinWorkerThread.
(in-ns 'go.arbace.jrt)

(go/file "forkjoin.go"
  :imports [[strconv "strconv"] [sync "sync"] [atomic "sync/atomic"] [time "time"]])

(go/type ForkJoinTask_I "ForkJoinTask_I is java.util.concurrent.ForkJoinTask's class interface.\n"
  (interface Object_I Future Serializable
    (Self_ForkJoinTask ^{:tag (* ForkJoinTask)} [])
    (Exec__Z ^bool [])
    (GetRawResult__O ^any [])
    (SetRawResult_O__V [^any v])
    (Fork__ForkJoinTask ^ForkJoinTask_I [])
    (Join__O ^any [])
    (Invoke__O ^any [])
    (IsCompletedNormally__Z ^bool [])
    (IsCompletedAbnormally__Z ^bool [])
    (GetException__Throwable ^Throwable_I [])
    (QuietlyJoin__V [])
    (QuietlyInvoke__V [])
    (Complete_O__V [^any v])
    (CompleteExceptionally_Throwable__V [^Throwable_I ex])
    (TryUnfork__Z ^bool [])
    (QuietlyComplete__V [])
    (TrySetThrown_Throwable__Z ^bool [^Throwable_I ex])
    (GetForkJoinTaskTag__S ^int16 [])
    (SetForkJoinTaskTag_S__S ^int16 [^int16 v])
    (CompareAndSetForkJoinTaskTag_S_S__Z ^bool [^int16 e ^int16 v])))

(go/type ForkJoinTask
  "ForkJoinTask is java.util.concurrent.ForkJoinTask's struct: the task's state (fjNew, then
fjRunning once claimed, fjDone), the exception it completed with, whether it was cancelled,
and the channel its waiters receive from (made by the first waiter, closed when it is done).\n"
  (struct Object
          ^{:tag atomic/Int32} state
          ^Throwable_I exc
          ^bool cancelled
          ^{:tag sync/Mutex} mu
          ^{:tag (chan (struct))} doneCh
          ^{:tag atomic/Int32 :doc "the task's tag (getForkJoinTaskTag; CompletableFuture's claims)\n"} tag))

(go/const
  [^{:tag int32 :val 0} fjNew iota]
  [^{:val 1} fjRunning]
  [^{:val 2} fjDone])

(go/var ForkJoinTask_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinTask" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Future_class Serializable_class)
                     :Go "arbace/jrt.ForkJoinTask"))))

(go/method Ctor "Ctor is ForkJoinTask().\n" [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this])
(go/method Self_ForkJoinTask ^{:tag (* ForkJoinTask)} [^{:tag (* ForkJoinTask)} t] t)
(go/method Is_Future [^{:tag (* ForkJoinTask)} t])
(go/method GetForkJoinTaskTag__S "GetForkJoinTaskTag__S is the final getForkJoinTaskTag.\n"
  ^int16 [^{:tag (* ForkJoinTask)} t]
  (conv int16 (.Load (.-tag t))))
(go/method SetForkJoinTaskTag_S__S "SetForkJoinTaskTag_S__S is setForkJoinTaskTag: the previous tag.\n"
  ^int16 [^{:tag (* ForkJoinTask)} t ^int16 v]
  (conv int16 (.Swap (.-tag t) (conv int32 v))))
(go/method CompareAndSetForkJoinTaskTag_S_S__Z
  "CompareAndSetForkJoinTaskTag_S_S__Z is compareAndSetForkJoinTaskTag (CompletableFuture's
claim of a completion).\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^int16 e ^int16 v]
  (.CompareAndSwap (.-tag t) (conv int32 e) (conv int32 v)))
(go/method Impl_ResultNow__O "Impl_ResultNow__O is resultNow: Future's default (JDK 19).\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (Future_ResultNow__O this))
(go/method Impl_ExceptionNow__Throwable ^Throwable_I [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (Future_ExceptionNow__Throwable this))
(go/method Impl_State__Future_State ^{:tag (* Future_State)} [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (Future_State__Future_State this))
(go/method Is_Serializable [^{:tag (* ForkJoinTask)} t])

(go/method tryRun
  "tryRun runs the task (exec) when no one has claimed it yet, keeping its exception, and
reports whether it did. A task whose exec returns false (a CountedCompleter) is done only when
completed explicitly (quietlyComplete, tryComplete).\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (when (not (.CompareAndSwap (.-state t) fjNew fjRunning))
    (return false))
  (let [^bool completed false
        exc (runCatching (fn [] (set! completed (.Exec__Z this))))]
    (when (or completed (!= exc nil))
      (.finish t exc)))
  true)

(go/method finish "finish completes the task with exc (nil: normally) and releases its waiters;
it reports false, doing nothing, when the task was done already.\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^Throwable_I exc]
  (.Lock (.-mu t))
  (defer (.Unlock (.-mu t)))
  (when (== (.Load (.-state t)) fjDone)
    (return false))
  (set! (.-exc t) exc)
  (.Store (.-state t) fjDone)
  (when (!= (.-doneCh t) nil)
    (close (.-doneCh t)))
  true)

(go/method await "await waits until the task is done (run by another thread).\n"
  [^{:tag (* ForkJoinTask)} t]
  (when (== (.Load (.-state t)) fjDone)
    (return))
  (.Lock (.-mu t))
  (when (== (.Load (.-state t)) fjDone)
    (.Unlock (.-mu t))
    (return))
  (when (== (.-doneCh t) nil)
    (set! (.-doneCh t) (make (chan (struct)))))
  (let [c (.-doneCh t)]
    (.Unlock (.-mu t))
    (<! c)))

(go/method complete "complete runs the task if no one has, then waits until it is done.\n"
  [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.tryRun t this)
  (.await t))

(go/method QuietlyComplete__V "QuietlyComplete__V is quietlyComplete: the task done normally,
without setting a result (a CountedCompleter's completion).\n"
  [^{:tag (* ForkJoinTask)} t]
  (.CompareAndSwap (.-state t) fjNew fjRunning)
  (.finish t nil))

(go/method TrySetThrown_Throwable__Z "TrySetThrown_Throwable__Z is the package-private
trySetThrown: the task done with ex unless it is done already.\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^Throwable_I ex]
  (.CompareAndSwap (.-state t) fjNew fjRunning)
  (.finish t ex))

(go/method Impl_Fork__ForkJoinTask
  "Impl_Fork__ForkJoinTask is fork: the task goes to the current worker's pool, or the common
pool outside one (ForkJoinPool.spawn: a new worker, or the joiner).\n"
  ^ForkJoinTask_I [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (let [p (currentPool)]
    (when (== p nil)
      (set! p (ForkJoinPool_CommonPool__ForkJoinPool)))
    (.spawn p this))
  this)

(go/method Impl_Join__O "Impl_Join__O is join: the task run or awaited, its exception rethrown, its result.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.complete t this)
  (when (!= (.-exc t) nil)
    (panic (.-exc t)))
  (.GetRawResult__O this))

(go/method Impl_Invoke__O "Impl_Invoke__O is invoke: the task run now (unless it runs elsewhere), then join.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.Impl_Join__O t this))

(go/method Impl_QuietlyJoin__V [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (.complete t this))
(go/method Impl_QuietlyInvoke__V [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (.complete t this))

(go/method Impl_Get__O "Impl_Get__O is get: join's result, its exception as an ExecutionException.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.complete t this)
  (when (!= (.-exc t) nil)
    (when (.-cancelled t)
      (panic (.-exc t)))
    (panic (ExecutionException_New_Throwable (.-exc t))))
  (.GetRawResult__O this))

(go/method Impl_Get_J_TimeUnit__O ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (.Impl_Get__O t this))

(go/method Impl_Cancel_Z__Z "Impl_Cancel_Z__Z is cancel: a task no one has claimed completes with a
CancellationException; a running or completed one is not cancelled (false, true when it was).\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^bool may]
  (when (.CompareAndSwap (.-state t) fjNew fjRunning)
    (set! (.-cancelled t) true)
    (.finish t (CancellationException_New))
    (return true))
  (.await t)
  (.-cancelled t))

(go/method Impl_IsCancelled__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (and (== (.Load (.-state t)) fjDone) (.-cancelled t)))
(go/method Impl_IsDone__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (== (.Load (.-state t)) fjDone))
(go/method Impl_IsCompletedNormally__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (and (== (.Load (.-state t)) fjDone) (== (.-exc t) nil)))
(go/method Impl_IsCompletedAbnormally__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (and (== (.Load (.-state t)) fjDone) (!= (.-exc t) nil)))
(go/method Impl_GetException__Throwable ^Throwable_I [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (when (!= (.Load (.-state t)) fjDone)
    (return nil))
  (.-exc t))

(go/method Impl_Complete_O__V "Impl_Complete_O__V is complete(value): the result set, the task done
normally (one not yet run is not run).\n"
  [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^any v]
  (.SetRawResult_O__V this v)
  (.CompareAndSwap (.-state t) fjNew fjRunning)
  (.finish t nil))

(go/method Impl_CompleteExceptionally_Throwable__V
  "Impl_CompleteExceptionally_Throwable__V is completeExceptionally: the task done with ex, a
checked exception wrapped in a RuntimeException as the JDK wraps it.\n"
  [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^Throwable_I ex]
  (when (and (!= ex nil) (not (RuntimeException_InstanceOf ex)) (not (Error_InstanceOf ex)))
    (set! ex (RuntimeException_New_Throwable ex)))
  (.CompareAndSwap (.-state t) fjNew fjRunning)
  (.finish t ex))

(go/method Impl_TryUnfork__Z "Impl_TryUnfork__Z is tryUnfork: a forked task no one has claimed is
taken back (it then runs when joined or invoked).\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  false)

(go/func ForkJoinTask_InForkJoinPool__Z
  "ForkJoinTask_InForkJoinPool__Z is ForkJoinTask.inForkJoinPool(): whether the current thread
is a pool's worker.\n"
  ^bool []
  (!= (currentPool) nil))

(go/func ForkJoinTask_GetPool__ForkJoinPool
  "ForkJoinTask_GetPool__ForkJoinPool is ForkJoinTask.getPool(): the current worker's pool, or null.\n"
  ^{:tag (* ForkJoinPool)} []
  (currentPool))

(go/func ForkJoinTask_InvokeAll_ForkJoinTask_ForkJoinTask__V
  "ForkJoinTask_InvokeAll_ForkJoinTask_ForkJoinTask__V is invokeAll(t1, t2): t2 forked, t1
invoked, t2 joined.\n"
  [^ForkJoinTask_I t1 ^ForkJoinTask_I t2]
  (nnIface t1)
  (nnIface t2)
  (.Fork__ForkJoinTask t2)
  (.Invoke__O t1)
  (.Join__O t2))

(go/func ForkJoinTask_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ForkJoinTask_I x)] ok))
(go/func ForkJoinTask_Cast ^ForkJoinTask_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ForkJoinTask_I x)]
    (when (not ok) (panic (ClassCast x ForkJoinTask_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; The adapters: ForkJoinTask.adapt of a Callable or a Runnable

(go/type ForkJoinTask_AdaptedCallable
  "ForkJoinTask_AdaptedCallable is java.util.concurrent.ForkJoinTask$AdaptedCallable.\n"
  (struct ForkJoinTask ^Callable callable ^any result))
(go/type ForkJoinTask_AdaptedRunnable
  "ForkJoinTask_AdaptedRunnable is java.util.concurrent.ForkJoinTask$AdaptedRunnable.\n"
  (struct ForkJoinTask ^Runnable runnable ^any result))
(go/type ForkJoinTask_AdaptedRunnableAction
  "ForkJoinTask_AdaptedRunnableAction is java.util.concurrent.ForkJoinTask$AdaptedRunnableAction.\n"
  (struct ForkJoinTask ^Runnable runnable))

(go/var
  [ForkJoinTask_AdaptedCallable_class
   (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinTask$AdaptedCallable" :Kind KindClass
                      :Modifiers (bit-or AccStatic AccFinal) :Super ForkJoinTask_class
                      :Interfaces (lit (slice (* Class)) Runnable_class)
                      :Go "arbace/jrt.ForkJoinTask_AdaptedCallable")))]
  [ForkJoinTask_AdaptedRunnable_class
   (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinTask$AdaptedRunnable" :Kind KindClass
                      :Modifiers (bit-or AccStatic AccFinal) :Super ForkJoinTask_class
                      :Interfaces (lit (slice (* Class)) Runnable_class)
                      :Go "arbace/jrt.ForkJoinTask_AdaptedRunnable")))]
  [ForkJoinTask_AdaptedRunnableAction_class
   (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinTask$AdaptedRunnableAction" :Kind KindClass
                      :Modifiers (bit-or AccStatic AccFinal) :Super ForkJoinTask_class
                      :Interfaces (lit (slice (* Class)) Runnable_class)
                      :Go "arbace/jrt.ForkJoinTask_AdaptedRunnableAction")))])

(go/func ForkJoinTask_Adapt_Callable__ForkJoinTask
  "ForkJoinTask_Adapt_Callable__ForkJoinTask is ForkJoinTask.adapt(Callable): exec calls it, a
checked exception wrapped in a RuntimeException.\n"
  ^ForkJoinTask_I [^Callable c]
  (addr (lit ForkJoinTask_AdaptedCallable :callable (nnIface c))))
(go/func ForkJoinTask_Adapt_Runnable_O__ForkJoinTask
  "ForkJoinTask_Adapt_Runnable_O__ForkJoinTask is ForkJoinTask.adapt(Runnable, result).\n"
  ^ForkJoinTask_I [^Runnable r ^any result]
  (addr (lit ForkJoinTask_AdaptedRunnable :runnable (nnIface r) :result result)))
(go/func ForkJoinTask_Adapt_Runnable__ForkJoinTask
  "ForkJoinTask_Adapt_Runnable__ForkJoinTask is ForkJoinTask.adapt(Runnable).\n"
  ^ForkJoinTask_I [^Runnable r]
  (addr (lit ForkJoinTask_AdaptedRunnableAction :runnable (nnIface r))))

(go/method Exec__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t]
  (let [exc (runCatching (fn [] (set! (.-result t) (.Call__O (.-callable t)))))]
    (when (!= exc nil)
      (when (or (RuntimeException_InstanceOf exc) (Error_InstanceOf exc))
        (panic exc))
      (panic (RuntimeException_New_Throwable exc))))
  true)
(go/method Exec__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Run__V (.-runnable t)) true)
(go/method Exec__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Run__V (.-runnable t)) true)
(go/method GetRawResult__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.-result t))
(go/method GetRawResult__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.-result t))
(go/method GetRawResult__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] nil)
(go/method SetRawResult_O__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t ^any v] (set! (.-result t) v))
(go/method SetRawResult_O__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t ^any v] (set! (.-result t) v))
(go/method SetRawResult_O__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t ^any v])
(go/method Run__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Invoke__O t))
(go/method Run__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Invoke__O t))
(go/method Run__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Invoke__O t))
(go/method Is_Runnable [^{:tag (* ForkJoinTask_AdaptedCallable)} t])
(go/method Is_Runnable [^{:tag (* ForkJoinTask_AdaptedRunnable)} t])
(go/method Is_Runnable [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t])

;; the dispatch methods of the three (C2G-SPEC §5.4: forwarders to ForkJoinTask's Impl_)
(go/method Fork__ForkJoinTask ^ForkJoinTask_I [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_Fork__ForkJoinTask (addr (.-ForkJoinTask t)) t))
(go/method Join__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_Join__O (addr (.-ForkJoinTask t)) t))
(go/method Invoke__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_Invoke__O (addr (.-ForkJoinTask t)) t))
(go/method QuietlyJoin__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_QuietlyJoin__V (addr (.-ForkJoinTask t)) t))
(go/method QuietlyInvoke__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_QuietlyInvoke__V (addr (.-ForkJoinTask t)) t))
(go/method Get__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_Get__O (addr (.-ForkJoinTask t)) t))
(go/method Get_J_TimeUnit__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t ^int64 timeout ^{:tag (* TimeUnit)} unit] (.Impl_Get_J_TimeUnit__O (addr (.-ForkJoinTask t)) t timeout unit))
(go/method Cancel_Z__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t ^bool may] (.Impl_Cancel_Z__Z (addr (.-ForkJoinTask t)) t may))
(go/method IsCancelled__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_IsCancelled__Z (addr (.-ForkJoinTask t)) t))
(go/method IsDone__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_IsDone__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedNormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_IsCompletedNormally__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedAbnormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_IsCompletedAbnormally__Z (addr (.-ForkJoinTask t)) t))
(go/method GetException__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_GetException__Throwable (addr (.-ForkJoinTask t)) t))
(go/method Complete_O__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t ^any v] (.Impl_Complete_O__V (addr (.-ForkJoinTask t)) t v))
(go/method CompleteExceptionally_Throwable__V [^{:tag (* ForkJoinTask_AdaptedCallable)} t ^Throwable_I ex] (.Impl_CompleteExceptionally_Throwable__V (addr (.-ForkJoinTask t)) t ex))
(go/method TryUnfork__Z ^bool [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_TryUnfork__Z (addr (.-ForkJoinTask t)) t))
(go/method ResultNow__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_ResultNow__O (addr (.-ForkJoinTask t)) t))
(go/method ExceptionNow__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_ExceptionNow__Throwable (addr (.-ForkJoinTask t)) t))
(go/method State__Future_State ^{:tag (* Future_State)} [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (.Impl_State__Future_State (addr (.-ForkJoinTask t)) t))
(go/method Ref ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ForkJoinTask_AdaptedCallable)} t] ForkJoinTask_AdaptedCallable_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ForkJoinTask_AdaptedCallable)} t]
  (Concat (Object_toString t) (Str "[Wrapped task = ") (StrOfObj (.-callable t)) (Str "]")))
(go/method Clone__O ^any [^{:tag (* ForkJoinTask_AdaptedCallable)} t] (panic (CloneNotSupported t)))

(go/method Fork__ForkJoinTask ^ForkJoinTask_I [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_Fork__ForkJoinTask (addr (.-ForkJoinTask t)) t))
(go/method Join__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_Join__O (addr (.-ForkJoinTask t)) t))
(go/method Invoke__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_Invoke__O (addr (.-ForkJoinTask t)) t))
(go/method QuietlyJoin__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_QuietlyJoin__V (addr (.-ForkJoinTask t)) t))
(go/method QuietlyInvoke__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_QuietlyInvoke__V (addr (.-ForkJoinTask t)) t))
(go/method Get__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_Get__O (addr (.-ForkJoinTask t)) t))
(go/method Get_J_TimeUnit__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t ^int64 timeout ^{:tag (* TimeUnit)} unit] (.Impl_Get_J_TimeUnit__O (addr (.-ForkJoinTask t)) t timeout unit))
(go/method Cancel_Z__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t ^bool may] (.Impl_Cancel_Z__Z (addr (.-ForkJoinTask t)) t may))
(go/method IsCancelled__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_IsCancelled__Z (addr (.-ForkJoinTask t)) t))
(go/method IsDone__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_IsDone__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedNormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_IsCompletedNormally__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedAbnormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_IsCompletedAbnormally__Z (addr (.-ForkJoinTask t)) t))
(go/method GetException__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_GetException__Throwable (addr (.-ForkJoinTask t)) t))
(go/method Complete_O__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t ^any v] (.Impl_Complete_O__V (addr (.-ForkJoinTask t)) t v))
(go/method CompleteExceptionally_Throwable__V [^{:tag (* ForkJoinTask_AdaptedRunnable)} t ^Throwable_I ex] (.Impl_CompleteExceptionally_Throwable__V (addr (.-ForkJoinTask t)) t ex))
(go/method TryUnfork__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_TryUnfork__Z (addr (.-ForkJoinTask t)) t))
(go/method ResultNow__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_ResultNow__O (addr (.-ForkJoinTask t)) t))
(go/method ExceptionNow__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_ExceptionNow__Throwable (addr (.-ForkJoinTask t)) t))
(go/method State__Future_State ^{:tag (* Future_State)} [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (.Impl_State__Future_State (addr (.-ForkJoinTask t)) t))
(go/method Ref ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] ForkJoinTask_AdaptedRunnable_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ForkJoinTask_AdaptedRunnable)} t]
  (Concat (Object_toString t) (Str "[Wrapped task = ") (StrOfObj (.-runnable t)) (Str "]")))
(go/method Clone__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnable)} t] (panic (CloneNotSupported t)))

(go/method Fork__ForkJoinTask ^ForkJoinTask_I [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_Fork__ForkJoinTask (addr (.-ForkJoinTask t)) t))
(go/method Join__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_Join__O (addr (.-ForkJoinTask t)) t))
(go/method Invoke__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_Invoke__O (addr (.-ForkJoinTask t)) t))
(go/method QuietlyJoin__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_QuietlyJoin__V (addr (.-ForkJoinTask t)) t))
(go/method QuietlyInvoke__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_QuietlyInvoke__V (addr (.-ForkJoinTask t)) t))
(go/method Get__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_Get__O (addr (.-ForkJoinTask t)) t))
(go/method Get_J_TimeUnit__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t ^int64 timeout ^{:tag (* TimeUnit)} unit] (.Impl_Get_J_TimeUnit__O (addr (.-ForkJoinTask t)) t timeout unit))
(go/method Cancel_Z__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t ^bool may] (.Impl_Cancel_Z__Z (addr (.-ForkJoinTask t)) t may))
(go/method IsCancelled__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_IsCancelled__Z (addr (.-ForkJoinTask t)) t))
(go/method IsDone__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_IsDone__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedNormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_IsCompletedNormally__Z (addr (.-ForkJoinTask t)) t))
(go/method IsCompletedAbnormally__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_IsCompletedAbnormally__Z (addr (.-ForkJoinTask t)) t))
(go/method GetException__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_GetException__Throwable (addr (.-ForkJoinTask t)) t))
(go/method Complete_O__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t ^any v] (.Impl_Complete_O__V (addr (.-ForkJoinTask t)) t v))
(go/method CompleteExceptionally_Throwable__V [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t ^Throwable_I ex] (.Impl_CompleteExceptionally_Throwable__V (addr (.-ForkJoinTask t)) t ex))
(go/method TryUnfork__Z ^bool [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_TryUnfork__Z (addr (.-ForkJoinTask t)) t))
(go/method ResultNow__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_ResultNow__O (addr (.-ForkJoinTask t)) t))
(go/method ExceptionNow__Throwable ^Throwable_I [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_ExceptionNow__Throwable (addr (.-ForkJoinTask t)) t))
(go/method State__Future_State ^{:tag (* Future_State)} [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (.Impl_State__Future_State (addr (.-ForkJoinTask t)) t))
(go/method Ref ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] ForkJoinTask_AdaptedRunnableAction_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t]
  (Concat (Object_toString t) (Str "[Wrapped task = ") (StrOfObj (.-runnable t)) (Str "]")))
(go/method Clone__O ^any [^{:tag (* ForkJoinTask_AdaptedRunnableAction)} t] (panic (CloneNotSupported t)))

;; ---------------------------------------------------------------------------------------
;; ForkJoinPool

(go/type ForkJoinPool
  "ForkJoinPool is java.util.concurrent.ForkJoinPool: its parallelism, the slots of the
workers forked tasks may start (parallelism - 1: the thread that forks works too), its
workers' name prefix and count, whether it was shut down, its running workers (for
termination: done is closed when the pool is shut down with none running).\n"
  (struct Object
          ^int32 parallelism
          ^{:tag (chan (struct))} slots
          ^string prefix
          ^{:tag atomic/Int64} workers
          ^{:tag atomic/Bool} shutdown
          ^{:tag sync/Mutex} mu
          ^int64 running
          ^{:tag (chan (struct))} done
          ^bool terminated))

(go/var ForkJoinPool_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinPool" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) ExecutorService_class)
                     :Go "arbace/jrt.ForkJoinPool"))))

;; ---------------------------------------------------------------------------------------
;; ForkJoinPool.ManagedBlocker and managedBlock (JC7)

(go/type ForkJoinPool_ManagedBlocker
  "ForkJoinPool_ManagedBlocker is java.util.concurrent.ForkJoinPool$ManagedBlocker.\n"
  (interface Object_I
    (Is_ForkJoinPool_ManagedBlocker [])
    (Block__Z ^bool [])
    (IsReleasable__Z ^bool [])))

(go/var ForkJoinPool_ManagedBlocker_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinPool$ManagedBlocker" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccStatic AccInterface AccAbstract)
                     :Declaring ForkJoinPool_class :Simple "ManagedBlocker"
                     :Go "arbace/jrt.ForkJoinPool_ManagedBlocker"))))

(go/func ForkJoinPool_ManagedBlocker_InstanceOf ^bool [^any x]
  (let [(values _ ok) (assert ForkJoinPool_ManagedBlocker x)] (dynNominal x ForkJoinPool_ManagedBlocker_class ok)))

(go/func ForkJoinPool_ManagedBlocker_Cast ^ForkJoinPool_ManagedBlocker [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ForkJoinPool_ManagedBlocker x)]
    (when (not (dynNominal x ForkJoinPool_ManagedBlocker_class ok)) (panic (ClassCast x ForkJoinPool_ManagedBlocker_class)))
    v))

(go/func ForkJoinPool_ManagedBlock_ForkJoinPool_ManagedBlocker__V
  "ForkJoinPool_ManagedBlock_ForkJoinPool_ManagedBlocker__V is ForkJoinPool.managedBlock: the
blocker's block until it is releasable, as the JDK does in a thread that is not a pool's
worker. jrt's workers need no compensation: a forked task finds no free worker and runs in the
forking thread, a task given from outside always gets a worker of its own (C2G-SPEC §8.4).\n"
  [^ForkJoinPool_ManagedBlocker b]
  (when (== b nil)
    (panic (NPE)))
  (while (and (not (.IsReleasable__Z b)) (not (.Block__Z b)))))

(go/var ^{:tag atomic/Int64 :doc "poolIds numbers the pools (ForkJoinPool-N-worker-M).\n"} poolIds)

(go/func newForkJoinPool ^{:tag (* ForkJoinPool)} [^int32 parallelism ^string prefix]
  (when (or (<= parallelism 0) (> parallelism 0x7fff))
    (panic (IllegalArgumentException_New)))
  (let [p (addr (lit ForkJoinPool :parallelism parallelism :prefix prefix))]
    (when (> parallelism 1)
      (set! (.-slots p) (make (chan (struct)) (- parallelism 1))))
    p))

(go/func ForkJoinPool_New "ForkJoinPool_New is new ForkJoinPool(): parallelism the processors.\n"
  ^{:tag (* ForkJoinPool)} []
  (ForkJoinPool_New_I (conv int32 (.NumCPU (CurrentHost)))))

(go/func ForkJoinPool_New_I "ForkJoinPool_New_I is new ForkJoinPool(parallelism).\n"
  ^{:tag (* ForkJoinPool)} [^int32 parallelism]
  (newForkJoinPool parallelism (+ "ForkJoinPool-" (strconv/FormatInt (.Add poolIds 1) 10) "-worker-")))

(go/var ^{:tag sync/Once} commonOnce)
(go/var ^{:tag (* ForkJoinPool)} commonPool)

(go/func ForkJoinPool_CommonPool__ForkJoinPool
  "ForkJoinPool_CommonPool__ForkJoinPool is ForkJoinPool.commonPool().\n"
  ^{:tag (* ForkJoinPool)} []
  (.Do commonOnce (fn [] (set! commonPool (newForkJoinPool (ForkJoinPool_GetCommonPoolParallelism__I)
                                                          "ForkJoinPool.commonPool-worker-"))))
  commonPool)

(go/func ForkJoinPool_AsyncCommonPool__ForkJoinPool
  "ForkJoinPool_AsyncCommonPool__ForkJoinPool is the package-private asyncCommonPool
(CompletableFuture's default executor): the common pool, whose parallelism is at least 1 and
whose tasks given from outside each get a worker (§8.4), so asynchronous tasks never run in
the caller.\n"
  ^{:tag (* ForkJoinPool)} []
  (ForkJoinPool_CommonPool__ForkJoinPool))

(go/func ForkJoinPool_GetCommonPoolParallelism__I
  "ForkJoinPool_GetCommonPoolParallelism__I is getCommonPoolParallelism: the processors less
one, at least 1, as the JVM's common pool has it.\n"
  ^int32 []
  (let [n (conv int32 (.NumCPU (CurrentHost)))]
    (when (< n 2)
      (return 1))
    (- n 1)))

(go/func currentPool "currentPool is the pool whose worker the current thread is, or nil.\n"
  ^{:tag (* ForkJoinPool)} []
  (.-fjPool (.Self_Thread (CurrentThread))))

(go/method startWorker
  "startWorker runs the task in a new worker thread of the pool (a daemon), then calls done.\n"
  [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task ^{:tag (func [])} done]
  (.Lock (.-mu p))
  (inc! (.-running p))
  (.Unlock (.-mu p))
  (let [name (Str (+ (.-prefix p) (strconv/FormatInt (.Add (.-workers p) 1) 10)))
        t (Thread_New_Runnable_String
            (RunnableOf (fn []
                          (defer (.workerDone p))
                          (defer (done))
                          (.tryRun (.Self_ForkJoinTask task) task)))
            name)]
    (set! (.-fjPool t) p)
    (.Store (.-daemon t) true)
    (.Start__V t)))

(go/method spawn
  "spawn is a forked task's start: a new worker when a slot is free, else the forking thread
runs it at once.\n"
  [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task]
  (when (== (.-slots p) nil)
    (.tryRun (.Self_ForkJoinTask task) task)
    (return))
  (select
    (case (>! (.-slots p) (lit (struct)))
      (.startWorker p task (fn [] (<! (.-slots p)))))
    (default
      ;; no worker free: the forking thread runs it now (a CountedCompleter's children are
      ;; never joined, so leaving them to a joiner could leave them unrun)
      (.tryRun (.Self_ForkJoinTask task) task))))

(go/method checkOpen [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task]
  (nnIface task)
  (when (.Load (.-shutdown p))
    (panic (RejectedExecutionException_New))))

(go/method Invoke_ForkJoinTask__O
  "Invoke_ForkJoinTask__O is invoke: in a worker the task runs at once (invoke), else in a
worker of the pool, the caller waiting for its result.\n"
  ^any [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task]
  (.checkOpen p task)
  (when (!= (currentPool) nil)
    (return (.Invoke__O task)))
  (.startWorker p task (fn []))
  (let [t (.Self_ForkJoinTask task)]
    (.await t)
    (when (!= (.-exc t) nil)
      (panic (.-exc t)))
    (.GetRawResult__O task)))

(go/method Execute_ForkJoinTask__V "Execute_ForkJoinTask__V is execute: the task in a worker of the pool.\n"
  [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task]
  (.checkOpen p task)
  (.startWorker p task (fn [])))

(go/method Submit_ForkJoinTask__ForkJoinTask "Submit_ForkJoinTask__ForkJoinTask is submit: execute, the task returned.\n"
  ^ForkJoinTask_I [^{:tag (* ForkJoinPool)} p ^ForkJoinTask_I task]
  (.Execute_ForkJoinTask__V p task)
  task)

(go/method GetParallelism__I ^int32 [^{:tag (* ForkJoinPool)} p] (.-parallelism p))
(go/method Shutdown__V "Shutdown__V is shutdown: no new tasks (the common pool is never shut down).\n"
  [^{:tag (* ForkJoinPool)} p]
  (when (!= p commonPool)
    (.Store (.-shutdown p) true)
    (.Lock (.-mu p))
    (.checkTerminated p)
    (.Unlock (.-mu p))))
(go/method IsShutdown__Z ^bool [^{:tag (* ForkJoinPool)} p] (.Load (.-shutdown p)))

;; ---- termination and the ExecutorService API (JC7)

(go/method workerDone "workerDone is a worker's end: the pool may terminate.\n" [^{:tag (* ForkJoinPool)} p]
  (.Lock (.-mu p))
  (dec! (.-running p))
  (.checkTerminated p)
  (.Unlock (.-mu p)))

(go/method checkTerminated
  "checkTerminated ends the pool when it is shut down with no worker running (mu held).\n"
  [^{:tag (* ForkJoinPool)} p]
  (when (and (.Load (.-shutdown p)) (== (.-running p) 0) (not (.-terminated p)))
    (set! (.-terminated p) true)
    (when (!= (.-done p) nil)
      (close (.-done p)))))

(go/method IsTerminated__Z "IsTerminated__Z is isTerminated: shut down, no worker running.\n"
  ^bool [^{:tag (* ForkJoinPool)} p]
  (.Lock (.-mu p))
  (defer (.Unlock (.-mu p)))
  (.-terminated p))

(go/method IsQuiescent__Z "IsQuiescent__Z is isQuiescent: no worker running.\n"
  ^bool [^{:tag (* ForkJoinPool)} p]
  (.Lock (.-mu p))
  (defer (.Unlock (.-mu p)))
  (== (.-running p) 0))

(go/method AwaitTermination_J_TimeUnit__Z
  "AwaitTermination_J_TimeUnit__Z is awaitTermination: true once the pool terminates, false
when the time passes first; the common pool, never shut down, waits for quiescence as the
JDK's does (awaitQuiescence) and answers false.\n"
  ^bool [^{:tag (* ForkJoinPool)} p ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (let [nanos (.ToNanos_J__J (NN unit) timeout)
        deadline (time/Now)]
    (set! deadline (.Add deadline (conv time/Duration nanos)))
    (when (== p commonPool)
      (while (not (.IsQuiescent__Z p))
        (when (.After (time/Now) deadline)
          (return false))
        (Thread_Sleep_J__V 1))
      (return false))
    (.Lock (.-mu p))
    (when (.-terminated p)
      (.Unlock (.-mu p))
      (return true))
    (when (== (.-done p) nil)
      (set! (.-done p) (make (chan (struct)))))
    (let [done (.-done p)]
      (.Unlock (.-mu p))
      (when (<= nanos 0)
        (return false))
      (let [tm (time/NewTimer (conv time/Duration nanos))
            me (CurrentThread)]
        (defer (.Stop tm))
        (select
          (case (<! done) (return true))
          (case (<! (.-intr me)) (panic (InterruptedException_New)))
          (case (<! (.-C tm)) (return false)))))))

(go/method Close__V "Close__V is close(): shutdown, then waiting for termination (not the common pool).\n"
  [^{:tag (* ForkJoinPool)} p]
  (when (== p commonPool)
    (return))
  (.Shutdown__V p)
  (while (not (.AwaitTermination_J_TimeUnit__Z p 1 TimeUnit_DAYS))))

(go/method Execute_Runnable__V
  "Execute_Runnable__V is execute(Runnable): a ForkJoinTask as itself, another Runnable adapted
(its exception goes to the worker's uncaught-exception handler, as the JDK's
RunnableExecuteAction does).\n"
  [^{:tag (* ForkJoinPool)} p ^Runnable r]
  (when (== r nil)
    (panic (NPE)))
  (let [(values t ok) (assert ForkJoinTask_I r)]
    (when ok
      (.Execute_ForkJoinTask__V p t)
      (return)))
  (.Execute_ForkJoinTask__V p (ForkJoinTask_Adapt_Runnable__ForkJoinTask
                                (RunnableOf (fn [] (let [exc (runCatching (fn [] (.Run__V r)))]
                                                     (when (!= exc nil)
                                                       (let [me (CurrentThread)]
                                                         (.dispatchUncaught me exc)))))))))

(go/method Submit_Callable__Future "Submit_Callable__Future is submit(Callable): the adapted task.\n"
  ^Future [^{:tag (* ForkJoinPool)} p ^Callable c]
  (when (== c nil) (panic (NPE)))
  (let [t (ForkJoinTask_Adapt_Callable__ForkJoinTask c)]
    (.Execute_ForkJoinTask__V p t)
    t))
(go/method Submit_Runnable__Future ^Future [^{:tag (* ForkJoinPool)} p ^Runnable r]
  (when (== r nil) (panic (NPE)))
  (let [(values ft ok) (assert ForkJoinTask_I r)]
    (when ok
      (.Execute_ForkJoinTask__V p ft)
      (return ft)))
  (let [t (ForkJoinTask_Adapt_Runnable__ForkJoinTask r)]
    (.Execute_ForkJoinTask__V p t)
    t))
(go/method Submit_Runnable_O__Future ^Future [^{:tag (* ForkJoinPool)} p ^Runnable r ^any result]
  (when (== r nil) (panic (NPE)))
  (let [t (ForkJoinTask_Adapt_Runnable_O__ForkJoinTask r result)]
    (.Execute_ForkJoinTask__V p t)
    t))
(go/method Is_Executor [^{:tag (* ForkJoinPool)} p])
(go/method Is_ExecutorService [^{:tag (* ForkJoinPool)} p])
(go/method Is_AutoCloseable "Is_AutoCloseable: ExecutorService is an AutoCloseable (JDK 19).\n" [^{:tag (* ForkJoinPool)} p])

(go/func ForkJoinPool_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* ForkJoinPool) x)] ok))

(go/func init []
  (set! (.-IsInstance (.Info ForkJoinTask_class)) ForkJoinTask_InstanceOf)
  (set! (.-IsInstance (.Info ForkJoinPool_class)) ForkJoinPool_InstanceOf)
  (set! (.-IsInstance (.Info ForkJoinPool_ManagedBlocker_class)) ForkJoinPool_ManagedBlocker_InstanceOf))

(go/method Ref ^any [^{:tag (* ForkJoinPool)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ForkJoinPool)} t] ForkJoinPool_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ForkJoinPool)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* ForkJoinPool)} t] (panic (CloneNotSupported t)))
