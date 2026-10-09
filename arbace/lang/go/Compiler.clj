;; Go-build variant of arbace.lang.Compiler (C2G-SPEC §4.6, §10.1): read by c2g only. The Go
;; build has no bytecode back end (ASM is cut, D6) and no system properties for compiler
;; options. The analyzer (reader, macroexpansion, resolution, parse into Expr trees) is
;; translated unchanged; what loads or writes classes is replaced here, and the evaluator over
;; the Expr tree (doc/go/EVAL-PLAN.md) takes the place of class generation: a fn* evaluates to
;; an EvalFn, whose invocation walks its FnMethod's body with Eval.
;;
;; ASM is erased (C2G-NOTES.md, proposed amendment B2): arbace.asm is outside the closed world,
;; and every value of one of its types (the back end's Type and Method constants, an ObjExpr's
;; objtype, a FnMethod's argtypes) is nil in Go, its computation skipped. The emit methods are
;; not reached once ObjExpr.compile is replaced, so they are not translated.
(in-ns 'arbace.lang)

(c2g/erase "arbace/asm/")

(c2g/variant Compiler
  ;; ARG_TYPES: ASM's types of the back end's method descriptors
  (c2g/cut (static-initializer 0))

  ;; *compiler-options* from the arbace.compiler.* system properties: none
  ^{:c2g/nth 1}
  (static-initializer
    (set! COMPILER_OPTIONS
          (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                                   (Symbol/intern "*compiler-options*")
                                   nil))))

  ;; the class file version the back end writes: Java 26's
  (field ^:public ^:static ^:final ^int JVM_BYTECODE_VERSION 70)

  ;; AOT compilation writes class files: there are none in the Go build (C2G-SPEC §10.3)
  (method ^:public ^:static compile :throws [IOException] [^Reader rdr ^String sourcePath
                                                            ^String sourceName]
    (throw (UnsupportedOperationException. "compile: the Go build writes no class files")))

  (method ^:public ^:static writeClassFile :throws [IOException] ^void [^String internalName
                                                                        ^byte/1 bytecode]
    (throw (UnsupportedOperationException. "compile: the Go build writes no class files")))

  ;; ---------------------------------------------------------------------------------------
  ;; The evaluator's interface (doc/go/EVAL-PLAN.md): a frame per fn invocation, the fn class,
  ;; and the walk over the Expr tree. Node kinds without a case here are evaluated by their own
  ;; Expr.eval, which is right for the nodes without locals (constants, vars, keywords, the
  ;; top-level forms Compiler.eval evaluates); the rest are B1a step 5.

  (c2g/add
    (defclass ^:public ^:static Frame
      ;; the locals of one invocation of a FnMethod, by LocalBinding.idx (slot 0 is the fn
      ;; itself unless the fn is direct)
      (field ^:public ^:final ^Object/1 slots)
      ;; the fn whose method runs (its closed-over values), nil at the top level
      (field ^:public ^:final ^EvalFn fn)
      (field ^:public ^:final ^ObjMethod method)

      (constructor ^:public [this ^int n ^EvalFn fn ^ObjMethod method]
        (set! (.-slots this) (new Object/1 n))
        (set! (.-fn this) fn)
        (set! (.-method this) method))))

  (c2g/add
    (defclass ^:public ^:static EvalFn
      :extends RestFn
      ;; a fn* value: the analyzed FnExpr and the values of the locals it closes over, in the
      ;; order of (keys (.-closes fe)). One class serves every arity, fixed or variadic: it is a
      ;; RestFn of required arity 0, so every invoke arrives at doInvoke with its arguments as
      ;; a seq (EVAL-PLAN.md, Q3: per-arity invoke methods are step 7's)
      (field ^:public ^:final ^FnExpr fe)
      (field ^:public ^:final ^LocalBinding/1 closedBindings)
      (field ^:public ^:final ^Object/1 closed)

      (constructor ^:public [this ^FnExpr fe ^Frame f]
        (set! (.-fe this) fe)
        (let [ks (RT/keys (.-closes fe))
              n (RT/count ks)
              bs (new LocalBinding/1 n)
              vs (new Object/1 n)]
          (loop [s ks ^int i 0]
            (when (some? s)
              (let [lb (cast LocalBinding (.first s))]
                (aset bs i lb)
                (aset vs i (Evaluator/local lb f))
                (recur (.next s) (unchecked-inc-int i)))))
          (set! (.-closedBindings this) bs)
          (set! (.-closed this) vs)))

      (method ^:public getRequiredArity ^int [this] 0)

      (method ^:protected doInvoke [this args]
        (Evaluator/invokeFn this (RT/seqToArray (RT/seq args))))))

  (c2g/add
    (defclass ^:public ^:static Evaluator
      ;; what RecurExpr returns to its loop or fn method
      (field ^:static ^:final RECUR (Object.))

      ;; a local's value: from the frame's slots if the running method declares it, else
      ;; from the fn's closed-over values
      (method ^:public ^:static local [^LocalBinding lb ^Frame f]
        (when (nil? f) (throw (UnsupportedOperationException. "Can't eval locals")))
        (if (or (nil? (.-fn f)) (.containsKey (.-locals (.-method f)) lb))
            (aget (.-slots f) (.-idx lb))
            (let [bs (.-closedBindings (.-fn f))]
              (loop [^int i 0]
                (if (< i (alength bs))
                    (if (identical? (aget bs i) lb)
                        (aget (.-closed (.-fn f)) i)
                        (recur (unchecked-inc-int i)))
                    (throw (IllegalStateException.
                             (java-str "evaluator: local " (.-sym lb) " not found"))))))))

      (method ^:static args ^Object/1 [^IPersistentVector es ^Frame f]
        (let [vs (new Object/1 (.count es))]
          (loop [^int i 0]
            (when (< i (.count es))
              (aset vs i (Evaluator/eval (cast Expr (.nth es i)) f))
              (recur (unchecked-inc-int i))))
          vs))

      (method ^:static execution ^CompilerException [^String source ^int line ^int column
                                                       ^Throwable e]
        (if (instance? CompilerException e)
            (cast CompilerException e)
            (CompilerException. source line column nil CompilerException/PHASE_EXECUTION e)))

      ;; the value of e in frame f (nil: no frame, a top-level form)
      (method ^:public ^:static eval [^Expr e ^Frame f]
        (cond
          (instance? LocalBindingExpr e) (Evaluator/local (.-b (cast LocalBindingExpr e)) f)

          (instance? BodyExpr e)
            (let [es (.-exprs (cast BodyExpr e))
                  n (.count es)]
              (loop [^int i 0 ret nil]
                (if (< i n)
                    (recur (unchecked-inc-int i) (Evaluator/eval (cast Expr (.nth es i)) f))
                    ret)))

          (instance? IfExpr e)
            (let [ie (cast IfExpr e)
                  t (Evaluator/eval (.-testExpr ie) f)]
              (if (and (some? t) (not (identical? t Boolean/FALSE)))
                  (Evaluator/eval (.-thenExpr ie) f)
                  (Evaluator/eval (.-elseExpr ie) f)))

          (instance? LetExpr e)
            (let [le (cast LetExpr e)
                  bis (.-bindingInits le)]
              (when (nil? f) (throw (UnsupportedOperationException. "Can't eval let/loop")))
              (loop [^int i 0]
                (when (< i (.count bis))
                  (let [bi (cast BindingInit (.nth bis i))]
                    (aset (.-slots f) (.-idx (.-binding bi)) (Evaluator/eval (.-init bi) f))
                    (recur (unchecked-inc-int i)))))
              (if (.-isLoop le)
                  (loop []
                    (let [r (Evaluator/eval (.-body le) f)]
                      (if (identical? r RECUR) (recur) r)))
                  (Evaluator/eval (.-body le) f)))

          (instance? RecurExpr e)
            (let [re (cast RecurExpr e)
                  vs (Evaluator/args (.-args re) f)
                  lls (.-loopLocals re)]
              (loop [^int i 0]
                (when (< i (alength vs))
                  (aset (.-slots f) (.-idx (cast LocalBinding (.nth lls i))) (aget vs i))
                  (recur (unchecked-inc-int i))))
              RECUR)

          (instance? FnExpr e) (EvalFn. (cast FnExpr e) f)

          (instance? InvokeExpr e)
            (let [ie (cast InvokeExpr e)]
              (try
                (let [fv (cast IFn (Evaluator/eval (.-fexpr ie) f))]
                  (.applyTo fv (RT/seq (Evaluator/args (.-args ie) f))))
                (catch Throwable t
                  (throw (Evaluator/execution (.-source ie) (.-line ie) (.-column ie) t)))))

          (instance? StaticMethodExpr e)
            (let [se (cast StaticMethodExpr e)]
              (try
                (let [vs (Evaluator/args (.-args se) f)]
                  (if (some? (.-method se))
                      (let [ms (LinkedList.)]
                        (.add ms (.-method se))
                        (Reflector/invokeMatchingMethod (.-methodName se) ms nil vs))
                      (Reflector/invokeStaticMethod (.-c se) (.-methodName se) vs)))
                (catch Throwable t
                  (throw (Evaluator/execution (.-source se) (.-line se) (.-column se) t)))))

          (instance? InstanceMethodExpr e)
            (let [me (cast InstanceMethodExpr e)]
              (try
                (let [target (Evaluator/eval (.-target me) f)
                      vs (Evaluator/args (.-args me) f)]
                  (cond
                    (some? (.-method me))
                      (let [ms (LinkedList.)]
                        (.add ms (.-method me))
                        (Reflector/invokeMatchingMethod (.-methodName me) ms target vs))
                    (some? (.-qualifyingClass me))
                      (Reflector/invokeInstanceMethodOfClass target (.-qualifyingClass me)
                                                             (.-methodName me) vs)
                    :else (Reflector/invokeInstanceMethod target (.-methodName me) vs)))
                (catch Throwable t
                  (throw (Evaluator/execution (.-source me) (.-line me) (.-column me) t)))))

          ;; B1a step 5: the other node kinds that can hold locals (letfn, try, throw, the
          ;; collection literals, def with a local init, set!, case, instance?, new, fields,
          ;; keyword invokes, monitors, deftype/reify through Dyn)
          :else (.eval e)))

      ;; a call of an evaluated fn: the method of the arity, its parameters bound in a new
      ;; frame, its body run until it does not recur
      (method ^:public ^:static invokeFn [^EvalFn fn ^Object/1 args]
        (let [fe (.-fe fn)
              n (alength args)
              ^:mutable ^FnMethod m nil]
          (for-each [o (cast java.util.Collection (.-methods fe))]
            (let [fm (cast FnMethod o)]
              (when (and (nil? m) (nil? (.-restParm fm)) (== (.count (.-reqParms fm)) n))
                (set! m fm))))
          (when (and (nil? m) (some? (.-variadicMethod fe))
                     (>= n (.count (.-reqParms (.-variadicMethod fe)))))
            (set! m (.-variadicMethod fe)))
          (when (nil? m) (throw (ArityException. n (.-name fe))))
          (let [f (Frame. (unchecked-add-int (.-maxLocal m) 2) fn m)
                req (.-reqParms m)
                nreq (.count req)]
            (when-not (.-canBeDirect fe) (aset (.-slots f) 0 fn))
            (loop [^int i 0]
              (when (< i nreq)
                (aset (.-slots f) (.-idx (cast LocalBinding (.nth req i))) (aget args i))
                (recur (unchecked-inc-int i))))
            (when (some? (.-restParm m))
              (aset (.-slots f) (.-idx (.-restParm m))
                    (when (> n nreq) (ArraySeq/create (^[Object/1 int int] Arrays/copyOfRange args nreq n)))))
            (loop []
              (let [r (Evaluator/eval (.-body m) f)]
                (if (identical? r RECUR) (recur) r)))))))))

(c2g/variant Compiler$ObjExpr
  ;; no class is generated: the Expr tree is evaluated (C2G-SPEC §10.1)
  (method compile :throws [IOException] ^void [this ^String superName ^String/1 interfaceNames
                                               ^boolean oneTimeUse]
    nil)

  (method ^:synchronized getCompiledClass ^Class [this] nil)

  ;; a fn* evaluated as a top-level form: a fn without a frame (it closes over nothing)
  (method ^:public eval [this]
    (if (instance? FnExpr this)
        (Compiler$EvalFn. (cast FnExpr this) nil)
        (throw (UnsupportedOperationException.
                 "evaluator: deftype and reify (B1a step 5, through Dyn)")))))

(c2g/variant Compiler$HostExpr
  ;; the descriptor of an array class from its symbol (String/2): a primitive's descriptor
  ;; by its name where the JVM build asks ASM's Type
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
        (.append arrayDescriptor
                 (if (.isPrimitive componentClass)
                     (switch (.getName componentClass)
                       "boolean" "Z" "byte" "B" "char" "C" "short" "S" "int" "I" "long" "J"
                       "float" "F" "double" "D" "V")
                     (java-str "L" (.getName componentClass) ";")))
        (.toString arrayDescriptor)))))

(c2g/variant Compiler$StaticInvokeExpr
  ;; direct linking calls a fn's static invokeStatic method: the evaluator's fns have none, so
  ;; a direct-linked call is an ordinary invoke (and *compiler-options* has no :direct-linking)
  (method ^:static parse ^Expr [^Var v ^ISeq args tag ^boolean tailPosition] nil))

(c2g/variant Compiler$QualifiedMethodExpr
  ;; the JVM build filters with streams, which the Go build has not (JAVA-SURFACE.md: rewritten
  ;; as loops); the same filters, in the same order
  (method ^:public ^:static methodOverloads ^{:tag (List Executable)} [^Class c ^String methodName
                                                                       ^MethodKind kind]
    (let [^{:tag (List Executable)} res (ArrayList.)]
      (for-each [^Executable m (.getMethods c)]
        (when (and (.equals (.getName m) methodName)
                   (switch kind
                     STATIC (arbace.lang.Compiler/isStaticMethod m)
                     INSTANCE (arbace.lang.Compiler/isInstanceMethod m)
                     false))
          (.add res m)))
      res))

  (method ^:static resolveHintedMethod ^Executable [^Class c ^String methodName ^MethodKind kind
                                                    ^{:tag (List Class)} hintedSig]
    (let [^{:tag (List Executable)} methods (QualifiedMethodExpr/methodsWithName
                                              c
                                              methodName
                                              kind)
          arity (.size hintedSig)
          ^{:tag (List Executable)} filteredMethods (ArrayList.)]
      (for-each [^Executable m methods]
        (when (and (== (.getParameterCount m) arity)
                   (not (.isSynthetic m))
                   (arbace.lang.Compiler/signatureMatches hintedSig m))
          (.add filteredMethods m)))
      (if (== (.size filteredMethods) 1)
          (cast Executable (.get filteredMethods 0))
          (throw (QualifiedMethodExpr/paramTagsDontResolveException c methodName hintedSig)))))

  (method ^:static paramTagsDontResolveException ^IllegalArgumentException [^Class c
                                                                            ^String methodName
                                                                            ^{:tag (List Class)} hintedSig]
    (let [^{:tag (List Object)} tags (ArrayList.)]
      (for-each [^Class tag hintedSig]
        (.add tags (if (nil? tag) ^Object PARAM_TAG_ANY ^Object tag)))
      (IllegalArgumentException.
        (java-str "Error - param-tags "
                  (PersistentVector/create tags)
                  " insufficient to resolve "
                  (arbace.lang.Compiler/methodDescription c methodName))))))
