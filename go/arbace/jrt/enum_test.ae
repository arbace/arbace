;; jrt's tests: an enum and a volatile field as c2g writes them.
(in-ns 'go.arbace.jrt)

(go/file "enum_test.go"
  :imports [[testing "testing"]])

;; enum test.Color { RED, GREEN }, as c2g writes a leaf enum (§7.13)
(go/type testColor (struct Enum))
(go/var testColor_class
  (Define (addr (lit ClassInfo :Name "test.Color" :Kind KindEnum :Modifiers (bit-or AccPublic AccFinal AccEnum)
                     :Super Enum_class :Go "arbace/jrt.testColor"))))
(go/var [^{:tag (* testColor)} testColor_RED] [^{:tag (* testColor)} testColor_GREEN])
(go/func init []
  (set! testColor_RED (testColor_New_String_I (Intern "RED") 0))
  (set! testColor_GREEN (testColor_New_String_I (Intern "GREEN") 1))
  (set! (.-Enum (.Info testColor_class))
        (fn ^{:tag (* RefArray)} [] (RefArrayOf testColor_class testColor_RED testColor_GREEN))))
(go/func testColor_New_String_I ^{:tag (* testColor)} [^{:tag (* String)} n ^int32 o]
  (let [t (addr (lit testColor))] (.Ctor_String_I t n o) t))
(go/method Ctor_String_I [^{:tag (* testColor)} t ^{:tag (* String)} n ^int32 o]
  (.Ctor_String_I (.-Enum t) t n o))
(go/method Ref ^any [^{:tag (* testColor)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* testColor)} t] testColor_class)
(go/method Clone__O ^any [^{:tag (* testColor)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* testColor)} t] (.Impl_ToString__String t t))
(go/method CompareTo_Enum__I ^int32 [^{:tag (* testColor)} t ^Enum_I o] (.Impl_CompareTo_Enum__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* testColor)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* testColor)} t] (.Impl_GetDeclaringClass__Class t t))

(go/func TestEnum [^{:tag (* testing/T)} t]
  (when (or (!= (.String (StrOfObj testColor_GREEN)) "GREEN") (!= (.Ordinal__I testColor_GREEN) 1)
            (!= (.Name__String testColor_RED) (Intern "RED")))
    (.Error t "toString, ordinal, name"))
  (when (or (>= (.CompareTo_O__I testColor_RED testColor_GREEN) 0) (!= (.GetDeclaringClass__Class testColor_RED) testColor_class))
    (.Error t "compareTo, getDeclaringClass"))
  (when (!= (Enum_ValueOf_Class_String__Enum testColor_class (Str "GREEN")) testColor_GREEN)
    (.Error t "valueOf"))
  (when (!= (res (fn ^string [] (StrOfObj (Enum_ValueOf_Class_String__Enum testColor_class (Str "BLUE"))) "x"))
            "!java.lang.IllegalArgumentException: No enum constant test.Color.BLUE")
    (.Error t "valueOf of a missing constant"))
  (when (!= (res (fn ^string [] (StrOfObj (.Clone__O testColor_RED)) "x")) "!java.lang.CloneNotSupportedException")
    (.Error t "clone"))
  ;; an enum switch reads F_ordinal (§7.8)
  (let [c testColor_GREEN]
    (switch (.-F_ordinal c)
      (case [1])
      (default (.Error t "switch on the ordinal"))))
  (when (or (not (Enum_InstanceOf testColor_RED)) (not (Comparable_InstanceOf testColor_RED)) (not (Serializable_InstanceOf testColor_RED)))
    (.Error t "instanceof")))

(go/func TestVolatile [^{:tag (* testing/T)} t]
  (let [^{:tag (Volatile CharSequence)} v (zero (Volatile CharSequence))]
    (when (!= (.Load v) nil)
      (.Error t "zero Volatile"))
    (.Store v (Str "a"))
    (when (!= (.String (.ToString__String (.Load v))) "a")
      (.Error t "Store, Load"))
    (.Store v nil)
    (when (!= (.Load v) nil)
      (.Error t "Store nil"))))
