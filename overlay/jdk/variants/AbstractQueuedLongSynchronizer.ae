;; Go-build variant of java.util.concurrent.locks.AbstractQueuedLongSynchronizer (C2G-SPEC §4.6,
;; §8.3; doc/go/JRT-NOTES.md, "Concurrency", JC6): read by c2g only. As
;; AbstractQueuedSynchronizer's: a node's waiter, a Thread (two words in Go) the releasing thread
;; reads while the waiting one writes it, is volatile here. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent.locks)

(c2g/variant AbstractQueuedLongSynchronizer$Node
  (field ^:volatile ^Thread waiter))
