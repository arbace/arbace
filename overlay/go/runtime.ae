;; The Go runtime files Arbace patches (doc/go/C2G-SPEC.md §9.3, §16 Q19; doc/go/JRT-NOTES.md,
;; phase 2a), as Go forms: proc.go and runtime2.go converted from go1.27.1 (bin/g2c convert
;; --goos linux runtime; the same forms for amd64 and arm64) with the patch's two changes,
;; marked "Arbace", and the new file arbace_local.go. Not a package of the program: bin/jrt
;; overlay prints these files and writes an overlay (BUILD.md, amendment B5) that replaces
;; $GOROOT/src/runtime's proc.go and runtime2.go and adds arbace_local.go.
(ns go.runtime
  (:require [arbace.go :as go]))

(go/package runtime
  :path "runtime"
  :files ["arbace_local.go" "proc.go" "runtime2.go"])

(load "runtime/arbace_local")
(load "runtime/proc")
(load "runtime/runtime2")
