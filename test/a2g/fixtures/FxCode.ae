;; c2g's fixtures (doc/go/C2G-NOTES.md, "Fixtures"): class forms exercising C2G-SPEC §14's
;; coverage that the oracle's class scripts do not reach. bin/c2g-check fixtures loads them on
;; the JVM Arbace, records the result of every public static no-argument method whose name
;; starts with t, translates them with c2g and compares. Code: arithmetic, conversions,
;; control flow, switch, exceptions, strings, arrays.

(in-ns 'c2g.fixtures)

(import '(java.util ArrayList Iterator))

(do

(defclass ^:public ^:enum FxColor
  (constants RED GREEN BLUE))

(defclass ^:public FxCode
  (field ^:static ^int counter 0)

  (method ^:static next ^int [] (set! counter (unchecked-add-int counter 1)) counter)

  ;; ----- arithmetic (§7.4)
  (method ^:public ^:static tIntOverflow ^String []
    (let [^int a Integer/MAX_VALUE ^long b Long/MIN_VALUE]
      (java-str (unchecked-add-int a 1) " " (unchecked-multiply-int a 3) " " (unchecked-negate-int Integer/MIN_VALUE)
                " " (unchecked-subtract b 1) " " (unchecked-divide-int Integer/MIN_VALUE -1)
                " " (unchecked-remainder-int -7 3) " " (unchecked-divide-int -7 2))))
  (method ^:public ^:static tShifts ^String []
    (let [^int x -8 ^long y -8 ^int n 33]
      (java-str (bit-shift-left-int 1 n) " " (bit-shift-right-int x 1) " " (unsigned-bit-shift-right-int x 28)
                " " (unsigned-bit-shift-right y 60) " " (bit-shift-left 1 65) " " (bit-xor-int x 5) " " (bit-not-int x)
                " " (bit-and y 255) " " (bit-or-int 12 3))))
  (method ^:public ^:static tConversions ^String []
    (let [^double big 1e20 ^double nan (unchecked-divide 0.0 0.0) ^float f (float 3.75) ^long l 300]
      (java-str (unchecked-int big) " " (unchecked-long big) " " (unchecked-int nan) " " (unchecked-long (unchecked-negate big))
                " " (unchecked-byte l) " " (unchecked-short 70000) " " (unchecked-char 65601) " " (unchecked-int f)
                " " (unchecked-float 16777217) " " (unchecked-double f) " " (unchecked-int (unchecked-char -1)))))
  (method ^:public ^:static tDivZero ^String []
    (let [^int z (unchecked-subtract-int (FxCode/next) counter)]
      (try (java-str (unchecked-divide-int 5 z))
           (catch ArithmeticException e (java-str "caught " (.getMessage e))))))
  (method ^:public ^:static tDoubles ^String []
    (let [^double a 0.1 ^double b 0.2 ^double c 0.3 ^float fa (float 0.1)]
      (java-str (unchecked-add a b) " " (unchecked-add (unchecked-multiply a b) c) " " (unchecked-remainder 7.5 2.0)
                " " (unchecked-divide 1.0 0.0) " " (unchecked-negate 0.0) " " (unchecked-multiply-float fa fa)
                " " (unchecked-divide 1.0 3.0) " " 1e-5 " " 1.0E7 " " 123456789.0 " " (float 1e10))))
  (method ^:public ^:static tCompound ^String []
    (let [^:mutable ^byte b 120 ^:mutable ^char c \a ^:mutable ^short s 32767 ^:mutable ^int i 0]
      (set! b (unchecked-byte (unchecked-add-int b 10)))
      (set! c (unchecked-char (unchecked-add-int c 2)))
      (set! s (unchecked-short (unchecked-add-int s 1)))
      (set! i (unchecked-add-int i (unchecked-int 2.9)))
      (java-str b " " c " " s " " i " " (unchecked-add-int c 1))))
  (method ^:public ^:static tMathExact ^String []
    (try (java-str (Math/addExact (int 1) (int 2)) " " (Math/multiplyExact Long/MAX_VALUE 2))
         (catch ArithmeticException e (java-str "overflow: " (.getMessage e)))))

  ;; ----- control flow (§7.7)
  (method ^:public ^:static tLabels ^String []
    (let [sb (StringBuilder.)]
      (label :outer
        (loop [^int i 0]
          (when (< i 5)
            (loop [^int j 0]
              (when (< j 5)
                (when (== j 3) (continue :outer (unchecked-inc-int i)))
                (when (== (unchecked-multiply-int i j) 6) (break :outer))
                (.append sb (java-str i j ","))
                (recur (unchecked-inc-int j))))
            (recur (unchecked-inc-int i)))))
      (.toString sb)))
  (method ^:public ^:static tWhileFor ^String []
    (let [sb (StringBuilder.) ^:mutable ^int n 10 xs (new int/1 [3 1 4 1 5])]
      (while (> n 0) (.append sb n) (set! n (unchecked-subtract-int n 3)))
      (for-each [^int x xs] (when (== x 1) (continue)) (.append sb (java-str "<" x ">")))
      (loop [] (.append sb "d") (set! n (unchecked-add-int n 1)) (when (< n 2) (recur)))
      (.toString sb)))
  (method ^:public ^:static tIterable ^String []
    (let [l (ArrayList.) sb (StringBuilder.)]
      (.add l "x") (.add l "y") (.add l "z")
      (for-each [^String s l] (.append sb (.toUpperCase s)))
      (let [^Iterator it (.iterator l)]
        (while (.hasNext it) (.append sb (.next it))))
      (.toString sb)))
  (method ^:public ^:static tYield ^String []
    (let [r (label :L
              (for-each [^int x (new int/1 [2 4 7 8])]
                (when (== 1 (unchecked-remainder-int x 2)) (break :L (Integer/valueOf x))))
              nil)]
      (java-str "found " r)))

  ;; ----- switch (§7.8)
  (method ^:static sw ^String [^int x]
    (switch x 0 "zero" (1 2 3) "small" 100 "hundred" -2147483648 "min" "other"))
  (method ^:static sws ^int [^String s]
    (switch s "a" 1 "b" 2 ("Aa" "BB") 3 "" 4 -1))
  (method ^:static swe ^String [^FxColor c]
    (switch c RED "r" (GREEN BLUE) "gb" nil "null"))
  (method ^:static swp ^String [^Object o]
    (switch o
      [^String s :when (.isEmpty s)] "empty string"
      [^String s] (java-str "string " s)
      [^Integer i] (java-str "int " (unchecked-add-int (.intValue i) 1))
      nil "null"
      [^Object x] "object"))
  (method ^:public ^:static tSwitch ^String []
    (java-str (FxCode/sw 0) (FxCode/sw 2) (FxCode/sw 100) (FxCode/sw -2147483648) (FxCode/sw 7)
              " " (FxCode/sws "a") (FxCode/sws "b") (FxCode/sws "Aa") (FxCode/sws "BB") (FxCode/sws "") (FxCode/sws "zz")
              " " (FxCode/swe FxColor/RED) (FxCode/swe FxColor/BLUE) (FxCode/swe nil)
              " " (FxCode/swp "") "/" (FxCode/swp "q") "/" (FxCode/swp (Integer/valueOf 41)) "/" (FxCode/swp nil) "/" (FxCode/swp (Long/valueOf 1))))
  (method ^:public ^:static tSwitchNull ^String []
    (try (java-str (FxCode/sws nil)) (catch NullPointerException e "npe")))
  (method ^:public ^:static tEnum ^String []
    (let [sb (StringBuilder.)]
      (for-each [^FxColor c (FxColor/values)] (.append sb (java-str (.name c) (.ordinal c) " ")))
      (.append sb (FxColor/valueOf "GREEN"))
      (.append sb (.compareTo FxColor/RED FxColor/BLUE))
      (try (FxColor/valueOf "PINK") (catch IllegalArgumentException e (.append sb " bad")))
      (.toString sb)))

  ;; ----- exceptions (§7.9)
  (method ^:static fin ^int [^StringBuilder sb]
    (try (.append sb "try,") (return 1)
         (finally (.append sb "finally,"))))
  (method ^:static finOverride ^int [^StringBuilder sb]
    (try (throw (IllegalStateException. "lost"))
         (finally (.append sb "fo,") (return 2))))
  (method ^:static nested ^String [^int k]
    (let [sb (StringBuilder.)]
      (try
        (try
          (when (== k 1) (throw (IllegalArgumentException. "one")))
          (when (== k 2) (throw (UnsupportedOperationException. "two")))
          (when (== k 3) (aget (new int/1 2) 5))
          (when (== k 4) (.length ^String (FxCode/nothing)))
          (.append sb "ok")
          (catch [IllegalArgumentException UnsupportedOperationException] e (.append sb (java-str "multi " (.getMessage e))))
          (finally (.append sb ";inner")))
        (catch RuntimeException e (.append sb (java-str ";outer " (.getName (.getClass e)))))
        (finally (.append sb ";end")))
      (.toString sb)))
  (method ^:static nothing ^Object [] nil)
  (method ^:public ^:static tExceptions ^String []
    (let [sb (StringBuilder.)]
      (.append sb (FxCode/fin sb))
      (.append sb (FxCode/finOverride sb))
      (java-str sb " | " (FxCode/nested 0) " | " (FxCode/nested 1) " | " (FxCode/nested 2) " | " (FxCode/nested 3) " | " (FxCode/nested 4))))
  (method ^:public ^:static tLoopFinally ^String []
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i 4)
          (try
            (when (== i 1) (continue (unchecked-inc-int i)))
            (when (== i 3) (break))
            (.append sb i)
            (finally (.append sb "f")))
          (recur (unchecked-inc-int i))))
      (.toString sb)))
  (method ^:public ^:static tCause ^String []
    (try (try (throw (IllegalStateException. "inner"))
              (catch IllegalStateException e (throw (RuntimeException. "outer" e))))
         (catch RuntimeException e (java-str (.getMessage e) " <- " (.getMessage (.getCause e))))))
  (method ^:public ^:static tUncaught ^String []
    (throw (UnsupportedOperationException. "fixture")))
  (method ^:public ^:static tResources ^String []
    (let [sb (StringBuilder.)]
      (try
        ;; java.lang.AutoCloseable is outside the closed world: a reader's close
        (with-resources [^java.io.StringReader a (anon java.io.StringReader ["a"] (method ^:public close ^void [this] (.append sb "ca,")))
                         ^java.io.StringReader b (anon java.io.StringReader ["b"] (method ^:public close ^void [this] (.append sb "cb,") (throw (IllegalStateException. "close b"))))]
          (.append sb "body,")
          (throw (RuntimeException. "body")))
        (catch RuntimeException e
          (.append sb (java-str (.getMessage e) " suppressed " (alength (.getSuppressed e)) " " (.getMessage (aget (.getSuppressed e) 0))))))
      (.toString sb)))
  (method ^:public ^:static tLocking ^String []
    (let [o (Object.) ^:mutable ^int n 0]
      (locking o (set! n 1) (locking o (set! n (unchecked-add-int n 1))))
      (try (locking o (throw (IllegalStateException. "in lock"))) (catch IllegalStateException e (set! n (unchecked-add-int n 10))))
      (locking o (set! n (unchecked-add-int n 100)))
      (java-str n)))

  ;; ----- strings (§7.5)
  (method ^:public ^:static tStrings ^String []
    (let [^String nl nil ^Object o nil ^char c \x ^long l -5 ^double d 2.5 ^float f (float -0.0) ^boolean z true]
      (java-str "s" nl o c l d f z 1.0E-4 (Double/valueOf 100.0) (unchecked-char 233) "😀" FxColor/GREEN (Integer/valueOf 7))))
  (method ^:public ^:static tStringOps ^String []
    (let [s "Hello, World"]
      (java-str (.length s) (.charAt s 4) (.indexOf s "o") (.substring s 7) (.toLowerCase s) (.hashCode s)
                (.equals s "Hello, World") (identical? s "Hello, World") (.compareTo "a" "b") (.trim "  x ")
                (.replace s \l \L) (String/valueOf 3.0) (.isEmpty "") (.contains s "World"))))

  ;; ----- arrays (§5.9)
  (method ^:public ^:static tArrays ^String []
    (let [a (new int/2 3 4) b (new String/2 2) c (new long/1 [1 2 3]) d (new Object/1 2) e (.clone c)]
      (aset a 1 2 7)
      (aset b 0 (new String/1 ["p" "q"]))
      (aset e 0 99)
      (java-str (aget a 1 2) (alength a) (alength (aget a 0)) (aget b 0 1) (nil? (aget b 1)) (aget c 0) (aget e 0)
                (alength (new boolean/1 0)) (instance? Object/1 b) (instance? int/1 d))))
  (method ^:public ^:static tArrayErrors ^String []
    (let [sb (StringBuilder.) ^Object/1 a (new String/1 1)]
      (try (aset a 0 (Integer/valueOf 1)) (catch ArrayStoreException e (.append sb (java-str "store " (.getMessage e) ";"))))
      (try (aget a 3) (catch ArrayIndexOutOfBoundsException e (.append sb (java-str (.getMessage e) ";"))))
      (try (new int/1 (unchecked-subtract-int 0 (FxCode/next))) (catch NegativeArraySizeException e (.append sb "neg;")))
      (try (alength ^int/1 (FxCode/nothing)) (catch NullPointerException e (.append sb "npe;")))
      (.toString sb)))
  (method ^:public ^:static tCasts ^String []
    (let [^Object o "str" ^Object n (Integer/valueOf 3)]
      (java-str (.length (cast String o)) (instance? Comparable o) (instance? Number o) (cast String nil)
                (try (.length (cast String n)) (catch ClassCastException e -1)))))
  (method ^:public ^:static tVarargs ^String []
    (java-str (String/format "%s-%d-%s" "a" (Integer/valueOf 1) "z")
              (java.util.Arrays/asList (new Object/1 ["x" "y"]))))))
