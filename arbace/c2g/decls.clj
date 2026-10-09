(ns arbace.c2g.decls
  "c2g's declarations (C2G-SPEC §4.5, §5, §6, §7.11-§7.13): what a translated class becomes in
  Go forms: its struct or interface, class interface C_I and accessor Self_C (non-leaf
  classes), markers, method implementations, dispatch methods and forwarders, constructors,
  statics and lazy class initialization, the Object protocol, instanceof and cast, and its
  registration with member tables (c2g_classes)."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.jrt :as jrt]
            [arbace.c2g.code :as c])
  (:import (arbace.asm Opcodes)))

(def ^:dynamic *reached* "The set of reached method keys [class name desc]." #{})

(defn- tag [sym type] (with-meta sym {:tag type}))

(defn- has? [flags f] (m/has? flags f))

(defn gt [pkg d] (m/go-type pkg d))

(defn- param-list
  "Go parameter symbols tagged with the Go types of descs, named after the analyzer's
  bindings when given."
  [pkg descs syms]
  (vec (map (fn [d s] (tag s (gt pkg d))) descs syms)))

(defn- result-meta [pkg r] (when-not (= r "V") (gt pkg r)))

(defn- with-ret [v pkg r] (if-let [g (result-meta pkg r)] (vary-meta v assoc :tag g) v))

(defn new-fn-state
  [pkg n & {:as more}]
  (merge {:pkg pkg :class n :fn-id (gensym "f") :targets {} :try-frame nil :captured {}
          :names (atom {}) :taken (atom (into #{} nm/reserved-locals)) :used (atom #{})
          :counter (atom {}) :label-used (atom #{})}
         more))

(defn terminating?
  "Is Go form f a terminating statement (Go spec, \"Terminating statements\"): a return or
  panic, a block (let, do) ending in one, an if whose two branches end in one?"
  [f]
  (and (seq? f)
       (case (first f)
         (return panic) true
         (let do) (terminating? (last f))
         if (and (= 4 (count f)) (terminating? (nth f 2)) (terminating? (nth f 3)))
         false)))

(defn ensure-terminated
  "Go needs a terminating statement in a function with results: a final panic when the last
  form is not one."
  [forms has-results]
  (if (and has-results (not (terminating? (last forms))))
    (concat forms [(list 'panic "c2g: unreachable")])
    forms))

(defn stub-body [n name desc]
  [(list 'panic (list (m/jrt-sym (m/pkg n) "C2g_NotTranslated")
                      (str (str/replace n "/" ".") "." name desc)))])

(defn- captured-fields
  "binding id -> Go form reading the captured local from the class's field (local and
  anonymous classes), for code of class n."
  [n tsym]
  (let [d (a/decl n)]
    (if (and d (#{:local :anon} (:nesting d)))
      (into {} (for [b (:captures @(:state d))]
                 [(:id b) (list (symbol (str ".-" (nm/field-name (str "val$" (:sym b))))) tsym)]))
      {})))

(defn member-line
  "The class forms line of method or constructor m (its member form's), or nil."
  [m]
  (:line (:member m)))

(defn- at-line
  "Declaration forms with the line of member m (C2G-SPEC §4.3)."
  [m forms]
  (if-let [l (member-line m)]
    (map #(if (seq? %) (vary-meta % assoc :c2g/line l) %) forms)
    forms))

(defn- method-code-forms
  "The Go statements of method m's body (analyzed body ab) of class n."
  [pkg n m ab {:keys [t this static]}]
  (let [ret (:ret m)]
    (binding [c/*f* (new-fn-state pkg n :static static :t t :this this :method-ret ret
                                  :captured (if t (captured-fields n t) {})
                                  :method-name (:name m)
                                  :sync (has? (:flags m) Opcodes/ACC_SYNCHRONIZED))
              c/*line* (member-line m)]
      (let [fid (:fn-id c/*f*)]
        (binding [c/*f* (assoc c/*f* :method-fn fid)]
          (let [params (vec (for [b (:params ab)] (c/binding-sym b)))
                body (c/with-block
                       (fn []
                         (when (has? (:flags m) Opcodes/ACC_SYNCHRONIZED)
                           (let [lk (if static (c/csym n "_class") (or this t))]
                             (c/emit! (list (c/jsym "MonitorEnter") lk))
                             (c/emit! (list 'defer (list (c/jsym "MonitorExit") lk)))))
                         (if (= "V" ret)
                           (c/stmt! (:body ab))
                           (c/ret! (:body ab)))))]
            [params (ensure-terminated body (not= "V" ret))]))))))

;; ---------------------------------------------------------------------------------------
;; members of a translated class

(defn class-fields [n]
  (for [f (:fields (a/decl n))
        :when (m/desc-in-world? (:desc f))]
    f))

(defn class-methods
  "The methods of translated class n that exist in Go (descriptors in the world)."
  [n]
  (for [mm (:methods (a/decl n))
        :when (not= "<clinit>" (:name mm))
        :when (if (= "<init>" (:name mm))
                (m/mdesc-in-world? (e/ctor-real-desc n mm))
                (m/mdesc-in-world? (:desc mm)))]
    mm))

(defn- body-of [n mm] (get (:bodies @(:state (a/decl n))) [(:name mm) (:desc mm)]))

(defn reached? [n mm] (contains? *reached* [n (:name mm) (:desc mm)]))

;; ---------------------------------------------------------------------------------------
;; types (§5.2, §5.3, §5.5)

(defn super-struct-type
  "The Go struct type a class embeds: its superclass's struct (jrt.Object at the root)."
  [pkg n]
  (let [s (:super (a/decl n))]
    (if (or (nil? s) (= s "java/lang/Object"))
      (m/jrt-sym pkg "Object")
      (m/class-sym pkg s))))

(defn super-iface-type [pkg n]
  (let [s (:super (a/decl n))]
    (if (or (nil? s) (= s "java/lang/Object"))
      (m/jrt-sym pkg "Object_I")
      (m/class-sym pkg s "_I"))))

(defn field-go-type
  "The Go type of a field, atomic for a volatile one (§8.2)."
  [pkg f]
  (let [d (:desc f)]
    (if (c/volatile-field? f)
      (case d
        "Z" 'atomic/Bool "I" 'atomic/Int32 "J" 'atomic/Int64
        ("B" "S" "C") 'atomic/Int32 "F" 'atomic/Uint32 "D" 'atomic/Uint64
        (if (m/pointer-desc? d)
          (let [g (gt pkg d)] (list 'atomic/Pointer (second g)))
          (list (m/jrt-sym pkg "Volatile") (gt pkg d))))
      (gt pkg d))))

(defn- extra-fields
  "The outer instance and captured locals' fields of an inner, local or anonymous class."
  [pkg n]
  (let [d (a/decl n)]
    (concat
      (when (and (:outer-instance? d) (m/in-world? (:outer d)))
        [(tag (symbol (nm/field-name (e/this0-name n))) (gt pkg (str "L" (:outer d) ";")))])
      (when (#{:local :anon} (:nesting d))
        (for [b (:captures @(:state d)) :when (m/desc-in-world? (:type b))]
          (tag (symbol (nm/field-name (str "val$" (:sym b)))) (gt pkg (:type b))))))))

(defn iface-method-elem [pkg mm]
  (let [[ps r] (t/parse-method-desc (:desc mm))]
    (list (symbol (nm/method-base (:name mm) (:desc mm)))
          (with-ret (vec (map-indexed (fn [i p] (tag (symbol (str "p" i)) (gt pkg p))) ps)) pkg r))))

(defn interface-closure
  "The interfaces a class or interface n implements (directly and through its supertypes),
  in the world."
  [n]
  (filter #(and (m/interface? %) (not= % n)) (m/all-supertypes n)))

(defn hw-markers
  "The Is_ markers jrt's hand-written struct (and its ancestors) has."
  [super]
  (let [g (m/go-name super)]
    (set (for [mn (jrt/struct-methods (:jrt m/*w*) g) :when (str/starts-with? mn "Is_")] (subs mn 3)))))

(defn markers
  "The interfaces whose marker class n's struct declares: those not already marked by its
  superclass's struct."
  [n]
  (let [s (:super (a/decl n))
        sup-marked (cond (or (nil? s) (= s "java/lang/Object")) #{}
                         (m/translated? s) (set (map m/go-name (interface-closure s)))
                         :else (hw-markers s))]
    (remove #(contains? sup-marked (m/go-name %)) (interface-closure n))))

;; ---------------------------------------------------------------------------------------
;; constructors (§5.2)

(defn ctor-desc [n mm] (e/ctor-real-desc n mm))

(defn- ctor-call-translator
  "Translates the constructor call node of a constructor of class n (super. or this.):
  the superclass's or this class's constructor body on the embedded struct."
  [pkg n info]
  (fn [node]
    (let [d (a/decl n)
          leaf (m/leaf? n)
          thisx (if leaf 't 'this)
          kind (:kind node)]
      (cond
        ;; a constructor whose parameters name a class outside the world does not exist in Go
        (some #(not (m/in-world? %)) (m/method-desc-classes (:desc (:ctor node))))
        (c/emit! (c/missing-expr (str "constructor " (or (:class node) n) (:desc (:ctor node))
                                      " is not in the closed world")
                                 "V"))
        (= kind :this)
        (let [real (ctor-desc n (:ctor node))
              base (nm/ctor-base real)
              extra (concat (when (:outer-instance? d) [(:outer-param info)])
                            (when (:super-outer d) [(:super-outer-param info)])
                            (when (or (= :enum (:kind d)) (:enum-body d)) (:enum-params info)))
              [ps _] (t/parse-method-desc (:desc (:ctor node)))
              args (c/operands (:args node) ps)
              caps (when (#{:local :anon} (:nesting d))
                     (for [b (:captures @(:state d))] (get (:cap-params info) (:id b))))]
          (c/emit! (apply list (symbol (str "." base)) 't
                          (concat (when-not leaf ['this]) extra (map :x args) caps))))
        :else
        (let [sup (:class node)]
          (cond
            (or (nil? sup) (= sup "java/lang/Object")) nil
            (not (m/in-world? sup)) (c/emit! (c/missing-expr (str "superclass " sup) "V"))
            :else
            (let [ss (list (symbol (str ".-" (m/go-name sup))) 't)
                  sctor (:ctor node)
                  real (if (m/translated? sup) (ctor-desc sup sctor) (:desc sctor))
                  base (nm/ctor-base real)
                  sd (a/decl sup)
                  thisv (if leaf 't 'this)]
              (cond
                (:enum-args node)
                (c/emit! (list (symbol (str "." base)) ss thisv (first (:enum-params info)) (second (:enum-params info))))
                (:anon-args node)
                (c/emit! (apply list (symbol (str "." base)) ss thisv
                                (concat (when (:super-outer d) [(:super-outer-param info)])
                                        (when (:super-outer-is-outer d) [(:outer-param info)])
                                        (when-let [p (:super-outer-path d)]
                                          [(reduce (fn [x cl] (list (symbol (str ".-" (nm/field-name (e/this0-name cl)))) x))
                                                   (:outer-param info) p)])
                                        (when (:enum-body d) (:enum-params info))
                                        (map (fn [b] (c/use-sym! (c/binding-sym b))) (:params info)))))
                :else
                (let [[ps _] (t/parse-method-desc (:desc sctor))
                      lead (concat (when (and sd (:outer-instance? sd)) [[(:outer node) (str "L" (:outer sd) ";")]]))
                      vals (c/operands (concat (map first lead) (:args node)) (concat (map second lead) ps))
                      caps (when (and sd (#{:local :anon} (:nesting sd)))
                             (for [b (:captures @(:state sd))] (:x (c/local-val {:op :local :b b}))))]
                  (c/emit! (apply list (symbol (str "." base)) ss thisv (concat (map :x vals) caps))))))))))))

(defn ctor-forms
  "The allocation function C_New_... (concrete classes) and the constructor body Ctor_... of
  constructor mm of class n."
  [pkg n mm]
  (let [d (a/decl n)
        g (m/go-name n)
        leaf (m/leaf? n)
        real (ctor-desc n mm)
        [rps _] (t/parse-method-desc real)
        base (nm/ctor-base real)
        ab (body-of n mm)
        reached (reached? n mm)
        st @(:state d)]
    (binding [c/*f* (new-fn-state pkg n :t 't :this (if leaf 't 'this) :method-ret "V" :ctor true :method-name "<init>")
              c/*line* (member-line mm)]
      (let [fid (:fn-id c/*f*)
            outer-p (when (:outer-instance? d) (c/fresh-raw "this_0"))
            so-p (when (:super-outer d) (c/fresh-raw "x0"))
            enum-ps (when (or (= :enum (:kind d)) (:enum-body d)) [(c/fresh-raw "enumName") (c/fresh-raw "enumOrdinal")])
            params (vec (for [b (:params ab)] (c/binding-sym b)))
            params (if (and (empty? params) (seq (:params mm)) (not ab))
                     (vec (for [i (range (count (:params mm)))] (c/fresh-raw (str "a" i))))
                     params)
            caps (when (#{:local :anon} (:nesting d)) (:captures st))
            cap-ps (vec (for [b caps] (c/fresh-raw (str "val_" (nm/munge-name (:sym b))))))
            all-ps (vec (concat (when outer-p [outer-p]) (when so-p [so-p]) enum-ps params cap-ps))
            info {:outer-param outer-p :super-outer-param so-p :enum-params enum-ps :params (:params ab)
                  :cap-params (zipmap (map :id caps) cap-ps)}
            body (binding [c/*f* (assoc c/*f* :method-fn fid :outer-param outer-p
                                        :ctor-call-fn (ctor-call-translator pkg n info)
                                        ;; captured locals come from the constructor's parameters
                                        :captured (zipmap (map :id caps) cap-ps))]
                   (if-not (or reached (:derived mm) (nil? ab))
                     (stub-body n "<init>" (:desc mm))
                     (c/with-block
                       (fn []
                         (doseq [s all-ps] (c/use-sym! s))
                         (let [calls-super (or (:calls-super ab) (nil? ab))]
                           (when calls-super
                             ;; captures and the outer instance first, as javac stores them
                             (doseq [[b p] (map vector caps cap-ps)]
                               (c/emit! (list 'set! (list (symbol (str ".-" (nm/field-name (str "val$" (:sym b))))) 't) p)))
                             (when outer-p
                               (let [od (str "L" (:outer d) ";")]
                                 (c/emit! (if (m/pointer-desc? od)
                                            (list (c/jsym "NN") outer-p)
                                            (list (list 'inst (c/jsym "C2g_NotNull") (gt pkg od)) outer-p))))
                               (c/emit! (list 'set! (list (symbol (str ".-" (nm/field-name (e/this0-name n)))) 't) outer-p))))
                           (if (:nested-call ab)
                             (c/stmt! (:body ab))
                             (do
                               (doseq [p (:prologue ab)] (c/stmt! p))
                               (if-let [call (:call ab)]
                                 ((ctor-call-translator pkg n info) call)
                                 ((ctor-call-translator pkg n info) {:op :ctor-call :kind :super :class (:super d)
                                                                     :ctor {:desc "()V"} :args []}))
                               (when calls-super
                                 (binding [c/*f* (assoc c/*f* :captured (captured-fields n 't))]
                                   (doseq [i (:init st)] (c/stmt! i))))
                               (when-let [b (:body ab)] (c/stmt! b))))
                           (when (and (= :record (:kind d)) (or (:compact mm) (= :record-canonical (:derived mm))))
                             (doseq [[b cm] (map vector (:params ab) (:components d))]
                               (c/emit! (list 'set! (list (symbol (str ".-" (nm/field-name (:name cm)))) 't)
                                              (c/binding-sym b))))))))))
            recv (tag 't (list '* (symbol g)))
            ctor-form (list* 'go/method (symbol base)
                             (vec (concat [recv] (when-not leaf [(tag 'this (symbol (str g "_I")))])
                                          (param-list pkg rps all-ps)))
                             body)
            new-ps (vec (for [i (range (count rps))] (tag (symbol (str "p" i)) (gt pkg (nth rps i)))))
            new-form (when-not (or (has? (:flags d) Opcodes/ACC_ABSTRACT) (m/interface? n))
                       (list* 'go/func (symbol (nm/new-name g real))
                             (with-meta new-ps {:tag (list '* (symbol g))})
                             (concat
                               (when-not (m/trivial-init? n) [(list (symbol (str g "_Init")))])
                               [(list 'let ['t (list 'addr (list 'lit (symbol g)))]
                                      (apply list (symbol (str "." base)) 't (concat (when-not leaf ['t]) (map #(symbol (str %)) new-ps)))
                                      't)])))]
        (at-line mm (remove nil? [new-form ctor-form]))))))

;; ---------------------------------------------------------------------------------------
;; methods (§5.4)

(defn- record-object-body
  "A record's derived toString, hashCode or equals, as java.lang.runtime.ObjectMethods makes
  them: Name[c=v, ...]; 31 * h + the component's hash, in order; the same class and equal
  components (wrapper compare for primitives, Objects.equals for references)."
  [pkg n mm ps]
  (let [d (a/decl n)
        comps (:components d)
        js (fn [s] (m/jrt-sym pkg s))
        fld (fn [o cm] (list (symbol (str ".-" (nm/field-name (:name cm)))) o))
        hash (fn [cm x]
               (case (:desc cm)
                 ("I" "S" "B" "C") (list 'conv 'int32 x)
                 "Z" (list 'if x 1231 1237)
                 "J" (list 'conv 'int32 (list 'bit-xor x (list 'conv 'int64 (list '>> (list 'conv 'uint64 x) 32))))
                 "F" (list (js "C2g_FloatBits") x)
                 "D" (let [b (list (js "C2g_DoubleBits") x)]
                       (list 'conv 'int32 (list 'bit-xor b (list 'conv 'int64 (list '>> (list 'conv 'uint64 b) 32)))))
                 (list (js "C2g_ObjHash") (c/coerce x (:desc cm) "Ljava/lang/Object;" false))))
        strof (fn [cm x]
                (case (:desc cm)
                  ("I" "S" "B") (list (js "StrOfInt") (list 'conv 'int32 x))
                  "J" (list (js "StrOfLong") x)
                  "C" (list (js "StrOfChar") x)
                  "Z" (list (js "StrOfBool") x)
                  "F" (list (js "StrOfFloat") x)
                  "D" (list (js "StrOfDouble") x)
                  (list (js "StrOfObj") (c/coerce x (:desc cm) "Ljava/lang/Object;" false))))]
    (case (:name mm)
      "toString"
      (let [simple (let [s (str/replace (subs n (inc (.lastIndexOf ^String n "/"))) #".*\$" "")] s)
            parts (concat [(c/string-lit (str simple "["))]
                          (apply concat
                                 (map-indexed (fn [i cm]
                                                [(c/string-lit (str (when (pos? i) ", ") (:name cm) "="))
                                                 (strof cm (fld 't cm))])
                                              comps))
                          [(c/string-lit "]")])]
        [(list 'return (apply list (js "Concat") parts))])
      "hashCode"
      [(list 'return (reduce (fn [acc cm] (list '+ (list '* acc 31) (hash cm (fld 't cm)))) (list 'conv 'int32 0) comps))]
      "equals"
      (let [o (first ps)
            g (m/go-name n)]
        [(list 'when (list '== (list 'conv 'any 't) o) (list 'return true))
         (list 'when (list 'or (list '== o nil) (list '!= (list (js "GetClass") o) (symbol (str g "_class")))) (list 'return false))
         (list 'let ['other (list 'assert (list '* (symbol g)) o)]
               (list 'return (reduce (fn [acc cm]
                                       (let [a (fld 't cm) b (fld 'other cm)
                                             e (case (:desc cm)
                                                 "F" (list '== (list (js "C2g_FloatBits") a) (list (js "C2g_FloatBits") b))
                                                 "D" (list '== (list (js "C2g_DoubleBits") a) (list (js "C2g_DoubleBits") b))
                                                 ("I" "S" "B" "C" "Z" "J") (list '== a b)
                                                 (list (js "C2g_ObjEquals") (c/coerce a (:desc cm) "Ljava/lang/Object;" false)
                                                       (c/coerce b (:desc cm) "Ljava/lang/Object;" false)))]
                                         (if (true? acc) e (list 'and acc e))))
                                     true comps)))]))))

(defn derived-body
  "Go statements of a derived method (enum values/valueOf/$values, bridges, record members)."
  [pkg n mm ps]
  (let [g (m/go-name n)
        d (a/decl n)
        leaf (m/leaf? n)
        this (if leaf 't 'this)]
    (case (:derived mm)
      :enum-values [(list 'return (list '.Copy (symbol (str g "__VALUES"))))]
      :enum-valueOf [(list 'return (list (symbol (str g "_Cast"))
                                         (list (m/jrt-sym pkg "Enum_ValueOf_Class_String__Enum")
                                               (symbol (str g "_class")) (first ps))))]
      :enum-$values [(list 'return (apply list (m/jrt-sym pkg "RefArrayOf") (symbol (str g "_class"))
                                          (for [cn (:constants d)] (symbol (str g "_" (nm/munge-name (:name cn)))))))]
      :bridge
      (let [tg (:bridge-of mm)
            [tps tr] (t/parse-method-desc (:desc tg))
            [bps br] (t/parse-method-desc (:desc mm))
            args (map (fn [p bp tp]
                        (cond (= bp tp) p
                              (and (t/ref? bp) (t/ref? tp) (env/assignable? bp tp)) (c/coerce p bp tp false)
                              :else (:x (c/cast-val {:class tp :expr {:op :arbace.c2g.code/go :x (c/coerce p bp "Ljava/lang/Object;" false) :t "Ljava/lang/Object;"}}))))
                      ps bps tps)
            tbase (nm/method-base (:name tg) (:desc tg))
            call (if (:special mm)
                   (let [sup (:super d)]
                     (if (and (m/hand-written? (:owner tg)) (not (contains? (jrt/struct-methods (:jrt m/*w*) (m/go-name (:owner tg))) (nm/impl-name tbase))))
                       (apply list (symbol (str "." tbase)) (list (symbol (str ".-" (m/go-name sup))) 't) args)
                       (apply list (symbol (str ".Impl_" tbase)) (list (symbol (str ".-" (m/go-name sup))) 't) this args)))
                   (apply list (symbol (str "." tbase)) this args))]
        (if (= "V" br)
          [call]
          [(list 'return
                 (if (and (t/ref? tr) (t/ref? br) (not= tr br) (not (env/assignable? tr br)))
                   ;; a bridge narrowing a generic return (Spliterators' EmptySpliterator.OfDouble's
                   ;; trySplit): javac's checkcast
                   (:x (c/cast-val {:class br :expr {:op :arbace.c2g.code/go :x (c/coerce call tr "Ljava/lang/Object;" false) :t "Ljava/lang/Object;"}}))
                   (c/coerce call tr br false)))]))
      :record-accessor (let [cm (:component mm)]
                         [(list 'return (list (symbol (str ".-" (nm/field-name (:name cm)))) 't))])
      :record-object-method (record-object-body pkg n mm ps)
      [(list 'panic (list (m/jrt-sym pkg "C2g_NotTranslated") (str "derived " (name (:derived mm)))))])))

(defn- touches-statics?
  "Does analyzed code ab access a static field of a class in classes (directly: the guarded
  accesses of other classes run their own initialization)?"
  [ab classes]
  (let [hit (volatile! false)]
    (letfn [(w [x]
              (when-not @hit
                (cond
                  ;; every map, not only nodes: a switch's cases are a map of bodies
                  (map? x) (do (when (and (:op x)
                                          (#{:get-static :set-static :var-deref :var-invoke} (:op x))
                                          (contains? classes (or (:declarer (:field x)) (:owner (:field x)))))
                                 (vreset! hit true))
                               (run! w (vals x)))
                  (or (vector? x) (seq? x)) (run! w x)
                  :else nil)))]
      (w (:body ab)))
    @hit))

(defn needs-entry-guard?
  "Does static method mm of class n (body ab) start with n's initialization guard (§6.2)? Not
  when the initialization is benign (unobservable but through n's statics, model/benign-init?)
  and the body reads none of the statics of n and its superclasses: then delaying the
  initialization to the first guarded access cannot be told apart (C2G-NOTES, phase 2D)."
  [n ab]
  (and (not (m/trivial-init? n))
       (or (nil? ab)
           (not (m/benign-init? n))
           (touches-statics? ab (set (m/superclass-chain n))))))

(defn method-forms
  "The Go method (instance) or function (static) of method mm of class n: its body when
  reached, a stub otherwise."
  [pkg n mm]
  (let [g (m/go-name n)
        iface (m/interface? n)
        leaf (and (not iface) (m/leaf? n))
        static (m/static? mm)
        [ps r] (t/parse-method-desc (:desc mm))
        base (nm/method-base (:name mm) (:desc mm))
        ab (body-of n mm)
        abstract (has? (:flags mm) Opcodes/ACC_ABSTRACT)
        native (has? (:flags mm) Opcodes/ACC_NATIVE)
        reached (reached? n mm)]
    (when-not abstract
      (let [[pnames body]
            (cond
              native
              (let [pn (vec (for [i (range (count ps))] (symbol (str "p" i))))
                    ;; jrt's hand-written native when jrt defines it (the JDK's, and the
                    ;; natives of arbace/lang's variants, phase 2A); else in the class's own
                    ;; package (arbace/lang's: the evaluator's that c2g writes, c2g_dyn.go)
                    fname (str g "_" base "_native")
                    f (if (contains? (:funcs (:jrt m/*w*)) fname)
                        (m/jrt-sym pkg fname)
                        (symbol fname))
                    call (apply list f (concat (when-not static ['t]) pn))]
                [pn [(if (= "V" r) call (list 'return call))]])
              (and (:derived mm)
                   (or (not= :bridge (:derived mm))
                       (and reached (m/mdesc-in-world? (:desc (:bridge-of mm))))))
              (let [pn (vec (for [i (range (count ps))] (symbol (str "p" i))))]
                [pn (derived-body pkg n mm pn)])
              ;; a void method without body forms is empty (as emit writes it)
              (and reached (nil? ab) (= "V" r) (not (:analysis-failed @(:state (a/decl n)))))
              [(vec (for [i (range (count ps))] (symbol (str "p" i)))) []]
              (and reached ab (not (:analysis-failed @(:state (a/decl n)))))
              (try
                (method-code-forms pkg n mm ab {:t (when (and (not static) (not iface)) 't)
                                                :this (when-not static (if (or leaf iface) (if iface 'this 't) 'this))
                                                :static static})
                (catch Exception ex
                  (if (:arbace.c2g.code/fail (ex-data ex))
                    [(vec (for [i (range (count ps))] (symbol (str "p" i))))
                     [(list 'panic (list (m/jrt-sym pkg "C2g_NotTranslated")
                                         (str (str/replace n "/" ".") "." (:name mm) (:desc mm) ": " (.getMessage ex))))]]
                    (throw (ex-info (str "c2g: translating " n "." (:name mm) (:desc mm) ": " (.getMessage ex)) {} ex)))))
              :else
              [(vec (for [i (range (count ps))] (symbol (str "p" i)))) (stub-body n (:name mm) (:desc mm))])
            plist (param-list pkg ps pnames)
            plist (with-ret plist pkg r)]
        (at-line mm
         (cond
          static
          [(list* 'go/func (symbol (str g "_" base)) plist
                  (concat (when (and (not native) (needs-entry-guard? n ab)) [(list (symbol (str g "_Init")))]) body))]
          iface
          ;; default and private methods of interfaces: functions taking the receiver first
          [(list* 'go/func (symbol (str g "_" base))
                  (with-meta (vec (cons (tag 'this (symbol g)) plist)) (meta plist))
                  body)]
          leaf
          [(list* 'go/method (symbol base) (with-meta (vec (cons (tag 't (list '* (symbol g))) plist)) (meta plist)) body)]
          :else
          [(list* 'go/method (symbol (nm/impl-name base))
                  (with-meta (vec (concat [(tag 't (list '* (symbol g))) (tag 'this (symbol (str g "_I")))] plist)) (meta plist))
                  body)]))))))

(defn- forwarder
  "A dispatch method of concrete class n for virtual method k: calls the implementation."
  [pkg n [name desc :as k] impl]
  (let [g (m/go-name n)
        [ps r] (t/parse-method-desc desc)
        base (nm/method-base name desc)
        pn (vec (for [i (range (count ps))] (symbol (str "p" i))))
        plist (with-ret (vec (cons (tag 't (list '* (symbol g))) (param-list pkg ps pn))) pkg r)
        call (case (:kind impl)
               (:class :jrt-impl) (apply list (symbol (str ".Impl_" base)) 't 't pn)
               :default (apply list (m/class-sym pkg (:owner impl) (str "_" base)) 't pn)
               nil)]
    (when call
      (list 'go/method (symbol base) plist (if (= "V" r) call call)))))

(defn object-forms
  "Ref, GetClass__Class, ToString__String, Clone__O, CloneShallow of concrete class n (§5.8)."
  [pkg n]
  (let [g (m/go-name n)
        recv (tag 't (list '* (symbol g)))
        ts (m/impl-of n ["toString" "()Ljava/lang/String;"])
        cl (m/impl-of n ["clone" "()Ljava/lang/Object;"])
        cloneable (env/assignable? (str "L" n ";") "Ljava/lang/Cloneable;")]
    (remove nil?
            [(list 'go/method 'Ref (with-meta [recv] {:tag 'any}) (list 'when (list '== 't nil) (list 'return nil)) 't)
             ;; a class with an instance field c2g$class (the evaluator's fns: one run-time class
             ;; per fn, EVAL-NOTES.md) answers that class when it is set
             (if-let [f (first (filter #(and (= "c2g$class" (:name %)) (not (m/static? %))) (class-fields n)))]
               (list 'go/method 'GetClass__Class (with-meta [recv] {:tag (list '* (m/jrt-sym pkg "Class"))})
                     (list 'when (list '!= (list (symbol (str ".-" (nm/field-name (:name f)))) 't) nil)
                           (list 'return (list (symbol (str ".-" (nm/field-name (:name f)))) 't)))
                     (symbol (str g "_class")))
               (list 'go/method 'GetClass__Class (with-meta [recv] {:tag (list '* (m/jrt-sym pkg "Class"))}) (symbol (str g "_class"))))
             (when (= :object (:kind ts))
               (list 'go/method 'ToString__String (with-meta [recv] {:tag (list '* (m/jrt-sym pkg "String"))})
                     (list (m/jrt-sym pkg "Object_toString") 't)))
             (when (= :object (:kind cl))
               (list 'go/method 'Clone__O (with-meta [recv] {:tag 'any}) (list (m/jrt-sym pkg "C2g_ObjectClone") 't)))
             (list 'go/method 'CloneShallow (with-meta [recv] {:tag 'any})
                   (if cloneable
                     (list 'let ['c (list 'arbace.core/deref 't)]
                           (list '.ClearHeader (list 'addr 'c))
                           (list 'addr 'c))
                     (list 'panic (list (m/jrt-sym pkg "CloneNotSupported") 't))))])))

(defn dispatch-forms
  "The dispatch methods and forwarders of concrete class n (§5.4)."
  [pkg n]
  (let [own (set (for [mm (class-methods n) :when (and (not (m/static? mm)) (not= "<init>" (:name mm)))]
                   [(:name mm) (:desc mm)]))
        leaf (m/leaf? n)]
    (for [[k mm] (concat (m/vmethods n) (for [k (sort m/object-keys)] [k nil]))
          :let [impl (m/impl-of n k)]
          :when (not (and leaf (contains? own k) (= n (:owner impl))))
          :let [f (cond
                    (nil? impl)
                    (let [[name desc] k
                          [ps r] (t/parse-method-desc desc)
                          pn (vec (for [i (range (count ps))] (symbol (str "p" i))))]
                      (list* 'go/method (symbol (nm/method-base name desc))
                             (with-ret (vec (cons (tag 't (list '* (symbol (m/go-name n)))) (param-list pkg ps pn))) pkg r)
                             [(list 'panic (list (m/jrt-sym pkg "C2g_NotTranslated") (str "abstract " (str/replace n "/" ".") "." name desc)))]))
                    (#{:promoted :object} (:kind impl)) nil
                    :else (forwarder pkg n k impl))]
          :when f]
      f)))

;; ---------------------------------------------------------------------------------------
;; statics and class initialization (§6)

(defn static-forms [pkg n]
  (let [g (m/go-name n)]
    (for [f (class-fields n) :when (m/static? f)
          :let [s (symbol (nm/static-field-name g (:name f)))
                cv (env/const-value f)]]
      (cond
        (and (some? cv) (t/prim? (:desc f)) (has? (:flags f) Opcodes/ACC_FINAL)
             (not (and (#{"F" "D"} (:desc f)) (let [d (double cv)] (or (Double/isNaN d) (Double/isInfinite d) (and (zero? d) (neg? (Math/copySign 1.0 d))))))))
        (let [v (case (:desc f)
                  "Z" (boolean cv)
                  "C" (long (int (if (char? cv) cv (char cv))))
                  ("F" "D") (c/float-lit (:desc f) cv)
                  (long cv))]
          (list 'go/const (with-meta s {:tag (gt pkg (:desc f)) :val v}) v))
        (and (some? cv) (#{"F" "D"} (:desc f)))
        (list 'go/var (tag s (gt pkg (:desc f))) (binding [c/*f* (new-fn-state pkg n)] (c/float-lit (:desc f) cv)))
        (and (some? cv) (string? cv))
        (list 'go/var (tag s (gt pkg (:desc f))) (binding [c/*f* (new-fn-state pkg n)] (c/string-lit cv)))
        :else
        (list 'go/var (tag s (field-go-type pkg f)))))))

(defn init-forms
  "C_init, C_Init and C_clinit of a class with non-trivial initialization (§6.2)."
  [pkg n]
  (when-not (m/trivial-init? n)
    (let [g (m/go-name n)
          d (a/decl n)
          st @(:state d)
          sup (:super d)
          reached (contains? *reached* [n "<clinit>" "()V"])
          body (binding [c/*f* (new-fn-state pkg n :static true :method-ret "V" :clinit true :method-name "<clinit>")]
                 (binding [c/*f* (assoc c/*f* :method-fn (:fn-id c/*f*))]
                   (c/with-block
                     (fn []
                       (when (and sup (m/translated? sup) (not (m/trivial-init? sup)))
                         (c/emit! (list (m/class-sym pkg sup "_Init"))))
                       (if-not reached
                         (c/emit! (list 'panic (list (m/jrt-sym pkg "C2g_NotTranslated") (str (str/replace n "/" ".") ".<clinit>"))))
                         (do
                           (doseq [[_ {:keys [name desc init]}] (:clj-consts st)]
                             (c/emit! (list 'set! (symbol (nm/static-field-name g name))
                                            (c/missing-expr (str "Clojure constant " (case (:kind init) :var (str "#'" (:ns init) "/" (:name init)) (:text init)))
                                                            desc))))
                           (doseq [nd (:clinit st)] (c/stmt! nd))))))))]
      [(list 'go/var (tag (symbol (str g "_init")) (m/jrt-sym pkg "ClassInit")))
       (list 'go/func (symbol (str g "_Init")) []
             (list 'when (list 'not (list '.Done (symbol (str g "_init"))))
                   (list '.Run (symbol (str g "_init")) (symbol (str g "_clinit")))))
       (list* 'go/func (symbol (str g "_clinit")) [] body)])))

(defn clj-const-fields [pkg n]
  (let [g (m/go-name n)
        st @(:state (a/decl n))]
    (for [[_ {:keys [name desc]}] (:clj-consts st)
          :when (m/desc-in-world? desc)]
      (list 'go/var (tag (symbol (nm/static-field-name g name)) (gt pkg desc))))))

(defn instance-forms
  "C_InstanceOf and C_Cast (§5.7)."
  [pkg n]
  (let [g (m/go-name n)
        ty (if (or (m/interface? n) (not (m/leaf? n)))
             (symbol (if (m/interface? n) g (str g "_I")))
             (list '* (symbol g)))]
    (if (m/interface? n)
      ;; an interface: the Go assertion succeeds for every object of a class made at run time
      ;; (Dyn, §5.12), whose class must implement the interface (the nominal check)
      (let [dyn-check (list 'when 'ok
                            (list 'let [(list 'values 'd 'dyn) (list 'assert (m/jrt-sym pkg "Dynamic") 'x)]
                                  (list 'when 'dyn (list 'set! 'ok (list '.DynImplements 'd (symbol (str g "_class")))))))]
        [(list 'go/func (symbol (str g "_InstanceOf")) (with-meta [(tag 'x 'any)] {:tag 'bool})
               (list 'let [(list 'values '_ 'ok) (list 'assert ty 'x)] dyn-check 'ok))
         (list 'go/func (symbol (str g "_Cast")) (with-meta [(tag 'x 'any)] {:tag ty})
               (list 'when (list '== 'x nil) (list 'return nil))
               (list 'let [(list 'values 'v 'ok) (list 'assert ty 'x)]
                     dyn-check
                     (list 'when (list 'not 'ok) (list 'panic (list (m/jrt-sym pkg "ClassCast") 'x (symbol (str g "_class")))))
                     'v))])
      [(list 'go/func (symbol (str g "_InstanceOf")) (with-meta [(tag 'x 'any)] {:tag 'bool})
             (list 'let [(list 'values '_ 'ok) (list 'assert ty 'x)] 'ok))
       (list 'go/func (symbol (str g "_Cast")) (with-meta [(tag 'x 'any)] {:tag ty})
             (list 'when (list '== 'x nil) (list 'return nil))
             (list 'let [(list 'values 'v 'ok) (list 'assert ty 'x)]
                   (list 'when (list 'not 'ok) (list 'panic (list (m/jrt-sym pkg "ClassCast") 'x (symbol (str g "_class")))))
                   'v))])))

;; ---------------------------------------------------------------------------------------
;; one class

(defn reflected-forms
  "A JDK interface declared from reflection (no class forms): its Go interface with the
  methods in the world, its default and static methods as stubs (no code to translate), and
  instanceof/cast."
  [n]
  (let [pkg (m/pkg n)
        g (m/go-name n)
        info (m/info n)
        ms (for [mm (:methods info) :when (m/mdesc-in-world? (:desc mm))] (assoc mm :owner n))]
    (vec
      (concat
        [(list 'c2g/comment (str "---- " (str/replace n "/" ".") " (declared from reflection)"))
         (list 'go/type (symbol g)
               (apply list 'interface (m/jrt-sym pkg "Object_I")
                      (concat
                        (for [s (interface-closure n)] (m/class-sym pkg s))
                        [(list (symbol (str "Is_" g)) [])]
                        (for [mm ms
                              :when (and (not (m/static? mm)) (not (m/private? mm)))
                              :when (not (m/object-keys [(:name mm) (:desc mm)]))]
                          (iface-method-elem pkg mm)))))]
        (for [mm ms
              :when (and (not (m/abstract-m? mm)) (not (m/private? mm)))
              :let [[ps r] (t/parse-method-desc (:desc mm))
                    pn (vec (for [i (range (count ps))] (symbol (str "p" i))))
                    plist (with-ret (param-list pkg ps pn) pkg r)
                    plist (if (m/static? mm) plist (with-meta (vec (cons (tag 'this (symbol g)) plist)) (meta plist)))]]
          (list* 'go/func (symbol (str g "_" (nm/method-base (:name mm) (:desc mm)))) plist
                 (stub-body n (:name mm) (:desc mm))))
        (instance-forms pkg n)))))

(defn class-forms
  "All Go forms of translated class n (in package pkg), in order."
  [n]
  (let [pkg (m/pkg n)
        g (m/go-name n)
        d (a/decl n)
        iface (m/interface? n)
        leaf (and (not iface) (m/leaf? n))
        abstract (has? (:flags d) Opcodes/ACC_ABSTRACT)
        fields (class-fields n)
        ifields (remove m/static? fields)
        methods (class-methods n)]
    (vec
      (concat
        [(list 'c2g/comment (str "---- " (str/replace n "/" ".")))]
        (if iface
          [(list 'go/type (symbol g)
                 (apply list 'interface (m/jrt-sym pkg "Object_I")
                        (concat
                          (for [s (interface-closure n)] (m/class-sym pkg s))
                          [(list (symbol (str "Is_" g)) [])]
                          (for [mm (:methods d)
                                :when (and (not (m/static? mm)) (not (m/private? mm)) (m/mdesc-in-world? (:desc mm)))
                                :when (not (m/object-keys [(:name mm) (:desc mm)]))]
                            (iface-method-elem pkg mm)))))]
          (concat
            [(list 'go/type (symbol g)
                   (apply list 'struct (super-struct-type pkg n)
                          (concat
                            (for [f ifields] (tag (symbol (nm/field-name (:name f))) (field-go-type pkg f)))
                            (extra-fields pkg n))))]
            (when-not leaf
              [(list 'go/type (symbol (str g "_I"))
                     (apply list 'interface (super-iface-type pkg n)
                            (concat
                              (for [s (interface-closure n)] (m/class-sym pkg s))
                              [(list (symbol (str "Self_" g)) (with-meta [] {:tag (list '* (symbol g))}))]
                              (for [mm methods
                                    :when (m/virtual-member? mm)
                                    :when (not (m/object-keys [(:name mm) (:desc mm)]))]
                                (iface-method-elem pkg mm)))))
               (list 'go/method (symbol (str "Self_" g)) (with-meta [(tag 't (list '* (symbol g)))] {:tag (list '* (symbol g))}) 't)])
            (for [j (markers n)]
              (list 'go/method (symbol (str "Is_" (m/go-name j))) [(tag 't (list '* (symbol g)))]))))
        (static-forms pkg n)
        (clj-const-fields pkg n)
        (init-forms pkg n)
        (when-not iface
          (mapcat #(ctor-forms pkg n %) (filter #(= "<init>" (:name %)) methods)))
        (mapcat #(method-forms pkg n %) (remove #(= "<init>" (:name %)) methods))
        (when (and (not iface) (not abstract))
          (concat (dispatch-forms pkg n) (object-forms pkg n)))
        (instance-forms pkg n)))))
