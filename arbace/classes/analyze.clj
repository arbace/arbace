(ns arbace.classes.analyze
  "Entering class declarations (names, supertypes, members, what javac derives) and analyzing
  code into typed nodes for arbace.classes.emit (SPEC §4 to §6)."
  (:require [clojure.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.parse :as p])
  (:import (arbace.asm Opcodes Type MethodVisitor ClassWriter TypeReference)))

;; ---------------------------------------------------------------------------------------------
;; compilation unit

(def ^:dynamic *unit*
  "The current compilation: {:order (atom [names in definition order]) :counters (atom {})}."
  nil)

(def ^:private ids (atom 0))
(defn next-id [] (swap! ids inc))

(defn fail
  ([msg] (fail msg {}))
  ([msg data] (throw (ex-info msg (assoc data :arbace/compile-error true)))))

(defn decl [n] (some-> env/*compile-set* deref (get n)))
(defn decl! [n] (or (decl n) (fail (str "Not in the compilation: " n))))
(defn update-decl! [n f & args] (apply swap! env/*compile-set* update n f args))
(defn state [n] (:state (decl! n)))

(defn has? [flags f] (not (zero? (bit-and (or flags 0) f))))
(defn static-flag? [m] (has? (:flags m) Opcodes/ACC_STATIC))

(defn nest-host [n] (env/nest-host n))

(defn ns-package [ns] (str/replace (munge (name (ns-name ns))) "." "/"))

;; ---------------------------------------------------------------------------------------------
;; class names (§5.2)

(declare enter-from-source!)

(def ^:dynamic *source-lookup*
  "Whether class names that resolve to nothing are looked up as p/C.clj sources (§9.2)."
  true)

(defn- class-exists? [n]
  (boolean (or (decl n) (env/load-class n)
               (and *source-lookup* *unit* (enter-from-source! n)))))

(defn resolve-class-sym
  "Resolves a class name symbol from `scope` {:class internal-name :ns ns :local-classes {}}:
  member classes of the class and its enclosing classes, then the namespace's mappings, then the
  class's own package, then java.lang. Returns an internal name or nil."
  [scope sym]
  (when (and (symbol? sym) (nil? (namespace sym)))
    (let [s (name sym)
          ns (:ns scope)]
      (cond
        (and (str/includes? s "$") (not (str/starts-with? s "$")))
        (or (let [v (when ns (get (ns-map ns) sym))]   ; (import '(java.util Map$Entry))
              (when (class? v) (str/replace (.getName ^Class v) "." "/")))
            (let [[head tail] (str/split s #"\$" 2)]
              (when-let [h (resolve-class-sym scope (symbol head))]
                (let [n (str h "$" tail)] (when (class-exists? n) n)))))

        (str/includes? s ".")
        (let [n (str/replace s "." "/")] (when (class-exists? n) n))

        :else
        (or (get (:local-classes scope) sym)
            (loop [c (:class scope)]
              (when-let [d (and c (decl c))]
                (or (when (= s (:simple d)) (when (not= :anon (:nesting d)) c))
                    (get (:member-classes d) s)
                    (get (:local-classes d) sym)
                    (recur (:outer d)))))
            (let [v (when ns (get (ns-map ns) sym))]
              (when (class? v) (str/replace (.getName ^Class v) "." "/")))
            (let [pkg (cond (:class scope) (t/package-of (:class scope))
                            ns (ns-package ns))
                  n (if (= pkg "") s (str pkg "/" s))]
              (when (class-exists? n) n))
            (let [n (str "java/lang/" s)] (when (class-exists? n) n)))))))

(declare implicit-outer)

(defn type-scope
  "A scope for types/parse-type: class name resolution plus type variables."
  [scope]
  (assoc scope
         :resolve #(resolve-class-sym scope %)
         :tvars (set (keys (:bounds scope)))
         :implicit-outer #(implicit-outer scope %)))

(defn- class-tparams [n]
  (if-let [d (decl n)]
    (mapv :sym (or (:tparams d) (map #(if (symbol? %) % (first %)) (get-in d [:opts :type-params]))))
    (when-let [c ^Class (env/load-class n)]
      (mapv #(symbol (.getName ^java.lang.reflect.TypeVariable %)) (.getTypeParameters c)))))

(defn implicit-outer
  "Inside a generic class, the simple name of one of its inner (non-static) member classes
  denotes Outer<T...>.Inner (JLS 6.5.5.1): the outer type node, or nil."
  [scope n]
  (let [i (env/info n)
        o (:outer i)]
    (when (and o (:outer-instance? i))
      (let [tps (class-tparams o)
            tvars (set (keys (:bounds scope)))]
        (cond
          (and (seq tps) (every? tvars tps))
          {:t :class :name o :args (mapv (fn [s] {:t :tvar :sym s}) tps)}
          :else (when-let [oo (implicit-outer scope o)]
                  {:t :class :name o :args [] :outer oo}))))))

(defn parse-type [scope form] (t/parse-type (type-scope scope) form))
(defn erase [scope tn] (t/erase (:bounds scope) tn))
(defn type-desc
  "The erased descriptor of a type form in scope."
  [scope form]
  (erase scope (parse-type scope form)))

(defn class-scope
  "The scope of the body of class n."
  [n]
  (let [d (decl! n)]
    {:class n :ns (:ns d) :local-classes (:local-classes d) :bounds (:bounds d)}))

;; ---------------------------------------------------------------------------------------------
;; annotations (§4.4), kept as data for the emitter: [{:type desc :visible bool :values ...}]

(defn- retention [n]
  (if-let [d (decl n)]
    (let [r (some (fn [[k v]] (when (and (symbol? k) (= "java/lang/annotation/Retention"
                                                         (resolve-class-sym (class-scope n) k)))
                                v))
                  (:meta d))]
      (if r (keyword (str/lower-case (name r))) :class))
    (if-let [c (env/load-class n)]
      (if-let [r (.getAnnotation ^Class c java.lang.annotation.Retention)]
        (keyword (str/lower-case (str (.value ^java.lang.annotation.Retention r))))
        :class)
      :class)))

(defn- element-type
  "The return descriptor of annotation element `el` of annotation type n."
  [n el]
  (some #(when (and (= el (:name %)) (str/starts-with? (:desc %) "()")) (subs (:desc %) 2))
        (:methods (env/info! n))))

(declare analyze-const-form)

(defn- ann-value [scope desc v]
  (cond
    (t/array? desc)
    {:array (mapv #(ann-value scope (t/elem-type desc) %) (if (vector? v) v [v]))}

    (and (seq? v) (symbol? (first v)))      ; nested annotation (B {:k v})
    (let [an (or (resolve-class-sym scope (first v)) (fail (str "Unknown annotation: " (first v))))]
      {:annotation (assoc (let [m (second v)] {:type (t/internal->desc an)
                                                :values (into {} (for [[k x] (if (map? m) m {:value m})]
                                                                   [(name k) (ann-value scope (element-type an (name k)) x)]))})
                          :nested true)})

    (and (symbol? v) (namespace v) (not (re-matches #"[0-9]" (name v)))
         (let [cn (resolve-class-sym scope (symbol (namespace v)))]
           (and cn (env/interface? cn) false)))
    nil

    (= desc "Ljava/lang/Class;")
    {:class (type-desc scope v)}

    (and (symbol? v) (namespace v) (t/class-desc? desc) (not= desc t/string-desc)
         (let [en (t/desc->internal desc)] (has? (:flags (env/info en)) Opcodes/ACC_ENUM)))
    {:enum desc :name (name v)}

    :else
    (let [c (analyze-const-form scope v desc)]
      {:const c})))

(declare annotations-now)

(defn annotations
  "Annotations (delayed, as annotation types of the compilation may not be entered yet) in metadata m (symbol keys naming annotation types), in scope.
  Returns [{:type desc :visible bool :values {name value}}], SOURCE ones dropped."
  [scope m]
  (delay (annotations-now scope m)))

(defn annotation-targets
  "The ElementType names of annotation type n's @Target, or nil when it has none."
  [n]
  (if-let [d (decl n)]
    (some (fn [[k v]] (when (and (symbol? k) (= "java/lang/annotation/Target"
                                                (resolve-class-sym (class-scope n) k)))
                        (set (map name (if (vector? v) v [v])))))
          (:meta d))
    (when-let [c (env/load-class n)]
      (when-let [t (.getAnnotation ^Class c java.lang.annotation.Target)]
        (set (map str (.value ^java.lang.annotation.Target t)))))))

(defn for-target
  "Annotations applicable to declaration context `target` (an ElementType name), as javac
  propagates record component annotations."
  [anns target]
  (delay (vec (filter #(let [ts (annotation-targets (t/desc->internal (:type %)))]
                         (or (nil? ts) (ts target)))
                      (force anns)))))

(declare annotations-now)

(defn decl-annotations
  "Declaration annotations in metadata m for declaration context `context` (an ElementType
  name): those without @Target or whose @Target includes it."
  [scope m context]
  (delay (vec (filter #(let [ts (annotation-targets (t/desc->internal (:type %)))]
                         (or (nil? ts) (ts context)
                             ;; an annotation interface is a type too
                             (and (= context "ANNOTATION_TYPE") (ts "TYPE"))))
                      (annotations-now scope m)))))

(defn type-annotations
  "Type annotations of a declared type tn at type reference `ref` (an int): those inside the
  type, and those of the declaration's metadata m whose @Target includes TYPE_USE."
  [scope ref tn m]
  (delay
    (vec (concat
           (when (and m tn (not= "V" (:desc tn)))
             (for [a (annotations-now scope m)
                   :let [ts (annotation-targets (t/desc->internal (:type a)))]
                   :when (and ts (ts "TYPE_USE"))]
               (assoc a :ref ref :path (t/element-path tn))))
           (when tn
             (for [[path anns] (t/type-anns tn)
                   a (annotations-now scope anns)]
               (assoc a :ref ref :path path)))))))

(defn- tparam-type-annotations
  "Type annotations of type parameters and their bounds (sort: class or method)."
  [scope tps class?]
  (delay
    (vec (apply concat
                (map-indexed
                  (fn [i {:keys [anns bounds]}]
                    (concat
                      (for [a (annotations-now scope (into {} (filter (comp symbol? key) anns)))]
                        (assoc a :ref (.getValue (TypeReference/newTypeParameterReference
                                                   (if class? TypeReference/CLASS_TYPE_PARAMETER
                                                              TypeReference/METHOD_TYPE_PARAMETER) i))
                                 :path ""))
                      (apply concat
                             (map-indexed
                               (fn [j b]
                                 ;; bound index 0 is the class bound, empty when the first bound is an interface
                                 (let [bj (if (and (= :class (:t (first bounds))) (env/interface? (:name (first bounds))))
                                            (inc j) j)]
                                   (force (type-annotations scope
                                                            (.getValue (TypeReference/newTypeParameterBoundReference
                                                                         (if class? TypeReference/CLASS_TYPE_PARAMETER_BOUND
                                                                                    TypeReference/METHOD_TYPE_PARAMETER_BOUND)
                                                                         i bj))
                                                            b nil))))
                               bounds))))
                  tps)))))

(defn annotations-now
  [scope m]
  (vec (for [[k v] m
             :when (symbol? k)
             :let [n (or (resolve-class-sym scope k) (fail (str "Unknown annotation type: " k)))
                   r (retention n)]
             :when (not= r :source)]
         {:type (t/internal->desc n)
          :visible (= r :runtime)
          :values (cond
                    (true? v) {}
                    (map? v) (into {} (for [[ek ev] v]
                                        [(name ek) (ann-value scope (element-type n (name ek)) ev)]))
                    :else {"value" (ann-value scope (element-type n "value") v)})})))

;; ---------------------------------------------------------------------------------------------
;; declaring classes

(defn top-name [ns parsed]
  (let [s (str (:sym parsed))
        pkg (get-in parsed [:opts :package])]
    (cond
      (some? pkg) (str (when (seq (str pkg)) (str (str/replace (str pkg) "." "/") "/")) s)
      (str/includes? s ".") (str/replace s "." "/")
      :else (let [p (ns-package ns)] (if (= p "") s (str p "/" s))))))

(defn local-class-name
  "javac's name for a local (simple name given) or anonymous class in class `outer`."
  [outer simple]
  (let [k [outer (or simple "")]
        n (get (swap! (:counters *unit*) update k (fnil inc 0)) k)]
    (str outer "$" n simple)))

(defn declare-class!
  "Enters a parsed class form and its member classes into the compilation.
  ctx: {:nesting :outer :name (for local and anonymous classes) :static-context bool
        :local-classes {} :enclosing-method {:name :desc}}. Returns the internal name."
  [ctx parsed]
  (let [nesting (:nesting ctx)
        ns (:ns parsed)
        n (case nesting
            :top (top-name ns parsed)
            :member (str (:outer ctx) "$" (:simple parsed))
            (:name ctx))
        outer-d (some-> (:outer ctx) decl)
        outer-instance? (case nesting
                          :top false
                          :member (and (not (:static? parsed))
                                       (not (#{:interface :annotation} (:kind outer-d))))
                          (and (not (:static-context ctx)) (= :class (:kind parsed))
                               (not (:static? parsed))))]
    (when (and (= nesting :top) (decl n))
      (fail (str "Class defined twice in one form: " n)))
    (swap! env/*compile-set* assoc n
           (merge parsed
                  {:name n :outer (:outer ctx) :nesting nesting
                   :outer-instance? outer-instance?
                   :enclosing-method (:enclosing-method ctx)
                   :local-classes (:local-classes ctx)
                   :nest-host (if outer-d (:nest-host outer-d) n)
                   :state (atom {:captures [] :uses-this0 false :lambdas 0 :extra []
                                 :anon-ctx nil})}))
    (swap! (:order *unit*) conj n)
    (let [mcs (doall
                (for [m (:members parsed) :when (= :class (:kind m))]
                  (let [pc (p/parse-class {:ns ns :nesting :member :outer (decl n)} (rest (:form m)))]
                    [(:simple pc) (declare-class! {:nesting :member :outer n} pc)])))]
      (update-decl! n assoc :member-classes (into {} mcs)))
    n))

(defn- bounds-of-scope
  "Type variable bounds visible in class n: its own and, unless static, its outer classes'."
  [n]
  (let [d (decl! n)]
    (merge (when (and (:outer d) (or (:outer-instance? d) (#{:local :anon} (:nesting d))))
             (:bounds (decl (:outer d))))
           (:own-bounds d))))

(defn resolve-header!
  "Resolves type parameters, superclass, interfaces and the class Signature of n."
  [n]
  (let [d (decl! n)
        tp-syms (map #(if (symbol? %) % (first %)) (get-in d [:opts :type-params]))
        _ (update-decl! n assoc :own-bounds (zipmap tp-syms (repeat [])))
        _ (update-decl! n assoc :bounds (bounds-of-scope n))
        scope (class-scope n)
        tps (t/parse-type-params (type-scope scope) (get-in d [:opts :type-params]))
        _ (update-decl! n assoc :own-bounds (t/bounds-map tps) :tparams tps)
        _ (update-decl! n assoc :bounds (bounds-of-scope n))
        scope (class-scope n)
        pt #(parse-type scope %)
        kind (:kind d)
        ext (get-in d [:opts :extends])
        impl (map pt (get-in d [:opts :implements]))
        [super-t ifaces-t]
        (case kind
          :enum [{:t :class :name "java/lang/Enum" :args [{:t :class :name n :args []}]} impl]
          :record [{:t :class :name "java/lang/Record" :args []} impl]
          (:interface :annotation)
          [{:t :class :name "java/lang/Object" :args []}
           (concat (map pt (if (vector? ext) ext (when ext [ext])))
                   (when (= kind :annotation) [{:t :class :name "java/lang/annotation/Annotation" :args []}]))]
          :class (if-let [as (:anon-super d)]
                   (if (env/interface? (:name as))
                     [{:t :class :name "java/lang/Object" :args []} [as]]
                     [as nil])
                   [(if ext (pt ext) {:t :class :name "java/lang/Object" :args []}) impl]))
        sig? (or (seq tps) (t/generic? super-t) (some t/generic? ifaces-t))
        sig (when sig?
              (str (t/type-params-signature env/interface? tps)
                   (t/signature super-t) (apply str (map t/signature ifaces-t))))]
    (update-decl! n assoc :headers-done true
                  :super (:name super-t) :interfaces (mapv :name ifaces-t)
                  :super-t super-t :interfaces-t (vec ifaces-t)
                  :signature sig
                  :permits (mapv #(:name (pt %)) (get-in d [:opts :permits]))
                  :annotations (decl-annotations scope (:meta d) (if (= kind :annotation) "ANNOTATION_TYPE" "TYPE"))
                  :type-annotations
                  (delay (vec (concat
                                (force (tparam-type-annotations (type-scope scope) tps true))
                                (when-not (#{:interface :annotation} kind)
                                  (force (type-annotations scope (.getValue (TypeReference/newSuperTypeReference -1)) super-t nil)))
                                (apply concat
                                       (map-indexed (fn [i it] (force (type-annotations scope (.getValue (TypeReference/newSuperTypeReference i)) it nil)))
                                                    ifaces-t))))))
    (when (has? (:flags (env/info (:name super-t))) Opcodes/ACC_FINAL)
      (when-not (and (= kind :enum) false)
        (fail (str "Cannot extend final class " (:name super-t)))))
    n))

;; members ----------------------------------------------------------------------------------------

(def ^:private field-mods #{:public :private :protected :static :final :transient :volatile :synthetic})
(def ^:private method-mods #{:public :private :protected :static :final :abstract :synchronized
                             :native :synthetic :bridge})

(defn- deprecated-flag [m]
  (if (or (:deprecated m) (some #(and (symbol? %) (#{"Deprecated" "java.lang.Deprecated"} (str %))) (keys m)))
    Opcodes/ACC_DEPRECATED 0))

(defn- iface-kind? [d] (#{:interface :annotation} (:kind d)))

(defn- tag-of [x] (:tag (meta x)))

(defn- param-info [scope sym]
  (let [tn (parse-type scope (or (tag-of sym) 'java.lang.Object))]
    {:sym sym :tn tn :desc (erase scope tn) :meta (meta sym)
     :flags (if (:final (meta sym)) Opcodes/ACC_FINAL 0)}))

(defn- infer-signature
  "deftype's rule: an untyped instance method takes the erased signature of the unique method of
  its supertypes with the same name and arity."
  [n name arity]
  (let [cands (distinct
                (for [s (rest (env/all-supertypes n))
                      m (:methods (env/info! s))
                      :when (= name (:name m))
                      :when (not (has? (:flags m) (bit-or Opcodes/ACC_STATIC Opcodes/ACC_PRIVATE
                                                          Opcodes/ACC_SYNTHETIC)))
                      :let [[ps _] (t/parse-method-desc (:desc m))]
                      :when (= arity (count ps))]
                  (:desc m)))]
    (when (= 1 (count cands)) (first cands))))

(defn- method-scope [scope tparams-form]
  (let [syms (map #(if (symbol? %) % (first %)) tparams-form)
        s1 (update scope :bounds merge (zipmap syms (repeat [])))
        tps (t/parse-type-params (type-scope s1) tparams-form)]
    [(update scope :bounds merge (t/bounds-map tps)) tps]))

(defn- method-signature
  [tps params ret-tn throws-tn]
  (when (or (seq tps) (some t/generic? (map :tn params)) (t/generic? ret-tn)
            (some t/generic? throws-tn))
    (str (t/type-params-signature env/interface? tps)
         "(" (apply str (map (comp t/signature :tn) params)) ")" (t/signature ret-tn)
         (when (some t/generic? throws-tn) (apply str (map #(str "^" (t/signature %)) throws-tn))))))

(defn- method-type-annotations [scope tps mmeta ret-tn pinfos throws-tn recv]
  (delay
    (vec (concat
           (force (tparam-type-annotations scope tps false))
           (when ret-tn
             (force (type-annotations scope (.getValue (TypeReference/newTypeReference TypeReference/METHOD_RETURN))
                                      ret-tn mmeta)))
           (when recv
             (for [a (annotations-now scope (into {} (filter (comp symbol? key) (clojure.core/meta recv))))
                   :let [ts (annotation-targets (t/desc->internal (:type a)))]
                   :when (and ts (ts "TYPE_USE"))]
               (assoc a :ref (.getValue (TypeReference/newTypeReference TypeReference/METHOD_RECEIVER)) :path "")))
           (apply concat
                  (map-indexed (fn [i p]
                                 (force (type-annotations scope (.getValue (TypeReference/newFormalParameterReference i))
                                                          (:tn p) (:meta p))))
                               pinfos))
           (apply concat
                  (map-indexed (fn [i tn]
                                 (force (type-annotations scope (.getValue (TypeReference/newExceptionReference i)) tn nil)))
                               throws-tn))))))

(defn- enter-method [n scope m]
  (let [d (decl! n)
        meta (:meta m)
        static? (boolean (:static meta))
        [scope tps] (method-scope scope (get-in m [:opts :type-params]))
        raw (:params m)
        [recv params] (if static? [nil raw] [(first raw) (rest raw)])
        _ (when (and (not static?) (nil? recv))
            (fail (str "Instance method " (:name m) " needs a receiver parameter")))
        untyped? (and (not static?) (nil? (:tag (:vmeta m))) (not-any? tag-of params)
                      (not (iface-kind? d)))
        inferred (when untyped? (infer-signature n (:name m) (count params)))
        pinfos (if inferred
                 (let [[ps r] (t/parse-method-desc inferred)]
                   (mapv (fn [s pd] {:sym s :desc pd :tn (parse-type scope (symbol (t/desc->class-name pd)))
                                     :meta (clojure.core/meta s) :flags 0})
                         params ps))
                 (mapv #(param-info scope %) params))
        pinfos (if inferred (mapv (fn [pi] (if (t/prim? (:desc pi)) (assoc pi :tn {:t :prim :desc (:desc pi)}) pi)) pinfos) pinfos)
        ret-tn (if inferred
                 (let [r (second (t/parse-method-desc inferred))]
                   (if (or (t/prim? r) (= r "V")) {:t :prim :desc r}
                       (parse-type scope (symbol (t/desc->class-name r)))))
                 (parse-type scope (or (:tag (:vmeta m)) 'java.lang.Object)))
        ret (erase scope ret-tn)
        throws-tn (mapv #(parse-type scope %) (get-in m [:opts :throws]))
        flags (p/flags-of meta method-mods)
        iface? (iface-kind? d)
        flags (cond-> flags
                iface? (cond-> (not (:private meta)) (bit-or Opcodes/ACC_PUBLIC)
                         (and (not (:has-body m)) (not static?) (not (:private meta)) (not (:default meta)))
                         (bit-or Opcodes/ACC_ABSTRACT))
                (:varargs m) (bit-or Opcodes/ACC_VARARGS)
                true (bit-or (deprecated-flag meta)))]
    (when (and (:varargs m) (not (t/array? (:desc (last pinfos)))))
      (fail (str "Variable arity parameter must be an array: " (:name m))))
    {:name (:name m) :owner n :flags flags :params pinfos :recv recv :ret ret :ret-tn ret-tn
     :desc (t/method-desc (map :desc pinfos) ret)
     :sig (method-signature tps pinfos ret-tn throws-tn)
     :throws (mapv #(t/desc->internal (erase scope %)) throws-tn)
     :annotations (decl-annotations scope meta "METHOD")
     :param-annotations (mapv #(decl-annotations scope (:meta %) "PARAMETER") pinfos)
     :type-annotations (method-type-annotations scope tps meta ret-tn pinfos throws-tn recv)
     :default (get-in m [:opts :default]) :has-default (contains? (:opts m) :default)
     :member m :scope scope :kind :method}))

(defn- class-access [d]
  (bit-and (:inner-flags d) (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_PROTECTED Opcodes/ACC_PRIVATE)))

(defn- enter-ctor [n scope m]
  (let [d (decl! n)
        vmeta (:vmeta m)
        [scope tps] (method-scope scope (get-in m [:opts :type-params]))
        [recv & params] (:params m)
        _ (when (nil? recv) (fail "A constructor needs a receiver parameter"))
        pinfos (mapv #(param-info scope %) params)
        throws-tn (mapv #(parse-type scope %) (get-in m [:opts :throws]))
        flags (cond-> (p/flags-of vmeta #{:public :private :protected :synthetic})
                (= :enum (:kind d)) (-> (bit-and (bit-not (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_PROTECTED)))
                                        (bit-or Opcodes/ACC_PRIVATE))
                (:varargs m) (bit-or Opcodes/ACC_VARARGS)
                true (bit-or (deprecated-flag vmeta)))]
    {:name "<init>" :owner n :flags flags :params pinfos :recv recv :ret "V"
     :ret-tn {:t :prim :desc "V"}
     :desc (t/method-desc (map :desc pinfos) "V")
     :sig (method-signature tps pinfos {:t :prim :desc "V"} throws-tn)
     :throws (mapv #(t/desc->internal (erase scope %)) throws-tn)
     :annotations (decl-annotations scope vmeta "CONSTRUCTOR")
     :param-annotations (mapv #(decl-annotations scope (:meta %) "PARAMETER") pinfos)
     :type-annotations (method-type-annotations scope tps nil nil pinfos throws-tn recv)
     :compact (boolean (:compact vmeta))
     :member m :scope scope :kind :ctor}))

(defn- enter-field [n scope m]
  (let [d (decl! n)
        meta (:meta m)
        tn (parse-type scope (or (tag-of (:sym m)) 'java.lang.Object))
        desc (erase scope tn)
        flags (cond-> (p/flags-of meta field-mods)
                (iface-kind? d) (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC Opcodes/ACC_FINAL)
                true (bit-or (deprecated-flag meta)))
        const? (and (has? flags Opcodes/ACC_FINAL) (:has-init m)
                    (or (t/prim? desc) (= desc t/string-desc)))]
    {:name (:name m) :owner n :flags flags :desc desc :tn :tn
     :sig (when (t/generic? tn) (t/signature tn))
     :annotations (decl-annotations scope meta "FIELD")
     :type-annotations (type-annotations scope (.getValue (TypeReference/newTypeReference TypeReference/FIELD)) tn meta)
     :init (:init m) :has-init (:has-init m) :member m
     :const (when const? (delay (:val (analyze-const-form scope (:init m) desc :lenient))))}))

(defn- derived-method [n name desc flags kind & {:as more}]
  (merge {:name name :desc desc :flags flags :owner n :derived kind
          :ret (second (t/parse-method-desc desc))
          :params (mapv (fn [pd] {:desc pd :flags 0}) (first (t/parse-method-desc desc)))}
         more))

(declare resolve-members!)

(defn resolve-members!
  "Enters the fields and methods of n, declared and derived (§6). Supertypes in the
  compilation are entered first (for deftype-style signature inference)."
  [n]
  (let [d (decl! n)]
    (when-not (:members-done d)
      (update-decl! n assoc :members-done :in-progress)
      (doseq [s (cons (:super d) (:interfaces d)) :when (and s (decl s))]
        (resolve-members! s))
      (let [scope (class-scope n)
            kind (:kind d)
            members (:members d)
            fields (vec (for [m members :when (= :field (:kind m))] (enter-field n scope m)))
            methods (vec (for [m members :when (= :method (:kind m))] (enter-method n scope m)))
            ctors (vec (for [m members :when (= :ctor (:kind m))] (enter-ctor n scope m)))
            self-desc (t/internal->desc n)
            psf (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC Opcodes/ACC_FINAL)
            ;; enums
            enum-fields (when (= kind :enum)
                          (concat
                            (for [c (:constants d)]
                              {:name (:name c) :owner n :desc self-desc
                               :flags (bit-or psf Opcodes/ACC_ENUM (deprecated-flag (:meta c)))
                               :annotations (annotations scope (:meta c)) :enum-constant c})
                            [{:name "$VALUES" :owner n :desc (t/array-of self-desc)
                              :flags (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_FINAL
                                             Opcodes/ACC_SYNTHETIC)
                              :derived :enum-values-field}]))
            enum-methods (when (= kind :enum)
                           [(derived-method n "values" (str "()[" self-desc)
                                            (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC) :enum-values)
                            (derived-method n "valueOf" (str "(Ljava/lang/String;)" self-desc)
                                            (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC) :enum-valueOf
                                            :mparams [["name" Opcodes/ACC_MANDATED]])
                            (derived-method n "$values" (str "()[" self-desc)
                                            (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC)
                                            :enum-$values)])
            ;; records
            comps (when (= kind :record)
                    (vec (for [c (:components d)]
                           (let [tn (parse-type scope (or (tag-of c) 'java.lang.Object))]
                             {:name (name c) :sym c :tn tn :desc (erase scope tn) :meta (meta c)
                              :sig (when (t/generic? tn) (t/signature tn))
                              :annotations (annotations scope (meta c))}))))
            record-fields (for [c comps]
                            {:name (:name c) :owner n :desc (:desc c) :sig (:sig c)
                             :flags (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_FINAL)
                             :annotations (for-target (:annotations c) "FIELD") :component c})
            declared? (fn [nm desc] (some #(and (= nm (:name %)) (= desc (:desc %))) methods))
            record-methods
            (when (= kind :record)
              (concat
                (for [c comps :when (not (declared? (:name c) (str "()" (:desc c))))]
                  (derived-method n (:name c) (str "()" (:desc c)) Opcodes/ACC_PUBLIC :record-accessor
                                  :component c :sig (:sig (assoc c :sig (some->> (:sig c) (str "()"))))
                                  :annotations (for-target (:annotations c) "METHOD")))
                (for [[nm desc] [["toString" "()Ljava/lang/String;"] ["hashCode" "()I"]
                                 ["equals" "(Ljava/lang/Object;)Z"]]
                      :when (not (declared? nm desc))]
                  (derived-method n nm desc (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                                  :record-object-method))))
            canonical-desc (when comps (t/method-desc (map :desc comps) "V"))
            ctors (if (= kind :record)
                    (let [explicit (some #(when (and (not (:compact %)) (= canonical-desc (:desc %))) %) ctors)
                          compact (some #(when (:compact %) %) ctors)]
                      (if explicit
                        (mapv #(if (identical? % explicit) (assoc % :canonical true) %) ctors)
                        (let [base {:name "<init>" :owner n :desc canonical-desc :ret "V"
                                    :flags (if compact (:flags compact) (class-access d))
                                    :params (mapv (fn [c] {:sym (:sym c) :desc (:desc c) :tn (:tn c) :flags 0
                                                           :meta (:meta c)}) comps)
                                    :param-annotations (mapv #(for-target (:annotations %) "PARAMETER") comps)
                                    :sig (when (some t/generic? (map :tn comps))
                                           (str "(" (apply str (map (comp t/signature :tn) comps)) ")V"))
                                    :canonical true :kind :ctor :scope scope}]
                          (into [(if compact
                                   (merge compact base {:flags (:flags compact) :compact true})
                                   (assoc base :derived :record-canonical))]
                                (remove :compact ctors)))))
                    ctors)
            ctors (if (and (empty? ctors) (not (iface-kind? d)))
                    (let [desc (or (:anon-ctor-desc d) "()V")]
                      [{:name "<init>" :owner n :desc desc :ret "V"
                        :params (mapv (fn [pd] {:desc pd :flags 0}) (first (t/parse-method-desc desc)))
                        :flags (cond (or (= kind :enum) (:enum-body d)) Opcodes/ACC_PRIVATE
                                     ;; an anonymous constructor is variable arity when the
                                     ;; superclass constructor is (javac)
                                     (= :anon (:nesting d)) (bit-and (or (:flags (:anon-super-ctor d)) 0) Opcodes/ACC_VARARGS)
                                     :else (class-access d))
                        :derived (if (= :anon (:nesting d)) :anon-ctor :default-ctor)
                        ;; an anonymous constructor throws what the superclass constructor throws
                        :throws (vec (:throws (:anon-super-ctor d)))
                        :kind :ctor :scope scope}])
                    ctors)
            all-fields (vec (concat enum-fields record-fields fields))
            all-methods (vec (concat enum-methods record-methods methods ctors))]
        (update-decl! n assoc :fields all-fields :methods all-methods :components comps
                      :members-done true)))
    n))

;; ---------------------------------------------------------------------------------------------
;; constants (§5.4): values are boxed Java values of the exact type

(defn const-of-type
  "Converts constant value v to the Java value of descriptor t."
  [t v]
  (case t
    "I" (int (if (char? v) (int v) v))
    "J" (long (if (char? v) (int v) v))
    "S" (short (if (char? v) (int v) v))
    "B" (byte (if (char? v) (int v) v))
    "C" (char (if (char? v) v (int v)))
    "F" (float (if (char? v) (int v) v))
    "D" (double (if (char? v) (int v) v))
    "Z" (boolean v)
    v))

(defn fits? [t v]
  (and (integer? v)
       (case t
         "I" (<= Integer/MIN_VALUE v Integer/MAX_VALUE)
         "S" (<= Short/MIN_VALUE v Short/MAX_VALUE)
         "B" (<= Byte/MIN_VALUE v Byte/MAX_VALUE)
         "C" (<= 0 v 65535)
         "J" true
         false)))

(defn const-node [t v & {:as more}]
  (merge {:op :const :type t :val v} more))

;; ---------------------------------------------------------------------------------------------
;; types of expressions

(defn value-type [t] (if (= t "V") :null t))

(def widening
  {"B" #{"S" "I" "J" "F" "D"} "S" #{"I" "J" "F" "D"} "C" #{"I" "J" "F" "D"}
   "I" #{"J" "F" "D"} "J" #{"F" "D"} "F" #{"D"} "D" #{} "Z" #{}})

(defn widens? [from to] (or (= from to) (contains? (widening from) to)))

(defn boxed-desc [p] (t/internal->desc (t/box-of p)))

(declare literal-tail-vals)

(defn conversion
  "How a value of type `from` converts to `to` in assignment and invocation contexts:
  nil when it cannot, else a keyword: :none :widen :box :unbox :narrow-const :null."
  [node from to & {:keys [loose literal] :or {loose true literal true}}]
  (let [from (value-type from)]
    (cond
      (= from :none) :none
      (= from to) :none
      (= to "V") :none
      (= from :null) (when (t/ref? to) :none)
      (and (t/prim? from) (t/prim? to))
      (cond (widens? from to) :widen
            (and literal (= :const (:op node)) (contains? #{"I" "S" "B" "C"} to)
                 (or (#{"I" "S" "B" "C"} from) (and (= from "J") (:literal node)))
                 (fits? to (let [v (:val node)] (if (char? v) (int v) v))))
            :narrow-const
            ;; a conditional whose values are all integer literals
            (and literal (not= :const (:op node)) (contains? #{"I" "S" "B" "C"} to) (= from "J")
                 (when-let [vs (literal-tail-vals node)] (every? #(fits? to %) vs)))
            :narrow-const
            :else nil)
      (t/prim? from)
      (when (and loose (env/assignable? (boxed-desc from) to)) :box)
      (t/prim? to)
      (when loose
        (when-let [p (t/unbox-of from)] (when (widens? p to) :unbox)))
      :else (when (env/assignable? from to) :none))))

(defn unify
  "The type of a value that is one of two branches' values."
  [a b]
  (let [a (if (= a "V") :null a) b (if (= b "V") :null b)]
    (cond
      (= a :none) b
      (= b :none) a
      (= a b) a
      ;; as Clojure's if: branches of different primitive types are boxed each by its own type
      (and (t/prim? a) (t/prim? b)) (unify (boxed-desc a) (boxed-desc b))
      (t/prim? a) (unify (boxed-desc a) b)
      (t/prim? b) (unify a (boxed-desc b))
      :else (env/lub a b))))

;; integer literals (longs to Clojure) take an int type from the other branches, as Java's
;; int literals do

(defn literal-tail?
  "Is the value of node an integer literal that fits an int (in every tail)?"
  [node]
  (case (:op node)
    :const (and (:literal node) (fits? "I" (:val node)))
    :do (literal-tail? (:ret node))
    :let (literal-tail? (:body node))
    :if (and (or (= :none (:type (:then node))) (literal-tail? (:then node)))
             (or (= :none (:type (:else node))) (literal-tail? (:else node))))
    :switch (every? #(or (= :none (:type %)) (literal-tail? %))
                    (cons (or (:default node) {:op :none}) (map :body (:cases node))))
    false))

(defn literal-tail-vals
  "The values of the integer literal tails of node, or nil if some tail is no literal."
  [node]
  (when (literal-tail? node)
    (case (:op node)
      :const [(:val node)]
      :do (literal-tail-vals (:ret node))
      :let (literal-tail-vals (:body node))
      :if (concat (when-not (= :none (:type (:then node))) (literal-tail-vals (:then node)))
                  (when-not (= :none (:type (:else node))) (literal-tail-vals (:else node))))
      :switch (mapcat #(when-not (= :none (:type %)) (literal-tail-vals %))
                      (cons (:default node) (map :body (:cases node))))
      nil)))

(declare convert-node)

(defn literal-branch
  "A branch of integer literals takes the int-like type of the whole (as Java types them)."
  [node t]
  (if (and (#{"I" "S" "B" "C"} t) (= "J" (:type node)) (literal-tail? node))
    (convert-node node t)
    node))

(defn unify-nodes
  "The type of a value that is one of the nodes' values."
  [nodes]
  (let [lits (filter literal-tail? nodes)
        others (remove literal-tail? nodes)
        t (when (seq others) (reduce unify (map :type others)))]
    (if (and (seq lits) (#{"I" "S" "B" "C"} t)
             (every? (fn [n] (let [v (some-> (if (= :const (:op n)) n) :val)] (or (nil? v) (fits? t v)))) lits))
      t
      (reduce unify (map :type nodes)))))

;; ---------------------------------------------------------------------------------------------
;; analysis context
;;
;; actx: {:class internal name of the class whose code this is
;;        :static bool, static context
;;        :frame frame (a method, lambda or initializer body)
;;        :locals {sym binding}
;;        :ret return descriptor of the method or lambda
;;        :loop innermost loop target, :labels {kw target}
;;        :local-classes {sym internal}, :ns, :bounds
;;        :ctor-prologue bool: before the super./this. call}
;; frame: {:id :parent frame :class name :boundary holder :kind :method|:lambda|:class-init}
;; binding: {:id :sym :type :mutable :frame :receiver class-or-nil :cval const-node}

(defn new-frame [kind cls parent boundary]
  {:id (next-id) :kind kind :class cls :parent parent :boundary boundary})

(defn make-binding [actx sym type & {:as more}]
  (merge {:id (next-id) :sym sym :type type :frame (:frame actx)} more))

(defn with-local [actx b] (assoc-in actx [:locals (:sym b)] b))

(defn scope-of [actx]
  {:class (:class actx) :ns (:ns actx) :local-classes (:local-classes actx) :bounds (:bounds actx)})

(defn actx-desc [actx form] (type-desc (scope-of actx) form))

(defn resolve-class [actx sym] (resolve-class-sym (scope-of actx) sym))

;; boundaries: a class (local or anonymous) or a lambda that captures locals
(defn- add-capture! [boundary b]
  (case (:kind boundary)
    :class (swap! (state (:class boundary)) update :captures
                  (fn [cs] (if (some #(= (:id %) (:id b)) cs) cs (conj cs b))))
    :lambda (swap! (:captures boundary)
                   (fn [cs] (if (some #(= (:id %) (:id b)) cs) cs (conj cs b))))
    nil))

(declare outer-this-node)

(defn- local-ref [actx b]
  (cond
    (:cval b) (:cval b)
    (:receiver b) (outer-this-node actx (:receiver b))
    (identical? (:frame b) (:frame actx)) {:op :local :b b :type (:type b)}
    :else
    (do
      (when (:mutable b)
        (fail (str "Cannot capture mutable local " (:sym b))))
      (loop [f (:frame actx)]
        (when-not f (fail (str "Local out of scope: " (:sym b))))
        (when-not (identical? f (:frame b))
          (add-capture! (:boundary f) b)
          (recur (:parent f))))
      {:op :local :b b :type (:type b) :captured true})))

(defn outer-this-node
  "Java's T.this from actx: the path of this$0 hops from the current class to class T."
  [actx target]
  (loop [f (:frame actx) path [] via-lambda false]
    (when-not f
      (fail (str "No enclosing instance of type " (str/replace target "/" ".") " in scope")))
    (cond
      (= :lambda (:kind f))
      (do (reset! (:uses-this (:boundary f)) true)
          (recur (:parent f) path true))

      (= (:class f) target)
      (do (when (:static f)
            (fail (str "No enclosing instance of type " (str/replace target "/" ".")
                       " in a static context")))
          {:op :this-path :path path :type (t/internal->desc target)})

      :else
      (let [d (decl! (:class f))]
        (when-not (:outer-instance? d)
          (fail (str "No enclosing instance of type " (str/replace target "/" ".") " in scope")))
        ;; a constructor body reads its outer instance parameter, which needs no this$0 field
        (when-not (and (:ctor f) (empty? path) (not via-lambda))
          (swap! (:state d) assoc :uses-this0 true))
        (recur (:parent f) (conj path (:class f)) via-lambda)))))

(defn class-parent-frame
  "The frame enclosing the code of class n: where a local or anonymous class was declared, a
  stand-in for the outer class's instance code for member classes, nil for top-level ones."
  [n]
  (let [d (decl! n)]
    (case (:nesting d)
      (:local :anon) (:creation-frame @(:state d))
      :member {:id (next-id) :kind :outer :class (:outer d) :static (not (:outer-instance? d))
               :parent (class-parent-frame (:outer d))}
      nil)))

;; ---------------------------------------------------------------------------------------------
;; macroexpansion

(def clojure-specials
  '#{def loop* recur if case* let* letfn* do fn* quote var clojure.core/import* . set! deftype*
     reify* try throw monitor-enter monitor-exit catch finally new &})

(def arbace-specials
  '#{class* label* break* continue* return* switch* lambda* method-ref* java-str* java-assert*
     for-each* with-resources* if-instance* class-literal* super. this.})

(defn- core-var? [v]
  (and (var? v) (#{"clojure.core" "arbace.classes.core"} (name (ns-name (.ns ^clojure.lang.Var v))))))

(defn resolve-var [actx sym]
  (when (and (symbol? sym) (not (contains? (:locals actx) sym)))
    (let [v (try (ns-resolve (:ns actx) sym) (catch Exception _ nil))]
      (when (var? v) v))))

(defn core-op
  "The name of the core var a head symbol resolves to, or nil."
  [actx sym]
  (when-let [v (resolve-var actx sym)]
    (when (core-var? v) (name (.sym ^clojure.lang.Var v)))))

(defn macroexpand1 [actx form]
  (let [op (first form)]
    (if (or (not (symbol? op)) (contains? (:locals actx) op)
            (clojure-specials op) (arbace-specials op))
      form
      (let [v (resolve-var actx op)]
        (if (and v (:macro (meta v)))
          (apply @v form (zipmap (keys (:locals actx)) (repeat nil)) (rest form))
          form)))))

;; ---------------------------------------------------------------------------------------------
;; analysis

(declare accessorize)

(declare outer-super-call code-type-anns analyze-method-call*)

(declare clj-constant clj-collection clj-var-field)

(declare analyze analyze-body analyze-invoke analyze-new analyze-dot analyze-op analyze-class-form
         analyze-symbol coerce-hint needs-outer-instance? ctor-call-node)

(defn- with-form-info [form f]
  (try (f)
       (catch clojure.lang.ExceptionInfo e
         (if (or (:line (ex-data e)) (not (:line (meta form))))
           (throw e)
           (throw (ex-info (str (ex-message e) " (line " (:line (meta form)) ")")
                           (assoc (ex-data e) :line (:line (meta form))) e))))))

(defn analyze
  "Analyzes a form of code into a typed node."
  [actx form]
  (with-form-info form
    (fn []
      (let [node (cond
                   (symbol? form) (analyze-symbol actx form)
                   (seq? form) (if (empty? form) (fail "Empty list in class body") (analyze-op actx form))
                   (nil? form) (const-node :null nil)
                   (boolean? form) (const-node "Z" form)
                   (string? form) (const-node t/string-desc form)
                   (char? form) (const-node "C" form)
                   (instance? Long form) (const-node "J" form :literal true)
                   (instance? Integer form) (const-node "I" form)
                   (instance? Double form) (const-node "D" form)
                   (instance? Float form) (const-node "F" form)
                   (instance? Short form) (const-node "S" form)
                   (instance? Byte form) (const-node "B" form)
                   ;; Clojure data (§5.13): needs the Clojure runtime
                   (keyword? form) (clj-constant actx form)
                   (vector? form) (clj-collection actx "vector" "Lclojure/lang/IPersistentVector;" (seq form))
                   (map? form) (clj-collection actx "map" "Lclojure/lang/IPersistentMap;" (mapcat identity form))
                   (set? form) (clj-collection actx "set" "Lclojure/lang/IPersistentSet;" (seq form))
                   (instance? clojure.lang.BigInt form) (const-node :bigint form)
                   :else (fail (str "Not supported in class bodies at stage 0: " (pr-str form)
                                    " (" (.getName (class form)) ")")))]
        (let [node (accessorize actx node)]
          (if-let [tag (and (instance? clojure.lang.IMeta form) (:tag (meta form)))]
            (coerce-hint actx node tag)
            node))))))

(defn coerce-hint
  "A tag on an expression gives its static type, as in Clojure; a checkcast keeps the
  verifier happy where the type is not already known."
  [actx node tag]
  (let [d (actx-desc actx tag)]
    (cond
      (= d (:type node)) node
      (t/prim? d) node
      (#{:null :none} (:type node)) node
      ;; the tag gives the static type, also a supertype (Clojure's hint)
      (and (t/ref? (:type node)) (env/assignable? (:type node) d)) (assoc node :type d)
      (t/ref? (:type node)) {:op :cast :class d :expr node :type d}
      :else node)))

(defn analyze-body
  "Analyzes forms as a `do`."
  [actx forms]
  (let [forms (seq forms)]
    (cond
      (nil? forms) (const-node :null nil)
      (nil? (next forms)) (analyze actx (first forms))
      :else (let [stmts (mapv #(analyze actx %) (butlast forms))
                  ret (analyze actx (last forms))]
              {:op :do :statements stmts :ret ret
               :type (if (some #(= :none (:type %)) stmts) :none (:type ret))}))))

;; symbols ---------------------------------------------------------------------------------------

(defn- field-node [actx f target]
  (let [static? (static-flag? f)
        c (env/const-value f)]
    (if (and (some? c) (has? (:flags f) Opcodes/ACC_FINAL))
      (assoc (const-node (:desc f) c) :field f)
      {:op (if static? :get-static :get-field) :field f :owner (:owner f) :target target
       :type (:desc f)})))

(defn find-own-field
  "A field of the current class or an enclosing class body, innermost first:
  [field class-name] or nil."
  [actx sym]
  (loop [c (:class actx)]
    (when-let [d (and c (decl c))]
      (if-let [f (some #(when (= (name sym) (:name %)) %) (remove :derived (:fields d)))]
        [f c]
        (recur (:outer d))))))

(defn this-node [actx]
  (when (:static actx) (fail "No this in a static context"))
  (outer-this-node actx (:class actx)))

(defn- class-lit [actx d]
  {:op :class-lit :class d :type "Ljava/lang/Class;"})

(defn analyze-symbol [actx sym]
  (let [ns-part (namespace sym)]
    (cond
      (contains? (:locals actx) sym)
      (local-ref actx (get (:locals actx) sym))

      (and (nil? ns-part) (find-own-field actx sym))
      (let [[f c] (find-own-field actx sym)]
        (if (static-flag? f)
          (field-node actx f nil)
          (field-node actx f (outer-this-node actx c))))

      (and ns-part (= "this" (name sym)) (resolve-class actx (symbol ns-part)))
      (outer-this-node actx (resolve-class actx (symbol ns-part)))

      (and ns-part (re-matches #"[1-9]" (name sym)))
      (class-lit actx (actx-desc actx sym))

      (and ns-part (resolve-class actx (symbol ns-part)))
      (let [cn (resolve-class actx (symbol ns-part))
            f (env/find-field cn (name sym))]
        (when-not (and f (static-flag? f))
          (fail (str "No static field " (name sym) " in " cn)))
        (when-not (env/accessible? (:class actx) f)
          (fail (str "Field not accessible: " cn "." (name sym))))
        (field-node actx (assoc f :owner cn :declarer (:owner f)) nil))

      (and (nil? ns-part) (t/prim-names sym))
      (fail (str "A primitive type is no value: " sym " (use Integer/TYPE)"))

      (resolve-class actx sym)
      (class-lit actx (t/internal->desc (resolve-class actx sym)))

      (and (nil? ns-part) (re-find #"\." (name sym)) (env/load-class (str/replace (name sym) "." "/")))
      (class-lit actx (t/internal->desc (str/replace (name sym) "." "/")))

      (resolve-var actx sym)
      {:op :var-deref :var (resolve-var actx sym) :field (clj-var-field actx (resolve-var actx sym))
       :owner (:class actx) :type t/object-desc}

      :else (fail (str "Unable to resolve symbol: " sym)))))

;; operators --------------------------------------------------------------------------------------

(def ^:private arith-ops
  ;; name -> [family op]; families: :int, :long (Clojure: double when a double operand), :float
  {"unchecked-add-int" [:int :add] "unchecked-subtract-int" [:int :sub]
   "unchecked-multiply-int" [:int :mul] "unchecked-divide-int" [:int :div]
   "unchecked-remainder-int" [:int :rem] "unchecked-negate-int" [:int :neg]
   "unchecked-inc-int" [:int :inc] "unchecked-dec-int" [:int :dec]
   "bit-and-int" [:int :and] "bit-or-int" [:int :or] "bit-xor-int" [:int :xor]
   "bit-not-int" [:int :not] "bit-shift-left-int" [:int :shl] "bit-shift-right-int" [:int :shr]
   "unsigned-bit-shift-right-int" [:int :ushr]
   "unchecked-add" [:long :add] "unchecked-subtract" [:long :sub] "unchecked-multiply" [:long :mul]
   "unchecked-divide" [:long :div] "unchecked-remainder" [:long :rem]
   "unchecked-negate" [:long :neg] "unchecked-inc" [:long :inc] "unchecked-dec" [:long :dec]
   "bit-and" [:longbits :and] "bit-or" [:longbits :or] "bit-xor" [:longbits :xor]
   "bit-not" [:longbits :not] "bit-shift-left" [:longbits :shl] "bit-shift-right" [:longbits :shr]
   "unsigned-bit-shift-right" [:longbits :ushr] "bit-and-not" [:longbits :andnot]
   "unchecked-add-float" [:float :add] "unchecked-subtract-float" [:float :sub]
   "unchecked-multiply-float" [:float :mul] "unchecked-divide-float" [:float :div]
   "unchecked-remainder-float" [:float :rem] "unchecked-negate-float" [:float :neg]
   ;; Clojure's checked arithmetic on primitives: exact on long, IEEE on double
   "+" [:exact :add] "-" [:exact :sub] "*" [:exact :mul] "inc" [:exact :inc] "dec" [:exact :dec]
   "quot" [:exact :div] "rem" [:exact :rem]})

(def ^:private conversions
  {"unchecked-int" "I" "unchecked-long" "J" "unchecked-short" "S" "unchecked-byte" "B"
   "unchecked-char" "C" "unchecked-float" "F" "unchecked-double" "D"})

(def ^:private checked-conversions
  {"int" "I" "long" "J" "short" "S" "byte" "B" "char" "C" "float" "F" "double" "D"})

(def ^:private comparisons #{"<" "<=" ">" ">=" "=="})

(defn- unboxed [node]
  (let [t (value-type (:type node))]
    (if-let [p (t/unbox-of t)]
      {:op :convert :expr node :from t :to p :type p}
      node)))

(defn- prim-type [node] (:type (unboxed node)))

(defn convert-node
  "Converts node to type `to` (an assignment conversion; a const is converted at compile time)."
  [node to & {:keys [what] :or {what "value"}}]
  (let [from (:type node)
        c (conversion node from to)]
    (cond
      (and (or (nil? c) (and (= c :narrow-const) (not= :const (:op node))))
           (#{:do :let :if :switch :label :if-instance :try :loop} (:op node))
           (or (t/prim? to) (t/ref? to)))
      ;; tails of a block take the context's type (literal narrowing in branches)
      (case (:op node)
        :switch (let [cs (mapv #(update % :body convert-node to :what what) (:cases node))
                      d (some-> (:default node) (convert-node to :what what))]
                  (when-not d (fail (str "switch without default has no value of type " to)))
                  (assoc node :cases cs :default d :type (unify-nodes (cons d (map :body cs)))))
        (:label :loop) (let [b (convert-node (:body node) to :what what)]
                         (doseq [v @(:breaks (:target node))]
                           (when-not (conversion v (:type v) to)
                             (fail (str "Cannot convert " what " of type " (pr-str (:type v)) " to " to))))
                         (assoc node :body b :type to))
        :if-instance (let [th (convert-node (:then node) to :what what)
                           el (convert-node (:else node) to :what what)]
                       (assoc node :then th :else el :type (unify (:type th) (:type el))))
        :try (let [b (convert-node (:body node) to :what what)
                   cs (mapv #(update % :body convert-node to :what what) (:catches node))]
               (assoc node :body b :catches cs :type to))
        :do (let [r (convert-node (:ret node) to :what what)] (assoc node :ret r :type (if (= :none (:type node)) :none to)))
        :let (let [b (convert-node (:body node) to :what what)] (assoc node :body b :type (:type b)))
        :if (let [th (convert-node (:then node) to :what what)
                  el (convert-node (:else node) to :what what)]
              (assoc node :then th :else el :type (unify (:type th) (:type el)))))
      (nil? c) (fail (str "Cannot convert " what " of type " (pr-str (value-type from)) " to " to)
                     {:from from :to to})
      (= c :none) node
      (and (= :const (:op node)) (t/prim? to) (t/prim? (value-type from)))
      (const-node to (const-of-type to (:val node)))
      :else {:op :convert :expr node :from (value-type from) :to to :type to})))

(defn- operand
  "Converts an operand to the operation type t: numeric promotion, literals narrowed."
  [node t]
  (let [n (unboxed node)
        from (:type n)]
    (cond
      (= from t) n
      (and (= :const (:op n)) (= t "I") (= from "J") (:literal n) (fits? "I" (:val n)))
      (const-node "I" (int (:val n)))
      (and (= t "I") (= from "J") (some->> (literal-tail-vals n) (every? #(fits? "I" %))))
      (convert-node n "I")
      (and (t/prim? from) (widens? from t))
      (if (= :const (:op n)) (const-node t (const-of-type t (:val n))) {:op :convert :expr n :from from :to t :type t})
      :else (fail (str "Operand of type " (pr-str from) " where " t " is needed")))))

(defn- fold-arith [t op a b]
  (try
    (case t
      "I" (let [a (int a) b (when (some? b) (int b))]
            (case op :add (unchecked-add-int a b) :sub (unchecked-subtract-int a b)
                  :mul (unchecked-multiply-int a b) :div (unchecked-divide-int a b)
                  :rem (unchecked-remainder-int a b) :neg (unchecked-negate-int a)
                  :inc (unchecked-inc-int a) :dec (unchecked-dec-int a)
                  :and (int (bit-and a b)) :or (int (bit-or a b)) :xor (int (bit-xor a b))
                  :not (int (bit-not a))
                  :shl (unchecked-int (bit-shift-left a (bit-and b 31)))
                  :shr (int (bit-shift-right a (bit-and b 31)))
                  :ushr (unchecked-int (unsigned-bit-shift-right (bit-and (long a) 0xffffffff) (bit-and b 31)))))
      "J" (let [a (long a) b (when (some? b) (long b))]
            (case op :add (unchecked-add a b) :sub (unchecked-subtract a b)
                  :mul (unchecked-multiply a b) :div (quot a b) :rem (rem a b)
                  :neg (unchecked-negate a) :inc (unchecked-inc a) :dec (unchecked-dec a)
                  :and (bit-and a b) :or (bit-or a b) :xor (bit-xor a b) :not (bit-not a)
                  :andnot (bit-and-not a b)
                  :shl (bit-shift-left a (bit-and b 63)) :shr (bit-shift-right a (bit-and b 63))
                  :ushr (unsigned-bit-shift-right a (bit-and b 63))))
      "F" (let [a (float a) b (when (some? b) (float b))]
            (case op :add (float (+ a b)) :sub (float (- a b)) :mul (float (* a b))
                  :div (float (/ (double a) (double b))) :rem (float (Math/IEEEremainder 0 1))
                  :neg (float (- a)) nil))
      "D" (let [a (double a) b (when (some? b) (double b))]
            (case op :add (+ a b) :sub (- a b) :mul (* a b) :div (/ a b)
                  :neg (- a) :inc (inc a) :dec (dec a) nil)))
    (catch ArithmeticException _ nil)))

(defn- analyze-arith [actx opname args]
  (let [[family op] (arith-ops opname)
        nodes (mapv #(unboxed (analyze actx %)) args)
        types (map :type nodes)
        unary? (#{:neg :inc :dec :not} op)
        _ (when-not (= (count nodes) (if unary? 1 2))
            (if (and (= family :exact) (#{:add :mul} op) (not= 2 (count nodes)))
              nil
              (fail (str opname " takes " (if unary? 1 2) " operand(s) in class bodies"))))
        _ (doseq [n nodes]
            (when-not (t/numeric? (:type n))
              (fail (str "Operand of " opname " has non-primitive type " (pr-str (:type n))
                         " (only primitives and their wrappers are arithmetic in class bodies)"))))
        shift? (#{:shl :shr :ushr} op)
        t (case family
            :int (do (doseq [n nodes]
                       (when-not (or (t/int-like? (:type n))
                                     (and (= :const (:op n)) (:literal n) (fits? "I" (:val n)))
                                     (some->> (literal-tail-vals n) (every? #(fits? "I" %))))
                         (fail (str opname ": operand of type " (:type n) " needs explicit narrowing"))))
                     "I")
            :longbits (do (doseq [n nodes]
                            (when-not (#{"I" "S" "B" "C" "J"} (:type n))
                              (fail (str opname ": operand of type " (:type n) " is not integral"))))
                          "J")
            :long (if (some #{"D" "F"} types) "D" "J")
            :exact (if (some #{"D" "F"} types) "D" "J")
            :float (do (when (some #{"D"} types)
                         (fail (str opname ": a double operand needs explicit narrowing")))
                       "F"))
        ops (if shift?
              [(operand (first nodes) t)
               (let [c (second nodes)]
                 (operand c (if (and (= t "J") (not (t/int-like? (:type c)))
                                     (not (and (= :const (:op c)) (:literal c) (fits? "I" (:val c)))))
                              "J" "I")))]
              (mapv #(operand % t) nodes))]
    (when (and (= family :exact) (#{:div :rem} op) (= t "D"))
      (fail (str opname " on doubles is not Java arithmetic; use unchecked-divide/remainder")))
    (if (every? #(= :const (:op %)) ops)
      (if-let [v (and (not= family :exact) (fold-arith t op (:val (first ops)) (:val (second ops))))]
        (const-node t v)
        {:op :arith :family family :o op :args ops :type t})
      {:op :arith :family family :o op :args ops :type t})))

(defn- analyze-conversion [actx to arg checked?]
  (let [n (unboxed (analyze actx arg))
        from (:type n)]
    (cond
      ;; (unchecked-long 0x8000000000000000): a big literal as Java's long or int literal
      (= from :bigint)
      (if checked?
        (fail (str "Literal out of range: " (:val n)))
        (let [l (.longValue ^Number (:val n))]
          (const-node to (case to "I" (unchecked-int l) "J" l "S" (unchecked-short l) "B" (unchecked-byte l)
                               "C" (unchecked-char l) "F" (unchecked-float l) "D" (unchecked-double l)))))
      (= from to) n
      (not (t/prim? from)) (fail (str "Cannot convert " (pr-str from) " to " to))
      (and (= :const (:op n)) (t/prim? from) (not= from "Z"))
      (const-node to (let [v (:val n)
                           v (if (char? v) (int v) v)]
                       (case to
                         "I" (unchecked-int v) "J" (unchecked-long v) "S" (unchecked-short v)
                         "B" (unchecked-byte v) "C" (unchecked-char v) "F" (unchecked-float v)
                         "D" (unchecked-double v))))
      (and checked? (not (widens? from to)))
      (fail (str "Narrowing " from " to " to " needs unchecked-" ({"I" "int" "J" "long" "S" "short" "B" "byte" "C" "char" "F" "float" "D" "double"} to)))
      (= from "Z") (fail "Cannot convert boolean to a number")
      :else {:op :convert :expr n :from from :to to :type to})))

(defn- compare-type [a b]
  (let [ta (:type a) tb (:type b)]
    (cond
      (= ta tb) ta
      (or (= ta "D") (= tb "D") (= ta "F") (= tb "F")) "D"
      (or (= ta "J") (= tb "J")) "J"
      :else "I")))

(defn- analyze-compare [actx op args & [nodes]]
  (when-not (= 2 (count (or nodes args)))
    (fail (str op " takes 2 operands in class bodies")))
  (let [[a b] (map unboxed (or nodes (map #(analyze actx %) args)))]
    (doseq [n [a b]]
      (when-not (t/numeric? (:type n))
        (fail (str op ": operand of type " (pr-str (:type n)) " is not a number"))))
    (let [lit-int? (fn [x y] (and (= :const (:op x)) (:literal x) (fits? "I" (:val x))
                                  (t/int-like? (:type y))))
          ct (let [ta (:type a) tb (:type b)]
               (cond (and (= ta "F") (= tb "F")) "F"
                     (or (lit-int? a b) (lit-int? b a)) "I"
                     :else (compare-type a b)))
          ct (if (t/int-like? ct) "I" ct)]
      (let [[x y :as args] [(operand a ct) (operand b ct)]]
        (if (and (= :const (:op x)) (= :const (:op y)))
          ;; a constant expression (JLS 15.29), as javac folds it
          (let [xv (let [v (:val x)] (if (char? v) (int v) v))
                yv (let [v (:val y)] (if (char? v) (int v) v))]
            (const-node "Z" (boolean (case op "<" (< xv yv) "<=" (<= xv yv) ">" (> xv yv)
                                           ">=" (>= xv yv) "==" (== xv yv)))))
          {:op :compare :cmp (keyword op) :t ct :args args :type "Z"})))))

(defn- bool-node [actx form]
  (let [n (analyze actx form)] n))

;; locals ------------------------------------------------------------------------------------------

(defn- lambda-form? [f]
  (and (seq? f) (symbol? (first f)) (#{"lambda" "lambda*" "method-ref" "method-ref*"} (name (first f)))))

(declare annotations-now annotation-targets)

(defn- local-type-anns
  "Type annotations of a local variable: TYPE_USE annotations on the binding symbol and those
  inside its tag."
  [actx sym]
  (let [m (meta sym)
        sc (scope-of actx)
        tn (when-let [tg (:tag m)] (parse-type sc tg))]
    (vec (concat
           (for [a (annotations-now sc (into {} (filter (comp symbol? key) m)))
                 :let [ts (annotation-targets (t/desc->internal (:type a)))]
                 :when (and ts (ts "TYPE_USE"))]
             [(if tn (t/element-path tn) "") a])
           (when tn
             (for [[path anns] (t/type-anns tn) a (annotations-now sc anns)] [path a]))))))

(defn- binding-type
  "The type of a let/loop local: its tag, else its initializer's type (Clojure's loop widens
  untagged int and float)."
  [actx sym init loop?]
  (let [tag (:tag (meta sym))]
    (if tag
      (actx-desc actx tag)
      (let [it (value-type (:type init))]
        (cond (= it :null) t/object-desc
              (= it :none) t/object-desc
              (and loop? (= it "I")) "J"
              (and loop? (= it "F")) "D"
              :else it)))))

(defn- bind-init [actx sym init type]
  (let [c (conversion init (:type init) type)]
    (cond
      (some? c) (convert-node init type)
      (and (t/ref? type) (t/ref? (value-type (:type init))))
      {:op :cast :class type :expr init :type type}
      :else (convert-node init type :what (str "initializer of " sym)))))

(defn- analyze-let [actx [_ bindings & body] loop?]
  (when-not (and (vector? bindings) (even? (count bindings)))
    (fail "let/loop needs a vector of bindings"))
  (let [[actx bs]
        (reduce (fn [[actx bs] [sym init]]
                  (when-not (and (symbol? sym) (nil? (namespace sym)))
                    (fail (str "Bad binding: " sym)))
                  (let [m (meta sym)
                        init-node (analyze (if (lambda-form? init) (assoc actx :pending-var sym) actx) init)
                        type (binding-type actx sym init-node loop?)
                        init-node (bind-init actx sym init-node type)
                        cval (when (and (:const m) (= :const (:op init-node))) init-node)
                        b (make-binding actx sym type :mutable (boolean (:mutable m)) :cval cval
                                        :loop-local loop?
                                        :type-anns (local-type-anns actx sym))]
                    (when (and (:const m) (not cval))
                      (fail (str "^:const local needs a constant initializer: " sym)))
                    [(with-local actx b) (conj bs [b init-node])]))
                [actx []]
                (partition 2 bindings))]
    (if loop?
      (let [target {:id (next-id) :kind :loop :bindings (mapv first bs) :breaks (atom [])}
            actx (cond-> (-> actx (assoc :loop target :break-target target) (dissoc :label-loop))
                   (:label-loop actx) (assoc-in [:labels (:label-loop actx) :loop] target))
            body (analyze-body actx body)
            ;; a loop's value: its body's value, or nil from a (break)
            bt (unify-nodes (cons body @(:breaks target)))
            bt (if (and (= :none (:type body)) (empty? @(:breaks target))) :none bt)]
        {:op :loop :target target :bindings bs :body body :type bt})
      (let [body (analyze-body actx body)]
        {:op :let :bindings bs :body body :type (:type body)}))))

;; control flow ---------------------------------------------------------------------------------------

(defn- analyze-if [actx [_ test then else :as form]]
  (when (> (count form) 4) (fail "Too many arguments to if"))
  (let [tn (if (keyword? test) (const-node "Z" true) (analyze actx test))]
    (if (and (= :const (:op tn)) (or (boolean? (:val tn)) (nil? (:val tn))))
      ;; a constant condition: the dead branch is left out, as javac does
      (if (:val tn) (analyze actx then) (analyze actx else))
      (let [thn (analyze actx then)
            eln (analyze actx else)
            ty (unify-nodes [thn eln])]
        {:op :if :test tn :then (literal-branch thn ty) :else (literal-branch eln ty) :type ty}))))

(defn- analyze-recur [actx args label]
  (let [target (if label
                 (let [l (or (get-in actx [:labels label]) (fail (str "continue: no label " label)))]
                   (or (:loop l) (fail (str "continue: label " label " is not a loop"))))
                 (or (:loop actx) (fail "recur/continue outside a loop")))
        bs (:bindings target)]
    (when-not (= (count args) (count bs))
      (fail (str "Mismatched argument count to recur/continue, expected: " (count bs)
                 " args, got: " (count args))))
    {:op :recur :target target
     :args (mapv (fn [b a] (convert-node (analyze actx a) (:type b) :what "recur argument")) bs args)
     :type :none}))

(defn- expand-to-loop
  "Macroexpands form until it is a loop* (or for-each*) form; nil if it does not become one."
  [actx form]
  (loop [f form]
    (when (seq? f)
      (cond (#{'loop* 'for-each*} (first f)) f
            :else (let [e (macroexpand1 actx f)] (when-not (identical? e f) (recur e)))))))

(defn- analyze-label [actx [_ kw & body]]
  (when-not (keyword? kw) (fail "label needs a keyword"))
  (let [target {:id (next-id) :kind :label :kw kw :breaks (atom [])}
        actx2 (assoc-in actx [:labels kw] target)
        lp (when (= 1 (count body)) (expand-to-loop actx (first body)))
        bn (if lp
             (analyze (assoc actx2 :label-loop kw) lp)
             (analyze-body actx2 body))
        bt (unify-nodes (cons bn @(:breaks target)))]
    {:op :label :target target :body bn :type bt}))

(defn- analyze-break [actx [_ & args]]
  (let [[kw v] (if (keyword? (first args)) [(first args) (second args)] [nil (first args)])
        target (if kw
                 (or (get-in actx [:labels kw]) (fail (str "break: no label " kw)))
                 (or (:break-target actx) (fail "break outside a loop")))
        vn (analyze actx v)]
    (swap! (:breaks target) conj vn)
    {:op :break :target target :val vn :type :none}))

(defn- analyze-return [actx [_ v :as form]]
  (when-not (:ret actx) (fail "return outside a method"))
  (let [vn (if (= (:ret actx) "V")
             (if (> (count form) 1) (analyze actx v) nil)
             (convert-node (analyze actx v) (:ret actx) :what "return value"))]
    {:op :return :val vn :type :none}))

(defn- analyze-throw [actx [_ e]]
  (let [en (analyze actx e)]
    (when-not (and (t/ref? (:type en)) (or (= :null (:type en)) (env/assignable? (:type en) "Ljava/lang/Throwable;")))
      (fail (str "throw of a non-Throwable: " (pr-str (:type en)))))
    {:op :throw :expr en :type :none}))

(defn- analyze-try [actx [_ & forms]]
  (let [catch? #(and (seq? %) (= 'catch (first %)))
        fin? #(and (seq? %) (= 'finally (first %)))
        body (remove #(or (catch? %) (fin? %)) forms)
        catches (filter catch? forms)
        fin (first (filter fin? forms))
        bn (analyze-body actx body)
        cns (vec (for [[_ cls sym & cbody] catches]
                   (let [classes (mapv #(or (resolve-class actx %) (fail (str "Unknown class: " %)))
                                       (if (vector? cls) cls [cls]))
                         ltype (reduce env/lub (map t/internal->desc classes))
                         b (make-binding actx sym ltype)]
                     {:classes classes :b b :body (analyze-body (with-local actx b) cbody)
                      :type-anns (mapv #(code-type-anns actx %) (if (vector? cls) cls [cls]))})))
        fn (when fin (analyze-body actx (rest fin)))]
    {:op :try :body bn :catches cns :finally fn
     :type (unify-nodes (cons bn (map :body cns)))}))

(defn- analyze-locking [actx [_ x & body]]
  (let [ln (analyze actx x)]
    (when-not (t/ref? (:type ln)) (fail "locking needs a reference"))
    (let [bn (analyze-body actx body)]
      {:op :monitor :lock ln :body bn :type (:type bn)})))

;; set! -----------------------------------------------------------------------------------------------

(declare analyze-field-access super-node)

(defn- analyze-set! [actx [_ target v]]
  (cond
    (and (symbol? target) (contains? (:locals actx) target))
    (let [b (get (:locals actx) target)]
      (when-not (identical? (:frame b) (:frame actx))
        (fail (str "Cannot assign captured local " target)))
      (when-not (:mutable b) (fail (str "Cannot assign immutable local " target " (use ^:mutable)")))
      (let [vn (convert-node (analyze actx v) (:type b) :what (str "value for " target))]
        {:op :set-local :b b :val vn :type (:type b)}))

    (and (symbol? target) (nil? (namespace target)) (find-own-field actx target))
    (let [[f c] (find-own-field actx target)
          vn (convert-node (analyze actx v) (:desc f) :what (str "value for " target))]
      (if (static-flag? f)
        {:op :set-static :field f :owner (:owner f) :val vn :type (:desc f)}
        {:op :set-field :field f :owner (:owner f) :target (outer-this-node actx c) :val vn
         :type (:desc f)}))

    (and (symbol? target) (namespace target))
    (let [cn (or (resolve-class actx (symbol (namespace target)))
                 (fail (str "Unknown class in set!: " target)))
          f (env/find-field cn (name target))]
      (when-not (and f (static-flag? f)) (fail (str "No static field " target)))
      {:op :set-static :field f :owner cn :type (:desc f)
       :val (convert-node (analyze actx v) (:desc f))})

    (and (seq? target) (let [h (first target)] (and (symbol? h) (str/starts-with? (name h) ".-"))))
    (let [fname (subs (name (first target)) 2)
          tn (if (= 'super (second target)) (super-node actx) (analyze actx (second target)))]
      (analyze-field-access actx tn fname (fn [f owner]
                                            {:op :set-field :field f :owner owner :target tn
                                             :val (convert-node (analyze actx v) (:desc f)) :type (:desc f)})))

    (and (seq? target) (= '. (first target)))
    (let [[_ obj fsym] target
          fname (str/replace (name (if (seq? fsym) (first fsym) fsym)) #"^-" "")]
      (if-let [cn (and (symbol? obj) (not (contains? (:locals actx) obj)) (resolve-class actx obj))]
        (analyze-set! actx (list 'set! (symbol (str/replace cn "/" ".") fname) v))
        (let [tn (analyze actx obj)]
          (analyze-field-access actx tn fname (fn [f owner]
                                                {:op :set-field :field f :owner owner :target tn
                                                 :val (convert-node (analyze actx v) (:desc f))
                                                 :type (:desc f)})))))

    :else (fail (str "Bad set! target: " (pr-str target)))))

;; interop --------------------------------------------------------------------------------------------

(defn- applicable? [phase arg-nodes param-descs]
  (every? true?
          (map (fn [a p]
                 (let [c (conversion a (:type a) p :loose (not= phase :strict) :literal (= phase :literal))]
                   (boolean (and c (case phase
                                     :strict (#{:none :widen} c)
                                     (:loose :varargs) (#{:none :widen :box :unbox} c)
                                     :literal true)))))
               arg-nodes param-descs)))

(defn- more-specific? [p1 p2]
  (every? true? (map (fn [a b] (or (= a b) (and (t/prim? a) (t/prim? b) (widens? a b))
                                   (and (t/ref? a) (t/ref? b) (env/assignable? a b))))
                     p1 p2)))

(defn- expand-varargs [params n]
  (let [fixed (vec (butlast params))
        el (t/elem-type (last params))]
    (into fixed (repeat (- n (count fixed)) el))))

(defn- param-tags-match? [actx tags m]
  (let [[ps _] (t/parse-method-desc (:desc m))]
    (and (= (count tags) (count ps))
         (every? true? (map (fn [tag p] (or (= tag '_) (= p (actx-desc actx tag)))) tags ps)))))

(defn select-method
  "Chooses among candidate methods for argument nodes (JLS 15.12.2 on erased types: strict,
  loose, then variable arity invocation; then literal narrowing). Returns [method varargs?]."
  [actx cands args what param-tags]
  (let [n (count args)
        cands (if param-tags (filter #(param-tags-match? actx param-tags %) cands) cands)
        pdescs (fn [m] (first (t/parse-method-desc (:desc m))))
        varargs? #(has? (:flags %) Opcodes/ACC_VARARGS)
        pick (fn [ms params-of]
               (when (seq ms)
                 (let [best (filter (fn [m] (every? #(more-specific? (params-of m) (params-of %)) ms)) ms)]
                   (cond (= 1 (count (distinct (map :desc best)))) (first best)
                         (empty? best) (fail (str "Ambiguous call: " what " " (pr-str (map :desc ms))))
                         :else (fail (str "Ambiguous call: " what " " (pr-str (map :desc best))))))))
        fixed (filter #(= n (count (pdescs %))) cands)]
    (if param-tags
      (let [m (first cands)]
        (when-not m (fail (str "No method matches the param-tags of " what)))
        [m (and (varargs? m) (not (and (= n (count (pdescs m))) (applicable? :loose args (pdescs m)))))])
      (or (when-let [m (pick (filter #(applicable? :strict args (pdescs %)) fixed) pdescs)] [m false])
          (when-let [m (pick (filter #(applicable? :loose args (pdescs %)) fixed) pdescs)] [m false])
          (let [vs (filter #(and (varargs? %) (>= n (dec (count (pdescs %))))
                                 (applicable? :varargs args (expand-varargs (pdescs %) n)))
                           cands)]
            (when-let [m (pick vs #(expand-varargs (pdescs %) n))] [m true]))
          (when-let [m (pick (filter #(applicable? :literal args (pdescs %)) fixed) pdescs)] [m false])
          (let [vs (filter #(and (varargs? %) (>= n (dec (count (pdescs %))))
                                 (applicable? :literal args (expand-varargs (pdescs %) n)))
                           cands)]
            (when-let [m (pick vs #(expand-varargs (pdescs %) n))] [m true]))
          (fail (str "No matching " what " for argument types " (pr-str (mapv (comp value-type :type) args))
                     (when (seq cands) (str "; candidates: " (pr-str (map :desc cands))))))))))

(defn convert-args
  "Converts argument nodes to the parameter types of method m (packing variable arity
  arguments into an array)."
  [m args varargs?]
  (let [[ps _] (t/parse-method-desc (:desc m))]
    (if varargs?
      (let [nfixed (dec (count ps))
            el (t/elem-type (last ps))]
        (conj (mapv #(convert-node %1 %2) (take nfixed args) ps)
              {:op :array-init :type (last ps) :elems (mapv #(convert-node % el) (drop nfixed args))}))
      (mapv #(convert-node %1 %2 :what "argument") args ps))))

(defn- qualifying-owner
  "JLS 13.1: the class named in the instruction is the receiver's static type, except for
  members of Object."
  [static-type m]
  (cond
    ;; javac qualifies Object's methods by the receiver's type when it is an interface
    (and (t/class-desc? static-type) (env/interface? (t/desc->internal static-type))
         (not (static-flag? m)))
    (t/desc->internal static-type)
    (= (:owner m) "java/lang/Object") "java/lang/Object"
    (t/array? static-type) (:owner m)
    (has? (:flags m) Opcodes/ACC_STATIC) (if (has? (:flags m) Opcodes/ACC_SYNTHETIC) (:owner m) (t/desc->internal static-type))
    :else (t/desc->internal static-type)))

(defn invoke-node [actx kind owner-desc m target args varargs?]
  (let [[_ ret] (t/parse-method-desc (:desc m))
        owner (qualifying-owner owner-desc m)
        itf (env/interface? owner)]
    {:op :invoke :kind (cond (= kind :special) :special
                             (static-flag? m) :static
                             itf :interface
                             :else :virtual)
     :owner owner :itf itf :name (:name m) :desc (:desc m) :target target
     :args (convert-args m args varargs?) :type ret :method m}))

(defn- signature-polymorphic?
  "JVMS 2.9.3: a native variable arity method of MethodHandle or VarHandle taking Object[]."
  [m]
  (and (#{"java/lang/invoke/MethodHandle" "java/lang/invoke/VarHandle"} (:owner m))
       (has? (:flags m) Opcodes/ACC_NATIVE) (has? (:flags m) Opcodes/ACC_VARARGS)
       (= "[Ljava/lang/Object;" (first (first (t/parse-method-desc (:desc m)))))
       (= 1 (count (first (t/parse-method-desc (:desc m)))))))

(defn- reflective-call
  "With ^{:reflection :warn} on the class, a call the compiler cannot resolve goes through
  clojure.lang.Reflector at run time, with a warning (SPEC §12 question 7)."
  [actx target mname arg-nodes why]
  (binding [*out* *err*]
    (println (str "Reflection warning, " (str/replace (:class actx) "/" ".") " - call to method "
                  mname " can't be resolved (" why ").")))
  {:op :invoke :kind :static :owner "clojure/lang/Reflector" :itf false :name "invokeInstanceMethod"
   :desc "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;"
   :target nil
   :args [(convert-node target t/object-desc) (const-node t/string-desc mname)
          {:op :array-init :type "[Ljava/lang/Object;" :elems (mapv #(convert-node % t/object-desc) arg-nodes)}]
   :type t/object-desc})

(defn analyze-method-call
  "(.name target args) where target is a node of static type `tt`."
  [actx target mname args & {:keys [special param-tags owner-override ret-tag]}]
  (if (and (= :warn (:reflection actx)) (not special))
    (try (analyze-method-call* actx target mname args :param-tags param-tags :owner-override owner-override
                               :ret-tag ret-tag)
         (catch clojure.lang.ExceptionInfo e
           (if (and (:arbace/compile-error (ex-data e))
                    (re-find #"^(No accessible method|No matching method|Ambiguous call)" (ex-message e))
                    (t/ref? (value-type (:type target))))
             (reflective-call actx target mname (mapv #(analyze actx %) args) (ex-message e))
             (throw e))))
    (analyze-method-call* actx target mname args :special special :param-tags param-tags
                          :owner-override owner-override :ret-tag ret-tag)))

(defn analyze-method-call*
  [actx target mname args & {:keys [special param-tags owner-override ret-tag]}]
  (let [tt (value-type (:type target))
        _ (when-not (t/ref? tt) (fail (str "Method call ." mname " on a value of type " (pr-str tt))))
        _ (when (= tt :null) (fail (str "Method call ." mname " on nil")))
        cn (if (t/array? tt) "java/lang/Object" (t/desc->internal tt))
        arg-nodes (mapv #(analyze actx %) args)]
    (if (and (t/array? tt) (= mname "clone") (empty? args))
      {:op :invoke :kind :virtual :owner tt :itf false :name "clone" :desc "()Ljava/lang/Object;"
       :target target :args [] :type tt :array-clone true}
      (let [cands (->> (env/member-methods cn mname)
                       (remove static-flag?)
                       (filter #(env/accessible? (:class actx) %)))
            _ (when (empty? cands)
                (fail (str "No accessible method " mname " in " (str/replace cn "/" "."))))
            poly (when (and (= 1 (count cands)) (signature-polymorphic? (first cands))) (first cands))]
        (if poly
          ;; the call site's descriptor: the param-tags (or the arguments' types), the tag of the
          ;; call form (or Object) as return type
          (let [ps (if param-tags (mapv #(actx-desc actx %) param-tags)
                       (mapv #(value-type (:type %)) arg-nodes))
                ret (if ret-tag (actx-desc actx ret-tag) t/object-desc)
                desc (t/method-desc ps ret)]
            {:op :invoke :kind :virtual :owner (:owner poly) :itf false :name mname :desc desc
             :target target :args (mapv #(convert-node %1 %2) arg-nodes ps) :type ret})
          (let [[m va] (select-method actx cands arg-nodes (str "method " mname) param-tags)]
            (invoke-node actx (when special :special) (or owner-override tt) m target arg-nodes va)))))))

(defn analyze-static-call [actx cn mname args & {:keys [param-tags]}]
  (let [arg-nodes (mapv #(analyze actx %) args)
        cands (->> (env/member-methods cn mname)
                   (filter static-flag?)
                   (filter #(env/accessible? (:class actx) %)))
        _ (when (empty? cands)
            (fail (str "No accessible static method " mname " in " (str/replace cn "/" "."))))
        [m va] (select-method actx cands arg-nodes (str "static method " mname) param-tags)]
    (invoke-node actx nil (t/internal->desc cn) m nil arg-nodes va)))

(defn analyze-field-access
  "Field `fname` of target node; k is called with the field and owner (default: a read)."
  ([actx tn fname] (analyze-field-access actx tn fname nil))
  ([actx tn fname k]
   (let [tt (value-type (:type tn))]
     (cond
       (and (t/array? tt) (= fname "length") (nil? k))
       {:op :alength :array tn :type "I"}
       (not (t/ref? tt)) (fail (str "Field access ." fname " on a value of type " (pr-str tt)))
       :else
       (let [cn (t/desc->internal tt)
             f (or (env/find-field cn fname)
                   (fail (str "No field " fname " in " (str/replace cn "/" "."))))]
         (when-not (env/accessible? (:class actx) f)
           (fail (str "Field not accessible: " fname)))
         (if k
           (k f (if (static-flag? f) (:owner f) cn))
           (if (static-flag? f)
             (field-node actx f nil)
             (let [c (env/const-value f)]
               (if (and (some? c) (has? (:flags f) Opcodes/ACC_FINAL) (= :this-path (:op tn)))
                 (const-node (:desc f) c)
                 {:op :get-field :field f :owner cn :target tn :type (:desc f)})))))))))

(defn- super-node [actx]
  (when (:static actx) (fail "super in a static context"))
  (let [d (decl! (:class actx))]
    (assoc (this-node actx) :type (t/internal->desc (:super d)) :super true)))

(defn- receiver-class
  "If form names a class (not a local), its internal name."
  [actx form]
  (when (and (symbol? form) (not (contains? (:locals actx) form)) (nil? (namespace form))
             (not (find-own-field actx form)))
    (resolve-class actx form)))

(defn analyze-dot
  "(. target member args*) and (. target (member args*))"
  [actx [_ target member & args :as form]]
  (let [[member args] (if (seq? member) [(first member) (rest member)] [member args])
        mname (name member)
        param-tags (:param-tags (meta member))]
    (cond
      (= mname "super")
      ;; (.super o args): Java's o.super(args), the superclass constructor of an inner superclass
      (let [_ (when-not (:in-ctor actx) (fail ".super outside a constructor"))
            sup (:super (decl! (:class actx)))
            _ (when-not (needs-outer-instance? sup)
                (fail (str (str/replace sup "/" ".") " is not an inner class")))
            on (analyze actx target)]
        (ctor-call-node actx sup args :super :outer {:op :null-checked :expr on :type (:type on)}))

      (and (= mname "new") (seq args) (symbol? (first args)))
      ;; (.new o Inner args): Java's o.new Inner(args)
      (let [on (analyze actx target)
            ot (value-type (:type on))
            _ (when-not (t/class-desc? ot) (fail ".new needs an outer instance"))
            isym (first args)
            cn (let [m (str (t/desc->internal ot) "$" (name isym))]
                 (if (class-exists? m) m (or (resolve-class actx isym) (fail (str "Unknown class " isym)))))]
        (when-not (needs-outer-instance? cn)
          (when-not (some-> (env/info cn) :outer)
            (fail (str (str/replace cn "/" ".") " is not an inner class"))))
        (ctor-call-node actx cn (rest args) :new
                        :outer (if (or (= :this-path (:op on)) (:receiver (:b on))) on {:op :null-checked :expr on :type ot})))

      (= target 'super)
      (if (str/starts-with? mname "-")
        (analyze-field-access actx (super-node actx) (subs mname 1))
        (let [st (super-node actx)]
          (analyze-method-call actx st mname args :special true :param-tags param-tags)))

      (and (symbol? target) (namespace target) (= "super" (name target)))
      (let [cn (or (resolve-class actx (symbol (namespace target))) (fail (str "Unknown class " target)))
            d (decl! (:class actx))]
        (cond
          (some #{cn} (:interfaces d))
          (analyze-method-call actx (assoc (this-node actx) :type (t/internal->desc cn)) mname args
                               :special true :param-tags param-tags)
          :else (outer-super-call actx cn mname args param-tags)))

      (receiver-class actx target)
      (let [cn (receiver-class actx target)]
        (cond
          (str/starts-with? mname "-") (analyze-symbol actx (symbol (str/replace cn "/" ".") (subs mname 1)))
          (and (empty? args) (not (seq (filter static-flag? (env/member-methods cn mname))))
               (some-> (env/find-field cn mname) static-flag?))
          (analyze-symbol actx (symbol (str/replace cn "/" ".") mname))
          :else (analyze-static-call actx cn mname args :param-tags param-tags)))

      :else
      (let [tn (analyze actx target)]
        (cond
          (str/starts-with? mname "-") (analyze-field-access actx tn (subs mname 1))
          (and (empty? args) (t/ref? (value-type (:type tn))) (not (t/array? (:type tn)))
               (empty? (env/member-methods (t/desc->internal (:type tn)) mname))
               (env/find-field (t/desc->internal (:type tn)) mname))
          (analyze-field-access actx tn mname)
          :else (analyze-method-call actx tn mname args :param-tags param-tags
                                     :ret-tag (:tag (meta form))))))))

;; new ----------------------------------------------------------------------------------------------

(declare outer-instance-for)

(defn ctor-call-node
  "A call of constructor of class cn with args; kind :new, :super or :this."
  [actx cn args kind & {:keys [outer param-tags]}]
  (let [arg-nodes (mapv #(analyze actx %) args)
        cands (filter #(env/accessible? (:class actx) %) (env/constructors cn))
        _ (when (empty? cands) (fail (str "No accessible constructor of " (str/replace cn "/" "."))))
        [m va] (select-method actx cands arg-nodes (str "constructor of " (str/replace cn "/" ".")) param-tags)]
    {:op (case kind :new :new :ctor-call) :kind kind :class cn :ctor m
     :args (convert-args m arg-nodes va)
     :outer outer
     :type (if (= kind :new) (t/internal->desc cn) "V")}))

(defn- array-new [actx tdesc args]
  (let [dims (t/array-dims tdesc)]
    (if (and (= 1 (count args)) (vector? (first args)))
      (letfn [(init [t v]
                (if (vector? v)
                  {:op :array-init :type t :elems (mapv #(init (t/elem-type t) %) v)}
                  (convert-node (analyze actx v) t :what "array element")))]
        (init tdesc (first args)))
      (do (when (or (empty? args) (> (count args) dims))
            (fail (str "Bad dimensions for new " tdesc)))
          {:op :new-array :type tdesc :dims (mapv #(operand (analyze actx %) "I") args)}))))

(defn- needs-outer-instance? [cn]
  (when-let [d (env/info cn)]
    (:outer-instance? d)))

(defn outer-instance-for
  "The implicit outer instance node for creating class cn from actx."
  [actx cn]
  (let [d (env/info cn)]
    (cond
      (and d (#{:local :anon} (:nesting d)))
      (outer-this-node actx (:outer d))
      d
      ;; a member inner class: the innermost enclosing instance of its outer class
      (let [target (:outer d)]
        (loop [c (:class actx)]
          (cond (nil? c) (fail (str "No enclosing instance for " cn))
                (env/subclass? c target) (outer-this-node actx c)
                :else (recur (:outer (decl c))))))
      :else nil)))

(defn analyze-new [actx [_ cls & args]]
  (let [cn (when (symbol? cls) (resolve-class actx cls))]
    (cond
      (and (symbol? cls) (namespace cls) (re-matches #"[1-9]" (name cls)))
      (array-new actx (actx-desc actx cls) args)
      (nil? cn) (fail (str "Unable to resolve class: " cls))
      :else
      (let [d (env/info! cn)]
        (when (has? (:flags d) (bit-or Opcodes/ACC_ABSTRACT Opcodes/ACC_INTERFACE))
          (fail (str "Cannot instantiate abstract class " (str/replace cn "/" "."))))
        ;; a local class's captured locals must be available where it is created
        (when-let [cd (decl cn)]
          (when (= :local (:nesting cd))
            (doseq [b (:captures @(:state cd))] (local-ref actx b))))
        (assoc (ctor-call-node actx cn args :new
                               :outer (when (needs-outer-instance? cn) (outer-instance-for actx cn))
                               :param-tags (:param-tags (meta cls)))
               :type-anns (code-type-anns actx cls))))))

;; arrays, tests -----------------------------------------------------------------------------------

(defn- analyze-aget [actx [_ a & idxs]]
  (reduce (fn [an i]
            (let [at (value-type (:type an))]
              (when-not (t/array? at) (fail (str "aget on a non-array of type " (pr-str at))))
              {:op :aget :array an :index (operand (analyze actx i) "I") :type (t/elem-type at)}))
          (analyze actx a) idxs))

(defn- analyze-aset [actx [_ a & more]]
  (let [idxs (butlast more) v (last more)
        an (reduce (fn [an i]
                     (let [at (value-type (:type an))]
                       (when-not (t/array? at) (fail "aset on a non-array"))
                       {:op :aget :array an :index (operand (analyze actx i) "I") :type (t/elem-type at)}))
                   (analyze actx a) (butlast idxs))
        at (value-type (:type an))
        _ (when-not (t/array? at) (fail (str "aset on a non-array of type " (pr-str at))))
        et (t/elem-type at)]
    {:op :aset :array an :index (operand (analyze actx (last idxs)) "I")
     :val (convert-node (analyze actx v) et :what "array element") :type et}))

(defn code-type-anns
  "Type annotations of a type form in code, for the instruction it types: [[path ann] ...]."
  [actx form]
  (vec (for [[path anns] (t/type-anns (parse-type (scope-of actx) form))
             a (annotations-now (scope-of actx) anns)]
         [path a])))

(defn- analyze-instance? [actx [_ cls x]]
  (let [cd (actx-desc actx cls)
        xn (analyze actx x)]
    (when-not (t/ref? cd) (fail "instance? needs a class"))
    (when-not (t/ref? (value-type (:type xn))) (fail "instance? of a primitive"))
    {:op :instance? :class cd :expr xn :type "Z" :type-anns (code-type-anns actx cls)}))

(defn- analyze-cast [actx [_ cls x]]
  (let [xn (analyze actx x)]
    (if (and (seq? cls) (= '& (first cls)))
      (reduce (fn [n [i c]] {:op :cast :class (actx-desc actx c) :expr n :type (actx-desc actx c)
                             :type-anns (code-type-anns actx c) :type-arg i})
              xn (map-indexed vector (rest cls)))
      (let [cd (actx-desc actx cls)]
        (cond
          (t/prim? cd) (analyze-conversion actx cd x true)
          (not (t/ref? (value-type (:type xn))))
          (convert-node xn cd)
          :else {:op :cast :class cd :expr xn :type cd :type-anns (code-type-anns actx cls) :type-arg 0})))))

(defn- ref-node [actx x what]
  (let [n (analyze actx x)]
    (when-not (t/ref? (value-type (:type n))) (fail (str what " of a primitive")))
    n))

;; dispatch ------------------------------------------------------------------------------------------

(declare analyze-anon analyze-letclass analyze-ctor-call analyze-java-str analyze-java-assert
         analyze-with-resources
         analyze-for-each analyze-switch analyze-lambda analyze-method-ref analyze-if-instance)

(defn- method-sugar
  "Clojure's interop sugar as a `.` form, or nil."
  [actx form]
  (let [[op & args] form
        s (name op)]
    (cond
      (and (str/starts-with? s ".") (> (count s) 1) (nil? (namespace op)) (not= s ".."))
      (let [tgt (first args)
            ;; as Clojure: (.m SomeClass) calls m on the Class object
            tgt (if (and (symbol? tgt) (not (contains? (:locals actx) tgt)) (nil? (namespace tgt))
                         (not (find-own-field actx tgt)) (resolve-class actx tgt))
                  (list 'class-literal* tgt)
                  tgt)]
        (with-meta (list* '. tgt (with-meta (symbol (subs s 1)) (meta op)) (rest args))
          (meta form)))

      (and (str/ends-with? s ".") (> (count s) 1) (nil? (namespace op))
           (not (#{"super." "this."} s)))
      (list* 'new (with-meta (symbol (subs s 0 (dec (count s)))) (meta op)) args)

      (and (namespace op) (not (contains? (:locals actx) op))
           (not (resolve-var actx op))
           (resolve-class actx (symbol (namespace op))))
      (let [c (symbol (namespace op))]
        (cond
          (= s "new") (list* 'new (with-meta c (meta op)) args)
          (str/starts-with? s ".") (let [cn (resolve-class actx c)]
                                     (list ::qualified-instance cn (with-meta (symbol (subs s 1)) (meta op)) args))
          :else (list* '. c (with-meta (symbol s) (meta op)) args)))

      :else nil)))

(defn analyze-op [actx form]
  (let [op (first form)]
    (cond
      (and (symbol? op) (contains? (:locals actx) op))
      (fail (str "Calling a local as a function is not supported in class bodies: " op))

      (not (symbol? op))
      (fail (str "Not supported in class bodies at stage 0: " (pr-str form)))

      :else
      (do
        (or
          (when (nil? (namespace op))
            (case op
              do (analyze-body actx (rest form))
              if (analyze-if actx form)
              let* (analyze-let actx form false)
              loop* (analyze-let actx form true)
              recur (analyze-recur actx (rest form) nil)
              quote (let [v (second form)]
                      (if (or (string? v) (number? v) (boolean? v) (nil? v) (char? v))
                        (analyze actx v)
                        (clj-constant actx v)))
              set! (analyze-set! actx form)
              . (analyze-dot actx form)
              new (analyze-new actx form)
              throw (analyze-throw actx form)
              try (analyze-try actx form)
              super. (analyze-ctor-call actx form :super)
              class-literal* (class-lit actx (actx-desc actx (second form)))
              this. (analyze-ctor-call actx form :this)
              label* (analyze-label actx form)
              break* (analyze-break actx form)
              continue* (let [[_ & args] form
                              [kw args] (if (keyword? (first args)) [(first args) (rest args)] [nil args])]
                          (analyze-recur actx args kw))
              return* (analyze-return actx form)
              class* (case (second form)
                       :anon (analyze-anon actx form)
                       :local (analyze-letclass actx form))
              java-str* (analyze-java-str actx form)
              java-assert* (analyze-java-assert actx form)
              for-each* (analyze-for-each actx form)
              if-instance* (analyze-if-instance actx form)
              with-resources* (analyze-with-resources actx form)
              switch* (analyze-switch actx form)
              lambda* (analyze-lambda actx form)
              method-ref* (analyze-method-ref actx form)
              fn* (fail "fn is not supported in class bodies at stage 0 (use lambda)")
              (letfn* case* def var deftype* reify* monitor-enter monitor-exit)
              (fail (str op " is not supported in class bodies at stage 0"))
              nil))
          (when-let [cop (core-op actx op)]
            (let [args (rest form)]
              (cond
                (arith-ops cop) (analyze-arith actx cop args)
                (conversions cop) (do (when-not (= 1 (count args)) (fail (str cop " takes 1 argument")))
                                      (analyze-conversion actx (conversions cop) (first args) false))
                (checked-conversions cop) (do (when-not (= 1 (count args)) (fail (str cop " takes 1 argument")))
                                              (analyze-conversion actx (checked-conversions cop) (first args) true))
                (comparisons cop) (analyze-compare actx cop args)
                :else
                (case cop
                  "not" (let [x (analyze actx (first args))]
                          (if (and (= :const (:op x)) (boolean? (:val x)))
                            (const-node "Z" (not (:val x)))
                            {:op :not :expr x :type "Z"}))
                  "nil?" {:op :nil? :expr (ref-node actx (first args) "nil?") :type "Z"}
                  "some?" {:op :not :expr {:op :nil? :expr (ref-node actx (first args) "some?") :type "Z"} :type "Z"}
                  "identical?" {:op :identical? :args [(ref-node actx (first args) "identical?")
                                                       (ref-node actx (second args) "identical?")] :type "Z"}
                  "=" (let [[a b] (map #(analyze actx %) args)]
                        (cond (and (= 2 (count args)) (= "Z" (prim-type a)) (= "Z" (prim-type b)))
                              {:op :bool= :args [(unboxed a) (unboxed b)] :type "Z"}
                              (and (= 2 (count args)) (t/numeric? (prim-type a)) (t/numeric? (prim-type b)))
                              (analyze-compare actx "==" nil [a b])
                              :else (fail "= on references needs the Clojure runtime; use .equals or identical?")))
                  "not=" (analyze actx (list 'clojure.core/not (cons 'clojure.core/= args)))
                  ("zero?" "pos?" "neg?")
                  (analyze-compare actx ({"zero?" "==" "pos?" ">" "neg?" "<"} cop) [(first args) 0])
                  "instance?" (analyze-instance? actx form)
                  "cast" (analyze-cast actx form)
                  "aget" (analyze-aget actx form)
                  "aset" (analyze-aset actx form)
                  "alength" (let [an (analyze actx (first args))]
                              (when-not (t/array? (value-type (:type an))) (fail "alength of a non-array"))
                              {:op :alength :array an :type "I"})
                  "and" (if (empty? args) (const-node "Z" true)
                            (let [ns (mapv #(analyze actx %) args)]
                              (if (every? #(and (= :const (:op %)) (boolean? (:val %))) ns)
                                (const-node "Z" (every? :val ns))
                                {:op :and :args ns :type (reduce unify (map :type ns))})))
                  "or" (if (empty? args) (const-node :null nil)
                           (let [ns (mapv #(analyze actx %) args)]
                             (if (every? #(and (= :const (:op %)) (boolean? (:val %))) ns)
                               (const-node "Z" (boolean (some :val ns)))
                               {:op :or :args ns :type (reduce unify (map :type ns))})))
                  "locking" (analyze-locking actx form)

                  "boolean" (let [n (unboxed (analyze actx (first args)))]
                              (if (= "Z" (:type n)) n (fail "boolean of a non-boolean")))
                  nil))))
          (when-let [s (method-sugar actx form)]
            (if (= ::qualified-instance (first s))
              (let [[_ cn mname [tgt & margs]] s
                    tn (analyze actx tgt)]
                (analyze-method-call actx (if (env/assignable? (value-type (:type tn)) (t/internal->desc cn))
                                            (assoc tn :type (t/internal->desc cn))
                                            {:op :cast :class (t/internal->desc cn) :expr tn :type (t/internal->desc cn)})
                                     (name mname) margs :param-tags (:param-tags (meta mname))
                                     :ret-tag (:tag (meta form))))
              (analyze actx s)))
          (let [ex (macroexpand1 actx form)]
            (if (identical? ex form)
              (if-let [v (resolve-var actx op)]
                {:op :var-invoke :var v :field (clj-var-field actx v) :owner (:class actx)
                 :args (mapv #(analyze actx %) (rest form)) :type t/object-desc}
                (fail (str "Unable to resolve: " op)))
              (analyze actx ex))))))))

;; the forms of later steps are filled in below or in the next sections

(def ^:private non-constant-heads
  '#{anon letclass lambda method-ref switch new class* lambda* method-ref* switch* fn fn* letfn})

(defn- may-be-constant?
  "Can form be a constant expression? Those declaring classes or lambdas cannot, and analyzing
  them would declare those (an anonymous class number would be taken)."
  [form]
  (cond (seq? form) (and (not (and (symbol? (first form)) (non-constant-heads (symbol (name (first form))))))
                         (every? may-be-constant? form))
        (coll? form) (every? may-be-constant? form)
        :else true))

(defn analyze-const-form
  "Analyzes a form expected to be a constant of type desc; returns the const node or nil."
  ([scope form desc] (analyze-const-form scope form desc false))
  ([scope form desc lenient]
   (let [actx {:class (:class scope) :ns (:ns scope) :local-classes (:local-classes scope)
               :bounds (:bounds scope) :static true :locals {}
               :frame (new-frame :class-init (:class scope) nil nil) :const-only true}
         n (when (or (not lenient) (may-be-constant? form))
             (try (analyze actx form) (catch Exception e (if lenient nil (throw e)))))]
     (when (and n (= :const (:op n)))
       (let [c (conversion n (:type n) desc)]
         (cond (= c :none) (if (= desc t/string-desc) n (const-node desc (const-of-type desc (:val n))))
               (#{:widen :narrow-const} c) (const-node desc (const-of-type desc (:val n)))
               lenient nil
               :else (fail (str "Constant of type " (:type n) " where " desc " is needed"))))))))

;; ---------------------------------------------------------------------------------------------
;; class bodies

(defn- creation-actx [n]
  (let [d (decl! n)]
    (case (:nesting d)
      (:local :anon) (:creation-actx @(:state d))
      :member (creation-actx (:outer d))
      nil)))

(defn- body-actx
  "The analysis context for code of class n."
  [n kind static? bounds method-info ret]
  (let [d (decl! n)
        boundary (when (#{:local :anon} (:nesting d)) {:kind :class :class n})
        f (assoc (new-frame kind n (class-parent-frame n) boundary) :static static?)
        cactx (creation-actx n)]
    {:class n :ns (:ns d) :bounds bounds :static static? :frame f
     :reflection (some #(:reflection (:meta (decl %))) (take-while some? (iterate #(some-> % decl :outer) n)))
     :locals (or (:locals cactx) {})
     :local-classes (merge (:local-classes cactx) (:local-classes d))
     :labels {} :method-info method-info :ret ret}))

(defn- bind-params
  "Binds the receiver (unless static) and the parameters of method entry m."
  [actx n m]
  (let [actx (if-let [r (:recv m)]
               (with-local actx (assoc (make-binding actx r (t/internal->desc n) :receiver n)
                                       :slot0 true))
               actx)]
    (reduce (fn [[actx bs] p]
              (let [b (make-binding actx (:sym p) (:desc p) :mutable (boolean (:mutable (:meta p)))
                                    :param true)]
                [(with-local actx b) (conj bs b)]))
            [actx []]
            (:params m))))

(defn- analyze-method-body [n m]
  (let [static? (static-flag? m)
        actx (body-actx n :method static? (:bounds (:scope m)) {:name (:name m) :method m} (:ret m))
        [actx bs] (bind-params actx n m)
        body (analyze-body actx (get-in m [:member :body]))
        body (if (= "V" (:ret m)) body (convert-node body (:ret m) :what (str "value of method " (:name m))))]
    {:params bs :body body :recv (get-in actx [:locals (:recv m)])}))

(defn- ctor-call-form? [f] (and (seq? f) (#{'super. 'this. '.super} (first f))))

(defn- implicit-super-call [actx n]
  (let [d (decl! n)]
    (case (:kind d)
      :enum {:op :ctor-call :kind :super :class "java/lang/Enum" :enum-args true
             :ctor {:name "<init>" :desc "(Ljava/lang/String;I)V" :owner "java/lang/Enum"} :args [] :type "V"}
      (if (and (= :anon (:nesting d)) (:anon-super-ctor d))
        {:op :ctor-call :kind :super :class (:super d) :ctor (:anon-super-ctor d) :anon-args true
         :args [] :type "V"}
        (let [sup (:super d)
              sd (decl sup)]
          (when (and sd (:outer-instance? sd) (#{:local :anon} (:nesting sd)))
            nil)
          (ctor-call-node actx sup [] :super
                          :outer (when (needs-outer-instance? sup) (outer-instance-for actx sup))))))))

(defn analyze-ctor-call [actx [_ & args] kind]
  (when-not (:in-ctor actx) (fail (str (if (= kind :super) "super." "this.") " outside a constructor")))
  (let [n (:class actx)
        d (decl! n)]
    (when (= :enum (:kind d)) (if (= kind :super) (fail "An enum constructor cannot call super.")))
    (if (= kind :this)
      (ctor-call-node actx n args :this)
      (let [sup (:super d)]
        (ctor-call-node actx sup args :super
                        :outer (when (needs-outer-instance? sup) (outer-instance-for actx sup)))))))

(defn- analyze-ctor-body [n m]
  (let [actx (-> (body-actx n :method false (:bounds (or (:scope m) (class-scope n)))
                            {:name "<init>" :method m} "V")
                 (assoc-in [:frame :ctor] true))
        ;; a compact constructor's components are assignable locals
        [actx bs] (bind-params actx n (if (:compact m)
                                        (update m :params (fn [ps] (mapv #(assoc-in % [:meta :mutable] true) ps)))
                                        m))
        actx (assoc actx :in-ctor true)
        forms (get-in m [:member :body])
        idx (first (keep-indexed (fn [i f] (when (ctor-call-form? f) i)) forms))
        [pre call post] (if idx
                          [(take idx forms) (nth forms idx) (drop (inc idx) forms)]
                          [nil nil forms])
        pre-nodes (mapv #(analyze (assoc actx :ctor-prologue true) %) pre)
        call-node (if call
                    (analyze (assoc actx :ctor-prologue true) call)
                    (implicit-super-call (assoc actx :ctor-prologue true) n))
        post-node (analyze-body actx post)]
    {:params bs :prologue pre-nodes :call call-node :body post-node
     :recv (get-in actx [:locals (:recv m)])
     :calls-super (not= :this (:kind call-node))
     :compact-locals (when (:compact m) bs)}))

(declare analyze-enum-body)

(defn- analyze-enum-constants
  "The <clinit> nodes creating the constants of enum n and $VALUES."
  [n sactx]
  (let [d (decl! n)
        self (t/internal->desc n)
        ctors (filter #(= "<init>" (:name %)) (:methods d))]
    (conj
      (vec (for [[i c] (map-indexed vector (:constants d))]
             (let [args (mapv #(analyze sactx %) (:args c))
                   [m va] (select-method sactx ctors args (str "constructor of enum constant " (:name c)) nil)
                   cls (if (:has-body c) (analyze-enum-body sactx n c m) n)
                   ctor (if (:has-body c)
                          (some #(when (= "<init>" (:name %)) %) (:methods (decl! cls)))
                          m)
                   f (some #(when (= (:name c) (:name %)) %) (:fields d))]
               {:op :set-static :field f :owner n :type self
                :val {:op :new :class cls :ctor ctor :args (convert-args m args va)
                      :enum-const {:name (:name c) :ordinal i} :type self}})))
      {:op :set-static :owner n :type (t/array-of self)
       :field (some #(when (= "$VALUES" (:name %)) %) (:fields d))
       :val {:op :invoke :kind :static :owner n :itf false :name "$values"
             :desc (str "()" (t/array-of self)) :args [] :type (t/array-of self)}})))

(defn analyze-class!
  "Analyzes all code of class n, in the textual order of its members (so anonymous classes are
  numbered as javac numbers them). Results go to the class's :state."
  [n]
  (let [d (decl! n)
        st (:state d)]
    (when-not (:analyzed @st)
      (swap! st assoc :analyzed true)
      (let [bounds (:bounds d)
            sactx (body-actx n :class-init true bounds nil nil)
            iactx (body-actx n :class-init false bounds nil nil)
            ;; field initializers and initializer bodies have no receiver parameter: `this` names
            ;; the instance there
            iactx (with-local iactx (make-binding iactx 'this (t/internal->desc n) :receiver n))
            fields-by-member (into {} (keep (fn [f] (when (:member f) [(:mid (:member f)) f])) (:fields d)))
            methods-by-member (into {} (keep (fn [m] (when (:member m) [(:mid (:member m)) m])) (:methods d)))
            clinit (atom []) init (atom []) bodies (atom {})]
        ;; enum constants come first, as javac creates them first in <clinit>
        (when (= :enum (:kind d))
          (swap! clinit into (analyze-enum-constants n sactx)))
        (doseq [mb (:members d)]
          (case (:kind mb)
            :field
            (let [f (fields-by-member (:mid mb))]
              (when (:has-init mb)
                (let [static? (static-flag? f)
                      actx (if static? sactx iactx)
                      const (when (:const f) (env/const-value f))]
                  (when-not (and static? (some? const))
                    (let [v (if (some? const)
                              (const-node (:desc f) const)
                              (convert-node (analyze (cond-> (assoc actx :field-name (:name f))
                                                              (lambda-form? (:init mb)) (assoc :pending-var (symbol (:name f))))
                                                     (:init mb))
                                            (:desc f)
                                            :what (str "initializer of field " (:name f))))]
                      (if static?
                        (swap! clinit conj {:op :set-static :field f :owner n :val v :type (:desc f)})
                        (swap! init conj {:op :set-field :field f :owner n :target (this-node actx)
                                          :val v :type (:desc f)})))))))
            :init (swap! init conj (analyze-body iactx (:body mb)))
            ;; member classes are analyzed in place, as javac translates them (accessor numbers)
            :class (when-let [mc (get (:member-classes d) (name (second (:form mb))))]
                     (analyze-class! mc))
            :clinit (swap! clinit conj (analyze-body sactx (:body mb)))
            :method (let [m (methods-by-member (:mid mb))]
                      (when (:has-body mb)
                        (swap! bodies assoc [(:name m) (:desc m)] (analyze-method-body n m))))
            :ctor (let [m (methods-by-member (:mid mb))
                        m (or m (some #(when (and (= "<init>" (:name %)) (:compact %)) %) (:methods d)))]
                    (swap! bodies assoc [(:name m) (:desc m)] (analyze-ctor-body n m)))
            nil))
        ;; derived constructors call super too
        (doseq [m (:methods d) :when (and (= "<init>" (:name m)) (#{:default-ctor :anon-ctor :record-canonical} (:derived m)))]
          (let [actx (assoc (body-actx n :method false bounds {:name "<init>" :method m} "V") :in-ctor true
                            :ctor-prologue true)
                [actx bs] (bind-params actx n (assoc m :recv 'this__))]
            (swap! bodies assoc ["<init>" (:desc m)]
                   {:params bs :prologue [] :call (implicit-super-call actx n) :body nil
                    :calls-super true :derived true})))
        (swap! st assoc :clinit @clinit :init @init :bodies @bodies)))
    n))

;; ---------------------------------------------------------------------------------------------
;; bridges (§6): javac's TransTypes.addBridges on erased signatures, with the supertypes' type
;; arguments substituted

(defn- class-generics
  "Generic view of class n: {:tparams [sym] :supers [tnode] :methods [{:name :desc :params [tnode]
  :bounds {}}]}."
  [n]
  (if-let [d (decl n)]
    {:tparams (mapv :sym (:tparams d))
     :supers (cons (:super-t d) (:interfaces-t d))
     :methods (for [m (:methods d) :when (not= "<init>" (:name m)) :when (not (:bridge-of m))]
                {:name (:name m) :desc (:desc m) :flags (:flags m) :throws (:throws m)
                 :params (mapv #(or (:tn %) (t/desc->tnode (:desc %))) (:params m))
                 :bounds (:bounds (:scope m))})}
    (when-let [c ^Class (env/load-class n)]
      {:tparams (mapv #(symbol (.getName ^java.lang.reflect.TypeVariable %)) (.getTypeParameters c))
       :supers (remove nil? (cons (some-> (.getGenericSuperclass c) t/reflect->tnode)
                                  (map t/reflect->tnode (.getGenericInterfaces c))))
       :methods (for [^java.lang.reflect.Method m (.getDeclaredMethods c)]
                  {:name (.getName m)
                   :desc (t/method-desc (map t/class->desc (.getParameterTypes m)) (t/class->desc (.getReturnType m)))
                   :flags (.getModifiers m)
                   :throws (mapv #(str/replace (.getName ^Class %) "." "/") (.getExceptionTypes m))
                   :params (mapv t/reflect->tnode (.getGenericParameterTypes m))
                   :bounds (into {} (for [^java.lang.reflect.TypeVariable tv (.getTypeParameters m)]
                                      [(symbol (.getName tv)) (mapv t/reflect->tnode (.getBounds tv))]))})})))

(defn- supertype-envs
  "For every supertype S of class n: S -> substitution of S's type variables in n's terms (nil
  for raw or non-generic supertypes)."
  [n]
  (loop [queue (for [st (:supers (class-generics n)) :when st] [st {}]) out {}]
    (if-let [[[st env] & more] (seq queue)]
      (let [st (t/subst env st)
            s (:name st)
            g (class-generics s)
            senv (when (and g (seq (:args st)) (= (count (:tparams g)) (count (:args st))))
                   (zipmap (:tparams g) (:args st)))]
        (if (or (nil? g) (contains? out s))
          (recur more out)
          (recur (concat more (for [x (:supers g) :when x] [x (or senv {})]))
                 (assoc out s senv))))
      out)))

(defn- erased-params [n envs c m]
  "Erased parameter descriptors of method m of class c, as a member of class n."
  (let [env (if (= c n) {} (or (get envs c) {}))
        bounds (merge (:bounds (decl n)) (:bounds m))]
    (mapv #(t/erase bounds (t/subst env %)) (:params m))))

(defn add-bridges!
  "Adds javac's bridge methods to class n (TransTypes.addBridgeIfNeeded): for each method meth
  of a supertype whose implementation in n has another erasure, unless a bridge with meth's
  erasure is already present at or below the implementation. Bridges of enclosing classes are
  not yet present, as javac translates nested classes before adding the outer class's bridges."
  [n]
  (let [d (decl! n)]
    (when-not (= :annotation (:kind d))
      (let [envs (supertype-envs n)
            iface? (= :interface (:kind d))
            chain (if iface? [n] (vec (env/superclass-chain n)))
            enclosing (set (take-while some? (iterate #(some-> % decl :outer) (:outer d))))
            gens (memoize class-generics)
            members (fn [c] (:methods (gens c)))
            bridges (atom [])
            present? (fn [name desc impl-owner]
                       (loop [[c & more] chain]
                         (cond
                           (nil? c) false
                           (some #(and (= name (:name %)) (= desc (:desc %))
                                       (not (and (enclosing c) (:bridge-of %))))
                                 (concat (if (= c n) (concat (:methods d) @bridges) (:methods (or (decl c) (gens c))))))
                           true
                           (= c impl-owner) false
                           :else (recur more))))]
        (doseq [[s env] envs
                meth (members s)
                :when (not= "<init>" (:name meth))
                :when (not (has? (:flags meth) (bit-or Opcodes/ACC_STATIC Opcodes/ACC_PRIVATE Opcodes/ACC_SYNTHETIC)))
                :let [mps (erased-params n envs s meth)
                      impl (some (fn [c]
                                   (some (fn [mm]
                                           (when (and (= (:name meth) (:name mm))
                                                      (= (count mps) (count (:params mm)))
                                                      (not (has? (:flags mm) (bit-or Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC)))
                                                      (or (= c n) (not (has? (:flags mm) Opcodes/ACC_PRIVATE)))
                                                      (= mps (erased-params n envs c mm)))
                                             (assoc mm :impl-owner c)))
                                         (members c)))
                                 chain)]
                :when impl
                :when (not= (:desc impl) (:desc meth))
                :when (not (present? (:name meth) (:desc meth) (:impl-owner impl)))]
          (let [[bps _] (t/parse-method-desc (:desc meth))]
            (swap! bridges conj
                   {:name (:name meth) :desc (:desc meth) :owner n
                    :bridge-of (assoc impl :owner (:impl-owner impl))
                    :special (not= n (:impl-owner impl))
                    :derived :bridge
                    ;; javac's bridge has the erased type of the overridden method, throws included
                    :throws (vec (:throws meth))
                    :ret (second (t/parse-method-desc (:desc meth)))
                    :params (mapv (fn [pd] {:desc pd :flags 0}) bps)
                    :flags (bit-or (bit-and (:flags impl) (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_PROTECTED Opcodes/ACC_PRIVATE))
                                   Opcodes/ACC_SYNTHETIC Opcodes/ACC_BRIDGE)})))
        ;; javac's "reflection" bridges: a public class re-declares the public methods it inherits
        ;; from a non-public superclass (TransTypes.addBridgeIfNeeded)
        (when (has? (:flags d) Opcodes/ACC_PUBLIC)
          (doseq [c (rest chain)
                  :when (not (has? (:flags (env/info! c)) Opcodes/ACC_PUBLIC))
                  mm (:methods (env/info! c))
                  :when (not= "<init>" (:name mm))
                  :when (has? (:flags mm) Opcodes/ACC_PUBLIC)
                  :when (not (has? (:flags mm) (bit-or Opcodes/ACC_STATIC Opcodes/ACC_ABSTRACT Opcodes/ACC_FINAL
                                                      Opcodes/ACC_SYNTHETIC)))
                  ;; it is the implementation: no class below c declares that signature
                  :when (not (some (fn [b] (some #(and (= (:name mm) (:name %))
                                                       (= (first (t/parse-method-desc (:desc mm)))
                                                          (first (t/parse-method-desc (:desc %)))))
                                                 (:methods (env/info! b))))
                                   (take-while #(not= % c) chain)))
                  :when (not (some #(and (= (:name mm) (:name %)) (= (:desc mm) (:desc %))) @bridges))]
            (let [[bps _] (t/parse-method-desc (:desc mm))]
              (swap! bridges conj
                     {:name (:name mm) :desc (:desc mm) :owner n
                      :bridge-of (assoc mm :owner c) :special true :derived :bridge
                      :throws (vec (:throws mm))
                      :ret (second (t/parse-method-desc (:desc mm)))
                      :params (mapv (fn [pd] {:desc pd :flags 0}) bps)
                      :flags (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_SYNTHETIC Opcodes/ACC_BRIDGE)}))))
        (when (seq @bridges)
          (update-decl! n update :methods into @bridges))))))

(defn- supertypes-first
  "Names ordered so that supertypes in the list come before their subtypes."
  [names]
  (let [in (set names) done (atom []) seen (atom #{})]
    (letfn [(visit [n]
              (when-not (@seen n)
                (swap! seen conj n)
                (let [d (decl n)]
                  (doseq [s (cons (:super d) (:interfaces d)) :when (in s)] (visit s)))
                (swap! done conj n)))]
      (doseq [n names] (visit n))
      @done)))

(defn process-classes!
  "Resolves headers and members of the classes declared since position `from` of the unit's
  order, then analyzes their code."
  [from]
  (let [names (subvec @(:order *unit*) from)]
    (doseq [n names :when (not (:headers-done (decl n)))] (resolve-header! n))
    (doseq [n names] (resolve-members! n))
    (doseq [n (supertypes-first names) :when (not (:declared-only (decl n)))] (add-bridges! n))
    (doseq [n names :when (not (:declared-only (decl n)))] (analyze-class! n))))

(defn analyze-anon [actx [_ _ super-form args & members]]
  (when-not (vector? args) (fail "anon needs a vector of constructor arguments"))
  (let [cur (:class actx)
        stn (parse-type (scope-of actx) super-form)
        _ (when-not (= :class (:t stn)) (fail (str "Bad anon supertype: " super-form)))
        sname (:name stn)
        from (count @(:order *unit*))
        n (local-class-name cur nil)
        parsed (-> (p/parse-class {:ns (:ns actx) :nesting :anon :outer (decl cur)} (cons 'anon members))
                   (assoc :simple nil :anon-super stn))
        ;; before the superclass constructor call there is no instance yet: a static context
        _ (declare-class! {:nesting :anon :outer cur :name n
                           :static-context (boolean (or (:static actx) (:ctor-prologue actx)))
                           :local-classes (:local-classes actx)
                           :enclosing-method (:method-info actx)} parsed)
        _ (swap! (state n) assoc :creation-frame (:frame actx) :creation-actx actx)
        _ (resolve-header! n)
        arg-nodes (mapv #(analyze actx %) args)
        iface? (env/interface? sname)
        _ (when (and iface? (seq args)) (fail "An anonymous class of an interface takes no arguments"))
        [sctor va] (when-not iface?
                     (select-method actx (filter #(env/accessible? n %) (env/constructors sname))
                                    arg-nodes (str "constructor of " sname) nil))
        ;; an anonymous subclass of an inner class takes the superclass's outer instance as its
        ;; first (mandated) constructor parameter, and has no outer instance of its own (javac)
        super-outer (when (and sctor (needs-outer-instance? sname))
                      (let [o (outer-instance-for actx sname)]
                        (if (and (:outer-instance? (decl n)) (= :this-path (:op o)) (empty? (:path o)))
                          ;; the creating instance is both the anonymous class's outer instance and
                          ;; its superclass's: one parameter (javac)
                          (do (update-decl! n assoc :super-outer-is-outer true) nil)
                          (do (update-decl! n assoc :outer-instance? false
                                            :super-outer (t/internal->desc (:outer (env/info sname))))
                              o))))
        _ (update-decl! n assoc :anon-super-ctor sctor :anon-ctor-desc (if iface? "()V" (:desc sctor)))
        _ (process-classes! from)
        d (decl! n)
        ctor (some #(when (= "<init>" (:name %)) %) (:methods d))]
    {:op :new :class n :ctor ctor
     :args (if sctor (convert-args sctor arg-nodes va) [])
     :outer (when (:outer-instance? d) (this-node actx))
     :super-outer (when super-outer
                    (if (or (= :this-path (:op super-outer)) (:receiver (:b super-outer)))
                      super-outer
                      {:op :null-checked :expr super-outer :type (:type super-outer)}))
     :type (t/internal->desc n)}))

(defn analyze-letclass [actx [_ _ specs & body]]
  (let [cur (:class actx)
        actx2 (reduce
                (fn [actx spec]
                  (let [parsed (p/parse-class {:ns (:ns actx) :nesting :local :outer (decl cur)} spec)
                        from (count @(:order *unit*))
                        n (local-class-name cur (:simple parsed))
                        lc (assoc (:local-classes actx) (symbol (:simple parsed)) n)
                        actx (assoc actx :local-classes lc)]
                    (declare-class! {:nesting :local :outer cur :name n
                                     :static-context (boolean (or (:static actx) (:ctor-prologue actx)))
                                     :local-classes lc :enclosing-method (:method-info actx)}
                                    parsed)
                    (swap! (state n) assoc :creation-frame (:frame actx) :creation-actx actx)
                    (process-classes! from)
                    actx))
                actx specs)]
    (analyze-body actx2 body)))

(defn- not-yet [what] (fn [& _] (fail (str what " is not implemented yet"))))

(defn analyze-enum-body
  "The anonymous class E$n of an enum constant with a body; its constructor takes the enum
  constructor's parameters (after name and ordinal) and passes them on."
  [actx n c ector]
  (let [cn (local-class-name n nil)
        from (count @(:order *unit*))
        parsed (-> (p/parse-class {:ns (:ns actx) :nesting :anon :outer (decl n)} (cons 'anon (:body c)))
                   (assoc :simple nil :anon-super {:t :class :name n :args []} :enum-body true))
        parsed (merge parsed (p/class-flags (assoc parsed :enum-body true)))]
    (declare-class! {:nesting :anon :outer n :name cn :static-context true
                     :local-classes (:local-classes actx) :enclosing-method nil} parsed)
    (swap! (state cn) assoc :creation-frame (:frame actx) :creation-actx actx)
    (update-decl! cn assoc :anon-super-ctor ector :anon-ctor-desc (:desc ector))
    (process-classes! from)
    cn))

;; ---------------------------------------------------------------------------------------------
;; java-str (§5.10): javac's StringConcat.IndyConstants

(defn const-string
  "Java's string conversion of a constant."
  [t v]
  (cond (nil? v) "null"
        (= t "C") (str (char v))
        (= t "Z") (str (boolean v))
        (= t "F") (str (float v))
        (= t "D") (str (double v))
        :else (str v)))

(defn- accessible-class? [actx n]
  (let [i (env/info n)]
    (or (nil? i) (has? (:flags i) Opcodes/ACC_PUBLIC)
        (= (t/package-of n) (t/package-of (:class actx))))))

(defn- sharpest-accessible [actx d]
  (cond
    (t/array? d) (t/array-of (sharpest-accessible actx (t/elem-type d)))
    (t/class-desc? d) (loop [n (t/desc->internal d)]
                        (if (or (accessible-class? actx n) (nil? (:super (env/info n))))
                          (t/internal->desc n)
                          (recur (:super (env/info n)))))
    :else d))

(defn analyze-java-str [actx [_ & args]]
  (let [nodes (mapv #(analyze actx %) args)
        ;; javac flattens nested string concatenation into one call site
        nodes (vec (mapcat #(if (= :java-str (:op %)) (:operands %) [%]) nodes))]
    (if (every? #(and (= :const (:op %)) (some? (:val %))) nodes)   ; null is no constant (JLS 15.29)
      (const-node t/string-desc (apply str (map #(const-string (:type %) (:val %)) nodes)))
      (let [parts (vec (for [n nodes
                             :let [ty (value-type (:type n))]
                             :when (not (and (= :const (:op n)) (= "" (:val n))))]
                         (cond
                           (= :const (:op n))
                           (let [sv (const-string ty (:val n))]
                             (if (or (str/includes? sv "\u0001") (str/includes? sv "\u0002"))
                               {:tag-const sv}
                               {:literal sv}))
                           (= ty "V") (fail "java-str of a void expression")
                           :else
                           (let [eager (and (not (t/prim? ty)) (not (t/unbox-of ty)) (not= ty t/string-desc))]
                             {:node n :type (if eager t/string-desc (sharpest-accessible actx ty))
                              :eager eager}))))]
        {:op :java-str :parts parts :operands nodes
         :args (vec (keep :node parts))
         :type t/string-desc}))))

;; java-assert (§5.9)

(defn analyze-java-assert [actx [_ c msg :as form]]
  (let [cn (analyze actx c)
        mn (when (> (count form) 2) (analyze actx msg))
        cls (:class actx)]
    ;; in an interface the flag lives in a synthetic holder class of the outermost class (javac)
    (if (#{:interface :annotation} (:kind (decl! cls)))
      (do (swap! (:assert-holders *unit*) assoc (nest-host cls) nil)
          (swap! (:holder-first *unit*) #(if (contains? % (nest-host cls)) % (assoc % (nest-host cls) :assert)))
          ;; and the interface's <clinit> reads it, so the holder initializes with it
          (swap! (state cls) assoc :interface-assert (nest-host cls)))
      (swap! (state cls) assoc :uses-assert true))
    {:op :assert :test cn :msg mn :class cls
     :holder-top (when (#{:interface :annotation} (:kind (decl! cls))) (nest-host cls))
     :msg-desc (when mn
                 (let [mt (value-type (:type mn))]
                   (case mt ("I" "S" "B") "I" "Z" "Z" "C" "C" "J" "J" "F" "F" "D" "D" t/object-desc)))
     :type "V"}))

;; for-each (§5.7)

(defn analyze-for-each [actx [_ [sym coll :as bv] & body]]
  (when-not (and (vector? bv) (= 2 (count bv)) (symbol? sym))
    (fail "for-each needs a binding vector [x coll]"))
  (let [cn (analyze actx coll)
        ct (value-type (:type cn))
        array? (t/array? ct)
        _ (when-not (or array? (and (t/ref? ct) (env/assignable? ct "Ljava/lang/Iterable;")))
            (fail (str "for-each over a value of type " (pr-str ct) ", neither an array nor Iterable")))
        tag (:tag (meta sym))
        bt (cond tag (actx-desc actx tag) array? (t/elem-type ct) :else t/object-desc)
        aux (fn [nm ty] (make-binding actx (gensym nm) ty))
        hidden (if array?
                 {:arr (aux "arr$" ct) :len (aux "len$" "I") :i (aux "i$" "I")}
                 {:it (aux "i$" "Ljava/util/Iterator;")})
        b (make-binding actx sym bt :mutable (boolean (:mutable (meta sym))))
        target {:id (next-id) :kind :loop :bindings [] :breaks (atom [])}
        iter-m (when-not array?
                 (let [ms (filter #(empty? (first (t/parse-method-desc (:desc %))))
                                  (env/member-methods (t/desc->internal ct) "iterator"))]
                   (invoke-node actx nil ct (first ms) cn [] false)))
        elem (if array?
               {:op :aget :array {:op :local :b (:arr hidden) :type ct}
                :index {:op :local :b (:i hidden) :type "I"} :type (t/elem-type ct)}
               {:op :invoke :kind :interface :owner "java/util/Iterator" :itf true :name "next"
                :desc "()Ljava/lang/Object;" :target {:op :local :b (:it hidden) :type "Ljava/util/Iterator;"}
                :args [] :type t/object-desc})
        elem (cond
               (or array? (= bt t/object-desc)) (convert-node elem bt :what "for-each element")
               (t/prim? bt) (convert-node {:op :cast :class (boxed-desc bt) :expr elem :type (boxed-desc bt)} bt)
               :else {:op :cast :class bt :expr elem :type bt})
        bactx (cond-> (-> actx (with-local b) (assoc :loop target :break-target target) (dissoc :label-loop))
                (:label-loop actx) (assoc-in [:labels (:label-loop actx) :loop] target))
        bn (analyze-body bactx body)]
    {:op :for-each :array array? :coll cn :hidden hidden :b b :elem elem :iterator iter-m
     :target target :body bn :type (if (seq @(:breaks target)) (unify-nodes (cons (const-node :null nil) @(:breaks target))) :null)}))

;; with-resources (§5.9): javac's desugaring of try-with-resources

(defn analyze-with-resources [actx [_ bindings & body]]
  (when-not (and (vector? bindings) (even? (count bindings)) (seq bindings))
    (fail "with-resources needs a vector of resource bindings"))
  (if (> (count bindings) 2)
    (analyze-with-resources actx (list 'with-resources* (subvec bindings 0 2)
                                       (list* 'with-resources* (subvec bindings 2) body)))
    (let [[sym init] bindings
          in (analyze actx init)
          ty (binding-type actx sym in false)
          in (bind-init actx sym in ty)
          rb (make-binding actx sym ty)
          ractx (with-local actx rb)
          bn (analyze-body ractx body)
          null-check? (not= :new (:op in))
          close (analyze ractx (list '. sym 'close))
          tsym (gensym "t$") xsym (gensym "x$")
          tb (make-binding actx tsym "Ljava/lang/Throwable;")
          cactx (with-local ractx tb)
          cbody (analyze cactx
                         (list 'do
                               (if null-check?
                                 (list 'if (list 'clojure.core/some? sym)
                                       (list 'try (list '. sym 'close)
                                             (list 'catch 'java.lang.Throwable xsym (list '. tsym 'addSuppressed xsym))))
                                 (list 'try (list '. sym 'close)
                                       (list 'catch 'java.lang.Throwable xsym (list '. tsym 'addSuppressed xsym))))
                               (list 'throw tsym)))
          normal (if null-check?
                   {:op :if :test {:op :not :expr {:op :nil? :expr {:op :local :b rb :type ty} :type "Z"} :type "Z"}
                    :then close :else (const-node :null nil) :type (unify (:type close) :null)}
                   close)]
      {:op :let :bindings [[rb in]]
       :body {:op :try :body bn :catches [{:classes ["java/lang/Throwable"] :b tb :body cbody}]
              :normal-finally normal :type (:type bn)}
       :type (:type bn)})))

;; ---------------------------------------------------------------------------------------------
;; lambda and method-ref (§5.12)

(def ^:private object-methods #{"equals(Ljava/lang/Object;)" "hashCode()" "toString()"})

(defn find-sam
  "The single abstract method of functional interface n: {:name :desc}."
  [n]
  (when-not (env/interface? n) (fail (str (str/replace n "/" ".") " is not an interface")))
  (let [seen (atom #{})
        ms (for [c (env/all-supertypes n)
                 :when (not= c "java/lang/Object")
                 m (:methods (env/info! c))
                 :let [k (str (:name m) (subs (:desc m) 0 (inc (.indexOf ^String (:desc m) ")"))))]
                 :when (not (object-methods k))
                 :when (not (contains? @seen k))
                 :let [_ (swap! seen conj k)]
                 :when (has? (:flags m) Opcodes/ACC_ABSTRACT)
                 :when (not (has? (:flags m) Opcodes/ACC_STATIC))]
             m)
        ms (vec ms)]
    (when-not (= 1 (count ms))
      (fail (str (str/replace n "/" ".") " is not a functional interface (abstract methods: "
                 (pr-str (map :name ms)) ")")))
    (first ms)))

(declare lambda-method-prefix)

(defn- fi-types
  "[main-interface markers serializable? intersection] of a lambda's target type form. Serializable
  is no marker: it makes the lambda serializable."
  [actx form]
  (let [tn (parse-type (scope-of actx) form)
        names (if (= :inter (:t tn)) (mapv :name (:types tn)) [(:name tn)])
        ser? (boolean (some #(env/assignable? (t/internal->desc %) "Ljava/io/Serializable;") names))]
    [(first names) (vec (remove #{"java/io/Serializable"} (rest names))) ser?
     (when (= :inter (:t tn)) names)]))

(defn- serial-lambda-name!
  "javac's name for a serializable lambda: a hash of the enclosing method's signature, the
  functional interface, the variable assigned and the captured locals (LambdaToMethod)."
  [actx fin sam boundary]
  (let [cls (:class actx)
        m (:method (:method-info actx))
        osig (when (and m (not= :lambda (:kind (:frame actx)))) (or (:sig m) (:desc m)))
        disam (str (when osig (str osig ":"))
                   (str/replace (:owner sam) "/" ".") " "
                   (when-let [pv (:pending-var actx)] (str pv "="))
                   (apply str (for [b @(:captures boundary)] (str (:type b) " " (:sym b) ","))))
        base (str "lambda$" (lambda-method-prefix actx) "$" (Integer/toHexString (.hashCode ^String disam)) "$")
        i (get-in (swap! (state cls) update-in [:lambda-counters base] (fnil inc 0)) [:lambda-counters base])]
    (str base i)))

(defn- lambda-method-prefix [actx]
  (let [mi (:method-info actx)]
    ;; javac names the lambdas it makes from method references in a field initializer after
    ;; the field (explicit lambdas there are static/new)
    (cond (and (:ref-lambda actx) (:field-name actx)) (:field-name actx)
          (nil? mi) (if (:static actx) "static" "new")
          (= "<init>" (:name mi)) "new"
          (= "<clinit>" (:name mi)) "static"
          :else (:name mi))))

(defn- lambda-name! [actx]
  (let [cls (:class actx)
        prefix (lambda-method-prefix actx)
        i (dec (get-in (swap! (state cls) update-in [:lambda-counters prefix] (fnil inc 0))
                       [:lambda-counters prefix]))]
    (str "lambda$" prefix "$" i)))

(defn analyze-lambda [actx [_ fi params & body]]
  (when-not (vector? params) (fail "lambda needs a parameter vector"))
  (let [[fin markers ser inter] (fi-types actx fi)
        sam (find-sam fin)
        [sps sret] (t/parse-method-desc (:desc sam))
        _ (when-not (= (count sps) (count params))
            (fail (str "lambda for " (:name sam) " needs " (count sps) " parameters")))
        ps (mapv (fn [p sp] (if-let [tg (:tag (meta p))] (actx-desc actx tg) sp)) params sps)
        ret (if-let [tg (:tag (meta params))] (actx-desc actx tg) sret)
        cls (:class actx)
        lname (when-not ser (lambda-name! actx))
        boundary {:kind :lambda :captures (atom []) :uses-this (atom false)}
        f (assoc (new-frame :lambda cls (:frame actx) boundary) :static (:static actx))
        lactx (-> actx (assoc :frame f :ret ret :labels {}) (dissoc :loop :break-target :label-loop :in-ctor :ctor-prologue :pending-var :ref-lambda))
        [lactx bs] (reduce (fn [[a bs] [p t]]
                             (let [b (make-binding a p t :mutable (boolean (:mutable (meta p))) :param true)]
                               [(with-local a b) (conj bs b)]))
                           [lactx []] (map vector params ps))
        bn (analyze-body lactx body)
        bn (if (= "V" ret) bn (convert-node bn ret :what "lambda value"))
        lname (or lname (serial-lambda-name! actx fin sam boundary))
        node {:op :lambda :fi fin :markers markers :serializable ser :intersection inter
              :sam sam :inst-desc (t/method-desc ps ret)
              :params bs :body bn :boundary boundary :name lname :class cls :ret ret
              :type (t/internal->desc fin)}]
    (swap! (state cls) update :lambda-nodes (fnil conj []) node)
    (when ser (swap! (state cls) update :serial-nodes (fnil conj []) node))
    node))

(defn- arg-stub [d] {:op :local :type d :b {:id -1 :type d}})

(defn- ref-as-lambda
  "javac makes a lambda method for references it cannot express as a method handle (variable
  arity adaptation): the equivalent lambda form."
  [actx fi ps ret call-fn]
  (let [args (vec (map-indexed (fn [i p] (with-meta (symbol (str "a$" i)) {:tag (symbol (if (t/prim? p) (t/prim-desc->name p) (t/desc->class-name p)))})) ps))
        rtag (symbol (if (or (t/prim? ret) (= "V" ret)) (t/prim-desc->name ret) (t/desc->class-name ret)))]
    (analyze (assoc actx :ref-lambda true) (list 'lambda* fi (with-meta args {:tag rtag}) (call-fn args)))))

(defn analyze-method-ref [actx [_ fi & more :as form]]
  (let [[sig more] (if (vector? (first more)) [(first more) (rest more)] [nil more])
        [recv msym] (case (count more) 1 [nil (first more)] 2 more
                      (fail "method-ref takes FI, an optional signature, an optional receiver and a method"))
        _ (when-not (and (symbol? msym) (namespace msym)) (fail (str "Bad method in method-ref: " msym)))
        [fin markers ser inter] (fi-types actx fi)
        sam (find-sam fin)
        [sps sret] (t/parse-method-desc (:desc sam))
        ps (if sig (mapv #(actx-desc actx %) sig) sps)
        ret (if-let [tg (and sig (:tag (meta sig)))] (actx-desc actx tg) sret)
        cn (or (resolve-class actx (symbol (namespace msym))) (fail (str "Unknown class " (namespace msym))))
        mname (name msym)
        param-tags (:param-tags (meta msym))
        base {:op :method-ref :fi fin :markers markers :serializable ser :intersection inter
              :sam sam :inst-desc (t/method-desc ps ret) :class (:class actx)
              :type (t/internal->desc fin)}
        register (fn [node]
                   (when (and ser (= :method-ref (:op node)))
                     (swap! (state (:class actx)) update :serial-nodes (fnil conj []) node))
                   node)]
    (register
    (cond
      (= recv 'super)
      ;; javac makes a lambda method for super::m
      (let [args (vec (map #(with-meta (gensym "a$") {:tag (symbol (t/desc->class-name %))}) ps))]
        (analyze actx (list 'lambda* fi (with-meta args {:tag (symbol (if (t/prim? ret) (t/prim-desc->name ret) (t/desc->class-name ret)))})
                            (list* '. 'super (symbol (subs mname (if (str/starts-with? mname ".") 1 0))) args))))

      (= mname "new")
      (let [[m va] (select-method actx (filter #(env/accessible? (:class actx) %) (env/constructors cn))
                                  (mapv arg-stub ps) (str "constructor of " cn) param-tags)]
        ;; javac makes a lambda for variable arity adaptation and for classes that need an outer
        ;; instance or captured values
        (if (or va (needs-outer-instance? cn) (= :local (:nesting (decl cn))))
          (ref-as-lambda actx fi ps ret (fn [args] (list* 'new (symbol (str/replace cn "/" ".")) args)))
          (assoc base :kind :new :owner cn :name "<init>" :desc (:desc m))))

      recv
      (let [rn (analyze actx recv)
            rt (value-type (:type rn))
            mn (if (str/starts-with? mname ".") (subs mname 1) mname)
            cands (->> (env/member-methods cn mn) (remove static-flag?) (filter #(env/accessible? (:class actx) %)))
            [m va] (select-method actx cands (mapv arg-stub ps) (str "method " mn) param-tags)]
        (when va (fail "Bound variable arity method references are not supported yet"))
        ;; method handles name the declaring class (javac)
        (assoc base :kind :bound :owner (:owner m) :name mn :desc (:desc m)
               :itf (env/interface? (:owner m)) :recv rn
               :null-check (not (or (= :this-path (:op rn)) (:receiver (:b rn))))))

      (str/starts-with? mname ".")
      (let [mn (subs mname 1)
            cands (->> (env/member-methods cn mn) (remove static-flag?) (filter #(env/accessible? (:class actx) %)))
            [m va] (select-method actx cands (mapv arg-stub (rest ps)) (str "method " mn) param-tags)]
        (when (empty? ps) (fail "An unbound method reference needs the receiver as first parameter"))
        (if va
          (ref-as-lambda actx fi ps ret (fn [[r & args]] (list* '. r (with-meta (symbol mn) {:param-tags (:param-tags (meta msym))}) args)))
        (assoc base :kind :unbound :owner (:owner m) :name mn :desc (:desc m)
               :itf (env/interface? (:owner m)))))

      :else
      (let [cands (->> (env/member-methods cn mname) (filter static-flag?) (filter #(env/accessible? (:class actx) %)))
            [m va] (select-method actx cands (mapv arg-stub ps) (str "static method " mname) param-tags)]
        (if va
          (ref-as-lambda actx fi ps ret (fn [args] (list* '. (symbol (str/replace cn "/" ".")) (with-meta (symbol mname) {:param-tags param-tags}) args)))
        (assoc base :kind :static :owner (:owner m) :name mname :desc (:desc m) :itf (env/interface? (:owner m)))))))))

;; ---------------------------------------------------------------------------------------------
;; patterns and switch (§5.8)
;;
;; pattern: {:kind :type :class desc :b binding-or-nil :test bool}
;;          {:kind :record :class desc :comps [{:accessor method :pattern p}]}

(defn- enum-class? [d]
  (and (t/class-desc? d) (has? (:flags (env/info (t/desc->internal d))) Opcodes/ACC_ENUM)))

(defn analyze-pattern
  "Analyzes a pattern form against a value of static type st. Returns [pattern actx-with-bindings]."
  [actx form st & {:keys [component]}]
  (cond
    (symbol? form)
    (let [tag (:tag (meta form))
          ty (cond tag (actx-desc actx tag)
                   component st
                   (= '_ form) st
                   :else (fail (str "A type pattern needs a type: " form)))
          unnamed (= "_" (name form))
          b (when-not unnamed (make-binding actx form ty :mutable (boolean (:mutable (meta form)))))
          test (not (or (= ty st) (and (t/ref? ty) (t/ref? st) (env/assignable? st ty))
                        (and (t/prim? ty) (= ty st))))]
      (when (and (t/prim? ty) (not= ty st))
        (fail (str "Primitive pattern of type " ty " for a value of type " st " is not supported")))
      [{:kind :type :class ty :b b :test test} (if b (with-local actx b) actx)])

    (and (seq? form) (symbol? (first form)))
    (let [rn (or (resolve-class actx (first form)) (fail (str "Unknown record " (first form))))
          rd (t/internal->desc rn)
          info (env/info! rn)
          comps (or (:components info)
                    (when-let [c (env/load-class rn)]
                      (when (.isRecord ^Class c)
                        (for [rc (.getRecordComponents ^Class c)]
                          {:name (.getName ^java.lang.reflect.RecordComponent rc)
                           :desc (t/class->desc (.getType ^java.lang.reflect.RecordComponent rc))})))
                    (fail (str (first form) " is not a record")))
          _ (when-not (= (count comps) (count (rest form)))
              (fail (str "Record pattern " (first form) " needs " (count comps) " component patterns")))
          [cps actx] (reduce (fn [[cps actx] [c f]]
                               (let [[p actx] (analyze-pattern actx f (:desc c) :component true)
                                     acc {:owner rn :name (:name c) :desc (str "()" (:desc c))}]
                                 [(conj cps {:accessor acc :pattern p}) actx]))
                             [[] actx] (map vector comps (rest form)))]
      [{:kind :record :class rd :comps cps
        :test (not (and (t/ref? st) (env/assignable? st rd)))} actx])

    :else (fail (str "Bad pattern: " (pr-str form)))))

(defn analyze-if-instance [actx [_ [pat x :as bv] then else :as form]]
  (when-not (and (vector? bv) (= 2 (count bv))) (fail "if-instance needs [pattern expr]"))
  (let [xn (analyze actx x)
        xt (value-type (:type xn))
        _ (when-not (t/ref? xt) (fail "if-instance of a primitive"))
        temp (make-binding actx (gensym "inst$") xt)
        [p pactx] (analyze-pattern actx pat xt)
        p (if (= :type (:kind p)) (assoc p :test true) (assoc p :test true))
        tn (analyze pactx then)
        en (analyze actx else)]
    {:op :if-instance :expr xn :temp temp :pattern p :then tn :else en
     :type (unify-nodes [tn en])}))

(defn- switch-map!
  "Registers a use of enum constant `c` of enum e in the outermost class's $SwitchMap$; returns
  its mapped value."
  [actx e c]
  (let [top (nest-host (:class actx))
        maps (:switch-maps *unit*)
        _ (swap! (:holder-first *unit*) #(if (contains? % top) % (assoc % top :switch)))]
    (get-in (swap! maps update-in [top e]
                   (fn [m] (let [m (or m {:values {} :order []})]
                             (if (get (:values m) c) m
                                 (-> m (assoc-in [:values c] (inc (count (:values m))))
                                     (update :order conj c))))))
            [top e :values c])))

(defn- constant-label [actx form desc]
  (let [n (analyze-const-form (scope-of actx) form desc)]
    (or n (fail (str "switch label is not a constant: " (pr-str form))))))

(defn analyze-switch [actx [_ sel & clauses]]
  (let [sn (analyze actx sel)
        st (value-type (:type sn))
        [pairs dflt] (if (odd? (count clauses))
                       [(partition 2 (butlast clauses)) [(last clauses)]]
                       [(partition 2 clauses) nil])
        null-label? #(or (nil? %) (and (seq? %) (some nil? %)))
        enum? (enum-class? st)
        kind (cond (some (fn [[l]] (or (vector? l) (null-label? l))) pairs) :pattern
                   (#{"I" "S" "B" "C"} (prim-type sn)) :int
                   (= st t/string-desc) :string
                   enum? :enum
                   :else (fail (str "switch on a value of type " (pr-str st))))
        sel-node (if (= kind :int) (unboxed sn) sn)
        enum-name (fn [l]
                    (when-not (symbol? l) (fail (str "Enum switch label must be a constant name: " (pr-str l))))
                    (let [en (t/desc->internal st)
                          f (env/find-field en (name l))]
                      (when-not (and f (has? (:flags f) Opcodes/ACC_ENUM))
                        (fail (str "No enum constant " (name l) " in " en)))
                      (name l)))
        cases
        (vec
          (for [[l body] pairs]
            (cond
              (vector? l)
              (let [[pf & opts] l
                    {guard :when} (apply hash-map opts)
                    [p pactx] (analyze-pattern actx pf st)
                    gn (when guard (analyze pactx guard))]
                {:labels [{:pattern p}] :guard gn :body (analyze pactx body)})

              :else
              (let [ls (if (seq? l) l [l])
                    labels (vec (for [x ls]
                                  (cond
                                    (nil? x) {:null true}
                                    (= :default x) {:default true}
                                    (= kind :enum) {:enum (enum-name x)}
                                    (and (= kind :pattern) enum?) {:enum (enum-name x)}
                                    :else
                                    (let [d (case kind :int "I" :string t/string-desc
                                                  (if (#{"Ljava/lang/Integer;" "Ljava/lang/Character;" "Ljava/lang/Short;" "Ljava/lang/Byte;"} st)
                                                    (t/unbox-of st) t/string-desc))
                                          c (constant-label actx x (if (= kind :pattern) (if (= d t/string-desc) d "I") d))]
                                      {:const (:val c) :ctype (:type c)}))))]
                {:labels labels :body (analyze actx body)}))))
        default (when dflt (analyze actx (first dflt)))
        _ (when (= kind :enum)
            (when-not (= (nest-host (:class actx)) (nest-host (t/desc->internal st)))
              (doseq [c cases l (:labels c)] (switch-map! actx (t/desc->internal st) (:enum l)))))
        default-arm? (some (fn [c] (some :default (:labels c))) cases)
        bodies (concat (map :body cases) (when-not default-arm? [(or default (const-node :null nil))]))
        ty (unify-nodes bodies)
        cases (mapv #(update % :body literal-branch ty) cases)
        default (some-> default (literal-branch ty))]
    {:op :switch :kind kind :sel sel-node :sel-type (if (= kind :int) "I" st) :cases cases :default default
     :enum-direct (and (= kind :enum) (= (nest-host (:class actx)) (nest-host (t/desc->internal st))))
     :sel-binding (make-binding actx (gensym "sel$") (if (= kind :int) "I" st))
     :restart (make-binding actx (gensym "restart$") "I")
     :top (nest-host (:class actx))
     :type ty}))

;; ---------------------------------------------------------------------------------------------
;; source path (§9.2): declarations of classes from p/C.clj, without compiling their code

(defn- read-source-forms [url]
  (with-open [r (clojure.lang.LineNumberingPushbackReader.
                  (java.io.InputStreamReader. (.openStream ^java.net.URL url) "UTF-8"))]
    (binding [*read-eval* false]
      (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof :read-cond :allow} r)))))))

(defn import-classes!
  "Imports the classes of an (import ...) form into namespace ns, without evaluating code (which
  the frozen compiler would compile into ns's package, prohibited for java.*). Returns the class
  names that could not be imported (a name already mapped to another class)."
  [ns form]
  (vec (for [spec (rest form)
             :let [spec (if (and (seq? spec) (= 'quote (first spec))) (second spec) spec)]
             cname (if (symbol? spec) [(str spec)] (map #(str (first spec) "." %) (rest spec)))
             :let [ok (try (.importClass ^clojure.lang.Namespace ns (clojure.lang.RT/classForNameNonLoading cname))
                           true
                           (catch Throwable _ false))]
             :when (not ok)]
         cname)))

(defn eval-ns-form!
  "Evaluates a namespace form of a class forms file: in-ns and import without compiling code."
  [f]
  (case (name (first f))
    "in-ns" (let [n (second f) n (if (seq? n) (second n) n)]
              (set! *ns* (create-ns n))
              (when-not (ns-resolve *ns* 'defclass) (refer 'clojure.core)))
    "import" (import-classes! *ns* f)
    (eval f)))

(defn enter-from-source!
  "Looks up the top-level class of internal name n as a source p/C.clj on the class path. Its
  namespace and import forms are evaluated and its class forms entered as declarations only
  (headers and members), so code being compiled can refer to them. Returns n when found."
  [n]
  (let [top (first (str/split n #"\$"))
        tried (or (:source-tried *unit*) (atom #{}))]
    (when (and (:source-tried *unit*) (not (@tried top)))
      (swap! tried conj top)
      (when-let [url (.getResource (clojure.lang.RT/baseLoader) (str top ".clj"))]
        (let [forms (read-source-forms url)
              ns-form (first (filter #(and (seq? %) (#{'in-ns 'ns 'clojure.core/in-ns} (first %))) forms))
              ns-name* (when ns-form (let [x (second ns-form)] (if (seq? x) (second x) x)))
              class-forms* (for [f forms
                                 f (if (and (seq? f) (= 'do (first f))) (rest f) [f])
                                 :when (and (seq? f) (symbol? (first f)) (= "defclass" (name (first f))))]
                             (rest f))
              ;; only a file that declares the class is its source: decided before evaluating anything
              declares? (and (symbol? ns-name*)
                             (some (fn [[nm & _ :as cf]]
                                     (and (symbol? nm)
                                          (= top (let [s (str nm)
                                                       pkg (get (apply hash-map (rest (drop-while (complement keyword?) cf))) :package)]
                                                   (cond (some? pkg) (str (when (seq (str pkg)) (str (str/replace (str pkg) "." "/") "/")) s)
                                                         (str/includes? s ".") (str/replace s "." "/")
                                                         :else (str (str/replace (munge (name ns-name*)) "." "/") "/" s))))))
                                   class-forms*))
              ns (when declares?
                   (binding [*ns* *ns*]
                   (doseq [f forms
                           :when (and (seq? f) (#{'in-ns 'ns 'import 'clojure.core/in-ns 'clojure.core/import}
                                                  (first f)))]
                     (eval-ns-form! f))
                   *ns*))
              class-forms (for [f forms
                                f (if (and (seq? f) (= 'do (first f))) (rest f) [f])
                                :when (and (seq? f) (symbol? (first f)) (= "defclass" (name (first f))))]
                            (rest f))
              from (count @(:order *unit*))
              names (doall (for [cf (when ns class-forms)]
                             (let [parsed (p/parse-class {:ns ns :nesting :top} cf)
                                   tn (top-name ns parsed)]
                               (when-not (decl tn)
                                 (declare-class! {:nesting :top} parsed)))))
              new (subvec @(:order *unit*) from)]
          (doseq [c new] (update-decl! c assoc :declared-only true))
          (doseq [c new] (resolve-header! c))
          (doseq [c new] (resolve-members! c))
          (when (decl n) n))))))

;; ---------------------------------------------------------------------------------------------
;; access$NNN accessors (§5.6, §6): protected members of a superclass in another package used
;; from a nested class, and Outer/super calls, go through a static synthetic method of the
;; enclosing class, named as javac names them

(defn- xop [d base] (.getOpcode (Type/getType (if (keyword? d) t/object-desc d)) (int base)))

(defn- add-accessor!
  "Registers accessor method code (emitted with the class); returns its name."
  [o code desc emit-body]
  (let [anum (dec (get (swap! (state o) update :access-count (fnil inc 0)) :access-count))
        nm (str "access$" anum (quot code 10) (mod code 10))]
    (swap! (state o) update :extra-methods (fnil conj [])
           (fn [^ClassWriter cw]
             (let [mv (.visitMethod cw (bit-or Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC) nm desc nil nil)]
               (.visitCode mv)
               (emit-body mv)
               (.visitMaxs mv 0 0)
               (.visitEnd mv))))
    nm))

(defn- load-params [^MethodVisitor mv descs start]
  (loop [[d & more] descs slot start]
    (when d
      (.visitVarInsn mv (xop d Opcodes/ILOAD) slot)
      (recur more (+ slot (t/size d))))))

(defn- accessor-owner
  "The enclosing class of the current class through which protected member m is accessible,
  when the current class itself cannot access it; else nil."
  [actx m]
  (let [c (:class actx)
        owner (:owner m)]
    (when (and m (has? (:flags m) Opcodes/ACC_PROTECTED)
               (not= (t/package-of owner) (t/package-of c))
               (not (env/subclass? c owner)))
      (loop [o (:outer (decl c))]
        (when o
          (if (env/subclass? o owner) o (recur (:outer (decl o)))))))))

(defn accessorize
  "Rewrites a member access that needs a javac accessor into a call of it."
  [actx node]
  (case (:op node)
    (:get-field :set-field :get-static :set-static)
    (let [f (:field node)]
      (if-let [o (accessor-owner actx (assoc f :owner (or (:declarer f) (:owner f))))]
        (let [static? (static-flag? f)
              od (t/internal->desc o)
              fd (:desc f)
              write? (#{:set-field :set-static} (:op node))
              ps (cond-> [] (not static?) (conj od) write? (conj fd))
              desc (t/method-desc ps fd)
              nm (add-accessor! o (if write? 2 0) desc
                                (fn [^MethodVisitor mv]
                                  (load-params mv ps 0)
                                  (when write?
                                    (.visitInsn mv (if (= 2 (t/size fd))
                                                     (if static? Opcodes/DUP2 Opcodes/DUP2_X1)
                                                     (if static? Opcodes/DUP Opcodes/DUP_X1))))
                                  (.visitFieldInsn mv (if static?
                                                        (if write? Opcodes/PUTSTATIC Opcodes/GETSTATIC)
                                                        (if write? Opcodes/PUTFIELD Opcodes/GETFIELD))
                                                   o (:name f) fd)
                                  (.visitInsn mv (xop fd Opcodes/IRETURN))))]
          {:op :invoke :kind :static :owner o :itf false :name nm :desc desc :target nil
           :args (vec (concat (when-not static? [(:target node)]) (when write? [(:val node)])))
           :type fd})
        node))

    :invoke
    (let [m (:method node)]
      (if-let [o (and m (not= :special (:kind node)) (accessor-owner actx m))]
        (let [static? (= :static (:kind node))
              od (t/internal->desc o)
              [mps r] (t/parse-method-desc (:desc node))
              ps (if static? mps (into [od] mps))
              desc (t/method-desc ps r)
              nm (add-accessor! o 0 desc
                                (fn [^MethodVisitor mv]
                                  (load-params mv ps 0)
                                  (.visitMethodInsn mv (if static? Opcodes/INVOKESTATIC Opcodes/INVOKEVIRTUAL)
                                                    o (:name node) (:desc node) false)
                                  (.visitInsn mv (if (= "V" r) Opcodes/RETURN (xop r Opcodes/IRETURN)))))]
          {:op :invoke :kind :static :owner o :itf false :name nm :desc desc :target nil
           :args (vec (concat (when-not static? [(:target node)]) (:args node))) :type r})
        node))
    node))

(defn outer-super-call
  "(.m Outer/super args) from a nested class or lambda: javac's access$N01 accessor in Outer."
  [actx o mname args param-tags]
  (let [d (decl! o)
        sup (:super d)
        on (outer-this-node actx o)
        arg-nodes (mapv #(analyze actx %) args)
        cands (->> (env/member-methods sup mname) (remove static-flag?))
        [m va] (select-method actx cands arg-nodes (str "method " mname) param-tags)
        [mps r] (t/parse-method-desc (:desc m))
        od (t/internal->desc o)
        ps (into [od] mps)
        desc (t/method-desc ps r)
        nm (add-accessor! o 1 desc
                          (fn [^MethodVisitor mv]
                            (load-params mv ps 0)
                            (.visitMethodInsn mv Opcodes/INVOKESPECIAL sup mname (:desc m) false)
                            (.visitInsn mv (if (= "V" r) Opcodes/RETURN (xop r Opcodes/IRETURN)))))]
    {:op :invoke :kind :static :owner o :itf false :name nm :desc desc :target nil
     :args (into [on] (convert-args m arg-nodes va)) :type r}))

;; ---------------------------------------------------------------------------------------------
;; Clojure data and vars in class bodies (§5.13): constants in private static synthetic fields
;; initialized in <clinit>, as deftype has them

(defn- constant-field!
  "A static field of the current class holding a Clojure constant: returns {:owner :name :desc}."
  [actx k desc init]
  (let [cls (:class actx)
        st (state cls)
        existing (get-in @st [:clj-consts k])]
    (or existing
        (let [i (count (:clj-consts @st))
              f {:owner cls :name (str "const__" i) :desc desc :init init}]
          (swap! st update :clj-consts (fnil assoc (array-map)) k f)
          f))))

(defn clj-constant
  "A keyword or quoted datum as a constant node (read at class initialization)."
  [actx v]
  (let [desc (cond (keyword? v) "Lclojure/lang/Keyword;"
                   (symbol? v) "Lclojure/lang/Symbol;"
                   :else t/object-desc)
        f (constant-field! actx [:const v] desc {:kind :read :text (binding [*print-meta* true] (pr-str v))})]
    {:op :get-static :field (assoc f :flags (bit-or Opcodes/ACC_STATIC Opcodes/ACC_FINAL)) :owner (:owner f) :type desc}))

(defn clj-var-field [actx v]
  (constant-field! actx [:var v] "Lclojure/lang/Var;"
                   {:kind :var :ns (str (ns-name (.ns ^clojure.lang.Var v))) :name (str (.sym ^clojure.lang.Var v))}))

(defn clj-collection
  "A collection literal: built by clojure.lang.RT/vector, map or set from its boxed elements."
  [actx fname desc elems]
  (let [ns (mapv #(analyze actx %) elems)]
    {:op :invoke :kind :static :owner "clojure/lang/RT" :itf false :name fname
     :desc (str "([Ljava/lang/Object;)" desc)
     :args [{:op :array-init :type "[Ljava/lang/Object;" :elems (mapv #(convert-node % t/object-desc) ns)}]
     :type desc}))
