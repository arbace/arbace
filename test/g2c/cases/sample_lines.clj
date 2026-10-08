;; g2c-test: original=sample.go lines
(ns go.sample (:require [arbace.go :as go]))
(go/package sample :path "sample" :files ["sample.go"] :init-order [ErrEmpty] :positions :lines)
(go/file "sample.go" :imports [[errors "errors"] [fmt "fmt"]])



(go/type Point (struct ^int32 X ^int32 Y))

(go/type Named (struct
  ^{:line 11} Point                                    ; embedded
  ^{:line 12 :tag string} Name))


^{:go/end 15} (go/method Move [^{:tag (* Point)} p ^int32 dx] (set! (.-X p) + dx))

^{:go/end 17} (go/method String ^string [^Point p] (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))

(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))

^{:go/end 21} (go/method Push [^{:tag (* (Stack T))} s ^T x] (set! (.-items s) (append (.-items s) x)))

^{:go/end 31} (go/method Pop [^{:tag (* (Stack T))} s] :results [T bool]
  (let [^T zero (zero T)]
    (when (== (len (.-items s)) 0)
      (return zero false))

    (let [x (aget (.-items s) (- (len (.-items s)) 1))]
      (set! (.-items s) (subslice (.-items s) _ (- (len (.-items s)) 1)))
      (return x true))))


(go/const
  [^{:val 0} A (* iota 10)]
  [^{:val 10 :line 35} B]
  [^{:val 1267650600228229401496703205376N} Big (<< 1 100)]
  [^{:val 4} Small (>> Big 98)])


(go/var ErrEmpty (errors/New "empty"))

^{:go/end 47} (go/func Sum [^{:tag (slice uint8)} xs] :results [^uint8 s]
  (range [_ x xs]
    (set! s + x))                          ; wraps

  (return))


^{:go/end 57} (go/func Safe [^{:tag (func [])} f] :results [^error err]
  (defer (^{:go/end 54} (fn []
            (when [r (recover)] (!= r nil)
              (set! err (fmt/Errorf "recovered: %v" r))))))


  (f)
  nil)


^{:go/end 77} (go/func Pipeline ^int [^int n]
  (let [ch (make (chan int))
        done (make (chan (struct)))]
    (go (^{:go/end 71} (fn []
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


^{:go/end 89} (go/func Describe ^string [^any v]
  (type-switch [x v]
    (case [nil]
      (return "nil"))
    (case [fmt/Stringer]
      (return (.String x)))
    (case [int int64]
      (return (fmt/Sprint x))))

  "?")


^{:go/end 97} (go/func Use ^int32 []
  (let [n (lit Named (lit Point 1 2) "n")]
    ^{:go/via [Point]} (.Move n Small)
    (let [p (addr (.-Y (.-Point n)))]
      (inc! @p)
      (+ ^{:go/via [Point]} (.-X n) @p))))
