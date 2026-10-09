;; jrt: java.lang.String over UTF-16 (C2G-SPEC §7.5; decision D4).
(in-ns 'go.arbace.jrt)

(go/file "string.go"
  :imports [[strconv "strconv"] [sync "sync"] [atomic "sync/atomic"] [utf8 "unicode/utf8"]])

(go/type String
  "String is java.lang.String (final: *String): immutable UTF-16 code units, the hash code
cached as Java's (computed on first use; 0 recomputed).\n"
  (struct Object ^{:tag (slice uint16)} value ^int32 hash))

(go/var String_class
  (Define (addr (lit ClassInfo :Name "java.lang.String" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Serializable_class Comparable_class CharSequence_class
                                       Constable_class ConstantDesc_class)
                     :Go "arbace/jrt.String"))))

(go/const ^{:tag bool :val true
            :doc "String_COMPACT_STRINGS is String.COMPACT_STRINGS: true, so the translated
classes build Latin-1 byte arrays (StringLatin1) where Java's would.\n"}
  String_COMPACT_STRINGS true)

;; ---------------------------------------------------------------------------------------
;; Go's side: making and reading Strings

(go/func newString "newString wraps v, which the caller gives up.\n"
  ^{:tag (* String)} [^{:tag (slice uint16)} v]
  (addr (lit String :value v)))

(go/func NewStringUTF16 "NewStringUTF16 is a String of a copy of the code units v.\n"
  ^{:tag (* String)} [^{:tag (slice uint16)} v]
  (let [c (make (slice uint16) (len v))]
    (copy c v)
    (newString c)))

(go/func appendUTF8 "appendUTF8 appends the UTF-16 of Go's UTF-8 s (U+FFFD for invalid bytes).\n"
  ^{:tag (slice uint16)} [^{:tag (slice uint16)} b ^string s]
  (for [i 0] (< i (len s)) _
    (let [c (aget s i)]
      (when (< c utf8/RuneSelf)
        (set! b (append b (conv uint16 c)))
        (inc! i)
        (continue))
      (let [(values r n) (utf8/DecodeRuneInString (subslice s i))]
        (set! i (+ i n))
        (set! b (appendCodePoint b r)))))
  b)

(go/func appendCodePoint ^{:tag (slice uint16)} [^{:tag (slice uint16)} b ^rune r]
  (when (< r 0x10000)
    (return (append b (conv uint16 r))))
  (let [r2 (- r 0x10000)]
    (append b (conv uint16 (+ 0xD800 (>> r2 10))) (conv uint16 (+ 0xDC00 (bit-and r2 0x3FF))))))

(go/func Str "Str is a new String of Go's UTF-8 text s (invalid bytes become U+FFFD).\n"
  ^{:tag (* String)} [^string s]
  (newString (appendUTF8 (make (slice uint16) 0 (len s)) s)))

(go/method String
  "String is the text as Go's UTF-8 (unpaired surrogates become U+FFFD); it makes *String a
fmt.Stringer.\n"
  ^string [^{:tag (* String)} t]
  (when (== t nil)
    (return "null"))
  (let [v (.-value t)
        b (make (slice byte) 0 (len v))]
    (for [i 0] (< i (len v)) (inc! i)
      (let [c (aget v i)]
        (cond
          (< c 0x80) (set! b (append b (conv byte c)))
          (and (isHighSurrogate c) (< (+ i 1) (len v)) (isLowSurrogate (aget v (+ i 1))))
          (do
            (set! b (utf8/AppendRune b (toCodePoint c (aget v (+ i 1)))))
            (inc! i))
          :else (set! b (utf8/AppendRune b (conv rune c))))))
    (conv string b)))

(go/method UTF16 "UTF16 returns the code units, which the caller must not modify.\n"
  ^{:tag (slice uint16)} [^{:tag (* String)} t]
  (.-value t))

(go/func isHighSurrogate ^bool [^uint16 c] (and (>= c 0xD800) (<= c 0xDBFF)))
(go/func isLowSurrogate ^bool [^uint16 c] (and (>= c 0xDC00) (<= c 0xDFFF)))
(go/func isSurrogate ^bool [^uint16 c] (and (>= c 0xD800) (<= c 0xDFFF)))
(go/func toCodePoint ^rune [^uint16 hi ^uint16 lo]
  (+ (<< (- (conv rune hi) 0xD800) 10) (- (conv rune lo) 0xDC00) 0x10000))

(go/func validUTF16 ^bool [^{:tag (slice uint16)} v]
  (for [i 0] (< i (len v)) (inc! i)
    (let [c (aget v i)]
      (cond
        (isHighSurrogate c)
        (do
          (when (or (>= (+ i 1) (len v)) (not (isLowSurrogate (aget v (+ i 1)))))
            (return false))
          (inc! i))
        (isLowSurrogate c) (return false))))
  true)

;; ---------------------------------------------------------------------------------------
;; Interning (§7.5): one table for literals and String.intern

(go/var ^{:tag sync/RWMutex} internMu)
(go/var ^{:tag (map string (* String))} internTable (make (map string (* String))))

(go/func internKey
  "internKey: the text as UTF-8 when the code units are valid UTF-16, else the raw code units
behind a 0xFF byte (which UTF-8 never contains).\n"
  ^string [^{:tag (slice uint16)} v]
  (when (validUTF16 v)
    (return (.String (newString v))))
  (let [b (make (slice byte) 1 (+ 1 (* 2 (len v))))]
    (aset b 0 0xFF)
    (range [_ c v]
      (set! b (append b (conv byte c) (conv byte (>> c 8)))))
    (conv string b)))

(go/func internAt ^{:tag (* String)} [^string key ^{:tag (func [] [(* String)])} mk]
  (.RLock internMu)
  (let [(values s ok) (aget internTable key)]
    (.RUnlock internMu)
    (when ok
      (return s)))
  (.Lock internMu)
  (let [(values s ok) (aget internTable key)]
    (when (not ok)
      (set! s (mk))
      (aset internTable key s))
    (.Unlock internMu)
    s))

(go/func Intern
  "Intern is the interned String of Go's UTF-8 s: string literals (c2g's pool, §7.5) and
String.intern share one table, so equal literals are one object.\n"
  ^{:tag (* String)} [^string s]
  (internAt s (fn ^{:tag (* String)} [] (Str s))))

(go/func InternUTF16
  "InternUTF16 is the interned String of the code units v (literals with unpaired surrogates,
which a Go string cannot hold).\n"
  ^{:tag (* String)} [^{:tag (slice uint16)} v]
  (internAt (internKey v) (fn ^{:tag (* String)} [] (NewStringUTF16 v))))

(go/method Intern__String ^{:tag (* String)} [^{:tag (* String)} t]
  (internAt (internKey (.-value t)) (fn ^{:tag (* String)} [] t)))

;; ---------------------------------------------------------------------------------------
;; Concatenation and string conversion (§7.5, JLS 5.1.11)

(go/func Concat
  "Concat is the string concatenation of java-str: its operands, already converted to Strings
(StrOf...), joined in one allocation.\n"
  ^{:tag (* String)} [& ^{:tag (slice (* String))} parts]
  (let [n 0]
    (range [_ p parts]
      (if (== p nil)
        (set! n (+ n 4))
        (set! n (+ n (len (.-value p))))))
    (let [v (make (slice uint16) 0 n)]
      (range [_ p parts]
        (if (== p nil)
          (set! v (appendUTF8 v "null"))
          (set! v (append v (spread (.-value p))))))
      (newString v))))

(go/var [litNull (Intern "null")] [litTrue (Intern "true")] [litFalse (Intern "false")]
        [litEmpty (Intern "")])

(go/func StrOfObj
  "StrOfObj is String.valueOf(Object): \"null\" for null, else toString() (\"null\" again
if that returns null).\n"
  ^{:tag (* String)} [^any x]
  (when (== x nil)
    (return litNull))
  (let [s (.ToString__String (asObject x))]
    (when (== s nil)
      (return litNull))
    s))

(go/func StrOfInt "StrOfInt is String.valueOf(int) (byte and short too).\n"
  ^{:tag (* String)} [^int32 i]
  (let [^{:tag (array 12 byte)} buf (zero (array 12 byte))]
    (latin1String (strconv/AppendInt (subslice buf _ 0) (conv int64 i) 10))))

(go/func StrOfLong "StrOfLong is String.valueOf(long).\n"
  ^{:tag (* String)} [^int64 i]
  (let [^{:tag (array 20 byte)} buf (zero (array 20 byte))]
    (latin1String (strconv/AppendInt (subslice buf _ 0) i 10))))

(go/func StrOfChar "StrOfChar is String.valueOf(char).\n"
  ^{:tag (* String)} [^uint16 c]
  (newString (lit (slice uint16) c)))

(go/func StrOfBool "StrOfBool is String.valueOf(boolean): the literals \"true\" and \"false\".\n"
  ^{:tag (* String)} [^bool b]
  (when b
    (return litTrue))
  litFalse)

(go/func StrOfFloat "StrOfFloat is String.valueOf(float): Float.toString.\n"
  ^{:tag (* String)} [^float32 f]
  (Str (FormatFloat f)))

(go/func StrOfDouble "StrOfDouble is String.valueOf(double): Double.toString.\n"
  ^{:tag (* String)} [^float64 d]
  (Str (FormatDouble d)))

(go/func latin1String ^{:tag (* String)} [^{:tag (slice byte)} b]
  (let [v (make (slice uint16) (len b))]
    (range [i c b]
      (aset v i (conv uint16 c)))
    (newString v)))

;; ---------------------------------------------------------------------------------------
;; Bounds checks with Java's exceptions (jdk.internal.util.Preconditions' messages)

(go/func sioobeIndex ^Throwable_I [^int32 i ^int n]
  (StringIndexOutOfBoundsException_New_String
    (Str (+ "Index " (strconv/Itoa (conv int i)) " out of bounds for length " (strconv/Itoa n)))))

(go/func sioobeRange ^Throwable_I [^int32 a ^int32 b ^int n]
  (StringIndexOutOfBoundsException_New_String
    (Str (+ "Range [" (strconv/Itoa (conv int a)) ", " (strconv/Itoa (conv int b))
            ") out of bounds for length " (strconv/Itoa n)))))

(go/func sioobeSize ^Throwable_I [^int32 a ^int32 s ^int n]
  (StringIndexOutOfBoundsException_New_String
    (Str (+ "Range [" (strconv/Itoa (conv int a)) ", " (strconv/Itoa (conv int a)) " + "
            (strconv/Itoa (conv int s)) ") out of bounds for length " (strconv/Itoa n)))))

(go/func checkIndex [^int32 i ^int n]
  (when (>= (conv uint32 i) (conv uint32 n))
    (panic (sioobeIndex i n))))

(go/func checkFromTo [^int32 a ^int32 b ^int n]
  (when (or (< a 0) (> a b) (> (conv int b) n))
    (panic (sioobeRange a b n))))

(go/func checkFromSize [^int32 a ^int32 s ^int n]
  (when (or (< a 0) (< s 0) (> (conv int64 a) (- (conv int64 n) (conv int64 s))))
    (panic (sioobeSize a s n))))

;; ---------------------------------------------------------------------------------------
;; Constructors (String is final: String_New_..., C2G-SPEC §4.4)

(go/func String_New "String_New is new String().\n" ^{:tag (* String)} []
  (newString (lit (slice uint16))))

(go/func String_New_String ^{:tag (* String)} [^{:tag (* String)} s]
  (newString (.-value (NN s))))

(go/func String_New_C1 ^{:tag (* String)} [^{:tag (* CharArray)} a]
  (NewStringUTF16 (.-A (NN a))))

(go/func String_New_C1_I_I ^{:tag (* String)} [^{:tag (* CharArray)} a ^int32 offset ^int32 count]
  (checkFromSize offset count (len (.-A (NN a))))
  (NewStringUTF16 (subslice (.-A a) offset (+ offset count))))

(go/func String_New_I1_I_I "String_New_I1_I_I is new String(int[] codePoints, offset, count).\n"
  ^{:tag (* String)} [^{:tag (* IntArray)} a ^int32 offset ^int32 count]
  (checkFromSize offset count (len (.-A (NN a))))
  (let [v (make (slice uint16) 0 count)]
    (range [_ cp (subslice (.-A a) offset (+ offset count))]
      (when (or (< cp 0) (> cp 0x10FFFF))
        (panic (IllegalArgumentException_New_String (Str (strconv/Itoa (conv int cp))))))
      (set! v (appendCodePoint v cp)))
    (newString v)))

(go/func String_New_B1 "String_New_B1 is new String(byte[]): UTF-8, the default charset.\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b]
  (newString (DecodeUTF8 (.-A (NN b)))))

(go/func String_New_B1_I_I ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 offset ^int32 length]
  (checkFromSize offset length (len (.-A (NN b))))
  (newString (DecodeUTF8 (subslice (.-A b) offset (+ offset length)))))

(go/func String_New_B1_I_I_I
  "String_New_B1_I_I_I is the deprecated new String(byte[] ascii, int hibyte, int offset,
int count).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 hibyte ^int32 offset ^int32 count]
  (checkFromSize offset count (len (.-A (NN b))))
  (let [v (make (slice uint16) count)
        hi (<< (conv uint16 (bit-and hibyte 0xff)) 8)]
    (range [i c (subslice (.-A b) offset (+ offset count))]
      (aset v i (bit-or hi (conv uint16 (conv uint8 c)))))
    (newString v)))

(go/func String_New_B1_I
  "String_New_B1_I is the deprecated new String(byte[] ascii, int hibyte).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 hibyte]
  (String_New_B1_I_I_I b hibyte 0 (conv int32 (len (.-A (NN b))))))

(go/func String_New_B1_I_I_String
  "String_New_B1_I_I_String is new String(byte[], offset, length, String charsetName), for
the charsets jrt knows (DecodeBytes).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 offset ^int32 length ^{:tag (* String)} charset]
  (NN charset)
  (checkFromSize offset length (len (.-A (NN b))))
  (newString (DecodeBytes (subslice (.-A b) offset (+ offset length)) charset)))

(go/func String_New_B1_String ^{:tag (* String)} [^{:tag (* ByteArray)} b ^{:tag (* String)} charset]
  (NN charset)
  (newString (DecodeBytes (.-A (NN b)) charset)))

(go/func String_New_StringBuilder ^{:tag (* String)} [^{:tag (* StringBuilder)} sb]
  (.ToString__String (NN sb)))

(go/func String_New_StringBuffer ^{:tag (* String)} [^{:tag (* StringBuffer)} sb]
  (.ToString__String (NN sb)))

(go/func String_NewStringWithLatin1Bytes_B1__String
  "String_NewStringWithLatin1Bytes_B1__String is String.newStringWithLatin1Bytes (package-private).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b]
  (latin1Bytes (.-A b)))

(go/func latin1Bytes ^{:tag (* String)} [^{:tag (slice int8)} b]
  (let [v (make (slice uint16) (len b))]
    (range [i c b]
      (aset v i (conv uint16 (conv uint8 c))))
    (newString v)))

(go/func StringLatin1_NewString_B1_I_I__String
  "StringLatin1_NewString_B1_I_I__String is StringLatin1.newString(byte[], int, int).\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 off ^int32 n]
  (latin1Bytes (subslice (.-A b) off (+ off n))))

(go/func StringUTF16_NewString_B1_I_I__String
  "StringUTF16_NewString_B1_I_I__String is StringUTF16.newString(byte[] value, int index, int
len): code units stored two bytes each, little-endian (jrt's choice, as StringUTF16.putChar
writes them on amd64 and arm64), index and len counted in code units.\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} b ^int32 index ^int32 n]
  (let [v (make (slice uint16) n)]
    (range [i _ v]
      (let [j (* 2 (+ (conv int index) i))]
        (aset v i (bit-or (conv uint16 (conv uint8 (aget (.-A b) j)))
                          (<< (conv uint16 (conv uint8 (aget (.-A b) (+ j 1)))) 8)))))
    (newString v)))

(go/func StringUTF16_PutChar_B1_I_I__V "StringUTF16_PutChar_B1_I_I__V is StringUTF16.putChar.\n"
  [^{:tag (* ByteArray)} b ^int32 index ^int32 c]
  (let [j (* 2 index)]
    (aset (.-A b) j (conv int8 c))
    (aset (.-A b) (+ j 1) (conv int8 (>> c 8)))))

;; ---------------------------------------------------------------------------------------
;; The generated members (§2) and Object's methods

(go/method Ref ^any [^{:tag (* String)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* String)} t] String_class)
(go/method Clone__O ^any [^{:tag (* String)} t] (panic (CloneNotSupported t)))
(go/method Is_Serializable [^{:tag (* String)} t])
(go/method Is_Comparable [^{:tag (* String)} t])
(go/method Is_CharSequence [^{:tag (* String)} t])
(go/method Is_Constable [^{:tag (* String)} t])
(go/method Is_ConstantDesc [^{:tag (* String)} t])
(go/method ToString__String ^{:tag (* String)} [^{:tag (* String)} t] t)

(go/method HashCode__I "HashCode__I is s[0]*31^(n-1) + ... + s[n-1], cached.\n"
  ^int32 [^{:tag (* String)} t]
  (let [h (atomic/LoadInt32 (addr (.-hash t)))]
    (when (and (== h 0) (> (len (.-value t)) 0))
      (range [_ c (.-value t)]
        (set! h (+ (* 31 h) (conv int32 c))))
      (atomic/StoreInt32 (addr (.-hash t)) h))
    h))

(go/func equalUnits ^bool [^{:tag (slice uint16)} a ^{:tag (slice uint16)} b]
  (when (!= (len a) (len b))
    (return false))
  (range [i c a]
    (when (!= c (aget b i))
      (return false)))
  true)

(go/method Equals_O__Z ^bool [^{:tag (* String)} t ^any x]
  (let [(values s ok) (assert (* String) x)]
    (when (or (not ok) (== s nil))
      (return false))
    (or (== s t) (equalUnits (.-value t) (.-value s)))))

;; ---------------------------------------------------------------------------------------
;; CharSequence

(go/method Length__I ^int32 [^{:tag (* String)} t] (conv int32 (len (.-value t))))
(go/method IsEmpty__Z ^bool [^{:tag (* String)} t] (== (len (.-value t)) 0))

(go/method CharAt_I__C ^uint16 [^{:tag (* String)} t ^int32 i]
  (checkIndex i (len (.-value t)))
  (aget (.-value t) i))

(go/method SubSequence_I_I__CharSequence ^CharSequence [^{:tag (* String)} t ^int32 a ^int32 b]
  (.Substring_I_I__String t a b))

(go/method Substring_I__String ^{:tag (* String)} [^{:tag (* String)} t ^int32 a]
  (.Substring_I_I__String t a (conv int32 (len (.-value t)))))

(go/method Substring_I_I__String ^{:tag (* String)} [^{:tag (* String)} t ^int32 a ^int32 b]
  (let [n (len (.-value t))]
    (checkFromTo a b n)
    (when (and (== a 0) (== (conv int b) n))
      (return t))
    (NewStringUTF16 (subslice (.-value t) a b))))

;; ---------------------------------------------------------------------------------------
;; Code points

(go/func codePointAt ^rune [^{:tag (slice uint16)} v ^int i ^int limit]
  (let [c (aget v i)]
    (when (and (isHighSurrogate c) (< (+ i 1) limit) (isLowSurrogate (aget v (+ i 1))))
      (return (toCodePoint c (aget v (+ i 1)))))
    (conv rune c)))

(go/func codePointBefore ^rune [^{:tag (slice uint16)} v ^int i ^int start]
  (let [c (aget v (- i 1))]
    (when (and (isLowSurrogate c) (> (- i 1) start) (isHighSurrogate (aget v (- i 2))))
      (return (toCodePoint (aget v (- i 2)) c)))
    (conv rune c)))

(go/func codePointCount ^int32 [^{:tag (slice uint16)} v]
  (let [n (conv int32 (len v))]
    (for [i 0] (< i (- (len v) 1)) (inc! i)
      (when (and (isHighSurrogate (aget v i)) (isLowSurrogate (aget v (+ i 1))))
        (dec! n)
        (inc! i)))
    n))

(go/func charCount ^int [^rune cp]
  (when (>= cp 0x10000)
    (return 2))
  1)

(go/method CodePointAt_I__I ^int32 [^{:tag (* String)} t ^int32 i]
  (checkIndex i (len (.-value t)))
  (codePointAt (.-value t) (conv int i) (len (.-value t))))

(go/method CodePointBefore_I__I ^int32 [^{:tag (* String)} t ^int32 i]
  (let [j (- i 1)]
    (checkIndex j (len (.-value t))))
  (codePointBefore (.-value t) (conv int i) 0))

(go/method CodePointCount_I_I__I
  "CodePointCount_I_I__I: IndexOutOfBoundsException (Preconditions without formatter), not
StringIndexOutOfBoundsException, as Java's.\n"
  ^int32 [^{:tag (* String)} t ^int32 a ^int32 b]
  (when (or (< a 0) (> a b) (> (conv int b) (len (.-value t))))
    (panic (IndexOutOfBoundsException_New_String (.GetMessage__String (sioobeRange a b (len (.-value t)))))))
  (codePointCount (subslice (.-value t) a b)))

(go/method OffsetByCodePoints_I_I__I ^int32 [^{:tag (* String)} t ^int32 index ^int32 offset]
  (offsetByCodePoints (.-value t) index offset))

(go/func offsetByCodePoints ^int32 [^{:tag (slice uint16)} v ^int32 index ^int32 offset]
  (let [n (conv int32 (len v))]
    (when (or (< index 0) (> index n))
      (panic (IndexOutOfBoundsException_New)))
    (let [x index]
      (if (>= offset 0)
        (for [i (conv int32 0)] (< i offset) (inc! i)
          (when (>= x n)
            (panic (IndexOutOfBoundsException_New)))
          (if (and (isHighSurrogate (aget v x)) (< (+ x 1) n) (isLowSurrogate (aget v (+ x 1))))
            (set! x (+ x 2))
            (inc! x)))
        (for [i offset] (< i 0) (inc! i)
          (when (<= x 0)
            (panic (IndexOutOfBoundsException_New)))
          (dec! x)
          (when (and (isLowSurrogate (aget v x)) (> x 0) (isHighSurrogate (aget v (- x 1))))
            (dec! x))))
      x)))

;; ---------------------------------------------------------------------------------------
;; Copying out

(go/method GetChars_I_I_C1_I__V [^{:tag (* String)} t ^int32 a ^int32 b ^{:tag (* CharArray)} dst ^int32 d]
  (checkFromTo a b (len (.-value t)))
  (checkFromSize d (- b a) (len (.-A (NN dst))))
  (copy (subslice (.-A dst) d) (subslice (.-value t) a b)))

(go/method GetBytes_I_I_B1_I__V "GetBytes_I_I_B1_I__V is the deprecated getBytes(int, int, byte[], int): low bytes.\n"
  [^{:tag (* String)} t ^int32 a ^int32 b ^{:tag (* ByteArray)} dst ^int32 d]
  (checkFromTo a b (len (.-value t)))
  (NN dst)
  (checkFromSize d (- b a) (len (.-A dst)))
  (range [i c (subslice (.-value t) a b)]
    (aset (.-A dst) (+ (conv int d) i) (conv int8 c))))

(go/method GetBytes__B1 "GetBytes__B1 is getBytes(): UTF-8, the default charset.\n"
  ^{:tag (* ByteArray)} [^{:tag (* String)} t]
  (ByteArrayOf (spread (EncodeUTF8 (.-value t)))))

(go/method GetBytes_String__B1 ^{:tag (* ByteArray)} [^{:tag (* String)} t ^{:tag (* String)} charset]
  (ByteArrayOf (spread (EncodeBytes (.-value t) (NN charset)))))

(go/method ToCharArray__C1 ^{:tag (* CharArray)} [^{:tag (* String)} t]
  (let [a (NewCharArray (conv int32 (len (.-value t))))]
    (copy (.-A a) (.-value t))
    a))

;; ---------------------------------------------------------------------------------------
;; Comparison

(go/method CompareTo_String__I "CompareTo_String__I compares code units, as Java.\n"
  ^int32 [^{:tag (* String)} t ^{:tag (* String)} s]
  (compareUnits (.-value t) (.-value (NN s))))

(go/func compareUnits ^int32 [^{:tag (slice uint16)} a ^{:tag (slice uint16)} b]
  (let [n (min (len a) (len b))]
    (for [i 0] (< i n) (inc! i)
      (when (!= (aget a i) (aget b i))
        (return (- (conv int32 (aget a i)) (conv int32 (aget b i))))))
    (- (conv int32 (len a)) (conv int32 (len b)))))

(go/method CompareTo_O__I "CompareTo_O__I is the bridge of Comparable.compareTo.\n"
  ^int32 [^{:tag (* String)} t ^any o]
  (.CompareTo_String__I t (String_Cast o)))

(go/func foldUpperLower ^rune [^rune c]
  (charToLower (charToUpper c)))

(go/func isLatin1
  "isLatin1: whether Java holds v as a Latin-1 string (COMPACT_STRINGS: every code unit
<= 0xFF), which decides some of its algorithms' results.\n"
  ^bool [^{:tag (slice uint16)} v]
  (range [_ c v]
    (when (> c 0xff)
      (return false)))
  true)

(go/func compareUnitCI ^int32 [^uint16 c1 ^uint16 c2]
  (when (== c1 c2)
    (return 0))
  (let [u1 (conv uint16 (charToUpper (conv rune c1)))
        u2 (conv uint16 (charToUpper (conv rune c2)))]
    (when (== u1 u2)
      (return 0))
    (let [l1 (conv uint16 (charToLower (conv rune u1)))
          l2 (conv uint16 (charToLower (conv rune u2)))]
      (- (conv int32 l1) (conv int32 l2)))))

(go/func compareCodePointCI ^int32 [^rune c1 ^rune c2]
  (set! c1 (charToUpper c1))
  (set! c2 (charToUpper c2))
  (when (!= c1 c2)
    (set! c1 (charToLower c1))
    (set! c2 (charToLower c2))
    (when (!= c1 c2)
      (return (- c1 c2))))
  0)

(go/func codePointIncluding
  "codePointIncluding is StringUTF16.codePointIncluding: the code point at index, negated
when it is a pair starting there.\n"
  ^rune [^{:tag (slice uint16)} v ^rune cp ^int index ^int start ^int end]
  (when (not (isSurrogate (conv uint16 cp)))
    (return cp))
  (if (isLowSurrogate (conv uint16 cp))
    (when (and (> index start) (isHighSurrogate (aget v (- index 1))))
      (return (toCodePoint (aget v (- index 1)) (conv uint16 cp))))
    (when (and (< (+ index 1) end) (isLowSurrogate (aget v (+ index 1))))
      (return (- (toCodePoint (conv uint16 cp) (aget v (+ index 1)))))))
  cp)

(go/func compareCI
  "compareCI is String.CASE_INSENSITIVE_ORDER as JDK 26 computes it: per code unit when one
string is Latin-1 (StringLatin1.compareToCI, compareToCI_UTF16), else by code points for
surrogate pairs (StringUTF16.compareToCIImpl); each pair compared by toUpperCase, then
toLowerCase.\n"
  ^int32 [^{:tag (slice uint16)} a ^{:tag (slice uint16)} b]
  (when (or (isLatin1 a) (isLatin1 b))
    (let [n (min (len a) (len b))]
      (for [k 0] (< k n) (inc! k)
        (let [d (compareUnitCI (aget a k) (aget b k))]
          (when (!= d 0)
            (return d))))
      (return (- (conv int32 (len a)) (conv int32 (len b))))))
  (compareCIImpl a b))

(go/func compareCIImpl ^int32 [^{:tag (slice uint16)} a ^{:tag (slice uint16)} b]
  (let [tlast (len a)
        olast (len b)
        k1 0
        k2 0]
    (while (and (< k1 tlast) (< k2 olast))
      (let [cp1 (conv rune (aget a k1))
            cp2 (conv rune (aget b k2))]
        (when (or (== cp1 cp2) (== (compareCodePointCI cp1 cp2) 0))
          (inc! k1)
          (inc! k2)
          (continue))
        (set! cp1 (codePointIncluding a cp1 k1 0 tlast))
        (when (< cp1 0)
          (inc! k1)
          (set! cp1 (- cp1)))
        (set! cp2 (codePointIncluding b cp2 k2 0 olast))
        (when (< cp2 0)
          (inc! k2)
          (set! cp2 (- cp2)))
        (let [d (compareCodePointCI cp1 cp2)]
          (when (!= d 0)
            (return d)))
        (inc! k1)
        (inc! k2)))
    (- (conv int32 tlast) (conv int32 olast))))

(go/method CompareToIgnoreCase_String__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s]
  (compareCI (.-value t) (.-value (NN s))))

(go/func regionMatchesCI
  "regionMatchesCI: whether two regions of equal length match ignoring case (compareCI's
comparison, which is zero exactly when they match).\n"
  ^bool [^{:tag (slice uint16)} a ^{:tag (slice uint16)} b]
  (== (compareCI a b) 0))

(go/method EqualsIgnoreCase_String__Z ^bool [^{:tag (* String)} t ^{:tag (* String)} s]
  (cond
    (== t s) (return true)
    (== s nil) (return false)
    (!= (len (.-value t)) (len (.-value s))) (return false))
  (regionMatchesCI (.-value t) (.-value s)))

(go/method RegionMatches_I_String_I_I__Z ^bool
  [^{:tag (* String)} t ^int32 toffset ^{:tag (* String)} other ^int32 ooffset ^int32 n]
  (.RegionMatches_Z_I_String_I_I__Z t false toffset other ooffset n))

(go/method RegionMatches_Z_I_String_I_I__Z ^bool
  [^{:tag (* String)} t ^bool ignoreCase ^int32 toffset ^{:tag (* String)} other ^int32 ooffset ^int32 n]
  (let [a (.-value t)
        b (.-value (NN other))]
    (when (or (< ooffset 0) (< toffset 0)
              (> (conv int64 toffset) (- (conv int64 (len a)) (conv int64 n)))
              (> (conv int64 ooffset) (- (conv int64 (len b)) (conv int64 n))))
      (return false))
    (when (<= n 0)
      (return true))
    (let [x (subslice a toffset (+ toffset n))
          y (subslice b ooffset (+ ooffset n))]
      (when ignoreCase
        (return (regionMatchesCI x y)))
      (equalUnits x y))))

(go/method ContentEquals_CharSequence__Z ^bool [^{:tag (* String)} t ^CharSequence cs]
  (let [n (.Length__I cs)]
    (when (!= (conv int n) (len (.-value t)))
      (return false))
    (for [i (conv int32 0)] (< i n) (inc! i)
      (when (!= (.CharAt_I__C cs i) (aget (.-value t) i))
        (return false)))
    true))

(go/method StartsWith_String_I__Z ^bool [^{:tag (* String)} t ^{:tag (* String)} p ^int32 off]
  (let [a (.-value t)
        b (.-value (NN p))]
    (when (or (< off 0) (> (conv int off) (- (len a) (len b))))
      (return false))
    (equalUnits (subslice a off (+ (conv int off) (len b))) b)))

(go/method StartsWith_String__Z ^bool [^{:tag (* String)} t ^{:tag (* String)} p]
  (.StartsWith_String_I__Z t p 0))

(go/method EndsWith_String__Z ^bool [^{:tag (* String)} t ^{:tag (* String)} p]
  (.StartsWith_String_I__Z t p (conv int32 (- (len (.-value t)) (len (.-value (NN p)))))))

;; ---------------------------------------------------------------------------------------
;; Searching

(go/func indexOfChar
  "indexOfChar is StringUTF16.indexOf(value, ch, from, to): from clamped to [0, to].\n"
  ^int32 [^{:tag (slice uint16)} v ^int32 ch ^int32 from ^int32 to]
  (when (< from 0) (set! from 0))
  (when (> to (conv int32 (len v))) (set! to (conv int32 (len v))))
  (when (< ch 0x10000)
    (for [i from] (< i to) (inc! i)
      (when (== (conv int32 (aget v i)) ch) (return i)))
    (return -1))
  (when (> ch 0x10FFFF)
    (return -1))
  (let [hi (conv uint16 (+ 0xD800 (>> (- ch 0x10000) 10)))
        lo (conv uint16 (+ 0xDC00 (bit-and (- ch 0x10000) 0x3FF)))]
    (for [i from] (< i (- to 1)) (inc! i)
      (when (and (== (aget v i) hi) (== (aget v (+ i 1)) lo)) (return i)))
    -1))

(go/func lastIndexOfChar
  "lastIndexOfChar is StringUTF16.lastIndexOf(value, ch, from).\n"
  ^int32 [^{:tag (slice uint16)} v ^int32 ch ^int32 from]
  (when (< ch 0x10000)
    (when (> from (- (conv int32 (len v)) 1)) (set! from (- (conv int32 (len v)) 1)))
    (for [i from] (>= i 0) (dec! i)
      (when (== (conv int32 (aget v i)) ch) (return i)))
    (return -1))
  (when (> ch 0x10FFFF)
    (return -1))
  (let [hi (conv uint16 (+ 0xD800 (>> (- ch 0x10000) 10)))
        lo (conv uint16 (+ 0xDC00 (bit-and (- ch 0x10000) 0x3FF)))]
    (when (> from (- (conv int32 (len v)) 2)) (set! from (- (conv int32 (len v)) 2)))
    (for [i from] (>= i 0) (dec! i)
      (when (and (== (aget v i) hi) (== (aget v (+ i 1)) lo)) (return i)))
    -1))

(go/func indexOfUnits
  "indexOfUnits is String.indexOf(src, coder, count, str, from): from clamped to [0, count],
\"\" found at from.\n"
  ^int32 [^{:tag (slice uint16)} v ^{:tag (slice uint16)} s ^int32 from ^int32 count]
  (when (< from 0) (set! from 0))
  (when (> from count) (set! from count))
  (let [n (conv int32 (len s))]
    (when (> n (- count from))
      (return -1))
    (when (== n 0)
      (return from))
    (for [i from] (<= (+ i n) count) (inc! i)
      (when (equalUnits (subslice v i (+ i n)) s)
        (return i)))
    -1))

(go/func lastIndexOfUnits
  "lastIndexOfUnits is String.lastIndexOf(src, coder, count, str, from).\n"
  ^int32 [^{:tag (slice uint16)} v ^{:tag (slice uint16)} s ^int32 from]
  (let [n (conv int32 (len s))
        right (- (conv int32 (len v)) n)]
    (when (> from right) (set! from right))
    (when (< from 0)
      (return -1))
    (when (== n 0)
      (return from))
    (for [i from] (>= i 0) (dec! i)
      (when (equalUnits (subslice v i (+ i n)) s)
        (return i)))
    -1))

(go/method IndexOf_I__I ^int32 [^{:tag (* String)} t ^int32 ch]
  (indexOfChar (.-value t) ch 0 (conv int32 (len (.-value t)))))
(go/method IndexOf_I_I__I ^int32 [^{:tag (* String)} t ^int32 ch ^int32 from]
  (indexOfChar (.-value t) ch from (conv int32 (len (.-value t)))))
(go/method IndexOf_I_I_I__I ^int32 [^{:tag (* String)} t ^int32 ch ^int32 from ^int32 to]
  (checkFromTo from to (len (.-value t)))
  (indexOfChar (.-value t) ch from to))
(go/method IndexOf_String__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s]
  (indexOfUnits (.-value t) (.-value (NN s)) 0 (conv int32 (len (.-value t)))))
(go/method IndexOf_String_I__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s ^int32 from]
  (indexOfUnits (.-value t) (.-value (NN s)) from (conv int32 (len (.-value t)))))
(go/method IndexOf_String_I_I__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s ^int32 from ^int32 to]
  (checkFromTo from to (len (.-value t)))
  (indexOfUnits (.-value t) (.-value (NN s)) from to))
(go/method LastIndexOf_I__I ^int32 [^{:tag (* String)} t ^int32 ch]
  (lastIndexOfChar (.-value t) ch (- (conv int32 (len (.-value t))) 1)))
(go/method LastIndexOf_I_I__I ^int32 [^{:tag (* String)} t ^int32 ch ^int32 from]
  (lastIndexOfChar (.-value t) ch from))
(go/method LastIndexOf_String__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s]
  (lastIndexOfUnits (.-value t) (.-value (NN s)) (conv int32 (len (.-value t)))))
(go/method LastIndexOf_String_I__I ^int32 [^{:tag (* String)} t ^{:tag (* String)} s ^int32 from]
  (lastIndexOfUnits (.-value t) (.-value (NN s)) from))

(go/method Contains_CharSequence__Z ^bool [^{:tag (* String)} t ^CharSequence s]
  (>= (indexOfUnits (.-value t) (.-value (.ToString__String (csNN s))) 0 (conv int32 (len (.-value t)))) 0))

(go/func csNN
  "csNN is the null check of a CharSequence (Go's call on a nil interface would panic as a nil
dereference, which Catch maps too; csNN makes it explicit).\n"
  ^CharSequence [^CharSequence s]
  (when (== s nil)
    (panic (NPE)))
  s)

;; ---------------------------------------------------------------------------------------
;; Making new strings

(go/method Concat_String__String ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* String)} s]
  (when (== (len (.-value (NN s))) 0)
    (return t))
  (when (== (len (.-value t)) 0)
    (return s))
  (Concat t s))

(go/method Replace_C_C__String ^{:tag (* String)} [^{:tag (* String)} t ^uint16 a ^uint16 b]
  (when (== a b)
    (return t))
  (let [i (indexOfChar (.-value t) (conv int32 a) 0 (conv int32 (len (.-value t))))]
    (when (< i 0)
      (return t))
    (let [v (make (slice uint16) (len (.-value t)))]
      (range [j c (.-value t)]
        (if (== c a) (aset v j b) (aset v j c)))
      (newString v))))

(go/method Replace_CharSequence_CharSequence__String ^{:tag (* String)}
  [^{:tag (* String)} t ^CharSequence target ^CharSequence replacement]
  (let [tv (.-value (.ToString__String (csNN target)))
        rv (.-value (.ToString__String (csNN replacement)))
        v (.-value t)
        j (indexOfUnits v tv 0 (conv int32 (len v)))]
    (when (< j 0)
      (return t))
    (let [^{:tag (slice uint16)} out nil
          i (conv int32 0)
          n (conv int32 (len tv))]
      (if (== n 0)
        (do
          ;; "" matches before every code unit and at the end
          (range [_ c v]
            (set! out (append out (spread rv)))
            (set! out (append out c)))
          (set! out (append out (spread rv))))
        (do
          (while (>= j 0)
            (set! out (append out (spread (subslice v i j))))
            (set! out (append out (spread rv)))
            (set! i (+ j n))
            (set! j (indexOfUnits v tv i (conv int32 (len v)))))
          (set! out (append out (spread (subslice v i))))))
      (newString out))))

(go/method Repeat_I__String ^{:tag (* String)} [^{:tag (* String)} t ^int32 count]
  (when (< count 0)
    (panic (IllegalArgumentException_New_String (Concat (Str "count is negative: ") (StrOfInt count)))))
  (when (== count 1)
    (return t))
  (let [n (len (.-value t))]
    (when (or (== n 0) (== count 0))
      (return litEmpty))
    (when (< (/ 2147483647 (conv int count)) n)
      (panic (OutOfMemoryError_New_String (Str "Required length exceeds implementation limit"))))
    (let [v (make (slice uint16) 0 (* n (conv int count)))]
      (for [i (conv int32 0)] (< i count) (inc! i)
        (set! v (append v (spread (.-value t)))))
      (newString v))))

(go/method Trim__String "Trim__String strips code units <= ' ' at both ends.\n"
  ^{:tag (* String)} [^{:tag (* String)} t]
  (let [v (.-value t)
        a 0
        b (len v)]
    (while (and (< a b) (<= (aget v a) \space)) (inc! a))
    (while (and (< a b) (<= (aget v (- b 1)) \space)) (dec! b))
    (when (and (== a 0) (== b (len v)))
      (return t))
    (NewStringUTF16 (subslice v a b))))

(go/func stripBounds [^{:tag (slice uint16)} v ^bool lead ^bool trail] :results [^int a ^int b]
  (set! b (len v))
  (when lead
    (while (< a b)
      (let [cp (codePointAt v a b)]
        (when (not (charIsWhitespace cp))
          (break))
        (set! a (+ a (charCount cp))))))
  (when trail
    (while (> b a)
      (let [cp (codePointBefore v b a)]
        (when (not (charIsWhitespace cp))
          (break))
        (set! b (- b (charCount cp))))))
  (return))

(go/method stripped ^{:tag (* String)} [^{:tag (* String)} t ^bool lead ^bool trail]
  (let [(values a b) (stripBounds (.-value t) lead trail)]
    (when (and (== a 0) (== b (len (.-value t))))
      (return t))
    (NewStringUTF16 (subslice (.-value t) a b))))

(go/method Strip__String ^{:tag (* String)} [^{:tag (* String)} t] (.stripped t true true))
(go/method StripLeading__String ^{:tag (* String)} [^{:tag (* String)} t] (.stripped t true false))
(go/method StripTrailing__String ^{:tag (* String)} [^{:tag (* String)} t] (.stripped t false true))
(go/method IsBlank__Z ^bool [^{:tag (* String)} t]
  (let [(values a b) (stripBounds (.-value t) true false)]
    (== a b)))

;; ---- step 5 phase 2B: indent, stripIndent, translateEscapes (jdk26u's String, over lines
;; split as String.lines splits them: at \n, \r and \r\n, no empty last line)

(go/func splitLines ^{:tag (slice (slice uint16))} [^{:tag (slice uint16)} v]
  (let [^{:tag (slice (slice uint16))} ls nil
        start 0
        i 0]
    (while (< i (len v))
      (let [c (aget v i)]
        (cond
          (== c \newline)
          (do (set! ls (append ls (subslice v start i))) (inc! i) (set! start i))
          (== c \return)
          (do (set! ls (append ls (subslice v start i)))
              (inc! i)
              (when (and (< i (len v)) (== (aget v i) \newline))
                (inc! i))
              (set! start i))
          :else (inc! i))))
    (when (< start (len v))
      (set! ls (append ls (subslice v start))))
    ls))

(go/func joinLines "joinLines joins ls with \\n, then suffix.\n"
  ^{:tag (* String)} [^{:tag (slice (slice uint16))} ls ^string suffix]
  (let [^{:tag (slice uint16)} v nil]
    (range [i l ls]
      (when (> i 0)
        (set! v (append v \newline)))
      (set! v (append v (spread l))))
    (range [_ c suffix]
      (set! v (append v (conv uint16 c))))
    (newString v)))

(go/method Indent_I__String "Indent_I__String is String.indent.\n"
  ^{:tag (* String)} [^{:tag (* String)} t ^int32 n]
  (when (== (len (.-value t)) 0)
    (return (Intern "")))
  (let [ls (splitLines (.-value t))]
    (range [i l ls]
      (cond
        (> n 0)
        (let [u (make (slice uint16) 0 (+ (len l) (conv int n)))]
          (for [k (conv int32 0)] (< k n) (inc! k)
            (set! u (append u \space)))
          (aset ls i (append u (spread l))))
        (== n -2147483648)
        (let [(values a _) (stripBounds l true false)]
          (aset ls i (subslice l a)))
        (< n 0)
        (let [(values a _) (stripBounds l true false)]
          (aset ls i (subslice l (min (conv int (- n)) a))))))
    (joinLines ls "\n")))

(go/method StripIndent__String "StripIndent__String is String.stripIndent.\n"
  ^{:tag (* String)} [^{:tag (* String)} t]
  (let [v (.-value t)
        n (len v)]
    (when (== n 0)
      (return (Intern "")))
    (let [last (aget v (- n 1))
          optOut (or (== last \newline) (== last \return))
          ls (splitLines v)
          outdent 0]
      (when (not optOut)
        (set! outdent 2147483647)
        (range [_ l ls]
          (let [(values a _) (stripBounds l true false)]
            (when (!= a (len l))
              (set! outdent (min outdent a)))))
        (let [ll (aget ls (- (len ls) 1))
              (values a _) (stripBounds ll true false)]
          (when (== a (len ll))
            (set! outdent (min outdent (len ll))))))
      (range [i l ls]
        (let [(values first _) (stripBounds l true false)
              (values _ lastNW) (stripBounds l false true)]
          (if (> first lastNW)
            (aset ls i nil)
            (aset ls i (subslice l (min outdent first) lastNW)))))
      (when optOut
        (return (joinLines ls "\n")))
      (joinLines ls ""))))

(go/method TranslateEscapes__String "TranslateEscapes__String is String.translateEscapes.\n"
  ^{:tag (* String)} [^{:tag (* String)} t]
  (let [cs (.-value t)
        n (len cs)
        out (make (slice uint16) 0 n)
        from 0]
    (when (== n 0)
      (return (Intern "")))
    (while (< from n)
      (let [ch (aget cs from)]
        (inc! from)
        (when (== ch \\)
          (if (< from n)
            (do (set! ch (aget cs from)) (inc! from))
            (set! ch 0))
          (switch ch
            (case [\b] (set! ch 8))
            (case [\f] (set! ch 12))
            (case [\n] (set! ch \newline))
            (case [\r] (set! ch \return))
            (case [\s] (set! ch \space))
            (case [\t] (set! ch \tab))
            (case [\' \" \\])
            (case [\0 \1 \2 \3 \4 \5 \6 \7]
              (let [k 1
                    code (conv int32 (- ch \0))]
                (when (<= ch \3)
                  (set! k 2))
                (let [limit (min (+ from k) n)]
                (while (< from limit)
                  (let [c (aget cs from)]
                    (when (or (< c \0) (< \7 c))
                      (break))
                    (inc! from)
                    (set! code (bit-or (<< code 3) (conv int32 (- c \0)))))))
                (set! ch (conv uint16 code))))
            (case [\newline] (continue))
            (case [\return]
              (when (and (< from n) (== (aget cs from) \newline))
                (inc! from))
              (continue))
            (default
              (let [h (hexUpper (conv uint32 ch))]
                (while (< (len h) 4) (set! h (+ "0" h)))
                (panic (IllegalArgumentException_New_String
                         (Concat (Str "Invalid escape sequence: \\") (NewStringUTF16 (lit (slice uint16) ch))
                                 (Str (+ " \\\\u" h)))))))))
        (set! out (append out ch))))
    (newString out)))

(go/method ContentEquals_StringBuffer__Z "ContentEquals_StringBuffer__Z is contentEquals(StringBuffer).\n"
  ^bool [^{:tag (* String)} t ^{:tag (* StringBuffer)} sb]
  (when (== sb nil)
    (panic (NPE)))
  (.ContentEquals_CharSequence__Z t sb))

;; ---------------------------------------------------------------------------------------
;; Case mapping (StringUTF16.toLowerCase/toUpperCase for the root locale; the Locale
;; overloads are phase 2's, with Locale)

(go/method ToLowerCase__String ^{:tag (* String)} [^{:tag (* String)} t]
  (let [v (.-value t)
        first -1]
    (for [i 0] (< i (len v)) _
      (let [cp (codePointAt v i (len v))]
        (when (or (== cp 0x03A3) (== cp 0x0130) (!= (charToLower cp) cp))
          (set! first i)
          (break))
        (set! i (+ i (charCount cp)))))
    (when (< first 0)
      (return t))
    (let [out (make (slice uint16) first (len v))]
      (copy out (subslice v _ first))
      (for [i first] (< i (len v)) _
        (let [cp (codePointAt v i (len v))
              n (charCount cp)]
          (cond
            (== cp 0x0130) (set! out (append out 0x0069 0x0307))
            (== cp 0x03A3) (if (isFinalSigma v i)
                             (set! out (append out 0x03C2))
                             (set! out (append out 0x03C3)))
            :else (set! out (appendCodePoint out (charToLower cp))))
          (set! i (+ i n))))
      (newString out))))

(go/func isFinalSigma
  "isFinalSigma is ConditionalSpecialCasing's Final_Sigma: a cased letter before (skipping
case-ignorable characters) and none after, within the word. Java finds the word with
java.text.BreakIterator; jrt takes letters, marks and digits as the word (JRT-NOTES.md).\n"
  ^bool [^{:tag (slice uint16)} v ^int index]
  (let [found false]
    (for [i index] (> i 0) _
      (let [cp (codePointBefore v i 0)]
        (when (not (isWordChar cp))
          (break))
        (when (isCased cp)
          (set! found true)
          (break))
        (set! i (- i (charCount cp)))))
    (when (not found)
      (return false))
    (for [i (+ index 1)] (< i (len v)) _
      (let [cp (codePointAt v i (len v))]
        (when (not (isWordChar cp))
          (break))
        (when (isCased cp)
          (return false))
        (set! i (+ i (charCount cp)))))
    true))

(go/method ToUpperCase__String ^{:tag (* String)} [^{:tag (* String)} t]
  (let [v (.-value t)
        first -1]
    (for [i 0] (< i (len v)) _
      (let [cp (codePointAt v i (len v))
            u (charToUpperEx cp)]
        (when (or (< u 0) (!= u cp))
          (set! first i)
          (break))
        (set! i (+ i (charCount cp)))))
    (when (< first 0)
      (return t))
    (let [out (make (slice uint16) first (len v))]
      (copy out (subslice v _ first))
      (for [i first] (< i (len v)) _
        (let [cp (codePointAt v i (len v))
              u (charToUpperEx cp)]
          (if (< u 0)
            (set! out (append out (spread (charToUpperArray cp))))
            (set! out (appendCodePoint out u)))
          (set! i (+ i (charCount cp)))))
      (newString out))))

;; ---------------------------------------------------------------------------------------
;; Static members

(go/func String_ValueOf_O__String ^{:tag (* String)} [^any x] (StrOfObj x))
(go/func String_ValueOf_C__String ^{:tag (* String)} [^uint16 c] (StrOfChar c))
(go/func String_ValueOf_I__String ^{:tag (* String)} [^int32 i] (StrOfInt i))
(go/func String_ValueOf_J__String ^{:tag (* String)} [^int64 i] (StrOfLong i))
(go/func String_ValueOf_F__String ^{:tag (* String)} [^float32 f] (StrOfFloat f))
(go/func String_ValueOf_D__String ^{:tag (* String)} [^float64 d] (StrOfDouble d))
(go/func String_ValueOf_Z__String ^{:tag (* String)} [^bool b] (StrOfBool b))
(go/func String_ValueOf_C1__String ^{:tag (* String)} [^{:tag (* CharArray)} a] (String_New_C1 a))
(go/func String_ValueOf_C1_I_I__String ^{:tag (* String)} [^{:tag (* CharArray)} a ^int32 o ^int32 n]
  (String_New_C1_I_I a o n))
(go/func String_CopyValueOf_C1__String ^{:tag (* String)} [^{:tag (* CharArray)} a] (String_New_C1 a))
(go/func String_CopyValueOf_C1_I_I__String ^{:tag (* String)} [^{:tag (* CharArray)} a ^int32 o ^int32 n]
  (String_New_C1_I_I a o n))

(go/func String_ValueOfCodePoint_I__String
  "String_ValueOfCodePoint_I__String is String.valueOfCodePoint (package-private).\n"
  ^{:tag (* String)} [^int32 cp]
  (when (or (< cp 0) (> cp 0x10FFFF))
    (panic (IllegalArgumentException_New_String
             (Str (+ "Not a valid Unicode code point: 0x" (hexUpper (conv uint32 cp)))))))
  (newString (appendCodePoint nil cp)))

(go/func String_Join_CharSequence_CharSequence1__String
  "String_Join_CharSequence_CharSequence1__String is String.join(CharSequence, CharSequence...).\n"
  ^{:tag (* String)} [^CharSequence sep ^{:tag (* RefArray)} elements]
  (let [s (.-value (StrOfObj (csNN sep)))
        ^{:tag (slice uint16)} v nil]
    (range [i e (.-A (NN elements))]
      (when (> i 0)
        (set! v (append v (spread s))))
      (set! v (append v (spread (.-value (StrOfObj e))))))
    (newString v)))

(go/func String_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* String) x)] ok))
(go/func String_Cast ^{:tag (* String)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* String) x)]
    (when (not ok) (panic (ClassCast x String_class)))
    v))
