;; jrt: member tables as data (doc/go/JRT-NOTES.md, "Size", amendment SZ3). c2g gives a
;; translated class its member table as a MemberTable: the members' names, descriptors and
;; modifiers as one string, and their functions (invokers, constructors, field accessors) as
;; slices that Go initializes statically. The table is decoded into the ClassInfo's Methods,
;; Ctors and Fields the first time reflection reads them (EnsureMembers), not at the program's
;; start: the per-class code that built the tables at initialization was about 3 MB.
(in-ns 'go.arbace.jrt)

(go/file "members.go"
  :imports [[strings "strings"] [strconv "strconv"] [sync "sync"]])

(go/type MemberTable
  "MemberTable is a class's members as c2g writes them. Data holds one line per member, in the
order of the function slices: \"M mods name desc\" (a method: Invoke), \"C mods desc\" (a
constructor: New), \"F mods name desc\" (a field: Get, and Set when not final); the
descriptors name classes by their binary names (\"(ILjava.lang.String;)V\"). A nil function is
a member that does not exist in Go (its descriptor names a class outside the closed world), or
any member of a cut class (Cut): it throws UnsupportedOperationException (EVAL-NOTES.md, \"Cut
classes\").\n"
  (struct ^string Data
          ^{:tag bool :doc "a cut class's table (its members throw, named without descriptors)\n"} Cut
          ^{:tag (slice (func [any (slice any)] [any]))} Invoke
          ^{:tag (slice (func [(slice any)] [any]))} New
          ^{:tag (slice (func [any] [any]))} Get
          ^{:tag (slice (func [any any]))} Set
          ^{:tag sync/Once} once))

(go/func EnsureMembers
  "EnsureMembers decodes the class's member table into its Methods, Ctors and Fields, once.\n"
  [^{:tag (* ClassInfo)} info]
  (let [t (.-Table info)]
    (when (== t nil)
      (return))
    (.Do (addr (.-once t)) (fn [] (decodeMembers info t)))))

(go/func descClass "descClass is the class of a descriptor (V: void).\n"
  ^{:tag (* Class)} [^string d]
  (when (== d "V")
    (return Prim_void))
  (let [(values c rest) (parseArrayName d)]
    (when (or (== c nil) (!= rest ""))
      (panic (+ "jrt: no class for the member table's descriptor " d)))
    c))

(go/func descParams "descParams are the parameter classes and the return class of a method descriptor.\n"
  [^string d] :results [^{:tag (slice (* Class))} ps ^{:tag (* Class)} r]
  (let [i (strings/IndexByte d \))
        s (subslice d 1 i)]
    (while (!= s "")
      (let [n (descLen s)]
        (set! ps (append ps (descClass (subslice s 0 n))))
        (set! s (subslice s n))))
    (return ps (descClass (subslice d (+ i 1))))))

(go/func descLen "descLen is the length of the first descriptor of s.\n" ^int [^string s]
  (let [i 0]
    (while (== (aget s i) \[)
      (inc! i))
    (when (== (aget s i) \L)
      (return (+ (strings/IndexByte s \;) 1)))
    (+ i 1)))

(go/func absentMember
  "absentMember is the UnsupportedOperationException of a member not in the Go build: named
C.name<descriptor> (C.<init><descriptor>), or for a cut class C.name and C's constructor.\n"
  ^Throwable_I [^{:tag (* MemberTable)} t ^{:tag (* ClassInfo)} info ^string name ^string desc]
  (let [what (+ (.-Name info) "." name (strings/ReplaceAll desc "." "/"))]
    (when (.-Cut t)
      (if (== name "<init>")
        (set! what (+ (.-Name info) "'s constructor"))
        (set! what (+ (.-Name info) "." name))))
    (Thrown (UnsupportedOperationException_New_String (Str (+ what " is not in the Go build"))))))

(go/func decodeMembers [^{:tag (* ClassInfo)} info ^{:tag (* MemberTable)} t]
  (let [mi 0 ci 0 fi 0]
    (range [_ line (strings/Split (.-Data t) "\n")]
      (when (== line "")
        (continue))
      (let [parts (strings/Split line " ")
            (values mods _) (strconv/Atoi (subslice (aget parts 0) 1))]
        (switch (aget line 0)
          (case [\M]
            (let [name (aget parts 1)
                  desc (aget parts 2)
                  (values ps r) (descParams desc)
                  f (aget (.-Invoke t) mi)]
              (when (== f nil)
                (set! f (fn ^any [^any this ^{:tag (slice any)} args] (panic (absentMember t info name desc)))))
              (set! (.-Methods info) (append (.-Methods info)
                                             (lit MethodInfo :Name name :Params ps :Return r :Modifiers (conv int32 mods) :Invoke f)))
              (inc! mi)))
          (case [\C]
            (let [desc (aget parts 1)
                  (values ps _) (descParams desc)
                  f (aget (.-New t) ci)]
              (when (== f nil)
                (set! f (fn ^any [^{:tag (slice any)} args] (panic (absentMember t info "<init>" desc)))))
              (set! (.-Ctors info) (append (.-Ctors info) (lit CtorInfo :Params ps :Modifiers (conv int32 mods) :New f)))
              (inc! ci)))
          (case [\F]
            (let [name (aget parts 1)
                  g (aget (.-Get t) fi)]
              (when (== g nil)
                (set! g (fn ^any [^any o] (panic (absentMember t info name "")))))
              (set! (.-Fields info) (append (.-Fields info)
                                            (lit FieldInfo :Name name :Type (descClass (aget parts 2))
                                                 :Modifiers (conv int32 mods)
                                                 :Get g :Set (aget (.-Set t) fi))))
              (inc! fi))))))))
