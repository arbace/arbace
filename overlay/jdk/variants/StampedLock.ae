;; Go-build variant of java.util.concurrent.locks.StampedLock (C2G-SPEC §4.6, §8.3;
;; doc/go/JRT-NOTES.md, "Concurrency"): read by c2g only. As AbstractQueuedSynchronizer's: a
;; queued node's waiter, a Thread (two words in Go) the releasing thread reads, is volatile here.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent.locks)

(c2g/variant StampedLock$Node
  (field ^:volatile ^Thread waiter))
