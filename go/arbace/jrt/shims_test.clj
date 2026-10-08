;; jrt's tests, phase 2b: charsets, locales and dates against the JVM (testdata/charsets.txt,
;; locales.txt, dates.txt, made by test/jrt/testdata_reflect.clj).
(in-ns 'go.arbace.jrt)

(go/file "shims_test.go"
  :imports [[regexp "regexp"] [strconv "strconv"] [strings "strings"] [testing "testing"]])

(go/func charsetOf3 ^Charset_I [^string n]
  (Charset_ForName_String__Charset (Str n)))

(go/func bytesOf ^{:tag (* ByteArray)} [^string s]
  (let [a (NewByteArray 0)]
    (when (!= s "")
      (range [_ p (strings/Split s ",")]
        (set! (.-A a) (append (.-A a) (conv int8 (atoi p))))))
    a))

(go/func bytesText ^any [^{:tag (* ByteArray)} b]
  (let [ps (make (slice string) (len (.-A b)))]
    (range [i x (.-A b)]
      (aset ps i (strconv/Itoa (conv int (conv uint8 x)))))
    (Str (strings/Join ps ","))))

(go/func TestCharsetsAgainstJVM [^{:tag (* testing/T)} t]
  (range [_ c (readCases t "charsets.txt")]
    ;; jrt has UTF-8, ISO-8859-1 and US-ASCII only (V9)
    (when (and (> (len c) 2) (== (aget c 1) "UTF-16"))
      (continue))
    (let [want (aget c (- (len c) 1))
          got ""]
      (switch (aget c 0)
        (case ["forName"] (set! got (outcome (fn ^any [] (.Name__String (Charset_ForName_String__Charset (jstr (aget c 1))))))))
        (case ["isSupported"] (set! got (outcome (fn ^any [] (Box (Charset_IsSupported_String__Z (jstr (aget c 1))))))))
        (case ["forNameNull"] (set! got (outcome (fn ^any [] (Charset_ForName_String__Charset nil)))))
        (case ["defaultCharset"] (set! got (outcome (fn ^any [] (.Name__String (Charset_DefaultCharset__Charset))))))
        (case ["encode"]
          (set! got (outcome (fn ^any [] (bytesText (.GetBytes_Charset__B1 (jstr (aget c 2)) (charsetOf3 (aget c 1))))))))
        (case ["decode"]
          (set! got (outcome (fn ^any [] (String_New_B1_Charset (bytesOf (aget c 2)) (charsetOf3 (aget c 1)))))))
        (case ["decodeRange"]
          (set! got (outcome (fn ^any [] (String_New_B1_I_I_Charset (bytesOf "97,195,169,98") 1 2 (charsetOf3 (aget c 1)))))))
        (case ["decodeRangeBad"]
          (set! got (outcome (fn ^any [] (String_New_B1_I_I_Charset (NewByteArray 3) 2 5 (charsetOf3 "UTF-8"))))))
        (default
          (let [cs (charsetOf3 (aget c 1))
                op (aget c 0)]
            (set! got (outcome (fn ^any []
                                 (switch op
                                   (case ["toString"] (return (.ToString__String cs)))
                                   (case ["hashCode"] (return (Box (.HashCode__I cs))))
                                   (case ["aliases"] (let [as (append (lit (slice string)) (spread (.Aliases (.Self_Charset cs))))]
                                                       (return (Str (sortedJoin as)))))
                                   (case ["canEncode"] (return (Box (.CanEncode__Z cs))))
                                   (case ["isRegistered"] (return (Box (.IsRegistered__Z cs))))
                                   (case ["containsUTF8"] (return (Box (.Contains_Charset__Z cs (charsetOf3 "UTF-8")))))
                                   (case ["containsASCII"] (return (Box (.Contains_Charset__Z cs (charsetOf3 "US-ASCII")))))
                                   (case ["containsLatin1"] (return (Box (.Contains_Charset__Z cs (charsetOf3 "ISO-8859-1")))))
                                   (case ["compareToUTF8"] (return (Box (.CompareTo_Charset__I cs (charsetOf3 "UTF-8")))))
                                   (case ["equalsUTF8"] (return (Box (.Equals_O__Z cs (charsetOf3 "UTF8")))))
                                   (case ["class"] (return (.GetName__String (GetClass cs)))))
                                 (panic (+ "unknown charset case " op))))))))
      (when (== (aget c 0) "aliases")
        (set! want (strings/ReplaceAll want "," "|")))
      (when (!= got want)
        (.Errorf t "%s: got %q, want %q" (strings/Join (subslice c _ (- (len c) 1)) " ") got want))))
  (when (or (!= StandardCharsets_UTF_8 (Charset_ForName_String__Charset (Str "utf-8")))
            (not (UTF_8_InstanceOf StandardCharsets_UTF_8)) (not (.IsAssignableFrom_Class__Z Charset_class UTF_8_class)))
    (.Error t "StandardCharsets, the classes")))

(go/func localeOf ^{:tag (* Locale)} [^string n]
  (switch n
    (case ["ROOT"] (return Locale_ROOT))
    (case ["US"] (return Locale_US))
    (case ["ENGLISH"] (return Locale_ENGLISH))
    (case ["UK"] (return Locale_UK)))
  (let [ps (strings/Split (strings/TrimPrefix n "of:") ",")]
    (for [] (< (len ps) 3) _
      (set! ps (append ps "")))
    (Locale_Of_String_String_String__Locale (Str (aget ps 0)) (Str (aget ps 1)) (Str (aget ps 2)))))

(go/func TestLocalesAgainstJVM [^{:tag (* testing/T)} t]
  (range [_ c (readCases t "locales.txt")]
    (let [want (aget c (- (len c) 1))
          op (aget c 0)
          got ""]
      (cond
        (== op "default") (set! got (outcome (fn ^any [] (.ToString__String (Locale_GetDefault__Locale)))))
        (== op "defaultFormat")
        (set! got (outcome (fn ^any [] (.ToString__String (Locale_GetDefault_Locale_Category__Locale Locale_Category_FORMAT)))))
        (== op "categories")
        (set! got (outcome (fn ^any []
                             (let [a (Locale_Category_Values__Locale_Category1)
                                   ps (make (slice string) (len (.-A a)))]
                               (range [i x (.-A a)]
                                 (aset ps i (.String (StrOfObj x))))
                               (Str (strings/Join ps ","))))))
        (or (== op "lower") (== op "upper"))
        (let [l (localeOf (aget c 1))
              s (jstr (aget c 2))]
          (set! got (outcome (fn ^any []
                               (when (== op "lower")
                                 (return (.ToLowerCase_Locale__String s l)))
                               (.ToUpperCase_Locale__String s l)))))
        (strings/HasPrefix op "dfs.")
        (let [d (DecimalFormatSymbols_GetInstance_Locale__DecimalFormatSymbols (localeOf (aget c 1)))]
          (set! got (outcome (fn ^any []
                               (switch (strings/TrimPrefix op "dfs.")
                                 (case ["zero"] (return (Box (conv int32 (.GetZeroDigit__C d)))))
                                 (case ["grouping"] (return (Box (conv int32 (.GetGroupingSeparator__C d)))))
                                 (case ["decimal"] (return (Box (conv int32 (.GetDecimalSeparator__C d)))))
                                 (case ["minus"] (return (Box (conv int32 (.GetMinusSign__C d)))))
                                 (case ["percent"] (return (Box (conv int32 (.GetPercent__C d)))))
                                 (case ["perMill"] (return (Box (conv int32 (.GetPerMill__C d)))))
                                 (case ["infinity"] (return (.GetInfinity__String d)))
                                 (case ["nan"] (return (.GetNaN__String d)))
                                 (case ["exponent"] (return (.GetExponentSeparator__String d)))
                                 (case ["currency"] (return (.GetCurrencySymbol__String d)))
                                 (case ["intlCurrency"] (return (.GetInternationalCurrencySymbol__String d)))
                                 (case ["locale"] (return (.ToString__String (.GetLocale__Locale d)))))
                               nil))))
        :else
        (let [l (localeOf (aget c 1))]
          (set! got (outcome (fn ^any []
                               (switch op
                                 (case ["toString"] (return (.ToString__String l)))
                                 (case ["hashCode"] (return (Box (.HashCode__I l))))
                                 (case ["toLanguageTag"] (return (.ToLanguageTag__String l)))
                                 (case ["language"] (return (.GetLanguage__String l)))
                                 (case ["country"] (return (.GetCountry__String l)))
                                 (case ["variant"] (return (.GetVariant__String l)))
                                 (case ["equalsUS"] (return (Box (.Equals_O__Z l Locale_US)))))
                               (panic (+ "unknown locale case " op)))))))
      (when (!= got want)
        (.Errorf t "%s: got %q, want %q" (strings/Join (subslice c _ (- (len c) 1)) " ") got want)))))

(go/func instFromGetters
  "instFromGetters is the #inst text composed from Date's Java API, as the reworked
arbace.instant prints it (JRT-NOTES.md, \"#inst\"): the deprecated GMT field getters, whose
year is the year of the era, and the milliseconds of getTime.\n"
  ^string [^{:tag (* Date)} d]
  (+ "#inst \"" (pad (+ (conv int64 (.GetYear__I d)) 1900) 4) "-" (pad (conv int64 (+ (.GetMonth__I d) 1)) 2) "-"
     (pad (conv int64 (.GetDate__I d)) 2) "T" (pad (conv int64 (.GetHours__I d)) 2) ":" (pad (conv int64 (.GetMinutes__I d)) 2) ":"
     (pad (conv int64 (.GetSeconds__I d)) 2) "." (pad (floorMod (.GetTime__J d) 1000) 3) "-00:00\""))

(go/var ^{:tag (* regexp/Regexp)} instRe
  (regexp/MustCompile "^(\\d\\d\\d\\d)(?:-(\\d\\d)(?:-(\\d\\d)(?:[T](\\d\\d)(?::(\\d\\d)(?::(\\d\\d)(?:[.](\\d+))?)?)?)?)?)?(?:[Z]|([-+])(\\d\\d):(\\d\\d))?$"))

(go/func readInstant
  "readInstant is arbace.instant's read-instant-date as the rework computes it over Date.UTC
(validation left out): the fields' GMT milliseconds, the offset subtracted, the fraction's
milliseconds added.\n"
  ^int64 [^string s]
  (let [m (.FindStringSubmatch instRe s)
        num (fn ^int64 [^string x ^int64 dflt]
              (when (== x "")
                (return dflt))
              (let [(values v _) (strconv/ParseInt x 10 64)] v))]
    (let [frac (aget m 7)]
      (while (< (len frac) 9)
        (set! frac (+ frac "0")))
      (let [nanos (num (subslice frac _ 9) 0)
            ms (Date_UTC_I_I_I_I_I_I__J (conv int32 (- (num (aget m 1) 0) 1900)) (conv int32 (- (num (aget m 2) 1) 1))
                                        (conv int32 (num (aget m 3) 1)) (conv int32 (num (aget m 4) 0))
                                        (conv int32 (num (aget m 5) 0)) (conv int32 (num (aget m 6) 0)))
            off (* (+ (* (num (aget m 9) 0) 60) (num (aget m 10) 0)) 60000)]
        (when (== (aget m 8) "-")
          (set! off (- off)))
        (+ (- ms off) (/ nanos 1000000))))))

(go/func TestDatesAgainstJVM [^{:tag (* testing/T)} t]
  (let [n 0]
    (range [_ c (readCases t "dates.txt")]
      (switch (aget c 0)
        (case ["date"]
          (inc! n)
          (let [ms (parseLong (aget c 1))
                d (Date_New_J ms)
                got (lit (slice string)
                         (outcome (fn ^any [] (.ToString__String d)))
                         (outcome (fn ^any [] (.ToGMTString__String d)))
                         (outcome (fn ^any [] (Str (strings/Join (lit (slice string)
                                                                      (strconv/Itoa (conv int (.GetYear__I d))) (strconv/Itoa (conv int (.GetMonth__I d)))
                                                                      (strconv/Itoa (conv int (.GetDate__I d))) (strconv/Itoa (conv int (.GetDay__I d)))
                                                                      (strconv/Itoa (conv int (.GetHours__I d))) (strconv/Itoa (conv int (.GetMinutes__I d)))
                                                                      (strconv/Itoa (conv int (.GetSeconds__I d))))
                                                                 ","))))
                         (outcome (fn ^any [] (Box (.HashCode__I d))))
                         (outcome (fn ^any [] (Str (+ "#inst \"" (.InstantText d) "\""))))
                         (outcome (fn ^any [] (Str (instFromGetters d)))))]
            (range [i g got]
              (let [w (aget c (+ 2 (min i 4)))]
                (when (!= g w)
                  (.Errorf t "date %d, field %d: got %q, want %q" ms i g w))))))
        (case ["utc"]
          (let [ps (strings/Split (aget c 1) ",")
                a (make (slice int32) 6)]
            (range [i p ps]
              (aset a i (atoi p)))
            (when [got (outcome (fn ^any [] (Box (Date_UTC_I_I_I_I_I_I__J (aget a 0) (aget a 1) (aget a 2) (aget a 3) (aget a 4) (aget a 5)))))]
                  (!= got (aget c 2))
              (.Errorf t "Date.UTC(%s): got %s, want %s" (aget c 1) got (aget c 2)))))
        (case ["readInst"]
          (when (not (strings/HasPrefix (aget c 2) "!"))
            (when [got (+ "java.lang.Long:" (strconv/FormatInt (readInstant (.String (jstr (aget c 1)))) 10))] (!= got (aget c 2))
              (.Errorf t "read-instant %s: got %s, want %s" (aget c 1) got (aget c 2)))))))
    (when (< n 500)
      (.Errorf t "only %d dates" n)))
  (let [a (Date_New_J 5)
        b (Date_New_J 5)]
    (when (or (== a b) (not (.Equals_O__Z a b)) (!= (.CompareTo_O__I a (Date_New_J 6)) -1) (not (.Before_Date__Z a (Date_New_J 6)))
              (!= (.GetTime__J (assert (* Date) (.Clone__O a))) 5))
      (.Error t "equals, compareTo, before, clone")))
  (when (< (.GetTime__J (Date_New)) 1700000000000)
    (.Error t "new Date() is now")))
