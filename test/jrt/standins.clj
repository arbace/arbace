(ns jrt.standins
  "jrt's stand-ins (doc/go/JRT-NOTES.md, \"Stand-ins\"): the Go forms of the JDK classes jrt's
  core needs before c2g translates them from jdk26u (the exceptions jrt throws, the interfaces
  its classes implement), written as C2G-SPEC says c2g writes them (§4.4 names, §5.3 class
  interfaces, §5.4 implementations and forwarders, §5.7 instanceof and casts), so that jrt and
  translated code meet at the same names. The file they make,
  go/arbace/jrt/standin_lang.clj, is generated and tracked; when c2g's output joins the
  build, it is deleted and the translated classes take the same names.

  bin/jrt standins   regenerates it (ARBACE_CLASSPATH=test bin/arbace -m jrt.standins FILE)."
  (:require [arbace.string :as str]))

;; Java's exceptions as jrt needs them: [simple-name super kind & ctors]. kind :leaf, :nonleaf
;; (has subclasses here), :abstract (non-leaf, no allocation). Constructors:
;;   :n ()  :s (String)  :st (String, Throwable)  :t (Throwable)  :o (Object, AssertionError)
;;   [:i "prefix"] (int): super(prefix + index)
;;   :eiie-n :eiie-s :eiie-t  ExceptionInInitializerError's three
(def exceptions
  [["Exception" "Throwable" :nonleaf :n :s :st :t]
   ["RuntimeException" "Exception" :nonleaf :n :s :st :t]
   ["Error" "Throwable" :nonleaf :n :s :st :t]
   ["NullPointerException" "RuntimeException" :leaf :n :s]
   ["ArithmeticException" "RuntimeException" :leaf :n :s]
   ["IndexOutOfBoundsException" "RuntimeException" :nonleaf :n :s [:i "Index out of range: "]]
   ["ArrayIndexOutOfBoundsException" "IndexOutOfBoundsException" :leaf :n :s [:i "Array index out of range: "]]
   ["StringIndexOutOfBoundsException" "IndexOutOfBoundsException" :leaf :n :s [:i "String index out of range: "]]
   ["ClassCastException" "RuntimeException" :leaf :n :s]
   ["ArrayStoreException" "RuntimeException" :leaf :n :s]
   ["NegativeArraySizeException" "RuntimeException" :leaf :n :s]
   ["IllegalArgumentException" "RuntimeException" :nonleaf :n :s :st :t]
   ["NumberFormatException" "IllegalArgumentException" :leaf :n :s]
   ["java.nio.charset.UnsupportedCharsetException" "IllegalArgumentException" :leaf :s]
   ["java.nio.charset.IllegalCharsetNameException" "IllegalArgumentException" :leaf :s]
   ["IllegalStateException" "RuntimeException" :nonleaf :n :s :st :t]
   ["IllegalMonitorStateException" "RuntimeException" :leaf :n :s]
   ["UnsupportedOperationException" "RuntimeException" :leaf :n :s :st :t]
   ["CloneNotSupportedException" "Exception" :leaf :n :s]
   ["InterruptedException" "Exception" :leaf :n :s]
   ;; phase 2a: threads and java.util.concurrent
   ["IllegalThreadStateException" "IllegalArgumentException" :leaf :n :s]
   ["java.util.concurrent.ExecutionException" "Exception" :leaf :n :s :st :t]
   ["java.util.concurrent.CancellationException" "IllegalStateException" :leaf :n :s]
   ["java.util.concurrent.TimeoutException" "Exception" :leaf :n :s]
   ["java.util.concurrent.RejectedExecutionException" "RuntimeException" :leaf :n :s :st :t]
   ["ReflectiveOperationException" "Exception" :nonleaf :n :s :st :t]
   ["ClassNotFoundException" "ReflectiveOperationException" :leaf :n :s :st]
   ["NoSuchMethodException" "ReflectiveOperationException" :leaf :n :s]
   ["NoSuchFieldException" "ReflectiveOperationException" :leaf :n :s]
   ["IllegalAccessException" "ReflectiveOperationException" :leaf :n :s]
   ["InstantiationException" "ReflectiveOperationException" :leaf :n :s]
   ["LinkageError" "Error" :nonleaf :n :s :st]
   ["ExceptionInInitializerError" "LinkageError" :leaf :eiie-n :eiie-s :eiie-t]
   ["NoClassDefFoundError" "LinkageError" :leaf :n :s]
   ["IncompatibleClassChangeError" "LinkageError" :nonleaf :n :s]
   ["AbstractMethodError" "IncompatibleClassChangeError" :leaf :n :s]
   ["VirtualMachineError" "Error" :abstract :n :s :st :t]
   ["OutOfMemoryError" "VirtualMachineError" :leaf :n :s]
   ["StackOverflowError" "VirtualMachineError" :leaf :n :s]
   ["InternalError" "VirtualMachineError" :leaf :n :s :st :t]
   ["AssertionError" "Error" :leaf :n :o]
   ;; c2g (C2G-SPEC §7.8): record patterns wrap an accessor's exception
   ["MatchException" "RuntimeException" :leaf :st]
   ["java.io.IOException" "Exception" :nonleaf :n :s :st :t]
   ["java.io.UnsupportedEncodingException" "IOException" :leaf :n :s]])

;; Throwable's virtual methods: [Go name, result tag, params, args]
(def throwable-methods
  [["ToString__String" "^{:tag (* String)} " "" ""]
   ["GetMessage__String" "^{:tag (* String)} " "" ""]
   ["GetLocalizedMessage__String" "^{:tag (* String)} " "" ""]
   ["GetCause__Throwable" "^Throwable_I " "" ""]
   ["InitCause_Throwable__Throwable" "^Throwable_I " " ^Throwable_I c" " c"]
   ["SetCause_Throwable__V" "" " ^Throwable_I c" " c"]
   ["FillInStackTrace__Throwable" "^Throwable_I " "" ""]
   ["GetStackTrace__StackTraceElement1" "^{:tag (* RefArray)} " "" ""]
   ["SetStackTrace_StackTraceElement1__V" "" " ^{:tag (* RefArray)} a" " a"]
   ["PrintStackTrace__V" "" "" ""]
   ["AddSuppressed_Throwable__V" "" " ^Throwable_I e" " e"]
   ["GetSuppressed__Throwable1" "^{:tag (* RefArray)} " "" ""]])

;; interfaces: [simple-name java-package [super-interfaces] methods defaults]
(def interfaces
  [["Serializable" "java.io" [] [] []]
   ["Cloneable" "java.lang" [] [] []]
   ["Comparable" "java.lang" [] ["(CompareTo_O__I ^int32 [^any o])"] []]
   ["CharSequence" "java.lang" []
    ["(Length__I ^int32 [])" "(CharAt_I__C ^uint16 [^int32 index])"
     "(SubSequence_I_I__CharSequence ^CharSequence [^int32 start ^int32 end])"
     "(IsEmpty__Z ^bool [])"]
    ["(go/func CharSequence_IsEmpty__Z \"CharSequence_IsEmpty__Z is CharSequence.isEmpty's default.\\n\"\n  ^bool [^CharSequence this]\n  (== (.Length__I this) 0))"]]
   ["Appendable" "java.lang" []
    ["(Append_CharSequence__Appendable ^Appendable [^CharSequence csq])"
     "(Append_CharSequence_I_I__Appendable ^Appendable [^CharSequence csq ^int32 start ^int32 end])"
     "(Append_C__Appendable ^Appendable [^uint16 c])"]
    []]
   ;; phase 2a: the functional interfaces jrt's threads and executors take
   ["Runnable" "java.lang" [] ["(Run__V [])"] []]
   ["Callable" "java.util.concurrent" [] ["(Call__O ^any [])"] []]
   ["Supplier" "java.util.function" [] ["(Get__O ^any [])"] []]])

(defn- simple [n] (last (str/split n #"\.")))
(defn- qualified [n] (if (str/includes? n ".") n (str "java.lang." n)))

(def ^:private by-name (into {} (map (juxt (comp simple first) identity) exceptions)))

(defn- leaf? [n] (= :leaf (nth (by-name n) 2)))
(defn- gotype [n] (if (or (= n "Throwable") (not (leaf? n))) (str n "_I") (str "(* " n ")")))
(defn- tag [n] (if (or (= n "Throwable") (not (leaf? n))) (str "^" n "_I") (str "^{:tag (* " n ")}")))

(defn- ctor-forms
  "The allocation function and constructor body of one constructor of class n."
  [n sup kind ctor]
  (let [leaf (= kind :leaf)
        this (if leaf "" (str " ^" n "_I this"))
        self (if leaf "t" "this")
        recv (str "[^{:tag (* " n ")} t" this)
        [ctor-name params args body]
        (cond
          (= ctor :n) ["Ctor" "" "" (format "(.Ctor (.-%s t) %s)" sup self)]
          (= ctor :s) ["Ctor_String" " ^{:tag (* String)} s" " s" (format "(.Ctor_String (.-%s t) %s s)" sup self)]
          (= ctor :st) ["Ctor_String_Throwable" " ^{:tag (* String)} s ^Throwable_I cause" " s cause"
                        (format "(.Ctor_String_Throwable (.-%s t) %s s cause)" sup self)]
          (= ctor :t) ["Ctor_Throwable" " ^Throwable_I cause" " cause" (format "(.Ctor_Throwable (.-%s t) %s cause)" sup self)]
          (= ctor :o) ["Ctor_O" " ^any detail" " detail"
                       (format "(.Ctor_String (.-%s t) %s (StrOfObj detail))\n  (when (Throwable_InstanceOf detail)\n    (.InitCause_Throwable__Throwable %s (Throwable_Cast detail)))" sup self self)]
          (= ctor :eiie-n) ["Ctor" "" "" (format "(.Ctor (.-%s t) %s)\n  (.InitCause_Throwable__Throwable %s nil)" sup self self)]
          (= ctor :eiie-s) ["Ctor_String" " ^{:tag (* String)} s" " s" (format "(.Ctor_String_Throwable (.-%s t) %s s nil)" sup self)]
          (= ctor :eiie-t) ["Ctor_Throwable" " ^Throwable_I thrown" " thrown" (format "(.Ctor_String_Throwable (.-%s t) %s nil thrown)" sup self)]
          (vector? ctor) ["Ctor_I" " ^int32 index" " index"
                          (format "(.Ctor_String (.-%s t) %s (Concat (Str %s) (StrOfInt index)))" sup self (pr-str (second ctor)))])
        new-name (str n "_New" (subs ctor-name 4))]
    (str
      (when-not (= kind :abstract)
        (format "(go/func %s ^{:tag (* %s)} [%s]\n  (let [t (addr (lit %s))] (.%s t%s%s) t))\n"
                new-name n (str/trim params) n ctor-name (if leaf "" " t") args))
      (format "(go/method %s %s%s]\n  %s)\n" ctor-name recv params body))))

(defn- class-forms [[qn sup kind & ctors]]
  (let [n (simple qn)
        g (gotype n)
        concrete (not= kind :abstract)]
    (str
      ";; ---- " (qualified qn) "\n\n"
      (format "(go/type %s (struct %s))\n" n sup)
      (when-not (= kind :leaf)
        (format "(go/type %s_I (interface %s_I (Self_%s ^{:tag (* %s)} [])))\n(go/method Self_%s ^{:tag (* %s)} [^{:tag (* %s)} t] t)\n"
                n sup n n n n n))
      (format "(go/var %s_class\n  (Define (addr (lit ClassInfo :Name \"%s\" :Kind KindClass :Modifiers %s\n                     :Super %s_class :Go \"arbace/jrt.%s\"))))\n"
              n (qualified qn) (if (= kind :abstract) "(bit-or AccPublic AccAbstract)" "AccPublic") sup n)
      (apply str (map #(ctor-forms n sup kind %) ctors))
      (when concrete
        (str
          (format "(go/method Ref ^any [^{:tag (* %s)} t] (when (== t nil) (return nil)) t)\n" n)
          (format "(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* %s)} t] %s_class)\n" n n)
          (format "(go/method Clone__O ^any [^{:tag (* %s)} t] (panic (CloneNotSupported t)))\n" n)
          (apply str
                 (for [[m res params args] throwable-methods]
                   (format "(go/method %s %s[^{:tag (* %s)} t%s] (.Impl_%s t t%s))\n" m res n params m args)))))
      (format "(go/func %s_InstanceOf ^bool [^any x] (let [(values _ ok) (assert %s x)] ok))\n" n g)
      (format "(go/func %s_Cast %s [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert %s x)]\n    (when (not ok) (panic (ClassCast x %s_class)))\n    v))\n"
              n (if (leaf? n) (str "^{:tag (* " n ")}") (str "^" n "_I")) g n)
      "\n")))

(defn- interface-forms [[n pkg supers methods defaults]]
  (str
    ";; ---- " pkg "." n "\n\n"
    (format "(go/type %s\n  (interface Object_I%s (Is_%s [])%s))\n" n
            (apply str (map #(str " " %) supers)) n
            (apply str (map #(str "\n    " %) methods)))
    (format "(go/var %s_class\n  (Define (addr (lit ClassInfo :Name \"%s.%s\" :Kind KindInterface\n                     :Modifiers (bit-or AccPublic AccInterface AccAbstract) :Go \"arbace/jrt.%s\"))))\n"
            n pkg n n)
    (apply str (map #(str % "\n") defaults))
    (format "(go/func %s_InstanceOf ^bool [^any x] (let [(values _ ok) (assert %s x)] ok))\n" n n)
    (format "(go/func %s_Cast ^%s [^any x]\n  (when (== x nil) (return nil))\n  (let [(values v ok) (assert %s x)]\n    (when (not ok) (panic (ClassCast x %s_class)))\n    v))\n\n"
            n n n n)))

(defn text []
  (str
    ";; Generated by test/jrt/standins.clj (bin/jrt standins); do not edit.\n"
    ";; jrt's stand-ins for JDK classes c2g will translate from jdk26u (doc/go/JRT-NOTES.md,\n"
    ";; \"Stand-ins\"), in the shapes C2G-SPEC gives translated classes. Deleted when c2g's\n"
    ";; output joins the build.\n"
    "(in-ns 'go.arbace.jrt)\n\n"
    "(go/file \"standin_lang.go\")\n\n"
    (apply str (map interface-forms interfaces))
    (apply str (map class-forms exceptions))
    ";; ClassInfo.IsInstance, set at init (§5.11)\n"
    "(go/func init []"
    (apply str (for [n (concat (map first interfaces) (map (comp simple first) exceptions))]
                 (format "\n  (set! (.-IsInstance (.Info %s_class)) %s_InstanceOf)" n n)))
    ")\n"))

(defn special-uppercase
  "The unconditional entries of SpecialCasing.txt whose uppercase is more than one code
  point: [[code-point [upper ...]] ...], sorted."
  [file]
  (sort-by first
           (for [line (str/split-lines (slurp file))
                 :let [line (str/trim (first (str/split line #"#" 2)))]
                 :when (not (str/blank? line))
                 :let [fields (mapv str/trim (str/split line #";"))
                       [cp _ _ upper & more] fields]
                 :when (empty? (remove str/blank? more))
                 :let [ups (mapv #(Long/parseLong % 16) (str/split upper #" +"))]
                 :when (> (count ups) 1)]
             [(Long/parseLong cp 16) ups])))

(defn character-text [jdk]
  (let [sc (str jdk "/src/java.base/share/data/unicodedata/SpecialCasing.txt")
        entries (special-uppercase sc)]
    (str
      ";; Generated by test/jrt/standins.clj (bin/jrt standins); do not edit.\n"
      ";; jrt's stand-in for java.lang.Character (doc/go/JRT-NOTES.md, \"Stand-ins\"): the case\n"
      ";; mappings and properties String needs, over Go's unicode (Unicode 17.0.0, as JDK 26's\n"
      ";; Character), with the special uppercase mappings of jdk26u's\n"
      ";; src/java.base/share/data/unicodedata/SpecialCasing.txt (" (count entries) " entries).\n"
      ";; Replaced by calls of the translated Character when c2g's output joins the build.\n"
      "(in-ns 'go.arbace.jrt)\n\n"
      "(go/file \"standin_character.go\"\n  :imports [[unicode \"unicode\"]])\n\n"
      "(go/func charToUpper \"charToUpper is Character.toUpperCase(int).\\n\" ^rune [^rune c]\n"
      "  (when (or (< c 0) (> c 0x10FFFF)) (return c))\n  (unicode/ToUpper c))\n\n"
      "(go/func charToLower \"charToLower is Character.toLowerCase(int).\\n\" ^rune [^rune c]\n"
      "  (when (or (< c 0) (> c 0x10FFFF)) (return c))\n  (unicode/ToLower c))\n\n"
      "(go/func charToUpperEx\n  \"charToUpperEx is Character.toUpperCaseEx: -1 (Character.ERROR) when the uppercase is\nseveral characters (charToUpperArray).\\n\"\n"
      "  ^rune [^rune c]\n"
      "  (let [(values _ ok) (aget specialUpper c)]\n    (when ok (return -1)))\n  (charToUpper c))\n\n"
      "(go/func charToUpperArray \"charToUpperArray is Character.toUpperCaseCharArray.\\n\"\n"
      "  ^{:tag (slice uint16)} [^rune c]\n"
      "  (let [(values s ok) (aget specialUpper c)]\n    (when ok (return s)))\n  (appendCodePoint nil (charToUpper c)))\n\n"
      "(go/func charIsWhitespace\n  \"charIsWhitespace is Character.isWhitespace(int): space separators but the no-break spaces,\nline and paragraph separators, and U+0009-U+000D, U+001C-U+001F.\\n\"\n"
      "  ^bool [^rune c]\n"
      "  (when (or (and (>= c 0x09) (<= c 0x0D)) (and (>= c 0x1C) (<= c 0x1F)))\n    (return true))\n"
      "  (when (or (== c 0xA0) (== c 0x2007) (== c 0x202F))\n    (return false))\n"
      "  (or (unicode/Is unicode/Zs c) (== c 0x2028) (== c 0x2029)))\n\n"
      "(go/func isCased \"isCased is ConditionalSpecialCasing.isCased.\\n\" ^bool [^rune c]\n"
      "  (or (unicode/Is unicode/Lu c) (unicode/Is unicode/Ll c) (unicode/Is unicode/Lt c)\n"
      "      (and (>= c 0x02B0) (<= c 0x02B8)) (and (>= c 0x02C0) (<= c 0x02C1))\n"
      "      (and (>= c 0x02E0) (<= c 0x02E4)) (== c 0x0345) (== c 0x037A)\n"
      "      (and (>= c 0x1D2C) (<= c 0x1D61)) (and (>= c 0x2160) (<= c 0x217F))\n"
      "      (and (>= c 0x24B6) (<= c 0x24E9))))\n\n"
      "(go/func isWordChar\n  \"isWordChar: whether c continues a word for the final sigma rule (letters, marks, digits,\nconnectors, apostrophes), jrt's approximation of java.text.BreakIterator's word boundaries.\\n\"\n"
      "  ^bool [^rune c]\n"
      "  (or (unicode/IsLetter c) (unicode/IsMark c) (unicode/IsDigit c) (unicode/Is unicode/Pc c)\n"
      "      (== c \\') (== c 0x2019) (== c 0x00B7)))\n\n"
      "(go/var ^{:tag (map rune (slice uint16))} specialUpper\n  (lit (map rune (slice uint16))"
      (apply str
             (for [[cp ups] entries]
               (format "\n    [0x%04X (lit (slice uint16) %s)]" cp
                       (str/join " " (map #(format "0x%04X" %)
                                          (mapcat (fn [u] (if (> u 0xFFFF)
                                                            [(+ 0xD800 (bit-shift-right (- u 0x10000) 10))
                                                             (+ 0xDC00 (bit-and (- u 0x10000) 0x3FF))]
                                                            [u]))
                                                  ups))))))
      "))\n")))

(defn -main [file & [char-file]]
  (spit file (text))
  (when char-file
    (spit char-file (character-text (or (System/getenv "JDK") "/root/jdk26u")))))
