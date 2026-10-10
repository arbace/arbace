(in-ns 'go.arbace.jrt)

;; jrt: String's Locale overloads (doc/go/JRT-NOTES.md, "Phase 2b" and "Text and locales"). The
;; Locale is jdk26u's, translated by c2g (jrt's own build has a stand-in, standin_locale.clj);
;; java.text's DecimalFormatSymbols is jdk26u's too. The case mapping is the root locale's for
;; every locale: no Turkish, Azerbaijani or Lithuanian rules (a deviation for those locales).

(go/file "locale.go")

;; ---------------------------------------------------------------------------------------
;; String's Locale overloads: the root locale's case mapping for every locale (no Turkish,
;; Azerbaijani or Lithuanian rules: JRT-NOTES.md, "Locales")

(go/method ToLowerCase_Locale__String ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* Locale)} l]
  (NN l)
  (.ToLowerCase__String t))

(go/method ToUpperCase_Locale__String ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* Locale)} l]
  (NN l)
  (.ToUpperCase__String t))

