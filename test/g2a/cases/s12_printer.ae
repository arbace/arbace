;; g2c-test: nogofmt   (gofmt's stripParens drops the parentheses that `switch (Pair[int, int]{})`
;; needs, so the gofmt layout cannot be gofmt's fixed point here)
;; SPEC §12.2: parentheses the grammar needs (composite literals in headers, conversions to
;; *, <-, func and chan types), tokens that must not touch; Go 1.27 generic methods and
;; promoted keys.
(ns go.printer
  (:require [arbace.go :as go]))

(go/package printer :path "printer" :files ["s12_printer.go"])

(go/file "s12_printer.go" :imports [])

(go/type P (struct ^int X))

(go/type N (struct P ^string Name))

(go/type Pair :type-params [^comparable K ^any V] (struct ^K Key ^V Val))

(go/type List :type-params [E] (struct ^{:tag (slice E)} items))

(go/method Apply "Apply is a generic method (Go 1.27).\n" :type-params [F]
  ^{:tag (List F)} [^{:tag (List E)} l ^{:tag (func [E] [F])} f]
  (let [^{:tag (List F)} out (zero (List F))]
    (range [_ x (.-items l)]
      (set! (.-items out) (append (.-items out) (f x))))
    out))

(go/var promoted ^{:go/via {:X [P]}} (lit N :X 1 :Name "a"))

(go/var applied (^{:inst [string]} .Apply (lit (List int)) (fn ^string [^int x] "")))

(go/func Headers ^int [^P p ^{:tag (Pair int int)} pr ^{:tag (func [] [int])} f]
  (switch (lit (Pair int int))
    (case [pr] (return 1)))
  (when (== pr (lit (Pair int int) 1 2))
    (return 2))
  (when (== (addr p) (addr (lit P)))
    (return 3))
  (when (g (lit P 1))
    (return 4))
  (range [_ x (.-items (lit (List int)))]
    (return x))
  (when (== ((conv (func [] [int]) f)) 0)
    (return 5))
  0)

(go/func g ^bool [P] false)

(go/func Exprs [^{:tag (chan int)} c ^{:tag (* P)} p ^int a ^int b]
  (set! _ (conv (chan :recv int) c))
  (set! _ (conv (chan :send int) c))
  (set! _ (method-expr (* P) Get))
  (set! _ (<! c))
  (set! _ (- (<! c)))
  (set! _ (- a (- b)))
  (set! _ (+ a (+ b)))
  (set! _ (/ a @(addr b)))
  (set! _ @(addr a))
  (set! _ (not (not true)))
  (set! _ (bit-not (bit-not a)))
  (set! _ (- (+ a b)))
  (set! _ (* (+ a b) (- (- a b))))
  (set! _ (>> (<< a b) a))
  (set! _ (bit-and-not a (bit-not b)))
  (>! c -1)
  (>! c (<! c))
  (dec! (.-X p)))

(go/method Get ^int [^{:tag (* P)} p] (.-X p))
