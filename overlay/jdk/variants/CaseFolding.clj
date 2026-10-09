;; Go-build variant of jdk.internal.lang.CaseFolding (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Phase
;; 2C"): read by c2g only. The generated class collects its expanded case map's keys through a
;; stream (java.util.stream is cut); the variant collects them with a loop, in the same
;; (unspecified) order of the map's key set. Copyright (c) the Arbace authors; Eclipse Public
;; License 1.0.
(in-ns 'jdk.internal.lang)

(c2g/variant CaseFolding
  (field ^:private ^:static ^:final ^int/1 expanded_case_cps
    (CaseFolding/keysOf expanded_case_map))

  (c2g/add
    (method ^:private ^:static keysOf ^int/1 [^Map m]
      (let [ks (.keySet m)
            a (new int/1 (.size ks))
            ^:mutable ^int i 0]
        (for-each [^Object k ks]
          (aset a i (.intValue (cast Integer k)))
          (set! i (unchecked-inc-int i)))
        a))))
