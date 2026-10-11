;; Go-build variant of java.util.concurrent.ForkJoinWorkerThread (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Concurrency"): read by c2g only. Its static initializer takes Unsafe offsets of the Thread's
;; thread-local maps, which are jrt's Go maps (no field Unsafe names): without the variant the
;; class fails to initialize, and with it the static hasKnownQueuedWork that
;; LinkedTransferQueue's and SynchronousQueue's waits call. jrt's fork-join workers are not
;; ForkJoinWorkerThreads, so hasKnownQueuedWork answers false as the JDK does for another
;; thread, and resetThreadLocals resets nothing. Copyright (c) the Arbace authors; Eclipse
;; Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant ForkJoinWorkerThread
  (c2g/cut (field ^:private ^:static ^:final ^long THREADLOCALS))
  (c2g/cut (field ^:private ^:static ^:final ^long INHERITABLETHREADLOCALS))
  (method ^:final resetThreadLocals ^void [this]
    (.onThreadLocalReset this)))
