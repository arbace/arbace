;; java.time (doc/go/JRT-NOTES.md, "Time"): the local and zoned date-times, instants, durations
;; and periods, the time-zone database and its rules, the formatters (ISO, patterns, localized
;; styles and skeletons, zone names), java.time.temporal and the ISO chronology, and their
;; interplay with java.util's TimeZone, Calendar and Date and #inst. Deterministic: fixed
;; instants and zones, no clock, the default zone not used; locales the root locale and English.

(import '(java.time Clock DayOfWeek Duration Instant LocalDate LocalDateTime LocalTime Month
                    MonthDay OffsetDateTime OffsetTime Period Year YearMonth ZoneId ZoneOffset
                    ZonedDateTime DateTimeException)
        '(java.time.format DateTimeFormatter DateTimeFormatterBuilder DateTimeParseException
                           DecimalStyle FormatStyle ResolverStyle SignStyle TextStyle)
        '(java.time.temporal ChronoField ChronoUnit IsoFields JulianFields TemporalAdjusters
                             TemporalQueries ValueRange WeekFields)
        '(java.time.chrono ChronoLocalDate Chronology IsoChronology IsoEra)
        '(java.time.zone ZoneRules ZoneRulesProvider ZoneOffsetTransition)
        '(java.util Locale TimeZone GregorianCalendar Calendar Date SimpleTimeZone))

(defn ex-msg [f] (try (f) (catch Exception e [(.getName (class e)) (.getMessage e)])))

;; ----- LocalDate
(str (LocalDate/of 2020 6 15))
(str (LocalDate/of 2020 Month/FEBRUARY 29))
(ex-msg #(LocalDate/of 2021 2 29))
(ex-msg #(LocalDate/of 2021 13 1))
(ex-msg #(LocalDate/of 2021 4 31))
(str (LocalDate/ofEpochDay 0))
(str (LocalDate/ofEpochDay -1))
(str (LocalDate/ofEpochDay 100000))
(str (LocalDate/ofYearDay 2024 366))
(str LocalDate/MIN)
(str LocalDate/MAX)
(str LocalDate/EPOCH)
(str (LocalDate/of -1 1 1))
(str (LocalDate/of 10000 1 1))
(str (LocalDate/of 0 12 31))
(str (LocalDate/parse "2020-06-15"))
(ex-msg #(LocalDate/parse "2020-6-15"))
(ex-msg #(LocalDate/parse "2020-02-30"))
(str (LocalDate/parse "+12345-01-02"))
(let [d (LocalDate/of 2020 6 15)]
  [(.getYear d) (.getMonthValue d) (.getMonth d) (.getDayOfMonth d) (.getDayOfYear d)
   (.getDayOfWeek d) (.isLeapYear d) (.lengthOfMonth d) (.lengthOfYear d) (.toEpochDay d)
   (str (.getEra d)) (str (.getChronology d))])
(let [d (LocalDate/of 2020 1 31)]
  (mapv str [(.plusMonths d 1) (.plusMonths d 13) (.minusMonths d 1) (.plusDays d 30)
             (.plusWeeks d 52) (.plusYears (LocalDate/of 2020 2 29) 1) (.minusYears d 2021)
             (.withDayOfMonth d 1) (.withMonth d 2) (.withYear (LocalDate/of 2020 2 29) 2021)
             (.withDayOfYear d 366)]))
(let [a (LocalDate/of 2020 1 31) b (LocalDate/of 2021 3 1)]
  [(str (.until a b)) (.until a b ChronoUnit/DAYS) (.until a b ChronoUnit/MONTHS)
   (.until a b ChronoUnit/WEEKS) (.until b a ChronoUnit/YEARS) (.compareTo a b)
   (.isBefore a b) (.isAfter a b) (.isEqual a a) (= a (LocalDate/of 2020 1 31)) (.hashCode a)])
(mapv str (iterator-seq (.iterator (.datesUntil (LocalDate/of 2020 2 26) (LocalDate/of 2020 3 3)))))
(mapv str (iterator-seq (.iterator (.datesUntil (LocalDate/of 2020 1 31) (LocalDate/of 2020 8 1) (Period/ofMonths 2)))))
(str (.atStartOfDay (LocalDate/of 2020 3 29) (ZoneId/of "Europe/London")))
(str (.atStartOfDay (LocalDate/of 2015 3 29) (ZoneId/of "Asia/Gaza")))
(str (.atTime (LocalDate/of 2020 6 15) 13 45 30 123456789))
(mapv #(str (.with (LocalDate/of 2020 6 15) %))
      [(TemporalAdjusters/firstDayOfMonth) (TemporalAdjusters/lastDayOfMonth)
       (TemporalAdjusters/firstDayOfNextMonth) (TemporalAdjusters/firstDayOfYear)
       (TemporalAdjusters/lastDayOfYear) (TemporalAdjusters/firstDayOfNextYear)
       (TemporalAdjusters/next DayOfWeek/MONDAY) (TemporalAdjusters/previousOrSame DayOfWeek/MONDAY)
       (TemporalAdjusters/firstInMonth DayOfWeek/FRIDAY) (TemporalAdjusters/lastInMonth DayOfWeek/FRIDAY)
       (TemporalAdjusters/dayOfWeekInMonth 3 DayOfWeek/TUESDAY)
       (TemporalAdjusters/dayOfWeekInMonth -2 DayOfWeek/SUNDAY)])
(ex-msg #(.plusDays LocalDate/MAX 1))
(ex-msg #(.get (LocalDate/of 2020 1 1) ChronoField/HOUR_OF_DAY))
(.range (LocalDate/of 2020 2 1) ChronoField/DAY_OF_MONTH)
(str (.range (LocalDate/of 2021 2 1) ChronoField/DAY_OF_MONTH))
(str (.range (LocalDate/of 2020 2 1) ChronoField/ALIGNED_WEEK_OF_MONTH))

;; ----- LocalTime, LocalDateTime
(str (LocalTime/of 0 0))
(str (LocalTime/of 13 45))
(str (LocalTime/of 13 45 30))
(str (LocalTime/of 13 45 30 100000000))
(str (LocalTime/of 13 45 30 120000))
(str (LocalTime/of 13 45 30 1))
(mapv str [LocalTime/MIN LocalTime/MAX LocalTime/NOON LocalTime/MIDNIGHT])
(ex-msg #(LocalTime/of 24 0))
(ex-msg #(LocalTime/parse "25:00"))
(str (LocalTime/ofSecondOfDay 86399))
(str (LocalTime/ofNanoOfDay 1234567890123))
(let [t (LocalTime/of 23 59 59 999999999)]
  (mapv str [(.plusNanos t 1) (.plusHours t 25) (.minusMinutes t 1440) (.withHour t 0)
             (.truncatedTo t ChronoUnit/MINUTES) (.truncatedTo t ChronoUnit/HALF_DAYS)
             (.truncatedTo t ChronoUnit/MILLIS)]))
(.toSecondOfDay (LocalTime/of 13 45 30))
(.toNanoOfDay (LocalTime/of 13 45 30 5))
(str (LocalDateTime/of 2020 6 15 13 45 30 123000000))
(str (LocalDateTime/parse "2020-06-15T13:45"))
(str (LocalDateTime/parse "2020-06-15T13:45:30.5"))
(ex-msg #(LocalDateTime/parse "2020-06-15 13:45"))
(str (LocalDateTime/ofEpochSecond 1592228730 5 ZoneOffset/UTC))
(str (LocalDateTime/ofEpochSecond -1 0 (ZoneOffset/ofHours 2)))
(str (LocalDateTime/ofInstant (Instant/ofEpochSecond 1592228730) (ZoneId/of "Asia/Tokyo")))
(let [a (LocalDateTime/of 2020 1 31 23 0) b (LocalDateTime/of 2020 3 1 1 30)]
  [(.until a b ChronoUnit/HOURS) (.until a b ChronoUnit/MONTHS) (.until a b ChronoUnit/DAYS)
   (str (.plusHours a 2)) (str (.plusMonths a 1)) (.toEpochSecond a ZoneOffset/UTC)
   (str (.toLocalDate b)) (str (.toLocalTime b)) (.getDayOfWeek b)])
(str LocalDateTime/MIN)
(str LocalDateTime/MAX)

;; ----- Instant
(str Instant/EPOCH)
(str (Instant/ofEpochSecond 1592228730 123456789))
(str (Instant/ofEpochMilli -1))
(str (Instant/ofEpochSecond -62135596800))
(str (Instant/ofEpochSecond 253402300800))
(str Instant/MIN)
(str Instant/MAX)
(str (Instant/parse "2020-06-15T13:45:30.123Z"))
(str (Instant/parse "2020-06-15T13:45:30+02:00"))
(ex-msg #(Instant/parse "2020-06-15T13:45:30"))
(str (.plus (Instant/ofEpochSecond 0) (Duration/ofDays 1)))
(str (.truncatedTo (Instant/ofEpochSecond 1592228730 123456789) ChronoUnit/HOURS))
(.until (Instant/ofEpochSecond 0) (Instant/ofEpochSecond 90061) ChronoUnit/MINUTES)
(str (.atZone (Instant/ofEpochSecond 1592228730) (ZoneId/of "America/New_York")))
(str (.atOffset (Instant/ofEpochSecond 1592228730) (ZoneOffset/of "-03:30")))
(.getLong (Instant/ofEpochSecond 5 7000000) ChronoField/MILLI_OF_SECOND)
(ex-msg #(.get (Instant/ofEpochSecond 5) ChronoField/YEAR))

;; ----- Duration, Period
(mapv str [(Duration/ofSeconds 0) (Duration/ofSeconds 59) (Duration/ofSeconds 3661)
           (Duration/ofMillis -1) (Duration/ofNanos 1) (Duration/ofDays 2) (Duration/ofMinutes -90)
           (Duration/ofSeconds -1 1) (Duration/ofSeconds 1 -1) (Duration/ofHours 100000)])
(mapv #(str (Duration/parse %)) ["PT20.345S" "PT15M" "PT10H" "P2D" "P2DT3H4M" "PT-6H3M" "-PT6H3M" "-PT-6H+3M" "PT0.000000001S" "pt1h"])
(ex-msg #(Duration/parse "P1Y"))
(let [d (Duration/ofSeconds 3723 456000000)]
  [(.toHours d) (.toMinutes d) (.toMillis d) (.toNanos d) (.toHoursPart d) (.toMinutesPart d)
   (.toSecondsPart d) (.toMillisPart d) (.getSeconds d) (.getNano d) (str (.negated d))
   (str (.multipliedBy d 3)) (str (.dividedBy d 7)) (.dividedBy d (Duration/ofSeconds 10))
   (str (.truncatedTo d ChronoUnit/MINUTES)) (str (.abs (.negated d)))])
(str (Duration/between (LocalDateTime/of 2020 1 1 0 0) (LocalDateTime/of 2020 3 1 12 30)))
(str (Duration/between (ZonedDateTime/of 2020 3 29 0 0 0 0 (ZoneId/of "Europe/Paris"))
                       (ZonedDateTime/of 2020 3 30 0 0 0 0 (ZoneId/of "Europe/Paris"))))
(ex-msg #(.toNanos (Duration/ofDays 200000)))
(mapv str [(Period/of 1 2 3) (Period/ofDays 0) (Period/ofWeeks 3) (Period/ofMonths -14)
           (.normalized (Period/ofMonths -14)) (.normalized (Period/of 1 -25 3))])
(mapv #(str (Period/parse %)) ["P2Y" "P3M" "P4W" "P5D" "P1Y2M3D" "P1Y2M3W4D" "P-1Y2M" "-P1Y2M"])
(ex-msg #(Period/parse "PT1H"))
(str (Period/between (LocalDate/of 2020 1 31) (LocalDate/of 2020 3 1)))
(str (Period/between (LocalDate/of 2020 3 1) (LocalDate/of 2020 1 31)))
(str (.plus (LocalDate/of 2020 1 31) (Period/of 0 1 1)))
(.toTotalMonths (Period/of 2 5 9))
(str (.addTo (Period/ofMonths 1) (LocalDateTime/of 2021 1 31 10 0)))

;; ----- Year, YearMonth, MonthDay, Month, DayOfWeek
[(.isLeap (Year/of 1900)) (.isLeap (Year/of 2000)) (.length (Year/of 2024)) (str (.atDay (Year/of 2024) 60))]
(mapv str [(YearMonth/of 2020 2) (.atEndOfMonth (YearMonth/of 2020 2)) (.plusMonths (YearMonth/of 2020 12) 1)
           (YearMonth/parse "2020-06") (MonthDay/of 2 29) (.atYear (MonthDay/of 2 29) 2021)
           (MonthDay/parse "--12-03") (Year/parse "-0042")])
(ex-msg #(MonthDay/of 2 30))
(.isValidYear (MonthDay/of 2 29) 2021)
(mapv #(vector (str %) (.getValue %) (.length % false) (.maxLength %) (str (.firstMonthOfQuarter %))) (Month/values))
(mapv #(vector (str %) (.getValue %) (str (.plus % 3))) (DayOfWeek/values))
(str (Month/from (LocalDate/of 2020 7 4)))
(ex-msg #(Month/of 13))

;; ----- zones and offsets
(mapv str [ZoneOffset/UTC ZoneOffset/MIN ZoneOffset/MAX (ZoneOffset/ofHours 5) (ZoneOffset/ofHoursMinutes -5 -30)
           (ZoneOffset/ofTotalSeconds 3723) (ZoneOffset/of "+5") (ZoneOffset/of "-0530") (ZoneOffset/of "+05:30:15")
           (ZoneOffset/of "Z")])
(identical? (ZoneOffset/ofHours 1) (ZoneOffset/of "+01:00"))
(ex-msg #(ZoneOffset/ofHours 19))
(ex-msg #(ZoneOffset/of "+5:30"))
(mapv #(let [z (ZoneId/of %)] [(str z) (str (class z)) (str (.normalized z))])
      ["Europe/Paris" "UTC" "Z" "GMT" "UT" "UTC+01:00" "GMT-05:30" "UT+3" "+02:00" "Etc/GMT+5" "Etc/UTC"
       "US/Eastern" "America/Argentina/Buenos_Aires" "Asia/Kolkata" "Asia/Calcutta"])
(ex-msg #(ZoneId/of "Mars/Olympus_Mons"))
(ex-msg #(ZoneId/of "Europe/Paris!"))
(ex-msg #(ZoneId/of "EST"))
(str (ZoneId/of "EST" ZoneId/SHORT_IDS))
(str (ZoneId/of "PST" ZoneId/SHORT_IDS))
(into (sorted-map) ZoneId/SHORT_IDS)
(count (ZoneId/getAvailableZoneIds))
(take 40 (sort (ZoneId/getAvailableZoneIds)))
(take-last 40 (sort (ZoneId/getAvailableZoneIds)))
(ZoneRulesProvider/getVersions "Europe/Paris")
(str (.getRules (ZoneId/of "Europe/Paris")))
(str (.getRules (ZoneOffset/ofHours 3)))
(let [r (.getRules (ZoneId/of "Europe/Paris"))
      i (Instant/parse "2020-07-01T00:00:00Z")]
  [(str (.getOffset r i)) (str (.getStandardOffset r i)) (str (.getDaylightSavings r i))
   (.isDaylightSavings r i) (str (.nextTransition r i)) (str (.previousTransition r i))
   (.isFixedOffset r) (count (.getTransitions r)) (mapv str (.getTransitionRules r))])
(mapv str (take 12 (.getTransitions (.getRules (ZoneId/of "America/New_York")))))
(mapv str (take-last 6 (.getTransitions (.getRules (ZoneId/of "America/New_York")))))
(mapv str (.getTransitionRules (.getRules (ZoneId/of "America/New_York"))))
(mapv str (.getTransitionRules (.getRules (ZoneId/of "Australia/Lord_Howe"))))
(mapv str (.getTransitionRules (.getRules (ZoneId/of "Europe/Dublin"))))
(mapv str (take-last 4 (.getTransitions (.getRules (ZoneId/of "Africa/Casablanca")))))
(let [r (.getRules (ZoneId/of "Europe/Paris"))]
  [(mapv str (.getValidOffsets r (LocalDateTime/of 2020 3 29 2 30)))
   (mapv str (.getValidOffsets r (LocalDateTime/of 2020 10 25 2 30)))
   (str (.getTransition r (LocalDateTime/of 2020 3 29 2 30)))
   (str (.getTransition r (LocalDateTime/of 2020 10 25 2 30)))
   (.isGap (.getTransition r (LocalDateTime/of 2020 3 29 2 30)))
   (str (.getDuration (.getTransition r (LocalDateTime/of 2020 10 25 2 30))))])
(mapv #(str (.getOffset (.getRules (ZoneId/of %)) (Instant/parse "1950-06-01T00:00:00Z")))
      ["Europe/Paris" "Europe/Moscow" "Asia/Kolkata" "America/Sao_Paulo" "Pacific/Kiritimati" "Asia/Kathmandu" "Australia/Eucla" "America/St_Johns"])
(mapv #(str (.getOffset (.getRules (ZoneId/of %)) (Instant/parse "2025-01-15T00:00:00Z")))
      ["Europe/Paris" "Europe/Moscow" "Asia/Kolkata" "America/Sao_Paulo" "Pacific/Kiritimati" "Asia/Kathmandu" "Australia/Eucla" "America/St_Johns" "Pacific/Chatham" "Asia/Tehran"])
(mapv #(str (.getOffset (.getRules (ZoneId/of %)) (Instant/parse "2040-07-15T00:00:00Z")))
      ["Europe/Paris" "America/New_York" "Australia/Sydney" "Pacific/Chatham" "America/Santiago" "Europe/Dublin" "Africa/Cairo"])
(mapv #(str (.getOffset (.getRules (ZoneId/of %)) (Instant/parse "1890-01-01T00:00:00Z")))
      ["Europe/Paris" "America/New_York" "Asia/Tokyo" "Europe/Amsterdam" "Africa/Monrovia"])

;; ----- ZonedDateTime, OffsetDateTime, OffsetTime
(str (ZonedDateTime/of 2020 6 15 13 45 30 0 (ZoneId/of "Europe/Paris")))
(str (ZonedDateTime/of 2020 3 29 2 30 0 0 (ZoneId/of "Europe/Paris")))
(str (ZonedDateTime/of 2020 10 25 2 30 0 0 (ZoneId/of "Europe/Paris")))
(str (.withLaterOffsetAtOverlap (ZonedDateTime/of 2020 10 25 2 30 0 0 (ZoneId/of "Europe/Paris"))))
(str (ZonedDateTime/ofStrict (LocalDateTime/of 2020 10 25 2 30) (ZoneOffset/ofHours 1) (ZoneId/of "Europe/Paris")))
(ex-msg #(ZonedDateTime/ofStrict (LocalDateTime/of 2020 3 29 2 30) (ZoneOffset/ofHours 1) (ZoneId/of "Europe/Paris")))
(let [z (ZonedDateTime/of 2020 3 28 12 0 0 0 (ZoneId/of "Europe/Paris"))]
  (mapv str [(.plusDays z 1) (.plusHours z 24) (.plus z (Duration/ofDays 1)) (.plusMonths z 7)
             (.withZoneSameInstant z (ZoneId/of "Asia/Tokyo")) (.withZoneSameLocal z (ZoneId/of "Asia/Tokyo"))
             (.toOffsetDateTime z) (.toInstant z) (.toLocalDateTime z) (.getOffset z)
             (.truncatedTo z ChronoUnit/DAYS) (.withFixedOffsetZone z)]))
(str (ZonedDateTime/parse "2020-06-15T13:45:30+02:00[Europe/Paris]"))
(str (ZonedDateTime/parse "2020-06-15T13:45:30Z"))
(str (ZonedDateTime/parse "2020-06-15T13:45:30-04:00[America/New_York]"))
(str (ZonedDateTime/parse "2020-06-15T13:45:30+05:00[Europe/Paris]"))
(ex-msg #(ZonedDateTime/parse "2020-06-15T13:45:30[Nowhere/Land]"))
(.until (ZonedDateTime/of 2020 3 28 12 0 0 0 (ZoneId/of "Europe/Paris"))
        (ZonedDateTime/of 2020 3 29 12 0 0 0 (ZoneId/of "Europe/Paris")) ChronoUnit/HOURS)
(str (ZonedDateTime/ofInstant (LocalDateTime/of 2020 1 1 0 0) (ZoneOffset/ofHours 9) (ZoneId/of "Asia/Seoul")))
(mapv str [(OffsetDateTime/of 2020 6 15 13 45 30 0 (ZoneOffset/ofHours -7))
           (OffsetDateTime/parse "2020-06-15T13:45:30.5+05:30")
           (.withOffsetSameInstant (OffsetDateTime/parse "2020-06-15T13:45:30Z") (ZoneOffset/ofHours 14))
           (.atZoneSameInstant (OffsetDateTime/parse "2020-06-15T13:45:30Z") (ZoneId/of "Asia/Kolkata"))
           (OffsetTime/of 13 45 0 0 (ZoneOffset/ofHours 2)) (OffsetTime/parse "10:15:30+01:00")])
(.compareTo (OffsetDateTime/parse "2020-06-15T13:45:30Z") (OffsetDateTime/parse "2020-06-15T15:45:30+02:00"))
(.isEqual (OffsetDateTime/parse "2020-06-15T13:45:30Z") (OffsetDateTime/parse "2020-06-15T15:45:30+02:00"))
(= (OffsetDateTime/parse "2020-06-15T13:45:30Z") (OffsetDateTime/parse "2020-06-15T15:45:30+02:00"))
(.toEpochSecond (ZonedDateTime/of 2020 6 15 13 45 30 0 (ZoneId/of "Europe/Paris")))
(.hashCode (ZonedDateTime/of 2020 6 15 13 45 30 0 (ZoneId/of "Europe/Paris")))
(str (Clock/fixed (Instant/parse "2020-06-15T13:45:30Z") (ZoneId/of "Europe/Paris")))
(let [c (Clock/fixed (Instant/parse "2020-06-15T13:45:30Z") (ZoneId/of "Europe/Paris"))]
  (mapv str [(LocalDate/now c) (LocalTime/now c) (ZonedDateTime/now c) (Instant/now c) (Year/now c)
             (Clock/offset c (Duration/ofHours 1)) (.instant (Clock/offset c (Duration/ofHours 1)))]))

;; ----- formatters: ISO
(let [z (ZonedDateTime/of 2020 6 15 13 45 30 123400000 (ZoneId/of "Europe/Paris"))]
  (mapv #(.format % z)
        [DateTimeFormatter/ISO_LOCAL_DATE DateTimeFormatter/ISO_OFFSET_DATE DateTimeFormatter/ISO_DATE
         DateTimeFormatter/ISO_LOCAL_TIME DateTimeFormatter/ISO_OFFSET_TIME DateTimeFormatter/ISO_TIME
         DateTimeFormatter/ISO_LOCAL_DATE_TIME DateTimeFormatter/ISO_OFFSET_DATE_TIME
         DateTimeFormatter/ISO_ZONED_DATE_TIME DateTimeFormatter/ISO_DATE_TIME DateTimeFormatter/ISO_ORDINAL_DATE
         DateTimeFormatter/ISO_WEEK_DATE DateTimeFormatter/ISO_INSTANT DateTimeFormatter/BASIC_ISO_DATE
         DateTimeFormatter/RFC_1123_DATE_TIME]))
(.format DateTimeFormatter/RFC_1123_DATE_TIME (OffsetDateTime/parse "2020-01-05T03:04:05-08:00"))
;; (a Parsed prints its fields in the order of a HashMap keyed by enums, by identity hashes: not printed)
(let [p (.parse DateTimeFormatter/RFC_1123_DATE_TIME "Tue, 3 Jun 2008 11:05:30 GMT")]
  [(.getLong p ChronoField/INSTANT_SECONDS) (.getLong p ChronoField/OFFSET_SECONDS) (str (.query p (TemporalQueries/localDate)))])
(str (.parse DateTimeFormatter/ISO_WEEK_DATE "2020-W53-5"))
(str (LocalDate/parse "2020-W53-5" DateTimeFormatter/ISO_WEEK_DATE))
(str (LocalDate/parse "20200615" DateTimeFormatter/BASIC_ISO_DATE))
(str (LocalDate/parse "2020-167" DateTimeFormatter/ISO_ORDINAL_DATE))
(str DateTimeFormatter/ISO_LOCAL_DATE)
(str DateTimeFormatter/ISO_ZONED_DATE_TIME)
(str DateTimeFormatter/RFC_1123_DATE_TIME)
(ex-msg #(LocalDate/parse "2020-06-15x"))
(ex-msg #(LocalTime/parse "13:4"))
(let [e (try (LocalDate/parse "2020-13-01") (catch DateTimeParseException e e))]
  [(.getMessage e) (.getParsedString e) (.getErrorIndex e) (some-> (.getCause e) .getMessage)])

;; ----- formatters: patterns (English)
(let [z (ZonedDateTime/of 2020 6 15 13 5 7 123456789 (ZoneId/of "America/New_York"))]
  (mapv #(vector % (.format (DateTimeFormatter/ofPattern % Locale/US) z))
        ["G GGGG GGGGG" "y yy yyy yyyy yyyyy" "u uu uuuu" "Y YY YYYY" "Q QQ QQQ QQQQ QQQQQ"
         "q qq qqq qqqq qqqqq" "M MM MMM MMMM MMMMM" "L LL LLL LLLL LLLLL" "w ww W" "d dd D DDD F"
         "E EE EEE EEEE EEEEE" "e ee eee eeee eeeee" "c ccc cccc ccccc" "a" "h hh K KK k kk H HH"
         "m mm s ss" "S SS SSS SSSSSS SSSSSSSSS" "n N A" "VV" "z zz zzz zzzz" "O OOOO"
         "X XX XXX XXXX XXXXX" "x xx xxx xxxx xxxxx" "Z ZZ ZZZ ZZZZ ZZZZZ" "v vvvv" "B BBBB BBBBB"
         "''yyyy'' 'at' h:mm a" "[yyyy][MM]" "g"]))
(let [t (LocalTime/of 0 30)]
  (mapv #(.format (DateTimeFormatter/ofPattern % Locale/US) t) ["h:mm a" "K:mm a" "k:mm" "H:mm" "B" "h B"]))
(mapv #(.format (DateTimeFormatter/ofPattern "h:mm B" Locale/US) (LocalTime/of % 0)) (range 24))
(mapv #(.format (DateTimeFormatter/ofPattern "h:mm BBBB" Locale/ENGLISH) (LocalTime/of % 30)) [0 6 12 13 18 21])
(let [z (ZonedDateTime/of 2020 1 15 13 5 7 0 (ZoneId/of "UTC"))]
  (mapv #(.format (DateTimeFormatter/ofPattern % Locale/US) z) ["z zzzz" "VV" "O" "X" "x" "Z" "v vvvv"]))
(let [z (ZonedDateTime/of 2020 1 15 13 5 7 0 (ZoneOffset/ofHoursMinutes 5 30))]
  (mapv #(.format (DateTimeFormatter/ofPattern % Locale/US) z) ["z zzzz" "VV" "O OOOO" "X" "x" "Z" "ZZZZ"]))
(ex-msg #(DateTimeFormatter/ofPattern "yyyy-MM-dd'T"))
(ex-msg #(DateTimeFormatter/ofPattern "ppp"))
(ex-msg #(DateTimeFormatter/ofPattern "{"))
(ex-msg #(.format (DateTimeFormatter/ofPattern "HH:mm") (LocalDate/of 2020 1 1)))
(ex-msg #(.format (DateTimeFormatter/ofPattern "yyyy z") (LocalDateTime/of 2020 1 1 0 0)))
(str (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS VV" Locale/US))
(str (LocalDate/parse "15 June 2020" (DateTimeFormatter/ofPattern "d MMMM yyyy" Locale/US)))
(str (LocalDate/parse "Mon, Jun 15, 2020" (DateTimeFormatter/ofPattern "EEE, MMM d, yyyy" Locale/US)))
(ex-msg #(LocalDate/parse "Tue, Jun 15, 2020" (DateTimeFormatter/ofPattern "EEE, MMM d, yyyy" Locale/US)))
(str (LocalDateTime/parse "2020-06-15 1:05 PM" (DateTimeFormatter/ofPattern "yyyy-MM-dd h:mm a" Locale/US)))
(str (ZonedDateTime/parse "2020-06-15 13:05 Europe/Paris" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm VV")))
(str (ZonedDateTime/parse "2020-06-15 13:05 +0530" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm Z")))
(str (ZonedDateTime/parse "2020-06-15 13:05 Pacific Daylight Time" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm zzzz" Locale/US)))
(str (ZonedDateTime/parse "2020-06-15 13:05 PDT" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm z" Locale/US)))
(str (ZonedDateTime/parse "2020-01-15 13:05 Central European Standard Time" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm zzzz" Locale/US)))
(str (ZonedDateTime/parse "2020-01-15 13:05 GMT+03:00" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm O" Locale/US)))
(str (LocalDate/parse "2020-2-30" (.withResolverStyle (DateTimeFormatter/ofPattern "uuuu-M-d") ResolverStyle/SMART)))
(ex-msg #(LocalDate/parse "2020-2-30" (.withResolverStyle (DateTimeFormatter/ofPattern "uuuu-M-d") ResolverStyle/STRICT)))
(str (LocalDate/parse "2020-14-35" (.withResolverStyle (DateTimeFormatter/ofPattern "uuuu-M-d") ResolverStyle/LENIENT)))
(let [[c m] (ex-msg #(LocalDate/parse "2020-2-28" (.withResolverStyle (DateTimeFormatter/ofPattern "yyyy-M-d") ResolverStyle/STRICT)))]
  [c (subs m 0 (.indexOf ^String m "{"))])
(str (.parseBest (DateTimeFormatter/ofPattern "uuuu-MM-dd[ HH:mm]") "2020-06-15"
                 (into-array java.time.temporal.TemporalQuery
                             [(reify java.time.temporal.TemporalQuery (queryFrom [_ t] (LocalDateTime/from t)))
                              (reify java.time.temporal.TemporalQuery (queryFrom [_ t] (LocalDate/from t)))])))
(let [p (.parseUnresolved (DateTimeFormatter/ofPattern "uuuu-MM-dd") "2020-06-99" (java.text.ParsePosition. 0))]
  (mapv #(.getLong p %) [ChronoField/YEAR ChronoField/MONTH_OF_YEAR ChronoField/DAY_OF_MONTH]))
(let [p (java.text.ParsePosition. 3)]
  [(str (.parse DateTimeFormatter/ISO_LOCAL_DATE "xx 2020-06-15 yy" p)) (.getIndex p) (.getErrorIndex p)])
(let [p (.parse (DateTimeFormatter/ofPattern "yyyy MM") "2020 06")]
  [(.isSupported p ChronoField/YEAR) (.getLong p ChronoField/YEAR) (.getLong p ChronoField/MONTH_OF_YEAR) (str (.query p (TemporalQueries/chronology)))])
(.format (.withDecimalStyle (DateTimeFormatter/ofPattern "yyyy-MM-dd") (.withZeroDigit DecimalStyle/STANDARD \٠)) (LocalDate/of 2020 6 15))
(str (DecimalStyle/ofDefaultLocale))
(str (DecimalStyle/of Locale/ROOT))
(.format (-> (DateTimeFormatterBuilder.)
             (.appendValue ChronoField/YEAR 4 10 SignStyle/EXCEEDS_PAD)
             (.appendLiteral \/)
             (.appendValueReduced ChronoField/YEAR 2 2 2000)
             (.appendLiteral " ")
             (.appendText ChronoField/MONTH_OF_YEAR TextStyle/SHORT_STANDALONE)
             (.appendLiteral " ")
             (.appendFraction ChronoField/NANO_OF_SECOND 0 9 true)
             (.appendLiteral " ")
             (.appendOffset "+HH:MM:ss" "Z")
             (.appendLiteral " ")
             (.appendLocalizedOffset TextStyle/FULL)
             (.appendLiteral " ")
             (.appendZoneText TextStyle/SHORT)
             (.appendLiteral " ")
             (.appendChronologyId)
             (.appendLiteral " ")
             (.appendDayPeriodText TextStyle/FULL)
             (.toFormatter Locale/US))
         (ZonedDateTime/of 2020 6 15 13 5 7 120000000 (ZoneId/of "Asia/Kathmandu")))
(.format (-> (DateTimeFormatterBuilder.)
             (.appendText ChronoField/MONTH_OF_YEAR {1 "JNY" 2 "FBY" 3 "MCH" 4 "APL" 5 "MAY" 6 "JUN"
                                                    7 "JLY" 8 "AGT" 9 "SPT" 10 "OCT" 11 "NOV" 12 "DEC"})
             (.appendLiteral " ")
             (.appendInstant 3)
             .toFormatter)
         (ZonedDateTime/of 2020 6 15 13 5 7 120000000 ZoneOffset/UTC))
(str (-> (DateTimeFormatterBuilder.) (.parseCaseInsensitive) (.appendPattern "MMM d yyyy") (.toFormatter Locale/US)))
(str (LocalDate/parse "jUN 15 2020" (-> (DateTimeFormatterBuilder.) (.parseCaseInsensitive) (.appendPattern "MMM d yyyy") (.toFormatter Locale/US))))

;; ----- formatters: localized styles and skeletons
(let [z (ZonedDateTime/of 2020 6 15 13 5 7 0 (ZoneId/of "America/Los_Angeles"))]
  (for [l [Locale/US Locale/ENGLISH Locale/ROOT] s (FormatStyle/values)]
    [(str l) (str s)
     (.format (.withLocale (DateTimeFormatter/ofLocalizedDate s) l) z)
     (.format (.withLocale (DateTimeFormatter/ofLocalizedTime s) l) z)
     (.format (.withLocale (DateTimeFormatter/ofLocalizedDateTime s) l) z)]))
(for [l [Locale/US Locale/ROOT] d (FormatStyle/values) t (FormatStyle/values)]
  (DateTimeFormatterBuilder/getLocalizedDateTimePattern d t IsoChronology/INSTANCE l))
(.format (.withLocale (DateTimeFormatter/ofLocalizedDateTime FormatStyle/MEDIUM FormatStyle/SHORT) Locale/US) (LocalDateTime/of 2020 6 15 13 5))
(ex-msg #(.format (DateTimeFormatter/ofLocalizedDateTime FormatStyle/FULL) (LocalDateTime/of 2020 6 15 13 5)))
(let [z (ZonedDateTime/of 2020 6 15 13 5 7 0 (ZoneId/of "Europe/Paris"))]
  (for [l [Locale/US Locale/ROOT] p ["yMMMd" "yMMMMEEEEd" "yMd" "MMMd" "yQQQ" "Hm" "hm" "jm" "Cm" "jms" "yMMMdjm" "MMMMd" "Ehm" "yMMM" "y" "hmv"]]
    [(str l) p (.format (DateTimeFormatter/ofLocalizedPattern p) (.withLocale z l))
     (DateTimeFormatterBuilder/getLocalizedDateTimePattern p IsoChronology/INSTANCE l)]))
(ex-msg #(DateTimeFormatter/ofLocalizedPattern "yyyyyyMMMd hh"))
(str (LocalDate/parse "Jun 15, 2020" (.withLocale (DateTimeFormatter/ofLocalizedDate FormatStyle/MEDIUM) Locale/US)))
(str (LocalDate/parse "6/15/20" (.withLocale (DateTimeFormatter/ofLocalizedDate FormatStyle/SHORT) Locale/US)))

;; ----- text: months, days, eras, AM/PM, quarters, fields, in the root and English locales
(for [l [Locale/US Locale/ROOT Locale/ENGLISH] s (TextStyle/values)]
  [(str l) (str s) (mapv #(.getDisplayName % s l) (Month/values)) (mapv #(.getDisplayName % s l) (DayOfWeek/values))])
(for [l [Locale/US Locale/ROOT] s (TextStyle/values)]
  [(str l) (str s) (mapv #(.getDisplayName % s l) (IsoEra/values)) (.getDisplayName IsoChronology/INSTANCE s l)])
(for [l [Locale/US Locale/ROOT] f (ChronoField/values)] (.getDisplayName f l))
(for [l [Locale/US Locale/ROOT] f [IsoFields/DAY_OF_QUARTER IsoFields/QUARTER_OF_YEAR IsoFields/WEEK_OF_WEEK_BASED_YEAR IsoFields/WEEK_BASED_YEAR]]
  [(.getDisplayName f l) (str f)])
(for [l [Locale/US Locale/ROOT Locale/UK Locale/FRANCE]]
  (let [w (WeekFields/of l)]
    [(str w) (.getDisplayName (.weekOfYear w) l) (.getDisplayName (.dayOfWeek w) l)]))
(let [q (DateTimeFormatter/ofPattern "QQQ QQQQ QQQQQ qqq qqqq qqqqq" Locale/ROOT)]
  (mapv #(.format q (LocalDate/of 2020 % 1)) [1 4 7 10]))
(.format (DateTimeFormatter/ofPattern "MMMM EEEE a G" Locale/FRANCE) (LocalDateTime/of 2020 6 15 13 5))
(.format (DateTimeFormatter/ofPattern "MMMM EEEE a G" Locale/UK) (LocalDateTime/of 2020 6 15 13 5))

;; ----- temporal fields and units
(mapv #(vector (str %) (str (.range %)) (.isDateBased %) (.isTimeBased %) (str (.getBaseUnit %)) (str (.getRangeUnit %))) (ChronoField/values))
(mapv #(vector (str %) (str (.getDuration %)) (.isDurationEstimated %)) (ChronoUnit/values))
(let [d (LocalDate/of 2020 12 31)]
  (mapv #(.getLong d %) [IsoFields/DAY_OF_QUARTER IsoFields/QUARTER_OF_YEAR IsoFields/WEEK_OF_WEEK_BASED_YEAR
                         IsoFields/WEEK_BASED_YEAR JulianFields/JULIAN_DAY JulianFields/MODIFIED_JULIAN_DAY
                         JulianFields/RATA_DIE ChronoField/ALIGNED_DAY_OF_WEEK_IN_YEAR ChronoField/PROLEPTIC_MONTH
                         ChronoField/EPOCH_DAY ChronoField/YEAR_OF_ERA ChronoField/ERA]))
(mapv #(str (.with (LocalDate/of 2021 1 3) IsoFields/WEEK_OF_WEEK_BASED_YEAR %)) [1 10 53])
(.between IsoFields/QUARTER_YEARS (LocalDate/of 2020 1 15) (LocalDate/of 2021 7 14))
(str (.plus (LocalDate/of 2020 12 28) 1 IsoFields/WEEK_BASED_YEARS))
(for [w [WeekFields/ISO WeekFields/SUNDAY_START (WeekFields/of DayOfWeek/SATURDAY 7)]
      d [(LocalDate/of 2020 1 1) (LocalDate/of 2020 12 31) (LocalDate/of 2021 1 3)]]
  [(str w) (str d) (.get d (.dayOfWeek w)) (.get d (.weekOfMonth w)) (.get d (.weekOfYear w))
   (.get d (.weekOfWeekBasedYear w)) (.get d (.weekBasedYear w))])
(str (LocalDate/parse "2020-W01-1" (DateTimeFormatter/ofPattern "YYYY-'W'ww-e" Locale/US)))
(str (.query (ZonedDateTime/of 2020 6 15 13 5 7 0 (ZoneId/of "Europe/Paris")) (TemporalQueries/zone)))
(str (.query (LocalDate/of 2020 6 15) (TemporalQueries/precision)))
(str (.query (LocalTime/of 1 2) (TemporalQueries/precision)))
(str (ValueRange/of 1 28 31))
(ex-msg #(.checkValidValue (ValueRange/of 1 12) 13 ChronoField/MONTH_OF_YEAR))

;; ----- the ISO chronology
(str (Chronology/of "ISO"))
(ex-msg #(Chronology/of "Martian"))
(str (Chronology/ofLocale Locale/US))
(str (.date IsoChronology/INSTANCE 2020 6 15))
(str (.dateYearDay IsoChronology/INSTANCE 2020 100))
(str (.date IsoChronology/INSTANCE IsoEra/BCE 5 3 1))
(.isLeapYear IsoChronology/INSTANCE -4)
(str (.period IsoChronology/INSTANCE 1 2 3))
(str (.resolveDate IsoChronology/INSTANCE (java.util.HashMap. {ChronoField/YEAR 2020 ChronoField/MONTH_OF_YEAR 2 ChronoField/DAY_OF_MONTH 31}) ResolverStyle/SMART))
(str (.zonedDateTime IsoChronology/INSTANCE (Instant/parse "2020-06-15T13:45:30Z") (ZoneId/of "Asia/Tokyo")))
(.epochSecond IsoChronology/INSTANCE 2020 6 15 13 45 30 ZoneOffset/UTC)

;; ----- java.util.TimeZone over the time-zone database
(let [tz (TimeZone/getTimeZone "Europe/Paris")]
  [(.getID tz) (.getRawOffset tz) (.getDSTSavings tz) (.useDaylightTime tz) (.observesDaylightTime tz)
   (.getOffset tz 1592228730000) (.getOffset tz 1579096800000) (.inDaylightTime tz (Date. 1592228730000))
   (.getOffset tz 1 2020 5 15 2 0) (str (.toZoneId tz)) (.hasSameRules tz (TimeZone/getTimeZone "Europe/Berlin"))
   (.hasSameRules tz (TimeZone/getTimeZone "Europe/London"))])
(mapv #(let [tz (TimeZone/getTimeZone %)] [(.getID tz) (.getRawOffset tz) (.getDSTSavings tz) (.useDaylightTime tz)])
      ["America/New_York" "US/Pacific" "Asia/Kolkata" "Australia/Lord_Howe" "Europe/Dublin" "Africa/Casablanca"
       "GMT" "UTC" "EST" "MST" "HST" "PST" "GMT+5" "GMT-0130" "Etc/GMT-14" "Nowhere/Land" "America/Sao_Paulo"])
(str (TimeZone/getTimeZone "Europe/Paris"))
(str (TimeZone/getTimeZone "GMT+05:30"))
(str (TimeZone/getTimeZone "America/Phoenix"))
(count (TimeZone/getAvailableIDs))
(vec (TimeZone/getAvailableIDs 19800000))
(vec (take 30 (TimeZone/getAvailableIDs)))
(mapv #(.getID (TimeZone/getTimeZone (ZoneId/of %))) ["Europe/Paris" "Z" "+05:30" "UTC+3" "GMT"])
(str (.toZoneId (TimeZone/getTimeZone "PST")))
(str (.toZoneId (TimeZone/getTimeZone "EST")))
(let [s (SimpleTimeZone. 3600000 "Custom" Calendar/MARCH -1 Calendar/SUNDAY 3600000
                         Calendar/OCTOBER -1 Calendar/SUNDAY 3600000)]
  [(.getOffset s 1592228730000) (.getOffset s 1579096800000) (.useDaylightTime s) (str s)])
(for [id ["Europe/Paris" "America/New_York" "Asia/Kolkata" "UTC" "GMT" "Australia/Lord_Howe" "Asia/Calcutta" "US/Eastern"
          "EST" "Etc/GMT+5" "GMT+05:30" "Africa/Casablanca" "Antarctica/Troll" "Europe/Dublin" "Pacific/Apia"]
      l [Locale/US Locale/ROOT]]
  (let [tz (TimeZone/getTimeZone id)]
    [id (str l) (.getDisplayName tz false TimeZone/LONG l) (.getDisplayName tz false TimeZone/SHORT l)
     (.getDisplayName tz true TimeZone/LONG l) (.getDisplayName tz true TimeZone/SHORT l)]))
(for [id ["Europe/Paris" "America/New_York" "Asia/Kolkata" "UTC" "Australia/Lord_Howe" "Asia/Tokyo" "America/Argentina/Buenos_Aires"]
      s [TextStyle/FULL TextStyle/SHORT TextStyle/NARROW]
      l [Locale/US Locale/ROOT]]
  (.getDisplayName (ZoneId/of id) s l))

;; ----- Calendar, Date and java.time
(let [c (GregorianCalendar. (TimeZone/getTimeZone "Europe/Paris"))]
  (.setTimeInMillis c 1585443600000)
  [(.get c Calendar/HOUR_OF_DAY) (.get c Calendar/ZONE_OFFSET) (.get c Calendar/DST_OFFSET)
   (do (.add c Calendar/HOUR_OF_DAY 1) (.get c Calendar/HOUR_OF_DAY)) (.getTimeInMillis c)
   (str (.toZonedDateTime c)) (str (.toInstant c))])
(let [c (GregorianCalendar. (TimeZone/getTimeZone "America/New_York"))]
  (.clear c)
  (.set c 2020 Calendar/MARCH 8 2 30)
  [(.getTimeInMillis c) (.get c Calendar/HOUR_OF_DAY) (.get c Calendar/DST_OFFSET)])
(let [c (GregorianCalendar. (TimeZone/getTimeZone "America/New_York"))]
  (.clear c)
  (.set c 2020 Calendar/NOVEMBER 1 1 30)
  [(.getTimeInMillis c) (.get c Calendar/DST_OFFSET)])
(let [c (GregorianCalendar. (TimeZone/getTimeZone "Europe/Moscow"))]
  (.setTimeInMillis c 1325376000000)
  [(.get c Calendar/ZONE_OFFSET) (.get c Calendar/DST_OFFSET) (.get c Calendar/HOUR_OF_DAY)])
(let [c (GregorianCalendar/from (ZonedDateTime/of 2020 6 15 13 45 30 500000000 (ZoneId/of "Asia/Tokyo")))]
  [(.getTimeInMillis c) (.getID (.getTimeZone c)) (.getFirstDayOfWeek c) (.getMinimalDaysInFirstWeek c)
   (.get c Calendar/HOUR_OF_DAY) (.get c Calendar/MILLISECOND)])
(str (.toInstant (Date. 1592228730123)))
(.getTime (Date/from (Instant/parse "2020-06-15T13:45:30.123456Z")))
(ex-msg #(Date/from Instant/MAX))
(str (.toInstant #inst "2020-06-15T13:45:30.123+02:00"))
(inst-ms (Instant/parse "2020-06-15T13:45:30.123Z"))
(inst? (Instant/parse "2020-06-15T13:45:30.123Z"))
(str (.toLocalDateTime (java.sql.Timestamp. 1592228730123)))
(str (.toInstant (java.sql.Timestamp/valueOf (LocalDateTime/of 2020 6 15 13 45 30 123456789))))
(str (.toLocalDate (java.sql.Date/valueOf (LocalDate/of 2020 6 15))))
(str (.toChronoUnit java.util.concurrent.TimeUnit/HOURS))
(str (java.util.concurrent.TimeUnit/of ChronoUnit/MILLIS))
(.convert java.util.concurrent.TimeUnit/SECONDS (Duration/ofMinutes 3))
(format "%tY-%tm-%td %tH:%tM:%tS %tz" (ZonedDateTime/of 2020 6 15 13 5 7 0 (ZoneId/of "Asia/Kolkata"))
        (LocalDate/of 2020 6 15) (LocalDate/of 2020 6 15) (LocalTime/of 13 5 7) (LocalTime/of 13 5 7)
        (LocalTime/of 13 5 7) (OffsetDateTime/parse "2020-06-15T13:45:30+05:30"))
(format "%tF %<tT %<tN %<tL %<tj %<ts %<tQ" (ZonedDateTime/of 2020 6 15 13 5 7 123456789 (ZoneId/of "Asia/Kolkata")))
