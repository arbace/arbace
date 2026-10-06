(ns arbace.javalisp.j2l
  "Java -> javalisp: turn a javac compilation unit into layout nodes (arbace.javalisp.layout).
  Every node carries the source line it must be printed on, so the printed text keeps each
  line that javac records. The forms are specified in doc/javalisp/SPEC.md."
  (:require [arbace.javalisp.javac :as javac]
            [arbace.javalisp.layout :as lay :refer [tok lst vect comma]]
            [arbace.javalisp.prec :as prec]
            [clojure.string :as str])
  (:import [com.sun.tools.javac.tree JCTree JCTree$JCCompilationUnit JCTree$JCPackageDecl
            JCTree$JCImport JCTree$JCModuleImport JCTree$JCClassDecl JCTree$JCMethodDecl
            JCTree$JCVariableDecl JCTree$JCSkip JCTree$JCBlock JCTree$JCDoWhileLoop
            JCTree$JCWhileLoop JCTree$JCForLoop JCTree$JCEnhancedForLoop JCTree$JCLabeledStatement
            JCTree$JCSwitch JCTree$JCCase JCTree$JCSwitchExpression JCTree$JCSynchronized
            JCTree$JCTry JCTree$JCCatch JCTree$JCConditional JCTree$JCIf
            JCTree$JCExpressionStatement JCTree$JCBreak JCTree$JCYield JCTree$JCContinue
            JCTree$JCReturn JCTree$JCThrow JCTree$JCAssert JCTree$JCMethodInvocation
            JCTree$JCNewClass JCTree$JCNewArray JCTree$JCLambda JCTree$JCParens JCTree$JCAssign
            JCTree$JCAssignOp JCTree$JCUnary JCTree$JCBinary JCTree$JCTypeCast JCTree$JCInstanceOf
            JCTree$JCAnyPattern JCTree$JCBindingPattern JCTree$JCDefaultCaseLabel
            JCTree$JCConstantCaseLabel JCTree$JCPatternCaseLabel JCTree$JCRecordPattern
            JCTree$JCArrayAccess JCTree$JCFieldAccess JCTree$JCMemberReference JCTree$JCIdent
            JCTree$JCLiteral JCTree$JCPrimitiveTypeTree JCTree$JCArrayTypeTree JCTree$JCTypeApply
            JCTree$JCTypeUnion JCTree$JCTypeIntersection JCTree$JCTypeParameter JCTree$JCWildcard
            JCTree$JCAnnotation JCTree$JCModifiers JCTree$JCAnnotatedType JCTree$JCModuleDecl
            JCTree$JCExports JCTree$JCOpens JCTree$JCProvides JCTree$JCRequires JCTree$JCUses
            JCTree$Tag JCTree$JCLambda$ParameterKind JCTree$JCVariableDecl$DeclKind]
           [com.sun.tools.javac.code Flags TypeTag BoundKind]
           [com.sun.source.tree CaseTree$CaseKind ModuleTree$ModuleKind]))

(def ^:dynamic ^JCTree$JCCompilationUnit *cu* nil)
(def ^:dynamic ^String *text* nil)
(def ^:dynamic *class-name* nil)
(def ^:dynamic *inline-docs*
  "{position text}: doc comments followed by code on their line, keyed by the position of
  that code. They are written #_\"...\" before the declaration they document."
  {})
(def ^:dynamic *used-docs* nil)

(defn ln
  "The line of tree `t`'s preferred position, or of position `pos`."
  [x]
  (if (instance? JCTree x)
    (javac/line *cu* (.-pos ^JCTree x))
    (javac/line *cu* x)))

(declare x xs typ block mods-forms class-decl var-decl)

(defn- start-pos [t] (com.sun.tools.javac.tree.TreeInfo/getStartPos t))

(defn- with-doc
  "Attach the inline doc comment that precedes tree `t` (if any) to the first node of `n`."
  [n t]
  (if-let [d (get *inline-docs* (start-pos t))]
    (let [_ (some-> *used-docs* (vswap! conj (start-pos t)))
          attach (fn attach [n]
                   (cond
                     (map? n) (assoc n :pre d)
                     (sequential? n) (let [[a & more] (drop-while #(or (nil? %) (and (sequential? %) (empty? (lay/kids [%])))) n)]
                                       (cons (attach a) more))
                     :else n))]
      (attach n))
    n))

;;; Literals

(defn- number-token
  "The source text of the numeric literal at `pos`, with the sign javac folded into it."
  [pos]
  (let [^String text *text*
        n (count text)
        neg? (= \- (.charAt text pos))
        i (if neg?
            (loop [i (inc pos)] (if (Character/isWhitespace (.charAt text i)) (recur (inc i)) i))
            pos)
        hex? (and (< (inc i) n) (= \0 (.charAt text i)) (#{\x \X} (.charAt text (inc i))))
        end (loop [j i]
              (if (>= j n)
                j
                (let [c (.charAt text j)
                      prev (.charAt text (dec j))]
                  (cond
                    (or (Character/isLetterOrDigit c) (= c \_) (= c \.)) (recur (inc j))
                    (and (#{\+ \-} c) (if hex? (#{\p \P} prev) (#{\e \E} prev))) (recur (inc j))
                    :else j))))]
    (str (when neg? "-") (subs text i end))))

(def ^:private clojure-float
  #"-?[0-9]+\.[0-9]*([eE][-+]?[0-9]+)?|-?[0-9]+[eE][-+]?[0-9]+")

(defn float-text
  "Clojure-readable text for the floating literal with Java text `s` and value `v`."
  [s v float?]
  (let [s (-> s (str/replace "_" "") (str/replace #"[fFdD]$" ""))
        s (str/replace s #"^(-?)\." "$10.")]
    (if (and (not (re-find #"[xX]" s)) (re-matches clojure-float s))
      s
      (if float? (Float/toString (float v)) (Double/toString (double v))))))

(defn int-text
  "Clojure-readable text for the integral literal with Java text `s`."
  [s]
  (let [s (-> s (str/replace "_" "") (str/replace #"[lL]$" ""))]
    (if-let [[_ sign digits] (re-matches #"(-?)0[bB]([01]+)" s)]
      (str sign "2r" digits)
      s)))

(def char-names
  {\newline "newline" \space "space" \tab "tab" \backspace "backspace" \formfeed "formfeed"
   \return "return"})

(defn printable?
  "True when code point `c` can appear literally in a character or string token."
  [c]
  (let [ch (char c)
        t (Character/getType ch)]
    (not (or (Character/isISOControl ch) (Character/isWhitespace ch) (Character/isSurrogate ch)
             (Character/isSpaceChar ch)
             (contains? #{(int Character/UNASSIGNED) (int Character/FORMAT) (int Character/PRIVATE_USE)} t)))))

(defn char-text
  "Clojure character literal text for char code `c`, or nil when the reader rejects it."
  [c]
  (let [ch (char c)]
    (cond
      (char-names ch) (str \\ (char-names ch))
      (Character/isSurrogate ch) nil
      (printable? c) (str \\ ch)
      :else (format "\\u%04x" (int c)))))

(defn string-text
  "Clojure string literal text for Java string `s`."
  [^String s]
  (let [sb (StringBuilder. "\"")]
    (doseq [c s]
      (case c
        \" (.append sb "\\\"")
        \\ (.append sb "\\\\")
        \newline (.append sb "\\n")
        \tab (.append sb "\\t")
        \return (.append sb "\\r")
        \backspace (.append sb "\\b")
        \formfeed (.append sb "\\f")
        (if (or (= c \space) (printable? (int c)))
          (.append sb c)
          (.append sb (format "\\u%04x" (int c))))))
    (str (.append sb "\""))))

(defn- literal [^JCTree$JCLiteral t]
  (let [l (ln t)
        v (.-value t)]
    (condp = (.-typetag t)
      TypeTag/INT (tok (int-text (number-token (.-pos t))) l)
      TypeTag/LONG (lst l (tok "long") (tok (int-text (number-token (.-pos t))) l))
      TypeTag/DOUBLE (tok (float-text (number-token (.-pos t)) v false) l)
      TypeTag/FLOAT (lst l (tok "float") (tok (float-text (number-token (.-pos t)) v true) l))
      TypeTag/CHAR (if-let [s (char-text v)]
                     (tok s l)
                     (lst l (tok "char") (tok (format "0x%04X" (int v)) l)))
      TypeTag/CLASS (tok (string-text v) l)
      TypeTag/BOOLEAN (tok (if (= 1 v) "true" "false") l)
      TypeTag/BOT (tok "null" l))))

;;; Operators

(def unary-ops
  {JCTree$Tag/POS "+" JCTree$Tag/NEG "-" JCTree$Tag/NOT "!" JCTree$Tag/COMPL "bit-not"
   JCTree$Tag/PREINC "++" JCTree$Tag/PREDEC "--" JCTree$Tag/POSTINC ":++" JCTree$Tag/POSTDEC ":--"})

(def binary-ops
  {JCTree$Tag/OR "||" JCTree$Tag/AND "&&" JCTree$Tag/BITOR "|" JCTree$Tag/BITXOR "bit-xor"
   JCTree$Tag/BITAND "&" JCTree$Tag/EQ "==" JCTree$Tag/NE "!=" JCTree$Tag/LT "<"
   JCTree$Tag/GT ">" JCTree$Tag/LE "<=" JCTree$Tag/GE ">=" JCTree$Tag/SL "<<"
   JCTree$Tag/SR ">>" JCTree$Tag/USR ">>>" JCTree$Tag/PLUS "+" JCTree$Tag/MINUS "-"
   JCTree$Tag/MUL "*" JCTree$Tag/DIV "/" JCTree$Tag/MOD "%"})

(def assign-ops
  {JCTree$Tag/BITOR_ASG "|=" JCTree$Tag/BITXOR_ASG "bit-xor=" JCTree$Tag/BITAND_ASG "&="
   JCTree$Tag/SL_ASG "<<=" JCTree$Tag/SR_ASG ">>=" JCTree$Tag/USR_ASG ">>>="
   JCTree$Tag/PLUS_ASG "+=" JCTree$Tag/MINUS_ASG "-=" JCTree$Tag/MUL_ASG "*="
   JCTree$Tag/DIV_ASG "div=" JCTree$Tag/MOD_ASG "%="})

(def nary-ops
  "Binary operators written n-ary: (op a b c) is ((a op b) op c)."
  #{"||" "&&" "|" "bit-xor" "&" "<<" ">>" ">>>" "+" "-" "*" "/" "%"})

(defn- unparen
  "The expression in the parentheses Java requires around a condition (if, while, ...)."
  [t]
  (if (instance? JCTree$JCParens t) (.-expr ^JCTree$JCParens t) t))

(declare x)

(defn- condition
  "The condition `t` (in the parentheses Java requires) of a statement whose `(` would by
  default be on line `dflt`; a `.` marker gives the line of the `(` when it is elsewhere."
  [t dflt]
  [(when (and (instance? JCTree$JCParens t) (not= (ln t) dflt)) (tok "." (ln t)))
   (x (unparen t))])

;;; Names and selections

(defn- name-chain
  "[[name line] ...] when `t` is an identifier or a chain of selections on one, else nil."
  [t]
  (cond
    (instance? JCTree$JCIdent t) [[(str (.-name ^JCTree$JCIdent t)) (ln t)]]
    (instance? JCTree$JCFieldAccess t)
    (let [^JCTree$JCFieldAccess f t]
      (when-let [c (name-chain (.-selected f))]
        (conj c [(str (.-name f)) (ln f)])))))

(defn- one-line-chain [t]
  (when-let [c (name-chain t)]
    (when (apply = (map second c)) c)))

(defn- simple-call?
  "True when call `m` can be written (a.b.name args...): its method is a name chain on one
  line, and its argument list starts on that line too."
  [^JCTree$JCMethodInvocation m]
  (let [meth (.-meth m)]
    (if (instance? JCTree$JCIdent meth)
      true
      (when-let [c (one-line-chain meth)]
        (= (second (first c)) (ln m))))))

(defn- chain-token [c]
  (tok (str/join "." (map first c)) (second (first c))))

(defn- args [ts] (map x ts))

(defn- targs [ts]
  (when (seq ts) (vect nil (map typ ts))))

(defn- class-body [^JCTree$JCClassDecl def]
  (binding [*class-name* (str (.-name def))]
    (lst (ln def) (tok "class") (xs (.-defs def)))))

(defn- new-class [^JCTree$JCNewClass t]
  (lst (ln t) (tok "new") (targs (.-typeargs t)) (typ (.-clazz t)) (args (.-args t))
       (some-> (.-def t) class-body)))

(defn- chain
  "[base & links] of a selection/call chain: links are field names, (method args) calls and
  (new ...) inner-class creations. A field name sits on the line of its dot; a call's name on
  the line of its argument list, preceded by a `.` marker on the dot's line when that differs."
  [t]
  (cond
    (and (instance? JCTree$JCFieldAccess t) (not (one-line-chain t)))
    (let [^JCTree$JCFieldAccess f t]
      (conj (chain (.-selected f)) (tok (str (.-name f)) (ln f))))

    (and (instance? JCTree$JCMethodInvocation t)
         (instance? JCTree$JCFieldAccess (.-meth ^JCTree$JCMethodInvocation t))
         (not (simple-call? t)))
    (let [^JCTree$JCMethodInvocation m t
          ^JCTree$JCFieldAccess f (.-meth m)
          call-line (ln m)]
      (-> (chain (.-selected f))
          (cond-> (not= (ln f) call-line) (conj (tok "." (ln f))))
          (conj (let [n (tok (str (.-name f)) call-line)
                      tv (targs (.-typeargs m))]
                  (if (and tv (< (lay/first-line tv) call-line))
                    (lst nil tv n (args (.-args m)))
                    (lst nil n tv (args (.-args m))))))))

    (and (instance? JCTree$JCAnnotatedType t)
         (instance? JCTree$JCFieldAccess (.-underlyingType ^JCTree$JCAnnotatedType t)))
    (let [^JCTree$JCFieldAccess f (.-underlyingType ^JCTree$JCAnnotatedType t)
          anns (map x (.-annotations ^JCTree$JCAnnotatedType t))]
      (-> (chain (.-selected f))
          (cond-> (not= (ln f) (lay/first-line (first anns))) (conj (tok "." (ln f))))
          (conj (lst nil (tok ":annotated") anns (tok (str (.-name f)))))))

    (and (instance? JCTree$JCNewClass t) (.-encl ^JCTree$JCNewClass t))
    (conj (chain (.-encl ^JCTree$JCNewClass t)) (new-class t))

    :else [(x t)]))

(defn- chain-form [t]
  (let [[base & links] (chain t)]
    (if links (lst nil (tok "..") base links) base)))

;;; Types

(defn- array-dims
  "[base dims] of type `t`: the innermost non-array element type, and the brackets around it
  outermost first, each as [annotations ArrayTypeTree] (annotations from an AnnotatedType
  wrapping that array type)."
  [t]
  (loop [t t dims []]
    (cond
      (and (instance? JCTree$JCAnnotatedType t)
           (instance? JCTree$JCArrayTypeTree (.-underlyingType ^JCTree$JCAnnotatedType t)))
      (let [^JCTree$JCArrayTypeTree a (.-underlyingType ^JCTree$JCAnnotatedType t)]
        (recur (.-elemtype a) (conj dims [(.-annotations ^JCTree$JCAnnotatedType t) a])))
      (instance? JCTree$JCArrayTypeTree t)
      (recur (.-elemtype ^JCTree$JCArrayTypeTree t) (conj dims [nil t]))
      :else [t dims])))

(defn- array-type
  "(base anns? [] ...) for array type `t`, outermost bracket first, each bracket preceded by
  its type annotations. `before` are extra brackets ahead of t's own, as [annotations
  size-or-nil] (sized dimensions of an array creation); `varargs?` writes the last bracket
  as `...`."
  ([t] (array-type t [] false))
  ([t before varargs?]
   (let [[base dims] (array-dims t)
         brackets (concat (for [[anns e] before] [(map x anns) (vect nil (some-> e x))])
                          (for [[anns a] dims] [(map x anns) (vect (ln a))]))
         brackets (if varargs?
                    (concat (butlast brackets) [[(first (last brackets)) (tok "...")]])
                    brackets)]
     (lst nil (typ base) brackets))))

(defn typ [t]
  (if (seq (second (array-dims t)))
    (array-type t)
    (x t)))

(defn- type-param [^JCTree$JCTypeParameter p]
  (if (and (empty? (.-bounds p)) (empty? (.-annotations p)))
    (tok (str (.-name p)) (ln p))
    (lst nil (map x (.-annotations p)) (tok (str (.-name p)) (when (empty? (.-annotations p)) (ln p)))
         (when (seq (.-bounds p)) [(tok "extends") (map typ (.-bounds p))]))))

(defn- tparams [ps]
  (when (seq ps) (vect nil (map type-param ps))))

;;; Modifiers

(def modifier-flags
  [[Flags/PUBLIC "public"] [Flags/PROTECTED "protected"] [Flags/PRIVATE "private"]
   [Flags/ABSTRACT "abstract"] [Flags/STATIC "static"] [Flags/FINAL "final"]
   [Flags/TRANSIENT "transient"] [Flags/VOLATILE "volatile"] [Flags/SYNCHRONIZED "synchronized"]
   [Flags/NATIVE "native"] [Flags/STRICTFP "strictfp"] [Flags/DEFAULT "default"]
   [Flags/SEALED ":sealed"] [Flags/NON_SEALED ":non-sealed"]])

(defn- keyword-pos
  "The position of the first of `words` in the source between positions `from` and `to`."
  [words from to]
  (when (and (seq words) from to (< -1 from to))
    (let [s (-> (subs *text* from (min to (count *text*)))
                (str/replace #"(?s)\"\"\".*?\"\"\"|\"(\\.|[^\"\\])*\"|'(\\.|[^'\\])*'|/\*.*?\*/|//[^\n]*"
                             #(apply str (repeat (count (first %)) \space))))
          m (re-matcher (re-pattern (str "\\b(" (str/join "|" (map #(str/replace % ":" "") words))
                                         ")\\b"))
                        s)]
      (when (.find m) (+ from (.start m))))))

(defn mods-forms
  "Annotations and modifier keywords of `m`, leaving out `except` flags, in source order (the
  keywords go together where the first of them is). `end` is the position of the
  declaration's name: the modifiers lie before it. `anns` overrides the annotations."
  ([m end] (mods-forms m end 0))
  ([m end except] (mods-forms m end except (some-> ^JCTree$JCModifiers m .-annotations)))
  ([^JCTree$JCModifiers m end except anns]
   (when m
     (let [flags (bit-and-not (.-flags m) except)
           words (for [[f w] modifier-flags :when (not (zero? (bit-and flags f)))] w)
           kp (keyword-pos words (.-pos m) end)
           kws (map-indexed (fn [i w] (tok w (when (and kp (zero? i)) (ln kp)))) words)
           [before after] (split-with #(or (nil? kp) (< (.-pos ^JCTree %) kp)) anns)]
       (concat (map x before) kws (map x after))))))

;;; Declarations

(defn- var-type
  "The type of variable `v`: a type form, `var`, or nil when implicitly typed."
  [^JCTree$JCVariableDecl v]
  (cond
    (.-vartype v)
    (if (not (zero? (bit-and (.-flags (.-mods v)) Flags/VARARGS)))
      (array-type (.-vartype v) [] true)
      (typ (.-vartype v)))
    (= (.-declKind v) JCTree$JCVariableDecl$DeclKind/VAR) (tok "var" (ln (.-typePos v)))))

(defn- var-name
  "The name of variable `v`: `_` for an unnamed one, which javac names with the empty name."
  [^JCTree$JCVariableDecl v]
  (if-let [e (.-nameexpr v)]
    (x e)
    (tok (let [n (str (.-name v))] (if (= n "") "_" n)) (ln v))))

(def implied-param-flags (bit-or Flags/PARAMETER Flags/VARARGS))

(defn param
  "The elements of parameter (or other variable without initializer) `v`."
  [^JCTree$JCVariableDecl v]
  (with-doc [(mods-forms (.-mods v) (.-pos v) implied-param-flags) (var-type v) (var-name v)] v))

(defn- params [ps]
  (vect nil (interpose comma (map param ps))))

(defn- peel
  "The chain of type trees from `t` down through its array element types."
  [t]
  (take-while some? (iterate #(when (instance? JCTree$JCArrayTypeTree %) (.-elemtype ^JCTree$JCArrayTypeTree %)) t)))

(defn var-decl
  "(:var mods type name = init, name2 = init2 ...) for the declarators `vs` of one
  declaration (javac gives them one shared modifiers tree). A declarator with brackets
  after its name (int a, b[]) is written (b []) when its type differs from the shared one."
  [vs]
  (let [^JCTree$JCVariableDecl v (first vs)
        chains (map #(peel (.-vartype ^JCTree$JCVariableDecl %)) vs)
        base (if (and (next vs) (.-vartype v))
               (first (filter (fn [t] (every? (fn [c] (some #(identical? t %) c)) chains)) (first chains)))
               (.-vartype v))
        dims (fn [c] (count (take-while #(not (identical? base %)) c)))]
    (lst nil (tok ":var") (mods-forms (.-mods v) (.-pos v) implied-param-flags)
         (if (and (next vs) base)
           (typ base)
           (var-type v))
         (for [[^JCTree$JCVariableDecl d c] (map vector vs chains)
               :let [k (if (and (next vs) base) (dims c) 0)]]
           [(if (pos? k) (lst nil (var-name d) (map #(vect (ln %)) (take k c))) (var-name d))
            (when (.-init d) [(tok "=") (x (.-init d))])
            (when-not (identical? d (last vs)) comma)]))))

(defn group-decls
  "Group the consecutive variable declarations in `trees` that javac split from one
  declaration (they share their modifiers tree) into vectors."
  [trees]
  (reduce (fn [acc t]
            (let [prev (peek acc)]
              (if (and (instance? JCTree$JCVariableDecl t) (vector? prev)
                       (identical? (.-mods ^JCTree$JCVariableDecl t) (.-mods ^JCTree$JCVariableDecl (first prev))))
                (conj (pop acc) (conj prev t))
                (conj acc (if (instance? JCTree$JCVariableDecl t) [t] t)))))
          [] trees))

(defn xs
  "Layout nodes for the statements or members `trees`, keeping multi-variable declarations
  together."
  [trees]
  (map #(if (vector? %) (with-doc (var-decl %) (first %)) (with-doc (x %) %)) (group-decls trees)))

(defn- method-decl
  "(:method mods [tparams]? type? name [params]? [] ... (throws ...)? default? body?): empty
  vectors after the parameters are legacy array brackets after them (int f()[])."
  [^JCTree$JCMethodDecl m]
  (let [ctor? (= "<init>" (str (.-name m)))
        compact? (not (zero? (bit-and (.-flags (.-mods m)) Flags/COMPACT_RECORD_CONSTRUCTOR)))
        trailing (take-while #(and (instance? JCTree$JCArrayTypeTree %) (> (.-pos ^JCTree %) (.-pos m)))
                             (iterate #(when (instance? JCTree$JCArrayTypeTree %) (.-elemtype ^JCTree$JCArrayTypeTree %))
                                      (.-restype m)))
        restype (if (seq trailing) (.-elemtype ^JCTree$JCArrayTypeTree (last trailing)) (.-restype m))]
    (lst nil (tok ":method")
         (let [tp (some-> (first (.-typarams m)) start-pos)
               [anns late] (if tp
                             (split-with #(< (.-pos ^JCTree %) tp) (.-annotations (.-mods m)))
                             [(.-annotations (.-mods m)) nil])]
           [(mods-forms (.-mods m) (.-pos m) Flags/COMPACT_RECORD_CONSTRUCTOR anns)
            (tparams (.-typarams m))
            (map x late)])
         (some-> restype typ)
         (tok (if ctor? *class-name* (str (.-name m))) (ln m))
         (when-not compact?
           (vect nil (interpose comma (map param (cond->> (.-params m)
                                                     (.-recvparam m) (cons (.-recvparam m)))))))
         (map #(vect (ln %)) trailing)
         (when (seq (.-thrown m)) (lst nil (tok "throws") (map typ (.-thrown m))))
         (when-let [d (.-defaultValue m)] [(tok "default") (x d)])
         (some-> (.-body m) block))))

(def kind-flags
  (bit-or Flags/INTERFACE Flags/ANNOTATION Flags/ENUM Flags/RECORD))

(defn- flag? [^JCTree$JCModifiers m f]
  (not (zero? (bit-and (.-flags m) f))))

(defn- enum-constant
  "NAME, or (annotations... NAME args... (class members...)). javac puts the creation at the
  token after the name; a `.` marker gives its line when that is not the name's."
  [^JCTree$JCVariableDecl v]
  (let [^JCTree$JCNewClass init (.-init v)
        anns (map x (.-annotations (.-mods v)))
        l (ln (.-clazz init))
        name (tok (str (.-name v)) l)]
    (if (and (empty? anns) (empty? (.-args init)) (nil? (.-def init)))
      name
      (lst nil anns name
           (when (not= l (ln init)) (tok "." (ln init)))
           (args (.-args init))
           (some-> (.-def init) class-body (assoc :l nil))))))

(defn- component [^JCTree$JCVariableDecl v]
  [(map x (.-annotations (.-mods v)))
   (var-type v) (var-name v)])

(defn class-decl [^JCTree$JCClassDecl c]
  (let [m (.-mods c)
        record? (flag? m Flags/RECORD)
        interface? (flag? m Flags/INTERFACE)
        head (cond (flag? m Flags/ANNOTATION) ":annotation"
                   interface? "interface"
                   (flag? m Flags/ENUM) "enum"
                   record? ":record"
                   :else "class")
        component? (fn [d] (and record? (instance? JCTree$JCVariableDecl d)
                                (flag? (.-mods ^JCTree$JCVariableDecl d) Flags/RECORD)))
        enum-const? (fn [d] (and (instance? JCTree$JCVariableDecl d)
                                 (flag? (.-mods ^JCTree$JCVariableDecl d) Flags/ENUM)))]
    (binding [*class-name* (str (.-name c))]
      (lst nil (tok head)
           (mods-forms m (.-pos c) kind-flags)
           (tok (str (.-name c)) (ln c))
           (tparams (.-typarams c))
           (when record?
             (vect nil (interpose comma (map component (filter component? (.-defs c))))))
           (when-let [e (.-extending c)] (lst nil (tok "extends") (typ e)))
           (when (seq (.-implementing c))
             (lst nil (tok (if interface? "extends" "implements")) (map typ (.-implementing c))))
           (when (seq (.-permitting c))
             (lst nil (tok ":permits") (map typ (.-permitting c))))
           (let [[consts members] (split-with enum-const? (remove component? (.-defs c)))]
             [(map #(with-doc (enum-constant %) %) consts) (xs members)])))))

;;; Statements

(defn block
  "(do stmts...) for block `b`. By default the `}` is on the line after the last statement,
  or on the opening line for a block that opens and ends on one line. Otherwise the closing
  paren is detached (whitespace before it) and sits on the line of the `}`."
  [^JCTree$JCBlock b]
  (let [stats (xs (.-stats b))
        n (lst (ln b) (tok "do") (when (not (zero? (bit-and (.-flags b) Flags/STATIC))) (tok "static"))
               stats)]
    (assoc n :brace (ln (.-bracePos b)))))

(defn- case-form [^JCTree$JCCase c]
  (let [labels (.-labels c)
        rule? (= (.-caseKind c) CaseTree$CaseKind/RULE)
        default-only? (and (= 1 (count labels)) (instance? JCTree$JCDefaultCaseLabel (first labels))
                           (nil? (.-guard c)))]
    (lst (ln c)
         (if default-only?
           (tok "default")
           [(tok "case") (vect nil (map x labels))
            (when-let [g (.-guard c)] (lst nil (tok ":when") (x g)))])
         (if rule?
           [(tok "->") (x (.-body c))]
           (map x (.-stats c))))))

(defn- switch-form [t selector cases brace-pos]
  (assoc (lst (ln t) (tok "switch") (condition selector (ln t)) (map case-form cases))
         :brace (ln brace-pos)))

(defn- lambda [^JCTree$JCLambda t]
  (let [ps (.-params t)]
    (lst nil (tok "->" (ln t))
         (if (and (= (.-paramKind t) JCTree$JCLambda$ParameterKind/IMPLICIT)
                  (not-any? #(= (.-declKind ^JCTree$JCVariableDecl %) JCTree$JCVariableDecl$DeclKind/VAR) ps))
           (if (= 1 (count ps))
             (var-name (first ps))
             (lst nil (map var-name ps)))
           (params ps))
         (let [b (.-body t)]
           (if (instance? JCTree$JCBlock b) (block b) (x b))))))

(defn- new-array
  "(new (elem [size] ... [] ...) [elems...]?) for an array creation, or [elems...] for a bare
  initializer. The brackets carry their type annotations as in a type."
  [^JCTree$JCNewArray t]
  (if-let [e (.-elemtype t)]
    (let [before (if (.-elems t)
                   [[(.-annotations t) nil]]
                   (map vector (concat (.-dimAnnotations t) (repeat nil)) (.-dims t)))]
      (lst (ln t) (tok "new")
           (array-type e before false)
           (when-let [es (.-elems t)] (vect nil (map x es)))))
    (vect (ln t) (map x (.-elems t)))))

(defn- infix
  "Operand nodes `nodes` with infix markers: before each operand after the first goes the
  operator `op`, anchored at its line from `lines`, when that line is not the one where the
  previous operand ends (the default)."
  [op lines nodes]
  (cons (first nodes)
        (mapcat (fn [l prev n]
                  (if (and l (not= l (lay/last-line prev))) [(tok op l) n] [n]))
                lines nodes (rest nodes))))

(defn- binary [^JCTree$JCBinary t]
  (let [op (binary-ops (.getTag t))
        [operands lines] (if (nary-ops op)
                           (loop [t t acc () ls ()]
                             (let [l (.-lhs ^JCTree$JCBinary t)
                                   acc (cons (.-rhs ^JCTree$JCBinary t) acc)
                                   ls (cons (ln t) ls)]
                               (if (and (instance? JCTree$JCBinary l) (= (.getTag ^JCTree l) (.getTag ^JCTree t)))
                                 (recur l acc ls)
                                 [(cons l acc) ls])))
                           [[(.-lhs t) (.-rhs t)] [(ln t)]])]
    (lst nil (tok op) (infix op lines (map x operands)))))

(defn- array-access
  "(:aget a i j ...): a `.` marker before an index gives the line of its `[` when that is not
  where the previous item ends."
  [^JCTree$JCArrayAccess t]
  (let [[a & is] (loop [t t acc ()]
                   (if (instance? JCTree$JCArrayAccess t)
                     (recur (.-indexed ^JCTree$JCArrayAccess t) (cons t acc))
                     (cons t acc)))
        nodes (cons (x a) (map #(x (.-index ^JCTree$JCArrayAccess %)) is))]
    (lst nil (tok ":aget") (infix "." (map ln is) nodes))))

(defn x
  "The layout node(s) for tree `t`."
  [t]
  (condp instance? t
    JCTree$JCIdent (tok (str (.-name ^JCTree$JCIdent t)) (ln t))
    JCTree$JCFieldAccess (if-let [c (one-line-chain t)] (chain-token c) (chain-form t))
    JCTree$JCLiteral (literal t)
    JCTree$JCParens (assoc (lst (ln t) (tok ":paren") (x (.-expr ^JCTree$JCParens t))) :paren true)
    JCTree$JCPrimitiveTypeTree (tok (str/lower-case (str (.-typetag ^JCTree$JCPrimitiveTypeTree t))) (ln t))
    JCTree$JCArrayTypeTree (array-type t)
    JCTree$JCTypeApply (let [c (typ (.-clazz ^JCTree$JCTypeApply t))]
                         (lst nil c (when (not= (ln t) (lay/last-line c)) (tok "." (ln t)))
                              (map typ (.-arguments ^JCTree$JCTypeApply t))))
    JCTree$JCWildcard (let [^JCTree$JCWildcard w t
                            k (.-kind (.-kind w))]
                        (if (= k BoundKind/UNBOUND)
                          (tok "?" (ln t))
                          (lst nil (tok "?" (ln t)) (tok (if (= k BoundKind/EXTENDS) "extends" "super"))
                               (typ (.-inner w)))))
    JCTree$JCTypeUnion (lst nil (tok "|") (map typ (.-alternatives ^JCTree$JCTypeUnion t)))
    JCTree$JCTypeIntersection (lst nil (tok "&") (map typ (.-bounds ^JCTree$JCTypeIntersection t)))
    JCTree$JCAnnotatedType (let [u (.-underlyingType ^JCTree$JCAnnotatedType t)]
                             (cond
                               (instance? JCTree$JCArrayTypeTree u) (array-type t)
                               (instance? JCTree$JCFieldAccess u) (chain-form t)
                               :else (lst nil (tok ":annotated") (map x (.-annotations ^JCTree$JCAnnotatedType t))
                                          (typ u))))
    JCTree$JCAnnotation (lst (ln t) (tok ":ann") (typ (.-annotationType ^JCTree$JCAnnotation t))
                             (args (.-args ^JCTree$JCAnnotation t)))

    JCTree$JCMethodInvocation
    (let [^JCTree$JCMethodInvocation m t
          meth (.-meth m)]
      (if (simple-call? m)
        (lst nil (chain-token (name-chain meth)) (targs (.-typeargs m))
             (when (not= (ln meth) (ln m)) (tok "." (ln m)))
             (args (.-args m)))
        (chain-form t)))
    JCTree$JCNewClass (if (.-encl ^JCTree$JCNewClass t) (chain-form t) (new-class t))
    JCTree$JCNewArray (new-array t)
    JCTree$JCLambda (lambda t)
    JCTree$JCMemberReference
    (let [^JCTree$JCMemberReference r t
          n (str (.-name r))]
      (lst nil (tok ":ref")
           (if (instance? JCTree$JCTypeApply (.-expr r)) (lst nil (tok ":type") (typ (.-expr r))) (typ (.-expr r)))
           (targs (.-typeargs r))
           (tok (if (= n "<init>") "new" n))))
    JCTree$JCAssign (lst nil (tok "=") (infix "=" [(ln t)] [(x (.-lhs ^JCTree$JCAssign t)) (x (.-rhs ^JCTree$JCAssign t))]))
    JCTree$JCAssignOp (let [op (assign-ops (.getTag ^JCTree t))]
                        (lst nil (tok op)
                             (infix op [(ln t)] [(x (.-lhs ^JCTree$JCAssignOp t)) (x (.-rhs ^JCTree$JCAssignOp t))])))
    JCTree$JCUnary (let [op (unary-ops (.getTag ^JCTree t))
                         arg (x (.-arg ^JCTree$JCUnary t))]
                     (if (str/starts-with? op ":")
                       (lst nil (tok op) arg (when (not= (ln t) (lay/last-line arg)) (tok "." (ln t))))
                       (lst (ln t) (tok op) arg)))
    JCTree$JCBinary (binary t)
    JCTree$JCTypeCast (lst (ln t) (tok ":cast") (typ (.-clazz ^JCTree$JCTypeCast t))
                           (x (.-expr ^JCTree$JCTypeCast t)))
    JCTree$JCInstanceOf (lst nil (tok "instanceof")
                             (infix "instanceof" [(ln t)] [(x (.-expr ^JCTree$JCInstanceOf t))
                                                           (typ (.-pattern ^JCTree$JCInstanceOf t))]))
    JCTree$JCConditional (lst nil (tok "?")
                              (infix "?" [(ln t)] [(x (.-cond ^JCTree$JCConditional t))
                                                   (x (.-truepart ^JCTree$JCConditional t))])
                              (x (.-falsepart ^JCTree$JCConditional t)))
    JCTree$JCArrayAccess (array-access t)
    JCTree$JCSwitchExpression (let [^JCTree$JCSwitchExpression s t]
                                (switch-form s (.-selector s) (.-cases s) (.-bracePos s)))

    JCTree$JCAnyPattern (tok "_" (ln t))
    JCTree$JCBindingPattern (let [v (.-var ^JCTree$JCBindingPattern t)] (lst nil (tok ":var") (param v)))
    JCTree$JCRecordPattern (lst nil (tok ":record") (if-let [d (.-deconstructor ^JCTree$JCRecordPattern t)] (typ d) (tok "var"))
                                (map x (.-nested ^JCTree$JCRecordPattern t)))
    JCTree$JCConstantCaseLabel (x (.-expr ^JCTree$JCConstantCaseLabel t))
    JCTree$JCPatternCaseLabel (x (.-pat ^JCTree$JCPatternCaseLabel t))
    JCTree$JCDefaultCaseLabel (tok "default" (ln t))

    JCTree$JCBlock (block t)
    JCTree$JCSkip (lst (ln t))
    JCTree$JCVariableDecl (var-decl [t])
    JCTree$JCClassDecl (class-decl t)
    JCTree$JCMethodDecl (method-decl t)
    JCTree$JCExpressionStatement (x (.-expr ^JCTree$JCExpressionStatement t))
    JCTree$JCIf (let [^JCTree$JCIf s t]
                  (lst (ln t) (tok "if") (condition (.-cond s) (ln t)) (x (.-thenpart s))
                       (some-> (.-elsepart s) x)))
    JCTree$JCWhileLoop (lst (ln t) (tok "while") (condition (.-cond ^JCTree$JCWhileLoop t) (ln t))
                            (x (.-body ^JCTree$JCWhileLoop t)))
    JCTree$JCDoWhileLoop (lst (ln t) (tok ":do-while") (x (.-body ^JCTree$JCDoWhileLoop t))
                              (let [b (.-body ^JCTree$JCDoWhileLoop t)]
                                (condition (.-cond ^JCTree$JCDoWhileLoop t)
                                           (if (instance? JCTree$JCBlock b)
                                             (ln (.-bracePos ^JCTree$JCBlock b))
                                             (lay/last-line (x b))))))
    JCTree$JCForLoop (let [^JCTree$JCForLoop f t]
                       (lst (ln t) (tok "for") (vect nil (xs (.-init f)))
                            (some-> (.-cond f) x) (vect nil (map x (.-step f))) (x (.-body f))))
    JCTree$JCEnhancedForLoop (let [^JCTree$JCEnhancedForLoop f t]
                               (lst (ln t) (tok "for") (vect nil (param (.-var f)) (x (.-expr f)))
                                    (x (.-body f))))
    JCTree$JCLabeledStatement (lst (ln t) (tok ":label") (tok (str (.-label ^JCTree$JCLabeledStatement t)))
                                   (x (.-body ^JCTree$JCLabeledStatement t)))
    JCTree$JCSwitch (let [^JCTree$JCSwitch s t]
                      (switch-form s (.-selector s) (.-cases s) (.-bracePos s)))
    JCTree$JCSynchronized (lst (ln t) (tok "synchronized") (condition (.-lock ^JCTree$JCSynchronized t) (ln t))
                               (x (.-body ^JCTree$JCSynchronized t)))
    JCTree$JCTry (let [^JCTree$JCTry s t]
                   (lst (ln t) (tok "try")
                        (when (seq (.-resources s)) (vect nil (map x (.-resources s))))
                        (x (.-body s))
                        (for [^JCTree$JCCatch c (.-catchers s)]
                          (lst (ln c) (tok "catch") (param (.-param c)) (x (.-body c))))
                        (when-let [f (.-finalizer s)] (lst nil (tok "finally") (x f)))))
    JCTree$JCBreak (lst (ln t) (tok "break") (some-> (.-label ^JCTree$JCBreak t) str tok))
    JCTree$JCContinue (lst (ln t) (tok "continue") (some-> (.-label ^JCTree$JCContinue t) str tok))
    JCTree$JCYield (lst (ln t) (tok ":yield") (x (.-value ^JCTree$JCYield t)))
    JCTree$JCReturn (lst (ln t) (tok "return") (some-> (.-expr ^JCTree$JCReturn t) x))
    JCTree$JCThrow (lst (ln t) (tok "throw") (x (.-expr ^JCTree$JCThrow t)))
    JCTree$JCAssert (lst (ln t) (tok "assert") (x (.-cond ^JCTree$JCAssert t))
                         (some-> (.-detail ^JCTree$JCAssert t) x))

    JCTree$JCPackageDecl (lst (when (empty? (.-annotations ^JCTree$JCPackageDecl t)) (ln t))
                              (tok "package") (map x (.-annotations ^JCTree$JCPackageDecl t))
                              (x (.-pid ^JCTree$JCPackageDecl t)))
    JCTree$JCImport (lst (ln t) (tok "import") (when (.-staticImport ^JCTree$JCImport t) (tok "static"))
                         (x (.-qualid ^JCTree$JCImport t)))
    JCTree$JCModuleImport (lst (ln t) (tok "import") (tok ":module") (x (.-module ^JCTree$JCModuleImport t)))
    JCTree$JCModuleDecl (let [^JCTree$JCModuleDecl d t]
                          (lst nil (tok ":module") (mods-forms (.-mods d) (.-pos d))
                               (when (= (.getModuleType d) ModuleTree$ModuleKind/OPEN) (tok ":open"))
                               (x (.-qualId d)) (map x (.-directives d))))
    JCTree$JCRequires (let [^JCTree$JCRequires r t]
                        (lst (ln t) (tok ":requires") (when (.-isTransitive r) (tok ":transitive"))
                             (when (.-isStaticPhase r) (tok "static")) (x (.-moduleName r))))
    JCTree$JCExports (let [^JCTree$JCExports e t]
                       (lst (ln t) (tok ":exports") (x (.-qualid e))
                            (when (seq (.-moduleNames e)) [(tok ":to") (map x (.-moduleNames e))])))
    JCTree$JCOpens (let [^JCTree$JCOpens e t]
                     (lst (ln t) (tok ":opens") (x (.-qualid e))
                          (when (seq (.-moduleNames e)) [(tok ":to") (map x (.-moduleNames e))])))
    JCTree$JCUses (lst (ln t) (tok ":uses") (x (.-qualid ^JCTree$JCUses t)))
    JCTree$JCProvides (let [^JCTree$JCProvides p t]
                        (lst (ln t) (tok ":provides") (x (.-serviceName p))
                             (tok ":with") (map x (.-implNames p))))
    (throw (ex-info (str "unsupported tree: " (.getSimpleName (class t))) {:line (ln t)}))))

;;; Comments and layout

(defn inline-doc
  "When a one-line doc comment starts at `i` in `text` and code follows it on its line,
  [position-of-that-code comment-text position-of-the-comment], else nil."
  [^String text i]
  (when (.startsWith text "/**" i)
    (let [e (.indexOf text "*/" (+ i 3))]
      (when (and (pos? e) (neg? (.indexOf (subs text i e) "\n")))
        (let [j (loop [j (+ e 2)] (if (and (< j (count text)) (#{\space \tab} (.charAt text j))) (recur (inc j)) j))]
          (when (and (< j (count text)) (not (#{\newline \return \/} (.charAt text j))))
            [j (subs text i (+ e 2)) i]))))))

(defn comment-pieces
  "The comments of Java `text`, as {line text}: each line's comment text, verbatim, except
  that a line starting with // has it written as ; (so the line prints as ;;). The doc
  comments at the positions in `inline` are left out (they are printed inline)."
  [^String text inline]
  (let [n (count text)
        pieces (volatile! (sorted-map))
        line (volatile! 1)
        add (fn [l s] (vswap! pieces update l #(if % (str % " " s) s)))]
    (loop [i 0 state :code]
      (when (< i n)
        (let [c (.charAt text i)
              nx (when (< (inc i) n) (.charAt text (inc i)))]
          (when (= c \newline) (vswap! line inc))
          (case state
            :code (cond
                    (and (= c \/) (= nx \/))
                    (let [e (let [e (.indexOf text "\n" i)] (if (neg? e) n e))]
                      (add @line (str/trimr (subs text i e)))
                      (recur e :code))
                    (and (= c \/) (= nx \*) (contains? inline i))
                    (let [e (+ 2 (.indexOf text "*/" (+ i 2)))]
                      (recur e :code))
                    (and (= c \/) (= nx \*))
                    (let [e (let [e (.indexOf text "*/" (+ i 2))] (if (neg? e) n (+ e 2)))
                          parts (str/split (subs text i e) #"\n" -1)]
                      (doseq [[k p] (map-indexed vector parts)]
                        (let [p (str/trimr (if (pos? k) (str/replace p #"^\s+" "") p))]
                          (when (seq p) (add (+ @line k) p))))
                      (vswap! line + (dec (count parts)))
                      (recur e :code))
                    (and (= c \") (= nx \") (< (+ i 2) n) (= \" (.charAt text (+ i 2))))
                    (recur (+ i 3) :text-block)
                    (= c \") (recur (inc i) :string)
                    (= c \') (recur (inc i) :char)
                    :else (recur (inc i) :code))
            :string (cond (= c \\) (recur (+ i 2) :string)
                          (= c \") (recur (inc i) :code)
                          :else (recur (inc i) :string))
            :char (cond (= c \\) (recur (+ i 2) :char)
                        (= c \') (recur (inc i) :code)
                        :else (recur (inc i) :char))
            :text-block (cond (= c \\) (do (when (= nx \newline) (vswap! line inc)) (recur (+ i 2) :text-block))
                              (and (= c \") (= nx \") (< (+ i 2) n) (= \" (.charAt text (+ i 2))))
                              (recur (+ i 3) :code)
                              :else (recur (inc i) :text-block))))))
    (into (sorted-map)
          (for [[l c] @pieces]
            [l (if (str/starts-with? c "//") (str ";" (subs c 2)) c)]))))

(defn- implicit-parens
  "Drop the (:paren x) wrappers whose parentheses Java needs anyway (prec/wrap?): printing
  Java puts those back. A wrapper stays when the `(` is not on the line where x starts.
  `root?` is as for prec/child-contexts."
  ([n] (implicit-parens n true))
  ([n root?]
   (cond
     (:V n) (assoc n :V (mapv implicit-parens (:V n)))
     (:L n)
     (let [n (loop [n n]
               (let [ctxs (prec/child-contexts n root?)
                     i (first (keep-indexed (fn [i c]
                                              (when-let [ctx (get ctxs i)]
                                                (when (and (:paren c)
                                                           (prec/wrap? (second (:L c)) ctx)
                                                           (= (:l c) (lay/first-line (second (:L c)))))
                                                  i)))
                                            (:L n)))]
                 (if i
                   (recur (assoc-in n [:L i] (second (:L (get-in n [:L i])))))
                   n)))
           ctxs (prec/child-contexts n root?)]
       (assoc n :L (vec (map-indexed (fn [i c]
                                       (let [ctx (get ctxs i)]
                                         (implicit-parens c (or (nil? ctx) (:paren c)
                                                                (prec/wrap? c ctx)
                                                                (not (:in-binary ctx))))))
                                     (:L n)))))
     :else n)))

(defn- brace-closings
  "Detach each block's and switch's closing paren and put it on its `}` line when the default
  rule (see `block`) would place the `}` elsewhere; report the cases the layout cannot express."
  [n report]
  (let [n (if-let [cs (lay/children n)]
            (assoc n (if (:L n) :L :V) (mapv #(brace-closings % report) cs))
            n)]
    (if-let [b (:brace n)]
      (let [open (lay/first-line n)
            last (or (some lay/last-line (rseq (subvec (:L n) 1))) open)
            dflt (if (= open last) open (inc last))]
        (cond
          (= b dflt) (dissoc n :brace)
          (>= b last) (-> n (dissoc :brace) (assoc :cl b :detach true))
          :else (do (report {:token "}" :line b :default dflt}) (dissoc n :brace))))
      n)))

(defn- all-indexes [^String s ^String sub]
  (take-while #(>= % 0) (iterate #(.indexOf s sub (int (inc %))) (.indexOf s sub))))

(defn transcribe
  "Transcribe Java source `text` (named `path`) to javalisp text.
  Returns {:text .. :violations [..] :errors [..]}."
  [path text]
  (let [{:keys [cu errors]} (javac/parse path text)]
    (if (seq errors)
      {:errors errors}
      (binding [*cu* cu *text* text
                *inline-docs* (into {} (keep #(some-> (inline-doc text %) pop)) (all-indexes text "/**"))
                *used-docs* (volatile! #{})]
        (let [defs (mapcat (fn [d]
                             (if (and (instance? JCTree$JCClassDecl d)
                                      (flag? (.-mods ^JCTree$JCClassDecl d) Flags/IMPLICIT_CLASS))
                               (xs (.-defs ^JCTree$JCClassDecl d))
                               [(with-doc (x d) d)]))
                           (.-defs cu))
              brace-violations (transient [])
              defs (mapv #(brace-closings (implicit-parens %) (fn [v] (conj! brace-violations v))) defs)
              java-lines (str/split text #"\n" -1)
              indent (fn [l] (re-find #"^\s*" (get java-lines (dec l) "")))
              {out :text vs :violations} (lay/render defs {:indent indent
                                                           :comments (comment-pieces
                                                                      text
                                                                      (into #{} (keep #(let [[j _ i] (inline-doc text %)]
                                                                                         (when (@*used-docs* j) i)))
                                                                            (all-indexes text "/**")))
                                                           :lines (count (re-seq #"\n" text))})]
          {:text out :violations (into vs (persistent! brace-violations))})))))
