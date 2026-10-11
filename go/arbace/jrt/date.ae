;; jrt: java.util.Date, small and hand-written over its millisecond count (JAVA-SURFACE.md,
;; decision 7, the user's: #inst over a Date shim; doc/go/JRT-NOTES.md, "Phase 2b"). The
;; deprecated field accessors, toString and the deprecated local constructors read and write the
;; fields in the default time zone, as the JDK's, through the DateZone hooks, which c2g sets to
;; jrt's own jdk.internal.jrt.DefaultZone over the translated TimeZone (JRT-NOTES.md, "Time");
;; without them (jrt's own tests) the zone is GMT. Date.UTC, toGMTString and #inst's text are
;; GMT's.
;; The calendar is Java's: Julian before the Gregorian cutover of 1582-10-15, Gregorian from it
;; (GregorianCalendar's default, which Date and the #inst printer's SimpleDateFormat share);
;; Date.UTC normalizes as jdk26u's Date.normalize does (the year 1582 as GregorianCalendar).
;; Date is non-leaf since phase 2B ("Dates"): c2g translates java.sql.Timestamp, which extends
;; it, so it has Date_I and Impl_ methods (C2G-SPEC §5.3, §5.4); toInstant and from(Instant)
;; are c2g's table entries (they name the translated Instant).
(in-ns 'go.arbace.jrt)

(go/file "date.go"
  :imports [[strconv "strconv"] [time "time"]])

(go/type Date_I
  "Date_I is java.util.Date's class interface (a non-leaf class since java.sql.Timestamp, which
c2g translates, extends it: Impl_ methods, C2G-SPEC §5.3, §5.4).\n"
  (interface Object_I
    (Self_Date ^{:tag (* Date)} [])
    (Is_Serializable [])
    (Is_Cloneable [])
    (Is_Comparable [])
    (GetTime__J ^int64 [])
    (SetTime_J__V [^int64 ms])
    (Before_Date__Z ^bool [^Date_I o])
    (After_Date__Z ^bool [^Date_I o])
    (CompareTo_Date__I ^int32 [^Date_I o])
    (CompareTo_O__I ^int32 [^any o])
    (GetYear__I ^int32 [])
    (GetMonth__I ^int32 [])
    (GetDate__I ^int32 [])
    (GetDay__I ^int32 [])
    (GetHours__I ^int32 [])
    (GetMinutes__I ^int32 [])
    (GetSeconds__I ^int32 [])
    (GetTimezoneOffset__I ^int32 [])
    (ToGMTString__String ^{:tag (* String)} [])))

(go/type Date
  "Date is java.util.Date's struct: milliseconds since the epoch.\n"
  (struct Object ^int64 F_fastTime))

(go/var Date_class
  (Define (addr (lit ClassInfo :Name "java.util.Date" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class Cloneable_class Comparable_class)
                     :Go "arbace/jrt.Date"))))

(go/func Date_New "Date_New is new Date(): now, from Go's clock.\n" ^{:tag (* Date)} []
  (let [t (addr (lit Date))] (.Ctor t t) t))
(go/func Date_New_J ^{:tag (* Date)} [^int64 ms] (let [t (addr (lit Date))] (.Ctor_J t t ms) t))
(go/func Date_New_I_I_I
  "Date_New_I_I_I is the deprecated new Date(year - 1900, month, date), in the default zone.\n"
  ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d]
  (let [t (addr (lit Date))] (.Ctor_I_I_I t t y m d) t))
(go/func Date_New_I_I_I_I_I ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi]
  (let [t (addr (lit Date))] (.Ctor_I_I_I_I_I t t y m d h mi) t))
(go/func Date_New_I_I_I_I_I_I ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi ^int32 s]
  (let [t (addr (lit Date))] (.Ctor_I_I_I_I_I_I t t y m d h mi s) t))

(go/method Ctor "Ctor is Date(): now, from Go's clock.\n" [^{:tag (* Date)} t ^Date_I this]
  (set! (.-F_fastTime t) (.UnixMilli (time/Now))))
(go/method Ctor_J [^{:tag (* Date)} t ^Date_I this ^int64 ms] (set! (.-F_fastTime t) ms))
(go/method Ctor_I_I_I [^{:tag (* Date)} t ^Date_I this ^int32 y ^int32 m ^int32 d]
  (set! (.-F_fastTime t) (utcOfLocal (Date_UTC_I_I_I_I_I_I__J y m d 0 0 0))))
(go/method Ctor_I_I_I_I_I [^{:tag (* Date)} t ^Date_I this ^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi]
  (set! (.-F_fastTime t) (utcOfLocal (Date_UTC_I_I_I_I_I_I__J y m d h mi 0))))
(go/method Ctor_I_I_I_I_I_I [^{:tag (* Date)} t ^Date_I this ^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi ^int32 s]
  (set! (.-F_fastTime t) (utcOfLocal (Date_UTC_I_I_I_I_I_I__J y m d h mi s))))

;; the implementations: this is the whole object, called virtually where the JDK's Date calls
;; getTime() (equals, hashCode, and getMillisOf's subclass case in compareTo, before, after)
(go/method Impl_GetTime__J ^int64 [^{:tag (* Date)} t ^Date_I this] (.-F_fastTime t))
(go/method Impl_SetTime_J__V [^{:tag (* Date)} t ^Date_I this ^int64 ms] (set! (.-F_fastTime t) ms))
(go/method Impl_Before_Date__Z ^bool [^{:tag (* Date)} t ^Date_I this ^Date_I o]
  (< (.GetTime__J this) (.GetTime__J o)))
(go/method Impl_After_Date__Z ^bool [^{:tag (* Date)} t ^Date_I this ^Date_I o]
  (> (.GetTime__J this) (.GetTime__J o)))
(go/method Impl_Equals_O__Z ^bool [^{:tag (* Date)} t ^Date_I this ^any o]
  (let [(values x ok) (assert Date_I o)]
    (and ok (== (.GetTime__J this) (.GetTime__J x)))))
(go/method Impl_HashCode__I "Impl_HashCode__I is Date.hashCode: (int) ht ^ (int) (ht >> 32), ht = getTime().\n"
  ^int32 [^{:tag (* Date)} t ^Date_I this]
  (let [ht (.GetTime__J this)]
    (bit-xor (conv int32 ht) (conv int32 (>> ht 32)))))
(go/method Impl_CompareTo_Date__I ^int32 [^{:tag (* Date)} t ^Date_I this ^Date_I o]
  (let [a (.GetTime__J this)
        b (.GetTime__J o)]
    (cond (< a b) (return -1) (== a b) (return 0) :else (return 1))))
(go/method Impl_CompareTo_O__I ^int32 [^{:tag (* Date)} t ^Date_I this ^any o]
  (.CompareTo_Date__I this (Date_Cast o)))
(go/type dateCloner "dateCloner: a translated subclass's shallow copy (c2g's CloneShallow).\n"
  (interface (CloneShallow ^any [])))
(go/method Impl_Clone__O "Impl_Clone__O is Date.clone: a copy of the whole object.\n"
  ^any [^{:tag (* Date)} t ^Date_I this]
  (let [(values d ok) (assert (* Date) this)]
    (when ok (return (Date_New_J (.-F_fastTime d)))))
  (.CloneShallow (assert dateCloner this)))

;; the dispatch methods of the concrete Date
(go/method GetTime__J ^int64 [^{:tag (* Date)} t] (.Impl_GetTime__J t t))
(go/method SetTime_J__V [^{:tag (* Date)} t ^int64 ms] (.Impl_SetTime_J__V t t ms))
(go/method Before_Date__Z ^bool [^{:tag (* Date)} t ^Date_I o] (.Impl_Before_Date__Z t t o))
(go/method After_Date__Z ^bool [^{:tag (* Date)} t ^Date_I o] (.Impl_After_Date__Z t t o))
(go/method Equals_O__Z ^bool [^{:tag (* Date)} t ^any o] (.Impl_Equals_O__Z t t o))
(go/method HashCode__I ^int32 [^{:tag (* Date)} t] (.Impl_HashCode__I t t))
(go/method CompareTo_Date__I ^int32 [^{:tag (* Date)} t ^Date_I o] (.Impl_CompareTo_Date__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* Date)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method Clone__O ^any [^{:tag (* Date)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Date)} t] (.Impl_ToString__String t t))
(go/method ToGMTString__String ^{:tag (* String)} [^{:tag (* Date)} t] (.Impl_ToGMTString__String t t))
(go/method GetYear__I ^int32 [^{:tag (* Date)} t] (.Impl_GetYear__I t t))
(go/method GetMonth__I ^int32 [^{:tag (* Date)} t] (.Impl_GetMonth__I t t))
(go/method GetDate__I ^int32 [^{:tag (* Date)} t] (.Impl_GetDate__I t t))
(go/method GetDay__I ^int32 [^{:tag (* Date)} t] (.Impl_GetDay__I t t))
(go/method GetHours__I ^int32 [^{:tag (* Date)} t] (.Impl_GetHours__I t t))
(go/method GetMinutes__I ^int32 [^{:tag (* Date)} t] (.Impl_GetMinutes__I t t))
(go/method GetSeconds__I ^int32 [^{:tag (* Date)} t] (.Impl_GetSeconds__I t t))
(go/method GetTimezoneOffset__I ^int32 [^{:tag (* Date)} t] (.Impl_GetTimezoneOffset__I t t))

(go/method Self_Date ^{:tag (* Date)} [^{:tag (* Date)} t] t)
(go/method Ref ^any [^{:tag (* Date)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Date)} t] Date_class)
(go/method Is_Serializable [^{:tag (* Date)} t])
(go/method Is_Cloneable [^{:tag (* Date)} t])
(go/method Is_Comparable [^{:tag (* Date)} t])
(go/func Date_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Date_I x)] ok))
(go/func Date_Cast ^Date_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Date_I x)]
    (when (not ok) (panic (ClassCast x Date_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; The calendar: days since 1970-01-01 to and from Julian and Gregorian dates

(go/const
  [^{:tag int64 :val 86400000} dayMillis 86400000]
  [^{:tag int64 :val -12219292800000 :doc "the Gregorian cutover, 1582-10-15T00:00Z\n"}
   gregorianCutover -12219292800000]
  [^{:tag int64 :val -141427} cutoverDay -141427])

(go/func floorDiv ^int64 [^int64 a ^int64 b]
  (let [q (/ a b)]
    (when (and (!= (* q b) a) (!= (< a 0) (< b 0)))
      (dec! q))
    q))

(go/func floorMod ^int64 [^int64 a ^int64 b] (- a (* (floorDiv a b) b)))

(go/func gregorianDays
  "gregorianDays: the days since 1970-01-01 of the proleptic Gregorian y-m-d (m 1..12, d any:
days beyond the month count on).\n"
  ^int64 [^int64 y ^int64 m ^int64 d]
  (when (<= m 2)
    (dec! y))
  (let [era (floorDiv y 400)
        yoe (- y (* era 400))
        mp (floorMod (+ m 9) 12)
        doy (+ (/ (+ (* 153 mp) 2) 5) (- d 1))
        doe (+ (* yoe 365) (/ yoe 4) (- (/ yoe 100)) doy)]
    (+ (* era 146097) doe -719468)))

(go/func julianDays
  "julianDays: the days since 1970-01-01 of the Julian y-m-d (astronomical years: 0 is 1 BC).\n"
  ^int64 [^int64 y ^int64 m ^int64 d]
  (let [a (floorDiv (- 14 m) 12)
        yy (- (+ y 4800) a)
        mm (- (+ m (* 12 a)) 3)]
    (- (+ d (floorDiv (+ (* 153 mm) 2) 5) (* 365 yy) (floorDiv yy 4) -32083) 2440588)))

(go/func civilOfDays
  "civilOfDays: the date of a day count in the calendar Date uses for it (Julian before the
cutover): astronomical year, month 1..12, day.\n"
  [^int64 days] :results [^int64 y ^int64 m ^int64 d]
  (if (>= days cutoverDay)
    (let [z (+ days 719468)
          era (floorDiv z 146097)
          doe (- z (* era 146097))
          yoe (/ (- doe (/ doe 1460) (- (/ doe 36524)) (/ doe 146096)) 365)
          doy (- doe (+ (* 365 yoe) (/ yoe 4) (- (/ yoe 100))))
          mp (/ (+ (* 5 doy) 2) 153)]
      (set! d (+ (- doy (/ (+ (* 153 mp) 2) 5)) 1))
      (set! m (+ mp 3))
      (when (> m 12)
        (set! m (- m 12)))
      (set! y (+ yoe (* era 400)))
      (when (<= m 2)
        (inc! y))
      (return))
    (let [c (+ days 2440588 32082)
          dd (floorDiv (+ (* 4 c) 3) 1461)
          e (- c (floorDiv (* 1461 dd) 4))
          mm (floorDiv (+ (* 5 e) 2) 153)]
      (set! d (+ (- e (floorDiv (+ (* 153 mm) 2) 5)) 1))
      (set! m (- (+ mm 3) (* 12 (floorDiv mm 10))))
      (set! y (+ (- dd 4800) (floorDiv mm 10)))
      (return))))

(go/type dateFields
  "dateFields are a Date's fields in a zone: the astronomical year, month 1..12, day, hours,
minutes, seconds, milliseconds, day of the week (1 Sunday ... 7 Saturday), and whether the
date is Julian.\n"
  (struct ^int64 year ^int64 month ^int64 day ^int64 hour ^int64 minute ^int64 second ^int64 millis
          ^int64 weekday ^bool julian))

(go/var
  [^{:tag (func [int64] [int32])
     :doc "DateZoneOffset is the default zone's offset in milliseconds at an instant (TimeZone.getOffset), or nil: GMT.\n"}
   DateZoneOffset nil]
  [^{:tag (func [int64] [int32])
     :doc "DateZoneOffsetByWall is the default zone's offset in milliseconds at a local time, as the JDK's calendars resolve one (ZoneInfo.getOffsetsByWall), or nil: GMT.\n"}
   DateZoneOffsetByWall nil]
  [^{:tag (func [int64] [string])
     :doc "DateZoneName is the default zone's short name at an instant, as Date.toString writes it (US English), or nil: GMT.\n"}
   DateZoneName nil])

(go/func localOffset "localOffset is the default zone's offset at the instant ms (0 without the hooks).\n"
  ^int64 [^int64 ms]
  (when (== DateZoneOffset nil)
    (return 0))
  (conv int64 (DateZoneOffset ms)))

(go/func utcOfLocal "utcOfLocal is the instant of the local time local in the default zone.\n"
  ^int64 [^int64 local]
  (when (== DateZoneOffsetByWall nil)
    (return local))
  (- local (conv int64 (DateZoneOffsetByWall local))))

(go/method fields "fields are the date's fields in the default zone.\n" ^dateFields [^{:tag (* Date)} t]
  (.fieldsAt t (localOffset (.-F_fastTime t))))

(go/method fieldsAt "fieldsAt are the date's fields at the offset off (milliseconds).\n"
  ^dateFields [^{:tag (* Date)} t ^int64 off]
  (let [ms (+ (.-F_fastTime t) off)
        days (floorDiv ms dayMillis)
        tod (floorMod ms dayMillis)
        (values y m d) (civilOfDays days)]
    (lit dateFields :year y :month m :day d :hour (/ tod 3600000) :minute (% (/ tod 60000) 60)
         :second (% (/ tod 1000) 60) :millis (% tod 1000) :weekday (+ (floorMod (+ days 4) 7) 1)
         :julian (< days cutoverDay))))

(go/method yearOfEra
  "yearOfEra: the year as Java prints a calendar date's year (y, or 1 - y before 1 AD in the
Julian calendar).\n"
  ^int64 [^dateFields f]
  (when (and (.-julian f) (<= (.-year f) 0))
    (return (- 1 (.-year f))))
  (.-year f))

(go/func Date_UTC_I_I_I_I_I_I__J
  "Date_UTC_I_I_I_I_I_I__J is the deprecated Date.UTC(year - 1900, month, date, hrs, min,
sec): the milliseconds of those GMT fields, normalized as Date.normalize does (out-of-range
fields count on; the calendar of the year, Julian before 1582, and the other one when the
result falls on its side of the cutover; in 1582 GregorianCalendar's rule).\n"
  ^int64 [^int32 year ^int32 month ^int32 date ^int32 hrs ^int32 mi ^int32 sec]
  (let [y (+ (conv int64 year) 1900)
        mo (conv int64 month)]
    (set! y (+ y (floorDiv mo 12)))
    (set! mo (+ (floorMod mo 12) 1))
    (let [tod (* (+ (* (+ (* (conv int64 hrs) 60) (conv int64 mi)) 60) (conv int64 sec)) 1000)
          d (conv int64 date)
          g (+ (* (gregorianDays y mo d) dayMillis) tod)
          j (+ (* (julianDays y mo d) dayMillis) tod)]
      (when (== y 1582)
        (when (>= (floorDiv g dayMillis) cutoverDay)
          (return g))
        (return j))
      (if (>= y 1582)
        (do
          (when (>= g gregorianCutover)
            (return g))
          (return j))
        (do
          (when (< j gregorianCutover)
            (return j))
          (return g))))))

(go/method Impl_GetYear__I "Impl_GetYear__I is the deprecated getYear: the year minus 1900.\n"
  ^int32 [^{:tag (* Date)} t ^Date_I this]
  (let [f (.fields t)]
    (conv int32 (- (.yearOfEra f) 1900))))
(go/method Impl_GetMonth__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (- (.-month (.fields t)) 1)))
(go/method Impl_GetDate__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (.-day (.fields t))))
(go/method Impl_GetDay__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (- (.-weekday (.fields t)) 1)))
(go/method Impl_GetHours__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (.-hour (.fields t))))
(go/method Impl_GetMinutes__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (.-minute (.fields t))))
(go/method Impl_GetSeconds__I ^int32 [^{:tag (* Date)} t ^Date_I this] (conv int32 (.-second (.fields t))))
(go/method Impl_GetTimezoneOffset__I
  "Impl_GetTimezoneOffset__I is the deprecated getTimezoneOffset: minutes to add to the local
time for UTC.\n"
  ^int32 [^{:tag (* Date)} t ^Date_I this]
  (conv int32 (/ (- (localOffset (.-F_fastTime t))) 60000)))

(go/var
  [^{:tag (slice string)} dayNames (lit (slice string) "" "Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat")]
  [^{:tag (slice string)} monthNames
   (lit (slice string) "" "Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec")])

(go/func pad
  "pad: v in decimal, zero-padded to n digits (a minus sign before them).\n"
  ^string [^int64 v ^int n]
  (let [neg (< v 0)]
    (when neg
      (set! v (- v)))
    (let [s (strconv/FormatInt v 10)]
      (while (< (len s) n)
        (set! s (+ "0" s)))
      (when neg
        (set! s (+ "-" s)))
      s)))

(go/method Impl_ToString__String
  "Impl_ToString__String is Date.toString in the default zone: Thu Jan 01 01:00:00 CET 1970.\n"
  ^{:tag (* String)} [^{:tag (* Date)} t ^Date_I this]
  (let [f (.fields t)
        zone "GMT"]
    (when (!= DateZoneName nil)
      (set! zone (DateZoneName (.-F_fastTime t))))
    (Str (+ (aget dayNames (.-weekday f)) " " (aget monthNames (.-month f)) " " (pad (.-day f) 2) " "
            (pad (.-hour f) 2) ":" (pad (.-minute f) 2) ":" (pad (.-second f) 2) " " zone " "
            (strconv/FormatInt (.yearOfEra f) 10)))))

(go/method Impl_ToGMTString__String
  "Impl_ToGMTString__String is the deprecated Date.toGMTString: 1 Jan 1970 00:00:00 GMT.\n"
  ^{:tag (* String)} [^{:tag (* Date)} t ^Date_I this]
  (let [f (.fieldsAt t 0)]
    (Str (+ (strconv/FormatInt (.-day f) 10) " " (aget monthNames (.-month f)) " "
            (strconv/FormatInt (.yearOfEra f) 10) " " (pad (.-hour f) 2) ":" (pad (.-minute f) 2) ":"
            (pad (.-second f) 2) " GMT"))))

(go/method InstantText
  "InstantText is the text #inst prints for the date, as arbace.instant's SimpleDateFormat
\"yyyy-MM-dd'T'HH:mm:ss.SSS-00:00\" in GMT writes it (the year of the era, at least four
digits): what the reworked arbace.instant needs (JRT-NOTES.md, \"#inst\").\n"
  ^string [^{:tag (* Date)} t]
  (let [f (.fieldsAt t 0)]
    (+ (pad (.yearOfEra f) 4) "-" (pad (.-month f) 2) "-" (pad (.-day f) 2) "T" (pad (.-hour f) 2) ":"
       (pad (.-minute f) 2) ":" (pad (.-second f) 2) "." (pad (.-millis f) 3) "-00:00")))

(go/func init []
  (set! (.-IsInstance (.Info Date_class)) Date_InstanceOf))
