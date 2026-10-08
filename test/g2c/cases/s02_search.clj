;; g2c-test: lines   (SPEC §2, sort.Search)
(ns go.search (:require [arbace.go :as go]))
(go/package search :path "search" :files ["s02_search.go"])
(go/file "s02_search.go" :imports [])
^{:go/end 20} (go/func Search "Search uses binary search to find and return the smallest index i in [0, n) at which\nf(i) is true.\n" ^int [^int n ^{:tag (func [int] [bool])} f]
  ;; Define f(-1) == false and f(n) == true.
  ;; Invariant: f(i-1) == false, f(j) == true.
  (let [(values i j) (values 0 n)]
    (while (< i j)
      (let [h (conv int (>> (conv uint (+ i j)) 1))]   ; avoid overflow when computing h
        ;; i ≤ h < j
        (if (not (f h))
          (set! i (+ h 1))                              ; preserves f(i-1) == false

          (set! j h))))                                 ; preserves f(j) == true


    ;; i == j, f(i-1) == false, and f(j) (= f(i)) == true  =>  answer is i.
    ^{:line 19} i))
