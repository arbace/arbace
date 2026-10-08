;; jrt: the charsets String needs (UTF-8, ISO-8859-1, US-ASCII), with Java's replacement
;; rules; phase 2's Charset shim (sun.nio.cs.UTF_8, ISO_8859_1) uses them too.
(in-ns 'go.arbace.jrt)

(go/file "codec.go")

(go/func DecodeUTF8
  "DecodeUTF8 is Java's UTF-8 decoding with replacement (String.decodeUTF8_UTF16): malformed
input becomes U+FFFD as Java's decoder replaces it (one for each maximal ill-formed
subsequence, as JDK 26 consumes them), surrogates encoded in 3 bytes too.\n"
  ^{:tag (slice uint16)} [^{:tag (slice int8)} src]
  (let [sp 0
        sl (len src)
        dst (make (slice uint16) 0 sl)
        ^uint16 repl 0xFFFD]
    (while (< sp sl)
      (let [b1 (conv int32 (aget src sp))]
        (inc! sp)
        (cond
          (>= b1 0)
          (set! dst (append dst (conv uint16 b1)))

          (and (== (>> b1 5) -2) (!= (bit-and b1 0x1e) 0))
          (do
            (when (< sp sl)
              (let [b2 (conv int32 (aget src sp))]
                (inc! sp)
                (if (notContinuation b2)
                  (do (set! dst (append dst repl)) (dec! sp))
                  (set! dst (append dst (conv uint16 (bit-xor (bit-xor (<< b1 6) b2) (bit-xor (<< -64 6) -128))))))
                (continue)))
            (set! dst (append dst repl))
            (break))

          (== (>> b1 4) -2)
          (do
            (when (< (+ sp 1) sl)
              (let [b2 (conv int32 (aget src sp))
                    b3 (conv int32 (aget src (+ sp 1)))]
                (set! sp (+ sp 2))
                (if (or (and (== b1 -32) (== (bit-and b2 0xe0) 0x80)) (notContinuation b2) (notContinuation b3))
                  (do
                    (set! dst (append dst repl))
                    (set! sp (- sp 3))
                    ;; malformed3
                    (if (or (and (== b1 -32) (== (bit-and b2 0xe0) 0x80)) (notContinuation b2))
                      (set! sp (+ sp 1))
                      (set! sp (+ sp 2))))
                  (let [c (conv uint16 (bit-xor (<< b1 12) (<< b2 6) (bit-xor b3 (bit-xor (<< -32 12) (<< -128 6) -128))))]
                    (if (isSurrogate c)
                      (set! dst (append dst repl))
                      (set! dst (append dst c)))))
                (continue)))
            (when (and (< sp sl) (or (and (== b1 -32) (== (bit-and (conv int32 (aget src sp)) 0xe0) 0x80))
                                     (notContinuation (conv int32 (aget src sp)))))
              (set! dst (append dst repl))
              (continue))
            (set! dst (append dst repl))
            (break))

          (== (>> b1 3) -2)
          (do
            (when (< (+ sp 2) sl)
              (let [b2 (conv int32 (aget src sp))
                    b3 (conv int32 (aget src (+ sp 1)))
                    b4 (conv int32 (aget src (+ sp 2)))
                    uc (bit-xor (<< b1 18) (<< b2 12) (<< b3 6) (bit-xor b4 (bit-xor (<< -16 18) (<< -128 12) (<< -128 6) -128)))]
                (set! sp (+ sp 3))
                (if (or (notContinuation b2) (notContinuation b3) (notContinuation b4)
                        (< uc 0x10000) (> uc 0x10FFFF))
                  (do
                    (set! dst (append dst repl))
                    (set! sp (- sp 4))
                    (set! sp (+ sp (malformed4 src sp))))
                  (set! dst (appendCodePoint dst uc)))
                (continue)))
            (set! b1 (bit-and b1 0xff))
            (when (or (> b1 0xf4) (and (< sp sl) (malformed4_2 b1 (bit-and (conv int32 (aget src sp)) 0xff))))
              (set! dst (append dst repl))
              (continue))
            (inc! sp)
            (set! dst (append dst repl))
            (when (and (< sp sl) (notContinuation (conv int32 (aget src sp))))
              (continue))
            (break))

          :else
          (set! dst (append dst repl)))))
    dst))

(go/func notContinuation ^bool [^int32 b] (!= (bit-and b 0xc0) 0x80))

(go/func malformed4_2 ^bool [^int32 b1 ^int32 b2]
  (or (and (== b1 0xf0) (or (< b2 0x90) (> b2 0xbf)))
      (and (== b1 0xf4) (!= (bit-and b2 0xf0) 0x80))
      (notContinuation b2)))

(go/func malformed4 ^int [^{:tag (slice int8)} src ^int sp]
  (let [b1 (bit-and (conv int32 (aget src sp)) 0xff)
        b2 (bit-and (conv int32 (aget src (+ sp 1))) 0xff)]
    (when (or (> b1 0xf4) (malformed4_2 b1 b2))
      (return 1))
    (when (notContinuation (conv int32 (aget src (+ sp 2))))
      (return 2))
    3))

(go/func EncodeUTF8
  "EncodeUTF8 is Java's UTF-8 encoding with replacement: an unpaired surrogate becomes '?'.\n"
  ^{:tag (slice int8)} [^{:tag (slice uint16)} v]
  (let [dst (make (slice int8) 0 (len v))]
    (for [i 0] (< i (len v)) (inc! i)
      (let [c (conv rune (aget v i))]
        (cond
          (< c 0x80) (set! dst (append dst (conv int8 c)))
          (< c 0x800) (set! dst (append dst (conv int8 (bit-or 0xc0 (>> c 6))) (conv int8 (bit-or 0x80 (bit-and c 0x3f)))))
          (isSurrogate (conv uint16 c))
          (if (and (isHighSurrogate (conv uint16 c)) (< (+ i 1) (len v)) (isLowSurrogate (aget v (+ i 1))))
            (let [uc (toCodePoint (conv uint16 c) (aget v (+ i 1)))]
              (inc! i)
              (set! dst (append dst (conv int8 (bit-or 0xf0 (>> uc 18)))
                                (conv int8 (bit-or 0x80 (bit-and (>> uc 12) 0x3f)))
                                (conv int8 (bit-or 0x80 (bit-and (>> uc 6) 0x3f)))
                                (conv int8 (bit-or 0x80 (bit-and uc 0x3f))))))
            (set! dst (append dst \?)))
          :else (set! dst (append dst (conv int8 (bit-or 0xe0 (>> c 12)))
                                  (conv int8 (bit-or 0x80 (bit-and (>> c 6) 0x3f)))
                                  (conv int8 (bit-or 0x80 (bit-and c 0x3f))))))))
    dst))

(go/const
  [^{:tag int :val 0} csUnknown iota]
  [^{:val 1} csUTF8]
  [^{:val 2} csLatin1]
  [^{:val 3} csASCII])

(go/func charsetOf "charsetOf: jrt's charsets by Java's names and aliases (case-insensitive; charset.clj).\n"
  ^int [^{:tag (* String)} name]
  (let [cs (lookupCharset (.String name))]
    (when (== cs nil)
      (return csUnknown))
    (.-kind (.Self_Charset cs))))

(go/func DecodeBytes
  "DecodeBytes decodes with the named charset (UTF-8, ISO-8859-1, US-ASCII: bytes above 0x7F
become U+FFFD); UnsupportedEncodingException for others.\n"
  ^{:tag (slice uint16)} [^{:tag (slice int8)} b ^{:tag (* String)} name]
  (switch (charsetOf name)
    (case [csUTF8] (return (DecodeUTF8 b)))
    (case [csLatin1]
      (let [v (make (slice uint16) (len b))]
        (range [i c b] (aset v i (conv uint16 (conv uint8 c))))
        (return v)))
    (case [csASCII]
      (let [v (make (slice uint16) (len b))]
        (range [i c b]
          (if (< c 0)
            (aset v i 0xFFFD)
            (aset v i (conv uint16 c))))
        (return v))))
  (panic (UnsupportedEncodingException_New_String name)))

(go/func EncodeBytes
  "EncodeBytes encodes with the named charset; unmappable characters become '?' (a surrogate
pair one '?').\n"
  ^{:tag (slice int8)} [^{:tag (slice uint16)} v ^{:tag (* String)} name]
  (let [cs (charsetOf name)
        limit (conv rune 0xff)]
    (switch cs
      (case [csUTF8] (return (EncodeUTF8 v)))
      (case [csASCII] (set! limit 0x7f))
      (case [csLatin1])
      (default (panic (UnsupportedEncodingException_New_String name))))
    (let [dst (make (slice int8) 0 (len v))]
      (for [i 0] (< i (len v)) (inc! i)
        (let [c (conv rune (aget v i))]
          (cond
            (<= c limit) (set! dst (append dst (conv int8 c)))
            (and (isHighSurrogate (conv uint16 c)) (< (+ i 1) (len v)) (isLowSurrogate (aget v (+ i 1))))
            (do (set! dst (append dst \?)) (inc! i))
            :else (set! dst (append dst \?)))))
      dst)))
