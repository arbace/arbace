;; jrt: the operations c2g compiles VarHandle access modes to (C2G-SPEC §8.5, amendment JC1;
;; doc/go/JRT-NOTES.md, "Concurrency"). A constant VarHandle on a field becomes an operation on
;; the field's volatile representation (§8.2: atomic.Int32, atomic.Int64, atomic.Bool,
;; atomic.Uint32 and atomic.Uint64 holding a float's or a double's bits, atomic.Pointer,
;; Volatile); c2g writes the simple ones as the atomic type's methods (Load, Store,
;; CompareAndSwap, Swap, Or, And) and calls these functions for the others. A handle on the
;; elements of an int[], long[] or reference array calls the Vh*Array* functions: int and long
;; elements by sync/atomic on the slice's element, reference elements (two-word any slots)
;; under the striped locks of Unsafe's reference slots (§9.2), so that they are atomic with
;; respect to each other and to Unsafe's. Go's atomics are sequentially consistent: every
;; access mode (plain, opaque, acquire/release, volatile) is the volatile one, stronger than
;; Java requires, and a weak compare-and-set never fails spuriously.
(in-ns 'go.arbace.jrt)

(go/file "varhandle.go"
  :imports [[math "math"] [sync "sync"] [atomic "sync/atomic"] [unsafe "unsafe"]])

;; ---------------------------------------------------------------------------------------
;; Fences

(go/func VhFence
  "VhFence is VarHandle's fullFence, acquireFence, releaseFence, loadLoadFence and
storeStoreFence: a sequentially consistent atomic operation (Go has no weaker fence).\n"
  []
  (.Add fence 0))

;; ---------------------------------------------------------------------------------------
;; Fields: compareAndExchange, getAndAdd, the bitwise modes Go's atomics lack

(go/func VhCmpXchgInt32
  "VhCmpXchgInt32 is compareAndExchange on an int field: the witness value, the new value
stored when it equals e.\n"
  ^int32 [^{:tag (* atomic/Int32)} p ^int32 e ^int32 n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgInt64 ^int64 [^{:tag (* atomic/Int64)} p ^int64 e ^int64 n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgUint32 "VhCmpXchgUint32 is compareAndExchange on a float field's bits.\n"
  ^uint32 [^{:tag (* atomic/Uint32)} p ^uint32 e ^uint32 n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgUint64 "VhCmpXchgUint64 is compareAndExchange on a double field's bits.\n"
  ^uint64 [^{:tag (* atomic/Uint64)} p ^uint64 e ^uint64 n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgBool ^bool [^{:tag (* atomic/Bool)} p ^bool e ^bool n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgPtr
  "VhCmpXchgPtr is compareAndExchange on a field of a pointer Go type (a leaf class, String,
an array).\n"
  :type-params [T] ^{:tag (* T)} [^{:tag (* (atomic/Pointer T))} p ^{:tag (* T)} e ^{:tag (* T)} n]
  (while true
    (let [v (.Load p)]
      (when (!= v e)
        (return v))
      (when (.CompareAndSwap p e n)
        (return e)))))

(go/func VhCmpXchgVol
  "VhCmpXchgVol is compareAndExchange on a field of an interface Go type (a Volatile):
references compared by identity.\n"
  :type-params [T] ^T [^{:tag (* (Volatile T))} p ^T e ^T n]
  (while true
    (let [v (.Load p)]
      (when (!= (conv any v) (conv any e))
        (return v))
      (when (.CompareAndSet p e n)
        (return e)))))

(go/func VhGetAndAddInt32 "VhGetAndAddInt32 is getAndAdd on an int field: the previous value.\n"
  ^int32 [^{:tag (* atomic/Int32)} p ^int32 d]
  (- (.Add p d) d))

(go/func VhGetAndAddInt64 ^int64 [^{:tag (* atomic/Int64)} p ^int64 d]
  (- (.Add p d) d))

(go/func VhGetAndAddFloat32 "VhGetAndAddFloat32 is getAndAdd on a float field (its bits).\n"
  ^float32 [^{:tag (* atomic/Uint32)} p ^float32 d]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (math/Float32bits (+ (math/Float32frombits v) d)))
        (return (math/Float32frombits v))))))

(go/func VhGetAndAddFloat64 "VhGetAndAddFloat64 is getAndAdd on a double field (its bits).\n"
  ^float64 [^{:tag (* atomic/Uint64)} p ^float64 d]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (math/Float64bits (+ (math/Float64frombits v) d)))
        (return (math/Float64frombits v))))))

(go/func VhGetAndXorInt32 ^int32 [^{:tag (* atomic/Int32)} p ^int32 x]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (bit-xor v x))
        (return v)))))

(go/func VhGetAndXorInt64 ^int64 [^{:tag (* atomic/Int64)} p ^int64 x]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (bit-xor v x))
        (return v)))))

(go/func VhGetAndOrBool "VhGetAndOrBool is getAndBitwiseOr on a boolean field.\n"
  ^bool [^{:tag (* atomic/Bool)} p ^bool x]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (or v x))
        (return v)))))

(go/func VhGetAndAndBool ^bool [^{:tag (* atomic/Bool)} p ^bool x]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (and v x))
        (return v)))))

(go/func VhGetAndXorBool ^bool [^{:tag (* atomic/Bool)} p ^bool x]
  (while true
    (let [v (.Load p)]
      (when (.CompareAndSwap p v (!= v x))
        (return v)))))

;; ---------------------------------------------------------------------------------------
;; Array elements

(go/func vhIntElem
  "vhIntElem is the address of a[i], with Java's NullPointerException and
ArrayIndexOutOfBoundsException.\n"
  ^{:tag (* int32)} [^{:tag (* IntArray)} a ^int32 i]
  (when (== a nil)
    (panic (NPE)))
  (when (>= (conv uint32 i) (conv uint32 (len (.-A a))))
    (panic (IndexOutOfBounds (conv int i) (len (.-A a)))))
  (addr (aget (.-A a) i)))

(go/func vhLongElem ^{:tag (* int64)} [^{:tag (* LongArray)} a ^int32 i]
  (when (== a nil)
    (panic (NPE)))
  (when (>= (conv uint32 i) (conv uint32 (len (.-A a))))
    (panic (IndexOutOfBounds (conv int i) (len (.-A a)))))
  (addr (aget (.-A a) i)))

(go/func VhIntArrayGet "VhIntArrayGet is an int[] handle's get (every get mode).\n"
  ^int32 [^{:tag (* IntArray)} a ^int32 i]
  (atomic/LoadInt32 (vhIntElem a i)))
(go/func VhIntArraySet [^{:tag (* IntArray)} a ^int32 i ^int32 x]
  (atomic/StoreInt32 (vhIntElem a i) x))
(go/func VhIntArrayCas ^bool [^{:tag (* IntArray)} a ^int32 i ^int32 e ^int32 x]
  (atomic/CompareAndSwapInt32 (vhIntElem a i) e x))
(go/func VhIntArrayCmpXchg ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 e ^int32 x]
  (let [p (vhIntElem a i)]
    (while true
      (let [v (atomic/LoadInt32 p)]
        (when (!= v e)
          (return v))
        (when (atomic/CompareAndSwapInt32 p e x)
          (return e))))))
(go/func VhIntArraySwap ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 x]
  (atomic/SwapInt32 (vhIntElem a i) x))
(go/func VhIntArrayGetAndAdd ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 d]
  (- (atomic/AddInt32 (vhIntElem a i) d) d))
(go/func VhIntArrayGetAndOr ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 x]
  (atomic/OrInt32 (vhIntElem a i) x))
(go/func VhIntArrayGetAndAnd ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 x]
  (atomic/AndInt32 (vhIntElem a i) x))
(go/func VhIntArrayGetAndXor ^int32 [^{:tag (* IntArray)} a ^int32 i ^int32 x]
  (let [p (vhIntElem a i)]
    (while true
      (let [v (atomic/LoadInt32 p)]
        (when (atomic/CompareAndSwapInt32 p v (bit-xor v x))
          (return v))))))

(go/func VhLongArrayGet "VhLongArrayGet is a long[] handle's get (every get mode).\n"
  ^int64 [^{:tag (* LongArray)} a ^int32 i]
  (atomic/LoadInt64 (vhLongElem a i)))
(go/func VhLongArraySet [^{:tag (* LongArray)} a ^int32 i ^int64 x]
  (atomic/StoreInt64 (vhLongElem a i) x))
(go/func VhLongArrayCas ^bool [^{:tag (* LongArray)} a ^int32 i ^int64 e ^int64 x]
  (atomic/CompareAndSwapInt64 (vhLongElem a i) e x))
(go/func VhLongArrayCmpXchg ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 e ^int64 x]
  (let [p (vhLongElem a i)]
    (while true
      (let [v (atomic/LoadInt64 p)]
        (when (!= v e)
          (return v))
        (when (atomic/CompareAndSwapInt64 p e x)
          (return e))))))
(go/func VhLongArraySwap ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 x]
  (atomic/SwapInt64 (vhLongElem a i) x))
(go/func VhLongArrayGetAndAdd ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 d]
  (- (atomic/AddInt64 (vhLongElem a i) d) d))
(go/func VhLongArrayGetAndOr ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 x]
  (atomic/OrInt64 (vhLongElem a i) x))
(go/func VhLongArrayGetAndAnd ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 x]
  (atomic/AndInt64 (vhLongElem a i) x))
(go/func VhLongArrayGetAndXor ^int64 [^{:tag (* LongArray)} a ^int32 i ^int64 x]
  (let [p (vhLongElem a i)]
    (while true
      (let [v (atomic/LoadInt64 p)]
        (when (atomic/CompareAndSwapInt64 p v (bit-xor v x))
          (return v))))))

(go/func vhRefElem
  "vhRefElem is the address of a[i] and its striped lock (§9.2), with Java's
NullPointerException and ArrayIndexOutOfBoundsException.\n"
  [^{:tag (* RefArray)} a ^int32 i] :results [(* any) (* sync/Mutex)]
  (when (== a nil)
    (panic (NPE)))
  (when (>= (conv uint32 i) (conv uint32 (len (.-A a))))
    (panic (IndexOutOfBounds (conv int i) (len (.-A a)))))
  (let [p (addr (aget (.-A a) i))]
    (return p (stripe (conv unsafe/Pointer p)))))

(go/func vhRefCheck
  "vhRefCheck is the ArrayStoreException of storing x into a (a handle checks its value against
the array's component type).\n"
  [^{:tag (* RefArray)} a ^any x]
  (when (and (!= x nil) (!= (.-Comp a) Object_class) (not (.IsInstance_O__Z (.-Comp a) x)))
    (panic (ArrayStoreException_New_String (Str (.GoName (GetClass x)))))))

(go/func VhRefArrayGet "VhRefArrayGet is a reference array handle's get (every get mode).\n"
  ^any [^{:tag (* RefArray)} a ^int32 i]
  (let [(values p m) (vhRefElem a i)]
    (.Lock m)
    (let [v @p]
      (.Unlock m)
      v)))
(go/func VhRefArraySet [^{:tag (* RefArray)} a ^int32 i ^any x]
  (let [(values p m) (vhRefElem a i)]
    (vhRefCheck a x)
    (.Lock m)
    (set! @p x)
    (.Unlock m)))
(go/func VhRefArrayCas "VhRefArrayCas is compareAndSet: references compared by identity.\n"
  ^bool [^{:tag (* RefArray)} a ^int32 i ^any e ^any x]
  (let [(values p m) (vhRefElem a i)]
    (vhRefCheck a x)
    (.Lock m)
    (defer (.Unlock m))
    (when (!= @p e)
      (return false))
    (set! @p x)
    true))
(go/func VhRefArrayCmpXchg ^any [^{:tag (* RefArray)} a ^int32 i ^any e ^any x]
  (let [(values p m) (vhRefElem a i)]
    (vhRefCheck a x)
    (.Lock m)
    (defer (.Unlock m))
    (let [v @p]
      (when (== v e)
        (set! @p x))
      v)))
(go/func VhRefArraySwap ^any [^{:tag (* RefArray)} a ^int32 i ^any x]
  (let [(values p m) (vhRefElem a i)]
    (vhRefCheck a x)
    (.Lock m)
    (defer (.Unlock m))
    (let [v @p]
      (set! @p x)
      v)))
