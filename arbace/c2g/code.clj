(ns arbace.c2g.code
  "c2g's translation of code (C2G-SPEC §7): the analyzer's typed nodes to Go statements and
  expressions, in four contexts (statement, return, assign, expression, §7.2), with
  temporaries for Java's order of evaluation, Java's arithmetic (§7.4), null checks and the
  typed-nil rule (§5.6), try/catch/finally and control codes (§7.9), monitors (§8.1), loops,
  labels and switches (§7.7, §7.8), lambdas (§7.11)."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.access :as acc]))

;; ---------------------------------------------------------------------------------------
;; state

(def ^:dynamic *f*
  "The Go function being written: {:pkg :class :static :t :this :method-ret :fn-id :targets
  :try-frame :captured :ctor-params, and atoms :names :taken :used :counter :pkgstate}."
  nil)

(def ^:dynamic *block* "The statement list being written: an atom of items." nil)

(def ^:dynamic *pkgstate*
  "Per package: {:lits (atom {string sym}) :ups (atom #{[from to]}) :lambdas (atom #{fi})
  :imports (atom #{})}."
  nil)

(defn fail [msg & [data]] (throw (ex-info (str "c2g: " msg) (merge {::fail true} data))))

(defn pkg [] (:pkg *f*))

(defn jsym [ident] (m/jrt-sym (pkg) ident))
(defn csym ([n] (m/class-sym (pkg) n)) ([n suffix] (m/class-sym (pkg) n suffix)))
(defn gotype [d] (m/go-type (pkg) d))

(defn- tag [sym type] (with-meta sym {:tag type}))

(defn new-counter! [k]
  (let [c (:counter *f*)] (k (swap! c update k (fnil inc 0)))))

(defn fresh
  "A fresh Go local name based on s."
  [s]
  (let [taken (:taken *f*)
        base (nm/local-name s)]
    (loop [i 1]
      (let [c (if (= i 1) base (str base "_" i))]
        (if (contains? @taken c)
          (recur (inc i))
          (do (swap! taken conj c) (symbol c)))))))

(defn fresh-raw
  "A fresh Go local name for c2g's own variables (not munged: no Java local has it)."
  [s]
  (let [taken (:taken *f*)]
    (loop [i 1]
      (let [c (if (= i 1) s (str s "_" i))]
        (if (contains? @taken c)
          (recur (inc i))
          (do (swap! taken conj c) (symbol c)))))))

(defn tmp [] (fresh-raw (str "tmp" (new-counter! :tmp))))

(defn use-sym! [s] (swap! (:used *f*) conj (str s)) s)

(defn binding-sym
  "The Go name of an analyzer binding in the current function."
  [b]
  (or (get @(:names *f*) (:id b))
      (let [s (fresh (or (:sym b) "p"))]
        (swap! (:names *f*) assoc (:id b) s)
        s)))

;; ---------------------------------------------------------------------------------------
;; blocks: statements with declarations scoping over the rest of the block (Go forms §7.3)

(defn emit! [form] (swap! *block* conj form) nil)

(defn decl!
  "Declares Go local sym of Go type gt with initial value init (nil: the zero value)."
  [sym gt init]
  (swap! *block* conj [:let (tag sym gt) (if (some? init) init (list 'zero gt))])
  sym)

(defn- fold-items [items used]
  (loop [items (reverse items) acc ()]
    (if-let [[it & more] (seq items)]
      (if (and (vector? it) (= :let (first it)))
        ;; gather consecutive lets
        (let [[lets more2] (loop [ls [it] more more]
                             (if (and (seq more) (vector? (first more)) (= :let (first (first more))))
                               (recur (conj ls (first more)) (rest more))
                               [ls more]))
              lets (reverse lets)
              bindings (vec (mapcat (fn [[_ s i]] [s i]) lets))
              unused (for [[_ s _] lets
                           v (if (seq? s) (rest s) [s])
                           :when (and (symbol? v) (not= '_ v) (not (contains? used (str v))))]
                       (list 'set! '_ (symbol (str v))))]
          (recur more2 (list (apply list 'let bindings (concat unused acc)))))
        (recur more (cons it acc)))
      acc)))

(defn with-block
  "Runs f with a fresh statement list; returns the list of Go statements."
  [f]
  (binding [*block* (atom [])]
    (f)
    (fold-items @*block* @(:used *f*))))

(defn block-forms [items] (fold-items items @(:used *f*)))

;; ---------------------------------------------------------------------------------------
;; types and values

(defn vt [node] (let [ty (a/value-type (:type node))] ty))

(defn object-like? [d] (or (= d "Ljava/lang/Object;") (= d :null)))

(defn zero-of
  "The Go zero value of descriptor d, as an expression."
  [d]
  (cond (= d "Z") false
        (#{"B" "C" "S" "I" "J"} d) 0
        (#{"F" "D"} d) 0.0
        :else nil))

(defn literal? [x] (or (number? x) (boolean? x) (string? x)))

(defn- fold-int-conv
  "A Go integer literal converted to Java type to (wrapping as Java's conversions do)."
  [v to]
  (let [v (long v)]
    (case to
      "I" (long (unchecked-int v)) "J" v "S" (long (unchecked-short v)) "B" (long (unchecked-byte v))
      "C" (long (int (unchecked-char v))) nil)))

(declare expr expr-op expr-as stmt! assign! ret! cond-expr translate-ctx missing-reason missing-expr go-call?
         prim-convert box-val)

(defn up-sym
  "The nil-preserving conversion of pointer type `from` (desc) to interface `to` (desc):
  .Ref for any, else a generated Up_ function."
  [x from to]
  (cond
    (object-like? to) (list '.Ref x)
    :else (do (swap! (:ups *pkgstate*) conj [from to])
              (list (symbol (str "Up_" (nm/desc-code from) "_" (nm/desc-code to))) x))))

(defn coerce
  "Converts Go expression x of Java static type from (a descriptor, :null) to Java type `to`
  as an assignment conversion that the analyzer left implicit (reference widening): pointer
  to interface conversions are nil-preserving unless nn (x known not null)."
  [x from to nn]
  (cond
    (or (nil? to) (= to "V")) x
    (= from :null) (if (t/prim? to) (zero-of to) nil)
    (= from :none) x
    (= from to) x
    ;; branches of a conditional keep their own primitive type (the JVM's int slot): widened
    (and (t/prim? from) (t/prim? to)) (prim-convert x from to)
    ;; a primitive branch of a conditional whose value is a reference: boxed by its own type
    (t/prim? from) (coerce (box-val x from) (str "L" (t/box-of from) ";") to true)
    (and (t/prim? to) (t/unbox-of from))
    (let [p (t/unbox-of from)]
      (prim-convert (list (symbol (str "." (nm/method-base (str (t/prim-desc->name p) "Value") (str "()" p))))
                          (if (m/pointer-desc? from) (list (jsym "NN") x) x))
                    p to))
    (t/prim? to) (fail (str "unexpected primitive coercion " from " -> " to))
    (and (m/pointer-desc? from) (m/iface-desc? to)) (if nn x (up-sym x from to))
    (and (m/pointer-desc? from) (m/pointer-desc? to))
    (if (= (gotype from) (gotype to)) x
        ;; arrays are all *RefArray (references) or one Go type per primitive element
        (fail (str "pointer coercion " from " -> " to)))
    (and (m/iface-desc? from) (m/iface-desc? to)) x
    ;; interface to pointer: a static type the analyzer knows is a subtype (a tag on a
    ;; branch...): an unchecked conversion
    (and (m/iface-desc? from) (m/pointer-desc? to)) (list (list 'inst (jsym "As") (gotype to)) (if (m/pointer-desc? from) x x))
    :else x))

;; values: {:x form :t desc :nn known-non-null :pure no-side-effect-no-state}

(defn- v [x t & {:as more}] (merge {:x x :t t} more))

;; ---------------------------------------------------------------------------------------
;; classifying nodes

(declare guarded-static?)

(def ^:private simple-ops
  #{:const :local :this-path :outer-param-path :class-lit :get-static :get-field :invoke :new
    :arith :compare :convert :cast :instance? :not :nil? :identical? :bool= :aget :alength
    :java-str :null-checked :var-deref :var-invoke :new-array :array-init :lambda :method-ref :fi-adapter
    :none ::go})

(defn expressible?
  "Is node's own operation one Go expression (its operands may still need statements)?"
  [node]
  (and (contains? simple-ops (:op node))
       (not (and (#{:and :or} (:op node)) (not= "Z" (:type node))))
       (not (and (= :method-ref (:op node)) (= :bound (:kind node))))
       (not (guarded-static? node))))

(defn- guarded-static?
  "A static access that runs C_Init() first (from code outside the class and its subclasses)."
  [node]
  (when (#{:get-static :set-static} (:op node))
    (let [f (:field node)
          o (or (:declarer f) (:owner f))]
      (and (m/translated? o) (not (m/trivial-init? o))
           (not (and (:class *f*) (env/subclass? (:class *f*) o)))))))

(defn simple?
  "Can node be one Go expression (§7.2)?"
  [node]
  (let [node (acc/unaccess node)]
    (and (expressible? node)
         (not (missing-reason node))
         (not= "V" (:type node))
         (every? simple? (case (:op node)
                           (:lambda :method-ref) (when (= :method-ref (:op node)) (remove nil? [(:recv node)]))
                           (concat (when (map? (:target node)) (when (:op (:target node)) [(:target node)]))
                                   (filter #(and (map? %) (:op %)) [(:expr node) (:val node) (:array node) (:index node) (:base node)])
                                   (:args node) (:elems node) (:dims node)
                                   (keep :node (:parts node))))))))

(def ^:private effect-ops
  #{:invoke :new :set-local :set-field :set-static :aset :new-array :array-init :java-str :var-invoke
    :var-deref :throw :monitor :try :assert :ctor-call :null-checked :cast :lambda :method-ref})

(defn effects?
  "Can evaluating node change state or throw (§7.2)?"
  [node]
  (let [hit (volatile! false)]
    (letfn [(w [n]
              (when (and (map? n) (:op n) (not @hit))
                (when (or (effect-ops (:op n)) (not (simple? n)) (guarded-static? n)
                          (#{:aget :get-field :alength :convert :arith} (:op n)))
                  ;; field reads, array reads, unboxing and division can throw
                  (when (or (effect-ops (:op n)) (not (simple? n)) (guarded-static? n)
                            (#{:aget :alength} (:op n))
                            (and (= :get-field (:op n)) (not= :this-path (:op (:target n))))
                            (and (= :convert (:op n)) (t/ref? (:from n)))
                            (and (= :arith (:op n)) (#{:div :rem} (:o n)) (#{"I" "J"} (:type n))))
                    (vreset! hit true)))
                (doseq [c (concat (when (map? (:target n)) [(:target n)]) [(:expr n) (:val n) (:array n) (:index n)]
                                  (:args n) (:elems n) (:dims n) (keep :node (:parts n)))]
                  (w c))))]
      (w node))
    @hit))

(defn reads-state?
  "Is node's value at risk from a later operand's call (§7.2): it reads a field, a static or
  an array element, or can throw (an integer division), outside the arguments of a call of
  its own (Go orders calls among themselves, and a call's arguments come before it)?"
  [node]
  (let [hit (volatile! false)]
    (letfn [(w [n]
              (when (and (map? n) (:op n) (not @hit))
                (when (or (#{:get-field :get-static :aget :alength :var-deref} (:op n))
                          (and (= :arith (:op n)) (#{:div :rem} (:o n)) (#{"I" "J"} (:type n))))
                  (vreset! hit true))
                (when-not (#{:invoke :new :var-invoke :java-str :lambda :method-ref} (:op n))
                  (doseq [c (concat (when (map? (:target n)) [(:target n)]) [(:expr n) (:val n) (:array n) (:index n)]
                                    (:args n) (:elems n) (:dims n))]
                    (w c)))))]
      (w node))
    @hit))

(defn stable?
  "Is node's value unaffected by any later evaluation (a constant, an immutable local, this)?"
  [node]
  (case (:op node)
    (:const :this-path :class-lit) true
    :local (not (:mutable (:b node)))
    false))

;; ---------------------------------------------------------------------------------------
;; operands in Java's order (§7.2)

(defn- hoist-value
  "Puts a translated value into a fresh temporary."
  [val]
  (let [s (tmp)
        gt (gotype (if (= :null (:t val)) "Ljava/lang/Object;" (:t val)))]
    (decl! s gt (:x val))
    (use-sym! s)
    (assoc val :x s :pure true)))

(defn operands
  "Translates nodes left to right as operands of one Go expression (or call), each converted
  to its type in `wants` (a desc or nil). An operand is moved to a temporary when a later
  operand writes statements or has side effects that could change what it reads."
  [nodes wants]
  (let [nodes (vec nodes)
        n (count nodes)
        stmt-after (vec (for [i (range n)] (some #(not (simple? %)) (subvec nodes (inc i)))))
        eff-after (vec (for [i (range n)] (some effects? (subvec nodes (inc i)))))
        base (vec (for [i (range n) :let [node (nth nodes i)]]
                    (boolean (and (not (stable? node))
                                  (or (nth stmt-after i)
                                      (and (nth eff-after i) (reads-state? node)))))))
        ;; a hoisted operand is evaluated before the expression: so must every operand before
        ;; it that is not stable, to keep Java's left-to-right order
        need-at (vec (for [i (range n)]
                       (or (nth base i)
                           (and (not (stable? (nth nodes i))) (some true? (subvec base (inc i)))))))]
    (vec (for [i (range n)]
           (let [node (nth nodes i)
                 want (nth wants i nil)
                 val (expr node)
                 val (cond
                       (and want (:missing val)) (assoc val :x (missing-expr (:missing val) want) :t want)
                       want (assoc val :x (coerce (:x val) (:t val) want (:nn val)) :t want)
                       :else val)
                 need (nth need-at i)]
             (if need (hoist-value val) val))))))

;; ---------------------------------------------------------------------------------------
;; constants and literals

(defn- java-hash [^String s] (.hashCode s))

(defn string-lit
  "The pooled literal (§7.5) of Java string s: a package variable Lit_n."
  [s]
  (let [lits (:lits *pkgstate*)]
    (or (get @lits s)
        (let [sym (symbol (str "Lit_" (count @lits)))]
          (swap! lits assoc s sym)
          sym))))

(defn float-lit [t x]
  (let [d (double x)]
    (cond
      (Double/isNaN d) (jsym (if (= t "F") "NaN32" "NaN64"))
      (Double/isInfinite d) (jsym (str (if (pos? d) "PosInf" "NegInf") (if (= t "F") "32" "64")))
      (and (zero? d) (neg? (Math/copySign 1.0 d))) (jsym (if (= t "F") "NegZero32" "NegZero64"))
      (= t "F") (Double/parseDouble (Float/toString (float x)))
      :else d)))

(defn const-value [node]
  (let [ty (:type node) val (:val node)]
    (cond
      (nil? val) (v nil :null)
      (= ty "Z") (v (boolean val) "Z" :pure true)
      (#{"I" "S" "B" "J"} ty) (v (long (if (char? val) (int val) val)) ty :pure true)
      (= ty "C") (v (long (if (char? val) (int val) val)) "C" :pure true)
      (#{"F" "D"} ty) (v (float-lit ty val) ty :pure true)
      (string? val) (v (use-sym! (string-lit val)) "Ljava/lang/String;" :nn true :pure true)
      :else (v (missing-expr "Clojure constant" (if (keyword? ty) "Ljava/lang/Object;" ty)) (if (keyword? ty) "Ljava/lang/Object;" ty)))))

;; ---------------------------------------------------------------------------------------
;; missing parts of the closed world: an operation c2g cannot translate throws
;; UnsupportedOperationException when reached (JRT-SOURCES.md, "Decided")

(defn missing-expr [why d]
  (if (m/erased-desc? d)
    ;; a value of an erased type (c2g/erase): nothing uses it but what the Go build cuts
    (list 'conv (gotype d) nil)
    (list (list 'inst (jsym "C2g_Missing") (if (or (= d "V") (nil? d) (keyword? d)) 'bool (gotype d)))
          (str why))))

(defn- class-missing [n]
  (when-not (m/in-world? n) (str "class " (str/replace n "/" ".") " is not in the closed world")))

(defn- desc-missing [d] (some class-missing (m/desc-classes d)))
(defn- mdesc-missing [d] (some class-missing (m/method-desc-classes d)))

(defn- hw-static-exists? [cls base]
  (contains? (:funcs (:jrt m/*w*)) (str (m/go-name cls) "_" base)))

(defn missing-reason
  "Why node cannot be translated in this world (a class or member it needs does not exist in
  Go), or nil."
  [node]
  (let [node (acc/unaccess node)]
    (case (:op node)
      :invoke
      (let [mm (:method node)
            owner (:owner node)
            mowner (or (:owner mm) owner)]
        (or (when-not (:array-clone node) (or (class-missing owner) (class-missing mowner)))
            (mdesc-missing (:desc node))
            (when (and (= :special (:kind node)) (:outer-super node)) "an outer class's super call")
            (when (and (m/hand-written? mowner) (not= mowner "java/lang/Object"))
              (let [base (nm/method-base (:name node) (:desc node))]
                (if (= :static (:kind node))
                  (when-not (hw-static-exists? mowner base)
                    (str "jrt lacks " (str/replace mowner "/" ".") "." (:name node) (:desc node)))
                  (when-not (or (m/hw-method-exists? mowner mm)
                                ;; the qualifying type (or a supertype of it) jrt has
                                (some #(and (m/in-world? %) (m/hand-written? %) (m/hw-method-exists? % mm))
                                      (cons owner (m/all-supertypes owner))))
                    (str "jrt lacks " (str/replace mowner "/" ".") "." (:name node) (:desc node))))))
            (when (and (m/translated? mowner) (not (m/member-in-world? mowner (assoc (or mm node) :desc (:desc node)))))
              (str "method " (:name node) (:desc node) " is not in the closed world"))))
      :new (or (class-missing (:class node)) (mdesc-missing (:desc (:ctor node)))
               (when (and (m/hand-written? (:class node)) (not= "java/lang/Object" (:class node)))
                 (let [n (nm/new-name (m/go-name (:class node)) (:desc (:ctor node)))]
                   (when-not (contains? (:funcs (:jrt m/*w*)) n)
                     (str "jrt lacks constructor " (str/replace (:class node) "/" ".") (:desc (:ctor node)))))))
      (:get-static :set-static :get-field :set-field)
      (let [f (:field node) o (or (:declarer f) (:owner f))]
        (or (class-missing o) (desc-missing (:desc f))
            (when (and (m/hand-written? o) (m/static? f))
              (let [s (nm/static-field-name (m/go-name o) (:name f))
                    scan (:jrt m/*w*)]
                (when-not (or (contains? (:vars scan) s) (contains? (:consts scan) s))
                  (str "jrt lacks " (str/replace o "/" ".") "." (:name f)))))))
      :cast (desc-missing (:class node))
      :instance? nil
      :class-lit nil
      (:new-array :array-init) (desc-missing (:type node))
      :lambda (class-missing (:fi node))
      :method-ref (or (class-missing (:fi node)) (class-missing (:owner node)) (mdesc-missing (:desc node)))
      :fi-adapter (class-missing (t/desc->internal (:type node)))
      :const (when (= :bigint (:type node)) "a BigInt constant")
      nil)))

;; ---------------------------------------------------------------------------------------
;; this, locals, fields

(defn this-val
  "The whole object of the current instance code: t in a leaf class, this otherwise."
  []
  (v (or (:this *f*) (fail "no this here")) (t/internal->desc (:class *f*)) :nn true :pure true))

(defn- outer-field [x cls]
  ;; the outer instance field this$N of class cls, on x of cls's Go type
  (let [fname (symbol (str ".-" (nm/field-name (e/this0-name cls))))]
    (if (m/leaf? cls)
      (list fname x)
      (list fname (list (symbol (str ".Self_" (m/go-name cls))) x)))))

(defn this-path-val [node]
  (let [path (:path node)]
    (if (empty? path)
      (this-val)
      (loop [[c & more] path
             x nil
             first? true]
        (let [x (if first?
                  (if-let [o (:outer-param *f*)] o (list (symbol (str ".-" (nm/field-name (e/this0-name c)))) (:t *f*)))
                  (outer-field x c))
              od (:outer (a/decl c))]
          (if (seq more)
            (recur more x false)
            (v x (str "L" od ";"))))))))

(defn local-val [node]
  (let [b (:b node)]
    (if-let [f (get (:captured *f*) (:id b))]
      (v f (:type b))
      (v (use-sym! (binding-sym b)) (:type b) :pure (not (:mutable b))))))

(defn struct-of
  "An expression of the struct of declaring class `decl` reached from value x of static type
  desc sd (§5.2): x itself for a pointer, Self_D for a class interface; through the embedded
  struct when a nearer class hides a field of the same name. ptr: x is the struct pointer of
  sd's class itself (t, the receiver)."
  [x sd decl fname & [ptr]]
  (let [sn (t/desc->internal sd)
        base (if (or ptr (m/pointer-desc? sd))
               x
               (list (symbol (str ".Self_" (m/go-name decl))) x))
        start (if (or ptr (m/pointer-desc? sd)) sn decl)
        hidden (and fname (some (fn [c] (and (not= c decl) (some #(and (= fname (:name %)) (not (m/static? %))) (m/fields-of c))))
                                (take-while #(not= % decl) (m/superclass-chain start))))]
    (if hidden (list (symbol (str ".-" (m/go-name decl))) base) base)))

(def ^:private volatile-flag 0x40)

(defn volatile-field? [f] (m/has? (:flags f) volatile-flag))

(defn volatile-read [place d]
  (let [ld (list '.Load place)]
    (case d
      ("B") (list 'conv 'int8 ld) ("S") (list 'conv 'int16 ld) ("C") (list 'conv 'uint16 ld)
      "F" (list 'math/Float32frombits ld) "D" (list 'math/Float64frombits ld)
      ld)))

(defn volatile-write [place d x]
  (list '.Store place
        (case d
          ("B" "S" "C") (list 'conv 'int32 x) "F" (list 'math/Float32bits x) "D" (list 'math/Float64bits x)
          x)))

(defn field-place
  "The Go place of instance field f (declared in class o) on value tv."
  [tv f]
  (let [o (or (:declarer f) (:owner f))]
    (list (symbol (str ".-" (nm/field-name (:name f))))
          (struct-of (:x tv) (:t tv) o (:name f) (:ptr tv)))))

(defn target-operands
  "operands, with this (the current object, no outer hops) as the receiver struct t."
  [nodes wants]
  (let [n0 (first nodes)]
    (if (and (= :this-path (:op n0)) (empty? (:path n0)) (:t *f*))
      (into [(v (:t *f*) (t/internal->desc (:class *f*)) :nn true :pure true :ptr true)]
            (operands (rest nodes) (rest wants)))
      (operands nodes wants))))

(defn static-place [f]
  (let [o (or (:declarer f) (:owner f))]
    (m/class-sym (pkg) o (str "_" (nm/munge-name (:name f))))))

(defn class-init-call [o]
  (when (and (m/translated? o) (not (m/trivial-init? o)))
    (list (csym o "_Init"))))

;; ---------------------------------------------------------------------------------------
;; class objects

(defn class-val
  "The Class object of descriptor d."
  [d]
  (cond
    (t/prim? d) (jsym (str "Prim_" (t/prim-desc->name d)))
    (= d "V") (jsym "Prim_void")
    (t/array? d) (list '.ArrayClass (class-val (t/elem-type d)))
    :else (let [n (t/desc->internal d)]
            (cond
              (= n "java/lang/Object") (jsym "Object_class")
              ;; a class outside the closed world, or one jrt declares without a Class object:
              ;; a Class object of its name only (a \"cut\" class, C2G-SPEC §4.1)
              (or (not (m/in-world? n))
                  (and (m/hand-written? n) (not (contains? (:vars (:jrt m/*w*)) (str (m/go-name n) "_class")))))
              (do (when-let [cuts (:cuts *pkgstate*)] (swap! cuts conj n))
                  (csym n "_class"))
              :else (csym n "_class")))))

;; ---------------------------------------------------------------------------------------
;; expressions

(declare invoke-val new-val arith-val convert-val cast-val instance-val java-str-val
         lambda-val method-ref-val array-val const-or-missing fi-adapter-val)

(defn hoist
  "Translates a node that is not one Go expression into a fresh temporary (assign context)."
  [node]
  (let [d (let [x (vt node)] (if (or (= x :null) (= x :none)) "Ljava/lang/Object;" x))]
    (if (= :none (vt node))
      (do (stmt! node) (v (missing-expr "unreachable" d) d))
      (let [s (tmp)]
        (decl! s (gotype d) nil)
        (assign! node {:var s :desc d})
        (use-sym! s)
        (v s d)))))

(defn expr
  "Translates node in expression context: {:x go-form :t java-desc :nn bool}."
  [node]
  (let [node (acc/unaccess node)]
    (if-let [why (missing-reason node)]
      (let [d (vt node) d (if (keyword? d) "Ljava/lang/Object;" d)]
        (if (m/erased-desc? d)
          (v (missing-expr why d) d)
          (v (missing-expr why d) d :missing why)))
      (cond
        (= ::go (:op node)) (v (:x node) (:t node) :nn (:nn node))
        (not (expressible? node)) (hoist node)
        (= "V" (:type node)) (do (emit! (:x (expr-op node))) (v nil :null))
        :else (expr-op node)))))

(defn expr-op
  "The Go expression of an expressible node's own operation (operands translated as needed)."
  [node]
  (let [node (acc/unaccess node)]
    (if-let [why (missing-reason node)]
      (let [d (vt node) d (if (keyword? d) "Ljava/lang/Object;" d)]
        (v (missing-expr why d) d))
        (case (:op node)
          :const (const-value node)
          :none (v (missing-expr "unreachable" "Ljava/lang/Object;") "Ljava/lang/Object;")
          :local (local-val node)
          :this-path (this-path-val node)
          :outer-param-path (let [b (expr (:base node))]
                              (loop [[c & more] (:path node) x (:x b)]
                                (if c
                                  (recur more (outer-field x c))
                                  (v x (:type node)))))
          :class-lit (v (class-val (:class node)) "Ljava/lang/Class;" :nn true)
          :get-static (let [f (:field node)
                            place (static-place f)]
                        (v (if (volatile-field? f) (volatile-read place (:desc f)) place) (:desc f)))
          :get-field (let [f (:field node)
                           [tv] (target-operands [(:target node)] [nil])
                           place (field-place tv f)]
                       (v (if (volatile-field? f) (volatile-read place (:desc f)) place) (:desc f)))
          :invoke (invoke-val node)
          :new (new-val node)
          :arith (arith-val node)
          :compare (let [[a b] (operands (:args node) [nil nil])]
                     (v (list (symbol (name (:cmp node))) (:x a) (:x b)) "Z"))
          :convert (convert-val node)
          :cast (cast-val node)
          :instance? (instance-val node)
          :not (v (list 'not (cond-expr (:expr node))) "Z")
          (:and :or) (v (apply list (if (= :and (:op node)) 'and 'or) (map cond-expr (:args node))) "Z")
          :nil? (let [x (expr (:expr node))]
                  (v (list '== (:x x) nil) "Z"))
          :identical? (let [[a b] (operands (:args node) [nil nil])]
                        (v (cond
                             (nil? (:x a)) (list '== (:x b) nil)
                             (nil? (:x b)) (list '== (:x a) nil)
                             (= (gotype (:t a)) (gotype (:t b))) (list '== (:x a) (:x b))
                             :else (list '== (coerce (:x a) (:t a) "Ljava/lang/Object;" (:nn a))
                                         (coerce (:x b) (:t b) "Ljava/lang/Object;" (:nn b))))
                           "Z"))
          :bool= (let [[a b] (operands (:args node) ["Z" "Z"])] (v (list '== (:x a) (:x b)) "Z"))
          :aget (let [[a i] (operands [(:array node) (:index node)] [nil "I"])
                      et (t/elem-type (:t a))
                      el (list 'aget (list '.-A (:x a)) (:x i))]
                  (v (if (or (t/prim? et) (= et "Ljava/lang/Object;"))
                       el
                       (list (list 'inst (jsym "As") (gotype et)) el))
                     et))
          :alength (let [a (expr (:array node))]
                     (v (list 'conv 'int32 (list 'len (list '.-A (:x a)))) "I"))
          :java-str (java-str-val node)
          :null-checked (let [x (expr (:expr node))]
                          (v (if (m/pointer-desc? (:t x))
                               (list (jsym "NN") (:x x))
                               (list (list 'inst (jsym "C2g_NotNull") (gotype (:t x))) (:x x)))
                             (:t x) :nn true))
          (:new-array :array-init) (array-val node)
          :lambda (lambda-val node)
          :fi-adapter (fi-adapter-val node)
          :method-ref (method-ref-val node)
          :var-deref (let [f (:field node)]
                       (v (list '.Deref__O (static-place f)) "Ljava/lang/Object;"))
          :var-invoke (let [f (:field node)
                            args (operands (:args node) (repeat "Ljava/lang/Object;"))
                            n (count args)
                            ifn (t/lang-class "IFn")
                            mname (nm/method-base "invoke" (t/method-desc (repeat n "Ljava/lang/Object;") "Ljava/lang/Object;"))]
                        (v (apply list (symbol (str "." mname))
                                  (list (csym ifn "_Cast") (list '.Deref__O (static-place f)))
                                  (map :x args))
                           "Ljava/lang/Object;"))
          (fail (str "no expression translation for " (:op node)))))))

(defn expr-as
  "node's Go expression converted to Java type `to`."
  [node to]
  (let [x (expr node)]
    (if (and (:missing x) to (not= to "V")) (missing-expr (:missing x) to) (coerce (:x x) (:t x) to (:nn x)))))

(defn cond-expr
  "A Go boolean expression of node as a condition (Clojure's truth on non-booleans)."
  [node]
  (let [ty (if (= ::go (:op node)) (:t node) (vt node))]
    (cond
      (= ty "Z") (:x (expr node))
      (= ty :null) (do (when-not (= :const (:op node)) (stmt! node)) false)
      (= ty :none) (do (stmt! node) false)
      (t/prim? ty) (do (when-not (#{:const :local} (:op node)) (stmt! node)) true)
      (or (= ty "Ljava/lang/Boolean;") (= ty "Ljava/lang/Object;"))
      (let [x (expr node)]
        (list (jsym "C2g_Truth") (coerce (:x x) (:t x) "Ljava/lang/Object;" (:nn x))))
      :else (let [x (expr node)] (list '!= (:x x) nil)))))

;; ---------------------------------------------------------------------------------------
;; arithmetic (§7.4)

(defn- go-lit? [x] (and (number? x) (not (float? x))))

(defn- unsigned-lit
  "A Go literal of the unsigned value of Java integer v of the given width (Go rejects the
  conversion of a negative constant to an unsigned type)."
  [v bits]
  (let [v (long v)]
    (if (neg? v)
      (if (= bits 64) (+ (bigint v) 18446744073709551616N) (+ v 4294967296))
      v)))

(defn arith-val [node]
  (let [{:keys [family o args type]} node
        ty type
        shift? (#{:shl :shr :ushr} o)
        vals (operands args (if shift? [ty nil] (repeat ty)))
        [a b] (map :x vals)
        bits (if (= ty "J") 63 31)
        cnt (fn [] (if (go-lit? b) (bit-and (long b) bits) (list 'bit-and b bits)))
        fmul (fn [x y] (list 'conv (gotype ty) (list '* x y)))]
    (v
      (if (and (= family :exact) (= ty "J"))
        (case o
          :add (list (jsym "Math_AddExact_J_J__J") a b)
          :sub (list (jsym "Math_SubtractExact_J_J__J") a b)
          :mul (list (jsym "Math_MultiplyExact_J_J__J") a b)
          :inc (list (jsym "Math_IncrementExact_J__J") a)
          :dec (list (jsym "Math_DecrementExact_J__J") a)
          :div (list '/ a b)
          :rem (list '% a b))
        (case o
          :add (list '+ a b)
          :sub (list '- a b)
          :mul (if (#{"F" "D"} ty) (fmul a b) (list '* a b))
          :div (list '/ a b)
          :rem (if (#{"F" "D"} ty)
                 (if (= ty "F")
                   (list 'conv 'float32 (list 'math/Mod (list 'conv 'float64 a) (list 'conv 'float64 b)))
                   (list 'math/Mod a b))
                 (list '% a b))
          :neg (list '- a)
          :inc (list '+ a 1)
          :dec (list '- a 1)
          :and (list 'bit-and a b)
          :or (list 'bit-or a b)
          :xor (list 'bit-xor a b)
          :not (list 'bit-not a)
          :andnot (list 'bit-and-not a b)
          :shl (list '<< a (cnt))
          :shr (list '>> a (cnt))
          :ushr (if (and (go-lit? a) (go-lit? b))
                  ;; both constant: folded here (Go would reject the overflowing constant)
                  (if (= ty "J")
                    (unsigned-bit-shift-right (long a) (bit-and (long b) 63))
                    (long (unchecked-int (unsigned-bit-shift-right (bit-and (long a) 0xffffffff) (bit-and (long b) 31)))))
                  (if (= ty "J")
                    (list 'conv 'int64 (list '>> (list 'conv 'uint64 (if (go-lit? a) (unsigned-lit a 64) a)) (cnt)))
                    (list 'conv 'int32 (list '>> (list 'conv 'uint32 (if (go-lit? a) (unsigned-lit a 32) a)) (cnt)))))))
      ty)))

;; ---------------------------------------------------------------------------------------
;; conversions (§7.4), boxing (§7.6)

(defn box-val [x p]
  (let [w (t/box-of p)]
    (list (m/class-sym (pkg) w (str "_" (nm/method-base "valueOf" (str "(" p ")L" w ";")))) x)))

(defn prim-convert [x from to]
  (let [f (if (#{"B" "S" "C" "I"} from) from from)]
    (cond
      (= from to) x
      (go-lit? x) (if (#{"F" "D"} to) (double x) (fold-int-conv x to))
      (and (number? x) (float? x) (#{"F" "D"} to)) x
      :else
      (case [(if (#{"B" "S" "C" "I"} from) "I" from) to]
        (["F" "I"]) (list (jsym "F2I") x)
        (["F" "J"]) (list (jsym "F2L") x)
        (["D" "I"]) (list (jsym "D2I") x)
        (["D" "J"]) (list (jsym "D2L") x)
        (["F" "S"] ["F" "B"] ["F" "C"]) (prim-convert (list (jsym "F2I") x) "I" to)
        (["D" "S"] ["D" "B"] ["D" "C"]) (prim-convert (list (jsym "D2I") x) "I" to)
        (["J" "S"] ["J" "B"] ["J" "C"]) (list 'conv (m/prim-go to) x)
        (list 'conv (m/prim-go to) x)))))

(defn convert-val [node]
  (let [{:keys [from to]} node
        x (expr (:expr node))
        from (if (= from :null) (:t x) from)]
    (cond
      (= from :null) (v (zero-of to) to)
      (and (t/prim? from) (t/prim? to)) (v (prim-convert (:x x) from to) to)
      (t/prim? from) (v (box-val (:x x) from) (str "L" (t/box-of from) ";") :nn true)
      (t/prim? to)
      (let [p (t/unbox-of from)
            _ (when-not p (fail (str "unboxing " from)))
            w (t/box-of p)
            recv (if (m/pointer-desc? from) (if (:nn x) (:x x) (list (jsym "NN") (:x x))) (:x x))
            un (list (symbol (str "." (nm/method-base (str (t/prim-desc->name p) "Value") (str "()" p)))) recv)]
        (v (prim-convert un p to) to))
      :else (v (coerce (:x x) (:t x) to (:nn x)) to))))

;; ---------------------------------------------------------------------------------------
;; casts and type tests (§5.7)

(defn cast-val [node]
  (let [d (:class node)
        x (expr (:expr node))]
    (cond
      (= :none (:t x)) x
      (= :null (:t x)) (v nil d)
      (env/assignable? (:t x) d) (v (coerce (:x x) (:t x) d (:nn x)) d :nn (:nn x))
      (t/array? d) (v (list (list 'inst (jsym "C2g_CastArray") (gotype d))
                            (coerce (:x x) (:t x) "Ljava/lang/Object;" (:nn x)) (class-val d))
                      d)
      :else (let [n (t/desc->internal d)]
              (v (list (csym n "_Cast") (coerce (:x x) (:t x) "Ljava/lang/Object;" (:nn x))) d)))))

(defn instance-val [node]
  (let [d (:class node)
        x (expr (:expr node))
        o (coerce (:x x) (:t x) "Ljava/lang/Object;" (:nn x))
        cut (some #(not (m/in-world? %)) (m/desc-classes d))]
    (v (cond
         ;; nothing is an instance of a class outside the closed world
         cut (list (jsym "C2g_Discard") o)
         (t/array? d)
         (list '.IsInstance_O__Z (class-val d) o)
         :else
         (let [n (t/desc->internal d)]
           (if (and (m/hand-written? n) (not (contains? (:funcs (:jrt m/*w*)) (str (m/go-name n) "_InstanceOf"))))
             (list '.IsInstance_O__Z (class-val d) o)
             (list (csym n "_InstanceOf") o))))
       "Z")))

;; ---------------------------------------------------------------------------------------
;; calls (§7.6, §5.4, §5.8)

(def ^:private object-helpers
  {["equals" "(Ljava/lang/Object;)Z"] "Equals" ["hashCode" "()I"] "HashCode"
   ["toString" "()Ljava/lang/String;"] "ToString" ["getClass" "()Ljava/lang/Class;"] "GetClass"
   ["notify" "()V"] "Notify" ["notifyAll" "()V"] "NotifyAll" ["wait" "()V"] "Wait"
   ["wait" "(J)V"] "WaitTimeout"})

(defn- args-for [m-desc arg-nodes]
  (let [[ps _] (t/parse-method-desc m-desc)]
    [ps (operands arg-nodes ps)]))

(defn super-struct
  "The embedded struct of the current class's superclass: (.-B t)."
  []
  (let [sup (:super (a/decl (:class *f*)))]
    (list (symbol (str ".-" (m/go-name sup))) (:t *f*))))

(defn invoke-val [node]
  (let [{:keys [kind owner name desc]} node
        mm (:method node)
        mowner (or (:owner mm) owner)
        [ps r] (t/parse-method-desc desc)
        base (nm/method-base name desc)
        rv (fn [x] (v x (if (= r "V") "V" r)))]
    (cond
      ;; an array's clone
      (:array-clone node)
      (let [x (expr (:target node))]
        (v (list '.Copy (if (:nn x) (:x x) (list (jsym "NN") (:x x)))) (or (:cast-to node) (:t x))))

      (= kind :static)
      (let [[_ args] (args-for desc (:args node))]
        (rv (apply list (if (= mowner "java/lang/Object") (fail "static Object method")
                            (m/class-sym (pkg) mowner (str "_" base)))
                   (map :x args))))

      (= kind :special)
      (let [[_ args] (args-for desc (:args node))
            thisx (:x (this-val))]
        (cond
          ;; Interface.super.m(): the default method's function
          (m/interface? mowner)
          (rv (apply list (m/class-sym (pkg) mowner (str "_" base)) thisx (map :x args)))
          (= mowner "java/lang/Object")
          (case name
            "toString" (rv (list (jsym "Object_toString") thisx))
            "hashCode" (rv (list (jsym "IdentityHash") thisx))
            "equals" (rv (list '== (coerce thisx (str "L" (:class *f*) ";") "Ljava/lang/Object;" true) (:x (first args))))
            "clone" (rv (list (jsym "C2g_ObjectClone") thisx))
            "getClass" (rv (list (jsym "GetClass") thisx))
            (fail (str "super call of Object." name)))
          (m/hand-written? mowner)
          (let [ms (arbace.c2g.jrt/struct-methods (:jrt m/*w*) (m/go-name mowner))]
            (if (contains? ms (nm/impl-name base))
              (rv (apply list (symbol (str ".Impl_" base)) (super-struct) thisx (map :x args)))
              (rv (apply list (symbol (str "." base)) (super-struct) (map :x args)))))
          :else
          (rv (apply list (symbol (str ".Impl_" base)) (super-struct) thisx (map :x args)))))

      ;; private methods are not virtual
      (and mm (m/private? mm) (m/translated? mowner))
      (let [[tv & args] (operands (cons (:target node) (:args node)) (cons nil ps))
            recv (:x tv)]
        (if (m/leaf? mowner)
          (rv (apply list (symbol (str "." base)) (if (:nn tv) recv (list (jsym "NN") recv)) (map :x args)))
          (rv (apply list (symbol (str ".Impl_" base))
                     (if (= recv (:this *f*)) (:t *f*) (list (symbol (str ".Self_" (m/go-name mowner))) recv))
                     recv (map :x args)))))

      :else
      (let [[tv & args] (operands (cons (:target node) (:args node)) (cons nil ps))
            st (:t tv)
            recv (:x tv)]
        (cond
          ;; Object's methods on an Object-typed receiver, or on an array: jrt's helpers
          (or (object-like? st) (t/array? st))
          (if-let [h (object-helpers [name desc])]
            (rv (apply list (jsym h) (coerce recv st "Ljava/lang/Object;" (:nn tv)) (map :x args)))
            (fail (str "Object method " name desc " on an Object")))
          (#{["wait" "()V"] ["wait" "(J)V"] ["notify" "()V"] ["notifyAll" "()V"]} [name desc])
          (rv (apply list (jsym (object-helpers [name desc])) (coerce recv st "Ljava/lang/Object;" (:nn tv)) (map :x args)))
          (m/pointer-desc? st)
          (rv (apply list (symbol (str "." base)) (if (:nn tv) recv (list (jsym "NN") recv)) (map :x args)))
          :else
          (rv (apply list (symbol (str "." base)) recv (map :x args))))))))

(defn ctor-args
  "The Go arguments of constructor ctor of class cn for a :new or :ctor-call node: the outer
  instance, the superclass's outer instance, the enum constant's name and ordinal, the
  declared arguments, the captured locals."
  [node cn]
  (let [d (a/decl cn)
        info (m/info cn)
        [ps _] (t/parse-method-desc (:desc (:ctor node)))
        lead (concat (when (:outer-instance? info) [[(:outer node) (str "L" (:outer info) ";")]])
                     (when-let [so (:super-outer node)] [[so (:type so)]]))
        vals (operands (concat (map first lead) (:args node))
                       (concat (map second lead) ps))
        [lead-v arg-v] (split-at (count lead) vals)
        enum (when-let [ec (:enum-const node)]
               [(use-sym! (string-lit (:name ec))) (:ordinal ec)])
        caps (when (and d (#{:local :anon} (:nesting d)))
               (for [b (:captures @(:state d))]
                 (:x (local-val {:op :local :b b}))))]
    (concat (map :x lead-v) enum (map :x arg-v) caps)))

(defn new-val [node]
  (let [cn (:class node)]
    (if (= cn "java/lang/Object")
      (v (list (jsym "Object_New")) "Ljava/lang/Object;" :nn true)
      (let [real (e/ctor-real-desc cn (:ctor node))]
        (v (apply list (symbol (str (if (= (m/pkg cn) (pkg)) "" (str (nm/pkg-name (m/pkg cn)) "/"))
                                    (nm/new-name (m/go-name cn) real)))
                  (ctor-args node cn))
           (str "L" cn ";") :nn true)))))

;; ---------------------------------------------------------------------------------------
;; arrays (§5.9)

(defn array-val [node]
  (let [d (:type node)
        et (t/elem-type d)]
    (case (:op node)
      :new-array
      (let [dims (operands (:dims node) (repeat "I"))]
        (if (> (count dims) 1)
          (v (apply list (jsym "NewMultiArray") (class-val d) (map :x dims)) d :nn true)
          (v (if (t/prim? et)
               (list (jsym (str "New" (m/prim-array-go et))) (:x (first dims)))
               (list (jsym "NewRefArray") (class-val et) (:x (first dims))))
             d :nn true)))
      :array-init
      (let [elems (operands (:elems node) (repeat (if (t/prim? et) et "Ljava/lang/Object;")))]
        (v (if (t/prim? et)
             (apply list (jsym (str (subs (m/prim-array-go et) 0 (- (count (m/prim-array-go et)) 5)) "ArrayOf")) (map :x elems))
             (apply list (jsym "RefArrayOf") (class-val et) (map :x elems)))
           d :nn true)))))

;; ---------------------------------------------------------------------------------------
;; string concatenation (§7.5)

(defn java-str-val [node]
  (let [parts (:parts node)
        dyn (filter :node parts)
        vals (operands (map :node dyn) (repeat nil))
        vals (zipmap (map-indexed (fn [i _] i) dyn) vals)
        idx (atom -1)
        xs (for [p parts]
             (cond
               (:literal p) (use-sym! (string-lit (:literal p)))
               (:tag-const p) (use-sym! (string-lit (:tag-const p)))
               :else (let [val (get vals (swap! idx inc))
                           ty (a/value-type (:type (:node p)))]
                       (case ty
                         ("I" "S" "B") (list (jsym "StrOfInt") (if (= ty "I") (:x val) (list 'conv 'int32 (:x val))))
                         "J" (list (jsym "StrOfLong") (:x val))
                         "C" (list (jsym "StrOfChar") (:x val))
                         "Z" (list (jsym "StrOfBool") (:x val))
                         "F" (list (jsym "StrOfFloat") (:x val))
                         "D" (list (jsym "StrOfDouble") (:x val))
                         "Ljava/lang/String;" (:x val)
                         (list (jsym "StrOfObj") (coerce (:x val) (:t val) "Ljava/lang/Object;" (:nn val)))))))]
    (v (apply list (jsym "Concat") xs) "Ljava/lang/String;" :nn true)))

;; ---------------------------------------------------------------------------------------
;; statements and contexts

(defn- target-info [target] (get (:targets *f*) (:id target)))

(defn- code-jump!
  "A jump (return, break, recur) leaving the Go function literal of a try or locking body: it
  is recorded in the innermost frame with a control code and performed after the literal
  returns (§7.9.3). jump is {:kind :return|:break|:recur ...}."
  [jump]
  (let [fr (:try-frame *f*)
        codes (:codes fr)
        k (+ 2 (count @codes))]
    (swap! codes conj (assoc jump :code k))
    (emit! (list 'return k (if (:rv jump) (:rv jump) (:rv-zero fr))))))

(defn- outside? [target-fn] (not= target-fn (:fn-id *f*)))

(defn- method-return!
  "A return from the method (::void for a void method's), a control code inside a try body."
  [val-form]
  (if (outside? (:method-fn *f*))
    (code-jump! {:kind :return :rv (if (= ::void val-form) nil val-form)})
    (emit! (if (= ::void val-form) (list 'return) (list 'return val-form)))))

(defn- set-static! [node]
  (let [f (:field node)
                          o (or (:declarer f) (:owner f))
                          guard (guarded-static? node)

                          x (let [xv (v (expr-as (:val node) (:desc f)) (:desc f))]
                              (:x (if (and guard (not (stable? (:val node)))) (hoist-value xv) xv)))
                          _ (when guard (emit! (class-init-call o)))
                          place (static-place f)]
                      (emit! (if (volatile-field? f) (volatile-write place (:desc f) x) (list 'set! place x)))))

(defn- dropped-store?
  "A store into a field that the closed world drops (its type is outside it) that can go:
  a static initializer's store into a field of its class (serialization's
  serialPersistentFields: nothing can read the field), or the store of the constant null
  into a field of this object or a static field (PrintWriter's psOut, a PrintStream: the
  field can hold nothing else, no instance of its type exists)."
  [node]
  (let [f (:field node)]
    (or (and (:clinit *f*) (= :set-static (:op node))
             (let [o (or (:declarer f) (:owner f))]
               (and (= o (:class *f*)) (not (m/desc-in-world? (:desc f))))))
        (and (#{:set-field :set-static} (:op node))
             (not (m/desc-in-world? (:desc f)))
             (let [vn (acc/unaccess (:val node))] (and (= :const (:op vn)) (nil? (:val vn))))
             (or (= :set-static (:op node)) (= :this-path (:op (acc/unaccess (:target node))))))
        ;; a store into a field of an erased type (c2g/erase, proposed amendment B2): the field
        ;; does not exist in Go, and the value, being of an erased type, has no effects to keep
        (and (#{:set-static :set-field} (:op node))
             (m/erased-desc? (:desc f))))))

(defn- set-field! [node]
  (let [f (:field node)
        [tv vv] (target-operands [(:target node) (:val node)] [nil (:desc f)])
        place (field-place tv f)]
    (emit! (if (volatile-field? f) (volatile-write place (:desc f) (:x vv)) (list 'set! place (:x vv))))))

(defn stmt!
  "Translates node in statement context."
  [node]
  (let [node (acc/unaccess node)]
    (if-let [why (and (not (dropped-store? node)) (missing-reason node))]
      (emit! (missing-expr why "V"))
      (case (:op node)
        (:const :local :this-path :class-lit :none) nil
        :set-static (if (dropped-store? node) nil (set-static! node))
        :set-field (if (dropped-store? node) nil (set-field! node))
        :do (do (run! stmt! (:statements node)) (stmt! (:ret node)))
        :let (do (doseq [[b init] (:bindings node)]
                   (let [s (binding-sym b)]
                     (if (and (= :const (:op init)) (:cval b))
                       nil
                       (if (simple? init)
                         (decl! s (gotype (:type b)) (expr-as init (:type b)))
                         (do (decl! s (gotype (:type b)) nil)
                             (assign! init {:var s :desc (:type b)}))))))
                 (stmt! (:body node)))
        :if (translate-ctx node {:k :stmt})
        :set-local (let [b (:b node)
                         x (expr-as (:val node) (:type b))]
                     (emit! (list 'set! (binding-sym b) x)))
        :get-static (do (when (guarded-static? node) (emit! (class-init-call (or (:declarer (:field node)) (:owner (:field node))))))
                        nil)
        :aset (if (m/erased-desc? (vt (:array node)))
                ;; a store into an array of an erased type: the array is nil (c2g/erase)
                nil
                (let [[a i val] (operands [(:array node) (:index node) (:val node)]
                                        [nil "I" (let [et (t/elem-type (vt (:array node)))] (if (t/prim? et) et "Ljava/lang/Object;"))])
                    et (t/elem-type (:t a))]
                (emit! (if (t/prim? et)
                         (list 'aset (list '.-A (:x a)) (:x i) (:x val))
                         (list '.Store (:x a) (:x i) (:x val))))))
        (:invoke :new :var-invoke) (let [x (expr-op node)] (emit! (:x x)))
        :throw (let [x (expr-as (:expr node) "Ljava/lang/Throwable;")]
                 (emit! (list 'panic (list (jsym "Thrown") x))))
        :return (if (:val node)
                  (if (= "V" (:method-ret *f*))
                    (do (stmt! (:val node)) (method-return! ::void))
                    (let [x (expr-as (:val node) (:method-ret *f*))]
                      (method-return! x)))
                  (method-return! ::void))
        (:loop :label :try :monitor :switch :if-instance :for-each :break :recur :assert)
        (translate-ctx node {:k :stmt})
        :ctor-call ((:ctor-call-fn *f*) node)
        (:and :or) (if (= "Z" (:type node))
                     (emit! (list 'when (cond-expr node)))
                     (translate-ctx node {:k :stmt}))
        ;; any other expression: evaluate for its effects
        (let [x (expr node)]
          (when (seq? (:x x))
            (emit! (if (go-call? (:x x)) (:x x) (list 'set! '_ (:x x))))))))))

(def ^:private go-heads
  '#{not == != < > <= >= and or + - * / % << >> bit-and bit-or bit-xor bit-not bit-and-not conv
     aget len cap addr lit assert subslice zero make new fn inst})

(defn go-call?
  "Is Go form x a call (which may stand as a statement)?"
  [x]
  (and (seq? x)
       (let [h (first x)]
         (cond (seq? h) true ; a call of a call's result, or of an instantiation
               (symbol? h) (and (not (contains? go-heads h))
                                (not (str/starts-with? (name h) ".-")))
               :else false))))

(defn- assign-value! [x ctx]
  (case (:k ctx)
    :stmt nil
    :assign (emit! (list 'set! (:var ctx) x))
    :return (method-return! x)))

(defn assign!
  "Translates node storing its value into ctx's variable."
  [node ctx]
  (translate-ctx node (assoc ctx :k :assign)))

(defn ret!
  "Translates node as the value of the current method (return context)."
  [node]
  (translate-ctx node {:k :return :desc (:method-ret *f*)}))

(defn- ctx-desc [ctx] (or (:desc ctx) (:method-ret *f*)))

(declare translate-try translate-monitor translate-loop translate-label translate-switch
         translate-for-each translate-if-instance translate-and-or)

(defn translate-ctx
  "Translates node in context ctx: {:k :stmt}, {:k :assign :var sym :desc d}, {:k :return}."
  [node ctx]
  (let [node (acc/unaccess node)
        k (:k ctx)]
    (cond
      (missing-reason node)
      (if (= k :stmt)
        (stmt! node)
        (let [to (ctx-desc ctx)]
          (assign-value! (missing-expr (missing-reason node) (if (= to "V") "Ljava/lang/Object;" to)) ctx)))

      (= k :stmt)
      (case (:op node)
        :if (let [c (cond-expr (:test node))
                  th (with-block #(translate-ctx (:then node) ctx))
                  el (with-block #(translate-ctx (:else node) ctx))]
              (emit! (if (empty? el)
                       (apply list 'when c th)
                       (list 'if c (cons 'do th) (cons 'do el)))))
        :loop (translate-loop node ctx)
        :label (translate-label node ctx)
        :try (translate-try node ctx)
        :monitor (translate-monitor node ctx)
        :switch (translate-switch node ctx)
        :if-instance (translate-if-instance node ctx)
        :for-each (translate-for-each node ctx)
        (:break :recur) (translate-ctx node {:k :jump})
        :assert (let [c (cond-expr (:test node))
                      msg (when (:msg node) (expr-as (:msg node) (:msg-desc node)))
                      ex (if msg
                           (list (jsym (str "AssertionError_New_" (nm/desc-code (:msg-desc node)))) msg)
                           (list (jsym "AssertionError_New")))]
                  (emit! (list 'when (list 'and (list 'not (jsym "C2g_AssertionsDisabled")) (list 'not c))
                               (list 'panic (list (jsym "Thrown") ex)))))
        (:and :or) (translate-and-or node ctx)
        (stmt! node))

      (= k :jump)
      (case (:op node)
        :break (let [ti (target-info (:target node))]
                 (when-not ti (fail "break target not in scope"))
                 (let [tctx (:ctx ti)]
                   (if (= :return (:k tctx))
                     ;; a loop in return context: its break value is the method's value
                     (translate-ctx (or (:val node) {:op :const :type :null :val nil}) tctx)
                     (do (if (:val node)
                           (translate-ctx (:val node) (if (= :assign (:k tctx)) tctx {:k :stmt}))
                           nil)
                         (if (outside? (:fn-id ti))
                           (code-jump! {:kind :break :target (:target node)})
                           (do (swap! (:label-used *f*) conj (:label ti))
                               (emit! (list 'break (:label ti)))))))))
        :recur (let [ti (target-info (:target node))]
                 (when-not ti (fail "recur target not in scope"))
                 (let [vars (:vars ti)
                       vals (operands (:args node) (:types ti))]
                   (cond
                     (= 1 (count vars)) (emit! (list 'set! (first vars) (:x (first vals))))
                     (seq vars) (emit! (list 'set! (apply list 'values vars) (apply list 'values (map :x vals))))
                     :else nil)
                   (if (outside? (:fn-id ti))
                     (code-jump! {:kind :recur :target (:target node)})
                     (do (swap! (:label-used *f*) conj (:label ti))
                         (emit! (list 'continue (:label ti))))))))

      :else
      (case (:op node)
        :if (let [c (cond-expr (:test node))
                  th (with-block #(translate-ctx (:then node) ctx))
                  el (with-block #(translate-ctx (:else node) ctx))]
              (emit! (list 'if c (cons 'do th) (cons 'do el))))
        :do (do (run! stmt! (:statements node)) (translate-ctx (:ret node) ctx))
        :let (do (stmt! (assoc node :body {:op :none :type :none}))
                 (translate-ctx (:body node) ctx))
        :loop (translate-loop node ctx)
        :label (translate-label node ctx)
        :try (translate-try node ctx)
        :monitor (translate-monitor node ctx)
        :switch (translate-switch node ctx)
        :if-instance (translate-if-instance node ctx)
        :for-each (translate-for-each node ctx)
        (:break :recur :return :throw) (stmt! node)
        (:and :or) (translate-and-or node ctx)
        :set-local (do (stmt! node) (translate-ctx {:op :local :b (:b node) :type (:type (:b node))} ctx))
        :set-field (let [f (:field node)
                         [tv vv] (target-operands [(:target node) (:val node)] [nil (:desc f)])
                         vv (hoist-value vv)
                         place (field-place tv f)]
                     (emit! (if (volatile-field? f) (volatile-write place (:desc f) (:x vv)) (list 'set! place (:x vv))))
                     (assign-value! (coerce (:x vv) (:desc f) (ctx-desc ctx) false) ctx))
        :set-static (let [f (:field node)
                          x (hoist-value (v (expr-as (:val node) (:desc f)) (:desc f)))]
                      (stmt! (assoc node :val {:op ::go :x (:x x) :t (:desc f)}))
                      (assign-value! (coerce (:x x) (:desc f) (ctx-desc ctx) false) ctx))
        :get-static (let [f (:field node)
                          o (or (:declarer f) (:owner f))
                          _ (when (guarded-static? node) (emit! (class-init-call o)))
                          place (static-place f)
                          x (if (volatile-field? f) (volatile-read place (:desc f)) place)]
                      (assign-value! (coerce x (:desc f) (ctx-desc ctx) false) ctx))
        :aset (let [et (t/elem-type (vt (:array node)))
                    [a i val] (operands [(:array node) (:index node) (:val node)] [nil "I" et])
                    val (hoist-value val)]
                (emit! (if (t/prim? et)
                         (list 'aset (list '.-A (:x a)) (:x i) (:x val))
                         (list '.Store (:x a) (:x i) (coerce (:x val) et "Ljava/lang/Object;" false))))
                (assign-value! (coerce (:x val) et (ctx-desc ctx) false) ctx))
        :assert (do (translate-ctx node {:k :stmt}) (assign-value! nil ctx))
        :ctor-call (stmt! node)
        (let [x (expr-op node)
              to (ctx-desc ctx)]
          (if (= "V" (:type node))
            (do (emit! (:x x)) (assign-value! (when-not (= :return k) nil) ctx))
            (assign-value! (if (= to "V") (:x x) (coerce (:x x) (:t x) to (:nn x))) ctx)))))))

;; Clojure's and/or with values (on references)
(defn translate-and-or [node ctx]
  (let [args (:args node)
        t (vt node)
        ctx (if (= (:k ctx) :return) ctx ctx)]
    (if (= (:k ctx) :stmt)
      (emit! (list 'when (cond-expr node)))
      (let [s (tmp)
            d (if (#{:null :none} t) "Ljava/lang/Object;" t)]
        (decl! s (gotype d) nil)
        (use-sym! s)
        ;; evaluate the operands in order until one decides
        (letfn [(go [[n & more]]
                  (assign! n {:var s :desc d})
                  (when (seq more)
                    (let [c (cond-expr {:op ::go :x s :t d :type d})
                          rest-forms (with-block #(go more))]
                      (emit! (if (= :and (:op node))
                               (apply list 'when c rest-forms)
                               (apply list 'when (list 'not c) rest-forms))))))]
          (go args))
        (assign-value! (coerce s d (ctx-desc ctx) false) ctx)))))

;; ---------------------------------------------------------------------------------------
;; loops, labels (§7.7)

(defn new-label [] (keyword (str "L" (new-counter! :label))))

(defn translate-loop [node ctx]
  (let [bs (:bindings node)
        vars (vec (for [[b init] bs]
                    (let [s (binding-sym b)]
                      (if (simple? init)
                        (decl! s (gotype (:type b)) (expr-as init (:type b)))
                        (do (decl! s (gotype (:type b)) nil)
                            (assign! init {:var s :desc (:type b)})))
                      (use-sym! s)
                      s)))
        lbl (new-label)
        ti {:fn-id (:fn-id *f*) :label lbl :ctx ctx :vars vars :types (mapv (comp :type first) bs)}
        body (binding [*f* (assoc-in *f* [:targets (:id (:target node))] ti)]
               (with-block (fn []
                             (translate-ctx (:body node) ctx)
                             ;; a tail value ends the loop
                             (when-not (or (= :return (:k ctx)) (= :none (:type (:body node))))
                               (swap! (:label-used *f*) conj lbl)
                               (emit! (list 'break lbl))))))]
    (emit! (if (contains? @(:label-used *f*) lbl)
             (list 'label lbl (apply list 'while true body))
             (apply list 'while true body)))))

(defn translate-label [node ctx]
  (let [lbl (new-label)
        ti {:fn-id (:fn-id *f*) :label lbl :ctx ctx}
        body (binding [*f* (assoc-in *f* [:targets (:id (:target node))] ti)]
               (with-block #(translate-ctx (:body node) ctx)))]
    (if (contains? @(:label-used *f*) lbl)
      (emit! (list 'label lbl (list 'switch (apply list 'default body))))
      (swap! *block* into body))))

(defn translate-for-each [node ctx]
  (let [lbl (new-label)
        b (:b node)
        bs (binding-sym b)
        ti {:fn-id (:fn-id *f*) :label lbl :ctx ctx :vars [] :types []}]
    (if (:array node)
      (let [h (:hidden node)
            arr (binding-sym (:arr h)) len (binding-sym (:len h)) i (binding-sym (:i h))
            coll (expr-as (:coll node) (vt (:coll node)))]
        (decl! arr (gotype (vt (:coll node))) coll)
        (decl! len 'int32 (list 'conv 'int32 (list 'len (list '.-A (use-sym! arr)))))
        (decl! i 'int32 0)
        (use-sym! len) (use-sym! i)
        (let [body (binding [*f* (assoc-in *f* [:targets (:id (:target node))] ti)]
                     (with-block (fn []
                                   (decl! bs (gotype (:type b)) (expr-as (:elem node) (:type b)))
                                   (translate-ctx (:body node) {:k :stmt}))))]
          (emit! (list 'label lbl (apply list 'for [] (list '< i len) (list 'inc! i) body)))))
      (let [it (binding-sym (:it (:hidden node)))
            itx (:x (expr (:iterator node)))]
        (decl! it (gotype "Ljava/util/Iterator;") itx)
        (use-sym! it)
        (let [body (binding [*f* (assoc-in *f* [:targets (:id (:target node))] ti)]
                     (with-block (fn []
                                   (decl! bs (gotype (:type b)) (expr-as (:elem node) (:type b)))
                                   (translate-ctx (:body node) {:k :stmt}))))]
          (emit! (list 'label lbl (apply list 'while (list '.HasNext__Z it) body))))))
    (when-not (contains? @(:label-used *f*) lbl)
      ;; unlabel: gc rejects an unused label
      (swap! *block* (fn [items] (conj (pop items) (nth (peek items) 2)))))
    (case (:k ctx)
      :assign (emit! (list 'set! (:var ctx) (coerce nil :null (:desc ctx) false)))
      :return (method-return! (coerce nil :null (:method-ret *f*) false))
      nil)))

;; ---------------------------------------------------------------------------------------
;; if-instance, switch (§7.8)

(defn- pattern-bindings [p]
  (concat (when-let [b (:b p)] [b]) (mapcat #(pattern-bindings (:pattern %)) (:comps p))))

(defn- record-pattern? [p] (= :record (:kind p)))

(defn- match-pattern!
  "Emits the statements matching Go value x (a stable expression of Java type vt) against
  pattern p: the type tests, the record components read through their accessors, the
  bindings assigned (declared before by declare-pattern!); calls k to emit what follows a
  match, inside the tests."
  [p x vt k]
  (let [xn {:op ::go :x x :t vt}
        inner (fn []
                (case (:kind p)
                  :type (do (when-let [b (:b p)]
                              (let [cv (cast-val {:class (:class p) :expr xn})]
                                (emit! (list 'set! (binding-sym b) (coerce (:x cv) (:t cv) (:type b) false)))))
                            (k))
                  :record (let [cv (cast-val {:class (:class p) :expr xn})
                                r (tmp)]
                            (decl! r (gotype (:class p)) (:x cv))
                            (use-sym! r)
                            ((fn comps [cs]
                               (if (empty? cs)
                                 (k)
                                 (let [{:keys [accessor pattern]} (first cs)
                                       ct (subs (:desc accessor) 2)
                                       c (tmp)
                                       cvv (invoke-val {:op :invoke :kind :virtual :owner (:owner accessor)
                                                        :name (:name accessor) :desc (:desc accessor)
                                                        :target {:op ::go :x r :t (:class p) :nn true} :args []})]
                                   (decl! c (gotype ct) (:x cvv))
                                   (use-sym! c)
                                   (match-pattern! pattern c ct #(comps (rest cs))))))
                             (:comps p)))))]
    (if (:test p)
      (emit! (list 'if (:x (instance-val {:class (:class p) :expr xn})) (cons 'do (with-block inner))))
      (inner))))

(defn- declare-pattern!
  "Declares the bindings of pattern p and a flag; returns the flag, set when the value
  matches (after match-pattern!)."
  [p x vt]
  (doseq [b (pattern-bindings p)]
    (decl! (binding-sym b) (gotype (:type b)) nil))
  (let [ok (tmp)]
    (decl! ok 'bool false)
    (use-sym! ok)
    (match-pattern! p x vt #(emit! (list 'set! ok true)))
    ok))

(defn translate-if-instance [node ctx]
  (if (record-pattern? (:pattern node))
    (let [x (hoist-value (expr (:expr node)))
          ok (declare-pattern! (:pattern node) (:x x) (:t x))
          then (with-block #(translate-ctx (:then node) ctx))
          else (with-block #(translate-ctx (:else node) ctx))]
      (emit! (list 'if ok (cons 'do then) (cons 'do else))))
  (let [x (hoist-value (expr (:expr node)))
        p (:pattern node)
        xn {:op ::go :x (:x x) :t (:t x) :nn (:nn x)}
        test (:x (instance-val {:class (:class p) :expr xn}))
        then (with-block (fn []
                           (when-let [b (:b p)]
                             (let [cv (cast-val {:class (:class p) :expr xn})]
                               (decl! (binding-sym b) (gotype (:type b)) (coerce (:x cv) (:t cv) (:type b) false))))
                           (translate-ctx (:then node) ctx)))
        else (with-block #(translate-ctx (:else node) ctx))]
    (emit! (list 'if test (cons 'do then) (cons 'do else))))))

(defn- enum-ordinal [e c]
  (if-let [d (a/decl e)]
    (first (keep-indexed (fn [i k] (when (= c (:name k)) i)) (:constants d)))
    (.ordinal ^Enum (Enum/valueOf (env/load-class e) c))))

(defn translate-switch [node ctx]
  (let [kind (:kind node)
        cases (:cases node)
        default-ci (some (fn [[ci c]] (when (some :default (:labels c)) ci)) (map-indexed vector cases))
        arm (fn [body] (with-block #(translate-ctx body ctx)))
        default-forms (cond default-ci (arm (:body (nth cases default-ci)))
                            (:default node) (arm (:default node))
                            :else (with-block #(case (:k ctx)
                                                 :assign (emit! (list 'set! (:var ctx) (coerce nil :null (:desc ctx) false)))
                                                 :return (method-return! (coerce nil :null (:method-ret *f*) false))
                                                 nil)))]
    (case kind
      :int
      (let [sel (expr-as (:sel node) "I")]
        (emit! (apply list 'switch sel
                      (concat
                        (for [c cases
                              :let [ls (remove :default (:labels c))]
                              :when (seq ls)]
                          (apply list 'case (vec (map (fn [l] (let [x (:const l)] (if (char? x) (int x) (long x)))) ls))
                                 (arm (:body c))))
                        [(apply list 'default default-forms)]))))
      :string
      (let [s (hoist-value (expr (:sel node)))
            k (tmp)
            strs (vec (for [[ci c] (map-indexed vector cases) l (:labels c) :when (contains? l :const)] [(:const l) ci]))
            buckets (group-by #(java-hash (first %)) (map-indexed (fn [i [sv ci]] [sv i ci]) strs))]
        (decl! k 'int32 -1)
        (use-sym! k)
        (emit! (apply list 'switch (list '.HashCode__I (list (jsym "NN") (:x s)))
                      (for [[h entries] (sort-by key buckets)]
                        (list 'case [(long h)]
                              (apply list 'cond
                                     (concat (mapcat (fn [[sv i _]] [(list '.Equals_O__Z (:x s) (use-sym! (string-lit sv)))
                                                                     (list 'set! k i)])
                                                     entries)))))))
        (emit! (apply list 'switch k
                      (concat
                        (for [[ci c] (map-indexed vector cases)
                              :let [is (keep-indexed (fn [i [_ cj]] (when (= cj ci) i)) strs)]
                              :when (seq is)]
                          (apply list 'case (vec is) (arm (:body c))))
                        [(apply list 'default default-forms)]))))
      :enum
      (let [e (t/desc->internal (:sel-type node))
            sel (expr (:sel node))
            ord (if (m/leaf? e) (list '.-F_ordinal (list (jsym "NN") (:x sel)))
                    (list '.-F_ordinal (list '.Self_Enum (:x sel))))]
        (emit! (apply list 'switch ord
                      (concat
                        (for [c cases :let [ls (remove :default (:labels c))] :when (seq ls)]
                          (apply list 'case (vec (map #(enum-ordinal e (:enum %)) ls)) (arm (:body c))))
                        [(apply list 'default default-forms)]))))
      :pattern
      (let [sel (hoist-value (expr (:sel node)))
            st (:t sel)
            null-ci (some (fn [[ci c]] (when (some :null (:labels c)) ci)) (map-indexed vector cases))
            enum? (and (t/class-desc? st) (m/has? (:flags (m/info (t/desc->internal st))) 0x4000))
            selv {:op ::go :x (:x sel) :t st}
            chain (fn chain [[[ci c] & more]]
                    (if (nil? ci)
                      default-forms
                      (let [l (first (remove #(or (:null %) (:default %)) (:labels c)))]
                        (if (nil? l)
                          (chain more)
                          (if (and (:pattern l) (record-pattern? (:pattern l)))
                            (with-block
                              (fn []
                                (let [ok (declare-pattern! (:pattern l) (:x sel) st)
                                      body (if-let [g (:guard c)]
                                             (with-block #(emit! (list 'if (cond-expr g) (cons 'do (arm (:body c))) (cons 'do (chain more)))))
                                             (arm (:body c)))]
                                  (emit! (list 'if ok (cons 'do body) (cons 'do (chain more)))))))
                          (let [p (:pattern l)
                                label-test (fn [l]
                                             (cond
                                               (:enum l) (list '== (:x sel) (m/class-sym (pkg) (t/desc->internal st) (str "_" (:enum l))))
                                               :else (let [c (:const l)]
                                                       (if (string? c)
                                                         (list '.Equals_O__Z (use-sym! (string-lit c)) (coerce (:x sel) st "Ljava/lang/Object;" false))
                                                         (missing-expr "a constant pattern label" "Z")))))
                                test (cond
                                       p (if (:test p)
                                           (:x (instance-val {:class (:class p) :expr selv}))
                                           (list '!= (:x sel) nil))
                                       ;; a case of several constants: any of them
                                       :else (let [ts (map label-test (remove #(or (:null %) (:default %)) (:labels c)))]
                                               (if (next ts) (apply list 'or ts) (first ts))))
                                body (with-block (fn []
                                                   (when-let [b (and p (:b p))]
                                                     (let [cv (cast-val {:class (:class p) :expr selv})]
                                                       (decl! (binding-sym b) (gotype (:type b)) (coerce (:x cv) (:t cv) (:type b) false))))
                                                   (if-let [g (:guard c)]
                                                     (let [gc (cond-expr g)]
                                                       (emit! (list 'if gc (cons 'do (arm (:body c))) (cons 'do (chain more)))))
                                                     (swap! *block* into (arm (:body c))))))]
                            [(list 'if test (cons 'do body) (cons 'do (chain more)))]))))))]
        (if null-ci
          (emit! (list 'if (list '== (:x sel) nil) (cons 'do (arm (:body (nth cases null-ci))))
                       (cons 'do (chain (map-indexed vector cases)))))
          (do (emit! (list 'when (list '== (:x sel) nil) (list 'panic (list (jsym "NPE")))))
              (swap! *block* into (chain (map-indexed vector cases))))))))
  (when (= :return (:k ctx))
    nil))

;; ---------------------------------------------------------------------------------------
;; try, catch, finally (§7.9), monitors (§8.1)

(defn- try-literal
  "Runs body-fn as the body of a Go function literal called in place, with control codes for
  the jumps out of it. Returns {:call form :codes [...] :has-exc bool :rv? bool} and the
  literal's statement forms."
  [body-fn {:keys [catch? monitor]}]
  (let [fid (gensym "fn")
        codes (atom [])
        rv-desc (:method-ret *f*)
        rv-zero (when-not (= "V" rv-desc) (coerce nil :null rv-desc false))
        frame {:codes codes :rv-zero rv-zero}
        forms (binding [*f* (assoc *f* :fn-id fid :try-frame frame)]
                (with-block body-fn))
        codes @codes
        rv? (and (not= "V" rv-desc) (some #(= :return (:kind %)) codes))
        ctl? (seq codes)]
    {:forms forms :codes codes :rv? rv? :ctl? ctl?}))

(defn- dispatch-codes!
  "After a try literal: performs the pending jump of each control code in the enclosing
  function."
  [ctl rv codes]
  (when (seq codes)
    (use-sym! ctl)
    (let [clause (fn [c]
                   (list 'case [(:code c)]
                         (cons 'do
                               (with-block
                                 (fn []
                                   (case (:kind c)
                                     :return (method-return! (if (= "V" (:method-ret *f*)) ::void (use-sym! rv)))
                                     :break (translate-ctx {:op :break :target (:target c) :val nil :type :none} {:k :jump})
                                     :recur (let [ti (target-info (:target c))]
                                              (if (outside? (:fn-id ti))
                                                (code-jump! {:kind :recur :target (:target c)})
                                                (do (swap! (:label-used *f*) conj (:label ti))
                                                    (emit! (list 'continue (:label ti))))))))))))]
      (emit! (apply list 'switch ctl (map clause codes))))))

(defn- literal-form
  "The Go function literal and its call: results ctl, rv (when used), exc (when catching)."
  [{:keys [forms codes rv? ctl?]} catch? lit-rv-desc]
  (let [results (cond-> []
                  ctl? (conj (tag 'ctl 'int32))
                  ctl? (conj (tag 'rv (if rv? (gotype lit-rv-desc) 'bool)))
                  catch? (conj (tag 'exc (jsym "Throwable_I"))))
        ;; returns inside forms were written (return k rv): rewrite to the result shape
        fix (fn fix [f]
              (cond
                (and (seq? f) (= 'return (first f)) (= 3 (count f)) (number? (second f)))
                (apply list 'return (cond-> [(second f) (if rv? (nth f 2) false)] catch? (conj nil)))
                (and (seq? f) (= 'fn (first f))) f
                (seq? f) (with-meta (apply list (map fix f)) (meta f))
                (vector? f) (with-meta (mapv fix f) (meta f))
                :else f))
        body (map fix forms)
        tail (cond-> [] ctl? (conj 0 (if rv? (coerce nil :null lit-rv-desc false) false)) catch? (conj nil))]
    (list (concat (if (seq results) (list 'fn [] :results results) (list 'fn []))
                  (when catch? [(list 'defer (list (jsym "Catch") (list 'addr 'exc)))])
                  body
                  (when (seq tail) [(apply list 'return tail)])))))

(defn translate-try [node ctx]
  (let [fin (:finally node)
        nfin (:normal-finally node)
        ;; a try in return context gives its value through a variable
        [ctx after] (if (= :return (:k ctx))
                      (let [d (:method-ret *f*)]
                        (if (= "V" d)
                          [{:k :stmt} nil]
                          (let [s (tmp)] (decl! s (gotype d) nil) (use-sym! s)
                            [{:k :assign :var s :desc d} s])))
                      [ctx nil])
        rv-desc (:method-ret *f*)
        inner (fn []
                ;; the body and its handlers
                (let [lit (try-literal #(translate-ctx (:body node) ctx) {:catch? true})
                      call (literal-form lit true rv-desc)
                      ctl (when (:ctl? lit) (tmp)) rv (when (:ctl? lit) (tmp)) exc (tmp)]
                  (if (:ctl? lit)
                    (swap! *block* conj [:let (list 'values ctl rv exc) call])
                    (swap! *block* conj [:let exc call]))
                  (use-sym! exc)
                  (when nfin
                    (let [nf (with-block #(stmt! nfin))]
                      (emit! (apply list 'when (list '== exc nil) nf))))
                  (when (seq (:catches node))
                    (let [clauses (mapcat
                                    (fn [c]
                                      (let [b (:b c)
                                            test (if (some #(= % "java/lang/Throwable") (:classes c))
                                                   true
                                                   (let [ts (for [cls (:classes c)]
                                                              (if (m/in-world? cls)
                                                                (list (csym cls "_InstanceOf") exc)
                                                                false))]
                                                     (if (= 1 (count ts)) (first ts) (apply list 'or ts))))
                                            body (with-block
                                                   (fn []
                                                     (let [s (binding-sym b)
                                                           cast (if (= (:type b) "Ljava/lang/Throwable;")
                                                                  exc
                                                                  (list (csym (t/desc->internal (:type b)) "_Cast") exc))]
                                                       (decl! s (gotype (:type b)) cast)
                                                       (translate-ctx (:body c) ctx))))]
                                        [test (cons 'do body)]))
                                    (:catches node))
                          has-all (some #(= true %) (take-nth 2 clauses))]
                      (emit! (list 'when (list '!= exc nil)
                                   (apply list 'cond (concat clauses (when-not has-all [:else (list 'panic exc)])))))))
                  (when (empty? (:catches node))
                    (emit! (list 'when (list '!= exc nil) (list 'panic exc))))
                  (when (:ctl? lit) (dispatch-codes! ctl rv (:codes lit)))))]
    (if fin
      (let [lit (try-literal inner {:catch? true})
            call (literal-form lit true rv-desc)
            ctl (when (:ctl? lit) (tmp)) rv (when (:ctl? lit) (tmp)) exc (tmp)]
        (if (:ctl? lit)
          (swap! *block* conj [:let (list 'values ctl rv exc) call])
          (swap! *block* conj [:let exc call]))
        (use-sym! exc)
        (stmt! fin)
        (emit! (list 'when (list '!= exc nil) (list 'panic exc)))
        (when (:ctl? lit) (dispatch-codes! ctl rv (:codes lit))))
      (inner))
    (when after (method-return! after))))

(defn translate-monitor [node ctx]
  (let [[ctx after] (if (= :return (:k ctx))
                      (let [d (:method-ret *f*)]
                        (if (= "V" d) [{:k :stmt} nil]
                            (let [s (tmp)] (decl! s (gotype d) nil) (use-sym! s) [{:k :assign :var s :desc d} s])))
                      [ctx nil])
        lk (hoist-value (let [x (expr (:lock node))] (assoc x :x (coerce (:x x) (:t x) "Ljava/lang/Object;" (:nn x)) :t "Ljava/lang/Object;")))
        _ (emit! (list (jsym "MonitorEnter") (:x lk)))
        lit (try-literal (fn []
                           (emit! (list 'defer (list (jsym "MonitorExit") (:x lk))))
                           (translate-ctx (:body node) ctx))
                         {})
        call (literal-form lit false (:method-ret *f*))]
    (if (:ctl? lit)
      (let [ctl (tmp) rv (tmp)]
        (swap! *block* conj [:let (list 'values ctl rv) call])
        (dispatch-codes! ctl rv (:codes lit)))
      (emit! call))
    (when after (method-return! after))))

;; ---------------------------------------------------------------------------------------
;; lambdas and method references (§7.11)

(defn sam-sig [fi]
  (let [sam (a/find-sam fi)
        [ps r] (t/parse-method-desc (:desc sam))]
    {:sam sam :ps ps :r r}))

(defn adapter-sym
  "The adapter type F_Fn of functional interface fi, recorded for generation in fi's package."
  [fi]
  (swap! (:lambdas *pkgstate*) conj fi)
  (m/class-sym (pkg) fi "_Fn"))

(defn- convert-ref-or-prim
  "Converts Go value x of Java type from to Java type to, as LambdaMetafactory adapts:
  casts, unboxing, boxing, primitive widening."
  [x from to]
  (cond
    (= from to) x
    (and (t/prim? from) (t/prim? to)) (prim-convert x from to)
    (t/prim? from) (coerce (box-val x from) (str "L" (t/box-of from) ";") to true)
    (t/prim? to) (let [w (str "L" (t/box-of to) ";")
                       cx (if (env/assignable? from w) x (list (csym (t/box-of to) "_Cast") (coerce x from "Ljava/lang/Object;" false)))]
                   (:x (convert-val {:from w :to to :expr {:op ::go :x cx :t w}})))
    (env/assignable? from to) (coerce x from to false)
    :else (:x (cast-val {:class to :expr {:op ::go :x (coerce x from "Ljava/lang/Object;" false) :t "Ljava/lang/Object;"}}))))

(defn lambda-val [node]
  (let [fi (:fi node)
        {:keys [ps r]} (sam-sig fi)
        params (:params node)
        [ips iret] (t/parse-method-desc (:inst-desc node))
        psyms (vec (for [i (range (count ps))] (fresh (str "p" i))))
        fnf (binding [*f* (assoc *f* :fn-id (gensym "lambda") :method-ret iret :method-fn nil :try-frame nil
                                 :targets {})]
              (let [*f2 (assoc *f* :method-fn (:fn-id *f*))]
                (binding [*f* *f2]
                  (let [body (with-block
                               (fn []
                                 (doseq [[b p sp] (map vector params psyms ps)]
                                   (decl! (binding-sym b) (gotype (:type b)) (convert-ref-or-prim p sp (:type b))))
                                 (if (= "V" iret)
                                   (stmt! (:body node))
                                   (let [x (expr-as (:body node) iret)]
                                     (emit! (list 'return (convert-ref-or-prim x iret r)))))))]
                    (concat (list 'fn (cond-> (with-meta (vec (map (fn [s d] (tag s (gotype d))) psyms ps)) {})
                                        (not= "V" r) (vary-meta assoc :tag (gotype r))))
                            body)))))]
    (v (list 'addr (list 'lit (adapter-sym fi) :Fn fnf)) (str "L" fi ";") :nn true)))

(defn fi-adapter-val
  "Clojure's adapter of an argument to a functional interface parameter (Compiler's
  FISupport.maybeEmitFIAdapter; :fi-adapter): an IFn that is not already an instance of the
  interface becomes its adapter F_Fn (§7.11) calling FnInvokers' invoker, as the JVM's
  LambdaMetafactory site does; anything else is cast."
  [node]
  (let [fi (t/desc->internal (:type node))
        {:keys [ps r]} (sam-sig fi)
        [ips ir] (t/parse-method-desc (:invoker-desc node))
        fni (t/lang-class "FnInvokers")
        x (expr-as (:expr node) "Ljava/lang/Object;")
        psyms (vec (for [i (range (count ps))] (symbol (str "p" i))))
        call (apply list (csym fni (str "_" (nm/method-base (:invoker node) (:invoker-desc node))))
                    'f (map (fn [p sp ip] (convert-ref-or-prim p sp ip)) psyms ps (rest ips)))
        fnf (list* 'fn (cond-> (vec (map (fn [s d] (tag s (gotype d))) psyms ps))
                         (not= "V" r) (vary-meta assoc :tag (gotype r)))
                   [(if (= "V" r) call (list 'return (convert-ref-or-prim call ir r)))])
        ty (gotype (:type node))]
    (v (list (list 'fn (with-meta [(tag 'x 'any)] {:tag ty})
                   (list 'let [(list 'values 'f 'isfn) (list 'assert (csym (t/lang-class "IFn")) 'x)]
                         (list 'when (list 'and 'isfn (list 'not (list (csym fi "_InstanceOf") 'x)))
                               (list 'return (list 'addr (list 'lit (adapter-sym fi) :Fn fnf)))))
                   (list (csym fi "_Cast") 'x))
             x)
       (:type node))))

(defn method-ref-val [node]
  (let [fi (:fi node)
        {:keys [ps r]} (sam-sig fi)
        kind (:kind node)
        recv (when (= kind :bound)
               (let [x (expr (:recv node))]
                 (hoist-value (if (:null-check node)
                                (assoc x :x (if (m/pointer-desc? (:t x)) (list (jsym "NN") (:x x))
                                                (list (list 'inst (jsym "C2g_NotNull") (gotype (:t x))) (:x x))))
                                x))))
        [mps mr] (t/parse-method-desc (:desc node))
        psyms (vec (for [i (range (count ps))] (fresh (str "p" i))))
        base (nm/method-base (:name node) (:desc node))
        owner (:owner node)
        callf (fn []
                (let [args (case kind
                             :unbound (map (fn [p sp md] (convert-ref-or-prim p sp md)) (rest psyms) (rest ps) mps)
                             (map (fn [p sp md] (convert-ref-or-prim p sp md)) psyms ps mps))]
                  (case kind
                    :static (apply list (m/class-sym (pkg) owner (str "_" base)) args)
                    :new (let [real (e/ctor-real-desc owner {:desc (:desc node)})]
                           (apply list (symbol (str (if (= (m/pkg owner) (pkg)) "" (str (nm/pkg-name (m/pkg owner)) "/"))
                                                    (nm/new-name (m/go-name owner) real)))
                                  args))
                    :bound (let [rx (:x recv) rt (:t recv)]
                             (if (object-like? rt)
                               (apply list (jsym (object-helpers [(:name node) (:desc node)])) rx args)
                               (apply list (symbol (str "." base)) rx args)))
                    :unbound (let [od (str "L" owner ";")
                                   rx (convert-ref-or-prim (first psyms) (first ps) od)]
                               (if (object-like? od)
                                 (apply list (jsym (object-helpers [(:name node) (:desc node)])) rx args)
                                 (apply list (symbol (str "." base)) (if (m/pointer-desc? od) (list (jsym "NN") rx) rx) args))))))
        ret-d (if (= kind :new) (str "L" owner ";") mr)
        fnf (concat (list 'fn (cond-> (vec (map (fn [s d] (tag s (gotype d))) psyms ps))
                                (not= "V" r) (vary-meta assoc :tag (gotype r))))
                    (if (= "V" r)
                      [(callf)]
                      [(list 'return (convert-ref-or-prim (callf) ret-d r))]))]
    (v (list 'addr (list 'lit (adapter-sym fi) :Fn fnf)) (str "L" fi ";") :nn true)))

