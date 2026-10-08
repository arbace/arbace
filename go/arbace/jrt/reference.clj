;; jrt: java.lang.ref (C2G-SPEC §8; JAVA-SURFACE.md; doc/go/JRT-NOTES.md, phase 2a):
;; Reference, WeakReference, SoftReference, ReferenceQueue over Go's weak pointers and
;; runtime.AddCleanup. Keyword's and Symbol's interning tables hold WeakReferences with a
;; queue that Util.clearCache polls.
(in-ns 'go.arbace.jrt)

(go/file "reference.go"
  :imports [[runtime "runtime"] [sync "sync"] [time "time"] [unsafe "unsafe"] [weak "weak"]])

;; ---------------------------------------------------------------------------------------
;; Reference (abstract, non-leaf)

(go/type Reference_I
  "Reference_I is java.lang.ref.Reference's class interface.\n"
  (interface Object_I
    (Self_Reference ^{:tag (* Reference)} [])
    (Get__O ^any [])
    (Clear__V [])
    (Enqueue__Z ^bool [])
    (IsEnqueued__Z ^bool [])
    (RefersTo_O__Z ^bool [^any o])))

(go/type Reference
  "Reference is java.lang.ref.Reference's struct. A weak reference holds a weak pointer to
the referent's header (every Java object's first field, so its address is the object's) and
the referent's type word, from which get rebuilds the reference; a soft one holds the
referent strongly (Go has no memory-pressure-sensitive pointers: a SoftReference is cleared
only by clear). When the weak referent is collected, a cleanup puts the Reference on its
queue.\n"
  (struct Object
          ^Reference_I self
          ^{:tag sync/Mutex} mu
          ^{:tag (weak/Pointer Object)} wp
          ^{:tag unsafe/Pointer} typ
          ^any strong
          ^bool cleared
          ^{:tag (* ReferenceQueue)} queue
          ^bool enqueued
          ^{:tag runtime/Cleanup} cleanup
          ^bool hasCleanup))

(go/var Reference_class
  (Define (addr (lit ClassInfo :Name "java.lang.ref.Reference" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Go "arbace/jrt.Reference"))))

(go/method initWeak
  "initWeak makes r refer weakly to o (nil: cleared) and, with a queue, registers the cleanup
that enqueues it.\n"
  [^{:tag (* Reference)} r ^Reference_I this ^any o ^{:tag (* ReferenceQueue)} q]
  (set! (.-self r) this)
  (set! (.-queue r) q)
  (when (== o nil)
    (set! (.-cleared r) true)
    (return))
  (let [h (.Self_Object (asObject o))]
    (set! (.-typ r) (typeWordOf o))
    (set! (.-wp r) (weak/Make h))
    (when (!= q nil)
      (set! (.-cleanup r) (runtime/AddCleanup h referentCollected r))
      (set! (.-hasCleanup r) true))))

(go/func referentCollected "referentCollected is the cleanup of a weak referent: enqueue its Reference.\n"
  [^{:tag (* Reference)} r]
  (.enqueue r))

(go/method Impl_Get__O "Impl_Get__O is get(): the referent, or null once it was collected or cleared.\n"
  ^any [^{:tag (* Reference)} r ^Reference_I this]
  (.Lock (.-mu r))
  (defer (.Unlock (.-mu r)))
  (when (.-cleared r)
    (return nil))
  (when (!= (.-strong r) nil)
    (return (.-strong r)))
  (let [p (.Value (.-wp r))]
    (when (== p nil)
      (return nil))
    (makeAny (.-typ r) (conv unsafe/Pointer p))))

(go/method Impl_RefersTo_O__Z ^bool [^{:tag (* Reference)} r ^Reference_I this ^any o]
  (== (.Get__O this) o))

(go/method Impl_Clear__V "Impl_Clear__V is clear(): the referent dropped, without enqueueing.\n"
  [^{:tag (* Reference)} r ^Reference_I this]
  (.Lock (.-mu r))
  (set! (.-cleared r) true)
  (set! (.-strong r) nil)
  (set! (.-wp r) (lit (weak/Pointer Object)))
  (.Unlock (.-mu r)))

(go/method enqueue "enqueue puts r on its queue once; whether it did.\n" ^bool [^{:tag (* Reference)} r]
  (.Lock (.-mu r))
  (when (or (== (.-queue r) nil) (.-enqueued r))
    (.Unlock (.-mu r))
    (return false))
  (set! (.-enqueued r) true)
  (set! (.-cleared r) true)
  (set! (.-strong r) nil)
  (let [q (.-queue r)]
    (.Unlock (.-mu r))
    (.add q (.-self r))
    true))

(go/method Impl_Enqueue__Z "Impl_Enqueue__Z is enqueue(): clears and enqueues.\n"
  ^bool [^{:tag (* Reference)} r ^Reference_I this]
  (.Lock (.-mu r))
  (let [c (.-cleanup r)
        has (.-hasCleanup r)]
    (set! (.-hasCleanup r) false)
    (.Unlock (.-mu r))
    (when has
      (.Stop c)))
  (.enqueue r))

(go/method Impl_IsEnqueued__Z ^bool [^{:tag (* Reference)} r ^Reference_I this]
  (.Lock (.-mu r))
  (defer (.Unlock (.-mu r)))
  (.-enqueued r))

(go/method Self_Reference ^{:tag (* Reference)} [^{:tag (* Reference)} r] r)

(go/func Reference_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Reference_I x)] ok))
(go/func Reference_Cast ^Reference_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Reference_I x)]
    (when (not ok) (panic (ClassCast x Reference_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; WeakReference and SoftReference (leaves here)

(go/type WeakReference (struct Reference))

(go/var WeakReference_class
  (Define (addr (lit ClassInfo :Name "java.lang.ref.WeakReference" :Kind KindClass :Modifiers AccPublic
                     :Super Reference_class :Go "arbace/jrt.WeakReference"))))

(go/func WeakReference_New_O ^{:tag (* WeakReference)} [^any o]
  (let [r (addr (lit WeakReference))] (.initWeak (.-Reference r) r o nil) r))
(go/func WeakReference_New_O_ReferenceQueue ^{:tag (* WeakReference)} [^any o ^{:tag (* ReferenceQueue)} q]
  (let [r (addr (lit WeakReference))] (.initWeak (.-Reference r) r o q) r))

(go/method Get__O ^any [^{:tag (* WeakReference)} r] (.Impl_Get__O r r))
(go/method Clear__V [^{:tag (* WeakReference)} r] (.Impl_Clear__V r r))
(go/method Enqueue__Z ^bool [^{:tag (* WeakReference)} r] (.Impl_Enqueue__Z r r))
(go/method IsEnqueued__Z ^bool [^{:tag (* WeakReference)} r] (.Impl_IsEnqueued__Z r r))
(go/method RefersTo_O__Z ^bool [^{:tag (* WeakReference)} r ^any o] (.Impl_RefersTo_O__Z r r o))
(go/method Ref ^any [^{:tag (* WeakReference)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* WeakReference)} t] WeakReference_class)
(go/method Clone__O ^any [^{:tag (* WeakReference)} t] (panic (CloneNotSupportedException_New)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* WeakReference)} t] (Object_toString t))
(go/func WeakReference_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* WeakReference) x)] ok))

(go/type SoftReference (struct Reference))

(go/var SoftReference_class
  (Define (addr (lit ClassInfo :Name "java.lang.ref.SoftReference" :Kind KindClass :Modifiers AccPublic
                     :Super Reference_class :Go "arbace/jrt.SoftReference"))))

(go/func SoftReference_New_O ^{:tag (* SoftReference)} [^any o]
  (let [r (addr (lit SoftReference))]
    (set! (.-strong (.-Reference r)) o)
    (set! (.-self (.-Reference r)) r)
    (set! (.-cleared (.-Reference r)) (== o nil))
    r))
(go/func SoftReference_New_O_ReferenceQueue ^{:tag (* SoftReference)} [^any o ^{:tag (* ReferenceQueue)} q]
  (let [r (SoftReference_New_O o)]
    (set! (.-queue (.-Reference r)) q)
    r))

(go/method Get__O ^any [^{:tag (* SoftReference)} r] (.Impl_Get__O r r))
(go/method Clear__V [^{:tag (* SoftReference)} r] (.Impl_Clear__V r r))
(go/method Enqueue__Z ^bool [^{:tag (* SoftReference)} r] (.Impl_Enqueue__Z r r))
(go/method IsEnqueued__Z ^bool [^{:tag (* SoftReference)} r] (.Impl_IsEnqueued__Z r r))
(go/method RefersTo_O__Z ^bool [^{:tag (* SoftReference)} r ^any o] (.Impl_RefersTo_O__Z r r o))
(go/method Ref ^any [^{:tag (* SoftReference)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* SoftReference)} t] SoftReference_class)
(go/method Clone__O ^any [^{:tag (* SoftReference)} t] (panic (CloneNotSupportedException_New)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* SoftReference)} t] (Object_toString t))
(go/func SoftReference_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* SoftReference) x)] ok))

;; ---------------------------------------------------------------------------------------
;; ReferenceQueue

(go/type ReferenceQueue
  "ReferenceQueue is java.lang.ref.ReferenceQueue: enqueued references, first in, first out;
gate wakes remove.\n"
  (struct Object ^{:tag sync/Mutex} mu ^{:tag (slice Reference_I)} refs ^gate g))

(go/var ReferenceQueue_class
  (Define (addr (lit ClassInfo :Name "java.lang.ref.ReferenceQueue" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class :Go "arbace/jrt.ReferenceQueue"))))

(go/func ReferenceQueue_New ^{:tag (* ReferenceQueue)} [] (addr (lit ReferenceQueue)))

(go/method add [^{:tag (* ReferenceQueue)} q ^Reference_I r]
  (.Lock (.-mu q))
  (set! (.-refs q) (append (.-refs q) r))
  (.open (addr (.-g q)))
  (.Unlock (.-mu q)))

(go/method Poll__Reference "Poll__Reference is poll(): the next enqueued reference, or null.\n"
  ^Reference_I [^{:tag (* ReferenceQueue)} q]
  (.Lock (.-mu q))
  (defer (.Unlock (.-mu q)))
  (when (== (len (.-refs q)) 0)
    (return nil))
  (let [r (aget (.-refs q) 0)]
    (aset (.-refs q) 0 nil)
    (set! (.-refs q) (subslice (.-refs q) 1))
    r))

(go/method Remove_J__Reference "Remove_J__Reference is remove(timeout): waits at most timeout ms (0: forever).\n"
  ^Reference_I [^{:tag (* ReferenceQueue)} q ^int64 timeout]
  (when (< timeout 0)
    (panic (IllegalArgumentException_New_String (Str "Negative timeout value"))))
  (let [me (CurrentThread)
        ^{:tag time/Time} dl (zero time/Time)]
    (when (> timeout 0)
      (set! dl (deadlineOf (* timeout 1000000))))
    (while true
      (.Lock (.-mu q))
      (when (> (len (.-refs q)) 0)
        (.Unlock (.-mu q))
        (return (.Poll__Reference q)))
      (let [ch (.wait (addr (.-g q)))]
        (.Unlock (.-mu q))
        (switch (block ch me true dl)
          (case [wokenInterrupted] (panic (InterruptedException_New)))
          (case [wokenTimeout] (return (.Poll__Reference q))))))))

(go/method Remove__Reference ^Reference_I [^{:tag (* ReferenceQueue)} q]
  (.Remove_J__Reference q 0))

(go/method Ref ^any [^{:tag (* ReferenceQueue)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ReferenceQueue)} t] ReferenceQueue_class)
(go/method Clone__O ^any [^{:tag (* ReferenceQueue)} t] (panic (CloneNotSupported t)))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* ReferenceQueue)} t] (Object_toString t))
(go/func ReferenceQueue_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* ReferenceQueue) x)] ok))

(go/func init []
  (set! (.-IsInstance (.Info Reference_class)) Reference_InstanceOf)
  (set! (.-IsInstance (.Info WeakReference_class)) WeakReference_InstanceOf)
  (set! (.-IsInstance (.Info SoftReference_class)) SoftReference_InstanceOf)
  (set! (.-IsInstance (.Info ReferenceQueue_class)) ReferenceQueue_InstanceOf))
