;; SPEC §14.2, unicode/utf8 (std, tamago/amd64): the excerpts, completed where the spec
;; elides ("..."); a doc ending in "..." is compared as a prefix. Two corrections of the
;; spec's text (doc/go/CONVERTER-NOTES.md, "The spec's examples"): runeErrorByte0-2 are
;; untyped runes (t3 | RuneError>>12 mixes an untyped int and an untyped rune), so their
;; :val is a character by §8.2's own table, not 239; the comments after acceptRange's
;; fields are line comments, not doc comments, so they are ; comments, not :doc.
(go/const
  "Numbers fundamental to the encoding.\n"
  [^{:val \uFFFD} RuneError \uFFFD]   ; the "error" Rune or "Unicode replacement character"
  [^{:val 128} RuneSelf 0x80]                ; characters below RuneSelf are represented as ...
  [^{:val (rune 0x10FFFF)} MaxRune (rune 0x10FFFF)]   ; Maximum valid Unicode code point.
  [^{:val 4} UTFMax 4])                      ; maximum number of bytes of a UTF-8 encoded ...

(go/const
  [^{:val \ï} runeErrorByte0 (bit-or t3 (>> RuneError 12))]
  [^{:val \¿} runeErrorByte1 (bit-or tx (bit-and (>> RuneError 6) maskx))]
  [^{:val \½} runeErrorByte2 (bit-or tx (bit-and RuneError maskx))])

(go/type acceptRange
  "acceptRange gives the range of valid values for the second byte in a UTF-8\nsequence.\n"
  (struct ^uint8 lo     ; lowest value for second byte.
          ^uint8 hi))   ; highest value for second byte.

(go/var ^{:doc "acceptRanges has size 16 to avoid bounds checks in the code that uses it.\n"} acceptRanges
  (lit (array 16 acceptRange)
    [0 (lit _ locb hicb)]
    [1 (lit _ 0xA0 hicb)]
    [2 (lit _ locb 0x9F)]
    [3 (lit _ 0x90 hicb)]
    [4 (lit _ locb 0x8F)]))

(go/func RuneLen
  "RuneLen returns the number of bytes in the UTF-8 encoding of the rune. ..."
  ^int [^rune r]
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

(go/func encodeRuneNonASCII ^int [^{:tag (slice byte)} p ^rune r]
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
      (set! _ (aget p 2))                                   ; eliminate bounds checks
      (aset p 0 runeErrorByte0)
      (aset p 1 runeErrorByte1)
      (aset p 2 runeErrorByte2)
      (return 3))))

(go/func DecodeRuneInString
  "DecodeRuneInString is like [DecodeRune] but its input is a string. ..."
  [^string s] :results [^rune r ^int size]
  ;; Inlineable fast path for ASCII characters; see #48195.
  (if (and (!= s "") (< (aget s 0) RuneSelf))
    (return (conv rune (aget s 0)) 1)
    (set! (values r size) (decodeRuneInStringSlow s)))
  (return))
