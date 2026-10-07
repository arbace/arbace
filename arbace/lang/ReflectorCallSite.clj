;; Arbace's own class, not converted from Clojure: the invokedynamic call sites of the
;; compiler's reflective calls (doc/VENDOR-NOTES.md).

(in-ns 'arbace.lang)

(import '(java.lang.invoke CallSite MethodHandle MethodHandles MethodHandles$Lookup MethodType
                           MutableCallSite)
        '(java.lang.reflect Constructor Field)
        '(java.util Arrays List))

;; A call site of a reflective call the compiler could not resolve, one of the kinds
;;   METHOD  an instance method: Reflector.invokeInstanceMethod, or invokeInstanceMethodOfClass
;;           with a qualifying class
;;   MEMBER  a no-argument member, method or else field: Reflector.invokeNoArgInstanceMember
;;   FIELD   a field (.-f x): invokeNoArgInstanceMember with requireField
;;   STATIC  a static method: Reflector.invokeStaticMethod
;;   NEW     a constructor: Reflector.invokeConstructor
;; The site's type is (Object target, Object arg...)Object, without the target for STATIC and
;; NEW. It is an inline cache in the manner of Dynalink: for each receiver class seen, and where
;; Reflector's choice among overloads depends on them, each combination of argument classes (nil
;; included), it caches a method handle of the member Reflector chooses, found by Reflector's own
;; code, and calls it with Reflector's argument conversion (boxArg, after widenBoxedArgs where
;; Reflector widens) and return conversion (prepRet). The first THRESHOLD calls of a site go
;; through Reflector, so a site run once never links. The entries form a chain of guards on the
;; classes, up to PIC_LIMIT; past that the site is megamorphic: its fallback looks the entries up
;; by a scan, up to MEGA_LIMIT of them. Whatever is not cached goes through Reflector's unchanged
;; path, so behaviour and messages are Reflector's: a nil receiver, a call Reflector refuses (its
;; exceptions are thrown by Reflector again), a member a method handle cannot reach. STATIC and
;; NEW sites, and METHOD sites with a qualifying class, are emitted only for classes of the boot
;; or platform loader, whose names always denote the same class, so the site resolves the name
;; once.
(defclass ^:public ^:final ReflectorCallSite
  :extends MutableCallSite

  ;; the kinds of sites (the bootstrap's int argument)
  (field ^:public ^:static ^:final ^int METHOD 0)
  (field ^:public ^:static ^:final ^int MEMBER 1)
  (field ^:public ^:static ^:final ^int FIELD 2)
  (field ^:public ^:static ^:final ^int STATIC 3)
  (field ^:public ^:static ^:final ^int NEW 4)

  (field ^:static ^:final ^int THRESHOLD 1)
  (field ^:static ^:final ^int PIC_LIMIT 8)
  (field ^:static ^:final ^int MEGA_LIMIT 64)
  (field ^:static ^:final ^int FAILURE_LIMIT 8)

  (field ^:private ^:static ^:final ^MethodHandles$Lookup LOOKUP)
  (field ^:private ^:static ^:final ^MethodHandle FALLBACK)
  (field ^:private ^:static ^:final ^MethodHandle CLASS_IS)
  (field ^:private ^:static ^:final ^MethodHandle/1 MATCH)
  (field ^:private ^:static ^:final ^MethodHandle CAST)
  (field ^:private ^:static ^:final ^MethodHandle BOX_ARG)
  (field ^:private ^:static ^:final ^MethodHandle WIDEN)
  (field ^:private ^:static ^:final ^MethodHandle PREP_RET)

  (static-initializer
    (try
      (let [l (MethodHandles/lookup)]
        (set! LOOKUP l)
        (set! FALLBACK
              (.findVirtual l ReflectorCallSite "fallback" (MethodType/methodType Object Object/1)))
        (set! CLASS_IS (.findStatic l ReflectorCallSite "classIs"
                                    (MethodType/methodType Boolean/TYPE Class
                                                           (new Class/1 [Object]))))
        (set! MATCH (new MethodHandle/1 5))
        (loop [^int k 2]
          (when (<= k 4)
            (let [ps (new Class/1 (unchecked-multiply-int 2 k))]
              (Arrays/fill ps 0 k Class)
              (Arrays/fill ps k (alength ps) Object)
              (aset MATCH k (.findStatic l ReflectorCallSite (java-str "match" k)
                                         (MethodType/methodType Boolean/TYPE ps))))
            (recur (unchecked-inc-int k))))
        (set! CAST (.findVirtual l Class "cast" (MethodType/methodType Object Object)))
        (set! BOX_ARG (.findStatic l ReflectorCallSite "boxArg"
                                   (MethodType/methodType Object Class (new Class/1 [Object]))))
        (set! WIDEN (.findStatic l ReflectorCallSite "widen" (MethodType/methodType Object Object)))
        (set! PREP_RET (.findStatic l Reflector "prepRet"
                                    (MethodType/methodType Object Class (new Class/1 [Object])))))
      (catch ReflectiveOperationException e (throw (ExceptionInInitializerError. e)))))

  ;; A cached member: the classes its call depends on, of the leading arguments of the site (the
  ;; receiver first, if any; Void for nil), and its handle, of the site's type
  (defclass ^:static ^:final Entry
    (field ^:final ^Class/1 classes)
    (field ^:final ^MethodHandle handle)
    (field ^:volatile ^MethodHandle spreader)

    (constructor [this ^Class/1 classes ^MethodHandle handle]
      (set! (.-classes this) classes)
      (set! (.-handle this) handle))

    (method matches ^boolean [this ^Object/1 all]
      (loop [^int i 0]
        (when (< i (alength classes))
          (when-not (identical? (ReflectorCallSite/classOf (aget all i)) (aget classes i))
            (return false))
          (recur (unchecked-inc-int i))))
      true)

    ;; the call through the handle, which takes the arguments as an array made when first needed
    (method invoke [this ^Object/1 all]
      (let [^:mutable ^MethodHandle s spreader]
        (when (nil? s)
          (set! s (.asSpreader handle Object/1 (alength all)))
          (set! spreader s))
        ^Object (^[Object/1] MethodHandle/.invokeExact s all))))

  (field ^:private ^:final ^String name)
  (field ^:private ^:final ^String className)
  (field ^:private ^:final ^int kind)
  (field ^:private ^:final ^MethodHandle fallbackHandle)
  (field ^:private ^Class namedClass)
  (field ^:private ^int calls)
  (field ^:private ^int failures)
  (field ^:private ^:volatile ^ReflectorCallSite$Entry/1 entries)

  (constructor [this ^MethodType type ^String name ^String className ^int kind]
    (super. type)
    (set! (.-name this) name)
    (set! (.-className this) className)
    (set! (.-kind this) kind)
    (set! (.-entries this) (new ReflectorCallSite$Entry/1 0))
    (set! (.-fallbackHandle this)
          (.asType (.asCollector (.bindTo FALLBACK this) Object/1 (.parameterCount type)) type))
    (.setTarget this fallbackHandle))

  ;; The bootstrap: name is the member's name, className the qualifying class's name, or the
  ;; class of a STATIC or NEW site, or "", kind one of the kinds above.
  (method ^:public ^:static bootstrap ^CallSite [^MethodHandles$Lookup caller ^String _
                                                 ^MethodType type ^String name ^String className
                                                 ^int kind]
    (ReflectorCallSite. type name (when-not (.isEmpty className) className) kind))

  ;; The guards
  (method ^:static classOf ^Class [o]
    (if (nil? o) Void (.getClass o)))

  (method ^:private ^:static classIs ^boolean [^Class c o]
    (identical? (ReflectorCallSite/classOf o) c))

  (method ^:private ^:static match2 ^boolean [^Class c0 ^Class c1 x0 x1]
    (and (identical? (ReflectorCallSite/classOf x0) c0)
         (identical? (ReflectorCallSite/classOf x1) c1)))

  (method ^:private ^:static match3 ^boolean [^Class c0 ^Class c1 ^Class c2 x0 x1 x2]
    (and (and (identical? (ReflectorCallSite/classOf x0) c0)
              (identical? (ReflectorCallSite/classOf x1) c1))
         (identical? (ReflectorCallSite/classOf x2) c2)))

  (method ^:private ^:static match4 ^boolean [^Class c0 ^Class c1 ^Class c2 ^Class c3 x0 x1 x2 x3]
    (and (and (and (identical? (ReflectorCallSite/classOf x0) c0)
                   (identical? (ReflectorCallSite/classOf x1) c1))
              (identical? (ReflectorCallSite/classOf x2) c2))
         (identical? (ReflectorCallSite/classOf x3) c3)))

  ;; Reflector.boxArg, an exception from it treated as Reflector.invokeMatchingMethod and
  ;; invokeConstructor treat it
  (method ^:private ^:static boxArg [^Class p x]
    (try
      (Reflector/boxArg p x)
      (catch Exception e (throw (Util/sneakyThrow (if (some? (.getCause e)) (.getCause e) e))))))

  ;; Reflector.widenBoxedArgs for one argument
  (method ^:private ^:static widen [x]
    (if (some? x)
        (let [c (.getClass x)]
          (cond
            (or (or (identical? c Integer) (identical? c Short)) (identical? c Byte))
              (.longValue (cast Number x))
            (identical? c Float) (.doubleValue (cast Number x))
            :else x))
        x))

  (method ^:private ^:static isWidened ^boolean [^Class c]
    (or (or (or (identical? c Integer) (identical? c Short)) (identical? c Byte))
        (identical? c Float)))

  (method ^:private hasTarget ^boolean [this] (< kind STATIC))

  ;; Every call the chain of guards does not take: before linking, a nil receiver, classes not
  ;; seen yet, all calls of a megamorphic site
  (method ^:public fallback [this ^Object/1 all]
    (when (and (.hasTarget this) (nil? (aget all 0))) (return (.reflect this all)))
    (for-each [^ReflectorCallSite$Entry e entries]
      (when (.matches e all) (return (.invoke e all))))
    ;; the call that links goes through Reflector, which chooses as the entry does
    (cond
      (< calls THRESHOLD) (set! calls (unchecked-inc-int calls))
      (< failures FAILURE_LIMIT) (.link this all))
    (.reflect this all))

  ;; Reflector's path, as the compiler emitted it before
  (method ^:private reflect [this ^Object/1 all]
    (switch kind
      0 (let [args (Arrays/copyOfRange all 1 (alength all))]
          (if (nil? className)
              (Reflector/invokeInstanceMethod (aget all 0) name args)
              (Reflector/invokeInstanceMethodOfClass (aget all 0) className name args)))
      1 (Reflector/invokeNoArgInstanceMember (aget all 0) name false)
      2 (Reflector/invokeNoArgInstanceMember (aget all 0) name true)
      3 (Reflector/invokeStaticMethod (RT/classForName className) name all)
      (Reflector/invokeConstructor (RT/classForName className) all)))

  ;; Finds the member Reflector would call with these arguments and caches it in a new entry,
  ;; unless the site has MEGA_LIMIT entries or that fails (after FAILURE_LIMIT failures, which
  ;; are mostly calls Reflector refuses, the site stops trying)
  (method ^:private link ^void [this ^Object/1 all]
    (locking this
      (let [es entries]
        (for-each [^ReflectorCallSite$Entry e es]
          (when (.matches e all) (return)))
        (when (>= (alength es) MEGA_LIMIT) (return))
        (let [e (try (.entry this all) (catch Throwable t nil))]
          (when (nil? e) (set! failures (unchecked-inc-int failures)) (return))
          (let [n (alength es)
                nes (cast ReflectorCallSite$Entry/1 (Arrays/copyOf es (unchecked-inc-int n)))]
            (aset nes n e)
            (set! entries nes)
            (cond
              (< n PIC_LIMIT) (.setTarget this (.guard this e (.getTarget this)))
              (== n PIC_LIMIT) (.setTarget this fallbackHandle)))))))

  ;; The entry's handle, guarded by its classes, and otherwise next
  (method ^:private guard ^MethodHandle [this ^ReflectorCallSite$Entry e ^MethodHandle next]
    (let [cs (.-classes e)
          k (alength cs)]
      (cond
        (== k 0) (.-handle e)
        (== k 1) (MethodHandles/guardWithTest (.bindTo CLASS_IS (aget cs 0)) (.-handle e) next)
        (<= k 4)
          (MethodHandles/guardWithTest (MethodHandles/insertArguments (aget MATCH k) 0 (cast Object/1 cs))
                                       (.-handle e)
                                       next)
        :else
          (let [ptypes (.parameterArray (.type this))
                ^:mutable ^MethodHandle h (.-handle e)]
            (loop [^int j (unchecked-dec-int k)]
              (when (>= j 0)
                (let [test (MethodHandles/dropArguments
                             (.bindTo CLASS_IS (aget cs j))
                             0
                             (cast Class/1 (Arrays/copyOf ptypes j)))]
                  (set! h (MethodHandles/guardWithTest test h next))
                  (recur (unchecked-dec-int j)))))
            h))))

  (method ^:private namedClass ^Class [this]
    (when (nil? namedClass) (set! namedClass (RT/classForName className)))
    namedClass)

  ;; The entry for these arguments, or nil. Reflector's selection runs on the actual target and
  ;; arguments; its exceptions make the call go through Reflector, which throws them again.
  (method ^:private entry ^ReflectorCallSite$Entry [this ^Object/1 all]
    (let [target (when (.hasTarget this) (aget all 0))
          c (when (some? target) (.getClass target))
          args (if (.hasTarget this) (Arrays/copyOfRange all 1 (alength all)) all)]
      (switch kind
        0 (let [cc (if (nil? className) c (.namedClass this))
                methods (Reflector/instanceMethods target cc name (alength args))]
            (when (.isEmpty methods) (return nil))
            (.methodEntry this methods cc target c args))
        1 (let [meths (Reflector/getMethods c 0 name false)]
            (if (> (.size meths) 0)
                (.methodEntry this meths c target c args)
                (.fieldEntry this c)))
        2 (.fieldEntry this c)
        3 (let [methods (Reflector/getMethods (.namedClass this) (alength args) name true)]
            (when (.isEmpty methods) (return nil))
            (.methodEntry this methods nil nil nil args))
        (let [ctors (Reflector/constructors (.namedClass this) (alength args))
              ctor (Reflector/selectConstructor (.namedClass this) args)]
          (.handleEntry this
                        (.unreflectConstructor LOOKUP ctor)
                        (.getParameterTypes ctor)
                        nil
                        (when (> (.size ctors) 1) args)
                        false)))))

  ;; The entry of the method Reflector.invokeMatchingMethod calls: from methods, with the context
  ;; class cc, on target, whose class c is the entry's guard; guarded on the argument classes
  ;; too when there is more than one method
  (method ^:private methodEntry ^ReflectorCallSite$Entry [this ^List methods ^Class cc target
                                                          ^Class c ^Object/1 args]
    (let [argsRef (new Object/2 1)]
      (aset argsRef 0 args)
      (let [m (Reflector/selectMatchingMethod name methods cc target argsRef)]
        (.handleEntry this
                      (.unreflect LOOKUP m)
                      (.getParameterTypes m)
                      c
                      (when (> (.size methods) 1) args)
                      (not (identical? (aget argsRef 0) args))))))

  ;; The entry of the handle h with the given parameter types (after the receiver, if any),
  ;; guarded on the receiver class c (unless nil) and on the classes of guardArgs (unless nil),
  ;; whose call converts the arguments as Reflector does, widened first if widened, and the
  ;; result as Reflector.prepRet does
  (method ^:private handleEntry ^ReflectorCallSite$Entry [this ^MethodHandle h ^Class/1 ptypes
                                                          ^Class c ^Object/1 guardArgs
                                                          ^boolean widened]
    (let [^int nc (if (some? c) 1 0)
          ^int na (if (some? guardArgs) (alength guardArgs) 0)
          classes (new Class/1 (unchecked-add-int nc na))
          filters (new MethodHandle/1 (alength ptypes))]
      (when (some? c) (aset classes 0 c))
      (loop [^int i 0]
        (when (< i (alength ptypes))
          (let [ac (when (some? guardArgs) (ReflectorCallSite/classOf (aget guardArgs i)))
                w (and widened (ReflectorCallSite/isWidened ac))
                p (aget ptypes i)
                ^:mutable ^MethodHandle f (ReflectorCallSite/argFilter
                                            p
                                            (cond
                                              (not w) ac
                                              (identical? ac Float) Double
                                              :else Long))]
            (when (some? ac) (aset classes (unchecked-add-int nc i) ac))
            (when w
              (set! f (if (nil? f)
                          (.asType WIDEN (MethodType/methodType p Object))
                          (MethodHandles/filterReturnValue WIDEN f))))
            (aset filters i f))
          (recur (unchecked-inc-int i))))
      (let [^:mutable ^MethodHandle h (MethodHandles/filterArguments h nc filters)]
        (when (identical? (.returnType (.type h)) Boolean)
          (set! h (MethodHandles/filterReturnValue (.asType h (.changeReturnType (.type h) Object))
                                                   (.bindTo PREP_RET Boolean))))
        (ReflectorCallSite$Entry. classes (.asType h (.type this))))))

  ;; The entry reading the field Reflector.getInstanceField reads, or nil
  (method ^:private fieldEntry ^ReflectorCallSite$Entry [this ^Class c]
    (let [f (Reflector/getField c name false)]
      (when (nil? f) (return nil))
      (let [^:mutable ^MethodHandle h (.unreflectGetter LOOKUP f)]
        (when (identical? (.getType f) Boolean)
          (set! h (MethodHandles/filterReturnValue (.asType h (.changeReturnType (.type h) Object))
                                                   (.bindTo PREP_RET Boolean))))
        (ReflectorCallSite$Entry. (new Class/1 [c]) (.asType h (.type this))))))

  ;; Reflector's conversion of an argument to a parameter type (boxArg), as a filter. With the
  ;; argument's class known (guarded, Void for nil), where boxArg only casts or unboxes, nil: the
  ;; handle's asType to the site's type then casts and unboxes.
  (method ^:private ^:static argFilter ^MethodHandle [^Class p ^Class ac]
    (cond
      (and (some? ac)
           (if (.isPrimitive p)
               (identical? ac (.returnType (.wrap (MethodType/methodType p))))
               (or (identical? ac Void) (.isAssignableFrom p ac))))
        nil
      (or (.isPrimitive p) (some? (Compiler$FISupport/maybeFIMethod p)))
        (.asType (.bindTo BOX_ARG p) (MethodType/methodType p Object))
      (identical? p Object) nil
      :else (.asType (.bindTo CAST p) (MethodType/methodType p Object)))))
