;; c2g's fixtures (see FxCode.clj): classes and objects. Initialization order, inheritance,
;; interfaces with default methods, inner, anonymous and local classes, records, enums with
;; bodies, lambdas and method references, volatile fields, and C2G-SPEC §15.4 and §15.5's
;; worked examples (Delay and LazySeq over an anonymous AFn; a lambda and an enum switch).

(in-ns 'c2g.fixtures)

(import '(java.util ArrayList Comparator)
        '(java.util.function Function Supplier BiFunction)
        '(arbace.lang AFn Delay LazySeq RT ISeq))

(do

(defclass ^:public FxLog
  (field ^:static ^StringBuilder log (StringBuilder.))
  (method ^:public ^:static add ^int [^String s] (.append log s) (.append log ",") 0)
  (method ^:public ^:static take ^String [] (let [s (.toString log)] (.setLength log 0) s)))

(defclass ^:public FxInitA
  (field ^:public ^:static ^int a (FxLog/add "A.a"))
  (static-initializer (FxLog/add "A.static"))
  (field ^:public ^:static ^:final ^int K 42)
  (field ^:public ^:static ^:final ^String S "const")
  (method ^:public ^:static touch ^int [] (FxLog/add "A.touch")))

(defclass ^:public FxInitB :extends FxInitA
  (field ^:public ^:static ^int b (FxLog/add "B.b"))
  (static-initializer (FxLog/add "B.static"))
  (method ^:public ^:static touch2 ^int [] (FxLog/add "B.touch2")))

(defclass ^:public ^:interface FxShape
  (method area ^double [this])
  (method ^:default describe ^String [this] (java-str (.name this) " of area " (.area this)))
  (method ^:default name ^String [this] "shape")
  (method ^:static unit ^FxShape [] (FxSquare. 1.0)))

(defclass ^:public ^:abstract FxBase :implements [FxShape]
  (field ^String tag)
  (field ^int initSeen)
  (constructor [this ^String tag] (set! (.-tag this) tag) (set! initSeen (.seen this)))
  (method seen ^int [this] -1)
  (method ^:public name ^String [this] (java-str "base:" tag))
  (method ^:public toString ^String [this] (java-str "<" (.name this) " " initSeen ">")))

(defclass ^:public FxSquare :extends FxBase
  (field ^double side)
  (field ^int marker 7)
  (constructor [this ^double side] (super. "square") (set! (.-side this) side))
  (method seen ^int [this] marker)
  (method ^:public area ^double [this] (unchecked-multiply side side))
  (method ^:public name ^String [this] (java-str "sq/" (.name super))))

(defclass ^:public FxCircle :extends FxBase
  (field ^double r)
  (constructor [this ^double r] (super. "circle") (set! (.-r this) r))
  (method ^:public area ^double [this] (unchecked-multiply 3.0 (unchecked-multiply r r)))
  (method ^:public describe ^String [this] (java-str "round " (.describe ^FxShape (FxSquare. r)))))

(defclass ^:public ^:record FxRange [^int lo ^int hi]
  (constructor ^:public ^:compact [this]
    (when (> lo hi) (throw (IllegalArgumentException. (java-str "bad range " lo ">" hi)))))
  (method ^:public size ^int [this] (unchecked-subtract-int hi lo)))

(defclass ^:public ^:record FxPair [^Object a ^String b])

;; a record whose declared accessor throws: a record pattern wraps it in MatchException
(defclass ^:public ^:record FxTouchy [^int v]
  (method ^:public v ^int [this]
    (when (neg? v) (throw (IllegalStateException. "negative")))
    v))

(defclass ^:public ^:enum FxOp
  (constants (PLUS ["+"] (method ^:public apply ^int [this ^int x ^int y] (unchecked-add-int x y)))
             (TIMES ["*"] (method ^:public apply ^int [this ^int x ^int y] (unchecked-multiply-int x y)))
             (NEG ["-"]))
  (field ^:private ^:final ^String sym)
  (constructor [this ^String sym] (set! (.-sym this) sym))
  (method ^:public apply ^int [this ^int x ^int y] (unchecked-negate-int x))
  (method ^:public sym ^String [this] sym))

(defclass ^:public ^:enum FxKind (constants CTOR INSTANCE STATIC))

;; the closed world (jrt's closure) has no java.util.function.Predicate: a fixture's own
(defclass ^:public ^:interface FxPredicate
  (method test ^boolean [this ^Object x])
  (method ^:default negate ^FxPredicate [this] (lambda FxPredicate [x] (not (.test this x))))
  (method ^:default and ^FxPredicate [this ^FxPredicate o] (lambda FxPredicate [x] (and (.test this x) (.test o x)))))

(defclass ^:public ^:interface FxIntOp
  (method applyAsInt ^int [this ^int x]))

(defclass ^:public FxOuter
  (field ^int f 5)
  (field ^:volatile ^int vol 0)
  (field ^:volatile ^Object vref)
  (field ^:volatile ^long vlong 0)
  (defclass ^:public Inner
    (field ^int g 1)
    (method ^:public get ^int [this] (unchecked-add-int f g)))
  (defclass ^:public ^:static Nested
    (method ^:public get ^int [this] 42))
  (method ^:public counter ^Runnable [this ^int/1 box]
    (anon Runnable []
      (method ^:public run ^void [r] (aset box 0 (unchecked-add-int (aget box 0) f)))))
  (method ^:public ^:static ofStatic ^Runnable [^int/1 box ^int k]
    (letclass [(Adder :implements [Runnable]
                 (method ^:public run ^void [r] (aset box 0 (unchecked-add-int (aget box 0) k))))]
      (anon Runnable []
        (method ^:public run ^void [r] (.run (Adder.)) (.run (Adder.))))))
  (method ^:public volatiles ^String [this]
    (set! vol (unchecked-add-int vol 3))
    (set! vref "v")
    (set! vlong (unchecked-add vlong 10))
    (java-str vol vref vlong))

  ;; ----- tests
  (method ^:public ^:static tInitOrder ^String []
    (FxLog/take)
    (let [k FxInitB/K s FxInitB/S]
      (FxLog/add (java-str "consts " k s))
      (FxInitB/touch2)
      (FxInitB/touch)
      (FxLog/add (java-str "b=" FxInitB/b))
      (FxLog/take)))
  (method ^:public ^:static tInherit ^String []
    (let [sq (FxSquare. 2.0) c (FxCircle. 1.0) ^FxShape u (FxShape/unit)]
      (java-str (.describe sq) " | " (.describe c) " | " sq " | " c " | " (.area u) " " (instance? FxBase u)
                " " (.name ^FxShape c))))
  (method ^:public ^:static tNested ^String []
    (let [o (FxOuter.) box (new int/1 [1])]
      (.run (.counter o box))
      (.run (FxOuter/ofStatic box 10))
      (java-str (.get (.new o Inner)) " " (.get (FxOuter$Nested.)) " " (aget box 0) " " (.volatiles o) " " (.volatiles o))))
  (method ^:public ^:static tRecords ^String []
    (let [r (FxRange. 1 4) p (FxPair. nil "b") q (FxPair. nil "b")]
      (java-str r " " (.size r) " " (.lo r) " " (.equals r (FxRange. 1 4)) " " (.equals r (FxRange. 1 5))
                " " (== (.hashCode r) (.hashCode (FxRange. 1 4))) " " p " " (.equals p q) " " (== (.hashCode p) (.hashCode q))
                " " (try (FxRange. 5 1) "no" (catch IllegalArgumentException e (.getMessage e))))))
  (method ^:public ^:static tRecordPattern ^String []
    (let [^Object o (FxRange. 2 9)]
      (java-str (switch o [(FxRange ^int lo hi)] (unchecked-add-int lo hi) [^Object x] -1)
                (if-instance [(FxPair a ^String b) o] b "-"))))
  (method ^:static classify ^String [^Object o]
    (switch o
      [(FxRange ^int lo hi) :when (> lo 5)] "big range"
      [(FxRange ^int lo hi) :when (== lo hi)] "empty range"
      [(FxRange ^int lo hi)] (java-str "range " (unchecked-subtract-int hi lo))
      [^String s :when (.isEmpty s)] "empty string"
      [^String s :when (> (.length s) 3)] (java-str "long " s)
      [^String s] (java-str "short " s)
      [(FxTouchy ^int v) :when (> v 10)] "touchy big"
      [(FxTouchy ^int v)] (java-str "touchy " v)
      nil "null"
      [^Object x] "other"))
  (method ^:public ^:static tPatternGuards ^String []
    (java-str (FxOuter/classify (FxRange. 7 9)) "|" (FxOuter/classify (FxRange. 3 3)) "|" (FxOuter/classify (FxRange. 1 4))
              "|" (FxOuter/classify "") "|" (FxOuter/classify "abcd") "|" (FxOuter/classify "ab")
              "|" (FxOuter/classify (FxTouchy. 11)) "|" (FxOuter/classify (FxTouchy. 2)) "|" (FxOuter/classify nil)
              "|" (FxOuter/classify (Integer/valueOf 1))))
  (method ^:public ^:static tMatchException ^String []
    (try (FxOuter/classify (FxTouchy. -1))
         (catch MatchException e
           (java-str (.getName (.getClass e)) ": " (.getMessage e) " / " (.getName (.getClass (.getCause e)))))))
  (method ^:public ^:static tEnumBodies ^String []
    (let [sb (StringBuilder.)]
      (for-each [^FxOp op (FxOp/values)]
        (.append sb (java-str (.sym op) (.apply op 6 7) " " (.name op) " " (identical? (.getDeclaringClass op) FxOp) ";")))
      (.toString sb)))
  (method ^:public ^:static tLambdas ^String []
    (let [^int k 3
          add (lambda FxIntOp [^int x] (unchecked-add-int x k))
          up (method-ref Function ^String [String] String/.toUpperCase)
          val (method-ref Function ^Integer [String] Integer/valueOf)
          sb (StringBuilder. "sb")
          bound (method-ref Supplier ^String [] sb StringBuilder/.toString)
          mk (method-ref Supplier ^ArrayList [] ArrayList/new)
          two (lambda BiFunction ^String [^String a ^String b] (java-str b a))
          p (lambda FxPredicate ^boolean [^String s] (.isEmpty s))
          np (.negate p)
          both (.and np (lambda FxPredicate ^boolean [^String s] (> (.length s) 2)))
          l (ArrayList.)]
      (.add l "pear") (.add l "fig") (.add l "apple")
      (.sort l (method-ref Comparator [String String] String/.compareTo))
      (java-str (.applyAsInt add 4) " " (.apply up "abc") " " (.intValue ^Integer (.apply val "12")) " " (.get bound)
                " " (.size ^ArrayList (.get mk)) " " (.apply two "x" "y") " " (.test p "") (.test np "") (.test both "abcd") (.test both "ab")
                " " l " " (.apply (.andThen up (method-ref Function ^Integer [String] String/.length)) "four"))))
  (method ^:static kindFilter ^FxPredicate [^String methodName ^FxKind kind]
    (if (some? methodName)
      (lambda FxPredicate ^boolean [^String m] (.equals m methodName))
      (lambda FxPredicate ^boolean [^String m]
        (switch kind
          STATIC (.startsWith m "s")
          INSTANCE (.startsWith m "i")
          false))))
  (method ^:public ^:static tLambdaEnum ^String []
    (java-str (.test (FxOuter/kindFilter "x" FxKind/CTOR) "x") (.test (FxOuter/kindFilter nil FxKind/STATIC) "sm")
              (.test (FxOuter/kindFilter nil FxKind/INSTANCE) "sm") (.test (FxOuter/kindFilter nil FxKind/CTOR) "im")))
  (method ^:public ^:static tDelay ^String []
    (let [box (new int/1 1)
          d (Delay. (anon AFn [] (method ^:public invoke ^Object [this] (aset box 0 (unchecked-add-int (aget box 0) 1)) "computed")))
          bad (Delay. (anon AFn [] (method ^:public invoke ^Object [this] (throw (IllegalStateException. "boom")))))]
      (java-str (.isRealized d) " " (.deref d) " " (.deref d) " " (aget box 0) " " (.isRealized d) " "
                (try (.deref bad) (catch IllegalStateException e (.getMessage e))) " "
                (try (.deref bad) (catch IllegalStateException e (java-str "again " (.getMessage e)))))))
  (method ^:public ^:static tLazySeq ^String []
    (let [box (new int/1 1)
          s (LazySeq. (anon AFn [] (method ^:public invoke ^Object [this]
                                     (aset box 0 (unchecked-add-int (aget box 0) 1))
                                     (RT/list "a" "b" "c"))))
          e (LazySeq. (anon AFn [] (method ^:public invoke ^Object [this] nil)))]
      (java-str (.isRealized s) " " (.count s) " " (.first s) " " (.first (.next s)) " " (aget box 0) " "
                (nil? (.seq e)) " " (.count e) " " (.equiv s (RT/list "a" "b" "c")))))))
