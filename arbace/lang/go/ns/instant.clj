;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


;; The Go build's arbace/instant.clj (doc/go/EVAL-NOTES.md, "Namespace variants";
;; JAVA-SURFACE.md decision 7): java.util.Date only, over its milliseconds, in UTC, with the
;; calendar arithmetic written out (H. Hinnant's civil-from-days and days-from-civil), where
;; arbace/instant.clj formats with SimpleDateFormat in a proxy'd ThreadLocal and constructs
;; with GregorianCalendar, which the Go build has not. java.util.Calendar and
;; java.sql.Timestamp instants are not available (read-instant-calendar and
;; read-instant-timestamp throw). The parsing and validation (up to "print integration") are
;; arbace/instant.clj's, unchanged (as of af29cc6).

(ns arbace.instant
  (:import [java.util Date]))

(set! *warn-on-reflection* true)

;;; ------------------------------------------------------------------------
;;; convenience macros

(defmacro ^:private fail
  [msg]
  `(throw (RuntimeException. ~msg)))

(defmacro ^:private verify
  ([test msg] `(when-not ~test (fail ~msg)))
  ([test] `(verify ~test ~(str "failed: " (pr-str test)))))

(defn- divisible?
  [num div]
  (zero? (mod num div)))

(defn- indivisible?
  [num div]
  (not (divisible? num div)))

;;; ------------------------------------------------------------------------
;;; parser implementation

(defn- parse-int [^String s]
  (Long/parseLong s))

(defn- zero-fill-right [^String s width]
  (cond (= width (count s)) s
        (< width (count s)) (.substring s 0 width)
        :else (loop [b (StringBuilder. s)]
                (if (< (.length b) width)
                  (recur (.append b \0))
                  (.toString b)))))

(def ^:private timestamp
  #"(\d\d\d\d)(?:-(\d\d)(?:-(\d\d)(?:[T](\d\d)(?::(\d\d)(?::(\d\d)(?:[.](\d+))?)?)?)?)?)?(?:[Z]|([-+])(\d\d):(\d\d))?")

(defn parse-timestamp
     "Parse a string containing an RFC3339-like like timestamp.

The function new-instant is called with the following arguments.

                min  max           default
                ---  ------------  -------
  years          0           9999      N/A (s must provide years)
  months         1             12        1
  days           1             31        1 (actual max days depends
  hours          0             23        0  on month and year)
  minutes        0             59        0
  seconds        0             60        0 (though 60 is only valid
  nanoseconds    0      999999999        0  when minutes is 59)
  offset-sign   -1              1        0
  offset-hours   0             23        0
  offset-minutes 0             59        0

These are all integers and will be non-nil. (The listed defaults
will be passed if the corresponding field is not present in s.)

Grammar (of s):

  date-fullyear   = 4DIGIT
  date-month      = 2DIGIT  ; 01-12
  date-mday       = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31 based on
                            ; month/year
  time-hour       = 2DIGIT  ; 00-23
  time-minute     = 2DIGIT  ; 00-59
  time-second     = 2DIGIT  ; 00-58, 00-59, 00-60 based on leap second
                            ; rules
  time-secfrac    = '.' 1*DIGIT
  time-numoffset  = ('+' / '-') time-hour ':' time-minute
  time-offset     = 'Z' / time-numoffset

  time-part       = time-hour [ ':' time-minute [ ':' time-second
                    [time-secfrac] [time-offset] ] ]

  timestamp       = date-year [ '-' date-month [ '-' date-mday
                    [ 'T' time-part ] ] ]

Unlike RFC3339:

  - we only parse the timestamp format
  - timestamp can elide trailing components
  - time-offset is optional (defaults to +00:00)

Though time-offset is syntactically optional, a missing time-offset
will be treated as if the time-offset zero (+00:00) had been
specified.
"
    [new-instant ^CharSequence cs]
         (if-let [[_ years months days hours minutes seconds fraction
                   offset-sign offset-hours offset-minutes]
                  (re-matches timestamp cs)]
           (new-instant
            (parse-int years)
            (if-not months   1 (parse-int months))
            (if-not days     1 (parse-int days))
            (if-not hours    0 (parse-int hours))
            (if-not minutes  0 (parse-int minutes))
            (if-not seconds  0 (parse-int seconds))
            (if-not fraction 0 (parse-int (zero-fill-right fraction 9)))
            (cond (= "-" offset-sign) -1
                  (= "+" offset-sign)  1
                  :else                0)
            (if-not offset-hours   0 (parse-int offset-hours))
            (if-not offset-minutes 0 (parse-int offset-minutes)))
           (fail (str "Unrecognized date/time syntax: " cs))))

;;; ------------------------------------------------------------------------
;;; Verification of Extra-Grammatical Restrictions from RFC3339

(defn- leap-year?
  [year]
  (and (divisible? year 4)
       (or (indivisible? year 100)
           (divisible? year 400))))

(def ^:private days-in-month
     (let [dim-norm [nil 31 28 31 30 31 30 31 31 30 31 30 31]
           dim-leap [nil 31 29 31 30 31 30 31 31 30 31 30 31]]
       (fn [month leap-year?]
         ((if leap-year? dim-leap dim-norm) month))))

(defn validated
  "Return a function which constructs an instant by calling constructor
after first validating that those arguments are in range and otherwise
plausible. The resulting function will throw an exception if called
with invalid arguments."
  [new-instance]
  (fn [years months days hours minutes seconds nanoseconds
       offset-sign offset-hours offset-minutes]
    (verify (<= 1 months 12))
    (verify (<= 1 days (days-in-month months (leap-year? years))))
    (verify (<= 0 hours 23))
    (verify (<= 0 minutes 59))
    (verify (<= 0 seconds (if (= minutes 59) 60 59)))
    (verify (<= 0 nanoseconds 999999999))
    (verify (<= -1 offset-sign 1))
    (verify (<= 0 offset-hours 23))
    (verify (<= 0 offset-minutes 59))
    (new-instance years months days hours minutes seconds nanoseconds
                  offset-sign offset-hours offset-minutes)))

;;; ------------------------------------------------------------------------
;;; print integration
;;; ------------------------------------------------------------------------
;;; print integration (the Go build: UTC arithmetic on the milliseconds)

(defn- days->civil
  "[year month day] of a count of days since 1970-01-01."
  [z]
  (let [z (+ z 719468)
        era (quot (if (>= z 0) z (- z 146096)) 146097)
        doe (- z (* era 146097))
        yoe (quot (- doe (quot doe 1460) (- (quot doe 36524)) (quot doe 146096)) 365)
        y (+ yoe (* era 400))
        doy (- doe (- (+ (* 365 yoe) (quot yoe 4)) (quot yoe 100)))
        mp (quot (+ (* 5 doy) 2) 153)
        d (inc (- doy (quot (+ (* 153 mp) 2) 5)))
        m (if (< mp 10) (+ mp 3) (- mp 9))]
    [(if (<= m 2) (inc y) y) m d]))

(defn- civil->days
  "The count of days since 1970-01-01 of a year, month, day."
  [y m d]
  (let [y (if (<= m 2) (dec y) y)
        era (quot (if (>= y 0) y (- y 399)) 400)
        yoe (- y (* era 400))
        doy (+ (quot (+ (* 153 (if (> m 2) (- m 3) (+ m 9))) 2) 5) (dec d))
        doe (- (+ (* yoe 365) (quot yoe 4) doy) (quot yoe 100))]
    (+ (* era 146097) doe -719468)))

(defn- print-date
  "Print a java.util.Date as RFC3339 timestamp, always in UTC."
  [^java.util.Date d, ^java.io.Writer w]
  (let [ms (.getTime d)
        days (Math/floorDiv ms 86400000)
        msd (Math/floorMod ms 86400000)
        [y m dd] (days->civil days)]
    (.write w "#inst \"")
    (.write w (format "%04d-%02d-%02dT%02d:%02d:%02d.%03d-00:00"
                      y m dd (quot msd 3600000) (rem (quot msd 60000) 60)
                      (rem (quot msd 1000) 60) (rem msd 1000)))
    (.write w "\"")))

(defmethod print-method java.util.Date
  [^java.util.Date d, ^java.io.Writer w]
  (print-date d w))

(defmethod print-dup java.util.Date
  [^java.util.Date d, ^java.io.Writer w]
  (print-date d w))

;;; ------------------------------------------------------------------------
;;; reader integration

(defn- construct-date
  "Construct a java.util.Date, which expresses the original instant as
milliseconds since the epoch, UTC."
  [years months days hours minutes seconds nanoseconds
   offset-sign offset-hours offset-minutes]
  (Date. (long (- (+ (* (civil->days years months days) 86400000)
                     (* hours 3600000) (* minutes 60000) (* seconds 1000)
                     (quot nanoseconds 1000000))
                  (* offset-sign (+ (* offset-hours 60) offset-minutes) 60000)))))

(defn read-instant-date
  "To read an instant as a java.util.Date, bind *data-readers* to a map with
this var as the value for the 'inst key. The timezone offset will be used
to convert into UTC."
  [^CharSequence cs]
  (parse-timestamp (validated construct-date) cs))

(defn read-instant-calendar
  "Not in the Go build (java.util.Calendar)."
  [^CharSequence cs]
  (throw (UnsupportedOperationException. "read-instant-calendar is not available in the Go build")))

(defn read-instant-timestamp
  "Not in the Go build (java.sql.Timestamp)."
  [^CharSequence cs]
  (throw (UnsupportedOperationException. "read-instant-timestamp is not available in the Go build")))
