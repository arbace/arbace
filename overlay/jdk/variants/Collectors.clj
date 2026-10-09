;; Go-build variant of java.util.stream.Collectors (C2G-SPEC §4.6): read by c2g only.
;; toUnmodifiableList's finisher wraps the list's array through SharedSecrets'
;; JavaUtilCollectionAccess (jdk.internal.access is not in the world); the variant's finisher
;; is List.copyOf (an unmodifiable list, null elements rejected, as the JDK's).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.stream)

(import '(java.util ArrayList List)
        '(java.util.function BiConsumer BinaryOperator Function Supplier))

(c2g/variant Collectors
  (method ^:public ^:static toUnmodifiableList :type-params [T] ^{:tag (Collector T ? (List T))} []
    (CollectorImpl. (method-ref Supplier ^ArrayList [] ArrayList/new)
                    (method-ref BiConsumer [ArrayList Object] List/.add)
                               (lambda BinaryOperator ^ArrayList [^ArrayList left ^ArrayList right]
                                 (.addAll left right)
                                 left)
                               (lambda Function ^List [^ArrayList list]
                                 (if (identical? (.getClass list) ArrayList)
                                     (List/copyOf list)
                                     (throw (IllegalArgumentException.))))
                               CH_NOID)))
