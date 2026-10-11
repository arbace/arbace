;; SPEC §7.5-7.7, §12.2: operators and precedence, selectors and calls, method values and
;; expressions, composite literals, indexing and slicing, conversions and assertions,
;; builtins, generics, composite literals in statement headers.
(ns go.exprs
  (:require [arbace.go :as go]))

(go/package exprs :path "exprs" :files ["s07_exprs.go"])

(go/file "s07_exprs.go" :imports [[io "io"] [slices "slices"] [unsafe "unsafe"]])

(go/type Point (struct ^int X ^int Y))

(go/type Named (struct Point ^string Name))

(go/method Len ^int [^Point p] (+ (.-X p) (.-Y p)))
(go/method Scale [^{:tag (* Point)} p ^int k] (set! (.-X p) * k))

(go/type Fns (struct ^{:tag (func [int] [int])} f))

(go/type Pair :type-params [^comparable K ^any V] (struct ^K Key ^V Val))

(go/func Ident :type-params [T] ^T [^T x] x)

(go/func Two :type-params [A B] [^A a ^B b] :results [A B] (return a b))

(go/type List :type-params [T] (struct ^{:tag (slice T)} items))

(go/method Add [^{:tag (* (List T))} l ^T x] (set! (.-items l) (append (.-items l) x)))

(go/func Ops ^int [^int a ^int b ^int c ^uint u ^{:tag (* int)} p ^bool f ^bool g]
  (let [x (+ a (* b c))]
    (set! x (* (+ a b) c))
    (set! x (- a (- b c)))
    (set! x (- a b c))
    (set! x (+ a b c))
    (set! x (/ a @p))
    (set! x (bit-and a (bit-not b)))
    (set! x (bit-and-not a b))
    (set! x (- a -1))
    (set! x (- (- a)))
    (set! x (- -1))
    (set! x (+ a))
    (set! x (bit-not a))
    (set! x (bit-or (<< a 2) (>> b 1)))
    (set! x (<< (% a b) 1))
    (set! x (conv int (>> u 3)))
    (set! x (bit-xor (bit-or a (bit-and b c)) c))
    (set! x (bit-and (bit-or a b) c))
    (set! x (* @p @p))
    (set! _ (or (and (not f) g) (and f (not g))))
    (set! _ (and (or f g) f))
    (set! _ (or f (and g f)))
    (set! _ (== (< a b) (< c a)))
    (set! _ (and (== a b) (!= b c) (<= a c) (>= a b) (> a 0)))
    (set! _ (bit-and x 1))
    x))

(go/func Selectors ^int [^Named n ^{:tag (* Named)} pn ^Fns fs ^{:tag io/Reader} r ^{:tag (* (List int))} l]
  (let [x ^{:go/via [Point]} (.-X n)]
    (set! x + (.-Y (.-Point n)))
    (set! x + ^{:go/via [Point]} (.Len pn))
    ^{:go/via [Point]} (.Scale pn 2)
    ^{:go/via [Point]} (.Scale n 3)
    (set! x + ((.-f fs) 1))
    (let [m ^{:go/via [Point]} (.-Len n)]
      (set! x + (m))
      (let [me (method-expr Point Len)]
        (set! x + (me (.-Point n)))
        (let [mp (method-expr (* Point) Scale)]
          (mp (addr (.-Point n)) 2)
          (let [ri (method-expr io/Reader Read)]
            (set! _ ri)
            (set! _ (.-Read r))
            (.Add l 1)
            (let [y (addr (.-Y (.-Point n)))]
              (inc! @y)
              (let [pp (addr y)]
                (set! @@pp 2)
                (+ x @y)))))))))

(go/func Literals ^any []
  (let [p (lit Point 1 2)
        q (lit Point :X 1 :Y 2)
        e (lit Point)
        pe (addr (lit Point))
        s (lit (slice int) 1 2 3)
        a (lit (array ... string) "a" "b")
        a2 (lit (array 4 int) [1 10] [3 30])
        m (lit (map string int) ["a" 1] ["b" 2])
        ps (lit (slice Point) (lit _ 1 2) (lit _ :X 3))
        pps (lit (slice (* Point)) (lit _ 1 2) (lit _))
        mm (lit (map Point string) [(lit _ 1 2) "a"])
        nn (lit Named (lit Point 1 2) "n")
        pr (lit (Pair string int) "a" 1)
        nested (lit (slice (slice int)) (lit _ 1) (lit _ 2 3))
        fn (fn ^int [^int x] (* x 2))
        st (lit (struct ^int A ^int B) 1 2)]
    (lit (slice any) p q e pe s a a2 m ps pps mm nn pr nested fn st)))

(go/func Index ^int [^{:tag (slice int)} s ^{:tag (map string int)} m ^string str ^{:tag (* (array 4 int))} arr]
  (let [x (+ (aget s 0) (aget s (- (len s) 1)))]
    (set! x + (aget m "k"))
    (let [(values v ok) (aget m "k")]
      (set! _ ok)
      (set! x + v)
      (set! x + (conv int (aget str 1)))
      (set! x + (aget arr 2))
      (set! _ (subslice s 1))
      (set! _ (subslice s _ 2))
      (set! _ (subslice s 1 2))
      (set! _ (subslice s))
      (set! _ (subslice s 1 2 3))
      (set! _ (subslice s _ 2 3))
      (set! _ (subslice s (+ x 1) (+ x 2)))
      (set! _ (subslice s _ (+ x 1)))
      (set! _ (subslice str 1))
      (set! _ (subslice arr))
      x)))

(go/func Conv [^{:tag (* int)} p ^{:tag (chan int)} c ^{:tag (func [])} f ^any a ^uintptr u]
  (set! _ (conv (* int) p))
  (set! _ (conv (chan :recv int) c))
  (set! _ (conv (func []) f))
  (set! _ (conv (chan int) c))
  (set! _ (conv unsafe/Pointer p))
  (set! _ (conv (slice byte) "abc"))
  (set! _ (conv string (conv rune 65)))
  (set! _ (conv float64 3))
  (set! _ (assert int a))
  (set! _ (assert (* Point) a))
  (set! _ (assert (interface (Len ^int [])) a))
  (let [(values n ok) (assert int a)]
    (set! (values _ _) (values n ok))
    (set! _ (conv unsafe/Pointer u))
    (set! _ (conv (* Point) (conv unsafe/Pointer p)))
    (set! _ (conv (array 2 int) (lit (slice int) 1 2)))
    (set! _ (conv (Pair string int) (lit (Pair string int))))))

(go/func Calls ^int [^{:tag (slice int)} xs & ^{:tag (slice any)} args]
  (let [n (+ (len xs) (cap xs))]
    (set! xs (append xs 1 2))
    (set! xs (append xs (spread xs)))
    (let [bs (append (conv (slice byte) "x") (spread "abc"))]
      (copy xs (subslice xs 1))
      (let [m (make (map string int))
            m2 (make (map string int) 10)]
        (delete m "a")
        (clear m2)
        (let [s (make (slice int) 2 10)
              c (make (chan int) 1)]
          (close c)
          (let [p (new int)
                q (new 42)
                r (new (inst Pair string int))
                z (complex 1 2)]
            (set! _ (+ (real z) (imag z)))
            (set! _ (min 1 2 n))
            (set! _ (max n 3))
            (print n)
            (println bs s p q r)
            (sink (spread args))
            (set! _ (^{:inst [int]} Ident 1))
            (set! _ ((inst Ident string) "a"))
            (let [f (inst Ident int)]
              (set! _ f)
              (set! (values _ _) (^{:inst [int string]} (inst Two int) 1 "b"))
              (^{:inst [(slice int) int]} slices/Sort xs)
              (set! _ (unsafe/Sizeof n))
              (set! _ (unsafe/Slice (addr (aget xs 0)) 1))
              (set! _ (unsafe/String (addr (aget bs 0)) 1))
              (set! _ (unsafe/Add (conv unsafe/Pointer p) 8))
              (set! _ (unsafe/Alignof n))
              (set! _ (unsafe/Offsetof (.-Y (lit Point))))
              ((fn []))
              (defer ((fn [^int x]) 1))
              (recover)
              (+ n (<! c)))))))))

(go/func Header ^int [^Point p ^{:tag (slice Point)} ps]
  (when (== p (lit Point))
    (return 1))
  (when (== p (lit Point 1 2))
    (return 2))
  (range [_ q (lit (slice Point) (lit _ 1 2))]
    (when (== q p)
      (return 3)))
  (for [i (lit Point)] (< (.-X i) 3) (inc! (.-X i)))
  (when (== p ((fn ^Point [] (lit Point))))
    (return 4))
  0)

(go/func sink [& ^{:tag (slice any)} xs])
