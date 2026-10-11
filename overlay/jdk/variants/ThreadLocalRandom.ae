;; Go-build variant of java.util.concurrent.ThreadLocalRandom (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Concurrency", JC4): read by c2g only. ThreadLocalRandom keeps its seed and probe in the
;; Thread's fields through Unsafe, which jrt's Thread has (F_threadLocalRandomSeed ...); its
;; thread-local maps (threadLocals, inheritableThreadLocals) are jrt's Go maps, which no Unsafe
;; offset names, so the two offsets go and eraseThreadLocals (the JDK's innocuous fork-join
;; workers') erases nothing. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant ThreadLocalRandom
  (c2g/cut (field ^:private ^:static ^:final ^long THREADLOCALS))
  (c2g/cut (field ^:private ^:static ^:final ^long INHERITABLETHREADLOCALS))
  (method ^:static ^:final eraseThreadLocals ^void [^Thread thread]
    nil))
