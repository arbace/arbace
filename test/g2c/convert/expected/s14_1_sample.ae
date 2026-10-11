;; SPEC §14.1, go/sample/sample.clj: the declarations, as the spec shows them.
(go/type Point (struct ^int32 X ^int32 Y))

(go/type Named (struct
  Point                                    ; embedded
  ^string Name))

^{:go/end 15} (go/method Move [^{:tag (* Point)} p ^int32 dx] (set! (.-X p) + dx))

(go/method String ^string [^Point p] (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))

(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))

(go/method Push [^{:tag (* (Stack T))} s ^T x] (set! (.-items s) (append (.-items s) x)))

(go/method Pop [^{:tag (* (Stack T))} s] :results [T bool]
  (let [^T zero (zero T)]
    (when (== (len (.-items s)) 0)
      (return zero false))

    (let [x (aget (.-items s) (- (len (.-items s)) 1))]
      (set! (.-items s) (subslice (.-items s) _ (- (len (.-items s)) 1)))
      (return x true))))


(go/const
  [^{:val 0} A (* iota 10)]
  [^{:val 10} B]
  [^{:val 1267650600228229401496703205376N} Big (<< 1 100)]
  [^{:val 4} Small (>> Big 98)])


(go/var ErrEmpty (errors/New "empty"))

(go/func Sum [^{:tag (slice uint8)} xs] :results [^uint8 s]
  (range [_ x xs]
    (set! s + x))                          ; wraps

  (return))

(go/func Safe [^{:tag (func [])} f] :results [^error err]
  (defer ((fn []
            (when [r (recover)] (!= r nil)
              (set! err (fmt/Errorf "recovered: %v" r))))))


  (f)
  nil)

(go/func Pipeline ^int [^int n]
  (let [ch (make (chan int))
        done (make (chan (struct)))]
    (go ((fn []
           (defer (close ch))
           (range [i n]
             (select
               (case (>! ch i))
               (case (<! done)
                 (return)))))))




    (let [total 0]
      (range [v ch]
        (set! total + v))

      total)))

(go/func Describe ^string [^any v]
  (type-switch [x v]
    (case [nil]
      (return "nil"))
    (case [fmt/Stringer]
      (return (.String x)))
    (case [int int64]
      (return (fmt/Sprint x))))

  "?")

(go/func Use ^int32 []
  (let [n (lit Named (lit Point 1 2) "n")]
    ^{:go/via [Point]} (.Move n Small)
    (let [p (addr (.-Y (.-Point n)))]
      (inc! @p)
      (+ ^{:go/via [Point]} (.-X n) @p))))
