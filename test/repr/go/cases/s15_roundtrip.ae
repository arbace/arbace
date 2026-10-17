;; The forms the round trip needed beyond the earlier cases (amendments C3-C6, the printer's
;; fixes): a directive among the imports, local generic types, a parameter named true, nested
;; labels, the most negative integer literal, an empty const group, parenthesized && and ||
;; operands.
(ns go.rt (:require [arbace.go :as go]))
(go/package rt :path "rt" :files ["s15_roundtrip.go"])
(go/file "s15_roundtrip.go" :build ["//go:build tamago"]
  :imports [(go/directive "//go:generate echo among the imports") ["fmt" "fmt"]])

(go/func Local ^any []
  (let-type [^{:type-params [^any T]} Box (struct ^T v)
             ^{:type-params [^comparable K ^any V]} Pair (struct ^K k ^V v)]
    (lit (inst Box (inst Pair string int)))))

(go/func Param ^bool [^bool (go/id "true")]
  (not (go/id "true")))

(go/func Labels ^int [^int n]
  (label :outer
    (label :inner
      (for [i 0] (< i n) (inc! i)
        (when (> i 2) (break :inner))
        (when (> i 3) (continue :inner)))))
  (when (< n 0) (goto :outer))
  (let [^:var x -0x8000000000000000]
    (switch
      (case [(> n 0)] (return (+ x n))))
    0))

(go/const)

(go/func Logic ^bool [^bool a ^bool b ^bool c]
  (or (and ^:go/paren (or a b) ^:go/paren (and b c))
      (and a (or false c))))

(go/var _ fmt/Sprint)
