;; Go-build variant of sun.util.resources.BreakIteratorResourceBundle (C2G-SPEC §4.6;
;; doc/go/JRT-NOTES.md, "Text and locales"): read by c2g only. The JDK reads a break iterator's
;; rule data from its bundle's module (getClass().getModule().getResourceAsStream; the Go
;; build has no modules); the variant reads the same path through Class.getResourceAsStream,
;; absolute, from the resource data bin/jrt-convert makes as the JDK build does
;; (sun/text/resources/WordBreakIteratorData, ...; amendment RD1).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.util.resources)

(c2g/variant BreakIteratorResourceBundle
  (method ^:protected handleGetObject [this ^String key]
    (when (.contains NON_DATA_KEYS key)
      (return nil))
    (let [info (.getBreakIteratorInfo this)]
      (when (not (.containsKey info key))
        (return nil))
      (let [path (java-str (.replace (.getPackageName (.getClass this)) \. \/) "/" (.getString info key))
            ^:mutable ^byte/1 data nil]
        (try
          (with-resources [is (.getResourceAsStream (.getClass this) (java-str "/" path))]
            (set! data (.readAllBytes is)))
          (catch Exception e
            (throw (InternalError. (java-str "Can't load " path) e))))
        data))))
