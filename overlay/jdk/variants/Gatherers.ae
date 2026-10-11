;; Go-build variant of java.util.stream.Gatherers (C2G-SPEC §4.6): read by c2g only.
;; mapConcurrent runs each element in a virtual thread through a local subclass of FutureTask,
;; which jrt hand-writes as a leaf class (no class interface to extend); the variant leaves it
;; out: mapConcurrent throws UnsupportedOperationException in the Go build. Copyright (c) the
;; Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.stream)

(c2g/variant Gatherers
  (method ^:public ^:static mapConcurrent :type-params [T R] ^{:tag (Gatherer T ? R)} [^:final ^int maxConcurrency
                                                                                       ^:final ^{:tag (java.util.function.Function (? super T) (? extends R))} mapper]
    (throw (UnsupportedOperationException. "Gatherers.mapConcurrent is not in the Go build"))))
