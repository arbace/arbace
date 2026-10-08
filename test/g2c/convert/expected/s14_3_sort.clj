;; SPEC §14.3, sort (std, tamago/amd64): the excerpts (docs compared as prefixes).
(go/func insertionSort "insertionSort sorts data[a:b] using insertion sort. ..." [^Interface data ^int a ^int b]
  (for [i (+ a 1)] (< i b) (inc! i)
    (for [j i] (and (> j a) (.Less data j (- j 1))) (dec! j)
      (.Swap data j (- j 1)))))

(go/func siftDown "siftDown implements the heap property on data[lo:hi]. ..." [^Interface data ^int lo ^int hi ^int first]
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

(go/type IntSlice "IntSlice attaches the methods of Interface to []int, sorting in increasing order.\n" (slice int))

(go/method Len ^int [^IntSlice x] (len x))
(go/method Less ^bool [^IntSlice x ^int i ^int j] (< (aget x i) (aget x j)))
(go/method Swap [^IntSlice x ^int i ^int j]
  (set! (values (aget x i) (aget x j)) (values (aget x j) (aget x i))))

(go/func Ints "Ints sorts a slice of ints in increasing order. ..." [^{:tag (slice int)} x] (^{:inst [(slice int) int]} slices/Sort x))
