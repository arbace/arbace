(ns arbace.c2g.out
  "c2g's output (C2G-SPEC §4.3): Go forms files, one per class forms file, plus the generated
  files of each package (c2g_classes: registrations and member tables, §5.11; c2g_strings: the
  literal pool, §7.5; c2g_frames: the frame table, §7.9.6; c2g_support: c2g's helpers), and
  the merge with jrt's hand-written forms into one program root for bin/g2c build."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.jrt :as jrt]
            [arbace.c2g.code :as c]
            [arbace.c2g.decls :as d])
  (:import (arbace.asm Opcodes)))

(defn- tag [sym type] (with-meta sym {:tag type}))

;; ---------------------------------------------------------------------------------------
;; printing forms

(defn clean
  "f without the reader's position metadata (quoted forms in c2g's source carry it)."
  [f]
  (let [cm (fn [x] (let [mt (meta x)]
                     (if mt
                       (let [mt (dissoc mt :line :column :file :end-line :end-column)
                             mt (if (contains? mt :tag) (update mt :tag clean) mt)]
                         (with-meta x (if (empty? mt) nil mt)))
                       x)))]
    (cond
      (symbol? f) (cm f)
      (seq? f) (cm (with-meta (apply list (map clean f)) (meta f)))
      (vector? f) (cm (with-meta (mapv clean f) (meta f)))
      (map? f) (into {} (map (fn [[k v]] [(clean k) (clean v)]) f))
      :else f)))

(declare write-form)

;; The layout of the forms text (C2G-SPEC §4.3, §7.9.6): a form carrying :c2g/line (the class
;; forms line of the statement or member it comes from) is written on that line of the file
;; when the text has not passed it yet, so that the reader's position is that line; else it
;; gets an explicit ^{:line n}, as does every list inside it without a line of its own. The
;; printer's lines layout (bin/g2c build --line-file) then gives gc's line tables the class
;; forms' lines.
(def ^:dynamic *lay*
  "nil (no layout), or {:line (volatile! current line) :forced line-or-nil}."
  nil)

(defn- out! [^StringBuilder sb ^String s]
  (.append sb s)
  (when *lay*
    (let [n (count (filter #(= % \newline) s))]
      (when (pos? n) (vswap! (:line *lay*) + n)))))

(defn- write-meta [^StringBuilder sb mt]
  (let [mt (dissoc mt :c2g/line)]
    (when (seq mt)
     (binding [*lay* (when *lay* (assoc *lay* :meta true))]
      (if (and (= [:tag] (keys mt)) (symbol? (:tag mt)))
        (do (out! sb "^") (write-form sb (:tag mt)) (out! sb " "))
        (do (out! sb "^") (write-form sb mt) (out! sb " ")))))))

(defn- write-items [^StringBuilder sb xs]
  (doseq [[i x] (map-indexed vector xs)]
    (when (pos? i) (out! sb " "))
    (write-form sb x)))

(defn- write-list [^StringBuilder sb f mt]
  (let [own (:c2g/line mt)
        forced (:forced *lay*)
        cur (when *lay* @(:line *lay*))
        explicit (cond
                   (nil? *lay*) nil
                   forced (or own forced)
                   ;; inside metadata (tags): no padding, the line of the form around
                   (:meta *lay*) nil
                   (nil? own) nil
                   (>= own cur) (do (out! sb (apply str (repeat (- own cur) "\n"))) nil)
                   :else own)
        body (fn []
               (write-meta sb (if explicit (assoc mt :line explicit) mt))
               (out! sb "(") (write-items sb f) (out! sb ")"))]
    (if (and explicit (not= explicit forced))
      (binding [*lay* (assoc *lay* :forced explicit)] (body))
      (body))))

(defn- write-form [^StringBuilder sb f]
  (let [mt (when (or (symbol? f) (seq? f) (vector? f)) (meta f))]
    (cond
      (seq? f) (write-list sb f mt)
      (vector? f) (do (write-meta sb mt)
                      (out! sb "[") (write-items sb f) (out! sb "]"))
      (map? f) (do (out! sb "{")
                   (doseq [[i [k v]] (map-indexed vector f)]
                     (when (pos? i) (out! sb " "))
                     (write-form sb k) (out! sb " ") (write-form sb v))
                   (out! sb "}"))
      (symbol? f) (do (write-meta sb mt) (out! sb (str f)))
      :else (out! sb (binding [*print-meta* false] (pr-str f))))))

(defn form-text [f]
  (let [sb (StringBuilder.)]
    (binding [*lay* nil] (write-form sb (clean f)))
    (str sb)))

(def import-paths
  {"jrt" "arbace/jrt" "math" "math" "atomic" "sync/atomic" "reflect" "reflect" "unsafe" "unsafe"
   "strings" "strings" "lang" "arbace/lang" "sync" "sync"})

(defn- used-qualifiers [forms]
  (let [acc (volatile! #{})]
    (letfn [(w [x]
              (cond
                (symbol? x) (do (when-let [q (namespace x)] (vswap! acc conj q))
                                (when-let [mt (meta x)] (w (:tag mt))))
                (seq? x) (do (when-let [mt (meta x)] (w (:tag mt))) (run! w x))
                (vector? x) (do (when-let [mt (meta x)] (w (:tag mt))) (run! w x))
                (map? x) (run! w (vals x))
                :else nil))]
      (run! w forms))
    @acc))

(defn file-text
  "The text of a Go forms file of package pkg named go-name holding forms."
  [pkg go-name forms & [header]]
  (let [quals (used-qualifiers forms)
        imports (vec (for [q (sort quals)
                           :when (contains? import-paths q)
                           :when (not (and (= pkg :jrt) (= q "jrt")))
                           :when (not (and (= q "arbace.core")))]
                       [(symbol q) (import-paths q)]))
        sb (StringBuilder.)]
    (binding [*lay* {:line (volatile! 1) :forced nil}]
      (out! sb (str ";; Generated by c2g (arbace/c2g, doc/go/C2G-SPEC.md); do not edit.\n"))
      (when header (out! sb header))
      (out! sb (str "(in-ns '" (nm/pkg-ns pkg) ")\n\n"))
      (write-form sb (clean (list 'go/file go-name :imports imports)))
      (out! sb "\n")
      ;; Go's package-level declarations may come in any order: those with a class forms line
      ;; first, by line (so that each lands on its line), then the rest in their order
      (doseq [f (let [line-of #(when (seq? %) (:c2g/line (meta %)))
                      lined (filter line-of forms)]
                  (concat (sort-by line-of lined) (remove line-of forms)))]
        (if (and (seq? f) (= 'c2g/comment (first f)))
          (out! sb (str "\n;; " (second f) "\n"))
          (let [own (when (seq? f) (:c2g/line (meta f)))]
            ;; a blank line between declarations, unless the next one's line comes later
            (when-not (and own (>= own @(:line *lay*))) (out! sb "\n"))
            (write-form sb (clean f)) (out! sb "\n")))))
    (str sb)))

(defn go-file-name
  "The Go file of a class forms file (§4.3): its name for arbace/lang, the Java source path
  with _ for / in jrt; _c2g appended when the name would read as a build constraint."
  [pkg top]
  (let [base (if (= pkg :jrt)
               (str/replace top "/" "_")
               (subs top (inc (.lastIndexOf ^String top "/"))))
        base (str/replace base "$" "_")
        parts (str/split base #"_")
        constraint #{"aix" "android" "darwin" "dragonfly" "freebsd" "hurd" "illumos" "ios" "js" "linux"
                     "nacl" "netbsd" "openbsd" "plan9" "solaris" "wasip1" "windows" "zos" "tamago"
                     "386" "amd64" "arm" "arm64" "loong64" "mips" "mipsle" "mips64" "mips64le" "ppc64"
                     "ppc64le" "riscv64" "s390x" "wasm" "test"}]
    (str base (if (and (> (count parts) 1) (constraint (last parts))) "_c2g" "") ".go")))

;; ---------------------------------------------------------------------------------------
;; generated package files

(defn strings-forms
  "The literal pool (§7.5): Lit_n vars interned at package initialization."
  [pkg lits]
  (for [[s sym] (sort-by (comp #(Long/parseLong (subs (name %) 4)) val) lits)]
    (let [units (map int s)
          lone? (some (fn [[i ch]]
                        (let [hi (Character/isHighSurrogate (char ch))
                              lo (Character/isLowSurrogate (char ch))]
                          (or (and hi (not (and (< (inc i) (count s)) (Character/isLowSurrogate (.charAt ^String s (inc i))))))
                              (and lo (not (and (pos? i) (Character/isHighSurrogate (.charAt ^String s (dec i)))))))))
                      (map-indexed vector units))]
      (list 'go/var (tag sym (list '* (m/jrt-sym pkg "String")))
            (if lone?
              (list (m/jrt-sym pkg "InternUTF16") (apply list 'lit '(slice uint16) units))
              (list (m/jrt-sym pkg "Intern") s))))))

(defn ups-forms
  "The nil-preserving conversions Up_From_To (§5.6) the package's code needs."
  [pkg ups]
  (binding [c/*f* {:pkg pkg}]
    (for [[from to] (sort ups)]
      (list 'go/func (symbol (str "Up_" (nm/desc-code from) "_" (nm/desc-code to)))
            (with-meta [(tag 'p (m/go-type pkg from))] {:tag (m/go-type pkg to)})
            (list 'when (list '== 'p nil) (list 'return nil))
            'p))))

(defn adapter-forms
  "The adapter F_Fn of functional interface fi (§7.11): its SAM calls Fn, default methods
  forward to their functions."
  [pkg fi]
  (let [g (m/go-name fi)
        a (str g "_Fn")
        sam (a/find-sam fi)
        [ps r] (t/parse-method-desc (:desc sam))
        pn (vec (for [i (range (count ps))] (symbol (str "p" i))))
        recv (tag 't (list '* (symbol a)))
        ftype (if (= "V" r)
                (list 'func (vec (map #(m/go-type pkg %) ps)))
                (list 'func (vec (map #(m/go-type pkg %) ps)) [(m/go-type pkg r)]))
        call (apply list (list '.-Fn 't) pn)]
    (concat
      [(list 'c2g/comment (str "---- the lambda adapter of " (str/replace fi "/" ".")))
       (list 'go/type (symbol a) (list 'struct (m/jrt-sym pkg "Object") (tag 'Fn ftype)))
       (list 'go/method (symbol (nm/method-base (:name sam) (:desc sam)))
             (cond-> (vec (cons recv (map #(tag %1 (m/go-type pkg %2)) pn ps)))
               (not= "V" r) (vary-meta assoc :tag (m/go-type pkg r)))
             (if (= "V" r) call (list 'return call)))]
      (for [j (cons fi (d/interface-closure fi))]
        (list 'go/method (symbol (str "Is_" (m/go-name j))) [recv]))
      ;; default methods of the interface and its superinterfaces
      (for [[[name desc :as k] mm] (m/vmethods fi)
            :when (not= [name desc] [(:name sam) (:desc sam)])
            :let [impl (m/impl-of fi k)
                  [mps mr] (t/parse-method-desc desc)
                  mpn (vec (for [i (range (count mps))] (symbol (str "p" i))))
                  base (nm/method-base name desc)]]
        (list 'go/method (symbol base)
              (cond-> (vec (cons recv (map #(tag %1 (m/go-type pkg %2)) mpn mps)))
                (not= "V" mr) (vary-meta assoc :tag (m/go-type pkg mr)))
              (if (and impl (= :default (:kind impl)))
                (apply list (m/class-sym pkg (:owner impl) (str "_" base)) 't mpn)
                (list 'panic (list (m/jrt-sym pkg "C2g_NotTranslated") (str "abstract " name desc))))))
      [(list 'go/method 'Ref (with-meta [recv] {:tag 'any}) (list 'when (list '== 't nil) (list 'return nil)) 't)
       (list 'go/method 'GetClass__Class (with-meta [recv] {:tag (list '* (m/jrt-sym pkg "Class"))}) (symbol (str a "_class")))
       (list 'go/method 'ToString__String (with-meta [recv] {:tag (list '* (m/jrt-sym pkg "String"))})
             (list (m/jrt-sym pkg "Object_toString") 't))
       (list 'go/method 'Clone__O (with-meta [recv] {:tag 'any}) (list 'panic (list (m/jrt-sym pkg "CloneNotSupported") 't)))
       (list 'go/var (symbol (str a "_class"))
             (list (m/jrt-sym pkg "Define")
                   (list 'addr (list 'lit (m/jrt-sym pkg "ClassInfo")
                                     :Name (str (str/replace fi "/" ".") "$$Lambda")
                                     :Modifiers 0x1010 :Kind (m/jrt-sym pkg "KindClass")
                                     :Super (m/jrt-sym pkg "Object_class")
                                     :Interfaces (list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) (m/class-sym pkg fi "_class"))
                                     :Go (str (nm/pkg-path pkg) "." a)))))])))

(def string-regex-methods
  "String's methods over java.util.regex, which c2g writes into jrt's c2g_support when Pattern is
  translated (jrt's String is hand-written; JRT-NOTES.md, \"waiting for the regex port\"): Go
  method name, then its definition as jdk26u's String defines it (split's one-character fast
  path gives what Pattern.split gives)."
  [["Split_String__String1"
    '(go/method Split_String__String1 "Split_String__String1 is String.split(regex).\n"
       ^{:tag (* RefArray)} [^{:tag (* String)} t ^{:tag (* String)} regex]
       (.Split_String_I__String1 t regex 0))]
   ["Split_String_I__String1"
    '(go/method Split_String_I__String1 "Split_String_I__String1 is String.split(regex, limit).\n"
       ^{:tag (* RefArray)} [^{:tag (* String)} t ^{:tag (* String)} regex ^int32 limit]
       (.Split_CharSequence_I__String1 (Pattern_Compile_String__Pattern regex) t limit))]
   ["ReplaceAll_String_String__String"
    '(go/method ReplaceAll_String_String__String "ReplaceAll_String_String__String is String.replaceAll.\n"
       ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* String)} regex ^{:tag (* String)} replacement]
       (.ReplaceAll_String__String (.Matcher_CharSequence__Matcher (Pattern_Compile_String__Pattern regex) t) replacement))]
   ["ReplaceFirst_String_String__String"
    '(go/method ReplaceFirst_String_String__String "ReplaceFirst_String_String__String is String.replaceFirst.\n"
       ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* String)} regex ^{:tag (* String)} replacement]
       (.ReplaceFirst_String__String (.Matcher_CharSequence__Matcher (Pattern_Compile_String__Pattern regex) t) replacement))]
   ["Matches_String__Z"
    '(go/method Matches_String__Z "Matches_String__Z is String.matches.\n"
       ^bool [^{:tag (* String)} t ^{:tag (* String)} regex]
       (Pattern_Matches_String_CharSequence__Z regex t))]])

(def java-names
  "Classes of jrt's own Java (overlay/jdk) that stand for a JDK class jrt cannot translate (a
  nested class of a hand-written one): the name Class.getName answers for them."
  {"jdk/internal/jrt/CaseInsensitiveComparator" "java.lang.String$CaseInsensitiveComparator"})

(defn java-name [n] (or (java-names n) (str/replace n "/" ".")))

(defn jrt-default-forwarders
  "Forwarders on jrt's hand-written leaf classes to the default methods of translated
  interfaces they implement and do not define themselves (CharSequence's chars and
  codePoints on String, StringBuilder and StringBuffer once streams are in the world): Go's
  interface satisfaction needs the methods, which jrt's own build cannot name (§5.4)."
  []
  (for [n (sort (keys (:jrt-classes m/*w*)))
        :when (and (m/hand-written? n) (not (m/interface? n)) (not (m/abstract? n)) (m/leaf? n)
                   (= :jrt (m/pkg n)))
        :let [g (m/go-name n)
              have (jrt/struct-methods (:jrt m/*w*) g)]
        ;; a class jrt declares without a Go type of its own (statics only) has no methods
        :when (contains? (:types (:jrt m/*w*)) g)
        [[name desc :as k] _] (m/vmethods n)
        :let [impl (m/impl-of n k)
              base (nm/method-base name desc)]
        :when (and impl (= :default (:kind impl)) (not (contains? have base)))
        :let [[mps mr] (t/parse-method-desc desc)
              mpn (vec (for [i (range (count mps))] (symbol (str "p" i))))
              call (apply list (m/class-sym :jrt (:owner impl) (str "_" base)) 't mpn)]]
    (list 'go/method (symbol base)
          (cond-> (vec (cons (tag 't (list '* (symbol g))) (map #(tag %1 (m/go-type :jrt %2)) mpn mps)))
            (not= "V" mr) (vary-meta assoc :tag (m/go-type :jrt mr)))
          (if (= "V" mr) call (list 'return call)))))

(defn support-forms
  "c2g's helpers in package jrt (c2g_support): what translated code calls besides jrt's API."
  []
  (let [bool-translated (m/translated? "java/lang/Boolean")
        bool-init (and bool-translated (not (m/trivial-init? "java/lang/Boolean")))]
    (concat
     ;; String's regex methods
     (when (m/translated? "java/util/regex/Pattern") (map second string-regex-methods))
     (jrt-default-forwarders)
     ;; String's members over translated classes (jrt's String cannot name them): Constable's
     ;; describeConstable once Optional is in the world, and lines() once streams are
     (when (and (m/translated? "java/util/Optional") (m/translated? "java/lang/constant/Constable"))
       ['(go/method DescribeConstable__Optional "DescribeConstable__Optional is String.describeConstable: Optional.of(this).\n"
          ^{:tag (* Optional)} [^{:tag (* String)} t]
          (Optional_Of_O__Optional t))])
     (when (and (m/translated? "java/util/stream/Stream") (m/translated? "java/util/ArrayList"))
       ['(go/method Lines__Stream "Lines__Stream is String.lines: the lines (split at \\n, \\r, \\r\\n) as a
sequential stream (over an ArrayList: not jdk26u's lazy spliterator).\n"
          ^Stream [^{:tag (* String)} t]
          (let [l (ArrayList_New)]
            (range [_ x (splitLines (.-value t))]
              (.Add_O__Z l (NewStringUTF16 x)))
            (.Stream__Stream l)))])
     (remove nil? [(list 'go/var (tag 'C2g_AssertionsDisabled 'bool) true)
     (list 'go/func 'C2g_NotTranslated (with-meta [(tag 'what 'string)] {:tag 'Throwable_I})
           (list 'UnsupportedOperationException_New_String (list 'Str (list '+ "c2g: not translated: " 'what))))
     (list 'go/func 'C2g_Missing :type-params ['T] (with-meta [(tag 'why 'string)] {:tag 'T})
           (list 'panic (list 'Thrown (list 'UnsupportedOperationException_New_String (list 'Str (list '+ "c2g: " 'why))))))
     (list 'go/func 'C2g_NotNull :type-params ['T] (with-meta [(tag 'x 'T)] {:tag 'T})
           (list 'when (list '== (list 'conv 'any 'x) nil) (list 'panic (list 'NPE)))
           'x)
     (apply list 'go/func 'C2g_Truth (with-meta [(tag 'x 'any)] {:tag 'bool})
            (remove nil?
                    [(list 'when (list '== 'x nil) (list 'return false))
                     (when bool-init (list 'Boolean_Init))
                     (list 'return (list '!= 'x (list 'conv 'any 'Boolean_FALSE)))]))
     (list 'go/func 'C2g_CastArray :type-params ['T] (with-meta [(tag 'x 'any) (tag 'cls '(* Class))] {:tag 'T})
           (list 'when (list '== 'x nil) (list 'let [(tag 'z 'T) (list 'zero 'T)] (list 'return 'z)))
           ;; checkcast's ClassCastException, with HotSpot's message (jrt.ClassCast)
           (list 'when (list 'not (list '.IsInstance_O__Z 'cls 'x)) (list 'panic (list 'ClassCast 'x 'cls)))
           (list 'assert 'T 'x))
     (list 'go/func 'C2g_Discard (with-meta [(tag 'x 'any)] {:tag 'bool}) false)
     '(go/func C2g_RefP "C2g_RefP is a pointer as an any, nil-preserving (§5.6).\n"
        :type-params [T] ^any [^{:tag (* T)} p]
        (when (== p nil) (return nil))
        p)
     (when (and (m/translated? "java/util/Formatter")
                (not (contains? (:funcs (arbace.c2g.jrt/scan-file (java.io.File. "go/arbace/jrt/string.clj"))) "String_Format_String_O1__String")))
       '(go/func String_Format_String_O1__String
          "String_Format_String_O1__String is String.format: a new Formatter's format (c2g's, over the
translated java.util.Formatter).\n"
          ^{:tag (* String)} [^{:tag (* String)} format ^{:tag (* RefArray)} args]
          (.ToString__String (.Format_String_O1__Formatter (Formatter_New) format args))))
     (when (m/translated? "java/util/Formatter")
       '(go/method Formatted_O1__String
          "Formatted_O1__String is String.formatted: String.format(this, args).\n"
          ^{:tag (* String)} [^{:tag (* String)} t ^{:tag (* RefArray)} args]
          (.ToString__String (.Format_String_O1__Formatter (Formatter_New) t args))))
     ;; String.join over an Iterable (jrt's String cannot name the translated Iterable)
     (when (and (m/translated? "java/lang/Iterable") (m/translated? "java/lang/CharSequence"))
       '(go/func String_Join_CharSequence_Iterable__String
          "String_Join_CharSequence_Iterable__String is String.join(CharSequence, Iterable): each
element cast to CharSequence (the for loop's checkcast), then joined as the array overload joins.\n"
          ^{:tag (* String)} [^CharSequence sep ^Iterable elements]
          (when (== sep nil) (panic (NPE)))
          (when (== elements nil) (panic (NPE)))
          (let [^{:tag (slice any)} es nil
                it (.Iterator__Iterator elements)]
            (while (.HasNext__Z it)
              (set! es (append es (CharSequence_Cast (.Next__O it)))))
            (String_Join_CharSequence_CharSequence1__String sep (RefArrayOf CharSequence_class (spread es))))))
     (when (m/translated? "java/util/Formatter")
       '(go/func String_Format_Locale_String_O1__String
          "String_Format_Locale_String_O1__String is String.format(Locale, ...).\n"
          ^{:tag (* String)} [^{:tag (* Locale)} l ^{:tag (* String)} format ^{:tag (* RefArray)} args]
          (.ToString__String (.Format_String_O1__Formatter (Formatter_New_Locale l) format args))))
     ;; Throwable.printStackTrace(PrintStream|PrintWriter) and System.getenv() for the REPL's
     ;; reflection (step 5 phase 2B): jrt cannot name the translated classes
     (when (m/translated? "java/io/PrintWriter")
       (list 'go/func 'C2g_PrintStackTraceWriter
             "C2g_PrintStackTraceWriter is Throwable.printStackTrace(PrintWriter): the trace's text printed.\n"
             [(tag 't 'Throwable_I) (tag 'w (m/go-type :jrt "Ljava/io/PrintWriter;"))]
             '(.Print_String__V (NN w) (StackTraceString t))))
     (when (m/translated? "java/io/PrintStream")
       (list 'go/func 'C2g_PrintStackTraceStream
             "C2g_PrintStackTraceStream is Throwable.printStackTrace(PrintStream).\n"
             [(tag 't 'Throwable_I) (tag 's (m/go-type :jrt "Ljava/io/PrintStream;"))]
             '(.Print_String__V (NN s) (StackTraceString t))))
     (when (and (m/translated? "java/util/HashMap") (m/translated? "java/util/Collections"))
       (list 'go/func 'C2g_SystemGetenv
             "C2g_SystemGetenv is System.getenv(): the host's environment as an unmodifiable map.\n"
             (with-meta [] {:tag (m/go-type :jrt "Ljava/util/Map;")})
             '(let [m (HashMap_New)]
                (range [_ kv (.Environ (CurrentHost))]
                  (let [i (strings/IndexByte kv \=)]
                    (when (> i 0)
                      (.Put_O_O__O m (Str (subslice kv _ i)) (Str (subslice kv (+ i 1)))))))
                (Collections_UnmodifiableMap_Map__Map m))))
     ;; jrt's statics whose values are translated objects (C2G-NOTES.md, phase 2C)
     (when (m/translated? "jdk/internal/jrt/StandardStreams")
       (let [is (m/go-type :jrt "Ljava/io/InputStream;")
             ps (m/go-type :jrt "Ljava/io/PrintStream;")]
         (list 'go/var [(tag 'System_in is)] [(tag 'System_out ps)] [(tag 'System_err ps)])))
     (when (m/translated? "jdk/internal/jrt/StandardStreams")
       (list 'go/func 'System_SetIn_InputStream__V "System_SetIn_InputStream__V is System.setIn.\n"
             [(tag 'in (m/go-type :jrt "Ljava/io/InputStream;"))] '(set! System_in in)))
     (when (m/translated? "jdk/internal/jrt/StandardStreams")
       (list 'go/func 'System_SetOut_PrintStream__V "System_SetOut_PrintStream__V is System.setOut.\n"
             [(tag 'out (m/go-type :jrt "Ljava/io/PrintStream;"))] '(set! System_out out)))
     (when (m/translated? "jdk/internal/jrt/StandardStreams")
       (list 'go/func 'System_SetErr_PrintStream__V "System_SetErr_PrintStream__V is System.setErr.\n"
             [(tag 'err (m/go-type :jrt "Ljava/io/PrintStream;"))] '(set! System_err err)))
     (when (m/translated? "jdk/internal/jrt/CaseInsensitiveComparator")
       (list 'go/var (tag 'String_CASE_INSENSITIVE_ORDER (m/go-type :jrt "Ljava/util/Comparator;"))))
     (when (or (m/translated? "jdk/internal/jrt/StandardStreams") (m/translated? "jdk/internal/jrt/CaseInsensitiveComparator"))
       (apply list 'go/func 'init []
              (concat
                (when (m/translated? "jdk/internal/jrt/CaseInsensitiveComparator")
                  (concat (when-not (m/trivial-init? "jdk/internal/jrt/CaseInsensitiveComparator")
                            ['(CaseInsensitiveComparator_Init)])
                          ['(set! String_CASE_INSENSITIVE_ORDER CaseInsensitiveComparator_INSTANCE)]))
                (when (m/translated? "jdk/internal/jrt/StandardStreams")
                  ['(set! System_in (StandardStreams_In__InputStream))
                   '(set! System_out (StandardStreams_Out__PrintStream))
                   '(set! System_err (StandardStreams_Err__PrintStream))
                   ;; printStackTrace() and uncaught exceptions print to System.err, as the
                   ;; JVM's do (jrt's host stream while it is null)
                   (list 'let [(tag 'host (list 'func ['string])) 'StderrPrint]
                         (list 'set! 'StderrPrint
                               (list 'fn [(tag 's 'string)]
                                     (list 'when (list '== 'System_err nil)
                                           '(host s)
                                           '(return))
                                     '(.Print_String__V System_err (Str s))
                                     '(.Flush__V System_err))))]))))
     ;; records' derived equals and hashCode (java.lang.runtime.ObjectMethods)
     '(go/func C2g_ObjHash "C2g_ObjHash is Objects.hashCode.\n" ^int32 [^any x]
        (when (== x nil) (return 0))
        (HashCode x))
     '(go/func C2g_ObjEquals "C2g_ObjEquals is Objects.equals.\n" ^bool [^any a ^any b]
        (when (== a b) (return true))
        (when (or (== a nil) (== b nil)) (return false))
        (Equals a b))
     '(go/func C2g_DoubleBits "C2g_DoubleBits is Double.doubleToLongBits (one NaN).\n" ^int64 [^float64 d]
        (when (math/IsNaN d) (return 0x7ff8000000000000))
        (conv int64 (math/Float64bits d)))
     '(go/func C2g_FloatBits "C2g_FloatBits is Float.floatToIntBits (one NaN).\n" ^int32 [^float32 f]
        (when (!= f f) (return 0x7fc00000))
        (conv int32 (math/Float32bits f)))
     (list 'go/type 'C2gCloner (list 'interface (list 'CloneShallow (with-meta [] {:tag 'any}))))
     (list 'go/func 'C2g_ObjectClone (with-meta [(tag 'x 'any)] {:tag 'any})
           (list '.CloneShallow (list 'assert 'C2gCloner 'x)))]))))

(defn- modifiers [n]
  (let [dd (a/decl n)
        fl (if (and dd (not= :top (:nesting dd))) (:inner-flags dd) (:flags (m/info n)))
        fl (or fl 1)]
    (bit-and fl 0x761f)))

(defn- kind-sym [pkg n]
  (let [dd (or (a/decl n) {:kind (if (m/interface? n) :interface :class)})]
    (m/jrt-sym pkg (case (:kind dd)
                     :interface "KindInterface" :annotation "KindAnnotation" :enum "KindEnum"
                     :record "KindRecord" "KindClass"))))

(defn- arg-conv [pkg d x]
  (cond (t/prim? d) (list 'assert (m/go-type pkg d) x)
        (= d "Ljava/lang/Object;") x
        :else (list (list 'inst (m/jrt-sym pkg "As") (m/go-type pkg d)) x)))

(defn- result-any [pkg d call]
  (cond (= d "V") nil
        (m/pointer-desc? d) (list (m/jrt-sym pkg "C2g_RefP") call)
        :else call))

(def ^:dynamic *absent-members*
  "Whether member tables list the public methods that do not exist in Go (bin/c2g --program)."
  false)

(defn member-tables
  "The member tables (§5.11, amendment R12) of translated class n: its public members."
  [pkg n]
  (binding [c/*f* {:pkg pkg}]
    (let [g (m/go-name n)
          iface (m/interface? n)
          leaf (and (not iface) (m/leaf? n))
          rty (cond iface (symbol g) leaf (list '* (symbol g)) :else (symbol (str g "_I")))
          abstract (m/has? (:flags (a/decl n)) Opcodes/ACC_ABSTRACT)
          public? #(m/has? (:flags %) Opcodes/ACC_PUBLIC)
          cls (fn [dd] (c/class-val dd))
          methods (for [mm (d/class-methods n)
                        :when (and (public? mm) (not= "<init>" (:name mm)))
                        :let [[ps r] (t/parse-method-desc (:desc mm))
                              base (nm/method-base (:name mm) (:desc mm))
                              static (m/static? mm)
                              args (map-indexed (fn [i p] (arg-conv pkg p (list 'aget 'args i))) ps)
                              call (cond static (apply list (symbol (str g "_" base)) args)
                                         :else (apply list (symbol (str "." base)) (list 'assert rty 'this) args))
                              body (if (= "V" r) [call nil] [(result-any pkg r call)])]]
                    (list 'lit (m/jrt-sym pkg "MethodInfo")
                          :Name (:name mm)
                          :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) (map cls ps)))
                          :Return (cls r)
                          :Modifiers (bit-and (:flags mm) 0x1fff)
                          :Invoke (list* 'fn (with-meta [(tag 'this 'any) (tag 'args '(slice any))] {:tag 'any}) body)))
          ;; --program: the public methods that do not exist in Go (their descriptors name
          ;; classes outside the world), as entries that throw, so that code naming them
          ;; analyzes (RT/toUrl) and fails when run (EVAL-NOTES.md, "Cut classes")
          absent (when *absent-members*
                   (let [present (set (map (juxt :name :desc) (d/class-methods n)))]
                     (for [mm (:methods (a/decl n))
                           :when (and (public? mm) (not= "<init>" (:name mm)) (not= "<clinit>" (:name mm))
                                      (not (present [(:name mm) (:desc mm)])))
                           :let [[ps r] (t/parse-method-desc (:desc mm))]]
                       (list 'lit (m/jrt-sym pkg "MethodInfo")
                             :Name (:name mm)
                             :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) (map cls ps)))
                             :Return (cls r)
                             :Modifiers (bit-and (:flags mm) 0x1fff)
                             :Invoke (list 'fn (with-meta [(tag 'this 'any) (tag 'args '(slice any))] {:tag 'any})
                                           (list 'panic (list (m/jrt-sym pkg "Thrown")
                                                              (list (m/jrt-sym pkg "UnsupportedOperationException_New_String")
                                                                    (list (m/jrt-sym pkg "Str")
                                                                          (str (java-name n) "." (:name mm) (:desc mm)
                                                                               " is not in the Go build"))))))))))
          methods (concat methods absent)
          ;; a name's overloads in the order the JVM's getDeclaredMethods gives them (when the
          ;; class is on this JVM), which Reflector's choice among applicable overloads follows
          mms (concat (for [mm (d/class-methods n) :when (and (public? mm) (not= "<init>" (:name mm)))] mm)
                      (when *absent-members*
                        (let [present (set (map (juxt :name :desc) (d/class-methods n)))]
                          (for [mm (:methods (a/decl n))
                                :when (and (public? mm) (not= "<init>" (:name mm)) (not= "<clinit>" (:name mm))
                                           (not (present [(:name mm) (:desc mm)])))]
                            mm))))
          methods (let [^Class jc (try (Class/forName (java-name n) false (ClassLoader/getSystemClassLoader))
                                       (catch Throwable _ nil))
                        order (when jc
                                (into {} (map-indexed (fn [i ^java.lang.reflect.Method x]
                                                        [[(.getName x) (vec (map #(.getName ^Class %) (.getParameterTypes x)))] i])
                                                      (.getDeclaredMethods jc))))
                        jname (fn [d] (cond (t/prim? d) (t/prim-desc->name d)
                                            (t/array? d) (str/replace d "/" ".")
                                            :else (str/replace (t/desc->internal d) "/" ".")))]
                    (if (and order (= (count mms) (count methods)))
                      (map (fn [[_ i]] (nth (vec methods) i))
                           (sort-by (fn [[mm i]]
                                             [(:name mm)
                                              (get order [(:name mm) (vec (map jname (first (t/parse-method-desc (:desc mm)))))]
                                                   Integer/MAX_VALUE)
                                              i])
                                    (map vector mms (range))))
                      methods))
          absent-ctors (when (and *absent-members* (not abstract) (not iface))
                         (let [present (set (map :desc (filter #(= "<init>" (:name %)) (d/class-methods n))))]
                           (for [mm (:methods (a/decl n))
                                 :when (and (public? mm) (= "<init>" (:name mm)) (not (present (:desc mm))))
                                 :let [[ps _] (t/parse-method-desc (:desc mm))]]
                             (list 'lit (m/jrt-sym pkg "CtorInfo")
                                   :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) (map cls ps)))
                                   :Modifiers (bit-and (:flags mm) 0x1fff)
                                   :New (list 'fn (with-meta [(tag 'args '(slice any))] {:tag 'any})
                                              (list 'panic (list (m/jrt-sym pkg "Thrown")
                                                                 (list (m/jrt-sym pkg "UnsupportedOperationException_New_String")
                                                                       (list (m/jrt-sym pkg "Str")
                                                                             (str (java-name n) ".<init>" (:desc mm)
                                                                                  " is not in the Go build"))))))))))
          ctors (for [mm (d/class-methods n)
                      :when (and (public? mm) (= "<init>" (:name mm)) (not abstract) (not iface))
                      :let [real (e/ctor-real-desc n mm)
                            [ps _] (t/parse-method-desc (:desc mm))
                            [rps _] (t/parse-method-desc real)]
                      :when (= (count ps) (count rps))]
                  (list 'lit (m/jrt-sym pkg "CtorInfo")
                        :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) (map cls ps)))
                        :Modifiers (bit-and (:flags mm) 0x1fff)
                        :New (list 'fn (with-meta [(tag 'args '(slice any))] {:tag 'any})
                                   (apply list (symbol (nm/new-name g real))
                                          (map-indexed (fn [i p] (arg-conv pkg p (list 'aget 'args i))) ps)))))
          fields (for [f (d/class-fields n)
                       :when (public? f)
                       :let [static (m/static? f)
                             place (if static (symbol (nm/static-field-name g (:name f)))
                                       (list (symbol (str ".-" (nm/field-name (:name f))))
                                             (if (or iface leaf) (list 'assert rty 'o)
                                                 (list (symbol (str ".Self_" g)) (list 'assert rty 'o)))))
                             vol (c/volatile-field? f)
                             cval (env/const-value f)
                             final (m/has? (:flags f) Opcodes/ACC_FINAL)]]
                   (list* 'lit (m/jrt-sym pkg "FieldInfo")
                          :Name (:name f) :Type (cls (:desc f)) :Modifiers (bit-and (:flags f) 0x5fff)
                          :Get (list 'fn (with-meta [(tag 'o 'any)] {:tag 'any})
                                     (let [r (if vol (c/volatile-read place (:desc f)) place)]
                                       (or (result-any pkg (:desc f) r) r)))
                          (when-not final
                            [:Set (list 'fn [(tag 'o 'any) (tag 'v 'any)]
                                        (if vol
                                          (c/volatile-write place (:desc f) (arg-conv pkg (:desc f) 'v))
                                          (list 'set! place (arg-conv pkg (:desc f) 'v))))])))]
      {:methods methods :ctors (concat ctors absent-ctors) :fields fields})))

(defn class-registration
  "C_class (Define, hierarchy only) and the init function setting its function and table
  fields (§5.11, amendment J1)."
  [pkg n]
  (binding [c/*f* {:pkg pkg}]
    (let [g (m/go-name n)
          dd (or (a/decl n) (assoc (m/info n) :kind :interface :nesting (if (:outer (m/info n)) :member :top)
                                   :simple (t/simple-name-of n) :reflected true))
          iface (m/interface? n)
          sup (:super dd)
          ;; a superinterface outside the world stands for its own superinterfaces (JDK 21's
          ;; SequencedCollection between List and Collection), so that getInterfaces still
          ;; reaches Collection (core's print-method preferences: EVAL-NOTES.md)
          ifaces (distinct (mapcat (fn up [i] (if (m/in-world? i) [i] (mapcat up (:interfaces (m/info i)))))
                                   (:interfaces dd)))
          tables (if (:reflected dd) {} (member-tables pkg n))
          cls-sym (symbol (str g "_class"))]
      [(list 'go/var cls-sym
             (list (m/jrt-sym pkg "Define")
                   (list 'addr
                         (apply list 'lit (m/jrt-sym pkg "ClassInfo")
                                (concat
                                  [:Name (java-name n) :Modifiers (modifiers n) :Kind (kind-sym pkg n)]
                                  (when (and sup (not iface)) [:Super (c/class-val (str "L" sup ";"))])
                                  (when (seq ifaces)
                                    [:Interfaces (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class")))
                                                        (map #(c/class-val (str "L" % ";")) ifaces))])
                                  (when (and (= :member (:nesting dd)) (m/in-world? (:outer dd)))
                                    [:Declaring (c/class-val (str "L" (:outer dd) ";"))])
                                  (when-not (= :top (:nesting dd))
                                    [:Simple (or (:simple dd) "")])
                                  [:Go (str (nm/pkg-path pkg) "." g)])))))
       (list 'go/func 'init []
             (list* 'let ['i (list '.Info cls-sym)]
                    (concat
                      [(list 'set! (list '.-IsInstance 'i) (symbol (str g "_InstanceOf")))]
                      (when-not (m/trivial-init? n)
                        [(list 'set! (list '.-Init 'i) (symbol (str g "_Init")))])
                      (when (= :enum (:kind dd))
                        [(list 'set! (list '.-Enum 'i)
                               (list* 'fn (with-meta [] {:tag (list '* (m/jrt-sym pkg "RefArray"))})
                                      (concat (when-not (m/trivial-init? n) [(list (symbol (str g "_Init")))])
                                              [(symbol (str g "__VALUES"))])))])
                      (when (seq (:methods tables))
                        [(list 'set! (list '.-Methods 'i) (apply list 'lit (list 'slice (m/jrt-sym pkg "MethodInfo")) (:methods tables)))])
                      (when (seq (:ctors tables))
                        [(list 'set! (list '.-Ctors 'i) (apply list 'lit (list 'slice (m/jrt-sym pkg "CtorInfo")) (:ctors tables)))])
                      (when (seq (:fields tables))
                        [(list 'set! (list '.-Fields 'i) (apply list 'lit (list 'slice (m/jrt-sym pkg "FieldInfo")) (:fields tables)))])
                      (when-not iface
                        [(list (m/jrt-sym pkg "RegisterGoType") cls-sym (list (list 'inst 'reflect/TypeFor (symbol g))))]))))])))

(defn cut-forms
  "Class objects of classes outside the closed world that code names (class literals):
  registered by name only, so that Class.forName finds them and nothing is their instance
  (\"cut\" classes, C2G-SPEC §4.1)."
  [pkg classes]
  (for [n (sort classes)]
    (list 'go/var (symbol (str (m/go-name n) "_class"))
          (list (m/jrt-sym pkg "Define")
                (list 'addr (list 'lit (m/jrt-sym pkg "ClassInfo")
                                  :Name (java-name n)
                                  :Modifiers 1 :Kind (if (env/interface? n) (m/jrt-sym pkg "KindInterface") (m/jrt-sym pkg "KindClass"))
                                  :Super (when-not (env/interface? n) (m/jrt-sym pkg "Object_class"))
                                  :Go (str (nm/pkg-path pkg) "." (m/go-name n) " (cut)")))))))

(defn support-table-forms
  "The member table entries of jrt's statics and methods that c2g writes (C2G-NOTES.md, phase
  2C: String.format, String's regex methods, System.in, out, err and their setters), so that
  reflection finds them at the REPL (System/out, String/format). In reflect_tables_c2g.go: its
  init runs after reflect_tables.go's, which sets the hand-written classes' tables."
  []
  (let [sig (fn [nme params ret mods call]
              (list 'lit 'MethodInfo :Name nme :Params (when (seq params) (apply list 'lit '(slice (* Class)) params))
                    :Return ret :Modifiers mods
                    :Invoke (list 'fn '^any [^any this ^{:tag (slice any)} args] call)))
        s0 '((inst As (* String)) (aget args 0))
        s1 '((inst As (* String)) (aget args 1))
        str-ms (concat
                 (when (m/translated? "java/util/Formatter")
                   [(sig "format" '[String_class (.ArrayClass Object_class)] 'String_class 0x89
                         (list 'String_Format_String_O1__String s0 '((inst As (* RefArray)) (aget args 1))))
                    (sig "format" '[Locale_class String_class (.ArrayClass Object_class)] 'String_class 0x89
                         (list 'String_Format_Locale_String_O1__String '((inst As (* Locale)) (aget args 0)) s1
                               '((inst As (* RefArray)) (aget args 2))))
                    (sig "formatted" '[(.ArrayClass Object_class)] 'String_class 0x81
                         (list '.Formatted_O1__String '((inst As (* String)) this) '((inst As (* RefArray)) (aget args 0))))])
                 (when (and (m/translated? "java/util/stream/Stream") (m/translated? "java/util/ArrayList"))
                   [(sig "lines" [] 'Stream_class 0x1 '(.Lines__Stream ((inst As (* String)) this)))])
                 (when (and (m/translated? "java/util/Optional") (m/translated? "java/lang/constant/Constable"))
                   [(sig "describeConstable" [] 'Optional_class 0x1 '(.DescribeConstable__Optional ((inst As (* String)) this)))])
                 (when (and (m/translated? "java/lang/Iterable") (m/translated? "java/lang/CharSequence"))
                   [(sig "join" '[CharSequence_class Iterable_class] 'String_class 0x9
                         (list 'String_Join_CharSequence_Iterable__String '((inst As CharSequence) (aget args 0))
                               '((inst As Iterable) (aget args 1))))])
                 (when (m/translated? "java/util/regex/Pattern")
                   [(sig "matches" '[String_class] 'Prim_boolean 0x1
                         (list '.Matches_String__Z '((inst As (* String)) this) s0))
                    (sig "replaceAll" '[String_class String_class] 'String_class 0x1
                         (list '.ReplaceAll_String_String__String '((inst As (* String)) this) s0 s1))
                    (sig "replaceFirst" '[String_class String_class] 'String_class 0x1
                         (list '.ReplaceFirst_String_String__String '((inst As (* String)) this) s0 s1))
                    (sig "split" '[String_class] '(.ArrayClass String_class) 0x1
                         (list '.Split_String__String1 '((inst As (* String)) this) s0))
                    (sig "split" '[String_class Prim_int] '(.ArrayClass String_class) 0x1
                         (list '.Split_String_I__String1 '((inst As (* String)) this) s0 '(assert int32 (aget args 1))))]))
        streams? (m/translated? "jdk/internal/jrt/StandardStreams")
        pst-w? (m/translated? "java/io/PrintWriter")
        pst-s? (m/translated? "java/io/PrintStream")
        getenv? (and (m/translated? "java/util/HashMap") (m/translated? "java/util/Collections"))
        ;; the context class loader and resources (core's data_readers lookup, io/resource):
        ;; the system loader; resources are the host's (the program's embedded sources), with
        ;; no URLs (java.net is cut)
        loaders? (and (m/translated? "java/util/Collections") (m/translated? "java/io/ByteArrayInputStream"))
        ci? (m/translated? "jdk/internal/jrt/CaseInsensitiveComparator")
        ;; Date's members that name java.time.Instant, which jrt's own build cannot (JRT-NOTES.md,
        ;; phase 2B "Dates")
        instant? (m/translated? "java/time/Instant")]
    (when (or (seq str-ms) streams? loaders? ci? pst-w? pst-s? getenv? instant?)
      [(apply list 'go/func 'init []
              (concat
                (when (or pst-w? pst-s?)
                  [(apply list 'set! '(.-Methods (.Info Throwable_class))
                          [(apply list 'append '(.-Methods (.Info Throwable_class))
                                  (concat
                                    (when pst-s?
                                      [(sig "printStackTrace" '[PrintStream_class] 'Prim_void 0x1
                                            '(do (C2g_PrintStackTraceStream (assert Throwable_I this) (PrintStream_Cast (aget args 0)))
                                                 (return nil)))])
                                    (when pst-w?
                                      [(sig "printStackTrace" '[PrintWriter_class] 'Prim_void 0x1
                                            '(do (C2g_PrintStackTraceWriter (assert Throwable_I this) (PrintWriter_Cast (aget args 0)))
                                                 (return nil)))])))])])
                (when getenv?
                  ['(set! (.-Methods (.Info System_class))
                          (append (.-Methods (.Info System_class))
                                  (lit MethodInfo :Name "getenv" :Return Map_class :Modifiers 0x9
                                       :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (C2g_SystemGetenv)))))])
                (when ci?
                  ['(set! (.-Fields (.Info String_class))
                          (append (.-Fields (.Info String_class))
                                  (lit FieldInfo :Name "CASE_INSENSITIVE_ORDER" :Type Comparator_class :Modifiers 0x19
                                       :Get (fn ^any [^any o] String_CASE_INSENSITIVE_ORDER))))])
                (when loaders?
                  [(list 'set! '(.-Methods (.Info Thread_class))
                         (list 'append '(.-Methods (.Info Thread_class))
                               (sig "getContextClassLoader" [] 'ClassLoader_class 0x1
                                    '(ClassLoader_GetSystemClassLoader__ClassLoader))
                               (sig "setContextClassLoader" '[ClassLoader_class] 'Prim_void 0x1 '(return nil))))
                   (list 'set! '(.-Methods (.Info ClassLoader_class))
                         (list 'append '(.-Methods (.Info ClassLoader_class))
                               (sig "getResources" '[String_class] 'Enumeration_class 0x1
                                    '(Collections_EmptyEnumeration__Enumeration))
                               (sig "getResourceAsStream" '[String_class] 'InputStream_class 0x1
                                    '(let [(values b ok) (ResourceOrPath (.String (NN ((inst As (* String)) (aget args 0)))))]
                                       (when (not ok) (return nil))
                                       (let [a (NewByteArray (conv int32 (len b)))]
                                         (range [i x b] (aset (.-A a) i (conv int8 x)))
                                         (return (ByteArrayInputStream_New_B1 a)))))))])
                (when instant?
                  [(list 'set! '(.-Methods (.Info Date_class))
                         (list 'append '(.-Methods (.Info Date_class))
                               (sig "from" '[Instant_class] 'Date_class 0x9
                                    '(Date_New_J (.ToEpochMilli__J (NN ((inst As (* Instant)) (aget args 0))))))
                               (sig "toInstant" [] 'Instant_class 0x1
                                    '(Instant_OfEpochMilli_J__Instant (.GetTime__J (assert Date_I this))))))])
                (when (seq str-ms)
                  [(list 'set! '(.-Methods (.Info String_class))
                         (apply list 'append '(.-Methods (.Info String_class)) str-ms))])
                (when streams?
                  ['(set! (.-Fields (.Info System_class))
                          (append (.-Fields (.Info System_class))
                                  (lit FieldInfo :Name "err" :Type PrintStream_class :Modifiers 0x19
                                       :Get (fn ^any [^any o] (.Ref System_err)))
                                  (lit FieldInfo :Name "in" :Type InputStream_class :Modifiers 0x19
                                       :Get (fn ^any [^any o] System_in))
                                  (lit FieldInfo :Name "out" :Type PrintStream_class :Modifiers 0x19
                                       :Get (fn ^any [^any o] (.Ref System_out)))))
                   (list 'set! '(.-Methods (.Info System_class))
                         (list 'append '(.-Methods (.Info System_class))
                               (sig "setErr" '[PrintStream_class] 'Prim_void 0x9
                                    '(do (System_SetErr_PrintStream__V (PrintStream_Cast (aget args 0))) (return nil)))
                               (sig "setIn" '[InputStream_class] 'Prim_void 0x9
                                    '(do (System_SetIn_InputStream__V (InputStream_Cast (aget args 0))) (return nil)))
                               (sig "setOut" '[PrintStream_class] 'Prim_void 0x9
                                    '(do (System_SetOut_PrintStream__V (PrintStream_Cast (aget args 0))) (return nil)))))])))])))

(defn- cut-type-expr
  "The Class expression of JVM class c as a cut class's member table names it from pkg, or nil
  when the Go build has no Class object for it there (a class outside the world that is not
  cut, or one of arbace/lang named from jrt)."
  [pkg cuts ^Class c]
  (cond
    (.isPrimitive c) (symbol (str (if (= pkg :jrt) "" "jrt/") "Prim_" (.getName c)))
    (.isArray c) (when-let [e (cut-type-expr pkg cuts (.getComponentType c))] (list '.ArrayClass e))
    :else (let [n (str/replace (.getName c) "." "/")]
            (when (and (or (m/in-world? n) (contains? cuts n))
                       (or (= pkg :lang) (= :jrt (m/pkg n)))
                       ;; jrt's hand-written classes without a Class object
                       (or (not (m/hand-written? n)) (= n "java/lang/Object")
                           (contains? (:vars (:jrt m/*w*)) (str (m/go-name n) "_class"))))
              (if (= n "java/lang/Object") (m/jrt-sym pkg "Object_class") (m/class-sym pkg n "_class"))))))

(defn cut-table-forms
  "The member tables of the cut classes of pkg (C2G-SPEC §4.1, D6; doc/go/EVAL-NOTES.md): their
  public members as the JVM reports them, whose types the Go build can name, each throwing
  UnsupportedOperationException when called, so that code naming them analyzes (core's
  annotation support names ASM's Type) and fails only when run."
  [pkg cuts classes]
  (let [stub (fn [what]
               (list 'panic (list (m/jrt-sym pkg "Thrown")
                                  (list (m/jrt-sym pkg "UnsupportedOperationException_New_String")
                                        (list (m/jrt-sym pkg "Str") (str what " is not in the Go build"))))))
        tables (for [n (sort classes)
                     :let [^Class c (try (Class/forName (str/replace n "/" ".") false (ClassLoader/getSystemClassLoader))
                                         (catch Throwable _ nil))]
                     :when c
                     :let [cls (m/class-sym pkg n "_class")
                           texpr #(cut-type-expr pkg cuts %)
                           ms (for [^java.lang.reflect.Method mt (sort-by #(str (.getName ^java.lang.reflect.Method %) (vec (map str (.getParameterTypes ^java.lang.reflect.Method %))))
                                                                         (.getMethods c))
                                    :when (= c (.getDeclaringClass mt))
                                    :let [ps (map texpr (.getParameterTypes mt))
                                          r (texpr (.getReturnType mt))]
                                    :when (and r (every? some? ps))]
                                (list 'lit (m/jrt-sym pkg "MethodInfo") :Name (.getName mt)
                                      :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) ps))
                                      :Return r :Modifiers (.getModifiers mt)
                                      :Invoke (list 'fn (with-meta [(with-meta 'this {:tag 'any}) (with-meta 'args {:tag '(slice any)})] {:tag 'any})
                                                    (stub (str (.getName c) "." (.getName mt))))))
                           fs (for [^java.lang.reflect.Field fd (sort-by #(.getName ^java.lang.reflect.Field %) (.getFields c))
                                    :when (= c (.getDeclaringClass fd))
                                    :let [t (texpr (.getType fd))]
                                    :when t]
                                (list 'lit (m/jrt-sym pkg "FieldInfo") :Name (.getName fd) :Type t :Modifiers (.getModifiers fd)
                                      :Get (list 'fn (with-meta [(with-meta 'o {:tag 'any})] {:tag 'any})
                                                 (stub (str (.getName c) "." (.getName fd))))))
                           ks (for [^java.lang.reflect.Constructor k (sort-by #(vec (map str (.getParameterTypes ^java.lang.reflect.Constructor %))) (.getConstructors c))
                                    :let [ps (map texpr (.getParameterTypes k))]
                                    :when (every? some? ps)]
                                (list 'lit (m/jrt-sym pkg "CtorInfo")
                                      :Params (when (seq ps) (apply list 'lit (list 'slice (list '* (m/jrt-sym pkg "Class"))) ps))
                                      :Modifiers (.getModifiers k)
                                      :New (list 'fn (with-meta [(with-meta 'args {:tag '(slice any)})] {:tag 'any})
                                                 (stub (str (.getName c) "'s constructor")))))]
                     :when (or (seq ms) (seq fs) (seq ks))]
                 (concat
                   (when (seq ms) [(list 'set! (list '.-Methods (list '.Info cls)) (apply list 'lit (list 'slice (m/jrt-sym pkg "MethodInfo")) ms))])
                   (when (seq fs) [(list 'set! (list '.-Fields (list '.Info cls)) (apply list 'lit (list 'slice (m/jrt-sym pkg "FieldInfo")) fs))])
                   (when (seq ks) [(list 'set! (list '.-Ctors (list '.Info cls)) (apply list 'lit (list 'slice (m/jrt-sym pkg "CtorInfo")) ks))])))]
    (when (seq tables)
      [(apply list 'go/func 'init [] (apply concat tables))])))

(defn frames-forms
  "The frame table (§7.9.6, amendment J4): entries for methods whose Java names the demangler
  cannot read back (names with _ or $)."
  [pkg classes]
  (let [entries (for [n classes
                      :when (a/decl n)
                      :let [g (m/go-name n)]
                      mm (d/class-methods n)
                      :when (and (not= "<init>" (:name mm)) (re-find #"[_$]" (:name mm)))
                      :let [base (nm/method-base (:name mm) (:desc mm))
                            static (m/static? mm)
                            iface (m/interface? n)
                            leaf (and (not iface) (m/leaf? n))
                            gofn (str (nm/pkg-path pkg) "."
                                      (cond (or static iface) (str g "_" base)
                                            leaf (str "(*" g ")." base)
                                            :else (str "(*" g ").Impl_" base)))]]
                  (list 'lit '_ gofn (str/replace n "/" ".") (:name mm) 0))]
    (when (seq entries)
      [(list 'go/func 'init []
             (list (m/jrt-sym pkg "RegisterFrames") (apply list 'lit (list 'slice (m/jrt-sym pkg "FrameInfo")) entries)))])))
