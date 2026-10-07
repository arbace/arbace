(ns arbace.classes.native
  "The boundary between arbace.lang.Compiler and the class forms compiler (SPEC §9.5).

  arbace.lang.Compiler knows the class forms' special forms (class*, label*, switch*, ...) but
  does not compile them itself: there is one implementation of the class forms, this library.
  The compiler loads this namespace on first use and calls the functions below:

  - `compile-top`, for (class* :top form) and (class* :tops forms), the expansions of
    `defclass` and `defclasses`: compiles and defines the classes, and returns the form the
    compiler analyzes in place of the class form (imports and the class or classes)."
  (:require [arbace.classes.compiler :as compiler]))

(defn compile-top
  "(class* :top form) or (class* :tops forms), each form the rest of a defclass form, in
  namespace ns: compiles the classes as one compilation, with `siblings` (the class forms of
  the enclosing top-level do, SPEC §9.2) entered as declarations, and defines them (and writes
  them under *compile-files*). Returns the form evaluating to the class (:top) or a vector of
  the classes (:tops) after importing them."
  [ns kind forms siblings]
  (let [forms (if (= :top kind) [forms] (vec forms))
        names (mapv first (compiler/compile-and-load! ns forms :siblings siblings))]
    `(do ~@(for [n names] (list 'arbace.core/import* n))
         ~(if (= :top kind) (symbol (first names)) (mapv symbol names)))))
