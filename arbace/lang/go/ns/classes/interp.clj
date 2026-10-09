(ns arbace.classes.interp
  "The Go build's class forms (doc/go/CLASSFORMS-REPL.md): the classes of class forms made at run
  time, their code interpreted. arbace.classes.analyze analyzes the class forms as for the JVM
  (declarations, members, what javac derives, code into typed nodes); in place of
  arbace.classes.emit, this namespace builds, from the analyzed nodes, the nodes of the
  interpreter (arbace.lang.Compiler$CF, class forms translated into the program), one tree per
  method on its first call, and makes the classes through the host (Compiler$CF$Rt/HOST: on Go
  classes made at run time, Dyn, C2G-SPEC §5.12).

  It is a namespace of the Go build (arbace/lang/go/ns/), and loads on the JVM too, where the
  interpreter is tested against the compiled classes (test/classforms/)."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.parse :as p]
            [arbace.classes.analyze :as a])
  (:import (arbace.lang Compiler$CF$Rt Compiler$CF$Klass Compiler$CF$Meth Compiler$CF$Node
                        Compiler$CF$Const Compiler$CF$Local Compiler$CF$SetLocal
                        Compiler$CF$SelfField Compiler$CF$OuterPath Compiler$CF$ArithJ
                        Compiler$CF$ArithD Compiler$CF$Conv Compiler$CF$Boxing Compiler$CF$Unbox
                        Compiler$CF$Cmp Compiler$CF$Test Compiler$CF$IsInst Compiler$CF$CheckCast
                        Compiler$CF$NullChecked Compiler$CF$Do Compiler$CF$Let Compiler$CF$Labeled
                        Compiler$CF$Jump Compiler$CF$Recur Compiler$CF$If Compiler$CF$AndOr
                        Compiler$CF$Throw Compiler$CF$Try Compiler$CF$Monitor Compiler$CF$CallI
                        Compiler$CF$CallV Compiler$CF$CallR Compiler$CF$ObjectCall
                        Compiler$CF$VarDeref Compiler$CF$VarInvoke Compiler$CF$FiAdapter
                        Compiler$CF$NewI Compiler$CF$CtorCall Compiler$CF$NewR Compiler$CF$GetField
                        Compiler$CF$SetField Compiler$CF$GetStatic Compiler$CF$SetStatic
                        Compiler$CF$FieldR Compiler$CF$NewArray Compiler$CF$ArrayInit
                        Compiler$CF$Aget Compiler$CF$Aset Compiler$CF$Alength Compiler$CF$ArrayClone
                        Compiler$CF$JavaStr Compiler$CF$ForEach Compiler$CF$Pat Compiler$CF$Switch
                        Compiler$CF$IfInstance Compiler$CF$MakeLambda Compiler$CF$RecordOp)
           (java.lang.reflect Method Constructor Field Modifier)))

;; ---------------------------------------------------------------------------------------------
;; the classes made so far

(defonce ^{:doc "internal name -> the Klass of the interpreted class of that name defined last."}
  registry (atom {}))

(def ^:dynamic *klasses*
  "internal name -> Klass of the classes of the compilation being built."
  nil)

(defn klass
  "The Klass of internal name n when n is an interpreted class (of this compilation, or defined
  earlier and still the class of that name), else nil."
  ^Compiler$CF$Klass [n]
  (when (string? n)
    (or (get *klasses* n)
        (when-let [^Compiler$CF$Klass k (get @registry n)]
          ;; on Go the name may have been taken by a class made since (deftype); on the JVM's
          ;; test host there are no Class objects
          (if (or (nil? (.-cls k)) (identical? (.-cls k) (env/load-class n))) k nil)))))

(defn- fail [msg & [data]] (throw (ex-info msg (merge {:arbace/compile-error true} data))))

;; ---------------------------------------------------------------------------------------------
;; types

(defn tc
  "The type char of a node or binding type: the descriptor's first char for primitives and V,
  L for references, N for null, X for none."
  [ty]
  (cond (= ty :null) \N
        (or (= ty :none) (nil? ty)) \X
        (keyword? ty) \L
        :else (let [c (.charAt ^String ty 0)] (if (or (= c \L) (= c \[)) \L c))))

(defn kind [ty] (Compiler$CF$Rt/kind (char (tc ty))))

(def ^:private prim-classes
  {"I" Integer/TYPE "J" Long/TYPE "S" Short/TYPE "B" Byte/TYPE "C" Character/TYPE
   "Z" Boolean/TYPE "F" Float/TYPE "D" Double/TYPE "V" Void/TYPE})

(defn desc->class
  "The Class of a descriptor: a primitive's, an interpreted class's (nil on the JVM's test host,
  where it is Object), an array's, or a loaded class."
  ^Class [d]
  (cond
    (prim-classes d) (prim-classes d)
    (= d :null) Object
    (t/array? d) (let [e (loop [e d] (if (t/array? e) (recur (t/elem-type e)) e))
                       ek (when (t/class-desc? e) (klass (t/desc->internal e)))]
                   (if (and ek (nil? (.-cls ek)))
                     (Class/forName (str (apply str (repeat (t/array-dims d) "[")) "Ljava.lang.Object;"))
                     (arbace.lang.RT/classForNameNonLoading (t/desc->class-name d))))
    :else (let [n (t/desc->internal d)]
            (if-let [k (klass n)]
              (or (.-cls k) Object)
              (or (env/load-class n) (fail (str "Class not found: " (str/replace n "/" "."))))))))

(defn internal->class ^Class [n] (desc->class (t/internal->desc n)))

;; ---------------------------------------------------------------------------------------------
;; building context: one per method body
;;
;; ctx: {:class internal-name :klass Klass :slots atom{binding-id [kind index]} :nr atom :np atom
;;       :targets {target-id {:brk code :rec code :slots [...] :kinds [...] :type t}}
;;       :ret desc :self slot (the receiver's, -1 for static code) :ctor-site fn}

(defn new-ctx [cls ret]
  {:class cls :klass (klass cls) :slots (atom {}) :nr (atom 0) :np (atom 0) :targets {}
   :ret ret :self -1})

(defn slot!
  "A new slot for a value of type ty (in the long slots for J and D kinds)."
  [ctx ty]
  (case (kind ty)
    (\J \D) (let [i @(:np ctx)] (swap! (:np ctx) inc) i)
    (let [i @(:nr ctx)] (swap! (:nr ctx) inc) i)))

(defn bind!
  "A slot for binding b (by its id)."
  [ctx b]
  (let [ty (:type b)
        i (slot! ctx ty)]
    (swap! (:slots ctx) assoc (:id b) [(kind ty) i])
    i))

(declare node coerce)

(defn ^Compiler$CF$Node konst [ty v] (Compiler$CF$Const. (char (tc ty)) v))

(defn nodes ^"[Larbace.lang.Compiler$CF$Node;" [xs]
  (into-array Compiler$CF$Node xs))

(defn- target-code [id rec?] (+ (* 2 id) (if rec? 1 0)))

;; field layouts

(defn instance-fields
  "The instance fields of interpreted class n in order: [{:name :desc :flags :owner}], its
  superclasses' first; then the hidden ones (this$0, val$x)."
  [^Compiler$CF$Klass k]
  (:fields (.-info k)))

(defn field-index
  "The index of field `fname` of interpreted class k (or of an interpreted superclass), or nil."
  [^Compiler$CF$Klass k fname]
  (loop [k k]
    (when k
      (if-let [i (get (:field-index (.-info k)) fname)]
        i
        (recur (.-sup k))))))

(defn static-index [^Compiler$CF$Klass k fname]
  (get (:static-index (.-info k)) fname))

(defn- find-static
  "[Klass index] of static field fname as seen from interpreted class k (its own, its
  superclasses', its interfaces'), or nil."
  [^Compiler$CF$Klass k fname]
  (when k
    (if-let [i (static-index k fname)]
      [k i]
      (or (some #(find-static % fname) (seq (.-ifaces k)))
          (find-static (.-sup k) fname)))))

;; reflection on classes that are not interpreted

(defn- params-of [desc] (into-array Class (map desc->class (first (t/parse-method-desc desc)))))

(defn find-method
  "The java.lang.reflect.Method named mname with descriptor desc of class c or its supertypes,
  accessible, or nil."
  ^Method [^Class c mname desc]
  (let [ps (params-of desc)]
    (or (try (.getMethod c mname ps) (catch Exception _ nil))
        (loop [c c]
          (when c
            (or (try (doto (.getDeclaredMethod c mname ps) (.setAccessible true))
                     (catch Exception _ nil))
                (some #(find-method % mname desc) (.getInterfaces c))
                (recur (.getSuperclass c))))))))

(defn real-method
  "The reflective method for a call of owner's method mname desc: owner's own, or for an
  interpreted owner the one its nearest non-interpreted supertype has (Object's toString)."
  ^Method [owner mname desc]
  (if-let [k (klass owner)]
    (or (when-let [sc (.-superClass k)] (find-method sc mname desc))
        (some #(find-method % mname desc) (seq (.-interfaces k)))
        (when (.-sup k) (real-method (.-iname (.-sup k)) mname desc))
        (some #(real-method (.-iname ^Compiler$CF$Klass %) mname desc) (seq (.-ifaces k))))
    (when-let [c (env/load-class owner)]
      (find-method c mname desc))))

(defn find-field ^Field [^Class c fname]
  (loop [c c]
    (when c
      (or (try (doto (.getDeclaredField c fname) (.setAccessible true)) (catch Exception _ nil))
          (some #(find-field % fname) (.getInterfaces c))
          (recur (.getSuperclass c))))))

;; ---------------------------------------------------------------------------------------------
;; locals

(defn local-node
  "The node reading binding b in ctx: its slot, the receiver, or a captured value (a field of
  the receiver: local and anonymous classes' val$x)."
  [ctx b]
  (let [ty (:type b)]
    (if-let [[_ i] (get @(:slots ctx) (:id b))]
      (Compiler$CF$Local. (char (tc ty)) (int i))
      (if (or (:receiver b) (:slot0 b))
        (if (neg? (:self ctx))
          (fail (str "No receiver here: " (:sym b)))
          (Compiler$CF$Local. \L (int (:self ctx))))
        (let [k (:klass ctx)
              i (when k (get (:capture-index (.-info k)) (:id b)))]
          (if (and i (>= (:self ctx) 0))
            (Compiler$CF$SelfField. (char (tc ty)) (int (:self ctx)) (int i))
            (fail (str "Local not available here: " (:sym b)))))))))

;; ---------------------------------------------------------------------------------------------
;; conversions (emit-convert)

(defn convert
  "Node n (of type from) as a value of type `to`, converted as the JVM converts: primitive
  widening and narrowing, boxing by the value's own type, unboxing a wrapper."
  [^Compiler$CF$Node n from to]
  (let [from (a/value-type from)]
    (cond
      (or (= from :none) (= from to) (= to "V") (nil? to) (= from :null)) n
      (and (t/prim? from) (t/prim? to))
      (if (= (tc from) (tc to)) n (Compiler$CF$Conv. (char (tc to)) (char (tc from)) n))
      (t/prim? from) (Compiler$CF$Boxing. (char (tc from)) n)
      (t/prim? to) (let [w (get t/unbox-of from)]
                     (Compiler$CF$Unbox. (char (tc to)) (char (if w (tc w) \L)) n))
      :else n)))

(defn coerce
  "The node of analyzed node an, converted to type `to` (emit-to)."
  [ctx an to]
  (if (and (= :const (:op an)) (t/prim? to) (t/prim? (:type an)) (not= "Z" to))
    (konst to (a/const-of-type to (:val an)))
    (convert (node ctx an) (:type an) to)))

(defn value-convert
  "Node n of type from as type to, also by checkcast and by unboxing from a non-wrapper
  (method references, bridges: LambdaMetafactory's and javac's adaptations)."
  [^Compiler$CF$Node n from to]
  (cond
    (or (= from to) (= to "V")) n
    (and (t/prim? from) (t/prim? to)) (convert n from to)
    (t/prim? from) (let [b (Compiler$CF$Boxing. (char (tc from)) n)]
                     (if (#{"Ljava/lang/Object;" (t/internal->desc (t/box-of from))} to)
                       b
                       (Compiler$CF$CheckCast. (desc->class to) (klass (t/desc->internal to)) b)))
    (t/prim? to) (let [w (t/internal->desc (t/box-of to))
                       n (if (= from w) n (Compiler$CF$CheckCast. (desc->class w) nil n))]
                   (Compiler$CF$Unbox. (char (tc to)) (char (tc to)) n))
    (or (= to "Ljava/lang/Object;") (and (string? from) (env/assignable? from to))) n
    :else (Compiler$CF$CheckCast. (desc->class to) (when (t/class-desc? to) (klass (t/desc->internal to))) n)))

;; ---------------------------------------------------------------------------------------------
;; the nodes

(def ^:private arith-ops
  {:add 0 :sub 1 :mul 2 :div 3 :rem 4 :neg 5 :inc 6 :dec 7 :and 8 :or 9 :xor 10 :shl 11
   :shr 12 :ushr 13 :not 14 :andnot 15})

(def ^:private cmp-ops {:< 0 :<= 1 :> 2 :>= 3 :== 4 :!= 5})

(defn- vt [an] (a/value-type (:type an)))

(defmulti build-op (fn [ctx an] (:op an)))

(defn node
  "The interpreter's node of analyzed node an."
  ^Compiler$CF$Node [ctx an]
  (build-op ctx an))

(defmethod build-op :default [ctx an]
  (fail (str "The Go build's class forms do not support " (:op an) " yet")))

(defmethod build-op :const [ctx an]
  (let [ty (:type an)]
    (konst ty (if (t/prim? ty) (a/const-of-type ty (:val an)) (:val an)))))

(defmethod build-op :none [ctx an] (konst :null nil))

(defmethod build-op :local [ctx an] (local-node ctx (:b an)))

(defmethod build-op :set-local [ctx an]
  (let [b (:b an)
        [_ i] (or (get @(:slots ctx) (:id b)) (fail (str "set! of a local not in this frame: " (:sym b))))]
    (Compiler$CF$SetLocal. (char (tc (:type b))) (int i) (coerce ctx (:val an) (:type b)))))

(defn- this0-index
  "The index of the this$0 field of interpreted class c."
  [c]
  (let [k (or (klass c) (fail (str "Not an interpreted class: " c)))]
    (or (get (:field-index (.-info k)) "this$0")
        (fail (str "No outer instance in " c)))))

(defmethod build-op :this-path [ctx an]
  (if (neg? (:self ctx))
    (fail "No receiver for an outer instance here")
    (Compiler$CF$OuterPath. (Compiler$CF$Local. \L (int (:self ctx)))
                            (int-array (map this0-index (:path an))))))

(defmethod build-op :outer-param-path [ctx an]
  (Compiler$CF$OuterPath. (node ctx (:base an)) (int-array (map this0-index (:path an)))))

(defmethod build-op :class-lit [ctx an]
  (konst "Ljava/lang/Class;" (let [d (:class an)]
                               (if-let [k (and (t/class-desc? d) (klass (t/desc->internal d)))]
                                 (.-cls k)
                                 (desc->class d)))))

(defmethod build-op :convert [ctx an]
  (convert (node ctx (:expr an)) (:from an) (:to an)))

(defmethod build-op :null-checked [ctx an]
  (Compiler$CF$NullChecked. (node ctx (:expr an))))

(defmethod build-op :cast [ctx an]
  (let [e (node ctx (:expr an))]
    (if (= :none (:type (:expr an)))
      e
      (let [d (:class an)]
        (Compiler$CF$CheckCast. (desc->class d) (when (t/class-desc? d) (klass (t/desc->internal d))) e)))))

(defmethod build-op :instance? [ctx an]
  (let [d (:class an)]
    (Compiler$CF$IsInst. (desc->class d) (when (t/class-desc? d) (klass (t/desc->internal d)))
                             (node ctx (:expr an)))))

(defmethod build-op :arith [ctx an]
  (let [{:keys [family o args type]} an
        ty type
        op (int (or (arith-ops o) (fail (str "arith " o))))
        [x y] args]
    (case (tc ty)
      (\F \D) (Compiler$CF$ArithD. (char (tc ty)) op (coerce ctx x ty) (when y (coerce ctx y ty)))
      (\I \J) (Compiler$CF$ArithJ. (char (tc ty)) op (boolean (= family :exact))
                                   (coerce ctx x ty)
                                   (when y (if (#{:shl :shr :ushr} o) (node ctx y) (coerce ctx y ty))))
      (fail (str "arith of type " ty)))))

(defmethod build-op :compare [ctx an]
  (let [{:keys [cmp t args]} an]
    (Compiler$CF$Cmp. (int (cmp-ops cmp)) (boolean (#{"F" "D"} t))
                      (coerce ctx (first args) t) (coerce ctx (second args) t))))

(defmethod build-op :not [ctx an] (Compiler$CF$Test. 0 (node ctx (:expr an)) nil))
(defmethod build-op :nil? [ctx an] (Compiler$CF$Test. 1 (node ctx (:expr an)) nil))
(defmethod build-op :identical? [ctx an]
  (Compiler$CF$Test. 2 (node ctx (first (:args an))) (node ctx (second (:args an)))))
(defmethod build-op :bool= [ctx an]
  (Compiler$CF$Test. 3 (coerce ctx (first (:args an)) "Z") (coerce ctx (second (:args an)) "Z")))

(defn- and-or [ctx an or?]
  (let [ty (vt an)]
    (Compiler$CF$AndOr. (char (tc ty)) (boolean or?)
                        (nodes (map #(if (= "Z" ty) (node ctx %) (coerce ctx % ty)) (:args an))))))

(defmethod build-op :and [ctx an] (and-or ctx an false))
(defmethod build-op :or [ctx an] (and-or ctx an true))

(defmethod build-op :if [ctx an]
  (let [ty (vt an)
        branch (fn [b] (if (#{:none :null} ty) (node ctx b) (coerce ctx b ty)))]
    (Compiler$CF$If. (char (tc ty)) (node ctx (:test an)) (branch (:then an)) (branch (:else an)))))

(defmethod build-op :do [ctx an]
  (Compiler$CF$Do. (char (tc (vt an))) (nodes (map #(node ctx %) (:statements an))) (node ctx (:ret an))))

(defn- let-node [ctx an loop?]
  (let [ty (vt an)
        bs (:bindings an)
        ;; the initial values are built before their bindings are entered (a binding's
        ;; initializer does not see it)
        parts (reduce (fn [acc [b init]]
                        (let [n (coerce ctx init (:type b))
                              i (bind! ctx b)]
                          (conj acc [i (if (= :none (:type init)) \X (kind (:type b))) n])))
                      [] bs)
        target (:target an)
        tid (when loop? (:id target))
        ctx (if loop?
              (assoc-in ctx [:targets tid]
                        {:brk (target-code tid false) :rec (target-code tid true)
                         :slots (mapv first parts)
                         :kinds (mapv (fn [[b _]] (kind (:type b))) bs)
                         :types (mapv (fn [[b _]] (:type b)) bs)
                         :type ty})
              ctx)
        body (node ctx (:body an))]
    (Compiler$CF$Let. (char (tc ty)) (int-array (map first parts)) (char-array (map second parts))
                      (nodes (map #(nth % 2) parts)) body
                      (int (if loop? (target-code tid false) 0))
                      (int (if loop? (target-code tid true) 0)))))

(defmethod build-op :let [ctx an] (let-node ctx an false))
(defmethod build-op :loop [ctx an] (let-node ctx an true))

(defmethod build-op :label [ctx an]
  (let [ty (vt an)
        tid (:id (:target an))
        ctx (assoc-in ctx [:targets tid] {:brk (target-code tid false) :type ty})]
    (Compiler$CF$Labeled. (char (tc ty)) (node ctx (:body an)) (int (target-code tid false)))))

(defmethod build-op :break [ctx an]
  (let [tg (or (get (:targets ctx) (:id (:target an))) (fail "break target not in scope"))
        ty (:type tg)
        v (:val an)]
    (cond
      (#{:none "V"} ty) (Compiler$CF$Jump. (int (:brk tg)) \V (when v (node ctx v)))
      (= :null ty) (Compiler$CF$Jump. (int (:brk tg)) \L (when v (node ctx v)))
      :else (Compiler$CF$Jump. (int (:brk tg)) (char (kind ty)) (when v (coerce ctx v ty))))))

(defmethod build-op :recur [ctx an]
  (let [tg (or (get (:targets ctx) (:id (:target an))) (fail "recur target not in scope"))
        args (:args an)]
    (Compiler$CF$Recur. (int (:rec tg)) (int-array (:slots tg)) (char-array (:kinds tg))
                        (nodes (map (fn [a ty] (coerce ctx a ty)) args (:types tg))))))

(defmethod build-op :return [ctx an]
  (let [ret (:ret ctx)
        v (:val an)]
    (if (= ret "V")
      (Compiler$CF$Jump. Compiler$CF$Rt/RETURN \V (when v (node ctx v)))
      (Compiler$CF$Jump. Compiler$CF$Rt/RETURN (char (kind ret)) (coerce ctx v ret)))))

(defmethod build-op :throw [ctx an] (Compiler$CF$Throw. (node ctx (:expr an))))

(defmethod build-op :try [ctx an]
  (let [ty (vt an)
        rt (if (= ty :null) "Ljava/lang/Object;" ty)
        expr? (not (#{:none "V"} ty))
        conv (fn [b] (if expr? (coerce ctx b rt) (node ctx b)))
        body (conv (:body an))
        catches (:catches an)
        slots (mapv (fn [c] (bind! ctx (:b c))) catches)
        handlers (mapv (fn [c] (conv (:body c))) catches)
        flat (for [[ci c] (map-indexed vector catches) cn (:classes c)] [ci cn])]
    (Compiler$CF$Try. (char (tc ty)) body
                      (into-array Class (map (fn [[_ cn]] (if (klass cn) nil (internal->class cn))) flat))
                      (into-array Compiler$CF$Klass (map (fn [[_ cn]] (klass cn)) flat))
                      (int-array (map first flat))
                      (int-array slots)
                      (nodes handlers)
                      (when (:finally an) (node ctx (:finally an)))
                      (when (:normal-finally an) (node ctx (:normal-finally an))))))

(defmethod build-op :monitor [ctx an]
  (let [ty (vt an)]
    (Compiler$CF$Monitor. (char (tc ty)) (node ctx (:lock an)) (node ctx (:body an)))))

(defmethod build-op :assert [ctx an]
  ;; $assertionsDisabled: assertions are off unless enabled (java -ea), as on the JVM by default
  (konst :null nil))

;; ---------------------------------------------------------------------------------------------
;; members

(defn- arg-nodes [ctx args desc]
  (nodes (map (fn [a ty] (coerce ctx a ty)) args (first (t/parse-method-desc desc)))))

(defn- ptypes [desc] (char-array (map tc (first (t/parse-method-desc desc)))))

(defn method-of
  "The Meth of interpreted class k declaring or inheriting method key (non-virtual lookup:
  its own, then its superclasses', then its interfaces' defaults and statics)."
  ^Compiler$CF$Meth [^Compiler$CF$Klass k key]
  (when k
    (or (.declared k key)
        (method-of (.-sup k) key)
        (some #(method-of % key) (seq (.-ifaces k))))))

(defmethod build-op :invoke [ctx an]
  (let [{:keys [kind owner name desc target args]} an
        ty (:type an)
        key (str name desc)
        k (when-not (t/array? owner) (klass owner))]
    (cond
      (:array-clone an)
      (let [n (Compiler$CF$ArrayClone. (node ctx target))]
        (if (:no-cast an) n
            (let [d (or (:cast-to an) owner)] (Compiler$CF$CheckCast. (desc->class d) nil n))))

      (= kind :static)
      (if k
        (Compiler$CF$CallI. (char (tc ty)) (or (method-of k key) (fail (str "No method " key " in " owner)))
                            nil (arg-nodes ctx args desc))
        (Compiler$CF$CallR. (char (tc ty)) (or (real-method owner name desc) (fail (str "No method " owner "." key)))
                            nil (arg-nodes ctx args desc) (ptypes desc)))

      (= kind :special)
      (let [tn (coerce ctx target (if (t/array? owner) owner (t/internal->desc owner)))]
        (cond
          (and k (method-of k key))
          (Compiler$CF$CallI. (char (tc ty)) (method-of k key) tn (arg-nodes ctx args desc))
          (and (#{"toString" "hashCode" "equals"} name)
               (= (real-method owner name desc) (find-method Object name desc)))
          (Compiler$CF$ObjectCall. (char (tc ty)) name tn (when (seq args) (coerce ctx (first args) "Ljava/lang/Object;")))
          :else
          (fail (str "The Go build's class forms cannot call " owner "." key " non-virtually yet"))))

      :else
      (let [tt (a/value-type (:type target))
            tn (coerce ctx target (if (t/array? tt) tt (t/internal->desc owner)))
            rm (real-method owner name desc)]
        (if (or k (nil? rm) true)
          (Compiler$CF$CallV. (char (tc ty)) key rm tn (arg-nodes ctx args desc) (ptypes desc))
          (Compiler$CF$CallR. (char (tc ty)) rm tn (arg-nodes ctx args desc) (ptypes desc)))))))

(defn- ctor-meth [^Compiler$CF$Klass k desc]
  (or (.get (.-ctors k) desc) (fail (str "No constructor " desc " in " (.-name k)))))

(defn- implicit-fields
  "[[field-index node] ...]: the outer instance and captured values a new object of interpreted
  class cn gets before its constructor runs (javac's implicit constructor arguments)."
  [ctx cn outer-an]
  (let [k (klass cn)
        info (.-info k)
        d (a/decl cn)]
    (concat
      (when-let [i (get (:field-index info) "this$0")]
        (when outer-an [[i (node ctx outer-an)]]))
      (for [b (:captures info)]
        [(get (:capture-index info) (:id b)) (local-node ctx b)]))))

(defn- super-outer-fields
  "The outer instance of an anonymous class's interpreted superclass, as [[index node]]."
  [ctx cn an]
  (let [d (a/decl cn)
        sup (:super d)
        sk (klass sup)
        i (when sk (get (:field-index (.-info sk)) "this$0"))]
    (when i
      (cond
        (:super-outer an) [[i (node ctx (:super-outer an))]]
        (:super-outer-is-outer d) (when (:outer an) [[i (node ctx (:outer an))]])
        (:super-outer-path d) (when (:outer an)
                                [[i (Compiler$CF$OuterPath. (node ctx (:outer an))
                                                            (int-array (map this0-index (:super-outer-path d))))]])
        :else nil))))

(defmethod build-op :new [ctx an]
  (let [cn (:class an)
        m (:ctor an)
        k (klass cn)]
    (if k
      (let [xs (vec (concat (implicit-fields ctx cn (:outer an)) (super-outer-fields ctx cn an)))]
        (when (:enum-const an) (fail "The Go build's class forms do not support enums yet"))
        (Compiler$CF$NewI. k (ctor-meth k (:desc m)) (arg-nodes ctx (:args an) (:desc m))
                           (int-array (map first xs)) (nodes (map second xs))))
      (let [c (internal->class cn)
            outer? (:outer-instance? (env/info cn))
            [ps _] (t/parse-method-desc (:desc m))
            real (if outer? (cons (t/internal->desc (:outer (env/info cn))) ps) ps)
            ctor (try (doto (.getDeclaredConstructor c (into-array Class (map desc->class real)))
                        (.setAccessible true))
                      (catch Exception e (fail (str "No constructor " (:desc m) " of " cn))))]
        (Compiler$CF$NewR. ctor
                           (nodes (concat (when outer? [(node ctx (:outer an))])
                                          (map (fn [a ty] (coerce ctx a ty)) (:args an) ps)))
                           (char-array (map tc real)))))))

(defn- clj-const-value
  "The value of a Clojure constant field (const__N) of class owner, or ::none."
  [owner fname]
  (let [d (a/decl owner)
        f (when d (some (fn [[_ f]] (when (= fname (:name f)) f)) (:clj-consts @(:state d))))]
    (if-let [init (:init f)]
      (case (:kind init)
        :var (arbace.lang.RT/var (:ns init) (:name init))
        :read (binding [*read-eval* true] (arbace.lang.RT/readString (:text init))))
      ::none)))

(defmethod build-op :get-static [ctx an]
  (let [f (:field an)
        owner (:owner an)
        fname (:name f)
        ty (:desc f)
        cv (when (str/starts-with? fname "const__") (clj-const-value (or (:owner f) owner) fname))]
    (cond
      (and cv (not= cv ::none)) (konst ty cv)
      (= fname "$assertionsDisabled") (konst "Z" true)
      (klass owner) (let [[sk i] (or (find-static (klass owner) fname)
                                     (fail (str "No static field " fname " in " owner)))]
                      (Compiler$CF$GetStatic. (char (tc ty)) sk (int i)))
      :else (Compiler$CF$FieldR. (char (tc ty)) (or (find-field (internal->class owner) fname)
                                                    (fail (str "No field " owner "." fname)))
                                 nil nil))))

(defmethod build-op :set-static [ctx an]
  (let [f (:field an)
        owner (:owner an)
        fname (:name f)
        ty (:desc f)]
    (if (klass owner)
      (let [[sk i] (or (find-static (klass owner) fname) (fail (str "No static field " fname " in " owner)))]
        (Compiler$CF$SetStatic. (char (tc ty)) sk (int i) (coerce ctx (:val an) ty)))
      (Compiler$CF$FieldR. (char (tc ty)) (or (find-field (internal->class owner) fname)
                                              (fail (str "No field " owner "." fname)))
                           nil (coerce ctx (:val an) ty)))))

(defmethod build-op :get-field [ctx an]
  (let [f (:field an)
        owner (:owner an)
        ty (:desc f)
        tn (coerce ctx (:target an) (t/internal->desc owner))]
    (if-let [k (klass owner)]
      (Compiler$CF$GetField. (char (tc ty)) tn (int (or (field-index k (:name f))
                                                        (fail (str "No field " (:name f) " in " owner)))))
      (Compiler$CF$FieldR. (char (tc ty)) (or (find-field (internal->class owner) (:name f))
                                              (fail (str "No field " owner "." (:name f))))
                           tn nil))))

(defmethod build-op :set-field [ctx an]
  (let [f (:field an)
        owner (:owner an)
        ty (:desc f)
        tn (coerce ctx (:target an) (t/internal->desc owner))
        v (coerce ctx (:val an) ty)]
    (if-let [k (klass owner)]
      (Compiler$CF$SetField. (char (tc ty)) tn (int (or (field-index k (:name f))
                                                        (fail (str "No field " (:name f) " in " owner))))
                             v)
      (let [fd (or (find-field (internal->class owner) (:name f)) (fail (str "No field " owner "." (:name f))))]
        (Compiler$CF$FieldR. (char (tc ty)) fd tn v)))))

(defmethod build-op :var-deref [ctx an]
  (let [f (:field an)
        v (clj-const-value (:owner f) (:name f))]
    (if (instance? arbace.lang.Var v)
      (Compiler$CF$VarDeref. v)
      (fail (str "No var for " (:name f))))))

(defmethod build-op :var-invoke [ctx an]
  (let [f (:field an)
        v (clj-const-value (:owner f) (:name f))]
    (Compiler$CF$VarInvoke. v (nodes (map #(coerce ctx % "Ljava/lang/Object;") (:args an))))))

(defmethod build-op :fi-adapter [ctx an]
  (Compiler$CF$FiAdapter. (desc->class (:type an)) (node ctx (:expr an))))

;; ---------------------------------------------------------------------------------------------
;; arrays, strings

(defmethod build-op :new-array [ctx an]
  (let [d (:type an)
        dims (:dims an)
        comp (nth (iterate t/elem-type d) (count dims))]
    (Compiler$CF$NewArray. (desc->class comp) (nodes (map #(coerce ctx % "I") dims)))))

(defmethod build-op :array-init [ctx an]
  (let [et (t/elem-type (:type an))]
    (Compiler$CF$ArrayInit. (desc->class et) (char (tc et)) (nodes (map #(coerce ctx % et) (:elems an))))))

(defmethod build-op :aget [ctx an]
  (Compiler$CF$Aget. (char (tc (:type an))) (node ctx (:array an)) (coerce ctx (:index an) "I")))

(defmethod build-op :aset [ctx an]
  (let [et (:type an)]
    (Compiler$CF$Aset. (char (tc et)) (node ctx (:array an)) (coerce ctx (:index an) "I") (coerce ctx (:val an) et))))

(defmethod build-op :alength [ctx an] (Compiler$CF$Alength. (node ctx (:array an))))

(defmethod build-op :java-str [ctx an]
  (let [parts (:parts an)]
    (Compiler$CF$JavaStr. (into-array String (map (fn [p] (cond (:node p) nil
                                                                (contains? p :tag-const) (str (:tag-const p))
                                                                :else (:literal p)))
                                                  parts))
                          (nodes (map (fn [p] (when (:node p) (node ctx (:node p)))) parts))
                          (char-array (map (fn [p] (if (:node p) (tc (or (:type p) (:type (:node p)))) \L)) parts))
                          (boolean-array (map (fn [p] (boolean (:eager p))) parts)))))

(defmethod build-op :for-each [ctx an]
  (let [h (:hidden an)
        array? (boolean (:array an))
        arr (bind! ctx (if array? (:arr h) (:it h)))
        len (if array? (bind! ctx (:len h)) 0)
        i (if array? (bind! ctx (:i h)) 0)
        coll (node ctx (if array? (:coll an) (:iterator an)))
        b (:b an)
        tid (:id (:target an))
        elem (coerce ctx (:elem an) (:type b))
        slot (bind! ctx b)
        ctx2 (assoc-in ctx [:targets tid] {:brk (target-code tid false) :rec (target-code tid true)
                                           :slots [] :kinds [] :types [] :type :null})]
    ;; the iterator variant evaluates (:iterator an), the hidden it slot holds the iterator
    (Compiler$CF$ForEach. (char (tc (vt an))) coll array? (int arr) (int len) (int i) elem (int slot)
                          (char (kind (:type b))) (node ctx2 (:body an))
                          (int (target-code tid false)) (int (target-code tid true)))))

;; ---------------------------------------------------------------------------------------------
;; patterns, switch

(defn- accessor-node
  "A call of record accessor acc on the value in ref slot `slot`."
  [ctx acc slot]
  (let [owner (:owner acc)
        key (str (:name acc) (:desc acc))
        ty (subs (:desc acc) 2)
        target (Compiler$CF$Local. \L (int slot))]
    (if-let [k (klass owner)]
      (Compiler$CF$CallI. (char (tc ty)) (or (method-of k key) (fail (str "No accessor " key))) target (nodes []))
      (Compiler$CF$CallR. (char (tc ty)) (real-method owner (:name acc) (:desc acc)) target (nodes []) (char-array [])))))

(defn pattern
  "The Pat of pattern p."
  ^Compiler$CF$Pat [ctx p]
  (let [pt (Compiler$CF$Pat.)
        cd (:class p)]
    (set! (.-test pt) (boolean (:test p)))
    (when (t/class-desc? cd)
      (set! (.-c pt) (desc->class cd))
      (set! (.-k pt) (klass (t/desc->internal cd))))
    (when (t/array? cd) (set! (.-c pt) (desc->class cd)))
    (case (:kind p)
      :type (when-let [b (:b p)]
              (set! (.-slot pt) (int (bind! ctx b)))
              (set! (.-kind pt) (char (kind (:type b)))))
      :record (let [s (slot! ctx "Ljava/lang/Object;")]
                (set! (.-slot pt) (int s))
                (set! (.-accessors pt) (nodes (map #(accessor-node ctx (:accessor %) s) (:comps p))))
                (set! (.-comps pt) (into-array Compiler$CF$Pat (map #(pattern ctx (:pattern %)) (:comps p))))))
    pt))

(defmethod build-op :if-instance [ctx an]
  (let [ty (vt an)
        e (node ctx (:expr an))
        pt (pattern ctx (:pattern an))
        br (fn [b] (if (#{:none :null "V"} ty) (node ctx b) (coerce ctx b ty)))]
    (Compiler$CF$IfInstance. (char (tc ty)) e pt (br (:then an)) (br (:else an)))))

(defmethod build-op :switch [ctx an]
  (let [ty (vt an)
        expr? (not (#{:none :null "V"} ty))
        arm (fn [b] (if expr? (coerce ctx b ty) (node ctx b)))
        cases (:cases an)
        indexed (for [[ci c] (map-indexed vector cases) l (:labels c)] [ci l])
        kind (:kind an)
        sel (case kind
              :int (coerce ctx (:sel an) "I")
              (node ctx (:sel an)))
        keys (java.util.HashMap.)
        dflt-arm (or (some (fn [[ci l]] (when (:default l) ci)) indexed) -1)
        null-arm (or (some (fn [[ci l]] (when (:null l) ci)) indexed) -1)
        labels (vec (for [[ci l] indexed :when (not (:null l)) :when (not (:default l))] [ci l]))]
    (case kind
      :int (doseq [[ci l] labels]
             (let [v (:const l)] (.put keys (Long/valueOf (long (if (char? v) (int v) v))) (Integer/valueOf (int ci)))))
      :string (doseq [[ci l] labels] (.put keys (:const l) (Integer/valueOf (int ci))))
      :enum (doseq [[ci l] labels] (.put keys (str (:enum l)) (Integer/valueOf (int ci))))
      :pattern nil)
    ;; the patterns' bindings and the guards are built before the arms that use them
    (let [pats (when (= kind :pattern) (mapv (fn [[_ l]] (when (:pattern l) (pattern ctx (:pattern l)))) labels))
          guards (when (= kind :pattern) (mapv (fn [[ci _]] (when-let [g (:guard (nth cases ci))] (node ctx g))) labels))
          arms (mapv (fn [c] (arm (:body c))) cases)]
      (Compiler$CF$Switch. (char (tc ty)) (int (case kind :int 0 :string 1 :enum 2 :pattern 3)) sel
                           (nodes arms) (when-let [d (:default an)] (arm d)) keys (int null-arm) (int dflt-arm)
                           (into-array Compiler$CF$Pat (or pats []))
                           (object-array (map (fn [[_ l]] (cond (:enum l) (str (:enum l))
                                                                (contains? l :const) (let [v (:const l)] (if (char? v) (Integer/valueOf (int v)) v))
                                                                :else nil))
                                              labels))
                           (nodes (or guards []))
                           (int-array (map first labels))))))

;; ---------------------------------------------------------------------------------------------
;; classes: Klass objects, field layouts, methods

(defn- has? [flags f] (not (zero? (bit-and (or flags 0) f))))
(def ^:private ACC_STATIC 0x0008)
(def ^:private ACC_PRIVATE 0x0002)
(def ^:private ACC_ABSTRACT 0x0400)
(def ^:private ACC_INTERFACE 0x0200)

(defn host ^arbace.lang.Compiler$CF$Host [] Compiler$CF$Rt/HOST)

(declare build-method! lambda-klass)

(defn- meth-of
  "A Meth of klass k for method entry m of declaration d."
  [^Compiler$CF$Klass k d m]
  (let [mt (Compiler$CF$Meth. k (:name m) (:desc m) (int (:flags m)))
        [ps r] (t/parse-method-desc (:desc m))]
    (set! (.-ret mt) (char (tc (if (= "<init>" (:name m)) "V" r))))
    (set! (.-ptypes mt) (char-array (map tc ps)))
    (set! (.-builder mt) (fn [mt] (build-method! k d m mt)))
    mt))

(defn- zero-of [desc] (Compiler$CF$Rt/zero (char (tc desc))))

(defn- make-klass
  "The Klass of class n of the compilation (members still to come)."
  [n]
  (let [d (a/decl n)
        k (Compiler$CF$Klass. (str/replace n "/" "."))]
    (set! (.-iname k) n)
    (set! (.-flags k) (int (:flags d)))
    (set! (.-iface k) (has? (:flags d) ACC_INTERFACE))
    (set! (.-fnClass k) (boolean (and (:clojure-fn (:meta d))
                                      (#{(t/lang-class "AFunction") (t/lang-class "RestFn")} (:super d)))))
    k))

(defn- layout!
  "Superclass and interfaces, the field layout, statics and methods of k (class n)."
  [^Compiler$CF$Klass k n]
  (let [d (a/decl n)
        st @(:state d)
        sup (:super d)
        sk (klass sup)]
    (when (and sup (not sk) (not (.-iface k)) (not (.-fnClass k))
               (not= sup "java/lang/Object") (not= sup "java/lang/Record"))
      (fail (str "The Go build's class forms cannot extend " (str/replace sup "/" ".") " yet")))
    (set! (.-sup k) sk)
    (set! (.-superClass k) (when (and sup (not sk)) (internal->class sup)))
    (set! (.-ifaces k) (into-array Compiler$CF$Klass (keep klass (:interfaces d))))
    (set! (.-interfaces k) (into-array Class (map internal->class (remove klass (:interfaces d)))))
    (let [base (if sk (.-nfields sk) 0)
          own (vec (remove #(has? (:flags %) ACC_STATIC) (:fields d)))
          this0? (:outer-instance? d)
          caps (when (#{:local :anon} (:nesting d)) (vec (:captures st)))
          names (concat (map :name own) (when this0? ["this$0"]) (map #(str "val$" (:sym %)) caps))
          descs (concat (map :desc own) (when this0? [(t/internal->desc (:outer d))]) (map :type caps))
          n-own (count names)
          defaults (object-array (concat (when sk (seq (.-defaults sk))) (map zero-of descs)))
          statics (vec (filter #(has? (:flags %) ACC_STATIC) (:fields d)))]
      (set! (.-nfields k) (int (+ base n-own)))
      (set! (.-defaults k) defaults)
      (set! (.-statics k) (object-array (map (fn [f]
                                               (let [c (when (:const f) (env/const-value f))]
                                                 (if (some? c)
                                                   (if (t/prim? (:desc f))
                                                     (Compiler$CF$Rt/box (char (tc (:desc f)))
                                                                         (long (cond (char? c) (int c) (boolean? c) (if c 1 0)
                                                                                     (instance? Number c) (if (#{"F" "D"} (:desc f)) 0 (long c))
                                                                                     :else 0)))
                                                     c)
                                                   (zero-of (:desc f)))))
                                             statics)))
      ;; float and double constants
      (doseq [[i f] (map-indexed vector statics)
              :let [c (when (:const f) (env/const-value f))]
              :when (and (some? c) (#{"F" "D"} (:desc f)))]
        (aset ^objects (.-statics k) i (Compiler$CF$Rt/boxD (char (tc (:desc f))) (double c))))
      (set! (.-info k)
            {:decl n
             :fields (vec (map (fn [nm ds] {:name nm :desc ds}) names descs))
             :own-fields own
             :field-index (into {} (map-indexed (fn [i nm] [nm (+ base i)]) names))
             :captures caps
             :capture-index (into {} (map-indexed (fn [i b] [(:id b) (+ base (count own) (if this0? 1 0) i)]) caps))
             :statics statics
             :static-index (into {} (map-indexed (fn [i f] [(:name f) i]) statics))})
      ;; methods and constructors
      (doseq [m (:methods d)]
        (let [mt (meth-of k d m)]
          (if (= "<init>" (:name m))
            (.put (.-ctors k) (:desc m) mt)
            (.put (.-methods k) (str (:name m) (:desc m)) mt)))))))

(defn- vtable!
  "The virtual methods of k: its superclass's, its interfaces' defaults (when nothing else
  gives them), then its own non-static, non-private methods."
  [^Compiler$CF$Klass k]
  (let [vt (.-vtable k)]
    (when-let [sk (.-sup k)] (.putAll vt (.-vtable sk)))
    (doseq [^Compiler$CF$Klass i (.-ifaces k)
            [key ^Compiler$CF$Meth m] (.-vtable i)]
      (when-not (and (.containsKey vt key)
                     (not (has? (.-flags ^Compiler$CF$Meth (.get vt key)) ACC_ABSTRACT)))
        (.put vt key m)))
    (doseq [[key ^Compiler$CF$Meth m] (.-methods k)]
      (when-not (has? (.-flags m) (bit-or ACC_STATIC ACC_PRIVATE))
        (.put vt key m)))))

;; ---------------------------------------------------------------------------------------------
;; method bodies

(defn- ctx-for
  "The building context of a method of class n with receiver binding recv (nil: static)."
  [n ret recv]
  (let [ctx (new-ctx n ret)]
    (if recv
      (let [i (bind! ctx recv)] (assoc ctx :self i))
      ctx)))

(defn- receiver-ctx [n ret]
  (let [ctx (new-ctx n ret)
        i (slot! ctx "Ljava/lang/Object;")]
    (assoc ctx :self i)))

(defn- params! [ctx ^Compiler$CF$Meth mt bs]
  (let [slots (mapv #(bind! ctx %) bs)]
    (set! (.-pslots mt) (int-array slots))
    (set! (.-ptypes mt) (char-array (map #(tc (:type %)) bs)))))

(defn- finish! [ctx ^Compiler$CF$Meth mt body]
  (set! (.-self mt) (int (:self ctx)))
  (set! (.-nr mt) (int @(:nr ctx)))
  (set! (.-np mt) (int @(:np ctx)))
  (set! (.-body mt) body))

(defn- record-op-node [^Compiler$CF$Klass k d m self]
  (let [comps (:components d)]
    (Compiler$CF$RecordOp. (char (tc (second (t/parse-method-desc (:desc m)))))
                           (case (:name m) "equals" 0 "hashCode" 1 2)
                           k (int self)
                           (into-array String (map :name comps))
                           (int-array (map #(field-index k (:name %)) comps))
                           (char-array (map #(tc (:desc %)) comps)))))

(defn- derived-body
  "The body of derived method m (bridges, record members)."
  [ctx ^Compiler$CF$Klass k d m]
  (let [self (:self ctx)
        [bps br] (t/parse-method-desc (:desc m))]
    (case (:derived m)
      :bridge
      (let [target (:bridge-of m)
            [tps tr] (t/parse-method-desc (:desc target))
            slots (vec (for [p bps] (slot! ctx p)))
            args (map (fn [s bp tp] (value-convert (Compiler$CF$Local. (char (tc bp)) (int s)) bp tp)) slots bps tps)
            this-node (Compiler$CF$Local. \L (int self))
            tk (klass (:owner target))
            key (str (:name m) (:desc target))
            call (if (:special m)
                   (Compiler$CF$CallI. (char (tc tr)) (or (method-of (or tk (.-sup k)) key) (fail (str "No bridge target " key)))
                                       this-node (nodes args))
                   (Compiler$CF$CallV. (char (tc tr)) key (real-method (:owner target) (:name m) (:desc target))
                                       this-node (nodes args) (char-array (map tc tps))))]
        [slots (value-convert call tr br)])
      :record-accessor
      (let [c (:component m)]
        [[] (Compiler$CF$GetField. (char (tc (:desc c))) (Compiler$CF$Local. \L (int self))
                                   (int (field-index k (:name c))))])
      :record-object-method
      (let [slots (vec (for [p bps] (slot! ctx p)))]
        [slots (record-op-node k d m self)])
      (fail (str "The Go build's class forms do not support " (:derived m) " yet")))))

(defn- init-nodes
  "The instance initializers of class n (field initializers and initializer blocks), in ctx."
  [ctx n]
  (mapv #(node ctx %) (:init @(:state (a/decl n)))))

(defn- trivial-super?
  "Is cn a superclass whose constructor the interpreter does not run: Object, Record, and the
  AFunction or RestFn of a Clojure fn's class (CF$FnObj's)."
  [^Compiler$CF$Klass k cn]
  (or (#{"java/lang/Object" "java/lang/Record"} cn)
      (and (.-fnClass k) (#{(t/lang-class "AFunction") (t/lang-class "RestFn")} cn))))

(defn- ctor-call-node
  "The node of a constructor's call (its super or this call), and after a super call the
  instance initializers."
  [ctx ^Compiler$CF$Klass k d ab call]
  (let [self (int (:self ctx))
        n (:name d)
        inits (fn [] (when (:calls-super ab) (init-nodes ctx n)))]
    (cond
      (nil? call) (inits)
      (:enum-args call) (fail "The Go build's class forms do not support enums yet")
      (:anon-args call)
      (let [sk (.-sup k)
            args (map #(local-node ctx %) (:params ab))]
        (concat
          (when sk
            [(Compiler$CF$CtorCall. (ctor-meth sk (:desc (:ctor call))) self (nodes args) (int-array []) (nodes []))])
          (when (and (not sk) (not (trivial-super? k (:class call))))
            (fail (str "The Go build's class forms cannot extend " (str/replace (:class call) "/" ".") " yet")))
          (inits)))
      (= :this (:kind call))
      [(Compiler$CF$CtorCall. (ctor-meth k (:desc (:ctor call))) self (arg-nodes ctx (:args call) (:desc (:ctor call)))
                              (int-array []) (nodes []))]
      :else
      (let [cn (:class call)
            sk (klass cn)
            xs (when (and sk (:outer call))
                 (when-let [i (get (:field-index (.-info sk)) "this$0")]
                   [[i (node ctx (:outer call))]]))]
        (concat
          (cond
            sk [(Compiler$CF$CtorCall. (ctor-meth sk (:desc (:ctor call))) self
                                       (arg-nodes ctx (:args call) (:desc (:ctor call)))
                                       (int-array (map first xs)) (nodes (map second xs)))]
            (trivial-super? k cn) []
            :else (fail (str "The Go build's class forms cannot extend " (str/replace cn "/" ".") " yet")))
          (inits))))))

(defmethod build-op :ctor-call [ctx an]
  (if-let [site (:ctor-site ctx)]
    (let [ns (site ctx an)] (Compiler$CF$Do. \V (nodes ns) (konst "V" nil)))
    (fail "Constructor call outside the constructor prologue")))

(defn build-method!
  "Builds Meth mt (method entry m of class n, declaration d): its frame layout and body."
  [^Compiler$CF$Klass k d m ^Compiler$CF$Meth mt]
  (binding [env/*compile-set* (atom (:compile-set (.-info k)))
            *klasses* (:klasses (.-info k))]
    (let [n (:name d)
          st @(:state d)
          ab (get (:bodies st) [(:name m) (:desc m)])
          ctor? (= "<init>" (:name m))
          static? (has? (:flags m) ACC_STATIC)]
      (cond
        ctor?
        (let [ctx (if (:recv ab) (ctx-for n "V" (:recv ab)) (receiver-ctx n "V"))
              _ (params! ctx mt (:params ab))
              call-node (fn [ctx call] (ctor-call-node ctx k d ab call))
              ctx (assoc ctx :ctor-site call-node)
              body (if (:nested-call ab)
                     [(node ctx (:body ab))]
                     (concat (map #(node ctx %) (:prologue ab))
                             (call-node ctx (:call ab))
                             (when-let [b (:body ab)] [(node ctx b)])))
              ;; a record's canonical (or compact) constructor assigns the fields at its end
              assigns (when (and (= :record (:kind d)) (or (:compact m) (= :record-canonical (:derived m))))
                        (for [[b c] (map vector (:params ab) (:components d))]
                          (Compiler$CF$SetField. (char (tc (:desc c))) (Compiler$CF$Local. \L (int (:self ctx)))
                                                 (int (field-index k (:name c))) (local-node ctx b))))]
          (finish! ctx mt (Compiler$CF$Do. \V (nodes (concat body assigns)) (konst "V" nil))))

        (:derived m)
        (let [ctx (if static? (new-ctx n (:ret m)) (receiver-ctx n (:ret m)))
              [slots body] (derived-body ctx k d m)]
          (set! (.-pslots mt) (int-array slots))
          (finish! ctx mt body))

        ab
        (let [ctx (if (:recv ab) (ctx-for n (:ret m) (:recv ab)) (new-ctx n (:ret m)))
              _ (params! ctx mt (:params ab))
              body (node ctx (:body ab))]
          (finish! ctx mt body))

        :else
        (fail (str "Method " (:name m) (:desc m) " of " n " has no body"))))))

;; ---------------------------------------------------------------------------------------------
;; defining a compilation

(defn- declare-siblings!
  "Enters sibling class forms (the other class forms of a top-level do, SPEC §9.2) as
  declarations only (arbace.classes.compiler/declare-siblings!)."
  [ns siblings]
  (let [from (count @(:order a/*unit*))]
    (doseq [f siblings]
      (let [parsed (p/parse-class {:ns ns :nesting :top} f)]
        (when-not (a/decl (a/top-name ns parsed))
          (a/declare-class! {:nesting :top} parsed))))
    (doseq [c (subvec @(:order a/*unit*) from)]
      (a/update-decl! c assoc :declared-only true))))

(defn analyze-forms
  "Analyzes class forms (each the rest of a defclass form) in namespace ns as one compilation,
  with sibling class forms entered as declarations only (arbace.classes.compiler/compile-forms
  without emitting). Must run inside the bindings of with-unit. Returns the top-level names."
  [ns forms siblings]
  (let [names (vec (for [f forms]
                     (a/declare-class! {:nesting :top} (p/parse-class {:ns ns :nesting :top} f))))]
    (declare-siblings! ns siblings)
    (a/process-classes! 0)
    names))

;; ---------------------------------------------------------------------------------------------
;; lambdas and method references: a class per site (named as the JVM names its hidden classes,
;; Outer$$Lambda/N), implementing the functional interface (and the markers), whose method is
;; the lambda's body; its objects hold the captured values

(defonce ^:private lambda-count (atom 0))

(defn- new-lambda-klass
  "A Klass for a lambda of class cls implementing fi and markers, with nf fields."
  [cls fi markers nf]
  (let [k (Compiler$CF$Klass. (str (str/replace cls "/" ".") "$$Lambda/" (swap! lambda-count inc)))
        is (cons fi markers)]
    (set! (.-iname k) (str/replace (.-name k) "." "/"))
    (set! (.-flags k) (int 0x1010))
    (set! (.-ifaces k) (into-array Compiler$CF$Klass (keep klass is)))
    (set! (.-interfaces k) (into-array Class (map internal->class (remove klass is))))
    (set! (.-superClass k) Object)
    (set! (.-nfields k) (int nf))
    (set! (.-defaults k) (object-array nf))
    (set! (.-statics k) (object-array 0))
    (set! (.-info k) {:fields [] :field-index {} :statics [] :static-index {} :lambda true})
    k))

(declare publish!)

(defn- lambda-done!
  "k (a lambda's class) with its method mt: defined by the host, the method published."
  [^Compiler$CF$Klass k ^Compiler$CF$Meth mt]
  (set! (.-self mt) (int -1))
  (.put (.-methods k) (.key mt) mt)
  (.put (.-vtable k) (.key mt) mt)
  (set! (.-cls k) (.define (host) k))
  (publish! k)
  k)

(defmethod build-op :lambda [ctx an]
  (let [b (:boundary an)
        inst? (boolean @(:uses-this b))
        caps @(:captures b)
        sam (:sam an)
        k (new-lambda-klass (:class ctx) (:fi an) (:markers an) (+ (count caps) (if inst? 1 0)))
        lctx (new-ctx (:class ctx) (:ret an))
        self (when inst? (slot! lctx "Ljava/lang/Object;"))
        lctx (assoc lctx :self (if inst? self -1) :klass (:klass ctx))
        cslots (mapv #(bind! lctx %) caps)
        mt (Compiler$CF$Meth. k (:name sam) (:desc sam) (int 1))
        _ (params! lctx mt (:params an))
        body (node lctx (:body an))]
    (set! (.-ret mt) (char (tc (:ret an))))
    (set! (.-cslots mt) (int-array (concat (when inst? [self]) cslots)))
    (set! (.-ctypes mt) (char-array (concat (when inst? [\L]) (map #(tc (:type %)) caps))))
    (finish! lctx mt body)
    (lambda-done! k mt)
    (Compiler$CF$MakeLambda. k (nodes (concat (when inst? [(Compiler$CF$Local. \L (int (:self ctx)))])
                                              (map #(local-node ctx %) caps))))))

(defmethod build-op :method-ref [ctx an]
  (let [{:keys [kind owner name desc]} an
        sam (:sam an)
        [ips iret] (t/parse-method-desc (:inst-desc an))
        [mps mret] (t/parse-method-desc desc)
        bound? (= kind :bound)
        k (new-lambda-klass (:class ctx) (:fi an) (:markers an) (if bound? 1 0))
        lctx (new-ctx (:class ctx) iret)
        recv (when bound? (slot! lctx "Ljava/lang/Object;"))
        pslots (mapv #(slot! lctx %) ips)
        params (map (fn [s p] (Compiler$CF$Local. (char (tc p)) (int s))) pslots ips)
        mt (Compiler$CF$Meth. k (:name sam) (:desc sam) (int 1))
        ok (klass owner)
        key (str name desc)
        conv-args (fn [ps tps] (nodes (map (fn [n p tp] (value-convert n p tp)) ps (take (count ps) ips) tps)))
        call (case kind
               :static (if ok
                         (Compiler$CF$CallI. (char (tc mret)) (method-of ok key) nil (conv-args params mps))
                         (Compiler$CF$CallR. (char (tc mret)) (real-method owner name desc) nil (conv-args params mps) (ptypes desc)))
               :bound (Compiler$CF$CallV. (char (tc mret)) key (real-method owner name desc)
                                          (Compiler$CF$Local. \L (int recv)) (conv-args params mps) (ptypes desc))
               :unbound (let [target (value-convert (first params) (first ips) (t/internal->desc owner))]
                          (Compiler$CF$CallV. (char (tc mret)) key (real-method owner name desc) target
                                              (nodes (map (fn [n p tp] (value-convert n p tp)) (rest params) (rest ips) mps))
                                              (ptypes desc)))
               :new (if ok
                      (Compiler$CF$NewI. ok (ctor-meth ok desc) (conv-args params mps) (int-array []) (nodes []))
                      (let [ctor (doto (.getDeclaredConstructor (internal->class owner) (params-of desc)) (.setAccessible true))]
                        (Compiler$CF$NewR. ctor (conv-args params mps) (ptypes desc)))))
        rt (if (= kind :new) (t/internal->desc owner) mret)
        body (if (= iret "V") call (value-convert call rt iret))]
    (when (and (not= kind :unbound) (not= (count ips) (count mps)))
      (fail "The Go build's class forms do not support variable arity method references yet"))
    (set! (.-pslots mt) (int-array pslots))
    (set! (.-ptypes mt) (char-array (map tc ips)))
    (set! (.-ret mt) (char (tc iret)))
    (when bound?
      (set! (.-cslots mt) (int-array [recv]))
      (set! (.-ctypes mt) (char-array [\L])))
    (finish! lctx mt body)
    (set! (.-self mt) (int -1))
    (lambda-done! k mt)
    (Compiler$CF$MakeLambda. k (nodes (when bound?
                                        [(let [r (node ctx (:recv an))]
                                           (if (:null-check an) (Compiler$CF$NullChecked. r) r))])))))

;; ---------------------------------------------------------------------------------------------
;; publishing: the host learns the members (Go: Dyn slots and member tables, for translated
;; code and reflection)

(defn- meth-classes [^Compiler$CF$Meth mt]
  (let [[ps r] (t/parse-method-desc (.-desc mt))]
    (set! (.-params mt) (into-array Class (map desc->class ps)))
    (set! (.-retClass mt) (desc->class (if (= "<init>" (.-name mt)) "V" r)))))

(defn publish!
  "Hands k's members to the host: its virtual methods (its own and those it inherits from
  interpreted classes), its static and private methods, constructors, fields."
  [^Compiler$CF$Klass k]
  (let [h (host)
        seen (java.util.HashSet.)]
    (doseq [[key ^Compiler$CF$Meth m] (concat (.-vtable k) (.-methods k))
            :when (.add seen key)
            :when (not (has? (.-flags m) ACC_ABSTRACT))
            :when (not= key "doInvoke")]
      (meth-classes m)
      (.method h k m))
    (doseq [[_ ^Compiler$CF$Meth m] (.-ctors k)]
      (meth-classes m)
      (.ctor h k m))
    (let [info (.-info k)]
      (doseq [f (:own-fields info)]
        (.addField h k (:name f) (desc->class (:desc f)) (int (:flags f)) (int (field-index k (:name f))) false))
      (doseq [[i f] (map-indexed vector (:statics info))]
        (.addField h k (:name f) (desc->class (:desc f)) (int (:flags f)) (int i) true)))))

;; ---------------------------------------------------------------------------------------------
;; a compilation

(defn- clinit!
  "The static initializer of k (class n)."
  [^Compiler$CF$Klass k n]
  (let [st @(:state (a/decl n))
        nodes* (:clinit st)]
    (when (seq nodes*)
      (let [ctx (new-ctx n "V")
            body (Compiler$CF$Do. \V (nodes (map #(node ctx %) nodes*)) (konst "V" nil))]
        (set! (.-clinit k) body)
        (set! (.-clinitR k) (int @(:nr ctx)))
        (set! (.-clinitP k) (int @(:np ctx)))))))

(defn define!
  "Compiles class forms (each the rest of a defclass form) in namespace ns as one compilation,
  the sibling class forms entered as declarations, into interpreted classes. Returns [[dotted-name
  Class] ...] of the top-level classes (the Class nil on the JVM's test host)."
  [ns forms siblings]
  (binding [env/*compile-set* (atom {})
            a/*unit* {:order (atom []) :counters (atom {}) :switch-maps (atom {})
                      :switch-holders (atom {}) :source-tried (atom #{})
                      :assert-holders (atom {}) :holder-first (atom {})}]
    (with-redefs [a/accessorize (fn [_ node] node)]
      (let [names (analyze-forms ns forms siblings)
            order (vec (remove #(or (:declared-only (a/decl %)) (:switch-holder (a/decl %))) @(:order a/*unit*)))
            sorted (#'a/supertypes-first order)
            ks (into {} (for [n order] [n (make-klass n)]))]
        (binding [*klasses* ks]
          (doseq [n sorted] (layout! (ks n) n))
          (doseq [n sorted]
            (let [^Compiler$CF$Klass k (ks n)]
              (set! (.-cls k) (.define (host) k))
              (set! (.-info k) (assoc (.-info k) :compile-set @env/*compile-set* :klasses ks))))
          (doseq [n sorted] (vtable! (ks n)))
          ;; fn classes: their variadic arity by name (CF$FnObj)
          (doseq [n order
                  :let [^Compiler$CF$Klass k (ks n)]
                  :when (.-fnClass k)
                  [key m] (.-vtable k)
                  :when (str/starts-with? key "doInvoke(")]
            (.put (.-vtable k) "doInvoke" m))
          ;; every body, as the JVM compiles them all
          (doseq [n order
                  :let [^Compiler$CF$Klass k (ks n)]]
            (doseq [[_ ^Compiler$CF$Meth m] (concat (.-methods k) (.-ctors k))
                    :when (not (has? (.-flags m) ACC_ABSTRACT))]
              (.ensure m))
            (clinit! k n))
          (doseq [n sorted] (publish! (ks n)))
          (doseq [n order
                  :let [^Compiler$CF$Klass k (ks n)]]
            (when (.-cls k)
              (swap! env/defined assoc n (assoc (a/decl n) :class (.-cls k) :loader nil))))
          (swap! registry merge ks)
          (mapv (fn [n] [(str/replace n "/" ".") (.-cls ^Compiler$CF$Klass (ks n))]) names))))))

(defn compile-and-load!
  "arbace.classes.compiler/compile-and-load! of the Go build (arbace.classes.native's
  compiler): the classes interpreted."
  [ns forms & {:keys [siblings]}]
  (define! ns forms siblings))

;; the Go build's host (Compiler$CFGo), unless one is set (the JVM's tests set the test host)
(when (nil? Compiler$CF$Rt/HOST)
  (when-let [c (try (Class/forName "arbace.lang.Compiler$CFGo$GoHost") (catch Throwable _ nil))]
    (Compiler$CF$Rt/setHost (.newInstance ^Class c))))
