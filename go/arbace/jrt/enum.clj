;; jrt: java.lang.Enum and java.lang.Record, the superclasses of c2g's enums and records
;; (C2G-SPEC §7.13); both are abstract, non-leaf classes (§5.3, §5.4).
(in-ns 'go.arbace.jrt)

(go/file "enum.go")

;; ---------------------------------------------------------------------------------------
;; Enum

(go/type Enum_I
  "Enum_I is java.lang.Enum's class interface.\n"
  (interface Object_I
    (Self_Enum ^{:tag (* Enum)} [])
    (Is_Comparable [])
    (Is_Serializable [])
    (Name__String ^{:tag (* String)} [])
    (Ordinal__I ^int32 [])
    (CompareTo_Enum__I ^int32 [^Enum_I o])
    (CompareTo_O__I ^int32 [^any o])
    (GetDeclaringClass__Class ^{:tag (* Class)} [])))

(go/type Enum
  "Enum is java.lang.Enum's struct: the constant's name and ordinal, which c2g's enum switches
read (F_ordinal, §7.8).\n"
  (struct Object ^{:tag (* String)} F_name ^int32 F_ordinal))

(go/var Enum_class
  (Define (addr (lit ClassInfo :Name "java.lang.Enum" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Comparable_class Serializable_class)
                     :Go "arbace/jrt.Enum"))))

(go/method Ctor_String_I "Ctor_String_I is Enum(String name, int ordinal).\n"
  [^{:tag (* Enum)} t ^Enum_I this ^{:tag (* String)} name ^int32 ordinal]
  (set! (.-F_name t) name)
  (set! (.-F_ordinal t) ordinal))

(go/method Self_Enum ^{:tag (* Enum)} [^{:tag (* Enum)} t] t)
(go/method Is_Comparable [^{:tag (* Enum)} t])
(go/method Is_Serializable [^{:tag (* Enum)} t])

;; the final methods that need no dynamic this: promotable, no forwarders (§9.1)
(go/method Name__String ^{:tag (* String)} [^{:tag (* Enum)} t] (.-F_name t))
(go/method Ordinal__I ^int32 [^{:tag (* Enum)} t] (.-F_ordinal t))

;; the implementations
(go/method Impl_Clone__O "Impl_Clone__O is Enum.clone (final): CloneNotSupportedException.\n"
  ^any [^{:tag (* Enum)} t ^Enum_I this]
  (panic (CloneNotSupportedException_New)))

(go/method Impl_ToString__String ^{:tag (* String)} [^{:tag (* Enum)} t ^Enum_I this]
  (.-F_name t))

(go/method Impl_GetDeclaringClass__Class "Impl_GetDeclaringClass__Class: the enum's class, also for a
constant with a body (an anonymous subclass).\n"
  ^{:tag (* Class)} [^{:tag (* Enum)} t ^Enum_I this]
  (let [c (.GetClass__Class this)
        s (.-Super (.-info c))]
    (when (== s Enum_class)
      (return c))
    s))

(go/method Impl_CompareTo_Enum__I ^int32 [^{:tag (* Enum)} t ^Enum_I this ^Enum_I o]
  (when (and (!= (.GetClass__Class this) (.GetClass__Class o))
             (!= (.GetDeclaringClass__Class this) (.GetDeclaringClass__Class o)))
    (panic (ClassCastException_New)))
  (- (.-F_ordinal t) (.-F_ordinal (.Self_Enum o))))

(go/method Impl_CompareTo_O__I "Impl_CompareTo_O__I is the bridge of Comparable.compareTo.\n"
  ^int32 [^{:tag (* Enum)} t ^Enum_I this ^any o]
  (.CompareTo_Enum__I this (Enum_Cast o)))

(go/func Enum_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Enum_I x)] ok))
(go/func Enum_Cast ^Enum_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Enum_I x)]
    (when (not ok) (panic (ClassCast x Enum_class)))
    v))

(go/func Enum_ValueOf_Class_String__Enum
  "Enum_ValueOf_Class_String__Enum is Enum.valueOf(Class, String): the constant of that name,
through the class's ClassInfo.Enum.\n"
  ^Enum_I [^{:tag (* Class)} c ^{:tag (* String)} name]
  (when (== name nil)
    (panic (NullPointerException_New_String (Str "Name is null"))))
  (when (!= (.-Enum (.-info (NN c))) nil)
    (range [_ x (.-A ((.-Enum (.-info c))))]
      (let [e (Enum_Cast x)]
        (when (.Equals_O__Z (.-F_name (.Self_Enum e)) name)
          (return e)))))
  (when (== (.-Enum (.-info c)) nil)
    (panic (IllegalArgumentException_New_String (Str (+ (.-Name (.-info c)) " is not an enum class")))))
  (panic (IllegalArgumentException_New_String
           (Concat (Str (+ "No enum constant " (canonicalName c) ".")) name))))

(go/func canonicalName "canonicalName: the binary name with $ as . (member classes).\n"
  ^string [^{:tag (* Class)} c]
  (let [b (conv (slice byte) (.-Name (.-info c)))]
    (range [i x b]
      (when (== x \$)
        (aset b i \.)))
    (conv string b)))

;; ---------------------------------------------------------------------------------------
;; Record

(go/type Record_I "Record_I is java.lang.Record's class interface.\n"
  (interface Object_I (Self_Record ^{:tag (* Record)} [])))

(go/type Record "Record is java.lang.Record's struct (no state).\n" (struct Object))

(go/var Record_class
  (Define (addr (lit ClassInfo :Name "java.lang.Record" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Go "arbace/jrt.Record"))))

(go/method Ctor "Ctor is Record(): nothing.\n" [^{:tag (* Record)} t ^Record_I this])
(go/method Self_Record ^{:tag (* Record)} [^{:tag (* Record)} t] t)

(go/func Record_InstanceOf ^bool [^any x] (let [(values _ ok) (assert Record_I x)] ok))
(go/func Record_Cast ^Record_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Record_I x)]
    (when (not ok) (panic (ClassCast x Record_class)))
    v))
