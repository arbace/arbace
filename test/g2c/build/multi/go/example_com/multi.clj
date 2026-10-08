;; The main package of the program multi (doc/go/BUILD.md).
(ns go.example_com.multi
  (:require [arbace.go :as go]))

(go/package main
  :path "example.com/multi"
  :files ["main.go"])

(load "multi/main")
