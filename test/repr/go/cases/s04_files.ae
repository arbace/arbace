;; SPEC §4: the file header (build lines, directives, package doc), every kind of import spec,
;; qualified and dot-imported names, go/call and go/id, labels.
(ns go.files
  (:require [arbace.go :as go]))

(go/package main :path "files" :files ["s04_files.go"])

(go/file "s04_files.go"
  :build ["//go:build tamago || linux"]
  :directives ["//go:debug panicnil=1"]
  :doc "Command files exercises the file header of SPEC §4.3: build lines, header\ndirectives, the package doc and every kind of import.\n"
  :imports [[_ "embed"]
            [. "math"]
            ^:alias [r "math/rand"]
            [strings "strings"]
            [utf8 "unicode/utf8"]])

(go/var ^int sink)

(go/func main []
  (set! sink (+ (utf8/RuneLen \a) (conv int (Sqrt 4)) (r/Intn 1) (conv int (Floor Pi))))
  (set! sink + (len (strings/Repeat "x" 2)))
  (let [fn (fn ^int [^int x] (+ x 1))]
    (set! sink (go/call fn sink))
    (g fn)
    (let [len (fn ^int [^string s] 0)]
      (set! sink + (go/call len "abc"))
      (let [(go/id "true") false]
        (set! _ (go/id "true"))
        (let [(values do when) (values 1 2)]
          (set! sink + (+ do when))
          (label :L
            (while true
              (break :L))))))))

(go/func g [^{:tag (func [int] [int])} f])
