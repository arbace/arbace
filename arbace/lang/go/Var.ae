;; Go-build variant of arbace.lang.Var (C2G-SPEC §4.6): read by c2g only, never by the JVM
;; build (doc/go/SPEED-NOTES.md). A dynamic var's thread binding is looked up with valAt, not
;; entryAt: the bindings map Vars to TBoxes, never to nil, so the result is the same, and no
;; MapEntry is allocated on every deref of a bound var (the JVM's escape analysis removes that
;; allocation; Go's cannot, the entry crossing an interface call).
(in-ns 'arbace.lang)

(c2g/variant Var
  (method ^:public ^:final getThreadBinding ^TBox [this]
    (when (.get threadBound)
      (return (cast TBox (.valAt (.-bindings (cast Frame (.get dvals))) this))))
    nil))
