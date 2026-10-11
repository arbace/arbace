;; SPEC §4.1: a package file loading one forms file per Go file; imports are per file.
(ns go.multi
  (:require [arbace.go :as go]))

(go/package multi
  :path "multi"
  :config {:goos "tamago" :goarch "amd64"}
  :files ["a.go" "b.go"]
  :init-order [b])

(load "s04_multi/a" "s04_multi/b")
