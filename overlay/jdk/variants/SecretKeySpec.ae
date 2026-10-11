;; Go-build variant of javax.crypto.spec.SecretKeySpec (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The static initializer, which gives SharedSecrets the access
;; to SecretKeySpec.clear (for the JDK's PBKDF2 and HKDF implementations, not in the Go build),
;; is cut. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'javax.crypto.spec)

(c2g/variant SecretKeySpec
  (c2g/cut (static-initializer 0)))
