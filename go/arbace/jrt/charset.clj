;; jrt: java.nio.charset.Charset with jrt's three charsets, sun.nio.cs.UTF_8, ISO_8859_1 and
;; US_ASCII (their names and aliases as JDK 26's), java.nio.charset.StandardCharsets, and
;; String's Charset overloads (JRT-SOURCES.md, the shim edge; doc/go/JRT-NOTES.md, "Phase 2b").
;; The coding itself is phase 1's (codec.clj: DecodeUTF8, EncodeUTF8, DecodeBytes,
;; EncodeBytes). Encoders, decoders and buffers (java.nio) are cut.
(in-ns 'go.arbace.jrt)

(go/file "charset.go"
  :imports [[strings "strings"]])

(go/type Charset_I
  "Charset_I is java.nio.charset.Charset's class interface.\n"
  (interface Object_I
    (Self_Charset ^{:tag (* Charset)} [])
    (Is_Comparable [])
    (CompareTo_O__I ^int32 [^any o])
    (Name__String ^{:tag (* String)} [])
    (DisplayName__String ^{:tag (* String)} [])
    (DisplayName_Locale__String ^{:tag (* String)} [^{:tag (* Locale)} l])
    (IsRegistered__Z ^bool [])
    (CanEncode__Z ^bool [])
    (Contains_Charset__Z ^bool [^Charset_I cs])
    (CompareTo_Charset__I ^int32 [^Charset_I cs])))

(go/type Charset
  "Charset is java.nio.charset.Charset (abstract): its canonical name, its aliases (Go
strings, as JDK 26 lists them) and the coding phase 1 implements (csUTF8, csLatin1,
csASCII).\n"
  (struct Object ^{:tag (* String)} name ^{:tag (slice string)} aliases ^int kind))

(go/var Charset_class
  (Define (addr (lit ClassInfo :Name "java.nio.charset.Charset" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Comparable_class)
                     :Go "arbace/jrt.Charset"))))

(go/method Self_Charset ^{:tag (* Charset)} [^{:tag (* Charset)} t] t)
(go/method Is_Comparable [^{:tag (* Charset)} t])
(go/method Name__String "Name__String is Charset.name: the canonical name.\n"
  ^{:tag (* String)} [^{:tag (* Charset)} t] (.-name t))
(go/method DisplayName__String ^{:tag (* String)} [^{:tag (* Charset)} t] (.-name t))
(go/method DisplayName_Locale__String ^{:tag (* String)} [^{:tag (* Charset)} t ^{:tag (* Locale)} l] (.-name t))
(go/method IsRegistered__Z ^bool [^{:tag (* Charset)} t] true)
(go/method CanEncode__Z ^bool [^{:tag (* Charset)} t] true)
(go/method Contains_Charset__Z
  "Contains_Charset__Z is Charset.contains: UTF-8 contains all three, ISO-8859-1 itself and
US-ASCII, US-ASCII itself.\n"
  ^bool [^{:tag (* Charset)} t ^Charset_I cs]
  (let [o (.-kind (.Self_Charset cs))]
    (or (== (.-kind t) csUTF8) (== o csASCII) (== o (.-kind t)))))
(go/method ToString__String "ToString__String is Charset.toString: the canonical name.\n"
  ^{:tag (* String)} [^{:tag (* Charset)} t] (.-name t))
(go/method Equals_O__Z "Equals_O__Z is Charset.equals: the same canonical name.\n"
  ^bool [^{:tag (* Charset)} t ^any o]
  (let [(values c ok) (assert Charset_I o)]
    (and ok (.Equals_O__Z (.-name t) (.-name (.Self_Charset c))))))
(go/method HashCode__I ^int32 [^{:tag (* Charset)} t] (.HashCode__I (.-name t)))
(go/method CompareTo_Charset__I
  "CompareTo_Charset__I is Charset.compareTo: the names compared ignoring case.\n"
  ^int32 [^{:tag (* Charset)} t ^Charset_I cs]
  (.CompareToIgnoreCase_String__I (.-name t) (.-name (.Self_Charset cs))))
(go/method CompareTo_O__I ^int32 [^{:tag (* Charset)} t ^any o]
  (.CompareTo_Charset__I t (Charset_Cast o)))

(go/method Aliases "Aliases are the charset's aliases, as Go strings.\n"
  ^{:tag (slice string)} [^{:tag (* Charset)} t] (.-aliases t))

(go/func Charset_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Charset_I x)] ok))
(go/func Charset_Cast ^Charset_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Charset_I x)]
    (when (not ok) (panic (ClassCast x Charset_class)))
    v))

;; ---- sun.nio.cs.Unicode, UTF_8, ISO_8859_1, US_ASCII

(go/type Unicode "Unicode is sun.nio.cs.Unicode (abstract), UTF_8's superclass.\n" (struct Charset))
(go/type Unicode_I (interface Charset_I (Self_Unicode ^{:tag (* Unicode)} [])))
(go/method Self_Unicode ^{:tag (* Unicode)} [^{:tag (* Unicode)} t] t)
(go/var Unicode_class
  (Define (addr (lit ClassInfo :Name "sun.nio.cs.Unicode" :Kind KindClass
                     :Modifiers AccAbstract :Super Charset_class
                     :Go "arbace/jrt.Unicode"))))

(go/type UTF_8 "UTF_8 is sun.nio.cs.UTF_8.\n" (struct Unicode))
(go/type ISO_8859_1 "ISO_8859_1 is sun.nio.cs.ISO_8859_1.\n" (struct Charset))
(go/type US_ASCII "US_ASCII is sun.nio.cs.US_ASCII.\n" (struct Charset))

(go/var
  [UTF_8_class
   (Define (addr (lit ClassInfo :Name "sun.nio.cs.UTF_8" :Kind KindClass
                      :Modifiers (bit-or AccPublic AccFinal) :Super Unicode_class :Go "arbace/jrt.UTF_8")))]
  [ISO_8859_1_class
   (Define (addr (lit ClassInfo :Name "sun.nio.cs.ISO_8859_1" :Kind KindClass
                      :Modifiers (bit-or AccPublic AccFinal) :Super Charset_class :Go "arbace/jrt.ISO_8859_1")))]
  [US_ASCII_class
   (Define (addr (lit ClassInfo :Name "sun.nio.cs.US_ASCII" :Kind KindClass
                      :Modifiers (bit-or AccPublic AccFinal) :Super Charset_class :Go "arbace/jrt.US_ASCII")))])

(go/var
  [^{:tag (* UTF_8) :doc "UTF_8_INSTANCE is sun.nio.cs.UTF_8.INSTANCE.\n"} UTF_8_INSTANCE
   (addr (lit UTF_8 :Unicode (lit Unicode :Charset (lit Charset :name (Intern "UTF-8") :kind csUTF8
                                                       :aliases (lit (slice string) "UTF8" "unicode-1-1-utf-8")))))]
  [^{:tag (* ISO_8859_1)} ISO_8859_1_INSTANCE
   (addr (lit ISO_8859_1 :Charset (lit Charset :name (Intern "ISO-8859-1") :kind csLatin1
                                       :aliases (lit (slice string) "819" "8859_1" "IBM-819" "IBM819" "ISO8859-1"
                                                     "ISO8859_1" "ISO_8859-1" "ISO_8859-1:1987" "ISO_8859_1"
                                                     "cp819" "csISOLatin1" "iso-ir-100" "l1" "latin1"))))]
  [^{:tag (* US_ASCII)} US_ASCII_INSTANCE
   (addr (lit US_ASCII :Charset (lit Charset :name (Intern "US-ASCII") :kind csASCII
                                     :aliases (lit (slice string) "646" "ANSI_X3.4-1968" "ANSI_X3.4-1986" "ASCII"
                                                   "IBM367" "ISO646-US" "ISO_646.irv:1991" "ascii7" "cp367"
                                                   "csASCII" "iso-ir-6" "iso_646.irv:1983" "us"))))])

(go/method Ref ^any [^{:tag (* UTF_8)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* UTF_8)} t] UTF_8_class)
(go/method Clone__O ^any [^{:tag (* UTF_8)} t] (panic (CloneNotSupported t)))
(go/method Ref ^any [^{:tag (* ISO_8859_1)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* ISO_8859_1)} t] ISO_8859_1_class)
(go/method Clone__O ^any [^{:tag (* ISO_8859_1)} t] (panic (CloneNotSupported t)))
(go/method Ref ^any [^{:tag (* US_ASCII)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* US_ASCII)} t] US_ASCII_class)
(go/method Clone__O ^any [^{:tag (* US_ASCII)} t] (panic (CloneNotSupported t)))

(go/func UTF_8_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* UTF_8) x)] ok))
(go/func ISO_8859_1_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* ISO_8859_1) x)] ok))
(go/func US_ASCII_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* US_ASCII) x)] ok))

(go/var ^{:tag (slice Charset_I)} allCharsets
  (lit (slice Charset_I) UTF_8_INSTANCE ISO_8859_1_INSTANCE US_ASCII_INSTANCE))

;; ---- lookup by name

(go/func legalCharsetName
  "legalCharsetName is Charset.checkName's rule: not empty; letters, digits and - + : _ . ,
the first a letter or a digit.\n"
  ^bool [^string n]
  (when (== (len n) 0)
    (return false))
  (range [i c n]
    (let [ok (or (and (>= c \A) (<= c \Z)) (and (>= c \a) (<= c \z)) (and (>= c \0) (<= c \9))
                 (and (> i 0) (or (== c \-) (== c \+) (== c \:) (== c \_) (== c \.))))]
      (when (not ok)
        (return false))))
  true)

(go/func lookupCharset
  "lookupCharset finds a charset by its canonical name or an alias, ignoring case; nil when
jrt has no such charset.\n"
  ^Charset_I [^string n]
  (range [_ cs allCharsets]
    (let [c (.Self_Charset cs)]
      (when (strings/EqualFold (.String (.-name c)) n)
        (return cs))
      (range [_ a (.-aliases c)]
        (when (strings/EqualFold a n)
          (return cs)))))
  nil)

(go/func checkedLookup
  "checkedLookup is Charset.lookup with its checks: IllegalArgumentException(\"Null charset
name\") for null, IllegalCharsetNameException for an illegal name.\n"
  ^Charset_I [^{:tag (* String)} name]
  (when (== name nil)
    (panic (IllegalArgumentException_New_String (Str "Null charset name"))))
  (let [n (.String name)
        cs (lookupCharset n)]
    (when (!= cs nil)
      (return cs))
    (when (not (legalCharsetName n))
      (panic (IllegalCharsetNameException_New_String name)))
    nil))

(go/func Charset_ForName_String__Charset
  "Charset_ForName_String__Charset is Charset.forName: UnsupportedCharsetException for a legal
name jrt does not have (V9: UTF-8, ISO-8859-1 and US-ASCII only).\n"
  ^Charset_I [^{:tag (* String)} name]
  (let [cs (checkedLookup name)]
    (when (== cs nil)
      (panic (UnsupportedCharsetException_New_String name)))
    cs))

(go/func Charset_ForName_String_Charset__Charset
  "Charset_ForName_String_Charset__Charset is Charset.forName(name, fallback).\n"
  ^Charset_I [^{:tag (* String)} name ^Charset_I fallback]
  (let [cs (checkedLookup name)]
    (when (== cs nil)
      (return fallback))
    cs))

(go/func Charset_IsSupported_String__Z ^bool [^{:tag (* String)} name]
  (!= (checkedLookup name) nil))

(go/func Charset_DefaultCharset__Charset
  "Charset_DefaultCharset__Charset is Charset.defaultCharset: UTF-8 (JEP 400).\n"
  ^Charset_I []
  UTF_8_INSTANCE)

;; ---- java.nio.charset.StandardCharsets

(go/var
  [^{:tag (* Class)} StandardCharsets_class
   (Define (addr (lit ClassInfo :Name "java.nio.charset.StandardCharsets" :Kind KindClass
                      :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                      :Go "arbace/jrt.StandardCharsets")))]
  [^{:tag Charset_I :doc "StandardCharsets_UTF_8 is StandardCharsets.UTF_8.\n"} StandardCharsets_UTF_8 UTF_8_INSTANCE]
  [^Charset_I StandardCharsets_ISO_8859_1 ISO_8859_1_INSTANCE]
  [^Charset_I StandardCharsets_US_ASCII US_ASCII_INSTANCE])

;; ---- String's Charset overloads

(go/func String_New_B1_Charset
  "String_New_B1_Charset is new String(byte[], Charset): malformed input replaced as the
charset's decoder replaces it.\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^Charset_I cs]
  (NN b)
  (newString (decodeWith (.-A b) (.Self_Charset cs))))

(go/func String_New_B1_I_I_Charset
  "String_New_B1_I_I_Charset is new String(byte[], int offset, int length, Charset).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 offset ^int32 length ^Charset_I cs]
  (when (== cs nil)
    (panic (NPE)))
  (checkFromSize offset length (len (.-A (NN b))))
  (newString (decodeWith (subslice (.-A b) offset (+ offset length)) (.Self_Charset cs))))

(go/method GetBytes_Charset__B1
  "GetBytes_Charset__B1 is String.getBytes(Charset): unmappable characters as '?'.\n"
  ^{:tag (* ByteArray)} [^{:tag (* String)} t ^Charset_I cs]
  (ByteArrayOf (spread (EncodeBytes (.-value t) (.-name (.Self_Charset cs))))))

(go/func decodeWith ^{:tag (slice uint16)} [^{:tag (slice int8)} b ^{:tag (* Charset)} c]
  (DecodeBytes b (.-name c)))
