;; SPEC §6: go/type, go/const, go/var, go/func, go/method; doc comments.
(ns go.decls
  (:require [arbace.go :as go]))

(go/package decls :path "decls" :files ["s06_decls.go"])

(go/file "s06_decls.go" :imports [[errors "errors"] [io "io"]])

(go/type Celsius "Celsius is a temperature.\n" float64)

(go/type ^:alias IntAlias int)

(go/type ^:alias Set :type-params [^comparable K] (map K bool))

(go/type
  [^{:doc "A is documented in the group.\n"} A int]
  [^:alias B string])

(go/type [One int])

(go/type Weekday "Weekday is a day.\n" int)

(go/const
  [^{:tag Weekday :val 0} Sunday iota]
  [^{:val 1} Monday]
  [^{:val 2} Tuesday])

(go/const ^{:val 4} UTFMax 4)

(go/const ^{:val 1267650600228229401496703205376N :doc "Big is big.\n"} Big (<< 1 100))

(go/const (values ^{:val 1} X ^{:val "y"} Y) (values 1 "y"))

(go/const
  [(values ^{:val 0} k1 ^{:val 0} k2) (values iota (* iota 10))]
  [(values ^{:val 1} k3 ^{:val 10} k4)])

(go/const ^{:tag int32 :val 5} typed 5)

(go/const [^{:val 1} c1 1])

(go/var ErrEmpty (errors/New "empty"))

(go/var ^{:tag (array 4 uint8)} first)

(go/var (values ^int a ^int b))

(go/var (values r w _) (pipe))

(go/var ^{:tag io/Reader} _ (conv (* T) nil))

(go/var
  [^{:doc "v1 is documented.\n"} v1 1]
  [^int v2 2]
  [^string v3])

(go/var [g1 1])

(go/func pipe [] :results [int int error] (return 0 1 nil))

(go/type T (struct))

(go/method Read [(* T) ^{:tag (slice byte)} p] :results [int error] (return 0 nil))

(go/func Divmod [^int a ^int b] :results [^int q ^int r]
  (set! q (/ a b))
  (set! r (% a b))
  (return))

(go/method Move "Move moves.\n" [^{:tag (* Point)} p ^int32 dx] (set! (.-X p) + dx))

(go/method Get [^Point p] :results [^int32 x ^int32 y] (return (.-X p) (.-Y p)))

(go/type Point (struct ^int32 X ^int32 Y))

(go/func init [])

(go/func init [] (set! a 1))

(go/func Len ^int [^{:tag (slice int)} x] (len x))

(go/func Map :type-params [T U] ^{:tag (slice U)} [^{:tag (slice T)} xs ^{:tag (func [T] [U])} f]
  (let [^{:tag (slice U)} out (zero (slice U))]
    (range [_ x xs]
      (set! out (append out (f x))))
    out))

(go/func Keys :type-params [^{:tag (tilde (map K V))} M ^comparable K ^any V] ^{:tag (slice K)} [^M m]
  (let [^{:tag (slice K)} ks (zero (slice K))]
    (range [k m]
      (set! ks (append ks k)))
    ks))

(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))

(go/method Push [^{:tag (* (Stack T))} s ^T x] (set! (.-items s) (append (.-items s) x)))

(go/method Len ^int [^{:tag (Stack T)} s] (len (.-items s)))

(go/func Nothing [])

(go/func Panics ^int [] (panic "no"))

(go/func Named [] :results [^error err] (return))

(go/func Variadic ^int [& ^{:tag (slice int)} xs] (len xs))

(go/func Variadic2 [^string f & ^{:tag (slice any)} xs])
