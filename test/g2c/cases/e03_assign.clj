;; g2c-test: error=not an assignment operator
(ns go.e03 (:require [arbace.go :as go]))
(go/package e03 :path "e03" :files ["e03.go"])
(go/file "e03.go" :imports [])
(go/func f [^int x] (set! x max 1))
