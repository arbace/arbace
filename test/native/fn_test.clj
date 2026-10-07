(ns native.fn-test
  "The class forms' code forms in ordinary fns (doc/classes/SPEC.md §9.5): arbace.lang.Compiler
  hands a fn using them over to the class forms compiler, which compiles it with Clojure's
  meaning (§5.13)."
  (:require [arbace.test :refer :all]))

(defn day [n] (switch (int n) 0 "Sun" (1 2 3 4 5) "work" "Sat"))

(defn find-first [pred xs]
  (label :found
    (for-each [x ^Iterable xs] (when (pred x) (break :found x)))
    nil))

(defn early [x] (when (neg? x) (return :neg)) (* 2 x))

(defn varargs [a & more] (switch (int a) 1 (count more) 2 (apply + more) -1))

(defn named [n] (switch (int n) 0 0 (+ n (named (dec n)))))

(deftest code-forms-in-fns
  (is (= ["Sun" "work" "Sat"] (map day [0 3 6])))
  (is (= 4 (find-first even? [1 3 4 5 6])))
  (is (nil? (find-first even? [1 3])))
  (is (= "a1cnull2.5" (java-str "a" 1 \c nil 2.5)))
  (is (= [:neg 8] [(early -1) (early 4)]))
  (is (= [2 12 -1] [(varargs 1 2 3) (varargs 2 3 4 5) (varargs 9)]))
  (is (= 10 (named 4)))
  (is (= 2 (switch "b" "a" 1 "b" 2 0)))
  (is (= "hi!" (.apply (lambda java.util.function.Function [s] (str s "!")) "hi")))
  (is (= :big (let [x 5] (label :L (if (> x 3) (break :L :big) :small)))))
  (is (= "anon!" (str (anon Object [] (method ^:public toString [this] "anon!"))))))

(deftest clojure-in-handed-over-fns
  (let [k 10 s "str"]
    (is (= 12 ((fn [x] (switch (int x) 1 (+ k 1) 2 (+ k 2) 0)) 2)))
    (is (= "str5" ((fn [] (switch (int 5) 5 (str s 5) :no))))))
  (is (= [2 4 6] (switch (int 1) 1 (vec (map (fn [x] (* x 2)) [1 2 3])) nil)))
  (is (= [true false]
         (letfn [(ev? [n] (if (zero? n) true (od? (dec n)))) (od? [n] (if (zero? n) false (ev? (dec n))))]
           (switch (int 0) 0 [(ev? 10) (ev? 7)] nil))))
  (is (= ["A" "num" "num" "B" "other"]
         (map (fn [x] (switch (int 1) 1 (case x :a "A" (1 2) "num" "b" "B" "other") nil)) [:a 1 2 "b" 'zz])))
  (is (= [:div0 1/2] (map (fn [x] (switch (int 1) 1 (try (/ 1 x) (catch ArithmeticException e :div0)) nil)) [0 2])))
  (is (= [1 2 3] (let [m {:a 1 :b 2}] (switch (int 1) 1 [(:a m) (m :b) ({:c 3} :c)] nil))))
  (is (= [:z :o :t :z :o]
         ((fn [n] (loop [i 0 acc []]
                    (if (< i n)
                      (switch (int (rem i 3)) 0 (recur (inc i) (conj acc :z)) 1 (recur (inc i) (conj acc :o))
                              (recur (inc i) (conj acc :t)))
                      acc)))
          5)))
  (is (= :three ((fn [s] (switch (int (.length s)) 3 :three :other)) "abc")))
  (is (= "(0 1 ...)" (switch (int 1) 1 (binding [*print-length* 2] (pr-str (range 10))) nil)))
  (is (= [1 2 3 4] ((fn [{:keys [a b]} [x y]] (switch (int 1) 1 [a b x y] nil)) {:a 1 :b 2} [3 4])))
  (is (= {:x 1} (switch (int 1) 1 (meta ^{:x 1} (fn [] 1)) nil))))

(def from-def (switch (int 2) 1 :one 2 :two :other))

(defn defining [] (switch (int 1) 1 (def defined-inside 42) nil) defined-inside)

(deftest defs
  (is (= :two from-def))
  (is (= 42 (defining)))
  (is (= "native/fn_test.clj" (:file (meta #'defined-inside)))))

(defn sum-ints [^ints a] (let [^:mutable ^int s 0] (for-each [^int x a] (set! s (unchecked-add-int s x))) s))

(deftest extended-special-forms
  (is (= 6 (sum-ints (int-array [1 2 3]))))
  (is (= [7 0 0] (let [a (new int/1 3)] (aset a 0 7) (vec a))))
  (is (= 5 (let [^:mutable c 0] (dotimes [i 5] (set! c (inc c))) c)))
  (is (= Integer (let [^int i 3] (class i))))
  (is (= "013" (with-out-str
                 ((fn [n] (loop [i 0] (when (< i n) (when (= i 2) (continue (inc i))) (print i) (recur (inc i))))) 4))))
  ;; recur out of tail position and across try stay Clojure's errors
  (is (thrown? arbace.lang.Compiler$CompilerException (eval '(loop [x 5] (try (recur 1))))))
  (is (thrown? arbace.lang.Compiler$CompilerException (eval '(loop [x 5] (inc (recur 1)))))))

(defclass ^:public Holder
  (method ^:public ^:static twice [xs] (vec (map (fn [x] (* 2 x)) xs)))
  (method ^:public ^:static pick [x] (case x :a 1 :b 2 0))
  (method ^:public ^:static evens [n]
    (letfn [(e? [n] (if (zero? n) true (o? (dec n)))) (o? [n] (if (zero? n) false (e? (dec n))))] (e? n))))

(deftest clojure-in-class-bodies
  (is (= [2 4] (Holder/twice [1 2])))
  (is (= 2 (Holder/pick :b)))
  (is (true? (Holder/evens 4))))

(defn sq ^long [^long x] (switch (int x) 0 0 (* x x)))

(defn mixed (^long [^long a ^Object b] (switch (int a) 1 (count b) a)) ([a] (java-str a)))

(defn obj-ret ^long [m] (switch (int 1) 1 (get m :a) 0))

(deftest primitive-signatures
  (is (= [25 0] [(sq 5) (sq 0)]))
  (is (= 36 (.invokePrim ^arbace.lang.IFn$LL sq 6)))
  (is (instance? arbace.lang.IFn$LOL mixed))
  (is (= [3 7 ":k"] [(mixed 1 [1 2 3]) (mixed 7 nil) (mixed :k)]))
  (is (= 41 (obj-ret {:a 41})))
  (is (= [2 true] (let [f (fn ^long [^long x] (java-str x) (inc x))] [(f 1) (instance? arbace.lang.IFn$LL f)]))))

(def reified (reify Runnable (run [this] (switch (int 1) 1 (print "reified") nil))))

(deftest reify-in-handed-over-code
  (is (= "reified" (with-out-str (.run ^Runnable reified))))
  (let [k 5
        o (reify Object (toString [this] (java-str "k=" k)))]
    (is (= ["k=5" nil {:a 1} "k=5"] [(str o) (meta o) (meta (with-meta o {:a 1})) (str (with-meta o {:a 1}))])))
  (is (= ["a" "bb" "ccc"]
         (sort (reify java.util.Comparator (compare [this a b] (switch (int 0) 0 (- (count a) (count b)) 0)))
               ["ccc" "a" "bb"]))))

(defn long-selector [n] (switch n 1 :one (2 3) :few :many))

(deftest selectors
  (is (= [:one :few :many] (map long-selector [1 3 10])))
  (is (= :x (switch 1 1 :x :y)))
  (is (thrown? ArithmeticException (long-selector Long/MAX_VALUE))))
