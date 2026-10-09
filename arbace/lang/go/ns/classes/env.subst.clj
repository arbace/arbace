;; The Go build (doc/go/CLASSFORMS-REPL.md): no weak references in jrt's maps; the class infos of
;; reflection are kept
["(def ^:private reflect-cache (java.util.WeakHashMap.))"
 "(def ^:private reflect-cache (java.util.HashMap.))"]
