;; The Go build (doc/go/CLASSFORMS-REPL.md): no weak references in jrt's maps; the class infos of
;; reflection are kept
["(def ^:private reflect-cache (java.util.WeakHashMap.))"
 "(def ^:private reflect-cache (java.util.HashMap.))"

 "                    :class c}]\n          (.put reflect-cache c info)"
 "                    :class c}\n              info (if (and (not (.isInterface c)) (empty? (filter #(= \"<init>\" (:name %)) (:methods info))))\n                     (update info :methods into (for [[d f] ((requiring-resolve 'arbace.classes.go/constructors) n)]\n                                                  {:name \"<init>\" :desc d :flags f :owner n :throws []}))\n                     info)]\n          (.put reflect-cache c info)"]
