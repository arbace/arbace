(in-ns 'go.hello)

(go/file "main.go"
  :doc "Hello greets, prints its arguments one per line and exits with their count.\n"
  :imports [[fmt "fmt"] [os "os"]])

(go/func main []
  (fmt/Println "hello, world")
  (range [i a (subslice os/Args 1)]
    (fmt/Printf "arg %d: %q\n" (+ i 1) a))
  (os/Exit (- (len os/Args) 1)))
