;; Go-build variant of java.util.concurrent.locks.AbstractOwnableSynchronizer (C2G-SPEC §4.6,
;; §8.3; doc/go/JRT-NOTES.md, "Concurrency", JC6): read by c2g only. The exclusive owner, a
;; Thread (an interface in Go: two words), is written by the owner and read by other threads
;; (ThreadPoolExecutor's workers, toString); volatile here, so that no read tears.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent.locks)

(c2g/variant AbstractOwnableSynchronizer
  (field ^:private ^:transient ^:volatile ^Thread exclusiveOwnerThread))
