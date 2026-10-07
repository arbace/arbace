(ns arbace.classes.lower
  "Clojure's special forms that the class forms compiler compiles by rewriting them into class
  forms and other forms it knows (SPEC §5.13, §9.5): fn*, reify*, letfn*, case* and def. These
  are pure form-to-form functions; arbace.classes.analyze analyzes their results.

  - (fn* ...) becomes an anonymous subclass of arbace.lang.AFunction (or RestFn when it has a
    variadic arity) with an invoke method per arity (doInvoke and getRequiredArity for the
    variadic one), as arbace.lang.Compiler's fn classes have them. Its body has Clojure's
    meaning: the parameters are Object, recur goes to the top of the arity, the fn's name is the
    instance. `fn-methods` makes the members; arbace.classes.native uses them for the top-level
    class of a fn that arbace.lang.Compiler hands over.
  - (reify* [interfaces] methods) becomes a local class implementing them and IObj.
  - (letfn* [f (fn* f ...) ...] body) binds boxes first, then the fns, whose bodies read the
    others from their boxes, then fills the boxes.
  - (case* ...) becomes a switch on the hash (or int value) inside a label, with the equality
    test in each arm and the default after the switch, as arbace.lang.Compiler's CaseExpr.
  - (def ...) becomes calls of the var's bindRoot, setMeta and setDynamic."
  (:require [arbace.string :as str]))

(def ^:private ids (atom 0))

(defn- gsym [prefix] (symbol (str prefix "__" (swap! ids inc) "__auto__")))

;; ---------------------------------------------------------------------------------------------
;; fn*

(defn fn-parts
  "(fn* name? [params] body*) or (fn* name? ([params] body*)+) as {:name :arities [[pv body]]}."
  [[_ & more :as form]]
  (let [[nm more] (if (symbol? (first more)) [(first more) (rest more)] [nil more])
        arities (if (vector? (first more)) [more] more)]
    (doseq [a arities]
      (when-not (and (seq? a) (vector? (first a)))
        (throw (ex-info (str "Bad fn form: " (pr-str form)) {:arbace/compile-error true}))))
    {:name nm :arities (mapv (fn [[pv & body]] [pv body]) arities)}))

(def ^:private prim-tags '#{long double int float short byte char boolean})

(defn- prim-tag? [x] (and (symbol? x) (nil? (namespace x)) (prim-tags x)))

(defn variadic? [[pv _]] (boolean (some #{'&} pv)))

(defn- class-char
  "Compiler.classChar: L for long, D for double, O for anything else that is no primitive."
  [tag]
  (cond (= 'long tag) "L"
        (= 'double tag) "D"
        (prim-tag? tag) (throw (ex-info "Only long and double primitives are supported"
                                        {:arbace/compile-error true}))
        :else "O"))

(defn prim-sig
  "The primitive signature of a fixed arity (\"LO\" for ^long x returning Object), as
  arbace.lang.Compiler's FnMethod.primInterface, or nil when it has none."
  [[pv _ :as arity]]
  (when-not (variadic? arity)
    (let [sig (apply str (concat (map #(class-char (:tag (meta %))) pv) [(class-char (:tag (meta pv)))]))]
      (when (re-find #"[LD]" sig)
        (when (> (count pv) 4)
          (throw (ex-info "fns taking primitives support only 4 or fewer args" {:arbace/compile-error true})))
        sig))))

(defn prim-interfaces
  "The IFn$... interfaces of the primitive signatures of the arities."
  [arities]
  (vec (distinct (for [a arities :let [sig (prim-sig a)] :when sig] (symbol (str (quote arbace.lang.IFn) "$" sig))))))

(def ^:private sig-types {"L" 'long "D" 'double "O" nil})

(defn fn-methods
  "The members of the class of a fn: an invoke (or doInvoke) method per arity, and
  getRequiredArity when variadic. `binds` are bindings [sym init ...] made at the start of every
  arity, before the fn's name (bound to the instance) and the parameters (initializers may use
  the receiver `rcv`). An arity with a primitive signature (prim-sig) has its body in invokePrim
  of its IFn$... interface, and invoke converts the arguments and calls it, as
  arbace.lang.Compiler's fn classes do."
  [rcv nm arities binds]
  (let [vs (filter variadic? arities)]
    (when (> (count vs) 1)
      (throw (ex-info "Can't have more than 1 variadic overload" {:arbace/compile-error true})))
    (concat
      (mapcat
        (fn [[pv body :as arity]]
          (let [[fixed [_ rest-p]] (split-with #(not= '& %) pv)
                params (vec (concat fixed (when rest-p [rest-p])))
                args (mapv (fn [_] (gsym "p")) params)
                sig (prim-sig arity)
                ;; parameter hints stay hints of the locals; a rest parameter is untyped
                lbinds (vec (mapcat (fn [p a] [(if (= p rest-p) (vary-meta p dissoc :tag) p) a])
                                    params args))
                body-form (list 'let* (vec (concat binds (when nm [nm rcv])))
                                (list* (with-meta 'loop* {:arbace.classes/fn-body true}) lbinds body))]
            (if sig
              (let [ptypes (map (comp sig-types str) (butlast sig))
                    rtype (sig-types (str (last sig)))
                    typed (fn [a t] (if t (with-meta a {:tag t}) a))]
                [(list 'method (with-meta 'invokePrim {:public true})
                       (with-meta (vec (cons rcv (map typed args ptypes))) (if rtype {:tag rtype} {}))
                       body-form)
                 (list 'method (with-meta 'invoke {:public true}) (vec (cons rcv args))
                       (list* '.invokePrim rcv
                              (map (fn [a t] (case t
                                               long (list 'arbace.lang.RT/longCast a)
                                               double (list 'arbace.lang.RT/doubleCast a)
                                               a))
                                   args ptypes)))])
              [(list 'method (with-meta (if rest-p 'doInvoke 'invoke) {(if rest-p :protected :public) true})
                     (vec (cons rcv args))
                     body-form)])))
        arities)
      (when-let [[pv] (first vs)]
        [(list 'method (with-meta 'getRequiredArity {:public true}) (with-meta [rcv] {:tag 'int})
               (count (take-while #(not= '& %) pv)))]))))

(defn fn-meta
  "The metadata of a fn* form that the fn object gets (without source positions), or nil."
  [form]
  (let [m (dissoc (meta form) :line :column :file :rettag :end-line :end-column)]
    (when (seq m) m)))

(defn lower-fn
  "(fn* ...) in code compiled by the class forms compiler: an anonymous AFunction or RestFn, or
  with primitive signatures a local class that also implements their IFn$... interfaces."
  [form]
  (let [{nm :name arities :arities} (fn-parts form)
        rcv (gsym "fn")
        super (if (some variadic? arities) 'arbace.lang.RestFn 'arbace.lang.AFunction)
        ifaces (prim-interfaces arities)
        anon (if (seq ifaces)
               (let [cn (symbol (str "Fn" (swap! ids inc)))]
                 (list 'class* :local
                       [(list* (with-meta cn {:clojure-fn true :final true}) :extends super :implements ifaces
                               (fn-methods rcv nm arities nil))]
                       (list 'new cn)))
               (list* 'class* :anon (with-meta super {:clojure-fn true}) []
                      (fn-methods rcv nm arities nil)))]
    (if-let [m (fn-meta form)]
      (list 'arbace.core/with-meta anon m)
      anon)))

;; ---------------------------------------------------------------------------------------------
;; reify*

(declare symbols-in)

(defn lower-reify
  "(reify* [interface*] (method [this params*] body*)*): a local class implementing the
  interfaces and arbace.lang.IObj (its metadata in __meta, withMeta making a copy), its methods
  public, their signatures inferred from the interfaces as for untyped methods (§4.6), with
  recur to the top of a method when its body uses recur. Evaluates to an instance with the
  form's metadata (without source positions)."
  [[_ ifaces & methods :as form]]
  (let [cn (symbol (str "Reify" (swap! ids inc)))
        pm 'arbace.lang.IPersistentMap
        mm (gsym "meta")
        rcv (gsym "this")
        m (fn-meta form)]
    (list 'class* :local
          [(concat
             [(with-meta cn {:clojure-fn true :final true})
              :implements (conj (vec ifaces) 'arbace.lang.IObj)
              (list 'field (with-meta '__meta {:tag pm :final true :private true}))
              (list 'constructor (with-meta [rcv (with-meta mm {:tag pm})] {:public true})
                    (list 'set! (list '.-__meta rcv) mm))
              (list 'method (with-meta 'meta {:public true}) (with-meta [rcv] {:tag pm}) (list '.-__meta rcv))
              (list 'method (with-meta 'withMeta {:public true})
                    (with-meta [rcv (with-meta mm {:tag pm})] {:tag 'arbace.lang.IObj})
                    (list 'new cn mm))]
             (for [[nm pv & body] methods]
               (let [[this & ps] pv]
                 (list 'method (vary-meta nm assoc :public true) pv
                       (if (contains? (symbols-in body) 'recur)
                         (list* (with-meta 'loop* {:arbace.classes/fn-body true})
                                (vec (mapcat (fn [p] [(vary-meta p dissoc :tag) p]) ps))
                                body)
                         (list* 'do body))))))]
          (list 'new cn m))))

;; ---------------------------------------------------------------------------------------------
;; letfn*

(defn symbols-in
  "The unqualified symbols occurring in form (any position)."
  [form]
  (cond
    (symbol? form) (if (namespace form) #{} #{form})
    (or (seq? form) (vector? form) (set? form)) (reduce into #{} (map symbols-in form))
    (map? form) (reduce into #{} (map symbols-in (mapcat identity form)))
    :else #{}))

(defn lower-letfn
  "(letfn* [f (fn f ...) ...] body*): each fn reads the others from boxes filled after all
  are created. `expand` macroexpands an initializer to its fn* form."
  [[_ bindings & body] expand]
  (let [pairs (partition 2 bindings)
        names (map first pairs)
        boxes (zipmap names (map #(gsym (str "box_" (name %))) names))
        with-others (fn [self init]
                      (let [fn-form (expand init)
                            _ (when-not (and (seq? fn-form) (= 'fn* (first fn-form)))
                                (throw (ex-info (str "letfn binding is not a fn: " (pr-str init))
                                                {:arbace/compile-error true})))
                            {nm :name arities :arities} (fn-parts fn-form)
                            others (remove (set [self nm]) names)]
                        (with-meta
                          (list* 'fn* (concat (when nm [nm])
                                              (for [[pv body] arities
                                                    :let [ps (set pv)
                                                          bs (vec (mapcat (fn [o] [o (list '.-val (boxes o))])
                                                                          (remove ps others)))]]
                                                (list pv (list* 'let* bs body)))))
                          (meta fn-form))))]
    (list* 'let* (vec (concat (mapcat (fn [n] [(boxes n) (list 'new 'arbace.lang.Box nil)]) names)
                              (mapcat (fn [[n f]] [n (with-others n f)]) pairs)))
           (concat (for [n names] (list 'set! (list '.-val (boxes n)) n))
                   body))))

;; ---------------------------------------------------------------------------------------------
;; case*

(defn lower-case
  "(case* ge shift mask default imap switch-type test-type skip-check?), as CaseExpr compiles
  it: the int value (test-type :int) or hash of ge, shifted and masked, selects an arm, which
  tests ge against the arm's constant (= or, for :hash-identity, identical?) and on success
  leaves the case with the arm's value; otherwise the default. `prim-int?` says whether ge is a
  local of an integral primitive type."
  [[_ ge shift mask default imap _switch-type test-type skip-check] prim-int?]
  (let [L (keyword (name (gsym "case")))
        shift (int shift) mask (int mask)
        sm (fn [h] (if (zero? mask)
                     h
                     (list 'arbace.core/bit-and-int (list 'arbace.core/bit-shift-right-int h shift) mask)))
        skip (set skip-check)
        arms (mapcat (fn [[k [test then]]]
                       [(int k)
                        (cond
                          (contains? skip (int k)) (list 'break* L then)
                          (= test-type :int)
                          (list 'if (if prim-int?
                                      (list 'arbace.core/== ge test)
                                      (list 'arbace.lang.Util/equiv ge (list 'quote test)))
                                (list 'break* L then) nil)
                          (= test-type :hash-identity)
                          (list 'if (list 'arbace.core/identical? ge (list 'quote test)) (list 'break* L then) nil)
                          :else
                          (list 'if (list 'arbace.lang.Util/equiv ge (list 'quote test)) (list 'break* L then) nil))])
                     (sort-by key imap))
        sw (fn [sel] (list* 'switch* sel arms))]
    (list 'label* L
          (cond
            (not= test-type :int) (sw (sm (list 'arbace.lang.Util/hash ge)))
            prim-int? (sw (sm (list 'arbace.core/unchecked-int ge)))
            :else (list 'if (list 'arbace.core/instance? 'java.lang.Number ge)
                        (sw (sm (list '.intValue (list 'arbace.core/cast 'java.lang.Number ge))))
                        nil))
          default)))

;; ---------------------------------------------------------------------------------------------
;; def

(defn lower-def
  "(def sym), (def sym init), (def sym doc init) as calls on the var (already interned): binds
  the root when there is an initializer, sets the metadata (with the source position and doc)
  and the dynamic flag, as DefExpr does. Returns the var."
  [[_ sym & more] file line column]
  (let [[doc init?, init] (case (count more)
                            0 [nil false nil]
                            1 [nil true (first more)]
                            2 [(first more) true (second more)]
                            (throw (ex-info "Too many arguments to def" {:arbace/compile-error true})))
        ;; evaluated, as DefExpr evaluates it (a :tag symbol naming a class gives the class)
        m (cond-> (assoc (meta sym) :line line :column column :file file)
            doc (assoc :doc doc))
        v (gsym "var")]
    (list 'let* [v (list 'var sym)]
          (when init? (list '. v 'bindRoot init))
          (list '. v 'setMeta m)
          (list '. v 'setDynamic (boolean (:dynamic (meta sym))))
          v)))
