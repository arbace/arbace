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
            [arbace.classes.analyze :as a]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]))

(def api-class "arbace/lang/Compiler$CFGo")

(defn enabled?
  "Is the class forms' host in this world (translated)?"
  []
  (m/translated? api-class))

(defn- tag [sym type] (with-meta sym {:tag type}))

(defn- native-name [mname desc]
  (symbol (str (m/go-name api-class) "_" (nm/method-base mname desc) "_native")))

(defn forms
  "The forms of c2g_cf.go (package arbace/lang)."
  []
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
            (let [info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers flags
                                  :Kind jrt/KindClass :Super super :Interfaces ifs
                                  :Go "arbace/lang.Dyn (class form)"))
                  dc (addr (lit DynClass :Slots (make (slice IFn) (len dynSlots)) :SlotMap dynSlots
                                :Ifaces (make (map (* jrt/Class) bool)) :ByKey (make (map string IFn))
                                :CF klass))]
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
                (.Unlock dynMu)
                c))))
   (list 'go/func (native-name "alloc" "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;")
         (with-meta [(tag 'c '(* jrt/Class)) (tag 'values '(* jrt/RefArray))] {:tag 'any})
         '(addr (lit Dyn :D (dynClassOf c) :F (.-A values))))
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
            (let [dc (dynClassOf c)
                  key (+ (.String (jrt/NN name)) "(")]
              (range [_ p ps] (set! key (+ key (.Descriptor p))))
              (set! key (+ key ")" (.Descriptor (jrt/NN ret))))
              (let [(values i ok) (aget (.-SlotMap dc) key)]
                (when ok (aset (.-Slots dc) i impl)))
              (aset (.-ByKey dc) key impl)
              (set! (.-Methods info)
                    (append (.-Methods info)
                            (lit jrt/MethodInfo :Name (.String name) :Params ps :Return ret :Modifiers flags
                                 :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                           ;; virtual: the receiver's class's method of this key
                                           (let [d (dynOf this)
                                                 f (aget (.-ByKey (.DynClassOf d)) key)]
                                             (when (== f nil) (set! f impl))
                                             (dynConvert ret (dynCall f d args))))))))))
   (list 'go/func (native-name "addCtor" "(Ljava/lang/Class;[Ljava/lang/Class;ILarbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'params '(* jrt/RefArray)) (tag 'flags 'int32) (tag 'impl 'IFn)]
         '(let [info (.Info c)]
            (set! (.-Ctors info)
                  (append (.-Ctors info)
                          (lit jrt/CtorInfo :Params (dynClassList params) :Modifiers flags
                               :New (fn ^any [^{:tag (slice any)} args] (dynCall impl nil args)))))))
   (list 'go/func (native-name "addField" "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;II)V")
         [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'type '(* jrt/Class)) (tag 'flags 'int32) (tag 'i 'int32)]
         '(let [info (.Info c)
                k i]
            (set! (.-Fields info)
                  (append (.-Fields info)
                          (lit jrt/FieldInfo :Name (.String (jrt/NN name)) :Type type :Modifiers flags
                               :Get (fn ^any [^any o] (cfGoValue type (aget (arbace.core/deref (.DynFields (dynOf o))) k)))
                               :Set (fn [^any o ^any v] (aset (arbace.core/deref (.DynFields (dynOf o))) k (jrt/Box v))))))))
   (list 'go/func (native-name "addStatic" "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Object;ILarbace/lang/IFn;)V")
         [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'type '(* jrt/Class)) (tag 'flags 'int32)
          (tag 'store '(* jrt/RefArray)) (tag 'i 'int32) (tag 'init 'IFn)]
         '(let [info (.Info c)
                k i]
            (when (== (.-Init info) nil)
              (set! (.-Init info) (fn [] (.Invoke__O init))))
            (set! (.-Fields info)
                  (append (.-Fields info)
                          (lit jrt/FieldInfo :Name (.String (jrt/NN name)) :Type type :Modifiers flags
                               :Get (fn ^any [^any o] (.Invoke__O init) (cfGoValue type (aget (.-A store) k)))
                               :Set (fn [^any o ^any v] (.Invoke__O init) (aset (.-A store) k (jrt/Box v))))))))
   '(go/func cfGoValue
      "cfGoValue: a field's value (an object) in the member tables' convention: a primitive as its Go
value.\n"
      ^any [^{:tag (* jrt/Class)} type ^any v]
      (when (and (.IsPrimitive__Z type) (!= v nil))
        (let [(values x ok) (jrt/Unbox type v)]
          (when ok (return x))))
      v)])

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
  (let [entries (for [n (sort T)
                      :let [d (a/decl n)]
                      :when (and d (not (#{:anon :local} (:nesting d))))
                      :let [g (try (#'a/class-generics n) (catch Throwable _ nil))]
                      :when g
                      :let [generic? (seq (:tparams g))
                            supers (vec (remove nil? (:supers g)))]
                      :when (or generic? (some (comp seq :args) supers))]
                  [n (strip-anns
                       (cond-> {:tparams (vec (:tparams g)) :supers supers}
                         generic? (assoc :methods (vec (for [mm (:methods g)
                                                             :when (zero? (bit-and (or (:flags mm) 0) 0x1002))]
                                                         (select-keys mm [:name :desc :flags :params :bounds]))))))])]
    (binding [*print-length* nil *print-level* nil *print-meta* false]
      (str ";; Written by c2g --program (arbace.c2g.dyncf/generics-text): the generic signatures of the\n"
           ";; classes of the world, for the class forms' analysis (doc/go/CLASSFORMS-REPL.md).\n{"
           (str/join "\n " (for [[n g] entries] (str (pr-str n) " " (pr-str g))))
           "}\n"))))
