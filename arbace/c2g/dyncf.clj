(ns arbace.c2g.dyncf
  "The natives of Compiler$CFGo (arbace/lang/go/ClassForms.clj), the Go build's host of the class
  forms at the REPL (doc/go/CLASSFORMS-REPL.md), in arbace/lang's c2g_cf.go: classes made at run
  time for interpreted classes, over Dyn (C2G-SPEC §5.12, arbace.c2g.dyn): a class extending
  Object or another interpreted class, implementing interfaces of the world (Dyn's slots) and
  interfaces made at run time (the class's methods by key); an interface; a Clojure fn's class
  (objects CF$FnObj). Member tables carry the classes' methods, constructors and fields, with
  their modifiers, for reflection."
  (:require [arbace.string :as str]
            [arbace.walk :as walk]
            [arbace.classes.types :as t]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.code :as c]
            [arbace.c2g.dyn :as dyn]
            [arbace.c2g.jrt :as jrt]))

(def api-class "arbace/lang/Compiler$CFGo")

(defn enabled?
  "Is the class forms' host in this world (translated)?"
  []
  (m/translated? api-class))

(defn- tag [sym type] (with-meta sym {:tag type}))

(defn- native-name [mname desc]
  (symbol (str (m/go-name api-class) "_" (nm/method-base mname desc) "_native")))


;; ---------------------------------------------------------------------------------------------
;; superclasses of the world (arbace.c2g.dyn/sub-supers): an interpreted class extending C (or
;; extending an interpreted class that does) has DynSub_C objects. For each C: the allocation
;; (C's static initialization, then the object, without a constructor), C's constructors on an
;; allocated object (the super call of the interpreted constructor), and C's implementations of
;; its virtual methods (invokespecial: super.m()), by descriptor and key, from objects.

(defn- sub-name [n] (str "DynSub_" (m/go-name n)))

(defn- from-object
  "The Java object x (an interpreter's argument) as the Go value of descriptor d."
  [d x]
  (cond (t/prim? d) (list 'assert (m/go-type :lang d)
                          (list 'dynUnbox (symbol (str "jrt/Prim_" (t/prim-desc->name d))) x))
        (= d "Ljava/lang/Object;") x
        :else (list (list 'inst 'jrt/As (m/go-type :lang d)) x)))

(defn- sub-forms
  "dynCfNew_G, dynCfCtors_G, dynCfSupers_G of class n."
  [n]
  (let [g (sub-name n)
        recv (list '* (symbol g))]
    (binding [c/*f* {:pkg :lang}]
      [(list 'go/func (symbol (str "dynCfNew_" g)) (with-meta [(tag 'dc '(* DynClass)) (tag 'f '(slice any))] {:tag 'any})
             (if (or (m/hand-written? n) (m/trivial-init? n))
               '(do)
               (list (m/class-sym :lang n "_Init")))
             (list 'let ['t (list 'addr (list 'lit (symbol g) :D 'dc :F 'f))]
                   ;; the header's dynamic flag (jrt.MarkDynamic: the nominal checks)
                   '(.MarkDynamic t)
                   't))
       (list 'go/var (tag (symbol (str "dynCfCtors_" g)) '(map string (func [any (slice any)])))
             (apply list 'lit '(map string (func [any (slice any)]))
                    (for [mm (if (m/hand-written? n) (m/methods-of n) (:methods (a/decl n)))
                          :when (and (= "<init>" (:name mm)) (not (m/private? mm)) (m/mdesc-in-world? (:desc mm)))
                          :when (or (not (m/hand-written? n))
                                    (contains? (jrt/struct-methods (:jrt m/*w*) (m/go-name n)) (nm/ctor-base (:desc mm))))
                          :let [real (if (m/hand-written? n) (:desc mm) (e/ctor-real-desc n mm))
                                [ps _] (t/parse-method-desc (:desc mm))
                                [rps _] (t/parse-method-desc real)]
                          :when (= (count ps) (count rps))]
                      [(:desc mm)
                       (list 'fn [(tag 'o 'any) (tag 'args '(slice any))]
                             (list 'let ['t (list 'assert recv 'o)]
                                   (apply list (symbol (str "." (nm/ctor-base real)))
                                          (list (symbol (str ".-" (m/go-name n))) 't) 't
                                          (map-indexed (fn [i p] (from-object p (list 'aget 'args i))) ps))))])))
       (list 'go/var (tag (symbol (str "dynCfSupers_" g)) '(map string (func [any (slice any)] [any])))
             (apply list 'lit '(map string (func [any (slice any)] [any]))
                    (for [[[name desc :as k] _] (sort-by first (m/vmethods n))
                          :when (m/mdesc-in-world? desc)
                          :let [impl (m/impl-of n k)
                                base (nm/method-base name desc)
                                [ps r] (t/parse-method-desc desc)
                                pn (map-indexed (fn [i p] (from-object p (list 'aget 'args i))) ps)
                                call (case (:kind impl)
                                       (:class :jrt-impl) (apply list (symbol (str ".Impl_" base)) 't 't pn)
                                       :default (apply list (m/class-sym :lang (:owner impl) (str "_" base)) 't pn)
                                       :promoted (apply list (symbol (str "." base)) (list (symbol (str ".-" (m/go-name n))) 't) pn)
                                       nil)]
                          :when call]
                      [(str name desc)
                       (list 'fn (with-meta [(tag 'o 'any) (tag 'args '(slice any))] {:tag 'any})
                             (list 'let ['t (list 'assert recv 'o)]
                                   (if (= "V" r)
                                     (list 'do call '(return nil))
                                     (list 'return (list 'jrt/Box call)))))])))])))

(defn subs-forms
  "The forms of the superclasses of the world, and cfSubFor (nil when c has none)."
  [T]
  (let [cs (dyn/proxy-classes T)]
    (concat
      (mapcat sub-forms cs)
      [(list 'go/func 'cfSubFor
             "cfSubFor: the allocation, constructors and implementations of superclass c of the world
(nil: c has no DynSub type).\n"
             [(tag 'c '(* jrt/Class))]
             :results '[(func [(* DynClass) (slice any)] [any]) (map string (func [any (slice any)]))
                        (map string (func [any (slice any)] [any]))]
             (apply list 'switch 'c
                    (for [p cs :let [g (sub-name p)]]
                      (list 'case [(m/class-sym :lang p "_class")]
                            (list 'return (symbol (str "dynCfNew_" g)) (symbol (str "dynCfCtors_" g))
                                  (symbol (str "dynCfSupers_" g))))))
             '(return nil nil nil))])))

(defn forms
  "The forms of c2g_cf.go (package arbace/lang)."
  [T]
  (concat
  (subs-forms T)
  [(list 'c2g/comment "---- the class forms at the REPL: Compiler$CFGo's natives (doc/go/CLASSFORMS-REPL.md)")
   '(go/func cfIsInstance
      "cfIsInstance: whether x's class is c or a subclass of c (an interpreted class's objects: Dyn,
or CF$FnObj, whose getClass is their class).\n"
      ^bool [^{:tag (* jrt/Class)} c ^any x]
      (when (== x nil) (return false))
      (let [(values _ ok) (assert jrt/Object_I x)]
        (when (not ok) (return false)))
      (let [k (jrt/GetClass x)]
        (while (!= k nil)
          (when (== k c) (return true))
          (set! k (.-Super (.Info k)))))
      false)
   (list 'go/func (native-name "defineClass" "(Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Class;IILjava/lang/Object;)Ljava/lang/Class;")
         (with-meta [(tag 'name '(* jrt/String)) (tag 'super '(* jrt/Class)) (tag 'interfaces '(* jrt/RefArray))
                     (tag 'flags 'int32) (tag 'kind 'int32) (tag 'klass 'any)]
           {:tag '(* jrt/Class)})
         '(let [ifs (dynClassList interfaces)]
            (when (== kind 1)
              (let [info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name))
                                    :Modifiers (bit-or flags jrt/AccInterface jrt/AccAbstract)
                                    :Kind jrt/KindInterface :Interfaces ifs
                                    :Go "arbace/lang.Dyn (class form interface)"))
                    c (jrt/DefineDynamic info)]
                (set! (.-IsInstance info)
                      (fn ^bool [^any x]
                        (let [(values d ok) (assert dynObject x)]
                          (and ok (.DynImplements d c)))))
                (return c)))
            (when (== super nil) (set! super jrt/Object_class))
            (when (== kind 2)
              (let [info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers flags
                                    :Kind jrt/KindClass :Super super :Interfaces ifs
                                    :Go "arbace/lang.Compiler_CF_FnObj (class form fn)"))
                    c (jrt/DefineDynamic info)]
                (set! (.-IsInstance info) (fn ^bool [^any x] (cfIsInstance c x)))
                (return c)))
            ;; the nearest superclass of the world: Object (Dyn) or one with a DynSub type
            (let [base super]
              (while (cfInterpreted base) (set! base (.-Super (.Info base))))
              (let [(values nw _ _) (cfSubFor base)
                    (values bslots _ bown) (dynSubFor base)]
                (when (and (!= base jrt/Object_class) (== nw nil))
                  (panic (jrt/Thrown (jrt/UnsupportedOperationException_New_String
                                       (jrt/Str (+ "a class extending " (.-Name (.Info base)) " is not in the Go build"))))))
            (let [slots bslots
                  info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers flags
                                  :Kind jrt/KindClass :Super super :Interfaces ifs
                                  :Go "arbace/lang.Dyn (class form)"))
                  dc (addr (lit DynClass :Slots (make (slice IFn) (len slots)) :SlotMap slots :Own bown
                                :Ifaces (make (map (* jrt/Class) bool)) :ByKey (make (map string IFn))
                                :CF klass))]
              (when (== nw nil)
                (set! (.-SlotMap dc) dynSlots)
                (set! (.-Slots dc) (make (slice IFn) (len dynSlots))))
              (range [_ i ifs] (dynAddInterface dc i))
              ;; the interfaces of the superclasses
              (let [s super]
                (while (!= s nil)
                  (range [_ i (.-Interfaces (.Info s))] (dynAddInterface dc i))
                  (set! s (.-Super (.Info s)))))
              (let [c (jrt/DefineDynamic info)]
                (set! (.-Cls dc) c)
                (set! (.-IsInstance info) (fn ^bool [^any x] (cfIsInstance c x)))
                (.Lock dynMu)
                (aset dynClasses c dc)
                (when (!= nw nil) (aset cfNews c nw))
                (.Unlock dynMu)
                c))))))
   '(go/var ^{:tag (map (* jrt/Class) (func [(* DynClass) (slice any)] [any]))
              :doc "cfNews: the allocation of the interpreted classes whose objects are DynSub_C (under dynMu).\n"}
      cfNews (make (map (* jrt/Class) (func [(* DynClass) (slice any)] [any]))))
   '(go/func cfInterpreted "cfInterpreted: whether c is an interpreted class (made by Compiler$CFGo).\n"
      ^bool [^{:tag (* jrt/Class)} c]
      (.Lock dynMu)
      (let [(values dc ok) (aget dynClasses c)]
        (.Unlock dynMu)
        (and ok (!= (.-CF dc) nil))))
   (list 'go/func (native-name "alloc" "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;")
         (with-meta [(tag 'c '(* jrt/Class)) (tag 'values '(* jrt/RefArray))] {:tag 'any})
         '(let [dc (dynClassOf c)]
            (.Lock dynMu)
            (let [nw (aget cfNews c)]
              (.Unlock dynMu)
              (when (!= nw nil) (return (nw dc (.-A values))))
              (newDyn dc (.-A values)))))
   (list 'go/func (native-name "canExtend" "(Ljava/lang/Class;)Z")
         (with-meta [(tag 'c '(* jrt/Class))] {:tag 'bool})
         '(let [(values nw _ _) (cfSubFor c)]
            (or (== c jrt/Object_class) (!= nw nil))))
   (list 'go/func (native-name "superCtor" "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)V")
         [(tag 'o 'any) (tag 'c '(* jrt/Class)) (tag 'desc '(* jrt/String)) (tag 'args '(* jrt/RefArray))]
         '(let [(values _ ctors _) (cfSubFor c)
                f (aget ctors (.String (jrt/NN desc)))]
            (when (== f nil)
              (panic (jrt/Thrown (jrt/UnsupportedOperationException_New_String
                                   (jrt/Str (+ "constructor " (.-Name (.Info c)) (.String desc) " is not in the Go build"))))))
            (f o (.-A args))))
   (list 'go/func (native-name "superCall" "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;")
         (with-meta [(tag 'o 'any) (tag 'c '(* jrt/Class)) (tag 'key '(* jrt/String)) (tag 'args '(* jrt/RefArray))] {:tag 'any})
         '(let [(values _ _ supers) (cfSubFor c)
                f (aget supers (.String (jrt/NN key)))]
            (when (== f nil)
              (panic (jrt/Thrown (jrt/UnsupportedOperationException_New_String
                                   (jrt/Str (+ "super call of " (.-Name (.Info c)) "." (.String key) " is not in the Go build"))))))
            (f o (.-A args))))
   (list 'go/func (native-name "setOuter" "(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)V")
         [(tag 'c '(* jrt/Class)) (tag 'outer '(* jrt/Class)) (tag 'simple '(* jrt/String))]
         '(let [info (.Info c)]
            (set! (.-Declaring info) outer)
            (when (!= simple nil) (set! (.-Simple info) (.String simple)))))
   (list 'go/func (native-name "setEnum" "(Ljava/lang/Class;Larbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'values 'IFn)]
         '(let [info (.Info c)]
            (set! (.-Kind info) jrt/KindEnum)
            (set! (.-Enum info) (fn ^{:tag (* jrt/RefArray)} [] (assert (* jrt/RefArray) (.Invoke__O values))))))
   (list 'go/func (native-name "klassOf" "(Ljava/lang/Object;)Ljava/lang/Object;")
         (with-meta [(tag 'o 'any)] {:tag 'any})
         '(let [(values d ok) (assert dynObject o)]
            (when (not ok) (return nil))
            (.-CF (.DynClassOf d))))
   (list 'go/func (native-name "setMethod" "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Ljava/lang/Class;ILarbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'params '(* jrt/RefArray)) (tag 'ret '(* jrt/Class))
          (tag 'flags 'int32) (tag 'impl 'IFn)]
         '(let [info (.Info c)
                ps (dynClassList params)]
            (when (!= (bit-and flags jrt/AccStatic) 0)
              (set! (.-Methods info)
                    (append (.-Methods info)
                            (lit jrt/MethodInfo :Name (.String (jrt/NN name)) :Params ps :Return ret :Modifiers flags
                                 :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                           (dynConvert ret (dynCall impl nil args))))))
              (return))
            (let [key (+ (.String (jrt/NN name)) "(")]
              (range [_ p ps] (set! key (+ key (.Descriptor p))))
              (set! key (+ key ")" (.Descriptor (jrt/NN ret))))
              ;; an interface's method, or an abstract one: the member table's entry only
              (.Lock dynMu)
              (let [(values dc ok) (aget dynClasses c)]
                (.Unlock dynMu)
                (when (and ok (!= impl nil))
                  (let [(values i ok) (aget (.-SlotMap dc) key)]
                    (when ok (aset (.-Slots dc) i impl)))
                  (aset (.-ByKey dc) key impl)))
              (set! (.-Methods info)
                    (append (.-Methods info)
                            (lit jrt/MethodInfo :Name (.String name) :Params ps :Return ret :Modifiers flags
                                 :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                           ;; virtual: the receiver's class's method of this key
                                           (let [d (dynOf this)
                                                 f (aget (.-ByKey (.DynClassOf d)) key)]
                                             (when (== f nil) (set! f impl))
                                             (when (== f nil)
                                               (panic (dynAbstract d (+ "'" (.String name) "' of " (.-Name (.Info c))))))
                                             (dynConvert ret (dynCall f d args))))))))))
   (list 'go/func (native-name "addCtor" "(Ljava/lang/Class;[Ljava/lang/Class;ILarbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'params '(* jrt/RefArray)) (tag 'flags 'int32) (tag 'impl 'IFn)]
         '(let [info (.Info c)]
            (set! (.-Ctors info)
                  (append (.-Ctors info)
                          (lit jrt/CtorInfo :Params (dynClassList params) :Modifiers flags
                               :New (fn ^any [^{:tag (slice any)} args] (dynCall impl nil args)))))))
   (list 'go/func (native-name "addField" "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;II)V")
         [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'ftype '(* jrt/Class)) (tag 'flags 'int32) (tag 'i 'int32)]
         '(let [info (.Info c)
                k i]
            (set! (.-Fields info)
                  (append (.-Fields info)
                          (lit jrt/FieldInfo :Name (.String (jrt/NN name)) :Type ftype :Modifiers flags
                               :Get (fn ^any [^any o] (cfGoValue ftype (aget (arbace.core/deref (.DynFields (dynOf o))) k)))
                               :Set (fn [^any o ^any v] (aset (arbace.core/deref (.DynFields (dynOf o))) k (jrt/Box v))))))))
   (list 'go/func (native-name "addStatic" "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Object;ILarbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'ftype '(* jrt/Class)) (tag 'flags 'int32)
          (tag 'store '(* jrt/RefArray)) (tag 'i 'int32) (tag 'init 'IFn)]
         '(let [info (.Info c)
                k i]
            (when (== (.-Init info) nil)
              (set! (.-Init info) (fn [] (.Invoke__O init))))
            (set! (.-Fields info)
                  (append (.-Fields info)
                          (lit jrt/FieldInfo :Name (.String (jrt/NN name)) :Type ftype :Modifiers flags
                               :Get (fn ^any [^any o] (.Invoke__O init) (cfGoValue ftype (aget (.-A store) k)))
                               :Set (fn [^any o ^any v] (.Invoke__O init) (aset (.-A store) k (jrt/Box v))))))))
   '(go/func cfGoValue
      "cfGoValue: a field's value (an object) in the member tables' convention: a primitive as its Go
value.\n"
      ^any [^{:tag (* jrt/Class)} ftype ^any v]
      (when (and (.IsPrimitive__Z ftype) (!= v nil))
        (let [(values x ok) (jrt/Unbox ftype v)]
          (when ok (return x))))
      v)]))

;; ---------------------------------------------------------------------------------------------
;; the generic signatures of the world (arbace/classes/generics.edn, embedded): what
;; arbace.classes.analyze/class-generics reads from the JVM's reflection, for javac's bridges

(defn- strip-anns [x]
  (walk/postwalk (fn [y] (if (and (map? y) (:anns y)) (dissoc y :anns) y)) x))

(defn generics-text
  "The generic view (class-generics) of the classes of T that are generic or have generic
  supertypes, as text read by arbace.classes.go/generics: {internal-name {:tparams [sym]
  :supers [tnode] :methods [...]}}, methods only for generic classes (the others' come from
  reflection)."
  [T]
  (let [extendable (set (concat dyn/sub-supers ["arbace/lang/AFunction" "arbace/lang/RestFn"]))
        entries (for [n (sort T)
                      :let [d (a/decl n)]
                      ;; what an interpreted class can extend or implement: interfaces, the
                      ;; superclasses with a DynSub type, a Clojure fn's superclasses
                      :when (and d (not (#{:anon :local} (:nesting d)))
                                 (or (m/interface? n) (extendable n)))
                      :let [g (try (#'a/class-generics n) (catch Throwable _ nil))]
                      :when g
                      :let [generic? (seq (:tparams g))
                            supers (vec (remove nil? (:supers g)))]
                      :let [abstract? (and (not (m/interface? n)) (pos? (bit-and (or (:flags d) 0) 0x0400)))]
                      :when (or generic? abstract? (some (comp seq :args) supers))]
                  [n (strip-anns
                       (cond-> {:tparams (vec (:tparams g)) :supers supers}
                         abstract? (assoc :ctors (vec (for [mm (:methods d)
                                                            :when (= "<init>" (:name mm))
                                                            :when (zero? (bit-and (or (:flags mm) 0) 0x1002))]
                                                        [(:desc mm) (:flags mm)])))
                         generic? (assoc :methods (vec (for [mm (:methods g)
                                                             :when (zero? (bit-and (or (:flags mm) 0) 0x1002))]
                                                         (select-keys mm [:name :desc :flags :params :bounds]))))))])]
    (binding [*print-length* nil *print-level* nil *print-meta* false]
      (str ";; Written by c2g --program (arbace.c2g.dyncf/generics-text): the generic signatures of the\n"
           ";; classes of the world, for the class forms' analysis (doc/go/CLASSFORMS-REPL.md).\n{"
           (str/join "\n " (for [[n g] entries] (str (pr-str n) " " (pr-str g))))
           "}\n"))))
