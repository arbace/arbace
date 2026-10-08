;; Driver of the class forms compiler (doc/classes/SPEC.md §9.6), at every stage.
;;
;; The compiler's sources name the runtime they run on as Arbace's: `arbace.core`,
;; `arbace.string`, `arbace.lang.RT`, `arbace.asm.ClassWriter`. That is the running runtime, and
;; this driver just requires the compiler's namespaces; `arbace.core` itself holds the class
;; forms' names (arbace/core_classes.clj) and `arbace.lang.Compiler` knows their special forms.
;; (Until the freeze, stage 0 was the frozen Clojure, and this driver read the compiler's
;; sources there with arbace.X renamed to clojure.X; that path is on the branch
;; arbace-for-java-26.)
;;
;; Use: (require 'arbace.classes.boot), or (load-file "arbace/classes/boot.clj").
(ns arbace.classes.boot)

(def ^:private namespaces
  '[arbace.classes.types
    arbace.classes.env
    arbace.classes.parse
    arbace.classes.lower
    arbace.classes.analyze
    arbace.classes.emit
    arbace.classes.compiler
    arbace.classes.native
    arbace.classes.shape
    arbace.classes.build])

(defn load-compiler!
  "Loads the compiler's namespaces in order."
  []
  (apply require namespaces))

(load-compiler!)
