(ns arbace.javalisp.prec
  "When Java needs parentheses around an expression form: the one rule both directions use.
  Printing Java (arbace.javalisp.l2j) inserts parentheses exactly where `wrap?` says so;
  transcribing Java (arbace.javalisp.j2l) writes a parenthesized expression as (:paren x)
  only where `wrap?` would not insert those parentheses itself.

  Forms are nodes as read or built: {:a text} atoms, {:L [...]} lists, {:V [...]} vectors.")

(def levels
  "javac's precedence levels (TreeInfo)."
  {:assign 1 :assignop 2 :cond 3 :or 4 :and 5 :bitor 6 :bitxor 7 :bitand 8 :eq 9 :ord 10
   :shift 11 :add 12 :mul 13 :prefix 14 :postfix 15 :primary 16})

(def binary-levels
  {"||" :or "&&" :and "|" :bitor "bit-xor" :bitxor "&" :bitand "==" :eq "!=" :eq "<" :ord
   ">" :ord "<=" :ord ">=" :ord "<<" :shift ">>" :shift ">>>" :shift "+" :add "-" :add
   "*" :mul "/" :mul "%" :mul})

(def assign-heads
  #{"=" "|=" "&=" "<<=" ">>=" ">>>=" "+=" "-=" "*=" "%=" "div=" "bit-xor="})

(def primitive-types #{"boolean" "byte" "short" "char" "int" "long" "float" "double"})

(defn- atom? [n] (contains? n :a))
(defn- head [n] (when (:L n) (:a (first (:L n)))))

(defn numeral? [n] (and (atom? n) (boolean (re-matches #"[-+]?[0-9].*" (:a n)))))
(defn string-atom? [n] (and (atom? n) (.startsWith ^String (:a n) "\"")))

(defn numeric-literal?
  "True for a number atom or a (long n) / (float n) literal form."
  [n]
  (or (numeral? n)
      (and (#{"long" "float"} (head n)) (numeral? (second (:L n))))))

(defn- foldable-literal?
  "A literal javac would fold a preceding - into: decimal int or long, not starting with 0."
  [n]
  (let [a (if (atom? n) n (when (= "long" (head n)) (second (:L n))))]
    (boolean (and a (re-matches #"[1-9][0-9]*" (:a a))))))

(defn- negative-literal? [n]
  (and (numeric-literal? n)
       (.startsWith ^String (:a (if (atom? n) n (second (:L n)))) "-")))

(defn- array-type?
  "A base type followed only by empty dimension brackets or `...`."
  [n]
  (let [ds (rest (:L n))]
    (and (some #(or (:V %) (= "..." (:a %))) ds)
         (every? #(or (and (:V %) (empty? (:V %))) (= "..." (:a %)) (= ":ann" (head %))) ds))))

(defn- sized-new-array?
  "(new (T [n] ...)) without an initializer."
  [n]
  (and (= "new" (head n))
       (let [t (second (:L n))]
         (and (:L t) (some #(seq (:V %)) (rest (:L t)))
              (not (:V (last (:L n))))))))

(defn produced
  "The precedence level of the Java that form `n` prints as, or :lambda."
  [n]
  (cond
    (atom? n) (if (and (numeral? n) (.startsWith ^String (:a n) "-")) :prefix :primary)
    (:V n) :primary
    :else
    (let [h (head n) k (count (:L n))]
      (cond
        (= h "->") :lambda
        (assign-heads h) :assign
        (= h "?") :cond
        (= h "instanceof") :ord
        (= h "switch") :prefix
        (= h ":cast") :prefix
        (= h ":ref") :postfix
        (and (#{"+" "-" "!" "bit-not" "++" "--"} h) (= k 2)) :prefix
        (#{":++" ":--"} h) :postfix
        (binary-levels h) (binary-levels h)
        (#{"long" "float"} h) (if (negative-literal? n) :prefix :primary)
        :else :primary))))

(defn- starts-with-sign?
  "True when form `n` prints starting with + or - (a unary or a negative literal)."
  [n]
  (or (negative-literal? n)
      (and (#{"+" "-" "++" "--"} (head n)) (= 2 (count (:L n))))))

(defn- block-lambda? [n]
  (and (= "->" (head n)) (= "do" (head (nth (:L n) 2 nil)))))

(defn wrap?
  "True when form `n`, printed in context `ctx`, needs parentheses. ctx:
    :min       the lowest precedence level allowed without them
    :lambda    a lambda may stand there unparenthesized
    :neg       operand of unary minus (javac folds - into a following decimal integer)
    :str-prev  a + operand right after a string literal (javac folds adjacent ones)
    :cast-ref  operand of a cast to a reference type (where + and - mean binary ops)
    :aget      the array of an array access (new int[n][i] would be a 2-D creation)"
  [n ctx]
  (let [p (produced n)]
    (cond
      (= p :lambda) (if (block-lambda? n)
                      (>= (:min ctx 0) (levels :postfix))
                      (not (:lambda ctx)))
      (< (levels p) (:min ctx 0)) true
      (and (:neg ctx) (foldable-literal? n)) true
      (and (:str-prev ctx) (string-atom? n)) true
      (and (:cast-ref ctx) (starts-with-sign? n)) true
      (and (:aget ctx) (sized-new-array? n)) true
      :else false)))

(def free {:min 0 :lambda true})

(defn operand-contexts
  "For the items of an infix form with operator `op`: a vector, aligned with `xs`, holding
  for each operand its index among the operands, and nil for each line marker (an atom
  equal to `op`, or `.`) that precedes an operand."
  [op xs]
  (loop [xs xs k 0 acc []]
    (if-let [[a & more] (seq xs)]
      (if (and (pos? k) (atom? a) (= op (:a a)))
        (recur more k (conj acc nil))
        (recur more (inc k) (conj acc k)))
      acc)))

(defn child-contexts
  "For list form `n`, a vector aligned with its items giving the context (see `wrap?`) of
  each item printed as an expression, or nil. `root?` says whether `n` is printed as the
  root of a binary expression (unparenthesized under a binary operator it is not); javac
  folds adjacent string literals of a + chain only at the root. Operands of binary
  operators get :in-binary."
  ([n] (child-contexts n true))
  ([n root?]
  (let [[hn & xs] (:L n)
        h (:a hn)
        k (count xs)
        at (fn [ctxs] (into [nil] ctxs))]
    (cond
      (nil? h) nil
      (binary-levels h)
      (if (and (#{"+" "-" "!" "bit-not" "++" "--"} h) (= k 1))
        (at [{:min (levels :prefix) :neg (= h "-")}])
        (let [lv (levels (binary-levels h))
              idx (operand-contexts h xs)]
          ;; left to right: an operand follows a bare string literal when the previous
          ;; operand is a string literal printed without parentheses
          (at (loop [items (map vector xs idx) prev nil acc []]
                (if-let [[[x o] & more] (seq items)]
                  (if (nil? o)
                    (recur more prev (conj acc nil))
                    (let [ctx {:min (if (zero? o) lv (inc lv))
                               :in-binary true
                               :str-prev (and root? (= h "+") (some? prev) (string-atom? (first prev))
                                              (not (wrap? (first prev) (second prev))))}]
                      (recur more [x ctx] (conj acc ctx))))
                  acc)))))
      (#{"!" "bit-not" "++" "--"} h) (at [{:min (levels :prefix)}])
      (#{":++" ":--"} h) (at [{:min (levels :postfix)}])
      (assign-heads h) (let [idx (operand-contexts h xs)]
                         (at (map #(case % 0 {:min (inc (levels :assign))} 1 free nil) idx)))
      (= h "?") (let [idx (operand-contexts h xs)]
                  (at (map #(case % 0 {:min (inc (levels :cond))} 1 free 2 {:min (levels :cond) :lambda true} nil) idx)))
      (= h "instanceof") (let [idx (operand-contexts h xs)]
                           (at (map #(when (= % 0) {:min (levels :ord) :in-binary true}) idx)))
      (= h ":cast") (let [[t _] xs
                          ref? (not (and (atom? t) (primitive-types (:a t))))]
                      (at [nil {:min (levels :prefix) :lambda true :cast-ref ref?}]))
      (= h ":aget") (let [idx (operand-contexts "." xs)]
                      (at (map #(case % nil nil 0 {:min (levels :primary) :aget true} free) idx)))
      (= h "..") (at (cons (when-not (array-type? (first xs)) {:min (levels :primary)})
                           (repeat (dec k) nil)))
      (= h ":ref") (let [[e] xs]
                     (at (cons (when-not (or (#{":type" ":annotated"} (head e)) (array-type? e))
                                 {:min (levels :primary)})
                               (repeat (dec k) nil))))
      :else nil))))
