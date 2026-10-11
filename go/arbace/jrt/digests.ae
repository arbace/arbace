;; jrt: the translated digests' block functions over Go's crypto (doc/go/JRT-NOTES.md,
;; "Security", amendment SC7), as the JVM's intrinsics replace DigestBase's
;; implCompressMultiBlock0 and the digests' implCompress0: the variants of
;; sun.security.provider's MD5, SHA, SHA2, SHA5 and SHA3 hand whole blocks and their chaining
;; state (the Java object's own int[] or long[]) to these natives, which run Go's block function
;; (assembly where Go has it) by way of the digest's binary state: the state words written into
;; a fresh Go digest's marshaled state (MarshalBinary's layout: a 4-byte magic, SHA-3's rate
;; byte, the state, an empty buffer, a zero length), unmarshaled, the blocks written, the new
;; state words read back. The Java object keeps the state, the buffering, the padding and the
;; final block, so clone, reset, partial digests and every message stay the JDK's.
(in-ns 'go.arbace.jrt)

(go/file "digests.go"
  :imports [[encoding "encoding"] [binary "encoding/binary"] [io "io"] [md5 "crypto/md5"]
            [sha1 "crypto/sha1"] [sha256 "crypto/sha256"] [sha512 "crypto/sha512"]
            [sha3 "crypto/sha3"]])

(go/type goDigest "goDigest is a Go digest whose state can be set and read.\n"
  (interface encoding/BinaryMarshaler encoding/BinaryUnmarshaler io/Writer))

(go/func goBlocks
  "goBlocks runs h's block function on data (whole blocks) from the chaining state words, the
state's bytes at offset off of h's marshaled state; it returns the new state's bytes.\n"
  ^{:tag (slice byte)} [^goDigest h ^int off ^{:tag (slice byte)} words ^{:tag (slice byte)} data]
  (let [(values m err) (.MarshalBinary h)]
    (when (!= err nil)
      (panic err))
    (copy (subslice m off) words)
    (range [i _ (subslice m (+ off (len words)))]
      (aset m (+ (+ off (len words)) i) 0))
    (let [err2 (.UnmarshalBinary h m)]
      (when (!= err2 nil)
        (panic err2)))
    (.Write h data)
    (let [(values m2 err3) (.MarshalBinary h)]
      (when (!= err3 nil)
        (panic err3))
      (subslice m2 off (+ off (len words))))))

(go/func intBlocks "intBlocks: goBlocks over an int[] chaining state, big-endian words.\n"
  [^goDigest h ^{:tag (* IntArray)} state ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (let [s (.-A state)
        words (make (slice byte) (* 4 (len s)))]
    (range [i x s]
      (.PutUint32 binary/BigEndian (subslice words (* 4 i)) (conv uint32 x)))
    (let [out (goBlocks h 4 words (byteView b ofs n))]
      (range [i _ s]
        (aset s i (conv int32 (.Uint32 binary/BigEndian (subslice out (* 4 i)))))))))

(go/func MD5_GoBlocks_I1_B1_I_I__V_native "MD5's blocks by crypto/md5.\n"
  [^{:tag (* IntArray)} state ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (intBlocks (assert goDigest (md5/New)) state b ofs n))

(go/func SHA_GoBlocks_I1_B1_I_I__V_native "SHA-1's blocks by crypto/sha1.\n"
  [^{:tag (* IntArray)} state ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (intBlocks (assert goDigest (sha1/New)) state b ofs n))

(go/func SHA2_GoBlocks_I1_B1_I_I__V_native
  "SHA-224's and SHA-256's blocks by crypto/sha256 (one block function; the state is the
algorithm's).\n"
  [^{:tag (* IntArray)} state ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (intBlocks (assert goDigest (sha256/New)) state b ofs n))

(go/func SHA5_GoBlocks_J1_B1_I_I__V_native
  "SHA-384's, SHA-512's, SHA-512/224's and SHA-512/256's blocks by crypto/sha512.\n"
  [^{:tag (* LongArray)} state ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (let [s (.-A state)
        words (make (slice byte) (* 8 (len s)))]
    (range [i x s]
      (.PutUint64 binary/BigEndian (subslice words (* 8 i)) (conv uint64 x)))
    (let [out (goBlocks (assert goDigest (sha512/New)) 4 words (byteView b ofs n))]
      (range [i _ s]
        (aset s i (conv int64 (.Uint64 binary/BigEndian (subslice out (* 8 i)))))))))

(go/func SHA3_GoBlocks_J1_I_B1_I_I__Z_native
  "SHA-3's and SHAKE's blocks by crypto/sha3, for a rate Go has a digest of (false otherwise):
the lanes little-endian, the Keccak state after the magic and the rate byte.\n"
  ^bool [^{:tag (* LongArray)} state ^int32 rate ^{:tag (* ByteArray)} b ^int32 ofs ^int32 n]
  (let [^goDigest h nil]
    (switch rate
      (case [144] (set! h (sha3/New224)))
      (case [136] (set! h (sha3/New256)))
      (case [104] (set! h (sha3/New384)))
      (case [72] (set! h (sha3/New512)))
      (case [168] (set! h (sha3/NewSHAKE128)))
      (default (return false)))
    (let [s (.-A state)
          words (make (slice byte) (* 8 (len s)))]
      (range [i x s]
        (.PutUint64 binary/LittleEndian (subslice words (* 8 i)) (conv uint64 x)))
      (let [out (goBlocks h 5 words (byteView b ofs n))]
        (range [i _ s]
          (aset s i (conv int64 (.Uint64 binary/LittleEndian (subslice out (* 8 i))))))))
    true))
