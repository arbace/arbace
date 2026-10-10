;; jrt's stand-in for java.util.concurrent.ExecutorService (doc/go/JRT-NOTES.md, "Stand-ins" and
;; "Concurrency", JC5): the interface as jrt's own build sees it, which its ForkJoinPool
;; implements. In the program ExecutorService is translated from jdk26u (all its methods,
;; invokeAll, invokeAny and shutdownNow among them, and its default close) and replaces this
;; file's definitions; c2g gives ForkJoinPool the methods whose types jrt cannot name
;; (C2G-SPEC §11, c2g_support).
(in-ns 'go.arbace.jrt)

(go/file "standin_executor.go")

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

(go/var ExecutorService_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.ExecutorService" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract)
                     :Interfaces (lit (slice (* Class)) Executor_class) :Go "arbace/jrt.ExecutorService"))))
(go/func ExecutorService_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ExecutorService x)] (dynNominal x ExecutorService_class ok)))
(go/func ExecutorService_Cast ^ExecutorService [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ExecutorService x)]
    (when (not (dynNominal x ExecutorService_class ok)) (panic (ClassCast x ExecutorService_class)))
    v))

(go/func init []
  (set! (.-IsInstance (.Info ExecutorService_class)) ExecutorService_InstanceOf))
