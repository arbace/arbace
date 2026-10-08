;; #inst and #uuid: literals of every precision and offset, arbace.instant's readers,
;; inst-ms, inst?, parse-uuid, and the printing of java.util.Date, Calendar and Timestamp.

(require '[arbace.instant :as inst])

;; ----- #inst literals
#inst "2020"
#inst "2020-06"
#inst "2020-06-15"
#inst "2020-06-15T12"
#inst "2020-06-15T12:34"
#inst "2020-06-15T12:34:56"
#inst "2020-06-15T12:34:56.7"
#inst "2020-06-15T12:34:56.78"
#inst "2020-06-15T12:34:56.789"
#inst "2020-06-15T12:34:56.789123"
#inst "2020-06-15T12:34:56.789123456"
#inst "2020-06-15T12:34:56Z"
#inst "2020-06-15T12:34:56.789Z"
#inst "2020-06-15T12:34:56-00:00"
#inst "2020-06-15T12:34:56+00:00"
#inst "2020-06-15T12:34:56+02:00"
#inst "2020-06-15T12:34:56-05:30"
#inst "2020-06-15T00:00:00+14:00"
#inst "2020-06-15T23:59:59-12:00"
#inst "2020-01-01T00:30:00+01:00"
#inst "1970-01-01T00:00:00Z"
#inst "1969-12-31T23:59:59.999Z"
#inst "1900-01-01T00:00:00Z"
#inst "0001-01-01T00:00:00Z"
#inst "9999-12-31T23:59:59.999Z"
#inst "2000-02-29T00:00:00Z"
#inst "2024-02-29"
#inst "1582-10-15T00:00:00Z"
#inst "1582-10-04T00:00:00Z"
#inst "2016-12-31T23:59:60Z"
(class #inst "2020")
(inst? #inst "2020")
(inst-ms #inst "2020")
(inst-ms #inst "1970-01-01T00:00:00Z")
(inst-ms #inst "1969-12-31T23:59:59.999Z")
(inst-ms #inst "2020-06-15T12:34:56.789Z")
(inst-ms #inst "2020-06-15T12:34:56.789+02:00")
(inst-ms #inst "2020-06-15T12:34:56.789123456Z")
(inst-ms #inst "0001-01-01T00:00:00Z")
(inst-ms #inst "9999-12-31T23:59:59.999Z")
(inst-ms #inst "1900-01-01T00:00:00Z")
(inst-ms #inst "1582-10-04T00:00:00Z")
(inst-ms #inst "1582-10-15T00:00:00Z")
(inst-ms #inst "2016-12-31T23:59:60Z")
(.getTime #inst "2000-01-01")
(= #inst "2020-01-01T00:00:00Z" #inst "2020-01-01T01:00:00+01:00")
(= #inst "2020" #inst "2020-01-01T00:00:00.000-00:00")
(compare #inst "2020" #inst "2021")
(compare #inst "2021" #inst "2020")
(sort [#inst "2021" #inst "1999" #inst "2020"])
(hash #inst "2020")
(pr-str #inst "2020-06-15T12:34:56.789+02:00")
(str (inst-ms #inst "2020"))
[#inst "2020" #inst "2021"]
{:at #inst "2020-01-01"}

;; ----- read-string of #inst, valid and not
(read-string "#inst \"2020-06-15T12:34:56.789Z\"")
(read-string "#inst \"2020-06-15T12:34:56.789-03:00\"")
(read-string "#inst \"2020-13-01\"")
(read-string "#inst \"2020-02-30\"")
(read-string "#inst \"2021-02-29\"")
(read-string "#inst \"2020-06-31\"")
(read-string "#inst \"2020-06-15T24:00:00Z\"")
(read-string "#inst \"2020-06-15T23:60:00Z\"")
(read-string "#inst \"2020-06-15T23:59:61Z\"")
(read-string "#inst \"2020-06-15T12:00:00+24:00\"")
(read-string "#inst \"2020-06-15T12:00:00+23:59\"")
(read-string "#inst \"2020-06-15T12:00:00+00:60\"")
(read-string "#inst \"2020-6-15\"")
(read-string "#inst \"20200615\"")
(read-string "#inst \"2020-06-15 12:00:00\"")
(read-string "#inst \"2020-06-15T12:00:00z\"")
(read-string "#inst \"2020-06-15T12:00:00.Z\"")
(read-string "#inst \"2020-06-15T12:00:00.1234567891Z\"")
(read-string "#inst \"-2020\"")
(read-string "#inst \"+2020\"")
(read-string "#inst \"02020\"")
(read-string "#inst \"\"")
(read-string "#inst \"2020-00-01\"")
(read-string "#inst \"2020-01-00\"")
(read-string "#inst \"0000\"")
(read-string "#inst \"2020-06-15T12:00:00+0200\"")
(read-string "#inst \"2020-06-15T12:00:00+02\"")
(read-string "#inst nil")
(read-string "#inst [2020]")

;; ----- arbace.instant
(inst/read-instant-date "2020-06-15T12:34:56.789Z")
(class (inst/read-instant-date "2020"))
(inst/read-instant-date "2020-06-15T12:34:56.789+05:00")
(inst/read-instant-date "1970")
(inst/read-instant-date "bogus")
(inst/read-instant-date "2020-06-15T12:34:56.789123456Z")
(inst/read-instant-calendar "2020-06-15T12:34:56.789Z")
(class (inst/read-instant-calendar "2020"))
(inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00")
(inst/read-instant-calendar "2020-06-15T12:34:56.789-08:00")
(inst/read-instant-calendar "1970")
(.getTimeInMillis (inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00"))
(.getOffset (.getTimeZone (inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00")) 0)
(.getID (.getTimeZone (inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00")))
(.get (inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00") java.util.Calendar/HOUR_OF_DAY)
(inst-ms (inst/read-instant-calendar "2020-06-15T12:34:56.789+05:00"))
(inst? (inst/read-instant-calendar "2020"))
(pr-str (inst/read-instant-calendar "2020-06-15T12:34:56.789-08:00"))
(inst/read-instant-timestamp "2020-06-15T12:34:56.789Z")
(class (inst/read-instant-timestamp "2020"))
(inst/read-instant-timestamp "2020-06-15T12:34:56.123456789Z")
(inst/read-instant-timestamp "2020-06-15T12:34:56.1Z")
(inst/read-instant-timestamp "2020-06-15T12:34:56.000000001Z")
(inst/read-instant-timestamp "2020-06-15T12:34:56.123456789+01:00")
(.getNanos (inst/read-instant-timestamp "2020-06-15T12:34:56.123456789Z"))
(inst-ms (inst/read-instant-timestamp "2020-06-15T12:34:56.123456789Z"))
(inst? (inst/read-instant-timestamp "2020"))
(pr-str (inst/read-instant-timestamp "1970-01-01T00:00:00.000000500Z"))
(binding [*data-readers* {'inst inst/read-instant-calendar}] (read-string "#inst \"2020-06-15T12:00:00+03:00\""))
(binding [*data-readers* {'inst inst/read-instant-timestamp}] (read-string "#inst \"2020-06-15T12:00:00.987654321Z\""))
(binding [*data-readers* {'inst inst/read-instant-date}] (class (read-string "#inst \"2020\"")))
(inst/parse-timestamp vector "2020-06-15T12:34:56.789-03:30")
(inst/parse-timestamp vector "2020")
(inst/parse-timestamp vector "2020-06-15T12:34:56.123456789Z")
(inst/parse-timestamp vector "2020-06-15T12:34:56+01:00")
(inst/parse-timestamp vector "nope")
((inst/validated vector) 2020 2 30 0 0 0 0 1 0 0)
((inst/validated vector) 2020 2 29 0 0 0 0 1 0 0)
((inst/validated vector) 2020 1 1 0 0 0 0 1 24 0)

;; ----- java.util.Date from fixed ms
(java.util.Date. 0)
(java.util.Date. 1)
(java.util.Date. -1)
(java.util.Date. 1000)
(java.util.Date. 86400000)
(java.util.Date. 1592224496789)
(java.util.Date. 253402300799999)
(java.util.Date. 253402300800000)
(java.util.Date. -62135596800000)
(java.util.Date. -62135596800001)
(java.util.Date. -62198755200000)
(java.util.Date. -12219292800000)
(java.util.Date. -12219292800001)
(java.util.Date. -2208988800000)
(java.util.Date. Long/MAX_VALUE)
(java.util.Date. Long/MIN_VALUE)
(java.util.Date. 4102444800000)
(java.util.Date. 951782400000)
(pr-str (java.util.Date. 0))
(print-str (java.util.Date. 0))
(binding [*print-dup* true] (pr-str (java.util.Date. 1592224496789)))
(inst-ms (java.util.Date. 12345))
(inst? (java.util.Date. 0))
(= (java.util.Date. 0) #inst "1970")
(= (java.util.Date. 0) (java.util.Date. 0))
(hash (java.util.Date. 12345))
(.getTime (java.util.Date. 12345))
(.before (java.util.Date. 0) (java.util.Date. 1))
(.after (java.util.Date. 0) (java.util.Date. 1))
(compare (java.util.Date. 2) (java.util.Date. 1))
(let [d (java.util.Date. 0)] (.setTime d 5000) d)
(read-string (pr-str (java.util.Date. 1592224496789)))
(= (java.util.Date. 1592224496789) (read-string (pr-str (java.util.Date. 1592224496789))))
(java.sql.Timestamp. 0)
(java.sql.Timestamp. 1592224496789)
(let [t (java.sql.Timestamp. 0)] (.setNanos t 123456789) t)
(inst? (java.sql.Timestamp. 0))
(inst-ms (java.sql.Timestamp. 1592224496789))
(let [c (java.util.GregorianCalendar. (java.util.TimeZone/getTimeZone "GMT"))] (.setTimeInMillis c 0) c)
(let [c (java.util.GregorianCalendar. (java.util.TimeZone/getTimeZone "GMT+02:00"))] (.setTimeInMillis c 0) c)
(let [c (java.util.GregorianCalendar. (java.util.TimeZone/getTimeZone "GMT-09:30"))] (.setTimeInMillis c 1592224496789) c)
(str (.toInstant (java.util.Date. 1592224496789)))
(inst? (java.time.Instant/ofEpochMilli 0))
(inst-ms (java.time.Instant/ofEpochMilli 1592224496789))
(inst-ms (java.time.Instant/ofEpochSecond 1 999999999))
(str (java.time.Instant/ofEpochMilli -1))
(inst? 0)
(inst? "2020")
(inst? nil)
(inst-ms 0)
(inst-ms nil)
(inst-ms "2020")

;; ----- #uuid
#uuid "00000000-0000-0000-0000-000000000000"
#uuid "123e4567-e89b-12d3-a456-426614174000"
#uuid "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"
#uuid "ffffffff-ffff-ffff-ffff-ffffffffffff"
#uuid "123E4567-E89B-12D3-A456-426614174000"
(class #uuid "123e4567-e89b-12d3-a456-426614174000")
(uuid? #uuid "123e4567-e89b-12d3-a456-426614174000")
(uuid? "123e4567-e89b-12d3-a456-426614174000")
(uuid? nil)
(= #uuid "123e4567-e89b-12d3-a456-426614174000" #uuid "123E4567-E89B-12D3-A456-426614174000")
(hash #uuid "123e4567-e89b-12d3-a456-426614174000")
(.hashCode #uuid "123e4567-e89b-12d3-a456-426614174000")
(str #uuid "123E4567-E89B-12D3-A456-426614174000")
(pr-str #uuid "123e4567-e89b-12d3-a456-426614174000")
(print-str #uuid "123e4567-e89b-12d3-a456-426614174000")
(binding [*print-dup* true] (pr-str #uuid "123e4567-e89b-12d3-a456-426614174000"))
(.version #uuid "123e4567-e89b-12d3-a456-426614174000")
(.variant #uuid "123e4567-e89b-12d3-a456-426614174000")
(.getMostSignificantBits #uuid "123e4567-e89b-12d3-a456-426614174000")
(.getLeastSignificantBits #uuid "123e4567-e89b-12d3-a456-426614174000")
(java.util.UUID. 0 0)
(java.util.UUID. -1 -1)
(java.util.UUID. 1311768467294899695 -81985529216486896)
(java.util.UUID/fromString "1-2-3-4-5")
(java.util.UUID/fromString "123e4567-e89b-12d3-a456-426614174000")
(java.util.UUID/nameUUIDFromBytes (.getBytes "arbace" "UTF-8"))
(compare #uuid "00000000-0000-0000-0000-000000000001" #uuid "00000000-0000-0000-0000-000000000002")
(compare #uuid "80000000-0000-0000-0000-000000000000" #uuid "00000000-0000-0000-0000-000000000000")
(sort [#uuid "00000000-0000-0000-0000-000000000002" #uuid "ffffffff-0000-0000-0000-000000000000" #uuid "00000000-0000-0000-0000-000000000001"])
(parse-uuid "123e4567-e89b-12d3-a456-426614174000")
(parse-uuid "123E4567-E89B-12D3-A456-426614174000")
(parse-uuid "00000000-0000-0000-0000-000000000000")
(parse-uuid "not a uuid")
(parse-uuid "")
(parse-uuid "123e4567e89b12d3a456426614174000")
(parse-uuid "123e4567-e89b-12d3-a456-42661417400")
(parse-uuid "123e4567-e89b-12d3-a456-4266141740000")
(parse-uuid "1-2-3-4-5")
(parse-uuid "g23e4567-e89b-12d3-a456-426614174000")
(parse-uuid " 123e4567-e89b-12d3-a456-426614174000")
(parse-uuid "{123e4567-e89b-12d3-a456-426614174000}")
(parse-uuid nil)
(parse-uuid 1)
(= (parse-uuid "123e4567-e89b-12d3-a456-426614174000") #uuid "123e4567-e89b-12d3-a456-426614174000")
(read-string "#uuid \"123e4567-e89b-12d3-a456-426614174000\"")
(read-string "#uuid \"123e4567\"")
(read-string "#uuid \"1-2-3-4-5\"")
(read-string "#uuid \"123e4567-e89b-12d3-a456-4266141740001\"")
(read-string "#uuid \"\"")
(read-string "#uuid nil")
(read-string (pr-str #uuid "123e4567-e89b-12d3-a456-426614174000"))
{#uuid "00000000-0000-0000-0000-000000000001" 1}
#{#uuid "00000000-0000-0000-0000-000000000001"}
(get {#uuid "00000000-0000-0000-0000-000000000001" :found} (parse-uuid "00000000-0000-0000-0000-000000000001"))
[#uuid "00000000-0000-0000-0000-000000000001" #inst "2020"]
