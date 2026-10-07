;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich Aug 21, 2007 */
;;
;; Converted from clojure/lang/Compiler.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(arbace.asm Attribute ClassVisitor ClassWriter FieldVisitor Handle Label Opcodes Type)
        '(arbace.asm.commons GeneratorAdapter Method)
        '(java.io File
                  FileInputStream
                  FileOutputStream
                  IOException
                  InputStreamReader
                  Reader
                  Serializable)
        '(java.lang.invoke MethodType)
        '(java.lang.reflect Constructor Executable Field Modifier)
        '(java.util ArrayList
                    Arrays
                    Collection
                    Comparator
                    HashMap
                    HashSet
                    IdentityHashMap
                    LinkedList
                    List
                    Map
                    Map$Entry
                    Set
                    SortedMap
                    TreeMap
                    TreeSet)
        '(java.util.function Function Predicate)
        '(java.util.regex Pattern)
        '(java.util.stream Collectors))

(defclass ^:public Compiler
  :implements [Opcodes]

  (field ^:static ^:final ^Symbol DEF (Symbol/intern "def"))

  (field ^:static ^:final ^Symbol LOOP (Symbol/intern "loop*"))

  (field ^:static ^:final ^Symbol RECUR (Symbol/intern "recur"))

  (field ^:static ^:final ^Symbol IF (Symbol/intern "if"))

  (field ^:static ^:final ^Symbol LET (Symbol/intern "let*"))

  (field ^:static ^:final ^Symbol LETFN (Symbol/intern "letfn*"))

  (field ^:static ^:final ^Symbol DO (Symbol/intern "do"))

  (field ^:static ^:final ^Symbol FN (Symbol/intern "fn*"))

  (field ^:static ^:final ^Symbol FNONCE
    (cast Symbol
          (.withMeta (Symbol/intern "fn*") (^[Object/1] RT/map (Keyword/intern nil "once") RT/T))))

  (field ^:static ^:final ^Symbol QUOTE (Symbol/intern "quote"))

  (field ^:static ^:final ^Symbol THE_VAR (Symbol/intern "var"))

  (field ^:static ^:final ^Symbol DOT (Symbol/intern "."))

  (field ^:static ^:final ^Symbol ASSIGN (Symbol/intern "set!"))

  (field ^:static ^:final ^Symbol TRY (Symbol/intern "try"))

  (field ^:static ^:final ^Symbol CATCH (Symbol/intern "catch"))

  (field ^:static ^:final ^Symbol FINALLY (Symbol/intern "finally"))

  (field ^:static ^:final ^Symbol THROW (Symbol/intern "throw"))

  (field ^:static ^:final ^Symbol MONITOR_ENTER (Symbol/intern "monitor-enter"))

  (field ^:static ^:final ^Symbol MONITOR_EXIT (Symbol/intern "monitor-exit"))

  (field ^:static ^:final ^Symbol IMPORT (Symbol/intern "arbace.core" "import*"))

  (field ^:static ^:final ^Symbol DEFTYPE (Symbol/intern "deftype*"))

  (field ^:static ^:final ^Symbol CASE (Symbol/intern "case*"))

  (field ^:static ^:final ^Symbol CLASS (Symbol/intern "Class"))

  (field ^:static ^:final ^Symbol NEW (Symbol/intern "new"))

  (field ^:static ^:final ^Symbol THIS (Symbol/intern "this"))

  (field ^:static ^:final ^Symbol REIFY (Symbol/intern "reify*"))

  ;; the class forms' special form for classes (doc/classes/SPEC.md §9.5)
  (field ^:static ^:final ^Symbol CLASS_STAR (Symbol/intern "class*"))

  (field ^:static ^:final ^Symbol LIST (Symbol/intern "arbace.core" "list"))

  (field ^:static ^:final ^Symbol HASHMAP (Symbol/intern "arbace.core" "hash-map"))

  (field ^:static ^:final ^Symbol VECTOR (Symbol/intern "arbace.core" "vector"))

  (field ^:static ^:final ^Symbol IDENTITY (Symbol/intern "arbace.core" "identity"))

  (field ^:static ^:final ^Symbol _AMP_ (Symbol/intern "&"))

  (field ^:static ^:final ^Symbol ISEQ (Symbol/intern "arbace.lang.ISeq"))

  (field ^:static ^:final ^Keyword loadNs (Keyword/intern nil "load-ns"))

  (field ^:static ^:final ^Keyword inlineKey (Keyword/intern nil "inline"))

  (field ^:static ^:final ^Keyword inlineAritiesKey (Keyword/intern nil "inline-arities"))

  (field ^:static ^:final ^Keyword staticKey (Keyword/intern nil "static"))

  (field ^:static ^:final ^Keyword arglistsKey (Keyword/intern nil "arglists"))

  (field ^:static ^:final ^Symbol INVOKE_STATIC (Symbol/intern "invokeStatic"))

  (field ^:static ^:final ^Keyword volatileKey (Keyword/intern nil "volatile"))

  (field ^:static ^:final ^Keyword implementsKey (Keyword/intern nil "implements"))

  (field ^:static ^:final ^String COMPILE_STUB_PREFIX "compile__stub")

  (field ^:static ^:final ^Keyword protocolKey (Keyword/intern nil "protocol"))

  (field ^:static ^:final ^Keyword onKey (Keyword/intern nil "on"))

  (field ^:static ^Keyword dynamicKey (Keyword/intern "dynamic"))

  (field ^:static ^:final ^Keyword redefKey (Keyword/intern nil "redef"))

  (field ^:static ^:final ^Symbol NS (Symbol/intern "ns"))

  (field ^:static ^:final ^Symbol IN_NS (Symbol/intern "in-ns"))

  (field ^:public ^:static ^:final ^IPersistentMap specials
    (PersistentHashMap/create
      (new Object/1
           [DEF
            (Compiler$DefExpr$Parser.)
            LOOP
            (Compiler$LetExpr$Parser.)
            RECUR
            (Compiler$RecurExpr$Parser.)
            IF
            (Compiler$IfExpr$Parser.)
            CASE
            (Compiler$CaseExpr$Parser.)
            LET
            (Compiler$LetExpr$Parser.)
            LETFN
            (Compiler$LetFnExpr$Parser.)
            DO
            (Compiler$BodyExpr$Parser.)
            FN
            nil
            QUOTE
            (Compiler$ConstantExpr$Parser.)
            THE_VAR
            (Compiler$TheVarExpr$Parser.)
            IMPORT
            (Compiler$ImportExpr$Parser.)
            DOT
            (Compiler$HostExpr$Parser.)
            ASSIGN
            (Compiler$AssignExpr$Parser.)
            DEFTYPE
            (Compiler$NewInstanceExpr$DeftypeParser.)
            REIFY
            (Compiler$NewInstanceExpr$ReifyParser.)
            TRY
            (Compiler$TryExpr$Parser.)
            THROW
            (Compiler$ThrowExpr$Parser.)
            MONITOR_ENTER
            (Compiler$MonitorEnterExpr$Parser.)
            MONITOR_EXIT
            (Compiler$MonitorExitExpr$Parser.)
            CATCH
            nil
            FINALLY
            nil
            NEW
            (Compiler$NewExpr$Parser.)
            _AMP_
            nil
            ;; the class forms' special forms (doc/classes/SPEC.md §9.5), compiled by arbace.classes
            CLASS_STAR
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "label*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "break*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "continue*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "return*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "switch*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "lambda*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "method-ref*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "java-str*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "java-assert*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "for-each*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "with-resources*")
            (Compiler$ClassFormsExpr$Parser.)
            (Symbol/intern "if-instance*")
            (Compiler$ClassFormsExpr$Parser.)])))

  ;; the class forms of the top-level do being evaluated or compiled, entered as declarations
  ;; when one of them is compiled (doc/classes/SPEC.md §9.2)
  (field ^:public ^:static ^:final ^Var CLASS_FORM_SIBLINGS (.setDynamic (Var/create nil)))

  ;; true while the initializers of a letfn* are analyzed: their fns are not handed over to the
  ;; class forms compiler one by one (their mutual references), the enclosing fn is
  (field ^:public ^:static ^:final ^Var CLASS_FORMS_NO_DELEGATE (.setDynamic (Var/create nil)))

  (field ^:private ^:static ^:final ^int MAX_POSITIONAL_ARITY 20)

  (field ^:private ^:static ^:final ^Type OBJECT_TYPE)

  (field ^:private ^:static ^:final ^Type KEYWORD_TYPE (Type/getType Keyword))

  (field ^:private ^:static ^:final ^Type VAR_TYPE (Type/getType Var))

  (field ^:private ^:static ^:final ^Type SYMBOL_TYPE (Type/getType Symbol))

  (field ^:private ^:static ^:final ^Type IFN_TYPE (Type/getType IFn))

  (field ^:private ^:static ^:final ^Type AFUNCTION_TYPE (Type/getType AFunction))

  (field ^:private ^:static ^:final ^Type RT_TYPE (Type/getType RT))

  (field ^:private ^:static ^:final ^Type NUMBERS_TYPE (Type/getType Numbers))

  (field ^:static ^:final ^Type CLASS_TYPE (Type/getType Class))

  (field ^:static ^:final ^Type NS_TYPE (Type/getType Namespace))

  (field ^:static ^:final ^Type UTIL_TYPE (Type/getType Util))

  (field ^:static ^:final ^Type REFLECTOR_TYPE (Type/getType Reflector))

  (field ^:static ^:final ^Type THROWABLE_TYPE (Type/getType Throwable))

  (field ^:static ^:final ^Type BOOLEAN_OBJECT_TYPE (Type/getType Boolean))

  (field ^:static ^:final ^Type IPERSISTENTMAP_TYPE (Type/getType IPersistentMap))

  (field ^:static ^:final ^Type IOBJ_TYPE (Type/getType IObj))

  (field ^:static ^:final ^Type TUPLE_TYPE (Type/getType Tuple))

  (field ^:static ^:final ^Method/1 createTupleMethods
    (new Method/1
         [(Method/getMethod "arbace.lang.IPersistentVector create()")
          (Method/getMethod "arbace.lang.IPersistentVector create(Object)")
          (Method/getMethod "arbace.lang.IPersistentVector create(Object,Object)")
          (Method/getMethod "arbace.lang.IPersistentVector create(Object,Object,Object)")
          (Method/getMethod "arbace.lang.IPersistentVector create(Object,Object,Object,Object)")
          (Method/getMethod
            "arbace.lang.IPersistentVector create(Object,Object,Object,Object,Object)")
          (Method/getMethod
            "arbace.lang.IPersistentVector create(Object,Object,Object,Object,Object,Object)")]))

  (field ^:private ^:static ^:final ^Type/2 ARG_TYPES)

  (field ^:private ^:static ^:final ^Type/1 EXCEPTION_TYPES (new Type/1 []))

  (static-initializer
    (set! OBJECT_TYPE (Type/getType Object))
    (set! ARG_TYPES (new Type/2 (unchecked-add-int MAX_POSITIONAL_ARITY 2)))
    (loop [^int i 0]
      (when (<= i MAX_POSITIONAL_ARITY)
        (let [a (new Type/1 i)]
          (loop [^int j 0] (when (< j i) (aset a j OBJECT_TYPE) (recur (unchecked-inc-int j))))
          (aset ARG_TYPES i a)
          (recur (unchecked-inc-int i)))))
    (let [a (new Type/1 (unchecked-add-int MAX_POSITIONAL_ARITY 1))]
      (loop [^int j 0]
        (when (< j MAX_POSITIONAL_ARITY) (aset a j OBJECT_TYPE) (recur (unchecked-inc-int j))))
      (aset a MAX_POSITIONAL_ARITY (Type/getType "[Ljava/lang/Object;"))
      (aset ARG_TYPES (unchecked-add-int MAX_POSITIONAL_ARITY 1) a)))

  (field ^:public ^:static ^:final ^Var LOCAL_ENV (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var LOOP_LOCALS (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var LOOP_LABEL (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var CONSTANTS (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var CONSTANT_IDS (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var KEYWORD_CALLSITES (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var PROTOCOL_CALLSITES (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var KEYWORDS (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var VARS (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var METHOD (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var IN_CATCH_FINALLY (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var METHOD_RETURN_CONTEXT (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var NO_RECUR (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var LOADER (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var SOURCE
    (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                             (Symbol/intern "*source-path*")
                             "NO_SOURCE_FILE")))

  (field ^:public ^:static ^:final ^Var SOURCE_PATH
    (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                             (Symbol/intern "*file*")
                             "NO_SOURCE_PATH")))

  (field ^:public ^:static ^:final ^Var COMPILE_PATH
    (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                             (Symbol/intern "*compile-path*")
                             nil)))

  (field ^:public ^:static ^:final ^Var COMPILE_FILES
    (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                             (Symbol/intern "*compile-files*")
                             Boolean/FALSE)))

  (field ^:public ^:static ^:final ^Var INSTANCE
    (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core")) (Symbol/intern "instance?")))

  (field ^:public ^:static ^:final ^Var ADD_ANNOTATIONS
    (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                (Symbol/intern "add-annotations")))

  (field ^:public ^:static ^:final ^Keyword disableLocalsClearingKey
    (Keyword/intern "disable-locals-clearing"))

  (field ^:public ^:static ^:final ^Keyword directLinkingKey (Keyword/intern "direct-linking"))

  (field ^:public ^:static ^:final ^Keyword elideMetaKey (Keyword/intern "elide-meta"))

  (field ^:public ^:static ^:final ^Var COMPILER_OPTIONS)

  (method ^:public ^:static getCompilerOption [^Keyword k]
    (RT/get (.deref COMPILER_OPTIONS) k))

  (static-initializer
    (let [^:mutable ^Object compilerOptions nil]
      (for-each [^Map$Entry e (.entrySet (System/getProperties))]
        (let [name (cast String (.getKey e))
              v (cast String (.getValue e))]
          (when (.startsWith name "arbace.compiler.")
            (set! compilerOptions
                  (RT/assoc compilerOptions
                            (RT/keyword nil
                                        (.substring
                                          name
                                          (unchecked-add-int 1 (.lastIndexOf name \.))))
                            (RT/readString v))))))
      (set! COMPILER_OPTIONS
            (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                                     (Symbol/intern "*compiler-options*")
                                     compilerOptions)))))

  (method ^:static elideMeta [^:mutable m]
    (let [^{:tag (Collection Object)} elides (cast
                                               Collection
                                               (arbace.lang.Compiler/getCompilerOption elideMetaKey))]
      (when (some? elides) (for-each [k elides] (set! m (RT/dissoc m k))))
      m))

  (field ^:public ^:static ^:final ^Var LINE (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var COLUMN (.setDynamic (Var/create (Integer/valueOf 0))))

  (method ^:static lineDeref ^int []
    (.intValue (cast Number (.deref LINE))))

  (method ^:static columnDeref ^int []
    (.intValue (cast Number (.deref COLUMN))))

  (field ^:public ^:static ^:final ^Var LINE_BEFORE (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var COLUMN_BEFORE
    (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var LINE_AFTER (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var COLUMN_AFTER (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var NEXT_LOCAL_NUM
    (.setDynamic (Var/create (Integer/valueOf 0))))

  (field ^:public ^:static ^:final ^Var RET_LOCAL_NUM (.setDynamic (Var/create)))

  (field ^:public ^:static ^:final ^Var COMPILE_STUB_SYM (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var COMPILE_STUB_CLASS (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var CLEAR_PATH (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var CLEAR_ROOT (.setDynamic (Var/create nil)))

  (field ^:public ^:static ^:final ^Var CLEAR_SITES (.setDynamic (Var/create nil)))

  (defclass ^:public ^:enum C
    (constants STATEMENT EXPRESSION RETURN EVAL))

  (field ^:public ^:static ^:final ^int JVM_BYTECODE_VERSION arbace.lang.Compiler/V17)

  (defclass ^:private Recur)

  (field ^:public ^:static ^:final ^Class RECUR_CLASS Recur)

  (defclass ^:interface Expr
    (method eval [this])

    (method emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen])

    (method hasJavaClass ^boolean [this])

    (method getJavaClass ^Class [this]))

  (defclass ^:public ^:abstract ^:static UntypedExpr
    :implements [Expr]

    (method ^:public getJavaClass ^Class [this]
      (throw (IllegalArgumentException. "Has no Java class")))

    (method ^:public hasJavaClass ^boolean [this] false))

  (defclass ^:interface IParser
    (method parse ^Expr [this ^C context form]))

  (method ^:static isSpecial ^boolean [sym] (.containsKey specials sym))

  (method ^:static inTailCall ^boolean [^C context]
    (and (and (identical? context C/RETURN) (some? (.deref METHOD_RETURN_CONTEXT)))
         (nil? (.deref IN_CATCH_FINALLY))))

  (method ^:static resolveSymbol ^Symbol [^Symbol sym]
    (cond
      (> (.indexOf (.-name sym) \.) 0) sym
      (some? (.-ns sym))
        (let [ns (arbace.lang.Compiler/namespaceFor sym)]
          (if (or (nil? ns)
                  (if (nil? (.-name (.-name ns)))
                      (nil? (.-ns sym))
                      (.equals (.-name (.-name ns)) (.-ns sym))))
              (let [ac (HostExpr/maybeArrayClass sym)]
                (if (some? ac) (Util/arrayTypeToSymbol ac) sym))
              (Symbol/intern (.-name (.-name ns)) (.-name sym))))
      :else
        (let [o (.getMapping (arbace.lang.Compiler/currentNS) sym)]
          (cond
            (nil? o) (Symbol/intern (.-name (.-name (arbace.lang.Compiler/currentNS))) (.-name sym))
            (instance? Class o) (Symbol/intern nil (.getName (cast Class o)))
            (instance? Var o)
              (let [v (cast Var o)] (Symbol/intern (.-name (.-name (.-ns v))) (.-name (.-sym v))))))))

  (defclass ^:static DefExpr
    :implements [Expr]

    (field ^:public ^:final ^Var var)

    (field ^:public ^:final ^Expr init)

    (field ^:public ^:final ^Expr meta)

    (field ^:public ^:final ^boolean initProvided)

    (field ^:public ^:final ^boolean isDynamic)

    (field ^:public ^:final ^String source)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:static ^:final ^Method bindRootMethod (Method/getMethod "void bindRoot(Object)"))

    (field ^:static ^:final ^Method setTagMethod
      (Method/getMethod "void setTag(arbace.lang.Symbol)"))

    (field ^:static ^:final ^Method setMetaMethod
      (Method/getMethod "void setMeta(arbace.lang.IPersistentMap)"))

    (field ^:static ^:final ^Method setDynamicMethod
      (Method/getMethod "arbace.lang.Var setDynamic(boolean)"))

    (field ^:static ^:final ^Method symintern
      (Method/getMethod "arbace.lang.Symbol intern(String, String)"))

    (constructor ^:public [this ^String source ^int line ^int column ^Var var ^Expr init ^Expr meta
                           ^boolean initProvided ^boolean isDynamic]
      (set! (.-source this) source)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-var this) var)
      (set! (.-init this) init)
      (set! (.-meta this) meta)
      (set! (.-isDynamic this) isDynamic)
      (set! (.-initProvided this) initProvided))

    (method ^:private includesExplicitMetadata ^boolean [this ^MapExpr expr]
      (loop [^int i 0]
        (when (< i (.count (.-keyvals expr)))
          (let [k (.-k (cast KeywordExpr (.nth (.-keyvals expr) i)))]
            (if (and (and (and (not (identical? k RT/FILE_KEY))
                               (not (identical? k RT/DECLARED_KEY)))
                          (not (identical? k RT/LINE_KEY)))
                     (not (identical? k RT/COLUMN_KEY)))
                (return true)
                (recur (unchecked-add-int i 2))))))
      false)

    (method ^:public eval [this]
      (try
        (when initProvided (.bindRoot var (.eval init)))
        (when (some? meta)
          (let [metaMap (cast IPersistentMap (.eval meta))]
            (when (or initProvided true) (.setMeta var metaMap))))
        (.setDynamic var isDynamic)
        (catch Throwable e
          (if (not (instance? CompilerException e))
              (throw (CompilerException. source
                                         line
                                         column
                                         arbace.lang.Compiler/DEF
                                         CompilerException/PHASE_EXECUTION
                                         e))
              (throw (cast CompilerException e))))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitVar objx gen var)
      (when isDynamic (.push gen isDynamic) (.invokeVirtual gen VAR_TYPE setDynamicMethod))
      (when (some? meta)
        (when (or initProvided true)
          (.dup gen)
          (.emit meta C/EXPRESSION objx gen)
          (.checkCast gen IPERSISTENTMAP_TYPE)
          (.invokeVirtual gen VAR_TYPE setMetaMethod)))
      (when initProvided
        (.dup gen)
        (if (instance? FnExpr init)
            (.emitForDefn (cast FnExpr init) objx gen)
            (.emit init C/EXPRESSION objx gen))
        (.invokeVirtual gen VAR_TYPE bindRootMethod))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] Var)

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context ^:mutable form]
        (let [^:mutable ^String docstring nil]
          (when (and (== (RT/count form) 4) (instance? String (RT/third form)))
            (set! docstring (cast String (RT/third form)))
            (set! form (RT/list (RT/first form) (RT/second form) (RT/fourth form))))
          (cond
            (> (RT/count form) 3) (throw (Util/runtimeException "Too many arguments to def"))
            (< (RT/count form) 2) (throw (Util/runtimeException "Too few arguments to def"))
            :else
              (when-not (instance? Symbol (RT/second form))
                (throw (Util/runtimeException "First argument to def must be a Symbol"))))
          (let [sym (cast Symbol (RT/second form))
                ^:mutable v (arbace.lang.Compiler/lookupVar sym true)]
            (when (nil? v)
              (throw (Util/runtimeException "Can't refer to qualified var that doesn't exist")))
            (when-not (.equals (.-ns v) (arbace.lang.Compiler/currentNS))
              (if (nil? (.-ns sym))
                  (do
                    (set! v (.intern (arbace.lang.Compiler/currentNS) sym))
                    (arbace.lang.Compiler/registerVar v))
                  (throw (Util/runtimeException "Can't create defs outside of current ns"))))
            (let [^:mutable mm (.meta sym)
                  isDynamic (RT/booleanCast (RT/get mm dynamicKey))]
              (when isDynamic (.setDynamic v))
              (when (and (and (and (not isDynamic) (.startsWith (.-name sym) "*"))
                              (.endsWith (.-name sym) "*"))
                         (> (.length (.-name sym)) 2))
                (.format
                  (RT/errPrintWriter)
                  "Warning: %1$s not declared dynamic and thus is not dynamically rebindable, but its name suggests otherwise. Please either indicate ^:dynamic %1$s or change the name. (%2$s:%3$d)\n"
                  (new Object/1 [sym (.get SOURCE_PATH) (.get LINE)])))
              (when (RT/booleanCast (RT/get mm arglistsKey))
                (let [^:mutable vm (.meta v)]
                  (set! vm
                        (cast IPersistentMap
                              (RT/assoc vm arglistsKey (RT/second (.valAt mm arglistsKey)))))
                  (.setMeta v vm)))
              (let [^:mutable source_path (.get SOURCE_PATH)]
                (set! source_path (if (nil? source_path) "NO_SOURCE_FILE" source_path))
                (set! mm
                      (cast IPersistentMap
                            (.assoc (.assoc (RT/assoc mm RT/LINE_KEY (.get LINE))
                                            RT/COLUMN_KEY
                                            (.get COLUMN))
                                    RT/FILE_KEY
                                    source_path)))
                (when (some? docstring)
                  (set! mm (cast IPersistentMap (RT/assoc mm RT/DOC_KEY docstring))))
                (set! mm (cast IPersistentMap (arbace.lang.Compiler/elideMeta mm)))
                (let [meta (when-not (== (.count mm) 0)
                             (arbace.lang.Compiler/analyze
                               (if (identical? context C/EVAL) context C/EXPRESSION)
                               mm))]
                  (DefExpr. (cast String (.deref SOURCE))
                            (arbace.lang.Compiler/lineDeref)
                            (arbace.lang.Compiler/columnDeref)
                            v
                            (arbace.lang.Compiler/analyze
                              (if (identical? context C/EVAL) context C/EXPRESSION)
                              (RT/third form)
                              (.-name (.-sym v)))
                            meta
                            (== (RT/count form) 3)
                            isDynamic)))))))))

  (defclass ^:public ^:static AssignExpr
    :implements [Expr]

    (field ^:public ^:final ^AssignableExpr target)

    (field ^:public ^:final ^Expr val)

    (constructor ^:public [this ^AssignableExpr target ^Expr val]
      (set! (.-target this) target)
      (set! (.-val this) val))

    (method ^:public eval [this] (.evalAssign target val))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitAssign target context objx gen val))

    (method ^:public hasJavaClass ^boolean [this] (.hasJavaClass val))

    (method ^:public getJavaClass ^Class [this] (.getJavaClass val))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (when-not (== (RT/length form) 3)
            (throw (IllegalArgumentException. "Malformed assignment, expecting (set! target val)")))
          (let [target (arbace.lang.Compiler/analyze C/EXPRESSION (RT/second form))]
            (when-not (instance? AssignableExpr target)
              (throw (IllegalArgumentException. "Invalid assignment target")))
            ;; set! of a local of the method (not a mutable deftype field, not a captured local):
            ;; a mutable local of the class forms (doc/classes/SPEC.md §5.3)
            (when (and (instance? LocalBindingExpr target) (some? (.deref METHOD)))
              (let [lb (.-b (cast LocalBindingExpr target))
                    objx (.-objx (cast ObjMethod (.deref METHOD)))]
                (when-not (or (.isMutable objx lb) (RT/booleanCast (RT/contains (.-closes objx) lb)))
                  (throw (ClassFormsExpr$Signal. "set! of a local")))))
            (AssignExpr. (cast AssignableExpr target)
                         (arbace.lang.Compiler/analyze C/EXPRESSION (RT/third form))))))))

  (defclass ^:public ^:static VarExpr
    :implements [Expr AssignableExpr]

    (field ^:public ^:final ^Var var)

    (field ^:public ^:final tag)

    (field ^:static ^:final ^Method getMethod (Method/getMethod "Object get()"))

    (field ^:static ^:final ^Method setMethod (Method/getMethod "Object set(Object)"))

    (field ^Class jc)

    (constructor ^:public [this ^Var var ^Symbol tag]
      (set! (.-var this) var)
      (set! (.-tag this) (if (some? tag) ^Object tag (.getTag var))))

    (method ^:public eval [this] (.deref var))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitVarValue objx gen var)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] (some? tag))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (HostExpr/tagToClass tag)))
      jc)

    (method ^:public evalAssign [this ^Expr val] (.set var (.eval val)))

    (method ^:public emitAssign ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen ^Expr val]
      (.emitVar objx gen var)
      (.emit val C/EXPRESSION objx gen)
      (.invokeVirtual gen VAR_TYPE setMethod)
      (when (identical? context C/STATEMENT) (.pop gen))))

  (defclass ^:public ^:static TheVarExpr
    :implements [Expr]

    (field ^:public ^:final ^Var var)

    (constructor ^:public [this ^Var var] (set! (.-var this) var))

    (method ^:public eval [this] var)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitVar objx gen var)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] Var)

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context form]
        (let [sym (cast Symbol (RT/second form))
              v (arbace.lang.Compiler/lookupVar sym false)]
          (if (some? v)
              (TheVarExpr. v)
              (throw (Util/runtimeException
                       (java-str "Unable to resolve var: " sym " in this context"))))))))

  (defclass ^:public ^:static KeywordExpr
    :extends LiteralExpr

    (field ^:public ^:final ^Keyword k)

    (constructor ^:public [this ^Keyword k] (set! (.-k this) k))

    (method val [this] k)

    (method ^:public eval [this] k)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitKeyword objx gen k)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] Keyword))

  ;; The class forms' special forms (doc/classes/SPEC.md §9.5) are compiled by the class forms
  ;; compiler, arbace.classes, loaded on first use: its namespace arbace.classes.native is the
  ;; boundary. (class* :top form) and (class* :tops forms), from defclass and defclasses, are
  ;; compiled and defined at analysis time, and analyzed as the imports and the class(es).
  (defclass ^:public ^:static ClassFormsExpr
    (field ^:static ^:final ^Keyword TOP (Keyword/intern nil "top"))

    (field ^:static ^:final ^Keyword TOPS (Keyword/intern nil "tops"))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)
              kind (RT/second form)]
          (if (and (.equals CLASS_STAR (RT/first form)) (or (.equals TOP kind) (.equals TOPS kind)))
              (arbace.lang.Compiler/analyze
                context
                (.invoke (arbace.lang.Compiler/classForms "compile-top")
                         (arbace.lang.Compiler/currentNS)
                         kind
                         (RT/third form)
                         (.deref CLASS_FORM_SIBLINGS)))
              ;; any other class form in code the compiler compiles itself: the enclosing fn is
              ;; handed over to the class forms compiler (FnExpr/parse, delegateFn)
              (throw (Signal. (java-str (RT/first form))))))))

    ;; Thrown while analyzing code that only the class forms compiler can compile; caught by
    ;; FnExpr/parse, or at the top level by eval and compile1, which wrap the form in a fn.
    (defclass ^:public ^:static Signal
      :extends RuntimeException

      (constructor ^:public [this ^String what]
        (super. (java-str what " outside a fn or class body"))))

    (method ^:static siblings ^IPersistentVector [^ISeq doForm ^IPersistentVector acc]
      (let [^:mutable ret acc]
        (loop [s (RT/next doForm)]
          (when (some? s)
            (let [x (RT/first s)]
              (when (and (instance? ISeq x) (instance? Symbol (RT/first x)))
                (let [op (cast Symbol (RT/first x))
                      mac (ClassFormsExpr/macroName op)]
                  (cond
                    (.equals DO op) (set! ret (ClassFormsExpr/siblings (cast ISeq x) ret))
                    (and (.equals CLASS_STAR op) (.equals TOP (RT/second x)))
                      (set! ret (cast IPersistentVector (RT/conj ret (RT/third x))))
                    (and (.equals CLASS_STAR op) (.equals TOPS (RT/second x)))
                      (loop [t (RT/seq (RT/third x))]
                        (when (some? t)
                          (set! ret (cast IPersistentVector (RT/conj ret (RT/first t))))
                          (recur (RT/next t))))
                    (.equals "defclass" mac) (set! ret (cast IPersistentVector (RT/conj ret (RT/next x))))
                    (.equals "defclasses" mac)
                      (loop [t (RT/next x)]
                        (when (some? t)
                          (set! ret (cast IPersistentVector (RT/conj ret (RT/next (RT/first t)))))
                          (recur (RT/next t))))))))
            (recur (RT/next s))))
        ret))

    (method ^:static macroName ^String [^Symbol op]
      (try
        (let [v (arbace.lang.Compiler/isMacro op)]
          (if (and (some? v) (.equals "arbace.core" (.-name (.-name (.-ns v)))))
              (.-name (.-sym v))
              nil))
        (catch Throwable e nil)))

    (method ^:static pushSiblings ^boolean [^ISeq doForm]
      (if (some? (.deref CLASS_FORM_SIBLINGS))
          false
          (let [sibs (ClassFormsExpr/siblings doForm PersistentVector/EMPTY)]
            (if (> (.count sibs) 1)
                (do (Var/pushThreadBindings (^[Object/1] RT/map CLASS_FORM_SIBLINGS sibs)) true)
                false)))))

  ;; FnExpr/parse caught a Signal: hand the fn over to the class forms compiler, unless it is
  ;; one of the compiler's own wrappers (fn* ^:once []) or a letfn* initializer
  (method ^:static delegateFn ^Expr [^C context ^ISeq form ^String name ^boolean onceOnly
                                     ^RuntimeException sig]
    (when (or (RT/booleanCast (.deref CLASS_FORMS_NO_DELEGATE))
              (and onceOnly
                   (instance? IPersistentVector (RT/second form))
                   (== 0 (RT/count (RT/second form)))))
      (throw sig))
    (arbace.lang.Compiler/analyze context
                                  (.invoke (arbace.lang.Compiler/classForms "compile-fn")
                                           (arbace.lang.Compiler/currentNS)
                                           name
                                           form
                                           (.deref LOCAL_ENV))))

  (method ^:static classForms ^IFn [^String name]
    (.invoke (RT/var "arbace.core" "require") (Symbol/intern "arbace.classes.native"))
    (RT/var "arbace.classes.native" name))

  (defclass ^:public ^:static ImportExpr
    :implements [Expr]

    (field ^:public ^:final ^String c)

    (field ^:static ^:final ^Method forNameMethod
      (Method/getMethod "Class classForNameNonLoading(String)"))

    (field ^:static ^:final ^Method importClassMethod (Method/getMethod "Class importClass(Class)"))

    (field ^:static ^:final ^Method derefMethod (Method/getMethod "Object deref()"))

    (constructor ^:public [this ^String c] (set! (.-c this) c))

    (method ^:public eval [this]
      (let [ns (cast Namespace (.deref RT/CURRENT_NS))]
        (.importClass ns (RT/classForNameNonLoading c))
        nil))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.getStatic gen RT_TYPE "CURRENT_NS" VAR_TYPE)
      (.invokeVirtual gen VAR_TYPE derefMethod)
      (.checkCast gen NS_TYPE)
      (.push gen c)
      (.invokeStatic gen RT_TYPE forNameMethod)
      (.invokeVirtual gen NS_TYPE importClassMethod)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] false)

    (method ^:public getJavaClass ^Class [this]
      (throw (IllegalArgumentException. "ImportExpr has no Java class")))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context form]
        (ImportExpr. (cast String (RT/second form))))))

  (defclass ^:public ^:abstract ^:static LiteralExpr
    :implements [Expr]

    (method ^:abstract val [this])

    (method ^:public eval [this] (.val this)))

  (defclass ^:static ^:interface AssignableExpr
    (method evalAssign [this ^Expr val])

    (method emitAssign ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen ^Expr val]))

  (defclass ^:public ^:static ^:interface MaybePrimitiveExpr
    :extends [Expr]

    (method ^:public canEmitPrimitive ^boolean [this])

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]))

  (defclass ^:public ^:abstract ^:static HostExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:static ^:final ^Type BOOLEAN_TYPE (Type/getType Boolean))

    (field ^:static ^:final ^Type CHAR_TYPE (Type/getType Character))

    (field ^:static ^:final ^Type INTEGER_TYPE (Type/getType Integer))

    (field ^:static ^:final ^Type LONG_TYPE (Type/getType Long))

    (field ^:static ^:final ^Type FLOAT_TYPE (Type/getType Float))

    (field ^:static ^:final ^Type DOUBLE_TYPE (Type/getType Double))

    (field ^:static ^:final ^Type SHORT_TYPE (Type/getType Short))

    (field ^:static ^:final ^Type BYTE_TYPE (Type/getType Byte))

    (field ^:static ^:final ^Type NUMBER_TYPE (Type/getType Number))

    (field ^:static ^:final ^Method charValueMethod (Method/getMethod "char charValue()"))

    (field ^:static ^:final ^Method booleanValueMethod (Method/getMethod "boolean booleanValue()"))

    (field ^:static ^:final ^Method charValueOfMethod (Method/getMethod "Character valueOf(char)"))

    (field ^:static ^:final ^Method intValueOfMethod (Method/getMethod "Integer valueOf(int)"))

    (field ^:static ^:final ^Method longValueOfMethod (Method/getMethod "Long valueOf(long)"))

    (field ^:static ^:final ^Method floatValueOfMethod (Method/getMethod "Float valueOf(float)"))

    (field ^:static ^:final ^Method doubleValueOfMethod (Method/getMethod "Double valueOf(double)"))

    (field ^:static ^:final ^Method shortValueOfMethod (Method/getMethod "Short valueOf(short)"))

    (field ^:static ^:final ^Method byteValueOfMethod (Method/getMethod "Byte valueOf(byte)"))

    (field ^:static ^:final ^Method intValueMethod (Method/getMethod "int intValue()"))

    (field ^:static ^:final ^Method longValueMethod (Method/getMethod "long longValue()"))

    (field ^:static ^:final ^Method floatValueMethod (Method/getMethod "float floatValue()"))

    (field ^:static ^:final ^Method doubleValueMethod (Method/getMethod "double doubleValue()"))

    (field ^:static ^:final ^Method byteValueMethod (Method/getMethod "byte byteValue()"))

    (field ^:static ^:final ^Method shortValueMethod (Method/getMethod "short shortValue()"))

    (field ^:static ^:final ^Method fromIntMethod (Method/getMethod "arbace.lang.Num from(int)"))

    (field ^:static ^:final ^Method fromLongMethod (Method/getMethod "arbace.lang.Num from(long)"))

    (field ^:static ^:final ^Method fromDoubleMethod
      (Method/getMethod "arbace.lang.Num from(double)"))

    (method ^:public ^:static emitBoxReturn ^void [^ObjExpr objx ^GeneratorAdapter gen
                                                   ^Class returnType]
      (when (.isPrimitive returnType)
        (cond
          (identical? returnType Boolean/TYPE)
            (let [falseLabel (.newLabel gen)
                  endLabel (.newLabel gen)]
              (.ifZCmp gen GeneratorAdapter/EQ falseLabel)
              (.getStatic gen BOOLEAN_OBJECT_TYPE "TRUE" BOOLEAN_OBJECT_TYPE)
              (.goTo gen endLabel)
              (.mark gen falseLabel)
              (.getStatic gen BOOLEAN_OBJECT_TYPE "FALSE" BOOLEAN_OBJECT_TYPE)
              (.mark gen endLabel))
          (identical? returnType Void/TYPE) (.emit NIL_EXPR C/EXPRESSION objx gen)
          (identical? returnType Character/TYPE) (.invokeStatic gen CHAR_TYPE charValueOfMethod)
          (identical? returnType Integer/TYPE) (.invokeStatic gen INTEGER_TYPE intValueOfMethod)
          (identical? returnType Float/TYPE) (.invokeStatic gen FLOAT_TYPE floatValueOfMethod)
          (identical? returnType Double/TYPE) (.invokeStatic gen DOUBLE_TYPE doubleValueOfMethod)
          (identical? returnType Long/TYPE)
            (.invokeStatic gen NUMBERS_TYPE (Method/getMethod "Number num(long)"))
          (identical? returnType Byte/TYPE) (.invokeStatic gen BYTE_TYPE byteValueOfMethod)
          (identical? returnType Short/TYPE) (.invokeStatic gen SHORT_TYPE shortValueOfMethod))))

    (method ^:public ^:static emitUnboxArg ^void [^ObjExpr objx ^GeneratorAdapter gen
                                                  ^Class paramType]
      (if (.isPrimitive paramType)
          (cond
            (identical? paramType Boolean/TYPE)
              (do
                (.checkCast gen BOOLEAN_TYPE)
                (.invokeVirtual gen BOOLEAN_TYPE booleanValueMethod))
            (identical? paramType Character/TYPE)
              (do (.checkCast gen CHAR_TYPE) (.invokeVirtual gen CHAR_TYPE charValueMethod))
            :else
              (let [^:mutable ^Method m nil]
                (.checkCast gen NUMBER_TYPE)
                (cond
                  (RT/booleanCast (.deref RT/UNCHECKED_MATH))
                    (cond
                      (identical? paramType Integer/TYPE)
                        (set! m (Method/getMethod "int uncheckedIntCast(Object)"))
                      (identical? paramType Float/TYPE)
                        (set! m (Method/getMethod "float uncheckedFloatCast(Object)"))
                      (identical? paramType Double/TYPE)
                        (set! m (Method/getMethod "double uncheckedDoubleCast(Object)"))
                      (identical? paramType Long/TYPE)
                        (set! m (Method/getMethod "long uncheckedLongCast(Object)"))
                      (identical? paramType Byte/TYPE)
                        (set! m (Method/getMethod "byte uncheckedByteCast(Object)"))
                      (identical? paramType Short/TYPE)
                        (set! m (Method/getMethod "short uncheckedShortCast(Object)")))
                  (identical? paramType Integer/TYPE)
                    (set! m (Method/getMethod "int intCast(Object)"))
                  (identical? paramType Float/TYPE)
                    (set! m (Method/getMethod "float floatCast(Object)"))
                  (identical? paramType Double/TYPE)
                    (set! m (Method/getMethod "double doubleCast(Object)"))
                  (identical? paramType Long/TYPE)
                    (set! m (Method/getMethod "long longCast(Object)"))
                  (identical? paramType Byte/TYPE)
                    (set! m (Method/getMethod "byte byteCast(Object)"))
                  (identical? paramType Short/TYPE)
                    (set! m (Method/getMethod "short shortCast(Object)")))
                (.invokeStatic gen RT_TYPE m)))
          (.checkCast gen (Type/getType paramType))))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (when (< (RT/length form) 3)
            (throw (IllegalArgumentException.
                     "Malformed member expression, expecting (. target member ...)")))
          (let [line (arbace.lang.Compiler/lineDeref)
                column (arbace.lang.Compiler/columnDeref)
                source (cast String (.deref SOURCE))
                c (HostExpr/maybeClass (RT/second form) false)
                ^:mutable ^Expr instance nil]
            (when (nil? c)
              (set! instance
                    (arbace.lang.Compiler/analyze
                      (if (identical? context C/EVAL) context C/EXPRESSION)
                      (RT/second form))))
            (let [^:mutable maybeField (and (== (RT/length form) 3)
                                            (instance? Symbol (RT/third form)))]
              (when (and maybeField
                         (not (== (.charAt (.-name (cast Symbol (RT/third form))) 0) \-)))
                (let [sym (cast Symbol (RT/third form))]
                  (cond
                    (some? c)
                      (set! maybeField
                            (== (.size (Reflector/getMethods
                                         c
                                         0
                                         (arbace.lang.Compiler/munge (.-name sym))
                                         true))
                                0))
                    (and (and (some? instance) (.hasJavaClass instance))
                         (some? (.getJavaClass instance)))
                      (set! maybeField
                            (== (.size (Reflector/getMethods
                                         (.getJavaClass instance)
                                         0
                                         (arbace.lang.Compiler/munge (.-name sym))
                                         false))
                                0)))))
              (if maybeField
                  (let [sym (if (== (.charAt (.-name (cast Symbol (RT/third form))) 0) \-)
                                (Symbol/intern
                                  (.substring (.-name (cast Symbol (RT/third form))) 1))
                                (cast Symbol (RT/third form)))
                        tag (arbace.lang.Compiler/tagOf form)]
                    (if (some? c)
                        (StaticFieldExpr. line
                                          column
                                          c
                                          (arbace.lang.Compiler/munge (.-name sym))
                                          tag)
                        (InstanceFieldExpr. line
                                            column
                                            instance
                                            (arbace.lang.Compiler/munge (.-name sym))
                                            tag
                                            (== (.charAt (.-name (cast Symbol (RT/third form))) 0)
                                                \-))))
                  (let [call (cast ISeq
                                   (if (instance? ISeq (RT/third form))
                                       (RT/third form)
                                       ^Object (RT/next (RT/next form))))]
                    (when-not (instance? Symbol (RT/first call))
                      (throw (IllegalArgumentException. "Malformed member expression")))
                    (let [sym (cast Symbol (RT/first call))
                          tag (arbace.lang.Compiler/tagOf form)
                          ^:mutable args PersistentVector/EMPTY
                          tailPosition (arbace.lang.Compiler/inTailCall context)]
                      (loop [s (RT/next call)]
                        (when (some? s)
                          (set! args
                                (.cons args
                                       (arbace.lang.Compiler/analyze
                                         (if (identical? context C/EVAL) context C/EXPRESSION)
                                         (.first s))))
                          (recur (.next s))))
                      (if (some? c)
                          (StaticMethodExpr. source
                                             line
                                             column
                                             tag
                                             c
                                             (arbace.lang.Compiler/munge (.-name sym))
                                             args
                                             tailPosition)
                          (InstanceMethodExpr. source
                                               line
                                               column
                                               tag
                                               instance
                                               nil
                                               (arbace.lang.Compiler/munge (.-name sym))
                                               args
                                               tailPosition))))))))))

    (method ^:public ^:static maybeClass ^Class [form ^boolean stringOk]
      (if (instance? Class form)
          (cast Class form)
          (let [^:mutable ^Class c nil]
            (cond
              (instance? Symbol form)
                (let [sym (cast Symbol form)]
                  (when (nil? (.-ns sym))
                    (when (Util/equals sym (.get COMPILE_STUB_SYM))
                      (return (cast Class (.get COMPILE_STUB_CLASS))))
                    (if (or (> (.indexOf (.-name sym) \.) 0) (== (.charAt (.-name sym) 0) \[))
                        (set! c (RT/classForNameNonLoading (.-name sym)))
                        (let [o (.getMapping (arbace.lang.Compiler/currentNS) sym)]
                          (cond
                            (instance? Class o) (set! c (cast Class o))
                            (and (some? (.deref LOCAL_ENV))
                                 (.containsKey (cast Map (.deref LOCAL_ENV)) form))
                              (return nil)
                            :else
                              (try
                                (set! c (RT/classForNameNonLoading (.-name sym)))
                                (catch Exception e)))))))
              (and stringOk (instance? String form))
                (set! c (RT/classForNameNonLoading (cast String form))))
            c)))

    (method ^:public ^:static maybeSpecialTag ^Class [^Symbol sym]
      (let [^:mutable c (arbace.lang.Compiler/primClass sym)]
        (if (some? c)
            c
            (do
              (cond
                (.equals (.-name sym) "objects") (set! c Object/1)
                (.equals (.-name sym) "ints") (set! c int/1)
                (.equals (.-name sym) "longs") (set! c long/1)
                (.equals (.-name sym) "floats") (set! c float/1)
                (.equals (.-name sym) "doubles") (set! c double/1)
                (.equals (.-name sym) "chars") (set! c char/1)
                (.equals (.-name sym) "shorts") (set! c short/1)
                (.equals (.-name sym) "bytes") (set! c byte/1)
                (.equals (.-name sym) "booleans") (set! c boolean/1))
              c))))

    (method ^:static tagToClass ^Class [tag]
      (let [^:mutable ^Class c nil]
        (when (instance? Symbol tag)
          (let [sym (cast Symbol tag)]
            (when (nil? (.-ns sym)) (set! c (HostExpr/maybeSpecialTag sym)))
            (when (nil? c) (set! c (HostExpr/maybeArrayClass sym)))))
        (when (nil? c) (set! c (HostExpr/maybeClass tag true)))
        (if (some? c)
            c
            (throw (IllegalArgumentException. (java-str "Unable to resolve classname: " tag))))))

    (method ^:public ^:static looksLikeArrayClass ^boolean [^Symbol sym]
      (and (some? (.-ns sym)) (Util/isPosDigit (.-name sym))))

    (method ^:public ^:static buildArrayClassDescriptor ^String [^Symbol sym]
      (let [dim (unchecked-subtract-int (.charAt (.-name sym) 0) \0)
            componentClassName (Symbol/intern nil (.-ns sym))
            ^:mutable componentClass (arbace.lang.Compiler/primClass componentClassName)]
        (when (nil? componentClass)
          (set! componentClass (HostExpr/maybeClass componentClassName false)))
        (when (nil? componentClass)
          (throw (Util/sneakyThrow
                   (ClassNotFoundException.
                     (java-str "Unable to resolve component classname: " componentClassName)))))
        (let [arrayDescriptor (StringBuilder.)]
          (loop [^int i 0]
            (when (< i dim)
              (^[char] StringBuilder/.append arrayDescriptor \[)
              (recur (unchecked-inc-int i))))
          (let [ccDescr (if (.isPrimitive componentClass)
                            (.getDescriptor (Type/getType componentClass))
                            (java-str "L" (.getName componentClass) ";"))]
            (.append arrayDescriptor ccDescr)
            (.toString arrayDescriptor)))))

    (method ^:public ^:static maybeArrayClass ^Class [^Symbol sym]
      (when-not (not (HostExpr/looksLikeArrayClass sym))
        (HostExpr/maybeClass (HostExpr/buildArrayClassDescriptor sym) true))))

  (defclass ^:static QualifiedMethodExpr
    :implements [Expr]

    (field ^:private ^:final ^Class c)

    (field ^:private ^:final ^{:tag (List Class)} hintedSig)

    (field ^:private ^:final ^Symbol methodSymbol)

    (field ^:private ^:final ^String methodName)

    (field ^:private ^:final ^MethodKind kind)

    (field ^:private ^:final ^Class tagClass)

    (field ^:private ^:final ^StaticFieldExpr fieldOverload)

    (defclass ^:private ^:enum MethodKind
      (constants CTOR INSTANCE STATIC))

    (constructor ^:public [this ^Class methodClass ^Symbol sym]
      (this. methodClass sym nil))

    (constructor ^:public [this ^Class methodClass ^Symbol sym ^StaticFieldExpr fieldOL]
      (set! c methodClass)
      (set! methodSymbol sym)
      (set! tagClass
            (when (some? (arbace.lang.Compiler/tagOf sym))
              (HostExpr/tagToClass (arbace.lang.Compiler/tagOf sym))))
      (set! hintedSig (arbace.lang.Compiler/tagsToClasses (arbace.lang.Compiler/paramTagsOf sym)))
      (cond
        (.startsWith (.-name sym) ".")
          (do (set! kind MethodKind/INSTANCE) (set! methodName (.substring (.-name sym) 1)))
        (.equals (.-name sym) "new") (do (set! kind MethodKind/CTOR) (set! methodName (.-name sym)))
        :else (do (set! kind MethodKind/STATIC) (set! methodName (.-name sym))))
      (set! fieldOverload fieldOL))

    (method ^:private preferOverloadedField ^boolean [this]
      (and (some? fieldOverload) (nil? (arbace.lang.Compiler/paramTagsOf methodSymbol))))

    (method ^:public eval [this]
      (if (.preferOverloadedField this)
          (.eval fieldOverload)
          (.eval (QualifiedMethodExpr/buildThunk C/EVAL this))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (.preferOverloadedField this)
          (.emit fieldOverload context objx gen)
          (.emit (QualifiedMethodExpr/buildThunk context this) context objx gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this]
      (cond
        (some? tagClass) tagClass
        (.preferOverloadedField this) (.getJavaClass fieldOverload)
        :else AFn))

    (method ^:private ^:static buildThunk ^FnExpr [^C context ^QualifiedMethodExpr qmexpr]
      (let [^:mutable ^IPersistentCollection form PersistentVector/EMPTY
            instanceParam (when (identical? (.-kind qmexpr) MethodKind/INSTANCE) THIS)
            thunkName (java-str "invoke__"
                                (.getSimpleName (.-c qmexpr))
                                "_"
                                (.-name (.-methodSymbol qmexpr)))
            arities (if (some? (.-hintedSig qmexpr))
                        ^Set (PersistentHashSet/create
                               (new Object/1 [(.size (.-hintedSig qmexpr))]))
                        (QualifiedMethodExpr/aritySet
                          (.-c qmexpr)
                          (.-methodName qmexpr)
                          (.-kind qmexpr)))]
        (for-each [a arities]
          (let [arity (.intValue (cast Integer a))
                params (QualifiedMethodExpr/buildParams instanceParam arity)
                body (RT/listStar (.-methodSymbol qmexpr) (.seq params))]
            (set! form (RT/conj form (RT/list params body)))))
        (let [thunkForm (RT/listStar (Symbol/intern "fn") (Symbol/intern thunkName) (RT/seq form))]
          (cast FnExpr (arbace.lang.Compiler/analyzeSeq context thunkForm thunkName)))))

    (method ^:private ^:static buildParams ^IPersistentVector [^Symbol instanceParam ^int arity]
      (let [^:mutable ^IPersistentVector params PersistentVector/EMPTY]
        (when (some? instanceParam) (set! params (.cons params instanceParam)))
        (loop [^int i 0]
          (when (< i arity)
            (set! params
                  (.cons params (Symbol/intern nil (java-str "arg" (unchecked-add-int i 1)))))
            (recur (unchecked-inc-int i))))
        params))

    (method ^:private ^:static aritySet ^Set [^Class c ^String methodName ^MethodKind kind]
      (let [^Set res (TreeSet.)
            ^{:tag (List Executable)} methods (QualifiedMethodExpr/methodsWithName
                                                c
                                                methodName
                                                kind)]
        (for-each [^Executable exec methods] (.add res (.getParameterCount exec)))
        res))

    (method ^:public ^:static methodOverloads ^{:tag (List Executable)} [^Class c ^String methodName
                                                                         ^MethodKind kind]
      (let [^Executable/1 methods (.getMethods c)]
        (cast List
              (.collect (.filter (.filter (Arrays/stream methods)
                                          (lambda Predicate ^boolean [^Executable m]
                                            (.equals (.getName m) methodName)))
                                 (lambda Predicate ^boolean [^Executable m]
                                   (switch kind
                                     STATIC (arbace.lang.Compiler/isStaticMethod m)
                                     INSTANCE (arbace.lang.Compiler/isInstanceMethod m)
                                     false)))
                        (Collectors/toList)))))

    (method ^:private ^:static methodsWithName ^{:tag (List Executable)} [^Class c
                                                                          ^String methodName
                                                                          ^MethodKind kind]
      (if (identical? kind MethodKind/CTOR)
          (let [^{:tag (List Executable)} ctors (Arrays/asList (.getConstructors c))]
            (when (.isEmpty ctors)
              (throw (QualifiedMethodExpr/noMethodWithNameException c methodName kind)))
            ctors)
          (let [^{:tag (List Executable)} res (QualifiedMethodExpr/methodOverloads
                                                c
                                                methodName
                                                kind)]
            (when (.isEmpty res)
              (throw (QualifiedMethodExpr/noMethodWithNameException c methodName kind)))
            res)))

    (method ^:static resolveHintedMethod ^Executable [^Class c ^String methodName ^MethodKind kind
                                                      ^{:tag (List Class)} hintedSig]
      (let [^{:tag (List Executable)} methods (QualifiedMethodExpr/methodsWithName
                                                c
                                                methodName
                                                kind)
            arity (.size hintedSig)
            ^{:tag (List Executable)} filteredMethods (cast
                                                        List
                                                        (.collect
                                                          (.filter
                                                            (.filter
                                                              (.filter
                                                                (.stream methods)
                                                                (lambda Predicate ^boolean [^Executable m]
                                                                  (== (.getParameterCount m) arity)))
                                                              (lambda Predicate ^boolean [^Executable m]
                                                                (not (.isSynthetic m))))
                                                            (lambda Predicate ^boolean [^Executable m]
                                                              (arbace.lang.Compiler/signatureMatches
                                                                hintedSig
                                                                m)))
                                                          (Collectors/toList)))]
        (if (== (.size filteredMethods) 1)
            (cast Executable (.get filteredMethods 0))
            (throw (QualifiedMethodExpr/paramTagsDontResolveException c methodName hintedSig)))))

    (method ^:static noMethodWithNameException ^IllegalArgumentException [^Class c
                                                                          ^String methodName
                                                                          ^MethodKind kind]
      (IllegalArgumentException.
        (java-str "Error - no matches found for "
                  (if (not (identical? kind MethodKind/CTOR))
                      (java-str (.toLowerCase (.toString kind)) " ")
                      "")
                  (arbace.lang.Compiler/methodDescription c methodName))))

    (method ^:static paramTagsDontResolveException ^IllegalArgumentException [^Class c
                                                                              ^String methodName
                                                                              ^{:tag (List Class)} hintedSig]
      (let [^IPersistentVector paramTags (PersistentVector/create
                                           (cast
                                             List
                                             (.collect
                                               (.map
                                                 (.stream hintedSig)
                                                 (lambda Function ^Serializable [^Class tag]
                                                   (if (nil? tag)
                                                       ^Serializable PARAM_TAG_ANY
                                                       ^Serializable tag)))
                                               (Collectors/toList))))]
        (IllegalArgumentException.
          (java-str "Error - param-tags "
                    paramTags
                    " insufficient to resolve "
                    (arbace.lang.Compiler/methodDescription c methodName)))))

    (method ^:public ^:static instanceNoTargetException ^IllegalArgumentException [^QualifiedMethodExpr qmexpr]
      (IllegalArgumentException.
        (java-str "Malformed method expression, expecting ("
                  (.getName (.-c qmexpr))
                  "/."
                  (.-methodName qmexpr)
                  " target ...)"))))

  (field ^:static ^:final ^Symbol PARAM_TAG_ANY (Symbol/intern nil "_"))

  (method ^:private ^:static paramTagsOf ^IPersistentVector [^Symbol sym]
    (let [paramTags (RT/get (RT/meta sym) RT/PARAM_TAGS_KEY)]
      (when (and (some? paramTags) (not (instance? IPersistentVector paramTags)))
        (throw (IllegalArgumentException.
                 (java-str "param-tags of symbol " sym " should be a vector."))))
      (cast IPersistentVector paramTags)))

  (method ^:private ^:static tagsToClasses ^{:tag (List Class)} [^IPersistentVector paramTags]
    (when (some? paramTags)
      (let [^{:tag (List Class)} sig (ArrayList.)]
        (loop [s (RT/seq paramTags)]
          (when (some? s)
            (let [t (.first s)]
              (if (.equals t PARAM_TAG_ANY)
                  (do (.add sig nil) (recur (.next s)))
                  (do (.add sig (HostExpr/tagToClass t)) (recur (.next s)))))))
        sig)))

  (method ^:private ^:static signatureMatches ^boolean [^{:tag (List Class)} sig ^Executable method]
    (let [methodSig (.getParameterTypes method)]
      (if (not (== (alength methodSig) (.size sig)))
          false
          (do
            (loop [^int i 0]
              (if (< i (alength methodSig))
                  (if (and (some? (.get sig i))
                           (not (.equals (cast Class (.get sig i)) (aget methodSig i))))
                      (return false)
                      (recur (unchecked-inc-int i)))
                  nil))
            true))))

  (method ^:static isStaticMethod ^boolean [^Executable method]
    (and (instance? java.lang.reflect.Method method) (Modifier/isStatic (.getModifiers method))))

  (method ^:static isInstanceMethod ^boolean [^Executable method]
    (and (instance? java.lang.reflect.Method method)
         (not (Modifier/isStatic (.getModifiers method)))))

  (method ^:static isConstructor ^boolean [^Executable method]
    (instance? Constructor method))

  (method ^:private ^:static checkMethodArity ^void [^Executable method ^int argCount]
    (when-not (== (.getParameterCount method) argCount)
      (throw (IllegalArgumentException.
               (java-str "Invocation of "
                         (arbace.lang.Compiler/methodDescription
                           (.getDeclaringClass method)
                           (if (instance? Constructor method) "new" (.getName method)))
                         " expected "
                         (.getParameterCount method)
                         " arguments, but received "
                         argCount)))))

  (method ^:private ^:static methodDescription ^String [^Class c ^String methodName]
    (let [isCtor (and (some? c) (.equals methodName "new"))
          type (if isCtor "constructor" "method")]
      (java-str type (if isCtor "" (java-str " " methodName)) " in class " (.getName c))))

  (defclass ^:abstract ^:static FieldExpr
    :extends HostExpr)

  (defclass ^:static InstanceFieldExpr
    :extends FieldExpr
    :implements [AssignableExpr]

    (field ^:public ^:final ^Expr target)

    (field ^:public ^:final ^Class targetClass)

    (field ^:public ^:final ^Field field)

    (field ^:public ^:final ^String fieldName)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final ^Symbol tag)

    (field ^:public ^:final ^boolean requireField)

    (field ^:static ^:final ^Method invokeNoArgInstanceMember
      (Method/getMethod "Object invokeNoArgInstanceMember(Object,String,boolean)"))

    (field ^:static ^:final ^Method setInstanceFieldMethod
      (Method/getMethod "Object setInstanceField(Object,String,Object)"))

    (field ^Class jc)

    (constructor ^:public [this ^int line ^int column ^Expr target ^String fieldName ^Symbol tag
                           ^boolean requireField]
      (set! (.-target this) target)
      (set! (.-targetClass this) (when (.hasJavaClass target) (.getJavaClass target)))
      (set! (.-field this)
            (when (some? targetClass) (Reflector/getField targetClass fieldName false)))
      (set! (.-fieldName this) fieldName)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-tag this) tag)
      (set! (.-requireField this) requireField)
      (when (and (nil? field) (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
        (if (nil? targetClass)
            (.format (RT/errPrintWriter)
                     "Reflection warning, %s:%d:%d - reference to field %s can't be resolved.\n"
                     (new Object/1 [(.deref SOURCE_PATH) line column fieldName]))
            (.format
              (RT/errPrintWriter)
              "Reflection warning, %s:%d:%d - reference to field %s on %s can't be resolved.\n"
              (new Object/1 [(.deref SOURCE_PATH) line column fieldName (.getName targetClass)])))))

    (method ^:public eval [this]
      (Reflector/invokeNoArgInstanceMember (.eval target) fieldName requireField))

    (method ^:public canEmitPrimitive ^boolean [this]
      (and (and (some? targetClass) (some? field)) (Util/isPrimitive (.getType field))))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (and (some? targetClass) (some? field))
          (do
            (.emit target C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.checkCast gen (arbace.lang.Compiler/getType targetClass))
            (.getField gen
                       (arbace.lang.Compiler/getType targetClass)
                       fieldName
                       (Type/getType (.getType field))))
          (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (and (some? targetClass) (some? field))
          (do
            (.emit target C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.checkCast gen (arbace.lang.Compiler/getType targetClass))
            (.getField gen
                       (arbace.lang.Compiler/getType targetClass)
                       fieldName
                       (Type/getType (.getType field)))
            (HostExpr/emitBoxReturn objx gen (.getType field))
            (when (identical? context C/STATEMENT) (.pop gen)))
          (do
            (.emit target C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.push gen fieldName)
            (.push gen requireField)
            (.invokeStatic gen REFLECTOR_TYPE invokeNoArgInstanceMember)
            (when (identical? context C/STATEMENT) (.pop gen)))))

    (method ^:public hasJavaClass ^boolean [this]
      (or (some? field) (some? tag)))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (if (some? tag) (HostExpr/tagToClass tag) (.getType field))))
      jc)

    (method ^:public evalAssign [this ^Expr val]
      (Reflector/setInstanceField (.eval target) fieldName (.eval val)))

    (method ^:public emitAssign ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen ^Expr val]
      (if (and (some? targetClass) (some? field))
          (do
            (.emit target C/EXPRESSION objx gen)
            (.checkCast gen (arbace.lang.Compiler/getType targetClass))
            (.emit val C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.dupX1 gen)
            (HostExpr/emitUnboxArg objx gen (.getType field))
            (.putField gen
                       (arbace.lang.Compiler/getType targetClass)
                       fieldName
                       (Type/getType (.getType field))))
          (do
            (.emit target C/EXPRESSION objx gen)
            (.push gen fieldName)
            (.emit val C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.invokeStatic gen REFLECTOR_TYPE setInstanceFieldMethod)))
      (when (identical? context C/STATEMENT) (.pop gen))))

  (defclass ^:static StaticFieldExpr
    :extends FieldExpr
    :implements [AssignableExpr]

    (field ^:public ^:final ^String fieldName)

    (field ^:public ^:final ^Class c)

    (field ^:public ^:final ^Field field)

    (field ^:public ^:final ^Symbol tag)

    (field ^:final ^int line)

    (field ^:final ^int column)

    (field ^Class jc)

    (constructor ^:public [this ^int line ^int column ^Class c ^String fieldName ^Symbol tag]
      (set! (.-fieldName this) fieldName)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-c this) c)
      (try
        (set! field (.getField c fieldName))
        (catch NoSuchFieldException e
          (for-each [^java.lang.reflect.Method m (.getMethods c)]
            (when (and (.equals fieldName (.getName m)) (Modifier/isStatic (.getModifiers m)))
              (throw (IllegalArgumentException.
                       (java-str "No matching method " fieldName " found taking 0 args for " c)))))
          (throw (Util/sneakyThrow e))))
      (set! (.-tag this) tag))

    (method ^:public eval [this] (Reflector/getStaticField c fieldName))

    (method ^:public canEmitPrimitive ^boolean [this]
      (Util/isPrimitive (.getType field)))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.visitLineNumber gen line (.mark gen))
      (.getStatic gen (Type/getType c) fieldName (Type/getType (.getType field))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.visitLineNumber gen line (.mark gen))
      (.getStatic gen (Type/getType c) fieldName (Type/getType (.getType field)))
      (HostExpr/emitBoxReturn objx gen (.getType field))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (if (some? tag) (HostExpr/tagToClass tag) (.getType field))))
      jc)

    (method ^:public evalAssign [this ^Expr val]
      (Reflector/setStaticField c fieldName (.eval val)))

    (method ^:public emitAssign ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen ^Expr val]
      (.emit val C/EXPRESSION objx gen)
      (.visitLineNumber gen line (.mark gen))
      (.dup gen)
      (HostExpr/emitUnboxArg objx gen (.getType field))
      (.putStatic gen (Type/getType c) fieldName (Type/getType (.getType field)))
      (when (identical? context C/STATEMENT) (.pop gen))))

  (method ^:static maybePrimitiveType ^Class [^Expr e]
    (when (and (and (instance? MaybePrimitiveExpr e) (.hasJavaClass e))
               (.canEmitPrimitive (cast MaybePrimitiveExpr e)))
      (let [c (.getJavaClass e)] (when (Util/isPrimitive c) (return c))))
    nil)

  (defclass ^:static FISupport
    (field ^:private ^:static ^:final ^IPersistentSet AFN_FIS
      (^[Object/1] RT/set Callable Runnable Comparator))

    (field ^:private ^:static ^:final ^IPersistentSet OBJECT_METHODS
      (^[Object/1] RT/set "equals" "toString" "hashCode"))

    (method ^:static maybeFIMethod ^java.lang.reflect.Method [^Class target]
      (when (and (and (some? target) (.isAnnotationPresent target FunctionalInterface))
                 (not (.contains AFN_FIS target)))
        (let [methods (.getMethods target)]
          (for-each [^java.lang.reflect.Method method methods]
            (when (and (and (and (>= (.getParameterCount method) 0)
                                 (<= (.getParameterCount method) 10))
                            (Modifier/isAbstract (.getModifiers method)))
                       (not (.contains OBJECT_METHODS (.getName method))))
              (return method)))))
      nil)

    (method ^:private ^:static toInvokerParamType ^Class [^Class c]
      (cond
        (or (or (or (.equals c Byte/TYPE) (.equals c Short/TYPE)) (.equals c Integer/TYPE))
            (.equals c Long/TYPE))
          Long/TYPE
        (or (.equals c Float/TYPE) (.equals c Double/TYPE)) Double/TYPE
        :else Object))

    (method ^:static maybeEmitFIAdapter ^boolean [^ObjExpr objx ^GeneratorAdapter gen ^Expr expr
                                                  ^Class targetClass]
      (let [targetMethod (FISupport/maybeFIMethod targetClass)]
        (if (nil? targetMethod)
            false
            (let [paramCount (.getParameterCount targetMethod)
                  invokerParams (new Class/1 (unchecked-add-int paramCount 1))]
              (aset invokerParams 0 IFn)
              (let [invokeMethodBuilder (StringBuilder. "invoke")]
                (loop [^int i 0]
                  (when (< i paramCount)
                    (aset invokerParams
                          (unchecked-add-int i 1)
                          (if (<= paramCount 2)
                              (FISupport/toInvokerParamType
                                (aget (.getParameterTypes targetMethod) i))
                              Object))
                    (^[char] StringBuilder/.append
                      invokeMethodBuilder
                      (FnInvokers/encodeInvokerType (aget invokerParams (unchecked-add-int i 1))))
                    (recur (unchecked-inc-int i))))
                (let [retType (.getReturnType targetMethod)
                      invokerReturnCode (FnInvokers/encodeInvokerType
                                          (if (<= paramCount 2) retType Object))]
                  (^[char] StringBuilder/.append invokeMethodBuilder invokerReturnCode)
                  (let [invokerMethodName (.toString invokeMethodBuilder)
                        samType (Type/getType targetClass)
                        ifnType (Type/getType IFn)]
                    (try
                      (let [fnInvokerMethod (.getMethod FnInvokers invokerMethodName invokerParams)]
                        (.emit expr C/EXPRESSION objx gen)
                        (.dup gen)
                        (.instanceOf gen ifnType)
                        (let [endLabel (.newLabel gen)]
                          (.ifZCmp gen Opcodes/IFEQ endLabel)
                          (.dup gen)
                          (.instanceOf gen samType)
                          (.ifZCmp gen Opcodes/IFNE endLabel)
                          (FISupport/emitInvokeDynamicAdapter
                            gen
                            targetClass
                            targetMethod
                            FnInvokers
                            fnInvokerMethod)
                          (.mark gen endLabel)
                          (.checkCast gen samType)
                          true))
                      (catch NoSuchMethodException e (throw (Util/sneakyThrow e)))))))))))

    (field ^:private ^:static ^:final ^Handle LMF_HANDLE
      (Handle.
        Opcodes/H_INVOKESTATIC
        "java/lang/invoke/LambdaMetafactory"
        "metafactory"
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
        false))

    (method ^:static emitInvokeDynamicAdapter ^void [^GeneratorAdapter gen ^Class targetClass
                                                     ^java.lang.reflect.Method targetMethod
                                                     ^Class implClass ^Executable implMethod]
      (let [implParams (.getParameterTypes implMethod)
            retClass (if (arbace.lang.Compiler/isConstructor implMethod)
                         implClass
                         (.getReturnType (cast java.lang.reflect.Method implMethod)))
            opCode (cond
                     (arbace.lang.Compiler/isConstructor implMethod) Opcodes/H_INVOKESPECIAL
                     (arbace.lang.Compiler/isStaticMethod implMethod) Opcodes/H_INVOKESTATIC
                     :else Opcodes/H_INVOKEVIRTUAL)
            implHandle (Handle. opCode
                                (Type/getInternalName implClass)
                                (.getName implMethod)
                                (.toMethodDescriptorString
                                  (MethodType/methodType retClass implParams))
                                false)
            ^:mutable implArgCount (alength implParams)]
        (when (arbace.lang.Compiler/isInstanceMethod implMethod)
          (set! implArgCount (unchecked-inc-int implArgCount)))
        (let [lambdaParams (Arrays/asList
                             (cast Class/1
                                   (^[Object/1 int int] Arrays/copyOfRange
                                     implParams
                                     0
                                     (unchecked-subtract-int
                                       implArgCount
                                       (.getParameterCount targetMethod)))))
              lambdaSig (MethodType/methodType targetClass lambdaParams)
              targetType (Type/getType targetMethod)]
          (.visitInvokeDynamicInsn gen
                                   (.getName targetMethod)
                                   (.toMethodDescriptorString lambdaSig)
                                   LMF_HANDLE
                                   (new Object/1 [targetType implHandle targetType]))))))

  (method ^:static maybeJavaClass ^Class [^{:tag (Collection Expr)} exprs]
    (let [^:mutable ^Class match nil]
      (try
        (for-each [^Expr e exprs]
          (when-not (instance? ThrowExpr e)
            (when-not (.hasJavaClass e) (return nil))
            (let [c (.getJavaClass e)]
              (when (nil? c) (return nil))
              (if (nil? match) (set! match c) (when-not (identical? match c) (return nil))))))
        (catch Exception e (return nil)))
      match))

  (defclass ^:abstract ^:static MethodExpr
    :extends HostExpr

    (method ^:static emitArgsAsArray ^void [^IPersistentVector args ^ObjExpr objx
                                            ^GeneratorAdapter gen]
      (.push gen (.count args))
      (.newArray gen OBJECT_TYPE)
      (loop [^int i 0]
        (when (< i (.count args))
          (.dup gen)
          (.push gen i)
          (.emit (cast Expr (.nth args i)) C/EXPRESSION objx gen)
          (.arrayStore gen OBJECT_TYPE)
          (recur (unchecked-inc-int i)))))

    (method ^:public ^:static emitTypedArgs ^void [^ObjExpr objx ^GeneratorAdapter gen
                                                   ^Class/1 parameterTypes ^IPersistentVector args]
      (loop [^int i 0]
        (when (< i (alength parameterTypes))
          (let [e (cast Expr (.nth args i))]
            (try
              (let [primc (arbace.lang.Compiler/maybePrimitiveType e)]
                (cond
                  (identical? primc (aget parameterTypes i))
                    (let [pe (cast MaybePrimitiveExpr e)] (.emitUnboxed pe C/EXPRESSION objx gen))
                  (and (identical? primc Integer/TYPE)
                       (identical? (aget parameterTypes i) Long/TYPE))
                    (let [pe (cast MaybePrimitiveExpr e)]
                      (.emitUnboxed pe C/EXPRESSION objx gen)
                      (.visitInsn gen Opcodes/I2L))
                  (and (identical? primc Long/TYPE)
                       (identical? (aget parameterTypes i) Integer/TYPE))
                    (let [pe (cast MaybePrimitiveExpr e)]
                      (.emitUnboxed pe C/EXPRESSION objx gen)
                      (if (RT/booleanCast (.deref RT/UNCHECKED_MATH))
                          (.invokeStatic gen
                                         RT_TYPE
                                         (Method/getMethod "int uncheckedIntCast(long)"))
                          (.invokeStatic gen RT_TYPE (Method/getMethod "int intCast(long)"))))
                  (and (identical? primc Float/TYPE)
                       (identical? (aget parameterTypes i) Double/TYPE))
                    (let [pe (cast MaybePrimitiveExpr e)]
                      (.emitUnboxed pe C/EXPRESSION objx gen)
                      (.visitInsn gen Opcodes/F2D))
                  (and (identical? primc Double/TYPE)
                       (identical? (aget parameterTypes i) Float/TYPE))
                    (let [pe (cast MaybePrimitiveExpr e)]
                      (.emitUnboxed pe C/EXPRESSION objx gen)
                      (.visitInsn gen Opcodes/D2F))
                  :else
                    (when-not (FISupport/maybeEmitFIAdapter objx gen e (aget parameterTypes i))
                      (.emit e C/EXPRESSION objx gen)
                      (HostExpr/emitUnboxArg objx gen (aget parameterTypes i)))))
              (catch Exception e1 (throw (Util/sneakyThrow e1))))
            (recur (unchecked-inc-int i)))))))

  (defclass ^:static InstanceMethodExpr
    :extends MethodExpr

    (field ^:public ^:final ^Expr target)

    (field ^:public ^:final ^String methodName)

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^String source)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final ^Symbol tag)

    (field ^:public ^:final ^boolean tailPosition)

    (field ^:public ^:final ^java.lang.reflect.Method method)

    (field ^:public ^:final ^Class qualifyingClass)

    (field ^Class jc)

    (field ^:static ^:final ^Method invokeInstanceMethodMethod
      (Method/getMethod "Object invokeInstanceMethod(Object,String,Object[])"))

    (field ^:static ^:final ^Method invokeInstanceMethodOfClassMethod
      (Method/getMethod "Object invokeInstanceMethodOfClass(Object,String,String,Object[])"))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^Expr target
                           ^Class qualifyingClass ^String methodName
                           ^java.lang.reflect.Method resolvedMethod ^IPersistentVector args
                           ^boolean tailPosition]
      (arbace.lang.Compiler/checkMethodArity resolvedMethod (RT/count args))
      (set! (.-source this) source)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-args this) args)
      (set! (.-methodName this) methodName)
      (set! (.-target this) target)
      (set! (.-tag this) tag)
      (set! (.-tailPosition this) tailPosition)
      (set! (.-method this) resolvedMethod)
      (set! (.-qualifyingClass this) qualifyingClass))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^Expr target
                           ^Class qualifyingClass ^String methodName ^IPersistentVector args
                           ^boolean tailPosition]
      (set! (.-source this) source)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-args this) args)
      (set! (.-methodName this) methodName)
      (set! (.-target this) target)
      (set! (.-tag this) tag)
      (set! (.-tailPosition this) tailPosition)
      (set! (.-qualifyingClass this) qualifyingClass)
      (let [contextClass (cond
                           (some? qualifyingClass) qualifyingClass
                           (.hasJavaClass target) (.getJavaClass target))]
        (if (some? contextClass)
            (let [methods (Reflector/getMethods contextClass (.count args) methodName false)]
              (if (.isEmpty methods)
                  (do
                    (set! method nil)
                    (when (RT/booleanCast (.deref RT/WARN_ON_REFLECTION))
                      (.format
                        (RT/errPrintWriter)
                        "Reflection warning, %s:%d:%d - call to method %s on %s can't be resolved (no such method).\n"
                        (new Object/1
                             [(.deref SOURCE_PATH) line column methodName (.getName contextClass)]))))
                  (let [^:mutable ^int methodidx 0]
                    (when (> (.size methods) 1)
                      (let [^{:tag (ArrayList Class/1)} params (ArrayList.)
                            ^{:tag (ArrayList Class)} rets (ArrayList.)]
                        (loop [^int i 0]
                          (when (< i (.size methods))
                            (let [m (cast java.lang.reflect.Method (.get methods i))]
                              (.add params (.getParameterTypes m))
                              (.add rets (.getReturnType m))
                              (recur (unchecked-inc-int i)))))
                        (set! methodidx
                              (arbace.lang.Compiler/getMatchingParams methodName params args rets))))
                    (let [^:mutable m (cast java.lang.reflect.Method
                                            (when (>= methodidx 0) (.get methods methodidx)))]
                      (when (and (some? m)
                                 (not (Modifier/isPublic (.getModifiers (.getDeclaringClass m)))))
                        (set! m (Reflector/getAsMethodOfPublicBase (.getDeclaringClass m) m)))
                      (set! method m)
                      (when (and (nil? method) (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
                        (.format
                          (RT/errPrintWriter)
                          "Reflection warning, %s:%d:%d - call to method %s on %s can't be resolved (argument types: %s).\n"
                          (new Object/1
                               [(.deref SOURCE_PATH)
                                line
                                column
                                methodName
                                (.getName contextClass)
                                (arbace.lang.Compiler/getTypeStringForArgs args)])))))))
            (do
              (set! method nil)
              (when (RT/booleanCast (.deref RT/WARN_ON_REFLECTION))
                (.format
                  (RT/errPrintWriter)
                  "Reflection warning, %s:%d:%d - call to method %s can't be resolved (target class is unknown).\n"
                  (new Object/1 [(.deref SOURCE_PATH) line column methodName])))))))

    (method ^:public eval [this]
      (try
        (let [targetval (.eval target)
              argvals (new Object/1 (.count args))]
          (loop [^int i 0]
            (when (< i (.count args))
              (aset argvals i (.eval (cast Expr (.nth args i))))
              (recur (unchecked-inc-int i))))
          (cond
            (some? method)
              (let [ms (LinkedList.)]
                (.add ms method)
                (Reflector/invokeMatchingMethod methodName ms targetval argvals))
            (some? qualifyingClass)
              (Reflector/invokeInstanceMethodOfClass targetval qualifyingClass methodName argvals)
            :else (Reflector/invokeInstanceMethod targetval methodName argvals)))
        (catch Throwable e
          (if (not (instance? CompilerException e))
              (throw
                (CompilerException. source line column nil CompilerException/PHASE_EXECUTION e))
              (throw (cast CompilerException e))))))

    (method ^:public canEmitPrimitive ^boolean [this]
      (and (some? method) (Util/isPrimitive (.getReturnType method))))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (some? method)
          (let [type (Type/getType (.getDeclaringClass method))]
            (.emit target C/EXPRESSION objx gen)
            (.checkCast gen type)
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes method) args)
            (.visitLineNumber gen line (.mark gen))
            (when (and tailPosition (not (.-canBeDirect objx)))
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearThis method gen)))
            (let [m (Method. methodName (Type/getReturnType method) (Type/getArgumentTypes method))]
              (if (.isInterface (.getDeclaringClass method))
                  (.invokeInterface gen type m)
                  (.invokeVirtual gen type m))))
          (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (some? method)
          (let [type (Type/getType (.getDeclaringClass method))]
            (.emit target C/EXPRESSION objx gen)
            (.checkCast gen type)
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes method) args)
            (.visitLineNumber gen line (.mark gen))
            (when (identical? context C/RETURN)
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
            (let [m (Method. methodName (Type/getReturnType method) (Type/getArgumentTypes method))]
              (if (.isInterface (.getDeclaringClass method))
                  (.invokeInterface gen type m)
                  (.invokeVirtual gen type m))
              (let [retClass (.getReturnType method)]
                (if (identical? context C/STATEMENT)
                    (if (or (identical? retClass Long/TYPE) (identical? retClass Double/TYPE))
                        (.pop2 gen)
                        (when-not (identical? retClass Void/TYPE) (.pop gen)))
                    (HostExpr/emitBoxReturn objx gen retClass)))))
          (do
            (.emit target C/EXPRESSION objx gen)
            (when (some? qualifyingClass) (.push gen (.getName qualifyingClass)))
            (.push gen methodName)
            (InstanceMethodExpr/emitArgsAsArray args objx gen)
            (.visitLineNumber gen line (.mark gen))
            (when (identical? context C/RETURN)
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
            (if (some? qualifyingClass)
                (.invokeStatic gen REFLECTOR_TYPE invokeInstanceMethodOfClassMethod)
                (.invokeStatic gen REFLECTOR_TYPE invokeInstanceMethodMethod))
            (when (identical? context C/STATEMENT) (.pop gen)))))

    (method ^:public hasJavaClass ^boolean [this]
      (or (some? method) (some? tag)))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (set! jc
              (arbace.lang.Compiler/retType (when (some? tag) (HostExpr/tagToClass tag))
                                            (when (some? method) (.getReturnType method)))))
      jc))

  (defclass ^:static StaticMethodExpr
    :extends MethodExpr

    (field ^:public ^:final ^Class c)

    (field ^:public ^:final ^String methodName)

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^String source)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final ^java.lang.reflect.Method method)

    (field ^:public ^:final ^Symbol tag)

    (field ^:public ^:final ^boolean tailPosition)

    (field ^:static ^:final ^Method forNameMethod (Method/getMethod "Class classForName(String)"))

    (field ^:static ^:final ^Method invokeStaticMethodMethod
      (Method/getMethod "Object invokeStaticMethod(Class,String,Object[])"))

    (field ^:static ^:final ^Keyword warnOnBoxedKeyword (Keyword/intern "warn-on-boxed"))

    (field ^Class jc)

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^Class c
                           ^String methodName ^java.lang.reflect.Method preferredMethod
                           ^IPersistentVector args ^boolean tailPosition]
      (arbace.lang.Compiler/checkMethodArity preferredMethod (RT/count args))
      (set! (.-c this) c)
      (set! (.-methodName this) methodName)
      (set! (.-args this) args)
      (set! (.-source this) source)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-tag this) tag)
      (set! (.-tailPosition this) tailPosition)
      (set! (.-method this) preferredMethod)
      (when (and (and (some? method) (.equals warnOnBoxedKeyword (.deref RT/UNCHECKED_MATH)))
                 (StaticMethodExpr/isBoxedMath method))
        (.format (RT/errPrintWriter)
                 "Boxed math warning, %s:%d:%d - call: %s.\n"
                 (new Object/1 [(.deref SOURCE_PATH) line column (.toString method)]))))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^Class c
                           ^String methodName ^IPersistentVector args ^boolean tailPosition]
      (set! (.-c this) c)
      (set! (.-methodName this) methodName)
      (set! (.-args this) args)
      (set! (.-source this) source)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-tag this) tag)
      (set! (.-tailPosition this) tailPosition)
      (let [methods (Reflector/getMethods c (.count args) methodName true)]
        (when (.isEmpty methods)
          (throw (IllegalArgumentException.
                   (java-str "No matching method "
                             methodName
                             " found taking "
                             (.count args)
                             " args for "
                             c))))
        (let [^:mutable ^int methodidx 0]
          (when (> (.size methods) 1)
            (let [^{:tag (ArrayList Class/1)} params (ArrayList.)
                  ^{:tag (ArrayList Class)} rets (ArrayList.)]
              (loop [^int i 0]
                (when (< i (.size methods))
                  (let [m (cast java.lang.reflect.Method (.get methods i))]
                    (.add params (.getParameterTypes m))
                    (.add rets (.getReturnType m))
                    (recur (unchecked-inc-int i)))))
              (set! methodidx (arbace.lang.Compiler/getMatchingParams methodName params args rets))))
          (set! method
                (cast java.lang.reflect.Method (when (>= methodidx 0) (.get methods methodidx))))
          (when (and (nil? method) (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
            (.format
              (RT/errPrintWriter)
              "Reflection warning, %s:%d:%d - call to static method %s on %s can't be resolved (argument types: %s).\n"
              (new Object/1
                   [(.deref SOURCE_PATH)
                    line
                    column
                    methodName
                    (.getName c)
                    (arbace.lang.Compiler/getTypeStringForArgs args)])))
          (when (and (and (some? method) (.equals warnOnBoxedKeyword (.deref RT/UNCHECKED_MATH)))
                     (StaticMethodExpr/isBoxedMath method))
            (.format (RT/errPrintWriter)
                     "Boxed math warning, %s:%d:%d - call: %s.\n"
                     (new Object/1 [(.deref SOURCE_PATH) line column (.toString method)]))))))

    (method ^:public ^:static isBoxedMath ^boolean [^java.lang.reflect.Method m]
      (let [c (.getDeclaringClass m)]
        (when (.equals c Numbers)
          (let [boxedMath (cast WarnBoxedMath (.getAnnotation m WarnBoxedMath))]
            (when (some? boxedMath) (return (.value boxedMath)))
            (let [argTypes (.getParameterTypes m)]
              (for-each [^Class argType argTypes]
                (when (or (.equals argType Object) (.equals argType Number)) (return true))))))
        false))

    (method ^:public eval [this]
      (try
        (let [argvals (new Object/1 (.count args))]
          (loop [^int i 0]
            (when (< i (.count args))
              (aset argvals i (.eval (cast Expr (.nth args i))))
              (recur (unchecked-inc-int i))))
          (if (some? method)
              (let [ms (LinkedList.)]
                (.add ms method)
                (Reflector/invokeMatchingMethod methodName ms nil argvals))
              (Reflector/invokeStaticMethod c methodName argvals)))
        (catch Throwable e
          (if (not (instance? CompilerException e))
              (throw
                (CompilerException. source line column nil CompilerException/PHASE_EXECUTION e))
              (throw (cast CompilerException e))))))

    (method ^:public canEmitPrimitive ^boolean [this]
      (and (some? method) (Util/isPrimitive (.getReturnType method))))

    (method ^:public canEmitIntrinsicPredicate ^boolean [this]
      (and (some? method) (some? (RT/get Intrinsics/preds (.toString method)))))

    (method ^:public emitIntrinsicPredicate ^void [this ^C context ^ObjExpr objx
                                                   ^GeneratorAdapter gen ^Label falseLabel]
      (.visitLineNumber gen line (.mark gen))
      (if (some? method)
          (do
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes method) args)
            (when (identical? context C/RETURN)
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
            (let [predOps (cast Object/1 (RT/get Intrinsics/preds (.toString method)))]
              (loop [^int i 0]
                (when (< i (unchecked-subtract-int (alength predOps) 1))
                  (.visitInsn gen (cast Integer (aget predOps i)))
                  (recur (unchecked-inc-int i))))
              (.visitJumpInsn gen
                              (cast Integer
                                    (aget predOps (unchecked-subtract-int (alength predOps) 1)))
                              falseLabel)))
          (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (some? method)
          (do
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes method) args)
            (.visitLineNumber gen line (.mark gen))
            (when (identical? context C/RETURN)
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
            (let [ops (RT/get Intrinsics/ops (.toString method))]
              (if (some? ops)
                  (if (instance? Object/1 ops)
                      (for-each [op (cast Object/1 ops)] (.visitInsn gen (cast Integer op)))
                      (.visitInsn gen (cast Integer ops)))
                  (let [type (Type/getType c)
                        m (Method. methodName
                                   (Type/getReturnType method)
                                   (Type/getArgumentTypes method))]
                    (.visitMethodInsn gen
                                      Opcodes/INVOKESTATIC
                                      (.getInternalName type)
                                      methodName
                                      (.getDescriptor m)
                                      (.isInterface c))))))
          (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (some? method)
          (do
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes method) args)
            (.visitLineNumber gen line (.mark gen))
            (when (and tailPosition (not (.-canBeDirect objx)))
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearThis method gen)))
            (let [type (Type/getType c)
                  m (Method. methodName (Type/getReturnType method) (Type/getArgumentTypes method))]
              (.visitMethodInsn gen
                                Opcodes/INVOKESTATIC
                                (.getInternalName type)
                                methodName
                                (.getDescriptor m)
                                (.isInterface c))
              (let [retClass (.getReturnType method)]
                (if (identical? context C/STATEMENT)
                    (if (or (identical? retClass Long/TYPE) (identical? retClass Double/TYPE))
                        (.pop2 gen)
                        (when-not (identical? retClass Void/TYPE) (.pop gen)))
                    (HostExpr/emitBoxReturn objx gen (.getReturnType method))))))
          (do
            (.visitLineNumber gen line (.mark gen))
            (.push gen (.getName c))
            (.invokeStatic gen RT_TYPE forNameMethod)
            (.push gen methodName)
            (StaticMethodExpr/emitArgsAsArray args objx gen)
            (.visitLineNumber gen line (.mark gen))
            (when (identical? context C/RETURN)
              (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
            (.invokeStatic gen REFLECTOR_TYPE invokeStaticMethodMethod)
            (when (identical? context C/STATEMENT) (.pop gen)))))

    (method ^:public hasJavaClass ^boolean [this]
      (or (some? method) (some? tag)))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (set! jc
              (arbace.lang.Compiler/retType (when (some? tag) (HostExpr/tagToClass tag))
                                            (when (some? method) (.getReturnType method)))))
      jc))

  (defclass ^:static UnresolvedVarExpr
    :implements [Expr]

    (field ^:public ^:final ^Symbol symbol)

    (constructor ^:public [this ^Symbol symbol]
      (set! (.-symbol this) symbol))

    (method ^:public hasJavaClass ^boolean [this] false)

    (method ^:public getJavaClass ^Class [this]
      (throw (IllegalArgumentException. "UnresolvedVarExpr has no Java class")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen])

    (method ^:public eval [this]
      (throw (IllegalArgumentException. "UnresolvedVarExpr cannot be evalled"))))

  (defclass ^:static NumberExpr
    :extends LiteralExpr
    :implements [MaybePrimitiveExpr]

    (field ^:final ^Number n)

    (field ^:public ^:final ^int id)

    (constructor ^:public [this ^Number n]
      (set! (.-n this) n)
      (set! (.-id this) (arbace.lang.Compiler/registerConstant n)))

    (method val [this] n)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (when-not (identical? context C/STATEMENT) (.emitConstant objx gen id)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this]
      (cond
        (instance? Integer n) Long/TYPE
        (instance? Double n) Double/TYPE
        (instance? Long n) Long/TYPE
        :else
          (throw (IllegalStateException.
                   (java-str "Unsupported Number type: " (.getName (.getClass n)))))))

    (method ^:public canEmitPrimitive ^boolean [this] true)

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (cond
        (instance? Integer n) (^[long] GeneratorAdapter/.push gen (.longValue n))
        (instance? Double n) (^[double] GeneratorAdapter/.push gen (.doubleValue n))
        (instance? Long n) (^[long] GeneratorAdapter/.push gen (.longValue n))))

    (method ^:public ^:static parse ^Expr [^Number form]
      (if (or (or (instance? Integer form) (instance? Double form)) (instance? Long form))
          (NumberExpr. form)
          (ConstantExpr. form))))

  (defclass ^:static ConstantExpr
    :extends LiteralExpr

    (field ^:public ^:final v)

    (field ^:public ^:final ^int id)

    (constructor ^:public [this v]
      (set! (.-v this) v)
      (set! (.-id this) (arbace.lang.Compiler/registerConstant v)))

    (method val [this] v)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitConstant objx gen id)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this]
      (Modifier/isPublic (.getModifiers (.getClass v))))

    (method ^:public getJavaClass ^Class [this]
      (cond
        (instance? APersistentMap v) APersistentMap
        (instance? APersistentSet v) APersistentSet
        (instance? APersistentVector v) APersistentVector
        :else (.getClass v)))

    (defclass ^:static Parser
      :implements [IParser]

      (field ^:static ^Keyword formKey (Keyword/intern "form"))

      (method ^:public parse ^Expr [this ^C context form]
        (let [argCount (unchecked-subtract-int (RT/count form) 1)]
          (when-not (== argCount 1)
            (let [^IPersistentMap exData (PersistentArrayMap. (new Object/1 [formKey form]))]
              (throw (arbace.lang.ExceptionInfo.
                       (java-str "Wrong number of args (" argCount ") passed to quote")
                       exData))))
          (let [v (RT/second form)]
            (cond
              (nil? v) NIL_EXPR
              (identical? v Boolean/TRUE) TRUE_EXPR
              (identical? v Boolean/FALSE) FALSE_EXPR
              (instance? Number v) (NumberExpr/parse (cast Number v))
              (instance? String v) (StringExpr. (cast String v))
              (and (and (instance? IPersistentCollection v)
                        (== (.count (cast IPersistentCollection v)) 0))
                   (or (not (instance? IObj v)) (nil? (.meta (cast IObj v)))))
                (EmptyExpr. v)
              :else (ConstantExpr. v)))))))

  (defclass ^:static NilExpr
    :extends LiteralExpr

    (method val [this] nil)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.visitInsn gen Opcodes/ACONST_NULL)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] nil))

  (field ^:static ^:final ^NilExpr NIL_EXPR (NilExpr.))

  (defclass ^:static BooleanExpr
    :extends LiteralExpr

    (field ^:public ^:final ^boolean val)

    (constructor ^:public [this ^boolean val] (set! (.-val this) val))

    (method val [this] (if val RT/T RT/F))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if val
          (.getStatic gen BOOLEAN_OBJECT_TYPE "TRUE" BOOLEAN_OBJECT_TYPE)
          (.getStatic gen BOOLEAN_OBJECT_TYPE "FALSE" BOOLEAN_OBJECT_TYPE))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] Boolean))

  (field ^:static ^:final ^BooleanExpr TRUE_EXPR (BooleanExpr. true))

  (field ^:static ^:final ^BooleanExpr FALSE_EXPR (BooleanExpr. false))

  (defclass ^:static StringExpr
    :extends LiteralExpr

    (field ^:public ^:final ^String str)

    (constructor ^:public [this ^String str] (set! (.-str this) str))

    (method val [this] str)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (when-not (identical? context C/STATEMENT) (.push gen str)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] String))

  (defclass ^:static MonitorEnterExpr
    :extends UntypedExpr

    (field ^:final ^Expr target)

    (constructor ^:public [this ^Expr target] (set! (.-target this) target))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval monitor-enter")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit target C/EXPRESSION objx gen)
      (.monitorEnter gen)
      (.emit NIL_EXPR context objx gen))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context form]
        (MonitorEnterExpr. (arbace.lang.Compiler/analyze C/EXPRESSION (RT/second form))))))

  (defclass ^:static MonitorExitExpr
    :extends UntypedExpr

    (field ^:final ^Expr target)

    (constructor ^:public [this ^Expr target] (set! (.-target this) target))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval monitor-exit")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit target C/EXPRESSION objx gen)
      (.monitorExit gen)
      (.emit NIL_EXPR context objx gen))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context form]
        (MonitorExitExpr. (arbace.lang.Compiler/analyze C/EXPRESSION (RT/second form))))))

  (defclass ^:public ^:static TryExpr
    :implements [Expr]

    (field ^:public ^:final ^Expr tryExpr)

    (field ^:public ^:final ^Expr finallyExpr)

    (field ^:public ^:final ^PersistentVector catchExprs)

    (field ^:public ^:final ^int retLocal)

    (field ^:public ^:final ^int finallyLocal)

    (defclass ^:public ^:static CatchClause
      (field ^:public ^:final ^Class c)

      (field ^:public ^:final ^LocalBinding lb)

      (field ^:public ^:final ^Expr handler)

      (field ^Label label)

      (field ^Label endLabel)

      (constructor ^:public [this ^Class c ^LocalBinding lb ^Expr handler]
        (set! (.-c this) c)
        (set! (.-lb this) lb)
        (set! (.-handler this) handler)))

    (constructor ^:public [this ^Expr tryExpr ^PersistentVector catchExprs ^Expr finallyExpr
                           ^int retLocal ^int finallyLocal]
      (set! (.-tryExpr this) tryExpr)
      (set! (.-catchExprs this) catchExprs)
      (set! (.-finallyExpr this) finallyExpr)
      (set! (.-retLocal this) retLocal)
      (set! (.-finallyLocal this) finallyLocal))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval try")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [startTry (.newLabel gen)
            endTry (.newLabel gen)
            end (.newLabel gen)
            ret (.newLabel gen)
            finallyLabel (.newLabel gen)]
        (loop [^int i 0]
          (when (< i (.count catchExprs))
            (let [clause (cast CatchClause (.nth catchExprs i))]
              (set! (.-label clause) (.newLabel gen))
              (set! (.-endLabel clause) (.newLabel gen))
              (recur (unchecked-inc-int i)))))
        (.mark gen startTry)
        (.emit tryExpr context objx gen)
        (when-not (identical? context C/STATEMENT)
          (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) retLocal))
        (.mark gen endTry)
        (when (some? finallyExpr) (.emit finallyExpr C/STATEMENT objx gen))
        (.goTo gen ret)
        (loop [^int i 0]
          (when (< i (.count catchExprs))
            (let [clause (cast CatchClause (.nth catchExprs i))]
              (.mark gen (.-label clause))
              (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) (.-idx (.-lb clause)))
              (.emit (.-handler clause) context objx gen)
              (when-not (identical? context C/STATEMENT)
                (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) retLocal))
              (.mark gen (.-endLabel clause))
              (when (some? finallyExpr) (.emit finallyExpr C/STATEMENT objx gen))
              (.goTo gen ret)
              (recur (unchecked-inc-int i)))))
        (when (some? finallyExpr)
          (.mark gen finallyLabel)
          (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) finallyLocal)
          (.emit finallyExpr C/STATEMENT objx gen)
          (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ILOAD) finallyLocal)
          (.throwException gen))
        (.mark gen ret)
        (when-not (identical? context C/STATEMENT)
          (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ILOAD) retLocal))
        (.mark gen end)
        (loop [^int i 0]
          (when (< i (.count catchExprs))
            (let [clause (cast CatchClause (.nth catchExprs i))]
              (.visitTryCatchBlock gen
                                   startTry
                                   endTry
                                   (.-label clause)
                                   (.replace (.getName (.-c clause)) \. \/))
              (recur (unchecked-inc-int i)))))
        (when (some? finallyExpr)
          (.visitTryCatchBlock gen startTry endTry finallyLabel nil)
          (loop [^int i 0]
            (when (< i (.count catchExprs))
              (let [clause (cast CatchClause (.nth catchExprs i))]
                (.visitTryCatchBlock gen (.-label clause) (.-endLabel clause) finallyLabel nil)
                (recur (unchecked-inc-int i))))))
        (loop [^int i 0]
          (when (< i (.count catchExprs))
            (let [clause (cast CatchClause (.nth catchExprs i))]
              (.visitLocalVariable gen
                                   (.-name (.-lb clause))
                                   "Ljava/lang/Object;"
                                   nil
                                   (.-label clause)
                                   (.-endLabel clause)
                                   (.-idx (.-lb clause)))
              (recur (unchecked-inc-int i)))))))

    (method ^:public hasJavaClass ^boolean [this] (.hasJavaClass tryExpr))

    (method ^:public getJavaClass ^Class [this] (.getJavaClass tryExpr))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (if (not (identical? context C/RETURN))
              (arbace.lang.Compiler/analyze context
                                            (RT/list (RT/list FNONCE PersistentVector/EMPTY form)))
              (let [^:mutable body PersistentVector/EMPTY
                    ^:mutable catches PersistentVector/EMPTY
                    ^:mutable ^Expr bodyExpr nil
                    ^:mutable ^Expr finallyExpr nil
                    ^:mutable caught false
                    retLocal (arbace.lang.Compiler/getAndIncLocalNum)
                    finallyLocal (arbace.lang.Compiler/getAndIncLocalNum)]
                (loop [fs (.next form)]
                  (when (some? fs)
                    (let [f (.first fs)
                          op (when (instance? ISeq f) (.first (cast ISeq f)))]
                      (if (and (not (Util/equals op CATCH)) (not (Util/equals op FINALLY)))
                          (do
                            (when caught
                              (throw
                                (Util/runtimeException
                                  "Only catch or finally clause can follow catch in try expression")))
                            (set! body (.cons body f))
                            (recur (.next fs)))
                          (do
                            (when (nil? bodyExpr)
                              (try
                                (Var/pushThreadBindings
                                  (^[Object/1] RT/map NO_RECUR true METHOD_RETURN_CONTEXT nil))
                                (set! bodyExpr
                                      (.parse (Compiler$BodyExpr$Parser.) context (RT/seq body)))
                                (finally (Var/popThreadBindings))))
                            (if (Util/equals op CATCH)
                                (let [c (HostExpr/maybeClass (RT/second f) false)]
                                  (when (nil? c)
                                    (throw (IllegalArgumentException.
                                             (java-str
                                               "Unable to resolve classname: "
                                               (RT/second f)))))
                                  (when-not (instance? Symbol (RT/third f))
                                    (throw (IllegalArgumentException.
                                             (java-str
                                               "Bad binding form, expected symbol, got: "
                                               (RT/third f)))))
                                  (let [sym (cast Symbol (RT/third f))]
                                    (when (some? (.getNamespace sym))
                                      (throw (Util/runtimeException
                                               (java-str "Can't bind qualified name:" sym))))
                                    (let [dynamicBindings (^[Object/1] RT/map
                                                            LOCAL_ENV
                                                            (.deref LOCAL_ENV)
                                                            NEXT_LOCAL_NUM
                                                            (.deref NEXT_LOCAL_NUM)
                                                            IN_CATCH_FINALLY
                                                            RT/T)]
                                      (try
                                        (Var/pushThreadBindings dynamicBindings)
                                        (let [lb (arbace.lang.Compiler/registerLocal
                                                   sym
                                                   (cast
                                                     Symbol
                                                     (when (instance? Symbol (RT/second f))
                                                       (RT/second f)))
                                                   nil
                                                   false)
                                              handler (.parse
                                                        (Compiler$BodyExpr$Parser.)
                                                        C/EXPRESSION
                                                        (RT/next (RT/next (RT/next f))))]
                                          (set! catches (.cons catches (CatchClause. c lb handler))))
                                        (finally (Var/popThreadBindings)))
                                      (set! caught true)
                                      (recur (.next fs)))))
                                (do
                                  (when (some? (.next fs))
                                    (throw (Util/runtimeException
                                             "finally clause must be last in try expression")))
                                  (try
                                    (Var/pushThreadBindings
                                      (^[Object/1] RT/map IN_CATCH_FINALLY RT/T))
                                    (set! finallyExpr
                                          (.parse
                                            (Compiler$BodyExpr$Parser.)
                                            C/STATEMENT
                                            (RT/next f)))
                                    (finally (Var/popThreadBindings)))
                                  (recur (.next fs)))))))))
                (if (nil? bodyExpr)
                    (do
                      (try
                        (Var/pushThreadBindings (^[Object/1] RT/map NO_RECUR true))
                        (set! bodyExpr (.parse (Compiler$BodyExpr$Parser.) context (RT/seq body)))
                        (finally (Var/popThreadBindings)))
                      bodyExpr)
                    (TryExpr. bodyExpr catches finallyExpr retLocal finallyLocal))))))))

  (defclass ^:static ThrowExpr
    :extends UntypedExpr

    (field ^:public ^:final ^Expr excExpr)

    (constructor ^:public [this ^Expr excExpr]
      (set! (.-excExpr this) excExpr))

    (method ^:public eval [this]
      (throw (Util/runtimeException "Can't eval throw")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit excExpr C/EXPRESSION objx gen)
      (.checkCast gen THROWABLE_TYPE)
      (.throwException gen))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context form]
        (if (identical? context C/EVAL)
            (arbace.lang.Compiler/analyze context
                                          (RT/list (RT/list FNONCE PersistentVector/EMPTY form)))
            (do
              (cond
                (== (RT/count form) 1)
                  (throw (Util/runtimeException
                           "Too few arguments to throw, throw expects a single Throwable instance"))
                (> (RT/count form) 2)
                  (throw
                    (Util/runtimeException
                      "Too many arguments to throw, throw expects a single Throwable instance")))
              (ThrowExpr. (arbace.lang.Compiler/analyze C/EXPRESSION (RT/second form))))))))

  (method ^:public ^:static subsumes ^boolean [^Class/1 c1 ^Class/1 c2]
    (let [^:mutable ^Boolean better false]
      (loop [^int i 0]
        (if (< i (alength c1))
            (if (not (identical? (aget c1 i) (aget c2 i)))
                (if (or (and (not (.isPrimitive (aget c1 i))) (.isPrimitive (aget c2 i)))
                        (.isAssignableFrom (aget c2 i) (aget c1 i)))
                    (do (set! better true) (recur (unchecked-inc-int i)))
                    (return false))
                (recur (unchecked-inc-int i)))
            nil))
      better))

  (method ^:static getTypeStringForArgs ^String [^IPersistentVector args]
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i (.count args))
          (let [arg (cast Expr (.nth args i))]
            (when (> i 0) (.append sb ", "))
            (.append sb
                     (if (and (.hasJavaClass arg) (some? (.getJavaClass arg)))
                         (.getName (.getJavaClass arg))
                         "unknown"))
            (recur (unchecked-inc-int i)))))
      (.toString sb)))

  (method ^:static getMatchingParams ^int [^String methodName ^{:tag (ArrayList Class/1)} paramlists
                                           ^IPersistentVector argexprs ^{:tag (List Class)} rets]
    (let [^:mutable ^int matchIdx -1
          ^:mutable tied false
          ^:mutable foundExact false]
      (loop [^int i 0]
        (when (< i (.size paramlists))
          (let [^:mutable match true
                ^:mutable aseq (.seq argexprs)
                ^:mutable ^int exact 0]
            (let [^:mutable ^int p 0]
              (while (and (and match (< p (.count argexprs))) (some? aseq))
                (let [arg (cast Expr (.first aseq))
                      aclass (if (.hasJavaClass arg) (.getJavaClass arg) Object)
                      pclass (aget (cast Class/1 (.get paramlists i)) p)]
                  (if (and (.hasJavaClass arg) (identical? aclass pclass))
                      (set! exact (unchecked-inc-int exact))
                      (set! match (Reflector/paramArgTypeMatch pclass aclass))))
                (set! p (unchecked-inc-int p))
                (set! aseq (.next aseq))))
            (cond
              (== exact (.count argexprs))
                (do
                  (when (or (or (not foundExact) (== matchIdx -1))
                            (.isAssignableFrom (cast Class (.get rets matchIdx))
                                               (cast Class (.get rets i))))
                    (set! matchIdx i))
                  (set! tied false)
                  (set! foundExact true)
                  (recur (unchecked-inc-int i)))
              (and match (not foundExact))
                (cond
                  (== matchIdx -1) (do (set! matchIdx i) (recur (unchecked-inc-int i)))
                  (arbace.lang.Compiler/subsumes
                    (cast Class/1 (.get paramlists i))
                    (cast Class/1 (.get paramlists matchIdx)))
                    (do (set! matchIdx i) (set! tied false) (recur (unchecked-inc-int i)))
                  (Arrays/equals (cast Object/1 (.get paramlists matchIdx))
                                 (cast Object/1 (.get paramlists i)))
                    (if (.isAssignableFrom (cast Class (.get rets matchIdx))
                                           (cast Class (.get rets i)))
                        (do (set! matchIdx i) (recur (unchecked-inc-int i)))
                        (recur (unchecked-inc-int i)))
                  (not (arbace.lang.Compiler/subsumes
                         (cast Class/1 (.get paramlists matchIdx))
                         (cast Class/1 (.get paramlists i))))
                    (do (set! tied true) (recur (unchecked-inc-int i)))
                  :else (recur (unchecked-inc-int i)))
              :else (recur (unchecked-inc-int i))))))
      (when tied
        (throw (IllegalArgumentException.
                 (java-str "More than one matching method found: " methodName))))
      matchIdx))

  (defclass ^:public ^:static NewExpr
    :implements [Expr]

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^Constructor ctor)

    (field ^:public ^:final ^Class c)

    (field ^:static ^:final ^Method invokeConstructorMethod
      (Method/getMethod "Object invokeConstructor(Class,Object[])"))

    (field ^:static ^:final ^Method forNameMethod (Method/getMethod "Class classForName(String)"))

    (constructor ^:public [this ^Class c ^Constructor preferredConstructor ^IPersistentVector args
                           ^int line ^int column]
      (arbace.lang.Compiler/checkMethodArity preferredConstructor (RT/count args))
      (set! (.-args this) args)
      (set! (.-c this) c)
      (set! (.-ctor this) preferredConstructor))

    (constructor ^:public [this ^Class c ^IPersistentVector args ^int line ^int column]
      (set! (.-args this) args)
      (set! (.-c this) c)
      (let [allctors (.getConstructors c)
            ctors (ArrayList.)
            ^{:tag (ArrayList Class/1)} params (ArrayList.)
            ^{:tag (ArrayList Class)} rets (ArrayList.)]
        (loop [^int i 0]
          (when (< i (alength allctors))
            (let [ctor (aget allctors i)]
              (if (== (alength (.getParameterTypes ctor)) (.count args))
                  (do
                    (.add ctors ctor)
                    (.add params (.getParameterTypes ctor))
                    (.add rets c)
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i))))))
        (when (.isEmpty ctors)
          (throw (IllegalArgumentException. (java-str "No matching ctor found for " c))))
        (let [^:mutable ^int ctoridx 0]
          (when (> (.size ctors) 1)
            (set! ctoridx (arbace.lang.Compiler/getMatchingParams (.getName c) params args rets)))
          (set! (.-ctor this) (when (>= ctoridx 0) (cast Constructor (.get ctors ctoridx))))
          (when (and (nil? ctor) (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
            (.format (RT/errPrintWriter)
                     "Reflection warning, %s:%d:%d - call to %s ctor can't be resolved.\n"
                     (new Object/1 [(.deref SOURCE_PATH) line column (.getName c)]))))))

    (method ^:public eval [this]
      (let [argvals (new Object/1 (.count args))]
        (loop [^int i 0]
          (when (< i (.count args))
            (aset argvals i (.eval (cast Expr (.nth args i))))
            (recur (unchecked-inc-int i))))
        (when (some? (.-ctor this))
          (try
            (return (.newInstance ctor (Reflector/boxArgs (.getParameterTypes ctor) argvals)))
            (catch Exception e (throw (Util/sneakyThrow e)))))
        (Reflector/invokeConstructor c argvals)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (some? (.-ctor this))
          (let [type (arbace.lang.Compiler/getType c)]
            (.newInstance gen type)
            (.dup gen)
            (MethodExpr/emitTypedArgs objx gen (.getParameterTypes ctor) args)
            (.invokeConstructor gen type (Method. "<init>" (Type/getConstructorDescriptor ctor))))
          (do
            (.push gen (arbace.lang.Compiler/destubClassName (.getName c)))
            (.invokeStatic gen RT_TYPE forNameMethod)
            (MethodExpr/emitArgsAsArray args objx gen)
            (.invokeStatic gen REFLECTOR_TYPE invokeConstructorMethod)))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] c)

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [line (arbace.lang.Compiler/lineDeref)
              column (arbace.lang.Compiler/columnDeref)
              form (cast ISeq frm)]
          (when (< (.count form) 2)
            (throw (Util/runtimeException
                     "wrong number of arguments, expecting: (new Classname args...)")))
          (let [c (HostExpr/maybeClass (RT/second form) false)]
            ;; new of an array class (doc/classes/SPEC.md §5.11)
            (when (or (and (some? c) (.isArray c))
                      (and (nil? c) (instance? Symbol (RT/second form))
                           (some? (HostExpr/maybeArrayClass (cast Symbol (RT/second form))))))
              (throw (ClassFormsExpr$Signal. "new of an array class")))
            (when (nil? c)
              (throw (IllegalArgumentException.
                       (java-str "Unable to resolve classname: " (RT/second form)))))
            (let [^:mutable args PersistentVector/EMPTY]
              (loop [s (RT/next (RT/next form))]
                (when (some? s)
                  (set! args
                        (.cons args
                               (arbace.lang.Compiler/analyze
                                 (if (identical? context C/EVAL) context C/EXPRESSION)
                                 (.first s))))
                  (recur (.next s))))
              (NewExpr. c args line column)))))))

  (defclass ^:public ^:static MetaExpr
    :implements [Expr]

    (field ^:public ^:final ^Expr expr)

    (field ^:public ^:final ^Expr meta)

    (field ^:static ^:final ^Type IOBJ_TYPE (Type/getType IObj))

    (field ^:static ^:final ^Method withMetaMethod
      (Method/getMethod "arbace.lang.IObj withMeta(arbace.lang.IPersistentMap)"))

    (constructor ^:public [this ^Expr expr ^Expr meta]
      (set! (.-expr this) expr)
      (set! (.-meta this) meta))

    (method ^:public eval [this]
      (.withMeta (cast IObj (.eval expr)) (cast IPersistentMap (.eval meta))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit expr C/EXPRESSION objx gen)
      (.checkCast gen IOBJ_TYPE)
      (.emit meta C/EXPRESSION objx gen)
      (.checkCast gen IPERSISTENTMAP_TYPE)
      (.invokeInterface gen IOBJ_TYPE withMetaMethod)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] (.hasJavaClass expr))

    (method ^:public getJavaClass ^Class [this] (.getJavaClass expr)))

  (defclass ^:public ^:static IfExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:public ^:final ^Expr testExpr)

    (field ^:public ^:final ^Expr thenExpr)

    (field ^:public ^:final ^Expr elseExpr)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (constructor ^:public [this ^int line ^int column ^Expr testExpr ^Expr thenExpr ^Expr elseExpr]
      (set! (.-testExpr this) testExpr)
      (set! (.-thenExpr this) thenExpr)
      (set! (.-elseExpr this) elseExpr)
      (set! (.-line this) line)
      (set! (.-column this) column))

    (method ^:public eval [this]
      (let [t (.eval testExpr)]
        (if (and (some? t) (not (identical? t Boolean/FALSE))) (.eval thenExpr) (.eval elseExpr))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen false))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen true))

    (method ^:public doEmit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen
                                   ^boolean emitUnboxed]
      (let [nullLabel (.newLabel gen)
            falseLabel (.newLabel gen)
            endLabel (.newLabel gen)]
        (.visitLineNumber gen line (.mark gen))
        (cond
          (and (instance? StaticMethodExpr testExpr)
               (.canEmitIntrinsicPredicate (cast StaticMethodExpr testExpr)))
            (.emitIntrinsicPredicate (cast StaticMethodExpr testExpr)
                                     C/EXPRESSION
                                     objx
                                     gen
                                     falseLabel)
          (identical? (arbace.lang.Compiler/maybePrimitiveType testExpr) Boolean/TYPE)
            (do
              (.emitUnboxed (cast MaybePrimitiveExpr testExpr) C/EXPRESSION objx gen)
              (.ifZCmp gen GeneratorAdapter/EQ falseLabel))
          :else
            (do
              (.emit testExpr C/EXPRESSION objx gen)
              (.dup gen)
              (.ifNull gen nullLabel)
              (.getStatic gen BOOLEAN_OBJECT_TYPE "FALSE" BOOLEAN_OBJECT_TYPE)
              (.visitJumpInsn gen Opcodes/IF_ACMPEQ falseLabel)))
        (if emitUnboxed
            (.emitUnboxed (cast MaybePrimitiveExpr thenExpr) context objx gen)
            (.emit thenExpr context objx gen))
        (.goTo gen endLabel)
        (.mark gen nullLabel)
        (.pop gen)
        (.mark gen falseLabel)
        (if emitUnboxed
            (.emitUnboxed (cast MaybePrimitiveExpr elseExpr) context objx gen)
            (.emit elseExpr context objx gen))
        (.mark gen endLabel)))

    (method ^:public hasJavaClass ^boolean [this]
      (and (and (.hasJavaClass thenExpr) (.hasJavaClass elseExpr))
           (or (or (or (or (identical? (.getJavaClass thenExpr) (.getJavaClass elseExpr))
                           (identical? (.getJavaClass thenExpr) RECUR_CLASS))
                       (identical? (.getJavaClass elseExpr) RECUR_CLASS))
                   (and (nil? (.getJavaClass thenExpr))
                        (not (.isPrimitive (.getJavaClass elseExpr)))))
               (and (nil? (.getJavaClass elseExpr)) (not (.isPrimitive (.getJavaClass thenExpr)))))))

    (method ^:public canEmitPrimitive ^boolean [this]
      (try
        (and (and (and (and (instance? MaybePrimitiveExpr thenExpr)
                            (instance? MaybePrimitiveExpr elseExpr))
                       (or (or (identical? (.getJavaClass thenExpr) (.getJavaClass elseExpr))
                               (identical? (.getJavaClass thenExpr) RECUR_CLASS))
                           (identical? (.getJavaClass elseExpr) RECUR_CLASS)))
                  (.canEmitPrimitive (cast MaybePrimitiveExpr thenExpr)))
             (.canEmitPrimitive (cast MaybePrimitiveExpr elseExpr)))
        (catch Exception e false)))

    (method ^:public getJavaClass ^Class [this]
      (let [thenClass (.getJavaClass thenExpr)]
        (if (and (some? thenClass) (not (identical? thenClass RECUR_CLASS)))
            thenClass
            (.getJavaClass elseExpr))))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (cond
            (> (.count form) 4) (throw (Util/runtimeException "Too many arguments to if"))
            (< (.count form) 3) (throw (Util/runtimeException "Too few arguments to if")))
          (let [branch (PathNode. PATHTYPE/BRANCH (cast PathNode (.get CLEAR_PATH)))
                testexpr (arbace.lang.Compiler/analyze
                           (if (identical? context C/EVAL) context C/EXPRESSION)
                           (RT/second form))
                ^:mutable ^Expr thenexpr nil
                ^:mutable ^Expr elseexpr nil]
            (try
              (Var/pushThreadBindings
                (^[Object/1] RT/map CLEAR_PATH (PathNode. PATHTYPE/PATH branch)))
              (set! thenexpr (arbace.lang.Compiler/analyze context (RT/third form)))
              (finally (Var/popThreadBindings)))
            (try
              (Var/pushThreadBindings
                (^[Object/1] RT/map CLEAR_PATH (PathNode. PATHTYPE/PATH branch)))
              (set! elseexpr (arbace.lang.Compiler/analyze context (RT/fourth form)))
              (finally (Var/popThreadBindings)))
            (IfExpr. (arbace.lang.Compiler/lineDeref)
                     (arbace.lang.Compiler/columnDeref)
                     testexpr
                     thenexpr
                     elseexpr))))))

  (field ^:public ^:static ^:final ^IPersistentMap CHAR_MAP
    (PersistentHashMap/create
      (new Object/1
           [\- "_" \: "_COLON_" \+ "_PLUS_" \> "_GT_" \< "_LT_" \= "_EQ_" \~ "_TILDE_" \! "_BANG_"
            \@ "_CIRCA_" \# "_SHARP_" \' "_SINGLEQUOTE_" \" "_DOUBLEQUOTE_" \% "_PERCENT_" \^
            "_CARET_" \& "_AMPERSAND_" \* "_STAR_" \| "_BAR_" \{ "_LBRACE_" \} "_RBRACE_" \[
            "_LBRACK_" \] "_RBRACK_" \/ "_SLASH_" \\ "_BSLASH_" \? "_QMARK_"])))

  (field ^:public ^:static ^:final ^IPersistentMap DEMUNGE_MAP)

  (field ^:public ^:static ^:final ^Pattern DEMUNGE_PATTERN)

  (static-initializer
    (let [^:mutable m (^[Object/1] RT/map "$" \/)]
      (loop [s (RT/seq CHAR_MAP)]
        (when (some? s)
          (let [e (cast IMapEntry (.first s))
                origCh (cast Character (.key e))
                escapeStr (cast String (.val e))]
            (set! m (.assoc m escapeStr origCh))
            (recur (.next s)))))
      (set! DEMUNGE_MAP m)
      (let [mungeStrs (RT/toArray (RT/keys m))]
        (Arrays/sort mungeStrs
                     (anon Comparator []
                       (method ^:public compare ^int [this s1 s2]
                         (unchecked-subtract-int
                           (.length (cast String s2))
                           (.length (cast String s1))))))
        (let [sb (StringBuilder.)
              ^:mutable first true]
          (for-each [s mungeStrs]
            (let [escapeStr (cast String s)]
              (when-not first (.append sb "|"))
              (set! first false)
              (.append sb "\\Q")
              (.append sb escapeStr)
              (.append sb "\\E")))
          (set! DEMUNGE_PATTERN (Pattern/compile (.toString sb)))))))

  (method ^:public ^:static munge ^String [^String name]
    (let [sb (StringBuilder.)]
      (for-each [^char c (.toCharArray name)]
        (let [sub (cast String (.valAt CHAR_MAP c))]
          (if (some? sub) (.append sb sub) (^[char] StringBuilder/.append sb c))))
      (.toString sb)))

  (method ^:public ^:static demunge ^String [^String mungedName]
    (let [sb (StringBuilder.)
          m (.matcher DEMUNGE_PATTERN mungedName)
          ^:mutable ^int lastMatchEnd 0]
      (while (.find m)
        (let [start (.start m)
              end (.end m)]
          (.append sb (.substring mungedName lastMatchEnd start))
          (set! lastMatchEnd end)
          (let [origCh (cast Character (.valAt DEMUNGE_MAP (.group m)))]
            (^[Object] StringBuilder/.append sb origCh))))
      (.append sb (.substring mungedName lastMatchEnd))
      (.toString sb)))

  (defclass ^:public ^:static EmptyExpr
    :implements [Expr]

    (field ^:public ^:final coll)

    (field ^:static ^:final ^Type HASHMAP_TYPE (Type/getType PersistentArrayMap))

    (field ^:static ^:final ^Type HASHSET_TYPE (Type/getType PersistentHashSet))

    (field ^:static ^:final ^Type VECTOR_TYPE (Type/getType PersistentVector))

    (field ^:static ^:final ^Type IVECTOR_TYPE (Type/getType IPersistentVector))

    (field ^:static ^:final ^Type TUPLE_TYPE (Type/getType Tuple))

    (field ^:static ^:final ^Type LIST_TYPE (Type/getType PersistentList))

    (field ^:static ^:final ^Type EMPTY_LIST_TYPE (Type/getType PersistentList$EmptyList))

    (constructor ^:public [this coll] (set! (.-coll this) coll))

    (method ^:public eval [this] coll)

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (cond
        (instance? IPersistentList coll) (.getStatic gen LIST_TYPE "EMPTY" EMPTY_LIST_TYPE)
        (instance? IPersistentVector coll) (.getStatic gen VECTOR_TYPE "EMPTY" VECTOR_TYPE)
        (instance? IPersistentMap coll) (.getStatic gen HASHMAP_TYPE "EMPTY" HASHMAP_TYPE)
        (instance? IPersistentSet coll) (.getStatic gen HASHSET_TYPE "EMPTY" HASHSET_TYPE)
        :else (throw (UnsupportedOperationException. "Unknown Collection type")))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this]
      (cond
        (instance? IPersistentList coll) IPersistentList
        (instance? IPersistentVector coll) IPersistentVector
        (instance? IPersistentMap coll) IPersistentMap
        (instance? IPersistentSet coll) IPersistentSet
        :else (throw (UnsupportedOperationException. "Unknown Collection type")))))

  (defclass ^:public ^:static ListExpr
    :implements [Expr]

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:static ^:final ^Method arrayToListMethod
      (Method/getMethod "arbace.lang.ISeq arrayToList(Object[])"))

    (constructor ^:public [this ^IPersistentVector args]
      (set! (.-args this) args))

    (method ^:public eval [this]
      (let [^:mutable ^IPersistentVector ret PersistentVector/EMPTY]
        (loop [^int i 0]
          (when (< i (.count args))
            (set! ret (.cons ret (.eval (cast Expr (.nth args i)))))
            (recur (unchecked-inc-int i))))
        (.seq ret)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (MethodExpr/emitArgsAsArray args objx gen)
      (.invokeStatic gen RT_TYPE arrayToListMethod)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] IPersistentList))

  (defclass ^:public ^:static MapExpr
    :implements [Expr]

    (field ^:public ^:final ^IPersistentVector keyvals)

    (field ^:static ^:final ^Method mapMethod
      (Method/getMethod "arbace.lang.IPersistentMap map(Object[])"))

    (field ^:static ^:final ^Method mapUniqueKeysMethod
      (Method/getMethod "arbace.lang.IPersistentMap mapUniqueKeys(Object[])"))

    (constructor ^:public [this ^IPersistentVector keyvals]
      (set! (.-keyvals this) keyvals))

    (method ^:public eval [this]
      (let [ret (new Object/1 (.count keyvals))]
        (loop [^int i 0]
          (when (< i (.count keyvals))
            (aset ret i (.eval (cast Expr (.nth keyvals i))))
            (recur (unchecked-inc-int i))))
        (RT/map ret)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [^:mutable allKeysConstant true
            ^:mutable allConstantKeysUnique true
            ^:mutable ^IPersistentSet constantKeys PersistentHashSet/EMPTY]
        (loop [^int i 0]
          (when (< i (.count keyvals))
            (let [k (cast Expr (.nth keyvals i))]
              (if (instance? LiteralExpr k)
                  (let [kval (.eval k)]
                    (if (.contains constantKeys kval)
                        (do (set! allConstantKeysUnique false) (recur (unchecked-add-int i 2)))
                        (do
                          (set! constantKeys (cast IPersistentSet (.cons constantKeys kval)))
                          (recur (unchecked-add-int i 2)))))
                  (do (set! allKeysConstant false) (recur (unchecked-add-int i 2)))))))
        (MethodExpr/emitArgsAsArray keyvals objx gen)
        (if (or (and allKeysConstant allConstantKeysUnique) (<= (.count keyvals) 2))
            (.invokeStatic gen RT_TYPE mapUniqueKeysMethod)
            (.invokeStatic gen RT_TYPE mapMethod))
        (when (identical? context C/STATEMENT) (.pop gen))))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] IPersistentMap)

    (method ^:public ^:static parse ^Expr [^C context ^IPersistentMap form]
      (let [^:mutable ^IPersistentVector keyvals PersistentVector/EMPTY
            ^:mutable keysConstant true
            ^:mutable valsConstant true
            ^:mutable allConstantKeysUnique true
            ^:mutable ^IPersistentSet constantKeys PersistentHashSet/EMPTY]
        (loop [s (RT/seq form)]
          (when (some? s)
            (let [e (cast IMapEntry (.first s))
                  k (arbace.lang.Compiler/analyze
                      (if (identical? context C/EVAL) context C/EXPRESSION)
                      (.key e))
                  v (arbace.lang.Compiler/analyze
                      (if (identical? context C/EVAL) context C/EXPRESSION)
                      (.val e))]
              (set! keyvals (.cons keyvals k))
              (set! keyvals (.cons keyvals v))
              (if (instance? LiteralExpr k)
                  (let [kval (.eval k)]
                    (if (.contains constantKeys kval)
                        (set! allConstantKeysUnique false)
                        (set! constantKeys (cast IPersistentSet (.cons constantKeys kval)))))
                  (set! keysConstant false))
              (if (not (instance? LiteralExpr v))
                  (do (set! valsConstant false) (recur (.next s)))
                  (recur (.next s))))))
        (let [^Expr ret (MapExpr. keyvals)]
          (cond
            (and (instance? IObj form) (some? (.meta (cast IObj form))))
              (MetaExpr. ret
                         (MapExpr/parse (if (identical? context C/EVAL) context C/EXPRESSION)
                                        (.meta (cast IObj form))))
            keysConstant
              (do
                (when-not allConstantKeysUnique
                  (throw (IllegalArgumentException. "Duplicate constant keys in map")))
                (if valsConstant
                    (let [^:mutable ^IPersistentMap m PersistentArrayMap/EMPTY]
                      (loop [^int i 0]
                        (when (< i (.length keyvals))
                          (set! m
                                (.assoc m
                                        (.val (cast LiteralExpr (.nth keyvals i)))
                                        (.val (cast
                                                LiteralExpr
                                                (.nth keyvals (unchecked-add-int i 1))))))
                          (recur (unchecked-add-int i 2))))
                      (ConstantExpr. m))
                    ret))
            :else ret)))))

  (defclass ^:public ^:static SetExpr
    :implements [Expr]

    (field ^:public ^:final ^IPersistentVector keys)

    (field ^:static ^:final ^Method setMethod
      (Method/getMethod "arbace.lang.IPersistentSet set(Object[])"))

    (constructor ^:public [this ^IPersistentVector keys]
      (set! (.-keys this) keys))

    (method ^:public eval [this]
      (let [ret (new Object/1 (.count keys))]
        (loop [^int i 0]
          (when (< i (.count keys))
            (aset ret i (.eval (cast Expr (.nth keys i))))
            (recur (unchecked-inc-int i))))
        (RT/set ret)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (MethodExpr/emitArgsAsArray keys objx gen)
      (.invokeStatic gen RT_TYPE setMethod)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] IPersistentSet)

    (method ^:public ^:static parse ^Expr [^C context ^IPersistentSet form]
      (let [^:mutable ^IPersistentVector keys PersistentVector/EMPTY
            ^:mutable constant true]
        (loop [s (RT/seq form)]
          (when (some? s)
            (let [e (.first s)
                  expr (arbace.lang.Compiler/analyze
                         (if (identical? context C/EVAL) context C/EXPRESSION)
                         e)]
              (set! keys (.cons keys expr))
              (if (not (instance? LiteralExpr expr))
                  (do (set! constant false) (recur (.next s)))
                  (recur (.next s))))))
        (let [^Expr ret (SetExpr. keys)]
          (cond
            (and (instance? IObj form) (some? (.meta (cast IObj form))))
              (MetaExpr. ret
                         (MapExpr/parse (if (identical? context C/EVAL) context C/EXPRESSION)
                                        (.meta (cast IObj form))))
            constant
              (let [^:mutable ^IPersistentSet set PersistentHashSet/EMPTY]
                (loop [^int i 0]
                  (when (< i (.count keys))
                    (let [ve (cast LiteralExpr (.nth keys i))]
                      (set! set (cast IPersistentSet (.cons set (.val ve))))
                      (recur (unchecked-inc-int i)))))
                (ConstantExpr. set))
            :else ret)))))

  (defclass ^:public ^:static VectorExpr
    :implements [Expr]

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:static ^:final ^Method vectorMethod
      (Method/getMethod "arbace.lang.IPersistentVector vector(Object[])"))

    (constructor ^:public [this ^IPersistentVector args]
      (set! (.-args this) args))

    (method ^:public eval [this]
      (let [^:mutable ^IPersistentVector ret PersistentVector/EMPTY]
        (loop [^int i 0]
          (when (< i (.count args))
            (set! ret (.cons ret (.eval (cast Expr (.nth args i)))))
            (recur (unchecked-inc-int i))))
        ret))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (<= (.count args) Tuple/MAX_SIZE)
          (do
            (loop [^int i 0]
              (when (< i (.count args))
                (.emit (cast Expr (.nth args i)) C/EXPRESSION objx gen)
                (recur (unchecked-inc-int i))))
            (.invokeStatic gen TUPLE_TYPE (aget createTupleMethods (.count args))))
          (do (MethodExpr/emitArgsAsArray args objx gen) (.invokeStatic gen RT_TYPE vectorMethod)))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] IPersistentVector)

    (method ^:public ^:static parse ^Expr [^C context ^IPersistentVector form]
      (let [^:mutable constant true
            ^:mutable ^IPersistentVector args PersistentVector/EMPTY]
        (loop [^int i 0]
          (when (< i (.count form))
            (let [v (arbace.lang.Compiler/analyze
                      (if (identical? context C/EVAL) context C/EXPRESSION)
                      (.nth form i))]
              (set! args (.cons args v))
              (if (not (instance? LiteralExpr v))
                  (do (set! constant false) (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i))))))
        (let [^Expr ret (VectorExpr. args)]
          (cond
            (and (instance? IObj form) (some? (.meta (cast IObj form))))
              (MetaExpr. ret
                         (MapExpr/parse (if (identical? context C/EVAL) context C/EXPRESSION)
                                        (.meta (cast IObj form))))
            constant
              (let [^:mutable ^IPersistentVector rv PersistentVector/EMPTY]
                (loop [^int i 0]
                  (when (< i (.count args))
                    (let [ve (cast LiteralExpr (.nth args i))]
                      (set! rv (.cons rv (.val ve)))
                      (recur (unchecked-inc-int i)))))
                (ConstantExpr. rv))
            :else ret)))))

  (defclass ^:static KeywordInvokeExpr
    :implements [Expr]

    (field ^:public ^:final ^KeywordExpr kw)

    (field ^:public ^:final tag)

    (field ^:public ^:final ^Expr target)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final ^int siteIndex)

    (field ^:public ^:final ^String source)

    (field ^:static ^Type ILOOKUP_TYPE (Type/getType ILookup))

    (field ^Class jc)

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^KeywordExpr kw
                           ^Expr target]
      (set! (.-source this) source)
      (set! (.-kw this) kw)
      (set! (.-target this) target)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-tag this) tag)
      (set! (.-siteIndex this) (arbace.lang.Compiler/registerKeywordCallsite (.-k kw))))

    (method ^:public eval [this]
      (try
        (.invoke (.-k kw) (.eval target))
        (catch Throwable e
          (if (not (instance? CompilerException e))
              (throw
                (CompilerException. source line column nil CompilerException/PHASE_EXECUTION e))
              (throw (cast CompilerException e))))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [endLabel (.newLabel gen)
            faultLabel (.newLabel gen)]
        (.visitLineNumber gen line (.mark gen))
        (.getStatic gen
                    (.-objtype objx)
                    (.thunkNameStatic objx siteIndex)
                    ObjExpr/ILOOKUP_THUNK_TYPE)
        (.dup gen)
        (.emit target C/EXPRESSION objx gen)
        (.visitLineNumber gen line (.mark gen))
        (.dupX2 gen)
        (.invokeInterface gen ObjExpr/ILOOKUP_THUNK_TYPE (Method/getMethod "Object get(Object)"))
        (.dupX2 gen)
        (.visitJumpInsn gen Opcodes/IF_ACMPEQ faultLabel)
        (.pop gen)
        (.goTo gen endLabel)
        (.mark gen faultLabel)
        (.swap gen)
        (.pop gen)
        (.dup gen)
        (.getStatic gen
                    (.-objtype objx)
                    (.siteNameStatic objx siteIndex)
                    ObjExpr/KEYWORD_LOOKUPSITE_TYPE)
        (.swap gen)
        (.invokeInterface gen
                          ObjExpr/ILOOKUP_SITE_TYPE
                          (Method/getMethod "arbace.lang.ILookupThunk fault(Object)"))
        (.dup gen)
        (.putStatic gen
                    (.-objtype objx)
                    (.thunkNameStatic objx siteIndex)
                    ObjExpr/ILOOKUP_THUNK_TYPE)
        (.swap gen)
        (.invokeInterface gen ObjExpr/ILOOKUP_THUNK_TYPE (Method/getMethod "Object get(Object)"))
        (.mark gen endLabel)
        (when (identical? context C/STATEMENT) (.pop gen))))

    (method ^:public hasJavaClass ^boolean [this] (some? tag))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (HostExpr/tagToClass tag)))
      jc))

  (defclass ^:public ^:static InstanceOfExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^Expr expr)

    (field ^Class c)

    (constructor ^:public [this ^Class c ^Expr expr]
      (set! (.-expr this) expr)
      (set! (.-c this) c))

    (method ^:public eval [this]
      (if (.isInstance c (.eval expr)) RT/T RT/F))

    (method ^:public canEmitPrimitive ^boolean [this] true)

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit expr C/EXPRESSION objx gen)
      (.instanceOf gen (arbace.lang.Compiler/getType c)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitUnboxed this context objx gen)
      (HostExpr/emitBoxReturn objx gen Boolean/TYPE)
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] Boolean/TYPE))

  (defclass ^:static StaticInvokeExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:public ^:final ^Type target)

    (field ^:public ^:final ^Class retClass)

    (field ^:public ^:final ^Class/1 paramclasses)

    (field ^:public ^:final ^Type/1 paramtypes)

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^boolean variadic)

    (field ^:public ^:final ^boolean tailPosition)

    (field ^:public ^:final tag)

    (field ^Class jc)

    (constructor [this ^Type target ^Class retClass ^Class/1 paramclasses ^Type/1 paramtypes
                  ^boolean variadic ^IPersistentVector args tag ^boolean tailPosition]
      (set! (.-target this) target)
      (set! (.-retClass this) retClass)
      (set! (.-paramclasses this) paramclasses)
      (set! (.-paramtypes this) paramtypes)
      (set! (.-args this) args)
      (set! (.-variadic this) variadic)
      (set! (.-tailPosition this) tailPosition)
      (set! (.-tag this) tag))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval StaticInvokeExpr")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitUnboxed this context objx gen)
      (when-not (identical? context C/STATEMENT) (HostExpr/emitBoxReturn objx gen retClass))
      (when (identical? context C/STATEMENT)
        (if (or (identical? retClass Long/TYPE) (identical? retClass Double/TYPE))
            (.pop2 gen)
            (.pop gen))))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (set! jc
              (arbace.lang.Compiler/retType (when (some? tag) (HostExpr/tagToClass tag)) retClass)))
      jc)

    (method ^:public canEmitPrimitive ^boolean [this]
      (.isPrimitive retClass))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [ms (Method. "invokeStatic" (.getReturnType this) paramtypes)]
        (if variadic
            (do
              (loop [^int i 0]
                (when (< i (unchecked-subtract-int (alength paramclasses) 1))
                  (let [e (cast Expr (.nth args i))]
                    (if (identical? (arbace.lang.Compiler/maybePrimitiveType e)
                                    (aget paramclasses i))
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr e) C/EXPRESSION objx gen)
                          (recur (unchecked-inc-int i)))
                        (do
                          (.emit e C/EXPRESSION objx gen)
                          (HostExpr/emitUnboxArg objx gen (aget paramclasses i))
                          (recur (unchecked-inc-int i)))))))
              (let [restArgs (RT/subvec args
                                        (unchecked-subtract-int (alength paramclasses) 1)
                                        (.count args))]
                (MethodExpr/emitArgsAsArray restArgs objx gen)
                (.invokeStatic gen
                               (Type/getType ArraySeq)
                               (Method/getMethod "arbace.lang.ArraySeq create(Object[])"))))
            (MethodExpr/emitTypedArgs objx gen paramclasses args))
        (when (and tailPosition (not (.-canBeDirect objx)))
          (let [method (cast ObjMethod (.deref METHOD))] (.emitClearThis method gen)))
        (.invokeStatic gen target ms)))

    (method ^:private getReturnType ^Type [this] (Type/getType retClass))

    (method ^:public ^:static parse ^Expr [^Var v ^ISeq args tag ^boolean tailPosition]
      (when-not (or (not (.isBound v)) (nil? (.get v)))
        (let [c (.getClass (.get v))
              cname (.getName c)
              allmethods (.getMethods c)
              ^:mutable variadic false
              argcount (RT/count args)
              ^:mutable ^java.lang.reflect.Method method nil]
          (for-each [^java.lang.reflect.Method m allmethods]
            (when (and (Modifier/isStatic (.getModifiers m)) (.equals (.getName m) "invokeStatic"))
              (let [params (.getParameterTypes m)]
                (if (== argcount (alength params))
                    (do
                      (set! method m)
                      (set! variadic
                            (and (> argcount 0)
                                 (identical? (aget
                                               params
                                               (unchecked-subtract-int (alength params) 1))
                                             ISeq)))
                      (break))
                    (when (and (and (> argcount (alength params)) (> (alength params) 0))
                               (identical? (aget params (unchecked-subtract-int (alength params) 1))
                                           ISeq))
                      (set! method m)
                      (set! variadic true)
                      (break))))))
          (when (some? method)
            (let [retClass (.getReturnType method)
                  paramClasses (.getParameterTypes method)
                  paramTypes (new Type/1 (alength paramClasses))]
              (loop [^int i 0]
                (when (< i (alength paramClasses))
                  (aset paramTypes i (Type/getType (aget paramClasses i)))
                  (recur (unchecked-inc-int i))))
              (let [target (Type/getType c)
                    ^:mutable argv PersistentVector/EMPTY]
                (loop [s (RT/seq args)]
                  (when (some? s)
                    (set! argv (.cons argv (arbace.lang.Compiler/analyze C/EXPRESSION (.first s))))
                    (recur (.next s))))
                (StaticInvokeExpr. target
                                   retClass
                                   paramClasses
                                   paramTypes
                                   variadic
                                   argv
                                   tag
                                   tailPosition))))))))

  (defclass ^:static InvokeExpr
    :implements [Expr]

    (field ^:public ^:final ^Expr fexpr)

    (field ^:public ^:final tag)

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final ^boolean tailPosition)

    (field ^:public ^:final ^String source)

    (field ^:public ^boolean isProtocol false)

    (field ^:public ^boolean isDirect false)

    (field ^:public ^int siteIndex -1)

    (field ^:public ^Class protocolOn)

    (field ^:public ^java.lang.reflect.Method onMethod)

    (field ^:static ^Keyword onKey (Keyword/intern "on"))

    (field ^:static ^Keyword methodMapKey (Keyword/intern "method-map"))

    (field ^Class jc)

    (method ^:static sigTag [^int argcount ^Var v]
      (let [arglists (RT/get (RT/meta v) arglistsKey)
            ^Object sigTag nil]
        (loop [s (RT/seq arglists)]
          (when (some? s)
            (let [sig (cast APersistentVector (.first s))
                  restOffset (.indexOf sig _AMP_)]
              (if (or (== argcount (.count sig)) (and (> restOffset -1) (>= argcount restOffset)))
                  (return (arbace.lang.Compiler/tagOf sig))
                  (recur (.next s))))))
        nil))

    (method ^:static shouldRegisterCallsites ^boolean [^Var callSiteVar]
      (some? (.deref callSiteVar)))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol tag ^Expr fexpr
                           ^IPersistentVector args ^boolean tailPosition]
      (set! (.-source this) source)
      (set! (.-fexpr this) fexpr)
      (set! (.-args this) args)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-tailPosition this) tailPosition)
      (when (instance? VarExpr fexpr)
        (let [fvar (.-var (cast VarExpr fexpr))
              pvar (cast Var (RT/get (.meta fvar) protocolKey))]
          (when (and (some? pvar) (InvokeExpr/shouldRegisterCallsites PROTOCOL_CALLSITES))
            (set! (.-isProtocol this) true)
            (set! (.-siteIndex this)
                  (arbace.lang.Compiler/registerProtocolCallsite (.-var (cast VarExpr fexpr))))
            (let [pon (RT/get (.get pvar) onKey)]
              (set! (.-protocolOn this) (HostExpr/maybeClass pon false))
              (when (some? (.-protocolOn this))
                (let [mmap (cast IPersistentMap (RT/get (.get pvar) methodMapKey))
                      mmapVal (cast Keyword (.valAt mmap (Keyword/intern (.-sym fvar))))]
                  (when (nil? mmapVal)
                    (throw (IllegalArgumentException.
                             (java-str
                               "No method of interface: "
                               (.getName protocolOn)
                               " found for function: "
                               (.-sym fvar)
                               " of protocol: "
                               (.-sym pvar)
                               " (The protocol method may have been defined before and removed.)"))))
                  (let [mname (arbace.lang.Compiler/munge (.toString (.-sym mmapVal)))
                        methods (Reflector/getMethods
                                  protocolOn
                                  (unchecked-subtract-int (.count args) 1)
                                  mname
                                  false)]
                    (when-not (== (.size methods) 1)
                      (throw (IllegalArgumentException.
                               (java-str "No single method: "
                                         mname
                                         " of interface: "
                                         (.getName protocolOn)
                                         " found for function: "
                                         (.-sym fvar)
                                         " of protocol: "
                                         (.-sym pvar)))))
                    (set! (.-onMethod this) (cast java.lang.reflect.Method (.get methods 0))))))))))
      (cond
        (some? tag) (set! (.-tag this) tag)
        (instance? VarExpr fexpr)
          (let [v (.-var (cast VarExpr fexpr))
                arglists (RT/get (RT/meta v) arglistsKey)
                sigTag (InvokeExpr/sigTag (.count args) v)]
            (set! (.-tag this) (if (nil? sigTag) (.-tag (cast VarExpr fexpr)) sigTag)))
        :else (set! (.-tag this) nil)))

    (method ^:public eval [this]
      (try
        (let [fn (cast IFn (.eval fexpr))
              ^:mutable argvs PersistentVector/EMPTY]
          (loop [^int i 0]
            (when (< i (.count args))
              (set! argvs (.cons argvs (.eval (cast Expr (.nth args i)))))
              (recur (unchecked-inc-int i))))
          (.applyTo fn (RT/seq (Util/ret1 argvs (set! argvs nil)))))
        (catch Throwable e
          (if (not (instance? CompilerException e))
              (throw
                (CompilerException. source line column nil CompilerException/PHASE_EXECUTION e))
              (throw (cast CompilerException e))))))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if isProtocol
          (do (.visitLineNumber gen line (.mark gen)) (.emitProto this context objx gen))
          (do
            (.emit fexpr C/EXPRESSION objx gen)
            (.visitLineNumber gen line (.mark gen))
            (.checkCast gen IFN_TYPE)
            (.emitArgsAndCall this 0 context objx gen)))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public emitProto ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [onLabel (.newLabel gen)
            callLabel (.newLabel gen)
            endLabel (.newLabel gen)
            v (.-var (cast VarExpr fexpr))
            e (cast Expr (.nth args 0))]
        (.emit e C/EXPRESSION objx gen)
        (.dup gen)
        (.invokeStatic gen UTIL_TYPE (Method/getMethod "Class classOf(Object)"))
        (.getStatic gen (.-objtype objx) (.cachedClassName objx siteIndex) CLASS_TYPE)
        (.visitJumpInsn gen Opcodes/IF_ACMPEQ callLabel)
        (when (some? protocolOn)
          (.dup gen)
          (.instanceOf gen (Type/getType protocolOn))
          (.ifZCmp gen GeneratorAdapter/NE onLabel))
        (.dup gen)
        (.invokeStatic gen UTIL_TYPE (Method/getMethod "Class classOf(Object)"))
        (.putStatic gen (.-objtype objx) (.cachedClassName objx siteIndex) CLASS_TYPE)
        (.mark gen callLabel)
        (.emitVar objx gen v)
        (.invokeVirtual gen VAR_TYPE (Method/getMethod "Object getRawRoot()"))
        (.swap gen)
        (.emitArgsAndCall this 1 context objx gen)
        (.goTo gen endLabel)
        (.mark gen onLabel)
        (when (some? protocolOn)
          (.checkCast gen (Type/getType protocolOn))
          (MethodExpr/emitTypedArgs objx
                                    gen
                                    (.getParameterTypes onMethod)
                                    (RT/subvec args 1 (.count args)))
          (when (identical? context C/RETURN)
            (let [method (cast ObjMethod (.deref METHOD))] (.emitClearLocals method gen)))
          (let [m (Method. (.getName onMethod)
                           (Type/getReturnType onMethod)
                           (Type/getArgumentTypes onMethod))]
            (.invokeInterface gen (Type/getType protocolOn) m)
            (HostExpr/emitBoxReturn objx gen (.getReturnType onMethod))))
        (.mark gen endLabel)))

    (method emitArgsAndCall ^void [this ^int firstArgToEmit ^C context ^ObjExpr objx
                                   ^GeneratorAdapter gen]
      (loop [^int i firstArgToEmit]
        (when (< i (Math/min MAX_POSITIONAL_ARITY (.count args)))
          (let [e (cast Expr (.nth args i))]
            (.emit e C/EXPRESSION objx gen)
            (recur (unchecked-inc-int i)))))
      (when (> (.count args) MAX_POSITIONAL_ARITY)
        (let [^:mutable restArgs PersistentVector/EMPTY]
          (loop [^int i MAX_POSITIONAL_ARITY]
            (when (< i (.count args))
              (set! restArgs (.cons restArgs (.nth args i)))
              (recur (unchecked-inc-int i))))
          (MethodExpr/emitArgsAsArray restArgs objx gen)))
      (.visitLineNumber gen line (.mark gen))
      (when (and tailPosition (not (.-canBeDirect objx)))
        (let [method (cast ObjMethod (.deref METHOD))] (.emitClearThis method gen)))
      (.invokeInterface gen
                        IFN_TYPE
                        (Method. "invoke"
                                 OBJECT_TYPE
                                 (aget ARG_TYPES
                                       (Math/min
                                         (unchecked-add-int MAX_POSITIONAL_ARITY 1)
                                         (.count args))))))

    (method ^:public hasJavaClass ^boolean [this] (some? tag))

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (HostExpr/tagToClass tag)))
      jc)

    (method ^:public ^:static parse ^Expr [^:mutable ^C context ^ISeq form]
      (let [tailPosition (arbace.lang.Compiler/inTailCall context)]
        (when-not (identical? context C/EVAL) (set! context C/EXPRESSION))
        (let [fexpr (arbace.lang.Compiler/analyze context (.first form))]
          (when (and (and (instance? VarExpr fexpr) (.equals (.-var (cast VarExpr fexpr)) INSTANCE))
                     (== (RT/count form) 3))
            (let [sexpr (arbace.lang.Compiler/analyze C/EXPRESSION (RT/second form))]
              (when (instance? ConstantExpr sexpr)
                (let [val (.val (cast ConstantExpr sexpr))]
                  (when (instance? Class val)
                    (return
                      (InstanceOfExpr. (cast Class val)
                                       (arbace.lang.Compiler/analyze context (RT/third form)))))))))
          (when (and (and (RT/booleanCast (arbace.lang.Compiler/getCompilerOption directLinkingKey))
                          (instance? VarExpr fexpr))
                     (not (identical? context C/EVAL)))
            (let [v (.-var (cast VarExpr fexpr))]
              (when (and (not (.isDynamic v))
                         (not (RT/booleanCast (RT/get (.meta v) redefKey false))))
                (let [formtag (arbace.lang.Compiler/tagOf form)
                      arglists (RT/get (RT/meta v) arglistsKey)
                      arity (RT/count (.next form))
                      sigtag (InvokeExpr/sigTag arity v)
                      vtag (RT/get (RT/meta v) RT/TAG_KEY)
                      ret (StaticInvokeExpr/parse
                            v
                            (RT/next form)
                            (cond (some? formtag) ^Object formtag (some? sigtag) sigtag :else vtag)
                            tailPosition)]
                  (when (some? ret) (return ret))))))
          (when (and (instance? VarExpr fexpr) (not (identical? context C/EVAL)))
            (let [v (.-var (cast VarExpr fexpr))
                  arglists (RT/get (RT/meta v) arglistsKey)
                  arity (RT/count (.next form))]
              (loop [s (RT/seq arglists)]
                (when (some? s)
                  (let [args (cast IPersistentVector (.first s))]
                    (if (== (.count args) arity)
                        (let [primc (FnMethod/primInterface args)]
                          (when (some? primc)
                            (return
                              (arbace.lang.Compiler/analyze
                                context
                                (.withMeta (cast
                                             IObj
                                             (RT/listStar
                                               (Symbol/intern ".invokePrim")
                                               (.withMeta
                                                 (cast Symbol (.first form))
                                                 (^[Object/1] RT/map
                                                   RT/TAG_KEY
                                                   (Symbol/intern primc)))
                                               (.next form)))
                                           (cast
                                             IPersistentMap
                                             (RT/conj (RT/meta v) (RT/meta form))))))))
                        (recur (.next s))))))))
          (if (and (and (instance? KeywordExpr fexpr) (== (RT/count form) 2))
                   (InvokeExpr/shouldRegisterCallsites KEYWORD_CALLSITES))
              (let [target (arbace.lang.Compiler/analyze context (RT/second form))]
                (KeywordInvokeExpr. (cast String (.deref SOURCE))
                                    (arbace.lang.Compiler/lineDeref)
                                    (arbace.lang.Compiler/columnDeref)
                                    (arbace.lang.Compiler/tagOf form)
                                    (cast KeywordExpr fexpr)
                                    target))
              (let [^:mutable args PersistentVector/EMPTY]
                (loop [s (RT/seq (.next form))]
                  (when (some? s)
                    (set! args (.cons args (arbace.lang.Compiler/analyze context (.first s))))
                    (recur (.next s))))
                (cond
                  (instance? StaticFieldExpr fexpr)
                    (if (== (RT/count args) 0)
                        fexpr
                        (throw (IllegalArgumentException.
                                 (java-str "No matching method "
                                           (.-fieldName (cast StaticFieldExpr fexpr))
                                           " found taking "
                                           (RT/count args)
                                           " args for "
                                           (.-c (cast StaticFieldExpr fexpr))))))
                  (instance? QualifiedMethodExpr fexpr)
                    (let [qmexpr (cast QualifiedMethodExpr fexpr)]
                      (when (and (identical? (.-kind qmexpr)
                                             Compiler$QualifiedMethodExpr$MethodKind/INSTANCE)
                                 (== (RT/count args) 0))
                        (throw (QualifiedMethodExpr/instanceNoTargetException qmexpr)))
                      (InvokeExpr/toHostExpr qmexpr
                                             (cast String (.deref SOURCE))
                                             (arbace.lang.Compiler/lineDeref)
                                             (arbace.lang.Compiler/columnDeref)
                                             (arbace.lang.Compiler/tagOf form)
                                             tailPosition
                                             args))
                  :else
                    (InvokeExpr. (cast String (.deref SOURCE))
                                 (arbace.lang.Compiler/lineDeref)
                                 (arbace.lang.Compiler/columnDeref)
                                 (arbace.lang.Compiler/tagOf form)
                                 fexpr
                                 args
                                 tailPosition)))))))

    (method ^:private ^:static toHostExpr ^Expr [^QualifiedMethodExpr qmexpr ^String source
                                                 ^int line ^int column ^Symbol tag
                                                 ^boolean tailPosition ^IPersistentVector args]
      (if (some? (.-hintedSig qmexpr))
          (let [method (QualifiedMethodExpr/resolveHintedMethod
                         (.-c qmexpr)
                         (.-methodName qmexpr)
                         (.-kind qmexpr)
                         (.-hintedSig qmexpr))]
            (switch (.-kind qmexpr)
              CTOR (NewExpr. (.-c qmexpr) (cast Constructor method) args line column)
              INSTANCE
                (InstanceMethodExpr. source
                                     line
                                     column
                                     tag
                                     (cast Expr (RT/first args))
                                     (.-c qmexpr)
                                     (arbace.lang.Compiler/munge (.-methodName qmexpr))
                                     (cast java.lang.reflect.Method method)
                                     (PersistentVector/create (RT/next args))
                                     tailPosition)
              (StaticMethodExpr. source
                                 line
                                 column
                                 tag
                                 (.-c qmexpr)
                                 (arbace.lang.Compiler/munge (.-methodName qmexpr))
                                 (cast java.lang.reflect.Method method)
                                 args
                                 tailPosition)))
          (switch (.-kind qmexpr)
            CTOR (NewExpr. (.-c qmexpr) args line column)
            INSTANCE
              (InstanceMethodExpr. source
                                   line
                                   column
                                   tag
                                   (cast Expr (RT/first args))
                                   (.-c qmexpr)
                                   (arbace.lang.Compiler/munge (.-methodName qmexpr))
                                   (PersistentVector/create (RT/next args))
                                   tailPosition)
            (StaticMethodExpr. source
                               line
                               column
                               tag
                               (.-c qmexpr)
                               (arbace.lang.Compiler/munge (.-methodName qmexpr))
                               args
                               tailPosition)))))

  (defclass ^:static SourceDebugExtensionAttribute
    :extends Attribute

    (constructor ^:public [this] (super. "SourceDebugExtension"))

    (method writeSMAP ^void [this ^ClassWriter cw ^String smap]
      (let [bv (.write this cw nil -1 -1 -1)] (.putUTF8 bv smap))))

  (defclass ^:public ^:static FnExpr
    :extends ObjExpr

    (field ^:static ^:final ^Type aFnType (Type/getType AFunction))

    (field ^:static ^:final ^Type restFnType (Type/getType RestFn))

    (field ^FnMethod variadicMethod nil)

    (field ^IPersistentCollection methods)

    (field ^:private ^boolean hasPrimSigs)

    (field ^:private ^boolean hasMeta)

    (field ^:private ^boolean hasEnclosingMethod)

    (field ^Class jc)

    (constructor ^:public [this tag] (super. tag))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method supportsMeta ^boolean [this] hasMeta)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (set! jc (if (some? (.-tag this)) (HostExpr/tagToClass (.-tag this)) AFunction)))
      jc)

    (method ^:protected emitMethods ^void [this ^ClassVisitor cv]
      (loop [s (RT/seq methods)]
        (when (some? s)
          (let [method (cast ObjMethod (.first s))] (.emit method this cv) (recur (.next s)))))
      (when (.isVariadic this)
        (let [gen (GeneratorAdapter. Opcodes/ACC_PUBLIC
                                     (Method/getMethod "int getRequiredArity()")
                                     nil
                                     nil
                                     cv)]
          (.visitCode gen)
          (.push gen (.count (.-reqParms variadicMethod)))
          (.returnValue gen)
          (.endMethod gen))))

    (method ^:static parse ^Expr [^C context ^:mutable ^ISeq form ^:mutable ^String name]
      (let [origForm form
            fn (FnExpr. (arbace.lang.Compiler/tagOf form))
            retkey (Keyword/intern nil "rettag")
            rettag (RT/get (RT/meta form) retkey)]
        (set! (.-src fn) form)
        (let [enclosingMethod (cast ObjMethod (.deref METHOD))]
          (set! (.-hasEnclosingMethod fn) (some? enclosingMethod))
          (when (some? (.meta (cast IMeta (.first form))))
            (set! (.-onceOnly fn)
                  (RT/booleanCast (RT/get (RT/meta (.first form)) (Keyword/intern nil "once")))))
          (let [basename (java-str (if (some? enclosingMethod)
                                       (.-name (.-objx enclosingMethod))
                                       (arbace.lang.Compiler/munge
                                         (.-name (.-name (arbace.lang.Compiler/currentNS)))))
                                   "$")
                ^:mutable ^Symbol nm nil]
            (cond
              (instance? Symbol (RT/second form))
                (do
                  (set! nm (cast Symbol (RT/second form)))
                  (set! name (java-str (.-name nm) "__" (RT/nextID))))
              (nil? name) (set! name (java-str "fn__" (RT/nextID)))
              (some? enclosingMethod) (set! name (java-str name (java-str "__" (RT/nextID)))))
            (let [simpleName (.replace (arbace.lang.Compiler/munge name) "." "_DOT_")]
              (set! (.-name fn) (java-str basename simpleName))
              (set! (.-internalName fn) (.replace (.-name fn) \. \/))
              (set! (.-objtype fn) (Type/getObjectType (.-internalName fn)))
              (try
              (let [^{:tag (ArrayList String)} prims (ArrayList.)]
                (try
                  (Var/pushThreadBindings
                    (^[Object/1] RT/mapUniqueKeys
                      CLASS_FORMS_NO_DELEGATE
                      nil
                      CONSTANTS
                      PersistentVector/EMPTY
                      CONSTANT_IDS
                      (IdentityHashMap.)
                      KEYWORDS
                      PersistentHashMap/EMPTY
                      VARS
                      PersistentHashMap/EMPTY
                      KEYWORD_CALLSITES
                      PersistentVector/EMPTY
                      PROTOCOL_CALLSITES
                      PersistentVector/EMPTY
                      NO_RECUR
                      nil))
                  (when (some? nm)
                    (set! (.-thisName fn) (.-name nm))
                    (set! form (RT/cons FN (RT/next (RT/next form)))))
                  (when (instance? IPersistentVector (RT/second form))
                    (set! form (RT/list FN (RT/next form))))
                  (set! (.-line fn) (arbace.lang.Compiler/lineDeref))
                  (set! (.-column fn) (arbace.lang.Compiler/columnDeref))
                  (let [methodArray (new FnMethod/1 (unchecked-add-int MAX_POSITIONAL_ARITY 1))
                        ^:mutable ^FnMethod variadicMethod nil
                        ^:mutable usesThis false]
                    (loop [s (RT/next form)]
                      (when (some? s)
                        (let [f (FnMethod/parse fn (cast ISeq (RT/first s)) rettag)]
                          (when (.-usesThis f) (set! usesThis true))
                          (cond
                            (.isVariadic f)
                              (if (nil? variadicMethod)
                                  (set! variadicMethod f)
                                  (throw (Util/runtimeException
                                           "Can't have more than 1 variadic overload")))
                            (nil? (aget methodArray (.count (.-reqParms f))))
                              (aset methodArray (.count (.-reqParms f)) f)
                            :else
                              (throw (Util/runtimeException
                                       "Can't have 2 overloads with same arity")))
                          (if (some? (.-prim f))
                              (do (.add prims (.-prim f)) (recur (RT/next s)))
                              (recur (RT/next s))))))
                    (when (some? variadicMethod)
                      (loop [^int i (unchecked-add-int (.count (.-reqParms variadicMethod)) 1)]
                        (if (<= i MAX_POSITIONAL_ARITY)
                            (if (some? (aget methodArray i))
                                (throw
                                  (Util/runtimeException
                                    "Can't have fixed arity function with more params than variadic function"))
                                (recur (unchecked-inc-int i)))
                            nil)))
                    (set! (.-canBeDirect fn)
                          (and (and (not (.-hasEnclosingMethod fn)) (== (.count (.-closes fn)) 0))
                               (not usesThis)))
                    (let [^:mutable ^IPersistentCollection methods nil]
                      (loop [^int i 0]
                        (if (< i (alength methodArray))
                            (if (some? (aget methodArray i))
                                (do
                                  (set! methods (RT/conj methods (aget methodArray i)))
                                  (recur (unchecked-inc-int i)))
                                (recur (unchecked-inc-int i)))
                            nil))
                      (when (some? variadicMethod) (set! methods (RT/conj methods variadicMethod)))
                      (when (.-canBeDirect fn)
                        (for-each [^FnMethod fm (cast Collection methods)]
                          (when (some? (.-locals fm))
                            (for-each [^LocalBinding lb (cast Collection (RT/keys (.-locals fm)))]
                              (when (.-isArg lb)
                                (set! (.-idx lb) (unchecked-subtract-int (.-idx lb) 1)))))))
                      (set! (.-methods fn) methods)
                      (set! (.-variadicMethod fn) variadicMethod)
                      (set! (.-keywords fn) (cast IPersistentMap (.deref KEYWORDS)))
                      (set! (.-vars fn) (cast IPersistentMap (.deref VARS)))
                      (set! (.-constants fn) (cast PersistentVector (.deref CONSTANTS)))
                      (set! (.-keywordCallsites fn)
                            (cast IPersistentVector (.deref KEYWORD_CALLSITES)))
                      (set! (.-protocolCallsites fn)
                            (cast IPersistentVector (.deref PROTOCOL_CALLSITES)))
                      (set! (.-constantsID fn) (RT/nextID))))
                  (finally (Var/popThreadBindings)))
                (set! (.-hasPrimSigs fn) (> (.size prims) 0))
                (let [^:mutable fmeta (RT/meta origForm)]
                  (when (some? fmeta)
                    (set! fmeta
                          (.without (.without (.without (.without fmeta RT/LINE_KEY) RT/COLUMN_KEY)
                                              RT/FILE_KEY)
                                    retkey)))
                  (set! (.-hasMeta fn) (> (RT/count fmeta) 0))
                  (try
                    (.compile fn
                              (if (.isVariadic fn) "arbace/lang/RestFn" "arbace/lang/AFunction")
                              (when-not (== (.size prims) 0)
                                (cast String/1 (.toArray prims (new String/1 (.size prims)))))
                              (.-onceOnly fn))
                    (catch IOException e (throw (Util/sneakyThrow e))))
                  (.getCompiledClass fn)
                  (if (.supportsMeta fn)
                      (MetaExpr. fn
                                 (MapExpr/parse
                                   (if (identical? context C/EVAL) context C/EXPRESSION)
                                   fmeta))
                      fn)))
              (catch ClassFormsExpr$Signal sig
                (arbace.lang.Compiler/delegateFn context origForm (.-name fn) (.-onceOnly fn) sig))))))))

    (method ^:public ^:final variadicMethod ^ObjMethod [this]
      variadicMethod)

    (method isVariadic ^boolean [this] (some? variadicMethod))

    (method ^:public ^:final methods ^IPersistentCollection [this] methods)

    (method ^:public emitForDefn ^void [this ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit this C/EXPRESSION objx gen)))

  (defclass ^:public ^:static ObjExpr
    :implements [Expr]

    (field ^:static ^:final ^String CONST_PREFIX "const__")

    (field ^String name)

    (field ^String internalName)

    (field ^String thisName)

    (field ^Type objtype)

    (field ^:public ^:final tag)

    (field ^IPersistentMap closes PersistentHashMap/EMPTY)

    (field ^IPersistentVector closesExprs PersistentVector/EMPTY)

    (field ^IPersistentSet volatiles PersistentHashSet/EMPTY)

    (field ^IPersistentMap fields nil)

    (field ^IPersistentVector hintedFields PersistentVector/EMPTY)

    (field ^IPersistentMap keywords PersistentHashMap/EMPTY)

    (field ^IPersistentMap vars PersistentHashMap/EMPTY)

    (field ^Class compiledClass)

    (field ^int line)

    (field ^int column)

    (field ^PersistentVector constants)

    (field ^IPersistentSet usedConstants PersistentHashSet/EMPTY)

    (field ^int constantsID)

    (field ^int altCtorDrops 0)

    (field ^IPersistentVector keywordCallsites)

    (field ^IPersistentVector protocolCallsites)

    (field ^boolean onceOnly false)

    (field src)

    (field ^IPersistentMap opts PersistentHashMap/EMPTY)

    (field ^:static ^:final ^Method voidctor (Method/getMethod "void <init>()"))

    (field ^:protected ^IPersistentMap classMeta)

    (field ^:protected ^boolean canBeDirect)

    (method ^:public ^:final name ^String [this] name)

    (method ^:public ^:final internalName ^String [this] internalName)

    (method ^:public ^:final thisName ^String [this] thisName)

    (method ^:public ^:final objtype ^Type [this] objtype)

    (method ^:public ^:final closes ^IPersistentMap [this] closes)

    (method ^:public ^:final keywords ^IPersistentMap [this] keywords)

    (method ^:public ^:final vars ^IPersistentMap [this] vars)

    (method ^:public ^:final compiledClass ^Class [this] compiledClass)

    (method ^:public ^:final line ^int [this] line)

    (method ^:public ^:final column ^int [this] column)

    (method ^:public ^:final constants ^PersistentVector [this] constants)

    (method ^:public ^:final constantsID ^int [this] constantsID)

    (field ^:static ^:final ^Method kwintern
      (Method/getMethod "arbace.lang.Keyword intern(String, String)"))

    (field ^:static ^:final ^Method symintern
      (Method/getMethod "arbace.lang.Symbol intern(String)"))

    (field ^:static ^:final ^Method varintern
      (Method/getMethod "arbace.lang.Var intern(arbace.lang.Symbol, arbace.lang.Symbol)"))

    (field ^:static ^:final ^Type DYNAMIC_CLASSLOADER_TYPE (Type/getType DynamicClassLoader))

    (field ^:static ^:final ^Method getClassMethod (Method/getMethod "Class getClass()"))

    (field ^:static ^:final ^Method getClassLoaderMethod
      (Method/getMethod "ClassLoader getClassLoader()"))

    (field ^:static ^:final ^Method getConstantsMethod
      (Method/getMethod "Object[] getConstants(int)"))

    (field ^:static ^:final ^Method readStringMethod (Method/getMethod "Object readString(String)"))

    (field ^:static ^:final ^Type ILOOKUP_SITE_TYPE (Type/getType ILookupSite))

    (field ^:static ^:final ^Type ILOOKUP_THUNK_TYPE (Type/getType ILookupThunk))

    (field ^:static ^:final ^Type KEYWORD_LOOKUPSITE_TYPE (Type/getType KeywordLookupSite))

    (field ^:private ^DynamicClassLoader loader)

    (field ^:private ^byte/1 bytecode)

    (constructor ^:public [this tag] (set! (.-tag this) tag))

    (method ^:static trimGenID ^String [^String name]
      (let [i (.lastIndexOf name "__")] (if (== i -1) name (.substring name 0 i))))

    (method ctorTypes ^Type/1 [this]
      (let [^:mutable tv (if (not (.supportsMeta this))
                             PersistentVector/EMPTY
                             (RT/vector IPERSISTENTMAP_TYPE))]
        (loop [s (RT/keys closes)]
          (when (some? s)
            (let [lb (cast LocalBinding (.first s))]
              (if (some? (.getPrimitiveType lb))
                  (do (set! tv (.cons tv (Type/getType (.getPrimitiveType lb)))) (recur (.next s)))
                  (do (set! tv (.cons tv OBJECT_TYPE)) (recur (.next s)))))))
        (let [ret (new Type/1 (.count tv))]
          (loop [^int i 0]
            (when (< i (.count tv))
              (aset ret i (cast Type (.nth tv i)))
              (recur (unchecked-inc-int i))))
          ret)))

    (method compile :throws [IOException] ^void [this ^String superName ^String/1 interfaceNames
                                                 ^boolean oneTimeUse]
      (let [cw (arbace.lang.Compiler/classWriter)
            ^ClassVisitor cv cw]
        (.visit cv
                JVM_BYTECODE_VERSION
                (unchecked-add-int (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
                                   Opcodes/ACC_FINAL)
                internalName
                nil
                superName
                interfaceNames)
        (let [source (cast String (.deref SOURCE))
              ^int lineBefore (cast Integer (.deref LINE_BEFORE))
              lineAfter (unchecked-add-int (cast Integer (.deref LINE_AFTER)) 1)
              ^int columnBefore (cast Integer (.deref COLUMN_BEFORE))
              columnAfter (unchecked-add-int (cast Integer (.deref COLUMN_AFTER)) 1)]
          (when (and (some? source) (some? (.deref SOURCE_PATH)))
            (let [smap (java-str "SMAP\n"
                                 (if (> (.lastIndexOf source \.) 0)
                                     (.substring source 0 (.lastIndexOf source \.))
                                     source)
                                 ".java\nClojure\n*S Clojure\n*F\n+ 1 "
                                 source
                                 "\n"
                                 (cast String (.deref SOURCE_PATH))
                                 "\n*L\n"
                                 (String/format "%d#1,%d:%d\n"
                                                (new
                                                  Object/1
                                                  [lineBefore
                                                   (unchecked-subtract-int lineAfter lineBefore)
                                                   lineBefore]))
                                 "*E")]
              (.visitSource cv source smap)))
          (arbace.lang.Compiler/addAnnotation cv classMeta)
          (when (.supportsMeta this)
            (.visitField cv Opcodes/ACC_FINAL "__meta" (.getDescriptor IPERSISTENTMAP_TYPE) nil nil))
          (loop [s (RT/keys closes)]
            (when (some? s)
              (let [lb (cast LocalBinding (.first s))]
                (cond
                  (.isDeftype this)
                    (let [access (cond
                                   (.isVolatile this lb) Opcodes/ACC_VOLATILE
                                   (.isMutable this lb) 0
                                   :else (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL))
                          ^FieldVisitor fv (if (some? (.getPrimitiveType lb))
                                               (.visitField
                                                 cv
                                                 access
                                                 (.-name lb)
                                                 (.getDescriptor
                                                   (Type/getType (.getPrimitiveType lb)))
                                                 nil
                                                 nil)
                                               (.visitField
                                                 cv
                                                 access
                                                 (.-name lb)
                                                 (.getDescriptor OBJECT_TYPE)
                                                 nil
                                                 nil))]
                      (arbace.lang.Compiler/addAnnotation fv (RT/meta (.-sym lb)))
                      (recur (.next s)))
                  (some? (.getPrimitiveType lb))
                    (do
                      (.visitField cv
                                   (unchecked-add-int
                                     0
                                     (if (.isVolatile this lb) Opcodes/ACC_VOLATILE 0))
                                   (.-name lb)
                                   (.getDescriptor (Type/getType (.getPrimitiveType lb)))
                                   nil
                                   nil)
                      (recur (.next s)))
                  :else
                    (do
                      (.visitField cv 0 (.-name lb) (.getDescriptor OBJECT_TYPE) nil nil)
                      (recur (.next s)))))))
          (loop [^int i 0]
            (when (< i (.count protocolCallsites))
              (.visitField cv
                           (unchecked-add-int Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC)
                           (.cachedClassName this i)
                           (.getDescriptor CLASS_TYPE)
                           nil
                           nil)
              (recur (unchecked-inc-int i))))
          (let [m (Method. "<init>" Type/VOID_TYPE (.ctorTypes this))
                ^:mutable ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
                start (.newLabel ctorgen)
                end (.newLabel ctorgen)]
            (.visitCode ctorgen)
            (.visitLineNumber ctorgen line (.mark ctorgen))
            (.visitLabel ctorgen start)
            (.loadThis ctorgen)
            (.invokeConstructor ctorgen (Type/getObjectType superName) voidctor)
            (when (.supportsMeta this)
              (.loadThis ctorgen)
              (.visitVarInsn ctorgen (.getOpcode IPERSISTENTMAP_TYPE Opcodes/ILOAD) 1)
              (.putField ctorgen objtype "__meta" IPERSISTENTMAP_TYPE))
            (let [^:mutable ^int a (if (.supportsMeta this) 2 1)]
              (let [^:mutable s (RT/keys closes)]
                (while (some? s)
                  (let [lb (cast LocalBinding (.first s))]
                    (.loadThis ctorgen)
                    (let [primc (.getPrimitiveType lb)]
                      (if (some? primc)
                          (do
                            (.visitVarInsn ctorgen
                                           (.getOpcode (Type/getType primc) Opcodes/ILOAD)
                                           a)
                            (.putField ctorgen objtype (.-name lb) (Type/getType primc))
                            (when (or (identical? primc Long/TYPE) (identical? primc Double/TYPE))
                              (set! a (unchecked-inc-int a))))
                          (do
                            (.visitVarInsn ctorgen (.getOpcode OBJECT_TYPE Opcodes/ILOAD) a)
                            (.putField ctorgen objtype (.-name lb) OBJECT_TYPE)))
                      (set! closesExprs (.cons closesExprs (LocalBindingExpr. lb nil)))))
                  (set! s (.next s))
                  (set! a (unchecked-inc-int a))))
              (.visitLabel ctorgen end)
              (.returnValue ctorgen)
              (.endMethod ctorgen)
              (when (> altCtorDrops 0)
                (let [ctorTypes (.ctorTypes this)
                      ^:mutable altCtorTypes (new
                                               Type/1
                                               (unchecked-subtract-int
                                                 (alength ctorTypes)
                                                 altCtorDrops))]
                  (loop [^int i 0]
                    (when (< i (alength altCtorTypes))
                      (aset altCtorTypes i (aget ctorTypes i))
                      (recur (unchecked-inc-int i))))
                  (let [^:mutable alt (Method. "<init>" Type/VOID_TYPE altCtorTypes)]
                    (set! ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC alt nil nil cv))
                    (.visitCode ctorgen)
                    (.loadThis ctorgen)
                    (.loadArgs ctorgen)
                    (.visitInsn ctorgen Opcodes/ACONST_NULL)
                    (.visitInsn ctorgen Opcodes/ACONST_NULL)
                    (.visitInsn ctorgen Opcodes/ICONST_0)
                    (.visitInsn ctorgen Opcodes/ICONST_0)
                    (.invokeConstructor ctorgen objtype (Method. "<init>" Type/VOID_TYPE ctorTypes))
                    (.returnValue ctorgen)
                    (.endMethod ctorgen)
                    (set! altCtorTypes (new Type/1 (unchecked-subtract-int (alength ctorTypes) 2)))
                    (loop [^int i 0]
                      (when (< i (alength altCtorTypes))
                        (aset altCtorTypes i (aget ctorTypes i))
                        (recur (unchecked-inc-int i))))
                    (set! alt (Method. "<init>" Type/VOID_TYPE altCtorTypes))
                    (set! ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC alt nil nil cv))
                    (.visitCode ctorgen)
                    (.loadThis ctorgen)
                    (.loadArgs ctorgen)
                    (.visitInsn ctorgen Opcodes/ICONST_0)
                    (.visitInsn ctorgen Opcodes/ICONST_0)
                    (.invokeConstructor ctorgen objtype (Method. "<init>" Type/VOID_TYPE ctorTypes))
                    (.returnValue ctorgen)
                    (.endMethod ctorgen))))
              (when (.supportsMeta this)
                (let [ctorTypes (.ctorTypes this)
                      noMetaCtorTypes (new Type/1 (unchecked-subtract-int (alength ctorTypes) 1))]
                  (loop [^int i 1]
                    (when (< i (alength ctorTypes))
                      (aset noMetaCtorTypes (unchecked-subtract-int i 1) (aget ctorTypes i))
                      (recur (unchecked-inc-int i))))
                  (let [alt (Method. "<init>" Type/VOID_TYPE noMetaCtorTypes)]
                    (set! ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC alt nil nil cv))
                    (.visitCode ctorgen)
                    (.loadThis ctorgen)
                    (.visitInsn ctorgen Opcodes/ACONST_NULL)
                    (.loadArgs ctorgen)
                    (.invokeConstructor ctorgen objtype (Method. "<init>" Type/VOID_TYPE ctorTypes))
                    (.returnValue ctorgen)
                    (.endMethod ctorgen)
                    (let [^:mutable meth (Method/getMethod "arbace.lang.IPersistentMap meta()")
                          ^:mutable gen (GeneratorAdapter. Opcodes/ACC_PUBLIC meth nil nil cv)]
                      (.visitCode gen)
                      (.loadThis gen)
                      (.getField gen objtype "__meta" IPERSISTENTMAP_TYPE)
                      (.returnValue gen)
                      (.endMethod gen)
                      (set! meth
                            (Method/getMethod
                              "arbace.lang.IObj withMeta(arbace.lang.IPersistentMap)"))
                      (set! gen (GeneratorAdapter. Opcodes/ACC_PUBLIC meth nil nil cv))
                      (.visitCode gen)
                      (.newInstance gen objtype)
                      (.dup gen)
                      (.loadArg gen 0)
                      (let [^:mutable s (RT/keys closes)]
                        (while (some? s)
                          (let [lb (cast LocalBinding (.first s))]
                            (.loadThis gen)
                            (let [primc (.getPrimitiveType lb)]
                              (if (some? primc)
                                  (.getField gen objtype (.-name lb) (Type/getType primc))
                                  (.getField gen objtype (.-name lb) OBJECT_TYPE))))
                          (set! s (.next s))
                          (set! a (unchecked-inc-int a))))
                      (.invokeConstructor gen objtype (Method. "<init>" Type/VOID_TYPE ctorTypes))
                      (.returnValue gen)
                      (.endMethod gen)))))
              (.emitStatics this cv)
              (.emitMethods this cv)
              (loop [^int i 0]
                (if (< i (.count constants))
                    (if (.contains usedConstants i)
                        (do
                          (.visitField cv
                                       (unchecked-add-int
                                         (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                                         Opcodes/ACC_STATIC)
                                       (.constantName this i)
                                       (.getDescriptor (.constantType this i))
                                       nil
                                       nil)
                          (recur (unchecked-inc-int i)))
                        (recur (unchecked-inc-int i)))
                    nil))
              (loop [^int i 0]
                (when (< i (.count keywordCallsites))
                  (.visitField cv
                               (unchecked-add-int Opcodes/ACC_FINAL Opcodes/ACC_STATIC)
                               (.siteNameStatic this i)
                               (.getDescriptor KEYWORD_LOOKUPSITE_TYPE)
                               nil
                               nil)
                  (.visitField cv
                               Opcodes/ACC_STATIC
                               (.thunkNameStatic this i)
                               (.getDescriptor ILOOKUP_THUNK_TYPE)
                               nil
                               nil)
                  (recur (unchecked-inc-int i))))
              (let [clinitgen (GeneratorAdapter.
                                (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC)
                                (Method/getMethod "void <clinit> ()")
                                nil
                                nil
                                cv)]
                (.visitCode clinitgen)
                (.visitLineNumber clinitgen line (.mark clinitgen))
                (when (> (.count constants) 0) (.emitConstants this clinitgen))
                (when (> (.count keywordCallsites) 0) (.emitKeywordCallsites this clinitgen))
                (when (and (.isDeftype this) (RT/booleanCast (RT/get opts loadNs)))
                  (let [nsname (.getNamespace (cast Symbol (RT/second src)))]
                    (when-not (.equals nsname "arbace.core")
                      (.push clinitgen "arbace.core")
                      (.push clinitgen "require")
                      (.invokeStatic clinitgen
                                     RT_TYPE
                                     (Method/getMethod "arbace.lang.Var var(String,String)"))
                      (.invokeVirtual clinitgen VAR_TYPE (Method/getMethod "Object getRawRoot()"))
                      (.checkCast clinitgen IFN_TYPE)
                      (.push clinitgen nsname)
                      (.invokeStatic clinitgen
                                     SYMBOL_TYPE
                                     (Method/getMethod "arbace.lang.Symbol create(String)"))
                      (.invokeInterface clinitgen
                                        IFN_TYPE
                                        (Method/getMethod "Object invoke(Object)"))
                      (.pop clinitgen))))
                (.returnValue clinitgen)
                (.endMethod clinitgen)
                (.visitEnd cv)
                (set! bytecode (.toByteArray cw))
                (when (RT/booleanCast (.deref COMPILE_FILES))
                  (arbace.lang.Compiler/writeClassFile internalName bytecode))))))))

    (method ^:private emitKeywordCallsites ^void [this ^GeneratorAdapter clinitgen]
      (loop [^int i 0]
        (when (< i (.count keywordCallsites))
          (let [k (cast Keyword (.nth keywordCallsites i))]
            (.newInstance clinitgen KEYWORD_LOOKUPSITE_TYPE)
            (.dup clinitgen)
            (.emitValue this k clinitgen)
            (.invokeConstructor clinitgen
                                KEYWORD_LOOKUPSITE_TYPE
                                (Method/getMethod "void <init>(arbace.lang.Keyword)"))
            (.dup clinitgen)
            (.putStatic clinitgen objtype (.siteNameStatic this i) KEYWORD_LOOKUPSITE_TYPE)
            (.putStatic clinitgen objtype (.thunkNameStatic this i) ILOOKUP_THUNK_TYPE)
            (recur (unchecked-inc-int i))))))

    (method ^:protected emitStatics ^void [this ^ClassVisitor gen])

    (method ^:protected emitMethods ^void [this ^ClassVisitor gen])

    (method emitListAsObjectArray ^void [this value ^GeneratorAdapter gen]
      (.push gen (.size (cast List value)))
      (.newArray gen OBJECT_TYPE)
      (let [^:mutable ^int i 0
            it (.iterator (cast List value))]
        (while (.hasNext it)
          (.dup gen)
          (.push gen i)
          (.emitValue this (.next it) gen)
          (.arrayStore gen OBJECT_TYPE)
          (set! i (unchecked-inc-int i)))))

    (method emitValue ^void [this value ^GeneratorAdapter gen]
      (let [^:mutable partial true]
        (cond
          (nil? value) (.visitInsn gen Opcodes/ACONST_NULL)
          (instance? String value) (.push gen (cast String value))
          (instance? Boolean value)
            (if (.booleanValue (cast Boolean value))
                (.getStatic gen BOOLEAN_OBJECT_TYPE "TRUE" BOOLEAN_OBJECT_TYPE)
                (.getStatic gen BOOLEAN_OBJECT_TYPE "FALSE" BOOLEAN_OBJECT_TYPE))
          (instance? Integer value)
            (do
              (.push gen (.intValue (cast Integer value)))
              (.invokeStatic gen (Type/getType Integer) (Method/getMethod "Integer valueOf(int)")))
          (instance? Long value)
            (do
              (^[long] GeneratorAdapter/.push gen (.longValue (cast Long value)))
              (.invokeStatic gen (Type/getType Long) (Method/getMethod "Long valueOf(long)")))
          (instance? Double value)
            (do
              (^[double] GeneratorAdapter/.push gen (.doubleValue (cast Double value)))
              (.invokeStatic gen (Type/getType Double) (Method/getMethod "Double valueOf(double)")))
          (instance? Character value)
            (do
              (.push gen (.charValue (cast Character value)))
              (.invokeStatic gen
                             (Type/getType Character)
                             (Method/getMethod "Character valueOf(char)")))
          (instance? Class value)
            (let [cc (cast Class value)]
              (if (.isPrimitive cc)
                  (let [^:mutable ^Type bt nil]
                    (cond
                      (identical? cc Boolean/TYPE) (set! bt (Type/getType Boolean))
                      (identical? cc Byte/TYPE) (set! bt (Type/getType Byte))
                      (identical? cc Character/TYPE) (set! bt (Type/getType Character))
                      (identical? cc Double/TYPE) (set! bt (Type/getType Double))
                      (identical? cc Float/TYPE) (set! bt (Type/getType Float))
                      (identical? cc Integer/TYPE) (set! bt (Type/getType Integer))
                      (identical? cc Long/TYPE) (set! bt (Type/getType Long))
                      (identical? cc Short/TYPE) (set! bt (Type/getType Short))
                      :else
                        (throw (Util/runtimeException
                                 (java-str "Can't embed unknown primitive in code: " value))))
                    (.getStatic gen bt "TYPE" (Type/getType Class)))
                  (do
                    (.push gen (arbace.lang.Compiler/destubClassName (.getName cc)))
                    (.invokeStatic gen
                                   RT_TYPE
                                   (Method/getMethod "Class classForNameNonLoading(String)")))))
          (instance? Symbol value)
            (do
              (.push gen (.-ns (cast Symbol value)))
              (.push gen (.-name (cast Symbol value)))
              (.invokeStatic gen
                             (Type/getType Symbol)
                             (Method/getMethod "arbace.lang.Symbol intern(String,String)")))
          (instance? Keyword value)
            (do
              (.push gen (.-ns (.-sym (cast Keyword value))))
              (.push gen (.-name (.-sym (cast Keyword value))))
              (.invokeStatic gen
                             RT_TYPE
                             (Method/getMethod "arbace.lang.Keyword keyword(String,String)")))
          (instance? Var value)
            (let [var (cast Var value)]
              (.push gen (.toString (.-name (.-ns var))))
              (.push gen (.toString (.-sym var)))
              (.invokeStatic gen RT_TYPE (Method/getMethod "arbace.lang.Var var(String,String)")))
          (instance? IType value)
            (let [ctor (Method. "<init>"
                                (Type/getConstructorDescriptor
                                  (aget (.getConstructors (.getClass value)) 0)))]
              (.newInstance gen (Type/getType (.getClass value)))
              (.dup gen)
              (let [fields (cast IPersistentVector
                                 (Reflector/invokeStaticMethod
                                   (.getClass value)
                                   "getBasis"
                                   (new Object/1 [])))]
                (loop [s (RT/seq fields)]
                  (when (some? s)
                    (let [field (cast Symbol (.first s))
                          k (arbace.lang.Compiler/tagClass (arbace.lang.Compiler/tagOf field))
                          val (Reflector/getInstanceField
                                value
                                (arbace.lang.Compiler/munge (.-name field)))]
                      (.emitValue this val gen)
                      (if (.isPrimitive k)
                          (let [b (Type/getType (arbace.lang.Compiler/boxClass k))
                                p (.getDescriptor (Type/getType k))
                                n (.getName k)]
                            (.invokeVirtual gen b (Method. (java-str n "Value") (java-str "()" p)))
                            (recur (.next s)))
                          (recur (.next s))))))
                (.invokeConstructor gen (Type/getType (.getClass value)) ctor)))
          (instance? IRecord value)
            (let [createMethod (Method/getMethod
                                 (java-str (.getName (.getClass value))
                                           " create(arbace.lang.IPersistentMap)"))]
              (.emitValue this (PersistentArrayMap/create (cast Map value)) gen)
              (.invokeStatic gen (arbace.lang.Compiler/getType (.getClass value)) createMethod))
          (instance? IPersistentMap value)
            (let [^List entries (ArrayList.)]
              (for-each [^Map$Entry entry (.entrySet (cast Map value))]
                (.add entries (.getKey entry))
                (.add entries (.getValue entry)))
              (.emitListAsObjectArray this entries gen)
              (.invokeStatic gen
                             RT_TYPE
                             (Method/getMethod "arbace.lang.IPersistentMap map(Object[])")))
          (instance? IPersistentVector value)
            (let [args (cast IPersistentVector value)]
              (if (<= (.count args) Tuple/MAX_SIZE)
                  (do
                    (loop [^int i 0]
                      (when (< i (.count args))
                        (.emitValue this (.nth args i) gen)
                        (recur (unchecked-inc-int i))))
                    (.invokeStatic gen TUPLE_TYPE (aget createTupleMethods (.count args))))
                  (do
                    (.emitListAsObjectArray this value gen)
                    (.invokeStatic gen
                                   RT_TYPE
                                   (Method/getMethod
                                     "arbace.lang.IPersistentVector vector(Object[])")))))
          (instance? PersistentHashSet value)
            (let [vs (RT/seq value)]
              (if (nil? vs)
                  (.getStatic gen
                              (Type/getType PersistentHashSet)
                              "EMPTY"
                              (Type/getType PersistentHashSet))
                  (do
                    (.emitListAsObjectArray this vs gen)
                    (.invokeStatic gen
                                   (Type/getType PersistentHashSet)
                                   (Method/getMethod
                                     "arbace.lang.PersistentHashSet create(Object[])")))))
          (or (instance? ISeq value) (instance? IPersistentList value))
            (do
              (.emitListAsObjectArray this value gen)
              (.invokeStatic gen
                             (Type/getType Arrays)
                             (Method/getMethod "java.util.List asList(Object[])"))
              (.invokeStatic gen
                             (Type/getType PersistentList)
                             (Method/getMethod
                               "arbace.lang.IPersistentList create(java.util.List)")))
          (instance? Pattern value)
            (do
              (.emitValue this (.toString value) gen)
              (.invokeStatic gen
                             (Type/getType Pattern)
                             (Method/getMethod "java.util.regex.Pattern compile(String)")))
          :else
            (let [^:mutable ^String cs nil]
              (try
                (set! cs (RT/printString value))
                (catch Exception e
                  (throw (Util/runtimeException
                           (java-str "Can't embed object in code, maybe print-dup not defined: "
                                     value)))))
              (when (== (.length cs) 0)
                (throw (Util/runtimeException
                         (java-str "Can't embed unreadable object in code: " value))))
              (when (.startsWith cs "#<")
                (throw (Util/runtimeException
                         (java-str "Can't embed unreadable object in code: " cs))))
              (.push gen cs)
              (.invokeStatic gen RT_TYPE readStringMethod)
              (set! partial false)))
        (when partial
          (when (and (instance? IObj value) (> (RT/count (.meta (cast IObj value))) 0))
            (.checkCast gen IOBJ_TYPE)
            (let [^Object m (.meta (cast IObj value))]
              (.emitValue this (arbace.lang.Compiler/elideMeta m) gen)
              (.checkCast gen IPERSISTENTMAP_TYPE)
              (.invokeInterface gen
                                IOBJ_TYPE
                                (Method/getMethod
                                  "arbace.lang.IObj withMeta(arbace.lang.IPersistentMap)")))))))

    (method emitConstants ^void [this ^GeneratorAdapter clinitgen]
      (try
        (Var/pushThreadBindings (^[Object/1] RT/map RT/PRINT_DUP RT/T))
        (loop [^int i 0]
          (if (< i (.count constants))
              (if (.contains usedConstants i)
                  (do
                    (.emitValue this (.nth constants i) clinitgen)
                    (.checkCast clinitgen (.constantType this i))
                    (.putStatic clinitgen objtype (.constantName this i) (.constantType this i))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil))
        (finally (Var/popThreadBindings))))

    (method isMutable ^boolean [this ^LocalBinding lb]
      (or (.isVolatile this lb)
          (and (RT/booleanCast (RT/contains fields (.-sym lb)))
               (RT/booleanCast
                 (RT/get (.meta (.-sym lb)) (Keyword/intern "unsynchronized-mutable"))))))

    (method isVolatile ^boolean [this ^LocalBinding lb]
      (and (RT/booleanCast (RT/contains fields (.-sym lb)))
           (RT/booleanCast (RT/get (.meta (.-sym lb)) (Keyword/intern "volatile-mutable")))))

    (method isDeftype ^boolean [this] (some? fields))

    (method supportsMeta ^boolean [this] (not (.isDeftype this)))

    (method emitClearCloses ^void [this ^GeneratorAdapter gen])

    (method ^:synchronized getCompiledClass ^Class [this]
      (when (nil? compiledClass)
        (set! loader (cast DynamicClassLoader (.deref LOADER)))
        (set! compiledClass (.defineClass loader name bytecode src)))
      compiledClass)

    (method ^:public eval [this]
      (when-not (.isDeftype this)
        (try
          (^[Object/1] Constructor/.newInstance
            (^[Class/1] Class/.getDeclaredConstructor (.getCompiledClass this)))
          (catch Exception e (throw (Util/sneakyThrow e))))))

    (method ^:public emitLetFnInits ^void [this ^GeneratorAdapter gen ^ObjExpr objx
                                           ^IPersistentSet letFnLocals]
      (.checkCast gen objtype)
      (loop [s (RT/keys closes)]
        (when (some? s)
          (let [lb (cast LocalBinding (.first s))]
            (if (.contains letFnLocals lb)
                (let [primc (.getPrimitiveType lb)]
                  (.dup gen)
                  (if (some? primc)
                      (do
                        (.emitUnboxedLocal objx gen lb)
                        (.putField gen objtype (.-name lb) (Type/getType primc))
                        (recur (.next s)))
                      (do
                        (.emitLocal objx gen lb false)
                        (.putField gen objtype (.-name lb) OBJECT_TYPE)
                        (recur (.next s)))))
                (recur (.next s))))))
      (.pop gen))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (if (.isDeftype this)
          (.visitInsn gen Opcodes/ACONST_NULL)
          (do
            (.newInstance gen objtype)
            (.dup gen)
            (when (.supportsMeta this) (.visitInsn gen Opcodes/ACONST_NULL))
            (loop [s (RT/seq closesExprs)]
              (when (some? s)
                (let [lbe (cast LocalBindingExpr (.first s))
                      lb (.-b lbe)]
                  (if (some? (.getPrimitiveType lb))
                      (do (.emitUnboxedLocal objx gen lb) (recur (.next s)))
                      (do (.emitLocal objx gen lb (.-shouldClear lbe)) (recur (.next s)))))))
            (.invokeConstructor gen objtype (Method. "<init>" Type/VOID_TYPE (.ctorTypes this)))))
      (when (identical? context C/STATEMENT) (.pop gen)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (field ^Class jc)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (set! jc
              (cond
                (some? compiledClass) compiledClass
                (some? tag) (HostExpr/tagToClass tag)
                :else IFn)))
      jc)

    (method ^:public emitAssignLocal ^void [this ^GeneratorAdapter gen ^LocalBinding lb ^Expr val]
      (when-not (.isMutable this lb)
        (throw (IllegalArgumentException. (java-str "Cannot assign to non-mutable: " (.-name lb)))))
      (let [primc (.getPrimitiveType lb)]
        (.loadThis gen)
        (if (some? primc)
            (do
              (when-not (and (instance? MaybePrimitiveExpr val)
                             (.canEmitPrimitive (cast MaybePrimitiveExpr val)))
                (throw (IllegalArgumentException.
                         (java-str "Must assign primitive to primitive mutable: " (.-name lb)))))
              (let [me (cast MaybePrimitiveExpr val)]
                (.emitUnboxed me C/EXPRESSION this gen)
                (.putField gen objtype (.-name lb) (Type/getType primc))))
            (do (.emit val C/EXPRESSION this gen) (.putField gen objtype (.-name lb) OBJECT_TYPE)))))

    (method ^:private emitLocal ^void [this ^GeneratorAdapter gen ^LocalBinding lb ^boolean clear]
      (if (.containsKey closes lb)
          (let [primc (.getPrimitiveType lb)]
            (.loadThis gen)
            (if (some? primc)
                (do
                  (.getField gen objtype (.-name lb) (Type/getType primc))
                  (HostExpr/emitBoxReturn this gen primc))
                (do
                  (.getField gen objtype (.-name lb) OBJECT_TYPE)
                  (when (and (and onceOnly clear) (.-canBeCleared lb))
                    (.loadThis gen)
                    (.visitInsn gen Opcodes/ACONST_NULL)
                    (.putField gen objtype (.-name lb) OBJECT_TYPE)))))
          (let [^int argoff (if canBeDirect 0 1)
                primc (.getPrimitiveType lb)]
            (cond
              (.-isArg lb)
                (do
                  (.loadArg gen (unchecked-subtract-int (.-idx lb) argoff))
                  (if (some? primc)
                      (HostExpr/emitBoxReturn this gen primc)
                      (when (and clear (.-canBeCleared lb))
                        (.visitInsn gen Opcodes/ACONST_NULL)
                        (.storeArg gen (unchecked-subtract-int (.-idx lb) argoff)))))
              (some? primc)
                (do
                  (.visitVarInsn gen (.getOpcode (Type/getType primc) Opcodes/ILOAD) (.-idx lb))
                  (HostExpr/emitBoxReturn this gen primc))
              :else
                (do
                  (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ILOAD) (.-idx lb))
                  (when (and clear (.-canBeCleared lb))
                    (.visitInsn gen Opcodes/ACONST_NULL)
                    (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) (.-idx lb))))))))

    (method ^:private emitUnboxedLocal ^void [this ^GeneratorAdapter gen ^LocalBinding lb]
      (let [^int argoff (if canBeDirect 0 1)
            primc (.getPrimitiveType lb)]
        (cond
          (.containsKey closes lb)
            (do (.loadThis gen) (.getField gen objtype (.-name lb) (Type/getType primc)))
          (.-isArg lb) (.loadArg gen (unchecked-subtract-int (.-idx lb) argoff))
          :else (.visitVarInsn gen (.getOpcode (Type/getType primc) Opcodes/ILOAD) (.-idx lb)))))

    (method ^:public emitVar ^void [this ^GeneratorAdapter gen ^Var var]
      (let [i (cast Integer (.valAt vars var))] (.emitConstant this gen i)))

    (field ^:static ^:final ^Method varGetMethod (Method/getMethod "Object get()"))

    (field ^:static ^:final ^Method varGetRawMethod (Method/getMethod "Object getRawRoot()"))

    (method ^:public emitVarValue ^void [this ^GeneratorAdapter gen ^Var v]
      (let [i (cast Integer (.valAt vars v))]
        (if (not (.isDynamic v))
            (do (.emitConstant this gen i) (.invokeVirtual gen VAR_TYPE varGetRawMethod))
            (do (.emitConstant this gen i) (.invokeVirtual gen VAR_TYPE varGetMethod)))))

    (method ^:public emitKeyword ^void [this ^GeneratorAdapter gen ^Keyword k]
      (let [i (cast Integer (.valAt keywords k))] (.emitConstant this gen i)))

    (method ^:public emitConstant ^void [this ^GeneratorAdapter gen ^int id]
      (set! usedConstants (cast IPersistentSet (.cons usedConstants id)))
      (.getStatic gen objtype (.constantName this id) (.constantType this id)))

    (method constantName ^String [this ^int id] (java-str CONST_PREFIX id))

    (method siteName ^String [this ^int n] (java-str "__site__" n))

    (method siteNameStatic ^String [this ^int n]
      (java-str (.siteName this n) "__"))

    (method thunkName ^String [this ^int n] (java-str "__thunk__" n))

    (method cachedClassName ^String [this ^int n]
      (java-str "__cached_class__" n))

    (method cachedVarName ^String [this ^int n]
      (java-str "__cached_var__" n))

    (method thunkNameStatic ^String [this ^int n]
      (java-str (.thunkName this n) "__"))

    (method constantType ^Type [this ^int id]
      (let [o (.nth constants id)
            c (Util/classOf o)]
        (when (and (some? c) (Modifier/isPublic (.getModifiers c)))
          (cond
            (.isAssignableFrom LazySeq c) (return (Type/getType ISeq))
            (identical? c Keyword) (return (Type/getType Keyword))
            (.isAssignableFrom RestFn c) (return (Type/getType RestFn))
            (.isAssignableFrom AFn c) (return (Type/getType AFn))
            (identical? c Var) (return (Type/getType Var))
            (identical? c String) (return (Type/getType String))))
        OBJECT_TYPE)))

  (defclass ^:enum PATHTYPE
    (constants PATH BRANCH))

  (defclass ^:static PathNode
    (field ^:final ^PATHTYPE type)

    (field ^:final ^PathNode parent)

    (constructor [this ^PATHTYPE type ^PathNode parent]
      (set! (.-type this) type)
      (set! (.-parent this) parent)))

  (method ^:static clearPathRoot ^PathNode []
    (cast PathNode (.get CLEAR_ROOT)))

  (defclass ^:enum PSTATE
    (constants REQ REST DONE))

  (defclass ^:public ^:static FnMethod
    :extends ObjMethod

    (field ^PersistentVector reqParms PersistentVector/EMPTY)

    (field ^LocalBinding restParm nil)

    (field ^Type/1 argtypes)

    (field ^Class/1 argclasses)

    (field ^Class retClass)

    (field ^String prim)

    (constructor ^:public [this ^ObjExpr objx ^ObjMethod parent]
      (super. objx parent))

    (method ^:public ^:static classChar ^char [x]
      (let [^:mutable ^Class c nil]
        (cond
          (instance? Class x) (set! c (cast Class x))
          (instance? Symbol x) (set! c (arbace.lang.Compiler/primClass (cast Symbol x))))
        (cond
          (or (nil? c) (not (.isPrimitive c))) \O
          (identical? c Long/TYPE) \L
          (identical? c Double/TYPE) \D
          :else (throw (IllegalArgumentException. "Only long and double primitives are supported")))))

    (method ^:public ^:static primInterface ^String [^IPersistentVector arglist]
      (let [sb (StringBuilder.)]
        (loop [^int i 0]
          (when (< i (.count arglist))
            (^[char] StringBuilder/.append sb
                                           (FnMethod/classChar
                                             (arbace.lang.Compiler/tagOf (.nth arglist i))))
            (recur (unchecked-inc-int i))))
        (^[char] StringBuilder/.append sb (FnMethod/classChar (arbace.lang.Compiler/tagOf arglist)))
        (let [ret (.toString sb)
              prim (or (.contains ret "L") (.contains ret "D"))]
          (when (and prim (> (.count arglist) 4))
            (throw (IllegalArgumentException. "fns taking primitives support only 4 or fewer args")))
          (when prim (java-str "arbace.lang.IFn$" ret)))))

    (method ^:static parse ^FnMethod [^ObjExpr objx ^ISeq form ^:mutable rettag]
      (let [parms (cast IPersistentVector (RT/first form))
            body (RT/next form)]
        (try
          (let [method (FnMethod. objx (cast ObjMethod (.deref METHOD)))]
            (set! (.-line method) (arbace.lang.Compiler/lineDeref))
            (set! (.-column method) (arbace.lang.Compiler/columnDeref))
            (let [pnode (PathNode. PATHTYPE/PATH nil)]
              (set! (.-clearRoot method) pnode)
              (Var/pushThreadBindings
                (^[Object/1] RT/mapUniqueKeys METHOD
                                              method
                                              LOCAL_ENV
                                              (.deref LOCAL_ENV)
                                              LOOP_LOCALS
                                              nil
                                              NEXT_LOCAL_NUM
                                              (Integer/valueOf 0)
                                              CLEAR_PATH
                                              pnode
                                              CLEAR_ROOT
                                              pnode
                                              CLEAR_SITES
                                              PersistentHashMap/EMPTY
                                              METHOD_RETURN_CONTEXT
                                              RT/T))
              (set! (.-prim method) (FnMethod/primInterface parms))
              (when (some? (.-prim method)) (set! (.-prim method) (.replace (.-prim method) \. \/)))
              (when (instance? String rettag)
                (set! rettag (Symbol/intern nil (cast String rettag))))
              (when-not (instance? Symbol rettag) (set! rettag nil))
              (when (some? rettag)
                (let [retstr (.getName (cast Symbol rettag))]
                  (when-not (or (.equals retstr "long") (.equals retstr "double"))
                    (set! rettag nil))))
              (set! (.-retClass method)
                    (arbace.lang.Compiler/tagClass
                      (if (some? (arbace.lang.Compiler/tagOf parms))
                          ^Object (arbace.lang.Compiler/tagOf parms)
                          rettag)))
              (if (.isPrimitive (.-retClass method))
                  (when-not (or (identical? (.-retClass method) Double/TYPE)
                                (identical? (.-retClass method) Long/TYPE))
                    (throw (IllegalArgumentException.
                             "Only long and double primitives are supported")))
                  (set! (.-retClass method) Object))
              (if (some? (.-thisName objx))
                  (arbace.lang.Compiler/registerLocal
                    (Symbol/intern (.-thisName objx))
                    nil
                    nil
                    false)
                  (arbace.lang.Compiler/getAndIncLocalNum))
              (let [^:mutable state PSTATE/REQ
                    ^:mutable argLocals PersistentVector/EMPTY
                    ^{:tag (ArrayList Type)} argtypes (ArrayList.)
                    ^{:tag (ArrayList Class)} argclasses (ArrayList.)]
                (loop [^int i 0]
                  (when (< i (.count parms))
                    (when-not (instance? Symbol (.nth parms i))
                      (throw (IllegalArgumentException. "fn params must be Symbols")))
                    (let [p (cast Symbol (.nth parms i))]
                      (when (some? (.getNamespace p))
                        (throw (Util/runtimeException
                                 (java-str "Can't use qualified name as parameter: " p))))
                      (if (.equals p _AMP_)
                          (if (identical? state PSTATE/REQ)
                              (do (set! state PSTATE/REST) (recur (unchecked-inc-int i)))
                              (throw (Util/runtimeException "Invalid parameter list")))
                          (let [^:mutable pc (arbace.lang.Compiler/primClass
                                               (arbace.lang.Compiler/tagClass
                                                 (arbace.lang.Compiler/tagOf p)))]
                            (when (and (.isPrimitive pc)
                                       (not (or (identical? pc Double/TYPE)
                                                (identical? pc Long/TYPE))))
                              (throw (IllegalArgumentException.
                                       (java-str
                                         "Only long and double primitives are supported: "
                                         p))))
                            (when (and (identical? state PSTATE/REST)
                                       (some? (arbace.lang.Compiler/tagOf p)))
                              (throw (Util/runtimeException "& arg cannot have type hint")))
                            (when (and (identical? state PSTATE/REST) (some? (.-prim method)))
                              (throw (Util/runtimeException
                                       "fns taking primitives cannot be variadic")))
                            (when (identical? state PSTATE/REST) (set! pc ISeq))
                            (.add argtypes (Type/getType pc))
                            (.add argclasses pc)
                            (let [lb (if (.isPrimitive pc)
                                         (arbace.lang.Compiler/registerLocal
                                           p
                                           nil
                                           (MethodParamExpr. pc)
                                           true)
                                         (arbace.lang.Compiler/registerLocal
                                           p
                                           (if (identical? state PSTATE/REST)
                                               ISEQ
                                               (arbace.lang.Compiler/tagOf p))
                                           nil
                                           true))]
                              (set! argLocals (.cons argLocals lb))
                              (switch state
                                REQ
                                  (do
                                    (set! (.-reqParms method) (.cons (.-reqParms method) lb))
                                    (recur (unchecked-inc-int i)))
                                REST
                                  (do
                                    (set! (.-restParm method) lb)
                                    (set! state PSTATE/DONE)
                                    (recur (unchecked-inc-int i)))
                                (throw (Util/runtimeException "Unexpected parameter")))))))))
                (when (> (.count (.-reqParms method)) MAX_POSITIONAL_ARITY)
                  (throw (Util/runtimeException
                           (java-str "Can't specify more than " MAX_POSITIONAL_ARITY " params"))))
                (.set LOOP_LOCALS argLocals)
                (set! (.-argLocals method) argLocals)
                (set! (.-argtypes method)
                      (cast Type/1 (.toArray argtypes (new Type/1 (.size argtypes)))))
                (set! (.-argclasses method)
                      (cast Class/1 (.toArray argclasses (new Class/1 (.size argtypes)))))
                (when (some? (.-prim method))
                  (loop [^int i 0]
                    (if (< i (alength (.-argclasses method)))
                        (if (or (identical? (aget (.-argclasses method) i) Long/TYPE)
                                (identical? (aget (.-argclasses method) i) Double/TYPE))
                            (do
                              (arbace.lang.Compiler/getAndIncLocalNum)
                              (recur (unchecked-inc-int i)))
                            (recur (unchecked-inc-int i)))
                        nil)))
                (set! (.-body method) (.parse (Compiler$BodyExpr$Parser.) C/RETURN body))
                method)))
          (finally (Var/popThreadBindings)))))

    (method ^:public emit ^void [this ^ObjExpr fn ^ClassVisitor cv]
      (cond
        (.-canBeDirect fn) (.doEmitStatic this fn cv)
        (some? prim) (.doEmitPrim this fn cv)
        :else (.doEmit this fn cv)))

    (method ^:public doEmitStatic ^void [this ^ObjExpr fn ^ClassVisitor cv]
      (let [^:mutable returnType (Type/getType retClass)
            ms (Method. "invokeStatic" returnType argtypes)
            ^:mutable gen (GeneratorAdapter.
                            (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC)
                            ms
                            nil
                            EXCEPTION_TYPES
                            cv)]
        (.visitCode gen)
        (let [loopLabel (.mark gen)]
          (.visitLineNumber gen (.-line this) loopLabel)
          (try
            (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel METHOD this))
            (FnMethod/emitBody (.-objx this) gen retClass (.-body this))
            (let [end (.mark gen)]
              (loop [lbs (.seq (.-argLocals this))]
                (when (some? lbs)
                  (let [lb (cast LocalBinding (.first lbs))]
                    (.visitLocalVariable gen
                                         (.-name lb)
                                         (.getDescriptor (aget argtypes (.-idx lb)))
                                         nil
                                         loopLabel
                                         end
                                         (.-idx lb))
                    (recur (.next lbs))))))
            (finally (Var/popThreadBindings)))
          (.returnValue gen)
          (.endMethod gen)
          (let [m (Method. (.getMethodName this) OBJECT_TYPE (.getArgTypes this))]
            (set! gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil EXCEPTION_TYPES cv))
            (.visitCode gen)
            (loop [^int i 0]
              (when (< i (alength argtypes))
                (.loadArg gen i)
                (HostExpr/emitUnboxArg fn gen (aget argclasses i))
                (if (not (.isPrimitive (aget argclasses i)))
                    (do
                      (.visitInsn gen Opcodes/ACONST_NULL)
                      (.storeArg gen i)
                      (recur (unchecked-inc-int i)))
                    (recur (unchecked-inc-int i)))))
            (let [callLabel (.mark gen)]
              (.visitLineNumber gen (.-line this) callLabel)
              (.invokeStatic gen (.-objtype (.-objx this)) ms)
              (if (or (.equals Type/LONG_TYPE returnType) (.equals Type/DOUBLE_TYPE returnType))
                  (.valueOf gen returnType)
                  (.box gen returnType))
              (.returnValue gen)
              (.endMethod gen)
              (when (some? prim)
                (if (or (identical? retClass Double/TYPE) (identical? retClass Long/TYPE))
                    (set! returnType (.getReturnType this))
                    (set! returnType OBJECT_TYPE))
                (let [pm (Method. "invokePrim" returnType argtypes)]
                  (set! gen
                        (GeneratorAdapter. (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                                           pm
                                           nil
                                           EXCEPTION_TYPES
                                           cv))
                  (.visitCode gen)
                  (loop [^int i 0]
                    (when (< i (alength argtypes))
                      (.loadArg gen i)
                      (if (not (.isPrimitive (aget argclasses i)))
                          (do
                            (.visitInsn gen Opcodes/ACONST_NULL)
                            (.storeArg gen i)
                            (recur (unchecked-inc-int i)))
                          (recur (unchecked-inc-int i)))))
                  (.invokeStatic gen (.-objtype (.-objx this)) ms)
                  (.returnValue gen)
                  (.endMethod gen))))))))

    (method ^:public doEmitPrim ^void [this ^ObjExpr fn ^ClassVisitor cv]
      (let [^Type returnType (if (or (identical? retClass Double/TYPE)
                                     (identical? retClass Long/TYPE))
                                 (.getReturnType this)
                                 OBJECT_TYPE)]
        (let [ms (Method. "invokePrim" returnType argtypes)
              ^:mutable gen (GeneratorAdapter.
                              (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                              ms
                              nil
                              EXCEPTION_TYPES
                              cv)]
          (.visitCode gen)
          (let [loopLabel (.mark gen)]
            (.visitLineNumber gen (.-line this) loopLabel)
            (try
              (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel METHOD this))
              (FnMethod/emitBody (.-objx this) gen retClass (.-body this))
              (let [end (.mark gen)]
                (.visitLocalVariable gen "this" "Ljava/lang/Object;" nil loopLabel end 0)
                (loop [lbs (.seq (.-argLocals this))]
                  (when (some? lbs)
                    (let [lb (cast LocalBinding (.first lbs))]
                      (.visitLocalVariable gen
                                           (.-name lb)
                                           (.getDescriptor
                                             (aget argtypes (unchecked-subtract-int (.-idx lb) 1)))
                                           nil
                                           loopLabel
                                           end
                                           (.-idx lb))
                      (recur (.next lbs))))))
              (finally (Var/popThreadBindings)))
            (.returnValue gen)
            (.endMethod gen)
            (let [m (Method. (.getMethodName this) OBJECT_TYPE (.getArgTypes this))]
              (set! gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil EXCEPTION_TYPES cv))
              (.visitCode gen)
              (.loadThis gen)
              (loop [^int i 0]
                (when (< i (alength argtypes))
                  (.loadArg gen i)
                  (HostExpr/emitUnboxArg fn gen (aget argclasses i))
                  (recur (unchecked-inc-int i))))
              (.invokeInterface gen (Type/getType (java-str "L" prim ";")) ms)
              (let [targetReturnType (.getReturnType this)]
                (if (or (.equals Type/LONG_TYPE targetReturnType)
                        (.equals Type/DOUBLE_TYPE targetReturnType))
                    (.valueOf gen targetReturnType)
                    (.box gen targetReturnType))
                (.returnValue gen)
                (.endMethod gen)))))))

    (method ^:public doEmit ^void [this ^ObjExpr fn ^ClassVisitor cv]
      (let [m (Method. (.getMethodName this) (.getReturnType this) (.getArgTypes this))
            gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil EXCEPTION_TYPES cv)]
        (.visitCode gen)
        (let [loopLabel (.mark gen)]
          (.visitLineNumber gen (.-line this) loopLabel)
          (try
            (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel METHOD this))
            (.emit (.-body this) C/RETURN fn gen)
            (let [end (.mark gen)]
              (.visitLocalVariable gen "this" "Ljava/lang/Object;" nil loopLabel end 0)
              (loop [lbs (.seq (.-argLocals this))]
                (when (some? lbs)
                  (let [lb (cast LocalBinding (.first lbs))]
                    (.visitLocalVariable gen
                                         (.-name lb)
                                         "Ljava/lang/Object;"
                                         nil
                                         loopLabel
                                         end
                                         (.-idx lb))
                    (recur (.next lbs))))))
            (finally (Var/popThreadBindings)))
          (.returnValue gen)
          (.endMethod gen))))

    (method ^:public ^:final reqParms ^PersistentVector [this] reqParms)

    (method ^:public ^:final restParm ^LocalBinding [this] restParm)

    (method isVariadic ^boolean [this] (some? restParm))

    (method numParams ^int [this]
      (unchecked-add-int (.count reqParms) (if (.isVariadic this) 1 0)))

    (method getMethodName ^String [this]
      (if (.isVariadic this) "doInvoke" "invoke"))

    (method getReturnType ^Type [this]
      (if (some? prim) (Type/getType retClass) OBJECT_TYPE))

    (method getArgTypes ^Type/1 [this]
      (if (and (.isVariadic this) (== (.count reqParms) MAX_POSITIONAL_ARITY))
          (let [ret (new Type/1 (unchecked-add-int MAX_POSITIONAL_ARITY 1))]
            (loop [^int i 0]
              (when (< i (unchecked-add-int MAX_POSITIONAL_ARITY 1))
                (aset ret i OBJECT_TYPE)
                (recur (unchecked-inc-int i))))
            ret)
          (aget ARG_TYPES (.numParams this))))

    (method emitClearLocals ^void [this ^GeneratorAdapter gen]))

  (defclass ^:public ^:abstract ^:static ObjMethod
    (field ^:public ^:final ^ObjMethod parent)

    (field ^IPersistentMap locals nil)

    (field ^IPersistentMap indexlocals nil)

    (field ^Expr body nil)

    (field ^ObjExpr objx)

    (field ^PersistentVector argLocals)

    (field ^int maxLocal 0)

    (field ^int line)

    (field ^int column)

    (field ^boolean usesThis false)

    (field ^PersistentHashSet localsUsedInCatchFinally PersistentHashSet/EMPTY)

    (field ^:protected ^IPersistentMap methodMeta)

    (field ^PathNode clearRoot)

    (method ^:public ^:final locals ^IPersistentMap [this] locals)

    (method ^:public ^:final body ^Expr [this] body)

    (method ^:public ^:final objx ^ObjExpr [this] objx)

    (method ^:public ^:final argLocals ^PersistentVector [this] argLocals)

    (method ^:public ^:final maxLocal ^int [this] maxLocal)

    (method ^:public ^:final line ^int [this] line)

    (method ^:public ^:final column ^int [this] column)

    (constructor ^:public [this ^ObjExpr objx ^ObjMethod parent]
      (set! (.-parent this) parent)
      (set! (.-objx this) objx))

    (method ^:static emitBody ^void [^ObjExpr objx ^GeneratorAdapter gen ^Class retClass ^Expr body]
      (let [be (cast MaybePrimitiveExpr body)]
        (if (and (Util/isPrimitive retClass) (.canEmitPrimitive be))
            (let [bc (arbace.lang.Compiler/maybePrimitiveType be)]
              (cond
                (identical? bc retClass) (.emitUnboxed be C/RETURN objx gen)
                (and (identical? retClass Long/TYPE) (identical? bc Integer/TYPE))
                  (do (.emitUnboxed be C/RETURN objx gen) (.visitInsn gen Opcodes/I2L))
                (and (identical? retClass Double/TYPE) (identical? bc Float/TYPE))
                  (do (.emitUnboxed be C/RETURN objx gen) (.visitInsn gen Opcodes/F2D))
                (and (identical? retClass Integer/TYPE) (identical? bc Long/TYPE))
                  (do
                    (.emitUnboxed be C/RETURN objx gen)
                    (.invokeStatic gen RT_TYPE (Method/getMethod "int intCast(long)")))
                (and (identical? retClass Float/TYPE) (identical? bc Double/TYPE))
                  (do (.emitUnboxed be C/RETURN objx gen) (.visitInsn gen Opcodes/D2F))
                :else
                  (throw (IllegalArgumentException.
                           (java-str "Mismatched primitive return, expected: "
                                     retClass
                                     ", had: "
                                     (.getJavaClass be))))))
            (do
              (.emit body C/RETURN objx gen)
              (if (identical? retClass Void/TYPE) (.pop gen) (.unbox gen (Type/getType retClass)))))))

    (method ^:abstract numParams ^int [this])

    (method ^:abstract getMethodName ^String [this])

    (method ^:abstract getReturnType ^Type [this])

    (method ^:abstract getArgTypes ^Type/1 [this])

    (method ^:public emit ^void [this ^ObjExpr fn ^ClassVisitor cv]
      (let [m (Method. (.getMethodName this) (.getReturnType this) (.getArgTypes this))
            gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil EXCEPTION_TYPES cv)]
        (.visitCode gen)
        (let [loopLabel (.mark gen)]
          (.visitLineNumber gen line loopLabel)
          (try
            (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel METHOD this))
            (.emit body C/RETURN fn gen)
            (let [end (.mark gen)]
              (.visitLocalVariable gen "this" "Ljava/lang/Object;" nil loopLabel end 0)
              (loop [lbs (.seq argLocals)]
                (when (some? lbs)
                  (let [lb (cast LocalBinding (.first lbs))]
                    (.visitLocalVariable gen
                                         (.-name lb)
                                         "Ljava/lang/Object;"
                                         nil
                                         loopLabel
                                         end
                                         (.-idx lb))
                    (recur (.next lbs))))))
            (finally (Var/popThreadBindings)))
          (.returnValue gen)
          (.endMethod gen))))

    (method emitClearLocals ^void [this ^GeneratorAdapter gen])

    (method emitClearLocalsOld ^void [this ^GeneratorAdapter gen]
      (loop [^int i 0]
        (when (< i (.count argLocals))
          (let [lb (cast LocalBinding (.nth argLocals i))]
            (if (and (not (.contains localsUsedInCatchFinally (.-idx lb)))
                     (nil? (.getPrimitiveType lb)))
                (do
                  (.visitInsn gen Opcodes/ACONST_NULL)
                  (.storeArg gen (unchecked-subtract-int (.-idx lb) 1))
                  (recur (unchecked-inc-int i)))
                (recur (unchecked-inc-int i))))))
      (loop [^int i (unchecked-add-int (.numParams this) 1)]
        (if (< i (unchecked-add-int maxLocal 1))
            (if (not (.contains localsUsedInCatchFinally i))
                (let [b (cast LocalBinding (RT/get indexlocals i))]
                  (if (or (nil? b) (nil? (arbace.lang.Compiler/maybePrimitiveType (.-init b))))
                      (do
                        (.visitInsn gen Opcodes/ACONST_NULL)
                        (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) i)
                        (recur (unchecked-inc-int i)))
                      (recur (unchecked-inc-int i))))
                (recur (unchecked-inc-int i)))
            nil)))

    (method emitClearThis ^void [this ^GeneratorAdapter gen]
      (.visitInsn gen Opcodes/ACONST_NULL)
      (.visitVarInsn gen Opcodes/ASTORE 0)))

  (defclass ^:public ^:static LocalBinding
    (field ^:public ^:final ^Symbol sym)

    (field ^:public ^:final ^Symbol tag)

    (field ^:public ^Expr init)

    (field ^int idx)

    (field ^:public ^:final ^String name)

    (field ^:public ^:final ^boolean isArg)

    (field ^:public ^:final ^PathNode clearPathRoot)

    (field ^:public ^boolean canBeCleared
      (not (RT/booleanCast (arbace.lang.Compiler/getCompilerOption disableLocalsClearingKey))))

    (field ^:public ^boolean recurMistmatch false)

    (field ^:public ^boolean used false)

    (constructor ^:public [this ^int num ^Symbol sym ^Symbol tag ^Expr init ^boolean isArg
                           ^PathNode clearPathRoot]
      ;; a primitive tag on a local with a primitive initializer: an exact primitive type of the
      ;; class forms (doc/classes/SPEC.md §5.3)
      (when (and (some? (arbace.lang.Compiler/maybePrimitiveType init)) (some? tag))
        (throw (ClassFormsExpr$Signal. "A type hint on a local with a primitive initializer")))
      (set! (.-idx this) num)
      (set! (.-sym this) sym)
      (set! (.-tag this) tag)
      (set! (.-init this) init)
      (set! (.-isArg this) isArg)
      (set! (.-clearPathRoot this) clearPathRoot)
      (set! name (arbace.lang.Compiler/munge (.-name sym))))

    (field ^Boolean hjc)

    (method ^:public hasJavaClass ^boolean [this]
      (when (nil? hjc)
        (if (and (and (and (some? init) (.hasJavaClass init))
                      (Util/isPrimitive (.getJavaClass init)))
                 (not (instance? MaybePrimitiveExpr init)))
            (set! hjc false)
            (set! hjc (or (some? tag) (and (some? init) (.hasJavaClass init))))))
      hjc)

    (field ^Class jc)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc) (set! jc (if (some? tag) (HostExpr/tagToClass tag) (.getJavaClass init))))
      jc)

    (method ^:public getPrimitiveType ^Class [this]
      (arbace.lang.Compiler/maybePrimitiveType init))

    ;; equality stays identity; a hash that does not depend on the JVM run, so the order of a
    ;; fn's closed-over locals (its fields and constructor parameters) is the same in every
    ;; compilation and AOT output is reproducible (Arbace)
    (method ^:public hashCode ^int [this]
      (unchecked-add-int (unchecked-multiply-int 31 idx) (.hashCode name))))

  (defclass ^:public ^:static LocalBindingExpr
    :implements [Expr MaybePrimitiveExpr AssignableExpr]

    (field ^:public ^:final ^LocalBinding b)

    (field ^:public ^:final ^Symbol tag)

    (field ^:public ^:final ^PathNode clearPath)

    (field ^:public ^:final ^PathNode clearRoot)

    (field ^:public ^boolean shouldClear false)

    (constructor ^:public [this ^LocalBinding b ^Symbol tag]
      (if (and (some? (.getPrimitiveType b)) (some? tag))
          (if (not (.equals (.getPrimitiveType b) (arbace.lang.Compiler/tagClass tag)))
              (throw (UnsupportedOperationException.
                       "Can't type hint a primitive local with a different type"))
              (set! (.-tag this) nil))
          (set! (.-tag this) tag))
      (set! (.-b this) b)
      (set! (.-clearPath this) (cast PathNode (.get CLEAR_PATH)))
      (set! (.-clearRoot this) (cast PathNode (.get CLEAR_ROOT)))
      (let [^:mutable sites (cast IPersistentCollection (RT/get (.get CLEAR_SITES) b))]
        (set! (.-used b) true)
        (when (> (.-idx b) 0)
          (when (some? sites)
            (loop [s (.seq sites)]
              (when (some? s)
                (let [o (cast LocalBindingExpr (.first s))
                      common (arbace.lang.Compiler/commonPath clearPath (.-clearPath o))]
                  (if (and (some? common) (identical? (.-type common) PATHTYPE/PATH))
                      (do (set! (.-shouldClear o) false) (recur (.next s)))
                      (recur (.next s)))))))
          (let [method (cast ObjMethod (.deref METHOD))
                closedOver (.containsKey (.-closes (.-objx method)) b)]
            (when (or (identical? clearRoot (.-clearPathRoot b))
                      (and closedOver (identical? clearRoot (.-clearRoot method))))
              (set! (.-shouldClear this) true)
              (set! sites (RT/conj sites this))
              (.set CLEAR_SITES (RT/assoc (.get CLEAR_SITES) b sites)))))))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval locals")))

    (method ^:public canEmitPrimitive ^boolean [this]
      (some? (.getPrimitiveType b)))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emitUnboxedLocal objx gen b))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (when-not (identical? context C/STATEMENT) (.emitLocal objx gen b shouldClear)))

    (method ^:public evalAssign [this ^Expr val]
      (throw (UnsupportedOperationException. "Can't eval locals")))

    (method ^:public emitAssign ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen ^Expr val]
      (.emitAssignLocal objx gen b val)
      (when-not (identical? context C/STATEMENT) (.emitLocal objx gen b false)))

    (method ^:public hasJavaClass ^boolean [this]
      (or (some? tag) (.hasJavaClass b)))

    (field ^Class jc)

    (method ^:public getJavaClass ^Class [this]
      (when (nil? jc)
        (if (some? tag) (set! jc (HostExpr/tagToClass tag)) (set! jc (.getJavaClass b))))
      jc))

  (defclass ^:public ^:static BodyExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^PersistentVector exprs)

    (method ^:public ^:final exprs ^PersistentVector [this] exprs)

    (constructor ^:public [this ^PersistentVector exprs]
      (set! (.-exprs this) exprs))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frms]
        (let [^:mutable forms (cast ISeq frms)]
          (when (Util/equals (RT/first forms) DO) (set! forms (RT/next forms)))
          (let [^:mutable exprs PersistentVector/EMPTY]
            (while (some? forms)
              (let [e (if (and (not (identical? context C/EVAL))
                               (or (identical? context C/STATEMENT) (some? (.next forms))))
                          (arbace.lang.Compiler/analyze C/STATEMENT (.first forms))
                          (arbace.lang.Compiler/analyze context (.first forms)))]
                (set! exprs (.cons exprs e)))
              (set! forms (.next forms)))
            (when (== (.count exprs) 0) (set! exprs (.cons exprs NIL_EXPR)))
            (BodyExpr. exprs)))))

    (method ^:public eval [this]
      (let [^:mutable ^Object ret nil]
        (for-each [o exprs] (let [e (cast Expr o)] (set! ret (.eval e))))
        ret))

    (method ^:public canEmitPrimitive ^boolean [this]
      (and (instance? MaybePrimitiveExpr (.lastExpr this))
           (.canEmitPrimitive (cast MaybePrimitiveExpr (.lastExpr this)))))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (loop [^int i 0]
        (when (< i (unchecked-subtract-int (.count exprs) 1))
          (let [e (cast Expr (.nth exprs i))]
            (.emit e C/STATEMENT objx gen)
            (recur (unchecked-inc-int i)))))
      (let [last (cast MaybePrimitiveExpr (.nth exprs (unchecked-subtract-int (.count exprs) 1)))]
        (.emitUnboxed last context objx gen)))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (loop [^int i 0]
        (when (< i (unchecked-subtract-int (.count exprs) 1))
          (let [e (cast Expr (.nth exprs i))]
            (.emit e C/STATEMENT objx gen)
            (recur (unchecked-inc-int i)))))
      (let [last (cast Expr (.nth exprs (unchecked-subtract-int (.count exprs) 1)))]
        (.emit last context objx gen)))

    (method ^:public hasJavaClass ^boolean [this]
      (.hasJavaClass (.lastExpr this)))

    (method ^:public getJavaClass ^Class [this]
      (.getJavaClass (.lastExpr this)))

    (method ^:private lastExpr ^Expr [this]
      (cast Expr (.nth exprs (unchecked-subtract-int (.count exprs) 1)))))

  (defclass ^:public ^:static BindingInit
    (field ^LocalBinding binding)

    (field ^Expr init)

    (method ^:public ^:final binding ^LocalBinding [this] binding)

    (method ^:public ^:final init ^Expr [this] init)

    (constructor ^:public [this ^LocalBinding binding ^Expr init]
      (set! (.-binding this) binding)
      (set! (.-init this) init)))

  (defclass ^:public ^:static LetFnExpr
    :implements [Expr]

    (field ^:public ^:final ^PersistentVector bindingInits)

    (field ^:public ^:final ^Expr body)

    (constructor ^:public [this ^PersistentVector bindingInits ^Expr body]
      (set! (.-bindingInits this) bindingInits)
      (set! (.-body this) body))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (when-not (instance? IPersistentVector (RT/second form))
            (throw (IllegalArgumentException. "Bad binding form, expected vector")))
          (let [bindings (cast IPersistentVector (RT/second form))]
            (when-not (== (unchecked-remainder-int (.count bindings) 2) 0)
              (throw (IllegalArgumentException.
                       "Bad binding form, expected matched symbol expression pairs")))
            (let [body (RT/next (RT/next form))]
              (if (identical? context C/EVAL)
                  (arbace.lang.Compiler/analyze context
                                                (RT/list
                                                  (RT/list FNONCE PersistentVector/EMPTY form)))
                  (let [dynamicBindings (^[Object/1] RT/map
                                          LOCAL_ENV
                                          (.deref LOCAL_ENV)
                                          NEXT_LOCAL_NUM
                                          (.deref NEXT_LOCAL_NUM))]
                    (try
                      (Var/pushThreadBindings dynamicBindings)
                      (let [^:mutable lbs PersistentVector/EMPTY]
                        (loop [^int i 0]
                          (when (< i (.count bindings))
                            (when-not (instance? Symbol (.nth bindings i))
                              (throw (IllegalArgumentException.
                                       (java-str
                                         "Bad binding form, expected symbol, got: "
                                         (.nth bindings i)))))
                            (let [sym (cast Symbol (.nth bindings i))]
                              (when (some? (.getNamespace sym))
                                (throw (Util/runtimeException
                                         (java-str "Can't let qualified name: " sym))))
                              (let [lb (arbace.lang.Compiler/registerLocal
                                         sym
                                         (arbace.lang.Compiler/tagOf sym)
                                         nil
                                         false)]
                                (set! (.-canBeCleared lb) false)
                                (set! lbs (.cons lbs lb))
                                (recur (unchecked-add-int i 2))))))
                        (let [^:mutable bindingInits PersistentVector/EMPTY]
                          (loop [^int i 0]
                            (when (< i (.count bindings))
                              (let [sym (cast Symbol (.nth bindings i))
                                    init (do
                                           (Var/pushThreadBindings
                                             (^[Object/1] RT/map CLASS_FORMS_NO_DELEGATE RT/T))
                                           (try
                                             (arbace.lang.Compiler/analyze
                                               C/EXPRESSION
                                               (.nth bindings (unchecked-add-int i 1))
                                               (.-name sym))
                                             (finally (Var/popThreadBindings))))
                                    lb (cast LocalBinding (.nth lbs (unchecked-divide-int i 2)))]
                                (set! (.-init lb) init)
                                (let [bi (BindingInit. lb init)]
                                  (set! bindingInits (.cons bindingInits bi))
                                  (recur (unchecked-add-int i 2))))))
                          (LetFnExpr. bindingInits
                                      (.parse (Compiler$BodyExpr$Parser.) context body))))
                      (finally (Var/popThreadBindings))))))))))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval letfns")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (loop [^int i 0]
        (when (< i (.count bindingInits))
          (let [bi (cast BindingInit (.nth bindingInits i))]
            (.visitInsn gen Opcodes/ACONST_NULL)
            (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) (.-idx (.-binding bi)))
            (recur (unchecked-inc-int i)))))
      (let [^:mutable ^IPersistentSet lbset PersistentHashSet/EMPTY]
        (loop [^int i 0]
          (when (< i (.count bindingInits))
            (let [bi (cast BindingInit (.nth bindingInits i))]
              (set! lbset (cast IPersistentSet (.cons lbset (.-binding bi))))
              (.emit (.-init bi) C/EXPRESSION objx gen)
              (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) (.-idx (.-binding bi)))
              (recur (unchecked-inc-int i)))))
        (loop [^int i 0]
          (when (< i (.count bindingInits))
            (let [bi (cast BindingInit (.nth bindingInits i))
                  fe (cast ObjExpr (.-init bi))]
              (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ILOAD) (.-idx (.-binding bi)))
              (.emitLetFnInits fe gen objx lbset)
              (recur (unchecked-inc-int i)))))
        (let [loopLabel (.mark gen)]
          (.emit body context objx gen)
          (let [end (.mark gen)]
            (loop [bis (.seq bindingInits)]
              (when (some? bis)
                (let [bi (cast BindingInit (.first bis))
                      ^:mutable lname (.-name (.-binding bi))]
                  (when (.endsWith lname "__auto__") (set! lname (java-str lname (RT/nextID))))
                  (let [primc (arbace.lang.Compiler/maybePrimitiveType (.-init bi))]
                    (if (some? primc)
                        (do
                          (.visitLocalVariable gen
                                               lname
                                               (Type/getDescriptor primc)
                                               nil
                                               loopLabel
                                               end
                                               (.-idx (.-binding bi)))
                          (recur (.next bis)))
                        (do
                          (.visitLocalVariable gen
                                               lname
                                               "Ljava/lang/Object;"
                                               nil
                                               loopLabel
                                               end
                                               (.-idx (.-binding bi)))
                          (recur (.next bis))))))))))))

    (method ^:public hasJavaClass ^boolean [this] (.hasJavaClass body))

    (method ^:public getJavaClass ^Class [this] (.getJavaClass body)))

  (defclass ^:public ^:static LetExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:public ^:final ^PersistentVector bindingInits)

    (field ^:public ^:final ^Expr body)

    (field ^:public ^:final ^boolean isLoop)

    (constructor ^:public [this ^PersistentVector bindingInits ^Expr body ^boolean isLoop]
      (set! (.-bindingInits this) bindingInits)
      (set! (.-body this) body)
      (set! (.-isLoop this) isLoop))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)
              isLoop (.equals (RT/first form) LOOP)]
          (when-not (instance? IPersistentVector (RT/second form))
            (throw (IllegalArgumentException. "Bad binding form, expected vector")))
          (let [bindings (cast IPersistentVector (RT/second form))]
            (when-not (== (unchecked-remainder-int (.count bindings) 2) 0)
              (throw (IllegalArgumentException.
                       "Bad binding form, expected matched symbol expression pairs")))
            (let [body (RT/next (RT/next form))]
              (if (or (identical? context C/EVAL) (and (identical? context C/EXPRESSION) isLoop))
                  (arbace.lang.Compiler/analyze context
                                                (RT/list
                                                  (RT/list FNONCE PersistentVector/EMPTY form)))
                  (let [method (cast ObjMethod (.deref METHOD))
                        backupMethodLocals (.-locals method)
                        backupMethodIndexLocals (.-indexlocals method)
                        ^:mutable ^IPersistentVector recurMismatches PersistentVector/EMPTY]
                    (loop [^int i 0]
                      (when (< i (unchecked-divide-int (.count bindings) 2))
                        (set! recurMismatches (.cons recurMismatches RT/F))
                        (recur (unchecked-inc-int i))))
                    (while true
                      (let [^:mutable dynamicBindings (^[Object/1] RT/map
                                                        LOCAL_ENV
                                                        (.deref LOCAL_ENV)
                                                        NEXT_LOCAL_NUM
                                                        (.deref NEXT_LOCAL_NUM))]
                        (set! (.-locals method) backupMethodLocals)
                        (set! (.-indexlocals method) backupMethodIndexLocals)
                        (let [looproot (PathNode. PATHTYPE/PATH (cast PathNode (.get CLEAR_PATH)))
                              clearroot (PathNode. PATHTYPE/PATH looproot)
                              clearpath (PathNode. PATHTYPE/PATH looproot)]
                          (when isLoop
                            (set! dynamicBindings (.assoc dynamicBindings LOOP_LOCALS nil)))
                          (try
                            (Var/pushThreadBindings dynamicBindings)
                            (let [^:mutable bindingInits PersistentVector/EMPTY
                                  ^:mutable loopLocals PersistentVector/EMPTY]
                              (loop [^int i 0]
                                (when (< i (.count bindings))
                                  (when-not (instance? Symbol (.nth bindings i))
                                    (throw (IllegalArgumentException.
                                             (java-str
                                               "Bad binding form, expected symbol, got: "
                                               (.nth bindings i)))))
                                  (let [sym (cast Symbol (.nth bindings i))]
                                    (when (some? (.getNamespace sym))
                                      (throw (Util/runtimeException
                                               (java-str "Can't let qualified name: " sym))))
                                    (when (.equals sym _AMP_)
                                      (throw (Util/runtimeException
                                               "Can't use & as a local binding")))
                                    (let [^:mutable init (arbace.lang.Compiler/analyze
                                                           C/EXPRESSION
                                                           (.nth bindings (unchecked-add-int i 1))
                                                           (.-name sym))]
                                      (when isLoop
                                        (cond
                                          (and (some? recurMismatches)
                                               (RT/booleanCast
                                                 (.nth recurMismatches (unchecked-divide-int i 2))))
                                            (do
                                              (set!
                                                init
                                                (StaticMethodExpr.
                                                  ""
                                                  0
                                                  0
                                                  nil
                                                  RT
                                                  "box"
                                                  (RT/vector init)
                                                  false))
                                              (when (RT/booleanCast (.deref RT/WARN_ON_REFLECTION))
                                                (.println
                                                  (RT/errPrintWriter)
                                                  (java-str "Auto-boxing loop arg: " sym))))
                                          (identical?
                                            (arbace.lang.Compiler/maybePrimitiveType init)
                                            Integer/TYPE)
                                            (set!
                                              init
                                              (StaticMethodExpr.
                                                ""
                                                0
                                                0
                                                nil
                                                RT
                                                "longCast"
                                                (RT/vector init)
                                                false))
                                          (identical?
                                            (arbace.lang.Compiler/maybePrimitiveType init)
                                            Float/TYPE)
                                            (set!
                                              init
                                              (StaticMethodExpr.
                                                ""
                                                0
                                                0
                                                nil
                                                RT
                                                "doubleCast"
                                                (RT/vector init)
                                                false))))
                                      (try
                                        (when isLoop
                                          (Var/pushThreadBindings
                                            (^[Object/1] RT/map
                                              CLEAR_PATH
                                              clearpath
                                              CLEAR_ROOT
                                              clearroot
                                              NO_RECUR
                                              nil)))
                                        (let [lb (arbace.lang.Compiler/registerLocal
                                                   sym
                                                   (arbace.lang.Compiler/tagOf sym)
                                                   init
                                                   false)
                                              bi (BindingInit. lb init)]
                                          (set! bindingInits (.cons bindingInits bi))
                                          (when isLoop (set! loopLocals (.cons loopLocals lb))))
                                        (finally (when isLoop (Var/popThreadBindings))))
                                      (recur (unchecked-add-int i 2))))))
                              (when isLoop (.set LOOP_LOCALS loopLocals))
                              (let [^:mutable ^Expr bodyExpr nil
                                    ^:mutable moreMismatches false]
                                (try
                                  (when isLoop
                                    (let [methodReturnContext (when (identical? context C/RETURN)
                                                                (.deref METHOD_RETURN_CONTEXT))]
                                      (Var/pushThreadBindings
                                        (^[Object/1] RT/map
                                          CLEAR_PATH
                                          clearpath
                                          CLEAR_ROOT
                                          clearroot
                                          NO_RECUR
                                          nil
                                          METHOD_RETURN_CONTEXT
                                          methodReturnContext))))
                                  (set! bodyExpr
                                        (.parse (Compiler$BodyExpr$Parser.)
                                                (if isLoop C/RETURN context)
                                                body))
                                  (finally
                                    (when isLoop
                                      (Var/popThreadBindings)
                                      (loop [^int i 0]
                                        (when (< i (.count loopLocals))
                                          (let [lb (cast LocalBinding (.nth loopLocals i))]
                                            (if (.-recurMistmatch lb)
                                                (do
                                                  (set!
                                                    recurMismatches
                                                    (cast
                                                      IPersistentVector
                                                      (.assoc recurMismatches i RT/T)))
                                                  (set! moreMismatches true)
                                                  (recur (unchecked-inc-int i)))
                                                (recur (unchecked-inc-int i)))))))))
                                (when-not moreMismatches
                                  (return (LetExpr. bindingInits bodyExpr isLoop)))))
                            (finally (Var/popThreadBindings)))))))))))))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval let/loop")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen false))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen true))

    (method ^:public doEmit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen
                                   ^boolean emitUnboxed]
      (let [^{:tag (HashMap BindingInit Label)} bindingLabels (HashMap.)]
        (loop [^int i 0]
          (when (< i (.count bindingInits))
            (let [bi (cast BindingInit (.nth bindingInits i))
                  primc (arbace.lang.Compiler/maybePrimitiveType (.-init bi))]
              (if (some? primc)
                  (do
                    (.emitUnboxed (cast MaybePrimitiveExpr (.-init bi)) C/EXPRESSION objx gen)
                    (.visitVarInsn gen
                                   (.getOpcode (Type/getType primc) Opcodes/ISTORE)
                                   (.-idx (.-binding bi))))
                  (let [bindingClass (HostExpr/maybeClass (.-tag (.-binding bi)) true)]
                    (when-not (FISupport/maybeEmitFIAdapter objx gen (.-init bi) bindingClass)
                      (.emit (.-init bi) C/EXPRESSION objx gen))
                    (if (and (not (.-used (.-binding bi))) (.-canBeCleared (.-binding bi)))
                        (.pop gen)
                        (.visitVarInsn gen
                                       (.getOpcode OBJECT_TYPE Opcodes/ISTORE)
                                       (.-idx (.-binding bi))))))
              (.put bindingLabels bi (.mark gen))
              (recur (unchecked-inc-int i)))))
        (let [loopLabel (.mark gen)]
          (cond
            isLoop
              (try
                (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel))
                (if emitUnboxed
                    (.emitUnboxed (cast MaybePrimitiveExpr body) context objx gen)
                    (.emit body context objx gen))
                (finally (Var/popThreadBindings)))
            emitUnboxed (.emitUnboxed (cast MaybePrimitiveExpr body) context objx gen)
            :else (.emit body context objx gen))
          (let [end (.mark gen)]
            (loop [bis (.seq bindingInits)]
              (when (some? bis)
                (let [bi (cast BindingInit (.first bis))
                      ^:mutable lname (.-name (.-binding bi))]
                  (when (.endsWith lname "__auto__") (set! lname (java-str lname (RT/nextID))))
                  (let [primc (arbace.lang.Compiler/maybePrimitiveType (.-init bi))]
                    (if (some? primc)
                        (do
                          (.visitLocalVariable gen
                                               lname
                                               (Type/getDescriptor primc)
                                               nil
                                               (cast Label (.get bindingLabels bi))
                                               end
                                               (.-idx (.-binding bi)))
                          (recur (.next bis)))
                        (do
                          (.visitLocalVariable gen
                                               lname
                                               "Ljava/lang/Object;"
                                               nil
                                               (cast Label (.get bindingLabels bi))
                                               end
                                               (.-idx (.-binding bi)))
                          (recur (.next bis))))))))))))

    (method ^:public hasJavaClass ^boolean [this] (.hasJavaClass body))

    (method ^:public getJavaClass ^Class [this] (.getJavaClass body))

    (method ^:public canEmitPrimitive ^boolean [this]
      (and (instance? MaybePrimitiveExpr body) (.canEmitPrimitive (cast MaybePrimitiveExpr body)))))

  (defclass ^:public ^:static RecurExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:public ^:final ^IPersistentVector args)

    (field ^:public ^:final ^IPersistentVector loopLocals)

    (field ^:final ^int line)

    (field ^:final ^int column)

    (field ^:final ^String source)

    (constructor ^:public [this ^IPersistentVector loopLocals ^IPersistentVector args ^int line
                           ^int column ^String source]
      (set! (.-loopLocals this) loopLocals)
      (set! (.-args this) args)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-source this) source))

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval recur")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (let [loopLabel (cast Label (.deref LOOP_LABEL))]
        (when (nil? loopLabel) (throw (IllegalStateException.)))
        (loop [^int i 0]
          (when (< i (.count loopLocals))
            (let [lb (cast LocalBinding (.nth loopLocals i))
                  arg (cast Expr (.nth args i))]
              (if (some? (.getPrimitiveType lb))
                  (let [primc (.getPrimitiveType lb)
                        pc (arbace.lang.Compiler/maybePrimitiveType arg)]
                    (cond
                      (identical? pc primc)
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr arg) C/EXPRESSION objx gen)
                          (recur (unchecked-inc-int i)))
                      (and (identical? primc Long/TYPE) (identical? pc Integer/TYPE))
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr arg) C/EXPRESSION objx gen)
                          (.visitInsn gen Opcodes/I2L)
                          (recur (unchecked-inc-int i)))
                      (and (identical? primc Double/TYPE) (identical? pc Float/TYPE))
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr arg) C/EXPRESSION objx gen)
                          (.visitInsn gen Opcodes/F2D)
                          (recur (unchecked-inc-int i)))
                      (and (identical? primc Integer/TYPE) (identical? pc Long/TYPE))
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr arg) C/EXPRESSION objx gen)
                          (.invokeStatic gen RT_TYPE (Method/getMethod "int intCast(long)"))
                          (recur (unchecked-inc-int i)))
                      (and (identical? primc Float/TYPE) (identical? pc Double/TYPE))
                        (do
                          (.emitUnboxed (cast MaybePrimitiveExpr arg) C/EXPRESSION objx gen)
                          (.visitInsn gen Opcodes/D2F)
                          (recur (unchecked-inc-int i)))
                      :else
                        (throw (IllegalArgumentException.
                                 (java-str " recur arg for primitive local: "
                                           (.-name lb)
                                           " is not matching primitive, had: "
                                           (if (.hasJavaClass arg)
                                               (.getName (.getJavaClass arg))
                                               "Object")
                                           ", needed: "
                                           (.getName primc))))))
                  (do (.emit arg C/EXPRESSION objx gen) (recur (unchecked-inc-int i)))))))
        (loop [^int i (unchecked-subtract-int (.count loopLocals) 1)]
          (when (>= i 0)
            (let [lb (cast LocalBinding (.nth loopLocals i))
                  primc (.getPrimitiveType lb)]
              (cond
                (.-isArg lb)
                  (do
                    (.storeArg gen
                               (unchecked-subtract-int (.-idx lb) (if (.-canBeDirect objx) 0 1)))
                    (recur (unchecked-dec-int i)))
                (some? primc)
                  (do
                    (.visitVarInsn gen (.getOpcode (Type/getType primc) Opcodes/ISTORE) (.-idx lb))
                    (recur (unchecked-dec-int i)))
                :else
                  (do
                    (.visitVarInsn gen (.getOpcode OBJECT_TYPE Opcodes/ISTORE) (.-idx lb))
                    (recur (unchecked-dec-int i)))))))
        (.goTo gen loopLabel)))

    (method ^:public hasJavaClass ^boolean [this] true)

    (method ^:public getJavaClass ^Class [this] RECUR_CLASS)

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [line (arbace.lang.Compiler/lineDeref)
              column (arbace.lang.Compiler/columnDeref)
              source (cast String (.deref SOURCE))
              method (cast ObjMethod (.deref METHOD))]
          (when (and (.-onceOnly (.-objx method))
                     (identical? (.-clearRoot method) (.deref CLEAR_ROOT)))
            (set! (.-onceOnly (.-objx method)) false))
          (let [form (cast ISeq frm)
                loopLocals (cast IPersistentVector (.deref LOOP_LOCALS))]
            ;; recur out of tail position and across try stay errors here (Clojure's test suite
            ;; holds the compiler to them); continue has that meaning (doc/classes/SPEC.md §5.7)
            (when (or (not (identical? context C/RETURN)) (nil? loopLocals))
              (throw (UnsupportedOperationException. "Can only recur from tail position")))
            (when (some? (.deref NO_RECUR))
              (throw (UnsupportedOperationException. "Cannot recur across try")))
            (let [^:mutable args PersistentVector/EMPTY]
              (loop [s (RT/seq (.next form))]
                (when (some? s)
                  (set! args (.cons args (arbace.lang.Compiler/analyze C/EXPRESSION (.first s))))
                  (recur (.next s))))
              (when-not (== (.count args) (.count loopLocals))
                (throw (IllegalArgumentException.
                         (String/format
                           "Mismatched argument count to recur, expected: %d args, got: %d"
                           (new Object/1 [(.count loopLocals) (.count args)])))))
              (loop [^int i 0]
                (when (< i (.count loopLocals))
                  (let [lb (cast LocalBinding (.nth loopLocals i))
                        primc (.getPrimitiveType lb)]
                    (if (some? primc)
                        (let [^:mutable mismatch false
                              pc (arbace.lang.Compiler/maybePrimitiveType (cast Expr (.nth args i)))]
                          (cond
                            (identical? primc Long/TYPE)
                              (when-not (or (or (or
                                                  (or
                                                    (identical? pc Long/TYPE)
                                                    (identical? pc Integer/TYPE))
                                                  (identical? pc Short/TYPE))
                                                (identical? pc Character/TYPE))
                                            (identical? pc Byte/TYPE))
                                (set! mismatch true))
                            (identical? primc Double/TYPE)
                              (when-not (or (identical? pc Double/TYPE) (identical? pc Float/TYPE))
                                (set! mismatch true)))
                          (if mismatch
                              (do
                                (set! (.-recurMistmatch lb) true)
                                (if (RT/booleanCast (.deref RT/WARN_ON_REFLECTION))
                                    (do
                                      (.println (RT/errPrintWriter)
                                                (java-str
                                                  source
                                                  ":"
                                                  line
                                                  " recur arg for primitive local: "
                                                  (.-name lb)
                                                  " is not matching primitive, had: "
                                                  (if (some? pc) (.getName pc) "Object")
                                                  ", needed: "
                                                  (.getName primc)))
                                      (recur (unchecked-inc-int i)))
                                    (recur (unchecked-inc-int i))))
                              (recur (unchecked-inc-int i))))
                        (recur (unchecked-inc-int i))))))
              (RecurExpr. loopLocals args line column source))))))

    (method ^:public canEmitPrimitive ^boolean [this] true)

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit this context objx gen)))

  (method ^:private ^:static registerLocal ^LocalBinding [^Symbol sym ^Symbol tag ^Expr init
                                                          ^boolean isArg]
    (let [num (arbace.lang.Compiler/getAndIncLocalNum)
          b (LocalBinding. num sym tag init isArg (arbace.lang.Compiler/clearPathRoot))
          localsMap (cast IPersistentMap (.deref LOCAL_ENV))]
      (.set LOCAL_ENV (RT/assoc localsMap (.-sym b) b))
      (let [method (cast ObjMethod (.deref METHOD))]
        (set! (.-locals method) (cast IPersistentMap (RT/assoc (.-locals method) b b)))
        (set! (.-indexlocals method) (cast IPersistentMap (RT/assoc (.-indexlocals method) num b)))
        b)))

  (method ^:private ^:static getAndIncLocalNum ^int []
    (let [num (.intValue (cast Number (.deref NEXT_LOCAL_NUM)))
          m (cast ObjMethod (.deref METHOD))]
      (when (> num (.-maxLocal m)) (set! (.-maxLocal m) num))
      (.set NEXT_LOCAL_NUM (unchecked-add-int num 1))
      num))

  (method ^:public ^:static analyze ^Expr [^C context form]
    (arbace.lang.Compiler/analyze context form nil))

  (method ^:private ^:static analyze ^Expr [^C context ^:mutable form ^String name]
    (try
      (when (instance? LazySeq form)
        (let [mform form]
          (set! form (RT/seq form))
          (when (nil? form) (set! form PersistentList/EMPTY))
          (set! form (.withMeta (cast IObj form) (RT/meta mform)))))
      (cond
        (nil? form) NIL_EXPR
        (identical? form Boolean/TRUE) TRUE_EXPR
        (identical? form Boolean/FALSE) FALSE_EXPR
        :else
          (let [fclass (.getClass form)]
            (cond
              (identical? fclass Symbol) (arbace.lang.Compiler/analyzeSymbol (cast Symbol form))
              (identical? fclass Keyword) (arbace.lang.Compiler/registerKeyword (cast Keyword form))
              (instance? Number form) (NumberExpr/parse (cast Number form))
              (identical? fclass String) (StringExpr. (.intern (cast String form)))
              (and (and (and (instance? IPersistentCollection form) (not (instance? IRecord form)))
                        (not (instance? IType form)))
                   (== (.count (cast IPersistentCollection form)) 0))
                (let [^:mutable ^Expr ret (EmptyExpr. form)]
                  (when (some? (RT/meta form))
                    (set! ret
                          (MetaExpr. ret
                                     (MapExpr/parse
                                       (if (identical? context C/EVAL) context C/EXPRESSION)
                                       (.meta (cast IObj form))))))
                  ret)
              (instance? ISeq form) (arbace.lang.Compiler/analyzeSeq context (cast ISeq form) name)
              (instance? IPersistentVector form)
                (VectorExpr/parse context (cast IPersistentVector form))
              (instance? IRecord form) (ConstantExpr. form)
              (instance? IType form) (ConstantExpr. form)
              (instance? IPersistentMap form) (MapExpr/parse context (cast IPersistentMap form))
              (instance? IPersistentSet form) (SetExpr/parse context (cast IPersistentSet form))
              :else (ConstantExpr. form))))
      (catch Throwable e
        (cond
          (instance? ClassFormsExpr$Signal e) (throw e)
          (not (instance? CompilerException e))
            (throw (CompilerException. (cast String (.deref SOURCE_PATH))
                                       (arbace.lang.Compiler/lineDeref)
                                       (arbace.lang.Compiler/columnDeref)
                                       e))
          :else (throw (cast CompilerException e))))))

  (defclass ^:public ^:static CompilerException
    :extends RuntimeException
    :implements [IExceptionInfo]

    (field ^:public ^:final ^String source)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final data)

    (field ^:public ^:static ^:final ^String ERR_NS "arbace.error")

    (field ^:public ^:static ^:final ^Keyword ERR_SOURCE (Keyword/intern ERR_NS "source"))

    (field ^:public ^:static ^:final ^Keyword ERR_LINE (Keyword/intern ERR_NS "line"))

    (field ^:public ^:static ^:final ^Keyword ERR_COLUMN (Keyword/intern ERR_NS "column"))

    (field ^:public ^:static ^:final ^Keyword ERR_PHASE (Keyword/intern ERR_NS "phase"))

    (field ^:public ^:static ^:final ^Keyword ERR_SYMBOL (Keyword/intern ERR_NS "symbol"))

    (field ^:public ^:static ^:final ^Keyword PHASE_READ (Keyword/intern nil "read-source"))

    (field ^:public ^:static ^:final ^Keyword PHASE_MACRO_SYNTAX_CHECK
      (Keyword/intern nil "macro-syntax-check"))

    (field ^:public ^:static ^:final ^Keyword PHASE_MACROEXPANSION
      (Keyword/intern nil "macroexpansion"))

    (field ^:public ^:static ^:final ^Keyword PHASE_COMPILE_SYNTAX_CHECK
      (Keyword/intern nil "compile-syntax-check"))

    (field ^:public ^:static ^:final ^Keyword PHASE_COMPILATION (Keyword/intern nil "compilation"))

    (field ^:public ^:static ^:final ^Keyword PHASE_EXECUTION (Keyword/intern nil "execution"))

    (field ^:public ^:static ^:final ^Keyword SPEC_PROBLEMS
      (Keyword/intern "clojure.spec.alpha" "problems"))

    (constructor ^:public [this ^String source ^int line ^int column ^Throwable cause]
      (this. source line column nil cause))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol sym ^Throwable cause]
      (this. source line column sym PHASE_COMPILE_SYNTAX_CHECK cause))

    (constructor ^:public [this ^String source ^int line ^int column ^Symbol sym ^Keyword phase
                           ^Throwable cause]
      (super. (CompilerException/makeMsg source line column sym phase cause) cause)
      (set! (.-source this) source)
      (set! (.-line this) line)
      (let [^:mutable ^Associative m (^[Object/1] RT/map
                                       ERR_PHASE
                                       phase
                                       ERR_LINE
                                       line
                                       ERR_COLUMN
                                       column)]
        (when (some? source) (set! m (RT/assoc m ERR_SOURCE source)))
        (when (some? sym) (set! m (RT/assoc m ERR_SYMBOL sym)))
        (set! (.-data this) m)))

    (method ^:public getData ^IPersistentMap [this]
      (cast IPersistentMap data))

    (method ^:private ^:static verb ^String [^Keyword phase]
      (cond
        (.equals PHASE_READ phase) "reading source"
        (or (.equals PHASE_COMPILE_SYNTAX_CHECK phase) (.equals PHASE_COMPILATION phase))
          "compiling"
        (.equals PHASE_EXECUTION phase) "executing"
        :else "macroexpanding"))

    (method ^:public ^:static makeMsg ^String [^String source ^int line ^int column ^Symbol sym
                                               ^Keyword phase ^Throwable cause]
      (java-str (if (.equals PHASE_MACROEXPANSION phase) "Unexpected error " "Syntax error ")
                (CompilerException/verb phase)
                " "
                (if (some? sym) (java-str sym " ") "")
                "at ("
                (if (and (some? source) (not (.equals source "NO_SOURCE_PATH")))
                    (java-str source ":")
                    "")
                line
                ":"
                column
                ")."))

    (method ^:public toString ^String [this]
      (let [cause (.getCause this)]
        (when (some? cause)
          (when (instance? IExceptionInfo cause)
            (let [data (.getData (cast IExceptionInfo cause))]
              (if (and (.equals PHASE_MACRO_SYNTAX_CHECK (.valAt data ERR_PHASE))
                       (some? (.valAt data SPEC_PROBLEMS)))
                  (return (String/format "%s" (new Object/1 [(.getMessage this)])))
                  (return (String/format "%s%n%s"
                                         (new Object/1 [(.getMessage this) (.getMessage cause)])))))))
        (.getMessage this))))

  (method ^:public ^:static isMacro ^Var [op]
    (when-not (and (instance? Symbol op)
                   (some? (arbace.lang.Compiler/referenceLocal (cast Symbol op))))
      (when (or (instance? Symbol op) (instance? Var op))
        (let [v (if (instance? Var op)
                    (cast Var op)
                    (arbace.lang.Compiler/lookupVar (cast Symbol op) false false))]
          (when (and (some? v) (.isMacro v))
            (when (and (not (identical? (.-ns v) (arbace.lang.Compiler/currentNS)))
                       (not (.isPublic v)))
              (throw (IllegalStateException. (java-str "var: " v " is not public"))))
            (return v))))
      nil))

  (method ^:public ^:static isInline ^IFn [op ^int arity]
    (when-not (and (instance? Symbol op)
                   (some? (arbace.lang.Compiler/referenceLocal (cast Symbol op))))
      (when (or (instance? Symbol op) (instance? Var op))
        (let [v (if (instance? Var op)
                    (cast Var op)
                    (arbace.lang.Compiler/lookupVar (cast Symbol op) false))]
          (when (some? v)
            (when (and (not (identical? (.-ns v) (arbace.lang.Compiler/currentNS)))
                       (not (.isPublic v)))
              (throw (IllegalStateException. (java-str "var: " v " is not public"))))
            (let [ret (cast IFn (RT/get (.meta v) inlineKey))]
              (when (some? ret)
                (let [arityPred (cast IFn (RT/get (.meta v) inlineAritiesKey))]
                  (when (or (nil? arityPred) (RT/booleanCast (.invoke arityPred arity)))
                    (return ret))))))))
      nil))

  (method ^:public ^:static namesStaticMember ^boolean [^Symbol sym]
    (and (some? (.-ns sym)) (nil? (arbace.lang.Compiler/namespaceFor sym))))

  (method ^:public ^:static preserveTag [^ISeq src dst]
    (let [tag (arbace.lang.Compiler/tagOf src)]
      (if (and (some? tag) (instance? IObj dst))
          (let [meta (RT/meta dst)]
            (.withMeta (cast IObj dst) (cast IPersistentMap (RT/assoc meta RT/TAG_KEY tag))))
          dst)))

  (field ^:private ^:static ^:volatile ^Var MACRO_CHECK nil)

  (field ^:private ^:static ^:volatile ^boolean MACRO_CHECK_LOADING false)

  (field ^:private ^:static ^:final MACRO_CHECK_LOCK (Object.))

  (method ^:private ^:static ensureMacroCheck :throws [ClassNotFoundException IOException] ^Var []
    (when (nil? MACRO_CHECK)
      (locking MACRO_CHECK_LOCK
        (when (nil? MACRO_CHECK)
          (set! MACRO_CHECK_LOADING true)
          (RT/load "clojure/spec/alpha")
          (RT/load "clojure/core/specs/alpha")
          (set! MACRO_CHECK (Var/find (Symbol/intern "clojure.spec.alpha" "macroexpand-check")))
          (set! MACRO_CHECK_LOADING false))))
    MACRO_CHECK)

  (method ^:public ^:static checkSpecs ^void [^Var v ^ISeq form]
    (when (and RT/CHECK_SPECS (not MACRO_CHECK_LOADING))
      (try
        (.applyTo (arbace.lang.Compiler/ensureMacroCheck) (RT/cons v (RT/list (.next form))))
        (catch Exception e
          (throw (CompilerException. (cast String (.deref SOURCE_PATH))
                                     (arbace.lang.Compiler/lineDeref)
                                     (arbace.lang.Compiler/columnDeref)
                                     (.toSymbol v)
                                     CompilerException/PHASE_MACRO_SYNTAX_CHECK
                                     e))))))

  (method ^:public ^:static macroexpand1 [x]
    (when (instance? ISeq x)
      (let [form (cast ISeq x)
            op (RT/first form)]
        (when (arbace.lang.Compiler/isSpecial op) (return x))
        (let [v (arbace.lang.Compiler/isMacro op)]
          (cond
            (some? v)
              (do
                (arbace.lang.Compiler/checkSpecs v form)
                (try
                  (let [args (RT/cons form
                                      (RT/cons (.get arbace.lang.Compiler/LOCAL_ENV) (.next form)))]
                    (return (.applyTo v args)))
                  (catch ArityException e
                    (if (.equals (.-name e)
                                 (java-str (arbace.lang.Compiler/munge (.-name (.-name (.-ns v))))
                                           "$"
                                           (arbace.lang.Compiler/munge (.-name (.-sym v)))))
                        (throw (ArityException. (unchecked-subtract-int (.-actual e) 2) (.-name e)))
                        (throw e)))
                  (catch CompilerException e (throw e))
                  (catch [IllegalArgumentException IllegalStateException arbace.lang.ExceptionInfo] e
                    (throw (CompilerException. (cast String (.deref SOURCE_PATH))
                                               (arbace.lang.Compiler/lineDeref)
                                               (arbace.lang.Compiler/columnDeref)
                                               (when (instance? Symbol op) (cast Symbol op))
                                               CompilerException/PHASE_MACRO_SYNTAX_CHECK
                                               e)))
                  (catch Throwable e
                    (throw (CompilerException. (cast String (.deref SOURCE_PATH))
                                               (arbace.lang.Compiler/lineDeref)
                                               (arbace.lang.Compiler/columnDeref)
                                               (when (instance? Symbol op) (cast Symbol op))
                                               (if (.equals (.getClass e) Exception)
                                                   CompilerException/PHASE_MACRO_SYNTAX_CHECK
                                                   CompilerException/PHASE_MACROEXPANSION)
                                               e)))))
            (instance? Symbol op)
              (let [sym (cast Symbol op)
                    sname (.-name sym)]
                (if (and (== (.charAt (.-name sym) 0) \.) (nil? (.-ns sym)))
                    (do
                      (when (< (RT/length form) 2)
                        (throw (IllegalArgumentException.
                                 "Malformed member expression, expecting (.member target ...)")))
                      (let [meth (Symbol/intern (.substring sname 1))
                            ^:mutable target (RT/second form)]
                        (when (some? (HostExpr/maybeClass target false))
                          (set! target
                                (.withMeta (cast IObj (RT/list IDENTITY target))
                                           (^[Object/1] RT/map RT/TAG_KEY CLASS))))
                        (return (arbace.lang.Compiler/preserveTag
                                  form
                                  (RT/listStar DOT target meth (.next (.next form)))))))
                    (let [idx (.lastIndexOf sname \.)]
                      (when (== idx (unchecked-subtract-int (.length sname) 1))
                        (return (RT/listStar NEW
                                             (Symbol/intern (.substring sname 0 idx))
                                             (.next form)))))))))))
    x)

  (method ^:static macroexpand [form]
    (let [exf (arbace.lang.Compiler/macroexpand1 form)]
      (if (not (identical? exf form)) (arbace.lang.Compiler/macroexpand exf) form)))

  (method ^:private ^:static analyzeSeq ^Expr [^C context ^ISeq form ^String name]
    (let [^:mutable ^Object line (arbace.lang.Compiler/lineDeref)
          ^:mutable ^Object column (arbace.lang.Compiler/columnDeref)]
      (when (and (some? (RT/meta form)) (.containsKey (RT/meta form) RT/LINE_KEY))
        (set! line (.valAt (RT/meta form) RT/LINE_KEY)))
      (when (and (some? (RT/meta form)) (.containsKey (RT/meta form) RT/COLUMN_KEY))
        (set! column (.valAt (RT/meta form) RT/COLUMN_KEY)))
      (Var/pushThreadBindings (^[Object/1] RT/map LINE line COLUMN column))
      (let [^:mutable ^Object op nil]
        (try
          (let [me (arbace.lang.Compiler/macroexpand1 form)]
            (if (not (identical? me form))
                (arbace.lang.Compiler/analyze context me name)
                (do
                  (set! op (RT/first form))
                  (when (nil? op)
                    (throw (IllegalArgumentException. (java-str "Can't call nil, form: " form))))
                  (let [inline (arbace.lang.Compiler/isInline op (RT/count (RT/next form)))]
                    (if (some? inline)
                        (arbace.lang.Compiler/analyze
                          context
                          (arbace.lang.Compiler/preserveTag form (.applyTo inline (RT/next form))))
                        (let [^:mutable ^IParser p nil]
                          (cond
                            (.equals op FN) (FnExpr/parse context form name)
                            (some? (set! p (cast IParser (.valAt specials op))))
                              (.parse p context form)
                            :else (InvokeExpr/parse context form))))))))
          (catch Throwable e
            (let [s (when (and (some? op) (instance? Symbol op)) (cast Symbol op))]
              (when (instance? ClassFormsExpr$Signal e) (throw e))
              (if (not (instance? CompilerException e))
                  (throw (CompilerException. (cast String (.deref SOURCE_PATH))
                                             (arbace.lang.Compiler/lineDeref)
                                             (arbace.lang.Compiler/columnDeref)
                                             s
                                             e))
                  (throw (cast CompilerException e)))))
          (finally (Var/popThreadBindings))))))

  (method ^:static errorMsg ^String [^String source ^int line ^int column ^String s]
    (String/format "%s, compiling:(%s:%d:%d)" (new Object/1 [s source line column])))

  (method ^:public ^:static eval [form]
    (arbace.lang.Compiler/eval form true))

  (method ^:public ^:static eval [^:mutable form ^boolean freshLoader]
    (let [^:mutable createdLoader false]
      (when true
        (Var/pushThreadBindings (^[Object/1] RT/map LOADER (RT/makeClassLoader)))
        (set! createdLoader true))
      (try
        (let [meta (RT/meta form)
              line (if (some? meta) (.valAt meta RT/LINE_KEY (.deref LINE)) (.deref LINE))
              column (if (some? meta) (.valAt meta RT/COLUMN_KEY (.deref COLUMN)) (.deref COLUMN))
              ^:mutable bindings (^[Object/1] RT/mapUniqueKeys LINE line COLUMN column)]
          (when (some? meta)
            (let [eval_file (.valAt meta RT/EVAL_FILE_KEY)]
              (when (some? eval_file)
                (set! bindings (.assoc bindings SOURCE_PATH eval_file))
                (try
                  (set! bindings
                        (.assoc bindings SOURCE (.getName (File. (cast String eval_file)))))
                  (catch Throwable t)))))
          (Var/pushThreadBindings bindings)
          (try
            (set! form (arbace.lang.Compiler/macroexpand form))
            (cond
              (and (instance? ISeq form) (Util/equals (RT/first form) DO))
                (let [^:mutable s (RT/next form)
                      pushed (ClassFormsExpr/pushSiblings (cast ISeq form))]
                  (try
                    (while (some? (RT/next s))
                      (arbace.lang.Compiler/eval (RT/first s) false)
                      (set! s (RT/next s)))
                    (arbace.lang.Compiler/eval (RT/first s) false)
                    (finally (when pushed (Var/popThreadBindings)))))
              (or (instance? IType form)
                  (and (instance? IPersistentCollection form)
                       (not (and (instance? Symbol (RT/first form))
                                 (.startsWith (.-name (cast Symbol (RT/first form))) "def")))))
                (let [fexpr (arbace.lang.Compiler/analyze
                              C/EXPRESSION
                              (RT/list FN PersistentVector/EMPTY form)
                              (java-str "eval" (RT/nextID)))
                      fn (cast IFn (.eval fexpr))]
                  (.invoke fn))
              :else
                (let [^:mutable ^Expr expr nil]
                  (try
                    (set! expr (arbace.lang.Compiler/analyze C/EVAL form))
                    ;; class forms outside a fn (in a def's initializer): evaluated as a fn
                    (catch ClassFormsExpr$Signal sig
                      (set! expr
                            (arbace.lang.Compiler/analyze
                              C/EVAL
                              (RT/list (RT/list FN PersistentVector/EMPTY form))))))
                  (.eval expr)))
            (finally (Var/popThreadBindings))))
        (finally (when createdLoader (Var/popThreadBindings))))))

  (method ^:private ^:static registerConstant ^int [o]
    (if (not (.isBound CONSTANTS))
        -1
        (let [v (cast PersistentVector (.deref CONSTANTS))
              ^{:tag (IdentityHashMap Object Integer)} ids (cast
                                                             IdentityHashMap
                                                             (.deref CONSTANT_IDS))
              i (cast Integer (.get ids o))]
          (if (some? i) i (do (.set CONSTANTS (RT/conj v o)) (.put ids o (.count v)) (.count v))))))

  (method ^:private ^:static registerKeyword ^KeywordExpr [^Keyword keyword]
    (if (not (.isBound KEYWORDS))
        (KeywordExpr. keyword)
        (let [keywordsMap (cast IPersistentMap (.deref KEYWORDS))
              id (RT/get keywordsMap keyword)]
          (when (nil? id)
            (.set KEYWORDS
                  (RT/assoc keywordsMap keyword (arbace.lang.Compiler/registerConstant keyword))))
          (KeywordExpr. keyword))))

  (method ^:private ^:static registerKeywordCallsite ^int [^Keyword keyword]
    (let [^:mutable keywordCallsites (cast IPersistentVector (.deref KEYWORD_CALLSITES))]
      (set! keywordCallsites (.cons keywordCallsites keyword))
      (.set KEYWORD_CALLSITES keywordCallsites)
      (unchecked-subtract-int (.count keywordCallsites) 1)))

  (method ^:private ^:static registerProtocolCallsite ^int [^Var v]
    (let [^:mutable protocolCallsites (cast IPersistentVector (.deref PROTOCOL_CALLSITES))]
      (set! protocolCallsites (.cons protocolCallsites v))
      (.set PROTOCOL_CALLSITES protocolCallsites)
      (unchecked-subtract-int (.count protocolCallsites) 1)))

  (method ^:static fwdPath ^ISeq [^:mutable ^PathNode p1]
    (let [^:mutable ^ISeq ret nil]
      (while (some? p1) (set! ret (RT/cons p1 ret)) (set! p1 (.-parent p1)))
      ret))

  (method ^:static commonPath ^PathNode [^PathNode n1 ^PathNode n2]
    (let [^:mutable xp (arbace.lang.Compiler/fwdPath n1)
          ^:mutable yp (arbace.lang.Compiler/fwdPath n2)]
      (when-not (not (identical? (RT/first xp) (RT/first yp)))
        (while (and (some? (RT/second xp)) (identical? (RT/second xp) (RT/second yp)))
          (set! xp (.next xp))
          (set! yp (.next yp)))
        (cast PathNode (RT/first xp)))))

  (method ^:static addAnnotation ^void [visitor ^IPersistentMap meta]
    (when (and (some? meta) (.isBound ADD_ANNOTATIONS)) (.invoke ADD_ANNOTATIONS visitor meta)))

  (method ^:static addParameterAnnotation ^void [visitor ^IPersistentMap meta ^int i]
    (when (and (some? meta) (.isBound ADD_ANNOTATIONS)) (.invoke ADD_ANNOTATIONS visitor meta i)))

  (method ^:private ^:static analyzeSymbol ^Expr [^Symbol sym]
    (let [tag (arbace.lang.Compiler/tagOf sym)]
      (cond
        (nil? (.-ns sym))
          (let [b (arbace.lang.Compiler/referenceLocal sym)]
            (when (some? b) (return (LocalBindingExpr. b tag))))
        (and (nil? (arbace.lang.Compiler/namespaceFor sym)) (not (Util/isPosDigit (.-name sym))))
          (let [nsSym (Symbol/intern (.-ns sym))
                c (HostExpr/maybeClass nsSym false)]
            (when (some? c)
              (if (some? (Reflector/getField c (.-name sym) true))
                  (let [^{:tag (List Executable)} maybeOverloads
                          (QualifiedMethodExpr/methodOverloads
                            c
                            (.-name sym)
                            Compiler$QualifiedMethodExpr$MethodKind/STATIC)]
                    (if (.isEmpty maybeOverloads)
                        (return (StaticFieldExpr.
                                  (arbace.lang.Compiler/lineDeref)
                                  (arbace.lang.Compiler/columnDeref)
                                  c
                                  (.-name sym)
                                  tag))
                        (return (QualifiedMethodExpr.
                                  c
                                  sym
                                  (StaticFieldExpr.
                                    (arbace.lang.Compiler/lineDeref)
                                    (arbace.lang.Compiler/columnDeref)
                                    c
                                    (.-name sym)
                                    tag)))))
                  (return (QualifiedMethodExpr. c sym))))))
      (let [o (arbace.lang.Compiler/resolve sym)]
        (cond
          (instance? Var o)
            (let [v (cast Var o)]
              (when (some? (arbace.lang.Compiler/isMacro v))
                (throw (Util/runtimeException (java-str "Can't take value of a macro: " v))))
              (if (RT/booleanCast (RT/get (.meta v) RT/CONST_KEY))
                  (arbace.lang.Compiler/analyze C/EXPRESSION (RT/list QUOTE (.get v)))
                  (do (arbace.lang.Compiler/registerVar v) (VarExpr. v tag))))
          (instance? Class o) (ConstantExpr. o)
          (instance? Symbol o) (UnresolvedVarExpr. (cast Symbol o))
          :else
            (throw (Util/runtimeException
                     (java-str "Unable to resolve symbol: " sym " in this context")))))))

  (method ^:static destubClassName ^String [^String className]
    (if (.startsWith className COMPILE_STUB_PREFIX)
        (.substring className (unchecked-add-int (.length COMPILE_STUB_PREFIX) 1))
        className))

  (method ^:static getType ^Type [^Class c]
    (let [^:mutable descriptor (.getDescriptor (Type/getType c))]
      (when (.startsWith descriptor "L")
        (set! descriptor
              (java-str "L" (arbace.lang.Compiler/destubClassName (.substring descriptor 1)))))
      (Type/getType descriptor)))

  (method ^:static resolve [^Symbol sym ^boolean allowPrivate]
    (arbace.lang.Compiler/resolveIn (arbace.lang.Compiler/currentNS) sym allowPrivate))

  (method ^:static resolve [^Symbol sym]
    (arbace.lang.Compiler/resolveIn (arbace.lang.Compiler/currentNS) sym false))

  (method ^:static namespaceFor ^Namespace [^Symbol sym]
    (arbace.lang.Compiler/namespaceFor (arbace.lang.Compiler/currentNS) sym))

  (method ^:static namespaceFor ^Namespace [^Namespace inns ^Symbol sym]
    (let [nsSym (Symbol/intern (.-ns sym))
          ^:mutable ns (.lookupAlias inns nsSym)]
      (when (nil? ns) (set! ns (Namespace/find nsSym)))
      ns))

  (method ^:public ^:static resolveIn [^Namespace n ^Symbol sym ^boolean allowPrivate]
    (cond
      (some? (.-ns sym))
        (let [ns (arbace.lang.Compiler/namespaceFor n sym)]
          (when (nil? ns)
            (let [ac (HostExpr/maybeArrayClass sym)]
              (when (some? ac) (return ac))
              (throw (Util/runtimeException (java-str "No such namespace: " (.-ns sym))))))
          (let [v (.findInternedVar ns (Symbol/intern (.-name sym)))]
            (cond
              (nil? v) (throw (Util/runtimeException (java-str "No such var: " sym)))
              (and (and (not (identical? (.-ns v) (arbace.lang.Compiler/currentNS)))
                        (not (.isPublic v)))
                   (not allowPrivate))
                (throw (IllegalStateException. (java-str "var: " sym " is not public"))))
            v))
      (or (> (.indexOf (.-name sym) \.) 0) (== (.charAt (.-name sym) 0) \[))
        (RT/classForName (.-name sym))
      (.equals sym NS) RT/NS_VAR
      (.equals sym IN_NS) RT/IN_NS_VAR
      (Util/equals sym (.get COMPILE_STUB_SYM)) (.get COMPILE_STUB_CLASS)
      :else
        (let [o (.getMapping n sym)]
          (when (nil? o)
            (if (RT/booleanCast (.deref RT/ALLOW_UNRESOLVED_VARS))
                (return sym)
                (throw (Util/runtimeException
                         (java-str "Unable to resolve symbol: " sym " in this context")))))
          o)))

  (method ^:public ^:static maybeResolveIn [^Namespace n ^Symbol sym]
    (cond
      (some? (.-ns sym))
        (let [ns (arbace.lang.Compiler/namespaceFor n sym)]
          (if (nil? ns)
              (HostExpr/maybeArrayClass sym)
              (let [v (.findInternedVar ns (Symbol/intern (.-name sym)))] (when (some? v) v))))
      (or (and (> (.indexOf (.-name sym) \.) 0) (not (.endsWith (.-name sym) ".")))
          (== (.charAt (.-name sym) 0) \[))
        (try
          (RT/classForName (.-name sym))
          (catch Exception e (when-not (instance? ClassNotFoundException e) (Util/sneakyThrow e))))
      (.equals sym NS) RT/NS_VAR
      (.equals sym IN_NS) RT/IN_NS_VAR
      :else (let [o (.getMapping n sym)] o)))

  (method ^:static lookupVar ^Var [^Symbol sym ^boolean internNew ^boolean registerMacro]
    (let [^:mutable ^Var var nil]
      (cond
        (some? (.-ns sym))
          (let [ns (arbace.lang.Compiler/namespaceFor sym)]
            (when (nil? ns) (return nil))
            (let [name (Symbol/intern (.-name sym))]
              (if (and internNew (identical? ns (arbace.lang.Compiler/currentNS)))
                  (set! var (.intern (arbace.lang.Compiler/currentNS) name))
                  (set! var (.findInternedVar ns name)))))
        (.equals sym NS) (set! var RT/NS_VAR)
        (.equals sym IN_NS) (set! var RT/IN_NS_VAR)
        :else
          (let [o (.getMapping (arbace.lang.Compiler/currentNS) sym)]
            (cond
              (nil? o)
                (when internNew
                  (set! var (.intern (arbace.lang.Compiler/currentNS) (Symbol/intern (.-name sym)))))
              (instance? Var o) (set! var (cast Var o))
              :else
                (throw (Util/runtimeException
                         (java-str "Expecting var, but " sym " is mapped to " o))))))
      (when (and (some? var) (or (not (.isMacro var)) registerMacro))
        (arbace.lang.Compiler/registerVar var))
      var))

  (method ^:static lookupVar ^Var [^Symbol sym ^boolean internNew]
    (arbace.lang.Compiler/lookupVar sym internNew true))

  (method ^:private ^:static registerVar ^void [^Var var]
    (when-not (not (.isBound VARS))
      (let [varsMap (cast IPersistentMap (.deref VARS))
            id (RT/get varsMap var)]
        (when (nil? id)
          (.set VARS (RT/assoc varsMap var (arbace.lang.Compiler/registerConstant var)))))))

  (method ^:static currentNS ^Namespace []
    (cast Namespace (.deref RT/CURRENT_NS)))

  (method ^:static closeOver ^void [^LocalBinding b ^ObjMethod method]
    (when (and (some? b) (some? method))
      (let [lb (cast LocalBinding (RT/get (.-locals method) b))]
        (if (nil? lb)
            (do
              (set! (.-closes (.-objx method))
                    (cast IPersistentMap (RT/assoc (.-closes (.-objx method)) b b)))
              (arbace.lang.Compiler/closeOver b (.-parent method)))
            (do
              (when (== (.-idx lb) 0) (set! (.-usesThis method) true))
              (when (some? (.deref IN_CATCH_FINALLY))
                (set!
                  (.-localsUsedInCatchFinally method)
                  (cast PersistentHashSet (.cons (.-localsUsedInCatchFinally method) (.-idx b))))))))))

  (method ^:static referenceLocal ^LocalBinding [^Symbol sym]
    (when-not (not (.isBound LOCAL_ENV))
      (let [b (cast LocalBinding (RT/get (.deref LOCAL_ENV) sym))]
        (when (some? b)
          (let [method (cast ObjMethod (.deref METHOD))]
            (when (== (.-idx b) 0) (set! (.-usesThis method) true))
            (arbace.lang.Compiler/closeOver b method)))
        b)))

  (method ^:private ^:static tagOf ^Symbol [o]
    (let [tag (RT/get (RT/meta o) RT/TAG_KEY)]
      (cond
        (instance? Symbol tag) (cast Symbol tag)
        (instance? String tag) (Symbol/intern nil (cast String tag)))))

  (method ^:public ^:static loadFile :throws [IOException] [^String file]
    (let [f (FileInputStream. file)]
      (try
        (arbace.lang.Compiler/load (InputStreamReader. f RT/UTF8)
                                   (.getAbsolutePath (File. file))
                                   (.getName (File. file)))
        (finally (.close f)))))

  (method ^:public ^:static load [^Reader rdr]
    (arbace.lang.Compiler/load rdr nil "NO_SOURCE_FILE"))

  (method ^:static consumeWhitespaces ^void [^LineNumberingPushbackReader pushbackReader]
    (let [^:mutable ch (LispReader/read1 pushbackReader)]
      (while (LispReader/isWhitespace ch) (set! ch (LispReader/read1 pushbackReader)))
      (LispReader/unread pushbackReader ch)))

  (field ^:private ^:static ^:final OPTS_COND_ALLOWED
    (^[Object/1] RT/mapUniqueKeys LispReader/OPT_READ_COND LispReader/COND_ALLOW))

  (method ^:private ^:static readerOpts [^String sourceName]
    (when (and (some? sourceName) (.endsWith sourceName ".cljc")) OPTS_COND_ALLOWED))

  (method ^:public ^:static load [^Reader rdr ^String sourcePath ^String sourceName]
    (let [EOF (Object.)
          ^:mutable ^Object ret nil
          pushbackReader (if (instance? LineNumberingPushbackReader rdr)
                             (cast LineNumberingPushbackReader rdr)
                             (LineNumberingPushbackReader. rdr))]
      (arbace.lang.Compiler/consumeWhitespaces pushbackReader)
      (Var/pushThreadBindings
        (^[Object/1] RT/mapUniqueKeys LOADER
                                      (RT/makeClassLoader)
                                      SOURCE_PATH
                                      sourcePath
                                      SOURCE
                                      sourceName
                                      METHOD
                                      nil
                                      LOCAL_ENV
                                      nil
                                      LOOP_LOCALS
                                      nil
                                      CLASS_FORM_SIBLINGS
                                      nil
                                      NEXT_LOCAL_NUM
                                      (Integer/valueOf 0)
                                      RT/READEVAL
                                      RT/T
                                      RT/CURRENT_NS
                                      (.deref RT/CURRENT_NS)
                                      LINE_BEFORE
                                      (.getLineNumber pushbackReader)
                                      COLUMN_BEFORE
                                      (.getColumnNumber pushbackReader)
                                      LINE_AFTER
                                      (.getLineNumber pushbackReader)
                                      COLUMN_AFTER
                                      (.getColumnNumber pushbackReader)
                                      RT/UNCHECKED_MATH
                                      (.deref RT/UNCHECKED_MATH)
                                      RT/WARN_ON_REFLECTION
                                      (.deref RT/WARN_ON_REFLECTION)
                                      RT/DATA_READERS
                                      (.deref RT/DATA_READERS)))
      (let [readerOpts (arbace.lang.Compiler/readerOpts sourceName)]
        (try
          (loop [r (LispReader/read pushbackReader false EOF false readerOpts)]
            (when-not (identical? r EOF)
              (arbace.lang.Compiler/consumeWhitespaces pushbackReader)
              (.set LINE_AFTER (.getLineNumber pushbackReader))
              (.set COLUMN_AFTER (.getColumnNumber pushbackReader))
              (set! ret (arbace.lang.Compiler/eval r false))
              (.set LINE_BEFORE (.getLineNumber pushbackReader))
              (.set COLUMN_BEFORE (.getColumnNumber pushbackReader))
              (recur (LispReader/read pushbackReader false EOF false readerOpts))))
          (catch LispReader$ReaderException e
            (throw (CompilerException. sourcePath
                                       (.-line e)
                                       (.-column e)
                                       nil
                                       CompilerException/PHASE_READ
                                       (.getCause e))))
          (catch Throwable e
            (if (not (instance? CompilerException e))
                (throw (CompilerException. sourcePath
                                           (cast Integer (.deref LINE_BEFORE))
                                           (cast Integer (.deref COLUMN_BEFORE))
                                           nil
                                           CompilerException/PHASE_EXECUTION
                                           e))
                (throw (cast CompilerException e))))
          (finally (Var/popThreadBindings)))
        ret)))

  (method ^:public ^:static writeClassFile :throws [IOException] ^void [^String internalName
                                                                        ^byte/1 bytecode]
    (let [genPath (cast String (.deref COMPILE_PATH))]
      (when (nil? genPath) (throw (Util/runtimeException "*compile-path* not set")))
      (let [dirs (.split internalName "/")
            ^:mutable p genPath]
        (loop [^int i 0]
          (when (< i (unchecked-subtract-int (alength dirs) 1))
            (set! p (java-str p (java-str File/separator (aget dirs i))))
            (.mkdir (File. p))
            (recur (unchecked-inc-int i))))
        (let [path (java-str genPath File/separator internalName ".class")
              cf (File. path)]
          (.createNewFile cf)
          (let [cfs (FileOutputStream. cf)]
            (try (.write cfs bytecode) (.flush cfs) (finally (.close cfs))))))))

  (method ^:public ^:static pushNS ^void []
    (Var/pushThreadBindings
      (PersistentHashMap/create
        (new Object/1
             [(.setDynamic (Var/intern (Symbol/intern "arbace.core") (Symbol/intern "*ns*"))) nil]))))

  (method ^:public ^:static pushNSandLoader ^void [^ClassLoader loader]
    (Var/pushThreadBindings
      (^[Object/1] RT/map
        (.setDynamic (Var/intern (Symbol/intern "arbace.core") (Symbol/intern "*ns*")))
        nil
        RT/FN_LOADER_VAR
        loader
        RT/READEVAL
        RT/T)))

  (method ^:public ^:static getLookupThunk ^ILookupThunk [target ^Keyword k]
    nil)

  (method ^:static compile1 ^void [^GeneratorAdapter gen ^ObjExpr objx ^:mutable form]
    (let [^:mutable ^Object line (arbace.lang.Compiler/lineDeref)
          ^:mutable ^Object column (arbace.lang.Compiler/columnDeref)]
      (when (and (some? (RT/meta form)) (.containsKey (RT/meta form) RT/LINE_KEY))
        (set! line (.valAt (RT/meta form) RT/LINE_KEY)))
      (when (and (some? (RT/meta form)) (.containsKey (RT/meta form) RT/COLUMN_KEY))
        (set! column (.valAt (RT/meta form) RT/COLUMN_KEY)))
      (Var/pushThreadBindings
        (^[Object/1] RT/map LINE line COLUMN column LOADER (RT/makeClassLoader)))
      (try
        (set! form (arbace.lang.Compiler/macroexpand form))
        (if (and (instance? ISeq form) (Util/equals (RT/first form) DO))
            (let [pushed (ClassFormsExpr/pushSiblings (cast ISeq form))]
              (try
                (loop [s (RT/next form)]
                  (when (some? s)
                    (arbace.lang.Compiler/compile1 gen objx (RT/first s))
                    (recur (RT/next s))))
                (finally (when pushed (Var/popThreadBindings)))))
            (let [expr (try
                         (arbace.lang.Compiler/analyze C/EVAL form)
                         ;; class forms outside a fn (in a def's initializer): compiled as a fn
                         (catch ClassFormsExpr$Signal sig
                           (arbace.lang.Compiler/analyze
                             C/EVAL
                             (RT/list (RT/list FN PersistentVector/EMPTY form)))))]
              (set! (.-keywords objx) (cast IPersistentMap (.deref KEYWORDS)))
              (set! (.-vars objx) (cast IPersistentMap (.deref VARS)))
              (set! (.-constants objx) (cast PersistentVector (.deref CONSTANTS)))
              (.emit expr C/EXPRESSION objx gen)
              (.eval expr)))
        (finally (Var/popThreadBindings)))))

  (method ^:public ^:static compile :throws [IOException] [^Reader rdr ^String sourcePath
                                                           ^String sourceName]
    (when (nil? (.deref COMPILE_PATH)) (throw (Util/runtimeException "*compile-path* not set")))
    (let [EOF (Object.)
          ^Object ret nil
          pushbackReader (if (instance? LineNumberingPushbackReader rdr)
                             (cast LineNumberingPushbackReader rdr)
                             (LineNumberingPushbackReader. rdr))]
      (Var/pushThreadBindings
        (^[Object/1] RT/mapUniqueKeys SOURCE_PATH
                                      sourcePath
                                      SOURCE
                                      sourceName
                                      METHOD
                                      nil
                                      LOCAL_ENV
                                      nil
                                      LOOP_LOCALS
                                      nil
                                      CLASS_FORM_SIBLINGS
                                      nil
                                      NEXT_LOCAL_NUM
                                      (Integer/valueOf 0)
                                      RT/READEVAL
                                      RT/T
                                      RT/CURRENT_NS
                                      (.deref RT/CURRENT_NS)
                                      LINE_BEFORE
                                      (.getLineNumber pushbackReader)
                                      COLUMN_BEFORE
                                      (.getColumnNumber pushbackReader)
                                      LINE_AFTER
                                      (.getLineNumber pushbackReader)
                                      COLUMN_AFTER
                                      (.getColumnNumber pushbackReader)
                                      CONSTANTS
                                      PersistentVector/EMPTY
                                      CONSTANT_IDS
                                      (IdentityHashMap.)
                                      KEYWORD_CALLSITES
                                      nil
                                      PROTOCOL_CALLSITES
                                      nil
                                      KEYWORDS
                                      PersistentHashMap/EMPTY
                                      VARS
                                      PersistentHashMap/EMPTY
                                      RT/UNCHECKED_MATH
                                      (.deref RT/UNCHECKED_MATH)
                                      RT/WARN_ON_REFLECTION
                                      (.deref RT/WARN_ON_REFLECTION)
                                      RT/DATA_READERS
                                      (.deref RT/DATA_READERS)))
      (try
        (let [objx (ObjExpr. nil)]
          (set! (.-internalName objx)
                (java-str (.substring (.replace sourcePath File/separator "/")
                                      0
                                      (.lastIndexOf sourcePath \.))
                          RT/LOADER_SUFFIX))
          (set! (.-objtype objx) (Type/getObjectType (.-internalName objx)))
          (let [cw (arbace.lang.Compiler/classWriter)
                ^ClassVisitor cv cw]
            (.visit cv
                    JVM_BYTECODE_VERSION
                    (unchecked-add-int arbace.lang.Compiler/ACC_PUBLIC
                                       arbace.lang.Compiler/ACC_SUPER)
                    (.-internalName objx)
                    nil
                    "java/lang/Object"
                    nil)
            (let [gen (GeneratorAdapter.
                        (unchecked-add-int arbace.lang.Compiler/ACC_PUBLIC
                                           arbace.lang.Compiler/ACC_STATIC)
                        (Method/getMethod "void load ()")
                        nil
                        nil
                        cv)]
              (.visitCode gen)
              (let [readerOpts (arbace.lang.Compiler/readerOpts sourceName)]
                (loop [r (LispReader/read pushbackReader false EOF false readerOpts)]
                  (when-not (identical? r EOF)
                    (.set LINE_AFTER (.getLineNumber pushbackReader))
                    (.set COLUMN_AFTER (.getColumnNumber pushbackReader))
                    (arbace.lang.Compiler/compile1 gen objx r)
                    (.set LINE_BEFORE (.getLineNumber pushbackReader))
                    (.set COLUMN_BEFORE (.getColumnNumber pushbackReader))
                    (recur (LispReader/read pushbackReader false EOF false readerOpts))))
                (.returnValue gen)
                (.endMethod gen)
                (loop [^int i 0]
                  (if (< i (.count (.-constants objx)))
                      (if (.contains (.-usedConstants objx) i)
                          (do
                            (.visitField cv
                                         (unchecked-add-int
                                           arbace.lang.Compiler/ACC_PUBLIC
                                           arbace.lang.Compiler/ACC_STATIC)
                                         (.constantName objx i)
                                         (.getDescriptor (.constantType objx i))
                                         nil
                                         nil)
                            (recur (unchecked-inc-int i)))
                          (recur (unchecked-inc-int i)))
                      nil))
                (let [^:const ^int INITS_PER 100
                      ^:mutable numInits (unchecked-divide-int
                                           (.count (.-constants objx))
                                           INITS_PER)]
                  (when-not (== (unchecked-remainder-int (.count (.-constants objx)) INITS_PER) 0)
                    (set! numInits (unchecked-inc-int numInits)))
                  (loop [^int n 0]
                    (when (< n numInits)
                      (let [clinitgen (GeneratorAdapter.
                                        (unchecked-add-int
                                          arbace.lang.Compiler/ACC_PUBLIC
                                          arbace.lang.Compiler/ACC_STATIC)
                                        (Method/getMethod (java-str "void __init" n "()"))
                                        nil
                                        nil
                                        cv)]
                        (.visitCode clinitgen)
                        (try
                          (Var/pushThreadBindings (^[Object/1] RT/map RT/PRINT_DUP RT/T))
                          (loop [^int i (unchecked-multiply-int n INITS_PER)]
                            (if (and (< i (.count (.-constants objx)))
                                     (< i
                                        (unchecked-multiply-int (unchecked-add-int n 1) INITS_PER)))
                                (if (.contains (.-usedConstants objx) i)
                                    (do
                                      (.emitValue objx (.nth (.-constants objx) i) clinitgen)
                                      (.checkCast clinitgen (.constantType objx i))
                                      (.putStatic
                                        clinitgen
                                        (.-objtype objx)
                                        (.constantName objx i)
                                        (.constantType objx i))
                                      (recur (unchecked-inc-int i)))
                                    (recur (unchecked-inc-int i)))
                                nil))
                          (finally (Var/popThreadBindings)))
                        (.returnValue clinitgen)
                        (.endMethod clinitgen)
                        (recur (unchecked-inc-int n)))))
                  (let [clinitgen (GeneratorAdapter.
                                    (unchecked-add-int
                                      arbace.lang.Compiler/ACC_PUBLIC
                                      arbace.lang.Compiler/ACC_STATIC)
                                    (Method/getMethod "void <clinit> ()")
                                    nil
                                    nil
                                    cv)]
                    (.visitCode clinitgen)
                    (let [startTry (.newLabel clinitgen)
                          endTry (.newLabel clinitgen)
                          end (.newLabel clinitgen)
                          finallyLabel (.newLabel clinitgen)]
                      (loop [^int n 0]
                        (when (< n numInits)
                          (.invokeStatic clinitgen
                                         (.-objtype objx)
                                         (Method/getMethod (java-str "void __init" n "()")))
                          (recur (unchecked-inc-int n))))
                      (.push clinitgen (.replace (.-internalName objx) \/ \.))
                      (.invokeStatic clinitgen
                                     RT_TYPE
                                     (Method/getMethod "Class classForName(String)"))
                      (.invokeVirtual clinitgen
                                      CLASS_TYPE
                                      (Method/getMethod "ClassLoader getClassLoader()"))
                      (.invokeStatic clinitgen
                                     (Type/getType arbace.lang.Compiler)
                                     (Method/getMethod "void pushNSandLoader(ClassLoader)"))
                      (.mark clinitgen startTry)
                      (.invokeStatic clinitgen (.-objtype objx) (Method/getMethod "void load()"))
                      (.mark clinitgen endTry)
                      (.invokeStatic clinitgen
                                     VAR_TYPE
                                     (Method/getMethod "void popThreadBindings()"))
                      (.goTo clinitgen end)
                      (.mark clinitgen finallyLabel)
                      (.invokeStatic clinitgen
                                     VAR_TYPE
                                     (Method/getMethod "void popThreadBindings()"))
                      (.throwException clinitgen)
                      (.mark clinitgen end)
                      (.visitTryCatchBlock clinitgen startTry endTry finallyLabel nil)
                      (.returnValue clinitgen)
                      (.endMethod clinitgen)
                      (.visitEnd cv)
                      (arbace.lang.Compiler/writeClassFile (.-internalName objx) (.toByteArray cw)))))))))
        (catch LispReader$ReaderException e
          (throw (CompilerException. sourcePath (.-line e) (.-column e) (.getCause e))))
        (finally (Var/popThreadBindings)))
      ret))

  (defclass ^:public ^:static NewInstanceExpr
    :extends ObjExpr

    (field ^IPersistentCollection methods)

    (field ^{:tag (Map IPersistentVector java.lang.reflect.Method)} mmap)

    (field ^{:tag (Map IPersistentVector (Set Class))} covariants)

    (constructor ^:public [this tag] (super. tag))

    (defclass ^:static DeftypeParser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context ^:final frm]
        (let [^:mutable rform (cast ISeq frm)]
          (set! rform (RT/next rform))
          (let [tagname (.getName (cast Symbol (.first rform)))]
            (set! rform (.next rform))
            (let [classname (cast Symbol (.first rform))]
              (set! rform (.next rform))
              (let [fields (cast IPersistentVector (.first rform))]
                (set! rform (.next rform))
                (let [^:mutable ^IPersistentMap opts PersistentHashMap/EMPTY]
                  (while (and (some? rform) (instance? Keyword (.first rform)))
                    (set! opts (.assoc opts (.first rform) (RT/second rform)))
                    (set! rform (.next (.next rform))))
                  ;; a method body using the class forms: the class forms compiler compiles
                  ;; the whole deftype (arbace.classes.native/compile-deftype); deftype* is nil
                  (try
                    (NewInstanceExpr/build
                      (cast IPersistentVector (RT/get opts implementsKey PersistentVector/EMPTY))
                      fields
                      nil
                      tagname
                      classname
                      (cast Symbol (RT/get opts RT/TAG_KEY))
                      rform
                      frm
                      opts)
                    (catch ClassFormsExpr$Signal sig
                      (arbace.lang.Compiler/analyze
                        context
                        (.invoke (arbace.lang.Compiler/classForms "compile-deftype")
                                 (arbace.lang.Compiler/currentNS)
                                 frm)))))))))))

    (defclass ^:static ReifyParser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)
              enclosingMethod (cast ObjMethod (.deref METHOD))
              basename (if (some? enclosingMethod)
                           (java-str (ObjExpr/trimGenID (.-name (.-objx enclosingMethod))) "$")
                           (java-str (arbace.lang.Compiler/munge
                                       (.-name (.-name (arbace.lang.Compiler/currentNS))))
                                     "$"))
              simpleName (java-str "reify__" (RT/nextID))
              classname (java-str basename simpleName)
              ^:mutable rform (RT/next form)
              interfaces (.cons (cast IPersistentVector (RT/first rform))
                                (Symbol/intern "arbace.lang.IObj"))]
          (set! rform (RT/next rform))
          (let [ret (NewInstanceExpr/build interfaces
                                           nil
                                           nil
                                           classname
                                           (Symbol/intern classname)
                                           nil
                                           rform
                                           frm
                                           nil)
                ^:mutable fmeta (RT/meta frm)]
            (when (some? fmeta)
              (set! fmeta
                    (.without (.without (.without fmeta RT/LINE_KEY) RT/COLUMN_KEY) RT/FILE_KEY)))
            (if (> (RT/count fmeta) 0)
                (MetaExpr. ret
                           (MapExpr/parse (if (identical? context C/EVAL) context C/EXPRESSION)
                                          fmeta))
                ret)))))

    (method ^:static build ^ObjExpr [^IPersistentVector interfaceSyms ^IPersistentVector fieldSyms
                                     ^Symbol thisSym ^String tagName ^Symbol className
                                     ^Symbol typeTag ^ISeq methodForms frm ^IPersistentMap opts]
      (let [ret (NewInstanceExpr. nil)]
        (set! (.-src ret) frm)
        (set! (.-name ret) (.toString className))
        (set! (.-classMeta ret) (RT/meta className))
        (set! (.-internalName ret) (.replace (.-name ret) \. \/))
        (set! (.-objtype ret) (Type/getObjectType (.-internalName ret)))
        (set! (.-opts ret) opts)
        (when (some? thisSym) (set! (.-thisName ret) (.-name thisSym)))
        (when (some? fieldSyms)
          (let [^:mutable ^IPersistentMap fmap PersistentHashMap/EMPTY
                closesvec (new Object/1 (unchecked-multiply-int 2 (.count fieldSyms)))]
            (loop [^int i 0]
              (when (< i (.count fieldSyms))
                (let [sym (cast Symbol (.nth fieldSyms i))
                      lb (LocalBinding. -1
                                        sym
                                        nil
                                        (MethodParamExpr.
                                          (arbace.lang.Compiler/tagClass
                                            (arbace.lang.Compiler/tagOf sym)))
                                        false
                                        nil)]
                  (set! fmap (.assoc fmap sym lb))
                  (aset closesvec (unchecked-multiply-int i 2) lb)
                  (aset closesvec (unchecked-add-int (unchecked-multiply-int i 2) 1) lb)
                  (recur (unchecked-inc-int i)))))
            (set! (.-closes ret) (PersistentArrayMap. closesvec))
            (set! (.-fields ret) fmap)
            (loop [^int i (unchecked-subtract-int (.count fieldSyms) 1)]
              (when (and (>= i 0)
                         (or (or (or (.equals (.-name (cast Symbol (.nth fieldSyms i))) "__meta")
                                     (.equals (.-name (cast Symbol (.nth fieldSyms i))) "__extmap"))
                                 (.equals (.-name (cast Symbol (.nth fieldSyms i))) "__hash"))
                             (.equals (.-name (cast Symbol (.nth fieldSyms i))) "__hasheq")))
                (set! (.-altCtorDrops ret) (unchecked-inc-int (.-altCtorDrops ret)))
                (recur (unchecked-dec-int i))))))
        (let [^:mutable interfaces PersistentVector/EMPTY]
          (loop [s (RT/seq interfaceSyms)]
            (when (some? s)
              (let [c (cast Class (arbace.lang.Compiler/resolve (cast Symbol (.first s))))]
                (when-not (.isInterface c)
                  (throw (IllegalArgumentException.
                           (java-str "only interfaces are supported, had: " (.getName c)))))
                (set! interfaces (.cons interfaces c))
                (recur (.next s)))))
          (let [superClass Object
                mc (NewInstanceExpr/gatherMethods superClass (RT/seq interfaces))
                overrideables (aget mc 0)
                covariants (aget mc 1)]
            (set! (.-mmap ret) overrideables)
            (set! (.-covariants ret) covariants)
            (let [inames (NewInstanceExpr/interfaceNames interfaces)
                  stub (NewInstanceExpr/compileStub
                         (NewInstanceExpr/slashname superClass)
                         ret
                         inames
                         frm)
                  thistag (Symbol/intern nil (.getName stub))]
              (try
                (Var/pushThreadBindings
                  (^[Object/1] RT/mapUniqueKeys CONSTANTS
                                                PersistentVector/EMPTY
                                                CONSTANT_IDS
                                                (IdentityHashMap.)
                                                KEYWORDS
                                                PersistentHashMap/EMPTY
                                                VARS
                                                PersistentHashMap/EMPTY
                                                KEYWORD_CALLSITES
                                                PersistentVector/EMPTY
                                                PROTOCOL_CALLSITES
                                                PersistentVector/EMPTY
                                                NO_RECUR
                                                nil))
                (when (.isDeftype ret)
                  (Var/pushThreadBindings
                    (^[Object/1] RT/mapUniqueKeys
                      METHOD
                      nil
                      LOCAL_ENV
                      (.-fields ret)
                      COMPILE_STUB_SYM
                      (Symbol/intern nil tagName)
                      COMPILE_STUB_CLASS
                      stub))
                  (set!
                    (.-hintedFields ret)
                    (RT/subvec fieldSyms
                               0
                               (unchecked-subtract-int (.count fieldSyms) (.-altCtorDrops ret)))))
                (set! (.-line ret) (arbace.lang.Compiler/lineDeref))
                (set! (.-column ret) (arbace.lang.Compiler/columnDeref))
                (let [^:mutable ^IPersistentCollection methods nil]
                  (loop [s methodForms]
                    (when (some? s)
                      (let [m (NewInstanceMethod/parse
                                ret
                                (cast ISeq (RT/first s))
                                thistag
                                overrideables)]
                        (set! methods (RT/conj methods m))
                        (recur (RT/next s)))))
                  (set! (.-methods ret) methods)
                  (set! (.-keywords ret) (cast IPersistentMap (.deref KEYWORDS)))
                  (set! (.-vars ret) (cast IPersistentMap (.deref VARS)))
                  (set! (.-constants ret) (cast PersistentVector (.deref CONSTANTS)))
                  (set! (.-constantsID ret) (RT/nextID))
                  (set! (.-keywordCallsites ret)
                        (cast IPersistentVector (.deref KEYWORD_CALLSITES)))
                  (set! (.-protocolCallsites ret)
                        (cast IPersistentVector (.deref PROTOCOL_CALLSITES))))
                (finally (when (.isDeftype ret) (Var/popThreadBindings)) (Var/popThreadBindings)))
              (try
                (.compile ret (NewInstanceExpr/slashname superClass) inames false)
                (catch IOException e (throw (Util/sneakyThrow e))))
              (.getCompiledClass ret)
              ret)))))

    (method ^:static compileStub ^Class [^String superName ^NewInstanceExpr ret
                                         ^String/1 interfaceNames frm]
      (let [cw (arbace.lang.Compiler/classWriter)
            ^ClassVisitor cv cw]
        (.visit cv
                JVM_BYTECODE_VERSION
                (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
                (java-str COMPILE_STUB_PREFIX "/" (.-internalName ret))
                nil
                superName
                interfaceNames)
        (loop [s (RT/keys (.-closes ret))]
          (when (some? s)
            (let [lb (cast LocalBinding (.first s))
                  access (unchecked-add-int Opcodes/ACC_PUBLIC
                                            (cond
                                              (.isVolatile ret lb) Opcodes/ACC_VOLATILE
                                              (.isMutable ret lb) 0
                                              :else Opcodes/ACC_FINAL))]
              (if (some? (.getPrimitiveType lb))
                  (do
                    (.visitField cv
                                 access
                                 (.-name lb)
                                 (.getDescriptor (Type/getType (.getPrimitiveType lb)))
                                 nil
                                 nil)
                    (recur (.next s)))
                  (do
                    (.visitField cv access (.-name lb) (.getDescriptor OBJECT_TYPE) nil nil)
                    (recur (.next s)))))))
        (let [m (Method. "<init>" Type/VOID_TYPE (.ctorTypes ret))
              ^:mutable ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)]
          (.visitCode ctorgen)
          (.loadThis ctorgen)
          (.invokeConstructor ctorgen (Type/getObjectType superName) NewInstanceExpr/voidctor)
          (.returnValue ctorgen)
          (.endMethod ctorgen)
          (when (> (.-altCtorDrops ret) 0)
            (let [ctorTypes (.ctorTypes ret)
                  ^:mutable altCtorTypes (new Type/1
                                              (unchecked-subtract-int
                                                (alength ctorTypes)
                                                (.-altCtorDrops ret)))]
              (loop [^int i 0]
                (when (< i (alength altCtorTypes))
                  (aset altCtorTypes i (aget ctorTypes i))
                  (recur (unchecked-inc-int i))))
              (let [^:mutable alt (Method. "<init>" Type/VOID_TYPE altCtorTypes)]
                (set! ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC alt nil nil cv))
                (.visitCode ctorgen)
                (.loadThis ctorgen)
                (.loadArgs ctorgen)
                (.visitInsn ctorgen Opcodes/ACONST_NULL)
                (.visitInsn ctorgen Opcodes/ACONST_NULL)
                (.visitInsn ctorgen Opcodes/ICONST_0)
                (.visitInsn ctorgen Opcodes/ICONST_0)
                (.invokeConstructor ctorgen
                                    (Type/getObjectType
                                      (java-str COMPILE_STUB_PREFIX "/" (.-internalName ret)))
                                    (Method. "<init>" Type/VOID_TYPE ctorTypes))
                (.returnValue ctorgen)
                (.endMethod ctorgen)
                (set! altCtorTypes (new Type/1 (unchecked-subtract-int (alength ctorTypes) 2)))
                (loop [^int i 0]
                  (when (< i (alength altCtorTypes))
                    (aset altCtorTypes i (aget ctorTypes i))
                    (recur (unchecked-inc-int i))))
                (set! alt (Method. "<init>" Type/VOID_TYPE altCtorTypes))
                (set! ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC alt nil nil cv))
                (.visitCode ctorgen)
                (.loadThis ctorgen)
                (.loadArgs ctorgen)
                (.visitInsn ctorgen Opcodes/ICONST_0)
                (.visitInsn ctorgen Opcodes/ICONST_0)
                (.invokeConstructor ctorgen
                                    (Type/getObjectType
                                      (java-str COMPILE_STUB_PREFIX "/" (.-internalName ret)))
                                    (Method. "<init>" Type/VOID_TYPE ctorTypes))
                (.returnValue ctorgen)
                (.endMethod ctorgen))))
          (.visitEnd cv)
          (let [bytecode (.toByteArray cw)
                loader (cast DynamicClassLoader (.deref LOADER))]
            (.defineClass loader (java-str COMPILE_STUB_PREFIX "." (.-name ret)) bytecode frm)))))

    (method ^:static interfaceNames ^String/1 [^IPersistentVector interfaces]
      (let [icnt (.count interfaces)
            inames (when (> icnt 0) (new String/1 icnt))]
        (loop [^int i 0]
          (when (< i icnt)
            (aset inames i (NewInstanceExpr/slashname (cast Class (.nth interfaces i))))
            (recur (unchecked-inc-int i))))
        inames))

    (method ^:static slashname ^String [^Class c]
      (.replace (.getName c) \. \/))

    (method ^:protected emitStatics ^void [this ^ClassVisitor cv]
      (when (.isDeftype this)
        (let [meth (Method/getMethod "arbace.lang.IPersistentVector getBasis()")
              gen (GeneratorAdapter. (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC)
                                     meth
                                     nil
                                     nil
                                     cv)]
          (.emitValue this (.-hintedFields this) gen)
          (.returnValue gen)
          (.endMethod gen)
          (when (and (.isDeftype this) (> (.count (.-fields this)) (.count (.-hintedFields this))))
            (let [className (.replace (.-name this) \. \/)
                  ^:mutable ^int i 1
                  fieldCount (.count (.-hintedFields this))
                  mv (.visitMethod cv
                                   (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC)
                                   "create"
                                   (java-str "(Larbace/lang/IPersistentMap;)L" className ";")
                                   nil
                                   nil)]
              (.visitCode mv)
              (let [^:mutable s (RT/seq (.-hintedFields this))]
                (while (some? s)
                  (let [bName (.-name (cast Symbol (.first s)))
                        k (arbace.lang.Compiler/tagClass (arbace.lang.Compiler/tagOf (.first s)))]
                    (.visitVarInsn mv Opcodes/ALOAD 0)
                    (.visitLdcInsn mv bName)
                    (.visitMethodInsn mv
                                      Opcodes/INVOKESTATIC
                                      "arbace/lang/Keyword"
                                      "intern"
                                      "(Ljava/lang/String;)Larbace/lang/Keyword;")
                    (.visitInsn mv Opcodes/ACONST_NULL)
                    (.visitMethodInsn mv
                                      Opcodes/INVOKEINTERFACE
                                      "arbace/lang/IPersistentMap"
                                      "valAt"
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                    (when (.isPrimitive k)
                      (.visitTypeInsn mv
                                      Opcodes/CHECKCAST
                                      (.getInternalName
                                        (Type/getType (arbace.lang.Compiler/boxClass k)))))
                    (.visitVarInsn mv Opcodes/ASTORE i)
                    (.visitVarInsn mv Opcodes/ALOAD 0)
                    (.visitLdcInsn mv bName)
                    (.visitMethodInsn mv
                                      Opcodes/INVOKESTATIC
                                      "arbace/lang/Keyword"
                                      "intern"
                                      "(Ljava/lang/String;)Larbace/lang/Keyword;")
                    (.visitMethodInsn mv
                                      Opcodes/INVOKEINTERFACE
                                      "arbace/lang/IPersistentMap"
                                      "without"
                                      "(Ljava/lang/Object;)Larbace/lang/IPersistentMap;")
                    (.visitVarInsn mv Opcodes/ASTORE 0))
                  (set! s (.next s))
                  (set! i (unchecked-inc-int i))))
              (.visitTypeInsn mv Opcodes/NEW className)
              (.visitInsn mv Opcodes/DUP)
              (let [ctor (Method. "<init>" Type/VOID_TYPE (.ctorTypes this))]
                (when (> (.count (.-hintedFields this)) 0)
                  (set! i 1)
                  (while (<= i fieldCount)
                    (.visitVarInsn mv Opcodes/ALOAD i)
                    (let [k (arbace.lang.Compiler/tagClass
                              (arbace.lang.Compiler/tagOf
                                (.nth (.-hintedFields this) (unchecked-subtract-int i 1))))]
                      (when (.isPrimitive k)
                        (let [b (.getInternalName (Type/getType (arbace.lang.Compiler/boxClass k)))
                              p (.getDescriptor (Type/getType k))
                              n (.getName k)]
                          (.visitMethodInsn mv
                                            Opcodes/INVOKEVIRTUAL
                                            b
                                            (java-str n "Value")
                                            (java-str "()" p)))))
                    (set! i (unchecked-inc-int i))))
                (.visitInsn mv Opcodes/ACONST_NULL)
                (.visitVarInsn mv Opcodes/ALOAD 0)
                (.visitMethodInsn mv
                                  Opcodes/INVOKESTATIC
                                  "arbace/lang/RT"
                                  "seqOrElse"
                                  "(Ljava/lang/Object;)Ljava/lang/Object;")
                (.visitInsn mv Opcodes/ICONST_0)
                (.visitInsn mv Opcodes/ICONST_0)
                (.visitMethodInsn mv Opcodes/INVOKESPECIAL className "<init>" (.getDescriptor ctor))
                (.visitInsn mv Opcodes/ARETURN)
                (.visitMaxs mv (unchecked-add-int 4 fieldCount) (unchecked-add-int 1 fieldCount))
                (.visitEnd mv)))))))

    (method ^:protected emitMethods ^void [this ^ClassVisitor cv]
      (loop [s (RT/seq methods)]
        (when (some? s)
          (let [method (cast ObjMethod (.first s))] (.emit method this cv) (recur (.next s)))))
      (for-each [^{:tag (Map$Entry IPersistentVector (Set Class))} e (.entrySet covariants)]
        (let [m (cast java.lang.reflect.Method (.get mmap (.getKey e)))
              params (.getParameterTypes m)
              argTypes (new Type/1 (alength params))]
          (loop [^int i 0]
            (when (< i (alength params))
              (aset argTypes i (Type/getType (aget params i)))
              (recur (unchecked-inc-int i))))
          (let [target (Method. (.getName m) (Type/getType (.getReturnType m)) argTypes)]
            (for-each [^Class retType (cast Set (.getValue e))]
              (let [meth (Method. (.getName m) (Type/getType retType) argTypes)
                    gen (GeneratorAdapter. (unchecked-add-int Opcodes/ACC_PUBLIC Opcodes/ACC_BRIDGE)
                                           meth
                                           nil
                                           EXCEPTION_TYPES
                                           cv)]
                (.visitCode gen)
                (.loadThis gen)
                (.loadArgs gen)
                (.invokeInterface gen (Type/getType (.getDeclaringClass m)) target)
                (.returnValue gen)
                (.endMethod gen)))))))

    (method ^:public ^:static msig ^IPersistentVector [^java.lang.reflect.Method m]
      (^[Object/1] RT/vector (.getName m) (RT/seq (.getParameterTypes m)) (.getReturnType m)))

    (method ^:static considerMethod ^void [^java.lang.reflect.Method m ^Map mm]
      (let [mk (NewInstanceExpr/msig m)
            mods (.getModifiers m)]
        (when-not (or (or (or (.containsKey mm mk)
                              (not (or (Modifier/isPublic mods) (Modifier/isProtected mods))))
                          (Modifier/isStatic mods))
                      (Modifier/isFinal mods))
          (.put mm mk m))))

    (method ^:static gatherMethods ^void [^:mutable ^Class c ^Map mm]
      (while (some? c)
        (for-each [^java.lang.reflect.Method m (.getDeclaredMethods c)]
          (NewInstanceExpr/considerMethod m mm))
        (for-each [^java.lang.reflect.Method m (.getMethods c)]
          (NewInstanceExpr/considerMethod m mm))
        (set! c (.getSuperclass c))))

    (method ^:public ^:static gatherMethods ^Map/1 [^Class sc ^:mutable ^ISeq interfaces]
      (let [^Map allm (HashMap.)]
        (NewInstanceExpr/gatherMethods sc allm)
        (while (some? interfaces)
          (NewInstanceExpr/gatherMethods (cast Class (.first interfaces)) allm)
          (set! interfaces (.next interfaces)))
        (let [^{:tag (Map IPersistentVector java.lang.reflect.Method)} mm (HashMap.)
              ^{:tag (Map IPersistentVector (Set Class))} covariants (HashMap.)]
          (for-each [o (.entrySet allm)]
            (let [e (cast Map$Entry o)
                  ^:mutable mk (cast IPersistentVector (.getKey e))]
              (set! mk (cast IPersistentVector (.pop mk)))
              (let [m (cast java.lang.reflect.Method (.getValue e))]
                (if (.containsKey mm mk)
                    (let [^:mutable ^{:tag (Set Class)} cvs (cast Set (.get covariants mk))]
                      (when (nil? cvs) (set! cvs (HashSet.)) (.put covariants mk cvs))
                      (let [om (cast java.lang.reflect.Method (.get mm mk))]
                        (if (.isAssignableFrom (.getReturnType om) (.getReturnType m))
                            (do (.add cvs (.getReturnType om)) (.put mm mk m))
                            (.add cvs (.getReturnType m)))))
                    (.put mm mk m)))))
          (new Map/1 [mm covariants])))))

  (defclass ^:public ^:static NewInstanceMethod
    :extends ObjMethod

    (field ^String name)

    (field ^Type/1 argTypes)

    (field ^Type retType)

    (field ^Class retClass)

    (field ^Class/1 exclasses)

    (field ^:static ^Symbol dummyThis (Symbol/intern nil "dummy_this_dlskjsdfower"))

    (field ^:private ^IPersistentVector parms)

    (constructor ^:public [this ^ObjExpr objx ^ObjMethod parent]
      (super. objx parent))

    (method numParams ^int [this] (.count (.-argLocals this)))

    (method getMethodName ^String [this] name)

    (method getReturnType ^Type [this] retType)

    (method getArgTypes ^Type/1 [this] argTypes)

    (method ^:public ^:static msig ^IPersistentVector [^String name ^Class/1 paramTypes]
      (^[Object/1] RT/vector name (RT/seq paramTypes)))

    (method ^:static parse ^NewInstanceMethod [^ObjExpr objx ^ISeq form ^Symbol thistag
                                               ^Map overrideables]
      (let [method (NewInstanceMethod. objx (cast ObjMethod (.deref METHOD)))
            dotname (cast Symbol (RT/first form))
            name (cast Symbol
                       (.withMeta (Symbol/intern nil (arbace.lang.Compiler/munge (.-name dotname)))
                                  (RT/meta dotname)))
            ^:mutable parms (cast IPersistentVector (RT/second form))]
        (when (== (.count parms) 0)
          (throw (IllegalArgumentException.
                   (java-str "Must supply at least one argument for 'this' in: " dotname))))
        (let [thisName (cast Symbol (.nth parms 0))]
          (set! parms (RT/subvec parms 1 (.count parms)))
          (let [body (RT/next (RT/next form))]
            (try
              (set! (.-line method) (arbace.lang.Compiler/lineDeref))
              (set! (.-column method) (arbace.lang.Compiler/columnDeref))
              (let [pnode (PathNode. PATHTYPE/PATH nil)]
                (set! (.-clearRoot method) pnode)
                (Var/pushThreadBindings
                  (^[Object/1] RT/mapUniqueKeys METHOD
                                                method
                                                LOCAL_ENV
                                                (.deref LOCAL_ENV)
                                                LOOP_LOCALS
                                                nil
                                                NEXT_LOCAL_NUM
                                                (Integer/valueOf 0)
                                                CLEAR_PATH
                                                pnode
                                                CLEAR_ROOT
                                                pnode
                                                CLEAR_SITES
                                                PersistentHashMap/EMPTY
                                                METHOD_RETURN_CONTEXT
                                                RT/T))
                (if (some? thisName)
                    (arbace.lang.Compiler/registerLocal
                      (if (nil? thisName) dummyThis thisName)
                      thistag
                      nil
                      false)
                    (arbace.lang.Compiler/getAndIncLocalNum))
                (let [^:mutable argLocals PersistentVector/EMPTY]
                  (set! (.-retClass method)
                        (arbace.lang.Compiler/tagClass (arbace.lang.Compiler/tagOf name)))
                  (set! (.-argTypes method) (new Type/1 (.count parms)))
                  (let [^:mutable hinted (some? (arbace.lang.Compiler/tagOf name))
                        ^:mutable pclasses (new Class/1 (.count parms))
                        psyms (new Symbol/1 (.count parms))]
                    (loop [^int i 0]
                      (when (< i (.count parms))
                        (when-not (instance? Symbol (.nth parms i))
                          (throw (IllegalArgumentException. "params must be Symbols")))
                        (let [^:mutable p (cast Symbol (.nth parms i))
                              ^Object tag (arbace.lang.Compiler/tagOf p)]
                          (when (some? tag) (set! hinted true))
                          (when (some? (.getNamespace p)) (set! p (Symbol/intern (.-name p))))
                          (let [pclass (arbace.lang.Compiler/tagClass tag)]
                            (aset pclasses i pclass)
                            (aset psyms i p)
                            (recur (unchecked-inc-int i))))))
                    (let [matches (NewInstanceMethod/findMethodsWithNameAndArity
                                    (.-name name)
                                    (.count parms)
                                    overrideables)
                          ^Object mk (NewInstanceMethod/msig (.-name name) pclasses)
                          ^:mutable ^java.lang.reflect.Method m nil]
                      (if (> (.size matches) 0)
                          (cond
                            (> (.size matches) 1)
                              (do
                                (when-not hinted
                                  (throw
                                    (IllegalArgumentException.
                                      (java-str "Must hint overloaded method: " (.-name name)))))
                                (set! m (cast java.lang.reflect.Method (.get matches mk)))
                                (when (nil? m)
                                  (throw (IllegalArgumentException.
                                           (java-str
                                             "Can't find matching overloaded method: "
                                             (.-name name)))))
                                (when-not (identical? (.getReturnType m) (.-retClass method))
                                  (throw (IllegalArgumentException.
                                           (java-str
                                             "Mismatched return type: "
                                             (.-name name)
                                             ", expected: "
                                             (.getName (.getReturnType m))
                                             ", had: "
                                             (.getName (.-retClass method)))))))
                            hinted
                              (do
                                (set! m (cast java.lang.reflect.Method (.get matches mk)))
                                (when (nil? m)
                                  (throw (IllegalArgumentException.
                                           (java-str
                                             "Can't find matching method: "
                                             (.-name name)
                                             ", leave off hints for auto match."))))
                                (when-not (identical? (.getReturnType m) (.-retClass method))
                                  (throw (IllegalArgumentException.
                                           (java-str
                                             "Mismatched return type: "
                                             (.-name name)
                                             ", expected: "
                                             (.getName (.getReturnType m))
                                             ", had: "
                                             (.getName (.-retClass method)))))))
                            :else
                              (do
                                (set! m
                                      (cast java.lang.reflect.Method
                                            (.next (.iterator (.values matches)))))
                                (set! (.-retClass method) (.getReturnType m))
                                (set! pclasses (.getParameterTypes m))))
                          (throw (IllegalArgumentException.
                                   (java-str "Can't define method not in interfaces: "
                                             (.-name name)))))
                      (set! (.-retType method) (Type/getType (.-retClass method)))
                      (set! (.-exclasses method) (.getExceptionTypes m))
                      (loop [^int i 0]
                        (when (< i (.count parms))
                          (let [lb (arbace.lang.Compiler/registerLocal
                                     (aget psyms i)
                                     nil
                                     (MethodParamExpr. (aget pclasses i))
                                     true)]
                            (set! argLocals (.assocN argLocals i lb))
                            (aset (.-argTypes method) i (Type/getType (aget pclasses i)))
                            (recur (unchecked-inc-int i)))))
                      (loop [^int i 0]
                        (if (< i (.count parms))
                            (if (or (identical? (aget pclasses i) Long/TYPE)
                                    (identical? (aget pclasses i) Double/TYPE))
                                (do
                                  (arbace.lang.Compiler/getAndIncLocalNum)
                                  (recur (unchecked-inc-int i)))
                                (recur (unchecked-inc-int i)))
                            nil))
                      (.set LOOP_LOCALS argLocals)
                      (set! (.-name method) (.-name name))
                      (set! (.-methodMeta method) (RT/meta name))
                      (set! (.-parms method) parms)
                      (set! (.-argLocals method) argLocals)
                      (set! (.-body method) (.parse (Compiler$BodyExpr$Parser.) C/RETURN body))
                      method))))
              (finally (Var/popThreadBindings)))))))

    (method ^:private ^:static findMethodsWithNameAndArity ^Map [^String name ^int arity ^Map mm]
      (let [^Map ret (HashMap.)]
        (for-each [o (.entrySet mm)]
          (let [e (cast Map$Entry o)
                m (cast java.lang.reflect.Method (.getValue e))]
            (when (and (.equals name (.getName m)) (== (alength (.getParameterTypes m)) arity))
              (.put ret (.getKey e) (.getValue e)))))
        ret))

    (method ^:private ^:static findMethodsWithName ^Map [^String name ^Map mm]
      (let [^Map ret (HashMap.)]
        (for-each [o (.entrySet mm)]
          (let [e (cast Map$Entry o)
                m (cast java.lang.reflect.Method (.getValue e))]
            (when (.equals name (.getName m)) (.put ret (.getKey e) (.getValue e)))))
        ret))

    (method ^:public emit ^void [this ^ObjExpr obj ^ClassVisitor cv]
      (let [m (Method. (.getMethodName this) (.getReturnType this) (.getArgTypes this))
            ^:mutable ^Type/1 extypes nil]
        (when (> (alength exclasses) 0)
          (set! extypes (new Type/1 (alength exclasses)))
          (loop [^int i 0]
            (when (< i (alength exclasses))
              (aset extypes i (Type/getType (aget exclasses i)))
              (recur (unchecked-inc-int i)))))
        (let [gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil extypes cv)]
          (arbace.lang.Compiler/addAnnotation gen (.-methodMeta this))
          (loop [^int i 0]
            (when (< i (.count parms))
              (let [meta (RT/meta (.nth parms i))]
                (arbace.lang.Compiler/addParameterAnnotation gen meta i)
                (recur (unchecked-inc-int i)))))
          (.visitCode gen)
          (let [loopLabel (.mark gen)]
            (.visitLineNumber gen (.-line this) loopLabel)
            (try
              (Var/pushThreadBindings (^[Object/1] RT/map LOOP_LABEL loopLabel METHOD this))
              (NewInstanceMethod/emitBody (.-objx this) gen retClass (.-body this))
              (let [end (.mark gen)]
                (.visitLocalVariable gen
                                     "this"
                                     (.getDescriptor (.-objtype obj))
                                     nil
                                     loopLabel
                                     end
                                     0)
                (loop [lbs (.seq (.-argLocals this))]
                  (when (some? lbs)
                    (let [lb (cast LocalBinding (.first lbs))]
                      (.visitLocalVariable gen
                                           (.-name lb)
                                           (.getDescriptor
                                             (aget argTypes (unchecked-subtract-int (.-idx lb) 1)))
                                           nil
                                           loopLabel
                                           end
                                           (.-idx lb))
                      (recur (.next lbs))))))
              (finally (Var/popThreadBindings)))
            (.returnValue gen)
            (.endMethod gen))))))

  (method ^:static inty ^boolean [^Class c]
    (or (or (or (identical? c Integer/TYPE) (identical? c Short/TYPE)) (identical? c Byte/TYPE))
        (identical? c Character/TYPE)))

  (method ^:static retType ^Class [^Class tc ^Class ret]
    (cond
      (nil? tc) ret
      (nil? ret) tc
      :else
        (do
          (when (and (.isPrimitive ret) (.isPrimitive tc))
            (when (or (and (arbace.lang.Compiler/inty ret) (arbace.lang.Compiler/inty tc))
                      (identical? ret tc))
              (return tc))
            (throw (UnsupportedOperationException.
                     (java-str "Cannot coerce " ret " to " tc ", use a cast instead"))))
          tc)))

  (method ^:public ^:static primClass ^Class [^Symbol sym]
    (when (some? sym)
      (let [^:mutable ^Class c nil]
        (cond
          (.equals (.-name sym) "int") (set! c Integer/TYPE)
          (.equals (.-name sym) "long") (set! c Long/TYPE)
          (.equals (.-name sym) "float") (set! c Float/TYPE)
          (.equals (.-name sym) "double") (set! c Double/TYPE)
          (.equals (.-name sym) "char") (set! c Character/TYPE)
          (.equals (.-name sym) "short") (set! c Short/TYPE)
          (.equals (.-name sym) "byte") (set! c Byte/TYPE)
          (.equals (.-name sym) "boolean") (set! c Boolean/TYPE)
          (.equals (.-name sym) "void") (set! c Void/TYPE))
        c)))

  (method ^:static tagClass ^Class [tag]
    (if (nil? tag)
        Object
        (let [^:mutable ^Class c nil]
          (when (instance? Symbol tag) (set! c (arbace.lang.Compiler/primClass (cast Symbol tag))))
          (when (nil? c) (set! c (HostExpr/tagToClass tag)))
          c)))

  (method ^:static primClass ^Class [^Class c]
    (if (.isPrimitive c) c Object))

  (method ^:static boxClass ^Class [^Class p]
    (if (not (.isPrimitive p))
        p
        (let [^:mutable ^Class c nil]
          (cond
            (identical? p Integer/TYPE) (set! c Integer)
            (identical? p Long/TYPE) (set! c Long)
            (identical? p Float/TYPE) (set! c Float)
            (identical? p Double/TYPE) (set! c Double)
            (identical? p Character/TYPE) (set! c Character)
            (identical? p Short/TYPE) (set! c Short)
            (identical? p Byte/TYPE) (set! c Byte)
            (identical? p Boolean/TYPE) (set! c Boolean))
          c)))

  (defclass ^:public ^:static MethodParamExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:final ^Class c)

    (constructor ^:public [this ^Class c] (set! (.-c this) c))

    (method ^:public eval [this]
      (throw (Util/runtimeException "Can't eval")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (throw (Util/runtimeException "Can't emit")))

    (method ^:public hasJavaClass ^boolean [this] (some? c))

    (method ^:public getJavaClass ^Class [this] c)

    (method ^:public canEmitPrimitive ^boolean [this] (Util/isPrimitive c))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (throw (Util/runtimeException "Can't emit"))))

  (defclass ^:public ^:static CaseExpr
    :implements [Expr MaybePrimitiveExpr]

    (field ^:public ^:final ^LocalBindingExpr expr)

    (field ^:public ^:final ^int shift)

    (field ^:public ^:final ^int mask)

    (field ^:public ^:final ^int low)

    (field ^:public ^:final ^int high)

    (field ^:public ^:final ^Expr defaultExpr)

    (field ^:public ^:final ^{:tag (SortedMap Integer Expr)} tests)

    (field ^:public ^:final ^{:tag (HashMap Integer Expr)} thens)

    (field ^:public ^:final ^Keyword switchType)

    (field ^:public ^:final ^Keyword testType)

    (field ^:public ^:final ^{:tag (Set Integer)} skipCheck)

    (field ^:public ^:final ^Class returnType)

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:static ^:final ^Type NUMBER_TYPE (Type/getType Number))

    (field ^:static ^:final ^Method intValueMethod (Method/getMethod "int intValue()"))

    (field ^:static ^:final ^Method hashMethod (Method/getMethod "int hash(Object)"))

    (field ^:static ^:final ^Method hashCodeMethod (Method/getMethod "int hashCode()"))

    (field ^:static ^:final ^Method equivMethod (Method/getMethod "boolean equiv(Object, Object)"))

    (field ^:static ^:final ^Keyword compactKey (Keyword/intern nil "compact"))

    (field ^:static ^:final ^Keyword sparseKey (Keyword/intern nil "sparse"))

    (field ^:static ^:final ^Keyword hashIdentityKey (Keyword/intern nil "hash-identity"))

    (field ^:static ^:final ^Keyword hashEquivKey (Keyword/intern nil "hash-equiv"))

    (field ^:static ^:final ^Keyword intKey (Keyword/intern nil "int"))

    (constructor ^:public [this ^int line ^int column ^LocalBindingExpr expr ^int shift ^int mask
                           ^int low ^int high ^Expr defaultExpr
                           ^{:tag (SortedMap Integer Expr)} tests
                           ^{:tag (HashMap Integer Expr)} thens ^Keyword switchType
                           ^Keyword testType ^{:tag (Set Integer)} skipCheck]
      (set! (.-expr this) expr)
      (set! (.-shift this) shift)
      (set! (.-mask this) mask)
      (set! (.-low this) low)
      (set! (.-high this) high)
      (set! (.-defaultExpr this) defaultExpr)
      (set! (.-tests this) tests)
      (set! (.-thens this) thens)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (when (and (not (identical? switchType compactKey)) (not (identical? switchType sparseKey)))
        (throw (IllegalArgumentException. (java-str "Unexpected switch type: " switchType))))
      (set! (.-switchType this) switchType)
      (when (and (and (not (identical? testType intKey)) (not (identical? testType hashEquivKey)))
                 (not (identical? testType hashIdentityKey)))
        (throw (IllegalArgumentException. (java-str "Unexpected test type: " switchType))))
      (set! (.-testType this) testType)
      (set! (.-skipCheck this) skipCheck)
      (let [^{:tag (Collection Expr)} returns (ArrayList. (.values thens))]
        (.add returns defaultExpr)
        (set! (.-returnType this) (arbace.lang.Compiler/maybeJavaClass returns))
        (when (and (> (RT/count skipCheck) 0) (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
          (.format
            (RT/errPrintWriter)
            "Performance warning, %s:%d:%d - hash collision of some case test constants; if selected, those entries will be tested sequentially.\n"
            (new Object/1 [(.deref SOURCE_PATH) line column])))))

    (method ^:public hasJavaClass ^boolean [this] (some? returnType))

    (method ^:public canEmitPrimitive ^boolean [this]
      (Util/isPrimitive returnType))

    (method ^:public getJavaClass ^Class [this] returnType)

    (method ^:public eval [this]
      (throw (UnsupportedOperationException. "Can't eval case")))

    (method ^:public emit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen false))

    (method ^:public emitUnboxed ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen]
      (.doEmit this context objx gen true))

    (method ^:public doEmit ^void [this ^C context ^ObjExpr objx ^GeneratorAdapter gen
                                   ^boolean emitUnboxed]
      (let [defaultLabel (.newLabel gen)
            endLabel (.newLabel gen)
            ^{:tag (SortedMap Integer Label)} labels (TreeMap.)]
        (for-each [^Integer i (.keySet tests)] (.put labels i (.newLabel gen)))
        (.visitLineNumber gen line (.mark gen))
        (let [primExprClass (arbace.lang.Compiler/maybePrimitiveType expr)
              primExprType (when (some? primExprClass) (Type/getType primExprClass))]
          (if (identical? testType intKey)
              (.emitExprForInts this objx gen primExprType defaultLabel)
              (.emitExprForHashes this objx gen))
          (if (identical? switchType sparseKey)
              (let [^:mutable la (new Label/1 (.size labels))]
                (set! la (cast Label/1 (.toArray (.values labels) la)))
                (let [ints (Numbers/int_array (.keySet tests))]
                  (.visitLookupSwitchInsn gen defaultLabel ints la)))
              (let [la (new Label/1 (unchecked-add-int (unchecked-subtract-int high low) 1))]
                (loop [^int i low]
                  (when (<= i high)
                    (aset la
                          (unchecked-subtract-int i low)
                          (if (.containsKey labels i) (cast Label (.get labels i)) defaultLabel))
                    (recur (unchecked-inc-int i))))
                (.visitTableSwitchInsn gen low high defaultLabel la)))
          (for-each [^Integer i (.keySet labels)]
            (.mark gen (cast Label (.get labels i)))
            (cond
              (identical? testType intKey)
                (.emitThenForInts this
                                  objx
                                  gen
                                  primExprType
                                  (cast Expr (.get tests i))
                                  (cast Expr (.get thens i))
                                  defaultLabel
                                  emitUnboxed)
              (identical? (RT/contains skipCheck i) RT/T)
                (CaseExpr/emitExpr objx gen (cast Expr (.get thens i)) emitUnboxed)
              :else
                (.emitThenForHashes this
                                    objx
                                    gen
                                    (cast Expr (.get tests i))
                                    (cast Expr (.get thens i))
                                    defaultLabel
                                    emitUnboxed))
            (.goTo gen endLabel))
          (.mark gen defaultLabel)
          (CaseExpr/emitExpr objx gen defaultExpr emitUnboxed)
          (.mark gen endLabel)
          (when (identical? context C/STATEMENT) (.pop gen)))))

    (method ^:private isShiftMasked ^boolean [this] (not (== mask 0)))

    (method ^:private emitShiftMask ^void [this ^GeneratorAdapter gen]
      (when (.isShiftMasked this)
        (.push gen shift)
        (.visitInsn gen Opcodes/ISHR)
        (.push gen mask)
        (.visitInsn gen Opcodes/IAND)))

    (method ^:private emitExprForInts ^void [this ^ObjExpr objx ^GeneratorAdapter gen ^Type exprType
                                             ^Label defaultLabel]
      (cond
        (nil? exprType)
          (do
            (when (RT/booleanCast (.deref RT/WARN_ON_REFLECTION))
              (.format
                (RT/errPrintWriter)
                "Performance warning, %s:%d:%d - case has int tests, but tested expression is not primitive.\n"
                (new Object/1 [(.deref SOURCE_PATH) line column])))
            (.emit expr C/EXPRESSION objx gen)
            (.instanceOf gen NUMBER_TYPE)
            (.ifZCmp gen GeneratorAdapter/EQ defaultLabel)
            (.emit expr C/EXPRESSION objx gen)
            (.checkCast gen NUMBER_TYPE)
            (.invokeVirtual gen NUMBER_TYPE intValueMethod)
            (.emitShiftMask this gen))
        (or (or (or (identical? exprType Type/LONG_TYPE) (identical? exprType Type/INT_TYPE))
                (identical? exprType Type/SHORT_TYPE))
            (identical? exprType Type/BYTE_TYPE))
          (do
            (.emitUnboxed expr C/EXPRESSION objx gen)
            (.cast gen exprType Type/INT_TYPE)
            (.emitShiftMask this gen))
        :else (.goTo gen defaultLabel)))

    (method ^:private emitThenForInts ^void [this ^ObjExpr objx ^GeneratorAdapter gen ^Type exprType
                                             ^Expr test ^Expr then ^Label defaultLabel
                                             ^boolean emitUnboxed]
      (cond
        (nil? exprType)
          (do
            (.emit expr C/EXPRESSION objx gen)
            (.emit test C/EXPRESSION objx gen)
            (.invokeStatic gen UTIL_TYPE equivMethod)
            (.ifZCmp gen GeneratorAdapter/EQ defaultLabel)
            (CaseExpr/emitExpr objx gen then emitUnboxed))
        (identical? exprType Type/LONG_TYPE)
          (do
            (.emitUnboxed (cast NumberExpr test) C/EXPRESSION objx gen)
            (.emitUnboxed expr C/EXPRESSION objx gen)
            (.ifCmp gen Type/LONG_TYPE GeneratorAdapter/NE defaultLabel)
            (CaseExpr/emitExpr objx gen then emitUnboxed))
        (or (or (identical? exprType Type/INT_TYPE) (identical? exprType Type/SHORT_TYPE))
            (identical? exprType Type/BYTE_TYPE))
          (do
            (when (.isShiftMasked this)
              (.emitUnboxed (cast NumberExpr test) C/EXPRESSION objx gen)
              (.emitUnboxed expr C/EXPRESSION objx gen)
              (.cast gen exprType Type/LONG_TYPE)
              (.ifCmp gen Type/LONG_TYPE GeneratorAdapter/NE defaultLabel))
            (CaseExpr/emitExpr objx gen then emitUnboxed))
        :else (.goTo gen defaultLabel)))

    (method ^:private emitExprForHashes ^void [this ^ObjExpr objx ^GeneratorAdapter gen]
      (.emit expr C/EXPRESSION objx gen)
      (.invokeStatic gen UTIL_TYPE hashMethod)
      (.emitShiftMask this gen))

    (method ^:private emitThenForHashes ^void [this ^ObjExpr objx ^GeneratorAdapter gen ^Expr test
                                               ^Expr then ^Label defaultLabel ^boolean emitUnboxed]
      (.emit expr C/EXPRESSION objx gen)
      (.emit test C/EXPRESSION objx gen)
      (if (identical? testType hashIdentityKey)
          (.visitJumpInsn gen Opcodes/IF_ACMPNE defaultLabel)
          (do
            (.invokeStatic gen UTIL_TYPE equivMethod)
            (.ifZCmp gen GeneratorAdapter/EQ defaultLabel)))
      (CaseExpr/emitExpr objx gen then emitUnboxed))

    (method ^:private ^:static emitExpr ^void [^ObjExpr objx ^GeneratorAdapter gen ^Expr expr
                                               ^boolean emitUnboxed]
      (if (and emitUnboxed (instance? MaybePrimitiveExpr expr))
          (.emitUnboxed (cast MaybePrimitiveExpr expr) C/EXPRESSION objx gen)
          (.emit expr C/EXPRESSION objx gen)))

    (defclass ^:static Parser
      :implements [IParser]

      (method ^:public parse ^Expr [this ^C context frm]
        (let [form (cast ISeq frm)]
          (if (identical? context C/EVAL)
              (arbace.lang.Compiler/analyze context
                                            (RT/list (RT/list FNONCE PersistentVector/EMPTY form)))
              (let [args (LazilyPersistentVector/create (.next form))
                    exprForm (.nth args 0)
                    shift (.intValue (cast Number (.nth args 1)))
                    mask (.intValue (cast Number (.nth args 2)))
                    defaultForm (.nth args 3)
                    caseMap (cast Map (.nth args 4))
                    switchType (cast Keyword (.nth args 5))
                    testType (cast Keyword (.nth args 6))
                    skipCheck (when-not (< (RT/count args) 8) (cast Set (.nth args 7)))
                    keys (RT/keys caseMap)
                    low (.intValue (cast Number (RT/first keys)))
                    high (.intValue (cast Number
                                          (RT/nth keys (unchecked-subtract-int (RT/count keys) 1))))
                    testexpr (cast LocalBindingExpr
                                   (arbace.lang.Compiler/analyze C/EXPRESSION exprForm))]
                (set! (.-shouldClear testexpr) false)
                (let [^{:tag (SortedMap Integer Expr)} tests (TreeMap.)
                      ^{:tag (HashMap Integer Expr)} thens (HashMap.)
                      branch (PathNode. PATHTYPE/BRANCH (cast PathNode (.get CLEAR_PATH)))]
                  (for-each [o (.entrySet caseMap)]
                    (let [e (cast Map$Entry o)
                          ^Integer minhash (.intValue (cast Number (.getKey e)))
                          pair (.getValue e)
                          testExpr (if (identical? testType intKey)
                                       (NumberExpr/parse (.intValue (cast Number (RT/first pair))))
                                       ^Expr (ConstantExpr. (RT/first pair)))]
                      (.put tests minhash testExpr)
                      (let [^:mutable ^Expr thenExpr nil]
                        (try
                          (Var/pushThreadBindings
                            (^[Object/1] RT/map CLEAR_PATH (PathNode. PATHTYPE/PATH branch)))
                          (set! thenExpr (arbace.lang.Compiler/analyze context (RT/second pair)))
                          (finally (Var/popThreadBindings)))
                        (.put thens minhash thenExpr))))
                  (let [^:mutable ^Expr defaultExpr nil]
                    (try
                      (Var/pushThreadBindings
                        (^[Object/1] RT/map CLEAR_PATH (PathNode. PATHTYPE/PATH branch)))
                      (set! defaultExpr (arbace.lang.Compiler/analyze context (.nth args 3)))
                      (finally (Var/popThreadBindings)))
                    (let [line (.intValue (cast Number (.deref LINE)))
                          column (.intValue (cast Number (.deref COLUMN)))]
                      (CaseExpr. line
                                 column
                                 testexpr
                                 shift
                                 mask
                                 low
                                 high
                                 defaultExpr
                                 tests
                                 thens
                                 switchType
                                 testType
                                 skipCheck))))))))))

  (method ^:static emptyVarCallSites ^IPersistentCollection []
    PersistentHashSet/EMPTY)

  (method ^:public ^:static classWriter ^ClassWriter []
    (let [^ClassWriter cw (anon ClassWriter [(unchecked-add-int
                                               ClassWriter/COMPUTE_MAXS
                                               ClassWriter/COMPUTE_FRAMES)]
                            (method ^:protected getCommonSuperClass ^String [this
                                                                             ^:final ^String type1
                                                                             ^:final ^String type2]
                              "java/lang/Object"))]
      (.setComputeLimits cw Integer/MAX_VALUE Long/MAX_VALUE)
      cw)))
