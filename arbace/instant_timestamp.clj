;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; Arbace (hand change 16, doc/VENDOR-NOTES.md): the java.sql.Timestamp part of arbace.instant
;; (instant.clj upstream), loaded by instant.clj only when the runtime has java.sql

(in-ns 'arbace.instant)
(import '[java.sql Timestamp])

(def ^:private ^ThreadLocal thread-local-utc-timestamp-format
  ;; SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  ;; http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335
  (proxy [ThreadLocal] []
    (initialValue []
      (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss")
        (.setTimeZone (java.util.TimeZone/getTimeZone "GMT"))))))

(defn- print-timestamp
  "Print a java.sql.Timestamp as RFC3339 timestamp, always in UTC."
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (let [^java.text.DateFormat utc-format (.get thread-local-utc-timestamp-format)]
    (.write w "#inst \"")
    (.write w (.format utc-format ts))
    ;; add on nanos and offset
    ;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
    (.write w (format ".%09d-00:00" (.getNanos ts)))
    (.write w "\"")))

(defmethod print-method java.sql.Timestamp
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (print-timestamp ts w))

(defmethod print-dup java.sql.Timestamp
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (print-timestamp ts w))

(defn- construct-timestamp
  "Construct a java.sql.Timestamp, which has nanosecond precision."
  [years months days hours minutes seconds nanoseconds
   offset-sign offset-hours offset-minutes]
  (doto (Timestamp.
         (.getTimeInMillis
          (construct-calendar years months days
                              hours minutes seconds 0
                              offset-sign offset-hours offset-minutes)))
    ;; nanos must be set separately, pass 0 above for the base calendar
    (.setNanos nanoseconds)))

(defn read-instant-timestamp
  "To read an instant as a java.sql.Timestamp, bind *data-readers* to a
map with this var as the value for the 'inst key. Timestamp preserves
fractional seconds with nanosecond precision. The timezone offset will
be used to convert into UTC."
  [^CharSequence cs]
  (parse-timestamp (validated construct-timestamp) cs))
