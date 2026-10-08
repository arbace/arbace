;; g2c-test: lines   SPEC §14.2: unicode/utf8 (excerpts), forms on Go's lines
(ns go.unicode.utf8 (:require [arbace.go :as go]))
(go/package utf8 :path "unicode/utf8" :files ["s14_2_utf8.go"] :positions :lines)
(go/file "s14_2_utf8.go" :doc "Package utf8 implements functions and constants to support text encoded in\nUTF-8. It includes functions to translate between runes and UTF-8 byte sequences.\nSee https://en.wikipedia.org/wiki/UTF-8\n" :imports [])










(go/const "Numbers fundamental to the encoding.\n"
  ^{:line 16} [^{:val \uFFFD} RuneError \uFFFD]      ; the "error" Rune or "Unicode replacement character"
  ^{:line 17} [^{:val 128} RuneSelf 0x80]              ; characters below RuneSelf are represented as themselves
  ^{:line 18} [^{:val (rune 0x10FFFF)} MaxRune (rune 0x10FFFF)]  ; Maximum valid Unicode code point.
  ^{:line 19} [^{:val 4} UTFMax 4])                     ; maximum number of bytes of a UTF-8 encoded Unicode character.



(go/const "Code points in the surrogate range are not valid for UTF-8.\n"
  ^{:line 24} [^{:val 55296} surrogateMin 0xD800]
  ^{:line 25} [^{:val 57343} surrogateMax 0xDFFF])


(go/const
  ^{:line 29} [^{:val 0} t1 2r00000000]
  ^{:line 30} [^{:val 128} tx 2r10000000]
  ^{:line 31} [^{:val 192} t2 2r11000000]
  ^{:line 32} [^{:val 224} t3 2r11100000]
  ^{:line 33} [^{:val 240} t4 2r11110000]
  ^{:line 34} [^{:val 248} t5 2r11111000]

  ^{:line 36} [^{:val 63} maskx 2r00111111]
  ^{:line 37} [^{:val 31} mask2 2r00011111]
  ^{:line 38} [^{:val 15} mask3 2r00001111]
  ^{:line 39} [^{:val 7} mask4 2r00000111]

  [^{:val 127} rune1Max (- (<< 1 7) 1)]
  [^{:val 2047} rune2Max (- (<< 1 11) 1)]
  [^{:val 65535} rune3Max (- (<< 1 16) 1)]

  ;; The default lowest and highest continuation byte.
  ^{:line 46} [^{:val 128 :doc "The default lowest and highest continuation byte.\n"} locb 2r10000000]
  ^{:line 47} [^{:val 191} hicb 2r10111111]

  ;; These names of these constants are chosen to give nice alignment in the
  ;; table below. ...


  ^{:line 53} [^{:val 241 :doc "These names of these constants are chosen to give nice alignment in the\ntable below. The first nibble is an index into acceptRanges or F for\nspecial one-byte cases. The second nibble is the Rune length or the\nStatus for the special one-byte case.\n"} xx 0xF1] ; invalid: size 1
  ^{:line 54} [^{:val 240} as 0xF0]   ; ASCII: size 1
  ^{:line 55} [^{:val 2} s1 0x02]
  ^{:line 56} [^{:val 19} s2 0x13]
  ^{:line 57} [^{:val 3} s3 0x03]
  ^{:line 58} [^{:val 35} s4 0x23]
  ^{:line 59} [^{:val 52} s5 0x34]
  ^{:line 60} [^{:val 4} s6 0x04]
  ^{:line 61} [^{:val 68} s7 0x44])


(go/const
  [^{:val 239} runeErrorByte0 (bit-or t3 (>> RuneError 12))]
  [^{:val 191} runeErrorByte1 (bit-or tx (bit-and (>> RuneError 6) maskx))]
  [^{:val 189} runeErrorByte2 (bit-or tx (bit-and RuneError maskx))])



(go/var ^{:doc "first is information about the first byte in a UTF-8 sequence.\n"} first
  (lit (array 256 uint8)
    ;;   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
    ^{:line 73} as as as as as as as as as as as as as as as as
    ^{:line 74} as as as as as as as as as as as as as as as as
    ^{:line 75} as as as as as as as as as as as as as as as as
    ^{:line 76} as as as as as as as as as as as as as as as as
    ^{:line 77} as as as as as as as as as as as as as as as as
    ^{:line 78} as as as as as as as as as as as as as as as as
    ^{:line 79} as as as as as as as as as as as as as as as as
    ^{:line 80} as as as as as as as as as as as as as as as as
    ;;   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
    ^{:line 82} xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx
    ^{:line 83} xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx
    ^{:line 84} xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx
    ^{:line 85} xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx
    ^{:line 86} xx xx s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1
    ^{:line 87} s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1 s1
    ^{:line 88} s2 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s4 s3 s3
    ^{:line 89} s5 s6 s6 s6 s7 xx xx xx xx xx xx xx xx xx xx xx))



(go/type acceptRange "acceptRange gives the range of valid values for the second byte in a UTF-8\nsequence.\n" (struct
  ^{:tag uint8 :line 95} lo   ; lowest value for second byte.
  ^{:tag uint8 :line 96} hi)) ; highest value for second byte.



(go/var ^{:doc "acceptRanges has size 16 to avoid bounds checks in the code that uses it.\n"} acceptRanges (lit (array 16 acceptRange)
  ^{:line 101} [0 (lit _ locb hicb)]
  ^{:line 102} [1 (lit _ 0xA0 hicb)]
  ^{:line 103} [2 (lit _ locb 0x9F)]
  ^{:line 104} [3 (lit _ 0x90 hicb)]
  ^{:line 105} [4 (lit _ locb 0x8F)]))










^{:go/end 126} (go/func DecodeRuneInString "DecodeRuneInString is like [DecodeRune] but its input is a string. If s is\nempty it returns ([RuneError], 0). Otherwise, if the encoding is invalid, it\nreturns (RuneError, 1). Both are impossible results for correct, non-empty\nUTF-8.\n\nAn encoding is invalid if it is incorrect UTF-8, encodes a rune that is\nout of range, or is not the shortest possible UTF-8 encoding for the\nvalue. No other validation is performed.\n" [^string s] :results [^rune r ^int size]
  ;; Inlineable fast path for ASCII characters; see #48195.
  ;; This implementation is a bit weird but effective at rendering the
  ;; function inlineable.
  (if (and (!= s "") (< (aget s 0) RuneSelf))
    (return (conv rune (aget s 0)) 1)

    (set! (values r size) (decodeRuneInStringSlow s)))

  (return))


^{:go/end 166} (go/func decodeRuneInStringSlow [^string s] :results [rune int]
  (let [n (len s)]
    (when (< n 1)
      (return RuneError 0))

    (let [s0 (aget s 0)
          x (aget first s0)]
      (when (>= x as)
        ;; The following code simulates an additional check for x == xx and
        ;; handling the ASCII and invalid cases accordingly. This mask-and-or
        ;; approach prevents an additional branch.
        (let [mask (>> (<< (conv rune x) 31) 31)]     ; Create 0x0000 or 0xFFFF.
          (return (bit-or (bit-and-not (conv rune (aget s 0)) mask) (bit-and RuneError mask)) 1)))

      (let [sz (conv int (bit-and x 7))
            accept (aget acceptRanges (>> x 4))]
        (when (< n sz)
          (return RuneError 1))

        (let [s1 (aget s 1)]
          (when (or (< s1 (.-lo accept)) (< (.-hi accept) s1))
            (return RuneError 1))

          (when (<= sz 2)     ; <= instead of == to help the compiler eliminate some bounds checks
            (return (bit-or (<< (conv rune (bit-and s0 mask2)) 6) (conv rune (bit-and s1 maskx))) 2))

          (let [s2 (aget s 2)]
            (when (or (< s2 locb) (< hicb s2))
              (return RuneError 1))

            (when (<= sz 3)
              (return (bit-or (<< (conv rune (bit-and s0 mask3)) 12) (<< (conv rune (bit-and s1 maskx)) 6) (conv rune (bit-and s2 maskx))) 3))

            (let [s3 (aget s 3)]
              (when (or (< s3 locb) (< hicb s3))
                (return RuneError 1))

              (return (bit-or (<< (conv rune (bit-and s0 mask4)) 18) (<< (conv rune (bit-and s1 maskx)) 12) (<< (conv rune (bit-and s2 maskx)) 6) (conv rune (bit-and s3 maskx))) 4))))))))




^{:go/end 186} (go/func RuneLen "RuneLen returns the number of bytes in the UTF-8 encoding of the rune.\nIt returns -1 if the rune is not a valid value to encode in UTF-8.\n" ^int [^rune r]
  (switch
    (case [(< r 0)]
      (return -1))
    (case [(<= r rune1Max)]
      (return 1))
    (case [(<= r rune2Max)]
      (return 2))
    (case [(and (<= surrogateMin r) (<= r surrogateMax))]
      (return -1))
    (case [(<= r rune3Max)]
      (return 3))
    (case [(<= r MaxRune)]
      (return 4)))

  -1)


^{:go/end 216} (go/func encodeRuneNonASCII ^int [^{:tag (slice byte)} p ^rune r]
  ;; Negative values are erroneous. Making it unsigned addresses the problem.
  (switch [i (conv uint32 r)]
    (case [(<= i rune2Max)]
      (set! _ (aget p 1))                                   ; eliminate bounds checks
      (aset p 0 (bit-or t2 (conv byte (>> r 6))))
      (aset p 1 (bit-or tx (bit-and (conv byte r) maskx)))
      (return 2))
    (case [(< i surrogateMin) (and (< surrogateMax i) (<= i rune3Max))]
      (set! _ (aget p 2))
      (aset p 0 (bit-or t3 (conv byte (>> r 12))))
      (aset p 1 (bit-or tx (bit-and (conv byte (>> r 6)) maskx)))
      (aset p 2 (bit-or tx (bit-and (conv byte r) maskx)))
      (return 3))
    (case [(and (> i rune3Max) (<= i MaxRune))]
      (set! _ (aget p 3))
      (aset p 0 (bit-or t4 (conv byte (>> r 18))))
      (aset p 1 (bit-or tx (bit-and (conv byte (>> r 12)) maskx)))
      (aset p 2 (bit-or tx (bit-and (conv byte (>> r 6)) maskx)))
      (aset p 3 (bit-or tx (bit-and (conv byte r) maskx)))
      (return 4))
    (default
      (set! _ (aget p 2))
      (aset p 0 runeErrorByte0)
      (aset p 1 runeErrorByte1)
      (aset p 2 runeErrorByte2)
      (return 3))))






^{:go/end 221} (go/func RuneStart "RuneStart reports whether the byte could be the first byte of an encoded,\npossibly invalid rune. Second and subsequent bytes always have the top two\nbits set to 10.\n" ^bool [^byte b] (!= (bit-and b 0xC0) 0x80))

(go/const ^{:val 8} ptrSize (<< 4 (>> (bit-not (conv uintptr 0)) 63)))
(go/const ^{:val -9187201950435737472} hiBits (>> 0x8080808080808080 (- 64 (* 8 ptrSize))))

^{:go/end 231} (go/func word :type-params [^{:tag (| string (slice byte))} T] ^uintptr [^T s]
  (when (== ptrSize 4)
    (return (bit-or (conv uintptr (aget s 0)) (<< (conv uintptr (aget s 1)) 8) (<< (conv uintptr (aget s 2)) 16) (<< (conv uintptr (aget s 3)) 24))))

  (conv uintptr (bit-or (conv uint64 (aget s 0)) (<< (conv uint64 (aget s 1)) 8) (<< (conv uint64 (aget s 2)) 16) (<< (conv uint64 (aget s 3)) 24) (<< (conv uint64 (aget s 4)) 32) (<< (conv uint64 (aget s 5)) 40) (<< (conv uint64 (aget s 6)) 48) (<< (conv uint64 (aget s 7)) 56))))
