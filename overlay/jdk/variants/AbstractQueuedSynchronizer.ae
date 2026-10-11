;; Go-build variant of java.util.concurrent.locks.AbstractQueuedSynchronizer (C2G-SPEC §4.6, §8.3;
;; doc/go/JRT-NOTES.md, "Concurrency", JC6): read by c2g only. A queued node's waiter, the
;; thread to unpark, is a plain field the JDK writes in the waiting thread and reads in the
;; releasing one (a benign race in Java); its Go type is an interface (Thread is not a leaf
;; class), two words a racing read could tear, so it is volatile here (§8.3's cure).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent.locks)

(c2g/variant AbstractQueuedSynchronizer$Node
  (field ^:volatile ^Thread waiter))
