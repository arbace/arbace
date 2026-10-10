;; The Go build's arbace/core/server.clj (doc/go/EVAL-NOTES.md, "Namespace variants";
;; doc/go/JRT-NOTES.md, "Sockets"): prepl makes no DynamicClassLoader for the thread (one loader,
;; C2G-SPEC §10.3), as the Go build's REPL (main.subst.clj).
["(.setContextClassLoader (Thread/currentThread) (DynamicClassLoader. cl))"
 "(.setContextClassLoader (Thread/currentThread) cl)"]
