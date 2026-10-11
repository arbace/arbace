
;; ---------------------------------------------------------------------------------------------
;; The Go build (doc/go/EVAL-NOTES.md, "Namespace variants"; appended to arbace/main.clj in the
;; executable): the REPL's start set trimmed (JAVA-SURFACE.md decision 6): no javadoc (a
;; browser), no repl.deps (processes, URLs); no pprint (its load takes about 8 s on the evaluator:
;; required when used, until start-up work makes it cheap, EVAL-NOTES.md phase 2A).
(def ^{:doc "A sequence of lib specs that are applied to `require`
by default when a new command-line REPL is started."} repl-requires
  '[[arbace.repl :refer (source apropos dir pst doc find-doc)]])
