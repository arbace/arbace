;; g2c-test: error=values outside a return
(ns go.e01 (:require [arbace.go :as go]))
(go/package e01 :path "e01" :files ["e01.go"])
(go/file "e01.go" :imports [])
(go/func f ^int [] (+ 1 (values 1 2)))
