;; SPEC §4.3: a file whose package clause differs from go/package's name (an external test
;; package, §15 Q14).
(ns go.files_test (:require [arbace.go :as go]))
(go/package files :path "files" :files [] :test-files ["s04_testpkg.go"])
(go/file "s04_testpkg.go" :package files_test :imports [[testing "testing"]])
(go/func TestX [^{:tag (* testing/T)} t]
  (.Log t "x"))
