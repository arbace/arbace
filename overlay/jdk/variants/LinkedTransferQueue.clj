;; Go-build variant of java.util.concurrent.LinkedTransferQueue (C2G-SPEC §4.6, §8.3;
;; doc/go/JRT-NOTES.md, "Concurrency"): read by c2g only. A node's waiter, the Thread a matching
;; thread unparks, is a plain field whose accesses the JDK orders by fences ("access order
;; constrained by context"); in Go a Thread is an interface, two words a racing read can tear
;; (Clojure's suite saw a NullPointerException in LockSupport.unpark), so it is volatile here.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant LinkedTransferQueue$DualNode
  (field ^:volatile ^Thread waiter))
