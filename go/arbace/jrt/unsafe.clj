;; jrt: jdk.internal.misc.Unsafe, the members the translated JDK classes use (C2G-SPEC §9.2;
;; JRT-SOURCES.md, the edge's 39; doc/go/JRT-NOTES.md, phase 2a).
;;
;; Offsets. An array offset is Java's: base + index * scale, with the JVM's bases (16) and
;; scales (1, 2, 4, 8; 4 for references, as compressed oops), so Java's own arithmetic on
;; them (ConcurrentHashMap's ABASE + (i << ASHIFT), ArraysSupport's byte offsets) holds;
;; Unsafe maps it back to the element (or the bytes) of the Go slice. A field offset is a key
;; into jrt's table of fields, made by objectFieldOffset from the class's Go struct type
;; (RegisterGoType) and the field's Go name F_name: its Go offset and how its Go type is
;; accessed.
;;
;; Atomicity. int and long slots are accessed with sync/atomic (a volatile field's Go type
;; is atomic.Int32/Int64, the same memory). A reference slot of pointer Go type (a leaf
;; class, String, an array; atomic.Pointer for a volatile one) is one word: atomic pointer
;; operations. A reference slot of interface Go type (two words: any, C_I) is accessed under
;; one of 64 striped locks keyed by its address (§9.2); every access ConcurrentHashMap makes
;; to its table goes through Unsafe, so they are atomic with respect to each other. A
;; volatile interface field (jrt.Volatile) goes through its own CAS.
(in-ns 'go.arbace.jrt)

(go/file "unsafe.go"
  :imports [[binary "encoding/binary"] [math "math"] [reflect "reflect"] [sync "sync"] [atomic "sync/atomic"]
            [unsafe "unsafe"]])

(go/type Unsafe "Unsafe is jdk.internal.misc.Unsafe (leaf; one instance).\n" (struct Object))

(go/var Unsafe_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.misc.Unsafe" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Go "arbace/jrt.Unsafe"))))

(go/var theUnsafe (addr (lit Unsafe)))

(go/func Unsafe_GetUnsafe__Unsafe "Unsafe_GetUnsafe__Unsafe is Unsafe.getUnsafe().\n" ^{:tag (* Unsafe)} []
  theUnsafe)

(go/const ^{:tag int64 :val 16} arrayBase 16)

(go/var
  [^int64 Unsafe_ARRAY_BOOLEAN_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_BYTE_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_SHORT_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_CHAR_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_INT_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_LONG_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_FLOAT_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_DOUBLE_BASE_OFFSET arrayBase]
  [^int64 Unsafe_ARRAY_OBJECT_BASE_OFFSET arrayBase]
  [^int32 Unsafe_ARRAY_BOOLEAN_INDEX_SCALE 1]
  [^int32 Unsafe_ARRAY_BYTE_INDEX_SCALE 1]
  [^int32 Unsafe_ARRAY_SHORT_INDEX_SCALE 2]
  [^int32 Unsafe_ARRAY_CHAR_INDEX_SCALE 2]
  [^int32 Unsafe_ARRAY_INT_INDEX_SCALE 4]
  [^int32 Unsafe_ARRAY_LONG_INDEX_SCALE 8]
  [^int32 Unsafe_ARRAY_FLOAT_INDEX_SCALE 4]
  [^int32 Unsafe_ARRAY_DOUBLE_INDEX_SCALE 8]
  [^int32 Unsafe_ARRAY_OBJECT_INDEX_SCALE 4]
  [^int32 Unsafe_ADDRESS_SIZE 8])

(go/method ArrayBaseOffset_Class__J "ArrayBaseOffset_Class__J is arrayBaseOffset: 16.\n"
  ^int64 [^{:tag (* Unsafe)} u ^{:tag (* Class)} c]
  (when (not (.IsArray__Z (NN c)))
    (panic (IllegalArgumentException_New)))
  arrayBase)

(go/method ArrayIndexScale_Class__I "ArrayIndexScale_Class__I is arrayIndexScale: the JVM's scales.\n"
  ^int32 [^{:tag (* Unsafe)} u ^{:tag (* Class)} c]
  (when (not (.IsArray__Z (NN c)))
    (panic (IllegalArgumentException_New)))
  (switch (.-desc (.-comp c))
    (case [\Z \B] (return 1))
    (case [\C \S] (return 2))
    (case [\J \D] (return 8)))
  4)

(go/method IsBigEndian__Z ^bool [^{:tag (* Unsafe)} u] false)
(go/method AddressSize__I ^int32 [^{:tag (* Unsafe)} u] 8)

;; ---------------------------------------------------------------------------------------
;; Go types of classes, and the field table

(go/var ^{:tag sync/Map :doc "goTypes maps a *Class to its Go struct's reflect.Type.\n"} goTypes)

(go/func RegisterGoType
  "RegisterGoType records the Go struct type of class c, for Unsafe.objectFieldOffset (c2g
registers each class whose fields the translated code names to objectFieldOffset).\n"
  [^{:tag (* Class)} c ^{:tag reflect/Type} t]
  (.Store goTypes c t))

(go/const
  [^{:tag int :val 0} slotInt32 iota]
  [^{:val 1} slotInt64]
  [^{:val 2} slotFloat32]
  [^{:val 3} slotFloat64]
  [^{:val 4} slotInt8]
  [^{:val 5} slotInt16]
  [^{:val 6} slotUint16]
  [^{:val 7} slotBool]
  [^{:val 8} slotPointer]
  [^{:val 9} slotAny]
  [^{:val 10} slotIface]
  [^{:val 11} slotVolatile])

(go/type unsafeField
  "unsafeField is one field objectFieldOffset handed out: its offset in the Go struct, how it
is accessed, and its Go type (the pointer's type for a pointer slot, an atomic.Pointer's
element pointer type included).\n"
  (struct ^uintptr off ^int kind ^{:tag reflect/Type} typ ^{:tag unsafe/Pointer} typeWord))

(go/const ^{:tag int64 :val 1099511627776} fieldTag (<< 1 40))

(go/var ^{:tag sync/Mutex} fieldsMu)
(go/var ^{:tag (atomic/Pointer (slice (* unsafeField)))} fieldsTable)
(go/var ^{:tag (map string int64)} fieldKeys (make (map string int64)))

(go/type eface "eface is the layout of an any: the type word and the data word.\n"
  (struct ^unsafe/Pointer typ ^unsafe/Pointer data))

(go/func dataOf "dataOf is x's data word: the object's address for a Java object.\n"
  ^unsafe/Pointer [^any x]
  (.-data @(conv (* eface) (conv unsafe/Pointer (addr x)))))

(go/func typeWordOf ^unsafe/Pointer [^any x]
  (.-typ @(conv (* eface) (conv unsafe/Pointer (addr x)))))

(go/func makeAny "makeAny is the any of a type word and a non-nil data word.\n"
  [^unsafe/Pointer typ ^unsafe/Pointer data] :results [^any x]
  (let [e (conv (* eface) (conv unsafe/Pointer (addr x)))]
    (set! (.-typ e) typ)
    (set! (.-data e) data)
    (return)))

(go/var
  [^{:tag reflect/Type} atomicInt32Type ((inst reflect/TypeFor atomic/Int32))]
  [^{:tag reflect/Type} atomicInt64Type ((inst reflect/TypeFor atomic/Int64))]
  [^{:tag reflect/Type} atomicBoolType ((inst reflect/TypeFor atomic/Bool))]
  [^{:tag reflect/Type} volatileRefType ((inst reflect/TypeFor volatileRef))])

(go/func fieldKind
  "fieldKind is how a field of Go type t is accessed, and the pointer type of a pointer slot.\n"
  [^{:tag reflect/Type} t] :results [int reflect/Type]
  (switch t
    (case [atomicInt32Type] (return slotInt32 nil))
    (case [atomicInt64Type] (return slotInt64 nil))
    (case [atomicBoolType] (return slotBool nil)))
  (switch (.Kind t)
    (case [reflect/Int32] (return slotInt32 nil))
    (case [reflect/Int64] (return slotInt64 nil))
    (case [reflect/Float32] (return slotFloat32 nil))
    (case [reflect/Float64] (return slotFloat64 nil))
    (case [reflect/Int8] (return slotInt8 nil))
    (case [reflect/Int16] (return slotInt16 nil))
    (case [reflect/Uint16] (return slotUint16 nil))
    (case [reflect/Bool] (return slotBool nil))
    (case [reflect/Pointer] (return slotPointer t))
    (case [reflect/Interface]
      (when (== (.NumMethod t) 0)
        (return slotAny nil))
      (return slotIface t))
    (case [reflect/Struct]
      ;; atomic.Pointer[T]: its Load's result is the pointer type
      (when (== (.PkgPath t) "sync/atomic")
        (let [(values m ok) (.MethodByName (reflect/PointerTo t) "Load")]
          (when (and ok (== (.NumOut (.-Type m)) 1) (== (.Kind (.Out (.-Type m) 0)) reflect/Pointer))
            (return slotPointer (.Out (.-Type m) 0)))))
      (when (.Implements (reflect/PointerTo t) volatileRefType)
        (return slotVolatile t))))
  (return -1 nil))

(go/method ObjectFieldOffset_Class_String__J
  "ObjectFieldOffset_Class_String__J is objectFieldOffset(Class, String): the key of the
field F_name of c's Go struct (found through embedded superclasses too); InternalError when
c has no registered Go type or no such field, as the JVM's for a missing field.\n"
  ^int64 [^{:tag (* Unsafe)} u ^{:tag (* Class)} c ^{:tag (* String)} name]
  (let [n (.String (NN name))
        key (+ (.GoName (NN c)) "." n)]
    (.Lock fieldsMu)
    (defer (.Unlock fieldsMu))
    (let [(values k ok) (aget fieldKeys key)]
      (when ok
        (return k)))
    (let [(values tv ok) (.Load goTypes c)]
      (when (not ok)
        (panic (InternalError_New_String (Str (+ "Unsafe.objectFieldOffset: no Go type registered for " (.GoName c))))))
      (let [t (assert reflect/Type tv)
            (values sf found) (.FieldByName t (+ "F_" n))]
        (when (not found)
          (panic (InternalError_New_String (Str n))))
        ;; the offset through the embedded structs
        (let [^uintptr off 0
              st t]
          (range [_ i (.-Index sf)]
            (let [f (.Field st i)]
              (set! off + (.-Offset f))
              (set! st (.-Type f))))
          (let [(values kind pt) (fieldKind (.-Type sf))]
            (when (< kind 0)
              (panic (InternalError_New_String (Str (+ "Unsafe: field " n " of Go type " (.String (.-Type sf)))))))
            (let [f (addr (lit unsafeField :off off :kind kind :typ (.-Type sf)))]
              (when (== kind slotPointer)
                (set! (.-typ f) pt)
                (set! (.-typeWord f) (typeWordOf (.Interface (reflect/Zero pt)))))
              (let [old (.Load fieldsTable)
                    ^{:tag (slice (* unsafeField))} tab nil]
                (when (!= old nil)
                  (set! tab @old))
                (let [nt (append (subslice tab _ (len tab) (len tab)) f)
                      k (+ fieldTag (conv int64 (- (len nt) 1)))]
                  (.Store fieldsTable (addr nt))
                  (aset fieldKeys key k)
                  k)))))))))

;; ---------------------------------------------------------------------------------------
;; Slots

(go/func arrayBytes
  "arrayBytes is the n bytes of the array o at Java offset off (base included), or nil when
o is not a primitive array.\n"
  ^{:tag (slice byte)} [^any o ^int64 off ^int64 n]
  (let [^unsafe/Pointer p nil
        ^int size 0]
    (type-switch [a o]
      (case [(* BooleanArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (len (.-A a))))
      (case [(* ByteArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (len (.-A a))))
      (case [(* CharArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 2 (len (.-A a)))))
      (case [(* ShortArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 2 (len (.-A a)))))
      (case [(* IntArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 4 (len (.-A a)))))
      (case [(* FloatArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 4 (len (.-A a)))))
      (case [(* LongArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 8 (len (.-A a)))))
      (case [(* DoubleArray)] (set! p (conv unsafe/Pointer (unsafe/SliceData (.-A a)))) (set! size (* 8 (len (.-A a)))))
      (default (return nil)))
    (let [i (- off arrayBase)]
      (when (or (< i 0) (> (+ i n) (conv int64 size)))
        (panic (IndexOutOfBounds (conv int i) size)))
      (subslice (unsafe/Slice (conv (* byte) p) size) i (+ i n)))))

(go/func fieldSlot
  "fieldSlot is the address of the field with key off in the object o, and the field.\n"
  [^any o ^int64 off] :results [unsafe/Pointer (* unsafeField)]
  (when (== o nil)
    (panic (NPE)))
  (let [tab (.Load fieldsTable)
        i (- off fieldTag)]
    (when (or (== tab nil) (< i 0) (>= i (conv int64 (len @tab))))
      (panic (InternalError_New_String (Str "Unsafe: not a field offset"))))
    (let [f (aget @tab i)]
      (return (unsafe/Add (dataOf o) (.-off f)) f))))

(go/func refSlot
  "refSlot is the address of a reference element (RefArray at Java offset off) or field, and
its field (nil for an array element, an any slot).\n"
  [^any o ^int64 off] :results [unsafe/Pointer (* unsafeField)]
  (let [(values a ok) (assert (* RefArray) o)]
    (when ok
      (let [i (- off arrayBase)]
        (when (or (< i 0) (!= (% i 4) 0))
          (panic (IndexOutOfBounds (conv int i) (len (.-A a)))))
        (return (conv unsafe/Pointer (addr (aget (.-A a) (/ i 4)))) nil))))
  (fieldSlot o off))

(go/func intSlot "intSlot is the address of an int element or field (aligned: atomic access).\n"
  ^{:tag (* int32)} [^any o ^int64 off]
  (let [(values a ok) (assert (* IntArray) o)]
    (when ok
      (let [i (- off arrayBase)]
        (return (addr (aget (.-A a) (/ i 4)))))))
  (let [(values p f) (fieldSlot o off)]
    (when (!= (.-kind f) slotInt32)
      (panic (InternalError_New_String (Str "Unsafe: not an int field"))))
    (conv (* int32) p)))

(go/func longSlot ^{:tag (* int64)} [^any o ^int64 off]
  (let [(values a ok) (assert (* LongArray) o)]
    (when ok
      (let [i (- off arrayBase)]
        (return (addr (aget (.-A a) (/ i 8)))))))
  (let [(values p f) (fieldSlot o off)]
    (when (!= (.-kind f) slotInt64)
      (panic (InternalError_New_String (Str "Unsafe: not a long field"))))
    (conv (* int64) p)))

(go/var ^{:tag (array 64 sync/Mutex) :doc "refStripes are the striped locks of two-word reference slots (§9.2).\n"} refStripes)

(go/func stripe ^{:tag (* sync/Mutex)} [^unsafe/Pointer p]
  (addr (aget refStripes (bit-and (>> (conv uintptr p) 4) 63))))

(go/func loadRef ^any [^unsafe/Pointer p ^{:tag (* unsafeField)} f]
  (let [kind slotAny]
    (when (!= f nil)
      (set! kind (.-kind f)))
    (switch kind
      (case [slotAny]
        (let [m (stripe p)]
          (.Lock m)
          (let [v @(conv (* any) p)]
            (.Unlock m)
            (return v))))
      (case [slotPointer]
        (let [d (atomic/LoadPointer (conv (* unsafe/Pointer) p))]
          (when (== d nil)
            (return nil))
          (return (makeAny (.-typeWord f) d))))
      (case [slotIface]
        (let [m (stripe p)]
          (.Lock m)
          (let [v (.Elem (reflect/NewAt (.-typ f) p))]
            (let [^any r nil]
              (when (not (.IsNil v))
                (set! r (.Interface v)))
              (.Unlock m)
              (return r)))))
      (case [slotVolatile]
        (return (.loadAny (assert volatileRef (.Interface (reflect/NewAt (.-typ f) p)))))))
    (panic (InternalError_New_String (Str "Unsafe: not a reference field")))))

(go/func setIface "setIface stores x into the interface slot v (nil: the zero value).\n"
  [^{:tag reflect/Value} v ^{:tag reflect/Type} t ^any x]
  (if (== x nil)
    (.Set v (reflect/Zero t))
    (.Set v (reflect/ValueOf x))))

(go/func storeRef [^unsafe/Pointer p ^{:tag (* unsafeField)} f ^any x]
  (let [kind slotAny]
    (when (!= f nil)
      (set! kind (.-kind f)))
    (switch kind
      (case [slotAny]
        (let [m (stripe p)]
          (.Lock m)
          (set! @(conv (* any) p) x)
          (.Unlock m)
          (return)))
      (case [slotPointer]
        (atomic/StorePointer (conv (* unsafe/Pointer) p) (dataOf x))
        (return))
      (case [slotIface]
        (let [m (stripe p)]
          (.Lock m)
          (setIface (.Elem (reflect/NewAt (.-typ f) p)) (.-typ f) x)
          (.Unlock m)
          (return)))
      (case [slotVolatile]
        (.storeAny (assert volatileRef (.Interface (reflect/NewAt (.-typ f) p))) x)
        (return)))
    (panic (InternalError_New_String (Str "Unsafe: not a reference field")))))

(go/func casRef ^bool [^unsafe/Pointer p ^{:tag (* unsafeField)} f ^any e ^any x]
  (let [kind slotAny]
    (when (!= f nil)
      (set! kind (.-kind f)))
    (switch kind
      (case [slotAny]
        (let [m (stripe p)]
          (.Lock m)
          (defer (.Unlock m))
          (when (!= @(conv (* any) p) e)
            (return false))
          (set! @(conv (* any) p) x)
          (return true)))
      (case [slotPointer]
        (return (atomic/CompareAndSwapPointer (conv (* unsafe/Pointer) p) (dataOf e) (dataOf x))))
      (case [slotIface]
        (let [m (stripe p)]
          (.Lock m)
          (defer (.Unlock m))
          (let [v (.Elem (reflect/NewAt (.-typ f) p))
                ^any cur nil]
            (when (not (.IsNil v))
              (set! cur (.Interface v)))
            (when (!= cur e)
              (return false))
            (setIface v (.-typ f) x)
            (return true))))
      (case [slotVolatile]
        (return (.casAny (assert volatileRef (.Interface (reflect/NewAt (.-typ f) p))) e x))))
    (panic (InternalError_New_String (Str "Unsafe: not a reference field")))))

;; ---------------------------------------------------------------------------------------
;; The operations

(go/method WeakCompareAndSetInt_O_J_I_I__Z "WeakCompareAndSetInt_O_J_I_I__Z: compareAndSetInt (Go's CAS does not fail spuriously).\n"
  ^bool [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 e ^int32 x]
  (.CompareAndSetInt_O_J_I_I__Z u o off e x))

(go/method CompareAndSetInt_O_J_I_I__Z ^bool [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 e ^int32 x]
  (atomic/CompareAndSwapInt32 (intSlot o off) e x))

(go/method CompareAndSetLong_O_J_J_J__Z ^bool [^{:tag (* Unsafe)} u ^any o ^int64 off ^int64 e ^int64 x]
  (atomic/CompareAndSwapInt64 (longSlot o off) e x))

(go/method CompareAndSetReference_O_J_O_O__Z ^bool [^{:tag (* Unsafe)} u ^any o ^int64 off ^any e ^any x]
  (let [(values p f) (refSlot o off)]
    (casRef p f e x)))

(go/method WeakCompareAndSetReference_O_J_O_O__Z
  "WeakCompareAndSetReference_O_J_O_O__Z: compareAndSetReference (Go's CAS does not fail
spuriously; AbstractQueuedSynchronizer, StampedLock).\n"
  ^bool [^{:tag (* Unsafe)} u ^any o ^int64 off ^any e ^any x]
  (.CompareAndSetReference_O_J_O_O__Z u o off e x))

(go/method GetAndBitwiseAndInt_O_J_I__I "GetAndBitwiseAndInt_O_J_I__I: the previous value (AbstractQueuedSynchronizer).\n"
  ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 m]
  (atomic/AndInt32 (intSlot o off) m))

(go/method PutIntOpaque_O_J_I__V "PutIntOpaque_O_J_I__V: a volatile store (Go has no weaker atomic one).\n"
  [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 x]
  (atomic/StoreInt32 (intSlot o off) x))

(go/method GetLongOpaque_O_J__J "GetLongOpaque_O_J__J: a volatile load (StampedLock).\n"
  ^int64 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (atomic/LoadInt64 (longSlot o off)))

(go/method Park_Z_J__V
  "Park_Z_J__V is Unsafe.park(isAbsolute, time), LockSupport's: until the permit, an interrupt,
the deadline in epoch milliseconds (absolute) or the nanoseconds (relative; 0: no limit).\n"
  [^{:tag (* Unsafe)} u ^bool isAbsolute ^int64 time]
  (cond
    isAbsolute (LockSupport_ParkUntil_J__V time)
    (== time 0) (LockSupport_Park__V)
    (> time 0) (LockSupport_ParkNanos_J__V time)))

(go/method Unpark_O__V "Unpark_O__V is Unsafe.unpark(thread).\n"
  [^{:tag (* Unsafe)} u ^any thread]
  (when (== thread nil)
    (return))
  (LockSupport_Unpark_Thread__V (assert Thread_I thread)))

(go/method GetAndAddInt_O_J_I__I ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 d]
  (- (atomic/AddInt32 (intSlot o off) d) d))

(go/method GetAndAddLong_O_J_J__J ^int64 [^{:tag (* Unsafe)} u ^any o ^int64 off ^int64 d]
  (- (atomic/AddInt64 (longSlot o off) d) d))

(go/method GetAndSetInt_O_J_I__I ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 x]
  (atomic/SwapInt32 (intSlot o off) x))

(go/method GetIntVolatile_O_J__I ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (atomic/LoadInt32 (intSlot o off)))
(go/method PutIntVolatile_O_J_I__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 x]
  (atomic/StoreInt32 (intSlot o off) x))
(go/method GetLongVolatile_O_J__J ^int64 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (atomic/LoadInt64 (longSlot o off)))
(go/method PutLongVolatile_O_J_J__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int64 x]
  (atomic/StoreInt64 (longSlot o off) x))

(go/method GetReference_O_J__O ^any [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [(values p f) (refSlot o off)] (loadRef p f)))
(go/method GetReferenceAcquire_O_J__O ^any [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [(values p f) (refSlot o off)] (loadRef p f)))
(go/method GetReferenceVolatile_O_J__O ^any [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [(values p f) (refSlot o off)] (loadRef p f)))
(go/method PutReference_O_J_O__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^any x]
  (let [(values p f) (refSlot o off)] (storeRef p f x)))
(go/method PutReferenceRelease_O_J_O__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^any x]
  (let [(values p f) (refSlot o off)] (storeRef p f x)))
(go/method PutReferenceVolatile_O_J_O__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^any x]
  (let [(values p f) (refSlot o off)] (storeRef p f x)))

(go/method PutInt_O_J_I__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 x]
  (let [b (arrayBytes o off 4)]
    (when (!= b nil)
      (.PutUint32 binary/LittleEndian b (conv uint32 x))
      (return)))
  (atomic/StoreInt32 (intSlot o off) x))

(go/method PutLong_O_J_J__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int64 x]
  (let [b (arrayBytes o off 8)]
    (when (!= b nil)
      (.PutUint64 binary/LittleEndian b (conv uint64 x))
      (return)))
  (atomic/StoreInt64 (longSlot o off) x))

(go/method GetInt_O_J__I ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [b (arrayBytes o off 4)]
    (when (!= b nil)
      (return (conv int32 (.Uint32 binary/LittleEndian b)))))
  (atomic/LoadInt32 (intSlot o off)))

(go/method GetLong_O_J__J ^int64 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [b (arrayBytes o off 8)]
    (when (!= b nil)
      (return (conv int64 (.Uint64 binary/LittleEndian b)))))
  (atomic/LoadInt64 (longSlot o off)))

(go/method PutFloat_O_J_F__V "PutFloat_O_J_F__V: a float field (HashMap.loadFactor) or element.\n"
  [^{:tag (* Unsafe)} u ^any o ^int64 off ^float32 x]
  (let [b (arrayBytes o off 4)]
    (when (!= b nil)
      (.PutUint32 binary/LittleEndian b (math/Float32bits x))
      (return)))
  (let [(values p f) (fieldSlot o off)]
    (when (!= (.-kind f) slotFloat32)
      (panic (InternalError_New_String (Str "Unsafe: not a float field"))))
    (atomic/StoreUint32 (conv (* uint32) p) (math/Float32bits x))))

(go/method PutByte_O_J_B__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int8 x]
  (let [b (arrayBytes o off 1)]
    (when (!= b nil)
      (aset b 0 (conv byte x))
      (return)))
  (let [(values p f) (fieldSlot o off)]
    (when (!= (.-kind f) slotInt8)
      (panic (InternalError_New_String (Str "Unsafe: not a byte field"))))
    (set! @(conv (* int8) p) x)))

(go/method GetByte_O_J__B ^int8 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (let [b (arrayBytes o off 1)]
    (when (!= b nil)
      (return (conv int8 (aget b 0)))))
  (let [(values p f) (fieldSlot o off)]
    (when (!= (.-kind f) slotInt8)
      (panic (InternalError_New_String (Str "Unsafe: not a byte field"))))
    @(conv (* int8) p)))

(go/method GetIntUnaligned_O_J__I "GetIntUnaligned_O_J__I: 4 bytes of a primitive array, little-endian (the native order).\n"
  ^int32 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (conv int32 (.Uint32 binary/LittleEndian (unalignedBytes o off 4))))

(go/method GetLongUnaligned_O_J__J ^int64 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (conv int64 (.Uint64 binary/LittleEndian (unalignedBytes o off 8))))

(go/method GetCharUnaligned_O_J__C ^uint16 [^{:tag (* Unsafe)} u ^any o ^int64 off]
  (.Uint16 binary/LittleEndian (unalignedBytes o off 2)))

(go/method PutCharUnaligned_O_J_C__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^uint16 x]
  (.PutUint16 binary/LittleEndian (unalignedBytes o off 2) x))

(go/method PutIntUnaligned_O_J_I__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int32 x]
  (.PutUint32 binary/LittleEndian (unalignedBytes o off 4) (conv uint32 x)))

(go/method PutLongUnaligned_O_J_J__V [^{:tag (* Unsafe)} u ^any o ^int64 off ^int64 x]
  (.PutUint64 binary/LittleEndian (unalignedBytes o off 8) (conv uint64 x)))

(go/func unalignedBytes ^{:tag (slice byte)} [^any o ^int64 off ^int64 n]
  (when (== o nil)
    (panic (NPE)))
  (let [b (arrayBytes o off n)]
    (when (== b nil)
      (panic (InternalError_New_String (Str "Unsafe: unaligned access outside a primitive array"))))
    b))

(go/var ^{:tag atomic/Int32} fence)

(go/method StoreFence__V "StoreFence__V: a sequentially consistent atomic operation (Go has no weaker fence).\n"
  [^{:tag (* Unsafe)} u] (.Add fence 0))
(go/method LoadFence__V [^{:tag (* Unsafe)} u] (.Load fence))
(go/method StoreStoreFence__V [^{:tag (* Unsafe)} u] (.Add fence 0))
(go/method FullFence__V [^{:tag (* Unsafe)} u] (.Add fence 0))

(go/method Ref ^any [^{:tag (* Unsafe)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Unsafe)} t] Unsafe_class)
(go/method Clone__O ^any [^{:tag (* Unsafe)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Unsafe)} t] (Object_toString t))

(go/func init []
  (set! (.-IsInstance (.Info Unsafe_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* Unsafe) x)] ok))))
