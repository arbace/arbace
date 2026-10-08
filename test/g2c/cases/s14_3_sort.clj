;; g2c-test: lines   SPEC §14.3: sort (excerpts), forms on Go's lines
(ns go.sort (:require [arbace.go :as go]))
(go/package sort :path "sort" :files ["s14_3_sort.go"] :positions :lines)
(go/file "s14_3_sort.go" :imports [[slices "slices"]])
(go/type Interface (interface
  (Len ^int [])
  (Less ^bool [^int i ^int j])
  (Swap [^int i ^int j])))


^{:go/end 17} (go/func insertionSort [^Interface data ^int a ^int b]
  (for [i (+ a 1)] (< i b) (inc! i)
    (for [j i] (and (> j a) (.Less data j (- j 1))) (dec! j)
      (.Swap data j (- j 1)))))




^{:go/end 35} (go/func siftDown [^Interface data ^int lo ^int hi ^int first]
  (let [root lo]
    (while true
      (let [child (+ (* 2 root) 1)]
        (when (>= child hi)
          (break))

        (when (and (< (+ child 1) hi) (.Less data (+ first child) (+ first child 1)))
          (inc! child))

        (when (not (.Less data (+ first root) (+ first child)))
          (return))

        (.Swap data (+ first root) (+ first child))
        (set! root child)))))



(go/type IntSlice (slice int))

^{:go/end 39} (go/method Len ^int [^IntSlice x] (len x))
^{:go/end 40} (go/method Less ^bool [^IntSlice x ^int i ^int j] (< (aget x i) (aget x j)))
^{:go/end 41} (go/method Swap [^IntSlice x ^int i ^int j] (set! (values (aget x i) (aget x j)) (values (aget x j) (aget x i))))

^{:go/end 43} (go/func Ints [^{:tag (slice int)} x] (^{:inst [(slice int) int]} slices/Sort x))
