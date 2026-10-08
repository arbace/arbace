;; jrt: java.util.concurrent.ForkJoinTask and ForkJoinPool, minimal (added with c2g,
;; doc/go/C2G-NOTES.md): the translated RecursiveTask extends ForkJoinTask, and BigInteger's
;; Toom-Cook multiplication runs its RecursiveOp tasks through fork, join and invoke. There is
;; no fork-join pool (JAVA-SURFACE.md: cut): a task runs when it is joined or invoked, in the
;; joining thread, which gives the same results (the parallel path of BigInteger's
;; parallelMultiply runs sequentially).
(in-ns 'go.arbace.jrt)

(go/file "forkjoin.go")

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
    (QuietlyInvoke__V [])))

(go/type ForkJoinTask
  "ForkJoinTask is java.util.concurrent.ForkJoinTask's struct: whether the task ran, and the
exception it threw.\n"
  (struct Object ^bool done ^Throwable_I exc))

(go/var ForkJoinTask_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinTask" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Future_class Serializable_class)
                     :Go "arbace/jrt.ForkJoinTask"))))

(go/method Ctor "Ctor is ForkJoinTask().\n" [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this])
(go/method Self_ForkJoinTask ^{:tag (* ForkJoinTask)} [^{:tag (* ForkJoinTask)} t] t)
(go/method Is_Future [^{:tag (* ForkJoinTask)} t])
(go/method Is_Serializable [^{:tag (* ForkJoinTask)} t])

(go/method run "run executes the task once (exec), keeping its exception.\n"
  [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (when (.-done t)
    (return))
  (set! (.-done t) true)
  (let [exc ((fn [] :results [^Throwable_I exc]
               (defer (Catch (addr exc)))
               (.Exec__Z this)
               (return)))]
    (set! (.-exc t) exc)))

(go/method Impl_Fork__ForkJoinTask "Impl_Fork__ForkJoinTask is fork: the task runs when joined.\n"
  ^ForkJoinTask_I [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  this)

(go/method Impl_Join__O "Impl_Join__O is join: runs the task, rethrows its exception, gives its result.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.run t this)
  (when (!= (.-exc t) nil)
    (panic (.-exc t)))
  (.GetRawResult__O this))

(go/method Impl_Invoke__O "Impl_Invoke__O is invoke: join's result, at once.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.Impl_Join__O t this))

(go/method Impl_QuietlyJoin__V [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (.run t this))
(go/method Impl_QuietlyInvoke__V [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (.run t this))

(go/method Impl_Get__O "Impl_Get__O is get: join's result, its exception as an ExecutionException.\n"
  ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.run t this)
  (when (!= (.-exc t) nil)
    (panic (ExecutionException_New_Throwable (.-exc t))))
  (.GetRawResult__O this))

(go/method Impl_Get_J_TimeUnit__O ^any [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^int64 timeout ^{:tag (* TimeUnit)} unit]
  (.Impl_Get__O t this))

(go/method Impl_Cancel_Z__Z "Impl_Cancel_Z__Z is cancel: a task that runs at join cannot be cancelled once
joined; one not yet run is not cancelled either (false), as Java's for a completed task.\n"
  ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this ^bool may]
  false)

(go/method Impl_IsCancelled__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] false)
(go/method Impl_IsDone__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this] (.-done t))
(go/method Impl_IsCompletedNormally__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (and (.-done t) (== (.-exc t) nil)))
(go/method Impl_IsCompletedAbnormally__Z ^bool [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (and (.-done t) (!= (.-exc t) nil)))
(go/method Impl_GetException__Throwable ^Throwable_I [^{:tag (* ForkJoinTask)} t ^ForkJoinTask_I this]
  (.-exc t))

(go/func ForkJoinTask_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ForkJoinTask_I x)] ok))
(go/func ForkJoinTask_Cast ^ForkJoinTask_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ForkJoinTask_I x)]
    (when (not ok) (panic (ClassCast x ForkJoinTask_class)))
    v))

(go/type ForkJoinPool "ForkJoinPool is java.util.concurrent.ForkJoinPool: no pool, only its statics.\n"
  (struct Object))

(go/var ForkJoinPool_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ForkJoinPool" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class :Go "arbace/jrt.ForkJoinPool"))))

(go/func ForkJoinPool_GetCommonPoolParallelism__I
  "ForkJoinPool_GetCommonPoolParallelism__I is getCommonPoolParallelism: the processors less
one, at least 1, as the JVM's common pool has it.\n"
  ^int32 []
  (let [n (conv int32 (.NumCPU (CurrentHost)))]
    (when (< n 2)
      (return 1))
    (- n 1)))

(go/func init []
  (set! (.-IsInstance (.Info ForkJoinTask_class)) ForkJoinTask_InstanceOf))

(go/method Ref ^any [^{:tag (* ForkJoinPool)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ForkJoinPool)} t] ForkJoinPool_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ForkJoinPool)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* ForkJoinPool)} t] (panic (CloneNotSupported t)))
