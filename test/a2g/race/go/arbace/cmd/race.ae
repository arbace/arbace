;; c2g's race program (doc/go/C2G-NOTES.md, "Phase 2D: races"): threads sharing translated
;; objects the ways Clojure's runtime shares them (hash caches, interning, atoms, namespaces),
;; for a build with Go's race detector. Built by bin/c2g-race.
(ns go.arbace.cmd.race (:require [arbace.go :as go]))
(go/package main :path "arbace/cmd/race" :files ["main.go"])
(load "race/main")
