;; jrt: java.util.Locale and Locale.Category, java.text.DecimalFormatSymbols, and String's Locale
;; overloads (doc/go/JRT-NOTES.md, "Phase 2b"). The locale providers Formatter and java.time
;; name (sun.util.locale.provider.LocaleProviderAdapter, LocaleResources, ...) are jrt's own Java
;; since java.time came into the Go build (overlay/jdk; JRT-NOTES.md, "Time").
;; One set of locale data, the root locale's, as decided for Formatter (JAVA-SURFACE.md,
;; decision 2): every locale formats and maps case as the root locale does; the default locale
;; is en_US, so that Formatter takes its Locale.US paths, as on a JVM started with
;; user.language=en, user.country=US.
(in-ns 'go.arbace.jrt)

(go/file "locale.go"
  :imports [[strings "strings"]])

;; ---------------------------------------------------------------------------------------
;; java.util.Locale

(go/type Locale
  "Locale is java.util.Locale (final: *Locale): language (lower case), country (upper case),
variant; scripts and extensions are not kept.\n"
  (struct Object ^{:tag (* String)} language ^{:tag (* String)} country ^{:tag (* String)} variant))

(go/var Locale_class
  (Define (addr (lit ClassInfo :Name "java.util.Locale" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Cloneable_class Serializable_class)
                     :Go "arbace/jrt.Locale"))))

(go/func newLocale ^{:tag (* Locale)} [^string l ^string c ^string v]
  (addr (lit Locale :language (Intern (strings/ToLower l)) :country (Intern (strings/ToUpper c))
             :variant (Intern v))))

(go/var
  [^{:tag (* Locale) :doc "Locale_ROOT is Locale.ROOT.\n"} Locale_ROOT (newLocale "" "" "")]
  [^{:tag (* Locale)} Locale_ENGLISH (newLocale "en" "" "")]
  [^{:tag (* Locale)} Locale_US (newLocale "en" "US" "")]
  [^{:tag (* Locale)} Locale_UK (newLocale "en" "GB" "")]
  ;; the JDK's other locale constants (java.time's forms name them; JRT-NOTES.md, "Time")
  [^{:tag (* Locale)} Locale_FRENCH (newLocale "fr" "" "")]
  [^{:tag (* Locale)} Locale_GERMAN (newLocale "de" "" "")]
  [^{:tag (* Locale)} Locale_ITALIAN (newLocale "it" "" "")]
  [^{:tag (* Locale)} Locale_JAPANESE (newLocale "ja" "" "")]
  [^{:tag (* Locale)} Locale_KOREAN (newLocale "ko" "" "")]
  [^{:tag (* Locale)} Locale_CHINESE (newLocale "zh" "" "")]
  [^{:tag (* Locale)} Locale_SIMPLIFIED_CHINESE (newLocale "zh" "CN" "")]
  [^{:tag (* Locale)} Locale_TRADITIONAL_CHINESE (newLocale "zh" "TW" "")]
  [^{:tag (* Locale)} Locale_FRANCE (newLocale "fr" "FR" "")]
  [^{:tag (* Locale)} Locale_GERMANY (newLocale "de" "DE" "")]
  [^{:tag (* Locale)} Locale_ITALY (newLocale "it" "IT" "")]
  [^{:tag (* Locale)} Locale_JAPAN (newLocale "ja" "JP" "")]
  [^{:tag (* Locale)} Locale_KOREA (newLocale "ko" "KR" "")]
  [^{:tag (* Locale)} Locale_CANADA (newLocale "en" "CA" "")]
  [^{:tag (* Locale)} Locale_CANADA_FRENCH (newLocale "fr" "CA" "")])

(go/var
  [^{:tag (* Locale) :doc "Locale_CHINA is Locale.CHINA, Locale.SIMPLIFIED_CHINESE.\n"} Locale_CHINA Locale_SIMPLIFIED_CHINESE]
  [^{:tag (* Locale)} Locale_PRC Locale_SIMPLIFIED_CHINESE]
  [^{:tag (* Locale)} Locale_TAIWAN Locale_TRADITIONAL_CHINESE])

(go/func Locale_Of_String__Locale "Locale_Of_String__Locale is Locale.of(language).\n"
  ^{:tag (* Locale)} [^{:tag (* String)} l]
  (newLocale (.String (NN l)) "" ""))
(go/func Locale_Of_String_String__Locale ^{:tag (* Locale)} [^{:tag (* String)} l ^{:tag (* String)} c]
  (newLocale (.String (NN l)) (.String (NN c)) ""))
(go/func Locale_Of_String_String_String__Locale
  ^{:tag (* Locale)} [^{:tag (* String)} l ^{:tag (* String)} c ^{:tag (* String)} v]
  (newLocale (.String (NN l)) (.String (NN c)) (.String (NN v))))
(go/func Locale_New_String "Locale_New_String is the deprecated new Locale(language).\n"
  ^{:tag (* Locale)} [^{:tag (* String)} l]
  (Locale_Of_String__Locale l))
(go/func Locale_New_String_String ^{:tag (* Locale)} [^{:tag (* String)} l ^{:tag (* String)} c]
  (Locale_Of_String_String__Locale l c))
(go/func Locale_New_String_String_String
  ^{:tag (* Locale)} [^{:tag (* String)} l ^{:tag (* String)} c ^{:tag (* String)} v]
  (Locale_Of_String_String_String__Locale l c v))

(go/func Locale_GetDefault__Locale
  "Locale_GetDefault__Locale is Locale.getDefault: en_US (JRT-NOTES.md, \"Locales\").\n"
  ^{:tag (* Locale)} []
  Locale_US)

(go/func Locale_GetDefault_Locale_Category__Locale ^{:tag (* Locale)} [^{:tag (* Locale_Category)} c]
  (NN c)
  Locale_US)

(go/method GetLanguage__String ^{:tag (* String)} [^{:tag (* Locale)} t] (.-language t))
(go/method GetCountry__String ^{:tag (* String)} [^{:tag (* Locale)} t] (.-country t))
(go/method GetVariant__String ^{:tag (* String)} [^{:tag (* Locale)} t] (.-variant t))
(go/method GetScript__String ^{:tag (* String)} [^{:tag (* Locale)} t] litEmpty)

(go/method HasExtensions__Z
  "HasExtensions__Z is Locale.hasExtensions: false, jrt's locales have no extensions (JRT-NOTES.md,
\"Time\").\n"
  ^bool [^{:tag (* Locale)} t]
  false)

(go/method StripExtensions__Locale "StripExtensions__Locale is Locale.stripExtensions: the locale.\n"
  ^{:tag (* Locale)} [^{:tag (* Locale)} t]
  t)

(go/method GetUnicodeLocaleType_String__String
  "GetUnicodeLocaleType_String__String is Locale.getUnicodeLocaleType: null (no Unicode locale
extension), after the JDK's check of the key: two letters or digits, else
IllegalArgumentException.\n"
  ^{:tag (* String)} [^{:tag (* Locale)} t ^{:tag (* String)} key]
  (let [k (.String (NN key))]
    (when (or (!= (len k) 2) (not (alnum (aget k 0))) (not (alnum (aget k 1))))
      (panic (IllegalArgumentException_New_String (Str (+ "Ill-formed Unicode locale key: " k)))))
    nil))

(go/func alnum ^bool [^byte c]
  (or (and (>= c \a) (<= c \z)) (and (>= c \A) (<= c \Z)) (and (>= c \0) (<= c \9))))

(go/method ToString__String
  "ToString__String is Locale.toString: language, _COUNTRY, _VARIANT as Java joins them
(\"en_US\", \"en\", \"\" for the root locale).\n"
  ^{:tag (* String)} [^{:tag (* Locale)} t]
  (let [l (.String (.-language t))
        c (.String (.-country t))
        v (.String (.-variant t))
        s l]
    (when (or (!= c "") (and (!= l "") (!= v "")))
      (set! s (+ s "_" c)))
    (when (and (!= v "") (or (!= l "") (!= c "")))
      (set! s (+ s "_" v)))
    (Str s)))

(go/method ToLanguageTag__String
  "ToLanguageTag__String is Locale.toLanguageTag for jrt's locales (\"en-US\", \"und\"): a
variant that is not a well-formed BCP 47 variant becomes the private use x-lvariant-V.\n"
  ^{:tag (* String)} [^{:tag (* Locale)} t]
  (let [l (.String (.-language t))
        c (.String (.-country t))
        v (.String (.-variant t))
        ^{:tag (slice string)} parts nil
        private ""]
    (when (!= l "")
      (set! parts (append parts l)))
    (when (!= c "")
      (set! parts (append parts c)))
    (when (!= v "")
      (if (wellFormedVariant v)
        (set! parts (append parts v))
        (set! private (+ "x-lvariant-" v))))
    (when (and (== l "") (or (> (len parts) 0) (== private "")))
      (set! parts (append (lit (slice string) "und") (spread parts))))
    (when (!= private "")
      (set! parts (append parts private)))
    (Str (strings/Join parts "-"))))

(go/func wellFormedVariant
  "wellFormedVariant: BCP 47's variant, 5 to 8 letters or digits, or 4 beginning with a digit.\n"
  ^bool [^string v]
  (range [_ r v]
    (when (not (or (and (>= r \a) (<= r \z)) (and (>= r \A) (<= r \Z)) (and (>= r \0) (<= r \9))))
      (return false)))
  (or (and (>= (len v) 5) (<= (len v) 8)) (and (== (len v) 4) (>= (aget v 0) \0) (<= (aget v 0) \9))))

(go/method Equals_O__Z ^bool [^{:tag (* Locale)} t ^any o]
  (let [(values x ok) (assert (* Locale) o)]
    (and ok (.Equals_O__Z (.-language t) (.-language x)) (.Equals_O__Z (.-country t) (.-country x))
         (.Equals_O__Z (.-variant t) (.-variant x)))))

(go/method HashCode__I
  "HashCode__I is Locale.hashCode: BaseLocale's hash over the lower-cased language and region
and the variant.\n"
  ^int32 [^{:tag (* Locale)} t]
  (let [h (conv int32 0)]
    (range [_ s (lit (slice (* String)) (.-language t) (.-country t))]
      (range [_ c (.-value s)]
        (when (and (>= c \A) (<= c \Z))
          (set! c (+ c 32)))
        (set! h (+ (* 31 h) (conv int32 c)))))
    (range [_ c (.-value (.-variant t))]
      (set! h (+ (* 31 h) (conv int32 c))))
    h))

(go/method Ref ^any [^{:tag (* Locale)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Locale)} t] Locale_class)
(go/method Is_Cloneable [^{:tag (* Locale)} t])
(go/method Is_Serializable [^{:tag (* Locale)} t])
(go/method Clone__O ^any [^{:tag (* Locale)} t]
  (let [c (addr (lit Locale :language (.-language t) :country (.-country t) :variant (.-variant t)))]
    c))
(go/func Locale_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Locale) x)] ok))
(go/func Locale_Cast ^{:tag (* Locale)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Locale) x)]
    (when (not ok) (panic (ClassCast x Locale_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; java.util.Locale.Category, an enum (§7.13)

(go/type Locale_Category "Locale_Category is the enum java.util.Locale.Category.\n" (struct Enum))

(go/var Locale_Category_class
  (Define (addr (lit ClassInfo :Name "java.util.Locale$Category" :Kind KindEnum
                     :Modifiers (bit-or AccPublic AccStatic AccFinal AccEnum) :Super Enum_class
                     :Declaring Locale_class :Simple "Category" :Go "arbace/jrt.Locale_Category"))))

(go/func newLocaleCategory ^{:tag (* Locale_Category)} [^string n ^int32 o]
  (let [t (addr (lit Locale_Category))]
    (.Ctor_String_I (.-Enum t) t (Intern n) o)
    t))

(go/var
  [^{:tag (* Locale_Category) :doc "Locale_Category_DISPLAY is Locale.Category.DISPLAY.\n"}
   Locale_Category_DISPLAY (newLocaleCategory "DISPLAY" 0)]
  [^{:tag (* Locale_Category)} Locale_Category_FORMAT (newLocaleCategory "FORMAT" 1)])

(go/func Locale_Category_Values__Locale_Category1 ^{:tag (* RefArray)} []
  (RefArrayOf Locale_Category_class Locale_Category_DISPLAY Locale_Category_FORMAT))

(go/func Locale_Category_ValueOf_String__Locale_Category ^{:tag (* Locale_Category)} [^{:tag (* String)} n]
  (assert (* Locale_Category) (Enum_ValueOf_Class_String__Enum Locale_Category_class n)))

(go/method Ref ^any [^{:tag (* Locale_Category)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Locale_Category)} t] Locale_Category_class)
(go/method Clone__O ^any [^{:tag (* Locale_Category)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Locale_Category)} t] (.Impl_ToString__String t t))
(go/method CompareTo_Enum__I ^int32 [^{:tag (* Locale_Category)} t ^Enum_I o] (.Impl_CompareTo_Enum__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* Locale_Category)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* Locale_Category)} t] (.Impl_GetDeclaringClass__Class t t))
(go/func Locale_Category_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Locale_Category) x)] ok))

;; ---------------------------------------------------------------------------------------
;; java.text.DecimalFormatSymbols: the root locale's symbols (en_US's currency)

(go/type DecimalFormatSymbols
  "DecimalFormatSymbols is java.text.DecimalFormatSymbols for the root locale's data.\n"
  (struct Object ^{:tag (* Locale)} locale))

(go/var DecimalFormatSymbols_class
  (Define (addr (lit ClassInfo :Name "java.text.DecimalFormatSymbols" :Kind KindClass
                     :Modifiers AccPublic :Super Object_class
                     :Interfaces (lit (slice (* Class)) Cloneable_class Serializable_class)
                     :Go "arbace/jrt.DecimalFormatSymbols"))))

(go/func DecimalFormatSymbols_New ^{:tag (* DecimalFormatSymbols)} []
  (addr (lit DecimalFormatSymbols :locale Locale_US)))
(go/func DecimalFormatSymbols_New_Locale ^{:tag (* DecimalFormatSymbols)} [^{:tag (* Locale)} l]
  (addr (lit DecimalFormatSymbols :locale (NN l))))
(go/func DecimalFormatSymbols_GetInstance__DecimalFormatSymbols ^{:tag (* DecimalFormatSymbols)} []
  (DecimalFormatSymbols_New))
(go/func DecimalFormatSymbols_GetInstance_Locale__DecimalFormatSymbols
  "DecimalFormatSymbols_GetInstance_Locale__DecimalFormatSymbols is
DecimalFormatSymbols.getInstance(Locale): a new instance (they are mutable in Java).\n"
  ^{:tag (* DecimalFormatSymbols)} [^{:tag (* Locale)} l]
  (DecimalFormatSymbols_New_Locale l))

(go/func DecimalFormatSymbols_GetAvailableLocales__Locale1
  "DecimalFormatSymbols_GetAvailableLocales__Locale1 is DecimalFormatSymbols.getAvailableLocales:
the locales of java.base's CLDR data, the root locale, en and en_US (JRT-NOTES.md, \"Time\").\n"
  ^{:tag (* RefArray)} []
  (RefArrayOf Locale_class Locale_ROOT Locale_ENGLISH Locale_US))

(go/method GetLocale__Locale ^{:tag (* Locale)} [^{:tag (* DecimalFormatSymbols)} t] (.-locale t))
(go/method GetZeroDigit__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \0)
(go/method GetGroupingSeparator__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \,)
(go/method GetDecimalSeparator__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \.)
(go/method GetMonetaryDecimalSeparator__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \.)
(go/method GetMinusSign__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \-)
(go/method GetPercent__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \%)
(go/method GetPerMill__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] 0x2030)
(go/method GetDigit__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \#)
(go/method GetPatternSeparator__C ^uint16 [^{:tag (* DecimalFormatSymbols)} t] \;)
(go/method GetInfinity__String ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t] (Intern "∞"))
(go/method GetNaN__String ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t] (Intern "NaN"))
(go/method GetExponentSeparator__String ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t] (Intern "E"))
(go/method GetCurrencySymbol__String
  "GetCurrencySymbol__String: $ for the United States, the generic ¤ otherwise.\n"
  ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t]
  (when (== (.String (.-country (.-locale t))) "US")
    (return (Intern "$")))
  (Intern "¤"))
(go/method GetInternationalCurrencySymbol__String ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t]
  (when (== (.String (.-country (.-locale t))) "US")
    (return (Intern "USD")))
  (Intern "XXX"))

(go/method Ref ^any [^{:tag (* DecimalFormatSymbols)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* DecimalFormatSymbols)} t] DecimalFormatSymbols_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* DecimalFormatSymbols)} t] (Object_toString t))
(go/method Is_Cloneable [^{:tag (* DecimalFormatSymbols)} t])
(go/method Is_Serializable [^{:tag (* DecimalFormatSymbols)} t])
(go/method Clone__O ^any [^{:tag (* DecimalFormatSymbols)} t] (DecimalFormatSymbols_New_Locale (.-locale t)))
(go/method Equals_O__Z ^bool [^{:tag (* DecimalFormatSymbols)} t ^any o]
  (let [(values x ok) (assert (* DecimalFormatSymbols) o)]
    (and ok (.Equals_O__Z (.-locale t) (.-locale x)))))
(go/method HashCode__I ^int32 [^{:tag (* DecimalFormatSymbols)} t] (+ (* 37 \0) \,))

;; ---------------------------------------------------------------------------------------
;; String's Locale overloads: the root locale's case mapping for every locale (no Turkish,
;; Azerbaijani or Lithuanian rules: JRT-NOTES.md, "Locales")

(go/method ToLowerCase_Locale__String ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* Locale)} l]
  (NN l)
  (.ToLowerCase__String t))

(go/method ToUpperCase_Locale__String ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* Locale)} l]
  (NN l)
  (.ToUpperCase__String t))

(go/func init []
  (set! (.-IsInstance (.Info Locale_class)) Locale_InstanceOf)
  (set! (.-IsInstance (.Info Locale_Category_class)) Locale_Category_InstanceOf)
  (set! (.-Enum (.Info Locale_Category_class)) Locale_Category_Values__Locale_Category1))
