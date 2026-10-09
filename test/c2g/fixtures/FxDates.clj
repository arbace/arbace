;; c2g's fixtures (see FxCode.clj): the date classes (doc/go/JRT-NOTES.md, phase 2B "Dates").
;; jrt's own GregorianCalendar, Calendar, TimeZone and ZoneInfo (overlay/jdk) against the JDK's,
;; and the translated java.sql.Timestamp and java.time.Instant (with its toString variant over
;; jrt's TimeText): fields from times and times from fields in fixed-offset zones around the
;; Julian-Gregorian cutover and in BC years, lenient arithmetic, add and roll, the actual
;; maxima, String.format's %t over a Calendar, the zone IDs. Every zone is named: the JVM's
;; default zone is the machine's. Long outputs are given as their length and String.hashCode
;; with a sample.

(in-ns 'c2g.fixtures)

(import '(java.util Calendar GregorianCalendar TimeZone Locale Date)
        '(java.sql Timestamp)
        '(java.time Instant))

(do

(defclass ^:public FxDates
  (field ^:static ^long seed 42)

  (method ^:static nextLong ^long []
    (set! seed (unchecked-add (unchecked-multiply seed 6364136223846793005) 1442695040888963407))
    seed)

  ;; a time in about +-3000 years around 1970, in milliseconds
  (method ^:static nextTime ^long []
    (rem (bit-shift-right (FxDates/nextLong) 8) 95000000000000))

  (method ^:static summary ^String [^StringBuilder sb]
    (let [s (.toString sb)]
      (java-str (.length s) " " (.hashCode s) " " (.substring s (unchecked-int 0) (Math/min (.length s) (unchecked-int 300))))))

  (method ^:static zones ^String/1 []
    (new String/1 ["GMT" "GMT+05:00" "GMT-09:30" "GMT+14:00"]))

  (method ^:static fields ^String [^Calendar c]
    (let [sb (StringBuilder.)]
      (loop [^int f 0]
        (when (< f Calendar/FIELD_COUNT)
          (.append sb (.get c f)) (.append sb " ")
          (recur (unchecked-inc-int f))))
      (.toString sb)))

  (method ^:public ^:static tZoneIds ^String []
    (let [sb (StringBuilder.)
          ids (new String/1 ["GMT" "UTC" "GMT+05:00" "GMT-09:30" "GMT+5" "GMT-0930" "GMT+00:00"
                             "GMT-00:00" "GMT+1:30" "Etc/GMT+5" "Etc/GMT-3" "bogus" "GMT+24" "GMT+05:7"
                             "Zulu" "Etc/UTC" "GMT+0" "Greenwich"])]
      (for-each [^String id ids]
        (let [tz (TimeZone/getTimeZone id)]
          (.append sb (java-str id "=" (.getID tz) "," (.getRawOffset tz) "," (.getOffset tz 0) ","
                                (.getDisplayName tz false TimeZone/SHORT Locale/US) ","
                                (.getDisplayName tz false TimeZone/LONG Locale/US) "," (.useDaylightTime tz) ","
                                (.hashCode tz) "," (.getName (.getClass tz)) ";"))))
      (.toString sb)))

  (method ^:public ^:static tFieldsOfTimes ^String []
    (set! seed 42)
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i 600)
          (let [t (if (< i 8)
                    (aget (new long/1 [0 -1 -12219292800000 -12219292800001 -12220156800000 -62135596800000
                                       -62198755200000 1592224496789]) i)
                    (FxDates/nextTime))
                z (aget (FxDates/zones) (unchecked-remainder-int i 4))
                c (GregorianCalendar. (TimeZone/getTimeZone z))]
            (.setTimeInMillis c t)
            (.append sb (java-str z " " t ": " (FxDates/fields c) "| "
                                  (.getActualMaximum c Calendar/DAY_OF_MONTH) " " (.getActualMaximum c Calendar/DAY_OF_YEAR) " "
                                  (.getActualMaximum c Calendar/WEEK_OF_YEAR) " " (.getActualMaximum c Calendar/WEEK_OF_MONTH) " "
                                  (.getActualMaximum c Calendar/DAY_OF_WEEK_IN_MONTH) " "
                                  (String/format "%1$tFT%1$tT.%1$tL%1$tz" (new Object/1 [c])) "\n")))
          (recur (unchecked-inc-int i))))
      (FxDates/summary sb)))

  (method ^:public ^:static tTimesOfFields ^String []
    (set! seed 7)
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i 600)
          (let [y (unchecked-int (rem (Math/abs (FxDates/nextLong)) 4000))
                m (unchecked-subtract-int (unchecked-int (rem (Math/abs (FxDates/nextLong)) 30)) 8)
                d (unchecked-subtract-int (unchecked-int (rem (Math/abs (FxDates/nextLong)) 70)) 20)
                h (unchecked-subtract-int (unchecked-int (rem (Math/abs (FxDates/nextLong)) 40)) 8)
                c (GregorianCalendar. y m d h (unchecked-int 61) (unchecked-int -5))]
            (.setTimeZone c (TimeZone/getTimeZone (aget (FxDates/zones) (unchecked-remainder-int i 4))))
            (.set c Calendar/MILLISECOND (unchecked-int 123))
            (.append sb (java-str (.getTimeInMillis c) " " (.get c Calendar/YEAR) " " (.get c Calendar/DAY_OF_YEAR) ";"))
            (let [e (GregorianCalendar. (TimeZone/getTimeZone "GMT"))]
              (.clear e)
              (.set e Calendar/YEAR y)
              (.set e Calendar/DAY_OF_YEAR d)
              (.set e Calendar/HOUR h)
              (.set e Calendar/AM_PM (unchecked-remainder-int i 2))
              (.append sb (java-str (.getTimeInMillis e) "\n"))))
          (recur (unchecked-inc-int i))))
      (FxDates/summary sb)))

  (method ^:public ^:static tAddRoll ^String []
    (set! seed 99)
    (let [sb (StringBuilder.)
          fs (new int/1 [Calendar/YEAR Calendar/MONTH Calendar/DAY_OF_MONTH Calendar/HOUR_OF_DAY
                         Calendar/MINUTE Calendar/DAY_OF_YEAR Calendar/WEEK_OF_YEAR Calendar/SECOND])]
      (loop [^int i 0]
        (when (< i 300)
          (let [c (GregorianCalendar. (TimeZone/getTimeZone (aget (FxDates/zones) (unchecked-remainder-int i 4))))]
            (.setTimeInMillis c (FxDates/nextTime))
            (for-each [^int f fs]
              (let [a (cast GregorianCalendar (.clone c))
                    r (cast GregorianCalendar (.clone c))]
                (.add a f (unchecked-int 13))
                (.roll r f (unchecked-int -5))
                (.append sb (java-str (.getTimeInMillis a) " " (.getTimeInMillis r) " "))))
            (.append sb "\n"))
          (recur (unchecked-inc-int i))))
      (FxDates/summary sb)))

  (method ^:static nextInt ^int [^int n]
    (unchecked-int (rem (Math/abs (bit-shift-right (FxDates/nextLong) 16)) n)))

  ;; week-based fields: setting them resolves the date (Calendar's field resolution)
  (method ^:public ^:static tWeekFields ^String []
    (set! seed 11)
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i 600)
          (let [c (GregorianCalendar. (TimeZone/getTimeZone (aget (FxDates/zones) (unchecked-remainder-int i 4))))
                k (unchecked-remainder-int i 3)]
            (.setFirstDayOfWeek c (unchecked-inc-int (FxDates/nextInt 7)))
            (.setMinimalDaysInFirstWeek c (unchecked-inc-int (FxDates/nextInt 7)))
            (.clear c)
            (.set c Calendar/YEAR (FxDates/nextInt 3000))
            (cond
              (== k 0) (.set c Calendar/WEEK_OF_YEAR (unchecked-subtract-int (FxDates/nextInt 60) 3))
              (== k 1) (do (.set c Calendar/MONTH (FxDates/nextInt 12))
                           (.set c Calendar/WEEK_OF_MONTH (FxDates/nextInt 7)))
              :else (do (.set c Calendar/MONTH (FxDates/nextInt 12))
                        (.set c Calendar/DAY_OF_WEEK_IN_MONTH (unchecked-subtract-int (FxDates/nextInt 11) 5))))
            (.set c Calendar/DAY_OF_WEEK (unchecked-inc-int (FxDates/nextInt 7)))
            (.append sb (java-str (.getTimeInMillis c) " " (.getWeekYear c) " " (.get c Calendar/WEEK_OF_YEAR) " "
                                  (.get c Calendar/WEEK_OF_MONTH) "\n")))
          (recur (unchecked-inc-int i))))
      (FxDates/summary sb)))

  (method ^:public ^:static tCalendarMisc ^String []
    (let [c (GregorianCalendar. (TimeZone/getTimeZone "GMT+05:00"))
          sb (StringBuilder.)]
      (.setTimeInMillis c 1592224496789)
      (.append sb (java-str (.toString c) ";"))
      (let [d (cast GregorianCalendar (.clone c))]
        (.append sb (java-str (.equals c d) " " (== (.hashCode c) (.hashCode d)) " " (.compareTo c d) " "))
        (.add d Calendar/MILLISECOND (unchecked-int 1))
        (.append sb (java-str (.before c d) " " (.after c d) " " (.equals c d) ";")))
      (let [nl (GregorianCalendar. (TimeZone/getTimeZone "GMT"))]
        (.setLenient nl false)
        (.clear nl)
        (.set nl (unchecked-int 2020) (unchecked-int 1) (unchecked-int 30))
        (try (.append sb (.getTimeInMillis nl)) (catch IllegalArgumentException e (.append sb (java-str "IAE " (.getMessage e) ";")))))
      (.setGregorianChange c (Date. Long/MIN_VALUE))
      (.append sb (java-str (.get c Calendar/YEAR) " " (.isLeapYear c (unchecked-int 1500)) " " (.getTimeInMillis c) ";"))
      (.setTimeInMillis c -50000000000000)
      (.append sb (java-str (FxDates/fields c) ";" (.getTime (.getGregorianChange c)) " " (.getCalendarType c)))
      (.toString sb)))

  ;; Timestamp's toString and valueOf use Date's deprecated fields in the default zone: GMT,
  ;; as jrt's Date always is (restored after)
  (method ^:public ^:static tTimestamp ^String []
    (let [old (TimeZone/getDefault)]
      (TimeZone/setDefault (TimeZone/getTimeZone "GMT"))
      (try (FxDates/timestamps) (finally (TimeZone/setDefault old)))))

  (method ^:static timestamps ^String []
    (let [sb (StringBuilder.)
          a (Timestamp. 1592224496789)
          b (Timestamp/valueOf "2020-06-15 12:34:56.123456789")
          n (Timestamp. -1)]
      (.append sb (java-str a " " b " " n " " (.getTime n) " " (.getNanos n) " " (.getNanos b) " " (.getTime b) ";"))
      (.setNanos a (unchecked-int 5))
      (.append sb (java-str a " " (.getTime a) " " (.equals a (Timestamp. 1592224496000)) " " (.compareTo a b) " "
                            (.compareTo b (Date. 1592224496123)) " " (.hashCode b) " " (.before a b) " " (.toInstant b) ";"))
      (.append sb (java-str (Timestamp/from (Instant/ofEpochSecond -5 7)) " " (.equals (Date. 1592224496123) b) ";"))
      (try (Timestamp/valueOf "2020-06-15") (catch IllegalArgumentException e (.append sb (java-str (.getMessage e) ";"))))
      (try (.setNanos a (unchecked-int -1)) (catch IllegalArgumentException e (.append sb (java-str (.getMessage e) ";"))))
      (.toString sb)))

  (method ^:public ^:static tInstant ^String []
    (let [sb (StringBuilder.)
          secs (new long/1 [0 -1 1592224496 -62135596801 -62167219200 -62167219201 253402300799 253402300800
                            -377705116800 -377705116801 -31557014167219200 31556889864403199 -315569520001])
          nanos (new int/1 [0 1 1000 1000000 123456789 999999999])]
      (loop [^int i 0]
        (when (< i (alength secs))
          (.append sb (java-str (Instant/ofEpochSecond (aget secs i) (aget nanos (unchecked-remainder-int i 6))) " "))
          (recur (unchecked-inc-int i))))
      (let [x (Instant/ofEpochMilli -1)]
        (.append sb (java-str x " " (.toEpochMilli x) " " (.getNano x) " " (.plusMillis x 1001) " "
                              (.compareTo x Instant/EPOCH) " " (.equals x (Instant/ofEpochSecond -1 999000000)) " "
                              (.hashCode x) " " Instant/MIN " " Instant/MAX)))
      (try (.toEpochMilli Instant/MAX) (catch ArithmeticException e (.append sb (java-str " " (.getMessage e)))))
      (try (Instant/ofEpochSecond Long/MAX_VALUE) (catch java.time.DateTimeException e (.append sb (java-str " " (.getMessage e)))))
      (.toString sb))))

)
