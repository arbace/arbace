;; SPEC §14.5: the amended forms (2026-10-08): a generic field group (A1), a local const group
;; with an implicit spec (C2), a labeled declaration (C9), a parenthesized || operand (C3),
;; a directive across a blank line (C1).
(ns go.p (:require [arbace.go :as go]))
(go/package p :path "p" :files ["s14_5_amended.go"])
(go/file "s14_5_amended.go" :imports [["cmp" "cmp"]])

(go/func ^:go/noinline Clamp :type-params [^cmp/Ordered T]
  ^T [^T x ^:go/grouped ^T lo ^:go/grouped ^T hi]
  (min (max x lo) hi))

(go/func Count ^int [^{:tag (slice byte)} s ^byte c]
  (let [^:const ^{:val -1} none (- iota 1)
        ^:const ^:go/grouped ^:go/implicit ^{:val 0} one (- iota 1)
        n (+ none one)]
    (let [^{:go/label :retry} i 0]
      (while (and (< i (len s)) ^:go/paren (or (== (aget s i) c) (== c 0)))
        (inc! n)
        (inc! i))
      (when (< n 0)
        (set! n 0)
        (goto :retry))
      n)))
