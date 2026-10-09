;; jrt's tests: the object model (identity, hash, toString, clone), arrays, Class objects and
;; class initialization.
(in-ns 'go.arbace.jrt)

(go/file "object_test.go"
  :imports [[strings "strings"] [sync "sync"] [atomic "sync/atomic"] [testing "testing"]])

(go/func TestIdentity [^{:tag (* testing/T)} t]
  (let [a (Object_New)
        b (Object_New)
        h (IdentityHash a)]
    (when (or (== a b) (Equals a b) (not (Equals a a)))
      (.Error t "identity"))
    (when (or (<= h 0) (!= h (HashCode a)) (!= h (IdentityHash a)))
      (.Errorf t "identity hash %d" h))
    (when (!= (IdentityHash nil) 0)
      (.Error t "identityHashCode(null)"))
    ;; Object.toString: class@hex(hashCode)
    (let [s (.String (ToString a))]
      (when (!= s (+ "java.lang.Object@" (hexString (conv uint64 h))))
        (.Errorf t "toString %q" s)))
    ;; the hash survives locking (the lock word shares the header)
    (MonitorEnter a)
    (MonitorEnter a)
    (when (!= (IdentityHash a) h)
      (.Error t "hash changed under a thin lock"))
    (MonitorExit a)
    (MonitorExit a)
    (when (!= (IdentityHash a) h)
      (.Error t "hash changed after unlocking"))
    ;; hashes of many objects: 31 bits, never 0
    (for [i 0] (< i 10000) (inc! i)
      (let [x (IdentityHash (Object_New))]
        (when (<= x 0)
          (.Fatalf t "hash %d" x))))
    ;; Object's methods on null throw NullPointerException
    (when (!= (res (fn ^string [] (bres (Equals nil a)))) "!java.lang.NullPointerException")
      (.Error t "null.equals"))
    (when (!= (res (fn ^string [] (sres (ToString nil)))) "!java.lang.NullPointerException")
      (.Error t "null.toString"))
    ;; clone: arrays copy, others throw CloneNotSupportedException
    (when (!= (res (fn ^string [] (StrOfObj (.Clone__O (Throwable_New))) "x")) "!java.lang.CloneNotSupportedException: java.lang.Throwable")
      (.Error t "clone of a Throwable"))
    (let [ia (IntArrayOf 1 2 3)
          c (assert (* IntArray) (.Clone__O ia))]
      (aset (.-A c) 0 9)
      (when (or (== c ia) (!= (aget (.-A ia) 0) 1) (!= (aget (.-A c) 0) 9) (!= (GetClass c) (GetClass ia)))
        (.Error t "array clone")))
    (let [ra (RefArrayOf String_class (Str "a"))
          c (.Copy ra)]
      (when (or (!= (.-Comp c) String_class) (!= (aget (.-A c) 0) (aget (.-A ra) 0)))
        (.Error t "reference array clone")))))

(go/func TestArrays [^{:tag (* testing/T)} t]
  ;; covariance: a String[] is an Object[]; stores are checked against the component
  (let [a (NewRefArray String_class 2)]
    (when (not (.IsInstance_O__Z (.ArrayClass Object_class) a))
      (.Error t "String[] instanceof Object[]"))
    (.Store a 0 (Str "s"))
    (.Store a 1 nil)
    (when (!= (res (fn ^string [] (.Store a 0 (Throwable_New)) "x")) "!java.lang.ArrayStoreException: java.lang.Throwable")
      (.Error t "store check")))
  ;; an Object[] made as Object[] takes anything
  (let [a (NewRefArray Object_class 1)]
    (.Store a 0 (NewIntArray 1)))
  ;; arrays of arrays: the component is the array class
  (let [m (NewMultiArray (.ArrayClass (.ArrayClass Prim_int)) 2 3)]
    (when (or (!= (len (.-A m)) 2) (!= (len (.-A (assert (* IntArray) (aget (.-A m) 1)))) 3))
      (.Error t "int[2][3]"))
    (when (!= (res (fn ^string [] (.Store m 0 (NewLongArray 1)) "x")) "!java.lang.ArrayStoreException: [J")
      (.Error t "store of long[] into int[][]")))
  (let [m (NewMultiArray (.ArrayClass (.ArrayClass (.ArrayClass String_class))) 2)]
    (when (or (!= (len (.-A m)) 2) (!= (aget (.-A m) 0) nil) (!= (.-Comp m) (.ArrayClass (.ArrayClass String_class))))
      (.Error t "String[2][][]")))
  ;; Array.newInstance
  (let [x (Array_NewInstance_Class_I__O Prim_char 4)]
    (when (!= (len (.-A (assert (* CharArray) x))) 4)
      (.Error t "Array.newInstance(char, 4)")))
  (when (!= (res (fn ^string [] (StrOfObj (Array_NewInstance_Class_I__O Prim_void 1)) "x")) "!java.lang.IllegalArgumentException")
    (.Error t "Array.newInstance(void)"))
  ;; initializers
  (let [b (BooleanArrayOf true false)
        d (DoubleArrayOf)]
    (when (or (!= (len (.-A b)) 2) (== (.-A d) nil) (!= (len (.-A d)) 0))
      (.Error t "initializers")))
  ;; arraycopy copies an element mismatch up to the failing element
  (let [src (RefArrayOf Object_class (Str "a") (Throwable_New) (Str "c"))
        dst (NewRefArray String_class 3)
        got (res (fn ^string [] (Arraycopy src 0 dst 0 3) "x"))]
    (when (or (!= got "!java.lang.ArrayStoreException: arraycopy: element type mismatch: can not cast one of the elements of java.lang.Object[] to the type of the destination array, java.lang.String")
              (== (aget (.-A dst) 0) nil) (!= (aget (.-A dst) 1) nil))
      (.Errorf t "arraycopy element mismatch: %q" got))))

(go/func TestClasses [^{:tag (* testing/T)} t]
  (when (or (!= (ForName "java.lang.String") String_class) (!= (ForName "[I") (.ArrayClass Prim_int))
            (!= (ForName "[[Ljava.lang.Object;") (.ArrayClass (.ArrayClass Object_class)))
            (!= (ForName "int") nil) (!= (ForName "[V") nil) (!= (ForName "[Lno.Such;") nil) (!= (ForName "[I;") nil))
    (.Error t "ForName"))
  (when (!= (Class_GetPrimitiveClass_String__Class (Str "double")) Prim_double)
    (.Error t "getPrimitiveClass"))
  (when (or (not (.IsInstance_O__Z CharSequence_class (Str "x"))) (.IsInstance_O__Z CharSequence_class (Object_New))
            (not (.IsInstance_O__Z Comparable_class (StringBuilder_New))) (.IsInstance_O__Z Object_class nil))
    (.Error t "isInstance"))
  (when (or (not (.IsAssignableFrom_Class__Z Throwable_class NullPointerException_class))
            (.IsAssignableFrom_Class__Z NullPointerException_class Throwable_class)
            (not (.IsAssignableFrom_Class__Z Serializable_class NullPointerException_class))
            (not (.IsAssignableFrom_Class__Z Object_class Comparable_class))
            (.IsAssignableFrom_Class__Z Prim_int Prim_long))
    (.Error t "isAssignableFrom"))
  (when (or (!= (.GetSuperclass__Class String_class) Object_class) (!= (.GetSuperclass__Class Object_class) nil)
            (!= (.GetSuperclass__Class (.ArrayClass String_class)) Object_class)
            (!= (.GetComponentType__Class (.ArrayClass String_class)) String_class))
    (.Error t "getSuperclass, getComponentType"))
  (when (!= (.String (.GetSimpleName__String NullPointerException_class)) "NullPointerException")
    (.Error t "getSimpleName"))
  (when (!= (.GetName__String String_class) (.GetName__String String_class))
    (.Error t "getName: one String per class"))
  (when (!= (len (.-A (.GetInterfaces__Class1 String_class))) 5)
    (.Error t "getInterfaces"))
  (when (!= (GetClass (Str "x")) String_class)
    (.Error t "getClass"))
  (when (!= (res (fn ^string [] (.GoName (Class_ForName_String__Class (Str "int"))))) "!java.lang.ClassNotFoundException: int")
    (.Error t "forName of a primitive name"))
  (when (!= (res (fn ^string [] (StrOfObj (.Cast_O__O String_class (Object_New))) "x")) "!java.lang.ClassCastException: Cannot cast java.lang.Object to java.lang.String")
    (.Error t "Class.cast"))
  ;; ClassCast's message for arrays and for Arbace's classes
  (let [e (ClassCast (NewRefArray Object_class 0) (.ArrayClass String_class))]
    (when (!= (.String (.GetMessage__String e)) "class [Ljava.lang.Object; cannot be cast to class [Ljava.lang.String; ([Ljava.lang.Object; and [Ljava.lang.String; are in module java.base of loader 'bootstrap')")
      (.Errorf t "ClassCast of arrays: %s" (.GetMessage__String e))))
  (let [c (Define (addr (lit ClassInfo :Name "arbace.test.Thing" :Kind KindClass :Super Object_class)))
        e (ClassCast (Str "x") c)]
    (when (!= (.String (.GetMessage__String e)) "class java.lang.String cannot be cast to class arbace.test.Thing (java.lang.String is in module java.base of loader 'bootstrap'; arbace.test.Thing is in unnamed module of loader 'app')")
      (.Errorf t "ClassCast to Arbace's class: %s" (.GetMessage__String e))))
  (when (!= (Throwable_Cast nil) nil)
    (.Error t "cast of null")))

;; ---------------------------------------------------------------------------------------
;; Class initialization (§6.2)

(go/var
  [^{:tag ClassInit} testA_init]
  [^int testA_runs]
  [^int testA_seen]
  [^{:tag ClassInit} testB_init]
  [^{:tag ClassInit} testC_init]
  [^int testC_runs])

(go/func testA_Init [] (when (not (.Done testA_init)) (.Run testA_init testA_clinit)))
(go/func testA_clinit []
  (inc! testA_runs)
  ;; re-entered by the same thread: returns at once (the class is seen half-initialized)
  (testA_Init)
  (set! testA_seen testA_runs))

(go/func testB_Init [] (when (not (.Done testB_init)) (.Run testB_init testB_clinit)))
(go/func testB_clinit []
  (panic (Thrown (IllegalStateException_New_String (Str "boom")))))

(go/func testC_Init [] (when (not (.Done testC_init)) (.Run testC_init testC_clinit)))
(go/func testC_clinit []
  (inc! testC_runs)
  (panic (Thrown (AssertionError_New))))

(go/func init []
  (RegisterFrames (lit (slice FrameInfo) (lit _ "arbace/jrt.testB_clinit" "test.B" "<clinit>" 0))))

(go/func TestClassInit [^{:tag (* testing/T)} t]
  (testA_Init)
  (testA_Init)
  (when (or (!= testA_runs 1) (!= testA_seen 1) (not (.Done testA_init)))
    (.Errorf t "runs %d seen %d" testA_runs testA_seen))
  ;; an exception: ExceptionInInitializerError, then NoClassDefFoundError with the cause
  (let [first (res (fn ^string [] (testB_Init) "x"))
        second (res (fn ^string [] (testB_Init) "x"))]
    (when (!= first "!java.lang.ExceptionInInitializerError")
      (.Errorf t "first: %q" first))
    (when (!= second "!java.lang.NoClassDefFoundError: Could not initialize class test.B")
      (.Errorf t "second: %q" second)))
  ;; an Error passes through unwrapped, and the class is failed
  (let [first (res (fn ^string [] (testC_Init) "x"))
        second (res (fn ^string [] (testC_Init) "x"))]
    (when (or (!= first "!java.lang.AssertionError") (not (strings/HasPrefix second "!java.lang.NoClassDefFoundError: Could not initialize class")) (!= testC_runs 1))
      (.Errorf t "Error in clinit: %q %q %d" first second testC_runs))))

(go/var [^{:tag ClassInit} testD_init] [^{:tag atomic/Int32} testD_runs] [^{:tag atomic/Int32} testD_value])

(go/func testD_Init [] (when (not (.Done testD_init)) (.Run testD_init testD_clinit)))
(go/func testD_clinit []
  (.Add testD_runs 1)
  ;; slow, so that other goroutines arrive while it runs and must wait
  (for [i 0] (< i 1000) (inc! i)
    (set! _ (Object_New)))
  (.Store testD_value 42))

(go/func TestClassInitConcurrent
  "Threads that use a class while another runs its initializer wait for it (JVMS 5.5).\n"
  [^{:tag (* testing/T)} t]
  (let [^sync/WaitGroup wg (zero sync/WaitGroup)
        ^{:tag atomic/Int32} bad (zero atomic/Int32)]
    (for [i 0] (< i 32) (inc! i)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (testD_Init)
             (when (!= (.Load testD_value) 42)
               (.Add bad 1))))))
    (.Wait wg)
    (when (or (!= (.Load testD_runs) 1) (!= (.Load bad) 0))
      (.Errorf t "runs %d, %d saw the class uninitialized" (.Load testD_runs) (.Load bad)))))
