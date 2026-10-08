;; g2c-test: lines   SPEC §10: forms on Go's lines
(ns go.positions (:require [arbace.go :as go]))
(go/package positions :path "positions" :files ["s10_positions.go"] :positions :lines)
(go/file "s10_positions.go" :imports [[strings "strings"] [bytes "bytes"]])

(go/var x (+ (strings/ToUpper "a") (conv string (bytes/ToUpper nil))))

(go/var table ^{:go/breaks [2 6]} (lit (array 8 uint8)
  1 2 3 4
  5 6 7 8))


(go/var rows ^{:go/breaks [2 4]} (lit (array ... string)
  "a" "b"
  "c"))


^{:go/end 22} (go/func Call ^int [^int a ^int b ^int c]
  (add a
    ^{:line 20} b
    ^{:line 21} c))


^{:go/end 32} (go/func Multi ^int [^int x]
  (let [y ^{:go/breaks [2]} (+ x
         1)
        z (add
            ^{:line 28} x
            ^{:line 29} y)]

    ^{:line 31} z))


^{:go/end 34} (go/func add ^int [& ^{:tag (slice int)} xs] (len xs))

^{:go/end 47} (go/func Lit ^{:tag (slice int)} [^int n]
  (let [s (lit (slice int)
            ^{:line 38} n
            (+ n 1))

        m (lit (map string int)
            ^{:line 42} ["a" 1]
            ^{:line 43} ["bcd" 2])]

    (set! _ m)
    ^{:line 46} s))


^{:go/end 54} (go/func Closure ^{:tag (func [] [int])} []
  (let [f ^{:go/end 52 :go/breaks [2]} (fn ^int []
          1)]

    ^{:line 53} f))


(go/type Doc (struct
  ^{:doc "A is a.\n" :tag int :line 58} A

  ^{:doc "B is b.\n" :tag string :line 61} B))




^{:go/end 69} (go/func Label []
  (label :L
    (while true
      (break :L))))



^{:go/end 71} (go/func Short ^int [] 1)
^{:go/end 72} (go/func Short2 ^int [] 2)
