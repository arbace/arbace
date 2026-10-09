;; jrt: java.util.concurrent.atomic (C2G-SPEC §8.2; doc/go/JRT-NOTES.md, phase 2a):
;; AtomicInteger, AtomicLong, AtomicBoolean over sync/atomic, AtomicReference over a
;; Volatile (an atomic pointer to a boxed two-word reference) with an identity CAS.
;; AtomicInteger and AtomicLong extend Number in Java: they embed Number (translated, a
;; stand-in in jrt's own build; amendment T12, done in step 5 phase 2B: (pos? (AtomicInteger.))
;; casts to Number) and define Number's methods themselves.
(in-ns 'go.arbace.jrt)

(go/file "atomic.go"
  :imports [[atomic "sync/atomic"]])

;; ---------------------------------------------------------------------------------------
;; Compare-and-set on a Volatile (references by identity)

(go/method CompareAndSet
  "CompareAndSet sets v to x when it holds expect (Java's ==: Go's == on the two values as
any, which compares pointers for Java objects) and reports whether it did. The CAS is on the
box pointer; a failed CAS whose value is still expect (another store of an identical
reference) retries, so the operation is linearizable as Java's.\n"
  ^bool [^{:tag (* (Volatile T))} v ^T expect ^T x]
  (while true
    (let [p (.Load (.-p v))
          ^any cur nil]
      (when (!= p nil)
        (set! cur (conv any @p)))
      (when (!= cur (conv any expect))
        (return false))
      (let [^{:tag (* T)} np nil]
        (when (!= (conv any x) nil)
          (set! np (addr x)))
        (when (.CompareAndSwap (.-p v) p np)
          (return true))))))

(go/method Swap "Swap stores x and returns the previous value (getAndSet).\n"
  ^T [^{:tag (* (Volatile T))} v ^T x]
  (let [^{:tag (* T)} np nil]
    (when (!= (conv any x) nil)
      (set! np (addr x)))
    (let [p (.Swap (.-p v) np)]
      (when (== p nil)
        (let [^T z (zero T)]
          (return z)))
      @p)))

;; Unsafe reaches a Volatile field of any T through this interface (unsafe.go)
(go/type volatileRef
  (interface
    (loadAny ^any [])
    (storeAny [^any x])
    (casAny ^bool [^any expect ^any x])))

(go/method loadAny ^any [^{:tag (* (Volatile T))} v] (conv any (.Load v)))
(go/method storeAny [^{:tag (* (Volatile T))} v ^any x]
  (let [^T y (zero T)]
    (when (!= x nil)
      (set! y (assert T x)))
    (.Store v y)))
(go/method casAny ^bool [^{:tag (* (Volatile T))} v ^any expect ^any x]
  (let [^T e (zero T)
        ^T y (zero T)]
    (when (!= expect nil)
      (let [(values ev ok) (assert T expect)]
        (when (not ok)
          (return false))
        (set! e ev)))
    (when (!= x nil)
      (set! y (assert T x)))
    (.CompareAndSet v e y)))

;; ---------------------------------------------------------------------------------------
;; AtomicInteger

(go/type AtomicInteger (struct Number ^{:tag atomic/Int32} v))

(go/var AtomicInteger_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.atomic.AtomicInteger" :Kind KindClass
                     :Modifiers AccPublic :Super Number_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.AtomicInteger"))))

(go/func AtomicInteger_New ^{:tag (* AtomicInteger)} [] (addr (lit AtomicInteger)))
(go/func AtomicInteger_New_I ^{:tag (* AtomicInteger)} [^int32 v]
  (let [a (addr (lit AtomicInteger))] (.Store (.-v a) v) a))

(go/method Get__I ^int32 [^{:tag (* AtomicInteger)} a] (.Load (.-v a)))
(go/method Set_I__V [^{:tag (* AtomicInteger)} a ^int32 x] (.Store (.-v a) x))
(go/method LazySet_I__V [^{:tag (* AtomicInteger)} a ^int32 x] (.Store (.-v a) x))
(go/method GetPlain__I ^int32 [^{:tag (* AtomicInteger)} a] (.Load (.-v a)))
(go/method SetPlain_I__V [^{:tag (* AtomicInteger)} a ^int32 x] (.Store (.-v a) x))
(go/method GetAcquire__I ^int32 [^{:tag (* AtomicInteger)} a] (.Load (.-v a)))
(go/method SetRelease_I__V [^{:tag (* AtomicInteger)} a ^int32 x] (.Store (.-v a) x))
(go/method GetAndSet_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 x] (.Swap (.-v a) x))
(go/method CompareAndSet_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetPlain_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetVolatile_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method CompareAndExchange_I_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x]
  (while true
    (let [c (.Load (.-v a))]
      (when (or (!= c e) (.CompareAndSwap (.-v a) c x))
        (return c)))))
(go/method GetAndIncrement__I ^int32 [^{:tag (* AtomicInteger)} a] (- (.Add (.-v a) 1) 1))
(go/method GetAndDecrement__I ^int32 [^{:tag (* AtomicInteger)} a] (+ (.Add (.-v a) -1) 1))
(go/method GetAndAdd_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 d] (- (.Add (.-v a) d) d))
(go/method IncrementAndGet__I ^int32 [^{:tag (* AtomicInteger)} a] (.Add (.-v a) 1))
(go/method DecrementAndGet__I ^int32 [^{:tag (* AtomicInteger)} a] (.Add (.-v a) -1))
(go/method AddAndGet_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 d] (.Add (.-v a) d))
(go/method IntValue__I ^int32 [^{:tag (* AtomicInteger)} a] (.Load (.-v a)))
(go/method LongValue__J ^int64 [^{:tag (* AtomicInteger)} a] (conv int64 (.Load (.-v a))))
(go/method FloatValue__F ^float32 [^{:tag (* AtomicInteger)} a] (conv float32 (.Load (.-v a))))
(go/method DoubleValue__D ^float64 [^{:tag (* AtomicInteger)} a] (conv float64 (.Load (.-v a))))
(go/method ByteValue__B ^int8 [^{:tag (* AtomicInteger)} a] (conv int8 (.Load (.-v a))))
(go/method ShortValue__S ^int16 [^{:tag (* AtomicInteger)} a] (conv int16 (.Load (.-v a))))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* AtomicInteger)} a] (StrOfInt (.Load (.-v a))))
(go/method Is_Serializable [^{:tag (* AtomicInteger)} a])
(go/method Ref ^any [^{:tag (* AtomicInteger)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* AtomicInteger)} t] AtomicInteger_class)
(go/method Clone__O ^any [^{:tag (* AtomicInteger)} t] (panic (CloneNotSupported t)))
;; step 5 phase 2B: the other memory orders (all sequentially consistent in Go)
(go/method GetOpaque__I ^int32 [^{:tag (* AtomicInteger)} a] (.Load (.-v a)))
(go/method SetOpaque_I__V [^{:tag (* AtomicInteger)} a ^int32 x] (.Store (.-v a) x))
(go/method WeakCompareAndSet_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetAcquire_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetRelease_I_I__Z ^bool [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndSwap (.-v a) e x))
(go/method CompareAndExchangeAcquire_I_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndExchange_I_I__I a e x))
(go/method CompareAndExchangeRelease_I_I__I ^int32 [^{:tag (* AtomicInteger)} a ^int32 e ^int32 x] (.CompareAndExchange_I_I__I a e x))
(go/func AtomicInteger_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* AtomicInteger) x)] ok))
(go/func AtomicInteger_Cast ^{:tag (* AtomicInteger)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* AtomicInteger) x)]
    (when (not ok) (panic (ClassCast x AtomicInteger_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; AtomicLong

(go/type AtomicLong (struct Number ^{:tag atomic/Int64} v))

(go/var AtomicLong_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.atomic.AtomicLong" :Kind KindClass
                     :Modifiers AccPublic :Super Number_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.AtomicLong"))))

(go/func AtomicLong_New ^{:tag (* AtomicLong)} [] (addr (lit AtomicLong)))
(go/func AtomicLong_New_J ^{:tag (* AtomicLong)} [^int64 v]
  (let [a (addr (lit AtomicLong))] (.Store (.-v a) v) a))

(go/method Get__J ^int64 [^{:tag (* AtomicLong)} a] (.Load (.-v a)))
(go/method Set_J__V [^{:tag (* AtomicLong)} a ^int64 x] (.Store (.-v a) x))
(go/method LazySet_J__V [^{:tag (* AtomicLong)} a ^int64 x] (.Store (.-v a) x))
(go/method GetPlain__J ^int64 [^{:tag (* AtomicLong)} a] (.Load (.-v a)))
(go/method SetPlain_J__V [^{:tag (* AtomicLong)} a ^int64 x] (.Store (.-v a) x))
(go/method GetAcquire__J ^int64 [^{:tag (* AtomicLong)} a] (.Load (.-v a)))
(go/method SetRelease_J__V [^{:tag (* AtomicLong)} a ^int64 x] (.Store (.-v a) x))
(go/method GetAndSet_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 x] (.Swap (.-v a) x))
(go/method CompareAndSet_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetPlain_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetVolatile_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method GetAndIncrement__J ^int64 [^{:tag (* AtomicLong)} a] (- (.Add (.-v a) 1) 1))
(go/method GetAndDecrement__J ^int64 [^{:tag (* AtomicLong)} a] (+ (.Add (.-v a) -1) 1))
(go/method GetAndAdd_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 d] (- (.Add (.-v a) d) d))
(go/method IncrementAndGet__J ^int64 [^{:tag (* AtomicLong)} a] (.Add (.-v a) 1))
(go/method DecrementAndGet__J ^int64 [^{:tag (* AtomicLong)} a] (.Add (.-v a) -1))
(go/method AddAndGet_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 d] (.Add (.-v a) d))
(go/method IntValue__I ^int32 [^{:tag (* AtomicLong)} a] (conv int32 (.Load (.-v a))))
(go/method LongValue__J ^int64 [^{:tag (* AtomicLong)} a] (.Load (.-v a)))
(go/method FloatValue__F ^float32 [^{:tag (* AtomicLong)} a] (conv float32 (.Load (.-v a))))
(go/method DoubleValue__D ^float64 [^{:tag (* AtomicLong)} a] (conv float64 (.Load (.-v a))))
(go/method ByteValue__B ^int8 [^{:tag (* AtomicLong)} a] (conv int8 (.Load (.-v a))))
(go/method ShortValue__S ^int16 [^{:tag (* AtomicLong)} a] (conv int16 (.Load (.-v a))))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* AtomicLong)} a] (StrOfLong (.Load (.-v a))))
(go/method Is_Serializable [^{:tag (* AtomicLong)} a])
(go/method Ref ^any [^{:tag (* AtomicLong)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* AtomicLong)} t] AtomicLong_class)
(go/method Clone__O ^any [^{:tag (* AtomicLong)} t] (panic (CloneNotSupported t)))
;; step 5 phase 2B: the other memory orders (all sequentially consistent in Go)
(go/method GetOpaque__J ^int64 [^{:tag (* AtomicLong)} a] (.Load (.-v a)))
(go/method SetOpaque_J__V [^{:tag (* AtomicLong)} a ^int64 x] (.Store (.-v a) x))
(go/method WeakCompareAndSet_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetAcquire_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetRelease_J_J__Z ^bool [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndSwap (.-v a) e x))
(go/method CompareAndExchangeAcquire_J_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndExchange_J_J__J a e x))
(go/method CompareAndExchangeRelease_J_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 e ^int64 x] (.CompareAndExchange_J_J__J a e x))
(go/method CompareAndExchange_J_J__J ^int64 [^{:tag (* AtomicLong)} a ^int64 e ^int64 x]
  (while true
    (let [c (.Load (.-v a))]
      (when (or (!= c e) (.CompareAndSwap (.-v a) c x))
        (return c)))))
(go/func AtomicLong_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* AtomicLong) x)] ok))
(go/func AtomicLong_Cast ^{:tag (* AtomicLong)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* AtomicLong) x)]
    (when (not ok) (panic (ClassCast x AtomicLong_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; AtomicBoolean

(go/type AtomicBoolean (struct Object ^{:tag atomic/Bool} v))

(go/var AtomicBoolean_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.atomic.AtomicBoolean" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.AtomicBoolean"))))

(go/func AtomicBoolean_New ^{:tag (* AtomicBoolean)} [] (addr (lit AtomicBoolean)))
(go/func AtomicBoolean_New_Z ^{:tag (* AtomicBoolean)} [^bool v]
  (let [a (addr (lit AtomicBoolean))] (.Store (.-v a) v) a))

(go/method Get__Z ^bool [^{:tag (* AtomicBoolean)} a] (.Load (.-v a)))
(go/method Set_Z__V [^{:tag (* AtomicBoolean)} a ^bool x] (.Store (.-v a) x))
(go/method LazySet_Z__V [^{:tag (* AtomicBoolean)} a ^bool x] (.Store (.-v a) x))
(go/method GetAndSet_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool x] (.Swap (.-v a) x))
(go/method CompareAndSet_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetVolatile_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* AtomicBoolean)} a] (StrOfBool (.Load (.-v a))))
(go/method Is_Serializable [^{:tag (* AtomicBoolean)} a])
(go/method Ref ^any [^{:tag (* AtomicBoolean)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* AtomicBoolean)} t] AtomicBoolean_class)
(go/method Clone__O ^any [^{:tag (* AtomicBoolean)} t] (panic (CloneNotSupported t)))
;; step 5 phase 2B: the other memory orders (all sequentially consistent in Go)
(go/method GetPlain__Z ^bool [^{:tag (* AtomicBoolean)} a] (.Load (.-v a)))
(go/method GetOpaque__Z ^bool [^{:tag (* AtomicBoolean)} a] (.Load (.-v a)))
(go/method GetAcquire__Z ^bool [^{:tag (* AtomicBoolean)} a] (.Load (.-v a)))
(go/method SetPlain_Z__V [^{:tag (* AtomicBoolean)} a ^bool x] (.Store (.-v a) x))
(go/method SetOpaque_Z__V [^{:tag (* AtomicBoolean)} a ^bool x] (.Store (.-v a) x))
(go/method SetRelease_Z__V [^{:tag (* AtomicBoolean)} a ^bool x] (.Store (.-v a) x))
(go/method WeakCompareAndSet_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetPlain_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetAcquire_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method WeakCompareAndSetRelease_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndSwap (.-v a) e x))
(go/method CompareAndExchange_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x]
  (while true
    (let [c (.Load (.-v a))]
      (when (or (!= c e) (.CompareAndSwap (.-v a) c x))
        (return c)))))
(go/method CompareAndExchangeAcquire_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndExchange_Z_Z__Z a e x))
(go/method CompareAndExchangeRelease_Z_Z__Z ^bool [^{:tag (* AtomicBoolean)} a ^bool e ^bool x] (.CompareAndExchange_Z_Z__Z a e x))
(go/func AtomicBoolean_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* AtomicBoolean) x)] ok))

;; ---------------------------------------------------------------------------------------
;; AtomicReference

(go/type AtomicReference (struct Object ^{:tag (Volatile any)} v))

(go/var AtomicReference_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.atomic.AtomicReference" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class)
                     :Go "arbace/jrt.AtomicReference"))))

(go/func AtomicReference_New ^{:tag (* AtomicReference)} [] (addr (lit AtomicReference)))
(go/func AtomicReference_New_O ^{:tag (* AtomicReference)} [^any v]
  (let [a (addr (lit AtomicReference))] (.Store (.-v a) v) a))

(go/method Get__O ^any [^{:tag (* AtomicReference)} a] (.Load (.-v a)))
(go/method Set_O__V [^{:tag (* AtomicReference)} a ^any x] (.Store (.-v a) x))
(go/method LazySet_O__V [^{:tag (* AtomicReference)} a ^any x] (.Store (.-v a) x))
(go/method GetPlain__O ^any [^{:tag (* AtomicReference)} a] (.Load (.-v a)))
(go/method SetPlain_O__V [^{:tag (* AtomicReference)} a ^any x] (.Store (.-v a) x))
(go/method GetAcquire__O ^any [^{:tag (* AtomicReference)} a] (.Load (.-v a)))
(go/method SetRelease_O__V [^{:tag (* AtomicReference)} a ^any x] (.Store (.-v a) x))
(go/method GetAndSet_O__O ^any [^{:tag (* AtomicReference)} a ^any x] (.Swap (.-v a) x))
(go/method CompareAndSet_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method WeakCompareAndSetPlain_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method WeakCompareAndSetVolatile_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* AtomicReference)} a] (StrOfObj (.Load (.-v a))))
;; step 5 phase 2B: the other memory orders (all sequentially consistent in Go)
(go/method GetOpaque__O ^any [^{:tag (* AtomicReference)} a] (.Load (.-v a)))
(go/method SetOpaque_O__V [^{:tag (* AtomicReference)} a ^any x] (.Store (.-v a) x))
(go/method WeakCompareAndSet_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method WeakCompareAndSetAcquire_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method WeakCompareAndSetRelease_O_O__Z ^bool [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndSet (.-v a) e x))
(go/method CompareAndExchange_O_O__O "CompareAndExchange_O_O__O is compareAndExchange: the witness value.\n"
  ^any [^{:tag (* AtomicReference)} a ^any e ^any x]
  (while true
    (let [c (.Load (.-v a))]
      (when (!= c e)
        (return c))
      (when (.CompareAndSet (.-v a) e x)
        (return e)))))
(go/method CompareAndExchangeAcquire_O_O__O ^any [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndExchange_O_O__O a e x))
(go/method CompareAndExchangeRelease_O_O__O ^any [^{:tag (* AtomicReference)} a ^any e ^any x] (.CompareAndExchange_O_O__O a e x))
(go/method Is_Serializable [^{:tag (* AtomicReference)} a])
(go/method Ref ^any [^{:tag (* AtomicReference)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* AtomicReference)} t] AtomicReference_class)
(go/method Clone__O ^any [^{:tag (* AtomicReference)} t] (panic (CloneNotSupported t)))
(go/func AtomicReference_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* AtomicReference) x)] ok))
(go/func AtomicReference_Cast ^{:tag (* AtomicReference)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* AtomicReference) x)]
    (when (not ok) (panic (ClassCast x AtomicReference_class)))
    v))

(go/func init []
  (set! (.-IsInstance (.Info AtomicInteger_class)) AtomicInteger_InstanceOf)
  (set! (.-IsInstance (.Info AtomicLong_class)) AtomicLong_InstanceOf)
  (set! (.-IsInstance (.Info AtomicBoolean_class)) AtomicBoolean_InstanceOf)
  (set! (.-IsInstance (.Info AtomicReference_class)) AtomicReference_InstanceOf))
