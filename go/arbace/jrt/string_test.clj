;; jrt's tests: String and StringBuilder against the JVM (testdata/strings.txt, chars.txt,
;; exceptions.txt) and by themselves.
(in-ns 'go.arbace.jrt)

(go/file "string_test.go"
  :imports [[strconv "strconv"] [strings "strings"] [testing "testing"]])

(go/func bytesRes ^string [^{:tag (slice int8)} b]
  (let [parts (make (slice string) (len b))]
    (range [i x b]
      (aset parts i (strconv/Itoa (conv int (conv uint8 x)))))
    (strings/Join parts ",")))

(go/func unitsRes ^string [^{:tag (slice uint16)} v]
  (let [parts (make (slice string) (len v))]
    (range [i x v]
      (aset parts i (strconv/Itoa (conv int x))))
    (strings/Join parts ",")))

(go/func TestStringCases "TestStringCases runs testdata/strings.txt.\n"
  [^{:tag (* testing/T)} t]
  (let [n 0]
    (range [_ c (readCases t "strings.txt")]
      (let [op (aget c 0)
            s (jstr (aget c 1))
            ^{:tag (func [] [string])} f nil]
        (switch op
          (case ["length"] (set! f (fn ^string [] (ires (.Length__I s)))))
          (case ["hashCode"] (set! f (fn ^string [] (ires (.HashCode__I s)))))
          (case ["isEmpty"] (set! f (fn ^string [] (bres (.IsEmpty__Z s)))))
          (case ["toUpperCase"] (set! f (fn ^string [] (sres (.ToUpperCase__String s)))))
          (case ["toLowerCase"] (set! f (fn ^string [] (sres (.ToLowerCase__String s)))))
          (case ["trim"] (set! f (fn ^string [] (sres (.Trim__String s)))))
          (case ["strip"] (set! f (fn ^string [] (sres (.Strip__String s)))))
          (case ["stripLeading"] (set! f (fn ^string [] (sres (.StripLeading__String s)))))
          (case ["stripTrailing"] (set! f (fn ^string [] (sres (.StripTrailing__String s)))))
          (case ["isBlank"] (set! f (fn ^string [] (bres (.IsBlank__Z s)))))
          (case ["codePointCount"] (set! f (fn ^string [] (ires (.CodePointCount_I_I__I s 0 (.Length__I s))))))
          (case ["intern=="] (set! f (fn ^string [] (bres (== (.Intern__String s) (.Intern__String (String_New_String s)))))))
          (case ["getBytesUTF8"] (set! f (fn ^string [] (bytesRes (.-A (.GetBytes_String__B1 s (Str "UTF-8")))))))
          (case ["getBytesLatin1"] (set! f (fn ^string [] (bytesRes (.-A (.GetBytes_String__B1 s (Str "ISO-8859-1")))))))
          (case ["getBytesASCII"] (set! f (fn ^string [] (bytesRes (.-A (.GetBytes_String__B1 s (Str "US-ASCII")))))))
          (case ["toCharArray"] (set! f (fn ^string [] (unitsRes (.-A (.ToCharArray__C1 s))))))
          (case ["charAt"] (set! f (fn ^string [] (ires (conv int32 (.CharAt_I__C s (atoi (aget c 2))))))))
          (case ["codePointAt"] (set! f (fn ^string [] (ires (.CodePointAt_I__I s (atoi (aget c 2)))))))
          (case ["codePointBefore"] (set! f (fn ^string [] (ires (.CodePointBefore_I__I s (atoi (aget c 2)))))))
          (case ["substring1"] (set! f (fn ^string [] (sres (.Substring_I__String s (atoi (aget c 2)))))))
          (case ["repeat"] (set! f (fn ^string [] (sres (.Repeat_I__String s (atoi (aget c 2)))))))
          (case ["indexOfChar"] (set! f (fn ^string [] (ires (.IndexOf_I__I s (+ 96 (atoi (aget c 2))))))))
          (case ["offsetByCodePoints"] (set! f (fn ^string [] (ires (.OffsetByCodePoints_I_I__I s 0 (atoi (aget c 2)))))))
          (case ["substring2"] (set! f (fn ^string [] (sres (.Substring_I_I__String s (atoi (aget c 2)) (atoi (aget c 3)))))))
          (case ["subSequence"] (set! f (fn ^string [] (sres (.ToString__String (.SubSequence_I_I__CharSequence s (atoi (aget c 2)) (atoi (aget c 3))))))))
          (case ["codePointCount2"] (set! f (fn ^string [] (ires (.CodePointCount_I_I__I s (atoi (aget c 2)) (atoi (aget c 3)))))))
          (case ["indexOfCharRange"] (set! f (fn ^string [] (ires (.IndexOf_I_I_I__I s 97 (atoi (aget c 2)) (atoi (aget c 3)))))))
          (case ["equals"] (set! f (fn ^string [] (bres (.Equals_O__Z s (jstr (aget c 2)))))))
          (case ["equalsIgnoreCase"] (set! f (fn ^string [] (bres (.EqualsIgnoreCase_String__Z s (jstr (aget c 2)))))))
          (case ["compareTo"] (set! f (fn ^string [] (ires (.CompareTo_String__I s (jstr (aget c 2)))))))
          (case ["compareToIgnoreCase"] (set! f (fn ^string [] (ires (.CompareToIgnoreCase_String__I s (jstr (aget c 2)))))))
          (case ["indexOf"] (set! f (fn ^string [] (ires (.IndexOf_String__I s (jstr (aget c 2)))))))
          (case ["indexOf1"] (set! f (fn ^string [] (ires (.IndexOf_String_I__I s (jstr (aget c 2)) 1)))))
          (case ["lastIndexOf"] (set! f (fn ^string [] (ires (.LastIndexOf_String__I s (jstr (aget c 2)))))))
          (case ["lastIndexOf2"] (set! f (fn ^string [] (ires (.LastIndexOf_String_I__I s (jstr (aget c 2)) 2)))))
          (case ["contains"] (set! f (fn ^string [] (bres (.Contains_CharSequence__Z s (jstr (aget c 2)))))))
          (case ["startsWith"] (set! f (fn ^string [] (bres (.StartsWith_String__Z s (jstr (aget c 2)))))))
          (case ["startsWith1"] (set! f (fn ^string [] (bres (.StartsWith_String_I__Z s (jstr (aget c 2)) 1)))))
          (case ["endsWith"] (set! f (fn ^string [] (bres (.EndsWith_String__Z s (jstr (aget c 2)))))))
          (case ["concat"] (set! f (fn ^string [] (sres (.Concat_String__String s (jstr (aget c 2)))))))
          (case ["replace"] (set! f (fn ^string [] (sres (.Replace_CharSequence_CharSequence__String s (jstr (aget c 2)) (Str "<>"))))))
          (case ["regionMatchesCI"] (set! f (fn ^string [] (bres (.RegionMatches_Z_I_String_I_I__Z s true 1 (jstr (aget c 2)) 0 2)))))
          (case ["lastIndexOfChar"]
            (set! f (fn ^string []
                      (let [n (jstr (aget c 2))
                            ^int32 ch 97]
                        (when (> (.Length__I n) 0)
                          (set! ch (.CodePointAt_I__I n 0)))
                        (ires (.LastIndexOf_I__I s ch))))))
          (case ["replaceChar"] (set! f (fn ^string [] (sres (.Replace_C_C__String s \a \Z)))))
          (case ["join"]
            (set! f (fn ^string []
                      (let [cs (jstr (aget c 2))]
                        (sres (String_Join_CharSequence_CharSequence1__String s (RefArrayOf CharSequence_class cs (Str "b") cs)))))))
          (case ["valueOfCodePoint"]
            (set! f (fn ^string [] (sres (String_ValueOfCodePoint_I__String (atoi (aget c 1)))))))
          (case ["newStringUTF8"]
            (set! f (fn ^string []
                      (let [^{:tag (slice int8)} b nil]
                        (when (!= (aget c 1) "")
                          (range [_ x (strings/Split (aget c 1) ",")]
                            (set! b (append b (conv int8 (atoi x))))))
                        (sres (String_New_B1_String (ByteArrayOf (spread b)) (Str "UTF-8")))))))
          (default
            (.Errorf t "unknown case %q" op)
            (continue)))
        (check t c (res f))
        (inc! n)))
    (failuresReport t)
    (.Logf t "%d cases" n)))

(go/func TestChars
  "TestChars checks String's case mapping of every code point against testdata/chars.txt (the
code points Java maps; all others must map to themselves).\n"
  [^{:tag (* testing/T)} t]
  (let [want (make (map rune (slice string)))]
    (range [_ c (readCases t "chars.txt")]
      (let [(values cp _) (strconv/ParseUint (aget c 0) 16 32)]
        (aset want (conv rune cp) c)))
    (let [bad 0]
      (for [cp (rune 0)] (< cp 0x110000) (inc! cp)
        (when (and (>= cp 0xD800) (<= cp 0xDFFF))
          (continue))
        (let [s (newString (appendCodePoint nil cp))
              u (esc (.-value (.ToUpperCase__String s)))
              l (esc (.-value (.ToLowerCase__String s)))
              (values w ok) (aget want cp)
              wu (esc (.-value s))
              wl wu]
          (when ok
            (set! wu (aget w 1))
            (set! wl (aget w 2)))
          (when (or (!= u wu) (!= l wl))
            (inc! bad)
            (when (< bad 20)
              (.Errorf t "U+%04X: upper %q lower %q, want %q %q" cp u l wu wl)))))
      (when (> bad 0)
        (.Errorf t "%d code points differ" bad)))))

(go/func TestStringBasics [^{:tag (* testing/T)} t]
  ;; literals are interned, Str is not
  (when (!= (Intern "abc") (Intern "abc"))
    (.Error t "Intern: two objects"))
  (when (== (Str "abc") (Intern "abc"))
    (.Error t "Str interned"))
  (when (!= (.Intern__String (Str "abc")) (Intern "abc"))
    (.Error t "intern() differs from the literal"))
  (when (!= (InternUTF16 (lit (slice uint16) 0xD800 0x41)) (.Intern__String (NewStringUTF16 (lit (slice uint16) 0xD800 0x41))))
    (.Error t "InternUTF16 of an unpaired surrogate"))
  ;; Go's text
  (let [s (Str "hé\uD83D\uDE00")]
    (when (or (!= (.Length__I s) 4) (!= (.String s) "hé\uD83D\uDE00"))
      (.Errorf t "Str: %d %q" (.Length__I s) (.String s))))
  (when (!= (.String (NewStringUTF16 (lit (slice uint16) 0x61 0xD800))) "a�")
    (.Error t "unpaired surrogate to Go"))
  (let [^{:tag (* String)} z nil]
    (when (!= (.String z) "null")
      (.Error t "nil String to Go")))
  ;; Concat and the conversions of java-str
  (let [s (Concat (Str "a") (StrOfInt -42) (StrOfLong 9223372036854775807) (StrOfChar \x)
                  (StrOfBool true) (StrOfObj nil) (StrOfFloat 1.5) (StrOfDouble 1.0E10) nil)]
    (when (!= (.String s) "a-429223372036854775807xtruenull1.51.0E10null")
      (.Errorf t "Concat: %q" (.String s))))
  (when (!= (StrOfBool false) (Intern "false"))
    (.Error t "String.valueOf(false) is not the literal"))
  ;; hash codes and switch hashing
  (when (!= (.HashCode__I (Str "hello")) 99162322)
    (.Errorf t "hashCode: %d" (.HashCode__I (Str "hello"))))
  (when (!= (.HashCode__I (Str "")) 0)
    (.Error t "hashCode of \"\""))
  ;; equals across Object and identity
  (when (not (Equals (Str "x") (Str "x")))
    (.Error t "Equals"))
  (when (Equals (Str "x") (StringBuilder_New_String (Str "x")))
    (.Error t "String equals StringBuilder")))

(go/func TestStringBuilder [^{:tag (* testing/T)} t]
  (let [b (StringBuilder_New)]
    (.Append_I__StringBuilder (.Append_String__StringBuilder b (Str "n=")) 42)
    (.Append_C__StringBuilder b \;)
    (.Append_D__StringBuilder b 0.1)
    (.Append_O__StringBuilder b nil)
    (.Append_Z__StringBuilder b false)
    (.AppendCodePoint_I__StringBuilder b 0x1F600)
    (.Insert_I_String__StringBuilder b 0 (Str ">"))
    (when (!= (.String (.ToString__String b)) ">n=42;0.1nullfalse\uD83D\uDE00")
      (.Errorf t "StringBuilder: %q" (.String (.ToString__String b))))
    (.Reverse__StringBuilder b)
    (when (!= (.String (.ToString__String b)) "\uD83D\uDE00eslafllun1.0;24=n>")
      (.Errorf t "reverse: %q" (.String (.ToString__String b))))
    (.SetLength_I__V b 2)
    (.DeleteCharAt_I__StringBuilder b 0)
    (when (!= (.String (.ToString__String b)) "\uFFFD")
      (.Errorf t "setLength/deleteCharAt: %q" (.ToString__String b))))
  ;; StringBuffer is the same, synchronized
  (let [b (StringBuffer_New_String (Str "ab"))]
    (.Append_String__StringBuffer b (Str "cd"))
    (when (or (!= (.String (.ToString__String b)) "abcd") (!= (.Capacity__I b) 18))
      (.Errorf t "StringBuffer: %q %d" (.ToString__String b) (.Capacity__I b)))))

(go/func TestExceptionCases
  "TestExceptionCases runs testdata/exceptions.txt: String's, StringBuilder's, arrays' and
Throwable's exceptions and messages, and the run-time errors' mapping.\n"
  [^{:tag (* testing/T)} t]
  (range [_ c (readCases t "exceptions.txt")]
    (let [f (exceptionCase (aget c 0))]
      (when (== f nil)
        (.Logf t "skipped: %s" (aget c 0))
        (continue))
      (check t c (res f))))
  (failuresReport t))

(go/func sb ^{:tag (* StringBuilder)} [] (StringBuilder_New_String (Str "abc")))

(go/func exceptionCase ^{:tag (func [] [string])} [^string name]
  (let [abc (Str "abc")]
    (switch name
      (case ["charAt5"] (return (fn ^string [] (ires (conv int32 (.CharAt_I__C abc 5))))))
      (case ["charAt-1"] (return (fn ^string [] (ires (conv int32 (.CharAt_I__C abc -1))))))
      (case ["substring5"] (return (fn ^string [] (sres (.Substring_I__String abc 5)))))
      (case ["substring-1"] (return (fn ^string [] (sres (.Substring_I__String abc -1)))))
      (case ["substring21"] (return (fn ^string [] (sres (.Substring_I_I__String abc 2 1)))))
      (case ["substring05"] (return (fn ^string [] (sres (.Substring_I_I__String abc 0 5)))))
      (case ["codePointAt5"] (return (fn ^string [] (ires (.CodePointAt_I__I abc 5)))))
      (case ["codePointBefore0"] (return (fn ^string [] (ires (.CodePointBefore_I__I abc 0)))))
      (case ["subSequence21"] (return (fn ^string [] (sres (.ToString__String (.SubSequence_I_I__CharSequence abc 2 1))))))
      (case ["repeat-1"] (return (fn ^string [] (sres (.Repeat_I__String abc -1)))))
      (case ["newStringChars"] (return (fn ^string [] (sres (String_New_C1_I_I (.ToCharArray__C1 abc) 2 5)))))
      (case ["newStringCodePoints"] (return (fn ^string [] (sres (String_New_I1_I_I (IntArrayOf 65 -1) 0 2)))))
      (case ["getChars"] (return (fn ^string [] (.GetChars_I_I_C1_I__V abc 0 3 (NewCharArray 2) 0) "null")))
      (case ["indexOfRange"] (return (fn ^string [] (ires (.IndexOf_String_I_I__I abc (Str "b") 2 1)))))
      (case ["sbCharAt5"] (return (fn ^string [] (ires (conv int32 (.CharAt_I__C (sb) 5))))))
      (case ["sbSetCharAt5"] (return (fn ^string [] (.SetCharAt_I_C__V (sb) 5 \x) "null")))
      (case ["sbDeleteCharAt5"] (return (fn ^string [] (sres (.ToString__String (.DeleteCharAt_I__StringBuilder (sb) 5))))))
      (case ["sbDelete21"] (return (fn ^string [] (sres (.ToString__String (.Delete_I_I__StringBuilder (sb) 2 1))))))
      (case ["sbDelete56"] (return (fn ^string [] (sres (.ToString__String (.Delete_I_I__StringBuilder (sb) 5 6))))))
      (case ["sbDelete15"] (return (fn ^string [] (sres (.ToString__String (.Delete_I_I__StringBuilder (sb) 1 5))))))
      (case ["sbInsert5"] (return (fn ^string [] (sres (.ToString__String (.Insert_I_String__StringBuilder (sb) 5 (Str "x")))))))
      (case ["sbInsert-1"] (return (fn ^string [] (sres (.ToString__String (.Insert_I_String__StringBuilder (sb) -1 (Str "x")))))))
      (case ["sbSetLength-1"] (return (fn ^string [] (.SetLength_I__V (sb) -1) "null")))
      (case ["sbSetLength5"] (return (fn ^string [] (let [b (sb)] (.SetLength_I__V b 5) (unitsRes (.-value (.ToString__String b)))))))
      (case ["sbSubstring5"] (return (fn ^string [] (sres (.Substring_I__String (sb) 5)))))
      (case ["sbSubstring21"] (return (fn ^string [] (sres (.Substring_I_I__String (sb) 2 1)))))
      (case ["sbAppendCS"] (return (fn ^string [] (sres (.ToString__String (.Append_CharSequence_I_I__StringBuilder (sb) (Str "xyz") 2 5))))))
      (case ["sbAppendChars"] (return (fn ^string [] (sres (.ToString__String (.Append_C1_I_I__StringBuilder (sb) (.ToCharArray__C1 (Str "xyz")) 2 5))))))
      (case ["sbAppendCodePoint"] (return (fn ^string [] (sres (.ToString__String (.AppendCodePoint_I__StringBuilder (sb) -1))))))
      (case ["sbAppendCodePointBig"] (return (fn ^string [] (sres (.ToString__String (.AppendCodePoint_I__StringBuilder (sb) 0x110000))))))
      (case ["sbRepeat-1"] (return (fn ^string [] (sres (.ToString__String (.Repeat_I_I__StringBuilder (sb) 65 -1))))))
      (case ["sbRepeatCP"] (return (fn ^string [] (sres (.ToString__String (.Repeat_I_I__StringBuilder (sb) 0x1F600 2))))))
      (case ["sbReverse"] (return (fn ^string [] (sres (.ToString__String (.Reverse__StringBuilder (StringBuilder_New_String (jstr "a\\uD83D\\uDE00b\\uDC00\\uD800c"))))))))
      (case ["sbReplace"] (return (fn ^string [] (sres (.ToString__String (.Replace_I_I_String__StringBuilder (sb) 1 10 (Str "XYZ")))))))
      (case ["sbInsertChar"] (return (fn ^string [] (sres (.ToString__String (.Insert_I_C__StringBuilder (sb) 1 \Z))))))
      (case ["sbCodePointCount"] (return (fn ^string [] (ires (.CodePointCount_I_I__I (sb) 2 1)))))
      (case ["sbOffsetByCodePoints"] (return (fn ^string [] (ires (.OffsetByCodePoints_I_I__I (sb) 4 0)))))
      (case ["sbGetChars"] (return (fn ^string [] (.GetChars_I_I_C1_I__V (sb) 0 3 (NewCharArray 2) 0) "null")))
      (case ["sbCapacity"] (return (fn ^string [] (ires (.Capacity__I (StringBuilder_New))))))
      (case ["sbCapacityGrow"] (return (fn ^string [] (let [b (StringBuilder_New)] (.Append_String__StringBuilder b (Str "01234567890123456")) (ires (.Capacity__I b))))))
      (case ["sbCapacityString"] (return (fn ^string [] (ires (.Capacity__I (sb))))))
      (case ["sbNew-1"] (return (fn ^string [] (sres (.ToString__String (StringBuilder_New_I -1))))))
      (case ["sbCompareTo"] (return (fn ^string [] (ires (.CompareTo_StringBuilder__I (StringBuilder_New_String (Str "abd")) (StringBuilder_New_String (Str "abc")))))))
      (case ["aget5"] (return (fn ^string [] (let [a (NewIntArray 3) i (conv int32 5)] (ires (aget (.-A a) i))))))
      (case ["aget-1"] (return (fn ^string [] (let [a (NewIntArray 3) i (conv int32 -1)] (ires (aget (.-A a) i))))))
      (case ["agetObj"] (return (fn ^string [] (let [a (NewRefArray Object_class 2) i (conv int32 2)] (sres (StrOfObj (aget (.-A a) i)))))))
      (case ["newArray-1"] (return (fn ^string [] (ires (conv int32 (len (.-A (NewIntArray -1))))))))
      (case ["newObjArray-1"] (return (fn ^string [] (ires (conv int32 (len (.-A (NewRefArray Object_class -1))))))))
      (case ["multiArray"] (return (fn ^string [] (ires (conv int32 (len (.-A (NewMultiArray (.ArrayClass (.ArrayClass Prim_int)) 2 -3))))))))
      (case ["arrayStore"] (return (fn ^string [] (.Store (NewRefArray String_class 1) 0 (Object_New)) "null")))
      (case ["arraycopyNull"] (return (fn ^string [] (Arraycopy nil 0 (NewIntArray 1) 0 1) "null")))
      (case ["arraycopyNotArray"] (return (fn ^string [] (Arraycopy (Str "x") 0 (NewIntArray 1) 0 1) "null")))
      (case ["arraycopyNotArrayDst"] (return (fn ^string [] (Arraycopy (NewIntArray 1) 0 (Str "x") 0 1) "null")))
      (case ["arraycopyPrimMismatch"] (return (fn ^string [] (Arraycopy (NewIntArray 1) 0 (NewLongArray 1) 0 1) "null")))
      (case ["arraycopyPrimToObj"] (return (fn ^string [] (Arraycopy (NewIntArray 1) 0 (NewRefArray Object_class 1) 0 1) "null")))
      (case ["arraycopyObjToPrim"] (return (fn ^string [] (Arraycopy (NewRefArray Object_class 1) 0 (NewIntArray 1) 0 1) "null")))
      (case ["arraycopySrcNeg"] (return (fn ^string [] (Arraycopy (NewIntArray 5) -1 (NewIntArray 5) 0 1) "null")))
      (case ["arraycopyDstNeg"] (return (fn ^string [] (Arraycopy (NewIntArray 5) 0 (NewIntArray 5) -1 1) "null")))
      (case ["arraycopyLenNeg"] (return (fn ^string [] (Arraycopy (NewIntArray 5) 0 (NewIntArray 5) 0 -1) "null")))
      (case ["arraycopySrcEnd"] (return (fn ^string [] (Arraycopy (NewIntArray 5) 3 (NewIntArray 5) 0 3) "null")))
      (case ["arraycopyDstEnd"] (return (fn ^string [] (Arraycopy (NewIntArray 5) 0 (NewIntArray 5) 3 3) "null")))
      (case ["arraycopyObjSrcEnd"] (return (fn ^string [] (Arraycopy (NewRefArray Object_class 5) 3 (NewRefArray Object_class 5) 0 3) "null")))
      (case ["arraycopyTypeMismatch"] (return (fn ^string [] (Arraycopy (NewRefArray Throwable_class 1) 0 (NewRefArray String_class 1) 0 1) "null")))
      (case ["arraycopyOverlap"]
        (return (fn ^string []
                  (let [a (IntArrayOf 1 2 3 4 5)
                        parts (make (slice string) 5)]
                    (Arraycopy a 0 a 1 4)
                    (range [i x (.-A a)] (aset parts i (strconv/Itoa (conv int x))))
                    (strings/Join parts ",")))))
      (case ["divZero"] (return (fn ^string [] (let [a (conv int32 1) b (conv int32 0)] (ires (/ a b))))))
      (case ["remZero"] (return (fn ^string [] (let [a (conv int32 1) b (conv int32 0)] (ires (% a b))))))
      (case ["ldivZero"] (return (fn ^string [] (let [a (conv int64 1) b (conv int64 0)] (strconv/FormatInt (/ a b) 10)))))
      (case ["classCast"] (return (fn ^string [] (sres (StrOfObj (.Cast_O__O String_class (RuntimeException_New)))))))
      (case ["classCastArbace"] (return nil))
      (case ["checkcast"] (return (fn ^string [] (sres (String_Cast (StringBuilder_New))))))
      (case ["suppressNull"] (return (fn ^string [] (.AddSuppressed_Throwable__V (RuntimeException_New) nil) "null")))
      (case ["suppressSelf"] (return (fn ^string [] (let [e (RuntimeException_New_String (Str "x"))] (.AddSuppressed_Throwable__V e e)) "null")))
      (case ["initCauseTwice"] (return (fn ^string [] (sres (StrOfObj (.InitCause_Throwable__Throwable (RuntimeException_New_String_Throwable (Str "a") (Exception_New_String (Str "b"))) (Exception_New_String (Str "c"))))))))
      (case ["initCauseSelf"] (return (fn ^string [] (let [e (RuntimeException_New_String (Str "x"))] (sres (StrOfObj (.InitCause_Throwable__Throwable e e)))))))
      (case ["toStringNoMsg"] (return (fn ^string [] (sres (StrOfObj (IllegalStateException_New))))))
      (case ["toStringMsg"] (return (fn ^string [] (sres (StrOfObj (IllegalStateException_New_String (Str "boom")))))))
      (case ["toStringCause"] (return (fn ^string [] (sres (StrOfObj (RuntimeException_New_Throwable (IllegalStateException_New_String (Str "inner"))))))))
      (case ["causeMsg"] (return (fn ^string [] (sres (.GetMessage__String (RuntimeException_New_Throwable (IllegalStateException_New_String (Str "inner"))))))))
      (case ["causeNull"] (return (fn ^string [] (sres (.GetMessage__String (RuntimeException_New_Throwable nil))))))
      (case ["aioobeInt"] (return (fn ^string [] (sres (.GetMessage__String (ArrayIndexOutOfBoundsException_New_I 7))))))
      (case ["sioobeInt"] (return (fn ^string [] (sres (.GetMessage__String (StringIndexOutOfBoundsException_New_I 7))))))
      (case ["ioobeInt"] (return (fn ^string [] (sres (.GetMessage__String (IndexOutOfBoundsException_New_I 7))))))
      (case ["assertionErrorObj"] (return (fn ^string [] (sres (.GetMessage__String (AssertionError_New_O (Str "5")))))))
      (case ["assertionErrorThrowable"] (return (fn ^string [] (sres (StrOfObj (.GetCause__Throwable (AssertionError_New_O (RuntimeException_New_String (Str "c")))))))))
      (case ["eiieToString"] (return (fn ^string [] (sres (StrOfObj (ExceptionInInitializerError_New_Throwable (RuntimeException_New_String (Str "c"))))))))
      (case ["eiieMessage"] (return (fn ^string [] (sres (.GetMessage__String (ExceptionInInitializerError_New_Throwable (RuntimeException_New_String (Str "c"))))))))
      (case ["steToString"] (return (fn ^string [] (sres (StrOfObj (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") (Str "B.java") 12))))))
      (case ["steToStringJdk"] (return nil))
      (case ["steToStringNoFile"] (return (fn ^string [] (sres (StrOfObj (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") nil -1))))))
      (case ["steToStringNative"] (return (fn ^string [] (sres (StrOfObj (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") (Str "B.java") -2))))))
      (case ["steHash"] (return (fn ^string [] (ires (.HashCode__I (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") (Str "B.java") 12))))))
      (case ["steHashNoFile"] (return (fn ^string [] (ires (.HashCode__I (StackTraceElement_New_String_String_String_I (Str "a.B") (Str "m") nil -1))))))
      (case ["waitNotOwner"] (return (fn ^string [] (Wait (Object_New)) "null")))
      (case ["notifyNotOwner"] (return (fn ^string [] (Notify (Object_New)) "null")))
      (case ["waitNegative"] (return (fn ^string [] (let [o (Object_New)] (MonitorEnter o) (defer (MonitorExit o)) (WaitTimeout o -1 0)) "null")))
      (case ["waitNanos"] (return (fn ^string [] (let [o (Object_New)] (MonitorEnter o) (defer (MonitorExit o)) (WaitTimeout o 1 1000000)) "null")))
      (case ["objectToString"]
        (return (fn ^string []
                  (let [s (.String (StrOfObj (Object_New)))]
                    (subslice s _ (strings/Index s "@"))))))
      (case ["arrayClassNames"]
        (return (fn ^string []
                  (strings/Join (lit (slice string) (.GoName (GetClass (NewIntArray 0))) (.GoName (GetClass (NewRefArray Object_class 0)))
                                     (.GoName (GetClass (NewRefArray String_class 0))) (.GoName (GetClass (NewMultiArray (.ArrayClass (.ArrayClass Prim_int)) 0 0)))
                                     (.GoName (GetClass (NewBooleanArray 0))) (.GoName (GetClass (NewMultiArray (.ArrayClass (.ArrayClass String_class)) 0 0))))
                                ","))))
      (case ["arraySimpleNames"]
        (return (fn ^string []
                  (+ (.String (.GetSimpleName__String (GetClass (NewIntArray 0)))) ","
                     (.String (.GetSimpleName__String (.ArrayClass (.ArrayClass String_class))))))))
      (case ["arrayModifiers"]
        (return (fn ^string []
                  (+ (ires (.GetModifiers__I (.ArrayClass Prim_int))) "," (ires (.GetModifiers__I (.ArrayClass String_class))) ","
                     (ires (.GetModifiers__I (.ArrayClass (.ArrayClass Prim_int))))))))
      (case ["primModifiers"] (return (fn ^string [] (ires (.GetModifiers__I Prim_int)))))
      (case ["classToString"]
        (return (fn ^string []
                  (+ (.String (.ToString__String String_class)) "," (.String (.ToString__String Prim_int)) ","
                     (.String (.ToString__String Comparable_class)) "," (.String (.ToString__String (.ArrayClass Prim_int)))))))
      (case ["arrayAssignable"]
        (return (fn ^string []
                  (let [oa (.ArrayClass Object_class)
                        sa (.ArrayClass String_class)
                        ia (.ArrayClass Prim_int)]
                    (strings/Join (lit (slice string)
                                       (bres (.IsAssignableFrom_Class__Z oa sa)) (bres (.IsAssignableFrom_Class__Z sa oa))
                                       (bres (.IsAssignableFrom_Class__Z Object_class ia)) (bres (.IsAssignableFrom_Class__Z Cloneable_class ia))
                                       (bres (.IsAssignableFrom_Class__Z Serializable_class sa)) (bres (.IsAssignableFrom_Class__Z oa ia))
                                       (bres (.IsAssignableFrom_Class__Z oa (.ArrayClass ia))))
                                  ",")))))
      (case ["forNameArray"] (return (fn ^string [] (.GoName (Class_ForName_String__Class (Str "[[Ljava.lang.String;"))))))
      (case ["forNameMissing"] (return (fn ^string [] (.GoName (Class_ForName_String__Class (Str "no.such.Class")))))))
    nil))
