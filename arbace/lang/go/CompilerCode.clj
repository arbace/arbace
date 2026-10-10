;; Go-build variant of arbace.lang.Compiler, part 2: the closure compiler (B1a step 7b,
;; doc/go/SPEED-NOTES.md, "The evaluator"). Read by c2g only.
;;
;; Each method of an evaluated fn, deftype, defrecord or reify is compiled once, when it is first
;; called, from its analyzed Expr tree into a tree of Code nodes (objects whose run methods c2g
;; makes Go methods): what the bytecode back end decides when it emits a method is decided here
;; once, not at every evaluation. Locals are slot indexes; long and double locals live unboxed in
;; the frame's prims (a double as its raw bits); constants are made once; Var and keyword nodes
;; hold their Var and Keyword; resolved static methods of Numbers, RT, Util and Math are called
;; directly with their arguments in their primitive types (CodeOpN, generated:
;; CompilerOps.clj), other resolved methods and constructors through their member table's
;; invoker without Reflector; a node whose type the analysis knows to be long, int, double or
;; boolean also answers runLong, runDouble or runBool, so that primitive arithmetic and tests do
;; not box (the bytecode's emitUnboxed); recur stores its values and sets the frame's recur flag,
;; which its loop clears. Calls of a fixed arity reach the compiled method without a seq
;; (EvalFn.invoke). The semantics are the evaluator's (doc/go/EVAL-NOTES.md: the bytecode's
;; conversions and casts), which evalIn keeps for top-level forms.
;;
;; A method holding a node kind the compiler does not know is compiled in compat mode: its long
;; and double locals stay boxed in the slots, and the unknown node is evaluated by its evalIn.
(in-ns 'arbace.lang)

(c2g/variant Compiler$ObjMethod
  ;; the method compiled (CodeRun.cmethod), or nil
  (c2g/add (field ^:public ^Compiler$CMethod evalCM)))

(c2g/variant Compiler$FnExpr
  ;; the methods of fixed arity by their arity, 0 to 20 (CodeRun.fixed)
  (c2g/add (field ^:public ^Compiler$FnMethod/1 evalArities)))

(c2g/variant Compiler
  ;; ---------------------------------------------------------------------------------------
  ;; Code: a compiled node. run is its value as an Object (boxed as the bytecode boxes it); a node
  ;; whose analyzed type is primitive (Compiler.maybePrimitiveType: long, int, double, boolean)
  ;; also gives it unboxed: runLong (long and int), runDouble, runBool. The defaults unbox run's
  ;; value, for the nodes that are not primitive but stand where a primitive is wanted only
  ;; through the converters (CodeUnbox).
  (c2g/add
    (defclass ^:public ^:abstract ^:static Code
      (method ^:public ^:abstract run [this ^Frame f])

      (method ^:public runLong ^long [this ^Frame f]
        (.longValue (cast Number (.run this f))))

      (method ^:public runDouble ^double [this ^Frame f]
        (.doubleValue (cast Number (.run this f))))

      (method ^:public runBool ^boolean [this ^Frame f]
        (.booleanValue (cast Boolean (.run this f))))))

  ;; a node kind the compiler does not know: the method is compiled again in compat mode
  (c2g/add
    (defclass ^:public ^:static CodeFallback
      :extends RuntimeException
      (constructor ^:public [this ^String msg] (super. msg))))

  ;; ---------------------------------------------------------------------------------------
  ;; A compiled method: its body, its frame's size, where its parameters go, how its result is
  ;; returned

  (c2g/add
    (defclass ^:public ^:static CMethod
      (field ^:public ^Code body)
      (field ^:public ^Expr bodyExpr)
      (field ^:public ^ObjMethod method)
      (field ^:public ^ObjExpr objx)
      ;; the frame's slots and prims (0: none)
      (field ^:public ^int nslots)
      (field ^:public ^int nprims)
      ;; slot 0 holds the fn (a fn that is not direct)
      (field ^:public ^boolean selfSlot)
      (field ^:public ^boolean compat)
      ;; per parameter (a fn's required ones, a deftype method's): its slot or prims index, and
      ;; its kind: 0 stored as is, 1 converted by Evaluator.prim(pclass), 2 a long in prims,
      ;; 3 a double in prims
      (field ^:public ^int/1 pidx)
      (field ^:public ^int/1 pkind)
      (field ^:public ^Class/1 pclass)
      ;; the result: 0 the body's value, 1 a long return of a long or int body, 2 a double
      ;; return of a double body, 3 converted by Evaluator.result
      (field ^:public ^int rkind)
      (field ^:public ^Class retClass)))

  ;; ---------------------------------------------------------------------------------------
  ;; The run-time side: entry points of EvalFn, EvalMethod and the evaluator, conversions

  (c2g/add
    (defclass ^:public ^:static CodeRun
      (field ^:static ^java.util.HashMap OPS)

      ;; the compiled method of fn's fixed arity n, or nil (none: a variadic call or an
      ;; ArityException, through RestFn's seq)
      (method ^:public ^:static fixed ^CMethod [^EvalFn fn ^int n]
        (let [fe (.-fe fn)
              ^:mutable ms (.-evalArities fe)]
          (when (nil? ms)
            (set! ms (CodeRun/arities fe)))
          (let [m (aget ms n)]
            (if (nil? m)
                nil
                (let [cm (.-evalCM m)]
                  (if (some? cm) cm (CodeRun/cmethod m fe true)))))))

      (method ^:static arities ^FnMethod/1 [^FnExpr fe]
        (let [ms (new FnMethod/1 (unchecked-inc-int MAX_POSITIONAL_ARITY))]
          (loop [s (RT/seq (.-methods fe))]
            (when (some? s)
              (let [fm (cast FnMethod (.first s))]
                (when (nil? (.-restParm fm))
                  (aset ms (.count (.-reqParms fm)) fm)))
              (recur (.next s))))
          (set! (.-evalArities fe) ms)
          ms))

      ;; the compiled method of a deftype's method called with n arguments (and the object)
      (method ^:public ^:static method ^CMethod [^EvalMethod em ^int n]
        (let [m (.-m em)]
          (when-not (== n (.count (.-argLocals m)))
            (throw (ArityException. n (java-str (.-name (.-nie em)) "." (.-name m)))))
          (let [cm (.-evalCM m)]
            (if (some? cm) cm (CodeRun/cmethod m (.-nie em) false)))))

      ;; method m of objx compiled (cached on the method); in compat mode when it holds a node
      ;; kind the compiler does not know
      (method ^:public ^:static cmethod ^CMethod [^ObjMethod m ^ObjExpr objx ^boolean isFn]
        (let [^:mutable ^CMethod cm nil]
          (try
            (set! cm (.compileMethod (CodeCompiler. objx m isFn false)))
            (catch CodeFallback e
              (set! cm (.compileMethod (CodeCompiler. objx m isFn true)))))
          (set! (.-evalCM m) cm)
          cm))

      ;; parameter i of frame f's method bound to v
      (method ^:public ^:static bind ^void [^Frame f ^CMethod cm ^int i v]
        (let [k (aget (.-pkind cm) i)
              j (aget (.-pidx cm) i)]
          (cond
            (== k 0) (aset (.-slots f) j v)
            (== k 1) (aset (.-slots f) j (Evaluator/prim (aget (.-pclass cm) i) v))
            (== k 2) (aset (.-prims f) j (CodeRun/toLong v))
            :else (aset (.-prims f) j (Double/doubleToRawLongBits (CodeRun/toDouble v))))))

      ;; the body of frame f's method run until it does not recur, its result as the method
      ;; returns it
      (method ^:public ^:static run [^CMethod cm ^Frame f]
        (let [k (.-rkind cm)]
          (cond
            (== k 1) (Long/valueOf (CodeRun/runLong cm f))
            (== k 2) (Double/valueOf (CodeRun/runDouble cm f))
            (== k 0) (CodeRun/runObject cm f)
            :else (Evaluator/result (.-retClass cm) (.-bodyExpr cm) (CodeRun/runObject cm f)))))

      (method ^:static runObject [^CMethod cm ^Frame f]
        (let [body (.-body cm)]
          (loop []
            (let [r (.run body f)]
              (if (or (.-recur f) (and (.-compat cm) (identical? r Evaluator/RECUR)))
                  (do (set! (.-recur f) false) (recur))
                  r)))))

      (method ^:static runLong ^long [^CMethod cm ^Frame f]
        (let [body (.-body cm)]
          (loop []
            (let [r (.runLong body f)]
              (if (.-recur f)
                  (do (set! (.-recur f) false) (recur))
                  r)))))

      (method ^:static runDouble ^double [^CMethod cm ^Frame f]
        (let [body (.-body cm)]
          (loop []
            (let [r (.runDouble body f)]
              (if (.-recur f)
                  (do (set! (.-recur f) false) (recur))
                  r)))))

      ;; a long parameter or recur value from a boxed one (Evaluator.prim of long)
      (method ^:public ^:static toLong ^long [v]
        (cond
          (instance? Long v) (.longValue (cast Long v))
          (instance? Character v) (long (.charValue (cast Character v)))
          :else (^[Object] RT/longCast v)))

      (method ^:public ^:static toDouble ^double [v]
        (cond
          (instance? Double v) (.doubleValue (cast Double v))
          (instance? Character v) (double (.charValue (cast Character v)))
          :else (^[Object] RT/doubleCast v)))

      ;; an argument for a primitive parameter as the compiled call unboxes it
      ;; (Evaluator.typedArgs: HostExpr.emitUnboxArg, a primitive char widened)
      (method ^:public ^:static unboxLong ^long [v]
        (when (nil? v) (throw (NullPointerException.)))
        (if (instance? Character v)
            (long (.charValue (cast Character v)))
            (^[Object] RT/longCast (cast Number v))))

      (method ^:public ^:static unboxInt ^int [v]
        (when (nil? v) (throw (NullPointerException.)))
        (if (instance? Character v)
            (int (.charValue (cast Character v)))
            (^[Object] RT/intCast (cast Number v))))

      (method ^:public ^:static unboxDouble ^double [v]
        (when (nil? v) (throw (NullPointerException.)))
        (if (instance? Character v)
            (double (.charValue (cast Character v)))
            (^[Object] RT/doubleCast (cast Number v))))

      (method ^:public ^:static unboxBool ^boolean [v]
        (when (nil? v) (throw (NullPointerException.)))
        (.booleanValue (cast Boolean v)))

      (method ^:public ^:static runAll ^Object/1 [^Code/1 cs ^Frame f]
        (let [n (alength cs)
              vs (new Object/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (aset vs i (.run (aget cs i) f))
              (recur (unchecked-inc-int i))))
          vs))

      ;; the arguments of a resolved method or constructor as the compiled call passes them
      ;; (Evaluator.typedArgs, a fn for a functional interface adapted)
      (method ^:public ^:static directArgs ^Object/1 [^Class/1 ps ^Object/1 vs]
        (Evaluator/typedArgs ps vs))

      ;; a resolved method's list for Reflector.invokeMatchingMethod
      (method ^:public ^:static one ^List [^java.lang.reflect.Method m]
        (let [l (LinkedList.)]
          (.add l m)
          l))

      ;; a hinted call of resolved method m (parameter types ps) on target (nil: static) with the
      ;; evaluated arguments vs: Evaluator.invokeResolved (its invoker directly) when direct
      ;; (Evaluator.directOk), else Reflector's, as the evaluator's evalIn calls it
      (method ^:public ^:static call [^java.lang.reflect.Method m ^Class/1 ps ^List ms
                                      ^boolean direct target ^Object/1 vs]
        (if direct
            (Evaluator/invokeResolved m ps target vs)
            (Reflector/invokeMatchingMethod (.getName m) ms target (Evaluator/typedArgs ps vs))))

      (method ^:public ^:static ^:native construct [^java.lang.reflect.Constructor k ^Object/1 args])

      ;; the op of a method CodeOpN calls directly (arity * 10000 + case), or -1
      (method ^:public ^:static op ^int [^java.lang.reflect.Method m]
        (let [^:mutable t OPS]
          (when (nil? t)
            (set! t (CodeOps/table))
            (set! OPS t))
          (let [k (.get t (java-str (.getName (.getDeclaringClass m)) "." (.getName m)
                                    (Evaluator/signature (.getParameterTypes m) (.getReturnType m))))]
            (if (nil? k) -1 (.intValue (cast Integer k))))))))

  ;; ---------------------------------------------------------------------------------------
  ;; The compiler: one method's Expr tree into Code

  (c2g/add
    (defclass ^:public ^:static CodeCompiler
      (field ^ObjExpr objx)
      (field ^ObjMethod method)
      (field ^boolean isFn)
      (field ^boolean compat)
      (field ^LocalBinding/1 closes)
      (field ^int nslots)
      (field ^int nprims)
      (field ^boolean usesPrims)

      (constructor ^:public [this ^ObjExpr objx ^ObjMethod method ^boolean isFn ^boolean compat]
        (set! (.-objx this) objx)
        (set! (.-method this) method)
        (set! (.-isFn this) isFn)
        (set! (.-compat this) compat)
        (set! (.-closes this) (Evaluator/closes objx))
        (set! (.-nslots this) (unchecked-add-int (.-maxLocal method) 2))
        (set! (.-nprims this) (unchecked-add-int (.-maxLocal method) 2)))

      ;; 2 a long, 3 a double, else 0: how a local of primitive type c is stored
      (method ^:static primKind ^int [^Class c]
        (cond
          (identical? c Long/TYPE) 2
          (identical? c Double/TYPE) 3
          :else 0))

      ;; the store of a local: 0 a slot, 1 a slot converted by Evaluator.prim, 2 a long in
      ;; prims, 3 a double in prims, 4 a slot boxed from a long, 5 from a double (compat mode)
      (method storeKind ^int [this ^Class c]
        (let [k (CodeCompiler/primKind c)]
          (cond
            (== k 0) (if (and (some? c) (.isPrimitive c)) 1 0)
            compat (unchecked-add-int k 2)
            :else (do (set! usesPrims true) k))))

      (method ^:public compileMethod ^CMethod [this]
        (let [m method
              cm (CMethod.)
              body (.compile this (.-body m))]
          (set! (.-body cm) body)
          (set! (.-bodyExpr cm) (.-body m))
          (set! (.-method cm) m)
          (set! (.-objx cm) objx)
          (set! (.-compat cm) compat)
          (set! (.-selfSlot cm) (and isFn (not (.-canBeDirect objx))))
          ;; the parameters
          (let [ps (if isFn (.-reqParms (cast FnMethod m)) (.-argLocals m))
                n (.count ps)
                pidx (new int/1 n)
                pkind (new int/1 n)
                pclass (new Class/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (let [lb (cast LocalBinding (.nth ps i))
                      pc (if isFn
                             (let [acs (.-argclasses (cast FnMethod m))]
                               (when (some? acs) (aget acs i)))
                             (.-c (cast MethodParamExpr (.-init lb))))
                      k (CodeCompiler/primKind pc)]
                  (aset pidx i (.-idx lb))
                  (aset pclass i pc)
                  (aset pkind i (cond
                                  (and (> k 0) (not compat)) (do (set! usesPrims true) k)
                                  (and (some? pc) (.isPrimitive pc)) 1
                                  :else 0)))
                (recur (unchecked-inc-int i))))
            (set! (.-pidx cm) pidx)
            (set! (.-pkind cm) pkind)
            (set! (.-pclass cm) pclass))
          ;; the result
          (let [rc (if isFn (.-retClass (cast FnMethod m)) (.-retClass (cast NewInstanceMethod m)))
                bc (arbace.lang.Compiler/maybePrimitiveType (.-body m))]
            (set! (.-retClass cm) rc)
            (set! (.-rkind cm)
                  (cond
                    (or (nil? rc) (not (.isPrimitive rc))) 0
                    (and (identical? rc Long/TYPE) (or (identical? bc Long/TYPE) (identical? bc Integer/TYPE))) 1
                    (and (identical? rc Double/TYPE) (identical? bc Double/TYPE)) 2
                    :else 3)))
          (set! (.-nslots cm) nslots)
          (set! (.-nprims cm) (if usesPrims nprims 0))
          cm))

      (method compileAll ^Code/1 [this ^IPersistentVector es]
        (let [n (.count es)
              cs (new Code/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (aset cs i (.compile this (cast Expr (.nth es i))))
              (recur (unchecked-inc-int i))))
          cs))

      ;; the index of lb among the closed-over locals, or -1
      (method closedIndex ^int [this ^LocalBinding lb]
        (loop [^int i 0]
          (if (< i (alength closes))
              (if (identical? (aget closes i) lb) i (recur (unchecked-inc-int i)))
              -1)))

      ;; the read of local lb
      (method ^:public local ^Code [this ^LocalBinding lb]
        (let [j (.closedIndex this lb)
              k (CodeCompiler/primKind (.getPrimitiveType lb))]
          (cond
            (>= j 0) (if isFn (CodeClosed. j k) (CodeField. j k))
            (and (== k 2) (not compat)) (do (set! usesPrims true) (CodeLocalLong. (.-idx lb)))
            (and (== k 3) (not compat)) (do (set! usesPrims true) (CodeLocalDouble. (.-idx lb)))
            (== k 0) (CodeLocal. (.-idx lb))
            :else (CodeLocalBoxed. (.-idx lb) k))))

      ;; the value of e as an argument for a parameter of class p (MethodExpr.emitTypedArgs)
      (method convert ^Code [this ^Class p ^Expr e]
        (let [c (.compile this e)
              k (arbace.lang.Compiler/maybePrimitiveType e)
              kl (or (identical? k Long/TYPE) (identical? k Integer/TYPE))]
          (cond
            (identical? p Long/TYPE) (if kl c (CodeUnbox. c 0))
            (identical? p Integer/TYPE)
              (cond
                (identical? k Integer/TYPE) c
                (identical? k Long/TYPE) (CodeL2I. c)
                :else (CodeUnbox. c 1))
            (identical? p Double/TYPE)
              (cond
                (identical? k Double/TYPE) c
                kl (CodeL2D. c)
                :else (CodeUnbox. c 2))
            (identical? p Boolean/TYPE) (if (identical? k Boolean/TYPE) c (CodeUnbox. c 3))
            :else c)))

      ;; a recur value for a local stored as store kind k (storeKind): its natural channel
      (method valueKind ^int [this ^Expr e]
        (let [k (arbace.lang.Compiler/maybePrimitiveType e)]
          (cond
            (or (identical? k Long/TYPE) (identical? k Integer/TYPE)) 1
            (identical? k Double/TYPE) 2
            :else 0)))

      (method ^:public compile ^Code [this ^Expr e]
        (cond
          (instance? LocalBindingExpr e) (.localUse this (cast LocalBindingExpr e))
          (or (instance? NumberExpr e) (instance? ConstantExpr e) (instance? StringExpr e)
              (instance? KeywordExpr e) (instance? NilExpr e) (instance? BooleanExpr e)
              (instance? EmptyExpr e) (instance? TheVarExpr e))
            (CodeConst. (.evalIn e nil))
          (instance? VarExpr e) (CodeVar. (.-var (cast VarExpr e)))
          (instance? IfExpr e)
            (let [ie (cast IfExpr e)]
              (CodeIf. (.compile this (.-testExpr ie))
                       (identical? (arbace.lang.Compiler/maybePrimitiveType (.-testExpr ie)) Boolean/TYPE)
                       (.compile this (.-thenExpr ie))
                       (.compile this (.-elseExpr ie))))
          (instance? BodyExpr e)
            (let [es (.-exprs (cast BodyExpr e))
                  n (.count es)]
              (if (== n 1)
                  (.compile this (cast Expr (.nth es 0)))
                  ;; a local as a statement is neither read nor cleared (LocalBindingExpr.emit
                  ;; in a statement context emits nothing)
                  (let [^{:tag (ArrayList Code)} cs (ArrayList.)]
                    (loop [^int i 0]
                      (when (< i n)
                        (let [x (cast Expr (.nth es i))]
                          (when-not (and (instance? LocalBindingExpr x) (< i (unchecked-dec-int n)))
                            (.add cs (.compile this x))))
                        (recur (unchecked-inc-int i))))
                    (CodeCompiler/chain (cast Code/1 (.toArray cs (new Code/1 (.size cs)))) 0))))
          (instance? InvokeExpr e)
            (let [ie (cast InvokeExpr e)]
              (CodeInvoke. (.compile this (.-fexpr ie)) (.compileAll this (.-args ie)) (.-line ie)))
          (instance? KeywordInvokeExpr e)
            (let [ke (cast KeywordInvokeExpr e)]
              (CodeKeywordInvoke. (.-k (.-kw ke)) (.compile this (.-target ke)) (.-line ke)))
          (instance? StaticMethodExpr e) (.staticMethod this (cast StaticMethodExpr e))
          (instance? InstanceMethodExpr e)
            (let [me (cast InstanceMethodExpr e)
                  t (.compile this (.-target me))
                  as (.compileAll this (.-args me))]
              (if (some? (.-method me))
                  (CodeHostInstance. t (.-method me) as (.-line me))
                  (CodeHostInstanceReflect. t (.-qualifyingClass me) (.-methodName me) as (.-line me))))
          (instance? LetExpr e) (.compileLet this (cast LetExpr e))
          (instance? RecurExpr e) (.compileRecur this (cast RecurExpr e))
          (instance? FnExpr e)
            (CodeFn. (cast FnExpr e) (.fetchers this (cast ObjExpr e)))
          (instance? NewInstanceExpr e)
            (let [nie (cast NewInstanceExpr e)]
              (if (.isDeftype nie)
                  (CodeConst. nil)
                  (CodeReify. nie (.fetchers this nie))))
          (instance? LetFnExpr e) (.compileLetFn this (cast LetFnExpr e))
          (instance? TryExpr e) (.compileTry this (cast TryExpr e))
          (instance? ThrowExpr e) (CodeThrow. (.compile this (.-excExpr (cast ThrowExpr e))))
          (instance? MetaExpr e)
            (let [me (cast MetaExpr e)]
              (CodeMeta. (.compile this (.-expr me)) (.compile this (.-meta me))))
          (instance? VectorExpr e) (CodeColl. 0 (.compileAll this (.-args (cast VectorExpr e))))
          (instance? ListExpr e) (CodeColl. 1 (.compileAll this (.-args (cast ListExpr e))))
          (instance? MapExpr e) (CodeColl. 2 (.compileAll this (.-keyvals (cast MapExpr e))))
          (instance? SetExpr e) (CodeColl. 3 (.compileAll this (.-keys (cast SetExpr e))))
          (instance? InstanceOfExpr e)
            (let [io (cast InstanceOfExpr e)]
              (CodeInstanceOf. (.-c io) (.compile this (.-expr io))))
          (instance? StrConcatExpr e)
            (let [se (cast StrConcatExpr e)]
              (CodeStrConcat. (.compileAll this (.-args se)) (.-texts se)))
          (instance? CaseExpr e) (.compileCase this (cast CaseExpr e))
          (instance? DefExpr e)
            (let [de (cast DefExpr e)]
              (CodeDef. (.-var de) (.-isDynamic de)
                        (when (some? (.-meta de)) (.compile this (.-meta de)))
                        (.-initProvided de)
                        (when (.-initProvided de) (.compile this (.-init de)))))
          (instance? AssignExpr e) (.compileAssign this (cast AssignExpr e))
          (instance? NewExpr e)
            (let [ne (cast NewExpr e)]
              (CodeHostNew. (.-c ne) (.-ctor ne) (.compileAll this (.-args ne))))
          (instance? InstanceFieldExpr e)
            (let [fe (cast InstanceFieldExpr e)]
              (CodeHostField. fe (.compile this (.-target fe))))
          (instance? MonitorEnterExpr e)
            (CodeMonitor. true (.compile this (.-target (cast MonitorEnterExpr e))))
          (instance? MonitorExitExpr e)
            (CodeMonitor. false (.compile this (.-target (cast MonitorExitExpr e))))
          ;; nodes without locals, evaluated each time by their evalIn
          (or (instance? StaticFieldExpr e) (instance? ImportExpr e)
              (instance? QualifiedMethodExpr e) (instance? UnresolvedVarExpr e))
            (CodeEval. e)
          compat (CodeEval. e)
          :else (throw (CodeFallback. (.getName (.getClass e))))))

      (method staticMethod ^Code [this ^StaticMethodExpr se]
        (let [m (.-method se)]
          (if (nil? m)
              (CodeHostStaticReflect. (.-c se) (.-methodName se) (.compileAll this (.-args se)) (.-line se))
              (let [op (CodeRun/op m)
                    ps (.getParameterTypes m)
                    as (.-args se)]
                (if (< op 0)
                    (CodeHostStatic. m (.compileAll this as) (.-line se))
                    (let [k (unchecked-divide-int op 10000)
                          o (unchecked-subtract-int op (unchecked-multiply-int k 10000))]
                      (cond
                        (== k 0) (CodeOp0. o (.-line se))
                        (== k 1) (CodeOp1. o (.-line se) (.convert this (aget ps 0) (cast Expr (.nth as 0))))
                        (== k 2) (CodeOp2. o (.-line se) (.convert this (aget ps 0) (cast Expr (.nth as 0)))
                                           (.convert this (aget ps 1) (cast Expr (.nth as 1))))
                        :else (CodeOp3. o (.-line se) (.convert this (aget ps 0) (cast Expr (.nth as 0)))
                                        (.convert this (aget ps 1) (cast Expr (.nth as 1)))
                                        (.convert this (aget ps 2) (cast Expr (.nth as 2)))))))))))

      (method compileLet ^Code [this ^LetExpr le]
        (let [bis (.-bindingInits le)
              n (.count bis)
              inits (new Code/1 n)
              kinds (new int/1 n)
              dest (new int/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (let [bi (cast BindingInit (.nth bis i))
                    lb (.-binding bi)]
                (aset inits i (.compile this (.-init bi)))
                (aset dest i (.-idx lb))
                ;; the init is of the local's type: a Evaluator.prim conversion is not needed
                (aset kinds i (let [k (.storeKind this (.getPrimitiveType lb))] (if (== k 1) 0 k))))
              (recur (unchecked-inc-int i))))
          ;; the stores, then the body: a chain of pairs (an element of a Code[] read in Go is an
          ;; interface assertion, a lookup of its method table: the hot nodes hold their parts in
          ;; fields)
          (let [body (.compile this (.-body le))
                stores (new Code/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (aset stores i (CodeStore. (aget kinds i) (aget dest i) (aget inits i)))
                (recur (unchecked-inc-int i))))
            (cond
              (.-isLoop le) (CodeLoop. (if (== n 0) nil (CodeCompiler/chain stores 0)) body compat)
              (== n 0) body
              :else (CodeSeq. (CodeCompiler/chain stores 0) body)))))

      ;; cs[i..] run in order, the last one's value the chain's
      (method ^:static chain ^Code [^Code/1 cs ^int i]
        (if (== i (unchecked-dec-int (alength cs)))
            (aget cs i)
            (CodeSeq. (aget cs i) (CodeCompiler/chain cs (unchecked-inc-int i)))))

      (method compileRecur ^Code [this ^RecurExpr re]
        (let [args (.-args re)
              lls (.-loopLocals re)
              n (.count args)
              cs (new Code/1 n)
              kinds (new int/1 n)
              vkinds (new int/1 n)
              dest (new int/1 n)
              tmp (new int/1 n)
              pcs (new Class/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (let [lb (cast LocalBinding (.nth lls i))
                    a (cast Expr (.nth args i))
                    k (.storeKind this (.getPrimitiveType lb))]
                (aset cs i (.compile this a))
                (aset kinds i k)
                (aset vkinds i (.valueKind this a))
                (aset dest i (.-idx lb))
                (aset pcs i (.getPrimitiveType lb))
                (when (> n 1)
                  ;; a temporary: the values are all computed before any is stored
                  (if (>= k 2)
                      (do (set! usesPrims true)
                          (aset tmp i nprims)
                          (set! nprims (unchecked-inc-int nprims)))
                      (do (aset tmp i nslots)
                          (set! nslots (unchecked-inc-int nslots))))))
              (recur (unchecked-inc-int i))))
          (CodeRecur. cs kinds vkinds dest tmp pcs)))

      ;; the reads, in the method being compiled, of the locals objx closes over: through its
      ;; closesExprs (made by ObjExpr.compile, as ObjExpr.emit loads them), which take part in
      ;; locals clearing (Evaluator.capture)
      (method fetchers ^Code/1 [this ^ObjExpr ox]
        (let [bs (Evaluator/closes ox)
              ces (.-closesExprs ox)
              byExpr (== (.count ces) (alength bs))
              cs (new Code/1 (alength bs))]
          (loop [^int i 0]
            (when (< i (alength bs))
              (aset cs i (if byExpr
                             (.localUse this (cast LocalBindingExpr (.nth ces i)))
                             (.local this (aget bs i))))
              (recur (unchecked-inc-int i))))
          cs))

      ;; a use of a local: its read, then, where the analyzer marks it as the last use on its
      ;; path (shouldClear), its slot cleared, or in a ^:once fn its closed-over value (locals
      ;; clearing, ObjExpr.emitLocal; Evaluator.clear). Primitive locals are not cleared
      (method localUse ^Code [this ^LocalBindingExpr lbe]
        (let [lb (.-b lbe)]
          (if (and (.-shouldClear lbe) (.-canBeCleared lb) (nil? (.getPrimitiveType lb)))
              (let [j (.closedIndex this lb)]
                (cond
                  (< j 0) (CodeLocalClear. (.-idx lb))
                  (and isFn (.-onceOnly objx)) (CodeClosedClear. j)
                  :else (.local this lb)))
              (.local this lb))))

      (method compileLetFn ^Code [this ^LetFnExpr le]
        (let [bis (.-bindingInits le)
              n (.count bis)
              inits (new Code/1 n)
              dest (new int/1 n)
              ;; per binding: the closed-over values of its fn to fill, and the binding each is
              patchJ (new Object/1 n)
              patchK (new Object/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (let [bi (cast BindingInit (.nth bis i))]
                (aset inits i (.compile this (.-init bi)))
                (aset dest i (.-idx (.-binding bi)))
                (let [^{:tag (ArrayList Integer)} js (ArrayList.)
                      ^{:tag (ArrayList Integer)} ks (ArrayList.)]
                  (when (instance? FnExpr (.-init bi))
                    (let [cbs (Evaluator/closes (cast ObjExpr (.-init bi)))]
                      (loop [^int j 0]
                        (when (< j (alength cbs))
                          (loop [^int k 0]
                            (when (< k n)
                              (if (identical? (.-binding (cast BindingInit (.nth bis k))) (aget cbs j))
                                  (do (.add js (Integer/valueOf j)) (.add ks (Integer/valueOf k)))
                                  (recur (unchecked-inc-int k)))))
                          (recur (unchecked-inc-int j))))))
                  (aset patchJ i (CodeCompiler/ints js))
                  (aset patchK i (CodeCompiler/ints ks))))
              (recur (unchecked-inc-int i))))
          (CodeLetFn. inits dest patchJ patchK (.compile this (.-body le)))))

      (method ^:static ints ^int/1 [^{:tag (ArrayList Integer)} xs]
        (let [a (new int/1 (.size xs))]
          (loop [^int i 0]
            (when (< i (alength a))
              (aset a i (.intValue (cast Integer (.get xs i))))
              (recur (unchecked-inc-int i))))
          a))

      (method compileTry ^Code [this ^TryExpr te]
        (let [cs (.-catchExprs te)
              n (.count cs)
              classes (new Class/1 n)
              slots (new int/1 n)
              handlers (new Code/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (let [c (cast Compiler$TryExpr$CatchClause (.nth cs i))]
                (aset classes i (.-c c))
                (aset slots i (.-idx (.-lb c)))
                (aset handlers i (.compile this (.-handler c))))
              (recur (unchecked-inc-int i))))
          (CodeTry. (.compile this (.-tryExpr te)) classes slots handlers
                    (when (some? (.-finallyExpr te)) (.compile this (.-finallyExpr te))))))

      (method compileCase ^Code [this ^CaseExpr ce]
        (let [^{:tag (HashMap Integer Object)} thens (HashMap.)
              ^{:tag (HashMap Integer Object)} tests (HashMap.)]
          (for-each [^Map$Entry en (.entrySet (.-thens ce))]
            (.put thens (cast Integer (.getKey en)) (.compile this (cast Expr (.getValue en)))))
          (for-each [^Map$Entry en (.entrySet (.-tests ce))]
            (.put tests (cast Integer (.getKey en)) (.eval (cast Expr (.getValue en)))))
          (CodeCase. ce (.compile this (.-expr ce)) (.compile this (.-defaultExpr ce)) thens tests
                     (arbace.lang.Compiler/maybePrimitiveType (.-expr ce)))))

      (method compileAssign ^Code [this ^AssignExpr ae]
        (let [target (.-target ae)
              v (.compile this (.-val ae))]
          (cond
            (instance? VarExpr target) (CodeAssign. 0 (.-var (cast VarExpr target)) v nil -1 nil)
            (instance? LocalBindingExpr target)
              (let [lb (.-b (cast LocalBindingExpr target))
                    j (.closedIndex this lb)]
                (cond
                  (and (>= j 0) (not isFn)) (CodeAssign. 1 nil v nil j nil)
                  (>= j 0) (CodeAssign. 2 lb v nil -1 nil)
                  :else (CodeAssign. 3 lb v nil (.-idx lb) nil)))
            (instance? InstanceFieldExpr target)
              (CodeAssign. 4 target v (.compile this (.-target (cast InstanceFieldExpr target))) -1 nil)
            (instance? StaticFieldExpr target) (CodeAssign. 5 target v nil -1 nil)
            ;; another assignable expression: its evalAssign of the value expression
            :else (CodeAssign. 6 target v nil -1 (.-val ae)))))))

  ;; ---------------------------------------------------------------------------------------
  ;; Nodes

  (c2g/add
    (defclass ^:public ^:static CodeConst
      :extends Code
      (field ^:public ^:final v)
      ;; a number's long and double values, made once
      (field ^:public ^:final ^long lv)
      (field ^:public ^:final ^double dv)
      (field ^:public ^:final ^boolean num)
      (constructor ^:public [this v]
        (set! (.-v this) v)
        (set! (.-num this) (instance? Number v))
        (set! (.-lv this) (if (instance? Number v) (.longValue (cast Number v)) 0))
        (set! (.-dv this) (if (instance? Number v) (.doubleValue (cast Number v)) 0.0)))
      (method ^:public run [this ^Frame f] v)
      (method ^:public runLong ^long [this ^Frame f]
        (if num lv (.longValue (cast Number v))))
      (method ^:public runDouble ^double [this ^Frame f]
        (if num dv (.doubleValue (cast Number v))))))

  (c2g/add
    (defclass ^:public ^:static CodeEval
      :extends Code
      ;; a node evaluated by its evalIn (without locals, or compat mode)
      (field ^:public ^:final ^Expr e)
      (constructor ^:public [this ^Expr e] (set! (.-e this) e))
      (method ^:public run [this ^Frame f] (.evalIn e f))))

  (c2g/add
    (defclass ^:public ^:static CodeVar
      :extends Code
      (field ^:public ^:final ^Var v)
      (constructor ^:public [this ^Var v] (set! (.-v this) v))
      (method ^:public run [this ^Frame f] (.deref v))))

  ;; locals: a slot; a long or a double in prims; a long or a double boxed in a slot (compat
  ;; mode); a closed-over value of the fn; a field of the deftype's object. A primitive local is
  ;; boxed anew at each use, as the bytecode boxes it ((let [x ##NaN] (identical? x x)) is false)

  (c2g/add
    (defclass ^:public ^:static CodeLocal
      :extends Code
      (field ^:public ^:final ^int i)
      (constructor ^:public [this ^int i] (set! (.-i this) i))
      (method ^:public run [this ^Frame f] (aget (.-slots f) i))))

  (c2g/add
    (defclass ^:public ^:static CodeLocalClear
      :extends Code
      ;; a slot local read for the last time on its path: cleared (CodeCompiler.localUse)
      (field ^:public ^:final ^int i)
      (constructor ^:public [this ^int i] (set! (.-i this) i))
      (method ^:public run [this ^Frame f]
        (let [slots (.-slots f)
              v (aget slots i)]
          (aset slots i nil)
          v))))

  (c2g/add
    (defclass ^:public ^:static CodeClosedClear
      :extends Code
      ;; a ^:once fn's closed-over value read for the last time on its path: cleared
      (field ^:public ^:final ^int j)
      (constructor ^:public [this ^int j] (set! (.-j this) j))
      (method ^:public run [this ^Frame f]
        (let [cl (.-closed (.-fn f))
              v (aget cl j)]
          (aset cl j nil)
          v))))

  (c2g/add
    (defclass ^:public ^:static CodeLocalLong
      :extends Code
      (field ^:public ^:final ^int i)
      (constructor ^:public [this ^int i] (set! (.-i this) i))
      (method ^:public run [this ^Frame f] (Long/valueOf (aget (.-prims f) i)))
      (method ^:public runLong ^long [this ^Frame f] (aget (.-prims f) i))))

  (c2g/add
    (defclass ^:public ^:static CodeLocalDouble
      :extends Code
      (field ^:public ^:final ^int i)
      (constructor ^:public [this ^int i] (set! (.-i this) i))
      (method ^:public run [this ^Frame f]
        (Double/valueOf (Double/longBitsToDouble (aget (.-prims f) i))))
      (method ^:public runDouble ^double [this ^Frame f]
        (Double/longBitsToDouble (aget (.-prims f) i)))))

  (c2g/add
    (defclass ^:public ^:static CodeLocalBoxed
      :extends Code
      ;; k: 2 long, 3 double
      (field ^:public ^:final ^int i)
      (field ^:public ^:final ^int k)
      (constructor ^:public [this ^int i ^int k] (set! (.-i this) i) (set! (.-k this) k))
      (method ^:public run [this ^Frame f]
        (CodeLocalBoxed/rebox k (aget (.-slots f) i)))
      (method ^:public ^:static rebox [^int k v]
        (cond
          (nil? v) v
          (== k 2) (Long/valueOf (.longValue (cast Number v)))
          (== k 3) (Double/valueOf (.doubleValue (cast Number v)))
          :else v))))

  (c2g/add
    (defclass ^:public ^:static CodeClosed
      :extends Code
      (field ^:public ^:final ^int j)
      (field ^:public ^:final ^int k)
      (constructor ^:public [this ^int j ^int k] (set! (.-j this) j) (set! (.-k this) k))
      (method ^:public run [this ^Frame f]
        (CodeLocalBoxed/rebox k (aget (.-closed (.-fn f)) j)))
      (method ^:public runLong ^long [this ^Frame f]
        (.longValue (cast Number (aget (.-closed (.-fn f)) j))))
      (method ^:public runDouble ^double [this ^Frame f]
        (.doubleValue (cast Number (aget (.-closed (.-fn f)) j))))))

  (c2g/add
    (defclass ^:public ^:static CodeField
      :extends Code
      (field ^:public ^:final ^int j)
      (field ^:public ^:final ^int k)
      (constructor ^:public [this ^int j ^int k] (set! (.-j this) j) (set! (.-k this) k))
      (method ^:public run [this ^Frame f]
        (CodeLocalBoxed/rebox k (Dyn/getField (.-self f) j)))
      (method ^:public runLong ^long [this ^Frame f]
        (.longValue (cast Number (Dyn/getField (.-self f) j))))
      (method ^:public runDouble ^double [this ^Frame f]
        (.doubleValue (cast Number (Dyn/getField (.-self f) j))))))

  ;; converters (CodeCompiler.convert): mode 0 long, 1 int, 2 double, 3 boolean

  (c2g/add
    (defclass ^:public ^:static CodeUnbox
      :extends Code
      (field ^:public ^:final ^Code c)
      (field ^:public ^:final ^int mode)
      (constructor ^:public [this ^Code c ^int mode] (set! (.-c this) c) (set! (.-mode this) mode))
      (method ^:public run [this ^Frame f] (.run c f))
      (method ^:public runLong ^long [this ^Frame f]
        (if (== mode 1)
            (CodeRun/unboxInt (.run c f))
            (CodeRun/unboxLong (.run c f))))
      (method ^:public runDouble ^double [this ^Frame f] (CodeRun/unboxDouble (.run c f)))
      (method ^:public runBool ^boolean [this ^Frame f] (CodeRun/unboxBool (.run c f)))))

  (c2g/add
    (defclass ^:public ^:static CodeL2I
      :extends Code
      (field ^:public ^:final ^Code c)
      (constructor ^:public [this ^Code c] (set! (.-c this) c))
      (method ^:public run [this ^Frame f] (Integer/valueOf (^[long] RT/intCast (.runLong c f))))
      (method ^:public runLong ^long [this ^Frame f] (^[long] RT/intCast (.runLong c f)))))

  (c2g/add
    (defclass ^:public ^:static CodeL2D
      :extends Code
      (field ^:public ^:final ^Code c)
      (constructor ^:public [this ^Code c] (set! (.-c this) c))
      (method ^:public run [this ^Frame f] (Double/valueOf (double (.runLong c f))))
      (method ^:public runDouble ^double [this ^Frame f] (double (.runLong c f)))))

  ;; control

  (c2g/add
    (defclass ^:public ^:static CodeIf
      :extends Code
      (field ^:public ^:final ^Code test)
      ;; the test is a boolean (runBool), else its value is tested for nil and false
      (field ^:public ^:final ^boolean z)
      (field ^:public ^:final ^Code thn)
      (field ^:public ^:final ^Code els)
      (constructor ^:public [this ^Code test ^boolean z ^Code thn ^Code els]
        (set! (.-test this) test)
        (set! (.-z this) z)
        (set! (.-thn this) thn)
        (set! (.-els this) els))
      (method holds ^boolean [this ^Frame f]
        (if z
            (.runBool test f)
            (let [t (.run test f)]
              (and (some? t) (not (identical? t Boolean/FALSE))))))
      (method ^:public run [this ^Frame f]
        (if (.holds this f) (.run thn f) (.run els f)))
      (method ^:public runLong ^long [this ^Frame f]
        (if (.holds this f) (.runLong thn f) (.runLong els f)))
      (method ^:public runDouble ^double [this ^Frame f]
        (if (.holds this f) (.runDouble thn f) (.runDouble els f)))
      (method ^:public runBool ^boolean [this ^Frame f]
        (if (.holds this f) (.runBool thn f) (.runBool els f)))))

  (c2g/add
    (defclass ^:public ^:static CodeSeq
      :extends Code
      ;; a then b, b's value (a do's statements, a let's stores then its body)
      (field ^:public ^:final ^Code a)
      (field ^:public ^:final ^Code b)
      (constructor ^:public [this ^Code a ^Code b] (set! (.-a this) a) (set! (.-b this) b))
      (method ^:public run [this ^Frame f] (.run a f) (.run b f))
      (method ^:public runLong ^long [this ^Frame f] (.run a f) (.runLong b f))
      (method ^:public runDouble ^double [this ^Frame f] (.run a f) (.runDouble b f))
      (method ^:public runBool ^boolean [this ^Frame f] (.run a f) (.runBool b f))))

  (c2g/add
    (defclass ^:public ^:static CodeStore
      :extends Code
      ;; a let's or loop's binding: its init stored as CodeCompiler.storeKind says (1 never: the
      ;; init is of the local's type)
      (field ^:public ^:final ^int kind)
      (field ^:public ^:final ^int d)
      (field ^:public ^:final ^Code c)
      (constructor ^:public [this ^int kind ^int d ^Code c]
        (set! (.-kind this) kind)
        (set! (.-d this) d)
        (set! (.-c this) c))
      (method ^:public run [this ^Frame f]
        (cond
          (== kind 0) (aset (.-slots f) d (.run c f))
          (== kind 2) (aset (.-prims f) d (.runLong c f))
          (== kind 3) (aset (.-prims f) d (Double/doubleToRawLongBits (.runDouble c f)))
          (== kind 4) (aset (.-slots f) d (Long/valueOf (.runLong c f)))
          :else (aset (.-slots f) d (Double/valueOf (.runDouble c f))))
        nil)))

  (c2g/add
    (defclass ^:public ^:static CodeLoop
      :extends Code
      ;; a loop: its stores (or nil), then its body again while it recurs
      (field ^:public ^:final ^Code stores)
      (field ^:public ^:final ^Code body)
      (field ^:public ^:final ^boolean compat)
      (constructor ^:public [this ^Code stores ^Code body ^boolean compat]
        (set! (.-stores this) stores)
        (set! (.-body this) body)
        (set! (.-compat this) compat))
      (method again ^boolean [this ^Frame f r]
        (if (or (.-recur f) (and compat (identical? r Evaluator/RECUR)))
            (do (set! (.-recur f) false) true)
            false))
      (method ^:public run [this ^Frame f]
        (when (some? stores) (.run stores f))
        (loop []
          (let [r (.run body f)]
            (if (.again this f r) (recur) r))))
      (method ^:public runLong ^long [this ^Frame f]
        (when (some? stores) (.run stores f))
        (loop []
          (let [r (.runLong body f)]
            (if (.again this f nil) (recur) r))))
      (method ^:public runDouble ^double [this ^Frame f]
        (when (some? stores) (.run stores f))
        (loop []
          (let [r (.runDouble body f)]
            (if (.again this f nil) (recur) r))))
      (method ^:public runBool ^boolean [this ^Frame f]
        (when (some? stores) (.run stores f))
        (loop []
          (let [r (.runBool body f)]
            (if (.again this f nil) (recur) r))))))

  (c2g/add
    (defclass ^:public ^:static CodeRecur
      :extends Code
      ;; the values, stored as CodeCompiler.storeKind says; with more than one, all are
      ;; computed into temporaries (tmp: prims for kinds 2 to 5, else slots) before any is stored
      (field ^:public ^:final ^Code/1 cs)
      (field ^:public ^:final ^int/1 kinds)
      ;; each value's natural channel: 0 run, 1 runLong, 2 runDouble
      (field ^:public ^:final ^int/1 vkinds)
      (field ^:public ^:final ^int/1 dest)
      (field ^:public ^:final ^int/1 tmp)
      (field ^:public ^:final ^Class/1 pcs)
      ;; the first four values in fields (CodeSeq)
      (field ^:public ^:final ^Code c0)
      (field ^:public ^:final ^Code c1)
      (field ^:public ^:final ^Code c2)
      (field ^:public ^:final ^Code c3)
      (constructor ^:public [this ^Code/1 cs ^int/1 kinds ^int/1 vkinds ^int/1 dest ^int/1 tmp
                             ^Class/1 pcs]
        (set! (.-cs this) cs)
        (set! (.-kinds this) kinds)
        (set! (.-vkinds this) vkinds)
        (set! (.-dest this) dest)
        (set! (.-tmp this) tmp)
        (set! (.-pcs this) pcs)
        (let [n (alength cs)]
          (set! (.-c0 this) (when (> n 0) (aget cs 0)))
          (set! (.-c1 this) (when (> n 1) (aget cs 1)))
          (set! (.-c2 this) (when (> n 2) (aget cs 2)))
          (set! (.-c3 this) (when (> n 3) (aget cs 3)))))
      (method code ^Code [this ^int i]
        (switch i 0 c0 1 c1 2 c2 3 c3 (aget cs i)))
      ;; value i for a long or double store: a long, or a double's raw bits
      (method bits ^long [this ^int i ^Frame f]
        (let [c (.code this i)
              vk (aget vkinds i)]
          (if (or (== (aget kinds i) 2) (== (aget kinds i) 4))
              (if (== vk 1) (.runLong c f) (CodeRun/toLong (.run c f)))
              (Double/doubleToRawLongBits
                (cond
                  (== vk 2) (.runDouble c f)
                  :else (CodeRun/toDouble (.run c f)))))))
      ;; value i for a slot store
      (method value [this ^int i ^Frame f]
        (let [v (.run (.code this i) f)]
          (if (== (aget kinds i) 1) (Evaluator/prim (aget pcs i) v) v)))
      ;; store i of a long (or double's bits) b, or of object v
      (method store ^void [this ^int i ^Frame f ^long b v]
        (let [k (aget kinds i)
              d (aget dest i)]
          (cond
            (< k 2) (aset (.-slots f) d v)
            (< k 4) (aset (.-prims f) d b)
            (== k 4) (aset (.-slots f) d (Long/valueOf b))
            :else (aset (.-slots f) d (Double/valueOf (Double/longBitsToDouble b))))))
      (method ^:public run [this ^Frame f]
        (let [n (alength cs)]
          (if (== n 1)
              (if (>= (aget kinds 0) 2)
                  (.store this 0 f (.bits this 0 f) nil)
                  (.store this 0 f 0 (.value this 0 f)))
              (do
                (loop [^int i 0]
                  (when (< i n)
                    (if (>= (aget kinds i) 2)
                        (aset (.-prims f) (aget tmp i) (.bits this i f))
                        (aset (.-slots f) (aget tmp i) (.value this i f)))
                    (recur (unchecked-inc-int i))))
                (loop [^int i 0]
                  (when (< i n)
                    (if (>= (aget kinds i) 2)
                        (.store this i f (aget (.-prims f) (aget tmp i)) nil)
                        (.store this i f 0 (aget (.-slots f) (aget tmp i))))
                    (recur (unchecked-inc-int i))))))
          (set! (.-recur f) true)
          nil))
      (method ^:public runLong ^long [this ^Frame f] (.run this f) 0)
      (method ^:public runDouble ^double [this ^Frame f] (.run this f) 0.0)
      (method ^:public runBool ^boolean [this ^Frame f] (.run this f) false)))

  (c2g/add
    (defclass ^:public ^:static CodeLetFn
      :extends Code
      ;; the fns made first, then each one's closed-over letfn locals filled (emitLetFnInits)
      (field ^:public ^:final ^Code/1 inits)
      (field ^:public ^:final ^int/1 dest)
      (field ^:public ^:final ^Object/1 patchJ)
      (field ^:public ^:final ^Object/1 patchK)
      (field ^:public ^:final ^Code body)
      (constructor ^:public [this ^Code/1 inits ^int/1 dest ^Object/1 patchJ ^Object/1 patchK ^Code body]
        (set! (.-inits this) inits)
        (set! (.-dest this) dest)
        (set! (.-patchJ this) patchJ)
        (set! (.-patchK this) patchK)
        (set! (.-body this) body))
      (method bind ^void [this ^Frame f]
        (let [n (alength inits)
              slots (.-slots f)]
          (loop [^int i 0]
            (when (< i n)
              (aset slots (aget dest i) nil)
              (recur (unchecked-inc-int i))))
          (loop [^int i 0]
            (when (< i n)
              (aset slots (aget dest i) (.run (aget inits i) f))
              (recur (unchecked-inc-int i))))
          (loop [^int i 0]
            (when (< i n)
              (let [v (aget slots (aget dest i))]
                (when (instance? EvalFn v)
                  (let [cl (.-closed (cast EvalFn v))
                        js (cast int/1 (aget patchJ i))
                        ks (cast int/1 (aget patchK i))]
                    (loop [^int x 0]
                      (when (< x (alength js))
                        (aset cl (aget js x) (aget slots (aget dest (aget ks x))))
                        (recur (unchecked-inc-int x)))))))
              (recur (unchecked-inc-int i))))))
      (method ^:public run [this ^Frame f] (.bind this f) (.run body f))
      (method ^:public runLong ^long [this ^Frame f] (.bind this f) (.runLong body f))
      (method ^:public runDouble ^double [this ^Frame f] (.bind this f) (.runDouble body f))
      (method ^:public runBool ^boolean [this ^Frame f] (.bind this f) (.runBool body f))))

  (c2g/add
    (defclass ^:public ^:static CodeTry
      :extends Code
      (field ^:public ^:final ^Code body)
      (field ^:public ^:final ^Class/1 classes)
      (field ^:public ^:final ^int/1 slots)
      (field ^:public ^:final ^Code/1 handlers)
      (field ^:public ^:final ^Code fin)
      (constructor ^:public [this ^Code body ^Class/1 classes ^int/1 slots ^Code/1 handlers ^Code fin]
        (set! (.-body this) body)
        (set! (.-classes this) classes)
        (set! (.-slots this) slots)
        (set! (.-handlers this) handlers)
        (set! (.-fin this) fin))
      ;; the clause catching t, or -1
      (method clause ^int [this ^Throwable t]
        (loop [^int i 0]
          (if (< i (alength classes))
              (if (.isInstance (aget classes i) t) i (recur (unchecked-inc-int i)))
              -1)))
      (method ^:public run [this ^Frame f]
        (if (and (nil? fin) (== (alength classes) 0))
            (.run body f)
            (try
              (.run body f)
              (catch Throwable t
                (let [i (.clause this t)]
                  (when (< i 0) (throw t))
                  (aset (.-slots f) (aget slots i) t)
                  (.run (aget handlers i) f)))
              (finally
                (when (some? fin) (.run fin f))))))))

  (c2g/add
    (defclass ^:public ^:static CodeThrow
      :extends Code
      (field ^:public ^:final ^Code c)
      (constructor ^:public [this ^Code c] (set! (.-c this) c))
      (method ^:public run [this ^Frame f]
        (throw (cast Throwable (.run c f))))))

  (c2g/add
    (defclass ^:public ^:static CodeMonitor
      :extends Code
      (field ^:public ^:final ^boolean enter)
      (field ^:public ^:final ^Code c)
      (constructor ^:public [this ^boolean enter ^Code c] (set! (.-enter this) enter) (set! (.-c this) c))
      (method ^:public run [this ^Frame f]
        (if enter
            (Evaluator/monitorEnter (.run c f))
            (Evaluator/monitorExit (.run c f)))
        nil)))

  (c2g/add
    (defclass ^:public ^:static CodeCase
      :extends Code
      ;; CaseExpr.doEmit's switch: the key's then when its test holds, else the default
      (field ^:public ^:final ^CaseExpr ce)
      (field ^:public ^:final ^Code expr)
      (field ^:public ^:final ^Code dflt)
      (field ^:public ^:final ^{:tag (HashMap Integer Object)} thens)
      (field ^:public ^:final ^{:tag (HashMap Integer Object)} tests)
      (field ^:public ^:final ^Class pc)
      (constructor ^:public [this ^CaseExpr ce ^Code expr ^Code dflt
                             ^{:tag (HashMap Integer Object)} thens
                             ^{:tag (HashMap Integer Object)} tests ^Class pc]
        (set! (.-ce this) ce)
        (set! (.-expr this) expr)
        (set! (.-dflt this) dflt)
        (set! (.-thens this) thens)
        (set! (.-tests this) tests)
        (set! (.-pc this) pc))
      (method choose ^Code [this ^Frame f]
        (let [v (.run expr f)
              k (Evaluator/caseKey ce v)]
          (if (or (nil? k) (not (.containsKey tests k)))
              dflt
              (let [test (.get tests k)
                    then (cast Code (.get thens k))
                    tt (.-testType ce)]
                (cond
                  (identical? tt CaseExpr/intKey)
                    (cond
                      (nil? pc) (if (Util/equiv v test) then dflt)
                      (identical? pc Long/TYPE)
                        (if (== (.longValue (cast Number test)) (.longValue (cast Number v))) then dflt)
                      (and (not (== (.-mask ce) 0))
                           (not (== (.longValue (cast Number test)) (.longValue (cast Number v)))))
                        dflt
                      :else then)
                  (RT/booleanCast (RT/contains (.-skipCheck ce) k)) then
                  (identical? tt CaseExpr/hashIdentityKey) (if (identical? v test) then dflt)
                  :else (if (Util/equiv v test) then dflt))))))
      (method ^:public run [this ^Frame f] (.run (.choose this f) f))
      (method ^:public runLong ^long [this ^Frame f] (.runLong (.choose this f) f))
      (method ^:public runDouble ^double [this ^Frame f] (.runDouble (.choose this f) f))
      (method ^:public runBool ^boolean [this ^Frame f] (.runBool (.choose this f) f))))

  ;; values

  (c2g/add
    (defclass ^:public ^:static CodeFn
      :extends Code
      ;; a fn: its closed-over values read in the creating frame
      (field ^:public ^:final ^FnExpr fe)
      (field ^:public ^:final ^Code/1 fetch)
      (constructor ^:public [this ^FnExpr fe ^Code/1 fetch] (set! (.-fe this) fe) (set! (.-fetch this) fetch))
      (method ^:public run [this ^Frame f]
        (EvalFn. fe (CodeRun/runAll fetch f)))))

  (c2g/add
    (defclass ^:public ^:static CodeReify
      :extends Code
      (field ^:public ^:final ^NewInstanceExpr nie)
      (field ^:public ^:final ^Code/1 fetch)
      (constructor ^:public [this ^NewInstanceExpr nie ^Code/1 fetch] (set! (.-nie this) nie) (set! (.-fetch this) fetch))
      (method ^:public run [this ^Frame f]
        (let [n (alength fetch)
              fs (new Object/1 (unchecked-inc-int n))]
          (loop [^int i 0]
            (when (< i n)
              (aset fs i (.run (aget fetch i) f))
              (recur (unchecked-inc-int i))))
          (Dyn/newInstance (.getCompiledClass nie) fs)))))

  (c2g/add
    (defclass ^:public ^:static CodeMeta
      :extends Code
      (field ^:public ^:final ^Code c)
      (field ^:public ^:final ^Code meta)
      (constructor ^:public [this ^Code c ^Code meta] (set! (.-c this) c) (set! (.-meta this) meta))
      (method ^:public run [this ^Frame f]
        (let [o (.run c f)]
          (.withMeta (cast IObj o) (cast IPersistentMap (.run meta f)))))))

  (c2g/add
    (defclass ^:public ^:static CodeColl
      :extends Code
      ;; kind 0 a vector, 1 a list, 2 a map, 3 a set
      (field ^:public ^:final ^int kind)
      (field ^:public ^:final ^Code/1 cs)
      (constructor ^:public [this ^int kind ^Code/1 cs] (set! (.-kind this) kind) (set! (.-cs this) cs))
      (method ^:public run [this ^Frame f]
        (cond
          (< kind 2)
            (let [n (alength cs)]
              (loop [^int i 0 ^IPersistentVector ret PersistentVector/EMPTY]
                (if (< i n)
                    (recur (unchecked-inc-int i) (.cons ret (.run (aget cs i) f)))
                    (if (== kind 0) ret (.seq ret)))))
          (== kind 2) (RT/map (CodeRun/runAll cs f))
          :else (RT/set (CodeRun/runAll cs f))))))

  (c2g/add
    (defclass ^:public ^:static CodeInstanceOf
      :extends Code
      (field ^:public ^:final ^Class c)
      (field ^:public ^:final ^boolean stub)
      (field ^:public ^:final ^Code e)
      ;; (deftype's stub class, in the methods of the type being defined: the class, looked up
      ;; when the test runs)
      (constructor ^:public [this ^Class c ^Code e]
        (set! (.-stub this) (.startsWith (.getName c) COMPILE_STUB_PREFIX))
        (set! (.-c this) c)
        (set! (.-e this) e))
      (method ^:public runBool ^boolean [this ^Frame f]
        (.isInstance (if stub (Evaluator/destub c) c) (.run e f)))
      (method ^:public run [this ^Frame f]
        (if (.runBool this f) RT/T RT/F))))

  (c2g/add
    (defclass ^:public ^:static CodeStrConcat
      :extends Code
      ;; (str x y ...) as the bytecode concatenates (StrConcatExpr.emit): a constant's text as
      ;; folded; the values all computed first, then each converted as str converts it (nil to
      ;; "", else its toString; the first one's null toString a NullPointerException, as
      ;; (StringBuilder. nil) throws in str)
      (field ^:public ^:final ^Code/1 cs)
      (field ^:public ^:final ^String/1 texts)
      (constructor ^:public [this ^Code/1 cs ^String/1 texts]
        (set! (.-cs this) cs)
        (set! (.-texts this) texts))
      (method ^:public run [this ^Frame f]
        (let [n (alength cs)
              vs (new Object/1 n)
              sb (StringBuilder.)]
          (loop [^int i 0]
            (when (< i n)
              (when (nil? (aget texts i))
                (aset vs i (.run (aget cs i) f)))
              (recur (unchecked-inc-int i))))
          (loop [^int i 0]
            (when (< i n)
              (let [t (aget texts i)]
                (if (some? t)
                    (.append sb t)
                    (let [v (aget vs i)]
                      (when (some? v)
                        (let [x (.toString v)]
                          (when (and (nil? x) (== i 0)) (throw (NullPointerException.)))
                          (.append sb x))))))
              (recur (unchecked-inc-int i))))
          (.toString sb)))))

  (c2g/add
    (defclass ^:public ^:static CodeDef
      :extends Code
      ;; a def inside a fn, as DefExpr.emit compiles it
      (field ^:public ^:final ^Var v)
      (field ^:public ^:final ^boolean isDynamic)
      (field ^:public ^:final ^Code meta)
      (field ^:public ^:final ^boolean initProvided)
      (field ^:public ^:final ^Code init)
      (constructor ^:public [this ^Var v ^boolean isDynamic ^Code meta ^boolean initProvided ^Code init]
        (set! (.-v this) v)
        (set! (.-isDynamic this) isDynamic)
        (set! (.-meta this) meta)
        (set! (.-initProvided this) initProvided)
        (set! (.-init this) init))
      (method ^:public run [this ^Frame f]
        (when isDynamic (.setDynamic v true))
        (when (some? meta)
          (.setMeta v (cast IPersistentMap (.run meta f))))
        (when initProvided (.bindRoot v (.run init f)))
        v)))

  (c2g/add
    (defclass ^:public ^:static CodeAssign
      :extends Code
      ;; set!: kind 0 a var, 1 a deftype's field (j), 2 a closed-over local of a fn (an error), 3 a
      ;; slot or prims local (j), 4 an instance field, 5 a static field, 6 evalAssign
      (field ^:public ^:final ^int kind)
      (field ^:public ^:final target)
      (field ^:public ^:final ^Code v)
      (field ^:public ^:final ^Code t)
      (field ^:public ^:final ^int j)
      (field ^:public ^:final ^Expr ve)
      (constructor ^:public [this ^int kind target ^Code v ^Code t ^int j ^Expr ve]
        (set! (.-kind this) kind)
        (set! (.-target this) target)
        (set! (.-v this) v)
        (set! (.-t this) t)
        (set! (.-j this) j)
        (set! (.-ve this) ve))
      (method ^:public run [this ^Frame f]
        (cond
          (== kind 0) (.set (cast Var target) (.run v f))
          (== kind 1) (let [x (.run v f)] (Dyn/setField (.-self f) j x) x)
          (== kind 2) (throw (IllegalArgumentException.
                               (java-str "Cannot assign to non-mutable: " (.-name (cast LocalBinding target)))))
          (== kind 3) (let [x (.run v f)] (aset (.-slots f) j x) x)
          (== kind 4)
            (let [ife (cast InstanceFieldExpr target)
                  o (.run t f)
                  tc (.-targetClass ife)]
              (if (and (some? tc) (some? (.-field ife))
                       (not (identical? (Evaluator/destub tc) tc)))
                  (let [x (.run v f)]
                    (Evaluator/checkCast (Evaluator/destub tc) o)
                    (when (nil? o) (throw (NullPointerException.)))
                    (Dyn/setField o (Evaluator/fieldIndex tc (.-fieldName ife)) x)
                    x)
                  (Reflector/setInstanceField o (.-fieldName ife) (.run v f))))
          (== kind 5)
            (let [sfe (cast StaticFieldExpr target)]
              (Reflector/setStaticField (.-c sfe) (.-fieldName sfe) (.run v f)))
          :else (.evalAssign (cast AssignableExpr target) ve)))))

  ;; calls

  (c2g/add
    (defclass ^:public ^:static CodeInvoke
      :extends Code
      ;; a call of a fn value: up to six arguments in fields (CodeSeq), more in args
      (field ^:public ^:final ^Code fexpr)
      (field ^:public ^:final ^Code/1 args)
      (field ^:public ^:final ^int n)
      (field ^:public ^:final ^int line)
      (field ^:public ^:final ^Code a0)
      (field ^:public ^:final ^Code a1)
      (field ^:public ^:final ^Code a2)
      (field ^:public ^:final ^Code a3)
      (field ^:public ^:final ^Code a4)
      (field ^:public ^:final ^Code a5)
      (constructor ^:public [this ^Code fexpr ^Code/1 args ^int line]
        (set! (.-fexpr this) fexpr)
        (set! (.-args this) args)
        (set! (.-n this) (alength args))
        (set! (.-line this) line)
        (set! (.-a0 this) (when (> (alength args) 0) (aget args 0)))
        (set! (.-a1 this) (when (> (alength args) 1) (aget args 1)))
        (set! (.-a2 this) (when (> (alength args) 2) (aget args 2)))
        (set! (.-a3 this) (when (> (alength args) 3) (aget args 3)))
        (set! (.-a4 this) (when (> (alength args) 4) (aget args 4)))
        (set! (.-a5 this) (when (> (alength args) 5) (aget args 5))))
      (method ^:public run [this ^Frame f]
        (let [fv (cast IFn (.run fexpr f))]
          (switch n
            0 (do (set! (.-line f) line) (.invoke fv))
            1 (let [x0 (.run a0 f)] (set! (.-line f) line) (.invoke fv x0))
            2 (let [x0 (.run a0 f) x1 (.run a1 f)] (set! (.-line f) line) (.invoke fv x0 x1))
            3 (let [x0 (.run a0 f) x1 (.run a1 f) x2 (.run a2 f)]
                (set! (.-line f) line)
                (.invoke fv x0 x1 x2))
            4 (let [x0 (.run a0 f) x1 (.run a1 f) x2 (.run a2 f) x3 (.run a3 f)]
                (set! (.-line f) line)
                (.invoke fv x0 x1 x2 x3))
            5 (let [x0 (.run a0 f) x1 (.run a1 f) x2 (.run a2 f) x3 (.run a3 f) x4 (.run a4 f)]
                (set! (.-line f) line)
                (.invoke fv x0 x1 x2 x3 x4))
            6 (let [x0 (.run a0 f) x1 (.run a1 f) x2 (.run a2 f) x3 (.run a3 f) x4 (.run a4 f)
                    x5 (.run a5 f)]
                (set! (.-line f) line)
                (.invoke fv x0 x1 x2 x3 x4 x5))
            (let [vs (CodeRun/runAll args f)]
              (set! (.-line f) line)
              (switch n
                7 (.invoke fv (aget vs 0) (aget vs 1) (aget vs 2) (aget vs 3) (aget vs 4) (aget vs 5)
                           (aget vs 6))
                8 (.invoke fv (aget vs 0) (aget vs 1) (aget vs 2) (aget vs 3) (aget vs 4) (aget vs 5)
                           (aget vs 6) (aget vs 7))
                (.applyTo fv (RT/seq vs)))))))))

  (c2g/add
    (defclass ^:public ^:static CodeKeywordInvoke
      :extends Code
      (field ^:public ^:final ^Keyword k)
      (field ^:public ^:final ^Code target)
      (field ^:public ^:final ^int line)
      (constructor ^:public [this ^Keyword k ^Code target ^int line]
        (set! (.-k this) k)
        (set! (.-target this) target)
        (set! (.-line this) line))
      (method ^:public run [this ^Frame f]
        (let [t (.run target f)]
          (set! (.-line f) line)
          (.invoke k t)))))

  ;; host calls (jrt's stack traces leave out the reflective frames above them: isEvalInterop)

  (c2g/add
    (defclass ^:public ^:static CodeHostStatic
      :extends Code
      ;; a resolved static method not listed for CodeOpN: through its invoker
      (field ^:public ^:final ^java.lang.reflect.Method m)
      (field ^:public ^:final ^Class/1 ps)
      (field ^:public ^:final ^Code/1 args)
      (field ^:public ^:final ^int line)
      ;; Evaluator.invokeResolved's path when the method's class is public, else Reflector's
      (field ^:public ^:final ^boolean direct)
      (field ^:public ^:final ^List ms)
      (constructor ^:public [this ^java.lang.reflect.Method m ^Code/1 args ^int line]
        (set! (.-m this) m)
        (set! (.-ps this) (.getParameterTypes m))
        (set! (.-args this) args)
        (set! (.-line this) line)
        (set! (.-direct this) (Evaluator/directOk m))
        (set! (.-ms this) (CodeRun/one m)))
      (method ^:public run [this ^Frame f]
        (let [vs (CodeRun/runAll args f)]
          (set! (.-line f) line)
          (CodeRun/call m ps ms direct nil vs)))))

  (c2g/add
    (defclass ^:public ^:static CodeHostStaticReflect
      :extends Code
      (field ^:public ^:final ^Class c)
      (field ^:public ^:final ^String name)
      (field ^:public ^:final ^Code/1 args)
      (field ^:public ^:final ^int line)
      (constructor ^:public [this ^Class c ^String name ^Code/1 args ^int line]
        (set! (.-c this) c)
        (set! (.-name this) name)
        (set! (.-args this) args)
        (set! (.-line this) line))
      (method ^:public run [this ^Frame f]
        (let [vs (CodeRun/runAll args f)]
          (set! (.-line f) line)
          (Reflector/invokeStaticMethod c name vs)))))

  (c2g/add
    (defclass ^:public ^:static CodeHostInstance
      :extends Code
      ;; a resolved instance method: the compiled call's checkcast of the target to the method's
      ;; class, then its invoker (virtual)
      (field ^:public ^:final ^Code target)
      (field ^:public ^:final ^java.lang.reflect.Method m)
      (field ^:public ^:final ^Class dc)
      (field ^:public ^:final ^Class/1 ps)
      (field ^:public ^:final ^Code/1 args)
      (field ^:public ^:final ^int line)
      (field ^:public ^:final ^boolean direct)
      (field ^:public ^:final ^List ms)
      (constructor ^:public [this ^Code target ^java.lang.reflect.Method m ^Code/1 args ^int line]
        (set! (.-target this) target)
        (set! (.-m this) m)
        (set! (.-dc this) (.getDeclaringClass m))
        (set! (.-ps this) (.getParameterTypes m))
        (set! (.-args this) args)
        (set! (.-line this) line)
        (set! (.-direct this) (Evaluator/directOk m))
        (set! (.-ms this) (CodeRun/one m)))
      (method ^:public run [this ^Frame f]
        (let [t (.run target f)
              vs (CodeRun/runAll args f)]
          (set! (.-line f) line)
          (Evaluator/checkCast dc t)
          (CodeRun/call m ps ms direct t vs)))))

  (c2g/add
    (defclass ^:public ^:static CodeHostInstanceReflect
      :extends Code
      (field ^:public ^:final ^Code target)
      (field ^:public ^:final ^Class qc)
      (field ^:public ^:final ^String name)
      (field ^:public ^:final ^Code/1 args)
      (field ^:public ^:final ^int line)
      (constructor ^:public [this ^Code target ^Class qc ^String name ^Code/1 args ^int line]
        (set! (.-target this) target)
        (set! (.-qc this) qc)
        (set! (.-name this) name)
        (set! (.-args this) args)
        (set! (.-line this) line))
      (method ^:public run [this ^Frame f]
        (let [t (.run target f)
              vs (CodeRun/runAll args f)]
          (set! (.-line f) line)
          (if (some? qc)
              (Reflector/invokeInstanceMethodOfClass t qc name vs)
              (Reflector/invokeInstanceMethod t name vs))))))

  (c2g/add
    (defclass ^:public ^:static CodeHostNew
      :extends Code
      (field ^:public ^:final ^Class c)
      (field ^:public ^:final ^java.lang.reflect.Constructor ctor)
      (field ^:public ^:final ^Code/1 args)
      (constructor ^:public [this ^Class c ^java.lang.reflect.Constructor ctor ^Code/1 args]
        (set! (.-c this) c)
        (set! (.-ctor this) ctor)
        (set! (.-args this) args))
      (method ^:public run [this ^Frame f]
        (let [vs (CodeRun/runAll args f)
              dc (Evaluator/destub c)]
          (if (some? ctor)
              (let [k (if (identical? dc c)
                          ctor
                          (.getConstructor dc (.getParameterTypes ctor)))]
                (CodeRun/construct k (CodeRun/directArgs (.getParameterTypes k) vs)))
              (Reflector/invokeConstructor dc vs))))))

  (c2g/add
    (defclass ^:public ^:static CodeHostField
      :extends Code
      ;; a resolved field as the bytecode reads it (checkcast to its class, getfield), else by
      ;; reflection
      (field ^:public ^:final ^InstanceFieldExpr fe)
      (field ^:public ^:final ^Code target)
      (constructor ^:public [this ^InstanceFieldExpr fe ^Code target]
        (set! (.-fe this) fe)
        (set! (.-target this) target))
      (method ^:public run [this ^Frame f]
        (let [t (.run target f)
              tc (.-targetClass fe)]
          (cond
            (or (nil? tc) (nil? (.-field fe)))
              (Reflector/invokeNoArgInstanceMember t (.-fieldName fe) (.-requireField fe))
            (not (identical? (Evaluator/destub tc) tc))
              (do
                (Evaluator/checkCast (Evaluator/destub tc) t)
                (when (nil? t) (throw (NullPointerException.)))
                (Dyn/getField t (Evaluator/fieldIndex tc (.-fieldName fe))))
            :else
              (do
                (Evaluator/checkCast tc t)
                (when (nil? t) (throw (NullPointerException.)))
                (.get (.-field fe) t))))))))
