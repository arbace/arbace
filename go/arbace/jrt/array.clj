;; jrt: arrays (C2G-SPEC §5.9).
(in-ns 'go.arbace.jrt)

(go/file "array.go"
  :imports [[strconv "strconv"] [unsafe "unsafe"]])

(go/func negativeArraySize ^Throwable_I [^int32 n]
  (NegativeArraySizeException_New_String (Str (strconv/Itoa (conv int n)))))

(go/func IndexOutOfBounds
  "IndexOutOfBounds is the ArrayIndexOutOfBoundsException of an array access a[i] on an array
of length n, with the JVM's message.\n"
  ^Throwable_I [^int i ^int n]
  (ArrayIndexOutOfBoundsException_New_String
    (Str (+ "Index " (strconv/Itoa i) " out of bounds for length " (strconv/Itoa n)))))
;; ---- boolean[]

(go/type BooleanArray "BooleanArray is boolean[].\n" (struct Object ^{:tag (slice bool)} A))

(go/func NewBooleanArray "NewBooleanArray is new boolean[n].\n"
  ^{:tag (* BooleanArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit BooleanArray :A (make (slice bool) n))))

(go/func BooleanArrayOf "BooleanArrayOf is an array initializer of boolean[].\n"
  ^{:tag (* BooleanArray)} [& ^{:tag (slice bool)} a]
  (when (== a nil)
    (set! a (lit (slice bool))))
  (addr (lit BooleanArray :A a)))

(go/method Ref ^any [^{:tag (* BooleanArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* BooleanArray)} t] (.ArrayClass Prim_boolean))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* BooleanArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* BooleanArray)} t])
(go/method Is_Serializable [^{:tag (* BooleanArray)} t])
(go/method Clone__O ^any [^{:tag (* BooleanArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* BooleanArray)} [^{:tag (* BooleanArray)} t]
  (let [a (make (slice bool) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit BooleanArray :A a))))
;; ---- byte[]

(go/type ByteArray "ByteArray is byte[].\n" (struct Object ^{:tag (slice int8)} A))

(go/func NewByteArray "NewByteArray is new byte[n].\n"
  ^{:tag (* ByteArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit ByteArray :A (make (slice int8) n))))

(go/func ByteArrayOf "ByteArrayOf is an array initializer of byte[].\n"
  ^{:tag (* ByteArray)} [& ^{:tag (slice int8)} a]
  (when (== a nil)
    (set! a (lit (slice int8))))
  (addr (lit ByteArray :A a)))

(go/method Ref ^any [^{:tag (* ByteArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ByteArray)} t] (.ArrayClass Prim_byte))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ByteArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* ByteArray)} t])
(go/method Is_Serializable [^{:tag (* ByteArray)} t])
(go/method Clone__O ^any [^{:tag (* ByteArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* ByteArray)} [^{:tag (* ByteArray)} t]
  (let [a (make (slice int8) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit ByteArray :A a))))
;; ---- char[]

(go/type CharArray "CharArray is char[].\n" (struct Object ^{:tag (slice uint16)} A))

(go/func NewCharArray "NewCharArray is new char[n].\n"
  ^{:tag (* CharArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit CharArray :A (make (slice uint16) n))))

(go/func CharArrayOf "CharArrayOf is an array initializer of char[].\n"
  ^{:tag (* CharArray)} [& ^{:tag (slice uint16)} a]
  (when (== a nil)
    (set! a (lit (slice uint16))))
  (addr (lit CharArray :A a)))

(go/method Ref ^any [^{:tag (* CharArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* CharArray)} t] (.ArrayClass Prim_char))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* CharArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* CharArray)} t])
(go/method Is_Serializable [^{:tag (* CharArray)} t])
(go/method Clone__O ^any [^{:tag (* CharArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* CharArray)} [^{:tag (* CharArray)} t]
  (let [a (make (slice uint16) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit CharArray :A a))))
;; ---- short[]

(go/type ShortArray "ShortArray is short[].\n" (struct Object ^{:tag (slice int16)} A))

(go/func NewShortArray "NewShortArray is new short[n].\n"
  ^{:tag (* ShortArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit ShortArray :A (make (slice int16) n))))

(go/func ShortArrayOf "ShortArrayOf is an array initializer of short[].\n"
  ^{:tag (* ShortArray)} [& ^{:tag (slice int16)} a]
  (when (== a nil)
    (set! a (lit (slice int16))))
  (addr (lit ShortArray :A a)))

(go/method Ref ^any [^{:tag (* ShortArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ShortArray)} t] (.ArrayClass Prim_short))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ShortArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* ShortArray)} t])
(go/method Is_Serializable [^{:tag (* ShortArray)} t])
(go/method Clone__O ^any [^{:tag (* ShortArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* ShortArray)} [^{:tag (* ShortArray)} t]
  (let [a (make (slice int16) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit ShortArray :A a))))
;; ---- int[]

(go/type IntArray "IntArray is int[].\n" (struct Object ^{:tag (slice int32)} A))

(go/func NewIntArray "NewIntArray is new int[n].\n"
  ^{:tag (* IntArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit IntArray :A (make (slice int32) n))))

(go/func IntArrayOf "IntArrayOf is an array initializer of int[].\n"
  ^{:tag (* IntArray)} [& ^{:tag (slice int32)} a]
  (when (== a nil)
    (set! a (lit (slice int32))))
  (addr (lit IntArray :A a)))

(go/method Ref ^any [^{:tag (* IntArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* IntArray)} t] (.ArrayClass Prim_int))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* IntArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* IntArray)} t])
(go/method Is_Serializable [^{:tag (* IntArray)} t])
(go/method Clone__O ^any [^{:tag (* IntArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* IntArray)} [^{:tag (* IntArray)} t]
  (let [a (make (slice int32) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit IntArray :A a))))
;; ---- long[]

(go/type LongArray "LongArray is long[].\n" (struct Object ^{:tag (slice int64)} A))

(go/func NewLongArray "NewLongArray is new long[n].\n"
  ^{:tag (* LongArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit LongArray :A (make (slice int64) n))))

(go/func LongArrayOf "LongArrayOf is an array initializer of long[].\n"
  ^{:tag (* LongArray)} [& ^{:tag (slice int64)} a]
  (when (== a nil)
    (set! a (lit (slice int64))))
  (addr (lit LongArray :A a)))

(go/method Ref ^any [^{:tag (* LongArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* LongArray)} t] (.ArrayClass Prim_long))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* LongArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* LongArray)} t])
(go/method Is_Serializable [^{:tag (* LongArray)} t])
(go/method Clone__O ^any [^{:tag (* LongArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* LongArray)} [^{:tag (* LongArray)} t]
  (let [a (make (slice int64) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit LongArray :A a))))
;; ---- float[]

(go/type FloatArray "FloatArray is float[].\n" (struct Object ^{:tag (slice float32)} A))

(go/func NewFloatArray "NewFloatArray is new float[n].\n"
  ^{:tag (* FloatArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit FloatArray :A (make (slice float32) n))))

(go/func FloatArrayOf "FloatArrayOf is an array initializer of float[].\n"
  ^{:tag (* FloatArray)} [& ^{:tag (slice float32)} a]
  (when (== a nil)
    (set! a (lit (slice float32))))
  (addr (lit FloatArray :A a)))

(go/method Ref ^any [^{:tag (* FloatArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* FloatArray)} t] (.ArrayClass Prim_float))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* FloatArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* FloatArray)} t])
(go/method Is_Serializable [^{:tag (* FloatArray)} t])
(go/method Clone__O ^any [^{:tag (* FloatArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* FloatArray)} [^{:tag (* FloatArray)} t]
  (let [a (make (slice float32) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit FloatArray :A a))))
;; ---- double[]

(go/type DoubleArray "DoubleArray is double[].\n" (struct Object ^{:tag (slice float64)} A))

(go/func NewDoubleArray "NewDoubleArray is new double[n].\n"
  ^{:tag (* DoubleArray)} [^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (addr (lit DoubleArray :A (make (slice float64) n))))

(go/func DoubleArrayOf "DoubleArrayOf is an array initializer of double[].\n"
  ^{:tag (* DoubleArray)} [& ^{:tag (slice float64)} a]
  (when (== a nil)
    (set! a (lit (slice float64))))
  (addr (lit DoubleArray :A a)))

(go/method Ref ^any [^{:tag (* DoubleArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* DoubleArray)} t] (.ArrayClass Prim_double))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* DoubleArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* DoubleArray)} t])
(go/method Is_Serializable [^{:tag (* DoubleArray)} t])
(go/method Clone__O ^any [^{:tag (* DoubleArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* DoubleArray)} [^{:tag (* DoubleArray)} t]
  (let [a (make (slice float64) (len (.-A t)))]
    (copy a (.-A t))
    (addr (lit DoubleArray :A a))))
;; ---- reference arrays

(go/type RefArray
  "RefArray is every array of references (Object[], String[], int[][] ...): Java's arrays are
covariant, Go's slices are not. Comp is the component class.\n"
  (struct Object ^{:tag (* Class)} Comp ^{:tag (slice any)} A))

(go/func NewRefArray "NewRefArray is new T[n] for a reference type T, comp its class.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} comp ^int32 n]
  (when (< n 0)
    (panic (negativeArraySize n)))
  (newRefArray comp n))

(go/type refArrayBuf
  "refArrayBuf is a reference array whose slots follow its header in the same allocation
(C2G-SPEC §13.4, D7; doc/go/SPEED-NOTES.md): B is an array type [N]any.\n"
  :type-params [B]
  (struct RefArray ^B buf))

(go/func newRefBuf
  "newRefBuf allocates a RefArray of n <= N slots in one object, its slots the array B.\n"
  :type-params [B] ^{:tag (* RefArray)} [^{:tag (* Class)} comp ^int32 n]
  (let [p (addr (lit (refArrayBuf B)))]
    (set! (.-Comp p) comp)
    (set! (.-A p) (unsafe/Slice (conv (* any) (conv unsafe/Pointer (addr (.-buf p)))) n))
    (addr (.-RefArray p))))

(go/func newRefArray
  "newRefArray is new T[n] (n >= 0): up to 32 slots in one allocation with the header, the
lengths rounded up to Go's size classes, larger arrays as a header and a slice.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} comp ^int32 n]
  (switch
    (case [(== n 0)] (return (addr (lit RefArray :Comp comp :A (lit (slice any))))))
    (case [(== n 1)] (return ((inst newRefBuf (array 1 any)) comp n)))
    (case [(== n 2)] (return ((inst newRefBuf (array 2 any)) comp n)))
    (case [(== n 3)] (return ((inst newRefBuf (array 3 any)) comp n)))
    (case [(== n 4)] (return ((inst newRefBuf (array 4 any)) comp n)))
    (case [(== n 5)] (return ((inst newRefBuf (array 5 any)) comp n)))
    (case [(== n 6)] (return ((inst newRefBuf (array 6 any)) comp n)))
    (case [(== n 7)] (return ((inst newRefBuf (array 7 any)) comp n)))
    (case [(== n 8)] (return ((inst newRefBuf (array 8 any)) comp n)))
    (case [(== n 9)] (return ((inst newRefBuf (array 9 any)) comp n)))
    (case [(== n 10)] (return ((inst newRefBuf (array 10 any)) comp n)))
    (case [(== n 11)] (return ((inst newRefBuf (array 11 any)) comp n)))
    (case [(== n 12)] (return ((inst newRefBuf (array 12 any)) comp n)))
    (case [(== n 13)] (return ((inst newRefBuf (array 13 any)) comp n)))
    (case [(<= n 15)] (return ((inst newRefBuf (array 15 any)) comp n)))
    (case [(<= n 17)] (return ((inst newRefBuf (array 17 any)) comp n)))
    (case [(<= n 19)] (return ((inst newRefBuf (array 19 any)) comp n)))
    (case [(<= n 21)] (return ((inst newRefBuf (array 21 any)) comp n)))
    (case [(<= n 23)] (return ((inst newRefBuf (array 23 any)) comp n)))
    (case [(<= n 25)] (return ((inst newRefBuf (array 25 any)) comp n)))
    (case [(<= n 27)] (return ((inst newRefBuf (array 27 any)) comp n)))
    (case [(<= n 29)] (return ((inst newRefBuf (array 29 any)) comp n)))
    (case [(<= n 32)] (return ((inst newRefBuf (array 32 any)) comp n))))
  (addr (lit RefArray :Comp comp :A (make (slice any) n))))

(go/func RefArrayOf "RefArrayOf is an array initializer of T[], comp T's class.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} comp & ^{:tag (slice any)} a]
  (when (== a nil)
    (set! a (lit (slice any))))
  (addr (lit RefArray :Comp comp :A a)))

(go/method Ref ^any [^{:tag (* RefArray)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* RefArray)} t] (.ArrayClass (.-Comp t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* RefArray)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* RefArray)} t])
(go/method Is_Serializable [^{:tag (* RefArray)} t])
(go/method Clone__O ^any [^{:tag (* RefArray)} t] (.Copy t))
(go/method Copy "Copy is clone() at the array's type.\n"
  ^{:tag (* RefArray)} [^{:tag (* RefArray)} t]
  (let [r (newRefArray (.-Comp t) (conv int32 (len (.-A t))))]
    (copy (.-A r) (.-A t))
    r))

(go/method Store
  "Store is aastore, a[i] = v with Java's checks in Java's order: the index
(ArrayIndexOutOfBoundsException), then the store (ArrayStoreException naming v's class). The
common case (in range, null or an Object[]) is small enough for gc to inline.\n"
  [^{:tag (* RefArray)} t ^int32 i ^any v]
  (when (and (< (conv uint32 i) (conv uint32 (len (.-A t)))) (or (== v nil) (== (.-Comp t) Object_class)))
    (aset (.-A t) i v)
    (return))
  (.storeChecked t i v))

(go/method storeChecked "storeChecked is Store's checks and store, out of line.\n"
  [^{:tag (* RefArray)} t ^int32 i ^any v]
  (when (>= (conv uint32 i) (conv uint32 (len (.-A t))))
    (panic (IndexOutOfBounds (conv int i) (len (.-A t)))))
  (when (and (!= v nil) (!= (.-Comp t) Object_class) (not (.IsInstance_O__Z (.-Comp t) v)))
    (panic (ArrayStoreException_New_String (Str (.GoName (GetClass v))))))
  (aset (.-A t) i v))

(go/func newArray "newArray is new C[n] for any component class (Array.newInstance).\n"
  ^any [^{:tag (* Class)} comp ^int32 n]
  (switch comp
    (case [Prim_boolean] (return (NewBooleanArray n)))
    (case [Prim_byte] (return (NewByteArray n)))
    (case [Prim_char] (return (NewCharArray n)))
    (case [Prim_short] (return (NewShortArray n)))
    (case [Prim_int] (return (NewIntArray n)))
    (case [Prim_long] (return (NewLongArray n)))
    (case [Prim_float] (return (NewFloatArray n)))
    (case [Prim_double] (return (NewDoubleArray n)))
    (case [Prim_void] (panic (IllegalArgumentException_New))))
  (NewRefArray comp n))

(go/func Array_NewInstance_Class_I__O
  "Array_NewInstance_Class_I__O is java.lang.reflect.Array.newInstance(Class, int).\n"
  ^any [^{:tag (* Class)} comp ^int32 n]
  (newArray (NN comp) n))

(go/func NewMultiArray
  "NewMultiArray is multianewarray: new T[d0][d1]... for the array class cls with
len(dims) <= its rank dimensions given; every size is checked before any is allocated.\n"
  ^{:tag (* RefArray)} [^{:tag (* Class)} cls & ^{:tag (slice int32)} dims]
  (range [_ d dims]
    (when (< d 0)
      (panic (negativeArraySize d))))
  (assert (* RefArray) (multiArray cls dims)))

(go/func multiArray ^any [^{:tag (* Class)} cls ^{:tag (slice int32)} dims]
  (when (== (len dims) 1)
    (return (newArray (.-comp cls) (aget dims 0))))
  (let [a (NewRefArray (.-comp cls) (aget dims 0))]
    (range [i _ (.-A a)]
      (aset (.-A a) i (multiArray (.-comp cls) (subslice dims 1))))
    a))

;; ---- System.arraycopy

(go/func elemName ^string [^{:tag (* Class)} c]
  (when (.IsPrimitive__Z (.-comp c))
    (return (.GoName (.-comp c))))
  "object array")

(go/func arrayLength ^int [^any a]
  (type-switch [x a]
    (case [(* BooleanArray)] (return (len (.-A x))))
    (case [(* ByteArray)] (return (len (.-A x))))
    (case [(* CharArray)] (return (len (.-A x))))
    (case [(* ShortArray)] (return (len (.-A x))))
    (case [(* IntArray)] (return (len (.-A x))))
    (case [(* LongArray)] (return (len (.-A x))))
    (case [(* FloatArray)] (return (len (.-A x))))
    (case [(* DoubleArray)] (return (len (.-A x))))
    (case [(* RefArray)] (return (len (.-A x)))))
  -1)

(go/func Arraycopy
  "Arraycopy is System.arraycopy, with HotSpot's checks and messages in its order: null
(NullPointerException), not arrays or mismatched element types (ArrayStoreException), negative
positions or length, then ranges (ArrayIndexOutOfBoundsException); overlapping copies within
one array are correct; a reference copy that needs element checks copies up to the first
element that fails and throws ArrayStoreException.\n"
  [^any src ^int32 srcPos ^any dst ^int32 dstPos ^int32 n]
  ;; the common case first: reference arrays whose element types need no checks, in range
  (let [(values x xok) (assert (* RefArray) src)
        (values y yok) (assert (* RefArray) dst)]
    (when (and xok yok (!= x nil) (!= y nil)
               (or (== x y) (== (.-Comp y) Object_class) (== (.-Comp y) (.-Comp x)))
               (>= srcPos 0) (>= dstPos 0) (>= n 0)
               (<= (+ (conv int64 srcPos) (conv int64 n)) (conv int64 (len (.-A x))))
               (<= (+ (conv int64 dstPos) (conv int64 n)) (conv int64 (len (.-A y)))))
      (copy (subslice (.-A y) dstPos) (subslice (.-A x) srcPos (+ srcPos n)))
      (return)))
  (when (or (== src nil) (== dst nil))
    (panic (NPE)))
  (let [sc (GetClass src)
        dc (GetClass dst)]
    (when (not (.IsArray__Z sc))
      (panic (ArrayStoreException_New_String (Str (+ "arraycopy: source type " (.GoName sc) " is not an array")))))
    (when (not (.IsArray__Z dc))
      (panic (ArrayStoreException_New_String (Str (+ "arraycopy: destination type " (.GoName dc) " is not an array")))))
    (let [sp (.IsPrimitive__Z (.-comp sc))
          dp (.IsPrimitive__Z (.-comp dc))]
      (when (or (!= sp dp) (and sp (!= (.-comp sc) (.-comp dc))))
        (panic (ArrayStoreException_New_String
                 (Str (+ "arraycopy: type mismatch: can not copy " (elemName sc) "[] into " (elemName dc) "[]"))))))
    (let [sl (arrayLength src)
          dl (arrayLength dst)
          en (elemName sc)]
      (cond
        (< srcPos 0)
        (panic (ArrayIndexOutOfBoundsException_New_String
                 (Str (+ "arraycopy: source index " (strconv/Itoa (conv int srcPos)) " out of bounds for " en "[" (strconv/Itoa sl) "]"))))
        (< dstPos 0)
        (panic (ArrayIndexOutOfBoundsException_New_String
                 (Str (+ "arraycopy: destination index " (strconv/Itoa (conv int dstPos)) " out of bounds for " (elemName dc) "[" (strconv/Itoa dl) "]"))))
        (< n 0)
        (panic (ArrayIndexOutOfBoundsException_New_String
                 (Str (+ "arraycopy: length " (strconv/Itoa (conv int n)) " is negative")))))
      (let [se (+ (conv uint32 n) (conv uint32 srcPos))
            de (+ (conv uint32 n) (conv uint32 dstPos))]
        (when (> se (conv uint32 sl))
          (panic (ArrayIndexOutOfBoundsException_New_String
                   (Str (+ "arraycopy: last source index " (strconv/FormatUint (conv uint64 se) 10) " out of bounds for " en "[" (strconv/Itoa sl) "]")))))
        (when (> de (conv uint32 dl))
          (panic (ArrayIndexOutOfBoundsException_New_String
                   (Str (+ "arraycopy: last destination index " (strconv/FormatUint (conv uint64 de) 10) " out of bounds for " (elemName dc) "[" (strconv/Itoa dl) "]")))))))
    (when (== n 0)
      (return))
    (let [s (conv int srcPos)
          d (conv int dstPos)
          e (conv int n)]
      (type-switch [x src]
        (case [(* BooleanArray)] (copy (subslice (.-A (assert (* BooleanArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* ByteArray)] (copy (subslice (.-A (assert (* ByteArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* CharArray)] (copy (subslice (.-A (assert (* CharArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* ShortArray)] (copy (subslice (.-A (assert (* ShortArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* IntArray)] (copy (subslice (.-A (assert (* IntArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* LongArray)] (copy (subslice (.-A (assert (* LongArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* FloatArray)] (copy (subslice (.-A (assert (* FloatArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* DoubleArray)] (copy (subslice (.-A (assert (* DoubleArray) dst)) d) (subslice (.-A x) s (+ s e))))
        (case [(* RefArray)]
          (let [y (assert (* RefArray) dst)]
            (if (or (== x y) (.assignableFrom (.-Comp y) (.-Comp x)))
              (copy (subslice (.-A y) d) (subslice (.-A x) s (+ s e)))
              (for [i 0] (< i e) (inc! i)
                (let [v (aget (.-A x) (+ s i))]
                  (when (and (!= v nil) (not (.IsInstance_O__Z (.-Comp y) v)))
                    (if (not (.assignableFrom (.-Comp x) (.-Comp y)))
                      (panic (ArrayStoreException_New_String
                               (Str (+ "arraycopy: type mismatch: can not copy " (.GoName (.-Comp x)) "[] into " (.GoName (.-Comp y)) "[]"))))
                      (panic (ArrayStoreException_New_String
                               (Str (+ "arraycopy: element type mismatch: can not cast one of the elements of "
                                       (.GoName (.-Comp x)) "[] to the type of the destination array, " (.GoName (.-Comp y))))))))
                  (aset (.-A y) (+ d i) v))))))))))

(go/func System_Arraycopy_O_I_O_I_I__V
  "System_Arraycopy_O_I_O_I_I__V is System.arraycopy (System is phase 2's; this member is the
object model's).\n"
  [^any src ^int32 srcPos ^any dst ^int32 dstPos ^int32 n]
  (Arraycopy src srcPos dst dstPos n))

(go/func System_IdentityHashCode_O__I "System_IdentityHashCode_O__I is System.identityHashCode.\n"
  ^int32 [^any x]
  (IdentityHash x))
