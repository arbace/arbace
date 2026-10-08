;; SPEC §5: every type form, signatures, struct and interface types, generics.
(ns go.types
  (:require [arbace.go :as go]))

(go/package types :path "types" :files ["s05_types.go"])

(go/file "s05_types.go" :imports [[io "io"] [unsafe "unsafe"]])

(go/type
  [P (* int)]
  [S (slice string)]
  [A (array 4 byte)]
  [AN (array N int)]
  [AE (array (* N 2) int)]
  [M (map string (slice int))]
  [C (chan int)]
  [CR (chan :recv int)]
  [CS (chan :send int)]
  [CC (chan (chan :recv int))]
  [CSR (chan :send (chan :recv int))]
  [F (func [int & (slice any)] [int error])]
  [FN (func [^int x] [^int n ^error err])]
  [FV (func [])]
  [FR (func [] [(func [] [int])])]
  [U unsafe/Pointer]
  [PP (* (* Point))])

(go/const ^{:val 3} N 3)

(go/type Point (struct ^int32 X ^int32 Y))

(go/type Fields
  (struct
    Point
    (* io/SectionReader)
    ^{:tag string :go/tag "json:\"name\"" :doc "Name is documented.\n"} Name
    ^int a ^int b
    ^{:tag (func [])} f
    (Pair string int)
    ^{:tag string :go/tag "plain"} t))

(go/type Pair :type-params [^comparable K ^any V]
  (struct ^K Key ^V Val))

(go/type Shape
  (interface
    (Area ^float64 [])
    io/Reader
    (Read2 [^{:tag (slice byte)} p] :results [^int n ^error err])))

(go/type Number (interface (| (tilde int) (tilde float64) string)))

(go/type Both (interface (tilde int) (String ^string [])))

(go/type slice :type-params [T] (slice T))

(go/type Uses
  (struct
    ^{:tag (inst slice int)} s
    ^{:tag (Pair string int)} p
    ^{:tag io/Reader} r
    ^{:tag (struct)} e
    ^{:tag (interface)} i
    ^any an
    ^{:tag (List (Pair int (inst slice byte)))} l))

(go/type List :type-params [T]
  (struct ^{:tag (* (List T))} next ^T val))

(go/type Ptr :type-params [^{:tag (* int)} P] (struct ^P p))

(go/type Ord :type-params [^{:tag (interface (| (tilde int) (tilde string)))} T] (struct ^T v))

(go/type Small (struct ^{:tag (map string int)} x))

(go/type Large (struct ^{:tag (map string (map string int64))} x))

(go/type OneMethod (interface (String ^string [])))

(go/type LongMethod (interface (Method [^int a ^int b ^int c] :results [string error])))
