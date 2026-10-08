;; jrt: Class objects, the class registry, class initialization (C2G-SPEC §5.11, §6.2).
(in-ns 'go.arbace.jrt)

(go/file "class.go"
  :imports [[reflect "reflect"] [runtime "runtime"] [strings "strings"] [sync "sync"]
            [atomic "sync/atomic"]])

;; ---------------------------------------------------------------------------------------
;; ClassInfo: what c2g's c2g_classes.go registers per class (§5.11)

(go/type Kind "Kind is the kind of a class (ClassInfo.Kind).\n" uint8)

(go/const
  [^{:tag Kind :val 0} KindClass iota]
  [^{:val 1} KindInterface]
  [^{:val 2} KindEnum]
  [^{:val 3} KindRecord]
  [^{:val 4} KindAnnotation]
  [^{:val 5} KindArray]
  [^{:val 6} KindPrimitive])

(go/const
  [^{:tag int32 :val 1} AccPublic 0x0001]
  [^{:tag int32 :val 2} AccPrivate 0x0002]
  [^{:tag int32 :val 4} AccProtected 0x0004]
  [^{:tag int32 :val 8} AccStatic 0x0008]
  [^{:tag int32 :val 16} AccFinal 0x0010]
  [^{:tag int32 :val 32} AccSynchronized 0x0020]
  [^{:tag int32 :val 64} AccVolatile 0x0040]
  [^{:tag int32 :val 64} AccBridge 0x0040]
  [^{:tag int32 :val 128} AccTransient 0x0080]
  [^{:tag int32 :val 128} AccVarargs 0x0080]
  [^{:tag int32 :val 256} AccNative 0x0100]
  [^{:tag int32 :val 512} AccInterface 0x0200]
  [^{:tag int32 :val 1024} AccAbstract 0x0400]
  [^{:tag int32 :val 4096} AccSynthetic 0x1000]
  [^{:tag int32 :val 8192} AccAnnotation 0x2000]
  [^{:tag int32 :val 16384} AccEnum 0x4000])

(go/type ClassInfo
  "ClassInfo describes a class of the closed world (C2G-SPEC §5.11). c2g's c2g_classes.go
registers one per class with Define at Go package initialization, giving Name, Modifiers,
Kind, Super, Interfaces, Declaring, Simple and Go; the fields holding functions and member
tables (IsInstance, Init, Enum, FromFn, Fields, Methods, Ctors) may refer to class variables
of their own package and are therefore set in an init function, through Info (Go rejects
initialization cycles that Java's classes have). Reflection's use of the member tables is
phase 2's (JRT-NOTES.md).\n"
  (struct
    ^{:tag string :doc "the binary name, \"arbace.lang.PersistentVector$Node\"\n"} Name
    ^{:tag int32 :doc "java.lang.reflect.Modifier bits\n"} Modifiers
    ^Kind Kind
    ^{:tag (* Class) :doc "the superclass; nil for Object, interfaces, primitive types\n"} Super
    ^{:tag (slice (* Class)) :doc "the direct superinterfaces, in declaration order\n"} Interfaces
    ^{:tag (* Class) :doc "the enclosing class of a member class\n"} Declaring
    ^{:tag string :doc "the simple name, when it is not the binary name's last part\n"} Simple
    ^{:tag string :doc "the Go type's qualified name, \"arbace/lang.PersistentVector_Node\",
for stack traces (JRT-NOTES.md, \"Stack traces\")\n"} Go
    ^{:tag (slice FieldInfo)} Fields
    ^{:tag (slice MethodInfo)} Methods
    ^{:tag (slice CtorInfo)} Ctors
    ^{:tag (func [any] [bool]) :doc "C_InstanceOf\n"} IsInstance
    ^{:tag (func []) :doc "C_Init, for Class.forName(name, true, ...)\n"} Init
    ^{:tag (func [] [(* RefArray)]) :doc "the enum constants in order\n"} Enum
    ^{:tag (func [any] [any]) :doc "for a functional interface: wraps an IFn into its adapter\n"} FromFn))

(go/type FieldInfo
  "FieldInfo is one field of a member table (§5.11); Get and Set box and unbox as
Field.get/set do.\n"
  (struct ^string Name ^{:tag (* Class)} Type ^int32 Modifiers
          ^{:tag (func [any] [any])} Get ^{:tag (func [any any])} Set))

(go/type MethodInfo
  "MethodInfo is one method of a member table (§5.11): Invoke calls it (virtually for an
instance method) with boxed arguments and returns its boxed result (nil for void).\n"
  (struct ^string Name ^{:tag (slice (* Class))} Params ^{:tag (* Class)} Return ^int32 Modifiers
          ^{:tag (func [any (slice any)] [any])} Invoke))

(go/type CtorInfo "CtorInfo is one constructor of a member table (§5.11).\n"
  (struct ^{:tag (slice (* Class))} Params ^int32 Modifiers ^{:tag (func [(slice any)] [any])} New))

;; ---------------------------------------------------------------------------------------
;; Class

(go/type Class
  "Class is java.lang.Class (final: *Class). Classes are created by Define (the closed world),
by ArrayClass (array classes, on demand) and for the primitive types (Prim_int ...).\n"
  (struct Object
          ^{:tag (* ClassInfo)} info
          ^{:tag (* String)} name
          ^{:tag (* Class)} comp
          ^byte desc
          ^{:tag (atomic/Pointer Class)} array))

(go/var ^{:tag sync/RWMutex} registryMu)
(go/var ^{:tag (map string (* Class))} registry (make (map string (* Class))))

(go/func Define
  "Define registers a class of the closed world under its binary name and returns its Class
object. A second class of the same name is an error.\n"
  ^{:tag (* Class)} [^{:tag (* ClassInfo)} info]
  (let [c (addr (lit Class :info info))]
    (.Lock registryMu)
    (when (!= (aget registry (.-Name info)) nil)
      (.Unlock registryMu)
      (panic (+ "jrt.Define: two classes named " (.-Name info))))
    (aset registry (.-Name info) c)
    (.Unlock registryMu)
    c))

(go/method Info "Info returns the class's ClassInfo (for attaching member tables at init).\n"
  ^{:tag (* ClassInfo)} [^{:tag (* Class)} c]
  (.-info c))

(go/func ForName
  "ForName looks a class up by its binary name, or an array class by its JVM name
(\"[I\", \"[Ljava.lang.String;\"), as Class.forName names them; nil when the closed world has
no such class (V9).\n"
  ^{:tag (* Class)} [^string name]
  (when (strings/HasPrefix name "[")
    (let [(values c rest) (parseArrayName name)]
      (when (or (== c nil) (!= rest ""))
        (return nil))
      (return c)))
  (.RLock registryMu)
  (let [c (aget registry name)]
    (.RUnlock registryMu)
    c))

(go/func parseArrayName [^string s] :results [^{:tag (* Class)} c ^string rest]
  (when (== (len s) 0)
    (return nil ""))
  (switch (aget s 0)
    (case [\[]
      (let [(values e r) (parseArrayName (subslice s 1))]
        (when (== e nil)
          (return nil ""))
        (return (.ArrayClass e) r)))
    (case [\L]
      (let [i (strings/IndexByte s \;)]
        (when (< i 0)
          (return nil ""))
        (.RLock registryMu)
        (let [e (aget registry (subslice s 1 i))]
          (.RUnlock registryMu)
          (return e (subslice s (+ i 1))))))
    (default
      (range [_ p primitives]
        (when (and (== (.-desc p) (aget s 0)) (!= (.-desc p) \V))
          (return p (subslice s 1))))
      (return nil ""))))

;; the primitive types

(go/func defPrim ^{:tag (* Class)} [^string name ^byte desc]
  (addr (lit Class
             :info (addr (lit ClassInfo :Name name :Kind KindPrimitive
                              :Modifiers (bit-or AccPublic AccFinal AccAbstract)))
             :desc desc)))

(go/var
  [^{:doc "Prim_boolean ... Prim_void are the primitive types' classes (Integer.TYPE ...).\n"}
   Prim_boolean (defPrim "boolean" \Z)]
  [Prim_byte (defPrim "byte" \B)]
  [Prim_char (defPrim "char" \C)]
  [Prim_short (defPrim "short" \S)]
  [Prim_int (defPrim "int" \I)]
  [Prim_long (defPrim "long" \J)]
  [Prim_float (defPrim "float" \F)]
  [Prim_double (defPrim "double" \D)]
  [Prim_void (defPrim "void" \V)])

(go/var ^{:tag (slice (* Class))} primitives
  (lit (slice (* Class)) Prim_boolean Prim_byte Prim_char Prim_short Prim_int Prim_long
       Prim_float Prim_double Prim_void))

(go/func Class_GetPrimitiveClass_String__Class
  "Class_GetPrimitiveClass_String__Class is Class.getPrimitiveClass (package-private).\n"
  ^{:tag (* Class)} [^{:tag (* String)} name]
  (let [n (.String (NN name))]
    (range [_ p primitives]
      (when (== (.-Name (.-info p)) n)
        (return p)))
    nil))

;; the classes of the object model's own types

(go/var Object_class
  (Define (addr (lit ClassInfo :Name "java.lang.Object" :Kind KindClass :Modifiers AccPublic
                     :Go "arbace/jrt.Object"))))

(go/var Class_class
  (Define (addr (lit ClassInfo :Name "java.lang.Class" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.Class"))))

;; array classes (§5.9)

(go/method ArrayClass
  "ArrayClass returns the class of arrays of c (\"[I\", \"[Ljava.lang.String;\"), created on
first use and then shared.\n"
  ^{:tag (* Class)} [^{:tag (* Class)} c]
  (let [a (.Load (.-array c))]
    (when (!= a nil)
      (return a))
    (let [name ""]
      (cond
        (== (.-Kind (.-info c)) KindArray) (set! name (+ "[" (.-Name (.-info c))))
        (== (.-Kind (.-info c)) KindPrimitive) (set! name (+ "[" (conv string (conv rune (.-desc c)))))
        :else (set! name (+ "[L" (.-Name (.-info c)) ";")))
      (let [mods (bit-or (bit-and (.GetModifiers__I c) (bit-or AccPublic AccPrivate AccProtected))
                         AccAbstract AccFinal)
            n (addr (lit Class
                         :info (addr (lit ClassInfo :Name name :Kind KindArray :Modifiers mods
                                          :Super Object_class
                                          :Interfaces (lit (slice (* Class)) Cloneable_class Serializable_class)))
                         :comp c))]
        (when (.CompareAndSwap (.-array c) nil n)
          (return n))
        (.Load (.-array c))))))

;; ---------------------------------------------------------------------------------------
;; Class's Java methods

(go/method Ref ^any [^{:tag (* Class)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Class)} t] Class_class)
(go/method Clone__O ^any [^{:tag (* Class)} t] (panic (CloneNotSupported t)))
(go/method Is_Serializable [^{:tag (* Class)} t])

(go/method GoName "GoName is the binary name as a Go string.\n"
  ^string [^{:tag (* Class)} c]
  (.-Name (.-info c)))

(go/method GetName__String "GetName__String is Class.getName (one String object per class).\n"
  ^{:tag (* String)} [^{:tag (* Class)} c]
  (when (== (.-name c) nil)
    (set! (.-name c) (Intern (.-Name (.-info c)))))
  (.-name c))

(go/method ToString__String ^{:tag (* String)} [^{:tag (* Class)} c]
  (cond
    (.IsPrimitive__Z c) (return (.GetName__String c))
    (.IsInterface__Z c) (return (Str (+ "interface " (.-Name (.-info c)))))
    :else (return (Str (+ "class " (.-Name (.-info c)))))))

(go/method GetSimpleName__String ^{:tag (* String)} [^{:tag (* Class)} c]
  (when (!= (.-comp c) nil)
    (return (Concat (.GetSimpleName__String (.-comp c)) (Str "[]"))))
  (when (!= (.-Simple (.-info c)) "")
    (return (Str (.-Simple (.-info c)))))
  (let [n (.-Name (.-info c))
        i (strings/LastIndexAny n ".$")]
    (Str (subslice n (+ i 1)))))

(go/method IsArray__Z ^bool [^{:tag (* Class)} c] (== (.-Kind (.-info c)) KindArray))
(go/method IsPrimitive__Z ^bool [^{:tag (* Class)} c] (== (.-Kind (.-info c)) KindPrimitive))
(go/method IsInterface__Z ^bool [^{:tag (* Class)} c]
  (or (== (.-Kind (.-info c)) KindInterface) (== (.-Kind (.-info c)) KindAnnotation)))
(go/method IsEnum__Z ^bool [^{:tag (* Class)} c] (== (.-Kind (.-info c)) KindEnum))
(go/method IsRecord__Z ^bool [^{:tag (* Class)} c] (== (.-Kind (.-info c)) KindRecord))
(go/method IsAnnotation__Z ^bool [^{:tag (* Class)} c] (== (.-Kind (.-info c)) KindAnnotation))
(go/method GetModifiers__I ^int32 [^{:tag (* Class)} c] (.-Modifiers (.-info c)))
(go/method GetComponentType__Class ^{:tag (* Class)} [^{:tag (* Class)} c] (.-comp c))
(go/method GetSuperclass__Class ^{:tag (* Class)} [^{:tag (* Class)} c] (.-Super (.-info c)))
(go/method DesiredAssertionStatus__Z
  "DesiredAssertionStatus__Z is false: no class runs with assertions enabled (-ea) yet.\n"
  ^bool [^{:tag (* Class)} c] false)

(go/method GetInterfaces__Class1 ^{:tag (* RefArray)} [^{:tag (* Class)} c]
  (let [is (.-Interfaces (.-info c))
        a (NewRefArray Class_class (conv int32 (len is)))]
    (range [i x is]
      (aset (.-A a) i x))
    a))

(go/method IsInstance_O__Z "IsInstance_O__Z is Class.isInstance.\n"
  ^bool [^{:tag (* Class)} c ^any x]
  (when (== x nil)
    (return false))
  (when (!= (.-IsInstance (.-info c)) nil)
    (return ((.-IsInstance (.-info c)) x)))
  (.IsAssignableFrom_Class__Z c (GetClass x)))

(go/method Cast_O__O "Cast_O__O is Class.cast.\n"
  ^any [^{:tag (* Class)} c ^any x]
  (when (and (!= x nil) (not (.IsInstance_O__Z c x)))
    (panic (ClassCastException_New_String
             (Str (+ "Cannot cast " (.GoName (GetClass x)) " to " (.-Name (.-info c)))))))
  x)

(go/method IsAssignableFrom_Class__Z "IsAssignableFrom_Class__Z is Class.isAssignableFrom.\n"
  ^bool [^{:tag (* Class)} c ^{:tag (* Class)} o]
  (.assignableFrom c (NN o)))

(go/method assignableFrom ^bool [^{:tag (* Class)} c ^{:tag (* Class)} o]
  (cond
    (== c o) (return true)
    (or (.IsPrimitive__Z c) (.IsPrimitive__Z o)) (return false)
    (== c Object_class) (return true)
    (.IsArray__Z o)
    (do
      (when (.IsArray__Z c)
        (let [cc (.-comp c)
              oc (.-comp o)]
          (when (or (.IsPrimitive__Z cc) (.IsPrimitive__Z oc))
            (return (== cc oc)))
          (return (.assignableFrom cc oc))))
      (return (or (== c Cloneable_class) (== c Serializable_class))))
    (.IsArray__Z c) (return false))
  (.inherits o c))

(go/method inherits "inherits: whether c is o or one of its supertypes (o not an array).\n"
  ^bool [^{:tag (* Class)} o ^{:tag (* Class)} c]
  (for [s o] (!= s nil) (set! s (.-Super (.-info s)))
    (when (== s c)
      (return true))
    (range [_ i (.-Interfaces (.-info s))]
      (when (.inherits i c)
        (return true))))
  false)

(go/func Class_ForName_String__Class
  "Class_ForName_String__Class is Class.forName(String): the class, initialized, or
ClassNotFoundException (V9: the closed world's classes only).\n"
  ^{:tag (* Class)} [^{:tag (* String)} name]
  (let [c (ForName (.String (NN name)))]
    (when (or (== c nil) (.IsPrimitive__Z c))
      (panic (ClassNotFoundException_New_String name)))
    (when (!= (.-Init (.-info c)) nil)
      ((.-Init (.-info c))))
    c))

;; ---------------------------------------------------------------------------------------
;; Class initialization (§6.2): JVMS 5.5's procedure

(go/type ClassInit
  "ClassInit is the initialization state of a class with non-trivial initialization
(C_init); C_Init is (when (not (.Done C_init)) (.Run C_init C_clinit)).\n"
  (struct ^{:tag atomic/Uint32} state
          ^{:tag sync/Mutex} mu
          ^{:tag (* sync/Cond)} cond
          ^uint64 owner
          ^Throwable_I err))

(go/const
  [^{:tag uint32 :val 0} initNone iota]
  [^{:val 1} initRunning]
  [^{:val 2} initDone]
  [^{:val 3} initFailed])

(go/method Done "Done reports whether the class is initialized: one atomic load.\n"
  ^bool [^{:tag (* ClassInit)} c]
  (== (.Load (.-state c)) initDone))

(go/method Run
  "Run initializes the class with clinit, its static initializer, as JVMS 5.5 says: done →
return; in progress by the current thread → return (the class is seen half-initialized); in
progress by another thread → wait for it; failed before → NoClassDefFoundError; otherwise run
clinit, and on an exception mark the class failed and throw ExceptionInInitializerError
wrapping it (the exception itself if it is an Error).\n"
  [^{:tag (* ClassInit)} c ^{:tag (func [])} clinit]
  (let [me (currentThreadID)]
    (.Lock (.-mu c))
    (while true
      (switch (.Load (.-state c))
        (case [initDone]
          (.Unlock (.-mu c))
          (return))
        (case [initFailed]
          (let [e (.-err c)]
            (.Unlock (.-mu c))
            (let [ncdfe (NoClassDefFoundError_New_String
                          (Str (+ "Could not initialize class " (clinitClassName clinit))))]
              (.InitCause_Throwable__Throwable ncdfe e)
              (panic ncdfe))))
        (case [initRunning]
          (when (== (.-owner c) me)
            (.Unlock (.-mu c))
            (return))
          (when (== (.-cond c) nil)
            (set! (.-cond c) (sync/NewCond (addr (.-mu c)))))
          (.Wait (.-cond c)))
        (default
          (.Store (.-state c) initRunning)
          (set! (.-owner c) me)
          (.Unlock (.-mu c))
          (let [exc (runCatching clinit)]
            (.Lock (.-mu c))
            (set! (.-owner c) 0)
            (if (== exc nil)
              (.Store (.-state c) initDone)
              (do
                (.Store (.-state c) initFailed)
                (set! (.-err c) exc)))
            (when (!= (.-cond c) nil)
              (.Broadcast (.-cond c)))
            (.Unlock (.-mu c))
            (when (== exc nil)
              (return))
            (when (Error_InstanceOf exc)
              (panic exc))
            (panic (ExceptionInInitializerError_New_Throwable exc))))))))

(go/func runCatching [^{:tag (func [])} f] :results [^Throwable_I exc]
  (defer (Catch (addr exc)))
  (f)
  (return))

(go/func clinitClassName
  "clinitClassName: the Java class whose static initializer clinit is, from its Go name
(\"arbace/lang.Util_clinit\") through the frame table, or its Go name.\n"
  ^string [^{:tag (func [])} clinit]
  (let [f (runtime/FuncForPC (.Pointer (reflect/ValueOf clinit)))]
    (when (== f nil)
      (return "?"))
    (let [name (.Name f)
          (values fr ok) (lookupFrame name)]
      (when ok
        (return (.-Class fr)))
      (strings/TrimSuffix name "_clinit"))))

;; ---------------------------------------------------------------------------------------
;; ClassCastException (§5.7)

(go/func ClassCast
  "ClassCast is the ClassCastException of a failed cast of x to the class to, with the JVM's
message: class X cannot be cast to class Y (X and Y are in module java.base of loader
'bootstrap'), the module and loader as the JVM names them for the JDK's classes (java.base,
'bootstrap') and Arbace's (unnamed module of loader 'app').\n"
  ^Throwable_I [^any x ^{:tag (* Class)} to]
  (let [from (.GoName (GetClass x))
        target (.GoName to)
        df (classPlace (GetClass x))
        dt (classPlace to)
        s (+ "class " from " cannot be cast to class " target " (")]
    (if (== df dt)
      (set! s (+ s from " and " target " are in " df ")"))
      (set! s (+ s from " is in " df "; " target " is in " dt ")")))
    (ClassCastException_New_String (Str s))))

(go/func classPlace ^string [^{:tag (* Class)} c]
  (while (!= (.-comp c) nil)
    (set! c (.-comp c)))
  (let [n (.-Name (.-info c))]
    (when (or (.IsPrimitive__Z c) (strings/HasPrefix n "java.") (strings/HasPrefix n "javax.")
              (strings/HasPrefix n "jdk.") (strings/HasPrefix n "sun."))
      (return "module java.base of loader 'bootstrap'"))
    "unnamed module of loader 'app'"))

(go/func Class_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Class) x)] ok))
(go/func Class_Cast ^{:tag (* Class)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Class) x)]
    (when (not ok) (panic (ClassCast x Class_class)))
    v))

(go/func init
  "init sets ClassInfo.IsInstance of jrt's hand-written classes (§5.11; after Go's variable
initialization, which must not see the cycles).\n"
  []
  (set! (.-IsInstance (.Info Class_class)) Class_InstanceOf)
  (set! (.-IsInstance (.Info String_class)) String_InstanceOf)
  (set! (.-IsInstance (.Info StringBuilder_class)) StringBuilder_InstanceOf)
  (set! (.-IsInstance (.Info StringBuffer_class)) StringBuffer_InstanceOf)
  (set! (.-IsInstance (.Info Throwable_class)) Throwable_InstanceOf)
  (set! (.-IsInstance (.Info Enum_class)) Enum_InstanceOf)
  (set! (.-IsInstance (.Info Record_class)) Record_InstanceOf))
