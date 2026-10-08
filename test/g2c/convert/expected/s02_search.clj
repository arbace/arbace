;; SPEC §2, the first example: sort.Search (doc comment shortened in the source).
(go/func Search
  "Search uses binary search to find and return the smallest index i\nin [0, n) at which f(i) is true.\n"
  ^int [^int n ^{:tag (func [int] [bool])} f]
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
    i))
