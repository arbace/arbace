(ns jrt.testdata
  "jrt's differential test data (doc/go/JRT-NOTES.md, \"Tests\"): expected values computed by
  the JVM (Arbace on the JDK 26 it runs on), written into DIR/*.txt (bin/jrt: .tmp/jrt-go/
  testdata, regenerated, not tracked; copied into the printed package's testdata/ for jrt's Go
  tests). Reproducible: fixed inputs and seeded java.util.Random, no hash order; two runs give
  the same bytes (bin/jrt testdata --check).

  bin/jrt testdata   runs ARBACE_CLASSPATH=test bin/arbace -m jrt.testdata DIR

  Format, one case per line, fields separated by a tab: the operation's name, its arguments,
  the result. Strings are escaped (\\\\ \\t \\n, and \\uXXXX for every code unit outside
  U+0020-U+007E), so each file is ASCII; a double is its raw bits as 16 hex digits, a float
  as 8; booleans true/false; an exception is !CLASS or !CLASS: MESSAGE."
  (:require [arbace.string :as str]
            [jrt.testdata-util :refer [esc result line write]]
            [jrt.testdata-numbers :as numbers]
            [jrt.testdata-reflect :as reflect])
  (:import [java.io File]))

;; ---------------------------------------------------------------------------------------
;; Strings

(def corpus
  "Strings for String's operations: ASCII, Latin-1, Greek with final sigma, Turkish dotted I,
  German sharp s, ligatures, supplementary characters, unpaired surrogates, whitespace."
  ["" "a" "abc" "ABC" "Hello, World!" "  padded \t\n" " nbsp em　" "x\u0000y"
   "straße" "STRASSE" "İstanbul" "ıi" "ﬁne ﬀ" "ŉ"
   "ΟΔΥΣΣΕΥΣ" "Σ" "AΣ" "ΣA" "AΣ b"
   "café" "CAFÉ" "été" "ǅǄǆ" "ẞ"
   "😀 grin" "a𐐀b" "𐐨" "lone \ud800 high" "lone \udc00 low" "\udc00\ud800"
   "abcabcabc" "aaa" "İ" "i̇" "ᾀᾳ" "և" "ßß" "Mixed Case 123"])

(def needles ["" "a" "b" "c" "abc" "bc" "ABC" "ß" "SS" "Σ" "😀" "\ud800" "x" "aa"])

(defn string-lines []
  (concat
    (for [s corpus
          [op f] [["length" #(.length ^String %)]
                  ["hashCode" #(.hashCode ^String %)]
                  ["isEmpty" #(.isEmpty ^String %)]
                  ["toUpperCase" #(.toUpperCase ^String % java.util.Locale/ROOT)]
                  ["toLowerCase" #(.toLowerCase ^String % java.util.Locale/ROOT)]
                  ["trim" #(.trim ^String %)]
                  ["strip" #(.strip ^String %)]
                  ["stripLeading" #(.stripLeading ^String %)]
                  ["stripTrailing" #(.stripTrailing ^String %)]
                  ["isBlank" #(.isBlank ^String %)]
                  ["codePointCount" #(.codePointCount ^String % 0 (.length ^String %))]
                  ["intern==" #(identical? (.intern ^String %) (.intern (String. ^String %)))]
                  ["getBytesUTF8" #(str/join "," (map (fn [b] (bit-and b 0xff)) (.getBytes ^String % "UTF-8")))]
                  ["getBytesLatin1" #(str/join "," (map (fn [b] (bit-and b 0xff)) (.getBytes ^String % "ISO-8859-1")))]
                  ["getBytesASCII" #(str/join "," (map (fn [b] (bit-and b 0xff)) (.getBytes ^String % "US-ASCII")))]
                  ["toCharArray" #(str/join "," (map int (.toCharArray ^String %)))]]]
      (line op (esc s) (result (f s))))
    (for [s corpus
          i [-1 0 1 2 5 100]
          [op f] [["charAt" #(int (.charAt ^String %1 (int %2)))]
                  ["codePointAt" #(.codePointAt ^String %1 (int %2))]
                  ["codePointBefore" #(.codePointBefore ^String %1 (int %2))]
                  ["substring1" #(.substring ^String %1 (int %2))]
                  ["repeat" #(.repeat ^String %1 (int %2))]
                  ["indexOfChar" #(.indexOf ^String %1 (int (+ 96 %2)))]
                  ["offsetByCodePoints" #(.offsetByCodePoints ^String %1 0 (int %2))]]]
      (line op (esc s) (str i) (result (f s i))))
    (for [s corpus
          [a b] [[0 0] [0 1] [1 3] [2 1] [-1 2] [0 100] [3 3]]
          [op f] [["substring2" #(.substring ^String %1 (int %2) (int %3))]
                  ["subSequence" #(str (.subSequence ^String %1 (int %2) (int %3)))]
                  ["codePointCount2" #(.codePointCount ^String %1 (int %2) (int %3))]
                  ["indexOfCharRange" #(.indexOf ^String %1 (int 97) (int %2) (int %3))]]]
      (line op (esc s) (str a) (str b) (result (f s a b))))
    (for [s corpus
          n needles
          [op f] [["equals" #(.equals ^String %1 %2)]
                  ["equalsIgnoreCase" #(.equalsIgnoreCase ^String %1 ^String %2)]
                  ["compareTo" #(.compareTo ^String %1 ^String %2)]
                  ["compareToIgnoreCase" #(.compareToIgnoreCase ^String %1 ^String %2)]
                  ["indexOf" #(.indexOf ^String %1 ^String %2)]
                  ["indexOf1" #(.indexOf ^String %1 ^String %2 (int 1))]
                  ["lastIndexOf" #(.lastIndexOf ^String %1 ^String %2)]
                  ["lastIndexOf2" #(.lastIndexOf ^String %1 ^String %2 (int 2))]
                  ["contains" #(.contains ^String %1 ^String %2)]
                  ["startsWith" #(.startsWith ^String %1 ^String %2)]
                  ["startsWith1" #(.startsWith ^String %1 ^String %2 (int 1))]
                  ["endsWith" #(.endsWith ^String %1 ^String %2)]
                  ["concat" #(.concat ^String %1 ^String %2)]
                  ["replace" #(.replace ^String %1 ^CharSequence %2 "<>")]
                  ["regionMatchesCI" #(.regionMatches ^String %1 true (int 1) ^String %2 (int 0) (int 2))]
                  ["lastIndexOfChar" #(.lastIndexOf ^String %1 (int (if (seq %2) (.codePointAt ^String %2 0) 97)))]]]
      (line op (esc s) (esc n) (result (f s n))))
    (for [s corpus]
      (line "replaceChar" (esc s) (result (.replace ^String s \a \Z))))
    (for [cs ["a" "é" "Σ" "😀"]
          sep ["" "," ", "]]
      (line "join" (esc sep) (esc cs) (result (String/join ^CharSequence sep ^"[Ljava.lang.CharSequence;" (into-array CharSequence [cs "b" cs])))))
    (for [cp [-1 0 0x41 0xe9 0xffff 0x10000 0x1f600 0x10ffff 0x110000]]
      (line "valueOfCodePoint" (str cp) (result (String/valueOf (Character/toChars (int cp))))))
    (for [bs [[] [0x41 0x42] [0xc3 0xa9] [0xe2 0x82 0xac] [0xf0 0x9f 0x98 0x80] [0xc3] [0xe2 0x82]
              [0xf0 0x9f 0x98] [0xff] [0x80 0x41] [0xed 0xa0 0x80] [0xc0 0xaf] [0xe0 0x80 0xaf]
              [0xf4 0x90 0x80 0x80] [0xf8 0x88 0x80 0x80 0x80] [0xe2 0x41 0x42] [0xf0 0x9f 0x41]
              [0xc3 0xa9 0xe2 0x82 0xac 0xf0 0x9f 0x98 0x80 0x41] [0xf0 0x41] [0xf5 0x80]
              [0xe0 0xa0] [0xed 0x9f 0xbf]]]
      (line "newStringUTF8" (str/join "," bs)
            (result (String. (byte-array (map unchecked-byte bs)) "UTF-8"))))))

;; String's and StringBuilder's exceptions and edge cases, and Java's messages of the
;; implicit checks

(defmacro case-line [name & body]
  `(line ~name (result ~@body)))

(defn sb [] (StringBuilder. "abc"))

(defn exception-lines []
  [(case-line "charAt5" (.charAt "abc" 5))
   (case-line "charAt-1" (.charAt "abc" -1))
   (case-line "substring5" (.substring "abc" 5))
   (case-line "substring-1" (.substring "abc" -1))
   (case-line "substring21" (.substring "abc" 2 1))
   (case-line "substring05" (.substring "abc" 0 5))
   (case-line "codePointAt5" (.codePointAt "abc" 5))
   (case-line "codePointBefore0" (.codePointBefore "abc" 0))
   (case-line "subSequence21" (.subSequence "abc" 2 1))
   (case-line "repeat-1" (.repeat "abc" -1))
   (case-line "newStringChars" (String. (char-array "abc") 2 5))
   (case-line "newStringCodePoints" (String. (int-array [65 -1]) 0 2))
   (case-line "getChars" (.getChars "abc" 0 3 (char-array 2) 0))
   (case-line "indexOfRange" (.indexOf "abc" "b" 2 1))
   (case-line "sbCharAt5" (.charAt (sb) 5))
   (case-line "sbSetCharAt5" (.setCharAt (sb) 5 \x))
   (case-line "sbDeleteCharAt5" (.deleteCharAt (sb) 5))
   (case-line "sbDelete21" (.delete (sb) 2 1))
   (case-line "sbDelete56" (.delete (sb) 5 6))
   (case-line "sbDelete15" (str (.delete (sb) 1 5)))
   (case-line "sbInsert5" (.insert (sb) 5 "x"))
   (case-line "sbInsert-1" (.insert (sb) -1 "x"))
   (case-line "sbSetLength-1" (.setLength (sb) -1))
   (case-line "sbSetLength5" (let [b (sb)] (.setLength b 5) (str/join "," (map int (str b)))))
   (case-line "sbSubstring5" (.substring (sb) 5))
   (case-line "sbSubstring21" (.substring (sb) 2 1))
   (case-line "sbAppendCS" (.append (sb) "xyz" 2 5))
   (case-line "sbAppendChars" (.append (sb) (char-array "xyz") 2 5))
   (case-line "sbAppendCodePoint" (.appendCodePoint (sb) -1))
   (case-line "sbAppendCodePointBig" (.appendCodePoint (sb) 0x110000))
   (case-line "sbRepeat-1" (.repeat (sb) (int 65) -1))
   (case-line "sbRepeatCP" (str (.repeat (sb) (int 0x1f600) 2)))
   (case-line "sbReverse" (str (.reverse (StringBuilder. "a😀b\udc00\ud800c"))))
   (case-line "sbReplace" (str (.replace (sb) 1 10 "XYZ")))
   (case-line "sbInsertChar" (str (.insert (sb) 1 \Z)))
   (case-line "sbCodePointCount" (.codePointCount (sb) 2 1))
   (case-line "sbOffsetByCodePoints" (.offsetByCodePoints (sb) 4 0))
   (case-line "sbGetChars" (.getChars (sb) 0 3 (char-array 2) 0))
   (case-line "sbCapacity" (.capacity (StringBuilder.)))
   (case-line "sbCapacityGrow" (let [b (StringBuilder.)] (.append b "01234567890123456") (.capacity b)))
   (case-line "sbCapacityString" (.capacity (StringBuilder. "abc")))
   (case-line "sbNew-1" (StringBuilder. -1))
   (case-line "sbCompareTo" (.compareTo (StringBuilder. "abd") (StringBuilder. "abc")))
   (case-line "aget5" (aget (int-array 3) 5))
   (case-line "aget-1" (aget (int-array 3) -1))
   (case-line "agetObj" (aget (object-array 2) 2))
   (case-line "newArray-1" (int-array -1))
   (case-line "newObjArray-1" (make-array Object -1))
   (case-line "multiArray" (make-array Integer/TYPE 2 -3))
   (case-line "arrayStore" (let [^"[Ljava.lang.Object;" a (make-array String 1)] (aset a 0 (Object.))))
   (case-line "arraycopyNull" (System/arraycopy nil 0 (int-array 1) 0 1))
   (case-line "arraycopyNotArray" (System/arraycopy "x" 0 (int-array 1) 0 1))
   (case-line "arraycopyNotArrayDst" (System/arraycopy (int-array 1) 0 "x" 0 1))
   (case-line "arraycopyPrimMismatch" (System/arraycopy (int-array 1) 0 (long-array 1) 0 1))
   (case-line "arraycopyPrimToObj" (System/arraycopy (int-array 1) 0 (object-array 1) 0 1))
   (case-line "arraycopyObjToPrim" (System/arraycopy (object-array 1) 0 (int-array 1) 0 1))
   (case-line "arraycopySrcNeg" (System/arraycopy (int-array 5) -1 (int-array 5) 0 1))
   (case-line "arraycopyDstNeg" (System/arraycopy (int-array 5) 0 (int-array 5) -1 1))
   (case-line "arraycopyLenNeg" (System/arraycopy (int-array 5) 0 (int-array 5) 0 -1))
   (case-line "arraycopySrcEnd" (System/arraycopy (int-array 5) 3 (int-array 5) 0 3))
   (case-line "arraycopyDstEnd" (System/arraycopy (int-array 5) 0 (int-array 5) 3 3))
   (case-line "arraycopyObjSrcEnd" (System/arraycopy (object-array 5) 3 (object-array 5) 0 3))
   (case-line "arraycopyTypeMismatch" (System/arraycopy (make-array Long 1) 0 (make-array String 1) 0 1))
   (case-line "arraycopyElemMismatch" (let [a (object-array [(Long/valueOf 1) "x"]) d (make-array Long 2)]
                                        (try (System/arraycopy a 0 d 0 2) (catch Throwable e (throw (RuntimeException. (str (vec d) " " (.getMessage e))))))))
   (case-line "arraycopyOverlap" (let [a (int-array [1 2 3 4 5])] (System/arraycopy a 0 a 1 4) (str/join "," a)))
   (case-line "divZero" (let [a (int 1) b (int 0)] (unchecked-divide-int a b)))
   (case-line "remZero" (let [a (int 1) b (int 0)] (unchecked-remainder-int a b)))
   (case-line "ldivZero" (let [a (long 1) b (long 0)] (quot a b)))
   (case-line "classCast" (let [^Object o (RuntimeException.)] (.length ^String (cast String o))))
   (case-line "classCastMsg" (try (String/valueOf ^chars (.cast (Class/forName "[C") (Long/valueOf 1))) (catch ClassCastException e (.getMessage e))))
   (case-line "classCastArbace" (let [^Object o "s"] (.invoke ^arbace.lang.IFn o)))
   (case-line "suppressNull" (.addSuppressed (RuntimeException.) nil))
   (case-line "suppressSelf" (let [e (RuntimeException. "x")] (.addSuppressed e e)))
   (case-line "initCauseTwice" (.initCause (RuntimeException. "a" (Exception. "b")) (Exception. "c")))
   (case-line "initCauseSelf" (let [e (RuntimeException. "x")] (.initCause e e)))
   (case-line "toStringNoMsg" (str (IllegalStateException.)))
   (case-line "toStringMsg" (str (IllegalStateException. "boom")))
   (case-line "toStringCause" (str (RuntimeException. (IllegalStateException. "inner"))))
   (case-line "causeMsg" (.getMessage (RuntimeException. (IllegalStateException. "inner"))))
   (case-line "causeNull" (.getMessage (RuntimeException. ^Throwable (identity nil))))
   (case-line "aioobeInt" (.getMessage (ArrayIndexOutOfBoundsException. 7)))
   (case-line "sioobeInt" (.getMessage (StringIndexOutOfBoundsException. 7)))
   (case-line "ioobeInt" (.getMessage (IndexOutOfBoundsException. 7)))
   (case-line "assertionErrorObj" (.getMessage (AssertionError. (Long/valueOf 5))))
   (case-line "checkcast" (let [^String s (identity (StringBuilder.))] (.length s)))
   (case-line "assertionErrorThrowable" (str (.getCause (AssertionError. (RuntimeException. "c")))))
   (case-line "eiieToString" (str (ExceptionInInitializerError. (RuntimeException. "c"))))
   (case-line "eiieMessage" (.getMessage (ExceptionInInitializerError. (RuntimeException. "c"))))
   (case-line "steToString" (str (StackTraceElement. "a.B" "m" "B.java" 12)))
   (case-line "steToStringJdk" (str (StackTraceElement. "java.lang.String" "charAt" "String.java" 1555)))
   (case-line "steToStringNoFile" (str (StackTraceElement. "a.B" "m" nil -1)))
   (case-line "steToStringNative" (str (StackTraceElement. "a.B" "m" "B.java" -2)))
   (case-line "steHash" (.hashCode (StackTraceElement. "a.B" "m" "B.java" 12)))
   (case-line "steHashNoFile" (.hashCode (StackTraceElement. "a.B" "m" nil -1)))
   (case-line "waitNotOwner" (.wait (Object.)))
   (case-line "notifyNotOwner" (.notify (Object.)))
   (case-line "waitNegative" (let [o (Object.)] (locking o (.wait o -1))))
   (case-line "waitNanos" (let [o (Object.)] (locking o (.wait o 1 1000000))))
   (case-line "objectToString" (let [s (str (Object.))] (subs s 0 (.indexOf s "@"))))
   (case-line "arrayClassNames" (str/join "," (map #(.getName (class %)) [(int-array 0) (object-array 0) (make-array String 0) (make-array Integer/TYPE 0 0) (boolean-array 0) (make-array String 0 0)])))
   (case-line "arraySimpleNames" (str/join "," (map #(.getSimpleName (class %)) [(int-array 0) (make-array String 0 0)])))
   (case-line "arrayModifiers" (str/join "," (map #(.getModifiers (class %)) [(int-array 0) (make-array String 0) (make-array Integer/TYPE 0 0)])))
   (case-line "primModifiers" (.getModifiers Integer/TYPE))
   (case-line "classToString" (str/join "," (map str [String Integer/TYPE Comparable (class (int-array 0))])))
   (case-line "arrayAssignable" (str/join "," [(.isAssignableFrom (class (object-array 0)) (class (make-array String 0)))
                                                (.isAssignableFrom (class (make-array String 0)) (class (object-array 0)))
                                                (.isAssignableFrom Object (class (int-array 0)))
                                                (.isAssignableFrom Cloneable (class (int-array 0)))
                                                (.isAssignableFrom java.io.Serializable (class (make-array String 0)))
                                                (.isAssignableFrom (class (object-array 0)) (class (int-array 0)))
                                                (.isAssignableFrom (class (object-array 0)) (class (make-array Integer/TYPE 0 0)))]))
   (case-line "forNameArray" (.getName (Class/forName "[[Ljava.lang.String;")))
   (case-line "forNameMissing" (Class/forName "no.such.Class"))])

(defn char-lines
  "Every code point whose String toUpperCase or toLowerCase (root locale) is not itself:
  the case mappings of JDK 26's Character and SpecialCasing, as String applies them."
  []
  (for [cp (range 0 0x110000)
        :when (not (<= 0xD800 cp 0xDFFF))
        :let [s (String. (Character/toChars cp))
              u (.toUpperCase s java.util.Locale/ROOT)
              l (.toLowerCase s java.util.Locale/ROOT)]
        :when (or (not= u s) (not= l s))]
    (line (format "%X" cp) (esc u) (esc l))))

(defn -main [dir]
  (let [d (File. ^String dir)]
    (.mkdirs d)
    (write d "strings.txt" (string-lines))
    (write d "exceptions.txt" (exception-lines))
    (write d "chars.txt" (char-lines))
    (numbers/write-all d)
    (reflect/write-all d)
    (shutdown-agents)))
