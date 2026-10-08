(ns arbace.g2c.lit
  "Go literals and constant values as Clojure literals (doc/go/SPEC.md §8): integers, exact
  floats (doubles, BigDecimals, ratios), runes, strings and byte strings, and the
  representation of constant values (:val, §8.2) from the helper's exact values and go/types'
  types.

  A literal is a Lit: the text written into the forms and the value the reader reads back.
  Composite spellings ((rune n), (imaginary x), (byte-string ...), (complex re im)) are lists."
  (:require [arbace.string :as str])
  (:import [java.math BigDecimal BigInteger]
           [java.nio.charset StandardCharsets]))

(defrecord Lit [text value])

(defn lit [text value] (->Lit text value))

(defn lit? [x] (instance? Lit x))

;;; Clojure text of atoms

(def ^:private char-names
  {\newline "newline" \space "space" \tab "tab" \backspace "backspace" \formfeed "formfeed"
   \return "return"})

(defn printable?
  "Whether the code point prints as itself in Clojure text."
  [cp]
  (let [cp (long cp)
        t (Character/getType (int cp))]
    (not (or (Character/isISOControl (int cp)) (Character/isWhitespace (int cp))
             (Character/isSpaceChar (int cp))
             (<= 0xD800 cp 0xDFFF)
             (<= 0xFFF0 cp 0xFFFF)
             (contains? #{(int Character/UNASSIGNED) (int Character/FORMAT)
                          (int Character/PRIVATE_USE) (int Character/SURROGATE)
                          (int Character/LINE_SEPARATOR) (int Character/PARAGRAPH_SEPARATOR)}
                        t)))))

(defn string-text
  "The Clojure string literal text of s."
  [^String s]
  (let [sb (StringBuilder. "\"")
        n (.length s)]
    (loop [i 0]
      (when (< i n)
        (let [cp (.codePointAt s i)
              w (Character/charCount cp)]
          (case (int cp)
            34 (.append sb "\\\"")
            92 (.append sb "\\\\")
            10 (.append sb "\\n")
            9 (.append sb "\\t")
            13 (.append sb "\\r")
            8 (.append sb "\\b")
            12 (.append sb "\\f")
            (cond
              (or (= cp 32) (printable? cp)) (.appendCodePoint sb cp)
              (< cp 0x10000) (.append sb (format "\\u%04x" cp))
              :else (doseq [c (Character/toChars cp)] (.append sb (format "\\u%04x" (int c))))))
          (recur (+ i w)))))
    (str (.append sb "\""))))

(defn char-text
  "Clojure character literal text for the BMP code point cp (not a surrogate)."
  [cp]
  (let [ch (char cp)]
    (cond
      (char-names ch) (str \\ (char-names ch))
      (printable? cp) (str \\ ch)
      :else (format "\\u%04x" (int cp)))))

;;; Integers

(defn int-lit
  "A Go integer literal's text as a Lit: the radix kept where Clojure has it (0x, 0 octal,
  2r for binary), underscores dropped."
  [^String text]
  (let [t (str/replace text "_" "")
        lower (str/lower-case t)
        [ctext radix digits]
        (cond
          (str/starts-with? lower "0x") [t 16 (subs t 2)]
          (str/starts-with? lower "0o") [(str "0" (subs t 2)) 8 (subs t 2)]
          (str/starts-with? lower "0b") [(str "2r" (subs t 2)) 2 (subs t 2)]
          (and (str/starts-with? t "0") (> (count t) 1)) [t 8 (subs t 1)]
          :else [t 10 t])
        v (bigint (BigInteger. ^String digits (int radix)))]
    (lit ctext (if (< (.bitLength (biginteger v)) 64) (long v) v))))

(defn integer-text
  "Decimal text of an integer value (long or BigInt), as the reader reads it back."
  [v]
  (let [b (biginteger v)]
    (if (< (.bitLength b) 64) (str (long v)) (str b "N"))))

(defn integer-lit [v]
  (let [b (biginteger v)]
    (lit (integer-text v) (if (< (.bitLength b) 64) (long v) (bigint b)))))

;;; Exact rationals and floats

(defn rational
  "An exact rational (Long, BigInt, Ratio) from num/den BigIntegers."
  [^BigInteger num ^BigInteger den]
  (/ (bigint num) (bigint den)))

(defn- pow2 ^BigInteger [n] (.shiftLeft BigInteger/ONE (int n)))

(defn- pow5-exponent
  "k when the odd number d is 5^k, else nil."
  [^BigInteger d]
  (let [guess (long (Math/floor (/ (* (.bitLength d) (Math/log 2)) (Math/log 5))))
        five (BigInteger/valueOf 5)]
    (some (fn [k] (when (and (>= k 0) (= d (.pow five (int k)))) k))
          [guess (dec guess) (inc guess)])))

(defn- num-den [r]
  (cond
    (ratio? r) [(numerator r) (denominator r)]
    :else [(biginteger r) BigInteger/ONE]))

(def ^:private max-decimal-length
  "Above this length a non-integral value with a terminating expansion is written as a
  ratio, which is then shorter."
  80)

(defn- terminating-decimal
  "The exact BigDecimal of num/den when den has only the prime factors 2 and 5, else nil."
  ^BigDecimal [^BigInteger num ^BigInteger den]
  (let [twos (.getLowestSetBit den)]
    (when-let [fives (pow5-exponent (.shiftRight den twos))]
      (let [scale (max twos fives)
            ;; num/den = num * 2^(scale-twos) * 5^(scale-fives) / 10^scale
            unscaled (-> num
                         (.shiftLeft (int (- scale twos)))
                         (.multiply (.pow (BigInteger/valueOf 5) (int (- scale fives)))))]
        (BigDecimal. unscaled (int scale))))))

(defn- double-exact-text
  "Double/toString of the value's nearest double when that text denotes exactly the value
  (a BigDecimal or an exact rational), else nil."
  [v]
  (let [d (double v)]
    (when-not (or (Double/isInfinite d) (Double/isNaN d))
      (let [s (Double/toString d)
            bd (BigDecimal. s)]
        (when (if (instance? BigDecimal v)
                (zero? (.compareTo bd ^BigDecimal v))
                (= (rationalize bd) v))
          s)))))

(defn- decimal-text
  "The text of BigDecimal bd as a float literal: stripped, with a decimal point or exponent."
  [^BigDecimal bd]
  (let [s (str (.stripTrailingZeros bd))]
    (if (or (str/includes? s ".") (str/includes? s "E")) s (str s ".0"))))

(defn float-lit
  "An exact value v (a BigDecimal, or a rational: Long, BigInt, Ratio) as a float-kind
  Clojure literal (§8.1): a double when its shortest text denotes v exactly, else a
  BigDecimal (M) when v has a terminating decimal expansion of reasonable length (always when
  v is an integer), else a ratio. opts :prefer-ratio (hex floats): a ratio unless v is an
  integer."
  ([v] (float-lit v {}))
  ([v {:keys [prefer-ratio]}]
   (if-let [s (and (not prefer-ratio) (double-exact-text v))]
     (lit s (Double/parseDouble s))
     (if (instance? BigDecimal v)
       (let [s (decimal-text v)] (lit (str s "M") (BigDecimal. s)))
       (let [[^BigInteger num ^BigInteger den] (num-den v)
             integral (= den BigInteger/ONE)
             bd (when-not (and prefer-ratio (not integral)) (terminating-decimal num den))]
         (if (and bd (or integral (<= (count (str bd)) max-decimal-length)))
           (let [s (decimal-text bd)] (lit (str s "M") (BigDecimal. s)))
           (lit (str num "/" den) v)))))))

(defn double-lit
  "A typed float value (float32/float64, already rounded): always a double (§8.2)."
  [r]
  (let [d (double r)]
    (lit (cond (Double/isNaN d) "##NaN"
               (Double/isInfinite d) (if (pos? d) "##Inf" "##-Inf")
               :else (Double/toString d))
         d)))

(defn- hex-float-value
  "The exact value of a Go hexadecimal float literal (underscores removed): 0xH.Hp±E."
  [^String t]
  (let [body (subs t 2)
        pi (str/index-of (str/lower-case body) "p")
        mant (subs body 0 pi)
        exp (Long/parseLong (str/replace (subs body (inc pi)) "+" ""))
        dot (str/index-of mant ".")
        int-part (if dot (subs mant 0 dot) mant)
        frac (if dot (subs mant (inc dot)) "")
        digits (str int-part frac)
        m (if (empty? digits) BigInteger/ZERO (BigInteger. digits 16))
        e (- exp (* 4 (count frac)))]
    (if (neg? e)
      (rational m (pow2 (- e)))
      (rational (.shiftLeft m (int e)) BigInteger/ONE))))

(defn float-value
  "The exact value of a Go float literal's text: a BigDecimal, or a rational for a
  hexadecimal float."
  [^String text]
  (let [t (str/replace text "_" "")]
    (if (str/starts-with? (str/lower-case t) "0x")
      (hex-float-value t)
      (BigDecimal. t))))

(defn float-text-lit
  "A Go float literal's text as a Lit (§8.1): hexadecimal floats exact as a ratio (or a
  BigDecimal when integral)."
  [^String text]
  (let [hex (str/starts-with? (str/lower-case text) "0x")]
    (float-lit (float-value text) {:prefer-ratio hex})))

(defn imag-inner
  "The literal inside (imaginary x) for a Go imaginary literal's text: an integer for an
  integer literal (decimal digits are decimal even with a leading 0), else a float."
  [^String text]
  (let [t (str/replace (subs text 0 (dec (count text))) "_" "")
        lower (str/lower-case t)]
    (cond
      (re-matches #"[0-9]+" t) (let [v (bigint (BigInteger. t))]
                                 (lit (integer-text v) (if (< (.bitLength (biginteger v)) 64) (long v) v)))
      (and (re-matches #"0[xob].*" lower) (not (str/includes? lower "p"))
           (not (str/includes? lower ".")))
      (int-lit t)
      :else (float-text-lit t))))

;;; Runes and strings

(defn- hexv [^String s] (Long/parseLong s 16))

(defn- unescape
  "Decodes the Go escapes of a literal's body into bytes (a ByteArrayOutputStream)."
  [^String body]
  (let [out (java.io.ByteArrayOutputStream.)
        n (.length body)
        put-cp (fn [cp]
                 (let [bs (.getBytes (String. (Character/toChars (int cp))) StandardCharsets/UTF_8)]
                   (.write out bs 0 (alength bs))))]
    (loop [i 0]
      (when (< i n)
        (let [c (.charAt body i)]
          (if (not= c \\)
            (let [cp (.codePointAt body i)]
              (put-cp cp)
              (recur (+ i (Character/charCount cp))))
            (let [e (.charAt body (inc i))]
              (case e
                \a (do (.write out 7) (recur (+ i 2)))
                \b (do (.write out 8) (recur (+ i 2)))
                \f (do (.write out 12) (recur (+ i 2)))
                \n (do (.write out 10) (recur (+ i 2)))
                \r (do (.write out 13) (recur (+ i 2)))
                \t (do (.write out 9) (recur (+ i 2)))
                \v (do (.write out 11) (recur (+ i 2)))
                \\ (do (.write out 92) (recur (+ i 2)))
                \' (do (.write out 39) (recur (+ i 2)))
                \" (do (.write out 34) (recur (+ i 2)))
                \x (do (.write out (int (hexv (subs body (+ i 2) (+ i 4))))) (recur (+ i 4)))
                \u (do (put-cp (hexv (subs body (+ i 2) (+ i 6)))) (recur (+ i 6)))
                \U (do (put-cp (hexv (subs body (+ i 2) (+ i 10)))) (recur (+ i 10)))
                (do (.write out (int (Long/parseLong (subs body (inc i) (+ i 4)) 8)))
                    (recur (+ i 4)))))))))
    out))

(defn string-bytes
  "The bytes of a Go string literal's source text (interpreted or raw)."
  ^bytes [^String text]
  (if (str/starts-with? text "`")
    (.getBytes (str/replace (subs text 1 (dec (count text))) "\r" "") StandardCharsets/UTF_8)
    (.toByteArray ^java.io.ByteArrayOutputStream (unescape (subs text 1 (dec (count text)))))))

(defn rune-value
  "The code point of a Go rune literal's source text."
  [^String text]
  (let [body (subs text 1 (dec (count text)))]
    (if (str/starts-with? body "\\")
      (case (.charAt body 1)
        \x (hexv (subs body 2))
        \u (hexv (subs body 2))
        \U (hexv (subs body 2))
        (\0 \1 \2 \3 \4 \5 \6 \7) (Long/parseLong (subs body 1) 8)
        (let [bs (.toByteArray ^java.io.ByteArrayOutputStream (unescape body))]
          (long (aget bs 0))))
      (long (.codePointAt body 0)))))

(defn- decode-rune
  "Go's utf8.DecodeRune on bs at i: [rune width], rune -1 when invalid (width 1)."
  [^bytes bs i n]
  (let [b (fn [k] (bit-and 0xFF (long (aget bs (int k)))))
        cont? (fn [k] (and (< k n) (= 0x80 (bit-and (b k) 0xC0))))
        b0 (b i)]
    (cond
      (< b0 0x80) [b0 1]
      (< b0 0xC2) [-1 1]
      (< b0 0xE0) (if (cont? (inc i))
                    [(bit-or (bit-shift-left (bit-and b0 0x1F) 6) (bit-and (b (inc i)) 0x3F)) 2]
                    [-1 1])
      (< b0 0xF0) (let [lo (case (int b0) 0xE0 0xA0 0x80)
                        hi (case (int b0) 0xED 0x9F 0xBF)]
                    (if (and (< (inc i) n) (<= lo (b (inc i)) hi) (cont? (+ i 2)))
                      [(bit-or (bit-shift-left (bit-and b0 0x0F) 12)
                               (bit-shift-left (bit-and (b (inc i)) 0x3F) 6)
                               (bit-and (b (+ i 2)) 0x3F)) 3]
                      [-1 1]))
      (< b0 0xF5) (let [lo (case (int b0) 0xF0 0x90 0x80)
                        hi (case (int b0) 0xF4 0x8F 0xBF)]
                    (if (and (< (inc i) n) (<= lo (b (inc i)) hi) (cont? (+ i 2)) (cont? (+ i 3)))
                      [(bit-or (bit-shift-left (bit-and b0 0x07) 18)
                               (bit-shift-left (bit-and (b (inc i)) 0x3F) 12)
                               (bit-shift-left (bit-and (b (+ i 2)) 0x3F) 6)
                               (bit-and (b (+ i 3)) 0x3F)) 4]
                      [-1 1]))
      :else [-1 1])))

(defn utf8-parts
  "Splits bytes into maximal valid UTF-8 runs (strings) and single invalid bytes (integers)."
  [^bytes bs]
  (let [n (alength bs)]
    (loop [i 0 run (StringBuilder.) parts []]
      (if (>= i n)
        (if (pos? (.length run)) (conj parts (str run)) parts)
        (let [[r w] (decode-rune bs i n)]
          (if (neg? (long r))
            (recur (inc i) (StringBuilder.)
                   (cond-> parts
                     (pos? (.length run)) (conj (str run))
                     true (conj (bit-and 0xFF (long (aget bs (int i)))))))
            (recur (+ i (long w)) (.appendCodePoint run (int r)) parts)))))))

(defn rune-form
  "A rune value (code point) as a character Lit, or (rune n) above the Basic Multilingual
  Plane, in the surrogates or negative."
  [cp]
  (let [cp (long cp)]
    (if (and (<= 0 cp 0xFFFF) (not (<= 0xD800 cp 0xDFFF)))
      (lit (char-text cp) (char cp))
      (list 'rune (lit (if (neg? cp) (str cp) (format "0x%X" cp)) cp)))))

(defn bytes-form
  "Go string bytes as a string Lit, or (byte-string part ...) when not valid UTF-8."
  [^bytes bs]
  (let [parts (utf8-parts bs)]
    (if (every? string? parts)
      (let [s (apply str parts)] (lit (string-text s) s))
      (apply list 'byte-string
             (for [p parts]
               (if (string? p) (lit (string-text p) p) (lit (format "0x%02x" (long p)) (long p))))))))

;;; Constant values (§8.2)

(defn- val-rational
  "The exact rational of a helper value: an integer, (:float R) or (:float M E)."
  [v]
  (cond
    (integer? v) v
    (and (seq? v) (= :float (first v)) (= 2 (count v))) (second v)
    (and (seq? v) (= :float (first v)))
    (let [[_ m e] v
          m (biginteger m)
          e (long e)]
      (if (neg? e) (rational m (pow2 (- e))) (rational (.shiftLeft m (int e)) BigInteger/ONE)))
    :else (throw (ex-info (str "not a numeric value: " (pr-str v)) {:v v}))))

(defn const-form
  "The representation (§8.2) of the helper's exact value v at the constant kind k: :bool,
  :string, :int, :rune, :float, :untyped-float, :complex, :untyped-complex (typed :float and
  :complex are rounded doubles), or nil when the kind is not known (then v's own kind)."
  [v k]
  (let [k (or k
              (cond
                (boolean? v) :bool
                (string? v) :string
                (and (seq? v) (= :bytes (first v))) :string
                (integer? v) :int
                (and (seq? v) (= :float (first v))) :float
                (and (seq? v) (= :complex (first v))) :complex))]
    (case k
      :bool (lit (str v) v)
      :string (if (string? v)
                (lit (string-text v) v)
                (bytes-form (byte-array (map unchecked-byte (second v)))))
      :int (let [r (val-rational v)]
             (if (integer? r)
               (integer-lit r)
               (throw (ex-info (str "integer constant with value " (pr-str v)) {:v v}))))
      :rune (rune-form (val-rational v))
      :float (double-lit (val-rational v))
      :untyped-float (float-lit (val-rational v))
      (:complex :untyped-complex)
      (let [[re im] (if (and (seq? v) (= :complex (first v)))
                      [(val-rational (nth v 1)) (val-rational (nth v 2))]
                      [(val-rational v) 0])
            f (if (= k :complex) double-lit float-lit)]
        (list 'complex (f re) (f im)))
      nil)))
