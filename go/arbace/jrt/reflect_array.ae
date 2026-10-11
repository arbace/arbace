;; jrt: java.lang.reflect.Array (phase 2b; Array.newInstance(Class, int) is phase 1's, in
;; array.clj): get, set and their primitive variants, getLength, newInstance(Class, int...),
;; with the JVM's exceptions and messages (Reflection::array_get/array_set of HotSpot, checked
;; against the JVM by the tests).
(in-ns 'go.arbace.jrt)

(go/file "reflect_array.go")

(go/var Array_class
  (Define (addr (lit ClassInfo :Name "java.lang.reflect.Array" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class :Go "arbace/jrt.Array"))))

(go/func notAnArray ^Throwable_I []
  (IllegalArgumentException_New_String (Str "Argument is not an array")))

(go/func argMismatch ^Throwable_I []
  (IllegalArgumentException_New_String (Str "argument type mismatch")))

(go/func arrayIndex
  "arrayIndex checks a: NullPointerException for null, IllegalArgumentException when it is
not an array, ArrayIndexOutOfBoundsException (without message, as the JVM's) for i.\n"
  [^any a ^int32 i]
  (when (== a nil)
    (panic (NPE)))
  (let [n (arrayLength a)]
    (when (< n 0)
      (panic (notAnArray)))
    (when (>= (conv uint32 i) (conv uint32 n))
      (panic (ArrayIndexOutOfBoundsException_New)))))

(go/func arrayGet
  "arrayGet: a[i] in the tables' convention (a Go primitive value, or the reference).\n"
  ^any [^any a ^int32 i]
  (arrayIndex a i)
  (type-switch [x a]
    (case [(* BooleanArray)] (return (aget (.-A x) i)))
    (case [(* ByteArray)] (return (aget (.-A x) i)))
    (case [(* CharArray)] (return (aget (.-A x) i)))
    (case [(* ShortArray)] (return (aget (.-A x) i)))
    (case [(* IntArray)] (return (aget (.-A x) i)))
    (case [(* LongArray)] (return (aget (.-A x) i)))
    (case [(* FloatArray)] (return (aget (.-A x) i)))
    (case [(* DoubleArray)] (return (aget (.-A x) i))))
  (aget (.-A (assert (* RefArray) a)) i))

(go/func arraySetPrim
  "arraySetPrim stores the primitive value v of code from into the primitive array a, widened
to its component type; IllegalArgumentException when that is not a widening.\n"
  [^any a ^int32 i ^any v ^byte from]
  (let [comp (.-comp (GetClass a))
        (values w ok) (widen v from (.-desc comp))]
    (when (not ok)
      (panic (argMismatch)))
    (type-switch [x a]
      (case [(* BooleanArray)] (aset (.-A x) i (assert bool w)))
      (case [(* ByteArray)] (aset (.-A x) i (assert int8 w)))
      (case [(* CharArray)] (aset (.-A x) i (assert uint16 w)))
      (case [(* ShortArray)] (aset (.-A x) i (assert int16 w)))
      (case [(* IntArray)] (aset (.-A x) i (assert int32 w)))
      (case [(* LongArray)] (aset (.-A x) i (assert int64 w)))
      (case [(* FloatArray)] (aset (.-A x) i (assert float32 w)))
      (case [(* DoubleArray)] (aset (.-A x) i (assert float64 w))))))

(go/func Array_Get_O_I__O "Array_Get_O_I__O is Array.get: primitive elements boxed.\n"
  ^any [^any a ^int32 i]
  (Box (arrayGet a i)))

(go/func Array_GetLength_O__I "Array_GetLength_O__I is Array.getLength.\n"
  ^int32 [^any a]
  (when (== a nil)
    (panic (NPE)))
  (let [n (arrayLength a)]
    (when (< n 0)
      (panic (notAnArray)))
    (conv int32 n)))

(go/func Array_Set_O_I_O__V
  "Array_Set_O_I_O__V is Array.set: for a primitive array the value unboxed and widened
(IllegalArgumentException otherwise, null included); for a reference array an instance of the
component type (IllegalArgumentException: array element type mismatch).\n"
  [^any a ^int32 i ^any v]
  (arrayIndex a i)
  (let [(values r ok) (assert (* RefArray) a)]
    (when ok
      (when (and (!= v nil) (not (.IsInstance_O__Z (.-Comp r) v)))
        (panic (IllegalArgumentException_New_String (Str "array element type mismatch"))))
      (aset (.-A r) i v)
      (return)))
  (let [(values p code) (unwrap v)]
    (when (== code 0)
      (panic (IllegalArgumentException_New)))
    (arraySetPrim a i p code)))

(go/func arraySetTyped
  "arraySetTyped is Array.setInt and its kin: v of code from into a primitive array.\n"
  [^any a ^int32 i ^any v ^byte from]
  (arrayIndex a i)
  (when (RefArray_isRef a)
    (panic (IllegalArgumentException_New_String (Str "Argument is not an array of primitive type"))))
  (arraySetPrim a i v from))

(go/func RefArray_isRef ^bool [^any a] (let [(values _ ok) (assert (* RefArray) a)] ok))

(go/func Array_SetBoolean_O_I_Z__V [^any a ^int32 i ^bool v] (arraySetTyped a i v \Z))
(go/func Array_SetByte_O_I_B__V [^any a ^int32 i ^int8 v] (arraySetTyped a i v \B))
(go/func Array_SetChar_O_I_C__V [^any a ^int32 i ^uint16 v] (arraySetTyped a i v \C))
(go/func Array_SetShort_O_I_S__V [^any a ^int32 i ^int16 v] (arraySetTyped a i v \S))
(go/func Array_SetInt_O_I_I__V [^any a ^int32 i ^int32 v] (arraySetTyped a i v \I))
(go/func Array_SetLong_O_I_J__V [^any a ^int32 i ^int64 v] (arraySetTyped a i v \J))
(go/func Array_SetFloat_O_I_F__V [^any a ^int32 i ^float32 v] (arraySetTyped a i v \F))
(go/func Array_SetDouble_O_I_D__V [^any a ^int32 i ^float64 v] (arraySetTyped a i v \D))

(go/func arrayGetTyped
  "arrayGetTyped is Array.getInt and its kin: a primitive element widened to code to.\n"
  ^any [^any a ^int32 i ^byte to]
  (arrayIndex a i)
  (when (RefArray_isRef a)
    (panic (IllegalArgumentException_New_String (Str "Argument is not an array of primitive type"))))
  (let [(values w ok) (widen (arrayGet a i) (.-desc (.-comp (GetClass a))) to)]
    (when (not ok)
      (panic (argMismatch)))
    w))

(go/func Array_GetBoolean_O_I__Z ^bool [^any a ^int32 i] (assert bool (arrayGetTyped a i \Z)))
(go/func Array_GetByte_O_I__B ^int8 [^any a ^int32 i] (assert int8 (arrayGetTyped a i \B)))
(go/func Array_GetChar_O_I__C ^uint16 [^any a ^int32 i] (assert uint16 (arrayGetTyped a i \C)))
(go/func Array_GetShort_O_I__S ^int16 [^any a ^int32 i] (assert int16 (arrayGetTyped a i \S)))
(go/func Array_GetInt_O_I__I ^int32 [^any a ^int32 i] (assert int32 (arrayGetTyped a i \I)))
(go/func Array_GetLong_O_I__J ^int64 [^any a ^int32 i] (assert int64 (arrayGetTyped a i \J)))
(go/func Array_GetFloat_O_I__F ^float32 [^any a ^int32 i] (assert float32 (arrayGetTyped a i \F)))
(go/func Array_GetDouble_O_I__D ^float64 [^any a ^int32 i] (assert float64 (arrayGetTyped a i \D)))

(go/func arrayRank ^int [^{:tag (* Class)} c]
  (let [n 0]
    (while (.IsArray__Z c)
      (inc! n)
      (set! c (.-comp c)))
    n))

(go/func Array_NewInstance_Class_I1__O
  "Array_NewInstance_Class_I1__O is Array.newInstance(Class, int...): an array of
len(dims) dimensions of comp (IllegalArgumentException for no dimensions, void, or more than
255 dimensions in all; NegativeArraySizeException for a negative size).\n"
  ^any [^{:tag (* Class)} comp ^{:tag (* IntArray)} dims]
  (NN comp)
  (let [ds (.-A (NN dims))]
    (when (or (== (len ds) 0) (== comp Prim_void) (> (+ (len ds) (arrayRank comp)) 255))
      (panic (IllegalArgumentException_New)))
    (range [_ d ds]
      (when (< d 0)
        (panic (negativeArraySize d))))
    (let [c comp]
      (range [(len ds)]
        (set! c (.ArrayClass c)))
      (multiArray c ds))))
