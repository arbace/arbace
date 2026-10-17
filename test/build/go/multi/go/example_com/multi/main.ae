(in-ns 'go.example_com.multi)

(go/file "main.go"
  :doc "Multi greets each argument in a goroutine of its own, counts its standard input in
another, and prints the greetings in argument order, then the counts. Without arguments it
complains on standard error and exits with 2; an argument \"panic\" makes it panic.\n"
  :imports [[fmt "fmt"] [os "os"] [sync "sync"]
            [greet "example.com/multi/greet"] [wc "example.com/multi/internal/wc"]])

(go/type result
  (struct ^int i ^string msg))

(go/func main []
  (let [args (subslice os/Args 1)
        results (make (chan result) (len args))
        ^sync/WaitGroup wg (zero sync/WaitGroup)]
    (range [i a args]
      (.Add wg 1)
      ;; Done after the send, not deferred: a panicking goroutine never lets main go on, so
      ;; the output stays deterministic
      (go ((fn []
             (>! results (lit result i (greet/Greeting a)))
             (.Done wg)))))
    (let [counts (make (chan wc/Counts) 1)
          errs (make (chan error) 1)]
      (go ((fn []
             (let [(values c err) (wc/Count os/Stdin)]
               (when (!= err nil)
                 (>! errs err)
                 (return))
               (>! counts c)))))
      (.Wait wg)
      (close results)
      (let [msgs (make (slice string) (len args))]
        (range [r results]
          (aset msgs (.-i r) (.-msg r)))
        (fmt/Print greet/Banner)
        (range [_ m msgs]
          (fmt/Println m))
        (select
          (case [c (<! counts)]
            (fmt/Println "stdin:" c))
          (case [err (<! errs)]
            (fmt/Fprintln os/Stderr "multi:" err)
            (os/Exit 1)))
        (when (== (len args) 0)
          (fmt/Fprintln os/Stderr "multi: no arguments")
          (os/Exit 2))))))
