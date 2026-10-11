;; Go-build variant of sun.security.provider.SHA3 (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Security",
;; amendment SC7): read by c2g only. Whole blocks are absorbed by Go's crypto/sha3 into this
;; digest's Keccak state (DigestBase's variant, jrt's digests.clj), as the JVM's intrinsic does,
;; for the rates Go has a digest of (SHA3-224/256/384/512, SHAKE128, SHAKE256); otherwise
;; jdk26u's loop. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant SHA3
  (c2g/add
    (method compressBlocks ^boolean [this ^byte/1 b ^int ofs ^int len]
      (SHA3/goBlocks state (.-blockSize this) b ofs len)))

  (c2g/add
    (method ^:private ^:static ^:native goBlocks ^boolean [^long/1 state ^int rate ^byte/1 b ^int ofs
                                                           ^int len])))
