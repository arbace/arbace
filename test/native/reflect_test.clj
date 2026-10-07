(ns native.reflect-test
  "The compiler's reflective calls are invokedynamic sites (arbace.lang.ReflectorCallSite) that
  cache, per receiver class (and argument classes where the choice depends on them), the member
  arbace.lang.Reflector chooses. Each call here is compared with Reflector's own result or
  exception, on the first calls (through Reflector) and on later ones (cached)."
  (:require [arbace.test :refer :all])
  (:import (arbace.lang Reflector)))

(defclass ^:public Probe
  (field ^:public a)
  (field ^:public ^:final ^Boolean flag)
  (field ^:public ^int n)
  (constructor ^:public [this a] (set! (.-a this) a) (set! (.-flag this) (Boolean. true)))
  (method ^:public a [this] (java-str "method a " (.-a this)))
  ;; whether the call came through Reflector's Method.invoke rather than a cached handle
  (method ^:public ^:static throughReflector ^boolean []
    (for-each [^StackTraceElement e (.getStackTrace (Thread/currentThread))]
      (when (and (.equals (.getClassName e) "java.lang.reflect.Method") (.equals (.getMethodName e) "invoke"))
        (return true)))
    false)
  (method ^:public via ^boolean [this x] (Probe/throughReflector))
  (method ^:public via ^boolean [this ^long x ^String y] (Probe/throughReflector))
  (method ^:public via ^boolean [this ^double x ^String y] (Probe/throughReflector))
  (method ^:public via ^boolean [this ^String x ^String y] (Probe/throughReflector))
  (method ^:public viaInt ^boolean [this ^int x] (Probe/throughReflector))
  (method ^:public viaFn ^boolean [this ^java.util.function.Supplier x] (Probe/throughReflector))
  (method ^:public boxed ^Boolean [this] (Boolean. false))
  (method ^:public over [this ^long x] (java-str "long " x))
  (method ^:public over [this ^String x] (java-str "String " x))
  (method ^:public over [this ^double x] (java-str "double " x))
  (method ^:public takesInt [this ^int x] (java-str "int " x))
  (method ^:public fi [this ^java.util.function.Function f x] (.apply f x)))

(defn outcome
  "The value of (f), or the class and message of what it throws"
  [f]
  (try [:ok (f)] (catch Throwable t [:threw (class t) (.getMessage t)])))

(defn reflector [target name & args]
  (outcome #(Reflector/invokeInstanceMethod target name (object-array args))))

;; each site below is called at least three times on the same classes, so its first call goes
;; through Reflector, its second links the entry and the third runs the cached handle

(defn call-length [x] (.length x))
(defn call-index-of [s x] (.indexOf s x))
(defn call-index-of2 [s x i] (.indexOf s x i))
(defn call-starts-with [s x] (.startsWith s x))
(defn call-foo [x y] (.foo x y))
(defn call-over [p x] (.over p x))
(defn call-takes-int [p x] (.takesInt p x))
(defn call-equals [x y] (.equals x y))
(defn call-size [c] (.size c))
(defn call-via [p x] (.via p x))
(defn call-via2 [p x y] (.via p x y))
(defn call-via-int [p x] (.viaInt p x))
(defn call-via-fn [p x] (.viaFn p x))
(defn call-boxed [p] (.boxed p))
(defn call-fi [p f x] (.fi p f x))
(defn call-remove-if [l f] (.removeIf l f))
(defn call-qualified [s x] (String/.indexOf s x))
(defn call-format [fm s args] (.format fm s args))
(defn field-a [p] (.-a p))
(defn field-flag [p] (.-flag p))
(defn field-n [p] (.-n p))
(defn member-a [p] (.a p))
(defn member-length [s] (.length s))
(defn field-missing [p] (.-missing p))
(defn member-to-string [x] (.toString x))

(defn thrice [f & args] (vec (repeatedly 3 #(outcome (fn [] (apply f args))))))

(deftest monomorphic
  (is (= (repeat 3 [:ok 3]) (thrice call-length "abc")))
  (is (= (repeat 3 [:ok 3]) (thrice call-length (StringBuilder. "abc"))))
  (is (= (repeat 3 (reflector "abcb" "startsWith" "ab")) (thrice call-starts-with "abcb" "ab")))
  (is (= [:ok true] (outcome #(call-equals "a" "a"))))
  (testing "the first call, and the second, which links, go through Reflector; the third does not"
    (let [p (Probe. 1)]
      (is (= [true true false false] (vec (repeatedly 4 #(call-via p 1)))))
      (is (= [true true false false] (vec (repeatedly 4 #(call-via-int p 1)))) "boxArg's conversion")
      (is (= [true true false false] (vec (repeatedly 4 #(call-via-fn p (fn [] 1))))) "an IFn adapted")
      (doseq [x [1 (int 1) (float 1.5) 2.5 "s" nil]]
        (is (= [true false] (let [r (vec (repeatedly 3 #(call-via2 p x "y")))] [(first r) (peek r)]))
            (str "overloads, chosen with (and cached per) the argument classes: " (pr-str x)))))))

(deftest polymorphic-and-megamorphic
  (let [cs [(java.util.ArrayList. [1]) (java.util.HashMap. {1 2}) (java.util.HashSet. [1 2])
            (java.util.LinkedList. [1 2 3]) (java.util.TreeMap. {1 2}) (java.util.TreeSet. [1])
            (java.util.ArrayDeque. [1 2]) (java.util.Vector. [1]) (java.util.LinkedHashMap. {1 2})
            (java.util.IdentityHashMap. {1 2}) (java.util.concurrent.ConcurrentHashMap. {1 2})
            (java.util.Hashtable. {1 2 3 4}) [1 2] {1 2 3 4} #{1}]]
    (dotimes [_ 4]
      (is (= (map #(reflector % "size") cs) (map #(outcome (fn [] (call-size %))) cs)))))
  (testing "past the megamorphic limit (64 classes) calls still go through Reflector"
    (let [xs (eval `(vector ~@(for [i (range 80)] `(reify Object (toString [_] ~(str "mega" i))))))]
      (dotimes [_ 3]
        (is (= (map #(str "mega" %) (range 80)) (map member-to-string xs))))))
  (testing "several threads"
    (let [xs ["abc" (StringBuilder. "abcd") "" (StringBuffer. "ab")]
          fs (doall (for [t (range 8)] (future (doall (for [i (range 2000)] (call-length (nth xs (mod (+ i t) 4))))))))]
      (is (every? #(= (frequencies %) {3 500 4 500 0 500 2 500}) (map deref fs))))))

(deftest overloads-by-runtime-types
  (let [p (Probe. 1)
        args [1 "s" 2.5 (int 3) (short 4) (byte 5) (float 1.5) nil \c 2/3 (bigint 7)]]
    (dotimes [_ 3]
      (doseq [x args]
        (is (= (reflector p "over" x) (outcome #(call-over p x))) (str "over " (pr-str x)))
        (is (= (reflector p "takesInt" x) (outcome #(call-takes-int p x))) (str "takesInt " (pr-str x)))))
    (dotimes [_ 3]
      (doseq [x ["c" \c 99 (int 99) nil 2.0]]
        (is (= (reflector "abcabc" "indexOf" x) (outcome #(call-index-of "abcabc" x))) (pr-str x))
        (is (= (reflector "abcabc" "indexOf" x 2) (outcome #(call-index-of2 "abcabc" x 2))) (pr-str x))))
    (dotimes [_ 3]
      (doseq [[a b] [["a" "a"] ["a" nil] [1 1] [1 1.0] [nil 1]]]
        (is (= (reflector a "equals" b) (outcome #(call-equals a b))))))))

(deftest nil-arguments-and-receivers
  (dotimes [_ 3]
    (is (= (reflector "abc" "startsWith" nil) (outcome #(call-starts-with "abc" nil))))
    (is (= (reflector "abc" "charAt" nil) (outcome #((fn [s x] (.charAt s x)) "abc" nil))))
    (is (= (outcome #(Reflector/invokeInstanceMethod nil "indexOf" (object-array ["a"])))
           (outcome #(call-index-of nil "a")))
        "a nil receiver throws as Reflector does")
    (is (= NullPointerException (second (outcome #(call-index-of nil "a")))))
    (is (= (outcome #(Reflector/invokeNoArgInstanceMember nil "length" false))
           (outcome #(call-length nil))))
    (is (= (outcome #(Reflector/invokeNoArgInstanceMember nil "a" true))
           (outcome #(field-a nil))))))

(deftest errors
  (dotimes [_ 3]
    (is (= [:threw IllegalArgumentException "No matching method foo found taking 1 args for class java.lang.String"]
           (outcome #(call-foo "a" 1))))
    (is (= (reflector "abc" "startsWith" 1) (outcome #(call-starts-with "abc" 1))))
    (is (= [:threw ClassCastException "Cannot cast java.lang.Long to java.lang.String"]
           (outcome #(call-starts-with "abc" 1))))
    (let [p (Probe. 1)]
      (is (= (outcome #(Reflector/invokeNoArgInstanceMember p "missing" true))
             (outcome #(field-missing p))))
      (is (= IllegalArgumentException (second (outcome #(field-missing p))))))
    (is (= [:threw IllegalArgumentException "No matching field found: nope for class java.lang.String"]
           (outcome #((fn [s] (.nope s)) "a"))))
    (testing "a site that keeps failing to link (Reflector refuses the call) stops trying"
      (let [o (Object.)]
        (dotimes [_ 12]
          (is (= (reflector "abc" "indexOf" o) (outcome #(call-index-of "abc" o)))))
        (is (= [:ok 1] (outcome #(call-index-of "abc" "b"))))))
    (testing "exceptions of the called method pass through unchanged"
      (is (= [:threw StringIndexOutOfBoundsException] (subvec (outcome #((fn [s i] (.charAt s i)) "abc" 7)) 0 2))))))

(deftest members-and-fields
  (let [p (Probe. 42)]
    (set! (.-n p) 7)
    (dotimes [_ 3]
      (is (= 42 (field-a p)))
      (is (= 7 (field-n p)))
      (is (= "method a 42" (member-a p)) "a no-argument method wins over a field")
      (is (= 3 (member-length "abc")))
      (is (identical? Boolean/TRUE (field-flag p)) "a Boolean field reads canonical, as prepRet makes it")
      (is (identical? Boolean/FALSE (call-boxed p)) "a Boolean result is canonical, as prepRet makes it"))))

(deftest conversions
  (let [p (Probe. 1)]
    (dotimes [_ 3]
      (is (= "1!" (call-fi p (fn [x] (str x "!")) 1)) "an IFn for a functional interface parameter")
      (is (= [1 3] (let [l (java.util.ArrayList. [1 2 3 4])] (call-remove-if l even?) (vec l))))
      (is (= (reflector "abcabc" "indexOf" "c") (outcome #(call-qualified "abcabc" "c"))))
      (is (= (outcome #(Reflector/invokeInstanceMethodOfClass 5 "java.lang.String" "indexOf" (object-array ["c"])))
             (outcome #(call-qualified 5 "c")))
          "a qualified call on a receiver of another class fails as in Reflector")
      (is (= "x a 1" (str (call-format (java.util.Formatter.) "x %s %s" (object-array ["a" 1]))))))))

(defn call-abs [x] (Math/abs x))
(defn call-max [x y] (Math/max x y))
(defn call-require [x f] (java.util.Objects/requireNonNull x f))
(defn new-list [x] (java.util.ArrayList. x))
(defn new-sb [x] (StringBuilder. x))
(defn call-insert [sb i x s e] (.insert sb i x s e))
(defn call-fill [a from to v] (java.util.Arrays/fill a from to v))

(deftest static-methods-and-constructors
  (dotimes [_ 3]
    (doseq [x [-1 (int -2) -2.5 (float -1.5) (short -3) nil "s" 2/3]]
      (is (= (outcome #(Reflector/invokeStaticMethod Math "abs" (object-array [x])))
             (outcome #(call-abs x)))
          (pr-str x)))
    (doseq [[x y] [[1 2] [1.5 2] [1.5 2.5] [(int 1) (int 2)] [nil 1]]]
      (is (= (outcome #(Reflector/invokeStaticMethod Math "max" (object-array [x y])))
             (outcome #(call-max x y)))
          (pr-str [x y])))
    (is (= [:threw IllegalArgumentException "No matching method abs found taking 1 args"]
           (outcome #(call-abs "s"))))
    (is (= [:threw NullPointerException "true"] (outcome #(call-require nil (fn [] "true")))))
    (doseq [x [4 (int 4) [1 2] nil "s" -1]]
      (is (= (outcome #(Reflector/invokeConstructor java.util.ArrayList (object-array [x])))
             (outcome #(new-list x)))
          (pr-str x)))
    (is (= [:threw IllegalArgumentException "No matching ctor found for class java.util.ArrayList"]
           (outcome #(new-list "s"))))
    (is (= [:threw IllegalArgumentException "Illegal Capacity: -1"] (outcome #(new-list -1))))
    (doseq [x ["abc" 16 (int 16) nil \c]]
      (is (= (update (outcome #(Reflector/invokeConstructor StringBuilder (object-array [x]))) 1 str)
             (update (outcome #(new-sb x)) 1 str))
          (pr-str x))))
  (testing "guards on four and five classes"
    (dotimes [_ 3]
      (doseq [x ["xyz" (char-array "xyz") (StringBuilder. "xyz")]]
        (let [sb (StringBuilder. "ab")]
          (is (= (outcome #(str (Reflector/invokeInstanceMethod (StringBuilder. "ab") "insert" (object-array [1 x 0 2]))))
                 (outcome #(str (call-insert sb 1 x 0 2))))
              (pr-str x))))
      (doseq [[a v] [[(long-array 3) 7] [(object-array 3) "s"] [(int-array 3) (int 7)] [(double-array 3) 1.5]]]
        (call-fill a 0 2 v)
        (is (= [v v] (take 2 (vec a))) (pr-str v)))))
  (testing "a static call through a cached handle; the site is linked already, the new fn class links at once"
    (let [via #(call-require nil (fn [] (str (Probe/throughReflector))))]
      (is (= ["true" "false" "false" "false"]
             (vec (repeatedly 4 #(nth (outcome via) 2))))))))
