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
        (set! (.-line this) (.-line method))
        (set! (.-source this) (.-evalSource objx)))

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

      ;; the arguments as RestFn passes them: a seq, realized only as far as the method's
      ;; fixed parameters (apply of an infinite seq to a variadic fn, as on the JVM)
      (method ^:protected doInvoke [this args]
        (Evaluator/invokeFn this (RT/seq args)))))

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

      ;; field i of a class made at run time is private (reflection does not find it)
      (method ^:public ^:static ^:native hideField ^void [^Class c ^int i])

      (method ^:public ^:static ^:native setField ^void [o ^int i v])

      ;; proxy (arbace/lang/go/ns/core_proxy.clj): a class named name extending super (Object,
      ;; or a class c2g gives a proxy type, arbace.c2g.dyn/proxy-supers) and implementing the
      ;; interfaces, with super's constructors and one private field, the fn map (field 0). Its
      ;; methods are set by setMethod; a method's fn answering superMarker makes the method
      ;; run super's implementation
      (method ^:public ^:static ^:native defineProxyClass ^Class [^String name ^Class super
                                                                  ^Class/1 interfaces])

      (method ^:public ^:static ^:native superMarker [])

      ;; a proxy's superclass methods that reflection does not list (the protected ones of
      ;; jrt's classes: ThreadLocal.initialValue) and that have no fn: (factory name) is theirs
      (method ^:public ^:static ^:native fillProxySlots ^void [^Class c ^IFn factory])))

  (c2g/add
    (defclass ^:public ^:static Evaluator
      ;; what RecurExpr returns to its loop or fn method
      (field ^:public ^:static ^:final RECUR (Object.))

      ;; the deepest nesting of evaluated fn and method calls on one thread before
      ;; StackOverflowError (EVAL-PLAN.md Q4; C2G-SPEC §10.5)
      (field ^:public ^:static ^:final ^int MAX_DEPTH 10000)

      (field ^:static ^:final ^ThreadLocal STATE (ThreadLocal.))

      ;; true while RT.load evaluates a source embedded in the program: the namespaces the
      ;; JVM ships AOT-compiled, whose top-level defs run as emitted (DefExpr.eval)
      (field ^:public ^:static ^:final ^Var EMBEDDED_LOAD (.setDynamic (Var/create false)))

      (static-initializer
        ;; stack traces: jrt asks for the evaluated frames when an exception is made
        (Evaluator/setTraceHook
          (anon java.util.function.Supplier []
            (method ^:public get [this] (Evaluator/trace)))))

      (method ^:static ^:native setTraceHook ^void [^java.util.function.Supplier s])

      (method ^:public ^:static ^:native monitorEnter ^void [o])

      ;; checkcast to a class known at run time: o, or ClassCastException with the JVM's
      ;; message (jrt.ClassCast)
      (method ^:public ^:static ^:native checkCast [^Class c o])

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
          (set! (.-line f) line)))

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
          ;; a char where the bytecode widens it (a primitive char hinted ^int)
          (and (instance? Character v) (.isPrimitive c) (not (identical? c Character/TYPE))
               (not (identical? c Boolean/TYPE)))
            (Evaluator/prim c (Integer/valueOf (int (.charValue (cast Character v)))))
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
                            :else (let [n (if (instance? Character v)
                                              ;; a primitive char the bytecode widens
                                              (Integer/valueOf (int (.charValue (cast Character v))))
                                              (cast Number v))]
                                    (cond
                                      (identical? p Integer/TYPE) (Integer/valueOf (RT/intCast n))
                                      (identical? p Long/TYPE) (Long/valueOf (RT/longCast n))
                                      (identical? p Double/TYPE) (Double/valueOf (RT/doubleCast n))
                                      (identical? p Float/TYPE) (Float/valueOf (RT/floatCast n))
                                      (identical? p Short/TYPE) (Short/valueOf (RT/shortCast n))
                                      (identical? p Byte/TYPE) (Byte/valueOf (RT/byteCast n))
                                      :else n)))))
                ;; a fn for a functional interface (FISupport's test, as the compiled call
                ;; adapts it); another interface is a checkcast
                (and (instance? IFn v) (.isInterface p) (not (.isInstance p v))
                     (some? (Compiler$FISupport/maybeFIMethod p)))
                  nil
                :else (Evaluator/checkCast p v)))
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

      ;; the index of field name among the fields of a class made at run time (its stub's
      ;; fields are all public, in order)
      (method ^:public ^:static fieldIndex ^int [^Class c ^String name]
        (let [fs (.getFields c)]
          (loop [^int i 0]
            (if (< i (alength fs))
                (if (.equals (.getName (aget fs i)) name) i (recur (unchecked-inc-int i)))
                (throw (IllegalArgumentException.
                         (java-str "No matching field found: " name " for class " (.getName c))))))))

      ;; a class of a deftype's methods: the class being defined (deftype's stub class)
      (method ^:public ^:static destub ^Class [^Class c]
        (if (.startsWith (.getName c) COMPILE_STUB_PREFIX)
            (Class/forName (arbace.lang.Compiler/destubClassName (.getName c)))
            c))

      ;; the largest fixed arity of fe's methods, or -1 (cached on the FnExpr)
      (method ^:static maxFixed ^int [^FnExpr fe]
        (let [^:mutable k (.-evalMaxFixed fe)]
          (when (== k 0)
            (set! k -1)
            (loop [s (RT/seq (.-methods fe))]
              (when (some? s)
                (let [fm (cast FnMethod (.first s))]
                  (when (and (nil? (.-restParm fm)) (> (.count (.-reqParms fm)) k))
                    (set! k (.count (.-reqParms fm)))))
                (recur (.next s))))
            ;; 0 means not known yet: store the arity plus one
            (set! (.-evalMaxFixed fe) (unchecked-inc-int k))
            (return k))
          (unchecked-dec-int k)))

      ;; a call of an evaluated fn with the arguments args (a seq, or nil): the method of the
      ;; arity (a fixed one first, then the variadic one), its parameters bound in a new frame
      ;; (the rest parameter the remaining seq, unrealized), its body run until it does not
      ;; recur, its result converted to a primitive return as the bytecode does
      (method ^:public ^:static invokeFn [^EvalFn fn ^ISeq args]
        (let [fe (.-fe fn)
              vm (.-variadicMethod fe)
              maxf (Evaluator/maxFixed fe)
              vreq (if (some? vm) (.count (.-reqParms vm)) -1)
              limit (if (> maxf vreq) maxf vreq)
              n (RT/boundedLength args limit)
              ^:mutable ^FnMethod m nil]
          (when (<= n maxf)
            (loop [s (RT/seq (.-methods fe))]
              (when (some? s)
                (let [fm (cast FnMethod (.first s))]
                  (if (and (nil? (.-restParm fm)) (== (.count (.-reqParms fm)) n))
                      (set! m fm)
                      (recur (.next s)))))))
          (when (and (nil? m) (some? vm) (>= n vreq))
            (set! m vm))
          (when (nil? m)
            (throw (ArityException. (if (instance? Counted args) (RT/count args) (RT/boundedLength args 20)) (.-name fe))))
          (let [f (Frame. (unchecked-add-int (.-maxLocal m) 2) fn m fe nil)
                slots (.-slots f)
                req (.-reqParms m)
                nreq (.count req)
                pcs (.-argclasses m)
                ^:mutable ^ISeq s args]
            (when-not (.-canBeDirect fe) (aset slots 0 fn))
            (loop [^int i 0]
              (when (< i nreq)
                (aset slots (.-idx (cast LocalBinding (.nth req i)))
                      (if (some? pcs) (Evaluator/prim (aget pcs i) (.first s)) (.first s)))
                (set! s (.next s))
                (recur (unchecked-inc-int i))))
            (when (some? (.-restParm m))
              (aset slots (.-idx (.-restParm m)) s))
            (let [st (Evaluator/push f)]
              (try
                (let [r (loop []
                          (let [r (.evalIn (.-body m) f)]
                            (if (identical? r RECUR) (recur) r)))]
                  (Evaluator/result (.-retClass m) (.-body m) r))
                (finally (Evaluator/pop st f)))))))

      ;; a method's result converted to its return class as the bytecode does
      ;; (ObjMethod.emitBody): a primitive body by RT's checked casts, any other unboxed
      ;; (Number.intValue ...), boxed again for the caller
      (method ^:public ^:static result [^Class rc ^Expr body r]
        (cond
          (or (nil? rc) (not (.isPrimitive rc))) r
          (identical? rc Void/TYPE) nil
          :else
            (let [bc (arbace.lang.Compiler/maybePrimitiveType body)]
              (cond
                (some? bc) (Evaluator/prim rc r)
                (identical? rc Boolean/TYPE) (if (.booleanValue (cast Boolean r)) Boolean/TRUE Boolean/FALSE)
                (identical? rc Character/TYPE) (cast Character r)
                :else
                  (let [x (cast Number r)]
                    (cond
                      (identical? rc Integer/TYPE) (Integer/valueOf (.intValue x))
                      (identical? rc Long/TYPE) (Long/valueOf (.longValue x))
                      (identical? rc Double/TYPE) (Double/valueOf (.doubleValue x))
                      (identical? rc Float/TYPE) (Float/valueOf (.floatValue x))
                      (identical? rc Short/TYPE) (Short/valueOf (.shortValue x))
                      (identical? rc Byte/TYPE) (Byte/valueOf (.byteValue x))
                      :else x))))))

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
              (let [r (loop []
                        (let [r (.evalIn (.-body m) f)]
                          (if (identical? r RECUR) (recur) r)))]
                (Evaluator/result (.-retClass m) (.-body m) r))
              (finally (Evaluator/pop s f))))))

      ;; a class's JVM descriptor (I, [Ljava/lang/String;, Ljava/lang/Object;)
      (method ^:static descriptor ^String [^Class c]
        (cond
          (.isPrimitive c) (cond (identical? c Boolean/TYPE) "Z" (identical? c Byte/TYPE) "B"
                                 (identical? c Character/TYPE) "C" (identical? c Short/TYPE) "S"
                                 (identical? c Integer/TYPE) "I" (identical? c Long/TYPE) "J"
                                 (identical? c Float/TYPE) "F" (identical? c Double/TYPE) "D"
                                 :else "V")
          (.isArray c) (.replace (.getName c) \. \/)
          :else (java-str "L" (.replace (.getName c) \. \/) ";")))

      ;; a method's JVM descriptor
      (method ^:static signature ^String [^Class/1 ps ^Class ret]
        (let [sb (StringBuilder. "(")]
          (loop [^int i 0]
            (when (< i (alength ps))
              (.append sb (Evaluator/descriptor (aget ps i)))
              (recur (unchecked-inc-int i))))
          (.append sb ")")
          (.append sb (Evaluator/descriptor ret))
          (.toString sb)))

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
            ;; a deftype's mutable fields are private (compileStub's are public: the methods
            ;; read them through the stub)
            (loop [^int i 0]
              (when (< i (alength bs))
                (when (.isMutable nie (aget bs i)) (Dyn/hideField c i))
                (recur (unchecked-inc-int i))))
            (when meta (Dyn/hideField c (alength bs)))
            ;; methods, with every covariant return they answer to; a method defined twice
            ;; is the JVM's ClassFormatError when it loads the class
            (loop [s (RT/seq (.-methods nie)) seen (java.util.HashSet.)]
              (when (some? s)
                (let [m (cast NewInstanceMethod (.first s))
                      ps (Evaluator/paramClasses m)
                      impl (EvalMethod. nie m)
                      sig (Evaluator/signature ps (.-retClass m))]
                  (when-not (.add seen (java-str (.-name m) sig))
                    (throw (ClassFormatError.
                             (java-str "Duplicate method name \"" (.-name m) "\" with signature \"" sig
                                       "\" in class file " (.replace (.-name nie) \. \/)))))
                  (Dyn/setMethod c (.-name m) ps (.-retClass m) impl)
                  (when (some? (.-covariants nie))
                    (let [cvs (cast java.util.Set
                                    (.get (.-covariants nie)
                                          (^[Object/1] RT/vector (.-name m) (RT/seq ps))))]
                      (when (some? cvs)
                        (for-each [^Class rc cvs]
                          (Dyn/setMethod c (.-name m) ps rc impl)))))
                  (recur (.next s) seen))))
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
    ;; the source file the fn or type is analyzed in (a compiled class's SourceFile): its
    ;; frames' file in stack traces
    (set! evalSource (cast String (.deref SOURCE)))
    (when (instance? NewInstanceExpr this)
      (Compiler$Image/event (new Object/1 ["type" name this interfaceNames]))
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
  (c2g/add (field ^:public ^String evalSource))
  (c2g/add (field ^:public ^int evalMaxFixed))
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
    (Compiler$Image/event (new Object/1 ["stub" (java-str COMPILE_STUB_PREFIX "." (.-name ret))
                                         superName ret interfaceNames frm]))
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
    ;; the local's primitive type (0: not known yet, 1: none, 2: long, 3: double)
    (field ^int evalPrim))
  (c2g/add
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [k evalWhere]
        (cond
          (> k 0) (.evalBox this (aget (.-slots f) (unchecked-dec-int k)))
          (< k 0) (.evalBox this (.closed f (unchecked-subtract-int -1 k)))
          :else (do
                  (set! evalWhere (Compiler$Evaluator/where b f))
                  (.evalIn this f))))))
  (c2g/add
    ;; a primitive local's value boxed anew at each use, as the bytecode boxes it where an
    ;; Object is wanted (LocalBindingExpr.emit: Double.valueOf, Long.valueOf with its cache), so
    ;; that (let [x ##NaN] (identical? x x)) is false as on the JVM
    (method evalBox [this v]
      (let [^:mutable p evalPrim]
        (when (== p 0)
          (let [c (.getPrimitiveType b)]
            (set! p (cond (identical? c Long/TYPE) 2 (identical? c Double/TYPE) 3 :else 1))
            (set! evalPrim p)))
        (cond
          (or (== p 1) (nil? v)) v
          (== p 2) (Long/valueOf (.longValue (cast Number v)))
          :else (Double/valueOf (.doubleValue (cast Number v)))))))
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

(c2g/variant Compiler$QualifiedMethodExpr
  ;; A method value (Class/method) is a fn the JVM analyzes when it emits the enclosing code
  ;; (buildThunk), so in the namespace being compiled. Its eval analyzes when it runs: in the
  ;; namespace recorded at analysis, once (the thunk's FnExpr is kept)
  (c2g/add (field ^Namespace evalNs))
  (c2g/add (field ^Compiler$FnExpr evalThunk))

  (constructor ^:public [this ^Class methodClass ^Symbol sym ^StaticFieldExpr fieldOL]
    (set! c methodClass)
    (set! methodSymbol sym)
    (set! tagClass
          (when (some? (arbace.lang.Compiler/tagOf sym))
            (HostExpr/tagToClass (arbace.lang.Compiler/tagOf sym))))
    (set! hintedSig (arbace.lang.Compiler/tagsToClasses (arbace.lang.Compiler/paramTagsOf sym)))
    (cond
      (.startsWith (.-name sym) ".")
        (do (set! kind Compiler$QualifiedMethodExpr$MethodKind/INSTANCE)
            (set! methodName (.substring (.-name sym) 1)))
      (.equals (.-name sym) "new")
        (do (set! kind Compiler$QualifiedMethodExpr$MethodKind/CTOR) (set! methodName (.-name sym)))
      :else (do (set! kind Compiler$QualifiedMethodExpr$MethodKind/STATIC) (set! methodName (.-name sym))))
    (set! fieldOverload fieldOL)
    (set! evalNs (cast Namespace (.deref RT/CURRENT_NS))))

  (method ^:public eval [this]
    (if (.preferOverloadedField this)
        (.eval fieldOverload)
        (let [^:mutable t evalThunk]
          (when (nil? t)
            (Var/pushThreadBindings (^[Object/1] RT/map RT/CURRENT_NS evalNs))
            (try
              (set! t (Compiler$QualifiedMethodExpr/buildThunk C/EVAL this))
              (finally (Var/popThreadBindings)))
            (set! evalThunk t))
          (.eval t)))))

(c2g/variant Compiler$StaticMethodExpr
  ;; *unchecked-math* :warn-on-boxed: jrt's reflection has no annotations. The methods of
  ;; Numbers marked ^{WarnBoxedMath false}, by name (each name's overloads are all marked)
  (method ^:public ^:static isBoxedMath ^boolean [^java.lang.reflect.Method m]
    (let [c (.getDeclaringClass m)]
      (when (.equals c Numbers)
        (when (.contains (java.util.Arrays/asList
                           (new String/1 ["boolean_array" "booleans" "byte_array" "bytes" "char_array"
                                          "chars" "double_array" "doubles" "float_array" "floats"
                                          "hasheq" "hasheqFrom" "int_array" "ints" "long_array" "longs"
                                          "rationalize" "reduceBigInt" "short_array" "shorts"
                                          "toBigDecimal" "toBigInt" "toBigInteger" "toRatio"]))
                         (.getName m))
          (return false))
        (let [argTypes (.getParameterTypes m)]
          (for-each [^Class argType argTypes]
            (when (or (.equals argType Object) (.equals argType Number)) (return true)))))
      false))
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
              (Compiler$Evaluator/checkCast (.getDeclaringClass method) t)
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
    ;; a resolved field as the bytecode reads it (checkcast to its class, getfield), else by
    ;; reflection
    (method ^:public evalIn [this ^Compiler$Frame f]
      (let [t (.evalIn target f)]
        (cond
          (or (nil? targetClass) (nil? field))
            (Reflector/invokeNoArgInstanceMember t fieldName requireField)
          ;; a field of the type being defined (its stub class, in its methods): the object's
          ;; field, private or not
          (not (identical? (Compiler$Evaluator/destub targetClass) targetClass))
            (do
              (Compiler$Evaluator/checkCast (Compiler$Evaluator/destub targetClass) t)
              (when (nil? t) (throw (NullPointerException.)))
              (Compiler$Dyn/getField t (Compiler$Evaluator/fieldIndex targetClass fieldName)))
          :else
            (do
              (Compiler$Evaluator/checkCast targetClass t)
              (when (nil? t) (throw (NullPointerException.)))
              (.get field t)))))))

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
                t (.evalIn (.-target ife) f)
                tc (.-targetClass ife)]
            (if (and (some? tc) (some? (.-field ife))
                     (not (identical? (Compiler$Evaluator/destub tc) tc)))
                ;; a field of the type being defined, in its methods ((set! (.x this) v)):
                ;; the object's field, mutable fields being private to reflection
                (let [v (.evalIn val f)]
                  (Compiler$Evaluator/checkCast (Compiler$Evaluator/destub tc) t)
                  (when (nil? t) (throw (NullPointerException.)))
                  (Compiler$Dyn/setField t (Compiler$Evaluator/fieldIndex tc (.-fieldName ife)) v)
                  v)
                (Reflector/setInstanceField t (.-fieldName ife) (.evalIn val f))))
        (instance? StaticFieldExpr target)
          (.assignIn (cast StaticFieldExpr target) f (.evalIn val f))
        :else (.evalAssign target val)))))

(c2g/variant Compiler$DefExpr
  ;; a top-level def: DefExpr.eval, but in a source embedded in the program (AOT-compiled on the
  ;; JVM) a def without ^:dynamic leaves the var's dynamic flag as it is, as DefExpr.emit does
  ;; (core_print's print-initialized redefines core's dynamic one), and a constant value or
  ;; metadata is the AOT-compiled namespace's (emitValue: a seq built by a macro, such as defn's
  ;; :arglists, is a PersistentList). The order stays eval's: emit's (meta, then bindRoot,
  ;; which drops :macro) is for code already macroexpanded
  (method ^:public eval [this]
    (try
      (let [aot (RT/booleanCast (.deref Compiler$Evaluator/EMBEDDED_LOAD))]
        (when initProvided
          (.bindRoot var (if (and aot (instance? Compiler$ConstantExpr init)) (.evalIn init nil) (.eval init))))
        (when (some? meta)
          (.setMeta var (cast IPersistentMap (if (and aot (instance? Compiler$ConstantExpr meta))
                                                 (.evalIn meta nil)
                                                 (.eval meta)))))
        (if (or isDynamic (not aot))
            (.setDynamic var isDynamic)
            var))
      (catch Throwable e
        (if (not (instance? CompilerException e))
            (throw (CompilerException. source line column arbace.lang.Compiler/DEF
                                       CompilerException/PHASE_EXECUTION e))
            (throw (cast CompilerException e))))))

  (c2g/add
    ;; a def inside a fn runs as DefExpr.emit compiles it
    (method ^:public evalIn [this ^Compiler$Frame f]
      (when isDynamic (.setDynamic var true))
      (when (some? meta)
        (.setMeta var (cast IPersistentMap (.evalIn meta f))))
      (when initProvided (.bindRoot var (.evalIn init f)))
      var)))

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

(c2g/variant Compiler$ImportExpr
  (c2g/add
    ;; the class, as the compiled import* leaves it (ImportExpr.emit; its eval gives nil)
    (method ^:public evalIn [this ^Compiler$Frame f]
      (.importClass (cast Namespace (.deref RT/CURRENT_NS)) (RT/classForNameNonLoading c)))))

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
  ;; CaseExpr's constructor, as in Compiler.clj, then the warning of emitExprForInts
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
          (new Object/1 [(.deref SOURCE_PATH) line column]))))
    ;; emitExprForInts' warning, which the JVM prints when it emits the case (the Go build
    ;; emits nothing): at analysis, after the constructor's own
    (when (and (identical? testType intKey)
               (nil? (arbace.lang.Compiler/maybePrimitiveType expr))
               (RT/booleanCast (.deref RT/WARN_ON_REFLECTION)))
      (.format
        (RT/errPrintWriter)
        "Performance warning, %s:%d:%d - case has int tests, but tested expression is not primitive.\n"
        (new Object/1 [(.deref SOURCE_PATH) line column]))))

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

;; ---------------------------------------------------------------------------------------
;; The prepared namespaces (B1a step 6, doc/go/EXEC-NOTES.md): the executable starts from
;; namespaces analyzed at build time. `bin/arbace-go --build` runs the executable once with
;; ARBACE_PREPARE set: each source embedded in the program that RT.load evaluates is then
;; recorded, unit by unit (a top-level form, or each form of a top-level do, as Compiler.eval
;; splits them), as the Expr tree its analysis made, with the classes made while it was
;; analyzed (deftype's stub and class, gen-interface's interfaces) as events; the program
;; writes the records (the image, Go's encoding of the object graph: the main package's
;; image.go) and the build embeds it. At run time RT.load replays an embedded source from the
;; image instead of reading, macroexpanding and analyzing it: each unit's events are made
;; again, then its Expr is evaluated as Compiler.eval evaluates it. Evaluation is the same,
;; analysis is skipped, as the JVM's AOT-compiled namespaces skip it.

(c2g/variant Compiler
  (c2g/add
    (defclass ^:public ^:static Image
      ;; the recorder of the source being loaded (Object[] {name, ArrayList events}) while
      ;; Compiler.load analyzes its forms; nil while they are evaluated
      (field ^:public ^:static ^:final ^Var RECORDER (.setDynamic (Var/create nil)))
      ;; the recorder RT.load hands Compiler.load (which takes it and binds this to nil)
      (field ^:public ^:static ^:final ^Var PENDING (.setDynamic (Var/create nil)))

      ;; 1 when the program records (ARBACE_PREPARE), else 0
      (method ^:public ^:static ^:native mode ^int [])
      ;; starts the record of source name (false: recorded already)
      (method ^:public ^:static ^:native begin ^boolean [^String name])
      ;; records a unit {kind line column lineBefore columnBefore expr events}
      (method ^:public ^:static ^:native unit ^void [^String name ^Object/1 u])
      ;; ends the record of source name, kept when ok
      (method ^:public ^:static ^:native end ^void [^String name ^boolean ok])
      ;; whether the image holds source name
      (method ^:public ^:static ^:native has ^boolean [^String name])
      ;; the next unit of source name from the image (opened on first use), nil at its end
      (method ^:public ^:static ^:native next ^Object/1 [^String name])
      ;; resolves the classes and members the units decoded so far name, when they exist, once
      ;; the first k events of the unit (which may make them) are made again (strict: all,
      ;; else IllegalStateException)
      (method ^:public ^:static ^:native resolve ^void [^String name ^int k ^boolean strict])

      ;; the program has started (Main.main, after arbace.main is loaded): the start's settings
      ;; end (the garbage collector's)
      (method ^:public ^:static ^:native started ^void [])

      ;; an event of the analysis being recorded
      (method ^:public ^:static event ^void [^Object/1 ev]
        (let [r (.deref RECORDER)]
          (when (some? r)
            (.add (cast ArrayList (aget (cast Object/1 r) 1)) ev))))

      ;; gen-interface's interfaces (genclass.clj), recorded
      (method ^:public ^:static defineInterface ^Class [^String name ^Class/1 extends]
        (Image/event (new Object/1 ["iface" name name extends]))
        (Compiler$Dyn/defineInterface name extends))

      (method ^:public ^:static addInterfaceMethod ^void [^Class c ^String name ^Class/1 params
                                                          ^Class ret]
        (Image/event (new Object/1 ["imethod" nil c name params ret]))
        (Compiler$Dyn/addInterfaceMethod c name params ret))

      ;; an event made again: {kind, the name of the class it makes or nil, args...}
      (method ^:static replay ^void [^Object/1 ev]
        (let [k (cast String (aget ev 0))]
          (cond
            (.equals k "stub")
              (NewInstanceExpr/compileStub (cast String (aget ev 2)) (cast NewInstanceExpr (aget ev 3))
                                           (cast String/1 (aget ev 4)) (aget ev 5))
            (.equals k "type")
              (let [nie (cast NewInstanceExpr (aget ev 2))]
                (set! (.-compiledClass nie) (Compiler$Evaluator/defineType nie (cast String/1 (aget ev 3)))))
            (.equals k "iface")
              (Compiler$Dyn/defineInterface (cast String (aget ev 2)) (cast Class/1 (aget ev 3)))
            (.equals k "proxy")
              (.applyTo (RT/var "arbace.core" "get-proxy-class")
                        (RT/cons (aget ev 2) (RT/seq (aget ev 3))))
            (.equals k "imethod")
              (Compiler$Dyn/addInterfaceMethod (cast Class (aget ev 2)) (cast String (aget ev 3))
                                               (cast Class/1 (aget ev 4)) (cast Class (aget ev 5)))
            :else (throw (IllegalStateException. (java-str "image: unknown event " k))))))

      ;; RT.load of an embedded source: replayed from the image when it holds it, else read
      ;; and evaluated by Compiler.load (recorded when the program prepares)
      (method ^:public ^:static load [^Reader rdr ^String sourcePath ^String sourceName]
        (when (some? (.deref RECORDER))
          ;; a load while a form is analyzed (Compiler.ensureMacroCheck's spec): the unit
          ;; being recorded will not load it when replayed
          (.println (RT/errPrintWriter)
                    (java-str "image: " sourcePath " loaded while "
                              (aget (cast Object/1 (.deref RECORDER)) 0) " is analyzed")))
        (cond
          (Image/has sourcePath) (Image/replayLoad sourcePath sourceName)
          (and (== (Image/mode) 1) (Image/begin sourcePath))
            (let [^:mutable ok false]
              (Var/pushThreadBindings
                (^[Object/1] RT/map PENDING (new Object/1 [sourcePath (ArrayList.)])))
              (try
                (let [r (arbace.lang.Compiler/load rdr sourcePath sourceName)]
                  (set! ok true)
                  r)
                (finally
                  (Var/popThreadBindings)
                  (Image/end sourcePath ok))))
          :else (arbace.lang.Compiler/load rdr sourcePath sourceName)))

      ;; Compiler.eval(form, false) of a top-level form of a source being recorded: its
      ;; analysis with the recorder bound, then the unit recorded, then its evaluation
      (method ^:public ^:static evalUnit [^:mutable form ^Object/1 rec]
        (Var/pushThreadBindings (^[Object/1] RT/map LOADER (RT/makeClassLoader)))
        (try
          (let [meta (RT/meta form)
                line (if (some? meta) (.valAt meta RT/LINE_KEY (.deref LINE)) (.deref LINE))
                column (if (some? meta) (.valAt meta RT/COLUMN_KEY (.deref COLUMN)) (.deref COLUMN))]
            (Var/pushThreadBindings (^[Object/1] RT/mapUniqueKeys LINE line COLUMN column))
            (try
              (Var/pushThreadBindings (^[Object/1] RT/map RECORDER rec))
              (let [^:mutable popped false]
                (try
                  (set! form (arbace.lang.Compiler/macroexpand form))
                  (cond
                    (and (instance? ISeq form) (Util/equals (RT/first form) DO))
                      (do
                        (Var/popThreadBindings)
                        (set! popped true)
                        (loop [s (RT/next form)]
                          (if (some? (RT/next s))
                              (do (Image/evalUnit (RT/first s) rec) (recur (RT/next s)))
                              (Image/evalUnit (RT/first s) rec))))
                    :else
                      (let [fn (or (instance? IType form)
                                   (and (instance? IPersistentCollection form)
                                        (not (and (instance? Symbol (RT/first form))
                                                  (.startsWith (.-name (cast Symbol (RT/first form))) "def")))))
                            ^:mutable ^Expr expr nil]
                        (if fn
                            (set! expr (arbace.lang.Compiler/analyze C/EXPRESSION
                                                                     (RT/list FN PersistentVector/EMPTY form)
                                                                     (java-str "eval" (RT/nextID))))
                            (try
                              (set! expr (arbace.lang.Compiler/analyze C/EVAL form))
                              (catch ClassFormsExpr$Signal sig
                                (set! expr (arbace.lang.Compiler/analyze
                                             C/EVAL
                                             (RT/list (RT/list FN PersistentVector/EMPTY form)))))))
                        (let [evs (cast ArrayList (aget rec 1))]
                          (Image/unit (cast String (aget rec 0))
                                      (new Object/1 [(Integer/valueOf (if fn 1 0)) line column
                                                     (.deref LINE_BEFORE) (.deref COLUMN_BEFORE)
                                                     expr (.toArray evs)]))
                          (.clear evs))
                        (Var/popThreadBindings)
                        (set! popped true)
                        (Var/pushThreadBindings (^[Object/1] RT/map RECORDER nil))
                        (try
                          (if fn
                              (.invoke (cast IFn (.eval expr)))
                              (.eval expr))
                          (finally (Var/popThreadBindings)))))
                  (finally (when-not popped (Var/popThreadBindings)))))
              (finally (Var/popThreadBindings))))
          (finally (Var/popThreadBindings))))

      ;; Compiler.load of a source from the image: the bindings Compiler.load makes, then each
      ;; unit's events made again and its Expr evaluated as Compiler.eval evaluates it
      (method ^:static replayLoad [^String sourcePath ^String sourceName]
        (Var/pushThreadBindings
          (^[Object/1] RT/mapUniqueKeys LOADER (RT/makeClassLoader) SOURCE_PATH sourcePath
                                        SOURCE sourceName METHOD nil LOCAL_ENV nil LOOP_LOCALS nil
                                        CLASS_FORM_SIBLINGS nil NEXT_LOCAL_NUM (Integer/valueOf 0)
                                        RT/READEVAL RT/T RT/CURRENT_NS (.deref RT/CURRENT_NS)
                                        LINE_BEFORE (Integer/valueOf 1) COLUMN_BEFORE (Integer/valueOf 1)
                                        LINE_AFTER (Integer/valueOf 1) COLUMN_AFTER (Integer/valueOf 1)
                                        RT/UNCHECKED_MATH (.deref RT/UNCHECKED_MATH)
                                        RT/WARN_ON_REFLECTION (.deref RT/WARN_ON_REFLECTION)
                                        RT/DATA_READERS (.deref RT/DATA_READERS)
                                        RECORDER nil PENDING nil))
        (try
          (loop [^:mutable ret nil]
            (let [u (Image/next sourcePath)]
              (if (nil? u)
                  ret
                  (let [evs (cast Object/1 (aget u 6))]
                    (.set LINE_BEFORE (aget u 3))
                    (.set COLUMN_BEFORE (aget u 4))
                    (loop [^int i 0]
                      (when (< i (alength evs))
                        (Image/resolve sourcePath i false)
                        (Image/replay (cast Object/1 (aget evs i)))
                        (recur (unchecked-inc-int i))))
                    (Image/resolve sourcePath (alength evs) true)
                    (Var/pushThreadBindings
                      (^[Object/1] RT/mapUniqueKeys LOADER (RT/makeClassLoader) LINE (aget u 1) COLUMN (aget u 2)))
                    (let [r (try
                              (let [e (cast Expr (aget u 5))]
                                (if (== (.intValue (cast Integer (aget u 0))) 1)
                                    (.invoke (cast IFn (.eval e)))
                                    (.eval e)))
                              (finally (Var/popThreadBindings)))]
                      (recur r))))))
          (catch Throwable e
            (if (not (instance? CompilerException e))
                (throw (CompilerException. sourcePath
                                           (cast Integer (.deref LINE_BEFORE))
                                           (cast Integer (.deref COLUMN_BEFORE))
                                           nil
                                           CompilerException/PHASE_EXECUTION
                                           e))
                (throw (cast CompilerException e))))
          (finally (Var/popThreadBindings))))))

  ;; Compiler.load: a source being recorded (Image.PENDING) has its top-level forms evaluated
  ;; by Image.evalUnit; else as upstream's
  (method ^:public ^:static load [^Reader rdr ^String sourcePath ^String sourceName]
    (let [EOF (Object.)
          rec (cast Object/1 (.deref Compiler$Image/PENDING))
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
                                      (.deref RT/DATA_READERS)
                                      Compiler$Image/PENDING
                                      nil
                                      Compiler$Image/RECORDER
                                      nil))
      (let [readerOpts (arbace.lang.Compiler/readerOpts sourceName)]
        (try
          (loop [r (LispReader/read pushbackReader false EOF false readerOpts)]
            (when-not (identical? r EOF)
              (arbace.lang.Compiler/consumeWhitespaces pushbackReader)
              (.set LINE_AFTER (.getLineNumber pushbackReader))
              (.set COLUMN_AFTER (.getColumnNumber pushbackReader))
              (set! ret (if (some? rec)
                            (Compiler$Image/evalUnit r rec)
                            (arbace.lang.Compiler/eval r false)))
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
        ret))))
