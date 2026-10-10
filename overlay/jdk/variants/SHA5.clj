;; Go-build variant of sun.security.provider.SHA5 (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Security",
;; amendment SC7): read by c2g only. Whole blocks are compressed by Go's crypto/sha512 on this
;; digest's chaining state (DigestBase's variant, jrt's digests.clj), as the JVM's intrinsics do.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant SHA5
  (c2g/add
    (method compressBlocks ^boolean [this ^byte/1 b ^int ofs ^int len]
      (SHA5/goBlocks state b ofs len)
      true))

  (c2g/add
    (method ^:private ^:static ^:native goBlocks ^void [^long/1 state ^byte/1 b ^int ofs ^int len])))
