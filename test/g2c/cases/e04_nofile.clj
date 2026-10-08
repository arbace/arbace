;; g2c-test: error=a declaration before any go/file
(ns go.e04 (:require [arbace.go :as go]))
(go/package e04 :path "e04" :files ["e04.go"])
(go/func f [])
