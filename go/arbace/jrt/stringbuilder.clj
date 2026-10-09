;; jrt: java.lang.StringBuilder and StringBuffer (C2G-SPEC §7.5): Java's
;; AbstractStringBuilder over a []uint16 whose length is the count and whose capacity is
;; Java's capacity (grown as Java grows it).
(in-ns 'go.arbace.jrt)

(go/file "stringbuilder.go"
  :imports [[strconv "strconv"]])

(go/type sbuf
  "sbuf is AbstractStringBuilder's state and behaviour, embedded in StringBuilder and
StringBuffer.\n"
  (struct ^{:tag (slice uint16)} value))

(go/var AbstractStringBuilder_class
  (Define (addr (lit ClassInfo :Name "java.lang.AbstractStringBuilder" :Kind KindClass
                     :Modifiers AccAbstract :Super Object_class
                     :Interfaces (lit (slice (* Class)) Appendable_class CharSequence_class)
                     :Go "arbace/jrt.sbuf"))))

(go/func newSbuf ^sbuf [^int32 capacity]
  (when (< capacity 0)
    (panic (negativeArraySize capacity)))
  (lit sbuf :value (make (slice uint16) 0 capacity)))

(go/method ensure "ensure grows the capacity to at least min as Java does: max(min, 2*old+2).\n"
  [^{:tag (* sbuf)} b ^int min]
  (let [old (cap (.-value b))]
    (when (> min old)
      (let [n (+ (* 2 old) 2)]
        (when (< n min) (set! n min))
        (let [v (make (slice uint16) (len (.-value b)) n)]
          (copy v (.-value b))
          (set! (.-value b) v))))))

(go/method appendUnits [^{:tag (* sbuf)} b ^{:tag (slice uint16)} s]
  (.ensure b (+ (len (.-value b)) (len s)))
  (set! (.-value b) (append (.-value b) (spread s))))

(go/method appendStr [^{:tag (* sbuf)} b ^{:tag (* String)} s]
  (if (== s nil)
    (.appendUnits b (.-value litNull))
    (.appendUnits b (.-value s))))

(go/method appendChar [^{:tag (* sbuf)} b ^uint16 c]
  (.ensure b (+ (len (.-value b)) 1))
  (set! (.-value b) (append (.-value b) c)))

(go/method appendCS [^{:tag (* sbuf)} b ^CharSequence s]
  (when (== s nil)
    (.appendUnits b (.-value litNull))
    (return))
  (let [(values str ok) (assert (* String) s)]
    (when ok
      (.appendUnits b (.-value str))
      (return)))
  (.appendCSRange b s 0 (.Length__I s)))

(go/method appendCSRange [^{:tag (* sbuf)} b ^CharSequence s ^int32 start ^int32 end]
  (when (== s nil)
    (set! s litNull))
  (let [n (.Length__I s)]
    (when (or (< start 0) (> start end) (> end n))
      (panic (IndexOutOfBoundsException_New_String
               (Str (+ "Range [" (strconv/Itoa (conv int start)) ", " (strconv/Itoa (conv int end))
                       ") out of bounds for length " (strconv/Itoa (conv int n))))))))
  (.ensure b (+ (len (.-value b)) (conv int (- end start))))
  (let [(values str ok) (assert (* String) s)]
    (when ok
      (set! (.-value b) (append (.-value b) (spread (subslice (.-value str) start end))))
      (return)))
  (for [i start] (< i end) (inc! i)
    (set! (.-value b) (append (.-value b) (.CharAt_I__C s i)))))

(go/method appendChars [^{:tag (* sbuf)} b ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (let [end (+ offset n)
        l (len (.-A (NN a)))]
    (when (or (< offset 0) (> offset end) (> (conv int end) l))
      (panic (IndexOutOfBoundsException_New_String
               (Str (+ "Range [" (strconv/Itoa (conv int offset)) ", " (strconv/Itoa (conv int end))
                       ") out of bounds for length " (strconv/Itoa l))))))
    (.appendUnits b (subslice (.-A a) offset end))))

(go/method insertCSRange
  "insertCSRange is AbstractStringBuilder.insert(dstOffset, s, start, end): null as \"null\",
the offset checked, then the range (IndexOutOfBoundsException).\n"
  [^{:tag (* sbuf)} b ^int32 o ^CharSequence s ^int32 start ^int32 end]
  (when (== s nil)
    (set! s litNull))
  (checkFromTo o (conv int32 (len (.-value b))) (len (.-value b)))
  (let [n (.Length__I s)]
    (when (or (< start 0) (> start end) (> end n))
      (panic (IndexOutOfBoundsException_New_String
               (Str (+ "Range [" (strconv/Itoa (conv int start)) ", " (strconv/Itoa (conv int end))
                       ") out of bounds for length " (strconv/Itoa (conv int n))))))))
  (let [u (make (slice uint16) 0 (- end start))]
    (for [i start] (< i end) (inc! i)
      (set! u (append u (.CharAt_I__C s i))))
    (.insertUnits b o u)))

(go/method insertChars
  "insertChars is AbstractStringBuilder.insert(index, char[], offset, len): the index checked,
then the range (StringIndexOutOfBoundsException).\n"
  [^{:tag (* sbuf)} b ^int32 o ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (checkFromTo o (conv int32 (len (.-value b))) (len (.-value b)))
  (checkFromTo offset (+ offset n) (len (.-A (NN a))))
  (.insertUnits b o (subslice (.-A a) offset (+ offset n))))

(go/method appendCodePoint [^{:tag (* sbuf)} b ^int32 cp]
  (when (or (< cp 0) (> cp 0x10FFFF))
    (panic (IllegalArgumentException_New_String
             (Str (+ "Not a valid Unicode code point: 0x" (hexUpper (conv uint32 cp)))))))
  (.appendUnits b (appendCodePoint nil cp)))

(go/func hexUpper ^string [^uint32 v]
  (let [s (strconv/FormatUint (conv uint64 v) 16)
        b (conv (slice byte) s)]
    (range [i c b]
      (when (and (>= c \a) (<= c \f))
        (aset b i (- c 32))))
    (conv string b)))

(go/method repeatCP [^{:tag (* sbuf)} b ^int32 cp ^int32 count]
  (when (< count 0)
    (panic (IllegalArgumentException_New_String (Concat (Str "count is negative: ") (StrOfInt count)))))
  (when (== count 0)
    (return))
  (when (or (< cp 0) (> cp 0x10FFFF))
    (panic (IllegalArgumentException_New_String
             (Str (+ "Not a valid Unicode code point: 0x" (hexUpper (conv uint32 cp)))))))
  (let [u (appendCodePoint nil cp)]
    (.ensure b (+ (len (.-value b)) (* (len u) (conv int count))))
    (for [i (conv int32 0)] (< i count) (inc! i)
      (set! (.-value b) (append (.-value b) (spread u))))))

(go/method repeatCS [^{:tag (* sbuf)} b ^CharSequence s ^int32 count]
  (when (< count 0)
    (panic (IllegalArgumentException_New_String (Concat (Str "count is negative: ") (StrOfInt count)))))
  (when (== s nil)
    (set! s litNull))
  (let [u (.-value (.ToString__String s))]
    (.ensure b (+ (len (.-value b)) (* (len u) (conv int count))))
    (for [i (conv int32 0)] (< i count) (inc! i)
      (set! (.-value b) (append (.-value b) (spread u))))))

(go/method charAt ^uint16 [^{:tag (* sbuf)} b ^int32 i]
  (checkIndex i (len (.-value b)))
  (aget (.-value b) i))

(go/method setCharAt [^{:tag (* sbuf)} b ^int32 i ^uint16 c]
  (checkIndex i (len (.-value b)))
  (aset (.-value b) i c))

(go/method deleteRange [^{:tag (* sbuf)} b ^int32 start ^int32 end]
  (let [n (conv int32 (len (.-value b)))]
    (when (> end n) (set! end n))
    (checkFromTo start end (conv int n))
    (when (> end start)
      (set! (.-value b) (append (subslice (.-value b) _ start) (spread (subslice (.-value b) end)))))))

(go/method deleteCharAt [^{:tag (* sbuf)} b ^int32 i]
  (checkIndex i (len (.-value b)))
  (set! (.-value b) (append (subslice (.-value b) _ i) (spread (subslice (.-value b) (+ i 1))))))

(go/method insertUnits [^{:tag (* sbuf)} b ^int32 offset ^{:tag (slice uint16)} s]
  (let [n (len (.-value b))]
    (checkFromTo offset (conv int32 n) n)
    (.ensure b (+ n (len s)))
    (set! (.-value b) (subslice (.-value b) _ (+ n (len s))))
    (copy (subslice (.-value b) (+ (conv int offset) (len s))) (subslice (.-value b) offset n))
    (copy (subslice (.-value b) offset) s)))

(go/method replaceRange [^{:tag (* sbuf)} b ^int32 start ^int32 end ^{:tag (* String)} s]
  (let [n (conv int32 (len (.-value b)))]
    (when (> end n) (set! end n))
    (checkFromTo start end (conv int n))
    (let [u (.-value (NN s))
          tail (append (lit (slice uint16)) (spread (subslice (.-value b) end)))]
      (set! (.-value b) (subslice (.-value b) _ start))
      (.appendUnits b u)
      (.appendUnits b tail))))

(go/method setLength [^{:tag (* sbuf)} b ^int32 n]
  (when (< n 0)
    (panic (StringIndexOutOfBoundsException_New_I n)))
  (.ensure b (conv int n))
  (let [old (len (.-value b))]
    (set! (.-value b) (subslice (.-value b) _ n))
    (for [i old] (< i (conv int n)) (inc! i)
      (aset (.-value b) i 0))))

(go/method reverse [^{:tag (* sbuf)} b]
  (let [v (.-value b)
        n (len v)
        surrogates false]
    (for [i 0] (< i (/ n 2)) (inc! i)
      (let [j (- n 1 i)]
        (when (or (isSurrogate (aget v i)) (isSurrogate (aget v j)))
          (set! surrogates true))
        (set! (values (aget v i) (aget v j)) (values (aget v j) (aget v i)))))
    (when (and (== (% n 2) 1) (isSurrogate (aget v (/ n 2))))
      (set! surrogates true))
    (when surrogates
      ;; put the pairs, reversed into low-high, back in order
      (for [i 0] (< i (- n 1)) (inc! i)
        (when (and (isLowSurrogate (aget v i)) (isHighSurrogate (aget v (+ i 1))))
          (set! (values (aget v i) (aget v (+ i 1))) (values (aget v (+ i 1)) (aget v i)))
          (inc! i))))))

(go/method substring ^{:tag (* String)} [^{:tag (* sbuf)} b ^int32 start ^int32 end]
  (checkFromTo start end (len (.-value b)))
  (NewStringUTF16 (subslice (.-value b) start end)))

(go/method getChars [^{:tag (* sbuf)} b ^int32 srcBegin ^int32 srcEnd ^{:tag (* CharArray)} dst ^int32 dstBegin]
  (checkFromTo srcBegin srcEnd (len (.-value b)))
  (let [n (- srcEnd srcBegin)
        l (len (.-A (NN dst)))]
    (when (or (< dstBegin 0) (> dstBegin (+ dstBegin n)) (> (conv int (+ dstBegin n)) l))
      (panic (IndexOutOfBoundsException_New_String
               (Str (+ "Range [" (strconv/Itoa (conv int dstBegin)) ", " (strconv/Itoa (conv int (+ dstBegin n)))
                       ") out of bounds for length " (strconv/Itoa l))))))
    (copy (subslice (.-A dst) dstBegin) (subslice (.-value b) srcBegin srcEnd))))

(go/method codePointAt ^int32 [^{:tag (* sbuf)} b ^int32 i]
  (checkIndex i (len (.-value b)))
  (codePointAt (.-value b) (conv int i) (len (.-value b))))

(go/method codePointBefore ^int32 [^{:tag (* sbuf)} b ^int32 i]
  (checkIndex (- i 1) (len (.-value b)))
  (codePointBefore (.-value b) (conv int i) 0))

(go/method codePointCount ^int32 [^{:tag (* sbuf)} b ^int32 start ^int32 end]
  (let [n (len (.-value b))]
    (when (or (< start 0) (> start end) (> (conv int end) n))
      (panic (IndexOutOfBoundsException_New_String
               (Str (+ "Range [" (strconv/Itoa (conv int start)) ", " (strconv/Itoa (conv int end))
                       ") out of bounds for length " (strconv/Itoa n)))))))
  (codePointCount (subslice (.-value b) start end)))

(go/method offsetByCodePoints ^int32 [^{:tag (* sbuf)} b ^int32 index ^int32 offset]
  (offsetByCodePoints (.-value b) index offset))

(go/method ensureCapacity [^{:tag (* sbuf)} b ^int32 min]
  (when (> min 0)
    (.ensure b (conv int min))))

(go/method trimToSize [^{:tag (* sbuf)} b]
  (when (< (len (.-value b)) (cap (.-value b)))
    (let [v (make (slice uint16) (len (.-value b)))]
      (copy v (.-value b))
      (set! (.-value b) v))))

(go/method toString ^{:tag (* String)} [^{:tag (* sbuf)} b]
  (NewStringUTF16 (.-value b)))

;; ---------------------------------------------------------------------------------------
;; java.lang.StringBuilder

(go/type StringBuilder "StringBuilder is java.lang.StringBuilder (final: *StringBuilder).\n"
  (struct Object sbuf))

(go/var StringBuilder_class
  (Define (addr (lit ClassInfo :Name "java.lang.StringBuilder" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super AbstractStringBuilder_class
                     :Interfaces (lit (slice (* Class)) Serializable_class Comparable_class CharSequence_class)
                     :Go "arbace/jrt.StringBuilder"))))

(go/func StringBuilder_New "StringBuilder_New is new StringBuilder(): capacity 16.\n" ^{:tag (* StringBuilder)} []
  (addr (lit StringBuilder :sbuf (newSbuf 16))))
(go/func StringBuilder_New_I ^{:tag (* StringBuilder)} [^int32 capacity]
  (addr (lit StringBuilder :sbuf (newSbuf capacity))))
(go/func StringBuilder_New_String ^{:tag (* StringBuilder)} [^{:tag (* String)} s]
  (let [t (addr (lit StringBuilder :sbuf (newSbuf (+ (conv int32 (len (.-value (NN s)))) 16))))]
    (.appendStr (addr (.-sbuf t)) s)
    t))
(go/func StringBuilder_New_CharSequence ^{:tag (* StringBuilder)} [^CharSequence s]
  (let [t (addr (lit StringBuilder :sbuf (newSbuf (+ (.Length__I (csNN s)) 16))))]
    (.appendCS (addr (.-sbuf t)) s)
    t))

(go/method Ref ^any [^{:tag (* StringBuilder)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* StringBuilder)} t] StringBuilder_class)
(go/method Clone__O ^any [^{:tag (* StringBuilder)} t] (panic (CloneNotSupported t)))
(go/method Is_Serializable [^{:tag (* StringBuilder)} t])
(go/method Is_Comparable [^{:tag (* StringBuilder)} t])
(go/method Is_CharSequence [^{:tag (* StringBuilder)} t])
(go/method Is_Appendable [^{:tag (* StringBuilder)} t])
(go/method Append_C__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^uint16 c]
  (.appendChar (addr (.-sbuf t)) c)
  t)
(go/method Append_D__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^float64 d]
  (.appendStr (addr (.-sbuf t)) (StrOfDouble d))
  t)
(go/method Append_F__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^float32 f]
  (.appendStr (addr (.-sbuf t)) (StrOfFloat f))
  t)
(go/method Append_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 i]
  (.appendStr (addr (.-sbuf t)) (StrOfInt i))
  t)
(go/method Append_J__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int64 l]
  (.appendStr (addr (.-sbuf t)) (StrOfLong l))
  t)
(go/method Append_Z__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^bool z]
  (.appendStr (addr (.-sbuf t)) (StrOfBool z))
  t)
(go/method Append_CharSequence__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^CharSequence s]
  (.appendCS (addr (.-sbuf t)) s)
  t)
(go/method Append_CharSequence_I_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^CharSequence s ^int32 start ^int32 end]
  (.appendCSRange (addr (.-sbuf t)) s start end)
  t)
(go/method Append_O__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^any o]
  (.appendStr (addr (.-sbuf t)) (StrOfObj o))
  t)
(go/method Append_String__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^{:tag (* String)} s]
  (.appendStr (addr (.-sbuf t)) s)
  t)
(go/method Append_StringBuffer__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^{:tag (* StringBuffer)} s]
  (.appendStr (addr (.-sbuf t)) (StrOfObj (.Ref s)))
  t)
(go/method Append_C1__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^{:tag (* CharArray)} a]
  (.appendUnits (addr (.-sbuf t)) (.-A (NN a)))
  t)
(go/method Append_C1_I_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (.appendChars (addr (.-sbuf t)) a offset n)
  t)
(go/method AppendCodePoint_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 cp]
  (.appendCodePoint (addr (.-sbuf t)) cp)
  t)
(go/method Append_C__Appendable ^Appendable [^{:tag (* StringBuilder)} t ^uint16 c]
  (.appendChar (addr (.-sbuf t)) c)
  t)
(go/method Append_CharSequence__Appendable ^Appendable [^{:tag (* StringBuilder)} t ^CharSequence s]
  (.appendCS (addr (.-sbuf t)) s)
  t)
(go/method Append_CharSequence_I_I__Appendable ^Appendable [^{:tag (* StringBuilder)} t ^CharSequence s ^int32 start ^int32 end]
  (.appendCSRange (addr (.-sbuf t)) s start end)
  t)
(go/method Delete_I_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 start ^int32 end]
  (.deleteRange (addr (.-sbuf t)) start end)
  t)
(go/method DeleteCharAt_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 i]
  (.deleteCharAt (addr (.-sbuf t)) i)
  t)
(go/method Insert_I_C__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^uint16 c]
  (.insertUnits (addr (.-sbuf t)) o (lit (slice uint16) c))
  t)
(go/method Insert_I_String__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^{:tag (* String)} s]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj (.Ref s))))
  t)
(go/method Insert_I_C1__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^{:tag (* CharArray)} a]
  (.insertUnits (addr (.-sbuf t)) o (.-A (NN a)))
  t)
(go/method Insert_I_O__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^any x]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj x)))
  t)
(go/method Insert_I_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^int32 i]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfInt i)))
  t)
(go/method Insert_I_J__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^int64 l]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfLong l)))
  t)
(go/method Insert_I_Z__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^bool z]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfBool z)))
  t)
(go/method Insert_I_CharSequence__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^CharSequence s]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj s)))
  t)
(go/method Insert_I_D__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^float64 d]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfDouble d)))
  t)
(go/method Insert_I_F__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 o ^float32 f]
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfFloat f)))
  t)
(go/method Insert_I_CharSequence_I_I__StringBuilder ^{:tag (* StringBuilder)}
  [^{:tag (* StringBuilder)} t ^int32 o ^CharSequence s ^int32 start ^int32 end]
  (.insertCSRange (addr (.-sbuf t)) o s start end)
  t)
(go/method Insert_I_C1_I_I__StringBuilder ^{:tag (* StringBuilder)}
  [^{:tag (* StringBuilder)} t ^int32 o ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (.insertChars (addr (.-sbuf t)) o a offset n)
  t)
(go/method Replace_I_I_String__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 start ^int32 end ^{:tag (* String)} s]
  (.replaceRange (addr (.-sbuf t)) start end s)
  t)
(go/method Reverse__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t]
  (.reverse (addr (.-sbuf t)))
  t)
(go/method Repeat_I_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^int32 cp ^int32 count]
  (.repeatCP (addr (.-sbuf t)) cp count)
  t)
(go/method Repeat_CharSequence_I__StringBuilder ^{:tag (* StringBuilder)} [^{:tag (* StringBuilder)} t ^CharSequence s ^int32 count]
  (.repeatCS (addr (.-sbuf t)) s count)
  t)
(go/method CharAt_I__C ^uint16 [^{:tag (* StringBuilder)} t ^int32 i]
  (.charAt (addr (.-sbuf t)) i))
(go/method SetCharAt_I_C__V [^{:tag (* StringBuilder)} t ^int32 i ^uint16 c]
  (.setCharAt (addr (.-sbuf t)) i c))
(go/method SetLength_I__V [^{:tag (* StringBuilder)} t ^int32 n]
  (.setLength (addr (.-sbuf t)) n))
(go/method Length__I ^int32 [^{:tag (* StringBuilder)} t]
  (conv int32 (len (.-value (.-sbuf t)))))
(go/method IsEmpty__Z ^bool [^{:tag (* StringBuilder)} t]
  (== (len (.-value (.-sbuf t))) 0))
(go/method Capacity__I ^int32 [^{:tag (* StringBuilder)} t]
  (conv int32 (cap (.-value (.-sbuf t)))))
(go/method EnsureCapacity_I__V [^{:tag (* StringBuilder)} t ^int32 min]
  (.ensureCapacity (addr (.-sbuf t)) min))
(go/method TrimToSize__V [^{:tag (* StringBuilder)} t]
  (.trimToSize (addr (.-sbuf t))))
(go/method CodePointAt_I__I ^int32 [^{:tag (* StringBuilder)} t ^int32 i]
  (.codePointAt (addr (.-sbuf t)) i))
(go/method CodePointBefore_I__I ^int32 [^{:tag (* StringBuilder)} t ^int32 i]
  (.codePointBefore (addr (.-sbuf t)) i))
(go/method CodePointCount_I_I__I ^int32 [^{:tag (* StringBuilder)} t ^int32 a ^int32 b]
  (.codePointCount (addr (.-sbuf t)) a b))
(go/method OffsetByCodePoints_I_I__I ^int32 [^{:tag (* StringBuilder)} t ^int32 index ^int32 offset]
  (.offsetByCodePoints (addr (.-sbuf t)) index offset))
(go/method Substring_I__String ^{:tag (* String)} [^{:tag (* StringBuilder)} t ^int32 start]
  (.substring (addr (.-sbuf t)) start (conv int32 (len (.-value (.-sbuf t))))))
(go/method Substring_I_I__String ^{:tag (* String)} [^{:tag (* StringBuilder)} t ^int32 start ^int32 end]
  (.substring (addr (.-sbuf t)) start end))
(go/method SubSequence_I_I__CharSequence ^CharSequence [^{:tag (* StringBuilder)} t ^int32 start ^int32 end]
  (.substring (addr (.-sbuf t)) start end))
(go/method GetChars_I_I_C1_I__V [^{:tag (* StringBuilder)} t ^int32 a ^int32 b ^{:tag (* CharArray)} dst ^int32 d]
  (.getChars (addr (.-sbuf t)) a b dst d))
(go/method IndexOf_String__I ^int32 [^{:tag (* StringBuilder)} t ^{:tag (* String)} s]
  (indexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) 0 (conv int32 (len (.-value (.-sbuf t))))))
(go/method IndexOf_String_I__I ^int32 [^{:tag (* StringBuilder)} t ^{:tag (* String)} s ^int32 from]
  (indexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) from (conv int32 (len (.-value (.-sbuf t))))))
(go/method LastIndexOf_String__I ^int32 [^{:tag (* StringBuilder)} t ^{:tag (* String)} s]
  (lastIndexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) (conv int32 (len (.-value (.-sbuf t))))))
(go/method LastIndexOf_String_I__I ^int32 [^{:tag (* StringBuilder)} t ^{:tag (* String)} s ^int32 from]
  (lastIndexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) from))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* StringBuilder)} t]
  (.toString (addr (.-sbuf t))))
(go/method CompareTo_StringBuilder__I ^int32 [^{:tag (* StringBuilder)} t ^{:tag (* StringBuilder)} o]
  (compareUnits (.-value (.-sbuf t)) (.-value (.-sbuf (NN o)))))
(go/method CompareTo_O__I ^int32 [^{:tag (* StringBuilder)} t ^any o]
  (compareUnits (.-value (.-sbuf t)) (.-value (.-sbuf (StringBuilder_Cast o)))))
(go/func StringBuilder_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* StringBuilder) x)] ok))
(go/func StringBuilder_Cast ^{:tag (* StringBuilder)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* StringBuilder) x)]
    (when (not ok) (panic (ClassCast x StringBuilder_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; java.lang.StringBuffer

(go/type StringBuffer "StringBuffer is java.lang.StringBuffer (final: *StringBuffer), its methods synchronized on itself.\n"
  (struct Object sbuf))

(go/var StringBuffer_class
  (Define (addr (lit ClassInfo :Name "java.lang.StringBuffer" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super AbstractStringBuilder_class
                     :Interfaces (lit (slice (* Class)) Serializable_class Comparable_class CharSequence_class)
                     :Go "arbace/jrt.StringBuffer"))))

(go/func StringBuffer_New "StringBuffer_New is new StringBuffer(): capacity 16.\n" ^{:tag (* StringBuffer)} []
  (addr (lit StringBuffer :sbuf (newSbuf 16))))
(go/func StringBuffer_New_I ^{:tag (* StringBuffer)} [^int32 capacity]
  (addr (lit StringBuffer :sbuf (newSbuf capacity))))
(go/func StringBuffer_New_String ^{:tag (* StringBuffer)} [^{:tag (* String)} s]
  (let [t (addr (lit StringBuffer :sbuf (newSbuf (+ (conv int32 (len (.-value (NN s)))) 16))))]
    (.appendStr (addr (.-sbuf t)) s)
    t))
(go/func StringBuffer_New_CharSequence ^{:tag (* StringBuffer)} [^CharSequence s]
  (let [t (addr (lit StringBuffer :sbuf (newSbuf (+ (.Length__I (csNN s)) 16))))]
    (.appendCS (addr (.-sbuf t)) s)
    t))

(go/method Ref ^any [^{:tag (* StringBuffer)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* StringBuffer)} t] StringBuffer_class)
(go/method Clone__O ^any [^{:tag (* StringBuffer)} t] (panic (CloneNotSupported t)))
(go/method Is_Serializable [^{:tag (* StringBuffer)} t])
(go/method Is_Comparable [^{:tag (* StringBuffer)} t])
(go/method Is_CharSequence [^{:tag (* StringBuffer)} t])
(go/method Is_Appendable [^{:tag (* StringBuffer)} t])
(go/method Append_C__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^uint16 c]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendChar (addr (.-sbuf t)) c)
  t)
(go/method Append_D__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^float64 d]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfDouble d))
  t)
(go/method Append_F__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^float32 f]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfFloat f))
  t)
(go/method Append_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfInt i))
  t)
(go/method Append_J__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int64 l]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfLong l))
  t)
(go/method Append_Z__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^bool z]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfBool z))
  t)
(go/method Append_CharSequence__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^CharSequence s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendCS (addr (.-sbuf t)) s)
  t)
(go/method Append_CharSequence_I_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^CharSequence s ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendCSRange (addr (.-sbuf t)) s start end)
  t)
(go/method Append_O__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^any o]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfObj o))
  t)
(go/method Append_String__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^{:tag (* String)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) s)
  t)
(go/method Append_StringBuffer__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^{:tag (* StringBuffer)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendStr (addr (.-sbuf t)) (StrOfObj (.Ref s)))
  t)
(go/method Append_C1__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^{:tag (* CharArray)} a]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendUnits (addr (.-sbuf t)) (.-A (NN a)))
  t)
(go/method Append_C1_I_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendChars (addr (.-sbuf t)) a offset n)
  t)
(go/method AppendCodePoint_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 cp]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendCodePoint (addr (.-sbuf t)) cp)
  t)
(go/method Append_C__Appendable ^Appendable [^{:tag (* StringBuffer)} t ^uint16 c]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendChar (addr (.-sbuf t)) c)
  t)
(go/method Append_CharSequence__Appendable ^Appendable [^{:tag (* StringBuffer)} t ^CharSequence s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendCS (addr (.-sbuf t)) s)
  t)
(go/method Append_CharSequence_I_I__Appendable ^Appendable [^{:tag (* StringBuffer)} t ^CharSequence s ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.appendCSRange (addr (.-sbuf t)) s start end)
  t)
(go/method Delete_I_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.deleteRange (addr (.-sbuf t)) start end)
  t)
(go/method DeleteCharAt_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.deleteCharAt (addr (.-sbuf t)) i)
  t)
(go/method Insert_I_C__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^uint16 c]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (lit (slice uint16) c))
  t)
(go/method Insert_I_String__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^{:tag (* String)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj (.Ref s))))
  t)
(go/method Insert_I_C1__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^{:tag (* CharArray)} a]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-A (NN a)))
  t)
(go/method Insert_I_O__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^any x]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj x)))
  t)
(go/method Insert_I_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfInt i)))
  t)
(go/method Insert_I_J__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^int64 l]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfLong l)))
  t)
(go/method Insert_I_Z__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^bool z]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfBool z)))
  t)
(go/method Insert_I_CharSequence__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^CharSequence s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfObj s)))
  t)
(go/method Insert_I_D__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^float64 d]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfDouble d)))
  t)
(go/method Insert_I_F__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 o ^float32 f]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertUnits (addr (.-sbuf t)) o (.-value (StrOfFloat f)))
  t)
(go/method Insert_I_CharSequence_I_I__StringBuffer ^{:tag (* StringBuffer)}
  [^{:tag (* StringBuffer)} t ^int32 o ^CharSequence s ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertCSRange (addr (.-sbuf t)) o s start end)
  t)
(go/method Insert_I_C1_I_I__StringBuffer ^{:tag (* StringBuffer)}
  [^{:tag (* StringBuffer)} t ^int32 o ^{:tag (* CharArray)} a ^int32 offset ^int32 n]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.insertChars (addr (.-sbuf t)) o a offset n)
  t)
(go/method Replace_I_I_String__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 start ^int32 end ^{:tag (* String)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.replaceRange (addr (.-sbuf t)) start end s)
  t)
(go/method Reverse__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.reverse (addr (.-sbuf t)))
  t)
(go/method Repeat_I_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^int32 cp ^int32 count]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.repeatCP (addr (.-sbuf t)) cp count)
  t)
(go/method Repeat_CharSequence_I__StringBuffer ^{:tag (* StringBuffer)} [^{:tag (* StringBuffer)} t ^CharSequence s ^int32 count]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.repeatCS (addr (.-sbuf t)) s count)
  t)
(go/method CharAt_I__C ^uint16 [^{:tag (* StringBuffer)} t ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.charAt (addr (.-sbuf t)) i))
(go/method SetCharAt_I_C__V [^{:tag (* StringBuffer)} t ^int32 i ^uint16 c]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.setCharAt (addr (.-sbuf t)) i c))
(go/method SetLength_I__V [^{:tag (* StringBuffer)} t ^int32 n]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.setLength (addr (.-sbuf t)) n))
(go/method Length__I ^int32 [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (conv int32 (len (.-value (.-sbuf t)))))
(go/method IsEmpty__Z ^bool [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (== (len (.-value (.-sbuf t))) 0))
(go/method Capacity__I ^int32 [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (conv int32 (cap (.-value (.-sbuf t)))))
(go/method EnsureCapacity_I__V [^{:tag (* StringBuffer)} t ^int32 min]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.ensureCapacity (addr (.-sbuf t)) min))
(go/method TrimToSize__V [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.trimToSize (addr (.-sbuf t))))
(go/method CodePointAt_I__I ^int32 [^{:tag (* StringBuffer)} t ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.codePointAt (addr (.-sbuf t)) i))
(go/method CodePointBefore_I__I ^int32 [^{:tag (* StringBuffer)} t ^int32 i]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.codePointBefore (addr (.-sbuf t)) i))
(go/method CodePointCount_I_I__I ^int32 [^{:tag (* StringBuffer)} t ^int32 a ^int32 b]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.codePointCount (addr (.-sbuf t)) a b))
(go/method OffsetByCodePoints_I_I__I ^int32 [^{:tag (* StringBuffer)} t ^int32 index ^int32 offset]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.offsetByCodePoints (addr (.-sbuf t)) index offset))
(go/method Substring_I__String ^{:tag (* String)} [^{:tag (* StringBuffer)} t ^int32 start]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.substring (addr (.-sbuf t)) start (conv int32 (len (.-value (.-sbuf t))))))
(go/method Substring_I_I__String ^{:tag (* String)} [^{:tag (* StringBuffer)} t ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.substring (addr (.-sbuf t)) start end))
(go/method SubSequence_I_I__CharSequence ^CharSequence [^{:tag (* StringBuffer)} t ^int32 start ^int32 end]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.substring (addr (.-sbuf t)) start end))
(go/method GetChars_I_I_C1_I__V [^{:tag (* StringBuffer)} t ^int32 a ^int32 b ^{:tag (* CharArray)} dst ^int32 d]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.getChars (addr (.-sbuf t)) a b dst d))
(go/method IndexOf_String__I ^int32 [^{:tag (* StringBuffer)} t ^{:tag (* String)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (indexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) 0 (conv int32 (len (.-value (.-sbuf t))))))
(go/method IndexOf_String_I__I ^int32 [^{:tag (* StringBuffer)} t ^{:tag (* String)} s ^int32 from]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (indexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) from (conv int32 (len (.-value (.-sbuf t))))))
(go/method LastIndexOf_String__I ^int32 [^{:tag (* StringBuffer)} t ^{:tag (* String)} s]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (lastIndexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) (conv int32 (len (.-value (.-sbuf t))))))
(go/method LastIndexOf_String_I__I ^int32 [^{:tag (* StringBuffer)} t ^{:tag (* String)} s ^int32 from]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (lastIndexOfUnits (.-value (.-sbuf t)) (.-value (NN s)) from))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* StringBuffer)} t]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (.toString (addr (.-sbuf t))))
(go/method CompareTo_StringBuffer__I ^int32 [^{:tag (* StringBuffer)} t ^{:tag (* StringBuffer)} o]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (compareUnits (.-value (.-sbuf t)) (.-value (.-sbuf (NN o)))))
(go/method CompareTo_O__I ^int32 [^{:tag (* StringBuffer)} t ^any o]
  (.monitorEnter (addr (.-Object t)))
  (defer (.monitorExit (addr (.-Object t))))
  (compareUnits (.-value (.-sbuf t)) (.-value (.-sbuf (StringBuffer_Cast o)))))
(go/func StringBuffer_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* StringBuffer) x)] ok))
(go/func StringBuffer_Cast ^{:tag (* StringBuffer)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* StringBuffer) x)]
    (when (not ok) (panic (ClassCast x StringBuffer_class)))
    v))

