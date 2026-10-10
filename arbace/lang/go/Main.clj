;; Go-build variant of arbace.lang.Main (C2G-SPEC §4.6): read by c2g only. B1a step 6
;; (doc/go/EXEC-NOTES.md): the program tells the image of prepared namespaces when the start is
;; over (arbace.core and arbace.main loaded), so that the start's garbage collector setting
;; ends there.
(in-ns 'arbace.lang)

(c2g/variant Main
  (method ^:public ^:static main ^void [^String/1 args]
    (RT/init)
    (.invoke REQUIRE CLOJURE_MAIN)
    (Compiler$Image/started)
    (.applyTo MAIN (RT/seq args))))
