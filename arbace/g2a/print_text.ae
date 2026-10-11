(ns arbace.g2c.print-text
  "The text level of g2c's printer (doc/go/PRINTER-NOTES.md): Go literal spellings of the Go
  forms' values (doc/go/SPEC.md §8.1, §12.2) and the column alignment gofmt does with
  text/tabwriter.

  The alignment is a port of go1.27.1's src/text/tabwriter/tabwriter.go (Writer.format,
  writeLines, writePadding) for the configuration gofmt uses (go/printer with UseSpaces and
  TabIndent: minwidth 0, tabwidth 8, padding 1, padchar ' ', DiscardEmptyColumns and
  TabIndent). Cells end at a tab (indentation, a hard tab) or a vertical tab (an alignment
  separator); a form feed ends a line and every column block, as in tabwriter. Text that must
  not be cut into cells (raw strings, comments) is written with the escape characters of
  `escape`, which `unescape` restores after alignment."
  (:require [arbace.string :as str])
  (:import [java.math BigDecimal BigInteger]))

;; ---------------------------------------------------------------------------------------
;; Literals

(defn- go-printable?
  "Go's strconv.IsPrint, for code points: letters, marks, numbers, punctuation, symbols and
  the ASCII space."
  [^long cp]
  (or (== cp 32)
      (let [t (Character/getType (int cp))]
        (contains? #{Character/UPPERCASE_LETTER Character/LOWERCASE_LETTER
                     Character/TITLECASE_LETTER Character/MODIFIER_LETTER Character/OTHER_LETTER
                     Character/NON_SPACING_MARK Character/ENCLOSING_MARK
                     Character/COMBINING_SPACING_MARK Character/DECIMAL_DIGIT_NUMBER
                     Character/LETTER_NUMBER Character/OTHER_NUMBER
                     Character/CONNECTOR_PUNCTUATION Character/DASH_PUNCTUATION
                     Character/START_PUNCTUATION Character/END_PUNCTUATION
                     Character/INITIAL_QUOTE_PUNCTUATION Character/FINAL_QUOTE_PUNCTUATION
                     Character/OTHER_PUNCTUATION Character/MATH_SYMBOL
                     Character/CURRENCY_SYMBOL Character/MODIFIER_SYMBOL
                     Character/OTHER_SYMBOL}
                   (int t)))))

(defn- hex [^long n ^long width]
  (let [s (Long/toHexString n)]
    (str (apply str (repeat (- width (count s)) \0)) s)))

(defn- escape-cp
  "The spelling of one code point inside a Go literal quoted by quote (\\\" or \\')."
  [^long cp quote]
  (case (int cp)
    7 "\\a" 8 "\\b" 12 "\\f" 10 "\\n" 13 "\\r" 9 "\\t" 11 "\\v" 92 "\\\\"
    (cond
      (== cp (long (int quote))) (str "\\" quote)
      (go-printable? cp) (String. (Character/toChars (int cp)))
      (< cp 0x80) (str "\\x" (hex cp 2))
      (<= cp 0xFFFF) (str "\\u" (hex cp 4))
      :else (str "\\U" (hex cp 8)))))

(defn code-points
  "The code points of a Clojure string; throws on an unpaired surrogate, which a Go string
  in the forms must not contain (§8.1)."
  [^String s]
  (loop [i 0 acc (transient [])]
    (if (< i (.length s))
      (let [cp (.codePointAt s i)]
        (when (and (>= cp 0xD800) (<= cp 0xDFFF))
          (throw (ex-info (str "unpaired surrogate in a Go string: " (pr-str s)) {:string s})))
        (recur (+ i (Character/charCount cp)) (conj! acc cp)))
      (persistent! acc))))

(defn go-string
  "An interpreted Go string literal denoting the UTF-8 encoding of s."
  [^String s]
  (let [sb (StringBuilder. "\"")]
    (doseq [cp (code-points s)]
      (.append sb ^String (escape-cp cp \")))
    (.append sb "\"")
    (.toString sb)))

(defn raw-string-ok?
  "Whether s can be a raw string literal: no backquote or carriage return, and only printable
  characters, tabs and newlines."
  [^String s]
  (every? (fn [cp] (and (not= cp 96) (not= cp 13)
                        (or (== cp 9) (== cp 10) (go-printable? cp))))
          (code-points s)))

(defn go-byte-string
  "(byte-string part ...): an interpreted string of the parts, strings as their UTF-8 and
  integers 0-255 as \\x escapes."
  [parts]
  (let [sb (StringBuilder. "\"")]
    (doseq [p parts]
      (cond
        (string? p) (doseq [cp (code-points p)] (.append sb ^String (escape-cp cp \")))
        (and (integer? p) (<= 0 p 255)) (.append sb (str "\\x" (hex (long p) 2)))
        :else (throw (ex-info (str "bad byte-string part " (pr-str p)) {:part p}))))
    (.append sb "\"")
    (.toString sb)))

(defn go-rune
  "A Go rune literal for code point cp."
  [^long cp]
  (when (or (neg? cp) (> cp 0x10FFFF) (and (>= cp 0xD800) (<= cp 0xDFFF)))
    (throw (ex-info (str "not a rune: " cp) {:rune cp})))
  (str "'" (escape-cp cp \') "'"))

(defn- float-kind
  "s with a decimal point when it has neither a point nor an exponent, so that it stays a
  float literal."
  [^String s]
  (if (or (str/includes? s ".") (str/includes? s "e") (str/includes? s "E")) s (str s ".0")))

(defn- pow2-exponent
  "k when n is 2^k, else nil."
  [^BigInteger n]
  (when (and (pos? (.signum n)) (== 1 (.bitCount n))) (dec (.bitLength n))))

(defn- strip-2-5
  "[rest a b]: n = rest * 2^a * 5^b with rest not divisible by 2 or 5."
  [^BigInteger n]
  (let [five (BigInteger/valueOf 5)]
    (loop [n n a 0 b 0]
      (cond
        (not (.testBit n 0)) (recur (.shiftRight n 1) (inc a) b)
        (zero? (.signum (.mod n five))) (recur (.divide n five) a (inc b))
        :else [n a b]))))

(defn go-float
  "The Go float literal of a non-negative float value of the forms (§8.1, §12.2): a double as
  the exact decimal of its shortest representation, a BigDecimal as its decimal, a ratio as a
  short decimal or a hex float."
  [x]
  (cond
    (instance? Double x)
    (let [d (double x)]
      (when (or (Double/isNaN d) (Double/isInfinite d))
        (throw (ex-info (str "no Go literal for " d) {:value d})))
      (str/lower-case (Double/toString d)))
    (instance? BigDecimal x) (float-kind (str/lower-case (.toString ^BigDecimal x)))
    (ratio? x)
    (let [n (biginteger (numerator x)) d (biginteger (denominator x))
          [rest a b] (strip-2-5 d)]
      (cond
        (and (= rest BigInteger/ONE) (<= (max a b) 40))
        (float-kind (.toPlainString (.divide (BigDecimal. ^BigInteger n) (BigDecimal. ^BigInteger d))))
        (pow2-exponent d)
        (str "0x" (.toString ^BigInteger n 16) "p-" (pow2-exponent d))
        :else (throw (ex-info (str "the ratio " x " has no finite Go float literal") {:value x}))))
    :else (throw (ex-info (str "not a float value: " (pr-str x)) {:value x}))))

(defn negative-number?
  "Whether a numeric literal of the forms is negative (Go: a unary minus on the literal; -0.0
  counts, as the reader keeps its sign)."
  [x]
  (cond
    (instance? Double x) (or (neg? (double x)) (== (Double/doubleToRawLongBits (double x))
                                                   Long/MIN_VALUE))
    (number? x) (neg? x)
    :else false))

(defn go-number
  "The Go literal of a non-negative number of the forms: integers in decimal, floats by
  go-float."
  [x]
  (cond
    (instance? Double x) (go-float (Math/abs (double x)))
    (integer? x) (str x)
    :else (go-float x)))

;; ---------------------------------------------------------------------------------------
;; Escaping text that must not be cut into cells

(def ^:private escapes {(char 9) (char 0xE000) (char 10) (char 0xE001) (char 11) (char 0xE002) (char 12) (char 0xE003)})
(def ^:private unescapes (into {} (map (fn [[k v]] [v k]) escapes)))

(defn escape
  "s with its tabs, newlines, vertical tabs and form feeds replaced by private characters,
  so that alignment treats it as one piece of text."
  [^String s]
  (if (some escapes s) (apply str (map #(escapes % %) s)) s))

(defn unescape [^String s]
  (if (some unescapes s) (apply str (map #(unescapes % %) s)) s))

;; ---------------------------------------------------------------------------------------
;; Alignment: text/tabwriter

(defn- cell-width [^String s] (.codePointCount s 0 (.length s)))

(defn- parse-lines
  "The lines of one block of text (no form feeds): each a vector of cells
  {:text :width :htab}, the last cell being the text after the last separator."
  [^String text]
  (vec (for [^String line (str/split text #"\n" -1)]
         (loop [i 0 start 0 cells (transient [])]
           (if (< i (.length line))
             (let [c (.charAt line i)]
               (if (or (= c (char 9)) (= c (char 11)))
                 (let [t (subs line start i)]
                   (recur (inc i) (inc i) (conj! cells {:text t :width (cell-width t) :htab (= c (char 9))})))
                 (recur (inc i) start cells)))
             (let [t (subs line start)]
               (persistent! (conj! cells {:text t :width (cell-width t) :htab false}))))))))

(defn- write-padding [^StringBuilder sb textw cellw use-tabs]
  (if use-tabs
    (let [cellw (* 8 (quot (+ cellw 7) 8))
          n (- cellw textw)]
      (dotimes [_ (quot (+ n 7) 8)] (.append sb \tab)))
    (dotimes [_ (- cellw textw)] (.append sb \space))))

(defn- write-lines [^StringBuilder sb lines widths line0 line1]
  (doseq [i (range line0 line1)]
    (let [line (nth lines i)
          nw (count widths)]
      (loop [j 0 use-tabs true]
        (when (< j (count line))
          (let [c (nth line j)]
            (if (zero? (count (:text c)))
              (do (when (< j nw) (write-padding sb (:width c) (nth widths j) use-tabs))
                  (recur (inc j) use-tabs))
              (do (.append sb ^String (:text c))
                  (when (< j nw) (write-padding sb (:width c) (nth widths j) false))
                  (recur (inc j) false))))))
      (when (< (inc i) (count lines)) (.append sb \newline)))))

(defn- format-block
  "tabwriter's Writer.format over lines [line0, line1) with the column widths found so far."
  [sb lines widths line0 line1]
  (let [column (count widths)]
    (loop [this line0 line0 line0]
      (if (< this line1)
        (let [line (nth lines this)]
          (if (>= column (dec (count line)))
            (recur (inc this) line0)
            (do
              (write-lines sb lines widths line0 this)
              (let [line0 this
                    [end width discardable]
                    (loop [this this width 0 discardable true]
                      (if (< this line1)
                        (let [line (nth lines this)]
                          (if (>= column (dec (count line)))
                            [this width discardable]
                            (let [c (nth line column)]
                              (recur (inc this) (max width (inc (:width c)))
                                     (and discardable (zero? (:width c)) (not (:htab c)))))))
                        [this width discardable]))
                    width (if discardable 0 width)]
                (format-block sb lines (conj widths width) line0 end)
                (recur (inc end) end)))))
        (write-lines sb lines widths line0 line1)))))

(defn align
  "Aligns text as gofmt's tabwriter does and restores escaped text."
  [^String text]
  (let [sb (StringBuilder.)
        blocks (str/split text #"\f" -1)]
    (doseq [[i block] (map-indexed vector blocks)]
      (when (pos? i) (.append sb \newline))
      (let [lines (parse-lines block)]
        (format-block sb lines [] 0 (count lines))))
    (unescape (.toString sb))))
