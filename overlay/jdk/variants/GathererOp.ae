;; Go-build variant of java.util.stream.GathererOp (C2G-SPEC §4.6): read by c2g only. evaluate,
;; the gatherer's execution, defines local classes nested in local classes over captured
;; variables (Sequential, Hybrid, Parallel), which c2g does not translate yet; the variant
;; throws instead: Stream.gather is not in the Go build. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'java.util.stream)

(import '(java.util Spliterator)
        '(java.util.function BiConsumer BinaryOperator Function Supplier))

(c2g/variant GathererOp
  (method ^:private evaluate :type-params [CA CR] ^CR [this
                                                       ^:final ^{:tag (Spliterator T)} spliterator
                                                       ^:final ^boolean parallel
                                                       ^:final ^{:tag (Gatherer T A R)} gatherer
                                                       ^:final ^{:tag (Supplier CA)} collectorSupplier
                                                       ^:final ^{:tag (BiConsumer CA (? super R))} collectorAccumulator
                                                       ^:final ^{:tag (BinaryOperator CA)} collectorCombiner
                                                       ^:final ^{:tag (Function CA CR)} collectorFinisher]
    (throw (UnsupportedOperationException. "Stream.gather is not in the Go build"))))
