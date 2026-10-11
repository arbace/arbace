;; jrt's stand-in for java.util.concurrent.TimeUnit (doc/go/JRT-NOTES.md, "Stand-ins" and
;; phase 2a), hand-written in the shape c2g gives a leaf enum (C2G-SPEC §7.13): the timed
;; waits of jrt's locks, latches and futures take one. TimeUnit is translated from jdk26u by
;; c2g; this file is deleted then. Only the conversions jrt uses are here, saturating as
;; Java's.
(in-ns 'go.arbace.jrt)

(go/file "standin_timeunit.go"
  :imports [[math "math"]])

(go/type TimeUnit (struct Enum ^int64 scale))

(go/var TimeUnit_class
  (Define (addr (lit ClassInfo :Name "java.util.concurrent.TimeUnit" :Kind KindEnum
                     :Modifiers (bit-or AccPublic AccFinal AccEnum)
                     :Super Enum_class :Go "arbace/jrt.TimeUnit"))))

(go/var [^{:tag (* TimeUnit)} TimeUnit_NANOSECONDS] [^{:tag (* TimeUnit)} TimeUnit_MICROSECONDS]
        [^{:tag (* TimeUnit)} TimeUnit_MILLISECONDS] [^{:tag (* TimeUnit)} TimeUnit_SECONDS]
        [^{:tag (* TimeUnit)} TimeUnit_MINUTES] [^{:tag (* TimeUnit)} TimeUnit_HOURS]
        [^{:tag (* TimeUnit)} TimeUnit_DAYS])

(go/func newTimeUnit ^{:tag (* TimeUnit)} [^string name ^int32 o ^int64 scale]
  (let [t (addr (lit TimeUnit :scale scale))]
    (.Ctor_String_I (.-Enum t) t (Intern name) o)
    t))

(go/func init []
  (set! TimeUnit_NANOSECONDS (newTimeUnit "NANOSECONDS" 0 1))
  (set! TimeUnit_MICROSECONDS (newTimeUnit "MICROSECONDS" 1 1000))
  (set! TimeUnit_MILLISECONDS (newTimeUnit "MILLISECONDS" 2 1000000))
  (set! TimeUnit_SECONDS (newTimeUnit "SECONDS" 3 1000000000))
  (set! TimeUnit_MINUTES (newTimeUnit "MINUTES" 4 60000000000))
  (set! TimeUnit_HOURS (newTimeUnit "HOURS" 5 3600000000000))
  (set! TimeUnit_DAYS (newTimeUnit "DAYS" 6 86400000000000))
  (set! (.-Enum (.Info TimeUnit_class))
        (fn ^{:tag (* RefArray)} []
          (RefArrayOf TimeUnit_class TimeUnit_NANOSECONDS TimeUnit_MICROSECONDS TimeUnit_MILLISECONDS
                      TimeUnit_SECONDS TimeUnit_MINUTES TimeUnit_HOURS TimeUnit_DAYS)))
  (set! (.-IsInstance (.Info TimeUnit_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert (* TimeUnit) x)] ok))))

(go/method ToNanos_J__J "ToNanos_J__J is TimeUnit.toNanos (saturating).\n"
  ^int64 [^{:tag (* TimeUnit)} u ^int64 d]
  (let [s (.-scale u)]
    (saturate d s)))

(go/func saturate "saturate is d*m, clamped to the long range.\n" ^int64 [^int64 d ^int64 m]
  (when (> d (/ math/MaxInt64 m))
    (return math/MaxInt64))
  (when (< d (/ math/MinInt64 m))
    (return math/MinInt64))
  (* d m))

(go/method ToMillis_J__J "ToMillis_J__J is TimeUnit.toMillis (saturating).\n"
  ^int64 [^{:tag (* TimeUnit)} u ^int64 d]
  (let [s (.-scale u)]
    (when (< s 1000000)
      (return (/ d (/ 1000000 s))))
    (saturate d (/ s 1000000))))

(go/method Ref ^any [^{:tag (* TimeUnit)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* TimeUnit)} t] TimeUnit_class)
(go/method Clone__O ^any [^{:tag (* TimeUnit)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* TimeUnit)} t] (.Impl_ToString__String t t))
(go/method CompareTo_Enum__I ^int32 [^{:tag (* TimeUnit)} t ^Enum_I o] (.Impl_CompareTo_Enum__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* TimeUnit)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* TimeUnit)} t] (.Impl_GetDeclaringClass__Class t t))
