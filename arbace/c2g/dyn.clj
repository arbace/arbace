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
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm])
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

(defn- method-form [ifaces i [name desc :as k]]
  (let [[ps r] (t/parse-method-desc desc)
        pn (vec (for [j (range (count ps))] (symbol (str "p" j))))
        base (nm/method-base name desc)
        params (vec (cons (tag 't '(* Dyn)) (map #(tag %1 (m/go-type :lang %2)) pn ps)))
        params (if (= "V" r) params (with-meta params {:tag (m/go-type :lang r)}))
        call (call-impl (map box-arg pn ps))
        dflt (concat
               (for [[j fsym] (defaults-for ifaces k)]
                 (list 'when (list '.DynImplements 't (m/class-sym :lang j "_class"))
                       (if (= "V" r) (list 'do (apply list fsym 't pn) '(return)) (list 'return (apply list fsym 't pn)))))
               [(list 'panic (list 'dynAbstract 't (str name desc)))])]
    (list 'go/method (symbol base) params
          (list 'let ['f (list 'aget (list '.-Slots (list '.-D 't)) i)]
                (apply list 'when (list '== 'f nil) dflt)
                (if (= "V" r) call (list 'return (convert-result call r)))))))

(defn- native-name [mname desc]
  (str (m/go-name api-class) "_" (nm/method-base mname desc) "_native"))

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
                  ^{:tag (map (* jrt/Class) bool)} Ifaces ^{:tag (map string IFn)} ByKey))
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
       '(go/func dynAbstract ^any [^{:tag (* Dyn)} t ^string m]
          (jrt/Thrown (jrt/AbstractMethodError_New_String (jrt/Str (+ (.-Name (.Info (.-Cls (.-D t)))) "." m)))))
       '(go/method DynImplements ^bool [^{:tag (* Dyn)} t ^{:tag (* jrt/Class)} c]
          (aget (.-Ifaces (.-D t)) c))
       '(go/method Ref ^any [^{:tag (* Dyn)} t] (when (== t nil) (return nil)) t)
       '(go/method GetClass__Class ^{:tag (* jrt/Class)} [^{:tag (* Dyn)} t] (.-Cls (.-D t)))
       '(go/method Clone__O ^any [^{:tag (* Dyn)} t] (panic (jrt/CloneNotSupported t)))
       '(go/method ToString__String ^{:tag (* jrt/String)} [^{:tag (* Dyn)} t]
          (let [f (aget (.-Slots (.-D t)) 0)]
            (when (== f nil) (return (jrt/Object_toString t)))
            (jrt/String_Cast (.Invoke_O__O f t))))
       '(go/method HashCode__I ^int32 [^{:tag (* Dyn)} t]
          (let [f (aget (.-Slots (.-D t)) 1)]
            (when (== f nil) (return (.HashCode__I (addr (.-Object t)))))
            (RT_IntCast_O__I (.Invoke_O__O f t))))
       '(go/method Equals_O__Z ^bool [^{:tag (* Dyn)} t ^any o]
          (let [f (aget (.-Slots (.-D t)) 2)]
            (when (== f nil) (return (.Equals_O__Z (addr (.-Object t)) o)))
            (assert bool (dynUnbox jrt/Prim_boolean (.Invoke_O_O__O f t o)))))]
      (for [j ifaces] (list 'go/method (symbol (str "Is_" (m/go-name j))) [(tag 't '(* Dyn))]))
      (keep-indexed (fn [i k] (when (>= i (count object-slots)) (method-form ifaces i k))) ks)
      ;; the natives of Compiler$Dyn
      [(list 'go/func (symbol (native-name "defineClass" "(Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/String;)Ljava/lang/Class;"))
             (with-meta [(tag 'name '(* jrt/String)) (tag 'interfaces '(* jrt/RefArray)) (tag 'fieldNames '(* jrt/RefArray))]
               {:tag '(* jrt/Class)})
             (list 'let ['info '(addr (lit jrt/ClassInfo :Name (.String (jrt/NN name)) :Modifiers (bit-or jrt/AccPublic jrt/AccFinal)
                                          :Kind jrt/KindClass :Super jrt/Object_class :Go "arbace/lang.Dyn"))
                         'dc (list 'addr (list 'lit 'DynClass :Slots (list 'make '(slice IFn) n)
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
                                             :Get (fn ^any [^any o] (aget (.-F (assert (* Dyn) o)) k))
                                             :Set (fn [^any o ^any v] (aset (.-F (assert (* Dyn) o)) k v))))))))
                   '(let [c (jrt/DefineDynamic info)]
                      (set! (.-Cls dc) c)
                      (set! (.-IsInstance info) (fn ^bool [^any x] (let [(values d ok) (assert (* Dyn) x)] (and ok (== (.-D d) dc)))))
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
                (let [(values i ok) (aget dynSlots key)]
                  (when ok (aset (.-Slots dc) i impl)))
                (aset (.-ByKey dc) key impl)
                (let [info (.Info c)]
                  (set! (.-Methods info)
                        (append (.-Methods info)
                                (lit jrt/MethodInfo :Name (.String name) :Params ps :Return ret :Modifiers jrt/AccPublic
                                     :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                                               (let [a (lit (slice any) this)]
                                                 (range [_ x args] (set! a (append a (jrt/Box x))))
                                                 (let [r (.ApplyTo_ISeq__O impl (RT_Seq_O__ISeq (jrt/RefArrayOf jrt/Object_class (spread a))))]
                                                   (dynConvert ret r))))))))))
       (list 'go/func (symbol (native-name "newInstance" "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;"))
             (with-meta [(tag 'c '(* jrt/Class)) (tag 'fieldValues '(* jrt/RefArray))] {:tag 'any})
             '(let [d (addr (lit Dyn :D (dynClassOf c)))]
                (when (!= fieldValues nil)
                  (set! (.-F d) (append (lit (slice any)) (spread (.-A fieldValues)))))
                d))
       (list 'go/func (symbol (native-name "getField" "(Ljava/lang/Object;I)Ljava/lang/Object;"))
             (with-meta [(tag 'o 'any) (tag 'i 'int32)] {:tag 'any})
             '(aget (.-F (dynOf o)) i))
       (list 'go/func (symbol (native-name "setField" "(Ljava/lang/Object;ILjava/lang/Object;)V"))
             [(tag 'o 'any) (tag 'i 'int32) (tag 'v 'any)]
             '(aset (.-F (dynOf o)) i v))
       '(go/func dynOf ^{:tag (* Dyn)} [^any o]
          (let [(values d ok) (assert (* Dyn) o)]
            (when (not ok)
              (when (== o nil) (panic (jrt/Thrown (jrt/NPE))))
              (panic (jrt/Thrown (jrt/IllegalArgumentException_New_String (jrt/Str "not an object of a class made at run time")))))
            d))])))
