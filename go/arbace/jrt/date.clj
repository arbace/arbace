;; jrt: java.util.Date, small and hand-written over its millisecond count (JAVA-SURFACE.md,
;; decision 7, the user's: #inst over a Date shim; doc/go/JRT-NOTES.md, "Phase 2b"). The default
;; time zone is GMT, so the deprecated field accessors and Date.UTC read and write UTC fields.
;; The calendar is Java's: Julian before the Gregorian cutover of 1582-10-15, Gregorian from it
;; (GregorianCalendar's default, which Date and the #inst printer's SimpleDateFormat share);
;; Date.UTC normalizes as jdk26u's Date.normalize does (the year 1582 as GregorianCalendar).
(in-ns 'go.arbace.jrt)

(go/file "date.go"
  :imports [[strconv "strconv"] [time "time"]])

(go/type Date
  "Date is java.util.Date (a leaf: *Date): milliseconds since the epoch.\n"
  (struct Object ^int64 F_fastTime))

(go/var Date_class
  (Define (addr (lit ClassInfo :Name "java.util.Date" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class Cloneable_class Comparable_class)
                     :Go "arbace/jrt.Date"))))

(go/func Date_New "Date_New is new Date(): now, from Go's clock.\n" ^{:tag (* Date)} []
  (addr (lit Date :F_fastTime (.UnixMilli (time/Now)))))
(go/func Date_New_J ^{:tag (* Date)} [^int64 ms] (addr (lit Date :F_fastTime ms)))
(go/func Date_New_I_I_I
  "Date_New_I_I_I is the deprecated new Date(year - 1900, month, date), in GMT.\n"
  ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d]
  (Date_New_J (Date_UTC_I_I_I_I_I_I__J y m d 0 0 0)))
(go/func Date_New_I_I_I_I_I ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi]
  (Date_New_J (Date_UTC_I_I_I_I_I_I__J y m d h mi 0)))
(go/func Date_New_I_I_I_I_I_I ^{:tag (* Date)} [^int32 y ^int32 m ^int32 d ^int32 h ^int32 mi ^int32 s]
  (Date_New_J (Date_UTC_I_I_I_I_I_I__J y m d h mi s)))

(go/method GetTime__J ^int64 [^{:tag (* Date)} t] (.-F_fastTime t))
(go/method SetTime_J__V [^{:tag (* Date)} t ^int64 ms] (set! (.-F_fastTime t) ms))
(go/method Before_Date__Z ^bool [^{:tag (* Date)} t ^{:tag (* Date)} o] (< (.-F_fastTime t) (.-F_fastTime (NN o))))
(go/method After_Date__Z ^bool [^{:tag (* Date)} t ^{:tag (* Date)} o] (> (.-F_fastTime t) (.-F_fastTime (NN o))))
(go/method Equals_O__Z ^bool [^{:tag (* Date)} t ^any o]
  (let [(values x ok) (assert (* Date) o)]
    (and ok (== (.-F_fastTime t) (.-F_fastTime x)))))
(go/method HashCode__I "HashCode__I is Date.hashCode: (int) ht ^ (int) (ht >> 32).\n"
  ^int32 [^{:tag (* Date)} t]
  (let [ht (.-F_fastTime t)]
    (bit-xor (conv int32 ht) (conv int32 (>> ht 32)))))
(go/method CompareTo_Date__I ^int32 [^{:tag (* Date)} t ^{:tag (* Date)} o]
  (let [a (.-F_fastTime t)
        b (.-F_fastTime (NN o))]
    (cond (< a b) (return -1) (== a b) (return 0) :else (return 1))))
(go/method CompareTo_O__I ^int32 [^{:tag (* Date)} t ^any o] (.CompareTo_Date__I t (Date_Cast o)))
(go/method Clone__O ^any [^{:tag (* Date)} t] (Date_New_J (.-F_fastTime t)))
(go/method Ref ^any [^{:tag (* Date)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Date)} t] Date_class)
(go/method Is_Serializable [^{:tag (* Date)} t])
(go/method Is_Cloneable [^{:tag (* Date)} t])
(go/method Is_Comparable [^{:tag (* Date)} t])
(go/func Date_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Date) x)] ok))
(go/func Date_Cast ^{:tag (* Date)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Date) x)]
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
  "dateFields are a Date's fields in GMT: the astronomical year, month 1..12, day, hours,
minutes, seconds, milliseconds, day of the week (1 Sunday ... 7 Saturday), and whether the
date is Julian.\n"
  (struct ^int64 year ^int64 month ^int64 day ^int64 hour ^int64 minute ^int64 second ^int64 millis
          ^int64 weekday ^bool julian))

(go/method fields ^dateFields [^{:tag (* Date)} t]
  (let [ms (.-F_fastTime t)
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

(go/method GetYear__I "GetYear__I is the deprecated getYear: the year minus 1900.\n"
  ^int32 [^{:tag (* Date)} t]
  (let [f (.fields t)]
    (conv int32 (- (.yearOfEra f) 1900))))
(go/method GetMonth__I ^int32 [^{:tag (* Date)} t] (conv int32 (- (.-month (.fields t)) 1)))
(go/method GetDate__I ^int32 [^{:tag (* Date)} t] (conv int32 (.-day (.fields t))))
(go/method GetDay__I ^int32 [^{:tag (* Date)} t] (conv int32 (- (.-weekday (.fields t)) 1)))
(go/method GetHours__I ^int32 [^{:tag (* Date)} t] (conv int32 (.-hour (.fields t))))
(go/method GetMinutes__I ^int32 [^{:tag (* Date)} t] (conv int32 (.-minute (.fields t))))
(go/method GetSeconds__I ^int32 [^{:tag (* Date)} t] (conv int32 (.-second (.fields t))))
(go/method GetTimezoneOffset__I ^int32 [^{:tag (* Date)} t] 0)

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

(go/method ToString__String
  "ToString__String is Date.toString in GMT: Thu Jan 01 00:00:00 GMT 1970.\n"
  ^{:tag (* String)} [^{:tag (* Date)} t]
  (let [f (.fields t)]
    (Str (+ (aget dayNames (.-weekday f)) " " (aget monthNames (.-month f)) " " (pad (.-day f) 2) " "
            (pad (.-hour f) 2) ":" (pad (.-minute f) 2) ":" (pad (.-second f) 2) " GMT "
            (strconv/FormatInt (.yearOfEra f) 10)))))

(go/method ToGMTString__String
  "ToGMTString__String is the deprecated Date.toGMTString: 1 Jan 1970 00:00:00 GMT.\n"
  ^{:tag (* String)} [^{:tag (* Date)} t]
  (let [f (.fields t)]
    (Str (+ (strconv/FormatInt (.-day f) 10) " " (aget monthNames (.-month f)) " "
            (strconv/FormatInt (.yearOfEra f) 10) " " (pad (.-hour f) 2) ":" (pad (.-minute f) 2) ":"
            (pad (.-second f) 2) " GMT"))))

(go/method InstantText
  "InstantText is the text #inst prints for the date, as arbace.instant's SimpleDateFormat
\"yyyy-MM-dd'T'HH:mm:ss.SSS-00:00\" in GMT writes it (the year of the era, at least four
digits): what the reworked arbace.instant needs (JRT-NOTES.md, \"#inst\").\n"
  ^string [^{:tag (* Date)} t]
  (let [f (.fields t)]
    (+ (pad (.yearOfEra f) 4) "-" (pad (.-month f) 2) "-" (pad (.-day f) 2) "T" (pad (.-hour f) 2) ":"
       (pad (.-minute f) 2) ":" (pad (.-second f) 2) "." (pad (.-millis f) 3) "-00:00")))

(go/func init []
  (set! (.-IsInstance (.Info Date_class)) Date_InstanceOf))
