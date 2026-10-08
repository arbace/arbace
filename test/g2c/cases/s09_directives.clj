;; SPEC §9: attached directives (sorted, after the doc comment), free-standing directives, a
;; //line directive in a body, go:linkname, body-less functions, go:embed; §12.4 other and
;; embedded files.
(ns go.dirs
  (:require [arbace.go :as go]))

(go/package dirs :path "dirs" :files ["dirs.go"]
  :config {:goos "tamago" :goarch "amd64" :goamd64 "v1" :cgo false}
  :other-files ["asm_amd64.s"]
  :embed-files [["a.txt" "b6a98d9ce9a2d9149288fa3df42d377c3e42737afdcdaf714e33c0a100b51060"] ["b.txt" "f2c82decdd7181cf98945929a62598db7e6b477e11f6e0eb0ae97020eff151ad"]])

(go/file "dirs.go" :imports [[embed "embed"] [_ "unsafe"]])

(go/var ^{:go/embed ["a.txt" "b.txt"] :tag embed/FS} files)

(go/var ^{:go/embed "a.txt" :tag string} text)

(go/func ^:go/nosplit ^:go/noinline f ^int [] 1)

(go/func ^:go/noinline g "g is documented.\n" ^int [] (f))

(go/func ^:extern ^{:go/linkname "nanotime runtime.nanotime"} nanotime ^int64 [])

(go/func ^:extern asm ^int [])

(go/func ^{:go/linkname "pushed"} pushed ^int [] 2)

(go/directive "//go:generate echo hi")

(go/func body []
  (go/directive "//line foo.go:10")
  (set! _ 1))

(go/var [^{:go/embed "b.txt" :tag string} more])
