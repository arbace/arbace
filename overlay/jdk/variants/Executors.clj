;; Go-build variant of java.util.concurrent.Executors (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Concurrency", JC5): read by c2g only.
;; - newThreadPerTaskExecutor: jrt's own jdk.internal.jrt.ThreadPerTaskExecutor (jdk26u's is a
;;   thread container, which jrt has not); newVirtualThreadPerTaskExecutor goes through it.
;; - newWorkStealingPool: jrt's ForkJoinPool of the parallelism (its four-argument constructor,
;;   with a worker factory and an asynchronous mode, is not jrt's).
;; - DefaultThreadFactory: no ThreadGroup (jrt's threads have none); the threads are named,
;;   non-daemon and of normal priority as the JDK's.
;; - AutoShutdownDelegatedExecutorService (newSingleThreadExecutor's): without the Cleaner that
;;   shuts the executor down when the wrapper becomes unreachable (java.lang.ref.Cleaner is not
;;   in the Go build): an unreachable single-thread executor's idle worker stays, as a fixed
;;   pool's does.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant Executors
  (method ^:public ^:static newThreadPerTaskExecutor ^ExecutorService [^ThreadFactory threadFactory]
    (jdk.internal.jrt.ThreadPerTaskExecutor/create threadFactory))

  (method ^:public ^:static newWorkStealingPool ^ExecutorService [^int parallelism]
    (ForkJoinPool. parallelism))

  (method ^:public ^:static newWorkStealingPool ^ExecutorService []
    (ForkJoinPool. (.availableProcessors (Runtime/getRuntime)))))

(c2g/variant Executors$DefaultThreadFactory
  (c2g/cut (field ^:private ^:final ^ThreadGroup group))

  (constructor [this]
    (set! namePrefix (java-str "pool-" (.getAndIncrement poolNumber) "-thread-")))

  (method ^:public newThread ^Thread [this ^Runnable r]
    (let [t (Thread. r (java-str namePrefix (.getAndIncrement threadNumber)))]
      (when (.isDaemon t) (.setDaemon t false))
      (when-not (== (.getPriority t) Thread/NORM_PRIORITY) (.setPriority t Thread/NORM_PRIORITY))
      t)))

(c2g/variant Executors$AutoShutdownDelegatedExecutorService
  (c2g/cut (field ^:private ^:final ^Cleaner$Cleanable cleanable))

  (constructor [this ^ExecutorService executor]
    (super. executor))

  (method ^:public shutdown ^void [this]
    (.shutdown super))

  (method ^:public shutdownNow ^{:tag (List Runnable)} [this]
    (.shutdownNow super)))
