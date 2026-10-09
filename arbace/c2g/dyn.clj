(ns arbace.c2g.dyn
  "C2G-SPEC §5.12: Dyn, the Go type of the objects of classes made at run time (deftype,
  defrecord, reify), in arbace/lang's c2g_dyn.go. Go cannot make types at run time, so one
  struct serves every such class: it has a Go method for every method of every interface of
  the closed world a run-time class may implement (the public interfaces translated, with their
  superinterfaces) and every marker, so that a Go assertion to any of them succeeds; each
  method calls the fn its class set for the method (by a slot number c2g assigns to the method's
  name and descriptor), else the interface's default method, else throws AbstractMethodError.
  The nominal check (does the object's class implement the interface) is in c2g's instanceof
  and checkcast (decls/instance-forms, jrt.Dynamic).

  The evaluator reaches it through the natives of Compiler$Dyn (arbace/lang/go/Compiler.clj):
  defineClass, setMethod, newInstance, getField, setField."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.model :as m]
            [arbace.c2g.code :as c]
            [arbace.c2g.names :as nm]
            [arbace.c2g.jrt :as jrt])
  (:import (arbace.asm Opcodes)))

(def api-class "arbace/lang/Compiler$Dyn")

(defn- tag [sym type] (with-meta sym {:tag type}))

(def object-slots
  "The Object methods a run-time class may override, at fixed slots."
  [["toString" "()Ljava/lang/String;"] ["hashCode" "()I"] ["equals" "(Ljava/lang/Object;)Z"]])

(def ^:private rt-casts
  "Clojure's conversion of a method's result to its primitive return type (as compiled deftype
  methods convert: HostExpr.emitUnboxArg)."
  {"I" "intCast" "J" "longCast" "D" "doubleCast" "F" "floatCast" "S" "shortCast" "B" "byteCast"})

(defn roots
  "What Dyn's code calls besides the world's interfaces: reachability roots."
  []
  (for [[r nme] rt-casts]
    ["arbace/lang/RT" nme (str "(Ljava/lang/Object;)" r)]))

(defn enabled?
  "Is the evaluator's Dyn API in this world (translated)?"
  []
  (m/translated? api-class))

(defn- public? [n] (not (zero? (bit-and (:flags (m/info n)) Opcodes/ACC_PUBLIC))))

(defn interfaces
  "The interfaces Dyn implements: every public interface translated (not an annotation), with
  every interface they extend that the world has."
  [T]
  (let [base (filter #(and (m/interface? %) (public? %)
                           (zero? (bit-and (:flags (m/info %)) Opcodes/ACC_ANNOTATION))
                           (not (m/reflected? %)))
                     T)]
    (sort (distinct (concat base
                            (for [n base s (m/all-supertypes n)
                                  :when (and (not= s n) (m/interface? s) (m/in-world? s))]
                              s))))))

(defn slot-keys
  "The methods of the interfaces, [name desc], sorted; Object's overridable ones first."
  [ifaces]
  (let [ks (sort (distinct (for [j ifaces [k _] (m/vmethods j) :when (not (m/object-keys k))] k)))]
    (vec (concat object-slots ks))))

(defn- defaults-for
  "The interfaces among ifaces with a default method for k, most specific first: [j fn-sym]."
  [ifaces k]
  (let [cands (for [j ifaces
                    :let [impl (m/impl-of j k)]
                    :when (and impl (= :default (:kind impl)) (m/translated? (:owner impl)))]
                [j (m/class-sym :lang (:owner impl) (str "_" (nm/method-base (first k) (second k))))])]
    (sort-by (fn [[j]] (- (count (m/all-supertypes j)))) cands)))

(defn- box-arg
  "A parameter of Go type for desc d as a Java object."
  [p d]
  (cond (t/prim? d) (list 'jrt/Box p)
        (m/pointer-desc? d) (list 'dynRef p)
        :else p))

(defn- convert-result
  "The Java object r as the Go value of return type d."
  [r d]
  (cond
    (= d "Ljava/lang/Object;") r
    (rt-casts d) (list (m/class-sym :lang "arbace/lang/RT" (str "_" (nm/method-base (rt-casts d) (str "(Ljava/lang/Object;)" d)))) r)
    (#{"Z" "C"} d) (list 'assert (m/go-type :lang d)
                         (list 'dynUnbox (if (= d "Z") 'jrt/Prim_boolean 'jrt/Prim_char) r))
    (t/array? d) (list (list 'inst 'jrt/C2g_CastArray (m/go-type :lang d)) r
                       (loop [d d n 0] (if (t/array? d) (recur (t/elem-type d) (inc n))
                                         (nth (iterate #(list '.ArrayClass %) (if (t/prim? d)
                                                                                 (symbol (str "jrt/Prim_" (t/prim-desc->name d)))
                                                                                 (m/class-sym :lang (t/desc->internal d) "_class")))
                                              n))))
    :else (list (m/class-sym :lang (t/desc->internal d) "_Cast") r)))

(defn- call-impl
  "The call of slot fn f with the object and the boxed arguments."
  [args]
  (let [n (inc (count args))]
    (if (<= n 20)
      (apply list (symbol (str "." (nm/method-base "invoke" (t/method-desc (repeat n "Ljava/lang/Object;") "Ljava/lang/Object;"))))
             'f 't args)
      (list '.ApplyTo_ISeq__O 'f (list 'RT_Seq_O__ISeq (apply list 'jrt/RefArrayOf 'jrt/Object_class 't args))))))

(defn- java-type-name
  "The Java name of descriptor d (int, java.lang.Object, byte[])."
  [d]
  (cond (t/array? d) (str (java-type-name (t/elem-type d)) "[]")
        (= 1 (count d)) ({"Z" "boolean" "B" "byte" "C" "char" "S" "short" "I" "int" "J" "long"
                          "F" "float" "D" "double" "V" "void"} d)
        :else (str/replace (t/desc->internal d) "/" ".")))

(defn- abstract-text
  "HotSpot's text of an unimplemented interface method, after the receiver class: the resolved
  method's signature and its interface (the first of ifaces declaring it)."
  [ifaces [name desc :as k]]
  (let [[ps r] (t/parse-method-desc desc)
        declares? (fn [j] (some #(and (= name (:name %)) (= desc (:desc %))) (:methods (a/decl j))))
        j (or (first (filter declares? ifaces))
              (first (filter #(some (fn [[kk _]] (= kk k)) (m/vmethods %)) ifaces)))]
    (str "'abstract " (java-type-name r) " " name "(" (str/join ", " (map java-type-name ps)) ")'"
         (when j (str " of interface " (str/replace j "/" "."))) ".")))

(defn- iface-fallback
  "What a method of the interfaces does when its slot is unset (or its fn answers SUPER): the
  interface's default method, else AbstractMethodError."
  [ifaces [name desc :as k] pn r]
  (concat
    (for [[j fsym] (defaults-for ifaces k)]
      (list 'when (list '.DynImplements 't (m/class-sym :lang j "_class"))
            (if (= "V" r) (list 'do (apply list fsym 't pn) '(return)) (list 'return (apply list fsym 't pn)))))
    [(list 'panic (list 'dynAbstract 't (abstract-text ifaces k)))]))

(defn- method-form
  "The Go method of slot i (method k) on receiver type recv (Dyn or a DynSub_C): the slot's fn
  when it is set and does not answer SUPER (a proxy's method without a mapping), else the
  statements of fallback."
  ([ifaces i k] (method-form '(* Dyn) i k (fn [pn r] (iface-fallback ifaces k pn r))))
  ([recv i [name desc :as k] fallback]
   (let [[ps r] (t/parse-method-desc desc)
         pn (vec (for [j (range (count ps))] (symbol (str "p" j))))
         base (nm/method-base name desc)
         params (vec (cons (tag 't recv) (map #(tag %1 (m/go-type :lang %2)) pn ps)))
         params (if (= "V" r) params (with-meta params {:tag (m/go-type :lang r)}))
         call (call-impl (map box-arg pn ps))]
     (apply list 'go/method (symbol base) params
            (list 'let ['f (list 'aget (list '.-Slots (list '.-D 't)) i)]
                  (list 'when (list '!= 'f nil)
                        (list 'let ['r call]
                              (list 'when (list '!= 'r 'dynSuper)
                                    (if (= "V" r) '(return) (list 'return (convert-result 'r r)))))))
            (fallback pn r)))))

(defn- native-name [mname desc]
  (str (m/go-name api-class) "_" (nm/method-base mname desc) "_native"))

;; ---------------------------------------------------------------------------------------
;; proxies of a class (EVAL-NOTES.md, phase 2A): proxy over Dyn

(def proxy-supers
  "The classes besides Object that a proxy may extend in the Go build: each gets a Go type
  DynSub_C, which embeds C's struct (so it is a C_I) and has Dyn's methods (every interface
  of the world) and C's virtual methods, each calling its slot's fn and, when unset or
  answering SUPER, C's implementation. A class must be translated, non-final and not a leaf
  (its values are C_I, §5.3)."
  ["java/io/Writer" "java/io/Reader" "java/io/PushbackReader" "java/io/InputStream"
   "java/io/OutputStream" "arbace/lang/APersistentMap"
   ;; jrt's hand-written ThreadLocal (test.check's random, arbace.instant on the JVM)
   "java/lang/ThreadLocal"])

(defn proxy-classes
  "The classes of proxy-supers a proxy may extend in this world (translated)."
  [T]
  (filter #(and (or (contains? T %) (m/hand-written? %)) (m/in-world? %)
                (not (m/interface? %)) (not (m/final? %)) (not (m/leaf? %)))
          proxy-supers))

(defn- sub-name [n] (str "DynSub_" (m/go-name n)))

(defn proxy-roots
  "Reachability roots of the proxies' superclasses: their constructors and virtual methods (a
  DynSub_C calls C's constructor bodies and implementations)."
  []
  (for [n proxy-supers
        :when (and (a/decl n) (not (m/hand-written? n)))
        c (m/superclass-chain n)
        :when (and c (not= c "java/lang/Object") (a/decl c))
        mm (:methods (a/decl c))
        :when (not (m/static? mm))
        :when (or (= "<init>" (:name mm)) (not (m/private? mm)))
        :when (or (= c n) (not= "<init>" (:name mm)))]
    [c (:name mm) (:desc mm)]))

(defn- object-fallback
  "The fallback of Object's method k (toString, hashCode, equals) in DynSub_C: C's
  implementation, else the header's."
  [n k pn]
  (let [impl (m/impl-of n k)
        base (apply nm/method-base k)]
    (if (#{:class :jrt-impl} (:kind impl))
      [(list 'return (apply list (symbol (str ".Impl_" base)) 't 't pn))]
      [(list 'return (case (first k)
                       "toString" '(jrt/Object_toString t)
                       "hashCode" '(.HashCode__I (.Self_Object t))
                       "equals" (list '.Equals_O__Z '(.Self_Object t) (first pn))))])))

(defn- class-fallback
  "The fallback of C's virtual method k in DynSub_C: C's implementation (the super call), else
  AbstractMethodError."
  [n [name desc :as k] pn r]
  (let [impl (m/impl-of n k)
        base (nm/method-base name desc)
        ret (fn [call] (if (= "V" r) [call] [(list 'return call)]))]
    (case (:kind impl)
      (:class :jrt-impl) (ret (apply list (symbol (str ".Impl_" base)) 't 't pn))
      :default (ret (apply list (m/class-sym :lang (:owner impl) (str "_" base)) 't pn))
      :promoted (ret (apply list (symbol (str "." base)) (list (symbol (str ".-" (m/go-name n))) 't) pn))
      (let [[ps rr] (t/parse-method-desc desc)]
        [(list 'panic (list 'dynAbstract 't (str "'abstract " (java-type-name rr) " " name "("
                                                 (str/join ", " (map java-type-name ps)) ")' of abstract class "
                                                 (str/replace n "/" ".") ".")))]))))

(defn- ctor-infos
  "The constructors of DynSub_C (C's non-private ones), as a Go function of the DynClass."
  [n]
  (let [g (sub-name n)
        arg-conv (fn [d x] (cond (t/prim? d) (list 'assert (m/go-type :lang d) x)
                                 (= d "Ljava/lang/Object;") x
                                 :else (list (list 'inst 'jrt/As (m/go-type :lang d)) x)))]
    (binding [c/*f* {:pkg :lang}]
      (list 'go/func (symbol (str "dynCtors_" g)) (with-meta [(tag 'dc '(* DynClass))] {:tag '(slice jrt/CtorInfo)})
            (list 'return
                  (apply list 'lit '(slice jrt/CtorInfo)
                         (for [mm (if (m/hand-written? n) (m/methods-of n) (:methods (a/decl n)))
                               :when (and (= "<init>" (:name mm)) (not (m/private? mm)) (m/mdesc-in-world? (:desc mm)))
                               :when (or (not (m/hand-written? n))
                                         (contains? (jrt/struct-methods (:jrt m/*w*) (m/go-name n)) (nm/ctor-base (:desc mm))))
                               :let [real (if (m/hand-written? n) (:desc mm) (e/ctor-real-desc n mm))
                                     [ps _] (t/parse-method-desc (:desc mm))
                                     [rps _] (t/parse-method-desc real)]
                               :when (= (count ps) (count rps))]
                           (list 'lit 'jrt/CtorInfo
                                 :Params (when (seq ps) (apply list 'lit '(slice (* jrt/Class)) (map c/class-val ps)))
                                 :Modifiers 'jrt/AccPublic
                                 :New (list* 'fn (with-meta [(tag 'args '(slice any))] {:tag 'any})
                                            (concat
                                              (when-not (or (m/hand-written? n) (m/trivial-init? n)) [(list (m/class-sym :lang n "_Init"))])
                                              [(list 'let ['t (list 'addr (list 'lit (symbol g) :D 'dc :F '(make (slice any) 1)))]
                                                     (apply list (symbol (str "." (nm/ctor-base real)))
                                                            (list (symbol (str ".-" (m/go-name n))) 't) 't
                                                            (map-indexed (fn [i p] (arg-conv p (list 'aget 'args i))) ps))
                                                     't)]))))))))))

(defn- sub-forms
  "The Go type DynSub_C of the proxies of class n, its methods and its slot table."
  [n ifaces dyn-keys]
  (let [g (sub-name n)
        recv (list '* (symbol g))
        own (into {} (for [[k mm] (m/vmethods n) :when (not (m/object-keys k))] [k mm]))
        ks (vec (concat object-slots (sort (distinct (concat (drop (count object-slots) dyn-keys) (keys own))))))
        slots (symbol (str "dynSlots_" g))]
    (concat
      [(list 'c2g/comment (str "---- DynSub_" (m/go-name n) ": proxies of " (str/replace n "/" ".") ", "
                               (count ks) " methods"))
       (list 'go/type (symbol g)
             (str g " is an object of a proxy class extending " (str/replace n "/" ".")
                  ": its struct, the class's dispatch table D and its fields F (the fn map).\n")
             (list 'struct (m/class-sym :lang n) (tag 'D '(* DynClass)) (tag 'F '(slice any))))
       (list 'go/var (tag slots '(map string int32))
             (apply list 'lit '(map string int32) (map-indexed (fn [i [nme desc]] [(str nme desc) i]) ks)))
       (list 'go/var (tag (symbol (str "dynOwn_" g)) '(map string string))
             (apply list 'lit '(map string string) (for [[nme desc] (sort (keys own))] [(str nme desc) nme])))
       (list 'go/method 'DynImplements (with-meta [(tag 't recv) (tag 'c '(* jrt/Class))] {:tag 'bool})
             '(aget (.-Ifaces (.-D t)) c))
       (list 'go/method 'DynClassOf (with-meta [(tag 't recv)] {:tag '(* DynClass)}) '(.-D t))
       (list 'go/method 'DynFields (with-meta [(tag 't recv)] {:tag '(* (slice any))}) '(addr (.-F t)))
       (list 'go/method 'Ref (with-meta [(tag 't recv)] {:tag 'any}) '(when (== t nil) (return nil)) 't)
       (list 'go/method 'GetClass__Class (with-meta [(tag 't recv)] {:tag '(* jrt/Class)}) '(.-Cls (.-D t)))
       (list 'go/method 'Clone__O (with-meta [(tag 't recv)] {:tag 'any}) '(panic (jrt/CloneNotSupported t)))
       (list 'go/method 'CloneShallow (with-meta [(tag 't recv)] {:tag 'any}) '(panic (jrt/CloneNotSupported t)))]
      (for [i (range (count object-slots))
            :let [k (nth object-slots i)]]
        (method-form recv i k (fn [pn r] (object-fallback n k pn))))
      (for [j ifaces] (list 'go/method (symbol (str "Is_" (m/go-name j))) [(tag 't recv)]))
      (keep-indexed (fn [i k]
                      (when (>= i (count object-slots))
                        (method-form recv i k (if (own k)
                                                (fn [pn r] (class-fallback n k pn r))
                                                (fn [pn r] (iface-fallback ifaces k pn r))))))
                    ks)
      [(ctor-infos n)])))

(defn forms
  "The forms of c2g_dyn.go (package arbace/lang) for the translated classes T."
  [T]
  (let [ifaces (interfaces T)
        ks (slot-keys ifaces)
        n (count ks)]
    (concat
      [(list 'c2g/comment (str "---- Dyn (C2G-SPEC §5.12): " (count ifaces) " interfaces, " n " methods"))
       '(go/type Dyn
          "Dyn is an object of a class made at run time (deftype, defrecord, reify): its class's
dispatch table D and its fields F.\n"
          (struct jrt/Object ^{:tag (* DynClass)} D ^{:tag (slice any)} F))
       '(go/type DynClass
          "DynClass is a class made at run time: its Class, the fns of its methods by slot (nil:
the interface's default, else AbstractMethodError), the interfaces it implements (with their
superinterfaces), and its methods by name and descriptor (all of them, for reflection and the
interfaces made at run time).\n"
          (struct ^{:tag (* jrt/Class)} Cls ^{:tag (slice IFn)} Slots
                  ^{:tag (map (* jrt/Class) bool)} Ifaces ^{:tag (map string IFn)} ByKey
                  ^{:tag (map string int32)} SlotMap ^bool Proxy ^{:tag (map string string)} Own))
       '(go/type dynObject
          "dynObject is an object of a class made at run time: a Dyn, or a DynSub_C (a proxy of class C).\n"
          (interface (DynClassOf ^{:tag (* DynClass)} []) (DynFields ^{:tag (* (slice any))} [])
                     (DynImplements ^bool [^{:tag (* jrt/Class)} c])))
       '(go/var ^{:tag any :doc "dynSuper is what a proxy's method fn answers when the proxy has no fn for the method: the\nsuperclass's implementation runs (Compiler$Dyn.superMarker).\n"}
          dynSuper (jrt/Object_New))
       '(go/var ^{:tag sync/Mutex} dynMu)
       '(go/var ^{:tag (map (* jrt/Class) (* DynClass))} dynClasses (make (map (* jrt/Class) (* DynClass))))
       (list 'go/var (tag 'dynSlots '(map string int32))
             (apply list 'lit '(map string int32) (map-indexed (fn [i [nme desc]] [(str nme desc) i]) ks)))
       '(go/func dynRef :type-params [T] ^any [^{:tag (* T)} p]
          (when (== p nil) (return nil))
          p)
       '(go/func dynUnbox ^any [^{:tag (* jrt/Class)} c ^any x]
          (let [(values v ok) (jrt/Unbox c x)]
            (when (not ok)
              (when (== x nil) (panic (jrt/Thrown (jrt/NPE))))
              (panic (jrt/ClassCast x c)))
            v))
       (list 'go/func 'dynConvert
             "dynConvert is a method's result r as the Go value of its return class (reflection's
Invoke): Clojure's conversion to a primitive, as compiled deftype methods convert.\n"
             (with-meta [(tag 'c '(* jrt/Class)) (tag 'r 'any)] {:tag 'any})
             (apply list 'switch '(.Descriptor c)
                    (concat
                      (for [[d nme] (sort rt-casts)]
                        (list 'case [d] (list 'return (list (m/class-sym :lang "arbace/lang/RT" (str "_" (nm/method-base nme (str "(Ljava/lang/Object;)" d)))) 'r))))
                      ['(case ["Z" "C"] (return (dynUnbox c r)))
                       '(case ["V"] (return nil))]))
             'r)
       '(go/func dynAbstract ^any [^dynObject t ^string m]
          (jrt/Thrown (jrt/AbstractMethodError_New_String
                        (jrt/Str (+ "Receiver class " (.-Name (.Info (.-Cls (.DynClassOf t))))
                                    " does not define or inherit an implementation of the resolved method " m)))))
       '(go/method DynImplements ^bool [^{:tag (* Dyn)} t ^{:tag (* jrt/Class)} c]
          (aget (.-Ifaces (.-D t)) c))
       '(go/method DynClassOf ^{:tag (* DynClass)} [^{:tag (* Dyn)} t] (.-D t))
       '(go/method DynFields ^{:tag (* (slice any))} [^{:tag (* Dyn)} t] (addr (.-F t)))
       '(go/method Ref ^any [^{:tag (* Dyn)} t] (when (== t nil) (return nil)) t)
       '(go/method GetClass__Class ^{:tag (* jrt/Class)} [^{:tag (* Dyn)} t] (.-Cls (.-D t)))
       '(go/method Clone__O ^any [^{:tag (* Dyn)} t] (panic (jrt/CloneNotSupported t)))
       '(go/method ToString__String ^{:tag (* jrt/String)} [^{:tag (* Dyn)} t]
          (let [f (aget (.-Slots (.-D t)) 0)]
            (when (!= f nil)
              (let [r (.Invoke_O__O f t)]
                (when (!= r dynSuper) (return (jrt/String_Cast r)))))
            (jrt/Object_toString t)))
       '(go/method HashCode__I ^int32 [^{:tag (* Dyn)} t]
          (let [f (aget (.-Slots (.-D t)) 1)]
            (when (!= f nil)
              (let [r (.Invoke_O__O f t)]
                (when (!= r dynSuper) (return (RT_IntCast_O__I r)))))
            (.HashCode__I (addr (.-Object t)))))
       '(go/method Equals_O__Z ^bool [^{:tag (* Dyn)} t ^any o]
          (let [f (aget (.-Slots (.-D t)) 2)]
            (when (!= f nil)
              (let [r (.Invoke_O_O__O f t o)]
                (when (!= r dynSuper) (return (assert bool (dynUnbox jrt/Prim_boolean r))))))
            (.Equals_O__Z (addr (.-Object t)) o)))]
      (for [j ifaces] (list 'go/method (symbol (str "Is_" (m/go-name j))) [(tag 't '(* Dyn))]))
      (keep-indexed (fn [i k] (when (>= i (count object-slots)) (method-form ifaces i k))) ks)
      ;; the natives of Compiler$Dyn
      [(list 'go/func (symbol (native-name "defineClass" "(Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/String;)Ljava/lang/Class;"))
             (with-meta [(tag 'name '(* jrt/String)) (tag 'interfaces '(* jrt/RefArray)) (tag 'fieldNames '(* jrt/RefArray))]
               {:tag '(* jrt/Class)})
             (list 'let ['info '(addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers (bit-or jrt/AccPublic jrt/AccFinal)
                                          :Kind jrt/KindClass :Super jrt/Object_class :Go "arbace/lang.Dyn"))
                         'dc (list 'addr (list 'lit 'DynClass :Slots (list 'make '(slice IFn) n) :SlotMap 'dynSlots
                                               :Ifaces '(make (map (* jrt/Class) bool)) :ByKey '(make (map string IFn))))]
                   '(when (!= interfaces nil)
                      (range [_ x (.-A interfaces)]
                        (let [c (assert (* jrt/Class) x)]
                          (set! (.-Interfaces info) (append (.-Interfaces info) c))
                          (dynAddInterface dc c))))
                   '(when (!= fieldNames nil)
                      (range [i x (.-A fieldNames)]
                        (let [k i]
                          (set! (.-Fields info)
                                (append (.-Fields info)
                                        (lit jrt/FieldInfo :Name (.String (assert (* jrt/String) x)) :Type jrt/Object_class
                                             :Modifiers jrt/AccPublic
                                             :Get (fn ^any [^any o] (aget (arbace.core/deref (.DynFields (dynOf o))) k))
                                             :Set (fn [^any o ^any v] (aset (arbace.core/deref (.DynFields (dynOf o))) k v))))))))
                   '(let [c (jrt/DefineDynamic info)]
                      (set! (.-Cls dc) c)
                      (set! (.-IsInstance info) (fn ^bool [^any x] (let [(values d ok) (assert dynObject x)] (and ok (== (.DynClassOf d) dc)))))
                      (.Lock dynMu)
                      (aset dynClasses c dc)
                      (.Unlock dynMu)
                      c)))
       '(go/func dynAddInterface [^{:tag (* DynClass)} dc ^{:tag (* jrt/Class)} c]
          (when (aget (.-Ifaces dc) c) (return))
          (aset (.-Ifaces dc) c true)
          (range [_ s (.-Interfaces (.Info c))] (dynAddInterface dc s)))
       '(go/func dynClassOf ^{:tag (* DynClass)} [^{:tag (* jrt/Class)} c]
          (.Lock dynMu)
          (let [dc (aget dynClasses c)]
            (.Unlock dynMu)
            (when (== dc nil)
              (panic (jrt/Thrown (jrt/IllegalArgumentException_New_String (jrt/Str (+ "not a class made at run time: " (.-Name (.Info c))))))))
            dc))
       (list 'go/func (symbol (native-name "setMethod" "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Ljava/lang/Class;Larbace/lang/IFn;)V"))
             [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'params '(* jrt/RefArray)) (tag 'ret '(* jrt/Class)) (tag 'impl 'IFn)]
             '(let [dc (dynClassOf c)
                    key (+ (.String (jrt/NN name)) "(")
                    ^{:tag (slice (* jrt/Class))} ps nil]
                (when (!= params nil)
                  (range [_ x (.-A params)]
                    (let [p (assert (* jrt/Class) x)]
                      (set! ps (append ps p))
                      (set! key (+ key (.Descriptor p))))))
                (set! key (+ key ")" (.Descriptor (jrt/NN ret))))
                (let [(values i ok) (aget (.-SlotMap dc) key)]
                  (when ok (aset (.-Slots dc) i impl))
                  ;; a proxy's method with a slot is reached by reflection through its
                  ;; superclass's or interface's member, which calls the Go method (the slot,
                  ;; else the superclass's implementation)
                  (when (and ok (.-Proxy dc)) (return)))
                (aset (.-ByKey dc) key impl)
                (let [info (.Info c)]
                  (set! (.-Methods info)
                        (append (.-Methods info)
                                (lit jrt/MethodInfo :Name (.String name) :Params ps :Return ret :Modifiers jrt/AccPublic
                                     :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                               (let [a (lit (slice any) this)]
                                                 (range [_ x args] (set! a (append a (jrt/Box x))))
                                                 (let [r (.ApplyTo_ISeq__O impl (RT_Seq_O__ISeq (jrt/RefArrayOf jrt/Object_class (spread a))))]
                                                   (when (== r dynSuper)
                                                     (panic (jrt/Thrown (jrt/UnsupportedOperationException_New_String name))))
                                                   (dynConvert ret r))))))))))
       (list 'go/func (symbol (native-name "setStaticMethod" "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Ljava/lang/Class;Larbace/lang/IFn;)V"))
             [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'params '(* jrt/RefArray)) (tag 'ret '(* jrt/Class)) (tag 'impl 'IFn)]
             '(let [info (.Info c)]
                (set! (.-Methods info)
                      (append (.-Methods info)
                              (lit jrt/MethodInfo :Name (.String (jrt/NN name)) :Params (dynClassList params) :Return ret
                                   :Modifiers (bit-or jrt/AccPublic jrt/AccStatic)
                                   :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                             (dynConvert ret (dynCall impl nil args))))))))
       (list 'go/func (symbol (native-name "defineCtor" "(Ljava/lang/Class;[Ljava/lang/Class;[Ljava/lang/Object;)V"))
             [(tag 'c '(* jrt/Class)) (tag 'params '(* jrt/RefArray)) (tag 'tail '(* jrt/RefArray))]
             '(let [dc (dynClassOf c)
                    info (.Info c)
                    ^{:tag (slice any)} tl nil]
                (when (!= tail nil)
                  (set! tl (append tl (spread (.-A tail)))))
                (set! (.-Ctors info)
                      (append (.-Ctors info)
                              (lit jrt/CtorInfo :Params (dynClassList params) :Modifiers jrt/AccPublic
                                   :New (fn ^any [^{:tag (slice any)} args]
                                          (let [f (make (slice any) 0 (+ (len args) (len tl)))]
                                            (range [_ x args] (set! f (append f (jrt/Box x))))
                                            (set! f (append f (spread tl)))
                                            (addr (lit Dyn :D dc :F f)))))))))
       (list 'go/func (symbol (native-name "defineInterface" "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/Class;"))
             (with-meta [(tag 'name '(* jrt/String)) (tag 'extends '(* jrt/RefArray))] {:tag '(* jrt/Class)})
             '(let [info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name))
                                    :Modifiers (bit-or jrt/AccPublic jrt/AccInterface jrt/AccAbstract)
                                    :Kind jrt/KindInterface :Interfaces (dynClassList extends)
                                    :Go "arbace/lang.Dyn (interface)"))
                    c (jrt/DefineDynamic info)]
                (set! (.-IsInstance info)
                      (fn ^bool [^any x]
                        (let [(values d ok) (assert dynObject x)]
                          (and ok (.DynImplements d c)))))
                c))
       (list 'go/func (symbol (native-name "addInterfaceMethod" "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Ljava/lang/Class;)V"))
             [(tag 'c '(* jrt/Class)) (tag 'name '(* jrt/String)) (tag 'params '(* jrt/RefArray)) (tag 'ret '(* jrt/Class))]
             '(let [info (.Info c)
                    ps (dynClassList params)
                    key (+ (.String (jrt/NN name)) "(")]
                (range [_ p ps] (set! key (+ key (.Descriptor p))))
                (set! key (+ key ")" (.Descriptor (jrt/NN ret))))
                (set! (.-Methods info)
                      (append (.-Methods info)
                              (lit jrt/MethodInfo :Name (.String name) :Params ps :Return ret
                                   :Modifiers (bit-or jrt/AccPublic jrt/AccAbstract)
                                   :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                             (let [d (dynOf this)
                                                   impl (aget (.-ByKey (.DynClassOf d)) key)]
                                               (when (== impl nil)
                                                 (panic (dynAbstract d (dynAbstractText c name ps ret))))
                                               (dynConvert ret (dynCall impl d args)))))))))
       (list 'go/func (symbol (native-name "defineFnClass" "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;"))
             (with-meta [(tag 'name '(* jrt/String)) (tag 'super '(* jrt/Class))] {:tag '(* jrt/Class)})
             '(jrt/DefineDynamic (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name))
                                            :Modifiers (bit-or jrt/AccPublic jrt/AccFinal)
                                            :Kind jrt/KindClass :Super super
                                            :Go "arbace/lang.Compiler_EvalFn (fn)"))))
       '(go/func dynTypeName "dynTypeName: Class.getTypeName (int, java.lang.Object, byte[])." ^string [^{:tag (* jrt/Class)} c]
          (when (.IsArray__Z c)
            (return (+ (dynTypeName (.GetComponentType__Class c)) "[]")))
          (.String (.GetName__String c)))
       '(go/func dynAbstractText
          "dynAbstractText: HotSpot's text of an unimplemented method of an interface made at run time."
          ^string [^{:tag (* jrt/Class)} c ^{:tag (* jrt/String)} name ^{:tag (slice (* jrt/Class))} ps ^{:tag (* jrt/Class)} ret]
          (let [s (+ "'abstract " (dynTypeName ret) " " (.String name) "(")]
            (range [i p ps]
              (when (> i 0) (set! s (+ s ", ")))
              (set! s (+ s (dynTypeName p))))
            (+ s ")' of interface " (.-Name (.Info c)) ".")))
       (list 'go/func (symbol (native-name "hideField" "(Ljava/lang/Class;I)V"))
             [(tag 'c '(* jrt/Class)) (tag 'i 'int32)]
             '(let [info (.Info c)]
                (set! (.-Modifiers (aget (.-Fields info) i)) jrt/AccPrivate)))
       '(go/func dynClassList
          "dynClassList: the classes of a Class[] (nil for null)."
          ^{:tag (slice (* jrt/Class))} [^{:tag (* jrt/RefArray)} a]
          (let [^{:tag (slice (* jrt/Class))} ps nil]
            (when (!= a nil)
              (range [_ x (.-A a)]
                (set! ps (append ps (assert (* jrt/Class) x)))))
            ps))
       '(go/func dynCall
          "dynCall calls a method's fn with the object (none for a static method) and the
arguments, boxed.\n"
          ^any [^IFn impl ^any this ^{:tag (slice any)} args]
          (let [^{:tag (slice any)} a nil]
            (when (!= this nil)
              (set! a (append a this)))
            (range [_ x args] (set! a (append a (jrt/Box x))))
            (.ApplyTo_ISeq__O impl (RT_Seq_O__ISeq (jrt/RefArrayOf jrt/Object_class (spread a))))))
       (list 'go/func (symbol (native-name "newInstance" "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;"))
             (with-meta [(tag 'c '(* jrt/Class)) (tag 'fieldValues '(* jrt/RefArray))] {:tag 'any})
             '(let [d (addr (lit Dyn :D (dynClassOf c)))]
                (when (!= fieldValues nil)
                  (set! (.-F d) (append (lit (slice any)) (spread (.-A fieldValues)))))
                d))
       (list 'go/func (symbol (native-name "getField" "(Ljava/lang/Object;I)Ljava/lang/Object;"))
             (with-meta [(tag 'o 'any) (tag 'i 'int32)] {:tag 'any})
             '(aget (arbace.core/deref (.DynFields (dynOf o))) i))
       (list 'go/func (symbol (native-name "setField" "(Ljava/lang/Object;ILjava/lang/Object;)V"))
             [(tag 'o 'any) (tag 'i 'int32) (tag 'v 'any)]
             '(aset (arbace.core/deref (.DynFields (dynOf o))) i v))
       '(go/func dynOf ^dynObject [^any o]
          (let [(values d ok) (assert dynObject o)]
            (when (not ok)
              (when (== o nil) (panic (jrt/Thrown (jrt/NPE))))
              (panic (jrt/Thrown (jrt/IllegalArgumentException_New_String (jrt/Str "not an object of a class made at run time")))))
            d))
       ;; proxies (EVAL-NOTES.md, phase 2A)
       (list 'go/func (symbol (native-name "fillProxySlots" "(Ljava/lang/Class;Larbace/lang/IFn;)V"))
             "the methods of a proxy's superclass that reflection does not list (protected ones of jrt's
classes) and that have no fn yet: the fn factory makes of their name.\n"
             [(tag 'c '(* jrt/Class)) (tag 'factory 'IFn)]
             '(let [dc (dynClassOf c)]
                (range [k i (.-SlotMap dc)]
                  (let [(values nme ok) (aget (.-Own dc) k)]
                    (when (and ok (== (aget (.-Slots dc) i) nil))
                      (aset (.-Slots dc) i (IFn_Cast (.Invoke_O__O factory (jrt/Str nme)))))))))
       (list 'go/func (symbol (native-name "superMarker" "()Ljava/lang/Object;"))
             (with-meta [] {:tag 'any})
             'dynSuper)
       (list 'go/func 'dynSubFor
             "dynSubFor: the slot table and constructors of the proxies of class c (nil: c has no DynSub type).\n"
             [(tag 'c '(* jrt/Class))] :results '[(map string int32) (func [(* DynClass)] [(slice jrt/CtorInfo)]) (map string string)]
             (apply list 'switch 'c
                    (for [p (proxy-classes T)]
                      (list 'case [(m/class-sym :lang p "_class")]
                            (list 'return (symbol (str "dynSlots_" (sub-name p))) (symbol (str "dynCtors_" (sub-name p)))
                                  (symbol (str "dynOwn_" (sub-name p)))))))
             '(return nil nil nil))
       (list 'go/func (symbol (native-name "defineProxyClass" "(Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/Class;"))
             (with-meta [(tag 'name '(* jrt/String)) (tag 'super '(* jrt/Class)) (tag 'interfaces '(* jrt/RefArray))]
               {:tag '(* jrt/Class)})
             (list 'when '(== super jrt/Object_class)
                   (list 'let ['c (list (symbol (native-name "defineClass" "(Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/String;)Ljava/lang/Class;"))
                                        'name 'interfaces '(jrt/RefArrayOf jrt/String_class (jrt/Str "__arbaceFnMap")))
                               'dc '(dynClassOf c)
                               'info '(.Info c)]
                         '(set! (.-Proxy dc) true)
                         '(set! (.-Modifiers (aget (.-Fields info) 0)) jrt/AccPrivate)
                         '(set! (.-Ctors info) (lit (slice jrt/CtorInfo)
                                                    (lit jrt/CtorInfo :Modifiers jrt/AccPublic
                                                         :New (fn ^any [^{:tag (slice any)} args]
                                                                (addr (lit Dyn :D dc :F (make (slice any) 1)))))))
                         '(return c)))
             '(let [(values slots ctors own) (dynSubFor super)]
                (when (== slots nil)
                  (panic (jrt/Thrown (jrt/UnsupportedOperationException_New_String
                                       (jrt/Str (+ "proxy of " (.-Name (.Info super)) " is not in the Go build"))))))
                (let [info (addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers jrt/AccPublic
                                      :Kind jrt/KindClass :Super super :Interfaces (dynClassList interfaces)
                                      :Go "arbace/lang.DynSub (proxy)"))
                      dc (addr (lit DynClass :Slots (make (slice IFn) (len slots)) :SlotMap slots :Proxy true :Own own
                                    :Ifaces (make (map (* jrt/Class) bool)) :ByKey (make (map string IFn))))]
                  (range [_ i (.-Interfaces info)] (dynAddInterface dc i))
                  (let [s super]
                    (while (!= s nil)
                      (range [_ i (.-Interfaces (.Info s))] (dynAddInterface dc i))
                      (set! s (.-Super (.Info s)))))
                  (let [c (jrt/DefineDynamic info)]
                    (set! (.-Cls dc) c)
                    (set! (.-IsInstance info) (fn ^bool [^any x] (let [(values d ok) (assert dynObject x)] (and ok (== (.DynClassOf d) dc)))))
                    (set! (.-Ctors info) (ctors dc))
                    (.Lock dynMu)
                    (aset dynClasses c dc)
                    (.Unlock dynMu)
                    c))))]
      (mapcat #(sub-forms % ifaces ks) (proxy-classes T)))))
