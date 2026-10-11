;; jrt: volatile fields of interface Go type (C2G-SPEC §8.2).
(in-ns 'go.arbace.jrt)

(go/file "volatile.go"
  :imports [[atomic "sync/atomic"]])

(go/type Volatile
  "Volatile is a volatile field whose Go type is an interface (any, C_I, J): an atomic pointer
to a boxed copy of the two-word value, so that loads and stores are atomic and sequentially
consistent as Java's volatile accesses; storing nil allocates nothing. The zero value holds
nil.\n"
  :type-params [T]
  (struct ^{:tag (atomic/Pointer T)} p))

(go/method Load "Load is a volatile read.\n" ^T [^{:tag (* (Volatile T))} v]
  (let [p (.Load (.-p v))]
    (when (== p nil)
      (let [^T z (zero T)]
        (return z)))
    @p))

(go/method Store "Store is a volatile write.\n" [^{:tag (* (Volatile T))} v ^T x]
  (when (== (conv any x) nil)
    (.Store (.-p v) nil)
    (return))
  (.Store (.-p v) (addr x)))
