(ns go.arbace.cmd.evalproof (:require [arbace.go :as go]))

(go/package main :path "arbace/cmd/evalproof" :files ["main.go"])

(load "evalproof/main")
