;; jrt: jdk.internal.access.SharedSecrets and JavaLangAccess, the six members the translated
;; JDK classes call (JRT-SOURCES.md, the shim edge; JRT-NOTES.md, "What phase 2 must add"),
;; over jrt's String (added with c2g, doc/go/C2G-NOTES.md). Byte arrays of UTF-16 code units
;; are little-endian, as StringUTF16.putChar writes them in jrt.
(in-ns 'go.arbace.jrt)

(go/file "access.go")

(go/type JavaLangAccess "JavaLangAccess is jdk.internal.access.JavaLangAccess (an interface).\n"
  (interface Object_I
    (Is_JavaLangAccess [])
    (CountPositives_B1_I_I__I ^int32 [^{:tag (* ByteArray)} ba ^int32 off ^int32 n])
    (UncheckedNewStringWithLatin1Bytes_B1__String ^{:tag (* String)} [^{:tag (* ByteArray)} b])
    (UncheckedGetUTF16Char_B1_I__C ^uint16 [^{:tag (* ByteArray)} v ^int32 index])
    (UncheckedPutCharUTF16_B1_I_I__V [^{:tag (* ByteArray)} v ^int32 index ^int32 c])
    (InflateBytesToChars_B1_I_C1_I_I__V [^{:tag (* ByteArray)} src ^int32 srcOff ^{:tag (* CharArray)} dst ^int32 dstOff ^int32 n])
    (CurrentCarrierThread__Thread ^Thread_I [])
    (GetEnumConstantsShared_Class__Enum1 ^{:tag (* RefArray)} [^{:tag (* Class)} c])
    (Join_String_String_String_String1_I__String ^{:tag (* String)}
      [^{:tag (* String)} prefix ^{:tag (* String)} suffix ^{:tag (* String)} delimiter ^{:tag (* RefArray)} elements ^int32 size])))

(go/var JavaLangAccess_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.access.JavaLangAccess" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go "arbace/jrt.JavaLangAccess"))))

(go/type javaLangAccess "javaLangAccess is jrt's JavaLangAccess.\n" (struct Object))

(go/method Is_JavaLangAccess [^{:tag (* javaLangAccess)} t])

(go/method CountPositives_B1_I_I__I "countPositives: the number of leading bytes that are not negative.\n"
  ^int32 [^{:tag (* javaLangAccess)} t ^{:tag (* ByteArray)} ba ^int32 off ^int32 n]
  (let [a (.-A (NN ba))]
    (for [i off] (< i (+ off n)) (inc! i)
      (when (< (aget a i) 0)
        (return (- i off))))
    n))

(go/method UncheckedNewStringWithLatin1Bytes_B1__String
  ^{:tag (* String)} [^{:tag (* javaLangAccess)} t ^{:tag (* ByteArray)} b]
  (String_NewStringWithLatin1Bytes_B1__String b))

(go/method UncheckedGetUTF16Char_B1_I__C
  ^uint16 [^{:tag (* javaLangAccess)} t ^{:tag (* ByteArray)} v ^int32 index]
  (let [a (.-A (NN v))
        j (* 2 index)]
    (bit-or (conv uint16 (conv uint8 (aget a j))) (<< (conv uint16 (conv uint8 (aget a (+ j 1)))) 8))))

(go/method UncheckedPutCharUTF16_B1_I_I__V
  [^{:tag (* javaLangAccess)} t ^{:tag (* ByteArray)} v ^int32 index ^int32 c]
  (StringUTF16_PutChar_B1_I_I__V v index c))

(go/method InflateBytesToChars_B1_I_C1_I_I__V
  [^{:tag (* javaLangAccess)} t ^{:tag (* ByteArray)} src ^int32 srcOff ^{:tag (* CharArray)} dst ^int32 dstOff ^int32 n]
  (let [s (.-A (NN src))
        d (.-A (NN dst))]
    (for [i (conv int32 0)] (< i n) (inc! i)
      (aset d (+ dstOff i) (conv uint16 (conv uint8 (aget s (+ srcOff i))))))))

(go/method CurrentCarrierThread__Thread ^Thread_I [^{:tag (* javaLangAccess)} t]
  (Thread_CurrentThread__Thread))

(go/method GetEnumConstantsShared_Class__Enum1
  "GetEnumConstantsShared_Class__Enum1 is getEnumConstantsShared (EnumSet): the enum's constants,
null for a class that is not an enum.\n"
  ^{:tag (* RefArray)} [^{:tag (* javaLangAccess)} t ^{:tag (* Class)} c]
  (.GetEnumConstants__O1 (NN c)))

(go/method Join_String_String_String_String1_I__String
  "Join_String_String_String_String1_I__String is join (StringJoiner, String.join): prefix, the
first size elements separated by delimiter, suffix.\n"
  ^{:tag (* String)} [^{:tag (* javaLangAccess)} t ^{:tag (* String)} prefix ^{:tag (* String)} suffix
                      ^{:tag (* String)} delimiter ^{:tag (* RefArray)} elements ^int32 size]
  (let [^{:tag (slice uint16)} v nil]
    (set! v (append v (spread (.-value (NN prefix)))))
    (for [i (conv int32 0)] (< i size) (inc! i)
      (when (> i 0)
        (set! v (append v (spread (.-value (NN delimiter))))))
      (set! v (append v (spread (.-value (StrOfObj (aget (.-A elements) i)))))))
    (set! v (append v (spread (.-value (NN suffix)))))
    (newString v)))

(go/method Ref ^any [^{:tag (* javaLangAccess)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* javaLangAccess)} t] JavaLangAccess_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* javaLangAccess)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* javaLangAccess)} t] (panic (CloneNotSupported t)))

(go/func JavaLangAccess_InstanceOf ^bool [^any x] (let [(values _ ok) (assert JavaLangAccess x)] ok))
(go/func JavaLangAccess_Cast ^JavaLangAccess [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert JavaLangAccess x)]
    (when (not ok) (panic (ClassCast x JavaLangAccess_class)))
    v))

(go/type SharedSecrets "SharedSecrets is jdk.internal.access.SharedSecrets: the accesses jrt has.\n"
  (struct Object))

(go/var SharedSecrets_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.access.SharedSecrets" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class :Go "arbace/jrt.SharedSecrets"))))

(go/var ^JavaLangAccess theJavaLangAccess (addr (lit javaLangAccess)))

(go/func SharedSecrets_GetJavaLangAccess__JavaLangAccess "SharedSecrets.getJavaLangAccess.\n"
  ^JavaLangAccess []
  theJavaLangAccess)

(go/func init []
  (set! (.-IsInstance (.Info JavaLangAccess_class)) JavaLangAccess_InstanceOf))
