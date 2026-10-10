;; Go-build variant of sun.security.provider.SHA (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Security",
;; amendment SC7): read by c2g only. Whole blocks are compressed by Go's crypto on this digest's
;; chaining state (DigestBase's variant, jrt's digests.clj), as the JVM's intrinsics do.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant SHA
  (c2g/add
    (method compressBlocks ^boolean [this ^byte/1 b ^int ofs ^int len]
      (SHA/goBlocks state b ofs len)
      true))

  (c2g/add
    (method ^:private ^:static ^:native goBlocks ^void [^int/1 state ^byte/1 b ^int ofs ^int len])))
