;; The Go build's arbace/java/io.clj (doc/go/EVAL-NOTES.md, "Namespace variants"): unchanged but
;; for reflection warnings, which it turns on and which the Go build's world would print at
;; every start, arbace.core requiring it: java.nio's channels (copy's fast path between files),
;; FileInputStream/FileOutputStream on java.io.File, the context class loader's URLs (D6, cut)
["(set! *warn-on-reflection* true)" "(set! *warn-on-reflection* false)"]
