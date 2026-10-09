;; jrt: reflection over the member tables (C2G-SPEC §5.11, doc/go/JRT-NOTES.md, "Phase 2b"):
;; java.lang.reflect's AccessibleObject, Executable, Method, Constructor, Field, Member,
;; InvocationHandler and Proxy, Class's reflective methods, and the value conventions of the
;; tables' invokers (Box, Unbox, As).
(in-ns 'go.arbace.jrt)

(go/file "reflect.go"
  :imports [[strconv "strconv"] [strings "strings"] [sync "sync"]])

;; ---------------------------------------------------------------------------------------
;; The value convention of the member tables (JRT-NOTES.md, "The member tables")
;;
;; An invoker (MethodInfo.Invoke, CtorInfo.New, FieldInfo.Get/Set) takes and returns values in
;; their Go representation (§5.1): a primitive as the Go value of its type (bool, int8, uint16,
;; int16, int32, int64, float32, float64), a reference as itself (nil for null). jrt converts
;; Java's boxed arguments before calling (Unbox: unboxing, then widening, as Method.invoke
;; does) and boxes primitive results after (Box), so that the invokers c2g generates stay one
;; call each.

(go/func As
  "As is a reference argument of an invoker as its Go type T: nil for null, x itself
otherwise (jrt checked it against the parameter's class before the call).\n"
  :type-params [T] ^T [^any x]
  (let [(values v ok) (assert T x)]
    (when (and (not ok) (!= x nil))
      (panic (ClassCastException_New_String (Str (+ "jrt.As: " (.GoName (GetClass x)))))))
    v))

(go/func Box
  "Box is the Java object of a value in the tables' convention: a primitive Go value becomes
its wrapper (Integer.valueOf ..., with their caches), a reference stays itself.\n"
  ^any [^any v]
  (type-switch [x v]
    (case [bool] (return (Boolean_ValueOf_Z__Boolean x)))
    (case [int8] (return (Byte_ValueOf_B__Byte x)))
    (case [uint16] (return (Character_ValueOf_C__Character x)))
    (case [int16] (return (Short_ValueOf_S__Short x)))
    (case [int32] (return (Integer_ValueOf_I__Integer x)))
    (case [int64] (return (Long_ValueOf_J__Long x)))
    (case [float32] (return (Float_ValueOf_F__Float x)))
    (case [float64] (return (Double_ValueOf_D__Double x))))
  ;; a nil pointer of a Java class, typed in the any (an invoker's *Class result): null
  (let [(values r ok) (assert refValue v)]
    (when ok
      (return (.Ref r))))
  v)

(go/type refValue
  "refValue is a Java object's Ref: itself, or nil for a nil pointer (Box's null).\n"
  (interface (Ref ^any [])))

(go/func unwrap
  "unwrap: the primitive value and descriptor code of a wrapper object (0 when x is not one).\n"
  [^any x] :results [^any v ^byte code]
  (type-switch [w x]
    (case [(* Boolean)] (return (.-F_value w) \Z))
    (case [(* Character)] (return (.-F_value w) \C))
    (case [(* Byte)] (return (.-F_value w) \B))
    (case [(* Short)] (return (.-F_value w) \S))
    (case [(* Integer)] (return (.-F_value w) \I))
    (case [(* Long)] (return (.-F_value w) \J))
    (case [(* Float)] (return (.-F_value w) \F))
    (case [(* Double)] (return (.-F_value w) \D)))
  (return nil 0))

(go/func widen
  "widen converts the primitive value v of code from to the primitive type of code to by an
identity or widening primitive conversion (JLS 5.1.2); false when there is none.\n"
  [^any v ^byte from ^byte to] :results [^any r ^bool ok]
  (when (== from to)
    (return v true))
  (let [i (conv int64 0)
        f (conv float64 0)]
    (switch from
      (case [\B] (set! i (conv int64 (assert int8 v))))
      (case [\S] (set! i (conv int64 (assert int16 v))))
      (case [\C] (set! i (conv int64 (assert uint16 v))))
      (case [\I] (set! i (conv int64 (assert int32 v))))
      (case [\J] (set! i (assert int64 v)))
      (case [\F] (set! f (conv float64 (assert float32 v))))
      (default (return nil false)))
    (switch to
      (case [\S] (when (== from \B) (return (conv int16 i) true)))
      (case [\I] (when (or (== from \B) (== from \S) (== from \C)) (return (conv int32 i) true)))
      (case [\J] (when (or (== from \B) (== from \S) (== from \C) (== from \I)) (return i true)))
      (case [\F]
        (when (== from \J) (return (conv float32 i) true))
        (when (!= from \F) (return (conv float32 i) true)))
      (case [\D]
        (when (== from \F) (return f true))
        (return (conv float64 i) true)))
    (return nil false)))

(go/func Unbox
  "Unbox converts the Java object x to a value of the primitive class to in the tables'
convention, as Method.invoke and Field.set convert arguments: unboxing, then a widening
primitive conversion; false for null or when no conversion exists.\n"
  [^{:tag (* Class)} to ^any x] :results [^any v ^bool ok]
  (let [(values p code) (unwrap x)]
    (when (== code 0)
      (return nil false))
    (return (widen p code (.-desc to)))))

(go/func convertArg
  "convertArg converts one boxed argument for a parameter of class p (Unbox for primitives,
an instance check for references): IllegalArgumentException as the JVM's accessors throw it.\n"
  ^any [^{:tag (* Class)} p ^any x]
  (if (.IsPrimitive__Z p)
    (do
      (when (== x nil)
        (panic (IllegalArgumentException_New_Throwable (NPE))))
      (let [(values v ok) (Unbox p x)]
        (when (not ok)
          (panic (IllegalArgumentException_New_String (Str "argument type mismatch"))))
        (return v)))
    (do
      (when (and (!= x nil) (!= p Object_class) (not (.IsInstance_O__Z p x)))
        (panic (IllegalArgumentException_New_String (Str "argument type mismatch"))))
      (return x))))

(go/func convertArgs
  "convertArgs checks the argument count and converts each argument (convertArg).\n"
  ^{:tag (slice any)} [^{:tag (slice (* Class))} params ^{:tag (* RefArray)} args]
  (let [n 0]
    (when (!= args nil)
      (set! n (len (.-A args))))
    (when (!= n (len params))
      (panic (IllegalArgumentException_New_String
               (Str (+ "wrong number of arguments: " (strconv/Itoa n) " expected: " (strconv/Itoa (len params)))))))
    (let [out (make (slice any) n)]
      (range [i p params]
        (aset out i (convertArg p (aget (.-A args) i))))
      out)))

(go/func callWrapping
  "callWrapping runs f and wraps a Java exception it throws (a Go run-time error mapped first,
as Catch does) in InvocationTargetException, as Method.invoke and Constructor.newInstance do.\n"
  ^any [^{:tag (func [] [any])} f]
  (let [^Throwable_I exc nil
        r ((fn ^any [] :results [^any v]
             (defer (Catch (addr exc)))
             (set! v (f))
             (return)))]
    (when (!= exc nil)
      (panic (InvocationTargetException_New_Throwable exc)))
    r))

;; ---------------------------------------------------------------------------------------
;; java.lang.reflect.Member, AccessibleObject, Executable

(go/type Member
  "Member is java.lang.reflect.Member.\n"
  (interface Object_I (Is_Member [])
    (GetDeclaringClass__Class ^{:tag (* Class)} [])
    (GetName__String ^{:tag (* String)} [])
    (GetModifiers__I ^int32 [])
    (IsSynthetic__Z ^bool [])))

(go/var Member_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Member" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.Member"))))

(go/func Member_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Member x)] (dynNominal x Member_class ok)))

(go/type AccessibleObject_I
  "AccessibleObject_I is java.lang.reflect.AccessibleObject's class interface.\n"
  (interface Object_I
    (Self_AccessibleObject ^{:tag (* AccessibleObject)} [])
    (SetAccessible_Z__V [^bool flag])
    (TrySetAccessible__Z ^bool [])
    (IsAccessible__Z ^bool [])
    (CanAccess_O__Z ^bool [^any obj])
    (IsAnnotationPresent_Class__Z ^bool [^{:tag (* Class)} a])))

(go/type AccessibleObject
  "AccessibleObject is java.lang.reflect.AccessibleObject: the accessible flag. Every member
of a member table is accessible (the tables hold what Clojure may reach), so the flag only
records setAccessible.\n"
  (struct Object ^bool override))

(go/var AccessibleObject_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.AccessibleObject" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class :Go "arbace/jrt.AccessibleObject"))))

(go/method Self_AccessibleObject ^{:tag (* AccessibleObject)} [^{:tag (* AccessibleObject)} a] a)
(go/method SetAccessible_Z__V [^{:tag (* AccessibleObject)} a ^bool flag] (set! (.-override a) flag))
(go/method TrySetAccessible__Z ^bool [^{:tag (* AccessibleObject)} a] (set! (.-override a) true) true)
(go/method IsAccessible__Z ^bool [^{:tag (* AccessibleObject)} a] (.-override a))
(go/method CanAccess_O__Z
  "CanAccess_O__Z is AccessibleObject.canAccess: true (the tables hold accessible members).\n"
  ^bool [^{:tag (* AccessibleObject)} a ^any obj] true)
(go/method IsAnnotationPresent_Class__Z
  "IsAnnotationPresent_Class__Z: false, annotations are not kept (C2G-SPEC §12).\n"
  ^bool [^{:tag (* AccessibleObject)} a ^{:tag (* Class)} c] false)

(go/type Executable_I
  "Executable_I is java.lang.reflect.Executable's class interface (Method and Constructor).\n"
  (interface AccessibleObject_I Member
    (Self_Executable ^{:tag (* Executable)} [])
    (GetParameterTypes__Class1 ^{:tag (* RefArray)} [])
    (GetParameterCount__I ^int32 [])
    (GetExceptionTypes__Class1 ^{:tag (* RefArray)} [])
    (IsVarArgs__Z ^bool [])
    (ToGenericString__String ^{:tag (* String)} [])))

(go/type Executable
  "Executable is java.lang.reflect.Executable: what Method and Constructor share, the
declaring class, the parameter classes and the modifiers of a member table entry.\n"
  (struct AccessibleObject
          ^{:tag (* Class)} clazz
          ^{:tag (slice (* Class))} params
          ^int32 mods))

(go/var Executable_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Executable" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super AccessibleObject_class
                     :Interfaces (lit (slice (* Class)) Member_class)
                     :Go "arbace/jrt.Executable"))))

(go/method Self_Executable ^{:tag (* Executable)} [^{:tag (* Executable)} e] e)
(go/method Is_Member [^{:tag (* Executable)} e])
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* Executable)} e] (.-clazz e))
(go/method GetModifiers__I
  "GetModifiers__I is getModifiers: the modifier bits the JVM reports for a method, the
bridge, varargs and synthetic bits among them (toString masks them out).\n"
  ^int32 [^{:tag (* Executable)} e]
  (bit-and (.-mods e) 0x1DFF))
(go/method GetParameterTypes__Class1 ^{:tag (* RefArray)} [^{:tag (* Executable)} e]
  (classArray (.-params e)))
(go/method GetParameterCount__I ^int32 [^{:tag (* Executable)} e] (conv int32 (len (.-params e))))
(go/method GetExceptionTypes__Class1
  "GetExceptionTypes__Class1: empty, the tables keep no throws clauses (JRT-NOTES.md).\n"
  ^{:tag (* RefArray)} [^{:tag (* Executable)} e]
  (NewRefArray Class_class 0))
(go/method IsVarArgs__Z ^bool [^{:tag (* Executable)} e] (!= (bit-and (.-mods e) AccVarargs) 0))
(go/method IsSynthetic__Z ^bool [^{:tag (* Executable)} e] (!= (bit-and (.-mods e) AccSynthetic) 0))

(go/func classArray ^{:tag (* RefArray)} [^{:tag (slice (* Class))} cs]
  (let [a (NewRefArray Class_class (conv int32 (len cs)))]
    (range [i c cs]
      (aset (.-A a) i c))
    a))

(go/func sameParams ^bool [^{:tag (slice (* Class))} a ^{:tag (slice (* Class))} b]
  (when (!= (len a) (len b))
    (return false))
  (range [i c a]
    (when (!= c (aget b i))
      (return false)))
  true)

;; ---------------------------------------------------------------------------------------
;; The text of members (Executable.sharedToString, Field.toString, Modifier.toString)

(go/func modifierString
  "modifierString is Modifier.toString: the modifier words in the JLS order.\n"
  ^string [^int32 m]
  (let [^{:tag (slice string)} ws nil]
    (range [_ p (lit (slice (struct ^int32 bit ^string w))
                     (lit _ AccPublic "public") (lit _ AccProtected "protected") (lit _ AccPrivate "private")
                     (lit _ AccAbstract "abstract") (lit _ AccStatic "static") (lit _ AccFinal "final")
                     (lit _ AccTransient "transient") (lit _ AccVolatile "volatile")
                     (lit _ AccSynchronized "synchronized") (lit _ AccNative "native")
                     (lit _ 0x800 "strictfp") (lit _ AccInterface "interface"))]
      (when (!= (bit-and m (.-bit p)) 0)
        (set! ws (append ws (.-w p)))))
    (strings/Join ws " ")))

(go/func typeName
  "typeName is Class.getTypeName: the name, with [] per dimension for arrays.\n"
  ^string [^{:tag (* Class)} c]
  (let [dims ""]
    (while (.IsArray__Z c)
      (set! dims (+ dims "[]"))
      (set! c (.-comp c)))
    (+ (.-Name (.-info c)) dims)))

(go/func paramList ^string [^{:tag (slice (* Class))} params]
  (let [ns (make (slice string) (len params))]
    (range [i p params]
      (aset ns i (typeName p)))
    (+ "(" (strings/Join ns ",") ")")))

(go/func executableString
  "executableString is Executable.sharedToString: the modifiers (default for an interface's
default method), then the rest; the throws clause is not kept.\n"
  ^string [^int32 mods ^bool isDefault ^string rest]
  (let [s ""]
    (if (and (!= mods 0) (not isDefault))
      (set! s (+ (modifierString mods) " "))
      (do
        (let [access (bit-and mods (bit-or AccPublic AccProtected AccPrivate))]
          (when (!= access 0)
            (set! s (+ (modifierString access) " ")))
          (when isDefault
            (set! s (+ s "default ")))
          (let [other (bit-and mods (bit-not (bit-or AccPublic AccProtected AccPrivate)))]
            (when (!= other 0)
              (set! s (+ s (modifierString other) " ")))))))
    (+ s rest)))

;; ---------------------------------------------------------------------------------------
;; java.lang.reflect.Method

(go/type Method
  "Method is java.lang.reflect.Method (final: *Method): a method of a member table and the
class whose table lists it.\n"
  (struct Executable ^{:tag (* MethodInfo)} info))

(go/var Method_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Method" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Executable_class
                     :Go "arbace/jrt.Method"))))

(go/func newMethod ^{:tag (* Method)} [^{:tag (* Class)} c ^{:tag (* MethodInfo)} info]
  (addr (lit Method :Executable (lit Executable :clazz c :params (.-Params info) :mods (.-Modifiers info))
             :info info)))

(go/method Info "Info is the method's member table entry.\n"
  ^{:tag (* MethodInfo)} [^{:tag (* Method)} m] (.-info m))

(go/method GetName__String ^{:tag (* String)} [^{:tag (* Method)} m] (Intern (.-Name (.-info m))))
(go/method GetReturnType__Class ^{:tag (* Class)} [^{:tag (* Method)} m] (.-Return (.-info m)))
(go/method IsBridge__Z ^bool [^{:tag (* Method)} m] (!= (bit-and (.-mods m) AccBridge) 0))
(go/method IsDefault__Z
  "IsDefault__Z: a public, non-abstract, non-static method of an interface.\n"
  ^bool [^{:tag (* Method)} m]
  (and (== (bit-and (.-mods m) (bit-or AccAbstract AccPublic AccStatic)) AccPublic)
       (.IsInterface__Z (.-clazz m))))

(go/method ToString__String
  "ToString__String is Method.toString: public static java.lang.String
java.lang.String.valueOf(java.lang.Object).\n"
  ^{:tag (* String)} [^{:tag (* Method)} m]
  (Str (executableString (bit-and (.GetModifiers__I m) 0xD3F) (.IsDefault__Z m)
                         (+ (typeName (.-Return (.-info m))) " " (typeName (.-clazz m)) "."
                            (.-Name (.-info m)) (paramList (.-params m))))))

(go/method ToGenericString__String ^{:tag (* String)} [^{:tag (* Method)} m] (.ToString__String m))

(go/method Equals_O__Z
  "Equals_O__Z is Method.equals: same declaring class, name, return type and parameters.\n"
  ^bool [^{:tag (* Method)} m ^any o]
  (let [(values x ok) (assert (* Method) o)]
    (and ok (== (.-clazz m) (.-clazz x)) (== (.-Name (.-info m)) (.-Name (.-info x)))
         (== (.-Return (.-info m)) (.-Return (.-info x))) (sameParams (.-params m) (.-params x)))))

(go/method HashCode__I
  "HashCode__I is Method.hashCode: the declaring class's name's hash xor the name's.\n"
  ^int32 [^{:tag (* Method)} m]
  (bit-xor (.HashCode__I (.GetName__String (.-clazz m))) (.HashCode__I (.GetName__String m))))

(go/method Invoke_O_O1__O
  "Invoke_O_O1__O is Method.invoke: for an instance method, NullPointerException for a null
receiver and IllegalArgumentException for one that is not an instance of the declaring class;
the arguments checked and converted (convertArgs); the method called through its invoker,
virtually; an exception it throws wrapped in InvocationTargetException; a primitive result
boxed, void giving null.\n"
  ^any [^{:tag (* Method)} m ^any obj ^{:tag (* RefArray)} args]
  (let [info (.-info m)]
    (when (== (bit-and (.-mods m) AccStatic) 0)
      (when (== obj nil)
        (panic (NPE)))
      (when (not (.IsInstance_O__Z (.-clazz m) obj))
        (panic (IllegalArgumentException_New_String
                 (Str (+ "object of type " (.GoName (GetClass obj)) " is not an instance of " (.GoName (.-clazz m))))))))
    (let [cargs (convertArgs (.-params m) args)]
      (when (== (.-Invoke info) nil)
        (panic (AbstractMethodError_New_String (Str (+ (.GoName (.-clazz m)) "." (.-Name info))))))
      (Box (callWrapping (fn ^any [] ((.-Invoke info) obj cargs)))))))

(go/method Ref ^any [^{:tag (* Method)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Method)} t] Method_class)
(go/method Clone__O ^any [^{:tag (* Method)} t] (panic (CloneNotSupported t)))
(go/func Method_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Method) x)] ok))
(go/func Method_Cast ^{:tag (* Method)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Method) x)]
    (when (not ok) (panic (ClassCast x Method_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; java.lang.reflect.Constructor

(go/type Constructor
  "Constructor is java.lang.reflect.Constructor (final: *Constructor).\n"
  (struct Executable ^{:tag (* CtorInfo)} info))

(go/var Constructor_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Constructor" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Executable_class
                     :Go "arbace/jrt.Constructor"))))

(go/func newConstructor ^{:tag (* Constructor)} [^{:tag (* Class)} c ^{:tag (* CtorInfo)} info]
  (addr (lit Constructor :Executable (lit Executable :clazz c :params (.-Params info) :mods (.-Modifiers info))
             :info info)))

(go/method Info "Info is the constructor's member table entry.\n"
  ^{:tag (* CtorInfo)} [^{:tag (* Constructor)} k] (.-info k))
(go/method GetName__String ^{:tag (* String)} [^{:tag (* Constructor)} k] (.GetName__String (.-clazz k)))
(go/method ToString__String
  "ToString__String is Constructor.toString: public java.lang.String(java.lang.String).\n"
  ^{:tag (* String)} [^{:tag (* Constructor)} k]
  (Str (executableString (bit-and (.GetModifiers__I k) (bit-or AccPublic AccProtected AccPrivate)) false (+ (typeName (.-clazz k)) (paramList (.-params k))))))
(go/method ToGenericString__String ^{:tag (* String)} [^{:tag (* Constructor)} k] (.ToString__String k))
(go/method Equals_O__Z ^bool [^{:tag (* Constructor)} k ^any o]
  (let [(values x ok) (assert (* Constructor) o)]
    (and ok (== (.-clazz k) (.-clazz x)) (sameParams (.-params k) (.-params x)))))
(go/method HashCode__I ^int32 [^{:tag (* Constructor)} k] (.HashCode__I (.GetName__String (.-clazz k))))

(go/method NewInstance_O1__O
  "NewInstance_O1__O is Constructor.newInstance: InstantiationException for an abstract class
or an interface, the arguments checked and converted, the constructor's exceptions wrapped in
InvocationTargetException.\n"
  ^any [^{:tag (* Constructor)} k ^{:tag (* RefArray)} args]
  (let [c (.-clazz k)]
    (when (or (!= (bit-and (.GetModifiers__I c) (bit-or AccAbstract AccInterface)) 0) (== (.-New (.-info k)) nil))
      (panic (InstantiationException_New)))
    (let [cargs (convertArgs (.-params k) args)]
      (when (!= (.-Init (.-info c)) nil)
        ((.-Init (.-info c))))
      (callWrapping (fn ^any [] ((.-New (.-info k)) cargs))))))

(go/method Ref ^any [^{:tag (* Constructor)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Constructor)} t] Constructor_class)
(go/method Clone__O ^any [^{:tag (* Constructor)} t] (panic (CloneNotSupported t)))
(go/func Constructor_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Constructor) x)] ok))
(go/func Constructor_Cast ^{:tag (* Constructor)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Constructor) x)]
    (when (not ok) (panic (ClassCast x Constructor_class)))
    v))

(go/func Executable_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Executable_I x)] ok))
(go/func Executable_Cast ^Executable_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Executable_I x)]
    (when (not ok) (panic (ClassCast x Executable_class)))
    v))
(go/func AccessibleObject_InstanceOf ^bool [^any x] (let [(values _ ok) (assert AccessibleObject_I x)] ok))

;; ---------------------------------------------------------------------------------------
;; java.lang.reflect.Field

(go/type Field
  "Field is java.lang.reflect.Field (final: *Field).\n"
  (struct AccessibleObject ^{:tag (* Class)} clazz ^{:tag (* FieldInfo)} info))

(go/var Field_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Field" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super AccessibleObject_class
                     :Interfaces (lit (slice (* Class)) Member_class)
                     :Go "arbace/jrt.Field"))))

(go/func newField ^{:tag (* Field)} [^{:tag (* Class)} c ^{:tag (* FieldInfo)} info]
  (addr (lit Field :clazz c :info info)))

(go/method Info "Info is the field's member table entry.\n"
  ^{:tag (* FieldInfo)} [^{:tag (* Field)} f] (.-info f))
(go/method Is_Member [^{:tag (* Field)} f])
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* Field)} f] (.-clazz f))
(go/method GetName__String ^{:tag (* String)} [^{:tag (* Field)} f] (Intern (.-Name (.-info f))))
(go/method GetModifiers__I ^int32 [^{:tag (* Field)} f]
  (bit-and (.-Modifiers (.-info f)) (bit-or AccPublic AccPrivate AccProtected AccStatic AccFinal
                                            AccVolatile AccTransient AccEnum AccSynthetic)))
(go/method GetType__Class ^{:tag (* Class)} [^{:tag (* Field)} f] (.-Type (.-info f)))
(go/method IsSynthetic__Z ^bool [^{:tag (* Field)} f] (!= (bit-and (.-Modifiers (.-info f)) AccSynthetic) 0))
(go/method IsEnumConstant__Z ^bool [^{:tag (* Field)} f] (!= (bit-and (.-Modifiers (.-info f)) AccEnum) 0))
(go/method ToString__String
  "ToString__String is Field.toString: public static final double java.lang.Math.PI.\n"
  ^{:tag (* String)} [^{:tag (* Field)} f]
  (let [m (.GetModifiers__I f)
        s ""]
    (when (!= m 0)
      (set! s (+ (modifierString m) " ")))
    (Str (+ s (typeName (.-Type (.-info f))) " " (typeName (.-clazz f)) "." (.-Name (.-info f))))))
(go/method Equals_O__Z ^bool [^{:tag (* Field)} f ^any o]
  (let [(values x ok) (assert (* Field) o)]
    (and ok (== (.-clazz f) (.-clazz x)) (== (.-Name (.-info f)) (.-Name (.-info x)))
         (== (.-Type (.-info f)) (.-Type (.-info x))))))
(go/method HashCode__I ^int32 [^{:tag (* Field)} f]
  (bit-xor (.HashCode__I (.GetName__String (.-clazz f))) (.HashCode__I (.GetName__String f))))

(go/method setMessage
  "setMessage is the JVM's \"Can not set [static] [final] T field C.f to X\".\n"
  ^{:tag (* String)} [^{:tag (* Field)} f ^any v]
  (let [m (.-Modifiers (.-info f))
        s "Can not set"]
    (when (!= (bit-and m AccStatic) 0)
      (set! s (+ s " static")))
    (when (!= (bit-and m AccFinal) 0)
      (set! s (+ s " final")))
    (set! s (+ s " " (.GoName (.-Type (.-info f))) " field " (.GoName (.-clazz f)) "." (.-Name (.-info f)) " to "))
    (if (== v nil)
      (set! s (+ s "null value"))
      (set! s (+ s (.GoName (GetClass v)))))
    (Str s)))

(go/method receiver
  "receiver checks the object of an instance field access: NullPointerException for null,
IllegalArgumentException for an object of another class (the JVM's set message, also for
get).\n"
  ^any [^{:tag (* Field)} f ^any obj]
  (when (!= (bit-and (.-Modifiers (.-info f)) AccStatic) 0)
    (when (!= (.-Init (.-info (.-clazz f))) nil)
      ((.-Init (.-info (.-clazz f)))))
    (return nil))
  (when (== obj nil)
    (panic (NPE)))
  (when (not (.IsInstance_O__Z (.-clazz f) obj))
    (panic (IllegalArgumentException_New_String (.setMessage f obj))))
  obj)

(go/method Get_O__O
  "Get_O__O is Field.get: the value, boxed when primitive.\n"
  ^any [^{:tag (* Field)} f ^any obj]
  (let [o (.receiver f obj)]
    (Box ((.-Get (.-info f)) o))))

(go/method Set_O_O__V
  "Set_O_O__V is Field.set: IllegalAccessException for a final field, the value converted as
Method.invoke converts arguments (IllegalArgumentException with the JVM's message otherwise).\n"
  [^{:tag (* Field)} f ^any obj ^any v]
  (let [o (.receiver f obj)
        t (.-Type (.-info f))]
    (when (or (!= (bit-and (.-Modifiers (.-info f)) AccFinal) 0) (== (.-Set (.-info f)) nil))
      (panic (IllegalAccessException_New_String (.setMessage f v))))
    (if (.IsPrimitive__Z t)
      (let [(values x ok) (Unbox t v)]
        (when (not ok)
          (panic (IllegalArgumentException_New_String (.setMessage f v))))
        ((.-Set (.-info f)) o x))
      (do
        (when (and (!= v nil) (not (.IsInstance_O__Z t v)))
          (panic (IllegalArgumentException_New_String (.setMessage f v))))
        ((.-Set (.-info f)) o v)))))

(go/method Ref ^any [^{:tag (* Field)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Field)} t] Field_class)
(go/method Clone__O ^any [^{:tag (* Field)} t] (panic (CloneNotSupported t)))
(go/func Field_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Field) x)] ok))
(go/func Field_Cast ^{:tag (* Field)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Field) x)]
    (when (not ok) (panic (ClassCast x Field_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; java.lang.reflect.InvocationHandler and Proxy

(go/type InvocationHandler
  "InvocationHandler is java.lang.reflect.InvocationHandler.\n"
  (interface Object_I (Is_InvocationHandler [])
    (Invoke_O_Method_O1__O ^any [^any proxy ^{:tag (* Method)} m ^{:tag (* RefArray)} args])))

(go/var InvocationHandler_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.InvocationHandler" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.InvocationHandler"))))
(go/func InvocationHandler_InstanceOf ^bool [^any x] (let [(values _ ok) (assert InvocationHandler x)] (dynNominal x InvocationHandler_class ok)))

(go/var Proxy_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Proxy" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class :Go "arbace/jrt.Proxy"))))

(go/func Proxy_NewProxyInstance_ClassLoader_Class1_InvocationHandler__O
  "Proxy_NewProxyInstance_ClassLoader_Class1_InvocationHandler__O is Proxy.newProxyInstance:
classes cannot be made at run time, so it throws UnsupportedOperationException.
Reflector.boxArg's adaptation of an IFn to a functional interface goes through the
interface's ClassInfo.FromFn instead (JRT-NOTES.md, proposed amendment A13).\n"
  ^any [^ClassLoader_I loader ^{:tag (* RefArray)} interfaces ^InvocationHandler h]
  (panic (UnsupportedOperationException_New_String (Str "java.lang.reflect.Proxy"))))

(go/func AdaptFn
  "AdaptFn adapts f, an IFn, to the functional interface c through c's ClassInfo.FromFn
(§5.11, §7.11): what Reflector.boxArg does with a Proxy on the JVM. Nil when c has no
adapter.\n"
  ^any [^{:tag (* Class)} c ^any f]
  (when (== (.-FromFn (.-info c)) nil)
    (return nil))
  ((.-FromFn (.-info c)) f))

;; ---------------------------------------------------------------------------------------
;; Class: the member lists (C2G-SPEC §5.11; Class.getMethods' algorithm, PublicMethods)

(go/type reflMember "reflMember is a member table entry with the class whose table lists it.\n"
  (struct ^{:tag (* Class)} decl ^{:tag (* MethodInfo)} m ^{:tag (* FieldInfo)} f))

(go/var ^{:tag sync/Map} publicMethodsCache)
(go/var ^{:tag sync/Map} publicFieldsCache)

(go/method publicMethods
  "publicMethods computes Class.getMethods' list once per class: the public methods the class
declares, then its superclass's public methods, then its superinterfaces' non-static ones,
merged as java.lang.PublicMethods merges them (an override knocks out what it overrides, a
class's method knocks out an interface's of the same signature, unrelated interfaces' are
kept); an interface does not list Object's methods.\n"
  ^{:tag (slice reflMember)} [^{:tag (* Class)} c]
  (let [(values v ok) (.Load publicMethodsCache c)]
    (when ok
      (return (assert (slice reflMember) v))))
  (let [^{:tag (map string int)} index (make (map string int))
        ^{:tag (slice (slice reflMember))} lists nil
        merge (fn [^reflMember e]
                (let [k (+ (.-Name (.-m e)) (paramList (.-Params (.-m e))))
                      (values i ok) (aget index k)]
                  (when (not ok)
                    (aset index k (len lists))
                    (set! lists (append lists (lit (slice reflMember) e)))
                    (return))
                  (aset lists i (mergeMethod (aget lists i) e))))]
    (when (and (!= (.-Kind (.-info c)) KindPrimitive) (!= (.-Kind (.-info c)) KindArray))
      (range [i _ (.-Methods (.-info c))]
        (let [m (addr (aget (.-Methods (.-info c)) i))]
          (when (!= (bit-and (.-Modifiers m) AccPublic) 0)
            (merge (lit reflMember :decl c :m m))))))
    (let [s (.-Super (.-info c))]
      (when (!= s nil)
        (range [_ e (.publicMethods s)]
          (merge e))))
    (range [_ i (.-Interfaces (.-info c))]
      (when (!= (.-Kind (.-info c)) KindArray)
        (range [_ e (.publicMethods i)]
          (when (== (bit-and (.-Modifiers (.-m e)) AccStatic) 0)
            (merge e)))))
    (let [^{:tag (slice reflMember)} out nil]
      (range [_ l lists]
        (set! out (append out (spread l))))
      (let [(values v _) (.LoadOrStore publicMethodsCache c out)]
        (assert (slice reflMember) v)))))

(go/func mergeMethod
  "mergeMethod is PublicMethods.MethodList.merge: l holds the methods of one name and
parameter list.\n"
  ^{:tag (slice reflMember)} [^{:tag (slice reflMember)} l ^reflMember e]
  (let [dc (.-decl e)
        rt (.-Return (.-m e))
        ^{:tag (slice reflMember)} out nil]
    (range [_ x l]
      (if (!= (.-Return (.-m x)) rt)
        (set! out (append out x))
        (let [xc (.-decl x)]
          (if (== (.IsInterface__Z dc) (.IsInterface__Z xc))
            (do
              (when (.assignableFrom dc xc)
                (return l))
              (when (not (.assignableFrom xc dc))
                (set! out (append out x))))
            (when (.IsInterface__Z dc)
              (return l))))))
    (append out e)))

(go/method declaredMethodInfo
  "declaredMethodInfo: the entry of the method name(params) in c's own table, nil if none.\n"
  ^{:tag (* MethodInfo)} [^{:tag (* Class)} c ^string name ^{:tag (slice (* Class))} params ^bool publicOnly]
  (range [i _ (.-Methods (.-info c))]
    (let [m (addr (aget (.-Methods (.-info c)) i))]
      (when (and (== (.-Name m) name) (sameParams (.-Params m) params)
                 (or (not publicOnly) (!= (bit-and (.-Modifiers m) AccPublic) 0)))
        (return m))))
  nil)

(go/method GetMethods__Method1
  "GetMethods__Method1 is Class.getMethods: a new Method per member.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [ms (.publicMethods c)
        a (NewRefArray Method_class (conv int32 (len ms)))]
    (range [i e ms]
      (aset (.-A a) i (newMethod (.-decl e) (.-m e))))
    a))

(go/method GetDeclaredMethods__Method1
  "GetDeclaredMethods__Method1 is Class.getDeclaredMethods: the methods of the class's own
member table (all its methods when the table lists its declared members, C2G-SPEC §16 Q13;
else its public ones).\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [ms (.-Methods (.-info c))
        a (NewRefArray Method_class (conv int32 (len ms)))]
    (range [i _ ms]
      (aset (.-A a) i (newMethod c (addr (aget ms i)))))
    a))

(go/func paramsOf
  "paramsOf: the classes of a Class[] argument (null as no parameters).\n"
  ^{:tag (slice (* Class))} [^{:tag (* RefArray)} a]
  (when (== a nil)
    (return nil))
  (let [ps (make (slice (* Class)) (len (.-A a)))]
    (range [i x (.-A a)]
      (when (!= x nil)
        (aset ps i (assert (* Class) x))))
    ps))

(go/method methodToString
  "methodToString is Class.methodToString: C.name(T1,T2), the text of NoSuchMethodException.\n"
  ^{:tag (* String)} [^{:tag (* Class)} c ^string name ^{:tag (slice (* Class))} params]
  (let [ns (make (slice string) (len params))]
    (range [i p params]
      (if (== p nil)
        (aset ns i "null")
        (aset ns i (.-Name (.-info p)))))
    (Str (+ (.-Name (.-info c)) "." name "(" (strings/Join ns ",") ")"))))

(go/method GetMethod_String_Class1__Method
  "GetMethod_String_Class1__Method is Class.getMethod: the public member method of that name
and those parameter classes (the declaring class's, its superclasses', its superinterfaces'
non-static ones); of several with different return types the one whose return type is most
specific; NoSuchMethodException otherwise.\n"
  ^{:tag (* Method)} [^{:tag (* Class)} c ^{:tag (* String)} name ^{:tag (* RefArray)} types]
  (let [n (.String (NN name))
        ps (paramsOf types)
        ^{:tag (* reflMember)} best nil]
    (range [i e (.publicMethods c)]
      (when (and (== (.-Name (.-m e)) n) (sameParams (.-Params (.-m e)) ps))
        (when (or (== best nil) (.assignableFrom (.-Return (.-m best)) (.-Return (.-m e))))
          (set! best (addr (aget (.publicMethods c) i))))))
    (when (== best nil)
      (panic (NoSuchMethodException_New_String (.methodToString c n ps))))
    (newMethod (.-decl @best) (.-m @best))))

(go/method GetDeclaredMethod_String_Class1__Method
  "GetDeclaredMethod_String_Class1__Method is Class.getDeclaredMethod over the class's own
table.\n"
  ^{:tag (* Method)} [^{:tag (* Class)} c ^{:tag (* String)} name ^{:tag (* RefArray)} types]
  (let [n (.String (NN name))
        ps (paramsOf types)
        m (.declaredMethodInfo c n ps false)]
    (when (== m nil)
      (panic (NoSuchMethodException_New_String (.methodToString c n ps))))
    (newMethod c m)))

;; fields

(go/method publicFields
  "publicFields computes Class.getFields' list once per class: the public fields the class
declares, then those of its superinterfaces (recursively, each interface once), then its
superclass's.\n"
  ^{:tag (slice reflMember)} [^{:tag (* Class)} c]
  (let [(values v ok) (.Load publicFieldsCache c)]
    (when ok
      (return (assert (slice reflMember) v))))
  (let [^{:tag (slice reflMember)} out nil
        ^{:tag (map (* Class) bool)} seen (make (map (* Class) bool))
        ^{:tag (func [(* Class)])} walk nil]
    (set! walk
          (fn [^{:tag (* Class)} k]
            (when (aget seen k)
              (return))
            (aset seen k true)
            (range [i _ (.-Fields (.-info k))]
              (let [f (addr (aget (.-Fields (.-info k)) i))]
                (when (!= (bit-and (.-Modifiers f) AccPublic) 0)
                  (set! out (append out (lit reflMember :decl k :f f))))))
            (range [_ s (.-Interfaces (.-info k))]
              (walk s))))
    (when (and (!= (.-Kind (.-info c)) KindPrimitive) (!= (.-Kind (.-info c)) KindArray))
      (walk c)
      (when (!= (.-Super (.-info c)) nil)
        (range [_ e (.publicFields (.-Super (.-info c)))]
          (when (not (aget seen (.-decl e)))
            (set! out (append out e))))))
    (let [(values v _) (.LoadOrStore publicFieldsCache c out)]
      (assert (slice reflMember) v))))

(go/method GetFields__Field1 "GetFields__Field1 is Class.getFields.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [fs (.publicFields c)
        a (NewRefArray Field_class (conv int32 (len fs)))]
    (range [i e fs]
      (aset (.-A a) i (newField (.-decl e) (.-f e))))
    a))

(go/method GetField_String__Field
  "GetField_String__Field is Class.getField: the first public field of that name in
getFields' order; NoSuchFieldException(name) otherwise.\n"
  ^{:tag (* Field)} [^{:tag (* Class)} c ^{:tag (* String)} name]
  (let [n (.String (NN name))]
    (range [_ e (.publicFields c)]
      (when (== (.-Name (.-f e)) n)
        (return (newField (.-decl e) (.-f e)))))
    (panic (NoSuchFieldException_New_String name))))

(go/method GetDeclaredFields__Field1 "GetDeclaredFields__Field1 is Class.getDeclaredFields.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [fs (.-Fields (.-info c))
        a (NewRefArray Field_class (conv int32 (len fs)))]
    (range [i _ fs]
      (aset (.-A a) i (newField c (addr (aget fs i)))))
    a))

(go/method GetDeclaredField_String__Field
  "GetDeclaredField_String__Field is Class.getDeclaredField over the class's own table.\n"
  ^{:tag (* Field)} [^{:tag (* Class)} c ^{:tag (* String)} name]
  (let [n (.String (NN name))]
    (range [i _ (.-Fields (.-info c))]
      (when (== (.-Name (aget (.-Fields (.-info c)) i)) n)
        (return (newField c (addr (aget (.-Fields (.-info c)) i))))))
    (panic (NoSuchFieldException_New_String name))))

;; constructors

(go/method ctors ^{:tag (* RefArray)} [^{:tag (* Class)} c ^bool publicOnly]
  (let [^{:tag (slice any)} ks nil]
    (when (and (!= (.-Kind (.-info c)) KindPrimitive) (!= (.-Kind (.-info c)) KindArray)
               (!= (.-Kind (.-info c)) KindInterface) (!= (.-Kind (.-info c)) KindAnnotation))
      (range [i _ (.-Ctors (.-info c))]
        (let [k (addr (aget (.-Ctors (.-info c)) i))]
          (when (or (not publicOnly) (!= (bit-and (.-Modifiers k) AccPublic) 0))
            (set! ks (append ks (newConstructor c k)))))))
    (let [a (NewRefArray Constructor_class (conv int32 (len ks)))]
      (copy (.-A a) ks)
      a)))

(go/method GetConstructors__Constructor1 "GetConstructors__Constructor1 is Class.getConstructors.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (.ctors c true))

(go/method GetDeclaredConstructors__Constructor1
  "GetDeclaredConstructors__Constructor1 is Class.getDeclaredConstructors.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (.ctors c false))

(go/method findCtor ^{:tag (* Constructor)} [^{:tag (* Class)} c ^{:tag (* RefArray)} types ^bool publicOnly]
  (let [ps (paramsOf types)]
    (range [_ k (.-A (.ctors c publicOnly))]
      (when (sameParams (.-params (assert (* Constructor) k)) ps)
        (return (assert (* Constructor) k))))
    (panic (NoSuchMethodException_New_String (.methodToString c "<init>" ps)))))

(go/method GetConstructor_Class1__Constructor
  "GetConstructor_Class1__Constructor is Class.getConstructor: NoSuchMethodException
C.<init>(T1,T2) when no public constructor takes those classes.\n"
  ^{:tag (* Constructor)} [^{:tag (* Class)} c ^{:tag (* RefArray)} types]
  (.findCtor c types true))

(go/method GetDeclaredConstructor_Class1__Constructor
  "GetDeclaredConstructor_Class1__Constructor is Class.getDeclaredConstructor.\n"
  ^{:tag (* Constructor)} [^{:tag (* Class)} c ^{:tag (* RefArray)} types]
  (.findCtor c types false))

;; ---------------------------------------------------------------------------------------
;; Class: the other reflective methods

(go/method Is_Type [^{:tag (* Class)} c])

(go/method GetTypeName__String "GetTypeName__String is Class.getTypeName (int[], java.lang.String[][]).\n"
  ^{:tag (* String)} [^{:tag (* Class)} c]
  (Str (typeName c)))

(go/method GetDeclaringClass__Class
  "GetDeclaringClass__Class is Class.getDeclaringClass: the enclosing class of a member class
(ClassInfo.Declaring), else null.\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c]
  (.-Declaring (.-info c)))

(go/method GetEnclosingClass__Class ^{:tag (* Class)} [^{:tag (* Class)} c] (.-Declaring (.-info c)))

(go/method IsMemberClass__Z ^bool [^{:tag (* Class)} c] (!= (.-Declaring (.-info c)) nil))

;; ---- step 5 phase 2B: more of Class's members for the REPL (jdk26u's Class.java)

(go/method AsSubclass_Class__Class "AsSubclass_Class__Class is Class.asSubclass: this, or
ClassCastException with this class's toString.\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c ^{:tag (* Class)} sup]
  (when (.IsAssignableFrom_Class__Z (NN sup) c)
    (return c))
  (panic (ClassCastException_New_String (.ToString__String c))))

(go/method ComponentType__Class "ComponentType__Class is Class.componentType: an array's
component type, else null.\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c]
  (.-comp c))

(go/method ArrayType__Class "ArrayType__Class is Class.arrayType: the class of arrays of c;
UnsupportedOperationException for void.\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c]
  (when (== c Prim_void)
    (panic (UnsupportedOperationException_New)))
  (.ArrayClass c))

(go/func descriptorOf ^string [^{:tag (* Class)} c]
  (cond
    (.IsArray__Z c) (return (+ "[" (descriptorOf (.-comp c))))
    (.IsPrimitive__Z c)
    (switch (.-Name (.-info c))
      (case ["boolean"] (return "Z")) (case ["byte"] (return "B")) (case ["char"] (return "C"))
      (case ["short"] (return "S")) (case ["int"] (return "I")) (case ["long"] (return "J"))
      (case ["float"] (return "F")) (case ["double"] (return "D")) (default (return "V"))))
  (let [b (conv (slice byte) (.-Name (.-info c)))]
    (range [i x b]
      (when (== x \.)
        (aset b i \/)))
    (+ "L" (conv string b) ";")))

(go/method DescriptorString__String "DescriptorString__String is Class.descriptorString (I,
[Ljava/lang/String;).\n"
  ^{:tag (* String)} [^{:tag (* Class)} c]
  (Str (descriptorOf c)))

(go/method IsAnonymousClass__Z "IsAnonymousClass__Z: false; the closed world's classes are named
(an anonymous class's ClassInfo says nothing of it).\n"
  ^bool [^{:tag (* Class)} c] false)
(go/method IsLocalClass__Z ^bool [^{:tag (* Class)} c] false)
(go/method IsHidden__Z ^bool [^{:tag (* Class)} c] false)
(go/method IsSealed__Z ^bool [^{:tag (* Class)} c] false)
(go/method GetEnclosingMethod__Method ^{:tag (* Method)} [^{:tag (* Class)} c] nil)
(go/method GetEnclosingConstructor__Constructor ^{:tag (* Constructor)} [^{:tag (* Class)} c] nil)

(go/method GetNestHost__Class "GetNestHost__Class is Class.getNestHost: the outermost enclosing
class (arrays and primitives: themselves).\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c]
  (let [h c]
    (while (!= (.-Declaring (.-info h)) nil)
      (set! h (.-Declaring (.-info h))))
    h))

(go/func Class_ForPrimitiveName_String__Class "Class_ForPrimitiveName_String__Class is
Class.forPrimitiveName: the primitive class of that name (void included), else null.\n"
  ^{:tag (* Class)} [^{:tag (* String)} name]
  (Class_GetPrimitiveClass_String__Class name))

(go/method NewInstance__O "NewInstance__O is the deprecated Class.newInstance: the nullary
constructor's newInstance, with InstantiationException when there is none and the
constructor's exception rethrown as is.\n"
  ^any [^{:tag (* Class)} c]
  (let [^{:tag (* Constructor)} k nil
        exc (runCatching (fn [] (set! k (.findCtor c (NewRefArray Class_class 0) false))))]
    (when (!= exc nil)
      (let [ie (InstantiationException_New_String (.GetName__String c))]
        (.InitCause_Throwable__Throwable ie exc)
        (panic ie)))
    (let [^any r nil
          exc2 (runCatching (fn [] (set! r (.NewInstance_O1__O k (NewRefArray Object_class 0)))))]
      (when (!= exc2 nil)
        (let [(values ite ok) (assert (* InvocationTargetException) exc2)]
          (when ok
            (panic (.GetTargetException__Throwable ite))))
        (panic exc2))
      r)))

(go/method GetPackageName__String
  "GetPackageName__String is Class.getPackageName: java.lang for int and int[] as for
Object.\n"
  ^{:tag (* String)} [^{:tag (* Class)} c]
  (while (.IsArray__Z c)
    (set! c (.-comp c)))
  (when (.IsPrimitive__Z c)
    (return (Intern "java.lang")))
  (let [n (.-Name (.-info c))
        i (strings/LastIndexByte n \.)]
    (when (< i 0)
      (return (Intern "")))
    (Str (subslice n _ i))))

(go/method GetCanonicalName__String
  "GetCanonicalName__String is Class.getCanonicalName for top-level, member and array
classes ($ of member classes as .).\n"
  ^{:tag (* String)} [^{:tag (* Class)} c]
  (when (.IsArray__Z c)
    (let [e (.GetCanonicalName__String (.-comp c))]
      (when (== e nil)
        (return nil))
      (return (Concat e (Str "[]")))))
  (when (!= (.-Declaring (.-info c)) nil)
    (let [o (.GetCanonicalName__String (.-Declaring (.-info c)))]
      (when (== o nil)
        (return nil))
      (return (Concat o (Str ".") (.GetSimpleName__String c)))))
  (.GetName__String c))

(go/method IsSynthetic__Z ^bool [^{:tag (* Class)} c] (!= (bit-and (.-Modifiers (.-info c)) AccSynthetic) 0))

(go/method IsAnnotationPresent_Class__Z
  "IsAnnotationPresent_Class__Z: annotations are not kept (C2G-SPEC §12), with one
exception the evaluator needs (Compiler.FISupport): FunctionalInterface is present exactly on
the interfaces whose ClassInfo has FromFn.\n"
  ^bool [^{:tag (* Class)} c ^{:tag (* Class)} a]
  (and (== (.-Name (.-info (NN a))) "java.lang.FunctionalInterface") (!= (.-FromFn (.-info c)) nil)))

(go/method GetGenericInterfaces__Type1
  "GetGenericInterfaces__Type1 is Class.getGenericInterfaces without generics: the
interfaces as Class objects (generic types are cut, so HashMap.comparableClassFor finds no
ParameterizedType).\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [is (.-Interfaces (.-info c))
        a (NewRefArray Type_class (conv int32 (len is)))]
    (range [i x is]
      (aset (.-A a) i x))
    a))

(go/method GetGenericSuperclass__Type ^Type [^{:tag (* Class)} c]
  (let [s (.-Super (.-info c))]
    (when (== s nil)
      (return nil))
    s))

(go/method GetEnumConstants__O1
  "GetEnumConstants__O1 is Class.getEnumConstants: a copy of ClassInfo.Enum's array, null for
a class that is not an enum.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (when (== (.-Enum (.-info c)) nil)
    (return nil))
  (when (!= (.-Init (.-info c)) nil)
    ((.-Init (.-info c))))
  (.Copy ((.-Enum (.-info c)))))

(go/method GetClassLoader__ClassLoader
  "GetClassLoader__ClassLoader is Class.getClassLoader: null (the bootstrap loader) for
primitive types and the JDK's classes (java., javax., jdk., sun.), the application loader
otherwise; arrays have their component's.\n"
  ^ClassLoader_I [^{:tag (* Class)} c]
  (when (== (classPlace c) "module java.base of loader 'bootstrap'")
    (return nil))
  appLoader)

(go/func Class_ForName_String_Z_ClassLoader__Class
  "Class_ForName_String_Z_ClassLoader__Class is Class.forName(name, initialize, loader): the
registry's class (the loader is not consulted), initialized when asked.\n"
  ^{:tag (* Class)} [^{:tag (* String)} name ^bool initialize ^ClassLoader_I loader]
  (let [c (ForName (.String (NN name)))]
    (when (or (== c nil) (.IsPrimitive__Z c))
      (panic (ClassNotFoundException_New_String name)))
    (when (and initialize (!= (.-Init (.-info c)) nil))
      ((.-Init (.-info c))))
    c))

;; ---------------------------------------------------------------------------------------
;; The Class objects of jrt's classes that have only static members (their member tables
;; are in reflect_tables.clj)

(go/var
  [Math_class
   (Define (addr (lit ClassInfo :Name "java.lang.Math" :Kind KindClass :Modifiers (bit-or AccPublic AccFinal)
                      :Super Object_class :Go "arbace/jrt.Math")))]
  [StrictMath_class
   (Define (addr (lit ClassInfo :Name "java.lang.StrictMath" :Kind KindClass :Modifiers (bit-or AccPublic AccFinal)
                      :Super Object_class :Go "arbace/jrt.StrictMath")))]
  [StringLatin1_class
   (Define (addr (lit ClassInfo :Name "java.lang.StringLatin1" :Kind KindClass :Modifiers AccFinal
                      :Super Object_class :Go "arbace/jrt.StringLatin1")))]
  [StringUTF16_class
   (Define (addr (lit ClassInfo :Name "java.lang.StringUTF16" :Kind KindClass :Modifiers AccFinal
                      :Super Object_class :Go "arbace/jrt.StringUTF16")))]
  [ThrowableTracer_class
   (Define (addr (lit ClassInfo :Name "jdk.internal.event.ThrowableTracer" :Kind KindClass :Modifiers AccPublic
                      :Super Object_class :Go "arbace/jrt.ThrowableTracer")))]
  [Utils_class
   (Define (addr (lit ClassInfo :Name "jdk.internal.foreign.Utils" :Kind KindClass :Modifiers (bit-or AccPublic AccFinal)
                      :Super Object_class :Go "arbace/jrt.Utils")))])

;; Math's and StrictMath's constants (static final fields, §6.1)
(go/const
  [^{:tag float64 :val 3.141592653589793 :doc "Math_PI is Math.PI.\n"} Math_PI 3.141592653589793]
  [^{:tag float64 :val 2.718281828459045} Math_E 2.718281828459045]
  [^{:tag float64 :val 6.283185307179586} Math_TAU 6.283185307179586]
  [^{:tag float64 :val 3.141592653589793} StrictMath_PI 3.141592653589793]
  [^{:tag float64 :val 2.718281828459045} StrictMath_E 2.718281828459045]
  [^{:tag float64 :val 6.283185307179586} StrictMath_TAU 6.283185307179586])
