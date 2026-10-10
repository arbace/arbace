;; The Go build's arbace/core_bean.clj (doc/go/EVAL-NOTES.md, "Namespace variants"): the Go
;; build has no modules, so every package counts as exported
["(.isExported (.getModule c) (.getPackageName c))" "true"]
