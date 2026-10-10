;; The Go build's arbace/main.clj (doc/go/EVAL-NOTES.md, "Namespace variants"):
;; - the REPL makes no DynamicClassLoader for the thread (one loader, C2G-SPEC §10.3);
;; - an error report is printed with prn, not pprint (loading pprint takes about 8 s). Since
;;   java.nio.file's Files is in the Go build (JRT-NOTES.md, "Files"), the report goes to a
;;   temporary file as on the JVM.
["(.setContextClassLoader (Thread/currentThread) (arbace.lang.DynamicClassLoader. cl))"
 "(.setContextClassLoader (Thread/currentThread) cl)"

 "((requiring-resolve 'arbace.pprint/pprint) report)"
 "(prn report)"]
