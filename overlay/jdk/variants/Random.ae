;; Go-build variant of java.util.Random (C2G-SPEC §4.6): read by c2g only. Random's static
;; initializer takes the offset of its private field seed through reflection
;; (getDeclaredField, then Unsafe.objectFieldOffset(Field)); jrt's member tables list public
;; members only, so the variant asks Unsafe.objectFieldOffset(Class, String), which jrt has, for
;; the same field (resetSeed, deserialization's). Copyright (c) the Arbace authors; Eclipse
;; Public License 1.0.
(in-ns 'java.util)

(c2g/variant Random
  (static-initializer
    (set! seedOffset (.objectFieldOffset unsafe Random "seed"))))
