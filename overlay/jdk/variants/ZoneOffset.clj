;; Go-build variant of java.time.ZoneOffset (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Time"): read
;; by c2g only. The cache of offsets by quarter hours is a ConcurrentHashMap keyed by the
;; quarters (AtomicReferenceArray is not in the closed world); as the JDK's, it keeps one
;; instance per quarter-hour offset, the first one made. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'java.time)

(c2g/variant ZoneOffset
  (field ^:private ^:static ^:final ^ConcurrentMap QUARTER_CACHE (ConcurrentHashMap. 16 (float 0.75) 4))

  (method ^:public ^:static ofTotalSeconds ^ZoneOffset [^int totalSeconds]
    (when (or (> totalSeconds MAX_SECONDS) (< totalSeconds (- MAX_SECONDS)))
      (throw (DateTimeException. "Zone offset not in valid range: -18:00 to +18:00")))
    (let [quarters (unchecked-divide-int totalSeconds SECONDS_PER_QUARTER)]
      (if (== (unchecked-multiply-int quarters SECONDS_PER_QUARTER) totalSeconds)
          (let [key (Integer/valueOf quarters)
                ^:mutable result (cast ZoneOffset (.get QUARTER_CACHE key))]
            (when (nil? result)
              (set! result (ZoneOffset. totalSeconds))
              (let [existing (cast ZoneOffset (.putIfAbsent QUARTER_CACHE key result))]
                (when (some? existing)
                  (set! result existing)))
              (.putIfAbsent ID_CACHE (.getId result) result))
            result)
          (ZoneOffset. totalSeconds)))))
