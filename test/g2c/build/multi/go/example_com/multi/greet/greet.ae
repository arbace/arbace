(in-ns 'go.example_com.multi.greet)

(go/file "greet.go"
  :doc "Package greet makes greetings.\n"
  :imports [[_ "embed"] [strings "strings"]])

(go/var ^{:go/embed "banner.txt" :tag string
          :doc "Banner is the program's banner, embedded from banner.txt.\n"}
        Banner)

(go/func Greeting
  "Greeting greets name, loudly when it ends in \"!\". The name \"panic\" panics.\n"
  ^string [^string name]
  (when (== name "panic")
    (panic "greet: asked to panic"))
  (when (strings/HasSuffix name "!")
    (return (+ "HELLO, " (strings/ToUpper (strings/TrimSuffix name "!")) "!")))
  (+ "hello, " name))
