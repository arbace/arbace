;; Go-build variant of java.util.stream.ReferencePipeline (C2G-SPEC §4.6): read by c2g only.
;; toList wraps the stream's array through SharedSecrets' JavaUtilCollectionAccess, which
;; ImmutableCollections registers in its static initializer (jdk.internal.access is not in the
;; world); the variant returns an unmodifiable list over the array (nulls allowed, as the
;; JDK's). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.stream)

(import '(java.util Arrays Collections List))

(c2g/variant ReferencePipeline
  (method ^:public toList ^{:tag (List P_OUT)} [this]
    (Collections/unmodifiableList (Arrays/asList (.toArray this)))))
