;; Go-build variant of java.util.concurrent.CompletableFuture (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Concurrency", JC7): read by c2g only. jdk26u schedules orTimeout's, completeOnTimeout's and
;; delayedExecutor's delays in its pool's DelayScheduler (ForkJoinPool.scheduleDelayedTask),
;; which jrt's ForkJoinPool has not; the variant schedules them with jrt's own
;; jdk.internal.jrt.Delays (a daemon thread per delay that a cancellation wakes), with the same
;; cancellation of a timeout once the future completes.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant CompletableFuture
  (method ^:private arrangeTimeout :type-params [U] ^void [this ^long nanoDelay
                                                           ^{:tag (Timeout U)} onTimeout]
    (when (nil? result)
      (let [^{:tag (Future ?)} t (jdk.internal.jrt.Delays/schedule onTimeout nanoDelay)]
        (.whenComplete this (Canceller. t))))))

(c2g/variant CompletableFuture$DelayedExecutor
  (method ^:public execute ^void [this ^Runnable r]
    (jdk.internal.jrt.Delays/schedule (TaskSubmitter. executor r) nanoDelay)))
