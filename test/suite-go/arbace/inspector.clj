;; A stand-in of arbace.inspector for Clojure's suite on the Go build only: bin/clojure-tests
;; puts test/suite-go on ARBACE_PATH for its Go runs (doc/go/EVAL-NOTES.md, "The suite's Java
;; fixtures"). arbace.inspector (Swing) is not in the Go build (B1-PLAN.md, D6), but
;; clojure.test-clojure.java-interop requires it in its ns form; the one test that calls it,
;; test-proxy-non-serializable, is skipped there (test/arbace-go-results.edn, :skipped-tests).
;; Its function throws, as a cut class's members do.
(ns arbace.inspector
  "Not in the Go build (D6: Swing): a stand-in for Clojure's suite.")

(defn list-model
  "Not in the Go build: throws."
  [& _]
  (throw (UnsupportedOperationException. "arbace.inspector is not in the Go build (D6: Swing)")))
