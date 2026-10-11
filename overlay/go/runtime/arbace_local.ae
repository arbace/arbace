;; Arbace's patch of the Go runtime (doc/go/C2G-SPEC.md §9.3, doc/go/JRT-NOTES.md, phase 2a):
;; the goroutine-local slot. With the field arbaceLocal of g (runtime2.go) and its clearing in
;; gdestroy (proc.go), this file is the whole patch; the three files enter a build as an
;; overlay (bin/jrt overlay). jrt pulls the two functions with //go:linkname.

(in-ns 'go.runtime) (go/file "arbace_local.go" :imports [[unsafe "unsafe"]])

(go/func ^:go/nosplit ^{:go/linkname "arbace_getLocal"} arbace_getLocal
  "arbace_getLocal returns the current goroutine's slot: nil in a new goroutine.\n"
  ^unsafe/Pointer []
  (.-arbaceLocal (getg)))

(go/func ^:go/nosplit ^{:go/linkname "arbace_setLocal"} arbace_setLocal
  "arbace_setLocal sets the current goroutine's slot; the garbage collector sees it as any
pointer field of g, and gdestroy clears it.\n"
  [^unsafe/Pointer p]
  (set! (.-arbaceLocal (getg)) p))
