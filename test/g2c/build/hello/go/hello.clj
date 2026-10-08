;; The package file of the program hello (doc/go/BUILD.md): a main package of one Go file.
(ns go.hello
  (:require [arbace.go :as go]))

(go/package main
  :path "hello"
  :files ["main.go"])

(load "hello/main")
