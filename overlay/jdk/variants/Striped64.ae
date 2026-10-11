;; Go-build variant of java.util.concurrent.atomic.Striped64 (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Concurrency"): read by c2g only. Striped64 reaches ThreadLocalRandom's probe through
;; SharedSecrets.getJavaUtilConcurrentTLRAccess(), which jrt's SharedSecrets has not; the variant
;; asks jrt's own java.util.concurrent.ThreadLocalRandomProbes, which calls the same
;; package-private methods of ThreadLocalRandom. Copyright (c) the Arbace authors; Eclipse Public
;; License 1.0.
(in-ns 'java.util.concurrent.atomic)

(c2g/variant Striped64
  (c2g/cut (field ^:private ^:static ^:final ^JavaUtilConcurrentTLRAccess TLR))
  (method ^:static ^:final getProbe ^int []
    (java.util.concurrent.ThreadLocalRandomProbes/getProbe))
  (method ^:static ^:final advanceProbe ^int [^int probe]
    (java.util.concurrent.ThreadLocalRandomProbes/advanceProbe probe)))
