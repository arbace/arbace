;; Go-build variant of sun.util.resources.OpenListResourceBundle (C2G-SPEC §4.6;
;; doc/go/JRT-NOTES.md, "Time"): read by c2g only. jdk26u computes the bundle's map and key set
;; once through java.lang.LazyConstant (a preview API the Go build does not have); the variant
;; keeps them in volatile fields computed at the first use (a race computes them twice, from the
;; same contents, as a LazyConstant's supplier may not run twice: the values are equal).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.util.resources)

(c2g/variant OpenListResourceBundle
  (field ^:private ^:volatile ^{:tag (Map String Object)} lookup)

  (field ^:private ^:volatile ^{:tag (Set String)} keyset)

  (method ^:protected handleGetObject [this ^String key]
    (Objects/requireNonNull key)
    (.get (.lookupMap this) key))

  (method ^:protected handleKeySet ^{:tag (Set String)} [this]
    (.keySet (.lookupMap this)))

  (method ^:public keySet ^{:tag (Set String)} [this]
    (let [^:mutable ^{:tag (Set String)} ks keyset]
      (when (nil? ks)
        (set! ks (.keyset0 this))
        (set! keyset ks))
      ks))

  (c2g/add
    (method ^:private lookupMap ^{:tag (Map String Object)} [this]
      (let [^:mutable ^{:tag (Map String Object)} m lookup]
        (when (nil? m)
          (set! m (.lookup0 this))
          (set! lookup m))
        m))))
