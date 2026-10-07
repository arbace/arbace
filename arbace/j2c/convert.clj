(ns arbace.j2c.convert
  "Java -> class forms: converts javac's attributed compilation units into the forms of
  doc/classes/SPEC.md, making explicit what §7 lists."
  (:require [arbace.j2c.forms :as f :refer [m+ tag member cref]]
            [arbace.j2c.jtypes :as jt :refer [erasure prim? ref-type? same? subtype? tag-name]]
            [arbace.j2c.resolve :as res]
            [clojure.string :as str])
  (:import [arbace.j2c.forms CRef]
           [com.sun.tools.javac.tree JCTree JCTree$JCCompilationUnit JCTree$JCClassDecl
            JCTree$JCMethodDecl JCTree$JCVariableDecl JCTree$JCBlock JCTree$JCExpressionStatement
            JCTree$JCIf JCTree$JCWhileLoop JCTree$JCDoWhileLoop JCTree$JCForLoop
            JCTree$JCEnhancedForLoop JCTree$JCLabeledStatement JCTree$JCSwitch JCTree$JCCase
            JCTree$JCSwitchExpression JCTree$JCSynchronized JCTree$JCTry JCTree$JCCatch
            JCTree$JCConditional JCTree$JCBreak JCTree$JCYield JCTree$JCContinue JCTree$JCReturn
            JCTree$JCThrow JCTree$JCAssert JCTree$JCMethodInvocation JCTree$JCNewClass
            JCTree$JCNewArray JCTree$JCLambda JCTree$JCParens JCTree$JCAssign JCTree$JCAssignOp
            JCTree$JCUnary JCTree$JCBinary JCTree$JCTypeCast JCTree$JCInstanceOf
            JCTree$JCAnyPattern JCTree$JCBindingPattern JCTree$JCDefaultCaseLabel
            JCTree$JCConstantCaseLabel JCTree$JCPatternCaseLabel JCTree$JCRecordPattern
            JCTree$JCArrayAccess JCTree$JCFieldAccess JCTree$JCMemberReference JCTree$JCIdent
            JCTree$JCLiteral JCTree$JCAnnotation JCTree$JCModifiers JCTree$JCSkip
            JCTree$JCPackageDecl JCTree$JCImport JCTree$JCModuleDecl JCTree$JCRequires
            JCTree$JCExports JCTree$JCOpens JCTree$JCProvides JCTree$JCUses JCTree$JCStatement
            JCTree$JCExpression JCTree$Tag JCTree$JCTypeParameter JCTree$JCAnnotatedType
            JCTree$JCLambda$ParameterKind TreeInfo JCTree$JCMemberReference$ReferenceKind
            JCTree$JCPattern JCTree$JCCaseLabel]
           [com.sun.tools.javac.code Type Type$ArrayType Type$ClassType Type$WildcardType
            Type$TypeVar Type$MethodType Type$IntersectionClassType Types Symtab Symbol
            Symbol$ClassSymbol Symbol$MethodSymbol Symbol$VarSymbol Symbol$TypeSymbol
            Symbol$PackageSymbol Flags Attribute Attribute$Constant Attribute$Class
            Attribute$Enum Attribute$Array Attribute$Compound Attribute$Error BoundKind
            TypeTag Kinds$Kind Scope$LookupKind Type$CapturedType Directive$RequiresDirective
            Directive$ExportsDirective Directive$OpensDirective Directive$ProvidesDirective
            Directive$UsesDirective]
           [com.sun.source.tree CaseTree$CaseKind Tree$Kind]
           [java.lang.annotation RetentionPolicy]))

;;; ------------------------------------------------------------------------------------------
;;; Statistics (for the coverage report)

(def ^:dynamic *stats* nil)

(defn note! [k] (when *stats* (swap! *stats* update k (fnil inc 0))))

(defn- kind-of [^JCTree t] (try (str (.getKind t)) (catch AssertionError _ (.getSimpleName (class t)))))

;;; ------------------------------------------------------------------------------------------
;;; Environment

(def ^:dynamic *text* "")

(defn- gensym! [env base]
  (let [n (swap! (:counter env) inc)]
    (symbol (str base "-" n))))

(def new-heads
  "Names of the new forms and operators (spec §5.4, §9.5), qualified with arbace.core/ when a
  local or field shadows them."
  #{"label" "break" "continue" "return" "switch" "lambda" "method-ref" "java-str" "java-assert"
    "for-each" "with-resources" "if-instance" "when-instance" "anon" "letclass"})

(defn h
  "The head symbol `s`, qualified when a local or field of that name is in scope."
  [env s]
  (let [n (name s)]
    (if (or (contains? (:names env) n) (contains? (:fields env) n))
      (symbol (if (or (new-heads n) (str/ends-with? n "-float") (str/ends-with? n "-int")
                      (#{"unchecked-divide" "unchecked-remainder"} n))
                "arbace.core" "clojure.core")
              n)
      s)))

(defn- shadow-set [env]
  (into (set (keys (:scope env))) (concat (:names env) (:fields env))))

(defn class-ref
  "The form naming class symbol `c` with `dims` array dimensions at this site."
  ([env c] (class-ref env c 0))
  ([env ^Symbol$ClassSymbol c dims]
   (let [simple (str (.getSimpleName c))
         in-scope (get (:scope env) simple)]
     (cond
       (and (identical? in-scope c) (not (str/blank? simple)))
       (if (pos? dims) (symbol simple (str dims)) (symbol simple))
       (and (jt/local-class? c) (not (jt/anonymous? c))) ;; a local class not in scope by name: should not happen
       (do (note! :warn/local-class-out-of-scope)
           (if (pos? dims) (symbol simple (str dims)) (symbol simple)))
       :else (do (when (jt/anonymous? c) (note! :warn/anonymous-class-named))
                 (f/->CRef (jt/binary-name c) dims nil (shadow-set env)))))))

;;; ------------------------------------------------------------------------------------------
;;; Types as forms (§4.3)

(declare type-form annotation-items)

(defn- array-dims [^Type t]
  (loop [t t n 0]
    (if (instance? Type$ArrayType t) (recur (.elemtype ^Type$ArrayType t) (inc n)) [t n])))

(declare type-ann-items)

(def ^:dynamic *skip-anns*
  "Binary names of the TYPE_USE annotations in a declaration's modifiers: javac also puts them
  on the declared type's element type, which the compiler derives from the declaration (§4.4)."
  #{})

(defn- annotate [form items]
  (if (seq items) (apply m+ form items) form))

(defn- type-form*
  [env ^Type t elem?]
  (let [tg (tag-name t)
        anns (type-ann-items env t (and elem? (not (instance? Type$ArrayType t))))]
    (annotate
     (cond
       (#{"BOOLEAN" "BYTE" "SHORT" "INT" "LONG" "CHAR" "FLOAT" "DOUBLE" "VOID"} tg) (jt/prim-sym t)
       (instance? Type$ArrayType t)
       (let [ef (type-form* env (.elemtype ^Type$ArrayType t) elem?)]
         (cond
           (seq (f/items ef)) (list 'array ef)
           (instance? CRef ef) (update ef :dims inc)
           (and (symbol? ef) (namespace ef)) (symbol (namespace ef) (str (inc (parse-long (name ef)))))
           (symbol? ef) (symbol (name ef) "1")
           :else (list 'array ef)))
       (instance? Type$CapturedType t) (type-form* env (erasure t) elem?)
       (instance? Type$TypeVar t) (symbol (str (.getSimpleName (.tsym t))))
       (instance? Type$WildcardType t)
       (let [w ^Type$WildcardType t]
         (condp = (.kind w)
           BoundKind/UNBOUND '?
           BoundKind/EXTENDS (list '? 'extends (type-form* env (.type w) false))
           BoundKind/SUPER (list '? 'super (type-form* env (.type w) false))))
       (jt/components t) (apply list '& (map #(type-form* env % false) (jt/components t)))
       (instance? Type$ClassType t)
       (let [ct ^Type$ClassType t
             c (.tsym ct)
             args (seq (.getTypeArguments ct))
             outer (.getEnclosingType ct)
             base (class-ref env c)
             self (if args (apply list base (map #(type-form* env % false) args)) base)]
         (if (and (instance? Type$ClassType outer) (seq (.allparams ^Type outer))
                  (not (jt/static? c)) (not (jt/local-class? c))
                  (not (and (identical? (get (:scope env) (str (.getSimpleName c))) c)
                            (.isSameType jt/*types* outer (.type (.tsym outer))))))
           (list '.. (type-form* env outer false)
                 (annotate (if args
                             (apply list (symbol (str (.getSimpleName c))) (map #(type-form* env % false) args))
                             (symbol (str (.getSimpleName c))))
                           anns))
           self))
       :else (do (note! :warn/odd-type) (class-ref env (.tsym (jt/object-type)))))
     ;; the annotations of an inner type of a generic outer go on its element of (.. ...)
     (when-not (and (instance? Type$ClassType t) (instance? Type$ClassType (.getEnclosingType ^Type t))
                    (seq (.allparams ^Type (.getEnclosingType ^Type t)))
                    (not (jt/static? (.tsym t))) (not (jt/local-class? (.tsym t)))
                    (not (and (identical? (get (:scope env) (str (.getSimpleName (.tsym t)))) (.tsym t))
                              (.isSameType jt/*types* (.getEnclosingType ^Type t) (.type (.tsym (.getEnclosingType ^Type t)))))))
       anns))))

(defn type-form
  "The declaration form of type `t` (generic where it is), with its type annotations (§4.4)."
  [env ^Type t]
  (type-form* env t true))

(defn erased-form
  "The form of the erasure of `t`: a primitive symbol, a class or an array class."
  [env t]
  (type-form env (erasure t)))

(defn type-annotated?
  "Does type `t` carry type annotations anywhere?"
  [^Type t]
  (boolean
   (and t
        (or (seq (.getAnnotationMirrors t))
            (and (instance? Type$ArrayType t) (type-annotated? (.elemtype ^Type$ArrayType t)))
            (and (instance? Type$WildcardType t) (type-annotated? (.type ^Type$WildcardType t)))
            (and (instance? Type$ClassType t)
                 (or (some type-annotated? (.getTypeArguments ^Type t))
                     (let [o (.getEnclosingType ^Type t)] (and (instance? Type$ClassType o) (type-annotated? o)))))))))

(defn code-type-form
  "The form of type `t` in code (cast, instance?, new, catch): erased, or with its type
  annotations where it has some (the compiler erases it)."
  [env ^Type t]
  (if (type-annotated? t) (type-form env t) (erased-form env t)))

(defn- generic? [^Type t]
  (cond
    (instance? Type$ArrayType t) (generic? (.elemtype ^Type$ArrayType t))
    (instance? Type$TypeVar t) true
    (instance? Type$ClassType t) (or (seq (.getTypeArguments ^Type t))
                                     (and (seq (.allparams ^Type t)) true))
    :else false))

(defn decl-tag
  "The tag item for a declaration of type `t`, or nil for Object when `omit-object?` (unless
  annotated)."
  [env ^Type t omit-object?]
  (let [f (type-form env t)]
    (when-not (and omit-object? (jt/object? t) (not (instance? Type$TypeVar t)) (empty? (f/items f)))
      [:tag f])))

(defn- mods-type-anns
  "*skip-anns* for a declaration with modifiers `mods`."
  [^JCTree$JCModifiers mods]
  (set (for [^JCTree$JCAnnotation a (some-> mods .annotations)
             :let [c (.attribute a)]
             :when c]
         (jt/binary-name (.tsym (.type ^Attribute$Compound c))))))

;;; ------------------------------------------------------------------------------------------
;;; Annotations (§4.4)

(defn- retention [^Attribute$Compound a]
  (.getRetention jt/*types* a))

(defn- attr-value [env ^Attribute a ^Type elem-type]
  (cond
    (instance? Attribute$Constant a)
    (let [v (.getValue a) tt (.type a)]
      (case (tag-name tt)
        "CHAR" (char (int v))
        "BOOLEAN" (if (number? v) (not (zero? (long v))) v)
        "FLOAT" (float v)
        "BYTE" (list 'unchecked-byte (long v))
        "SHORT" (list 'unchecked-short (long v))
        v))
    (instance? Attribute$Class a) (let [t (.getValue ^Attribute$Class a)]
                                     (if (prim? t)
                                       (member (cref (str "java.lang." (str/capitalize (str (jt/prim-sym t))))) "TYPE")
                                       (erased-form env t)))
    (instance? Attribute$Enum a) (member (class-ref env (.tsym (.type a))) (str (.value ^Attribute$Enum a)))
    (instance? Attribute$Array a) (mapv #(attr-value env % nil) (.values ^Attribute$Array a))
    (instance? Attribute$Compound a)
    (let [c ^Attribute$Compound a
          vals (.values c)]
      (list (class-ref env (.tsym (.type c)))
            (into {} (for [p vals] [(keyword (str (.name ^Symbol (.fst p)))) (attr-value env (.snd p) nil)]))))
    :else (do (note! :warn/annotation-value) nil)))

(declare compound-item)

(defn annotation-items
  "Metadata items for the annotations `anns` (JCAnnotation trees), leaving out SOURCE ones."
  [env anns]
  (keep (fn [^JCTree$JCAnnotation a] (some->> (.attribute a) (compound-item env))) anns))

(defn type-ann-items
  "Metadata items for the type annotations of type `t`; at a declaration's element type
  (`elem?`) without those of the declaration's modifiers (*skip-anns*)."
  [env ^Type t elem?]
  (when t
    (keep (fn [^Attribute$Compound c]
            (when-not (and elem? (contains? *skip-anns* (jt/binary-name (.tsym (.type c)))))
              (compound-item env c)))
          (.getAnnotationMirrors t))))

(defn- compound-item
  "The metadata item for annotation `c`, nil for a SOURCE one."
  [env ^Attribute$Compound c]
  (when (not= "SOURCE" (str (retention c)))
    (let [vals (seq (.values c))
          v (cond
              (empty? vals) true
              ;; @A(true) is not the marker ^{A true}
              (and (= 1 (count vals)) (= "value" (str (.name ^Symbol (.fst (first vals)))))
                   (not (true? (attr-value env (.snd (first vals)) nil))))
              (attr-value env (.snd (first vals)) nil)
              :else (into {} (for [p vals]
                               [(keyword (str (.name ^Symbol (.fst p)))) (attr-value env (.snd p) nil)])))]
      (note! :form/annotation)
      [:ann (class-ref env (.tsym (.type c))) v])))

;;; ------------------------------------------------------------------------------------------
;;; Modifiers (§4.2)

(def flag-keywords
  [[Flags/PUBLIC :public] [Flags/PROTECTED :protected] [Flags/PRIVATE :private]
   [Flags/ABSTRACT :abstract] [Flags/STATIC :static] [Flags/FINAL :final]
   [Flags/TRANSIENT :transient] [Flags/VOLATILE :volatile] [Flags/SYNCHRONIZED :synchronized]
   [Flags/NATIVE :native] [Flags/STRICTFP :strictfp] [Flags/DEFAULT :default]
   [Flags/SEALED :sealed] [Flags/NON_SEALED :non-sealed]])

(defn modifier-items
  "Metadata items for the modifiers written in `mods`, and `extra` flags, then annotations."
  [env ^JCTree$JCModifiers mods & {:keys [drop]}]
  (let [fl (.flags mods)
        drop (set drop)]
    (concat
     (for [[bit kw] flag-keywords
           :when (and (not (zero? (bit-and fl (long bit)))) (not (drop kw)))]
       (do (note! (keyword "mod" (name kw))) kw))
     (annotation-items env (.annotations mods)))))

(defn deprecated-item [^Symbol sym ^JCTree$JCModifiers mods]
  (when (and (jt/has-flag? sym Flags/DEPRECATED)
             (not (jt/has-flag? sym Flags/DEPRECATED_ANNOTATION)))
    (note! :mod/deprecated)
    :deprecated))

;;; ------------------------------------------------------------------------------------------
;;; Expression results
;;
;; An expression converts to {:f form, :t type}: :t is the static type Arbace gives the form,
;; an erased javac Type, or :lit-int for an int literal (a long to Clojure, narrowed by the
;; context), or :null.

(defn- r [f t] {:f f :t t})

(defn- int-type ^Type [] (.intType jt/*syms*))
(defn- bool-type ^Type [] (.booleanType jt/*syms*))

(defn atype
  "The Arbace type of result `x` as a javac type (int for :lit-int, nil for :null)."
  [x]
  (case (:t x) :lit-int (int-type) :null nil (:t x)))

(defn pure?
  "True when evaluating `form` twice is the same as once: a local, this, a literal."
  [form]
  (or (and (symbol? form) (not (namespace form)))
      (and (instance? CRef form) (= "this" (:member form)))
      (string? form) (number? form) (char? form) (boolean? form) (nil? form) (keyword? form)))

(defn- strip-do [forms]
  (mapcat (fn [x] (if (and (seq? x) (= 'do (first x))) (rest x) [x])) forms))

(defn do-form
  "One form for the forms `xs`: nil, the form, or a do."
  [xs]
  (let [xs (strip-do xs)]
    (case (count xs)
      0 nil
      1 (first xs)
      (apply list 'do xs))))

(defn- hint
  "Form `f` with static type `t` without a cast: a tag on a symbol or list, else `f`."
  [env f ^Type t]
  (if (or (and (symbol? f) (not (namespace f))) (seq? f))
    (do (note! :conv/hint) (tag f (erased-form env t)))
    f))

(def unbox-method
  {"INT" ".intValue" "LONG" ".longValue" "SHORT" ".shortValue" "BYTE" ".byteValue"
   "CHAR" ".charValue" "FLOAT" ".floatValue" "DOUBLE" ".doubleValue" "BOOLEAN" ".booleanValue"})

(def narrow-op
  {"INT" 'unchecked-int "LONG" 'unchecked-long "SHORT" 'unchecked-short "BYTE" 'unchecked-byte
   "CHAR" 'unchecked-char "FLOAT" 'unchecked-float "DOUBLE" 'unchecked-double})

(def widen-op
  {"INT" 'int "LONG" 'long "FLOAT" 'float "DOUBLE" 'double "SHORT" 'short "CHAR" 'char})

(defn- box-call [env form ^Type p]
  (let [b (jt/boxed p)]
    (note! :conv/box)
    (list (member (class-ref env (.tsym b)) "valueOf") form)))

(defn- unbox-call [env form ^Type wrapper]
  (let [u (jt/unboxed wrapper)]
    (note! :conv/unbox)
    (list (symbol (unbox-method (tag-name u))) form)))

(defn cast-form [env form ^Type t]
  (note! :conv/cast)
  (list (h env 'cast) (erased-form env t) form))

(defn coerce
  "Convert result `x` to the erased target type `target` as javac's TransTypes and Lower do
  in assignment contexts, writing out what Arbace does not do implicitly (§5.5, §7.4).
  With :explicit, primitive widening and boxing are written too (conditional branches)."
  ([env x target] (coerce env x target false))
  ([env x ^Type target explicit?]
   (let [form (:f x) t (:t x)]
     (cond
       (nil? target) form
       (jt/void? target) form
       (= t :null) form
       (prim? target)
       (cond
         (= t :lit-int)
         (cond
           (and (number? form) (= "DOUBLE" (tag-name target))) (double form)
           (and (number? form) (= "FLOAT" (tag-name target))) (list 'float (double form))
           (not explicit?) form
           (#{"INT" "SHORT" "BYTE" "CHAR"} (tag-name target)) form
           (= "LONG" (tag-name target)) form
           (= "DOUBLE" (tag-name target)) (if (number? form) (double form) (list 'double form))
           :else (if (number? form) (float form) (list (widen-op (tag-name target)) form)))
         (prim? t)
         (cond
           (same? t target) form
           (jt/widens? t target) (if explicit? (list (widen-op (tag-name target)) form) form)
           ;; constant narrowing
           :else (list (narrow-op (tag-name target)) form))
         :else ;; unboxing
         (let [u (jt/unboxed t)]
           (if u
             (if (and (not explicit?) (or (same? u target) (jt/widens? u target)))
               form
               (let [v (unbox-call env form t)]
                 (if (and explicit? (not (same? u target))) (list (widen-op (tag-name target)) v) v)))
             ;; from a non-wrapper type (a generic result): cast to javac's type when it is
             ;; a wrapper (TransTypes), else to the wrapper of the target
             (let [jw (some-> (:jt x) erasure)
                   w (if (and jw (jt/unboxed jw)) jw (jt/boxed target))
                   v (unbox-call env (cast-form env form w) w)
                   u (jt/unboxed w)]
               (if (and explicit? (not (same? u target))) (list (widen-op (tag-name target)) v) v)))))
       :else ;; reference target
       (cond
         (= t :lit-int) (box-call env form (int-type))
         (prim? t) (if explicit? (box-call env form t) form)
         (subtype? t target) form
         :else (cast-form env form target))))))

(defn coerce-r
  "Like coerce, returning a result with the target as type."
  [env x ^Type target]
  (if (nil? target)
    x
    (let [f (coerce env x target)]
      (if (identical? f (:f x))
        x
        (r f (erasure target))))))

(defn test-form
  "Result `x` as a condition: primitive boolean, unboxing a Boolean."
  [env x]
  (let [t (:t x)]
    (if (and (instance? Type t) (not (prim? t)))
      (coerce env x (bool-type) true)
      (:f x))))

;;; ------------------------------------------------------------------------------------------
;;; Literals

(defn- number-token
  "The source text of the numeric literal at `pos`, with a folded sign."
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

(defn- radix-text
  "Clojure text for a hex, octal or binary Java integer token, or nil for decimal ones."
  [tok]
  (let [s (-> tok (str/replace "_" "") (str/replace #"[lL]$" ""))]
    (cond
      (re-matches #"-?0[xX][0-9a-fA-F]+" s) s
      (re-matches #"-?0[bB][01]+" s) (str/replace s #"0[bB]" "2r")
      (re-matches #"-?0[0-7]+" s) s
      :else nil)))

(defn- read-long [s] (let [v (read-string s)] (when (integer? v) v)))

(defn- literal [env ^JCTree$JCLiteral t]
  (let [v (.getValue t)
        tt (.type t)]
    (note! :lit/any)
    (case (tag-name tt)
      "INT" (let [tok (try (number-token (TreeInfo/getStartPos t)) (catch Exception _ ""))
                  rt (radix-text tok)
                  v (long v)]
              (if-let [lv (and rt (read-long rt))]
                (if (= lv v)
                  (assoc (r (f/raw rt) :lit-int) :vals [v])
                  (r (list 'unchecked-int (f/raw rt)) (int-type)))
                (assoc (r v :lit-int) :vals [v])))
      "LONG" (let [tok (try (number-token (TreeInfo/getStartPos t)) (catch Exception _ ""))
                   rt (radix-text tok)
                   v (long v)]
               (if-let [lv (and rt (read-long rt))]
                 (if (= lv v)
                   (r (f/raw rt) tt)
                   (r (list 'unchecked-long (f/raw rt)) tt))
                 (r v tt)))
      "FLOAT" (let [fv (float v)
                    d (Double/parseDouble (Float/toString fv))]
                ;; the shortest decimal when it rounds back to the float, else the float's
                ;; exact value as a double
                (if (and (<= (Math/abs d) Float/MAX_VALUE) (= (unchecked-float d) fv))
                  (r (list 'float d) tt)
                  (r (list 'float (double fv)) tt)))
      "DOUBLE" (r (double v) tt)
      "CHAR" (let [c (char (int v))]
               (if (Character/isSurrogate c)
                 (r (list 'unchecked-char (f/raw (format "0x%04X" (int c)))) tt)
                 (r c tt)))
      "BOOLEAN" (r (if (number? v) (not (zero? (long v))) (boolean v)) tt)
      "CLASS" (r (str v) tt)
      "BOT" (r nil :null))))

;;; ------------------------------------------------------------------------------------------
;;; Names: locals, fields, enclosing instances

(declare with-type-args capture-values captured-mutables poly-call? assigned-syms local? simplify ex ex-stmt stmts stmt class-form anon-form lambda-form method-ref-form switch-form
         call-args)

(defn- enclosing-classes [env] (:classes env))

(defn- current-class ^Symbol$ClassSymbol [env] (first (:classes env)))

(defn- member-of? [^Symbol sym ^Symbol$ClassSymbol c]
  (.isMemberOf sym c jt/*types*))

(defn- this-of
  "The form for the instance of enclosing class `c` (`this` for the current class)."
  [env ^Symbol$ClassSymbol c]
  (if (identical? c (current-class env))
    (or (:this env) (do (note! :warn/no-this) 'this))
    (do (note! :form/outer-this)
        (if (jt/anonymous? c)
          (if-let [s (get (:anon-this env) c)]
            (do (note! :form/anonymous-outer-receiver) s)
            (do (note! :warn/anonymous-outer-this) (member (class-ref env c) "this")))
          (member (class-ref env c) "this")))))

(defn- qualifying-class
  "For a member referenced by simple name: for an instance member the innermost enclosing
  class of which it is a member (whose instance is the receiver); for a static member the
  current class when it is a member of it, else its declaring class, as javac does."
  [env ^Symbol sym]
  (if (jt/static? sym)
    (cond
      (not (member-of? sym (current-class env))) (.owner sym)
      ;; javac's qualifier would be the anonymous class itself, which has no name to write
      (jt/anonymous? (current-class env)) (do (note! :warn/static-via-anonymous) (.owner sym))
      :else (current-class env))
    (or (first (filter #(member-of? sym %) (enclosing-classes env)))
        (.owner sym))))

(defn- field-bare?
  "True when field `sym` can be written by its bare name here (§4.5, §5.2)."
  [env ^Symbol sym]
  (let [nm (str (.name sym))
        owner (.owner sym)
        classes (enclosing-classes env)
        idx (first (keep-indexed (fn [i c] (when (identical? c owner) i)) classes))]
    (and idx
         ;; a field inherited by a closer class is that class's member (note 7)
         (identical? (qualifying-class env sym) owner)
         (not (#{"nil" "true" "false"} nm))
         (not (contains? (:names env) nm))
         ;; no closer class declares a field of that name
         (not-any? (fn [^Symbol$ClassSymbol c]
                     (some #(= "VAR" (jt/kind %))
                           (.getSymbolsByName (.members c) (.name sym))))
                   (take idx classes)))))

(defn- field-ref
  "The form reading field `sym` referenced by simple name."
  [env ^Symbol$VarSymbol sym]
  (let [nm (str (.name sym))]
    (cond
      (field-bare? env sym) (symbol nm)
      (jt/static? sym) (member (class-ref env (qualifying-class env sym)) nm)
      :else (list (symbol (str ".-" nm)) (this-of env (qualifying-class env sym))))))

(defn- var-type ^Type [^Symbol sym] (.erasure sym jt/*types*))

(defn- local? [^Symbol sym]
  (let [k (jt/kind sym)]
    (and (= "VAR" k)
         (let [ok (str (.kind (.owner sym)))]
           (or (= "MTH" ok) (= "VAR" ok) (not (#{"TYP" "PCK"} ok)))))))

(defn- ident [env ^JCTree$JCIdent t]
  (let [sym (.sym t)
        nm (str (.name t))]
    (cond
      (= nm "this") (r (or (:this env) 'this) (erasure (.type t)))
      (= nm "super") (r 'super (erasure (.type t)))
      (and (= "VAR" (jt/kind sym)) (local? sym))
      (r (or (get (:locals env) sym)
             (do (note! :warn/unknown-local)
                 (when (System/getenv "J2C_DEBUG") (println "unknown local" nm "in" (str (current-class env))))
                 (symbol nm)))
         (var-type sym))
      (= "VAR" (jt/kind sym))
      (do (note! :form/field-read)
          (r (field-ref env sym) (var-type sym)))
      (= "TYP" (jt/kind sym)) (r (class-ref env sym) :type)
      :else (do (note! :warn/odd-ident) (r (symbol nm) (erasure (.type t)))))))

(defn- type-expr?
  "True when expression tree `t` names a type (or package), not a value."
  [^JCTree t]
  (let [s (TreeInfo/symbol t)]
    (and s (#{"TYP" "PCK"} (jt/kind s)))))

(defn- qual-type
  "The erased type javac uses for qualifier expression `q`."
  ^Type [^JCTree$JCExpression q]
  (let [t (.skipTypeVars jt/*types* (.type q) false)]
    (erasure t)))

(defn- compound-qual?
  "Is qualifier `q`'s type (through type variables) an intersection?"
  [^JCTree$JCExpression q]
  (.isCompound (.skipTypeVars jt/*types* (.type q) false)))

(defn- class-literal [env ^Type t]
  (note! :form/class-literal)
  (if (or (prim? t) (jt/void? t))
    (member (cref (str "java.lang." ({"int" "Integer" "char" "Character" "void" "Void"}
                                     (str (jt/prim-sym t)) (str/capitalize (str (jt/prim-sym t))))))
            "TYPE")
    (erased-form env t)))

(defn- select [env ^JCTree$JCFieldAccess t]
  (let [sym (.sym t)
        nm (str (.name t))
        q (.selected t)]
    (cond
      (= nm "class") (r (class-literal env (.type q)) (erasure (.type t)))
      (= nm "this") (r (this-of env (.tsym (.type q))) (erasure (.type t)))
      (= nm "super") (r (if (identical? (.tsym (.type q)) (current-class env))
                          'super
                          (member (class-ref env (.tsym (.type q))) "super"))
                        (erasure (.type t)))
      (#{"TYP" "PCK"} (jt/kind sym)) (r (if (= "TYP" (jt/kind sym)) (class-ref env sym) (symbol (str sym))) :type)
      (and (= nm "length") (instance? Type$ArrayType (.type q)))
      (do (note! :form/alength)
          (r (list (h env 'alength) (coerce env (ex env q) (qual-type q))) (int-type)))
      (jt/static? sym)
      (let [c (if (type-expr? q) (.tsym (qual-type q)) (.tsym (qual-type q)))
            ref (member (class-ref env c) nm)]
        (note! :form/static-field)
        (r (if (type-expr? q)
             ref
             (let [qf (:f (ex env q))]
               (if (pure? qf) ref (list 'do qf ref))))
           (var-type sym)))
      :else
      (let [qr (ex env q)
            qf (if (compound-qual? q)
                 (coerce env (coerce-r env qr (qual-type q)) (erasure (.type (.owner sym))))
                 (coerce env qr (qual-type q)))]
        (note! :form/instance-field)
        (r (list (symbol (str ".-" nm)) qf) (var-type sym))))))

;;; ------------------------------------------------------------------------------------------
;;; Overloads (§5.6, §7.3)

(defn- erased-params [^Symbol$MethodSymbol m]
  (vec (.getParameterTypes (.erasure m jt/*types*))))

(defn- sig-key [m] (mapv #(str (erasure %)) (erased-params m)))

(defn- accessible? [env ^Symbol m]
  (let [fl (.flags m)
        cur (current-class env)]
    (cond
      (not (zero? (bit-and fl (long Flags/PUBLIC)))) true
      (not (zero? (bit-and fl (long Flags/PRIVATE))))
      (identical? (.outermostClass m) (.outermostClass cur))
      :else true)))

(defn- methods-named
  "All methods named `nm` in type `site` and its supertypes, one per erased signature."
  [env ^Type site nm ctor?]
  (let [site (if (instance? Type$ArrayType site) (jt/object-type) site)
        types (if ctor?
                [site]
                (cond-> (vec (.closure jt/*types* site))
                  (.isInterface (.tsym site)) (conj (jt/object-type))))
        name (jt/jname nm)]
    (->> (for [^Type ty types
               :when (instance? Type$ClassType ty)
               ^Symbol s (.getSymbolsByName (.members (.tsym ty)) name)
               :when (and (= "MTH" (jt/kind s))
                          (zero? (bit-and (.flags s) (long (bit-or Flags/SYNTHETIC Flags/BRIDGE))))
                          (accessible? env s))]
           s)
         (reduce (fn [[seen out] m]
                   (let [k (sig-key m)]
                     (if (seen k) [seen out] [(conj seen k) (conj out m)])))
                 [#{} []])
         second)))

(defn- pin-needed?
  "Does a call of javac's method `m` need param-tags, that is, does the class forms compiler's
  resolution (SPEC §5.6, §7.3) choose another method, another arity mode or none without them?
  `kind` is :instance (receiver of static type `site`), :static (class `site`) or :ctor (of
  class `site`); `from` the class whose code it is; `nodes` the arguments as the compiler will
  see them; `varargs?` whether the variable arguments are written unpacked."
  [kind ^Symbol$ClassSymbol from site ^Symbol$MethodSymbol m nodes varargs?]
  (note! :pin/decided)
  (let [need (res/pin-needed? kind from site m nodes varargs?)]
    (when need (note! (keyword "pin" (name kind))))
    need))

(defn- param-tags [env ^Symbol$MethodSymbol m]
  (note! :form/param-tags)
  [:param-tags (mapv #(erased-form env %) (erased-params m))])

;;; ------------------------------------------------------------------------------------------
;;; Calls

(defn- arg-targets
  "The erased types javac's TransTypes converts the arguments to."
  [^Symbol$MethodSymbol m ^Type mtype n varargs-elem]
  (let [ps (vec (if (and mtype (instance? Type$MethodType (.asMethodType mtype)))
                  (.getParameterTypes mtype)
                  (.getParameterTypes (.erasure m jt/*types*))))
        poly? (.isSignaturePolymorphic jt/*types* ^Symbol$MethodSymbol (.baseSymbol ^Symbol m))]
    (cond
      poly? (repeat n nil)
      varargs-elem (concat (map erasure (butlast ps)) (repeat (erasure varargs-elem)))
      :else (map erasure ps))))

(defn- arg-node [x] (res/arg-node (:t x) (:vals x)))

(defn call-args
  "Convert argument trees to [forms types nodes] for method `m`: the nodes are the arguments
  as the class forms compiler will see them (for its resolution, `arbace.j2c.resolve`). Packs
  variable arguments into an array unless `loose?`."
  [env ^Symbol$MethodSymbol m mtype args varargs-elem loose?]
  (let [targets (arg-targets m mtype (count args) varargs-elem)
        rs (doall (map (fn [a t]
                         (let [x (coerce-r env (ex env a) t)
                               a (TreeInfo/skipParens a)]
                           ;; the compiler unifies a conditional's branches by their class
                           ;; chain only: javac's type for it (the parameter's) as a hint
                           (if (and t (not (prim? t)) (not (jt/object? t)) (seq? (:f x))
                                    (or (instance? JCTree$JCConditional a) (instance? JCTree$JCSwitchExpression a)))
                             (assoc x :f (hint env (:f x) t) :t (erasure t))
                             x)))
                       args targets))]
    (if (and varargs-elem (not loose?))
      (let [nfixed (dec (count (erased-params m)))
            [fixed rest] (split-at nfixed rs)
            arr-t (.makeArrayType jt/*types* (erasure varargs-elem))]
        (note! :form/varargs-packed)
        [(concat (map :f fixed) [(list 'new (erased-form env arr-t) (mapv :f rest))])
         (concat (map :t fixed) [arr-t])
         (concat (map arg-node fixed) [(res/arg-node arr-t nil)])])
      (do (when varargs-elem (note! :form/varargs-loose))
          [(map :f rs) (map :t rs) (map arg-node rs)]))))

(defn- only-method-named? [env ^Symbol$MethodSymbol m ^Type site ctor?]
  (= 1 (count (methods-named env site (str (.name m)) ctor?))))

(defn- erased-varargs-elem?
  "Is the element type of javac's variable arguments array, `varargs-elem` (the inferred one),
  erased the element type of m's erased last parameter, as the compiler makes the array?"
  [^Symbol$MethodSymbol m varargs-elem]
  (.isSameType jt/*types* (erasure varargs-elem)
               (.elemtype ^Type$ArrayType (last (erased-params m)))))

(defn- loose-varargs?
  "Are the variable arguments of a call of `m` (javac's array element `ve`, nil when the call
  is not of variable arity) written unpacked? When m is the only method of its name and the
  compiler makes javac's array (SPEC note 6 of COMPILER-NOTES)."
  [env m site ctor? ve]
  (and ve (only-method-named? env m site ctor?) (erased-varargs-elem? m ve)))

(defn- ret-type ^Type [^Symbol$MethodSymbol m]
  (.getReturnType (.erasure m jt/*types*)))

(defn- invocation [env ^JCTree$JCMethodInvocation t]
  (let [meth (.meth t)
        m ^Symbol$MethodSymbol (TreeInfo/symbol meth)
        nm (str (.name m))
        args (.args t)
        ve (.varargsElement t)]
    (cond
      ;; this(...) / super(...)
      (= nm "<init>")
      (let [callee (str (TreeInfo/name meth))
            site (.type (.owner m))
            loose? (loose-varargs? env m site true ve)
            qual (when (instance? JCTree$JCFieldAccess meth) (.selected ^JCTree$JCFieldAccess meth))
            [afs _ nodes] (call-args env m (.type meth) args ve loose?)
            qf (when qual (:f (ex env qual)))
            pin? (pin-needed? :ctor (current-class env) (.owner m) m nodes (and ve loose?))
            head (fn [s] (if pin? (m+ s (param-tags env m)) s))]
        (note! (if (= callee "this") :form/this-call :form/super-call))
        (r (cond
             (= callee "this") (apply list (head 'this.) afs)
             qual (apply list (head '.super) qf afs)
             :else (apply list (head 'super.) afs))
           (jt/object-type)))
      :else
      (let [static? (jt/static? m)
            [recv site head-class]
            (cond
              (instance? JCTree$JCIdent meth)
              (if static?
                (let [q (qualifying-class env m)] [nil (.type q) q])
                (let [q (qualifying-class env m)] [(this-of env q) (.type q) q]))
              :else
              (let [fa ^JCTree$JCFieldAccess meth
                    q (.selected fa)
                    qn (str (TreeInfo/name q))]
                (cond
                  (and (instance? JCTree$JCIdent q) (= qn "super"))
                  ['super (erasure (.type q)) (.tsym (.type q))]
                  (and (instance? JCTree$JCFieldAccess q) (= qn "super"))
                  (let [x (select env q)] [(:f x) (erasure (.type q)) (.tsym (.type q))])
                  (type-expr? q) [nil (qual-type q) (.tsym (qual-type q))]
                  static? (let [qf (:f (ex env q))]
                            [(when-not (pure? qf) qf) (qual-type q) (.tsym (qual-type q))])
                  (compound-qual? q)
                  ;; an intersection-typed qualifier is cast to the member's class (TransTypes)
                  (let [qt (erasure (.type (.owner m)))
                        qr (coerce-r env (ex env q) (qual-type q))]
                    [(coerce env qr qt) qt (.tsym qt)])
                  :else (let [qr (ex env q)
                              qt (qual-type q)]
                          [(coerce env qr qt) qt (.tsym qt)]))))
            loose? (loose-varargs? env m site false ve)
            [afs _ nodes] (call-args env m (.type meth) args ve loose?)
            poly? (.isSignaturePolymorphic jt/*types* ^Symbol$MethodSymbol (.baseSymbol ^Symbol m))
            pin? (or poly?
                     (if static?
                       (pin-needed? :static (current-class env) head-class m nodes (and ve loose?))
                       (pin-needed? :instance (current-class env) site m nodes (and ve loose?))))
            ;; an anonymous class cannot be named: pin with the declaring class, or not at all
            head-class (if (and (not static?) (jt/class-sym? head-class) (jt/anonymous? head-class))
                         (let [o (.owner m)]
                           (when pin? (note! :warn/anonymous-qualifier))
                           (if (and (jt/class-sym? o) (not (jt/anonymous? o))) o nil))
                         head-class)
            pin? (and pin? head-class)
            rt (if (and (= nm "clone") (instance? Type$ArrayType site)) site (ret-type m))
            rt (if poly? (erasure (.type t)) rt)]
        (when poly? (note! :form/signature-polymorphic))
        (note! (if static? :form/static-call :form/instance-call))
        (update (r (cond
             static?
             (let [head (cond-> (member (class-ref env head-class) nm)
                          pin? (m+ (param-tags env m)))
                   call (apply list head afs)]
               (if (and recv (not (identical? recv 'super))) (list 'do recv call) call))
             poly?
             ;; the call site descriptor: param-tags from javac's call site types, the
             ;; result type as a tag on the call
             (let [call (apply list (m+ (member (class-ref env head-class) (str "." nm))
                                        [:param-tags (mapv #(erased-form env %) (.getParameterTypes (.type meth)))])
                               recv afs)]
               (if (or (jt/object? rt) (jt/void? rt)) call (tag call (erased-form env rt))))
             pin?
             (apply list (m+ (member (class-ref env head-class) (str "." nm)) (param-tags env m))
                    recv afs)
             :else (apply list (symbol (str "." nm)) recv afs))
           rt)
                :f #(with-type-args env % (.typeargs t)))))))

(defn- with-type-args
  "Call `form` with the type annotations of its explicit type arguments `targs` (trees) on the
  method symbol: ^{:type-args [...]} (§8.4)."
  [env form targs]
  (if (and (seq? form) (some #(type-annotated? (.type ^JCTree %)) targs))
    (let [ta [:type-args (mapv #(type-form env (.type ^JCTree %)) targs)]]
      (note! :form/type-args)
      (if (= 'do (first form))
        (apply list (concat (butlast form) [(with-type-args env (last form) targs)]))
        (apply list (m+ (first form) ta) (rest form))))
    form))

(defn- new-class [env ^JCTree$JCNewClass t]
  (if (.def t)
    (let [x (anon-form env t)] (assoc x :f (capture-values env (.def t) (:f x))))
    (let [c ^Symbol$ClassSymbol (.tsym (.type t))
          m ^Symbol$MethodSymbol (.constructor t)
          ve (.varargsElement t)
          site (.type c)
          loose? (loose-varargs? env m site true ve)
          [afs _ nodes] (call-args env m (.constructorType t) (.args t) ve loose?)
          encl (when (.encl t) (coerce env (ex env (.encl t)) (erasure (.type (.encl t)))))
          pin? (pin-needed? :ctor (current-class env) c m nodes (and ve loose?))
          cr (class-ref env c)]
      (note! :form/new)
      (r (cond
           (.encl t) (do (note! :form/qualified-new)
                         (apply list (cond-> '.new pin? (m+ (param-tags env m))) encl cr afs))
           ;; a class type with type annotations: (new ^{A true} (C ^{B true} T) args)
           (type-annotated? (.type (.clazz t)))
           (apply list 'new (cond-> (type-form env (.type (.clazz t))) pin? (m+ (param-tags env m))) afs)
           pin? (apply list (m+ (member cr "new") (param-tags env m)) afs)
           :else (apply list (if (symbol? cr) (symbol (str cr ".")) (member cr ".")) afs))
         (erasure (.type t))))))

(defn- new-array [env ^JCTree$JCNewArray t]
  (let [ty (erasure (.type t))
        elem (.elemtype ^Type$ArrayType ty)]
    (note! :form/new-array)
    (letfn [(init [^JCTree$JCNewArray a ^Type ety]
              (mapv (fn [e]
                      (if (and (instance? JCTree$JCNewArray e) (nil? (.elemtype ^JCTree$JCNewArray e)))
                        (init e (.elemtype ^Type$ArrayType (erasure (.type ^JCTree e))))
                        (coerce env (ex env e) ety)))
                    (.elems a)))]
      (r (if (.elems t)
           (list 'new (code-type-form env (.type t)) (init t elem))
           (apply list 'new (code-type-form env (.type t))
                  (map #(coerce env (ex env %) (int-type)) (.dims t))))
         ty))))

;;; ------------------------------------------------------------------------------------------
;;; Operators (§5.4)

(def int-ops
  {"PLUS" 'unchecked-add-int "MINUS" 'unchecked-subtract-int "MUL" 'unchecked-multiply-int
   "DIV" 'unchecked-divide-int "MOD" 'unchecked-remainder-int "BITAND" 'bit-and-int
   "BITOR" 'bit-or-int "BITXOR" 'bit-xor-int "SL" 'bit-shift-left-int "SR" 'bit-shift-right-int
   "USR" 'unsigned-bit-shift-right-int})

(def long-ops
  {"PLUS" 'unchecked-add "MINUS" 'unchecked-subtract "MUL" 'unchecked-multiply
   "DIV" 'unchecked-divide "MOD" 'unchecked-remainder "BITAND" 'bit-and "BITOR" 'bit-or
   "BITXOR" 'bit-xor "SL" 'bit-shift-left "SR" 'bit-shift-right "USR" 'unsigned-bit-shift-right})

(def float-ops
  {"PLUS" 'unchecked-add-float "MINUS" 'unchecked-subtract-float
   "MUL" 'unchecked-multiply-float "DIV" 'unchecked-divide-float
   "MOD" 'unchecked-remainder-float})

(def double-ops
  {"PLUS" 'unchecked-add "MINUS" 'unchecked-subtract "MUL" 'unchecked-multiply
   "DIV" 'unchecked-divide "MOD" 'unchecked-remainder})

(def cmp-ops {"LT" '< "GT" '> "LE" '<= "GE" '>=})

(defn- op-sym
  "The operator for binary tag `tg` on operands of (promoted) type `t`."
  [tg ^Type t]
  (let [table (case (tag-name t)
                ("INT" "SHORT" "BYTE" "CHAR") int-ops
                "LONG" long-ops
                "FLOAT" float-ops
                "DOUBLE" double-ops
                nil)]
    (get table tg)))

(defn- operator-params [^JCTree t]
  (let [op (cond (instance? JCTree$JCBinary t) (.operator ^JCTree$JCBinary t)
                 (instance? JCTree$JCAssignOp t) (.operator ^JCTree$JCAssignOp t)
                 (instance? JCTree$JCUnary t) (.operator ^JCTree$JCUnary t))]
    (vec (.getParameterTypes (.type ^Symbol op)))))

(defn- flatten-concat
  "Operands of a string concatenation chain. A constant operand that is itself a
  concatenation stays one operand (javac's StringConcat takes it as one constant)."
  ([^JCTree t] (flatten-concat t true))
  ([^JCTree t top?]
   (let [t (TreeInfo/skipParens t)]
     (if (and (instance? JCTree$JCBinary t) (= "PLUS" (str (.getTag t)))
              (jt/string-type? (.type t))
              (or top? (nil? (.constValue (.type t)))))
       (concat (flatten-concat (.lhs ^JCTree$JCBinary t) false) (flatten-concat (.rhs ^JCTree$JCBinary t) false))
       [t]))))

(defn- concat-operand
  "The form of string concatenation operand tree `o` converted to result `x`. Operands are
  converted by their own type (an int literal needs no conversion since its digits are the
  same); a generic one of type String is cast to String, since javac's operator for it takes a
  String (TransTypes)."
  [env ^JCTree o x]
  (let [t (.type o)]
    (if (and (jt/string-type? t) (instance? Type (:t x)) (not (jt/string-type? (:t x))))
      (coerce env x (erasure t))
      (:f x))))

(defn- float-compare-operand [env x]
  (if (and (#{"INT" "LONG" "SHORT" "BYTE" "CHAR"} (some-> (atype x) tag-name)))
    (do (note! :conv/float-promotion) (list 'unchecked-float (:f x)))
    (:f x)))

(defn- logic-form
  "Java's non-short-circuit boolean & | ^ over results `a` and `b`."
  [env tg a b]
  (let [both-pure (and (pure? (:f a)) (pure? (:f b)))
        build (fn [x y] (case tg
                          "BITAND" (list (h env 'and) x y)
                          "BITOR" (list (h env 'or) x y)
                          "BITXOR" (list (h env 'not) (list '= x y))))]
    (note! :form/boolean-logic)
    (if (or both-pure (= tg "BITXOR"))
      (build (:f a) (:f b))
      (let [ta (gensym! env "a") tb (gensym! env "b")]
        (list 'let [ta (:f a) tb (:f b)] (build ta tb))))))

(defn binary-op
  "Form for binary operator tag `tg` with operator parameter types `ps` on converted operands."
  [env tg ps ^Type result a b]
  (let [[pa pb] ps]
    (cond
      (and (= tg "PLUS") (jt/string-type? result))
      (list (h env 'java-str) (:f a) (:f b))
      (#{"AND" "OR"} tg)
      (list (h env (if (= tg "AND") 'and 'or)) (test-form env a) (test-form env b))
      (and (#{"BITAND" "BITOR" "BITXOR"} tg) (= "BOOLEAN" (tag-name pa)))
      (logic-form env tg (assoc a :f (test-form env a)) (assoc b :f (test-form env b)))
      (#{"EQ" "NE"} tg)
      (let [refs? (not (prim? pa))
            af (if refs? (:f a) (coerce env a pa))
            bf (if refs? (:f b) (coerce env b pb))
            float? (= "FLOAT" (tag-name pa))
            af (if float? (float-compare-operand env a) af)
            bf (if float? (float-compare-operand env b) bf)
            eq (cond
                 (and refs? (nil? bf) (= :null (:t b))) (if (= tg "EQ") (list (h env 'nil?) af) (list (h env 'some?) af))
                 (and refs? (nil? af) (= :null (:t a))) (if (= tg "EQ") (list (h env 'nil?) bf) (list (h env 'some?) bf))
                 refs? (list (h env 'identical?) af bf)
                 (= "BOOLEAN" (tag-name pa)) (list '= af bf)
                 :else (list '== af bf))]
        (if (and (= tg "NE") (not (#{'nil? 'some?} (first eq))) (not (#{"nil?" "some?"} (name (first eq)))))
          (list (h env 'not) eq)
          eq))
      (cmp-ops tg)
      (let [float? (= "FLOAT" (tag-name pa))]
        (list (cmp-ops tg)
              (if float? (float-compare-operand env a) (coerce env a pa))
              (if float? (float-compare-operand env b) (coerce env b pb))))
      :else
      (let [shift? (#{"SL" "SR" "USR"} tg)
            sym (op-sym tg (if shift? pa result))]
        (when-not sym (throw (ex-info (str "no operator for " tg " " pa) {})))
        (list (h env sym) (coerce env a pa) (coerce env b (if shift? pb pb)))))))

(declare has-patterns? pattern-bool cond-form pattern-binds dup-sides)

(defn- binary [env ^JCTree$JCBinary t]
  (let [tg (str (.getTag t))
        result (erasure (.type t))]
    (note! (keyword "op" tg))
    (cond
      (and (#{"AND" "OR"} tg) (has-patterns? t)) (r (pattern-bool env t) result)
      :else
    (if (and (= tg "PLUS") (jt/string-type? result))
      (do (note! :form/java-str)
          (r (apply list (h env 'java-str)
                    (map (fn [o] (concat-operand env o (ex env o)))
                         (flatten-concat t)))
             result))
      (r (binary-op env tg (operator-params t) result (ex env (.lhs t)) (ex env (.rhs t)))
         (let [c (.constValue (.type t))]
           (if (and c (= "INT" (tag-name result)) false) :lit-int result)))))))

(defn- unary-value [env ^JCTree$JCUnary t]
  (let [tg (str (.getTag t))
        x (when-not (and (= tg "NOT") (has-patterns? (.arg t))) (ex env (.arg t)))
        [p] (operator-params t)
        result (erasure (.type t))]
    (note! (keyword "op" tg))
    (case tg
      "NOT" (r (if (has-patterns? (.arg t)) (pattern-bool env t) (list (h env 'not) (test-form env x))) result)
      "POS" (r (coerce env x p) result)
      "NEG" (if (and (number? (:f x)) false)
              x
              (r (list (h env (case (tag-name p)
                                ("INT" "SHORT" "BYTE" "CHAR") 'unchecked-negate-int
                                "LONG" 'unchecked-negate
                                "FLOAT" 'unchecked-negate-float
                                "DOUBLE" 'unchecked-negate))
                       (coerce env x p))
                 result))
      "COMPL" (r (list (h env (if (= "LONG" (tag-name p)) 'bit-not 'bit-not-int)) (coerce env x p))
                 result))))

;;; ------------------------------------------------------------------------------------------
;;; Assignments, compound assignments, increments (§5.4, §7.6)

(def ^:dynamic *hoistable*
  "Postfix increments (JCUnary trees, by identity) whose update may move out of the
  expression: before it (binding the old value) or after the statement."
  #{})
(def ^:dynamic *hoisted* nil)

(defn- tree-children [^JCTree x]
  (for [fld (.getFields (class x))
        :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers ^java.lang.reflect.Field fld)))
        :let [nm (.getName ^java.lang.reflect.Field fld)]
        :when (not (#{"type" "sym" "target" "operator" "constructor" "varargsElement"
                      "constructorType" "polyKind"} nm))
        :let [v (.get ^java.lang.reflect.Field fld x)]
        c (cond (instance? JCTree v) [v]
                (instance? com.sun.tools.javac.util.List v) (filter #(instance? JCTree %) v)
                :else [])]
    c))

(declare mutable?)

(defn- captured-mutables
  "The forms of the ^:mutable locals of env that class or lambda tree `t` refers to (captures)."
  [env ^JCTree t]
  (distinct
   (for [x (tree-seq #(instance? JCTree %) tree-children t)
         :when (instance? JCTree$JCIdent x)
         :let [sym (.sym ^JCTree$JCIdent x)]
         :when (and sym (= "VAR" (jt/kind sym)) (local? sym) (mutable? sym))
         :let [f (get (:locals env) sym)]
         :when f]
     f)))

(defn- capture-values
  "Java captures only effectively final locals, so a captured local that the forms make
  ^:mutable (a blank final, assigned once before) is rebound immutably around the capturing
  `form`: (let [x x] form)."
  [env ^JCTree t form]
  (if-let [cs (seq (captured-mutables env t))]
    (do (note! :form/capture-rebind)
        (list (h env 'let) (vec (mapcat (fn [c] [(with-meta c nil) c]) cs)) form))
    form))

(defn- all-trees [^JCTree t]
  (tree-seq #(and (instance? JCTree %) (not (instance? JCTree$JCClassDecl %))) tree-children t))

(defn hoistable-increments
  "The postfix increments in expression `e` evaluated unconditionally, on a variable that `e`
  does not otherwise mention (a local, or a field when `e` calls nothing)."
  [^JCTree e]
  (let [trees (all-trees e)
        effects? (some #(or (instance? JCTree$JCMethodInvocation %) (instance? JCTree$JCNewClass %)
                            (instance? JCTree$JCAssign %) (instance? JCTree$JCAssignOp %)
                            (instance? JCTree$JCLambda %) (instance? JCTree$JCMemberReference %))
                       trees)
        uses (frequencies (keep #(cond (instance? JCTree$JCIdent %) (.sym ^JCTree$JCIdent %)
                                       (instance? JCTree$JCFieldAccess %) (.sym ^JCTree$JCFieldAccess %))
                                trees))
        cands (volatile! #{})]
    (letfn [(walk [^JCTree x]
              (cond
                (instance? JCTree$JCConditional x) (walk (.cond ^JCTree$JCConditional x))
                (and (instance? JCTree$JCBinary x) (#{"AND" "OR"} (str (.getTag x))))
                (walk (.lhs ^JCTree$JCBinary x))
                (or (instance? JCTree$JCLambda x) (instance? JCTree$JCClassDecl x)
                    (instance? JCTree$JCSwitchExpression x) (instance? JCTree$JCNewClass x)) nil
                :else
                (do (when (and (instance? JCTree$JCUnary x) (#{"POSTINC" "POSTDEC"} (str (.getTag x))))
                      (let [a (TreeInfo/skipParens (.arg ^JCTree$JCUnary x))]
                        (when (instance? JCTree$JCIdent a)
                          (let [sym (.sym ^JCTree$JCIdent a)]
                            (when (and (= 1 (uses sym))
                                       (or (local? sym) (not effects?)))
                              (vswap! cands conj x))))))
                    (run! walk (tree-children x)))))]
      (walk e))
    (java.util.Collections/newSetFromMap (java.util.IdentityHashMap.))
    (let [s (java.util.Collections/newSetFromMap (java.util.IdentityHashMap.))]
      (doseq [c @cands] (.add s c))
      (set s))))

(defn with-hoisting
  "Convert expression `e` with `f` (env e -> form), moving postfix increments out: :after the
  statement (returns [form set!...]) or :before it (returns a let form)."
  [env ^JCTree e mode f]
  (let [h (hoistable-increments e)]
    (if (empty? h)
      [(f env e)]
      (binding [*hoistable* h
                *hoisted* (atom {:mode mode :incs []})]
        (let [form (f env e)
              incs (:incs @*hoisted*)]
          (cond
            (empty? incs) [form]
            (= mode :after) (into [form] (map #(nth % 2) incs))
            :else [(apply list 'let (vec (mapcat (fn [[v g _]] [v g]) incs))
                          (concat (map #(nth % 2) incs) [form]))]))))))

(defn- place
  "An assignable place for lhs tree `t`: {:get form :set (fn [v] form) :t type :binds [..]}."
  [env ^JCTree t want-pure?]
  (let [t (TreeInfo/skipParens t)]
    (cond
      (instance? JCTree$JCIdent t)
      (let [x (ident env t)
            f (:f x)]
        {:get f :set (fn [v] (list 'set! f v)) :t (:t x) :binds []})
      (instance? JCTree$JCFieldAccess t)
      (let [fa ^JCTree$JCFieldAccess t
            sym (.sym fa)
            nm (str (.name fa))
            q (.selected fa)]
        (if (or (jt/static? sym) (type-expr? q))
          (let [x (select env fa)
                f (:f x)]
            (if (and (seq? f) (= 'do (first f)))
              {:get (last f) :set (fn [v] (list 'set! (last f) v)) :t (:t x) :binds [(gensym! env "_") (second f)]}
              {:get f :set (fn [v] (list 'set! f v)) :t (:t x) :binds []}))
          (let [qf (coerce env (ex env q) (qual-type q))
                [qf binds] (if (and want-pure? (not (pure? qf)))
                             (let [s (gensym! env "o")] [s [s qf]])
                             [qf []])
                acc (list (symbol (str ".-" nm)) qf)]
            {:get acc :set (fn [v] (list 'set! acc v)) :t (var-type sym) :binds binds})))
      (instance? JCTree$JCArrayAccess t)
      (let [aa ^JCTree$JCArrayAccess t
            af (coerce env (ex env (.indexed aa)) (erasure (.type (.indexed aa))))
            if (coerce env (ex env (.index aa)) (int-type))
            [af b1] (if (and want-pure? (not (pure? af))) (let [s (gensym! env "arr")] [s [s af]]) [af []])
            [if b2] (if (and want-pure? (not (pure? if))) (let [s (gensym! env "i")] [s [s if]]) [if []])]
        {:get (list (h env 'aget) af if) :set (fn [v] (list (h env 'aset) af if v))
         :t (erasure (.type aa)) :binds (into b1 b2)})
      :else (throw (ex-info (str "unsupported assignment target " (.getKind t)) {})))))

(defn- with-binds [binds form]
  (if (seq binds) (list 'let (vec binds) form) form))

(defn- assign [env ^JCTree$JCAssign t]
  (let [p (place env (.lhs t) false)
        v (coerce env (ex env (.rhs t)) (erasure (.type (.lhs t))))]
    (note! :form/set!)
    (r (with-binds (:binds p) ((:set p) v)) (erasure (.type (.lhs t))))))

(defn- narrow-to
  "Form `f` of type `from` converted to the variable type `to` as Java's compound assignment
  does (implicit narrowing written out, boxing left implicit)."
  [env f ^Type from ^Type to]
  (let [to-prim (if (prim? to) to (jt/unboxed to))]
    (cond
      (nil? to-prim) f
      (or (same? from to-prim) (jt/widens? from to-prim)) f
      :else (list (narrow-op (tag-name to-prim)) f))))

(defn- literal-branches
  "A conditional form of integer literal branches with the branches as constants of primitive
  type `pt` (long, float or double): javac types the constants of a compound assignment's
  conditional operand by the operator (Gen.visitConditional)."
  [form ^Type pt]
  (cond
    (and (seq? form) (= 'if (first form)) (= 4 (count form)))
    (let [[_ c a b] form] (list 'if c (literal-branches a pt) (literal-branches b pt)))
    (integer? form) (case (tag-name pt)
                      "LONG" (list 'long form)
                      "DOUBLE" (double form)
                      "FLOAT" (list 'float (double form))
                      form)
    :else form))

(defn- assign-op [env ^JCTree$JCAssignOp t]
  (let [tg (str (.getTag t))
        base (str/replace tg "_ASG" "")
        lt (erasure (.type (.lhs t)))
        p (place env (.lhs t) true)
        [pa pb] (operator-params t)
        result (.getReturnType (.type ^Symbol (.operator t)))
        rhs (ex env (.rhs t))
        rhs (if (and (instance? JCTree$JCConditional (TreeInfo/skipParens (.rhs t)))
                     (#{"LONG" "FLOAT" "DOUBLE"} (tag-name pb))
                     (= :lit-int (:t rhs)))
              (assoc rhs :f (literal-branches (:f rhs) pb) :t pb)
              rhs)
        cur {:f (:get p) :t (:t p)}
        val (if (and (= base "PLUS") (jt/string-type? lt))
              (list (h env 'java-str) (:get p) (concat-operand env (.rhs t) rhs))
              (narrow-to env (binary-op env base [pa pb] (erasure result) cur rhs) (erasure result) lt))]
    (note! (keyword "op" tg))
    (r (with-binds (:binds p) ((:set p) val)) lt)))

(defn- inc-dec [env ^JCTree$JCUnary t value?]
  (let [tg (str (.getTag t))
        inc? (#{"PREINC" "POSTINC"} tg)
        post? (#{"POSTINC" "POSTDEC"} tg)
        lt (erasure (.type (.arg t)))
        pt (if (prim? lt) lt (jt/unboxed lt))
        p (place env (.arg t) true)
        g (:get p)
        newv (case (tag-name pt)
               "INT" (list (h env (if inc? 'unchecked-inc-int 'unchecked-dec-int)) g)
               ("SHORT" "BYTE" "CHAR") (list (narrow-op (tag-name pt))
                                             (list (h env (if inc? 'unchecked-inc-int 'unchecked-dec-int)) g))
               "LONG" (list (h env (if inc? 'unchecked-inc 'unchecked-dec)) g)
               "DOUBLE" (list (h env (if inc? 'unchecked-inc 'unchecked-dec)) g)
               "FLOAT" (list (h env (if inc? 'unchecked-add-float 'unchecked-subtract-float)) g 1))
        setf ((:set p) newv)]
    (note! (keyword "op" tg))
    (r (if (and value? post? *hoisted* (contains? *hoistable* t))
         (let [mode (:mode @*hoisted*)
               v (if (= mode :after) g (gensym! env (str/replace (str g) #"[^A-Za-z0-9_$]" "")))]
           (note! :flow/hoisted-increment)
           (swap! *hoisted* update :incs conj [v g setf])
           v)
         (if (and value? post?)
         (let [old (gensym! env "old")]
           (list 'let (into (vec (:binds p)) [old g]) setf old))
         (with-binds (:binds p) setf)))
       lt)))

;;; ------------------------------------------------------------------------------------------
;;; Casts, instanceof, conditionals

(defn- poly-call? [^JCTree e]
  (let [e (TreeInfo/skipParens e)]
    (and (instance? JCTree$JCMethodInvocation e)
         (let [m (TreeInfo/symbol (.meth ^JCTree$JCMethodInvocation e))]
           (and (instance? Symbol$MethodSymbol m) (.isSignaturePolymorphic jt/*types* ^Symbol$MethodSymbol (.baseSymbol ^Symbol m)))))))

(defn- type-cast [env ^JCTree$JCTypeCast t]
  (let [x (ex env (.expr t))
        target (or (.type (.clazz t)) (.type t))
        te (erasure target)
        from (:t x)]
    (note! :form/cast)
    (cond
      ;; a signature polymorphic call takes its result type from the cast (JLS 15.12.3)
      (poly-call? (.expr t)) (r (if (some #(and (vector? %) (= :tag (first %))) (f/items (:f x)))
                                  (:f x)
                                  (tag (:f x) (erased-form env te)))
                                te)
      (prim? te)
      (cond
        (= from :lit-int)
        (if (= "INT" (tag-name te)) (assoc (r (:f x) :lit-int) :vals (:vals x))
            (r (list (narrow-op (tag-name te)) (:f x)) te))
        (prim? from)
        (cond
          (same? from te) (r (:f x) te)
          (jt/widens? from te) (r (list (widen-op (tag-name te)) (:f x)) te)
          :else (r (list (narrow-op (tag-name te)) (:f x)) te))
        :else ;; unboxing (with a cast from non-wrappers), then widening
        (let [u (jt/unboxed from)
              [f u] (if u
                      [(unbox-call env (:f x) from) u]
                      (let [w (or (some-> (.unboxedType jt/*types* (.type (.expr t))) (#(when (prim? %) (jt/boxed %))))
                                  (jt/boxed te))]
                        [(unbox-call env (cast-form env (:f x) w) w) (jt/unboxed w)]))]
          (r (if (same? u te) f (list (widen-op (tag-name te)) f)) te)))
      (jt/components target)
      (let [needed (remove #(and (instance? Type from) (subtype? from %)) (jt/components target))]
        (note! :form/intersection-cast)
        (cond
          (empty? needed) (r (if (same? from te) (:f x) (hint env (:f x) te)) te)
          (= 1 (count needed)) (r (cast-form env (:f x) (first needed)) (erasure (first needed)))
          :else (r (list (h env 'cast) (apply list '& (map #(erased-form env %) needed)) (:f x))
                   (erasure (first needed)))))
      (= from :null) (r (cast-form env nil te) te)
      (prim? from) (let [b (if (= from :lit-int) (int-type) from)]
                     (r (box-call env (:f x) b) (erasure (jt/boxed b))))
      (= from :lit-int) (r (box-call env (:f x) (int-type)) (erasure (jt/boxed (int-type))))
      ;; javac emits no checkcast for an upcast, except to an array type (Gen.visitTypeCast
      ;; asks asSuper, which arrays answer only for Object and the array interfaces)
      (and (subtype? from te) (not (and (instance? Type$ArrayType te) (not (same? from te)))))
      (if (same? from te) x (r (hint env (:f x) te) te))
      (type-annotated? target) (r (list (h env 'cast) (code-type-form env target) (:f x)) te)
      :else (r (cast-form env (:f x) te) te))))

(defn- pattern-form
  "The form of pattern tree `p`, adding its bindings to the env atom `penv`."
  [env penv ^JCTree p]
  (cond
    (instance? JCTree$JCAnyPattern p) (do (note! :form/any-pattern) '_)
    (instance? JCTree$JCBindingPattern p)
    (let [v (.var ^JCTree$JCBindingPattern p)
          sym (.sym v)
          nm (str (.name v))
          s (symbol (if (= nm "") "_" nm))
          implicit? (or (nil? (.vartype v)) (.declaredUsingVar v))]
      (note! :form/binding-pattern)
      (swap! penv (fn [e] (-> e (assoc-in [:locals sym] s) (update :names conj nm))))
      (let [s (if (mutable? sym) (m+ s :mutable) s)]
        (if implicit?
          s
          (tag s (type-form env (.type sym))))))
    (instance? JCTree$JCRecordPattern p)
    (let [rp ^JCTree$JCRecordPattern p]
      (note! :form/record-pattern)
      (apply list (class-ref env (.tsym (.type rp)))
             (map #(pattern-form env penv %) (.nested rp))))
    :else (throw (ex-info "unknown pattern" {}))))

(declare pattern-instanceof?)

(defn dup-sides
  "The branches of a condition that cond-form repeats: #{:then :else}."
  [^JCTree c]
  (let [c (TreeInfo/skipParens c)
        tg (str (.getTag c))]
    (cond
      (pattern-instanceof? c) #{}
      (and (= tg "NOT") (has-patterns? (.arg ^JCTree$JCUnary c)))
      (set (map {:then :else :else :then} (dup-sides (.arg ^JCTree$JCUnary c))))
      (and (= tg "AND") (has-patterns? c))
      (conj (into (dup-sides (.lhs ^JCTree$JCBinary c)) (dup-sides (.rhs ^JCTree$JCBinary c))) :else)
      (and (= tg "OR") (has-patterns? c))
      (conj (into (dup-sides (.lhs ^JCTree$JCBinary c)) (dup-sides (.rhs ^JCTree$JCBinary c))) :then)
      :else #{})))

(defn- pattern-instanceof? [^JCTree c]
  (and (instance? JCTree$JCInstanceOf c) (instance? JCTree$JCPattern (.pattern ^JCTree$JCInstanceOf c))))

(defn has-patterns?
  "True when condition `c` contains a pattern instanceof outside lambdas and classes."
  [^JCTree c]
  (letfn [(walk [x]
            (cond
              (pattern-instanceof? x) true
              (or (instance? JCTree$JCLambda x) (instance? JCTree$JCClassDecl x)
                  (instance? JCTree$JCSwitchExpression x)) false
              :else (some walk (tree-children x))))]
    (boolean (walk c))))

(defn- pattern-vars [^JCTree p]
  (set (for [x (all-trees p) :when (instance? JCTree$JCBindingPattern x)]
         (.sym (.var ^JCTree$JCBindingPattern x)))))

(defn pattern-binds
  "{:t vars :f vars}: the pattern variables condition `c` introduces when true and when false
  (JLS 6.3.1)."
  [^JCTree c]
  (let [c (TreeInfo/skipParens c)
        none {:t #{} :f #{}}]
    (cond
      (pattern-instanceof? c) {:t (pattern-vars (.pattern ^JCTree$JCInstanceOf c)) :f #{}}
      (and (instance? JCTree$JCUnary c) (= "NOT" (str (.getTag c))))
      (let [{:keys [t f]} (pattern-binds (.arg ^JCTree$JCUnary c))] {:t f :f t})
      (and (instance? JCTree$JCBinary c) (= "AND" (str (.getTag c))))
      {:t (into (:t (pattern-binds (.lhs ^JCTree$JCBinary c))) (:t (pattern-binds (.rhs ^JCTree$JCBinary c)))) :f #{}}
      (and (instance? JCTree$JCBinary c) (= "OR" (str (.getTag c))))
      {:t #{} :f (into (:f (pattern-binds (.lhs ^JCTree$JCBinary c))) (:f (pattern-binds (.rhs ^JCTree$JCBinary c))))}
      :else none)))

(defn cond-form
  "The form testing condition `c` that runs (then-fn env') or (else-fn env') with the pattern
  variables in scope where Java has them (§5.8 if-instance). A branch may be repeated."
  [env ^JCTree c then-fn else-fn]
  (let [c (TreeInfo/skipParens c)
        tg (str (.getTag c))]
    (cond
      (pattern-instanceof? c)
      (let [io ^JCTree$JCInstanceOf c
            x (ex env (.expr io))
            penv (atom env)
            pf (pattern-form env penv (.pattern io))]
        (note! :form/if-instance)
        (list (h env 'if-instance) [pf (:f x)] (then-fn @penv) (else-fn env)))
      (and (= tg "NOT") (has-patterns? (.arg ^JCTree$JCUnary c)))
      (cond-form env (.arg ^JCTree$JCUnary c) else-fn then-fn)
      (and (= tg "AND") (has-patterns? c))
      (do (note! :flow/pattern-and)
          (cond-form env (.lhs ^JCTree$JCBinary c)
                     (fn [e] (cond-form e (.rhs ^JCTree$JCBinary c) then-fn else-fn))
                     else-fn))
      (and (= tg "OR") (has-patterns? c))
      (do (note! :flow/pattern-or)
          (cond-form env (.lhs ^JCTree$JCBinary c) then-fn
                     (fn [e] (cond-form e (.rhs ^JCTree$JCBinary c) then-fn else-fn))))
      :else (list 'if (test-form env (ex env c)) (then-fn env) (else-fn env)))))

(defn- pattern-bool
  "A boolean expression with patterns, as a value."
  [env ^JCTree c]
  (let [c (TreeInfo/skipParens c)
        tg (str (.getTag c))]
    (cond
      (pattern-instanceof? c) (cond-form env c (fn [_] true) (fn [_] false))
      (= tg "NOT") (list (h env 'not) (test-form env (ex env (.arg ^JCTree$JCUnary c))))
      (= tg "AND")
      (let [a (.lhs ^JCTree$JCBinary c) b (.rhs ^JCTree$JCBinary c)]
        (if (has-patterns? a)
          (cond-form env a (fn [e] (test-form e (ex e b))) (fn [_] false))
          (list (h env 'and) (test-form env (ex env a)) (test-form env (ex env b)))))
      (= tg "OR")
      (let [a (.lhs ^JCTree$JCBinary c) b (.rhs ^JCTree$JCBinary c)]
        (if (has-patterns? a)
          (cond-form env a (fn [_] true) (fn [e] (test-form e (ex e b))))
          (list (h env 'or) (test-form env (ex env a)) (test-form env (ex env b))))))))

(defn- instance-of [env ^JCTree$JCInstanceOf t]
  (if (instance? JCTree$JCPattern (.pattern t))
    (r (pattern-bool env t) (bool-type))
    (let [x (ex env (.expr t))]
      (note! :form/instance?)
      (r (list (h env 'instance?) (code-type-form env (.type (.pattern t))) (:f x)) (bool-type)))))

(defn- conditional [env ^JCTree$JCConditional t]
  (let [target (erasure (.type t))
        c (when-not (has-patterns? (.cond t)) (test-form env (ex env (.cond t))))
        a (when c (ex env (.truepart t)))
        b (when c (ex env (.falsepart t)))
        branch (fn [x]
                 (if (and (prim? target) (= :lit-int (:t x)) (#{"INT"} (tag-name target)))
                   (:f x)
                   (let [f (coerce env x target true)]
                     ;; reference branches of other types than the whole: hint to it
                     (if (and (not (prim? target)) (identical? f (:f x)) (instance? Type (:t x))
                              (not (same? (:t x) target)))
                       (hint env f target)
                       f))))]
    (note! :form/if)
    (cond-> (r (if c
                 (list 'if c (branch a) (branch b))
                 (cond-form env (.cond t) (fn [e] (branch (ex e (.truepart t)))) (fn [e] (branch (ex e (.falsepart t))))))
               (if (and a b (= :lit-int (:t a)) (= :lit-int (:t b))) :lit-int target))
      (and a b (= :lit-int (:t a)) (= :lit-int (:t b))) (assoc :vals (concat (:vals a) (:vals b))))))

;;; ------------------------------------------------------------------------------------------
;;; Expressions

(declare ex*)

(defn ex
  "Convert expression tree `t` to a result {:f form :t type}; :jt is javac's type of `t`."
  [env ^JCTree t]
  (let [x (ex* env t)]
    (if (contains? x :jt) x (assoc x :jt (.type t)))))

(defn- ex*
  [env ^JCTree t]
  (condp instance? t
    JCTree$JCParens (ex env (.expr ^JCTree$JCParens t))
    JCTree$JCLiteral (literal env t)
    JCTree$JCIdent (ident env t)
    JCTree$JCFieldAccess (select env t)
    JCTree$JCMethodInvocation (invocation env t)
    JCTree$JCNewClass (new-class env t)
    JCTree$JCNewArray (new-array env t)
    JCTree$JCArrayAccess (let [aa ^JCTree$JCArrayAccess t
                               idx (fn idx [^JCTree e acc]
                                     (let [e (TreeInfo/skipParens e)]
                                       (if (instance? JCTree$JCArrayAccess e)
                                         (idx (.indexed ^JCTree$JCArrayAccess e)
                                              (cons (.index ^JCTree$JCArrayAccess e) acc))
                                         [e acc])))
                               [base is] (idx aa nil)]
                           (note! :form/aget)
                           (r (apply list (h env 'aget) (coerce env (ex env base) (erasure (.type ^JCTree base)))
                                     (map #(coerce env (ex env %) (int-type)) is))
                              (erasure (.type t))))
    JCTree$JCAssign (assign env t)
    JCTree$JCAssignOp (assign-op env t)
    JCTree$JCUnary (if (#{"PREINC" "PREDEC" "POSTINC" "POSTDEC"} (str (.getTag t)))
                     (inc-dec env t true)
                     (unary-value env t))
    JCTree$JCBinary (binary env t)
    JCTree$JCTypeCast (type-cast env t)
    JCTree$JCInstanceOf (instance-of env t)
    JCTree$JCConditional (conditional env t)
    JCTree$JCLambda (let [x (lambda-form env t)] (assoc x :f (capture-values env t (:f x))))
    JCTree$JCMemberReference (method-ref-form env t)
    JCTree$JCSwitchExpression (switch-form env t nil)
    (throw (ex-info (str "unsupported expression " (.getKind t)) {:kind (kind-of t)}))))

(defn ex-stmt
  "Convert an expression used as a statement."
  [env ^JCTree t]
  (let [t (TreeInfo/skipParens t)]
    (if (and (instance? JCTree$JCUnary t)
             (#{"PREINC" "PREDEC" "POSTINC" "POSTDEC"} (str (.getTag t))))
      (:f (inc-dec env t false))
      (if (poly-call? t)
        (tag (:f (ex env t)) 'void)
        (:f (ex env t))))))

;;; ------------------------------------------------------------------------------------------
;;; Control flow analysis

(defn- const-true? [^JCTree c]
  (or (nil? c)
      (let [v (.constValue (.type c))]
        (and v (= 1 (if (number? v) (long v) (if v 1 0)))))))

(defn- norm-target [^JCTree t]
  (if (instance? JCTree$JCLabeledStatement t)
    (norm-target (.body ^JCTree$JCLabeledStatement t))
    t))

(defn- jumps-to
  "The jump statements (break, continue, yield) in `t` whose target is `target`, not looking
  into classes and lambdas."
  [^JCTree t target pred]
  (let [found (volatile! [])
        target (norm-target target)]
    (letfn [(walk [x]
              (cond
                (nil? x) nil
                (instance? JCTree$JCClassDecl x) nil
                (instance? JCTree$JCLambda x) nil
                (instance? JCTree x)
                (do (when (and (pred x)
                               (identical? target
                                           (norm-target
                                            (cond (instance? JCTree$JCBreak x) (.target ^JCTree$JCBreak x)
                                                  (instance? JCTree$JCContinue x) (.target ^JCTree$JCContinue x)
                                                  (instance? JCTree$JCYield x) (.target ^JCTree$JCYield x)))))
                      (vswap! found conj x))
                    (doseq [fld (.getFields (class x))
                            :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers ^java.lang.reflect.Field fld)))
                            :let [nm (.getName ^java.lang.reflect.Field fld)]
                            :when (not (#{"type" "sym" "target" "operator" "constructor" "varargsElement"
                                          "constructorType" "polyKind" "pos" "flags"} nm))]
                      (let [v (.get ^java.lang.reflect.Field fld x)]
                        (cond
                          (instance? JCTree v) (walk v)
                          (instance? com.sun.tools.javac.util.List v) (run! walk v)))))))]
      (walk t))
    @found))

(defn- has-break? [t target]
  (seq (jumps-to t target #(instance? JCTree$JCBreak %))))

(defn- has-continue? [t target]
  (seq (jumps-to t target #(instance? JCTree$JCContinue %))))

(declare cn?)

(defn- case-groups-complete? [cases]
  (some (fn [^JCTree$JCCase c]
          (if (= CaseTree$CaseKind/RULE (.caseKind c))
            (let [b (.body c)] (or (not (instance? JCTree$JCStatement b)) (cn? b)))
            false))
        cases))

(defn cn?
  "Can statement `t` complete normally (JLS 14.22, conservatively)?"
  [^JCTree t]
  (condp instance? t
    JCTree$JCReturn false
    JCTree$JCThrow false
    JCTree$JCBreak false
    JCTree$JCContinue false
    JCTree$JCYield false
    JCTree$JCBlock (let [ss (.stats ^JCTree$JCBlock t)] (or (empty? ss) (cn? (last ss))))
    JCTree$JCIf (let [i ^JCTree$JCIf t]
                  (or (nil? (.elsepart i)) (cn? (.thenpart i)) (cn? (.elsepart i))))
    JCTree$JCWhileLoop (or (not (const-true? (.cond ^JCTree$JCWhileLoop t))) (boolean (has-break? t t)))
    JCTree$JCDoWhileLoop (or (not (const-true? (.cond ^JCTree$JCDoWhileLoop t))) (boolean (has-break? t t)))
    JCTree$JCForLoop (or (not (const-true? (.cond ^JCTree$JCForLoop t))) (boolean (has-break? t t)))
    JCTree$JCLabeledStatement (or (cn? (.body ^JCTree$JCLabeledStatement t)) (boolean (has-break? t t)))
    JCTree$JCSwitch (let [s ^JCTree$JCSwitch t
                          cases (.cases s)
                          has-default (or (.hasUnconditionalPattern s)
                                          (some (fn [^JCTree$JCCase c] (some #(instance? JCTree$JCDefaultCaseLabel %) (.labels c))) cases)
                                          (.isExhaustive s))]
                      (or (not has-default)
                          (boolean (has-break? t t))
                          (empty? cases)
                          (let [lc ^JCTree$JCCase (last cases)]
                            (if (= CaseTree$CaseKind/RULE (.caseKind lc))
                              (boolean (case-groups-complete? cases))
                              (.completesNormally lc)))))
    JCTree$JCTry (let [tr ^JCTree$JCTry t]
                   (and (or (cn? (.body tr)) (some #(cn? (.body ^JCTree$JCCatch %)) (.catchers tr)))
                        (or (nil? (.finalizer tr)) (cn? (.finalizer tr)))))
    JCTree$JCSynchronized (cn? (.body ^JCTree$JCSynchronized t))
    true))

;;; ------------------------------------------------------------------------------------------
;;; Statements
;;
;; ctx = {:fall forms, :jumps {key forms}, :vpos key}: :fall is what follows when the
;; statement completes normally (e.g. [(recur ...)] at the end of a loop body); :jumps maps the
;; jump targets that are equivalent to completing normally here ([:return], [:break T],
;; [:continue T], [:yield T]) to the forms that replace such a jump; :vpos names the target
;; whose value this position delivers ([:return] in a method returning a value), so that
;; `return e` here is just e.

(def ctx0 {:fall [] :jumps {} :vpos nil})

(def ^:dynamic *predeclared*
  "Local variables declared in a switch case and used in another case: bound before the switch."
  #{})

(defn- seq-ctx [ctx] ctx0)

(defn- recur-free
  "ctx for the body of try/locking: forms that jump (recur) cannot be inside, so they go after."
  [ctx]
  (let [has-recur? (fn [forms] (and (sequential? forms) (some #(and (seq? %) (#{'recur} (first %))) forms)))]
    {:fall (if (has-recur? (:fall ctx)) [] (:fall ctx))
     :jumps (into {} (remove (fn [[k v]] (has-recur? v)) (:jumps ctx)))
     :vpos (:vpos ctx)
     :after (when (has-recur? (:fall ctx)) (:fall ctx))}))

(defn- label-kw! [env ^JCTree target default]
  (let [lbl (:lbl env)
        t (norm-target target)]
    (or (:kw (get @lbl t))
        (let [kw (keyword default)]
          (swap! lbl assoc t {:kw kw :used false})
          kw))))

(defn- use-label! [env target]
  (let [t (norm-target target)]
    (swap! (:lbl env) update t assoc :used true)
    (:kw (get @(:lbl env) t))))

(defn- label-used? [env target]
  (:used (get @(:lbl env) (norm-target target))))

(defn- wrap-label [env target form]
  (if (label-used? env target)
    (do (note! :form/label)
        (list (h env 'label) (label-kw! env target "L") form))
    form))

(defn- loop-info [env target]
  (first (filter #(identical? (:tree %) (norm-target target)) (:loops env))))

(defn- jump-key [^JCTree t]
  (cond
    (instance? JCTree$JCBreak t) [:break (norm-target (.target ^JCTree$JCBreak t))]
    (instance? JCTree$JCContinue t) [:continue (norm-target (.target ^JCTree$JCContinue t))]
    (instance? JCTree$JCYield t) [:yield (.target ^JCTree$JCYield t)]
    (instance? JCTree$JCReturn t) [:return]))

(defn- ret-value [env ^JCTree$JCReturn t]
  (coerce env (ex env (.expr t)) (:ret env)))

(defn- jump-stmt [env ctx ^JCTree t]
  (let [k (jump-key t)]
    (if (and (contains? (:jumps ctx) k)
             (or (not (instance? JCTree$JCReturn t)) (nil? (.expr ^JCTree$JCReturn t)) (= (:vpos ctx) k))
             (or (not (instance? JCTree$JCYield t)) (= (:vpos ctx) k)))
      (do (note! :flow/elided-jump)
          (cond
            (and (instance? JCTree$JCReturn t) (.expr ^JCTree$JCReturn t))
            (with-hoisting env (.expr ^JCTree$JCReturn t) :before (fn [env e] (coerce env (ex env e) (:ret env))))
            (instance? JCTree$JCYield t) [(coerce env (ex env (.value ^JCTree$JCYield t)) (:yield-type env))]
            :else (get (:jumps ctx) k)))
      (condp instance? t
        JCTree$JCReturn (do (note! :form/return)
                            (if (.expr ^JCTree$JCReturn t)
                              (with-hoisting env (.expr ^JCTree$JCReturn t) :before
                                (fn [env e] (list (h env 'return) (coerce env (ex env e) (:ret env)))))
                              [(list (h env 'return))]))
        JCTree$JCYield (let [y ^JCTree$JCYield t]
                         (note! :form/yield-break)
                         [(list (h env 'break) (use-label! env (.target y))
                                (coerce env (ex env (.value y)) (:yield-type env)))])
        JCTree$JCBreak (let [target (norm-target (.target ^JCTree$JCBreak t))
                             li (loop-info env target)]
                         (note! :form/break)
                         [(if (and li (identical? li (first (:loops env))))
                            (list (h env 'break))
                            (list (h env 'break) (use-label! env target)))])
        JCTree$JCContinue (let [target (norm-target (.target ^JCTree$JCContinue t))
                                li (loop-info env target)
                                innermost? (identical? li (first (:loops env)))
                                lbl (when-not innermost? (use-label! env target))]
                            (note! :form/continue)
                            (case (:kind li)
                              :recur [(apply list (h env 'continue) (concat (when lbl [lbl]) (:args li)))]
                              :do [(list (h env 'break) (use-label! env [:body target]))]
                              [(do-form (concat (:update li)
                                                [(apply list (h env 'continue) (when lbl [lbl]))]))]))))))

(defn- default-value [^Type t]
  (case (tag-name t)
    ("INT" "LONG" "SHORT" "BYTE") 0
    "CHAR" (list 'unchecked-char 0)
    "FLOAT" (list 'float 0.0)
    "DOUBLE" 0.0
    "BOOLEAN" false
    nil))

(def reserved-names
  "Java names that would change the meaning of forms the converter writes if they were locals."
  #{})

(defn- local-sym [env ^Symbol$VarSymbol sym]
  (let [n (str (.name sym))]
    (symbol (cond (= n "") "_"
                  (#{"nil" "true" "false"} n) (str n "_")
                  :else n))))

(defn- add-local [env ^Symbol$VarSymbol sym s]
  (-> env
      (assoc-in [:locals sym] s)
      (update :names conj (name s))))

(def ^:dynamic *local-names*
  "The names of the local variables and parameters of the compilation unit being converted:
  heads that simplify writes are qualified when a local could shadow them."
  #{})

(def ^:dynamic *assigned*
  "The local variables assigned somewhere in the class being converted (set by class-form)."
  #{})

(defn- mutable? [^Symbol$VarSymbol sym]
  (contains? *assigned* sym))

(defn- local-tag
  "The tag for a local of declared type `decl` whose initializer has Arbace type `init-t`."
  [env ^JCTree$JCVariableDecl d init-t]
  (binding [*skip-anns* (mods-type-anns (.mods d))]
   (let [decl (.type (.sym d))
        var? (or (nil? (.vartype d)) (.declaredUsingVar d))]
    (cond
      ;; a type with type annotations of its own
      (and (not var?) (seq (f/items (type-form env decl)))) (type-form env decl)
      (or (= init-t :null) (= init-t :lit-int) (nil? init-t)) (if (and (= init-t :lit-int) (= "LONG" (tag-name decl))) nil
                                                                  (type-form env (if var? (erasure decl) decl)))
      (not (same? init-t decl)) (type-form env (if var? (erasure decl) decl))
      (and (not var?) (generic? decl) (not (instance? Type$TypeVar decl))) (type-form env decl)
      (and (not var?) (instance? Type$TypeVar decl)) (type-form env decl)
      :else nil))))

(defn- binding-for
  "[sym-form init-form env'] for local declaration `d` (with `init` result or nil). In a
  loop binding (`loop?`) a primitive type other than long, double or boolean is always tagged,
  since loop widens untagged int and float locals."
  [env ^JCTree$JCVariableDecl d init & [loop?]]
  (let [sym (.sym d)
        s (local-sym env sym)
        decl (erasure (.type sym))
        const? (and (jt/has-flag? sym Flags/FINAL) (.getConstValue sym))
        mut? (or (nil? init) (mutable? sym))
        init-f (if init (coerce env init decl) (default-value decl))
        init-t (if init (if (identical? init-f (:f init)) (:t init) decl) nil)
        tg (or (local-tag env d init-t)
               (when (and loop? (prim? decl) (not (#{"LONG" "DOUBLE" "BOOLEAN"} (tag-name decl))))
                 (jt/prim-sym decl))
               ;; the compiler unifies the branches of a conditional by their class chain
               ;; only: javac's type (a common interface) is written
               ;; (and an int switch of literals would be a long)
               (when (and init (not (jt/object? decl))
                          (not (#{"LONG" "DOUBLE" "BOOLEAN"} (tag-name decl)))
                          (let [it (TreeInfo/skipParens (.init d))]
                            (or (instance? JCTree$JCConditional it) (instance? JCTree$JCSwitchExpression it))))
                 (if (prim? decl) (jt/prim-sym decl) (type-form env (erasure decl)))))
        sf (apply m+ s (concat (when (and mut? (not const?)) [:mutable])
                               (when const? [:const])
                               (when (jt/has-flag? sym Flags/FINAL) (when-not const? nil))
                               (annotation-items env (.annotations (.mods d)))
                               (when tg [[:tag tg]])))]
    (note! (if mut? :form/mutable-local :form/local))
    (when const? (note! :form/const-local))
    [sf init-f (add-local env sym s)]))

(defn- assigns-only?
  "If statement `t` is an if/else chain whose every branch is the single statement `x = e`
  for variable `sym`, the list of [cond-tree e-tree] with a final [nil e]."
  [^JCTree t sym]
  (letfn [(single [^JCTree s]
            (let [s (if (and (instance? JCTree$JCBlock s) (= 1 (count (.stats ^JCTree$JCBlock s))))
                      (first (.stats ^JCTree$JCBlock s))
                      s)]
              (when (instance? JCTree$JCExpressionStatement s)
                (let [e (TreeInfo/skipParens (.expr ^JCTree$JCExpressionStatement s))]
                  (when (and (instance? JCTree$JCAssign e)
                             (instance? JCTree$JCIdent (.lhs ^JCTree$JCAssign e))
                             (identical? sym (.sym ^JCTree$JCIdent (.lhs ^JCTree$JCAssign e))))
                    (.rhs ^JCTree$JCAssign e))))))
            (chain [^JCTree s]
              (if (instance? JCTree$JCIf s)
                (let [i ^JCTree$JCIf s
                      a (single (.thenpart i))]
                  (when (and a (.elsepart i))
                    (when-let [more (chain (.elsepart i))]
                      (cons [(.cond i) a] more))))
                (when-let [e (single s)] [[nil e]])))]
    (when (instance? JCTree$JCIf t) (chain t))))

(defn- refs-sym? [^JCTree t sym]
  (let [found (volatile! false)]
    (letfn [(walk [x]
              (when (and (not @found) (instance? JCTree x))
                (when (and (instance? JCTree$JCIdent x) (identical? sym (.sym ^JCTree$JCIdent x)))
                  (vreset! found true))
                (doseq [fld (.getFields (class x))
                        :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers ^java.lang.reflect.Field fld)))
                        :let [nm (.getName ^java.lang.reflect.Field fld)]
                        :when (not (#{"type" "sym" "target" "operator" "constructor" "varargsElement" "constructorType" "polyKind"} nm))]
                  (let [v (.get ^java.lang.reflect.Field fld x)]
                    (cond (instance? JCTree v) (walk v)
                          (instance? com.sun.tools.javac.util.List v) (run! walk v))))))]
      (walk t))
    @found))

(defn- cond-chain-form [env pairs ^Type decl]
  (let [[[c e] & more] pairs
        conv (fn [env e] (coerce env (ex env e) decl))]
    (if (nil? c)
      (conv env e)
      (cond-form env c (fn [env'] (conv env' e)) (fn [env'] (cond-chain-form env' more decl))))))

(declare local-class-binding foldable-if? cond-form dup-sides)

(defn stmts
  "Convert the statement sequence `ss` in `ctx` to a vector of forms."
  [env ctx ss]
  (let [ss (vec (remove #(instance? JCTree$JCSkip %) ss))]
    (if (empty? ss)
      (vec (:fall ctx))
      (let [s (first ss)
            more (subvec ss 1)]
        (cond
          ;; a local declared in one switch case and used in another: declared before the
          ;; switch, assigned here
          (and (instance? JCTree$JCVariableDecl s) (contains? *predeclared* (.sym ^JCTree$JCVariableDecl s)))
          (let [d ^JCTree$JCVariableDecl s
                sym (.sym d)
                x (get-in env [:locals sym])]
            (into (if (.init d)
                    [(list 'set! x (coerce env (ex env (.init d)) (erasure (.type sym))))]
                    [])
                  (stmts env ctx more)))
          ;; local variables: a let over the rest of the block
          (instance? JCTree$JCVariableDecl s)
          (let [d ^JCTree$JCVariableDecl s
                sym (.sym d)
                nxt (first more)
                chain (when (and (nil? (.init d)) nxt) (assigns-only? nxt sym))]
            (if chain
              (let [decl (erasure (.type sym))
                    init (cond-chain-form env chain decl)
                    s (local-sym env sym)
                    tg (type-form env (if (.declaredUsingVar d) decl (.type sym)))
                    env' (add-local env sym s)]
                (note! :flow/assigned-by-if)
                [(apply list (h env 'let)
                        [(apply m+ s (concat (when (some #(contains? (assigned-syms %) sym) (subvec more 1)) [:mutable])
                                             (when-not (jt/object? decl) [[:tag tg]])))
                         init]
                        (or (seq (stmts env' ctx (subvec more 1))) [nil]))])
              (let [[sf init-f env'] (binding-for env d (when (.init d) (ex env (.init d))))
                    body (stmts env' ctx more)]
                ;; merge directly nested lets
                (if (and (= 1 (count body)) (seq? (first body)) (= 'let (first (first body))))
                  (let [[_ bv & bbody] (first body)]
                    [(apply list (h env 'let) (into [sf init-f] bv) bbody)])
                  [(apply list (h env 'let) [sf init-f] (if (empty? body) [nil] body))]))))
          ;; local classes: letclass over the rest of the block
          (instance? JCTree$JCClassDecl s)
          (let [[bind env'] (local-class-binding env s)
                body (stmts env' ctx more)
                cs (captured-mutables env s)]
            (note! :form/letclass)
            (cond
              (seq cs)
              (let [f (apply list (h env 'letclass) [bind] (if (empty? body) [nil] body))]
                [(capture-values env s f)])
              (and (= 1 (count body)) (seq? (first body)) (= 'letclass (first (first body))))
              (let [[_ bv & bbody] (first body)]
                [(apply list (h env 'letclass) (into [bind] bv) bbody)])
              :else
              [(apply list (h env 'letclass) [bind] (if (empty? body) [nil] body))]))
          (empty? more) (stmt env ctx s)
          ;; if whose pattern variables are in scope in the rest of the block
          (and (instance? JCTree$JCIf s) (has-patterns? (.cond ^JCTree$JCIf s))
               (let [{:keys [t f]} (pattern-binds (.cond ^JCTree$JCIf s))]
                 (some (fn [v] (some #(refs-sym? % v) more)) (concat t f))))
          (let [i ^JCTree$JCIf s
                {:keys [t]} (pattern-binds (.cond i))
                into-then? (some (fn [v] (some #(refs-sym? % v) more)) t)
                a (.thenpart i) b (.elsepart i)]
            (note! :flow/pattern-scope)
            [(if into-then?
               (cond-form env (.cond i)
                          (fn [e] (do-form (stmts e ctx (into [a] more))))
                          (fn [e] (do-form (stmts e ctx (if b [b] [])))))
               (cond-form env (.cond i)
                          (fn [e] (do-form (stmts e ctx [a])))
                          (fn [e] (do-form (stmts e ctx (if b (into [b] more) more))))))])
          ;; if whose one branch ends in a jump that the rest makes implicit (unless the rest
          ;; would be repeated)
          (and (instance? JCTree$JCIf s) (foldable-if? ctx s)
               (let [ds (dup-sides (.cond ^JCTree$JCIf s))]
                 (not (ds (if (cn? (.thenpart ^JCTree$JCIf s)) :then :else)))))
          (let [i ^JCTree$JCIf s
                a (.thenpart i) b (.elsepart i)]
            (note! :flow/folded-if)
            (if (not (cn? a))
              [(cond-form env (.cond i) (fn [e] (do-form (stmts e ctx [a])))
                          (fn [e] (do-form (stmts e ctx (if b (into [b] more) more)))))]
              [(cond-form env (.cond i) (fn [e] (do-form (stmts e ctx (into [a] more))))
                          (fn [e] (do-form (stmts e ctx [b]))))]))
          :else
          (into (stmt env ctx0 s) (stmts env ctx more)))))))

(defn- ends-in-jump?
  "True when statement `t` ends (on every path that ends) in a jump that `ctx` makes
  implicit."
  [ctx ^JCTree t]
  (condp instance? t
    JCTree$JCBlock (let [ss (.stats ^JCTree$JCBlock t)] (and (seq ss) (ends-in-jump? ctx (last ss))))
    JCTree$JCIf (let [i ^JCTree$JCIf t]
                  (and (.elsepart i)
                       (or (ends-in-jump? ctx (.thenpart i)) (instance? JCTree$JCThrow (.thenpart i)))
                       (or (ends-in-jump? ctx (.elsepart i)) (instance? JCTree$JCThrow (.elsepart i)))
                       (or (ends-in-jump? ctx (.thenpart i)) (ends-in-jump? ctx (.elsepart i)))))
    (if (or (instance? JCTree$JCReturn t) (instance? JCTree$JCBreak t) (instance? JCTree$JCContinue t))
      (let [k (jump-key t)]
        (and (contains? (:jumps ctx) k)
             (or (not (instance? JCTree$JCReturn t)) (nil? (.expr ^JCTree$JCReturn t)) (= (:vpos ctx) k))))
      false)))

(defn- foldable-if? [ctx ^JCTree$JCIf i]
  (let [a (.thenpart i) b (.elsepart i)]
    (or (and (not (cn? a)) (ends-in-jump? ctx a) (or (nil? b) (cn? b)))
        (and b (not (cn? b)) (ends-in-jump? ctx b) (cn? a)))))

(defn- stmt-body
  "Statement `t` converted as a body in `ctx`, as one form."
  [env ctx t]
  (do-form (stmts env ctx [t])))

(declare loop-stmt switch-stmt try-stmt switch-stmt-1)

(defn stmt
  "Convert statement `t` in `ctx` to a vector of forms (including ctx's :fall where `t` can
  complete normally)."
  [env ctx ^JCTree t]
  (if (or (instance? JCTree$JCReturn t) (instance? JCTree$JCBreak t)
          (instance? JCTree$JCContinue t) (instance? JCTree$JCYield t))
    (jump-stmt env ctx t)
   (condp instance? t
    JCTree$JCBlock (stmts env ctx (.stats ^JCTree$JCBlock t))
    JCTree$JCSkip (vec (:fall ctx))
    JCTree$JCExpressionStatement (into (with-hoisting env (.expr ^JCTree$JCExpressionStatement t) :after ex-stmt)
                                       (:fall ctx))
    JCTree$JCIf (let [i ^JCTree$JCIf t]
                  (note! :form/if)
                  (if (has-patterns? (.cond i))
                    [(cond-form env (.cond i)
                                (fn [e] (stmt-body e ctx (.thenpart i)))
                                (fn [e] (if (.elsepart i) (stmt-body e ctx (.elsepart i)) (do-form (:fall ctx)))))]
                    (let [c (test-form env (ex env (.cond i)))
                          a (stmt-body env ctx (.thenpart i))
                          b (if (.elsepart i) (stmt-body env ctx (.elsepart i)) (do-form (:fall ctx)))]
                      [(if (nil? b) (list (h env 'when) c a) (list 'if c a b))])))
    JCTree$JCThrow (do (note! :form/throw)
                       [(list 'throw (coerce env (ex env (.expr ^JCTree$JCThrow t)) (erasure (.type (.expr ^JCTree$JCThrow t)))))])
    JCTree$JCAssert (let [a ^JCTree$JCAssert t]
                      (note! :form/java-assert)
                      (into [(apply list (h env 'java-assert) (test-form env (ex env (.cond a)))
                                    (when (.detail a) [(:f (ex env (.detail a)))]))]
                            (:fall ctx)))
    JCTree$JCWhileLoop (loop-stmt env ctx t nil)
    JCTree$JCDoWhileLoop (loop-stmt env ctx t nil)
    JCTree$JCForLoop (loop-stmt env ctx t nil)
    JCTree$JCEnhancedForLoop (loop-stmt env ctx t nil)
    JCTree$JCLabeledStatement
    (let [l ^JCTree$JCLabeledStatement t
          body (.body l)
          kw (label-kw! env l (str (.label l)))]
      (if (instance? JCTree$JCStatement (norm-target l))
        (let [target (norm-target l)]
          (if (or (instance? JCTree$JCWhileLoop target) (instance? JCTree$JCDoWhileLoop target)
                  (instance? JCTree$JCForLoop target) (instance? JCTree$JCEnhancedForLoop target))
            (loop-stmt env ctx target kw)
            (let [ctx' (assoc-in ctx [:jumps [:break target]] (:fall ctx))
                  forms (stmts env ctx' [target])]
              (if (label-used? env target)
                (do (note! :form/label)
                    [(apply list (h env 'label) kw forms)])
                forms))))
        (stmt env ctx body)))
    JCTree$JCSwitch (switch-stmt env ctx t)
    JCTree$JCTry (try-stmt env ctx t)
    JCTree$JCSynchronized (let [s ^JCTree$JCSynchronized t
                                ctx' (recur-free ctx)]
                            (note! :form/locking)
                            (into [(apply list (h env 'locking)
                                          (coerce env (ex env (.lock s)) (erasure (.type (.lock s))))
                                          (stmts env ctx' (.stats (.body s))))]
                                  (when (cn? t) (:after ctx'))))
    JCTree$JCClassDecl (stmts env ctx [t])
    JCTree$JCVariableDecl (stmts env ctx [t])
    (throw (ex-info (str "unsupported statement " (.getKind t)) {:kind (kind-of t)})))))

;;; ------------------------------------------------------------------------------------------
;;; Loops (§5.7)

(defn- assigned-syms
  "The local variable symbols assigned (=, op=, ++, --) anywhere in tree `t`."
  [^JCTree t]
  (let [found (volatile! #{})]
    (letfn [(lhs-sym [^JCTree e]
              (let [e (TreeInfo/skipParens e)]
                (when (instance? JCTree$JCIdent e) (.sym ^JCTree$JCIdent e))))
            (walk [x]
              (when (instance? JCTree x)
                (cond
                  (instance? JCTree$JCAssign x) (some->> (lhs-sym (.lhs ^JCTree$JCAssign x)) (vswap! found conj))
                  (instance? JCTree$JCAssignOp x) (some->> (lhs-sym (.lhs ^JCTree$JCAssignOp x)) (vswap! found conj))
                  (and (instance? JCTree$JCUnary x)
                       (#{"PREINC" "PREDEC" "POSTINC" "POSTDEC"} (str (.getTag ^JCTree x))))
                  (some->> (lhs-sym (.arg ^JCTree$JCUnary x)) (vswap! found conj)))
                (doseq [fld (.getFields (class x))
                        :when (not (java.lang.reflect.Modifier/isStatic (.getModifiers ^java.lang.reflect.Field fld)))
                        :let [nm (.getName ^java.lang.reflect.Field fld)]
                        :when (not (#{"type" "sym" "target" "operator" "constructor" "varargsElement" "constructorType" "polyKind"} nm))]
                  (let [v (.get ^java.lang.reflect.Field fld x)]
                    (cond (instance? JCTree v) (walk v)
                          (instance? com.sun.tools.javac.util.List v) (run! walk v))))))]
      (walk t))
    @found))

(defn- push-loop [env li] (update env :loops conj li))

(defn- loop-done
  "The forms of a converted loop: `form` (labeled if needed) and ctx's :fall when the loop
  can complete normally."
  [env ctx t form]
  (into [(wrap-label env t form)] (when (cn? t) (:fall ctx))))

(defn- while-loop [env ctx ^JCTree$JCWhileLoop t]
  (if (has-patterns? (.cond t))
    (let [li {:tree t :kind :recur :args []}
          rec '(recur)
          bctx {:fall [rec] :jumps {[:continue t] [rec] [:break t] []} :vpos nil}]
      (note! :form/loop)
      (loop-done env ctx t (list (h env 'loop) []
                                 (cond-form env (.cond t)
                                            (fn [e] (do-form (stmts (push-loop e li) bctx [(.body t)])))
                                            (fn [_] nil)))))
  (let [li {:tree t :kind :while :update []}
        env' (push-loop env li)
        c (test-form env (ex env (.cond t)))
        body (stmts env' {:fall [] :jumps {[:continue t] []} :vpos nil} [(.body t)])]
    (note! :form/while)
    (loop-done env ctx t (apply list (h env 'while) c body)))))

(defn- do-loop [env ctx ^JCTree$JCDoWhileLoop t]
  (let [li {:tree t :kind :do}
        _ (label-kw! env [:body t] "do-body")
        env' (push-loop env li)
        body (stmts env' {:fall [] :jumps {[:continue t] []} :vpos nil} [(.body t)])
        body (if (label-used? env [:body t])
               [(apply list (h env 'label) (label-kw! env [:body t] "do-body") body)]
               body)
        c (test-form env (ex env' (.cond t)))]
    (note! :form/do-while)
    (loop-done env ctx t (apply list (h env 'loop) [] (concat body [(list (h env 'when) c '(recur))])))))

(defn- step-update
  "For a for-loop update expression statement: [VarSymbol tree] when it assigns one local."
  [^JCTree s]
  (when (instance? JCTree$JCExpressionStatement s)
    (let [e (TreeInfo/skipParens (.expr ^JCTree$JCExpressionStatement s))
          lhs (cond
                (instance? JCTree$JCAssign e) (.lhs ^JCTree$JCAssign e)
                (instance? JCTree$JCAssignOp e) (.lhs ^JCTree$JCAssignOp e)
                (and (instance? JCTree$JCUnary e)
                     (#{"PREINC" "PREDEC" "POSTINC" "POSTDEC"} (str (.getTag ^JCTree e))))
                (.arg ^JCTree$JCUnary e))
          lhs (some-> lhs TreeInfo/skipParens)]
      (when (instance? JCTree$JCIdent lhs)
        [(.sym ^JCTree$JCIdent lhs) e]))))

(defn- recur-style? [^JCTree$JCForLoop t]
  (let [init (.init t)
        syms (map #(.sym ^JCTree$JCVariableDecl %) init)
        steps (map step-update (.step t))]
    (and (seq init)
         (every? #(instance? JCTree$JCVariableDecl %) init)
         (every? some? steps)
         (every? (set syms) (map first steps))
         (or (empty? steps) (apply distinct? (map first steps)))
         (let [assigned (into (assigned-syms (.body t)) (when (.cond t) (assigned-syms (.cond t))))]
           (not-any? assigned syms))
         ;; a later update does not read a variable an earlier one updated
         (every? true?
                 (for [[i [s1 _]] (map-indexed vector steps)
                       [_ e2] (drop (inc i) steps)]
                   (not (refs-sym? e2 s1)))))))

(defn- for-loop-patterns
  "A for loop whose condition binds pattern variables used in the body or the update: a
  `loop` testing the condition with if-instance."
  [env ctx ^JCTree$JCForLoop t]
  (let [decls (filter #(instance? JCTree$JCVariableDecl %) (.init t))
        exprs (remove #(instance? JCTree$JCVariableDecl %) (.init t))
        [bv env'] (reduce (fn [[bv env] ^JCTree$JCVariableDecl d]
                            (let [[sf init env'] (binding-for env d (when (.init d) (ex env (.init d))))]
                              [(conj bv sf init) env']))
                          [[] env] decls)
        pre (mapv #(ex-stmt env' (.expr ^JCTree$JCExpressionStatement %)) exprs)
        body-fn (fn [e]
                  (let [update (mapv #(ex-stmt e (.expr ^JCTree$JCExpressionStatement %)) (.step t))
                        next (conj update '(recur))
                        li {:tree t :kind :while :update update}]
                    (do-form (stmts (push-loop e li)
                                    {:fall next :jumps {[:continue t] next [:break t] []} :vpos nil}
                                    [(.body t)]))))
        l (wrap-label env t (list (h env 'loop) [] (cond-form env' (.cond t) body-fn (fn [_] nil))))]
    (note! :form/loop)
    (into (cond
            (seq bv) [(list (h env 'let) bv l)]
            (seq pre) (conj pre l)
            :else [l])
          (when (cn? t) (:fall ctx)))))

(defn- for-loop [env ctx ^JCTree$JCForLoop t]
  (if (and (.cond t) (has-patterns? (.cond t)))
    (for-loop-patterns env ctx t)
  (if (recur-style? t)
    (let [[bv env'] (reduce (fn [[bv env] ^JCTree$JCVariableDecl d]
                              (let [[sf init env'] (binding-for env d (ex env (.init d)) true)
                                    sf (vary-meta sf update ::f/m (fn [items] (vec (remove #{:mutable} items))))]
                                [(conj bv sf init) env']))
                            [[] env] (.init t))
          syms (map #(.sym ^JCTree$JCVariableDecl %) (.init t))
          updates (into {} (for [s (.step t)
                                 :let [[sym e] (step-update s)
                                       f (ex-stmt env' e)]]
                             [sym (nth f 2)]))
          args (vec (for [s syms] (get updates s (get-in env' [:locals s]))))
          rec (apply list 'recur args)
          li {:tree t :kind :recur :args args}
          env'' (push-loop env' li)
          body (stmts env'' {:fall [rec] :jumps {[:continue t] [rec] [:break t] []} :vpos nil} [(.body t)])
          c (when (.cond t) (test-form env' (ex env' (.cond t))))]
      (note! :form/loop)
      (loop-done env ctx t (list (h env 'loop) bv
                                 (if c
                                   (if (= 1 (count body))
                                     (list 'if c (first body) nil)
                                     (apply list (h env 'when) c body))
                                   (do-form body)))))
    (let [decls (filter #(instance? JCTree$JCVariableDecl %) (.init t))
          exprs (remove #(instance? JCTree$JCVariableDecl %) (.init t))
          [bv env'] (reduce (fn [[bv env] ^JCTree$JCVariableDecl d]
                              (let [[sf init env'] (binding-for env d (when (.init d) (ex env (.init d))))]
                                [(conj bv sf init) env']))
                            [[] env] decls)
          pre (mapv #(ex-stmt env' (.expr ^JCTree$JCExpressionStatement %)) exprs)
          update (mapv #(ex-stmt env' (.expr ^JCTree$JCExpressionStatement %)) (.step t))
          li {:tree t :kind :while :update update}
          env'' (push-loop env' li)
          body (stmts env'' {:fall [] :jumps {[:continue t] []} :vpos nil} [(.body t)])
          body (if (or (cn? (.body t)) (has-continue? (.body t) t)) (into body update) body)
          c (if (.cond t) (test-form env' (ex env' (.cond t))) true)
          w (wrap-label env t (apply list (h env 'while) c body))]
      (note! :form/while)
      (into (cond
              (seq bv) [(list (h env 'let) bv w)]
              (seq pre) (conj pre w)
              :else [w])
            (when (cn? t) (:fall ctx)))))))

(defn- for-each-loop [env ctx ^JCTree$JCEnhancedForLoop t]
  (let [d (.var t)
        sym (.sym d)
        s (local-sym env sym)
        vt (.type sym)
        sf (apply m+ s (concat (when (mutable? sym) [:mutable])
                               (annotation-items env (.annotations (.mods d)))
                               (let [tf (binding [*skip-anns* (mods-type-anns (.mods d))]
                                          (type-form env (if (.declaredUsingVar d) (erasure vt) vt)))]
                                 (when-not (and (jt/object? vt) (empty? (f/items tf)))
                                   [[:tag tf]]))))
        coll (coerce env (ex env (.expr t)) (erasure (.type (.expr t))))
        env' (push-loop (add-local env sym s) {:tree t :kind :for-each})
        body (stmts env' {:fall [] :jumps {[:continue t] []} :vpos nil} [(.body t)])]
    (note! :form/for-each)
    (loop-done env ctx t (apply list (h env 'for-each) [sf coll] body))))

(defn loop-stmt [env ctx ^JCTree t kw]
  (when kw (swap! (:lbl env) assoc (norm-target t) {:kw kw :used false}))
  (condp instance? t
    JCTree$JCWhileLoop (while-loop env ctx t)
    JCTree$JCDoWhileLoop (do-loop env ctx t)
    JCTree$JCForLoop (for-loop env ctx t)
    JCTree$JCEnhancedForLoop (for-each-loop env ctx t)))

;;; ------------------------------------------------------------------------------------------
;;; switch (§5.8)

(defn- enum-type? [^Type t]
  (and (instance? Type t) (.tsym t) (jt/has-flag? (.tsym t) Flags/ENUM)))

(defn- legacy-selector? [^Type t]
  (let [t (erasure t)
        u (or (when (prim? t) t) (jt/unboxed t))]
    (or (and u (#{"INT" "SHORT" "BYTE" "CHAR"} (tag-name u)))
        (jt/string-type? t)
        (enum-type? t))))

(defn- const-label [env ^Type sel-type ^JCTree e]
  (let [e (TreeInfo/skipParens e)]
    (cond
      (= "BOT" (tag-name (.type e))) nil
      (enum-type? sel-type) (symbol (str (TreeInfo/name e)))
      (instance? JCTree$JCLiteral e)
      (let [x (literal env e)]
        (if (seq? (:f x)) (.constValue (.type e)) (:f x)))
      (and (or (instance? JCTree$JCIdent e) (instance? JCTree$JCFieldAccess e))
           (let [s (TreeInfo/symbol e)] (and s (= "VAR" (jt/kind s)))))
      (let [x (ex env e)] (if (seq? (:f x)) (.constValue (.type e)) (:f x)))
      :else (let [v (.constValue (.type e))]
              (note! :switch/folded-label)
              (if (= "CHAR" (tag-name (.type e))) (char (int v)) v)))))

(defn- default-label? [l] (instance? JCTree$JCDefaultCaseLabel l))

(defn- null-label? [l]
  (and (instance? JCTree$JCConstantCaseLabel l)
       (= "BOT" (tag-name (.type (.expr ^JCTree$JCConstantCaseLabel l))))))

(defn- case-groups
  "The arms of a switch: [{:labels [JCCaseLabel] :guard tree :stats [stmts] :rule body}]."
  [cases]
  (let [cases (vec cases)
        n (count cases)]
    (loop [i 0 pending [] out []]
      (if (>= i n)
        (if (seq pending) (conj out {:labels pending :stats []}) out)
        (let [c ^JCTree$JCCase (cases i)]
          (if (= CaseTree$CaseKind/RULE (.caseKind c))
            (recur (inc i) [] (conj out {:labels (vec (.labels c)) :guard (.guard c) :rule (.body c)
                                         :case c}))
            (if (and (empty? (.stats c)) (< i (dec n)) (nil? (.guard c))
                     (not-any? #(instance? JCTree$JCPatternCaseLabel %) (.labels c)))
              (recur (inc i) (into pending (.labels c)) out)
              (let [stats (loop [j i acc []]
                            (let [cj ^JCTree$JCCase (cases j)
                                  acc (into acc (.stats cj))]
                              (if (and (.completesNormally cj) (< j (dec n)))
                                (recur (inc j) acc)
                                acc)))]
                (when (and (.completesNormally c) (< i (dec n))) (note! :switch/fall-through))
                (recur (inc i) [] (conj out {:labels (into pending (.labels c)) :guard (.guard c)
                                             :stats stats :case c}))))))))))

(defn- arm-label
  "The label form of an arm and the env for its body (pattern bindings)."
  [env sel-type {:keys [labels guard]}]
  (let [penv (atom env)
        pats (filter #(instance? JCTree$JCPatternCaseLabel %) labels)
        consts (filter #(instance? JCTree$JCConstantCaseLabel %) labels)
        default? (some default-label? labels)]
    (cond
      default? [(if (some null-label? labels) (list nil :default) :default) env]
      (seq pats)
      (let [pf (pattern-form env penv (.pat ^JCTree$JCPatternCaseLabel (first pats)))
            g (when guard (test-form @penv (ex @penv guard)))]
        (when (next pats) (note! :warn/multiple-patterns))
        (note! :switch/pattern)
        [(if g [pf :when g] [pf]) @penv])
      :else
      (let [ls (map #(const-label env sel-type (.expr ^JCTree$JCConstantCaseLabel %)) consts)]
        [(if (= 1 (count ls)) (first ls) (apply list ls)) env]))))

(defn- exhaustive-default? [t expr?]
  (let [cases (if expr? (.cases ^JCTree$JCSwitchExpression t) (.cases ^JCTree$JCSwitch t))
        sel (if expr? (.selector ^JCTree$JCSwitchExpression t) (.selector ^JCTree$JCSwitch t))
        uncond (if expr? (.hasUnconditionalPattern ^JCTree$JCSwitchExpression t)
                   (.hasUnconditionalPattern ^JCTree$JCSwitch t))
        has-default (some (fn [^JCTree$JCCase c] (some default-label? (.labels c))) cases)
        pattern? (if expr? (.patternSwitch ^JCTree$JCSwitchExpression t) (.patternSwitch ^JCTree$JCSwitch t))
        has-null (some (fn [^JCTree$JCCase c] (some null-label? (.labels c))) cases)]
    (and (not has-default) (not uncond)
         (or expr? pattern? has-null (not (legacy-selector? (.type ^JCTree sel)))))))

(defn- selector-form [env ^JCTree sel pattern?]
  (let [x (ex env sel)
        t (:t x)]
    (if (and (not pattern?) (instance? Type t) (not (prim? t)) (jt/unboxed t))
      (coerce env x (jt/unboxed t) true)
      (coerce env x (erasure (.type sel))))))

(def match-exception-default
  (list 'throw (list (f/member (f/cref "java.lang.MatchException") ".") nil nil)))

(defn- split-pattern-groups
  "A case with several patterns (case A _, B _ ->) becomes one arm per pattern."
  [groups]
  (mapcat (fn [g]
            (let [pats (filter #(instance? JCTree$JCPatternCaseLabel %) (:labels g))]
              (if (> (count pats) 1)
                (do (note! :switch/split-patterns)
                    (for [p pats] (assoc g :labels [p])))
                [g])))
          groups))

(defn- switch-arms
  "Convert the arms; `body-fn` converts one arm's body given its env."
  [env sel-type groups body-fn]
  (let [arms (for [g (split-pattern-groups groups)]
               (let [[lbl env'] (arm-label env sel-type g)]
                 [lbl (body-fn env' g)]))
        defaults (filter #(or (= :default (first %)) (and (seq? (first %)) (= :default (second (first %))))) arms)
        others (remove (set defaults) arms)]
    [others (first defaults)]))

(defn- switch-stmt* [env ctx ^JCTree$JCSwitch t]
  (let [sel-type (erasure (.type (.selector t)))
        sel (selector-form env (.selector t) (.patternSwitch t))
        groups (case-groups (.cases t))
        arm-ctx {:fall (:fall ctx) :jumps (assoc (:jumps ctx) [:break t] (:fall ctx)) :vpos (:vpos ctx)}
        body-fn (fn [env' g]
                  (if (:rule g)
                    (do-form (stmts env' arm-ctx [(:rule g)]))
                    (do-form (stmts env' arm-ctx (:stats g)))))
        [arms default] (switch-arms env sel-type groups body-fn)
        default (cond
                  default default
                  (exhaustive-default? t false) (do (note! :switch/match-exception) [:default match-exception-default])
                  (seq (:fall ctx)) [:default (do-form (:fall ctx))]
                  :else nil)
        form (apply list (h env 'switch) sel
                    (concat (mapcat (fn [[l b]] [l b]) arms)
                            (when default
                              (if (= :default (first default))
                                [(second default)]
                                [(first default) (second default)]))))]
    (note! :form/switch)
    form))

(defn- cross-case-locals
  "The local declarations at the top level of one case of switch `t` that another case uses."
  [^JCTree$JCSwitch t]
  (let [cases (vec (.cases t))]
    (for [[i ^JCTree$JCCase c] (map-indexed vector cases)
          d (.stats c)
          :when (instance? JCTree$JCVariableDecl d)
          :let [sym (.sym ^JCTree$JCVariableDecl d)]
          :when (some (fn [[j ^JCTree$JCCase c2]]
                        (and (not= i j) (some #(refs-sym? % sym) (.stats c2))))
                      (map-indexed vector cases))]
      d)))

(defn switch-stmt [env ctx ^JCTree$JCSwitch t]
  (let [pre (cross-case-locals t)]
    (if (seq pre)
      (let [[bv env'] (reduce (fn [[bv env] ^JCTree$JCVariableDecl d]
                                (let [[sf init env'] (binding-for env d nil)]
                                  [(conj bv sf init) env']))
                              [[] env] pre)]
        (note! :switch/predeclared-local)
        (binding [*predeclared* (into *predeclared* (map #(.sym ^JCTree$JCVariableDecl %) pre))]
          [(apply list (h env 'let) bv (switch-stmt-1 env' ctx t))]))
      (switch-stmt-1 env ctx t))))

(defn- switch-stmt-1 [env ctx ^JCTree$JCSwitch t]
  (label-kw! env t "switch")
  (let [form (switch-stmt* env ctx t)]
    (if (and (label-used? env t) (seq (:fall ctx)))
      ;; an explicit break leaves the switch: the fall-through forms go after it
      (do (swap! (:lbl env) assoc-in [(norm-target t) :used] false)
          (let [form (switch-stmt* env ctx0 t)]
            (into [(wrap-label env t form)] (when (cn? t) (:fall ctx)))))
      [(wrap-label env t form)])))

(defn switch-form
  "A switch expression."
  [env ^JCTree$JCSwitchExpression t _]
  (label-kw! env t "switch")
  (let [ty (erasure (.type t))
        sel-type (erasure (.type (.selector t)))
        sel (selector-form env (.selector t) (.patternSwitch t))
        groups (case-groups (.cases t))
        k [:yield t]
        arm-ctx {:fall [] :jumps {k :value} :vpos k}
        body-fn (fn [env' g]
                  (let [env' (assoc env' :yield-type ty)
                        b (:rule g)]
                    (cond
                      (and b (instance? JCTree$JCExpression b))
                      (let [x (ex env' b)]
                        (coerce env' x ty true))
                      b (do-form (stmts env' arm-ctx [b]))
                      :else (do-form (stmts env' arm-ctx (:stats g))))))
        [arms default] (switch-arms env sel-type groups body-fn)
        default (or default
                    (when (exhaustive-default? t true)
                      (note! :switch/match-exception)
                      [:default match-exception-default]))
        form (apply list (h env 'switch) sel
                    (concat (mapcat (fn [[l b]] [l b]) arms)
                            (when default
                              (if (= :default (first default))
                                [(second default)]
                                [(first default) (second default)]))))]
    (note! :form/switch-expression)
    (r (wrap-label env t form) ty)))

;;; ------------------------------------------------------------------------------------------
;;; try, with-resources (§5.9)

(defn- catch-form [env ctx ^JCTree$JCCatch c]
  (let [p (.param c)
        sym (.sym p)
        s (local-sym env sym)
        vt (.vartype p)
        types (if (instance? com.sun.tools.javac.tree.JCTree$JCTypeUnion vt)
                (mapv #(code-type-form env (.type ^JCTree %)) (.alternatives ^com.sun.tools.javac.tree.JCTree$JCTypeUnion vt))
                (binding [*skip-anns* (mods-type-anns (.mods p))] (code-type-form env (.type sym))))
        sf (apply m+ s (concat (when (mutable? sym) [:mutable]) (annotation-items env (.annotations (.mods p)))))
        env' (add-local env sym s)]
    (note! (if (vector? types) :form/multi-catch :form/catch))
    (apply list 'catch types sf (stmts env' ctx (.stats (.body c))))))

(defn try-stmt [env ctx ^JCTree$JCTry t]
  (let [ctx' (recur-free ctx)
        ctx' (if (:after ctx') (assoc ctx' :jumps {} :vpos nil) ctx')
        resources (.resources t)
        [rbinds env'] (reduce (fn [[bv env] ^JCTree res]
                                (if (instance? JCTree$JCVariableDecl res)
                                  (let [d ^JCTree$JCVariableDecl res
                                        [sf init env'] (binding-for env d (ex env (.init d)))
                                        sf (vary-meta sf update ::f/m (fn [items] (vec (remove #{:mutable} items))))]
                                    [(conj bv sf init) env'])
                                  (let [x (ex env res)
                                        nm (or (some-> (TreeInfo/symbol res) .name str) "r")
                                        s (if (and (symbol? (:f x)) (not (namespace (:f x)))) (:f x) (gensym! env nm))]
                                    [(conj bv s (:f x)) env])))
                              [[] env] resources)
        body (stmts env' ctx' (.stats (.body t)))
        body (if (seq resources)
               (do (note! :form/with-resources)
                   [(apply list (h env 'with-resources) rbinds body)])
               body)
        catches (map #(catch-form env ctx' %) (.catchers t))
        fin (when (.finalizer t)
              (note! :form/finally)
              (apply list 'finally (stmts env ctx0 (.stats (.finalizer t)))))
        form (if (or (seq catches) fin)
               (do (note! :form/try)
                   (apply list 'try (concat body catches (when fin [fin]))))
               (do-form body))]
    (into [form] (when (and (:after ctx') (cn? t)) (:after ctx')))))

;;; ------------------------------------------------------------------------------------------
;;; Lambdas and method references (§5.12)

(defn- fi-form [env ^Type target]
  (if-let [cs (jt/components target)]
    (apply list '& (map #(erased-form env %) cs))
    (erased-form env target)))

(defn- descriptor ^Type [^Type target]
  (.findDescriptorType jt/*types* target))

(defn lambda-form [env ^JCTree$JCLambda t]
  (let [target (or (.target t) (.type t))
        desc (descriptor target)
        rt (erasure (.getReturnType desc))
        void? (jt/void? rt)
        [params env'] (reduce (fn [[ps env] ^JCTree$JCVariableDecl d]
                                (let [sym (.sym d)
                                      s (local-sym env sym)
                                      pt (erasure (.type sym))
                                      sf (apply m+ s (concat (when (mutable? sym) [:mutable])
                                                             (annotation-items env (.annotations (.mods d)))
                                                             (when-not (jt/object? pt) [[:tag (erased-form env pt)]])))]
                                  [(conj ps sf) (add-local env sym s)]))
                              [[] env] (.params t))
        params (cond-> params
                 (not (jt/object? rt)) (m+ [:tag (erased-form env rt)]))
        env' (assoc env' :ret (when-not void? rt) :loops () :yield-type nil)
        body (.body t)
        forms (if (instance? JCTree$JCExpression body)
                [(if void? (ex-stmt env' body) (coerce env' (ex env' body) rt))]
                (stmts env' {:fall [] :jumps {[:return] []} :vpos (when-not void? [:return])}
                       (.stats ^JCTree$JCBlock body)))]
    (note! :form/lambda)
    (r (apply list (h env 'lambda) (fi-form env target) params forms) (erasure target))))

(defn method-ref-form [env ^JCTree$JCMemberReference t]
  (let [target (or (.target t) (.type t))
        desc (descriptor target)
        sam (.findDescriptorSymbol jt/*types* (.tsym (erasure (if-let [cs (jt/components target)] (first cs) target))))
        sam-t (.erasure ^Symbol sam jt/*types*)
        inst-ps (map erasure (.getParameterTypes desc))
        inst-r (erasure (.getReturnType desc))
        sig (when-not (and (every? true? (map same? inst-ps (.getParameterTypes sam-t)))
                           (= (count inst-ps) (count (.getParameterTypes sam-t)))
                           (same? inst-r (.getReturnType sam-t)))
              (cond-> (mapv #(erased-form env %) inst-ps)
                (not (same? inst-r (.getReturnType sam-t))) (m+ [:tag (erased-form env inst-r)])))
        kind (str (.kind t))
        m ^Symbol (.sym t)
        qt (erasure (.type (.expr t)))
        fi (fi-form env target)]
    (note! :form/method-ref)
    (if (= kind "ARRAY_CTOR")
      (let [n (gensym! env "n")]
        (note! :form/array-ctor-ref)
        (r (list (h env 'lambda) fi (m+ (tag [(tag n 'int)] (erased-form env qt)) :method-ref)
                 (list 'new (erased-form env qt) n))
           (erasure target)))
      (let [ctor? (#{"IMPLICIT_INNER" "TOPLEVEL"} kind)
            owner (if (instance? Type$ArrayType qt) qt qt)
            overloaded? (> (count (methods-named env owner (str (.name m)) (boolean ctor?))) 1)
            msym (cond
                   (and (not ctor?) (jt/class-sym? (.tsym qt)) (jt/anonymous? (.tsym qt)))
                   (do (note! :form/method-ref-anonymous) (symbol (str "." (.name m))))
                   ctor? (member (class-ref env (.tsym qt)) "new")
                   (= kind "STATIC") (member (class-ref env (.tsym qt)) (str (.name m)))
                   (instance? Type$ArrayType qt) (member (erased-form env qt) (str "." (.name m)))
                   :else (member (class-ref env (.tsym qt)) (str "." (.name m))))
            ;; the compiler resolves the method with the instantiated parameter types (§5.12)
            pin? (cond
                   (not overloaded?) false
                   (or (= kind "SUPER") (instance? Type$ArrayType qt) (not (jt/class-sym? (.tsym qt)))
                       (and (not ctor?) (jt/anonymous? (.tsym qt))))
                   true
                   :else
                   (let [stubs (map #(res/arg-node % nil) inst-ps)
                         va (some? (.varargsElement t))]
                     (case kind
                       ("IMPLICIT_INNER" "TOPLEVEL") (pin-needed? :ctor (current-class env) (.tsym qt) m stubs va)
                       "STATIC" (pin-needed? :static (current-class env) (.tsym qt) m stubs va)
                       "BOUND" (pin-needed? :instance (current-class env) qt m stubs va)
                       "UNBOUND" (pin-needed? :instance (current-class env) qt m (rest stubs) va)
                       true)))
            msym (if pin? (m+ msym (param-tags env m)) msym)
            ;; type annotations of the qualifying type and of explicit type arguments (§4.4)
            qtype (when (#{"STATIC" "UNBOUND" "IMPLICIT_INNER" "TOPLEVEL"} kind) (.type (.expr t)))
            msym (cond-> msym
                   (type-annotated? qtype) (m+ [:qualifier (type-form env qtype)])
                   (some #(type-annotated? (.type ^JCTree %)) (.typeargs t))
                   (m+ [:type-args (mapv #(type-form env (.type ^JCTree %)) (.typeargs t))]))
            recv (case kind
                   "BOUND" [(coerce env (ex env (.expr t)) qt)]
                   "SUPER" [(:f (ex env (.expr t)))]
                   nil)]
        (r (apply list (h env 'method-ref) fi (concat (when sig [sig]) recv [msym])) (erasure target))))))

;;; ------------------------------------------------------------------------------------------
;;; Classes (§4)

(declare class-body)

(defn- class-env
  "The env inside the body of class `c`."
  [env ^Symbol$ClassSymbol c]
  (let [members (for [^Symbol s (.getSymbols (.members c))
                      :when (jt/class-sym? s)]
                  s)
        fields (for [^Symbol s (.getSymbols (.members c))
                     :when (= "VAR" (jt/kind s))]
                 (str (.name s)))
        tvars (for [^Type tv (.getTypeArguments (.type c))] (str (.getSimpleName (.tsym tv))))]
    (-> env
        (update :classes conj c)
        (update :scope (fn [sc] (merge sc
                                       (into {} (for [^Symbol s members] [(str (.getSimpleName s)) s]))
                                       (into {} (for [tv tvars] [tv :tvar])))))
        (update :fields into fields))))

(defn- type-params [env tps]
  (when (seq tps)
    (note! :form/type-params)
    (vec (for [^JCTree$JCTypeParameter tp tps]
           (let [nm (symbol (str (.name tp)))
                 nm (apply m+ nm (annotation-items env (.annotations tp)))
                 bounds (.bounds tp)]
             (if (empty? bounds)
               nm
               (apply list nm 'extends (map #(type-form env (.type ^JCTree %)) bounds))))))))

(defn- method-env [env ^Symbol$MethodSymbol m]
  (let [tvars (for [^Type tv (.getTypeArguments (.type m))] (str (.getSimpleName (.tsym tv))))]
    (update env :scope merge (into {} (for [tv tvars] [tv :tvar])))))

(defn- inherited-candidates
  "Non-private methods of the supertypes of `c` named like `m` with its arity."
  [^Symbol$ClassSymbol c ^Symbol$MethodSymbol m]
  (let [n (count (.getParameterTypes (.type m)))
        sups (rest (.closure jt/*types* (.type c)))
        sups (if (.isInterface c) (concat sups [(jt/object-type)]) sups)]
    (->> (for [^Type ty sups
               :when (instance? Type$ClassType ty)
               ^Symbol s (.getSymbolsByName (.members (.tsym ty)) (.name m))
               :when (and (= "MTH" (jt/kind s))
                          (not (jt/static? s))
                          (zero? (bit-and (.flags s) (long (bit-or Flags/PRIVATE Flags/SYNTHETIC))))
                          (= n (count (.getParameterTypes (.type s)))))]
           s)
         (map (fn [s] [(sig-key s) (str (ret-type s))]))
         distinct)))

(defn- omit-object-tags?
  "Whether `Object` types of method `m` may be left out (§4.6: an untyped method takes the
  signature of a unique inherited method with its name and arity)."
  [^Symbol$ClassSymbol c ^Symbol$MethodSymbol m]
  (let [ps (erased-params m)
        rt (ret-type m)
        all-object? (and (every? jt/object? ps) (jt/object? rt))]
    (if-not all-object?
      true
      (let [cands (inherited-candidates c m)]
        (or (empty? cands)
            (and (= 1 (count cands))
                 (let [[ks r] (first cands)]
                   (and (every? #(= "java.lang.Object" %) ks) (= "java.lang.Object" r)))))))))

(defn- implicit-super-call? [^JCTree s]
  (when (instance? JCTree$JCExpressionStatement s)
    (let [e (.expr ^JCTree$JCExpressionStatement s)]
      (and (instance? JCTree$JCMethodInvocation e)
           ;; o.super() is never implicit
           (instance? JCTree$JCIdent (.meth ^JCTree$JCMethodInvocation e))
           (= "super" (str (TreeInfo/name (.meth ^JCTree$JCMethodInvocation e))))
           (empty? (.args ^JCTree$JCMethodInvocation e))
           (let [p (TreeInfo/getStartPos s)]
             (or (neg? p) (>= p (count *text*)) (not= \s (.charAt ^String *text* p))))))))

(defn- param-forms
  "[params-vector env'] for method tree `md`."
  [env ^JCTree$JCMethodDecl md omit-object?]
  (let [m (.sym md)
        varargs? (jt/has-flag? m Flags/VARARGS)
        ps (vec (.params md))
        n (count ps)]
    (reduce (fn [[pv env] [i ^JCTree$JCVariableDecl d]]
              (let [sym (.sym d)
                    s (local-sym env sym)
                    pt (.type sym)
                    sf (apply m+ s (concat (when (jt/has-flag? sym Flags/FINAL) [:final])
                                           (when (and (mutable? sym) (not (jt/has-flag? sym Flags/FINAL))) [:mutable])
                                           (annotation-items env (.annotations (.mods d)))
                                           (when-let [tg (binding [*skip-anns* (mods-type-anns (.mods d))]
                                                           (decl-tag env pt omit-object?))]
                                             [tg])))
                    pv (if (and varargs? (= i (dec n))) (conj pv '& sf) (conj pv sf))]
                (when (and (mutable? sym) (not (jt/has-flag? sym Flags/FINAL))) (note! :form/mutable-param))
                [pv (add-local env sym s)]))
            [[] env] (map-indexed vector ps))))

(def unreadable-names
  "Java identifiers that Clojure reads as literals, not symbols."
  #{"nil" "true" "false"})

(defn- member-name
  "The symbol naming a declared member: its Java name, or, for a name that reads as a Clojure
  literal (javac's List.nil()), qualified by the simple name of its class: List/nil."
  [^Symbol c ^String n]
  (if (unreadable-names n)
    (do (note! :form/qualified-member-name) (symbol (str (.getSimpleName c)) n))
    (symbol n)))

(defn- method-form [env ^Symbol$ClassSymbol c ^JCTree$JCMethodDecl md]
  (let [m (.sym md)
        ctor? (= "<init>" (str (.name md)))
        static? (jt/static? m)
        compact? (jt/has-flag? m Flags/COMPACT_RECORD_CONSTRUCTOR)
        env (method-env env m)
        omit? (or ctor? (omit-object-tags? c m))
        ;; the receiver of an anonymous class's method that a nested class refers to gets
        ;; its own name, since the class has none to write Outer/this with
        recv (if (and (not static?) (jt/anonymous? c) (.body md)
                      (some #(instance? JCTree$JCClassDecl %) (all-trees (.body md))))
               (symbol (str "this" (count (filter jt/anonymous? (:classes env)))))
               'this)
        env (if (= recv 'this) env (assoc-in env [:anon-this c] recv))
        [pv env'] (if compact? [[] env] (param-forms env md omit?))
        ;; a receiver parameter's annotations go on the receiver symbol (§4.6)
        recv-sym (if-let [rp (.recvparam md)]
                   (apply m+ recv (annotation-items env (.annotations (.mods rp))))
                   recv)
        pv (if static? pv (into [recv-sym] pv))
        rt (.getReturnType (.type m))
        mods (concat (modifier-items env (.mods md)) [(deprecated-item m (.mods md))]
                     (when compact? [:compact]))
        pv (if ctor?
             (apply m+ pv mods)
             (if (or (jt/void? rt) (not (and omit? (jt/object? rt))) (instance? Type$TypeVar rt))
               (m+ pv [:tag (binding [*skip-anns* (mods-type-anns (.mods md))] (type-form env rt))])
               pv))
        opts (concat (when-let [tp (type-params env (.typarams md))] [:type-params tp])
                     (when (seq (.thrown md))
                       (note! :form/throws)
                       [:throws (mapv #(type-form env (.type ^JCTree %)) (.thrown md))])
                     (when-let [d (.defaultValue m)]
                       (note! :form/annotation-default)
                       [:default (attr-value env d nil)]))
        void? (or ctor? (jt/void? rt))
        env' (cond-> (assoc env' :this (when-not static? recv) :ret (when-not void? (erasure rt))
                            :loops () :yield-type nil)
               (not static?) (update :names conj (name recv)))
        compact-env (if compact?
                      (reduce (fn [e ^Symbol$VarSymbol p] (add-local e p (symbol (str (.name p))))) env' (.params m))
                      env')
        body (when-let [b (.body md)]
               (let [ss (vec (.stats b))
                     ss (if (and ctor? (seq ss) (implicit-super-call? (first ss))) (subvec ss 1) ss)]
                 (stmts compact-env {:fall [] :jumps {[:return] []} :vpos (when-not void? [:return])} ss)))
        nm (apply m+ (member-name c (str (.name md))) (when-not ctor? mods))]
    (note! (if ctor? :form/constructor :form/method))
    (if ctor?
      (apply list 'constructor (concat opts [pv] body))
      (apply list 'method nm (concat opts [pv] body)))))

(defn- field-form [env ^JCTree$JCVariableDecl d]
  (let [sym (.sym d)
        static? (jt/static? sym)
        env' (assoc env :this (when-not static? 'this) :ret nil :loops ())
        nm (apply m+ (member-name (.owner sym) (str (.name d)))
                  (concat (modifier-items env (.mods d)) [(deprecated-item sym (.mods d))]
                          [(binding [*skip-anns* (mods-type-anns (.mods d))] (decl-tag env (.type sym) true))]))]
    (note! :form/field)
    (if (.init d)
      (list 'field nm (coerce env' (ex env' (.init d)) (erasure (.type sym))))
      (list 'field nm))))

(defn- anon-super-ctor
  "The superclass constructor that the constructor javac made for anonymous class `cd` calls,
  else `fallback`."
  ^Symbol$MethodSymbol [^JCTree$JCClassDecl cd fallback]
  (or (some (fn [d]
              (when (and (instance? JCTree$JCMethodDecl d) (= "<init>" (str (.name ^JCTree$JCMethodDecl d)))
                         (.body ^JCTree$JCMethodDecl d))
                (some (fn [st]
                        (when (instance? JCTree$JCExpressionStatement st)
                          (let [e (.expr ^JCTree$JCExpressionStatement st)]
                            (when (instance? JCTree$JCMethodInvocation e)
                              (let [s (TreeInfo/symbol (.meth ^JCTree$JCMethodInvocation e))]
                                (when (and (instance? Symbol$MethodSymbol s) (= "<init>" (str (.name ^Symbol s))))
                                  s))))))
                      (.stats (.body ^JCTree$JCMethodDecl d)))))
            (.defs cd))
      fallback))

(defn- enum-constant [env ^JCTree$JCVariableDecl d]
  (let [sym (.sym d)
        nm (apply m+ (member-name (.owner sym) (str (.name d))) (concat (annotation-items env (.annotations (.mods d)))
                                                     [(deprecated-item sym (.mods d))]))
        nc ^JCTree$JCNewClass (.init d)
        m (if (.def nc) (anon-super-ctor (.def nc) (.constructor nc)) (.constructor nc))
        ve (.varargsElement nc)
        loose? (and ve (erased-varargs-elem? m ve))
        [afs _ nodes] (call-args env m (.constructorType nc) (.args nc) ve loose?)
        pin? (pin-needed? :ctor (.owner sym) (.owner sym) m nodes loose?)]
    (note! :form/enum-constant)
    (if (or (seq (.args nc)) (.def nc) pin?)
      (apply list nm (cond-> (vec afs) pin? (m+ (param-tags env m))) (when (.def nc)
                                 (note! :form/enum-constant-body)
                                 (class-body (class-env env (.sym (.def nc))) (.def nc))))
      nm)))

(defn- generated? [^JCTree d]
  (cond
    (instance? JCTree$JCMethodDecl d)
    (let [m (.sym ^JCTree$JCMethodDecl d)]
      (or (jt/has-flag? m Flags/GENERATEDCONSTR)
          (jt/has-flag? m Flags/ANONCONSTR)
          (jt/has-flag? m Flags/GENERATED_MEMBER)
          (and (jt/has-flag? m Flags/RECORD) (jt/has-flag? m Flags/GENERATED_MEMBER))))
    (instance? JCTree$JCVariableDecl d)
    (jt/has-flag? (.sym ^JCTree$JCVariableDecl d) Flags/GENERATED_MEMBER)
    :else false))

(defn class-body
  "The member forms of class tree `cd` (env already inside the class)."
  [env ^JCTree$JCClassDecl cd]
  (let [c (.sym cd)
        record? (jt/has-flag? c Flags/RECORD)
        defs (remove generated? (.defs cd))
        consts (filter #(and (instance? JCTree$JCVariableDecl %)
                             (jt/has-flag? (.sym ^JCTree$JCVariableDecl %) Flags/ENUM)) defs)
        members (for [d defs
                      :when (not (some #(identical? d %) consts))
                      :when (not (and record? (instance? JCTree$JCVariableDecl d)
                                      (jt/has-flag? (.sym ^JCTree$JCVariableDecl d) Flags/RECORD)))]
                  (condp instance? d
                    JCTree$JCVariableDecl (field-form env d)
                    JCTree$JCMethodDecl (method-form env c d)
                    JCTree$JCBlock (let [b ^JCTree$JCBlock d
                                         static? (not (zero? (bit-and (.flags b) (long Flags/STATIC))))
                                         env' (assoc env :this (when-not static? 'this) :ret nil :loops ())]
                                     (note! (if static? :form/static-initializer :form/initializer))
                                     (apply list (if static? 'static-initializer 'initializer)
                                            (stmts env' ctx0 (.stats b))))
                    JCTree$JCClassDecl (class-form env d)
                    (throw (ex-info (str "unsupported member " (.getKind ^JCTree d)) {}))))]
    (concat (when (seq consts) [(apply list 'constants (map #(enum-constant env %) consts))])
            members)))

(defn- class-header
  "[name-with-meta record-components? options] for class tree `cd`."
  [env ^JCTree$JCClassDecl cd]
  (let [c ^Symbol$ClassSymbol (.sym cd)
        k (str (.getKind cd))
        kind-kw ({"INTERFACE" :interface "ENUM" :enum "RECORD" :record "ANNOTATION_TYPE" :annotation} k)
        mods (concat (modifier-items env (.mods cd) :drop (when (#{"INTERFACE" "ANNOTATION_TYPE"} k) [:abstract]))
                     [(deprecated-item c (.mods cd))] [kind-kw])
        nm (apply m+ (symbol (str (.name cd))) mods)
        ext (.extending cd)
        impl (seq (.implementing cd))
        perm (seq (.permitting cd))
        comps (when (= k "RECORD")
                (vec (for [d (.defs cd)
                           :when (and (instance? JCTree$JCVariableDecl d)
                                      (jt/has-flag? (.sym ^JCTree$JCVariableDecl d) Flags/RECORD))]
                       (let [d ^JCTree$JCVariableDecl d]
                         (apply m+ (symbol (str (.name d)))
                                (concat (annotation-items env (.annotations (.mods d)))
                                        [(binding [*skip-anns* (mods-type-anns (.mods d))]
                                           (decl-tag env (.type (.sym d)) true))]))))))
        ;; a variable arity record: & before the last component
        comps (if (and (seq comps)
                       (some-> (last (seq (.getRecordComponents ^Symbol$ClassSymbol (.sym cd))))
                               (.isVarargs)))
                (conj (pop comps) '& (peek comps))
                comps)
        opts (concat
              (when (and ext (not (#{"INTERFACE" "ANNOTATION_TYPE"} k)))
                [:extends (type-form env (.type ^JCTree ext))])
              (when impl
                [(if (#{"INTERFACE" "ANNOTATION_TYPE"} k) :extends :implements)
                 (mapv #(type-form env (.type ^JCTree %)) impl)])
              (when perm [:permits (mapv #(type-form env (.type ^JCTree %)) perm)])
              (when-let [tp (type-params env (.typarams cd))] [:type-params tp]))]
    (note! (keyword "class" (or (some-> kind-kw name) "class")))
    [nm comps opts]))

(defn class-form
  "A defclass form for member or top-level class tree `cd`."
  [env ^JCTree$JCClassDecl cd]
  (let [[nm comps opts] (class-header env cd)
        env' (class-env (assoc env :this nil) (.sym cd))]
    (apply list 'defclass nm (concat (when comps [comps]) opts (class-body env' cd)))))

(defn local-class-binding
  "[(Name ...) env'] for a local class declaration."
  [env ^JCTree$JCClassDecl cd]
  (let [c (.sym cd)
        env' (update env :scope assoc (str (.getSimpleName c)) c)
        [nm comps opts] (class-header env' cd)
        inner (class-env (assoc env' :this nil) c)]
    [(with-meta (apply list nm (concat (when comps [comps]) opts (class-body inner cd)))
       {:class-body? true})
     env']))

(defn anon-form [env ^JCTree$JCNewClass t]
  (let [cd (.def t)
        c ^Symbol$ClassSymbol (.sym cd)
        ifaces (seq (.getInterfaces c))
        super (if ifaces (first ifaces) (.getSuperclass c))
        m (anon-super-ctor cd (.constructor t))
        ;; Java's o.new Inner(args) {...} (§4.8): javac's constructor type has the outer
        ;; instance as first parameter
        ctype (let [ct (.constructorType t)]
                (if (and (.encl t) (instance? Type$MethodType ct))
                  (Type$MethodType. (.tail (.getParameterTypes ct)) (.getReturnType ct)
                                    (.getThrownTypes ct) (.tsym ct))
                  ct))
        ve (.varargsElement t)
        loose? (and ve (erased-varargs-elem? m ve))
        [afs _ nodes] (call-args env m ctype (.args t) ve loose?)
        encl (when (.encl t) (coerce env (ex env (.encl t)) (erasure (.type (.encl t)))))
        pin? (and (not ifaces)
                  (pin-needed? :ctor c (.tsym (erasure super)) m nodes loose?))
        inner (class-env (assoc env :this nil) c)]
    (note! :form/anon)
    (when encl (note! :form/anon-outer))
    (r (apply list (h env 'anon)
              ;; Java's diamond: javac gives the constructor of an anonymous class of an
              ;; interface no Signature then (§4.8)
              (cond-> (type-form env super)
                (and ifaces (TreeInfo/isDiamond t)) (m+ (do (note! :form/anon-diamond) :diamond)))
              (cond-> (vec afs) pin? (m+ (param-tags env m)))
              (concat (when encl [:outer encl]) (class-body inner cd)))
       (erasure (.type t)))))

;;; ------------------------------------------------------------------------------------------
;;; Modules and packages (§4.14)

(defn- full-name
  "A class named by its binary name, as module declarations name them."
  [^Type t]
  (symbol (jt/binary-name (.tsym t))))

(defn- module-form [env ^JCTree$JCModuleDecl md]
  (note! :form/defmodule)
  (let [nm (apply m+ (symbol (str (TreeInfo/fullName (.qualId md))))
                  (concat (when (= "OPEN" (str (.getModuleType md))) [:open])
                          (annotation-items env (.annotations (.mods md)))))
        name-of (fn [^JCTree e] (symbol (str (TreeInfo/fullName e))))
        dirs (for [d (.directives md)]
               (condp instance? d
                 JCTree$JCRequires (let [rq ^JCTree$JCRequires d]
                                     (list 'requires (apply m+ (name-of (.moduleName rq))
                                                            (concat (when (.isTransitive rq) [:transitive])
                                                                    (when (.isStaticPhase rq) [:static])))))
                 JCTree$JCExports (let [e ^JCTree$JCExports d]
                                    (apply list 'exports (name-of (.qualid e))
                                           (when (seq (.moduleNames e)) [:to (mapv name-of (.moduleNames e))])))
                 JCTree$JCOpens (let [e ^JCTree$JCOpens d]
                                  (apply list 'opens (name-of (.qualid e))
                                         (when (seq (.moduleNames e)) [:to (mapv name-of (.moduleNames e))])))
                 JCTree$JCUses (list 'uses (full-name (.type (.qualid ^JCTree$JCUses d))))
                 JCTree$JCProvides (let [p ^JCTree$JCProvides d]
                                     (list 'provides (full-name (.type (.serviceName p)))
                                           :with (mapv #(full-name (.type ^JCTree %)) (.implNames p))))))]
    (apply list 'defmodule nm dirs)))

;;; ------------------------------------------------------------------------------------------
;;; Compilation units

(defn base-env [opts]
  {:classes () :scope {} :locals {} :names #{} :fields #{} :this nil :ret nil :loops ()
   :lbl (atom {}) :counter (atom 0)})

(defn- count-kinds!
  "Count the tree kinds of `t` (for the coverage report)."
  [^JCTree t]
  (when *stats*
    (letfn [(walk [x]
              (when (instance? JCTree x)
                (when-not (instance? com.sun.tools.javac.tree.JCTree$TypeBoundKind x) (note! (keyword "kind" (kind-of x))))
                (run! walk (tree-children x))))]
      (walk t))))

(defn convert-unit
  "Convert compilation unit `cu`. Returns {:package p :forms [..] :file name :classes [names]}."
  [^JCTree$JCCompilationUnit cu opts]
  (count-kinds! cu)
  (binding [*text* (str (.getCharContent (.getSourceFile cu) true))
            *assigned* (into #{} (mapcat assigned-syms) (.getTypeDecls cu))
            *local-names* (into #{} (for [d (.getTypeDecls cu)
                                          x (tree-seq #(instance? JCTree %) tree-children d)
                                          :when (and (instance? JCTree$JCVariableDecl x)
                                                     (some-> (.sym ^JCTree$JCVariableDecl x) local?))]
                                      (str (.name ^JCTree$JCVariableDecl x))))]
    (let [env (base-env opts)
          pkg (some-> (.getPackage cu) .packge)
          pkg-name (if pkg (jt/package-name pkg) "")
          file (.getName (java.io.File. (.getName (.getSourceFile cu))))
          forms (vec (for [d (.getTypeDecls cu)
                           :when (not (instance? JCTree$JCSkip d))]
                       (simplify
                        (condp instance? d
                          JCTree$JCClassDecl (class-form env d)
                          JCTree$JCModuleDecl (module-form env d)))))
          forms (if-let [md (.getModuleDecl cu)]
                  (into [(simplify (module-form env md))] forms)
                  forms)
          pkg-anns (when-let [pd (.getPackage cu)]
                     (seq (annotation-items env (.annotations ^JCTree$JCPackageDecl pd))))
          forms (if (and (= file "package-info.java") pkg-anns)
                  (into [(apply list 'defpackage (apply m+ (symbol pkg-name) pkg-anns) [])] forms)
                  forms)]
      {:package pkg-name :file file :forms forms
       :supers (set (for [d (.getTypeDecls cu)
                          :when (instance? JCTree$JCClassDecl d)
                          ^Type st (.closure jt/*types* (.type (.sym ^JCTree$JCClassDecl d)))
                          :let [c (.outermostClass (.tsym st))]
                          :when (and (identical? (.packge c) pkg)
                                     (not (identical? c (.sym ^JCTree$JCClassDecl d))))]
                      (str (.getSimpleName c))))
       :pkg-classes (when pkg (set (for [^Symbol s (.getSymbols (.members pkg))
                                         :when (jt/class-sym? s)]
                                     (str (.getSimpleName s)))))
       :classes (vec (for [d (.getTypeDecls cu) :when (instance? JCTree$JCClassDecl d)]
                       (str (.name ^JCTree$JCClassDecl d))))})))

;;; ------------------------------------------------------------------------------------------
;;; Simplification of the output: when, when-not, cond, spliced do

(defn- body-start
  "Index of the first body form in list `x` whose body forms may be spliced, or nil."
  [x]
  (let [hd (first x)]
    (case (when (symbol? hd) (name hd))
      ("do" "try" "finally" "initializer" "static-initializer") 1
      ("when" "when-not" "while" "locking" "let" "loop" "for-each" "with-resources" "label"
       "letclass" "when-instance") 2
      "catch" 3
      ("method" "constructor" "lambda")
      (let [v (vec x)]
        (loop [i 1]
          (cond
            (>= i (count v)) nil
            (keyword? (v i)) (recur (+ i 2))
            (vector? (v i)) (inc i)
            :else (recur (inc i)))))
      nil)))

(defn- keep-meta [old new] (if (meta old) (with-meta new (meta old)) new))

(defn- if-head? [x s] (and (seq? x) (= s (first x))))

(defn- sh [s]
  (if (contains? *local-names* (name s))
    (symbol (if (new-heads (name s)) "arbace.core" "clojure.core") (name s))
    s))

(defn simplify [x]
  (cond
    (and (seq? x) (seq x))
    (let [y (keep-meta x (apply list (map simplify x)))
          y (if-let [i (body-start y)]
              (let [[pre body] (split-at i y)
                    ;; a try's catch and finally clauses follow its body
                    [body clauses] (if (if-head? y 'try)
                                     (split-with #(not (or (if-head? % 'catch) (if-head? % 'finally))) body)
                                     [body nil])
                    body (mapcat (fn [b] (if (and (if-head? b 'do) (not (meta b))) (rest b) [b])) body)
                    ;; nils that are not the value
                    body (concat (remove nil? (butlast body)) (when (seq body) [(last body)]))]
                (keep-meta y (apply list (concat pre body clauses))))
              y)
          splice (fn [b] (if (and (if-head? b 'do) (not (meta b))) (rest b) [b]))]
      (cond
        (and (if-head? y 'if) (= 4 (count y)))
        (let [[_ c a b] y]
          (cond
            (and (nil? b) (not (if-head? a 'if)))
            (keep-meta y (apply list (sh (if (and (seq? c) (= 'not (first c))) 'when-not 'when))
                                (if (and (seq? c) (= 'not (first c))) (second c) c) (splice a)))
            (nil? a)
            (if (and (seq? c) (= 'nil? (first c)) (= 2 (count c)))
              (keep-meta y (apply list (sh 'when) (list (sh 'some?) (second c)) (splice b)))
              (keep-meta y (apply list (sh 'when-not) c (splice b))))
            (or (and (if-head? b 'if) (= 4 (count b))) (if-head? b 'cond)
                (and (if-head? b 'when) (= 3 (count b))))
            (let [tail (cond
                         (if-head? b 'cond) (rest b)
                         (if-head? b 'when) [(nth b 1) (nth b 2)]
                         :else (let [[_ c2 a2 b2] b] (if (nil? b2) [c2 a2] [c2 a2 :else b2])))]
              (keep-meta y (apply list (sh 'cond) c a tail)))
            :else y))
        (and (if-head? y 'when) (seq? (second y)) (= 'not (first (second y))) (= 2 (count (second y))))
        (keep-meta y (apply list (sh 'when-not) (second (second y)) (drop 2 y)))
        (and (if-head? y 'if-instance) (= 4 (count y)) (nil? (nth y 3)))
        (keep-meta y (apply list (sh 'when-instance) (second y) (splice (nth y 2))))
        (and (if-head? y 'if) (= 3 (count y)))
        (let [[_ c a] y] (keep-meta y (apply list (sh 'when) c (splice a))))
        (and (if-head? y 'when-not) (seq? (second y)) (= 'nil? (first (second y))))
        (keep-meta y (apply list (sh 'when) (list (sh 'some?) (second (second y))) (drop 2 y)))
        (and (if-head? y 'when-not) (seq? (second y)) (= 'some? (first (second y))))
        (keep-meta y (apply list (sh 'when) (list (sh 'nil?) (second (second y))) (drop 2 y)))
        (and (if-head? y 'do) (= 2 (count y)) (not (meta y))) (second y)
        :else y))
    (vector? x) (keep-meta x (mapv simplify x))
    :else x))
