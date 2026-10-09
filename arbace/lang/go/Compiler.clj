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

  ;; load-file: java.io.File is outside the Go build's world; the same path arithmetic on the
  ;; name (an absolute path from the working directory, the name after the last /)
  (method ^:public ^:static loadFile :throws [IOException] [^String file]
    (let [f (FileInputStream. file)
          abs (if (.startsWith file "/")
                  file
                  (java-str (System/getProperty "user.dir") "/" file))
          slash (.lastIndexOf file "/")]
      (try
        (arbace.lang.Compiler/load (InputStreamReader. f RT/UTF8)
                                   abs
                                   (if (>= slash 0) (.substring file (unchecked-inc-int slash)) file))
        (finally (.close f)))))

  ;; ---------------------------------------------------------------------------------------
  ;; The evaluator (doc/go/EVAL-PLAN.md, doc/go/EVAL-NOTES.md). Every Expr has evalIn(Frame), a
  ;; default method of the interface calling the node's own eval (right for the nodes that
  ;; hold no locals: constants, vars, keywords, imports); the node kinds that can hold locals
  ;; override it below (their variants). A Frame is the slots of one invocation of a fn's or a
  ;; deftype's method, indexed by LocalBinding.idx; a closed-over local is read from the
  ;; EvalFn's captured values or the Dyn object's fields, in the order of ObjExpr.closes.

  (c2g/add
    (defclass ^:public ^:static Frame
      ;; the locals of one invocation, by LocalBinding.idx
      (field ^:public ^:final ^Object/1 slots)
      ;; the fn whose method runs (its closed-over values); nil in a deftype's method
      (field ^:public ^:final ^EvalFn fn)
      (field ^:public ^:final ^ObjMethod method)
      ;; the FnExpr or NewInstanceExpr whose method runs
      (field ^:public ^:final ^ObjExpr objx)
      ;; a deftype's or reify's method: the object (its fields are the closed-over values)
      (field ^:public ^:final self)
      ;; where the frame is: the line and source of the call being evaluated (stack traces)
      (field ^:public ^int line)
      (field ^:public ^String source)
      ;; the calling frame on this thread
      (field ^:public ^Frame caller)

      (constructor ^:public [this ^int n ^EvalFn fn ^ObjMethod method ^ObjExpr objx self]
        (set! (.-slots this) (new Object/1 n))
        (set! (.-fn this) fn)
        (set! (.-method this) method)
        (set! (.-objx this) objx)
        (set! (.-self this) self)
        (set! (.-line this) (.-line method)))

      ;; the i-th closed-over value
      (method ^:public closed [this ^int i]
        (if (some? fn)
            (aget (.-closed fn) i)
            (Dyn/getField self i)))))

  (c2g/add
    (defclass ^:public ^:static EvalFn
      :extends RestFn
      ;; a fn* value: the analyzed FnExpr and the values of the locals it closes over, in the
      ;; order of (keys (.-closes fe)). One class serves every arity, fixed or variadic: it is a
      ;; RestFn of required arity 0, so every invoke arrives at doInvoke with its arguments as
      ;; a seq (EVAL-PLAN.md, Q3: per-arity invoke methods are step 7's)
      (field ^:public ^:final ^FnExpr fe)
      (field ^:public ^:final ^Object/1 closed)
      ;; the fn's class, named as the JVM names the fn's compiled class (arbace.core$map,
      ;; user$eval12$fn__13), a subclass of EvalFn made at run time: getClass answers it (c2g's
      ;; rule for a field c2g$class; EVAL-NOTES.md)
      (field ^:public ^:final ^Class c2g$class)

      (constructor ^:public [this ^FnExpr fe ^Frame f]
        (set! (.-fe this) fe)
        (set! (.-c2g$class this) (Evaluator/fnClass fe))
        (set! (.-closed this) (Evaluator/capture fe f)))

      (method ^:public getRequiredArity ^int [this] 0)

      (method ^:protected doInvoke [this args]
        (Evaluator/invokeFn this (RT/seqToArray (RT/seq args))))))

  (c2g/add
    (defclass ^:public ^:static EvalMethod
      :extends RestFn
      ;; a method of a deftype, defrecord or reify (a NewInstanceMethod), as the fn Dyn's
      ;; dispatch calls with the object and the arguments
      (field ^:public ^:final ^NewInstanceExpr nie)
      (field ^:public ^:final ^NewInstanceMethod m)

      (constructor ^:public [this ^NewInstanceExpr nie ^NewInstanceMethod m]
        (set! (.-nie this) nie)
        (set! (.-m this) m))

      (method ^:public getRequiredArity ^int [this] 0)

      (method ^:protected doInvoke [this args]
        (Evaluator/invokeMethod this (RT/seqToArray (RT/seq args))))))

  (c2g/add
    (defclass ^:public ^:static EvalState
      ;; one thread's evaluator: the innermost frame and the depth (C2G-SPEC §10.5, §10.7)
      (field ^:public ^Frame top)
      (field ^:public ^int depth)))

  (c2g/add
    (defclass ^:public ^:static Dyn
      ;; classes made at run time (deftype, defrecord, reify; C2G-SPEC §5.12, §10.4): their
      ;; instances are values of Go's Dyn, a type c2g generates with a method for every method
      ;; of every interface of the closed world, each calling the fn set for it here. The
      ;; natives are c2g's (arbace/lang's c2g_dyn.go)

      ;; a class extending Object, implementing the interfaces (closed-world ones dispatch
      ;; through Go's Dyn methods, run-time ones through the class's member table), with
      ;; public fields of these names
      (method ^:public ^:static ^:native defineClass ^Class [^String name ^Class/1 interfaces
                                                             ^String/1 fieldNames])

      ;; the implementation of method name(params)ret: impl is called with the object and
      ;; the arguments (boxed), its result converted to ret as compiled deftype methods convert
      (method ^:public ^:static ^:native setMethod ^void [^Class c ^String name ^Class/1 params
                                                          ^Class ret ^IFn impl])

      ;; a static method of a class made at run time (a deftype's getBasis, a record's create):
      ;; impl is called with the arguments (boxed)
      (method ^:public ^:static ^:native setStaticMethod ^void [^Class c ^String name
                                                                ^Class/1 params ^Class ret
                                                                ^IFn impl])

      ;; a constructor taking params; the object's fields are the arguments (boxed) followed
      ;; by tail (a record's constructors without its hidden fields)
      (method ^:public ^:static ^:native defineCtor ^void [^Class c ^Class/1 params
                                                           ^Object/1 tail])

      ;; an interface made at run time (definterface, defprotocol: C2G-SPEC §10.4), extending
      ;; the interfaces; its methods are added by addInterfaceMethod
      (method ^:public ^:static ^:native defineInterface ^Class [^String name ^Class/1 extends])

      ;; an abstract method of an interface made at run time: called (by reflection) on an
      ;; object of a class made at run time, it calls that class's method of the same name and
      ;; descriptor
      (method ^:public ^:static ^:native addInterfaceMethod ^void [^Class c ^String name
                                                                   ^Class/1 params ^Class ret])

      ;; the class of an evaluated fn: a subclass of super (EvalFn) named name, made at run time
      (method ^:public ^:static ^:native defineFnClass ^Class [^String name ^Class super])

      (method ^:public ^:static ^:native newInstance [^Class c ^Object/1 fieldValues])

      (method ^:public ^:static ^:native getField [o ^int i])

      (method ^:public ^:static ^:native setField ^void [o ^int i v])))

  (c2g/add
    (defclass ^:public ^:static Evaluator
      ;; what RecurExpr returns to its loop or fn method
      (field ^:public ^:static ^:final RECUR (Object.))

      ;; the deepest nesting of evaluated fn and method calls on one thread before
      ;; StackOverflowError (EVAL-PLAN.md Q4; C2G-SPEC §10.5)
      (field ^:public ^:static ^:final ^int MAX_DEPTH 10000)

      (field ^:static ^:final ^ThreadLocal STATE (ThreadLocal.))

      (static-initializer
        ;; stack traces: jrt asks for the evaluated frames when an exception is made
        (Evaluator/setTraceHook
          (anon java.util.function.Supplier []
            (method ^:public get [this] (Evaluator/trace)))))

      (method ^:static ^:native setTraceHook ^void [^java.util.function.Supplier s])

      (method ^:public ^:static ^:native monitorEnter ^void [o])

      (method ^:public ^:static ^:native monitorExit ^void [o])

      (method ^:public ^:static state ^EvalState []
        (let [^:mutable s (cast EvalState (.get STATE))]
          (when (nil? s)
            (set! s (EvalState.))
            (.set STATE s))
          s))

      ;; the evaluated frames of this thread, innermost first, as StackTraceElements: the fn's
      ;; or the type's class name, its method, the source file and the line being evaluated
      (method ^:public ^:static trace []
        (let [s (cast EvalState (.get STATE))
              ^{:tag (ArrayList StackTraceElement)} es (ArrayList.)]
          (when (some? s)
            (loop [fr (.-top s)]
              (when (some? fr)
                (.add es (StackTraceElement. (.-name (.-objx fr))
                                             (if (some? (.-fn fr))
                                                 "invoke"
                                                 (.-name (cast NewInstanceMethod (.-method fr))))
                                             (.-source fr)
                                             (.-line fr)))
                (recur (.-caller fr)))))
          (.toArray es (new StackTraceElement/1 (.size es)))))

      (method ^:static push ^EvalState [^Frame fr]
        (let [s (Evaluator/state)]
          (set! (.-caller fr) (.-top s))
          (let [c (.-top s)] (when (some? c) (set! (.-source fr) (.-source c))))
          (set! (.-top s) fr)
          (set! (.-depth s) (unchecked-inc-int (.-depth s)))
          (when (> (.-depth s) MAX_DEPTH)
            (set! (.-top s) (.-caller fr))
            (set! (.-depth s) (unchecked-dec-int (.-depth s)))
            (throw (StackOverflowError.)))
          s))

      (method ^:static pop ^void [^EvalState s ^Frame fr]
        (set! (.-top s) (.-caller fr))
        (set! (.-depth s) (unchecked-dec-int (.-depth s))))

      ;; the line and source of the form being evaluated in frame f (stack traces)
      (method ^:public ^:static at ^void [^Frame f ^int line ^String source]
        (when (some? f)
          (set! (.-line f) line)
          (when (some? source) (set! (.-source f) source))))

      ;; the class of fe's fns (cached on the FnExpr)
      (method ^:public ^:static fnClass ^Class [^FnExpr fe]
        (let [^:mutable c (.-evalClass fe)]
          (when (nil? c)
            (set! c (Dyn/defineFnClass (.-name fe) EvalFn))
            (set! (.-evalClass fe) c))
          c))

      ;; the locals objx closes over, in the order of its closes (cached on the ObjExpr)
      (method ^:public ^:static closes ^LocalBinding/1 [^ObjExpr objx]
        (let [^:mutable bs (.-evalCloses objx)]
          (when (nil? bs)
            (let [ks (RT/keys (.-closes objx))
                  n (RT/count ks)
                  a (new LocalBinding/1 n)]
              (loop [s ks ^int i 0]
                (when (some? s)
                  (aset a i (cast LocalBinding (.first s)))
                  (recur (.next s) (unchecked-inc-int i))))
              (set! (.-evalCloses objx) a)
              (set! bs a)))
          bs))

      ;; where a local is in frame f: k > 0 the slot k - 1, k < 0 the closed-over value
      ;; -k - 1 (the analyzer gives every expression one method, so the answer is cached)
      ;; (By identity among the closed-over locals, not by the method's locals map: a direct
      ;; fn's parameters are renumbered after they are entered there, and LocalBinding's hash
      ;; depends on the number)
      (method ^:public ^:static where ^int [^LocalBinding lb ^Frame f]
        (when (nil? f) (throw (UnsupportedOperationException. "Can't eval locals")))
        (let [bs (Evaluator/closes (.-objx f))]
          (loop [^int i 0]
            (if (< i (alength bs))
                (if (identical? (aget bs i) lb)
                    (unchecked-subtract-int -1 i)
                    (recur (unchecked-inc-int i)))
                (unchecked-inc-int (.-idx lb))))))

      (method ^:public ^:static local [^LocalBinding lb ^Frame f]
        (let [k (Evaluator/where lb f)]
          (if (> k 0)
              (aget (.-slots f) (unchecked-dec-int k))
              (.closed f (unchecked-subtract-int -1 k)))))

      ;; the values objx (a fn* or a reify being made in frame f) closes over
      (method ^:public ^:static capture ^Object/1 [^ObjExpr objx ^Frame f]
        (let [bs (Evaluator/closes objx)
              n (alength bs)
              vs (new Object/1 n)]
          (when (> n 0)
            (let [^:mutable ws (.-evalWhere objx)]
              (when (nil? ws)
                (set! ws (new int/1 n))
                (loop [^int i 0]
                  (when (< i n)
                    (aset ws i (Evaluator/where (aget bs i) f))
                    (recur (unchecked-inc-int i))))
                (set! (.-evalWhere objx) ws))
              (loop [^int i 0]
                (when (< i n)
                  (let [k (aget ws i)]
                    (aset vs i (if (> k 0)
                                   (aget (.-slots f) (unchecked-dec-int k))
                                   (.closed f (unchecked-subtract-int -1 k)))))
                  (recur (unchecked-inc-int i))))))
          vs))

      (method ^:public ^:static arg [^IPersistentVector es ^int i ^Frame f]
        (.evalIn (cast Expr (.nth es i)) f))

      (method ^:public ^:static args ^Object/1 [^IPersistentVector es ^Frame f]
        (let [n (.count es)
              vs (new Object/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (aset vs i (.evalIn (cast Expr (.nth es i)) f))
              (recur (unchecked-inc-int i))))
          vs))

      ;; a value for a local of primitive type c (EVAL-PLAN.md §3: primitives are boxed; the
      ;; conversions the bytecode does on a store)
      (method ^:public ^:static prim [^Class c v]
        (cond
          (nil? c) v
          (identical? c Long/TYPE) (if (instance? Long v) v (Numbers/num (RT/longCast v)))
          (identical? c Double/TYPE) (if (instance? Double v) v (Double/valueOf (RT/doubleCast v)))
          (identical? c Integer/TYPE) (if (instance? Integer v) v (Integer/valueOf (RT/intCast v)))
          (identical? c Float/TYPE) (if (instance? Float v) v (Float/valueOf (RT/floatCast v)))
          :else v))

      ;; the arguments of a resolved method or constructor as the compiled call passes them
      ;; (MethodExpr.emitTypedArgs, HostExpr.emitUnboxArg): a primitive parameter's argument
      ;; cast to Number (Boolean, Character) and converted by RT's checked casts, a reference
      ;; parameter's cast to its class (a fn passed for a functional interface is left to
      ;; Reflector's adapter): ClassCastException and NullPointerException as compiled code
      ;; throws them. (*unchecked-math*'s unchecked casts are not distinguished: EVAL-NOTES.md.)
      (method ^:public ^:static typedArgs ^Object/1 [^Class/1 ps ^Object/1 vs]
        (loop [^int i 0]
          (when (and (< i (alength ps)) (< i (alength vs)))
            (let [p (aget ps i)
                  v (aget vs i)]
              (cond
                (.isPrimitive p)
                  (do
                    (when (nil? v) (throw (NullPointerException.)))
                    (aset vs i
                          (cond
                            (identical? p Boolean/TYPE) (cast Boolean v)
                            (identical? p Character/TYPE) (cast Character v)
                            :else (let [n (cast Number v)]
                                    (cond
                                      (identical? p Integer/TYPE) (Integer/valueOf (RT/intCast n))
                                      (identical? p Long/TYPE) (Long/valueOf (RT/longCast n))
                                      (identical? p Double/TYPE) (Double/valueOf (RT/doubleCast n))
                                      (identical? p Float/TYPE) (Float/valueOf (RT/floatCast n))
                                      (identical? p Short/TYPE) (Short/valueOf (RT/shortCast n))
                                      (identical? p Byte/TYPE) (Byte/valueOf (RT/byteCast n))
                                      :else n)))))
                (and (instance? IFn v) (.isInterface p) (not (.isInstance p v))) nil
                :else (.cast p v)))
            (recur (unchecked-inc-int i))))
        vs)

      ;; a call of a reflected method or constructor, its exception unwrapped as compiled code
      ;; would have thrown it
      (method ^:public ^:static unwrap ^Throwable [^Throwable e]
        (if (and (instance? java.lang.reflect.InvocationTargetException e) (some? (.getCause e)))
            (.getCause e)
            e))

      ;; a constant as the compiled class's static initializer makes it (ObjExpr.emitValue):
      ;; collections rebuilt (a seq as a PersistentList, a vector, a map by RT.map, a hash set),
      ;; their elements and metadata likewise; other values themselves
      (method ^:public ^:static constant [v]
        (cond
          (nil? v) nil
          (instance? Boolean v) (if (.booleanValue (cast Boolean v)) Boolean/TRUE Boolean/FALSE)
          (or (instance? IRecord v) (instance? IType v)) v
          (not (or (instance? IPersistentMap v) (instance? IPersistentVector v)
                   (instance? PersistentHashSet v) (instance? ISeq v) (instance? IPersistentList v)))
            v
          :else
            (let [r (cond
                      (instance? IPersistentMap v)
                        (let [^{:tag (ArrayList Object)} kvs (ArrayList.)]
                          (for-each [^Map$Entry e (.entrySet (cast Map v))]
                            (.add kvs (Evaluator/constant (.getKey e)))
                            (.add kvs (Evaluator/constant (.getValue e))))
                          (RT/map (.toArray kvs)))
                      (instance? IPersistentVector v)
                        (^[Object/1] RT/vector (Evaluator/constants (RT/toArray v)))
                      (instance? PersistentHashSet v)
                        (if (nil? (RT/seq v))
                            PersistentHashSet/EMPTY
                            (PersistentHashSet/create (Evaluator/constants (RT/toArray v))))
                      :else
                        (PersistentList/create (Arrays/asList (Evaluator/constants (RT/seqToArray (RT/seq v))))))
                  m (when (instance? IObj v) (.meta (cast IObj v)))]
              (if (> (RT/count m) 0)
                  (.withMeta (cast IObj r)
                             (cast IPersistentMap (Evaluator/constant (arbace.lang.Compiler/elideMeta m))))
                  r))))

      (method ^:static constants ^Object/1 [^Object/1 vs]
        (loop [^int i 0]
          (when (< i (alength vs))
            (aset vs i (Evaluator/constant (aget vs i)))
            (recur (unchecked-inc-int i))))
        vs)

      ;; the value of e in frame f (nil: no frame, a top-level form)
      (method ^:public ^:static eval [^Expr e ^Frame f] (.evalIn e f))

      ;; a class of a deftype's methods: the class being defined (deftype's stub class)
      (method ^:public ^:static destub ^Class [^Class c]
        (if (.startsWith (.getName c) COMPILE_STUB_PREFIX)
            (Class/forName (arbace.lang.Compiler/destubClassName (.getName c)))
            c))

      ;; a call of an evaluated fn: the method of the arity, its parameters bound in a new
      ;; frame, its body run until it does not recur
      (method ^:public ^:static invokeFn [^EvalFn fn ^Object/1 args]
        (let [fe (.-fe fn)
              n (alength args)
              ^:mutable ^FnMethod m nil]
          (loop [s (RT/seq (.-methods fe))]
            (when (some? s)
              (let [fm (cast FnMethod (.first s))]
                (if (and (nil? (.-restParm fm)) (== (.count (.-reqParms fm)) n))
                    (set! m fm)
                    (recur (.next s))))))
          (when (and (nil? m) (some? (.-variadicMethod fe))
                     (>= n (.count (.-reqParms (.-variadicMethod fe)))))
            (set! m (.-variadicMethod fe)))
          (when (nil? m) (throw (ArityException. n (.-name fe))))
          (let [f (Frame. (unchecked-add-int (.-maxLocal m) 2) fn m fe nil)
                slots (.-slots f)
                req (.-reqParms m)
                nreq (.count req)
                pcs (.-argclasses m)]
            (when-not (.-canBeDirect fe) (aset slots 0 fn))
            (loop [^int i 0]
              (when (< i nreq)
                (aset slots (.-idx (cast LocalBinding (.nth req i)))
                      (if (some? pcs) (Evaluator/prim (aget pcs i) (aget args i)) (aget args i)))
                (recur (unchecked-inc-int i))))
            (when (some? (.-restParm m))
              (aset slots (.-idx (.-restParm m))
                    (when (> n nreq)
                      (ArraySeq/create (^[Object/1 int int] Arrays/copyOfRange args nreq n)))))
            (let [s (Evaluator/push f)]
              (try
                (let [rc (.-retClass m)
                      r (loop []
                          (let [r (.evalIn (.-body m) f)]
                            (if (identical? r RECUR) (recur) r)))]
                  (if (and (some? rc) (.isPrimitive rc)) (Evaluator/prim rc r) r))
                (finally (Evaluator/pop s f)))))))

      ;; a call of a deftype's or reify's method: args[0] is the object (this, slot 0)
      (method ^:public ^:static invokeMethod [^EvalMethod em ^Object/1 args]
        (let [m (.-m em)
              f (Frame. (unchecked-add-int (.-maxLocal m) 2) nil m (.-nie em) (aget args 0))
              slots (.-slots f)
              ps (.-argLocals m)
              n (.count ps)]
          (when-not (== (alength args) (unchecked-inc-int n))
            (throw (ArityException. (unchecked-dec-int (alength args))
                                    (java-str (.-name (.-nie em)) "." (.-name m)))))
          (aset slots 0 (aget args 0))
          (loop [^int i 0]
            (when (< i n)
              (let [lb (cast LocalBinding (.nth ps i))]
                (aset slots (.-idx lb)
                      (Evaluator/prim (.-c (cast MethodParamExpr (.-init lb)))
                                      (aget args (unchecked-inc-int i)))))
              (recur (unchecked-inc-int i))))
          (let [s (Evaluator/push f)]
            (try
              (loop []
                (let [r (.evalIn (.-body m) f)]
                  (if (identical? r RECUR) (recur) r)))
              (finally (Evaluator/pop s f))))))

      ;; the parameter classes of a deftype's method
      (method ^:static paramClasses ^Class/1 [^NewInstanceMethod m]
        (let [ps (.-argLocals m)
              a (new Class/1 (.count ps))]
          (loop [^int i 0]
            (when (< i (alength a))
              (aset a i (.-c (cast MethodParamExpr (.-init (cast LocalBinding (.nth ps i))))))
              (recur (unchecked-inc-int i))))
          a))

      ;; deftype, defrecord, reify: the class made at run time, at analysis as on the JVM
      ;; (ObjExpr.compile); its fields are the NewInstanceExpr's closes (a deftype's fields, a
      ;; reify's closed-over locals), then for a reify its meta
      (method ^:public ^:static defineType ^Class [^NewInstanceExpr nie ^String/1 interfaceNames]
        (let [ni (if (nil? interfaceNames) 0 (alength interfaceNames))
              ifaces (new Class/1 ni)
              bs (Evaluator/closes nie)
              meta (.supportsMeta nie)
              nf (if meta (unchecked-inc-int (alength bs)) (alength bs))
              names (new String/1 nf)]
          (loop [^int i 0]
            (when (< i ni)
              (aset ifaces i (RT/classForName (.replace (aget interfaceNames i) \/ \.)))
              (recur (unchecked-inc-int i))))
          (loop [^int i 0]
            (when (< i (alength bs))
              (aset names i (.-name (aget bs i)))
              (recur (unchecked-inc-int i))))
          (when meta (aset names (alength bs) "__meta"))
          (let [c (Dyn/defineClass (.-name nie) ifaces names)]
            ;; methods, with every covariant return they answer to
            (loop [s (RT/seq (.-methods nie))]
              (when (some? s)
                (let [m (cast NewInstanceMethod (.first s))
                      ps (Evaluator/paramClasses m)
                      impl (EvalMethod. nie m)]
                  (Dyn/setMethod c (.-name m) ps (.-retClass m) impl)
                  (when (some? (.-covariants nie))
                    (let [cvs (cast java.util.Set
                                    (.get (.-covariants nie)
                                          (^[Object/1] RT/vector (.-name m) (RT/seq ps))))]
                      (when (some? cvs)
                        (for-each [^Class rc cvs]
                          (Dyn/setMethod c (.-name m) ps rc impl)))))
                  (recur (.next s)))))
            (if (.isDeftype nie)
                (let [nh (.count (.-hintedFields nie))
                      basis (.-hintedFields nie)]
                  (Evaluator/defineCtors c nie bs)
                  (Dyn/setStaticMethod c "getBasis" (new Class/1 0) IPersistentVector
                                       (anon AFn []
                                         (method ^:public invoke [this]
                                           (Evaluator/constant basis))))
                  (when (> (.count (.-fields nie)) nh)
                    (Dyn/setStaticMethod c "create" (new Class/1 [IPersistentMap]) c
                                         (anon AFn []
                                           (method ^:public invoke [this m]
                                             (Evaluator/create c basis bs (cast IPersistentMap m)))))))
                ;; reify: meta and withMeta over the last field
                (let [n (alength bs)]
                  (Dyn/setMethod c "meta" (new Class/1 0) IPersistentMap
                                 (anon AFn []
                                   (method ^:public invoke [this o] (Dyn/getField o n))))
                  (Dyn/setMethod c "withMeta" (new Class/1 [IPersistentMap]) IObj
                                 (anon AFn []
                                   (method ^:public invoke [this o mm]
                                     (let [vs (new Object/1 (unchecked-inc-int n))]
                                       (loop [^int i 0]
                                         (when (< i n)
                                           (aset vs i (Dyn/getField o i))
                                           (recur (unchecked-inc-int i))))
                                       (aset vs n mm)
                                       (Dyn/newInstance c vs)))))))
            c)))

      ;; a deftype's constructors: all fields, and for a record those without its hidden
      ;; fields (__meta __extmap __hash __hasheq, then __hash __hasheq) as the JVM's stub has
      (method ^:static defineCtors ^void [^Class c ^NewInstanceExpr nie ^LocalBinding/1 bs]
        (let [n (alength bs)
              ps (new Class/1 n)]
          (loop [^int i 0]
            (when (< i n)
              (let [pc (.getPrimitiveType (aget bs i))]
                (aset ps i (if (some? pc) pc Object)))
              (recur (unchecked-inc-int i))))
          (Dyn/defineCtor c ps (new Object/1 0))
          (when (> (.-altCtorDrops nie) 0)
            (Dyn/defineCtor c (cast Class/1 (^[Object/1 int int] Arrays/copyOfRange ps 0 (unchecked-subtract-int n (.-altCtorDrops nie))))
                            (new Object/1 [nil nil (Integer/valueOf 0) (Integer/valueOf 0)]))
            (Dyn/defineCtor c (cast Class/1 (^[Object/1 int int] Arrays/copyOfRange ps 0 (unchecked-subtract-int n 2)))
                            (new Object/1 [(Integer/valueOf 0) (Integer/valueOf 0)])))))

      ;; a record's create(IPersistentMap): the basis fields taken from m, the rest its extmap
      ;; (ObjExpr.emitStatics)
      (method ^:static create [^Class c ^IPersistentVector basis ^LocalBinding/1 bs
                               ^IPersistentMap m]
        (let [n (.count basis)
              vs (new Object/1 (alength bs))
              ^:mutable ^IPersistentMap rest m]
          (loop [^int i 0]
            (when (< i n)
              (let [k (Keyword/intern (.-name (cast Symbol (.nth basis i))))]
                (aset vs i (Evaluator/prim (.getPrimitiveType (aget bs i)) (.valAt m k)))
                (set! rest (.without rest k)))
              (recur (unchecked-inc-int i))))
          (aset vs n nil)
          (aset vs (unchecked-inc-int n) (RT/seqOrElse rest))
          (aset vs (unchecked-add-int n 2) (Integer/valueOf 0))
          (aset vs (unchecked-add-int n 3) (Integer/valueOf 0))
          (Dyn/newInstance c vs)))

      ;; a reify (or a deftype analyzed in an expression: nil) in frame f
      (method ^:public ^:static newInstance [^NewInstanceExpr nie ^Frame f]
        (if (.isDeftype nie)
            nil
            (let [vs (Evaluator/capture nie f)
                  n (alength vs)
                  fs (new Object/1 (unchecked-inc-int n))]
              (System/arraycopy vs 0 fs 0 n)
              (Dyn/newInstance (.getCompiledClass nie) fs))))

      ;; the clause of a try catching t, or nil
      (method ^:public ^:static catchClause ^Compiler$TryExpr$CatchClause [^TryExpr te ^Throwable t]
        (let [cs (.-catchExprs te)
              n (.count cs)]
          (loop [^int i 0]
            (if (< i n)
                (let [c (cast Compiler$TryExpr$CatchClause (.nth cs i))]
                  (if (.isInstance (.-c c) t) c (recur (unchecked-inc-int i))))
                nil))))

      ;; case*: the key of the switch (CaseExpr.doEmit), Integer, or nil for the default
      (method ^:public ^:static caseKey ^Integer [^CaseExpr ce v]
        (let [pc (arbace.lang.Compiler/maybePrimitiveType (.-expr ce))
              ^:mutable ^int k 0]
          (if (identical? (.-testType ce) CaseExpr/intKey)
              (cond
                (nil? pc)
                  (if (instance? Number v)
                      (set! k (.intValue (cast Number v)))
                      (return nil))
                (or (identical? pc Long/TYPE) (identical? pc Integer/TYPE)
                    (identical? pc Short/TYPE) (identical? pc Byte/TYPE))
                  (set! k (unchecked-int (.longValue (cast Number v))))
                :else (return nil))
              (set! k (Util/hash v)))
          (when-not (== (.-mask ce) 0)
            (set! k (unchecked-int (bit-and (bit-shift-right k (.-shift ce)) (.-mask ce)))))
          (Integer/valueOf k)))))

)

(c2g/variant Compiler$Expr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f] (.eval this))))

(c2g/variant Compiler$ObjExpr
  ;; no class is generated: the Expr tree is evaluated (C2G-SPEC §10.1). A deftype's or a
  ;; reify's class is made at run time (Compiler$Dyn), at analysis as the JVM loads it
  (method compile :throws [IOException] ^void [this ^String superName ^String/1 interfaceNames
                                               ^boolean oneTimeUse]
    (when (instance? NewInstanceExpr this)
      (set! compiledClass (Compiler$Evaluator/defineType (cast NewInstanceExpr this) interfaceNames))))

  (method ^:synchronized getCompiledClass ^Class [this] compiledClass)

  ;; a fn* evaluated as a top-level form: a fn without a frame (it closes over nothing); a
  ;; deftype: nil; a reify: its object
  (method ^:public eval [this]
    (if (instance? FnExpr this)
        (Compiler$EvalFn. (cast FnExpr this) nil)
        (Compiler$Evaluator/newInstance (cast NewInstanceExpr this) nil)))

  ;; the evaluator's caches: the closed-over locals in order, and where the creating frame
  ;; has each (Evaluator.where's encoding)
  (c2g/add (field ^:public ^LocalBinding/1 evalCloses))
  (c2g/add (field ^:public ^int/1 evalWhere))
  (c2g/add (field ^:public ^Class evalClass))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (if (instance? FnExpr this)
          (Compiler$EvalFn. (cast FnExpr this) f)
          (Compiler$Evaluator/newInstance (cast NewInstanceExpr this) f)))))

(c2g/variant Compiler$NewInstanceExpr
  ;; the stub class giving this and the fields a type while the methods are analyzed: a class
  ;; made at run time with the interfaces and the fields (no methods), which the evaluator maps
  ;; to the class itself where code names it (Evaluator.destub)
  (method ^:static compileStub ^Class [^String superName ^NewInstanceExpr ret
                                       ^String/1 interfaceNames frm]
    ;; (the closes of a reify are not known yet: its methods are analyzed after the stub)
    (let [ni (if (nil? interfaceNames) 0 (alength interfaceNames))
          ifaces (new Class/1 ni)
          ks (RT/keys (.-closes ret))
          bs (new LocalBinding/1 (RT/count ks))
          names (new String/1 (alength bs))]
      (loop [^int i 0]
        (when (< i ni)
          (aset ifaces i (RT/classForName (.replace (aget interfaceNames i) \/ \.)))
          (recur (unchecked-inc-int i))))
      (loop [s ks ^int i 0]
        (when (some? s)
          (aset bs i (cast LocalBinding (.first s)))
          (aset names i (.-name (aget bs i)))
          (recur (.next s) (unchecked-inc-int i))))
      (let [c (Compiler$Dyn/defineClass (java-str COMPILE_STUB_PREFIX "." (.-name ret)) ifaces names)]
        (when (.isDeftype ret) (Compiler$Evaluator/defineCtors c ret bs))
        c))))

(c2g/variant Compiler$FnMethod
  ;; primitive fns are evaluated boxed (EVAL-PLAN.md Q1, C2G-SPEC §10.2): no fn has a
  ;; primitive interface, so callers call invoke; the evaluator converts ^long and ^double
  ;; parameters and returns
  (method ^:public ^:static primInterface ^String [^IPersistentVector arglist] nil))

(c2g/variant Compiler$LocalBindingExpr
  (c2g/add
    ;; where the local is (Evaluator.where's encoding; 0: not known yet)
    (field ^int evalWhere))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [k evalWhere]
        (cond
          (> k 0) (aget (.-slots f) (unchecked-dec-int k))
          (< k 0) (.closed f (unchecked-subtract-int -1 k))
          :else (do
                  (set! evalWhere (Compiler$Evaluator/where b f))
                  (.evalIn this f))))))
  (c2g/add
    ;; set! of a local: a deftype's mutable field, in a method of the deftype
    (method ^:public assignIn [this ^Compiler$Frame f v]
      (let [k (Compiler$Evaluator/where b f)]
        (cond
          (> k 0) (aset (.-slots f) (unchecked-dec-int k) v)
          (some? (.-self f)) (Compiler$Dyn/setField (.-self f) (unchecked-subtract-int -1 k) v)
          :else (throw (IllegalArgumentException.
                         (java-str "Cannot assign to non-mutable: " (.-name b)))))
        v))))

(c2g/variant Compiler$BodyExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [es exprs
            n (.count es)]
        (loop [^int i 0 ret nil]
          (if (< i n)
              (recur (unchecked-inc-int i) (.evalIn (cast Expr (.nth es i)) f))
              ret))))))

(c2g/variant Compiler$IfExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [t (.evalIn testExpr f)]
        (if (and (some? t) (not (identical? t Boolean/FALSE)))
            (.evalIn thenExpr f)
            (.evalIn elseExpr f))))))

(c2g/variant Compiler$LetExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [bis bindingInits
            n (.count bis)
            slots (.-slots f)]
        (loop [^int i 0]
          (when (< i n)
            (let [bi (cast BindingInit (.nth bis i))]
              (aset slots (.-idx (.-binding bi)) (.evalIn (.-init bi) f))
              (recur (unchecked-inc-int i)))))
        (if isLoop
            (loop []
              (let [r (.evalIn body f)]
                (if (identical? r Compiler$Evaluator/RECUR) (recur) r)))
            (.evalIn body f))))))

(c2g/variant Compiler$RecurExpr
  (c2g/add
    ;; the primitive types of the loop locals (cached)
    (field ^Class/1 evalPrims))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [vs (Compiler$Evaluator/args args f)
            lls loopLocals
            n (alength vs)
            slots (.-slots f)
            ^:mutable ps evalPrims]
        (when (nil? ps)
          (set! ps (new Class/1 n))
          (loop [^int i 0]
            (when (< i n)
              (aset ps i (.getPrimitiveType (cast LocalBinding (.nth lls i))))
              (recur (unchecked-inc-int i))))
          (set! evalPrims ps))
        (loop [^int i 0]
          (when (< i n)
            (aset slots (.-idx (cast LocalBinding (.nth lls i)))
                  (Compiler$Evaluator/prim (aget ps i) (aget vs i)))
            (recur (unchecked-inc-int i))))
        Compiler$Evaluator/RECUR))))

(c2g/variant Compiler$LetFnExpr
  (c2g/add
    ;; the fns made first, then each one's closed-over letfn locals filled (emitLetFnInits)
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [bis bindingInits
            n (.count bis)
            slots (.-slots f)]
        (loop [^int i 0]
          (when (< i n)
            (aset slots (.-idx (.-binding (cast BindingInit (.nth bis i)))) nil)
            (recur (unchecked-inc-int i))))
        (loop [^int i 0]
          (when (< i n)
            (let [bi (cast BindingInit (.nth bis i))]
              (aset slots (.-idx (.-binding bi)) (.evalIn (.-init bi) f))
              (recur (unchecked-inc-int i)))))
        (loop [^int i 0]
          (when (< i n)
            (let [v (aget slots (.-idx (.-binding (cast BindingInit (.nth bis i)))))]
              (when (instance? Compiler$EvalFn v)
                (let [fv (cast Compiler$EvalFn v)
                      cbs (Compiler$Evaluator/closes (.-fe fv))]
                  (loop [^int j 0]
                    (when (< j (alength cbs))
                      (loop [^int k 0]
                        (when (< k n)
                          (let [lb (.-binding (cast BindingInit (.nth bis k)))]
                            (if (identical? lb (aget cbs j))
                                (aset (.-closed fv) j (aget slots (.-idx lb)))
                                (recur (unchecked-inc-int k))))))
                      (recur (unchecked-inc-int j))))))
              (recur (unchecked-inc-int i)))))
        (.evalIn body f)))))

(c2g/variant Compiler$TryExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (try
        (.evalIn tryExpr f)
        (catch Throwable t
          (let [c (Compiler$Evaluator/catchClause this t)]
            (when (nil? c) (throw t))
            (aset (.-slots f) (.-idx (.-lb c)) t)
            (.evalIn (.-handler c) f)))
        (finally
          (when (some? finallyExpr) (.evalIn finallyExpr f)))))))

(c2g/variant Compiler$ThrowExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (throw (cast Throwable (.evalIn excExpr f))))))

(c2g/variant Compiler$MonitorEnterExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (Compiler$Evaluator/monitorEnter (.evalIn target f))
      nil)))

(c2g/variant Compiler$MonitorExitExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (Compiler$Evaluator/monitorExit (.evalIn target f))
      nil)))

(c2g/variant Compiler$InvokeExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [fv (cast IFn (.evalIn fexpr f))
            as args
            n (.count as)]
        (Compiler$Evaluator/at f line source)
        (switch n
          0 (.invoke fv)
          1 (.invoke fv (Compiler$Evaluator/arg as 0 f))
          2 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f))
          3 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f))
          4 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f) (Compiler$Evaluator/arg as 3 f))
          5 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f) (Compiler$Evaluator/arg as 3 f) (Compiler$Evaluator/arg as 4 f))
          6 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f) (Compiler$Evaluator/arg as 3 f) (Compiler$Evaluator/arg as 4 f) (Compiler$Evaluator/arg as 5 f))
          7 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f) (Compiler$Evaluator/arg as 3 f) (Compiler$Evaluator/arg as 4 f) (Compiler$Evaluator/arg as 5 f) (Compiler$Evaluator/arg as 6 f))
          8 (.invoke fv (Compiler$Evaluator/arg as 0 f) (Compiler$Evaluator/arg as 1 f) (Compiler$Evaluator/arg as 2 f) (Compiler$Evaluator/arg as 3 f) (Compiler$Evaluator/arg as 4 f) (Compiler$Evaluator/arg as 5 f) (Compiler$Evaluator/arg as 6 f) (Compiler$Evaluator/arg as 7 f))
          (.applyTo fv (RT/seq (Compiler$Evaluator/args as f))))))))

(c2g/variant Compiler$KeywordInvokeExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [t (.evalIn target f)]
        (Compiler$Evaluator/at f line source)
        (.invoke (.-k kw) t)))))

(c2g/variant Compiler$StaticMethodExpr
  (c2g/add
    (field ^List evalMethods))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [vs (Compiler$Evaluator/args args f)]
        (Compiler$Evaluator/at f line source)
        (if (some? method)
            (let [^:mutable ms evalMethods]
              (when (nil? ms)
                (set! ms (LinkedList.))
                (.add ms method)
                (set! evalMethods ms))
              (Reflector/invokeMatchingMethod methodName ms nil
                                              (Compiler$Evaluator/typedArgs (.getParameterTypes method) vs)))
            (Reflector/invokeStaticMethod c methodName vs))))))

(c2g/variant Compiler$InstanceMethodExpr
  (c2g/add
    (field ^List evalMethods))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [t (.evalIn target f)
            vs (Compiler$Evaluator/args args f)]
        (Compiler$Evaluator/at f line source)
        (cond
          (some? method)
            (let [^:mutable ms evalMethods]
              ;; the compiled call's checkcast to the method's class (ClassCastException, not
              ;; Method.invoke's IllegalArgumentException)
              (.cast (.getDeclaringClass method) t)
              (when (nil? ms)
                (set! ms (LinkedList.))
                (.add ms method)
                (set! evalMethods ms))
              (Reflector/invokeMatchingMethod methodName ms t
                                              (Compiler$Evaluator/typedArgs (.getParameterTypes method) vs)))
          (some? qualifyingClass)
            (Reflector/invokeInstanceMethodOfClass t qualifyingClass methodName vs)
          :else (Reflector/invokeInstanceMethod t methodName vs))))))

(c2g/variant Compiler$InstanceFieldExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (Reflector/invokeNoArgInstanceMember (.evalIn target f) fieldName requireField))))

(c2g/variant Compiler$StaticFieldExpr
  (c2g/add
    (method ^:public assignIn [this ^Compiler$Frame f v]
      (Reflector/setStaticField c fieldName v))))

(c2g/variant Compiler$AssignExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (cond
        (instance? VarExpr target)
          (.set (.-var (cast VarExpr target)) (.evalIn val f))
        (instance? LocalBindingExpr target)
          (.assignIn (cast LocalBindingExpr target) f (.evalIn val f))
        (instance? InstanceFieldExpr target)
          (let [ife (cast InstanceFieldExpr target)
                t (.evalIn (.-target ife) f)]
            (Reflector/setInstanceField t (.-fieldName ife) (.evalIn val f)))
        (instance? StaticFieldExpr target)
          (.assignIn (cast StaticFieldExpr target) f (.evalIn val f))
        :else (.evalAssign target val)))))

(c2g/variant Compiler$DefExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (when initProvided (.bindRoot var (.evalIn init f)))
      (when (some? meta)
        (.setMeta var (cast IPersistentMap (.evalIn meta f))))
      (.setDynamic var isDynamic))))

(c2g/variant Compiler$NewExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [vs (Compiler$Evaluator/args args f)
            dc (Compiler$Evaluator/destub c)]
        (if (some? ctor)
            (let [k (if (identical? dc c)
                        ctor
                        (.getConstructor dc (.getParameterTypes ctor)))]
              (try
                (.newInstance k (Reflector/boxArgs (.getParameterTypes k)
                                                   (Compiler$Evaluator/typedArgs (.getParameterTypes k) vs)))
                (catch Exception e
                  (throw (Util/sneakyThrow (Compiler$Evaluator/unwrap e))))))
            (Reflector/invokeConstructor dc vs))))))

(c2g/variant Compiler$ConstantExpr
  (c2g/add (field ^Object evalValue))
  (c2g/add (field ^boolean evalMade))
  (c2g/add
    ;; the constant as the compiled code holds it, made once (Evaluator.constant)
    (method ^:public evalIn [this ^Compiler$Frame f]
      (when-not evalMade
        (set! evalValue (Compiler$Evaluator/constant v))
        (set! evalMade true))
      evalValue)))

(c2g/variant Compiler$EmptyExpr
  (c2g/add
    ;; the type's empty value, as the compiled code loads it (EmptyExpr.emit), so that two
    ;; empty literals are identical
    (method ^:public evalIn [this ^Compiler$Frame f]
      (cond
        (instance? IPersistentList coll) PersistentList/EMPTY
        (instance? IPersistentVector coll) PersistentVector/EMPTY
        (instance? IPersistentMap coll) PersistentArrayMap/EMPTY
        (instance? IPersistentSet coll) PersistentHashSet/EMPTY
        :else (throw (UnsupportedOperationException. "Unknown Collection type"))))))

(c2g/variant Compiler$MetaExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [o (.evalIn expr f)]
        (.withMeta (cast IObj o) (cast IPersistentMap (.evalIn meta f)))))))

(c2g/variant Compiler$VectorExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [n (.count args)]
        (loop [^int i 0 ^IPersistentVector ret PersistentVector/EMPTY]
          (if (< i n)
              (recur (unchecked-inc-int i) (.cons ret (Compiler$Evaluator/arg args i f)))
              ret))))))

(c2g/variant Compiler$ListExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [n (.count args)]
        (loop [^int i 0 ^IPersistentVector ret PersistentVector/EMPTY]
          (if (< i n)
              (recur (unchecked-inc-int i) (.cons ret (Compiler$Evaluator/arg args i f)))
              (.seq ret)))))))

(c2g/variant Compiler$MapExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (RT/map (Compiler$Evaluator/args keyvals f)))))

(c2g/variant Compiler$SetExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (RT/set (Compiler$Evaluator/args keys f)))))

(c2g/variant Compiler$InstanceOfExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (if (.isInstance (Compiler$Evaluator/destub c) (.evalIn expr f)) RT/T RT/F))))

(c2g/variant Compiler$StrConcatExpr
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [vs (Compiler$Evaluator/args args f)]
        (.applyTo (cast IFn (.deref STR_VAR)) (RT/seq vs))))))

(c2g/variant Compiler$CaseExpr
  (c2g/add
    ;; CaseExpr.doEmit's switch: the key's then when its test holds, else the default
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [v (.evalIn expr f)
            k (Compiler$Evaluator/caseKey this v)]
        (if (or (nil? k) (not (.containsKey tests k)))
            (.evalIn defaultExpr f)
            (let [test (cast Expr (.get tests k))
                  then (cast Expr (.get thens k))]
              (cond
                (identical? testType intKey)
                  (let [pc (arbace.lang.Compiler/maybePrimitiveType expr)]
                    (cond
                      (nil? pc)
                        (if (Util/equiv v (.eval test)) (.evalIn then f) (.evalIn defaultExpr f))
                      (identical? pc Long/TYPE)
                        (if (== (.longValue (cast Number (.eval test))) (.longValue (cast Number v)))
                            (.evalIn then f)
                            (.evalIn defaultExpr f))
                      (and (not (== mask 0))
                           (not (== (.longValue (cast Number (.eval test))) (.longValue (cast Number v)))))
                        (.evalIn defaultExpr f)
                      :else (.evalIn then f)))
                (RT/booleanCast (RT/contains skipCheck k)) (.evalIn then f)
                (identical? testType hashIdentityKey)
                  (if (identical? v (.eval test)) (.evalIn then f) (.evalIn defaultExpr f))
                :else
                  (if (Util/equiv v (.eval test)) (.evalIn then f) (.evalIn defaultExpr f)))))))))

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
