;; Go-build variant of sun.net.util.IPAddressUtil (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"):
;; read by c2g only. parseBsdLiteralV4 (the BSD forms of IPv4 literals: octal, hexadecimal, fewer
;; fields; InetAddress's literal checks reach it) walks the text through a java.nio.CharBuffer,
;; outside the closed world; the variant walks the String with its position in an int[1], the
;; same steps as jdk26u's parseV4FieldBsd, checkPrefix, isOctalFieldStart, isDecimalFieldStart and
;; isHexFieldStart, whose code it keeps (baf63fb; Copyright (c) Oracle and/or its affiliates,
;; GPL 2 with the Classpath Exception: LICENSE.md); the rest Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'sun.net.util)

(c2g/variant IPAddressUtil
  (method ^:public ^:static parseBsdLiteralV4 ^byte/1 [^String input]
    (let [res (new byte/1 [0 0 0 0])
          len (.length input)]
      (when-not (== len 0)
        (let [firstSymbol (.charAt input 0)]
          (when-not (== (IPAddressUtil/parseAsciiDigit firstSymbol DECIMAL) -1)
            (let [lastSymbol (.charAt input (unchecked-subtract-int len 1))]
              (when-not (or (== lastSymbol \.)
                            (== (IPAddressUtil/parseAsciiHexDigit lastSymbol) -1))
                (let [pos (new int/1 1)
                      ^:mutable ^int fieldNumber 0
                      ^:mutable fieldValue -1]
                  (while (< (aget pos 0) len)
                    (set! fieldValue -1)
                    (for-each [^int radix SUPPORTED_RADIXES]
                      (set! fieldValue (IPAddressUtil/parseV4FieldBsdAt radix input pos fieldNumber))
                      (cond
                        (>= fieldValue 0)
                          (do
                            (when (< fieldValue 256)
                              (aset res fieldNumber (unchecked-byte fieldValue)))
                            (set! fieldNumber (unchecked-inc-int fieldNumber))
                            (break))
                        (== fieldValue TERMINAL_PARSE_ERROR) (return nil)))
                    (when (< fieldValue 0) (return nil)))
                  (when-not (< fieldValue 0)
                    (when (< fieldNumber 4)
                      (loop [^int i 3]
                        (when (>= i (unchecked-subtract-int fieldNumber 1))
                          (aset res i (unchecked-byte (bit-and fieldValue 255)))
                          (set! fieldValue (bit-shift-right fieldValue 8))
                          (recur (unchecked-dec-int i)))))
                    res)))))))))

  (c2g/add
    (method ^:private ^:static parseV4FieldBsdAt ^long [^int radix ^String s ^int/1 pos ^int fieldNumber]
      (let [initialPos (aget pos 0)
            ^:mutable val 0
            ^:mutable ^int digitsCount 0]
        (when-not (IPAddressUtil/checkPrefixAt s pos radix) (set! val CANT_PARSE_IN_RADIX))
        (let [^:mutable dotSeen false]
          (while (and (and (< (aget pos 0) (.length s)) (not (== val CANT_PARSE_IN_RADIX))) (not dotSeen))
            (let [c (.charAt s (aget pos 0))]
              (aset pos 0 (unchecked-inc-int (aget pos 0)))
              (if (== c \.)
                  (do
                    (set! dotSeen true)
                    (when (== fieldNumber 3) (return TERMINAL_PARSE_ERROR))
                    (when (== digitsCount 0) (return TERMINAL_PARSE_ERROR))
                    (when (> val 255) (return TERMINAL_PARSE_ERROR)))
                  (let [dv (IPAddressUtil/parseAsciiDigit c radix)]
                    (if (>= dv 0)
                        (do
                          (set! digitsCount (unchecked-inc-int digitsCount))
                          (set! val (unchecked-multiply val radix))
                          (set! val (unchecked-add val dv)))
                        (return TERMINAL_PARSE_ERROR))))))
          (if (== val CANT_PARSE_IN_RADIX)
              (aset pos 0 initialPos)
              (when-not dotSeen
                (let [maxValue (unchecked-subtract
                                 (bit-shift-left 1
                                                 (unchecked-multiply-int
                                                   (unchecked-subtract-int 4 fieldNumber)
                                                   8))
                                 1)]
                  (when (> val maxValue) (return TERMINAL_PARSE_ERROR)))))
          val))))

  (c2g/add
    (method ^:private ^:static checkPrefixAt ^boolean [^String s ^int/1 pos ^int radix]
      (let [p (aget pos 0)
            remaining (unchecked-subtract-int (.length s) p)]
        (cond
          ;; octal: '0' then not '.', the '0' consumed; .0<EOS> is not octal
          (== radix OCTAL)
            (if (< remaining 2)
                false
                (let [first (.charAt s p)
                      second (.charAt s (unchecked-inc-int p))
                      isOctalPrefix (and (== first \0) (not (== second \.)))]
                  (aset pos 0 (if isOctalPrefix (unchecked-inc-int p) (unchecked-add-int p 2)))
                  isOctalPrefix))
          (== radix DECIMAL) (> remaining 0)
          ;; hexadecimal: '0x' or '0X', both consumed
          (== radix HEXADECIMAL)
            (if (< remaining 2)
                false
                (let [first (.charAt s p)
                      second (.charAt s (unchecked-inc-int p))]
                  (aset pos 0 (unchecked-add-int p 2))
                  (and (== first \0) (or (== second \x) (== second \X)))))
          :else (throw (AssertionError. "Not supported radix")))))))
