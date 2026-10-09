;; jrt's tests, phase 2b: reflection over member tables. A small hierarchy written as c2g writes
;; classes and their member tables (C2G-SPEC §5.11; JRT-NOTES.md, "The member tables"), looked
;; up and invoked as arbace.lang.Reflector does; and reflection on jrt's own classes against
;; the JVM (testdata/reflect.txt, test/jrt/testdata_reflect.clj).
(in-ns 'go.arbace.jrt)

(go/file "reflect_test.go"
  :imports [[sort "sort"] [strconv "strconv"] [strings "strings"] [testing "testing"]])

(go/func parseLong ^int64 [^string s] (let [(values v _) (strconv/ParseInt s 10 64)] v))
(go/func parseJavaDouble ^float64 [^string s] (let [(values v _) (strconv/ParseFloat s 64)] v))

;; ---------------------------------------------------------------------------------------
;; test.Shape, test.Base, test.Square, as c2g writes them:
;;
;;   public interface Shape { int SIDES = 0; double area();
;;                            default String describe() { return "shape"; }
;;                            static Shape unit() { return new Square(1.0); } }
;;   public abstract class Base implements Shape {
;;     public int id; public static int count; public Base() {}
;;     public Object get() { return "base"; } public String name() { return "Base"; }
;;     public int over(int x) { return 1; } public long over(long x) { return 2; }
;;     public String over(Object x) { return "O"; } public String over(String x) { return "S"; }
;;     protected void hidden() {} }
;;   public class Square extends Base {
;;     public double side; public final int fixed = 7;
;;     public Square() { this(2.0); } public Square(double s) { if (s < 0) throw new IllegalArgumentException("negative"); side = s; }
;;     public double area() { return side * side; }
;;     public String get() { return "square"; }   // and the bridge Object get()
;;     public String name() { return "Square"; }
;;     public int over(int x) { return 10 + x; }
;;     public static String join(String sep, Object... xs) { ... }
;;     public int boom() { return ((String) null).length(); } }

(go/type testShape (interface Object_I (Is_testShape [])
                     (Area__D ^float64 [])
                     (Describe__String ^{:tag (* String)} [])))
(go/var testShape_class
  (Define (addr (lit ClassInfo :Name "test.Shape" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.testShape"))))
(go/var ^int32 testShape_SIDES 0)
(go/func testShape_Describe__String ^{:tag (* String)} [^testShape this] (Intern "shape"))
(go/func testShape_Unit__testShape ^testShape [] (testSquare_New_D 1.0))
(go/func testShape_InstanceOf ^bool [^any x] (let [(values _ ok) (assert testShape x)] ok))

(go/type testBase (struct Object ^int32 F_id))
(go/type testBase_I (interface testShape
                      (Self_testBase ^{:tag (* testBase)} [])
                      (Get__O ^any [])
                      (Name__String ^{:tag (* String)} [])
                      (Over_I__I ^int32 [^int32 x])
                      (Over_J__J ^int64 [^int64 x])
                      (Over_O__String ^{:tag (* String)} [^any x])
                      (Over_String__String ^{:tag (* String)} [^{:tag (* String)} x])
                      (Hidden__V [])))
(go/var testBase_class
  (Define (addr (lit ClassInfo :Name "test.Base" :Kind KindClass :Modifiers (bit-or AccPublic AccAbstract)
                     :Super Object_class :Interfaces (lit (slice (* Class)) testShape_class)
                     :Go "arbace/jrt.testBase"))))
(go/var ^int32 testBase_count)
(go/method Self_testBase ^{:tag (* testBase)} [^{:tag (* testBase)} t] t)
(go/method Is_testShape [^{:tag (* testBase)} t])
(go/method Ctor [^{:tag (* testBase)} t ^testBase_I this])
(go/method Get__O ^any [^{:tag (* testBase)} t] (Intern "base"))
(go/method Name__String ^{:tag (* String)} [^{:tag (* testBase)} t] (Intern "Base"))
(go/method Over_I__I ^int32 [^{:tag (* testBase)} t ^int32 x] 1)
(go/method Over_J__J ^int64 [^{:tag (* testBase)} t ^int64 x] 2)
(go/method Over_O__String ^{:tag (* String)} [^{:tag (* testBase)} t ^any x] (Intern "O"))
(go/method Over_String__String ^{:tag (* String)} [^{:tag (* testBase)} t ^{:tag (* String)} x] (Intern "S"))
(go/method Hidden__V [^{:tag (* testBase)} t])
(go/method Describe__String ^{:tag (* String)} [^{:tag (* testSquare)} t] (testShape_Describe__String t))
(go/func testBase_InstanceOf ^bool [^any x] (let [(values _ ok) (assert testBase_I x)] ok))

(go/type testSquare (struct testBase ^float64 F_side ^int32 F_fixed))
(go/var testSquare_class
  (Define (addr (lit ClassInfo :Name "test.Square" :Kind KindClass :Modifiers AccPublic
                     :Super testBase_class :Go "arbace/jrt.testSquare"))))
(go/func testSquare_New ^{:tag (* testSquare)} [] (let [t (addr (lit testSquare))] (.Ctor_D t 2.0) t))
(go/func testSquare_New_D ^{:tag (* testSquare)} [^float64 s] (let [t (addr (lit testSquare))] (.Ctor_D t s) t))
(go/method Ctor_D [^{:tag (* testSquare)} t ^float64 s]
  (.Ctor (.-testBase t) t)
  (set! (.-F_fixed t) 7)
  (when (< s 0)
    (panic (IllegalArgumentException_New_String (Str "negative"))))
  (set! (.-F_side t) s))
(go/method Area__D ^float64 [^{:tag (* testSquare)} t] (* (.-F_side t) (.-F_side t)))
(go/method Get__String ^{:tag (* String)} [^{:tag (* testSquare)} t] (Intern "square"))
(go/method Get__O ^any [^{:tag (* testSquare)} t] (.Get__String t))
(go/method Name__String ^{:tag (* String)} [^{:tag (* testSquare)} t] (Intern "Square"))
(go/method Over_I__I ^int32 [^{:tag (* testSquare)} t ^int32 x] (+ 10 x))
(go/method Boom__I ^int32 [^{:tag (* testSquare)} t]
  (let [^{:tag (* String)} s nil]
    (.Length__I s)))
(go/func testSquare_Join_String_O1__String ^{:tag (* String)} [^{:tag (* String)} sep ^{:tag (* RefArray)} xs]
  (let [parts (make (slice string) (len (.-A xs)))]
    (range [i x (.-A xs)]
      (aset parts i (.String (StrOfObj x))))
    (Str (strings/Join parts (.String sep)))))
(go/method Ref ^any [^{:tag (* testSquare)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* testSquare)} t] testSquare_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* testSquare)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* testSquare)} t] (panic (CloneNotSupported t)))
(go/func testSquare_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* testSquare) x)] ok))

(go/var ^{:tag (* Class)} objectArrayClass (.ArrayClass Object_class))

;; the member tables c2g writes (c2g_classes.go), set in init (A1)
(go/func init []
  (set! (.-IsInstance (.Info testShape_class)) testShape_InstanceOf)
  (set! (.-Methods (.Info testShape_class))
        (lit (slice MethodInfo)
             (lit MethodInfo :Name "area" :Return Prim_double :Modifiers (bit-or AccPublic AccAbstract)
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Area__D (assert testShape this))))
             (lit MethodInfo :Name "describe" :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Describe__String (assert testShape this))))
             (lit MethodInfo :Name "unit" :Return testShape_class :Modifiers (bit-or AccPublic AccStatic)
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (testShape_Unit__testShape)))))
  (set! (.-Fields (.Info testShape_class))
        (lit (slice FieldInfo)
             (lit FieldInfo :Name "SIDES" :Type Prim_int :Modifiers (bit-or AccPublic AccStatic AccFinal)
                  :Get (fn ^any [^any o] testShape_SIDES))))
  (set! (.-IsInstance (.Info testBase_class)) testBase_InstanceOf)
  (set! (.-Methods (.Info testBase_class))
        (lit (slice MethodInfo)
             (lit MethodInfo :Name "get" :Return Object_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Get__O (assert testBase_I this))))
             (lit MethodInfo :Name "name" :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Name__String (assert testBase_I this))))
             (lit MethodInfo :Name "over" :Params (lit (slice (* Class)) Prim_int) :Return Prim_int :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Over_I__I (assert testBase_I this) (assert int32 (aget args 0)))))
             (lit MethodInfo :Name "over" :Params (lit (slice (* Class)) Prim_long) :Return Prim_long :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Over_J__J (assert testBase_I this) (assert int64 (aget args 0)))))
             (lit MethodInfo :Name "over" :Params (lit (slice (* Class)) Object_class) :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Over_O__String (assert testBase_I this) (aget args 0))))
             (lit MethodInfo :Name "over" :Params (lit (slice (* Class)) String_class) :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                            (.Over_String__String (assert testBase_I this) ((inst As (* String)) (aget args 0)))))
             (lit MethodInfo :Name "hidden" :Return Prim_void :Modifiers AccProtected
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Hidden__V (assert testBase_I this)) nil))))
  (set! (.-Fields (.Info testBase_class))
        (lit (slice FieldInfo)
             (lit FieldInfo :Name "id" :Type Prim_int :Modifiers AccPublic
                  :Get (fn ^any [^any o] (.-F_id (.Self_testBase (assert testBase_I o))))
                  :Set (fn [^any o ^any v] (set! (.-F_id (.Self_testBase (assert testBase_I o))) (assert int32 v))))
             (lit FieldInfo :Name "count" :Type Prim_int :Modifiers (bit-or AccPublic AccStatic)
                  :Get (fn ^any [^any o] testBase_count)
                  :Set (fn [^any o ^any v] (set! testBase_count (assert int32 v))))))
  (set! (.-Ctors (.Info testBase_class))
        (lit (slice CtorInfo) (lit CtorInfo :Modifiers AccPublic)))
  (set! (.-IsInstance (.Info testSquare_class)) testSquare_InstanceOf)
  (set! (.-Methods (.Info testSquare_class))
        (lit (slice MethodInfo)
             (lit MethodInfo :Name "area" :Return Prim_double :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Area__D (assert (* testSquare) this))))
             (lit MethodInfo :Name "get" :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Get__String (assert (* testSquare) this))))
             (lit MethodInfo :Name "get" :Return Object_class :Modifiers (bit-or AccPublic AccBridge AccSynthetic)
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Get__O (assert (* testSquare) this))))
             (lit MethodInfo :Name "name" :Return String_class :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Name__String (assert (* testSquare) this))))
             (lit MethodInfo :Name "over" :Params (lit (slice (* Class)) Prim_int) :Return Prim_int :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Over_I__I (assert (* testSquare) this) (assert int32 (aget args 0)))))
             (lit MethodInfo :Name "join" :Params (lit (slice (* Class)) String_class objectArrayClass) :Return String_class
                  :Modifiers (bit-or AccPublic AccStatic AccVarargs)
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args]
                            (testSquare_Join_String_O1__String ((inst As (* String)) (aget args 0)) ((inst As (* RefArray)) (aget args 1)))))
             (lit MethodInfo :Name "boom" :Return Prim_int :Modifiers AccPublic
                  :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.Boom__I (assert (* testSquare) this))))))
  (set! (.-Fields (.Info testSquare_class))
        (lit (slice FieldInfo)
             (lit FieldInfo :Name "side" :Type Prim_double :Modifiers AccPublic
                  :Get (fn ^any [^any o] (.-F_side (assert (* testSquare) o)))
                  :Set (fn [^any o ^any v] (set! (.-F_side (assert (* testSquare) o)) (assert float64 v))))
             (lit FieldInfo :Name "fixed" :Type Prim_int :Modifiers (bit-or AccPublic AccFinal)
                  :Get (fn ^any [^any o] (.-F_fixed (assert (* testSquare) o))))))
  (set! (.-Ctors (.Info testSquare_class))
        (lit (slice CtorInfo)
             (lit CtorInfo :Modifiers AccPublic :New (fn ^any [^{:tag (slice any)} args] (testSquare_New)))
             (lit CtorInfo :Params (lit (slice (* Class)) Prim_double) :Modifiers AccPublic
                  :New (fn ^any [^{:tag (slice any)} args] (testSquare_New_D (assert float64 (aget args 0))))))))

;; ---------------------------------------------------------------------------------------
;; Reflector's lookups (arbace/lang/Reflector.clj: getMethods, isCongruent, paramArgTypeMatch,
;; matchMethod; Compiler.subsumes), over jrt's API

(go/func rGetMethods
  "rGetMethods is Reflector.getMethods: the public methods of that name, arity and staticness,
the bridges only when nothing else matches; for an interface, Object's too.\n"
  ^{:tag (slice (* Method))} [^{:tag (* Class)} c ^int arity ^string name ^bool statics]
  (let [^{:tag (slice (* Method))} ms nil
        ^{:tag (slice (* Method))} bridges nil
        pick (fn [^{:tag (* RefArray)} all]
               (range [_ x (.-A all)]
                 (let [m (assert (* Method) x)]
                   (when (and (== (.String (.GetName__String m)) name)
                              (== (!= (bit-and (.GetModifiers__I m) AccStatic) 0) statics)
                              (== (conv int (.GetParameterCount__I m)) arity))
                     (if (.IsBridge__Z m)
                       (set! bridges (append bridges m))
                       (set! ms (append ms m)))))))]
    (pick (.GetMethods__Method1 c))
    (when (== (len ms) 0)
      (set! ms bridges))
    (when (and (not statics) (.IsInterface__Z c))
      (set! bridges nil)
      (pick (.GetMethods__Method1 Object_class)))
    ms))

(go/func rParamArgTypeMatch ^bool [^{:tag (* Class)} p ^{:tag (* Class)} a]
  (cond
    (== a nil) (return (not (.IsPrimitive__Z p)))
    (or (== p a) (.IsAssignableFrom_Class__Z p a)) (return true)
    (== p Prim_int) (return (or (== a Integer_class) (== a Prim_long) (== a Long_class) (== a Prim_short) (== a Prim_byte)))
    (== p Prim_float) (return (or (== a Float_class) (== a Prim_double)))
    (== p Prim_double) (return (or (== a Double_class) (== a Prim_float)))
    (== p Prim_long) (return (or (== a Long_class) (== a Prim_int) (== a Prim_short) (== a Prim_byte)))
    (== p Prim_char) (return (== a Character_class))
    (== p Prim_short) (return (== a Short_class))
    (== p Prim_byte) (return (== a Byte_class))
    (== p Prim_boolean) (return (== a Boolean_class)))
  false)

(go/func rCongruent ^bool [^{:tag (* RefArray)} params ^{:tag (slice any)} args]
  (when (!= (len (.-A params)) (len args))
    (return false))
  (range [i p (.-A params)]
    (let [^{:tag (* Class)} a nil]
      (when (!= (aget args i) nil)
        (set! a (GetClass (aget args i))))
      (when (not (rParamArgTypeMatch (assert (* Class) p) a))
        (return false))))
  true)

(go/func rSubsumes ^bool [^{:tag (* RefArray)} c1 ^{:tag (* RefArray)} c2]
  (let [better false]
    (range [i x (.-A c1)]
      (let [a (assert (* Class) x)
            b (assert (* Class) (aget (.-A c2) i))]
        (when (!= a b)
          (if (or (and (not (.IsPrimitive__Z a)) (.IsPrimitive__Z b)) (.IsAssignableFrom_Class__Z b a))
            (set! better true)
            (return false)))))
    better))

(go/func rMatch ^{:tag (* Method)} [^{:tag (slice (* Method))} ms ^{:tag (slice any)} args]
  (let [^{:tag (* Method)} found nil]
    (range [_ m ms]
      (when (and (rCongruent (.GetParameterTypes__Class1 m) args)
                 (or (== found nil) (rSubsumes (.GetParameterTypes__Class1 m) (.GetParameterTypes__Class1 found))))
        (set! found m)))
    found))

(go/func methodKeys
  "methodKeys: getMethods as sorted DECLARING.name(params)return strings.\n"
  ^string [^{:tag (* Class)} c]
  (let [^{:tag (slice string)} ks nil]
    (range [_ x (.-A (.GetMethods__Method1 c))]
      (let [m (assert (* Method) x)]
        (set! ks (append ks (+ (.GoName (.GetDeclaringClass__Class m)) "." (.String (.GetName__String m))
                               (paramList (.-params m)) (typeName (.GetReturnType__Class m)))))))
    (sort/Strings ks)
    (strings/Join ks " ")))

(go/func objArgs ^{:tag (* RefArray)} [& ^{:tag (slice any)} xs]
  (RefArrayOf Object_class (spread xs)))

(go/func classArgs ^{:tag (* RefArray)} [& ^{:tag (slice any)} cs]
  (RefArrayOf Class_class (spread cs)))

(go/func outcome
  "outcome is f's result as the test files write it (CLASS:TEXT, null), or its exception
(an InvocationTargetException followed by / and its cause).\n"
  ^string [^{:tag (func [] [any])} f]
  (let [^Throwable_I exc nil
        v ((fn ^any [] :results [^any r]
             (defer (Catch (addr exc)))
             (set! r (f))
             (return)))]
    (when (!= exc nil)
      (let [(values ite ok) (assert (* InvocationTargetException) exc)]
        (when ok
          (return (+ (exText exc) " / " (exText (.GetTargetException__Throwable ite))))))
      (return (exText exc)))
    (textOf v)))

(go/func textOf ^string [^any v]
  (when (== v nil)
    (return "null"))
  (let [c (GetClass v)]
    (when (.IsArray__Z c)
      (let [n (conv int (Array_GetLength_O__I v))
            parts (make (slice string) n)]
        (for [i 0] (< i n) (inc! i)
          (aset parts i (esc (.-value (StrOfObj (Array_Get_O_I__O v (conv int32 i)))))))
        (return (+ (.GoName c) ":[" (strings/Join parts ",") "]"))))
    (+ (.GoName c) ":" (esc (.-value (StrOfObj v))))))

(go/func TestReflectLookups [^{:tag (* testing/T)} t]
  ;; getMethods: inherited and interface methods, overrides, bridges, statics of interfaces
  (let [want (+ "java.lang.Object.equals(java.lang.Object)boolean java.lang.Object.getClass()java.lang.Class "
                "java.lang.Object.hashCode()int java.lang.Object.notify()void java.lang.Object.notifyAll()void "
                "java.lang.Object.toString()java.lang.String java.lang.Object.wait()void java.lang.Object.wait(long)void "
                "java.lang.Object.wait(long,int)void test.Base.over(java.lang.Object)java.lang.String "
                "test.Base.over(java.lang.String)java.lang.String test.Base.over(long)long "
                "test.Shape.describe()java.lang.String test.Square.area()double test.Square.boom()int "
                "test.Square.get()java.lang.Object test.Square.get()java.lang.String "
                "test.Square.join(java.lang.String,java.lang.Object[])java.lang.String test.Square.name()java.lang.String "
                "test.Square.over(int)int")]
    (when [got (methodKeys testSquare_class)] (!= got want)
      (.Errorf t "getMethods(Square):\n got %s\nwant %s" got want)))
  (let [want "test.Shape.area()double test.Shape.describe()java.lang.String test.Shape.unit()test.Shape"]
    (when [got (methodKeys testShape_class)] (!= got want)
      (.Errorf t "getMethods(Shape): got %s" got)))
  ;; getMethod: the most specific return type among the bridge and the method
  (let [m (.GetMethod_String_Class1__Method testSquare_class (Str "get") nil)]
    (when (or (!= (.GetReturnType__Class m) String_class) (.IsBridge__Z m))
      (.Errorf t "getMethod(get): %s" (.String (.ToString__String m)))))
  (when [got (res (fn ^string [] (.String (.ToString__String (.GetMethod_String_Class1__Method testSquare_class (Str "unit") nil)))))]
        (!= got "!java.lang.NoSuchMethodException: test.Square.unit()")
    (.Errorf t "getMethod(unit) on Square: %s" got))
  (when [got (.String (.ToString__String (.GetMethod_String_Class1__Method testShape_class (Str "unit") nil)))]
        (!= got "public static test.Shape test.Shape.unit()")
    (.Errorf t "getMethod(unit) on Shape: %s" got))
  (when [got (.String (.ToString__String (.GetMethod_String_Class1__Method testSquare_class (Str "describe") nil)))]
        (!= got "public default java.lang.String test.Shape.describe()")
    (.Errorf t "default method: %s" got))
  (when [got (.String (.ToString__String (.GetMethod_String_Class1__Method testSquare_class (Str "join") (classArgs String_class objectArrayClass))))]
        (!= got "public static java.lang.String test.Square.join(java.lang.String,java.lang.Object[])")
    (.Errorf t "varargs toString: %s" got))
  ;; declared members include the listed protected one
  (let [n 0]
    (range [_ x (.-A (.GetDeclaredMethods__Method1 testBase_class))]
      (when (== (.String (.GetName__String (assert (* Method) x))) "hidden")
        (inc! n)))
    (when (!= n 1)
      (.Error t "getDeclaredMethods(Base) lacks hidden")))
  ;; Reflector: arity, staticness, bridges, overloads by argument classes
  (let [gets (rGetMethods testSquare_class 0 "get" false)]
    (when (or (!= (len gets) 1) (.IsBridge__Z (aget gets 0)))
      (.Errorf t "Reflector.getMethods(get): %d" (len gets))))
  (let [overs (rGetMethods testSquare_class 1 "over" false)]
    (when (!= (len overs) 4)
      (.Errorf t "Reflector.getMethods(over): %d" (len overs)))
    (let [m (rMatch overs (lit (slice any) (Str "s")))]
      (when (or (== m nil) (!= (aget (.-A (.GetParameterTypes__Class1 m)) 0) String_class))
        (.Error t "over(String) for a String")))
    (let [m (rMatch overs (lit (slice any) (Long_ValueOf_J__Long 5)))]
      (when (== m nil)
        (.Error t "no over for a Long"))))
  (when (!= (len (rGetMethods testSquare_class 2 "join" true)) 1)
    (.Error t "Reflector.getMethods(join, statics)"))
  (when (!= (len (rGetMethods testShape_class 0 "hashCode" false)) 1)
    (.Error t "Reflector.getMethods on an interface adds Object's"))
  ;; isVarArgs, isBridge, isSynthetic, getModifiers
  (let [j (.GetMethod_String_Class1__Method testSquare_class (Str "join") (classArgs String_class objectArrayClass))]
    (when (or (not (.IsVarArgs__Z j)) (!= (.GetModifiers__I j) (bit-or AccPublic AccStatic AccVarargs)))
      (.Error t "varargs modifiers")))
  ;; fields: the class's, its superinterfaces', its superclass's
  (let [ns (make (slice string) 0)]
    (range [_ x (.-A (.GetFields__Field1 testSquare_class))]
      (set! ns (append ns (.String (.GetName__String (assert (* Field) x))))))
    (when (!= (strings/Join ns ",") "side,fixed,id,count,SIDES")
      (.Errorf t "getFields(Square): %v" ns)))
  (when [got (.String (.ToString__String (.GetField_String__Field testSquare_class (Str "fixed"))))]
        (!= got "public final int test.Square.fixed")
    (.Errorf t "Field.toString: %s" got))
  ;; constructors
  (when (!= (len (.-A (.GetConstructors__Constructor1 testSquare_class))) 2)
    (.Error t "getConstructors(Square)"))
  (when [got (.String (.ToString__String (.GetConstructor_Class1__Constructor testSquare_class (classArgs Prim_double))))]
        (!= got "public test.Square(double)")
    (.Errorf t "Constructor.toString: %s" got))
  (when [got (res (fn ^string [] (.String (.ToString__String (.GetConstructor_Class1__Constructor testSquare_class (classArgs Prim_int))))))]
        (!= got "!java.lang.NoSuchMethodException: test.Square.<init>(int)")
    (.Errorf t "getConstructor(int): %s" got))
  ;; Method equality
  (let [a (.GetMethod_String_Class1__Method testSquare_class (Str "area") nil)
        b (.GetMethod_String_Class1__Method testSquare_class (Str "area") nil)]
    (when (or (== a b) (not (.Equals_O__Z a b)) (!= (.HashCode__I a) (.HashCode__I b)))
      (.Error t "Method.equals, hashCode"))))

(go/func TestReflectInvoke [^{:tag (* testing/T)} t]
  (let [sq (testSquare_New_D 3.0)
        cases (lit (slice (struct ^string name ^{:tag (func [] [any])} f ^string want))
                ;; virtual dispatch through Base's and Shape's Method objects
                (lit _ "Base.name on a Square"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testBase_class (Str "name") nil) sq nil))
                     "java.lang.String:Square")
                (lit _ "Shape.area on a Square"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testShape_class (Str "area") nil) sq nil))
                     "java.lang.Double:9.0")
                (lit _ "default method"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "describe") nil) sq (objArgs)))
                     "java.lang.String:shape")
                (lit _ "over(long) with an Integer: widening"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "over") (classArgs Prim_long)) sq (objArgs (Integer_ValueOf_I__Integer 4))))
                     "java.lang.Long:2")
                (lit _ "over(int) with a Short, overridden"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testBase_class (Str "over") (classArgs Prim_int)) sq (objArgs (Short_ValueOf_S__Short 4))))
                     "java.lang.Integer:14")
                (lit _ "over(int) with a Long"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "over") (classArgs Prim_int)) sq (objArgs (Long_ValueOf_J__Long 4))))
                     "!java.lang.IllegalArgumentException: argument type mismatch")
                (lit _ "over(String) with an Integer"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "over") (classArgs String_class)) sq (objArgs (Integer_ValueOf_I__Integer 4))))
                     "!java.lang.IllegalArgumentException: argument type mismatch")
                (lit _ "over(String) with null"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "over") (classArgs String_class)) sq (objArgs nil)))
                     "java.lang.String:S")
                (lit _ "static varargs, receiver ignored"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "join") (classArgs String_class objectArrayClass))
                                                  (Str "ignored") (objArgs (Str "-") (objArgs (Integer_ValueOf_I__Integer 1) (Str "b") nil))))
                     "java.lang.String:1-b-null")
                (lit _ "static method of an interface"
                     (fn ^any [] (GetClass (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testShape_class (Str "unit") nil) nil nil)))
                     "java.lang.Class:class test.Square")
                (lit _ "a receiver of another class"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "area") nil) (Str "x") nil))
                     "!java.lang.IllegalArgumentException: object of type java.lang.String is not an instance of test.Square")
                (lit _ "a null receiver"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "area") nil) nil nil))
                     "!java.lang.NullPointerException")
                (lit _ "a Go run-time error in the method, wrapped"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testSquare_class (Str "boom") nil) sq nil))
                     "!java.lang.reflect.InvocationTargetException / !java.lang.NullPointerException")
                (lit _ "an abstract method's invoker is the interface call"
                     (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method testShape_class (Str "area") nil) (Str "x") nil))
                     "!java.lang.IllegalArgumentException: object of type java.lang.String is not an instance of test.Shape")
                ;; constructors
                (lit _ "newInstance(double) with an Integer"
                     (fn ^any [] (Box (.-F_side (assert (* testSquare) (.NewInstance_O1__O (.GetConstructor_Class1__Constructor testSquare_class (classArgs Prim_double)) (objArgs (Integer_ValueOf_I__Integer 5)))))))
                     "java.lang.Double:5.0")
                (lit _ "a constructor's exception, wrapped"
                     (fn ^any [] (.NewInstance_O1__O (.GetConstructor_Class1__Constructor testSquare_class (classArgs Prim_double)) (objArgs (Double_ValueOf_D__Double -1))))
                     "!java.lang.reflect.InvocationTargetException / !java.lang.IllegalArgumentException: negative")
                (lit _ "an abstract class"
                     (fn ^any [] (.NewInstance_O1__O (.GetConstructor_Class1__Constructor testBase_class nil) nil))
                     "!java.lang.InstantiationException")
                ;; fields
                (lit _ "Field.set widens an Integer to double"
                     (fn ^any [] (let [f (.GetField_String__Field testSquare_class (Str "side"))]
                                   (.Set_O_O__V f sq (Integer_ValueOf_I__Integer 4))
                                   (.Get_O__O f sq)))
                     "java.lang.Double:4.0")
                (lit _ "Field.set of a final field"
                     (fn ^any [] (.Set_O_O__V (.GetField_String__Field testSquare_class (Str "fixed")) sq (Integer_ValueOf_I__Integer 1)) nil)
                     "!java.lang.IllegalAccessException: Can not set final int field test.Square.fixed to java.lang.Integer")
                (lit _ "Field.set with null for a primitive"
                     (fn ^any [] (.Set_O_O__V (.GetField_String__Field testSquare_class (Str "id")) sq nil) nil)
                     "!java.lang.IllegalArgumentException: Can not set int field test.Base.id to null value")
                (lit _ "Field.get on another class"
                     (fn ^any [] (.Get_O__O (.GetField_String__Field testSquare_class (Str "id")) (Str "x")))
                     "!java.lang.IllegalArgumentException: Can not set int field test.Base.id to java.lang.String")
                (lit _ "a static field, the receiver ignored"
                     (fn ^any [] (let [f (.GetField_String__Field testSquare_class (Str "count"))]
                                   (.Set_O_O__V f nil (Byte_ValueOf_B__Byte 9))
                                   (.Get_O__O f (Str "ignored"))))
                     "java.lang.Integer:9")
                (lit _ "an interface's constant"
                     (fn ^any [] (.Get_O__O (.GetField_String__Field testSquare_class (Str "SIDES")) nil))
                     "java.lang.Integer:0"))]
    (range [_ c cases]
      (when [got (outcome (.-f c))] (!= got (.-want c))
        (.Errorf t "%s: got %s, want %s" (.-name c) got (.-want c))))))

(go/func TestBoxing [^{:tag (* testing/T)} t]
  (when (or (!= (Box (conv int32 5)) (Integer_ValueOf_I__Integer 5)) (== (Box (conv int32 500)) (Box (conv int32 500))))
    (.Error t "Box uses Integer's cache"))
  (let [cases (lit (slice (struct ^{:tag (* Class)} to ^any x ^any want ^bool ok))
                (lit _ Prim_long (Integer_ValueOf_I__Integer -3) (conv int64 -3) true)
                (lit _ Prim_double (Character_ValueOf_C__Character 65) (conv float64 65) true)
                (lit _ Prim_float (Long_ValueOf_J__Long 16777217) (conv float32 16777216) true)
                (lit _ Prim_int (Character_ValueOf_C__Character 65) (conv int32 65) true)
                (lit _ Prim_short (Byte_ValueOf_B__Byte -1) (conv int16 -1) true)
                (lit _ Prim_char (Byte_ValueOf_B__Byte 1) nil false)
                (lit _ Prim_byte (Short_ValueOf_S__Short 1) nil false)
                (lit _ Prim_boolean (Boolean_ValueOf_Z__Boolean true) true true)
                (lit _ Prim_int (Boolean_ValueOf_Z__Boolean true) nil false)
                (lit _ Prim_int (Str "1") nil false)
                (lit _ Prim_int nil nil false))]
    (range [i c cases]
      (let [(values v ok) (Unbox (.-to c) (.-x c))]
        (when (or (!= ok (.-ok c)) (!= v (.-want c)))
          (.Errorf t "Unbox case %d: %v %v" i v ok))))))

(go/func TestClassLoaders [^{:tag (* testing/T)} t]
  (let [app (ClassLoader_GetSystemClassLoader__ClassLoader)]
    (when (!= (.LoadClass_String__Class app (Str "java.lang.String")) String_class)
      (.Error t "loadClass"))
    (when (!= (res (fn ^string [] (.String (.GetName__String (.LoadClass_String__Class app (Str "no.Such")))))) "!java.lang.ClassNotFoundException: no.Such")
      (.Error t "loadClass of a missing class"))
    (when (or (!= (.GetClassLoader__ClassLoader String_class) nil) (!= (.GetClassLoader__ClassLoader testSquare_class) app)
              (!= (.GetClassLoader__ClassLoader (.ArrayClass testSquare_class)) app))
      (.Error t "getClassLoader"))
    (when (!= (.GoName (GetClass (.GetParent__ClassLoader app))) "jdk.internal.loader.ClassLoaders$PlatformClassLoader")
      (.Error t "the platform loader"))
    (when (!= (Class_ForName_String_Z_ClassLoader__Class (Str "[Ltest.Square;") false app) (.ArrayClass testSquare_class))
      (.Error t "forName of an array class")))
  (when [got (res (fn ^string [] (Utils_CheckNonNegativeArgument_J_String__V -2 (Str "size")) ""))]
        (!= got "!java.lang.IllegalArgumentException: The provided size is negative: -2")
    (.Errorf t "checkNonNegativeArgument: %s" got)))

;; ---------------------------------------------------------------------------------------
;; Against the JVM: reflection on jrt's classes (testdata/reflect.txt)

(go/func classOf ^{:tag (* Class)} [^string n]
  (range [_ p primitives]
    (when (== (.GoName p) n)
      (return p)))
  (ForName n))

(go/func tokenValue ^any [^string tok]
  (let [(values k v _) (strings/Cut tok ":")]
    (switch k
      (case ["n"] (return nil))
      (case ["s"] (return (jstr v)))
      (case ["i"] (return (Integer_ValueOf_I__Integer (atoi v))))
      (case ["j"] (return (Long_ValueOf_J__Long (parseLong v))))
      (case ["h"] (return (Short_ValueOf_S__Short (conv int16 (atoi v)))))
      (case ["b"] (return (Byte_ValueOf_B__Byte (conv int8 (atoi v)))))
      (case ["c"] (return (Character_ValueOf_C__Character (conv uint16 (atoi v)))))
      (case ["z"] (return (Boolean_ValueOf_Z__Boolean (== v "true"))))
      (case ["d"] (return (Double_ValueOf_D__Double (parseJavaDouble v))))
      (case ["C"] (return (classOf v)))
      (case ["L"] (switch v
                    (case ["ROOT"] (return Locale_ROOT))
                    (case ["US"] (return Locale_US))
                    (default (return Locale_ENGLISH))))
      (case ["cs"] (return (Charset_ForName_String__Charset (Str v))))
      (case ["ia"] (let [ps (strings/Split v ",")
                         a (NewIntArray (conv int32 (len ps)))]
                     (range [i p ps]
                       (aset (.-A a) i (atoi p)))
                     (return a)))
      (case ["ca"] (return (.ToCharArray__C1 (Str v)))))
    (panic (+ "bad token " tok))))

(go/func paramClasses ^{:tag (* RefArray)} [^string ps]
  (let [a (NewRefArray Class_class 0)]
    (when (!= ps "")
      (range [_ p (strings/Split ps ",")]
        (set! (.-A a) (append (.-A a) (classOf p)))))
    a))

(go/func argValues ^{:tag (* RefArray)} [^string as]
  (let [a (NewRefArray Object_class 0)]
    (when (!= as "")
      (range [_ x (strings/Split as " ")]
        (set! (.-A a) (append (.-A a) (tokenValue x)))))
    a))

(go/func descOf ^string [^{:tag (* Class)} c]
  (cond
    (.IsArray__Z c) (return (+ "[" (descOf (.-comp c))))
    (.IsPrimitive__Z c) (return (conv string (conv rune (.-desc c)))))
  (+ "L" (strings/ReplaceAll (.GoName c) "." "/") ";"))

(go/func sigOf ^string [^{:tag (* Method)} m]
  (let [s (+ (.String (.GetName__String m)) "(")]
    (range [_ p (.-params m)]
      (set! s (+ s (descOf p))))
    (set! s (+ s ")" (descOf (.GetReturnType__Class m))))
    (when (!= (bit-and (.GetModifiers__I m) AccStatic) 0)
      (set! s (+ s " static")))
    s))

(go/func sortedJoin ^string [^{:tag (slice string)} xs]
  (sort/Strings xs)
  (let [^{:tag (slice string)} out nil]
    (range [i x xs]
      (when (or (== i 0) (!= x (aget xs (- i 1))))
        (set! out (append out x))))
    (strings/Join out "|")))

(go/func lenientNPE
  "lenientNPE: the JVM's helpful NullPointerException message is not reproduced (A6), so a
want of that kind matches the bare class.\n"
  ^string [^string want]
  (when (strings/HasPrefix want "!java.lang.NullPointerException: ")
    (return "!java.lang.NullPointerException"))
  want)

(go/func TestReflectAgainstJVM [^{:tag (* testing/T)} t]
  (range [_ c (readCases t "reflect.txt")]
    (let [want (lenientNPE (aget c (- (len c) 1)))
          got ""]
      ;; the tables keep no throws clauses
      (let [parts (strings/Split want "|")]
        (range [i p parts]
          (when [j (strings/Index p " throws ")] (>= j 0)
            (aset parts i (subslice p _ j))))
        (set! want (strings/Join parts "|")))
      (switch (aget c 0)
        (case ["methods" "methods-c2g"]
          ;; methods-c2g holds when c2g's translation of the JDK is in the program (bin/jrt
          ;; test --prog): its class jdk.internal.jrt.StandardStreams, always translated, is registered
          (when (!= (== (aget c 0) "methods-c2g") (!= (ForName "jdk.internal.jrt.StandardStreams") nil))
            (continue))
          (let [^{:tag (slice string)} ss nil]
            (range [_ x (.-A (.GetMethods__Method1 (classOf (aget c 1))))]
              (set! ss (append ss (sigOf (assert (* Method) x)))))
            (set! got (sortedJoin ss))
            (when (== (len c) 2)
              (set! want ""))))
        (case ["getMethod"]
          (set! got (outcome (fn ^any [] (.ToString__String (.GetMethod_String_Class1__Method (classOf (aget c 1)) (Str (aget c 2)) (paramClasses (aget c 3))))))))
        (case ["getConstructor"]
          (set! got (outcome (fn ^any [] (.ToString__String (.GetConstructor_Class1__Constructor (classOf (aget c 1)) (paramClasses (aget c 2))))))))
        (case ["getField"]
          (set! got (outcome (fn ^any [] (.ToString__String (.GetField_String__Field (classOf (aget c 1)) (Str (aget c 2))))))))
        (case ["fields"]
          (let [^{:tag (slice string)} ss nil]
            (range [_ x (.-A (.GetFields__Field1 (classOf (aget c 1))))]
              (set! ss (append ss (.String (.ToString__String (assert (* Field) x))))))
            (set! got (sortedJoin ss))
            (when (== (len c) 2)
              (set! want ""))))
        (case ["constructors"]
          (let [^{:tag (slice string)} ss nil]
            (range [_ x (.-A (.GetConstructors__Constructor1 (classOf (aget c 1))))]
              (set! ss (append ss (.String (.ToString__String (assert (* Constructor) x))))))
            (set! got (sortedJoin ss))
            (when (== (len c) 2)
              (set! want ""))))
        (case ["invoke"]
          (set! got (outcome (fn ^any [] (.Invoke_O_O1__O (.GetMethod_String_Class1__Method (classOf (aget c 1)) (Str (aget c 2)) (paramClasses (aget c 3)))
                                                          (tokenValue (aget c 4)) (argValues (aget c 5)))))))
        (case ["newInstance"]
          (set! got (outcome (fn ^any []
                               (let [v (.NewInstance_O1__O (.GetConstructor_Class1__Constructor (classOf (aget c 1)) (paramClasses (aget c 2))) (argValues (aget c 3)))]
                                 (when (== (aget c 1) "java.lang.Object")
                                   (return (.GetName__String (GetClass v))))
                                 v)))))
        (case ["fieldGet"]
          (set! got (outcome (fn ^any [] (.Get_O__O (.GetField_String__Field (classOf (aget c 1)) (Str (aget c 2))) (tokenValue (aget c 3)))))))
        (case ["fieldSet"]
          (set! got (outcome (fn ^any [] (.Set_O_O__V (.GetField_String__Field (classOf (aget c 1)) (Str (aget c 2))) nil (tokenValue (aget c 3))) nil))))
        (case ["typeName" "canonicalName" "packageName" "simpleName" "declaringClass" "modifiers" "superclass"
               "classLoaderNull" "enumConstants"]
          (let [k (classOf (aget c 1))
                op (aget c 0)]
            (set! got (outcome (fn ^any [] (classOp op k))))))
        (case ["array"]
          (set! got (outcome (fn ^any [] (arrayOp (aget c 1))))))
        (default (.Fatalf t "unknown case %v" c)))
      (when (!= got want)
        (.Errorf t "%s: got %q, want %q" (strings/Join (subslice c _ (- (len c) 1)) " ") got want)))))

(go/func classOp ^any [^string op ^{:tag (* Class)} k]
  (switch op
    (case ["typeName"] (return (.GetTypeName__String k)))
    (case ["canonicalName"] (return (.GetCanonicalName__String k)))
    (case ["packageName"] (return (.GetPackageName__String k)))
    (case ["simpleName"] (return (.GetSimpleName__String k)))
    (case ["declaringClass"] (let [d (.GetDeclaringClass__Class k)]
                               (when (== d nil) (return nil))
                               (return (.GetName__String d))))
    (case ["modifiers"] (return (Box (.GetModifiers__I k))))
    (case ["superclass"] (let [d (.GetSuperclass__Class k)]
                           (when (== d nil) (return nil))
                           (return (.GetName__String d))))
    (case ["classLoaderNull"] (return (Box (== (.GetClassLoader__ClassLoader k) nil))))
    (case ["enumConstants"] (let [a (.GetEnumConstants__O1 k)]
                              (when (== a nil) (return nil))
                              (let [ps (make (slice string) (len (.-A a)))]
                                (range [i x (.-A a)]
                                  (aset ps i (.String (StrOfObj x))))
                                (return (Str (strings/Join ps ",")))))))
  nil)

(go/func vecText
  "vecText is the text of a Clojure vector of the array's elements, as the JVM side prints
(vec a).\n"
  ^any [^any a]
  (let [n (conv int (Array_GetLength_O__I a))
        ps (make (slice string) n)]
    (for [i 0] (< i n) (inc! i)
      (aset ps i (.String (StrOfObj (Array_Get_O_I__O a (conv int32 i))))))
    (Str (strings/Join ps ","))))

(go/func arrayOp ^any [^string n]
  (switch n
    (case ["get int[] 1"] (return (Array_Get_O_I__O (IntArrayOf 5 6 7) 1)))
    (case ["get char[] 0"] (return (Array_Get_O_I__O (CharArrayOf \q) 0)))
    (case ["get oob"] (return (Array_Get_O_I__O (NewIntArray 3) 5)))
    (case ["get neg"] (return (Array_Get_O_I__O (NewRefArray Object_class 3) -1)))
    (case ["get notarray"] (return (Array_Get_O_I__O (Str "x") 0)))
    (case ["get null"] (return (Array_Get_O_I__O nil 0)))
    (case ["set int null"] (Array_Set_O_I_O__V (NewIntArray 3) 0 nil) (return nil))
    (case ["set int short"] (let [a (NewIntArray 3)] (Array_Set_O_I_O__V a 0 (Short_ValueOf_S__Short 5)) (return (vecText a))))
    (case ["set int long"] (Array_Set_O_I_O__V (NewIntArray 3) 0 (Long_ValueOf_J__Long 5)) (return nil))
    (case ["set int oob mismatch"] (Array_Set_O_I_O__V (NewIntArray 3) 5 (Str "x")) (return nil))
    (case ["set str oob mismatch"] (Array_Set_O_I_O__V (NewRefArray String_class 3) 5 (Long_ValueOf_J__Long 1)) (return nil))
    (case ["set str mismatch"] (Array_Set_O_I_O__V (NewRefArray String_class 3) 0 (Long_ValueOf_J__Long 1)) (return nil))
    (case ["set obj short"] (let [a (NewRefArray Object_class 3)] (Array_Set_O_I_O__V a 0 (Short_ValueOf_S__Short 5))
                              (return (.GetName__String (GetClass (aget (.-A a) 0))))))
    (case ["set notarray"] (Array_Set_O_I_O__V (Str "x") 0 (Long_ValueOf_J__Long 1)) (return nil))
    (case ["setInt long[]"] (let [a (NewLongArray 3)] (Array_SetInt_O_I_I__V a 0 5) (return (vecText a))))
    (case ["setInt notarray"] (Array_SetInt_O_I_I__V (Str "x") 0 1) (return nil))
    (case ["setInt objarray"] (Array_SetInt_O_I_I__V (NewRefArray Object_class 3) 0 1) (return nil))
    (case ["setLong int[]"] (Array_SetLong_O_I_J__V (NewIntArray 3) 0 5) (return nil))
    (case ["setChar int[]"] (let [a (NewIntArray 3)] (Array_SetChar_O_I_C__V a 1 \a) (return (vecText a))))
    (case ["setByte char[]"] (Array_SetByte_O_I_B__V (NewCharArray 3) 0 1) (return nil))
    (case ["setBoolean int[]"] (Array_SetBoolean_O_I_Z__V (NewIntArray 3) 0 true) (return nil))
    (case ["setDouble float[]"] (Array_SetDouble_O_I_D__V (NewFloatArray 3) 0 1.0) (return nil))
    (case ["setFloat double[]"] (let [a (NewDoubleArray 1)] (Array_SetFloat_O_I_F__V a 0 (conv float32 0.1)) (return (vecText a))))
    (case ["getInt long[]"] (return (Box (Array_GetInt_O_I__I (NewLongArray 3) 0))))
    (case ["getLong int[]"] (return (Box (Array_GetLong_O_I__J (IntArrayOf 9) 0))))
    (case ["getDouble char[]"] (return (Box (Array_GetDouble_O_I__D (CharArrayOf \A) 0))))
    (case ["getInt obj[]"] (return (Box (Array_GetInt_O_I__I (NewRefArray Object_class 3) 0))))
    (case ["getLength int[]"] (return (Box (Array_GetLength_O__I (NewIntArray 4)))))
    (case ["getLength null"] (return (Box (Array_GetLength_O__I nil))))
    (case ["getLength notarray"] (return (Box (Array_GetLength_O__I (Str "x")))))
    (case ["newInstance void"] (return (Array_NewInstance_Class_I__O Prim_void 3)))
    (case ["newInstance -1"] (return (Array_NewInstance_Class_I__O String_class -1)))
    (case ["newInstance dims empty"] (return (Array_NewInstance_Class_I1__O Prim_int (IntArrayOf))))
    (case ["newInstance dims void"] (return (Array_NewInstance_Class_I1__O Prim_void (IntArrayOf 1))))
    (case ["newInstance dims neg"] (return (Array_NewInstance_Class_I1__O Prim_int (IntArrayOf 2 -1))))
    (case ["newInstance dims 256"] (let [d (NewIntArray 256)]
                                     (range [i _ (.-A d)] (aset (.-A d) i 1))
                                     (return (Array_NewInstance_Class_I1__O Prim_int d))))
    (case ["newInstance dims 2x3"] (let [a (Array_NewInstance_Class_I1__O Prim_int (IntArrayOf 2 3))]
                                     (return (Str (+ (.GoName (GetClass a)) " " (.String (StrOfInt (Array_GetLength_O__I a))) " "
                                                     (.String (StrOfInt (Array_GetLength_O__I (Array_Get_O_I__O a 0)))))))))
    (case ["newInstance dims String 2"] (return (.GetName__String (GetClass (Array_NewInstance_Class_I1__O String_class (IntArrayOf 2)))))))
  (panic (+ "unknown array case " n)))
