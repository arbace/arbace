;; The Go build (doc/go/CLASSFORMS-REPL.md): jrt's classes have no Package objects; the runtime's
;; package is arbace/lang
["(str/replace (.getName (.getPackage arbace.lang.RT)) \".\" \"/\"))"
 "\"arbace/lang\")"]
