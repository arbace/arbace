;; java.text and the locales (doc/go/JRT-NOTES.md, "Text and locales"): the number formats
;; (DecimalFormat, NumberFormat, CompactNumberFormat, ChoiceFormat), their symbols, the date
;; formats (SimpleDateFormat, DateFormat, DateFormatSymbols), MessageFormat, the field
;; positions, Formatter's locale-dependent conversions, DateTimeFormatter.toFormat, Locale,
;; Currency, ListFormat, Collator, BreakIterator and Bidi; and the locales beyond the root
;; locale and English (the CLDR data of jdk.localedata: French, German, Japanese, Chinese,
;; ...). Deterministic: every locale, zone and date explicit; the default locale (en_US on the
;; JVM recording and on Go) and the default zone are not used where they would change a result.

(import '(java.text NumberFormat DecimalFormat DecimalFormatSymbols ChoiceFormat MessageFormat
                    SimpleDateFormat DateFormat DateFormatSymbols FieldPosition ParsePosition
                    ParseException Format Format$Field NumberFormat$Field NumberFormat$Style
                    DateFormat$Field MessageFormat$Field CompactNumberFormat Collator
                    RuleBasedCollator CollationKey BreakIterator StringCharacterIterator
                    AttributedString AttributedCharacterIterator Bidi ListFormat ListFormat$Type
                    ListFormat$Style Normalizer Normalizer$Form AttributedCharacterIterator$Attribute)
        '(java.util Locale Locale$Builder Locale$Category Locale$LanguageRange Currency TimeZone GregorianCalendar
                    Calendar Date Formatter)
        '(java.math BigDecimal BigInteger RoundingMode)
        '(java.time LocalDate LocalDateTime LocalTime ZonedDateTime ZoneId ZoneOffset Instant
                    Month DayOfWeek)
        '(java.time.format DateTimeFormatter FormatStyle TextStyle))

(defn ex-msg [f] (try (f) (catch Exception e [(.getName (class e)) (.getMessage e)])))

(def locales [Locale/US Locale/ROOT Locale/UK Locale/FRANCE Locale/GERMANY Locale/ITALY
              Locale/JAPAN Locale/KOREA Locale/CHINA Locale/TAIWAN Locale/CANADA
              Locale/CANADA_FRENCH (Locale/of "es" "ES") (Locale/of "es" "MX")
              (Locale/of "pt" "BR") (Locale/of "pt" "PT") (Locale/of "ru" "RU")
              (Locale/of "nl" "NL") (Locale/of "pl" "PL") (Locale/of "sv" "SE")
              (Locale/of "tr" "TR") (Locale/of "ar" "EG") (Locale/of "hi" "IN")
              (Locale/of "de" "CH") (Locale/of "en" "IN") (Locale/of "en" "AU")])

;; a fixed instant: 2020-06-15T13:05:09.123Z (a Monday), and a winter one
(def t1 1592226309123)
(def t2 1578007845000)
(defn utc-cal [] (doto (GregorianCalendar. (TimeZone/getTimeZone "UTC") Locale/US) (.setTimeInMillis t1)))

;; ----- Locale
(mapv str locales)
(mapv #(.toLanguageTag %) locales)
(mapv #(vector (.getLanguage %) (.getScript %) (.getCountry %) (.getVariant %)) locales)
(mapv #(str (Locale/forLanguageTag %))
      ["en-US" "fr" "zh-Hant-TW" "zh-Hans" "sr-Latn-RS" "und" "" "de-CH-1996" "ja-JP-u-ca-japanese"
       "en-US-x-lvariant-POSIX" "no-NO-x-lvariant-NY" "iw-IL" "he" "in" "ji" "x-private" "i-klingon"
       "en-GB-oed" "EN-us" "en_US" "zh-cmn-Hans-CN" "es-419" "sgn-BE-FR"])
(mapv #(.toLanguageTag (Locale/forLanguageTag %))
      ["en-US" "zh-Hant-TW" "de-CH-1996" "ja-JP-u-ca-japanese" "en-US-x-lvariant-POSIX"
       "iw-IL" "EN-us" "es-419" "de-DE-u-co-phonebk-nu-latn" "en-a-bbb-x-a-ccc"])
(mapv #(.toLanguageTag %) [(Locale/of "") (Locale/of "en" "" "POSIX") (Locale/of "no" "NO" "NY")
                           (Locale/of "iw") (Locale/of "ji" "IL") (Locale/of "in" "ID")
                           (Locale/of "ja" "JP" "JP") (Locale/of "th" "TH" "TH") (Locale/of "x" "y" "z")])
(mapv str [(Locale/of "iw") (Locale/of "ji" "IL") (Locale/of "in" "ID") (Locale/of "EN" "us")
           (Locale/of "" "US") (Locale/of "" "" "POSIX") (Locale/of "en" "" "POSIX")])
(let [l (Locale/forLanguageTag "de-DE-u-co-phonebk-nu-latn")]
  [(str l) (.hasExtensions l) (str (.stripExtensions l)) (.getUnicodeLocaleType l "nu")
   (.getUnicodeLocaleType l "co") (.getUnicodeLocaleType l "ca") (vec (sort (.getUnicodeLocaleKeys l)))
   (vec (sort (.getExtensionKeys l))) (.getExtension l \u)])
(ex-msg #(.getUnicodeLocaleType Locale/US "abc"))
(let [b (doto (Locale$Builder.) (.setLanguage "sr") (.setScript "Latn") (.setRegion "RS"))]
  [(str (.build b)) (.toLanguageTag (.build b)) (str (.build (.setUnicodeLocaleKeyword b "nu" "arab")))])
(ex-msg #(.build (.setLanguage (Locale$Builder.) "toolonglanguage")))
(ex-msg #(.build (.setRegion (Locale$Builder.) "USA")))
(= (Locale/of "en" "US") Locale/US)
(= (Locale/forLanguageTag "en-US") Locale/US)
(mapv #(.hashCode %) [Locale/US Locale/ROOT Locale/FRANCE (Locale/forLanguageTag "zh-Hant-TW")])
(for [l [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN Locale/CHINA (Locale/of "es" "MX")]]
  [(str l) (mapv #(.getDisplayName % l) [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN Locale/CHINA
                                          Locale/TAIWAN (Locale/forLanguageTag "sr-Latn-RS") (Locale/of "en" "US" "POSIX")])])
(for [l [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN]]
  [(.getDisplayLanguage Locale/CANADA_FRENCH l) (.getDisplayCountry Locale/CANADA_FRENCH l)
   (.getDisplayScript (Locale/forLanguageTag "zh-Hant") l) (.getDisplayVariant (Locale/of "de" "CH" "1996") l)])
(mapv #(vector (.getISO3Language %) (.getISO3Country %)) [Locale/US Locale/FRANCE Locale/JAPAN Locale/CHINA])
(count (Locale/getISOCountries))
(count (Locale/getISOLanguages))
(take 5 (sort (Locale/getISOCountries)))
(str (Locale/lookup (Locale$LanguageRange/parse "fr-CH, fr;q=0.9, en;q=0.8") [Locale/US Locale/FRANCE Locale/GERMANY]))
(mapv str (Locale/filter (Locale$LanguageRange/parse "en-*;q=1.0, fr;q=0.5") [Locale/US Locale/FRANCE Locale/UK Locale/GERMANY]))

;; ----- DecimalFormatSymbols
(for [l locales]
  (let [s (DecimalFormatSymbols/getInstance l)]
    [(str l) (.getZeroDigit s) (.getGroupingSeparator s) (.getDecimalSeparator s) (.getMinusSign s)
     (.getPercent s) (.getPerMill s) (.getExponentSeparator s) (.getInfinity s) (.getNaN s)
     (.getCurrencySymbol s) (.getInternationalCurrencySymbol s) (.getMonetaryDecimalSeparator s)
     (.getMonetaryGroupingSeparator s) (.getPatternSeparator s) (.getDigit s)]))
(let [s (DecimalFormatSymbols/getInstance (Locale/forLanguageTag "ar-EG-u-nu-arab"))]
  [(.getZeroDigit s) (.getDecimalSeparator s) (.getGroupingSeparator s) (.getMinusSign s)])
(let [s (DecimalFormatSymbols/getInstance (Locale/forLanguageTag "en-US-u-nu-thai"))]
  [(.getZeroDigit s) (.getDecimalSeparator s)])
(let [s (DecimalFormatSymbols. Locale/US)]
  (.setDecimalSeparator s \,) (.setGroupingSeparator s \.)
  [(.getDecimalSeparator s) (= s (DecimalFormatSymbols. Locale/US)) (= (.clone s) s) (str (.getCurrency s))])
(mapv (fn [l] (boolean (some #(= % l) (DecimalFormatSymbols/getAvailableLocales)))) [Locale/US Locale/FRANCE Locale/ROOT])

;; ----- NumberFormat
(for [l locales]
  (let [n (NumberFormat/getInstance l) i (NumberFormat/getIntegerInstance l)
        p (NumberFormat/getPercentInstance l) c (NumberFormat/getCurrencyInstance l)]
    [(str l) (.format n 1234567.891) (.format n -0.5) (.format i 1234567.5) (.format i 2.5)
     (.format p 0.256) (.format c 1234.5) (.format c -1234.5) (.toPattern n) (.toPattern c)]))
(for [l [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN (Locale/of "hi" "IN") (Locale/of "es" "ES")]
      s [NumberFormat$Style/SHORT NumberFormat$Style/LONG]]
  (let [f (NumberFormat/getCompactNumberInstance l s)]
    [(str l) (str s) (mapv #(.format f %) [0 999 1000 1500 12345 999999 1000000 2500000000 -42000])]))
(let [f (NumberFormat/getCompactNumberInstance Locale/US NumberFormat$Style/SHORT)]
  (.setMaximumFractionDigits f 2)
  [(.format f 1234567) (str (.parse f "1.5K")) (str (.parse f "2M"))])
(let [n (NumberFormat/getInstance Locale/US)]
  [(str (.parse n "1,234.56")) (str (.parse n "-12")) (ex-msg #(.parse n "abc"))
   (str (.parse n "12abc")) (.isGroupingUsed n) (.getMaximumFractionDigits n) (.getMinimumIntegerDigits n)
   (str (.getRoundingMode n)) (.isParseIntegerOnly n)])
(let [n (NumberFormat/getInstance Locale/FRANCE)]
  [(str (.parse n "1 234,56")) (str (.parse n "1 234,56")) (str (.parse n "-3,5"))])
(let [n (NumberFormat/getInstance Locale/GERMANY)]
  [(str (.parse n "1.234,56")) (.format n 1.0E-5) (.format n 1.0E20) (.format n Double/NaN)
   (.format n Double/POSITIVE_INFINITY) (.format n (BigDecimal. "12345678901234567890.123456789"))
   (.format n (BigInteger. "123456789012345678901234567890"))])
(let [c (NumberFormat/getCurrencyInstance Locale/JAPAN)] [(.format c 1234.567) (.getMaximumFractionDigits c)])
(let [c (doto (NumberFormat/getCurrencyInstance Locale/US) (.setCurrency (Currency/getInstance "EUR")))] (.format c 12.5))
(let [c (NumberFormat/getCurrencyInstance (Locale/forLanguageTag "en-US-u-cf-account"))] (.format c -5))
(mapv (fn [l] (boolean (some #(= % l) (NumberFormat/getAvailableLocales)))) [Locale/US Locale/FRANCE Locale/ROOT])

;; ----- DecimalFormat
(let [sym (DecimalFormatSymbols/getInstance Locale/US)]
  (for [p ["#,##0.00" "0.###E0" "00.00%" "#,##0.00;(#,##0.00)" "¤#,##0.00" "¤¤ #,##0.00"
           "#,##,###" "0.0##‰" "'#'#" "##0.#####E0" "@#" "0000" "#.##" ".00" "0.0E00"]]
    (let [f (DecimalFormat. p sym)]
      [p (.format f 1234.5678) (.format f -0.001234) (.format f 0) (.format f 1.0E10)
       (.toPattern f) (.toLocalizedPattern f)])))
(let [f (DecimalFormat. "#,##0.00" (DecimalFormatSymbols/getInstance Locale/FRANCE))]
  [(.format f 1234.5) (.toLocalizedPattern f) (str (.parse f "1 234,50"))])
(ex-msg #(DecimalFormat. "#,##0.0.0"))
(ex-msg #(DecimalFormat. "0#"))
(for [m (RoundingMode/values)]
  (let [f (doto (DecimalFormat. "0.0" (DecimalFormatSymbols/getInstance Locale/US)) (.setRoundingMode m))]
    [(str m) (mapv #(ex-msg (fn [] (.format f %))) [0.05 0.15 0.25 -0.05 -0.15 1.0 0.149999])]))
(let [f (DecimalFormat. "#,##0.###" (DecimalFormatSymbols/getInstance Locale/US))]
  [(.format f 0.1) (.format f 1.0E-7) (.format f 123456789.987654321) (.format f Long/MAX_VALUE)
   (.format f Long/MIN_VALUE) (.format f (BigDecimal. "1.23456789E+30")) (.format f 1/3)
   (.format f -0.0) (.format f 0.0005) (.format f 0.0015) (.format f 0.0025)])
(let [f (doto (DecimalFormat. "#,##0.###" (DecimalFormatSymbols/getInstance Locale/US)) (.setParseBigDecimal true))]
  [(str (.parse f "1,234.5678901234567890")) (class (.parse f "12")) (str (.parse f "-0"))])
(let [f (DecimalFormat. "0.00" (DecimalFormatSymbols/getInstance Locale/US))
      pp (ParsePosition. 2)]
  [(str (.parse f "xx12.345yy" pp)) (.getIndex pp) (.getErrorIndex pp)
   (let [p2 (ParsePosition. 0)] [(.parse f "zz" p2) (.getIndex p2) (.getErrorIndex p2)])])
(let [f (doto (DecimalFormat. "0.00" (DecimalFormatSymbols/getInstance Locale/US))
          (.setPositivePrefix "+") (.setNegativeSuffix "-") (.setMultiplier 100) (.setGroupingSize 2)
          (.setGroupingUsed true) (.setMinimumIntegerDigits 4) (.setDecimalSeparatorAlwaysShown true))]
  [(.format f 1.5) (.format f -0.25) (.toPattern f) (.getMultiplier f) (.getGroupingSize f)])
(let [f (DecimalFormat. "#,##0.00" (DecimalFormatSymbols/getInstance Locale/US))
      fp (FieldPosition. NumberFormat$Field/INTEGER)
      sb (StringBuffer.)]
  (.format f -1234567.891 sb fp)
  [(str sb) (.getBeginIndex fp) (.getEndIndex fp)
   (let [fp2 (FieldPosition. NumberFormat/FRACTION_FIELD)] (.format f 3.25 (StringBuffer.) fp2) [(.getBeginIndex fp2) (.getEndIndex fp2)])])
(let [f (DecimalFormat. "#,##0.00%" (DecimalFormatSymbols/getInstance Locale/US))
      it (.formatToCharacterIterator f -1234.5)]
  (loop [acc [] i (.getBeginIndex it)]
    (if (>= i (.getEndIndex it)) acc
      (do (.setIndex it i)
          (recur (conj acc [(str (.current it)) (mapv str (sort-by str (.keySet (.getAttributes it))))]) (inc i))))))
(let [a (DecimalFormat. "0.00") b (DecimalFormat. "0.00")] [(= a b) (= (.hashCode a) (.hashCode b)) (= a (.clone a))])

;; ----- ChoiceFormat
(let [f (ChoiceFormat. "0#no files|1#one file|1<{0,number,integer} files|100<many files")]
  [(mapv #(.format f %) [-1 0 1 1.5 2 100 101]) (.toPattern f) (vec (.getLimits f)) (vec (.getFormats f))])
(let [f (ChoiceFormat. (double-array [1 2 3]) (into-array String ["Sun" "Mon" "Tue"]))]
  [(mapv #(.format f %) [0 1 2 3 4]) (str (.parse f "Mon" (ParsePosition. 0))) (.toPattern f)])
[(ChoiceFormat/nextDouble 1.0) (ChoiceFormat/previousDouble 1.0) (ChoiceFormat/nextDouble 1.0 false)]
(ex-msg #(ChoiceFormat. "1#a|0#b"))
(ex-msg #(ChoiceFormat. "abc"))

;; ----- MessageFormat
(let [mf (MessageFormat. "{0} has {1,number,integer} items costing {2,number,currency}, {3,number,percent} done; {4}" Locale/US)]
  [(.format mf (to-array ["Ann" 1234 5.5 0.75 nil])) (.toPattern mf) (count (.getFormats mf))])
(let [mf (MessageFormat. "{0,choice,0#no files|1#one file|1<{0,number,integer} files}" Locale/US)]
  (mapv #(.format mf (to-array [%])) [0 1 2 1000]))
(let [mf (doto (MessageFormat. "{0,date,long} {0,time,short} {0,date,yyyy-MM-dd} {0,date} {0,time}" Locale/FRANCE))]
  (doseq [f (.getFormats mf)] (when (instance? DateFormat f) (.setTimeZone f (TimeZone/getTimeZone "Europe/Paris"))))
  (.format mf (to-array [(Date. t1)])))
(let [mf (MessageFormat. "{0,number,#.##} '{1}' ''quoted'' {1}" Locale/GERMANY)]
  [(.format mf (to-array [3.14159 "x"])) (.toPattern mf)])
(let [mf (MessageFormat. "{1} before {0}" Locale/US)] (vec (.parse mf "b before a")))
(let [mf (MessageFormat. "{0,number} and {1}" Locale/US)] (mapv str (.parse mf "1,234.5 and more")))
(ex-msg #(MessageFormat. "{0" Locale/US))
(ex-msg #(MessageFormat. "{x}" Locale/US))
(ex-msg #(.parse (MessageFormat. "a{0}b" Locale/US) "xyz"))
(MessageFormat/format "{0} + {1} = {2}" (to-array [1 2 3]))
(let [mf (MessageFormat. "{0,number,integer} {1}" Locale/FRANCE)
      it (.formatToCharacterIterator mf (to-array [1234567 "x"]))]
  [(- (.getEndIndex it) (.getBeginIndex it)) (mapv str (sort-by str (.getAllAttributeKeys it)))])
(str (.getLocale (MessageFormat. "{0}" Locale/JAPAN)))
(let [mf (MessageFormat. "{0,dtf_date,MEDIUM} {0,dtf_time,SHORT} {0,dtf_datetime,MEDIUM} {0,dtf_date,yyyy-MM-dd}" Locale/US)]
  (.format mf (to-array [(LocalDateTime/of 2020 6 15 13 5 9)])))

;; ----- DateFormatSymbols
(for [l locales]
  (let [s (DateFormatSymbols/getInstance l)]
    [(str l) (vec (.getMonths s)) (vec (.getShortMonths s)) (vec (.getWeekdays s))
     (vec (.getShortWeekdays s)) (vec (.getAmPmStrings s)) (vec (.getEras s))
     (.getLocalPatternChars s)]))
(let [s (DateFormatSymbols. Locale/US)]
  (.setMonths s (into-array String (map #(str "M" %) (range 13))))
  [(first (.getMonths s)) (= s (DateFormatSymbols. Locale/US)) (= (.clone s) s)])
(let [z (.getZoneStrings (DateFormatSymbols/getInstance Locale/US))]
  [(> (count z) 400) (vec (first (filter #(= "Europe/Paris" (first %)) z)))
   (vec (first (filter #(= "America/New_York" (first %)) z)))])
(let [z (.getZoneStrings (DateFormatSymbols/getInstance Locale/FRANCE))]
  (vec (first (filter #(= "Europe/Paris" (first %)) z))))
(mapv (fn [l] (boolean (some #(= % l) (DateFormatSymbols/getAvailableLocales)))) [Locale/US Locale/FRANCE Locale/ROOT])

;; ----- SimpleDateFormat and DateFormat
(defn sdf [p l z] (doto (SimpleDateFormat. p l) (.setTimeZone (TimeZone/getTimeZone z))))
(for [l locales]
  [(str l) (.format (sdf "EEEE d MMMM yyyy G, HH:mm:ss.SSS a z Z X" l "UTC") (Date. t1))
   (.format (sdf "EEE, d MMM yy h:mm a zzzz" l "Europe/Paris") (Date. t2))
   (.format (sdf "LLLL w W D F u k K" l "Asia/Tokyo") (Date. t1))])
(for [l locales]
  (let [d (Date. t1)]
    [(str l)
     (mapv #(.format (doto (DateFormat/getDateInstance % l) (.setTimeZone (TimeZone/getTimeZone "UTC"))) d)
           [DateFormat/FULL DateFormat/LONG DateFormat/MEDIUM DateFormat/SHORT])
     (mapv #(.format (doto (DateFormat/getTimeInstance % l) (.setTimeZone (TimeZone/getTimeZone "America/New_York"))) d)
           [DateFormat/FULL DateFormat/LONG DateFormat/MEDIUM DateFormat/SHORT])
     (.format (doto (DateFormat/getDateTimeInstance DateFormat/MEDIUM DateFormat/SHORT l) (.setTimeZone (TimeZone/getTimeZone "UTC"))) d)
     (.toPattern (DateFormat/getDateTimeInstance DateFormat/LONG DateFormat/LONG l))]))
(let [f (sdf "yyyy-MM-dd HH:mm:ss Z" Locale/US "UTC")]
  [(.getTime (.parse f "2020-06-15 13:05:09 +0200")) (.getTime (.parse f "2020-02-30 25:61:61 +0000"))
   (ex-msg #(.parse (doto (sdf "yyyy-MM-dd" Locale/US "UTC") (.setLenient false)) "2020-02-30"))
   (ex-msg #(.parse f "garbage"))])
(let [f (sdf "d MMMM yyyy" Locale/FRANCE "UTC")] [(.getTime (.parse f "15 juin 2020")) (.getTime (.parse f "1 février 2021"))])
(let [f (sdf "EEEE, d. MMMM yyyy" Locale/GERMANY "UTC")] (.getTime (.parse f "Montag, 15. Juni 2020")))
(let [f (sdf "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" Locale/US "Asia/Kolkata")] [(.format f (Date. t1)) (.getTime (.parse f "2020-06-15T18:35:09.123+05:30"))])
(let [f (sdf "yyyy MMM dd zzz" Locale/US "UTC")] [(.getTime (.parse f "2020 Jun 15 PDT")) (.getTime (.parse f "2020 Jun 15 Pacific Daylight Time"))])
(let [f (sdf "Y-'W'ww-u" Locale/US "UTC")] (mapv #(.format f (Date. %)) [t1 t2 1609459200000]))
(let [f (sdf "Y-'W'ww-u" Locale/FRANCE "UTC")] (mapv #(.format f (Date. %)) [t1 t2 1609459200000]))
(let [f (sdf "yyyy.MM.dd G 'at' HH:mm:ss" Locale/US "UTC")] [(.toPattern f) (.toLocalizedPattern f) (.format f (Date. -62135769600000))])
(let [f (sdf "dd/MM/yyyy HH:mm" Locale/UK "Europe/London")
      fp (FieldPosition. DateFormat/MONTH_FIELD) sb (StringBuffer.)]
  (.format f (Date. t1) sb fp) [(str sb) (.getBeginIndex fp) (.getEndIndex fp)])
(let [f (sdf "EEE d MMM yyyy" Locale/US "UTC") it (.formatToCharacterIterator f (Date. t1))]
  [(- (.getEndIndex it) (.getBeginIndex it)) (mapv str (sort-by str (.getAllAttributeKeys it)))])
(ex-msg #(SimpleDateFormat. "yyyy-qq"))
(let [f (sdf "yyyy" Locale/US "UTC")] [(.equals f (sdf "yyyy" Locale/US "UTC")) (= (.hashCode f) (.hashCode (sdf "yyyy" Locale/US "UTC")))])
(let [f (sdf "yy" Locale/US "UTC")] (.set2DigitYearStart f (Date. 0)) [(.getTime (.parse f "05")) (.getTime (.parse f "69")) (.getTime (.get2DigitYearStart f))])
(let [c (doto (GregorianCalendar. (TimeZone/getTimeZone "UTC") Locale/FRANCE) (.setTimeInMillis t1))]
  [(.getFirstDayOfWeek c) (.getMinimalDaysInFirstWeek c) (.get c Calendar/WEEK_OF_YEAR)
   (.getDisplayName c Calendar/MONTH Calendar/LONG Locale/FRANCE) (.getDisplayName c Calendar/DAY_OF_WEEK Calendar/SHORT Locale/GERMANY)
   (into (sorted-map) (.getDisplayNames c Calendar/MONTH Calendar/SHORT Locale/ITALY))])
(let [c (doto (GregorianCalendar. (TimeZone/getTimeZone "UTC") Locale/US) (.setTimeInMillis t1))]
  [(.getFirstDayOfWeek c) (.get c Calendar/WEEK_OF_YEAR) (.getDisplayName c Calendar/ERA Calendar/LONG Locale/US)
   (.getDisplayName c Calendar/AM_PM Calendar/SHORT Locale/JAPAN)])
(mapv #(.getFirstDayOfWeek (Calendar/getInstance (TimeZone/getTimeZone "UTC") %)) locales)

;; ----- Formatter with locales
(for [l locales]
  (let [z (ZonedDateTime/of 2020 6 15 13 5 9 123000000 (ZoneId/of "Europe/Paris"))]
    [(str l) (String/format l "%tB %tb %tA %ta %tp %Tp" (to-array [z z z z z z]))
     (String/format l "%tc" (to-array [z])) (String/format l "%tr %tD %tF" (to-array [z z z]))
     (String/format l "%,d %,.2f %.3e %(,.1f %+d %s" (to-array [1234567 1234567.891 0.000123 -4567.25 42 1.5]))]))
(let [c (utc-cal)]
  (mapv #(String/format % "%1$tB %1$tA %1$tp %1$tZ %1$tc" (to-array [c])) [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN]))
(String/format Locale/FRANCE "%,.2f|%10.1f|%-10.1f|%010.2f" (to-array [1234.5 -2.25 3.5 -1.25]))
(String/format Locale/GERMANY "%,d %x %o %h %S %c %b %n" (to-array [-1234567 255 8 "x" "abc" (int \a) nil]))
(String/format (Locale/forLanguageTag "ar-EG-u-nu-arab") "%d %,.2f" (to-array [1234 1234.5]))
(String/format (Locale/forLanguageTag "hi-IN") "%,d %,.2f" (to-array [123456789 1234567.25]))
(String/format Locale/ROOT "%,d %tB" (to-array [1234567 (LocalDate/of 2020 6 15)]))
(let [sb (StringBuilder.)] (-> (Formatter. sb Locale/ITALY) (.format "%,.3f %tA" (to-array [9876.54321 (LocalDate/of 2020 6 15)]))) (str sb))

;; ----- DateTimeFormatter.toFormat and java.time with the locales
(let [f (.toFormat (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm" Locale/US))]
  [(.format f (LocalDateTime/of 2020 6 15 13 5)) (str (.parseObject f "2021-01-02 03:04"))
   (class (.parseObject f "2021-01-02 03:04")) (ex-msg #(.parseObject f "nope"))
   (ex-msg #(.format f "string"))])
(let [f (.toFormat (DateTimeFormatter/ofPattern "yyyy-MM-dd") (reify java.time.temporal.TemporalQuery (queryFrom [_ t] (LocalDate/from t))))]
  [(str (.parseObject f "2020-06-15")) (class (.parseObject f "2020-06-15"))])
(let [f (.toFormat (DateTimeFormatter/ofLocalizedDate FormatStyle/LONG) )
      it (.formatToCharacterIterator f (LocalDate/of 2020 6 15))]
  (- (.getEndIndex it) (.getBeginIndex it)))
(for [l locales]
  [(str l) (.format (DateTimeFormatter/ofPattern "MMMM EEEE a G MMM EEE LLLL cccc QQQQ" l) (LocalDateTime/of 2020 6 15 13 5))
   (mapv #(.format (.withLocale (DateTimeFormatter/ofLocalizedDate %) l) (LocalDate/of 2020 6 15))
         [FormatStyle/FULL FormatStyle/LONG FormatStyle/MEDIUM FormatStyle/SHORT])
   (.format (.withLocale (DateTimeFormatter/ofLocalizedDateTime FormatStyle/MEDIUM FormatStyle/SHORT) l) (LocalDateTime/of 2020 6 15 13 5))
   (.format (.withLocale (DateTimeFormatter/ofLocalizedPattern "yMMMEd") l) (LocalDate/of 2020 6 15))
   (.format (DateTimeFormatter/ofPattern "zzzz z VV O" l) (ZonedDateTime/of 2020 6 15 13 5 0 0 (ZoneId/of "Europe/Paris")))
   (mapv #(.getDisplayName % TextStyle/FULL l) [Month/JANUARY Month/AUGUST DayOfWeek/SUNDAY])
   (.getDisplayName (ZoneId/of "America/New_York") TextStyle/FULL l)
   (str (java.time.temporal.WeekFields/of l))])
(.format (DateTimeFormatter/ofPattern "MMMM EEEE a G" Locale/FRANCE) (LocalDateTime/of 2020 6 15 13 5))
(.format (DateTimeFormatter/ofPattern "MMMM EEEE a G" Locale/UK) (LocalDateTime/of 2020 6 15 13 5))
(str (LocalDate/parse "15 juin 2020" (DateTimeFormatter/ofPattern "d MMMM yyyy" Locale/FRANCE)))
(str (LocalDate/parse "15. Juni 2020" (DateTimeFormatter/ofPattern "d. MMMM yyyy" Locale/GERMANY)))
(.getDisplayName (TimeZone/getTimeZone "Europe/Paris") Locale/FRANCE)
(.getDisplayName (TimeZone/getTimeZone "Asia/Tokyo") true TimeZone/LONG Locale/JAPAN)
(.getDisplayName (TimeZone/getTimeZone "America/Los_Angeles") false TimeZone/SHORT Locale/GERMANY)

;; ----- Currency
(let [c (Currency/getInstance "EUR")]
  [(.getCurrencyCode c) (.getDefaultFractionDigits c) (.getNumericCode c) (.getNumericCodeAsString c)
   (mapv #(.getSymbol c %) [Locale/US Locale/FRANCE Locale/UK Locale/JAPAN Locale/ROOT])
   (mapv #(.getDisplayName c %) [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN])
   (str c)])
(mapv #(str (Currency/getInstance %)) [Locale/US Locale/FRANCE Locale/UK Locale/JAPAN Locale/CHINA (Locale/of "pt" "BR") (Locale/of "hi" "IN")])
(ex-msg #(Currency/getInstance "XYZ"))
(ex-msg #(Currency/getInstance Locale/ROOT))
(mapv #(.getDefaultFractionDigits (Currency/getInstance %)) ["USD" "JPY" "BHD" "XAU" "CLF"])
(> (count (Currency/getAvailableCurrencies)) 200)
(mapv #(.getSymbol (Currency/getInstance %) Locale/US) ["USD" "CAD" "AUD" "GBP" "JPY" "CNY" "INR" "CHF"])

;; ----- ListFormat
(for [l [Locale/US Locale/FRANCE Locale/GERMANY Locale/JAPAN (Locale/of "es" "ES")]
      t (ListFormat$Type/values) s (ListFormat$Style/values)]
  [(str l) (str t) (str s) (.format (ListFormat/getInstance l t s) ["a" "b" "c"])])
(let [f (ListFormat/getInstance Locale/US ListFormat$Type/STANDARD ListFormat$Style/FULL)]
  [(ex-msg #(.format f [])) (.format f ["x"]) (.format f ["x" "y"]) (vec (.parse f "a, b, and c"))])

;; ----- Collator
(defn sorted-with [c xs] (vec (sort c xs)))
(def words ["peach" "Péché" "péché" "pêche" "apple" "Apple" "zebra" "Æble" "côte" "coté" "côté" "cote" "ångström" "Zürich" "zz" "a-b" "ab" "a b" "Ä" "äb" "Strasse" "Straße"])
(for [l [Locale/US Locale/FRANCE Locale/GERMANY (Locale/of "sv" "SE") Locale/ROOT]]
  [(str l) (sorted-with (Collator/getInstance l) words)])
(for [s [Collator/PRIMARY Collator/SECONDARY Collator/TERTIARY Collator/IDENTICAL]]
  (let [c (doto (Collator/getInstance Locale/US) (.setStrength s))]
    [s (.compare c "a" "A") (.compare c "a" "á") (.compare c "a" "b") (.equals c "abc" "ABC") (.compare c "é" "é")]))
(let [c (doto (Collator/getInstance Locale/US) (.setDecomposition Collator/CANONICAL_DECOMPOSITION))]
  [(.compare c "é" "é") (.getDecomposition c) (.getStrength c)])
(let [c (Collator/getInstance Locale/US) k1 (.getCollationKey c "apple") k2 (.getCollationKey c "Apple")]
  [(.compareTo k1 k2) (.getSourceString k1) (count (.toByteArray k1)) (= (.toByteArray k1) (.toByteArray k2))])
(let [c (RuleBasedCollator. "< c < b < a")] [(sorted-with c ["a" "b" "c"]) (.getRules c)])
(ex-msg #(RuleBasedCollator. "& z < "))
(let [c (Collator/getInstance Locale/US) it (.getCollationElementIterator c "ab")]
  [(str (class c)) (loop [acc []] (let [e (.next it)] (if (= e java.text.CollationElementIterator/NULLORDER) acc (recur (conj acc [(java.text.CollationElementIterator/primaryOrder e) (java.text.CollationElementIterator/secondaryOrder e)])))))])
(count (.getRules (Collator/getInstance Locale/ROOT)))
(= (.getRules (Collator/getInstance Locale/FRANCE)) (.getRules (Collator/getInstance Locale/US)))

;; ----- BreakIterator
(defn breaks [bi s] (.setText bi s) (loop [acc [(.first bi)]] (let [n (.next bi)] (if (= n BreakIterator/DONE) acc (recur (conj acc n))))))
(def text "Hello, world! It's 3.14 o'clock. Mr. Smith isn't here.\nNew line? Yes. 你好世界。é 👍🏽 ok")
(breaks (BreakIterator/getCharacterInstance Locale/US) text)
(breaks (BreakIterator/getWordInstance Locale/US) text)
(breaks (BreakIterator/getSentenceInstance Locale/US) text)
(breaks (BreakIterator/getLineInstance Locale/US) text)
(breaks (BreakIterator/getWordInstance Locale/FRANCE) "l'école d'été, aujourd'hui.")
(let [bi (doto (BreakIterator/getWordInstance) (.setText "one two three"))]
  [(.following bi 1) (.preceding bi 9) (.isBoundary bi 3) (.isBoundary bi 4) (.last bi) (.previous bi) (.current bi)])
(let [bi (doto (BreakIterator/getSentenceInstance Locale/US) (.setText (StringCharacterIterator. "A b. C d? E")))]
  [(.next bi 2) (.current bi)])

;; ----- Bidi
(let [b (Bidi. "abc אבג def" Bidi/DIRECTION_LEFT_TO_RIGHT)]
  [(.isMixed b) (.isLeftToRight b) (.getRunCount b) (mapv #(vector (.getRunStart b %) (.getRunLimit b %) (.getRunLevel b %)) (range (.getRunCount b)))
   (.getBaseLevel b) (.getLevelAt b 5)])
(let [b (Bidi. "אב 12 abc" Bidi/DIRECTION_DEFAULT_LEFT_TO_RIGHT)]
  [(.isRightToLeft b) (.getBaseLevel b) (.getRunCount b) (.getLength b)])
[(Bidi/requiresBidi (char-array "abc") 0 3) (Bidi/requiresBidi (char-array "aא") 0 2)]
(let [levels (byte-array [0 1 1 0]) objs (to-array ["a" "b" "c" "d"])] (Bidi/reorderVisually levels 0 objs 0 4) (vec objs))

;; ----- AttributedString, StringCharacterIterator, ParsePosition, FieldPosition, Format.Field
(let [s (doto (AttributedString. "hello world") (.addAttribute AttributedCharacterIterator$Attribute/LANGUAGE Locale/FRANCE 0 5))
      it (.getIterator s)]
  [(.getRunLimit it) (str (.getAttribute it AttributedCharacterIterator$Attribute/LANGUAGE)) (.getEndIndex it)])
(let [it (StringCharacterIterator. "abc" 1)] [(.current it) (.next it) (.next it) (.next it) (= (.next it) StringCharacterIterator/DONE) (.previous it) (.first it) (.last it)])
(let [p (ParsePosition. 3)] (.setErrorIndex p 5) [(.getIndex p) (.getErrorIndex p) (str p) (= p (ParsePosition. 3))])
(let [f (FieldPosition. NumberFormat$Field/INTEGER 0)] (.setBeginIndex f 2) (.setEndIndex f 4) [(.getField f) (str (.getFieldAttribute f)) (str f)])
(mapv str [NumberFormat$Field/GROUPING_SEPARATOR DateFormat$Field/ERA DateFormat$Field/HOUR_OF_DAY0 MessageFormat$Field/ARGUMENT])
[(.getCalendarField DateFormat$Field/MONTH) (str (DateFormat$Field/ofCalendarField Calendar/YEAR))]
(let [e (ParseException. "bad" 7)] [(.getMessage e) (.getErrorOffset e)])
(mapv #(str (Normalizer/normalize "é ﬁ ①" %)) (Normalizer$Form/values))
