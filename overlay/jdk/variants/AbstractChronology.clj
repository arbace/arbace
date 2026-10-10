;; Go-build variant of java.time.chrono.AbstractChronology (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Time"): read by c2g only. The Go build has the ISO chronology only (the Hijrah, Japanese,
;; Minguo and Thai Buddhist chronologies are cut) and no ServiceLoader: the cache registers
;; IsoChronology alone, and a chronology not found is the JDK's DateTimeException at once.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.time.chrono)

(c2g/variant AbstractChronology
  (method ^:private ^:static initCache ^boolean []
    (if (nil? (.get CHRONOS_BY_ID "ISO"))
        (do
          (AbstractChronology/registerChrono IsoChronology/INSTANCE)
          true)
        false))

  (method ^:static ofLocale ^Chronology [^Locale locale]
    (Objects/requireNonNull locale "locale")
    (let [type (.getUnicodeLocaleType locale "ca")]
      (if (or (nil? type) (.equals "iso" type) (.equals "iso8601" type))
          IsoChronology/INSTANCE
          (do
            (loop []
              (let [chrono (cast Chronology (.get CHRONOS_BY_TYPE type))]
                (when (some? chrono)
                  (return chrono)))
              (when (AbstractChronology/initCache)
                (recur)))
            (throw (DateTimeException. (java-str "Unknown calendar system: " type)))))))

  (method ^:static of ^Chronology [^String id]
    (Objects/requireNonNull id "id")
    (loop []
      (let [chrono (AbstractChronology/of0 id)]
        (when (some? chrono)
          (return chrono)))
      (when (AbstractChronology/initCache)
        (recur)))
    (throw (DateTimeException. (java-str "Unknown chronology: " id))))

  (method ^:static getAvailableChronologies ^Set [] 
    (AbstractChronology/initCache)
    (HashSet. (.values CHRONOS_BY_ID))))
