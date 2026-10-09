;; Go-build variant of arbace.lang.Reflector (C2G-SPEC §4.6): read by c2g only. The JVM build
;; looks up Method.canAccess through a method handle (for Java 8); the Go build has no method
;; handles, and canAccess is true without one, as on Java 8.
(in-ns 'arbace.lang)

(c2g/variant Reflector
  (static-initializer
    (set! CAN_ACCESS_PRED nil))

  (method ^:private ^:static canAccess ^boolean [^java.lang.reflect.Method m target] true))
