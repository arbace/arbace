;; The class forms at the REPL (doc/go/CLASSFORMS-REPL.md): defclass and the code forms (SPEC
;; §9.5, §10) typed at the REPL, the classes then used from Clojure. On the JVM they are
;; compiled to bytecode; in the Go build they are classes made at run time, their code
;; interpreted. Arithmetic and conversions, control flow, strings, arrays, objects, statics and
;; their initialization, inheritance, interfaces, nested, local and anonymous classes, lambdas and
;; method references, switch and patterns, records, exceptions, Clojure in class bodies,
;; reflection from Clojure.

;; ----- static methods: arithmetic and conversions
(defclass ^:public Arith
  (method ^:public ^:static addI ^int [^int a ^int b] (unchecked-add-int a b))
  (method ^:public ^:static mulL ^long [^long a ^long b] (unchecked-multiply a b))
  (method ^:public ^:static divI ^int [^int a ^int b] (unchecked-divide-int a b))
  (method ^:public ^:static remI ^int [^int a ^int b] (unchecked-remainder-int a b))
  (method ^:public ^:static divL ^long [^long a ^long b] (unchecked-divide a b))
  (method ^:public ^:static mix ^double [^int a ^long b ^float c ^double d]
    (let [i (unchecked-add-int
              (unchecked-add-int
                (unchecked-subtract-int
                  (unchecked-remainder-int (unchecked-divide-int (unchecked-add-int (unchecked-multiply-int a 3) 7) 2) 5)
                  (bit-or-int (bit-and-int a 6) (bit-xor-int 1 a)))
                (bit-shift-left-int a 2))
              (unchecked-add-int (bit-shift-right-int a 1) (unsigned-bit-shift-right-int a 3)))
          l (unchecked-add (unchecked-subtract (unchecked-add (unchecked-multiply b b) (bit-and b 3)) (unchecked-divide b 3))
                           (unchecked-remainder b 7))
          f (unchecked-subtract-float
              (unchecked-add-float (unchecked-multiply-float c (float 2.5)) (unchecked-divide-float c (float 3)))
              (unchecked-remainder-float c (float 2)))]
      (unchecked-subtract
        (unchecked-add (unchecked-add (unchecked-add (unchecked-long i) l) (unchecked-double f))
                       (unchecked-divide (unchecked-multiply d d) 2.0))
        (unchecked-remainder d 3.0))))
  (method ^:public ^:static shifts ^String [^int a ^long b ^int n]
    (java-str (bit-shift-left-int a n) " " (bit-shift-right-int a n) " " (unsigned-bit-shift-right-int a n) " "
              (bit-shift-left b n) " " (bit-shift-right b n) " " (unsigned-bit-shift-right b n) " " (bit-not-int a)
              " " (bit-not b)))
  (method ^:public ^:static narrow ^String [^long l ^double d]
    (java-str (unchecked-int l) " " (unchecked-short l) " " (unchecked-byte l) " " (unchecked-int (unchecked-char l))
              " " (unchecked-int d) " " (unchecked-long d) " " (unchecked-float d) " " (unchecked-int (unchecked-float d))))
  (method ^:public ^:static chr ^char [^char c] (unchecked-char (unchecked-add-int c 1)))
  (method ^:public ^:static floats ^String [^float a ^float b]
    (java-str (unchecked-add-float a b) " " (unchecked-multiply-float a b) " " (unchecked-divide-float a b)
              " " (unchecked-remainder-float a b) " " (unchecked-negate-float a)))
  (method ^:public ^:static exact ^long [^long a ^long b] (+ a b))
  (method ^:public ^:static cmp ^boolean [^int a ^long b ^double d o]
    (or (and (< a b) (>= d 0.5) (some? o)) (and (nil? o) (not (== a 3)))))
  (method ^:public ^:static nan ^String [^double d]
    (java-str (< d 1.0) (> d 1.0) (== d d) (not (== d d)) " " (unchecked-int d) " " (unchecked-long d))))

(Arith/addI 2147483647 1)
(Arith/mulL 4294967296 4294967296)
(Arith/divI 7 -2)
(Arith/remI -7 2)
(Arith/divI 1 0)
(Arith/divL Long/MIN_VALUE -1)
(Arith/mix 13 1234567891234 (float 3.25) 2.75)
(Arith/mix -13 -98765 (float -1.5) -0.125)
(Arith/shifts -12345 -1234567890123 3)
(Arith/shifts 12345 1234567890123 35)
(Arith/narrow 4294967295 3.99)
(Arith/narrow -129 -1e20)
(Arith/narrow 65601 1e20)
(Arith/chr \a)
(class (Arith/chr \z))
(Arith/floats (float 1.1) (float 3.3))
(Arith/floats (float -7.5) (float 0.0))
(Arith/exact 1 2)
(Arith/exact Long/MAX_VALUE 1)
[(Arith/cmp 1 2 0.5 :x) (Arith/cmp 3 2 0.5 nil) (Arith/cmp 4 2 0.5 nil) (Arith/cmp 1 2 0.1 :x)]
(Arith/nan ##NaN)
(Arith/nan 2.5)
(Arith/nan ##-Inf)
(class (Arith/addI 1 2))
(class (Arith/mulL 1 2))

;; ----- control flow
(defclass ^:public Flow
  (method ^:public ^:static rowOf ^int [^int/2 grid ^int v]
    (label :outer
      (loop [^int i 0]
        (when (< i (alength grid))
          (loop [^int j 0]
            (when (< j (alength (aget grid i)))
              (when (< (aget grid i j) 0)
                (continue :outer (unchecked-inc-int i)))
              (when (== (aget grid i j) v)
                (return i))
              (recur (unchecked-inc-int j))))
          (recur (unchecked-inc-int i)))))
    -1)
  (method ^:public ^:static fact ^long [^:mutable ^int n]
    (let [^:mutable ^long acc 1]
      (while (> n 1)
        (set! acc (unchecked-multiply acc n))
        (set! n (unchecked-dec-int n)))
      acc))
  (method ^:public ^:static loops ^long [^int n]
    (let [^:mutable ^long s 0]
      (dotimes [i n] (set! s (unchecked-add s i)))
      (loop [^int i 0]
        (when (< i n)
          (when (== i 3) (break))
          (set! s (unchecked-add s 100))
          (recur (unchecked-inc-int i))))
      s))
  (method ^:public ^:static labeled ^int [^int x]
    (unchecked-add-int 1 (label :L (when (> x 0) (break :L (unchecked-multiply-int x 10))) 5)))
  (method ^:public ^:static fin ^String [^StringBuilder sb ^int n]
    (loop [^int i 0]
      (when (< i n)
        (try
          (when (== i 1) (continue (unchecked-inc-int i)))
          (when (== i 3) (return (.toString sb)))
          (.append sb i)
          (finally (.append sb "f")))
        (recur (unchecked-inc-int i))))
    (.toString sb))
  (method ^:public ^:static tryExpr ^int [^String s]
    (unchecked-add-int 1 (try (Integer/parseInt s) (catch NumberFormatException e -1))))
  (method ^:public ^:static nested ^String [^int k]
    (let [sb (StringBuilder.)]
      (try
        (try
          (.append sb "a")
          (when (== k 1) (throw (IllegalStateException. "one")))
          (when (== k 2) (return (.toString (.append sb "r"))))
          (.append sb "b")
          (catch IllegalStateException e (.append sb (.getMessage e)))
          (finally (.append sb "F1")))
        (when (== k 3) (throw (UnsupportedOperationException. "three")))
        (catch [ArithmeticException UnsupportedOperationException] e (.append sb (java-str "<" (.getMessage e) ">")))
        (finally (.append sb "F2")))
      (.toString sb)))
  (method ^:public ^:static finRet ^int [] (try (return 1) (finally (return 2))))
  (method ^:public ^:static truth ^String [o] (if o "yes" "no"))
  (method ^:public ^:static sync ^int [^Object lock ^int/1 a]
    (locking lock (let [x (aget a 0)] (aset a 0 (unchecked-inc-int x)) x)))
  (method ^:public ^:static spill ^String [^int n]
    (java-str "n=" n ", sum="
              (loop [^int i 0 ^int acc 0]
                (if (< i n) (recur (unchecked-inc-int i) (unchecked-add-int acc i)) acc))
              ", t=" (try (if (> n 3) (throw (RuntimeException.)) "ok") (catch RuntimeException e "caught")))))

(Flow/rowOf (into-array (map int-array [[1 2 3] [-1 7] [4 7 6]])) 7)
(Flow/rowOf (into-array (map int-array [[1 2 3] [4 5 6]])) 9)
(Flow/fact 20)
(Flow/loops 10)
[(Flow/labeled 3) (Flow/labeled -3)]
[(Flow/fin (StringBuilder.) 3) (Flow/fin (StringBuilder.) 5)]
[(Flow/tryExpr "42") (Flow/tryExpr "x")]
(map #(Flow/nested %) [0 1 2 3])
(Flow/finRet)
(map #(Flow/truth %) [1 nil false true 0 ""])
(let [a (int-array [41])] [(Flow/sync (Object.) a) (vec a)])
[(Flow/spill (int 3)) (Flow/spill (int 5))]

;; ----- strings and arrays
(defclass ^:public Arrays2
  (method ^:public ^:static str ^String [^int i ^long l ^char c ^boolean z ^float f ^double d ^String s o]
    (java-str "i" i "l" l "c" c "z" z "f" f "d" d "s" s "o" o "n" nil))
  (method ^:public ^:static sum ^long [^int/1 xs]
    (let [^:mutable ^long s 0]
      (for-each [^int x xs] (set! s (unchecked-add s x)))
      s))
  (method ^:public ^:static grid ^String []
    (let [g (new int/2 3 4)]
      (dotimes [i 3] (dotimes [j 4] (aset g (unchecked-int i) (unchecked-int j) (unchecked-multiply-int (unchecked-int i) (unchecked-int j)))))
      (java-str (java.util.Arrays/deepToString g) " " (alength g) " " (alength (aget g 0)))))
  (method ^:public ^:static jagged ^String []
    (let [g (new long/2 2)]
      (aset g 0 (new long/1 [1 2 3]))
      (aset g 1 (new long/1 0))
      (java.util.Arrays/deepToString g)))
  (method ^:public ^:static kinds ^String []
    (let [bs (new byte/1 [1 -2 127]) ss (new short/1 [300 -300]) cs (new char/1 [\x \y])
          zs (new boolean/1 [true false]) fs (new float/1 [(float 1.5) (float 2.5)]) ds (new double/1 [0.1 0.2])
          os (new String/1 ["a" nil "c"])]
      (aset bs 0 (unchecked-byte 200))
      (aset cs 1 \z)
      (aset zs 1 true)
      (java-str (java.util.Arrays/toString bs) (java.util.Arrays/toString ss) (java.util.Arrays/toString cs)
                (java.util.Arrays/toString zs) (java.util.Arrays/toString fs) (java.util.Arrays/toString ds)
                (java.util.Arrays/toString os))))
  (method ^:public ^:static cloned ^String [^int/1 xs]
    (let [ys (.clone xs)]
      (aset ys 0 99)
      (java-str (java.util.Arrays/toString xs) (java.util.Arrays/toString ys))))
  (method ^:public ^:static oob ^int [^int/1 xs ^int i] (aget xs i))
  (method ^:public ^:static neg ^int [^int n] (alength (new int/1 n)))
  (method ^:public ^:static iter ^String [^java.util.List l]
    (let [sb (StringBuilder.)]
      (for-each [^String s l] (.append sb (.toUpperCase s)))
      (.toString sb)))
  (method ^:public ^:static breakEach ^int [^int/1 xs]
    (unchecked-add-int (aget xs 0)
                       (label :out
                         (for-each [^int x xs] (when (< x 0) (break :out x)))
                         0))))

(Arrays2/str 1 2 \c true (float 1.5) 2.25 "s" :kw)
(Arrays2/str -1 Long/MIN_VALUE \u00e9 false (float 1e10) 1e-5 nil [1 2])
(Arrays2/sum (int-array [1 2 3 4]))
(Arrays2/grid)
(Arrays2/jagged)
(Arrays2/kinds)
(Arrays2/cloned (int-array [1 2 3]))
(Arrays2/oob (int-array [1 2 3]) 3)
(Arrays2/oob nil 0)
(Arrays2/neg -1)
(Arrays2/iter ["a" "bc" "d"])
[(Arrays2/breakEach (int-array [1 -3 2])) (Arrays2/breakEach (int-array [4 3]))]

;; ----- objects: fields, constructors, initializers, statics
(defclass ^:public Counter
  (field ^:private ^int n)
  (field ^:public ^String label "c")
  (field ^:public ^:static ^int created 0)
  (field ^:public ^:static ^:final ^int LIMIT 100)
  (field ^:public ^:static ^:final ^String NAME "counter")
  (constructor ^:public [this] (this. 0))
  (constructor ^:public [this ^int start]
    (set! n start)
    (set! created (unchecked-inc-int created)))
  (method ^:public inc ^int [this] (set! n (unchecked-inc-int n)) n)
  (method ^:public add ^Counter [this ^int k] (set! n (unchecked-add-int n k)) this)
  (method ^:public get ^int [this] n)
  (method ^:public toString ^String [this] (java-str label "=" n)))

(def c (Counter.))
[(.inc c) (.inc c) (.get c)]
(.get (.add (Counter. 10) 5))
(str c)
(.-label c)
(set! (.-label c) "d")
(str c)
Counter/created
Counter/LIMIT
Counter/NAME
(class c)
(instance? Counter c)
(.getName Counter)
(.getSimpleName Counter)
(let [c2 (Counter. 3)] [(.inc c2) Counter/created])
(map #(.getName %) (sort-by #(.getName %) (.getDeclaredConstructors Counter)))

(defclass ^:public Init
  (field ^:public ^:static ^StringBuilder LOG (StringBuilder.))
  (field ^:public ^:static ^int A (do (.append LOG "A") 1))
  (static-initializer (.append LOG "S"))
  (field ^:public ^int x (do (.append LOG "x") 5))
  (initializer (.append LOG "i"))
  (constructor ^:public [this] (.append LOG "C"))
  (method ^:public ^:static log ^String [] (.toString LOG)))

(Init/log)
(.-x (Init.))
(Init/log)

(defclass ^:public Boom
  (field ^:public ^:static ^int V (Integer/parseInt "nope")))
(try Boom/V (catch Throwable e [(class e) (class (ex-cause e))]))
(try Boom/V (catch Throwable e (class e)))

;; ----- inheritance and interfaces
(defclass ^:public ^:interface Shape
  (method ^:public ^:abstract area ^double [this])
  (method ^:public describe ^String [this] (java-str (.name this) " of area " (.area this)))
  (method ^:public name ^String [this] "shape")
  (method ^:public ^:static unit ^double [] 1.0))

(defclass ^:public ^:abstract Base
  :implements [Shape Comparable]
  (field ^:protected ^double scale 1.0)
  (method ^:public compareTo ^int [this o] (Double/compare (.area this) (.area ^Shape o)))
  (method ^:public name ^String [this] "base")
  (method ^:public toString ^String [this] (.describe this)))

(defclass ^:public Square
  :extends Base
  (field ^:private ^double side)
  (constructor ^:public [this ^double side] (super.) (set! (.-side this) side))
  (method ^:public area ^double [this] (unchecked-multiply side side))
  (method ^:public name ^String [this] (java-str "square/" (.name super))))

(defclass ^:public Circle
  :extends Base
  (field ^:private ^double r)
  (constructor ^:public [this ^double r] (set! (.-r this) r) (set! (.-scale this) 2.0))
  (method ^:public area ^double [this] (unchecked-multiply 3.0 (unchecked-multiply r r)))
  (method ^:public ^:static of ^Circle [^double r] (Circle. r)))

(def shapes [(Square. 2.0) (Circle/of 1.0) (Square. 1.5)])
(map str shapes)
(map #(.area %) shapes)
(map #(.area ^Shape %) shapes)
(sort shapes)
(map #(.name %) (sort shapes))
(Shape/unit)
[(instance? Shape (Square. 1.0)) (instance? Base (Circle. 1.0)) (instance? Comparable (Circle. 1.0)) (instance? Square (Circle. 1.0))]
(.getName (.getSuperclass Square))
(map #(.getName %) (.getInterfaces Base))
(compare (Square. 3.0) (Circle. 1.0))
(.compareTo (Square. 1.0) (Square. 1.0))
(try (Base.) (catch Throwable e (class e)))

;; ----- the world's interfaces: called from Clojure and from the runtime
(defclass ^:public Range3
  :implements [Iterable arbace.lang.Counted arbace.lang.ILookup]
  (field ^:private ^int n)
  (constructor ^:public [this ^int n] (set! (.-n this) n))
  (method ^:public count ^int [this] n)
  (method ^:public valAt [this k] (.valAt this k nil))
  (method ^:public valAt [this k nf] (if (and (instance? Long k) (< (.longValue ^Long k) n)) (unchecked-multiply 10 (.longValue ^Long k)) nf))
  (method ^:public iterator ^java.util.Iterator [this]
    (let [i (new int/1 1)]
      (anon java.util.Iterator []
        (method ^:public hasNext ^boolean [it] (< (aget i 0) n))
        (method ^:public next [it] (let [v (aget i 0)] (aset i 0 (unchecked-inc-int v)) (Integer/valueOf v)))))))

(def r3 (Range3. 3))
(count r3)
(get r3 2)
(get r3 7 :none)
(:x r3 :nf)
(seq r3)
(vec r3)
(reduce + r3)
(into [] (map inc) r3)

;; ----- nested, inner, local and anonymous classes
(defclass ^:public Outer
  (field ^:private ^int v)
  (constructor ^:public [this ^int v] (set! (.-v this) v))
  (defclass ^:public Inner
    (field ^:private ^int w)
    (constructor ^:public [this ^int w] (set! (.-w this) w))
    (method ^:public sum ^int [this] (unchecked-add-int (.-v Outer/this) w)))
  (defclass ^:public ^:static Nested
    (method ^:public ^:static twice ^int [^int x] (unchecked-multiply-int 2 x)))
  (method ^:public inner ^Outer$Inner [this ^int w] (.new this Inner w))
  (method ^:public local ^String [this ^int k]
    (letclass [(Adder (method ^:public add ^int [a ^int x] (unchecked-add-int x (unchecked-add-int k v))))]
      (java-str (.add (Adder.) 1) " " (.add (Adder.) 2))))
  (method ^:public anonymous ^Runnable [this ^StringBuilder sb]
    (anon Runnable []
      (method ^:public run ^void [r] (.append sb (java-str "ran " v))))))

(.sum (.inner (Outer. 10) 5))
(user.Outer$Nested/twice 21)
(.local (Outer. 100) 7)
(let [sb (StringBuilder.)] (.run (.anonymous (Outer. 3) sb)) (str sb))
(let [sb (StringBuilder.) t (Thread. (.anonymous (Outer. 4) sb))] (.start t) (.join t) (str sb))
(.getName (class (.inner (Outer. 1) 2)))

;; ----- lambdas and method references
(defclass ^:public Funs
  (method ^:public ^:static adder ^java.util.function.IntUnaryOperator [^int k]
    (lambda java.util.function.IntUnaryOperator [^int x] (unchecked-add-int x k)))
  (method ^:public ^:static upper ^java.util.function.Function []
    (method-ref java.util.function.Function [String] String/.toUpperCase))
  (method ^:public ^:static parse ^java.util.function.ToIntFunction []
    (method-ref java.util.function.ToIntFunction [String] Integer/parseInt))
  (method ^:public ^:static maker ^java.util.function.Supplier []
    (method-ref java.util.function.Supplier StringBuilder/new))
  (method ^:public ^:static sorted ^java.util.List [^java.util.List l]
    (let [a (java.util.ArrayList. l)]
      (.sort a (lambda java.util.Comparator [x y] (unchecked-subtract-int (.length ^String y) (.length ^String x))))
      a))
  (method ^:public ^:static counter ^java.util.function.Supplier []
    (let [a (new int/1 1)]
      (lambda java.util.function.Supplier [] (aset a 0 (unchecked-inc-int (aget a 0))) (Integer/valueOf (aget a 0))))))

(.applyAsInt (Funs/adder 10) 5)
(.apply (Funs/upper) "abc")
(.applyAsInt (Funs/parse) "123")
(str (.append ^StringBuilder (.get (Funs/maker)) "x"))
(Funs/sorted ["a" "ccc" "bb"])
(let [s (Funs/counter)] [(.get s) (.get s) (.get s)])
(instance? java.util.function.Supplier (Funs/counter))

;; ----- switch and patterns, records
(defclass ^:public ^:record Point [^int x ^int y]
  (method ^:public dist ^int [this] (unchecked-add-int (Math/abs x) (Math/abs y))))

(def p (Point. 3 -4))
[(.x p) (.y p) (.dist p)]
(str p)
(= p (Point. 3 -4))
(= p (Point. 3 4))
(= (hash p) (hash (Point. 3 -4)))
(.hashCode p)

(defclass ^:public Sw
  (method ^:public ^:static day ^String [^int d] (switch d 0 "Sun" (1 2 3 4 5) "work" 6 "Sat" "?"))
  (method ^:public ^:static str ^int [^String s] (switch s "a" 1 "b" 2 ("c" "d") 3 0))
  (method ^:public ^:static pat ^String [o]
    (switch o
      [^String s] (java-str "string " (.length s))
      [(Point x y) :when (== x y)] "diagonal"
      [(Point x y)] (java-str "point " x "," y)
      [^Integer i :when (> (.intValue i) 10)] "big"
      [^Integer i] "small"
      nil "null"
      "other"))
  (method ^:public ^:static inst ^int [o]
    (if-instance [^String s o] (.length s) -1))
  (method ^:public ^:static rec ^int [o]
    (if-instance [(Point a b) o] (unchecked-multiply-int a b) 0)))

(map #(Sw/day %) [0 3 6 9])
(map #(Sw/str %) ["a" "b" "c" "d" "e"])
(try (Sw/str nil) (catch Throwable e (class e)))
(map #(Sw/pat %) ["abc" (Point. 2 2) (Point. 1 2) (int 50) (int 5) nil 1.5])
[(Sw/inst "hello") (Sw/inst 1) (Sw/rec (Point. 6 7)) (Sw/rec "x")]

;; ----- exceptions, casts
(defclass ^:public Ex
  (method ^:public ^:static npe ^int [^String s] (.length s))
  (method ^:public ^:static cce ^String [o] (cast String o))
  (method ^:public ^:static thrower ^void [^int k]
    (switch k
      0 (throw (IllegalArgumentException. "bad"))
      1 (throw (arbace.lang.ExceptionInfo. "info" {:k 1}))
      nil))
  (method ^:public ^:static deep ^long [^long n] (if (== n 0) 0 (unchecked-inc (Ex/deep (unchecked-dec n))))))

(try (Ex/npe nil) (catch NullPointerException e :npe))
(try (Ex/cce 1) (catch ClassCastException e :cce))
(try (Ex/thrower 0) (catch IllegalArgumentException e (ex-message e)))
(try (Ex/thrower 1) (catch Exception e [(ex-message e) (ex-data e)]))
(Ex/thrower 2)
(Ex/deep 1000)

;; ----- Clojure inside class bodies
(def ^:dynamic *dyn* 1)
(defn twice [x] (* 2 x))
(defclass ^:public Clj
  (method ^:public ^:static data [^int n]
    {:n n :kw :foo :sym 'bar :vec [1 "two" n] :set #{:a} :twice (twice n) :quoted '(1 (2 3) {:x [y]})})
  (method ^:public ^:static mapped [xs] (vec (map (fn [x] (* 2 x)) xs)))
  (method ^:public ^:static adder [n] (fn [x] (+ x n)))
  (method ^:public ^:static named [n] ((fn f [i] (if (zero? i) :done (f (dec i)))) n))
  (method ^:public ^:static variadic [] ((fn [a & more] [a more]) 1 2 3))
  (method ^:public ^:static pick [x] (case x :a 1 (:b :c) 2 "s" 3 0))
  (method ^:public ^:static evens [n]
    (letfn [(e? [n] (if (zero? n) true (o? (dec n))))
            (o? [n] (if (zero? n) false (e? (dec n))))]
      (e? n)))
  (method ^:public ^:static objs [a b] [(= a b) (+ a b) (not a) (str a b)])
  (method ^:public ^:static theVar [] (var *dyn*))
  (method ^:public ^:static dyn [] *dyn*)
  (method ^:public ^:static ratio [] [1/3 2.5M 'sym (/ 1 3)]))

(Clj/data 3)
(Clj/mapped [1 2 3])
((Clj/adder 5) 10)
(Clj/named 3)
(Clj/variadic)
(map #(Clj/pick %) [:a :b :c "s" 'x])
[(Clj/evens 10) (Clj/evens 7)]
(Clj/objs 1 2)
(Clj/theVar)
(binding [*dyn* 42] (Clj/dyn))
(Clj/ratio)

;; ----- enums
(defclass ^:public ^:enum Color (constants RED GREEN BLUE)
  (method ^:public next ^Color [this] (aget (Color/values) (unchecked-remainder-int (unchecked-inc-int (.ordinal this)) 3))))
(Color/values)
(vec (Color/values))
[(.name Color/GREEN) (.ordinal Color/GREEN) (str Color/BLUE)]
(Color/valueOf "BLUE")
(try (Color/valueOf "PURPLE") (catch IllegalArgumentException e (ex-message e)))
(.next Color/BLUE)
(compare Color/RED Color/BLUE)
(Enum/valueOf Color "RED")
[(instance? Enum Color/RED) (.isEnum Color) (= Color (.getDeclaringClass Color/RED))]
(seq (.getEnumConstants Color))
(sort [Color/BLUE Color/RED Color/GREEN])

(defclass ^:public ^:enum Op
  (constants (PLUS ["+"] (method ^:public apply ^int [this ^int a ^int b] (unchecked-add-int a b)))
             (TIMES ["*"] (method ^:public apply ^int [this ^int a ^int b] (unchecked-multiply-int a b))))
  (field ^:private ^:final ^String sym)
  (constructor [this ^String sym] (set! (.-sym this) sym))
  (method ^:public ^:abstract apply ^int [this ^int a ^int b])
  (method ^:public sym ^String [this] sym)
  (method ^:public ^:static eval ^String [^Op op ^int a ^int b]
    (java-str a " " (.sym op) " " b " = " (.apply op a b) " (" (switch op PLUS "plus" TIMES "times") ")")))
(map #(Op/eval % 6 7) (Op/values))
(.apply Op/TIMES 3 4)
[(.name Op/PLUS) (.ordinal Op/TIMES) (= Op (.getDeclaringClass Op/TIMES))]

;; ----- exceptions of class forms
(defclass ^:public AppException
  :extends Exception
  (field ^:private ^int code)
  (constructor ^:public [this ^String msg ^int code] (super. msg) (set! (.-code this) code))
  (method ^:public code ^int [this] code)
  (method ^:public getMessage ^String [this] (java-str "[" code "] " (.getMessage super))))
(defclass ^:public NotFound
  :extends AppException
  (constructor ^:public [this ^String what] (super. (java-str what " not found") 404)))
(defclass ^:public Thrower
  (method ^:public ^:static find ^String [^String k]
    (if (.equals k "x") "found" (throw (NotFound. k))))
  (method ^:public ^:static safe ^String [^String k]
    (try (Thrower/find k)
      (catch NotFound e (java-str "nf:" (.code e)))
      (catch AppException e "app")))
  (method ^:public ^:static wrap ^String [^String k]
    (try (Thrower/find k) (catch Exception e (.getMessage e)))))
[(Thrower/safe "x") (Thrower/safe "y") (Thrower/wrap "z")]
(try (Thrower/find "q") (catch AppException e [(ex-message e) (.code e) (class e)]))
(try (throw (NotFound. "w")) (catch Exception e [(instance? AppException e) (instance? Exception e) (instance? RuntimeException e)]))
(.getMessage (AppException. "m" 7))
(str (AppException. "m" 7))
(.getSuperclass NotFound)

(defclass ^:public Unchecked
  :extends IllegalStateException
  (constructor ^:public [this ^String m] (super. m)))
(try (throw (Unchecked. "u")) (catch IllegalStateException e [(ex-message e) (class e)]))

;; ----- a class of class forms as a Clojure fn: extending AFn
(defclass ^:public Adder
  :extends arbace.lang.AFn
  (field ^:private ^long k)
  (constructor ^:public [this ^long k] (set! (.-k this) k))
  (method ^:public invoke [this x] (+ x k))
  (method ^:public invoke [this x y] (+ x y k)))
((Adder. 10) 5)
((Adder. 10) 5 6)
(map (Adder. 1) [1 2 3])
(apply (Adder. 100) [1 2])
(try ((Adder. 1)) (catch Exception e (class e)))

;; ----- code forms in fns and deftypes (handed over to the class forms compiler, SPEC §9.5)
(defn cf-square [n] (let [^int x (unchecked-int n)] (unchecked-multiply-int x x)))
(cf-square 7)
(cf-square 100000)
(defn cf-sum [n] (let [^:mutable s 0] (dotimes [i n] (set! s (+ s i))) s))
(cf-sum 10)
(defn cf-first-neg [xs] (for-each [x (cast Iterable xs)] (when (neg? x) (return x))) :none)
[(cf-first-neg [1 -2 3]) (cf-first-neg [1 2])]
(let [k 10] (defn cf-adder [x] (let [^long y x] (unchecked-add y k))))
(cf-adder 5)
(map (fn [x] (let [^:mutable acc 1] (dotimes [_ x] (set! acc (* acc 2))) acc)) [0 1 5])
(class (fn [] (let [^int a 1] a)))
(deftype CFCounter [^:unsynchronized-mutable n]
  arbace.lang.IDeref
  (deref [this] (let [^int k 1] (set! n (+ n k)) n)))
(def cfc (CFCounter. 1))
[(deref cfc) (deref cfc) @cfc]
(defrecord CFRec [a b] arbace.lang.IDeref (deref [this] (let [^int c 2] (unchecked-add-int c 1))))
[(deref (->CFRec 1 2)) (:a (->CFRec 1 2)) (into {} (->CFRec 1 2))]

;; ----- reflection on the classes
(map #(.getName %) (sort-by #(.getName %) (.getDeclaredMethods Counter)))
(.getName (.getDeclaringClass user.Outer$Inner))
(java.lang.reflect.Modifier/toString (.getModifiers Base))
(.isInterface Shape)
(.invoke (.getMethod Counter "inc" (into-array Class [])) (Counter. 41) (object-array 0))

;; ----- redefinition at the REPL
(defclass ^:public Again (method ^:public ^:static v ^int [] 1))
(def again-1 Again)
(Again/v)
(defclass ^:public Again (method ^:public ^:static v ^int [] 2))
(Again/v)
(identical? again-1 Again)
