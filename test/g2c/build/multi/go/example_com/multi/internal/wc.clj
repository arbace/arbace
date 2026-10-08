;; A local package of the program multi, internal to its module.
(ns go.example_com.multi.internal.wc
  (:require [arbace.go :as go]))

(go/package wc
  :path "example.com/multi/internal/wc"
  :files ["wc.go"])

(load "wc/wc")
