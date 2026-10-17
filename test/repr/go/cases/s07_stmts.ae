;; SPEC §7: statements (let, set!, if/when/cond, switch, type-switch, select, loops, labels,
;; jumps, go, defer, return).
(ns go.stmts
  (:require [arbace.go :as go]))

(go/package stmts :path "stmts" :files ["s07_stmts.go"])

(go/file "s07_stmts.go" :imports [[fmt "fmt"]])

(go/type T
  (struct ^int f ^{:tag (func [int] [int])} fn ^{:tag (map string int)} m ^{:tag (array 3 int)} a))

(go/func g [] :results [int error] (return 1 nil))

(go/func Decls ^int []
  (let [x 1
        (values a b) (values 2 3)
        (values c err) (g)
        (values d ^:assign err) (g)
        ^int e (zero int)
        ^int f 4
        ^:var h 5
        (values ^int i ^int j) (zero int)
        (values ^int k ^int l) (values 6 7)
        ^:const ^{:val 10} N 10
        ^:const ^{:tag int :val 11} M 11]
    (let-type [L (struct ^int v)
               ^:alias LA int]
      (set! (values _ _) (values err (lit L)))
      (let [^LA la (zero LA)]
        (+ x a b c d e f h i j k l N M la)))))

(go/func Assign [^{:tag (* T)} t ^{:tag (* int)} p ^{:tag (slice int)} s]
  (let [x 0]
    (set! x 1)
    (set! (values x @p) (values @p x))
    (set! _ x)
    (set! x + 1)
    (set! x - 1)
    (set! x * 2)
    (set! x / 2)
    (set! x % 3)
    (set! x << 1)
    (set! x >> 1)
    (set! x bit-and 7)
    (set! x bit-or 8)
    (set! x bit-xor 1)
    (set! x bit-and-not 2)
    (inc! x)
    (dec! x)
    (aset s 0 x)
    (set! (values (aget s 1) (aget s 2)) (values (aget s 2) (aget s 1)))
    (set! (.-f t) x)
    (aset (.-a t) 1 2)
    (aset (.-m t) "k" 3)
    (set! @p 4)
    (do
      (let [y x]
        (set! _ y)))
    (do)))

(go/func If ^string [^int x]
  (when (> x 0)
    (return "pos"))
  (if (< x -10)
    (inc! x)
    (dec! x))
  (when [y (* x 2)] (> y 4)
    (return "big"))
  (when [(inc! x)] (> x 3)
    (return "x"))
  (cond
    (== x 1) (return "one")
    (== x 2) (return "two")
    :else (return "many")))

(go/func Else ^int [^int x]
  (if (== x 0)
    (return 1)
    (do (when (== x 1)
          (return 2))))
  (cond
    (== x 3) (inc! x)
    (== x 4) (dec! x))
  (if (== x 5)
    (inc! x)
    (if [y x] (== y 6)
      (dec! x)
      (set! x 0)))
  (when (== x 7))
  x)

(go/func Switch ^int [^int x]
  (switch x
    (case [1 2] (inc! x))
    (default (dec! x))
    (case [3]
      (set! x * 2)
      (fallthrough))
    (case [4] (set! x / 2)))
  (switch
    (case [(> x 10)] (return 1)))
  (switch [y (* x 2)]
    (case [(> y 4)] (return 2)))
  (switch [y x] y
    (case [0]))
  (switch [x (+ x 1)] x)
  (switch)
  x)

(go/func TypeSwitch ^int [^any v]
  (type-switch [x v]
    (case [nil] (return 0))
    (case [int int64]
      (set! _ x)
      (return 1))
    (case [fmt/Stringer] (return (len (.String x))))
    (default (return 3))))

(go/func TypeSwitch2 ^int [^any v]
  (type-switch v
    (case [string] (return 1)))
  (type-switch [w v] [x w]
    (case [error] (return (len (.Error x)))))
  0)

(go/func Select [^{:tag (chan int)} ch ^{:tag (chan (struct))} done] :results [^int n]
  (select
    (case (>! ch 1))
    (case [v (<! ch)]
      (set! n v))
    (case [(values v ok) (<! ch)]
      (set! (values _ n) (values ok v)))
    (case (set! n (<! ch)))
    (case (set! (values n _) (<! ch)))
    (case (<! done)
      (return))
    (default))
  (select))

(go/func Loops [^{:tag (slice int)} xs ^{:tag (map string int)} m ^{:tag (chan int)} ch] :results [^int n]
  (for [i 0] (< i 10) (inc! i)
    (set! n + i))
  (while (< n 100)
    (set! n * 2))
  (while true
    (break))
  (for [] true _
    (break))
  (for [i 0] _ _
    (set! _ i)
    (break))
  (for [] _ (inc! n)
    (when (> n 5)
      (break)))
  (for [(values i j) (values 0 10)] (< i j) (set! (values i j) (values (+ i 1) (- j 1)))
    (set! n + (- j i)))
  (range [xs])
  (range [i xs]
    (set! n + i))
  (range [i x xs]
    (set! n + (* i x)))
  (range [_ x xs]
    (set! n + x))
  (range [k _ m]
    (set! _ k))
  (range [v ch]
    (set! n + v))
  (range [i 10]
    (set! n + i))
  (let [^string k (zero string)
        ^int v (zero int)]
    ^:assign (range [k v m])
    ^:assign (range [k m])
    (set! (values _ _) (values k v))
    (return)))

(go/func Jumps ^int [^{:tag (slice int)} xs]
  (let [n 0]
    (label :outer
      (range [i xs]
        (range [j xs]
          (when (> j i)
            (continue :outer))
          (when (== j 5)
            (break :outer))
          (when (== j 6)
            (continue))
          (inc! n))))
    (goto :end)
    (label :end (return n))))

(go/func Empty []
  (goto :L)
  (label :L))

(go/func Empty2 [^bool b]
  (when b
    (goto :M))
  (label :M)
  (println))

(go/func GoDefer [^{:tag (chan int)} ch ^{:tag (func [int])} f]
  (go (f 1))
  (defer (f 2))
  (defer ((fn []
            (recover))))
  (go ((fn [^int x]
         (>! ch x))
       3))
  (<! ch)
  (>! ch 4))

(go/func Return [] :results [int string]
  (return 1 "a"))

(go/func Return2 [] :results [^int a ^string b]
  (set! (values a b) (Return))
  (return))

(go/func Return3 [] :results [int string]
  (Return))

(go/func Fn ^int [^int x]
  (when (> x 0)
    (return x))
  (- x))
