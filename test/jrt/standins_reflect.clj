(ns jrt.standins-reflect
  "jrt's stand-ins for phase 2b (doc/go/JRT-NOTES.md, \"Phase 2b\"): the Go forms of JDK classes
  that reflection, charsets and dates need before c2g translates them from jdk26u, written as
  C2G-SPEC says c2g writes them (§4.4 names, §5.3 class interfaces, §5.7 instanceof and casts),
  as test/jrt/standins.clj does for phase 1's:

  - the wrapper classes Number, Boolean, Character, Byte, Short, Integer, Long, Float, Double
    and Void (the parts jrt's boxing and unboxing use: the value field F_value, valueOf with
    jdk26u's caches, xxxValue, equals, hashCode, toString, compareTo, TYPE);
  - java.lang.reflect.Type (Class implements it) and InvocationTargetException.

  The file they make, go/arbace/jrt/standin_reflect.clj, is generated and tracked; when c2g's
  output joins the build it is deleted and the translated classes take the same names.

  bin/jrt standins   regenerates it with standin_lang.clj (ARBACE_CLASSPATH=test bin/arbace
                     -m jrt.standins-reflect FILE)."
  (:require [arbace.string :as str]))

;; [Java name, Go type of the value, descriptor code, primitive class, cache [lo hi] or nil,
;;  hashCode body (v is the value), toString body, compare body (a, b the values)]
(def boxes
  [["Boolean" "bool" "Z" "Prim_boolean" nil
    "(if v (return 1231) (return 1237))" "(StrOfBool v)"
    "(cond (== a b) (return 0) a (return 1) :else (return -1))"]
   ["Character" "uint16" "C" "Prim_char" [0 127]
    "(conv int32 v)" "(StrOfChar v)" "(- (conv int32 a) (conv int32 b))"]
   ["Byte" "int8" "B" "Prim_byte" [-128 127]
    "(conv int32 v)" "(StrOfInt (conv int32 v))" "(- (conv int32 a) (conv int32 b))"]
   ["Short" "int16" "S" "Prim_short" [-128 127]
    "(conv int32 v)" "(StrOfInt (conv int32 v))" "(- (conv int32 a) (conv int32 b))"]
   ["Integer" "int32" "I" "Prim_int" [-128 127]
    "v" "(StrOfInt v)" "(cond (< a b) (return -1) (== a b) (return 0) :else (return 1))"]
   ["Long" "int64" "J" "Prim_long" [-128 127]
    "(conv int32 (bit-xor v (conv int64 (>> (conv uint64 v) 32))))" "(StrOfLong v)"
    "(cond (< a b) (return -1) (== a b) (return 0) :else (return 1))"]
   ["Float" "float32" "F" "Prim_float" nil
    "(floatToIntBits v)" "(StrOfFloat v)" "(compareFloat64 (conv float64 a) (conv float64 b))"]
   ["Double" "float64" "D" "Prim_double" nil
    "(let [b (doubleToLongBits v)] (conv int32 (bit-xor b (conv int64 (>> (conv uint64 b) 32)))))"
    "(StrOfDouble v)" "(compareFloat64 a b)"]])

(def numeric #{"Byte" "Short" "Integer" "Long" "Float" "Double"})

(def number-methods
  ;; [Go name, Go result type, conversion of v]
  [["ByteValue__B" "int8" "int8"] ["ShortValue__S" "int16" "int16"] ["IntValue__I" "int32" "int32"]
   ["LongValue__J" "int64" "int64"] ["FloatValue__F" "float32" "float32"] ["DoubleValue__D" "float64" "float64"]])

(defn- conv-expr
  "The Java primitive conversion of v (Go type from) to Go type to, as c2g writes it (§7.4)."
  [from to]
  (cond
    (= from to) "v"
    (and (#{"float32" "float64"} from) (= to "int32")) (if (= from "float32") "(F2I v)" "(D2I v)")
    (and (#{"float32" "float64"} from) (= to "int64")) (if (= from "float32") "(F2L v)" "(D2L v)")
    (and (#{"float32" "float64"} from) (#{"int8" "int16"} to))
    (format "(conv %s (%s v))" to (if (= from "float32") "F2I" "D2I"))
    :else (format "(conv %s v)" to)))

(defn box-forms [[n gt code prim cache hash tostr cmp]]
  (let [p (str "^{:tag (* " n ")}")
        num (numeric n)]
    (str
      ";; ---- java.lang." n "\n\n"
      (format "(go/type %s (struct %s ^%s F_value))\n" n (if num "Number" "Object") gt)
      (format "(go/var %s_class\n  (Define (addr (lit ClassInfo :Name \"java.lang.%s\" :Kind KindClass :Modifiers (bit-or AccPublic AccFinal)\n                     :Super %s :Interfaces (lit (slice (* Class)) %s)\n                     :Go \"arbace/jrt.%s\"))))\n"
              n n (if num "Number_class" "Object_class")
              (if num "Comparable_class" "Serializable_class Comparable_class") n)
      (format "(go/var %s_TYPE %s)\n" n prim)
      (format "(go/func %s_New_%s %s [^%s v] (addr (lit %s :F_value v)))\n" n code p gt n)
      (if cache
        (let [[lo hi] cache]
          (str
            (format "(go/var ^{:tag (slice (* %s))} %sCache (new%sCache))\n(go/func new%sCache ^{:tag (slice (* %s))} []\n  (let [c (make (slice (* %s)) %d)]\n    (range [i _ c] (aset c i (%s_New_%s (conv %s (+ i %d)))))\n    c))\n"
                    n (str/lower-case n) n n n n (inc (- hi lo)) n code gt lo)
            (format "(go/func %s_ValueOf_%s__%s \"%s_ValueOf_%s__%s is %s.valueOf: cached from %d to %d, as jdk26u's.\\n\"\n  %s [^%s v]\n  (when (and (>= v %d) (<= v %d))\n    (return (aget %sCache (- (conv int v) %d))))\n  (%s_New_%s v))\n"
                    n code n n code n n lo hi p gt lo hi (str/lower-case n) lo n code)))
        (if (= n "Boolean")
          (str
            "(go/var [Boolean_TRUE (Boolean_New_Z true)] [Boolean_FALSE (Boolean_New_Z false)])\n"
            "(go/func Boolean_ValueOf_Z__Boolean ^{:tag (* Boolean)} [^bool v]\n  (when v (return Boolean_TRUE))\n  Boolean_FALSE)\n"
            "(go/method BooleanValue__Z ^bool [^{:tag (* Boolean)} t] (.-F_value t))\n")
          (format "(go/func %s_ValueOf_%s__%s %s [^%s v] (%s_New_%s v))\n" n code n p gt n code)))
      (when (= n "Character")
        "(go/method CharValue__C ^uint16 [^{:tag (* Character)} t] (.-F_value t))\n")
      (when num
        (apply str (for [[m rt to] number-methods]
                     (format "(go/method %s ^%s [%s t] (let [v (.-F_value t)] %s))\n" m rt p (conv-expr gt to)))))
      (format "(go/method HashCode__I ^int32 [%s t] (let [v (.-F_value t)] %s))\n" p hash)
      (format "(go/method Equals_O__Z ^bool [%s t ^any o]\n  (let [(values x ok) (assert (* %s) o)]\n    (and ok %s)))\n"
              p n (case n
                    "Float" "(== (floatToIntBits (.-F_value t)) (floatToIntBits (.-F_value x)))"
                    "Double" "(== (doubleToLongBits (.-F_value t)) (doubleToLongBits (.-F_value x)))"
                    "(== (.-F_value t) (.-F_value x))"))
      (format "(go/method ToString__String ^{:tag (* String)} [%s t] (let [v (.-F_value t)] %s))\n" p tostr)
      (format "(go/method CompareTo_%s__I ^int32 [%s t %s o]\n  (let [a (.-F_value t) b (.-F_value (NN o))]\n    %s))\n" n p p cmp)
      (format "(go/method CompareTo_O__I ^int32 [%s t ^any o] (.CompareTo_%s__I t (%s_Cast o)))\n" p n n)
      (format "(go/method Is_Comparable [%s t])\n" p)
      (when-not num (format "(go/method Is_Serializable [%s t])\n" p))
      (format "(go/method Ref ^any [%s t] (when (== t nil) (return nil)) t)\n" p)
      (format "(go/method GetClass__Class ^{:tag (* Class)} [%s t] %s_class)\n" p n)
      (format "(go/method Clone__O ^any [%s t] (panic (CloneNotSupported t)))\n" p)
      (format "(go/func %s_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* %s) x)] ok))\n" n n)
      (format "(go/func %s_Cast %s [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert (* %s) x)]\n    (when (not ok) (panic (ClassCast x %s_class)))\n    v))\n\n"
              n p n n))))

(def number-forms
  (str
    ";; ---- java.lang.Number\n\n"
    "(go/type Number (struct Object))\n"
    "(go/type Number_I\n  (interface Object_I (Self_Number ^{:tag (* Number)} []) (Is_Serializable [])\n"
    (str/join "\n" (for [[m rt _] number-methods] (format "    (%s ^%s [])" m rt)))
    "))\n"
    "(go/method Self_Number ^{:tag (* Number)} [^{:tag (* Number)} t] t)\n"
    "(go/method Is_Serializable [^{:tag (* Number)} t])\n"
    "(go/method Ctor [^{:tag (* Number)} t ^Number_I this])\n"
    "(go/var Number_class\n  (Define (addr (lit ClassInfo :Name \"java.lang.Number\" :Kind KindClass :Modifiers (bit-or AccPublic AccAbstract)\n                     :Super Object_class :Interfaces (lit (slice (* Class)) Serializable_class)\n                     :Go \"arbace/jrt.Number\"))))\n"
    "(go/func Number_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Number_I x)] ok))\n"
    "(go/func Number_Cast ^Number_I [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert Number_I x)]\n    (when (not ok) (panic (ClassCast x Number_class)))\n    v))\n\n"))

(def void-forms
  (str
    ";; ---- java.lang.Void\n\n"
    "(go/type Void (struct Object))\n"
    "(go/var Void_class\n  (Define (addr (lit ClassInfo :Name \"java.lang.Void\" :Kind KindClass :Modifiers (bit-or AccPublic AccFinal)\n                     :Super Object_class :Go \"arbace/jrt.Void\"))))\n"
    "(go/var Void_TYPE Prim_void)\n"
    "(go/method Ref ^any [^{:tag (* Void)} t] (when (== t nil) (return nil)) t)\n"
    "(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Void)} t] Void_class)\n"
    "(go/method ToString__String ^{:tag (* String)} [^{:tag (* Void)} t] (Object_toString t))\n"
    "(go/method Clone__O ^any [^{:tag (* Void)} t] (panic (CloneNotSupported t)))\n"
    "(go/func Void_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Void) x)] ok))\n\n"))

(def helpers
  (str
    ";; the bits of Float.floatToIntBits and Double.doubleToLongBits (one NaN) and Double.compare\n"
    "(go/func floatToIntBits ^int32 [^float32 f]\n  (when (!= f f) (return 0x7fc00000))\n  (conv int32 (math/Float32bits f)))\n"
    "(go/func doubleToLongBits ^int64 [^float64 d]\n  (when (!= d d) (return 0x7ff8000000000000))\n  (conv int64 (math/Float64bits d)))\n"
    "(go/func compareFloat64 ^int32 [^float64 a ^float64 b]\n"
    "  (cond (< a b) (return -1) (> a b) (return 1))\n"
    "  (let [x (doubleToLongBits a) y (doubleToLongBits b)]\n"
    "    (cond (== x y) (return 0) (< x y) (return -1) :else (return 1))))\n\n"))

(def type-forms
  (str
    ";; ---- java.lang.reflect.Type\n\n"
    "(go/type Type\n  (interface Object_I (Is_Type [])\n    (GetTypeName__String ^{:tag (* String)} [])))\n"
    "(go/var Type_class\n  (Define (addr (lit ClassInfo :Name \"java.lang.reflect.Type\" :Kind KindInterface\n                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go \"arbace/jrt.Type\"))))\n"
    "(go/func Type_GetTypeName__String \"Type_GetTypeName__String is Type.getTypeName's default: toString().\\n\"\n  ^{:tag (* String)} [^Type this]\n  (.ToString__String this))\n"
    "(go/func Type_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Type x)] ok))\n"
    "(go/func Type_Cast ^Type [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert Type x)]\n    (when (not ok) (panic (ClassCast x Type_class)))\n    v))\n\n"))

(def throwable-methods
  (deref (requiring-resolve 'jrt.standins/throwable-methods)))

(def ite-forms
  (str
    ";; ---- java.lang.reflect.InvocationTargetException (getCause is the target)\n\n"
    "(go/type InvocationTargetException (struct ReflectiveOperationException ^Throwable_I F_target))\n"
    "(go/var InvocationTargetException_class\n  (Define (addr (lit ClassInfo :Name \"java.lang.reflect.InvocationTargetException\" :Kind KindClass :Modifiers AccPublic\n                     :Super ReflectiveOperationException_class :Go \"arbace/jrt.InvocationTargetException\"))))\n"
    "(go/func InvocationTargetException_New ^{:tag (* InvocationTargetException)} []\n  (let [t (addr (lit InvocationTargetException))] (.Ctor t) t))\n"
    "(go/method Ctor [^{:tag (* InvocationTargetException)} t]\n  (.Ctor_Throwable (.-ReflectiveOperationException t) t nil))\n"
    "(go/func InvocationTargetException_New_Throwable ^{:tag (* InvocationTargetException)} [^Throwable_I target]\n  (let [t (addr (lit InvocationTargetException))] (.Ctor_Throwable t target) t))\n"
    "(go/method Ctor_Throwable [^{:tag (* InvocationTargetException)} t ^Throwable_I target]\n  (.Ctor_Throwable (.-ReflectiveOperationException t) t nil)\n  (set! (.-F_target t) target))\n"
    "(go/func InvocationTargetException_New_Throwable_String ^{:tag (* InvocationTargetException)} [^Throwable_I target ^{:tag (* String)} s]\n  (let [t (addr (lit InvocationTargetException))] (.Ctor_Throwable_String t target s) t))\n"
    "(go/method Ctor_Throwable_String [^{:tag (* InvocationTargetException)} t ^Throwable_I target ^{:tag (* String)} s]\n  (.Ctor_String_Throwable (.-ReflectiveOperationException t) t s nil)\n  (set! (.-F_target t) target))\n"
    "(go/method GetTargetException__Throwable ^Throwable_I [^{:tag (* InvocationTargetException)} t] (.-F_target t))\n"
    "(go/method Ref ^any [^{:tag (* InvocationTargetException)} t] (when (== t nil) (return nil)) t)\n"
    "(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* InvocationTargetException)} t] InvocationTargetException_class)\n"
    "(go/method Clone__O ^any [^{:tag (* InvocationTargetException)} t] (panic (CloneNotSupported t)))\n"
    (apply str
           (for [[m res params args] throwable-methods]
             (if (= m "GetCause__Throwable")
               "(go/method GetCause__Throwable ^Throwable_I [^{:tag (* InvocationTargetException)} t] (.-F_target t))\n"
               (format "(go/method %s %s[^{:tag (* InvocationTargetException)} t%s] (.Impl_%s t t%s))\n" m res params m args))))
    "(go/func InvocationTargetException_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* InvocationTargetException) x)] ok))\n"
    "(go/func InvocationTargetException_Cast ^{:tag (* InvocationTargetException)} [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert (* InvocationTargetException) x)]\n    (when (not ok) (panic (ClassCast x InvocationTargetException_class)))\n    v))\n\n"))

(defn text []
  (str
    ";; Generated by test/jrt/standins_reflect.clj (bin/jrt standins); do not edit.\n"
    ";; jrt's stand-ins for JDK classes c2g will translate from jdk26u, phase 2b (doc/go/JRT-NOTES.md,\n"
    ";; \"Phase 2b\"): the wrapper classes, java.lang.reflect.Type, InvocationTargetException, in the\n"
    ";; shapes C2G-SPEC gives translated classes. Deleted when c2g's output joins the build.\n"
    "(in-ns 'go.arbace.jrt)\n\n"
    "(go/file \"standin_reflect.go\"\n  :imports [[math \"math\"]])\n\n"
    number-forms
    (apply str (map box-forms boxes))
    void-forms
    helpers
    type-forms
    ite-forms
    ";; ClassInfo.IsInstance, set at init (§5.11)\n"
    "(go/func init []"
    (apply str (for [n (concat ["Number" "Void" "Type" "InvocationTargetException"] (map first boxes))]
                 (format "\n  (set! (.-IsInstance (.Info %s_class)) %s_InstanceOf)" n n)))
    ")\n"))

(defn -main [file]
  (spit file (text)))
