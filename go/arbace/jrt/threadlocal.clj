;; jrt: ThreadLocal, InheritableThreadLocal and ThreadLocalRandom's probes over the current
;; Thread (C2G-SPEC §8.4; doc/go/JRT-NOTES.md, phase 2a).
(in-ns 'go.arbace.jrt)

(go/file "threadlocal.go"
  :imports [[atomic "sync/atomic"]])

;; ---------------------------------------------------------------------------------------
;; ThreadLocal (non-leaf: Var's dynamic bindings subclass it for initialValue, withInitial's
;; SuppliedThreadLocal, InheritableThreadLocal)

(go/type ThreadLocal_I
  "ThreadLocal_I is java.lang.ThreadLocal's class interface.\n"
  (interface Object_I
    (Self_ThreadLocal ^{:tag (* ThreadLocal)} [])
    (Get__O ^any [])
    (Set_O__V [^any v])
    (Remove__V [])
    (InitialValue__O ^any [])
    (ChildValue_O__O ^any [^any parent])))

(go/type ThreadLocal
  "ThreadLocal is java.lang.ThreadLocal's struct. The values live in the current Thread's map
(Thread.locals, or Thread.inheritable for an InheritableThreadLocal), keyed by the struct; the
map is the thread's own, so no lock is needed. A thread's map holds its keys strongly (Java's
holds them weakly): a ThreadLocal dropped by the program stays in the maps of the threads that
set it until they end.\n"
  (struct Object ^ThreadLocal_I self ^bool inheritable))

(go/var ThreadLocal_class
  (Define (addr (lit ClassInfo :Name "java.lang.ThreadLocal" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class :Go "arbace/jrt.ThreadLocal"))))

(go/func ThreadLocal_New ^{:tag (* ThreadLocal)} []
  (let [t (addr (lit ThreadLocal))] (.Ctor t t) t))

(go/method Ctor "Ctor is ThreadLocal().\n" [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this]
  (set! (.-self t) this))

(go/method Self_ThreadLocal ^{:tag (* ThreadLocal)} [^{:tag (* ThreadLocal)} t] t)
(go/method Ref ^any [^{:tag (* ThreadLocal)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ThreadLocal)} t] ThreadLocal_class)
(go/method Clone__O ^any [^{:tag (* ThreadLocal)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ThreadLocal)} t] (Object_toString t))

(go/method table "table is the current thread's map for t, created when create.\n"
  ^{:tag (map (* ThreadLocal) any)} [^{:tag (* ThreadLocal)} t ^{:tag (* Thread)} th ^bool create]
  (when (.-inheritable t)
    (when (and create (== (.-inheritable th) nil))
      (set! (.-inheritable th) (make (map (* ThreadLocal) any))))
    (return (.-inheritable th)))
  (when (and create (== (.-locals th) nil))
    (set! (.-locals th) (make (map (* ThreadLocal) any))))
  (.-locals th))

(go/method Impl_Get__O
  "Impl_Get__O is ThreadLocal.get: the current thread's value, initialValue() stored first
when there is none.\n"
  ^any [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this]
  (let [th (CurrentThread)
        m (.table t th false)]
    (let [(values v ok) (aget m t)]
      (when ok
        (return v)))
    (let [v (.InitialValue__O this)]
      (aset (.table t th true) t v)
      v)))

(go/method Impl_Set_O__V [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this ^any v]
  (aset (.table t (CurrentThread) true) t v))

(go/method Impl_Remove__V [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this]
  (let [m (.table t (CurrentThread) false)]
    (when (!= m nil)
      (delete m t))))

(go/method Impl_InitialValue__O ^any [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this] nil)

(go/method Impl_ChildValue_O__O
  "Impl_ChildValue_O__O is ThreadLocal.childValue: UnsupportedOperationException (only an
InheritableThreadLocal has child values).\n"
  ^any [^{:tag (* ThreadLocal)} t ^ThreadLocal_I this ^any parent]
  (panic (UnsupportedOperationException_New)))

(go/method Get__O ^any [^{:tag (* ThreadLocal)} t] (.Impl_Get__O t t))
(go/method Set_O__V [^{:tag (* ThreadLocal)} t ^any v] (.Impl_Set_O__V t t v))
(go/method Remove__V [^{:tag (* ThreadLocal)} t] (.Impl_Remove__V t t))
(go/method InitialValue__O ^any [^{:tag (* ThreadLocal)} t] (.Impl_InitialValue__O t t))
(go/method ChildValue_O__O ^any [^{:tag (* ThreadLocal)} t ^any p] (.Impl_ChildValue_O__O t t p))

(go/func ThreadLocal_InstanceOf ^bool [^any x]
  (let [(values _ ok) (assert ThreadLocal_I x)] ok))

(go/func ThreadLocal_Cast ^ThreadLocal_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ThreadLocal_I x)]
    (when (not ok) (panic (ClassCast x ThreadLocal_class)))
    v))

;; ThreadLocal.withInitial: java.lang.ThreadLocal$SuppliedThreadLocal

(go/type ThreadLocal_SuppliedThreadLocal (struct ThreadLocal ^Supplier supplier))

(go/var ThreadLocal_SuppliedThreadLocal_class
  (Define (addr (lit ClassInfo :Name "java.lang.ThreadLocal$SuppliedThreadLocal" :Kind KindClass
                     :Modifiers (bit-or AccStatic AccFinal) :Super ThreadLocal_class
                     :Declaring ThreadLocal_class :Simple "SuppliedThreadLocal"
                     :Go "arbace/jrt.ThreadLocal_SuppliedThreadLocal"))))

(go/func ThreadLocal_WithInitial_Supplier__ThreadLocal
  "ThreadLocal_WithInitial_Supplier__ThreadLocal is ThreadLocal.withInitial.\n"
  ^ThreadLocal_I [^Supplier s]
  (let [t (addr (lit ThreadLocal_SuppliedThreadLocal))]
    (.Ctor (.-ThreadLocal t) t)
    (set! (.-supplier t) (nnIface s))
    t))

(go/method InitialValue__O ^any [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t]
  (.Get__O (.-supplier t)))
(go/method Get__O ^any [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] (.Impl_Get__O t t))
(go/method Set_O__V [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t ^any v] (.Impl_Set_O__V t t v))
(go/method Remove__V [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] (.Impl_Remove__V t t))
(go/method ChildValue_O__O ^any [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t ^any p] (.Impl_ChildValue_O__O t t p))
(go/method Ref ^any [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] ThreadLocal_SuppliedThreadLocal_class)
(go/method Clone__O ^any [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ThreadLocal_SuppliedThreadLocal)} t] (Object_toString t))

;; ---------------------------------------------------------------------------------------
;; InheritableThreadLocal: values copied into a thread at its start (Thread.start), through
;; childValue

(go/type InheritableThreadLocal (struct ThreadLocal))

(go/var InheritableThreadLocal_class
  (Define (addr (lit ClassInfo :Name "java.lang.InheritableThreadLocal" :Kind KindClass :Modifiers AccPublic
                     :Super ThreadLocal_class :Go "arbace/jrt.InheritableThreadLocal"))))

(go/func InheritableThreadLocal_New ^{:tag (* InheritableThreadLocal)} []
  (let [t (addr (lit InheritableThreadLocal))] (.Ctor t t) t))

(go/method Ctor [^{:tag (* InheritableThreadLocal)} t ^ThreadLocal_I this]
  (.Ctor (.-ThreadLocal t) this)
  (set! (.-inheritable (.-ThreadLocal t)) true))

(go/method ChildValue_O__O "ChildValue_O__O is InheritableThreadLocal.childValue: the parent's value.\n"
  ^any [^{:tag (* InheritableThreadLocal)} t ^any parent]
  parent)
(go/method Get__O ^any [^{:tag (* InheritableThreadLocal)} t] (.Impl_Get__O t t))
(go/method Set_O__V [^{:tag (* InheritableThreadLocal)} t ^any v] (.Impl_Set_O__V t t v))
(go/method Remove__V [^{:tag (* InheritableThreadLocal)} t] (.Impl_Remove__V t t))
(go/method InitialValue__O ^any [^{:tag (* InheritableThreadLocal)} t] (.Impl_InitialValue__O t t))
(go/method Ref ^any [^{:tag (* InheritableThreadLocal)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* InheritableThreadLocal)} t] InheritableThreadLocal_class)
(go/method Clone__O ^any [^{:tag (* InheritableThreadLocal)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* InheritableThreadLocal)} t] (Object_toString t))

(go/func InheritableThreadLocal_InstanceOf ^bool [^any x]
  (let [(values v ok) (assert ThreadLocal_I x)]
    (and ok (.-inheritable (.Self_ThreadLocal v)))))

;; ---------------------------------------------------------------------------------------
;; ThreadLocalRandom's probes (ConcurrentHashMap's counter cells): per thread, in the Thread
;; (ThreadLocalRandom.current() waits for the translated Random, its superclass)

(go/var ^{:tag atomic/Int32} probeGenerator)
(go/var ^{:tag atomic/Int64} seeder)

(go/func ThreadLocalRandom_LocalInit__V
  "ThreadLocalRandom_LocalInit__V is ThreadLocalRandom.localInit: the current thread's probe
(never 0) and seed, as Java's.\n"
  []
  (let [p (.Add probeGenerator -1640531527)]
    (when (== p 0)
      (set! p 1))
    (let [s (mix64 (.Add seeder -4942790177534073029))
          t (CurrentThread)]
      (set! (.-seed t) s)
      (set! (.-probe t) p))))

(go/func ThreadLocalRandom_GetProbe__I
  "ThreadLocalRandom_GetProbe__I is ThreadLocalRandom.getProbe: 0 until localInit.\n"
  ^int32 []
  (.-probe (CurrentThread)))

(go/func ThreadLocalRandom_AdvanceProbe_I__I
  "ThreadLocalRandom_AdvanceProbe_I__I is ThreadLocalRandom.advanceProbe: a xorshift step.\n"
  ^int32 [^int32 probe]
  (set! probe bit-xor (<< probe 13))
  (set! probe bit-xor (conv int32 (>> (conv uint32 probe) 17)))
  (set! probe bit-xor (<< probe 5))
  (set! (.-probe (CurrentThread)) probe)
  probe)

(go/func mix64 ^int64 [^int64 z]
  (let [u (conv uint64 z)]
    (set! u (* (bit-xor u (>> u 33)) 0xff51afd7ed558ccd))
    (set! u (* (bit-xor u (>> u 33)) 0xc4ceb9fe1a85ec53))
    (conv int64 (bit-xor u (>> u 33)))))

(go/func init []
  (.Store seeder 0x5DEECE66D)
  (set! (.-IsInstance (.Info ThreadLocal_class)) ThreadLocal_InstanceOf)
  (set! (.-IsInstance (.Info ThreadLocal_SuppliedThreadLocal_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* ThreadLocal_SuppliedThreadLocal) x)] ok)))
  (set! (.-IsInstance (.Info InheritableThreadLocal_class)) InheritableThreadLocal_InstanceOf))
