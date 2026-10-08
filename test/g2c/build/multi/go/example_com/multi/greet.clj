;; A local package of the program multi.
(ns go.example_com.multi.greet
  (:require [arbace.go :as go]))

(go/package greet
  :path "example.com/multi/greet"
  :files ["greet.go"]
  :embed-files [["banner.txt" "460e44bbb9c16d0e2dfba575906c1465ec649da04d8b099506d70b6384ede358"]])

(load "greet/greet")
