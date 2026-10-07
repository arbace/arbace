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
;; /* rich Apr 19, 2006 */
;;
;; Converted from clojure/lang/Reflector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.lang.invoke MethodHandle MethodHandles MethodType)
        '(java.lang.reflect Constructor Field InvocationHandler Modifier Proxy)
        '(java.util ArrayDeque ArrayList Arrays Collection Deque HashSet List Objects Set)
        '(java.util.function Function Predicate)
        '(java.util.stream Collectors))

(defclass ^:public Reflector
  (field ^:private ^:static ^:final ^MethodHandle CAN_ACCESS_PRED)

  (method ^:private ^:static isJava8 ^boolean []
    (.equals (System/getProperty "java.vm.specification.version") "1.8"))

  (static-initializer
    (let [^:mutable ^MethodHandle pred nil]
      (try
        (when-not (Reflector/isJava8)
          (set! pred
                (.findVirtual (MethodHandles/lookup)
                              java.lang.reflect.Method
                              "canAccess"
                              (MethodType/methodType Boolean/TYPE Object))))
        (catch Throwable t (Util/sneakyThrow t)))
      (set! CAN_ACCESS_PRED pred)))

  (method ^:private ^:static canAccess ^boolean [^java.lang.reflect.Method m target]
    (if (some? CAN_ACCESS_PRED)
        (try
          ^boolean (^[java.lang.reflect.Method Object] MethodHandle/.invoke
                     CAN_ACCESS_PRED
                     m
                     target)
          (catch Throwable t (throw (Util/sneakyThrow t))))
        true))

  (method ^:private ^:static interfaces ^{:tag (Collection Class)} [^Class c]
    (let [^{:tag (Set Class)} interfaces (HashSet.)
          ^{:tag (Deque Class)} toWalk (ArrayDeque.)]
      (.addAll toWalk (Arrays/asList (.getInterfaces c)))
      (let [^:mutable iface (cast Class (.poll toWalk))]
        (while (some? iface)
          (.add interfaces iface)
          (.addAll toWalk (Arrays/asList (.getInterfaces iface)))
          (set! iface (cast Class (.poll toWalk))))
        interfaces)))

  (method ^:private ^:static tryFindMethod ^java.lang.reflect.Method [^Class c
                                                                      ^java.lang.reflect.Method m]
    (when (some? c)
      (try (.getMethod c (.getName m) (.getParameterTypes m)) (catch NoSuchMethodException e nil))))

  (method ^:private ^:static toAccessibleSuperMethod ^java.lang.reflect.Method [^java.lang.reflect.Method m
                                                                                target]
    (let [^:mutable selected m]
      (while (some? selected)
        (when (Reflector/canAccess selected target) (return selected))
        (set! selected (Reflector/tryFindMethod (.getSuperclass (.getDeclaringClass selected)) m)))
      (let [^{:tag (Collection Class)} interfaces (Reflector/interfaces (.getDeclaringClass m))]
        (for-each [^Class c interfaces]
          (set! selected (Reflector/tryFindMethod c m))
          (when (some? selected) (return selected)))
        nil)))

  (method ^:public ^:static invokeInstanceMethod [target ^String methodName ^Object/1 args]
    (Reflector/invokeInstanceMethodOfClass target (.getClass target) methodName args))

  (method ^:public ^:static invokeInstanceMethodOfClass [target ^Class c ^String methodName
                                                         ^Object/1 args]
    (Reflector/invokeMatchingMethod methodName
                                    (Reflector/instanceMethods target c methodName (alength args))
                                    c
                                    target
                                    args))

  ;; The candidates of invokeInstanceMethodOfClass, also used by ReflectorCallSite (Arbace)
  (method ^:static instanceMethods ^List [target ^Class c ^String methodName ^int arity]
    (cast List
          (.collect
            (.filter
              (.map
                (.stream (Reflector/getMethods c arity methodName false))
                (lambda Function ^java.lang.reflect.Method [^java.lang.reflect.Method method]
                  (Reflector/toAccessibleSuperMethod method target)))
              (method-ref Predicate [java.lang.reflect.Method] Objects/nonNull))
            (Collectors/toList))))

  (method ^:public ^:static invokeInstanceMethodOfClass [target ^String className ^String methodName
                                                         ^Object/1 args]
    (Reflector/invokeInstanceMethodOfClass target (RT/classForName className) methodName args))

  (method ^:private ^:static getCauseOrElse ^Throwable [^Exception e]
    (if (some? (.getCause e)) (.getCause e) e))

  (method ^:private ^:static throwCauseOrElseException ^RuntimeException [^Exception e]
    (when (some? (.getCause e)) (throw (Util/sneakyThrow (.getCause e))))
    (throw (Util/sneakyThrow e)))

  (method ^:private ^:static noMethodReport ^String [^String methodName ^Class contextClass
                                                     ^Object/1 args]
    (java-str "No matching method "
              methodName
              " found taking "
              (alength args)
              " args"
              (if (some? contextClass) (java-str " for " contextClass) "")))

  (method ^:private ^:static matchMethod ^java.lang.reflect.Method [^List methods ^Object/1 args]
    (let [^:mutable ^java.lang.reflect.Method foundm nil]
      (loop [i (.iterator methods)]
        (when (.hasNext i)
          (let [m (cast java.lang.reflect.Method (.next i))
                params (.getParameterTypes m)]
            (if (and (Reflector/isCongruent params args)
                     (or (nil? foundm)
                         (arbace.lang.Compiler/subsumes params (.getParameterTypes foundm))))
                (do (set! foundm m) (recur i))
                (recur i)))))
      foundm))

  (method ^:private ^:static widenBoxedArgs ^Object/1 [^Object/1 args]
    (let [widenedArgs (new Object/1 (alength args))]
      (loop [^int i 0]
        (if (< i (alength args))
            (if (some? (aget args i))
                (let [valClass (.getClass (aget args i))]
                  (cond
                    (or (or (identical? valClass Integer) (identical? valClass Short))
                        (identical? valClass Byte))
                      (do
                        (aset widenedArgs i (.longValue (cast Number (aget args i))))
                        (recur (unchecked-inc-int i)))
                    (identical? valClass Float)
                      (do
                        (aset widenedArgs i (.doubleValue (cast Number (aget args i))))
                        (recur (unchecked-inc-int i)))
                    :else (do (aset widenedArgs i (aget args i)) (recur (unchecked-inc-int i)))))
                (recur (unchecked-inc-int i)))
            nil))
      widenedArgs))

  (method ^:static invokeMatchingMethod [^String methodName ^List methods target ^Object/1 args]
    (Reflector/invokeMatchingMethod methodName
                                    methods
                                    (when (some? target) (.getClass target))
                                    target
                                    args))

  (method ^:static invokeMatchingMethod [^String methodName ^List methods ^Class contextClass target
                                         ^Object/1 args]
    (let [argsRef (new Object/2 1)]
      (aset argsRef 0 args)
      (let [m (Reflector/selectMatchingMethod methodName methods contextClass target argsRef)
            args (aget argsRef 0)]
        (try
          (Reflector/prepRet (.getReturnType m)
                             (.invoke m target (Reflector/boxArgs (.getParameterTypes m) args)))
          (catch Exception e (throw (Util/sneakyThrow (Reflector/getCauseOrElse e))))))))

  ;; The method invokeMatchingMethod calls, split from it for ReflectorCallSite (Arbace). It may
  ;; replace the arguments in argsRef[0] with their widened copy.
  (method ^:static selectMatchingMethod ^java.lang.reflect.Method [^String methodName ^List methods
                                                                   ^Class contextClass target
                                                                   ^Object/2 argsRef]
    (let [^:mutable ^java.lang.reflect.Method m nil
          ^:mutable ^Object/1 args (aget argsRef 0)]
      (cond
        (.isEmpty methods)
          (throw
            (IllegalArgumentException. (Reflector/noMethodReport methodName contextClass args)))
        (== (.size methods) 1) (set! m (cast java.lang.reflect.Method (.get methods 0)))
        :else
          (do
            (set! m (Reflector/matchMethod methods args))
            (when (nil? m)
              (set! args (Reflector/widenBoxedArgs args))
              (aset argsRef 0 args)
              (set! m (Reflector/matchMethod methods args)))))
      (when (nil? m)
        (throw (IllegalArgumentException. (Reflector/noMethodReport methodName contextClass args))))
      (when (or (not (Modifier/isPublic (.getModifiers (.getDeclaringClass m))))
                (not (Reflector/canAccess m target)))
        (let [oldm m]
          (set! m (Reflector/getAsMethodOfAccessibleBase contextClass m target))
          (when (nil? m)
            (throw (IllegalArgumentException.
                     (java-str "Can't call public method of non-public class: " (.toString oldm)))))))
      m))

  (method ^:public ^:static getAsMethodOfPublicBase ^java.lang.reflect.Method [^Class c
                                                                               ^java.lang.reflect.Method m]
    (for-each [^Class iface (.getInterfaces c)]
      (for-each [^java.lang.reflect.Method im (.getMethods iface)]
        (when (Reflector/isMatch im m) (return im))))
    (let [sc (.getSuperclass c)]
      (when (some? sc)
        (for-each [^java.lang.reflect.Method scm (.getMethods sc)]
          (when (Reflector/isMatch scm m) (return scm)))
        (Reflector/getAsMethodOfPublicBase sc m))))

  (method ^:public ^:static isMatch ^boolean [^java.lang.reflect.Method lhs
                                              ^java.lang.reflect.Method rhs]
    (if (or (not (.equals (.getName lhs) (.getName rhs)))
            (not (Modifier/isPublic (.getModifiers (.getDeclaringClass lhs)))))
        false
        (let [types1 (.getParameterTypes lhs)
              types2 (.getParameterTypes rhs)]
          (if (not (== (alength types1) (alength types2)))
              false
              (let [^:mutable match true]
                (loop [^int i 0]
                  (if (< i (alength types1))
                      (if (not (.isAssignableFrom (aget types1 i) (aget types2 i)))
                          (set! match false)
                          (recur (unchecked-inc-int i)))
                      nil))
                match)))))

  (method ^:public ^:static getAsMethodOfAccessibleBase ^java.lang.reflect.Method [^Class c
                                                                                   ^java.lang.reflect.Method m
                                                                                   target]
    (for-each [^Class iface (.getInterfaces c)]
      (for-each [^java.lang.reflect.Method im (.getMethods iface)]
        (when (Reflector/isAccessibleMatch im m target) (return im))))
    (let [sc (.getSuperclass c)]
      (when (some? sc)
        (for-each [^java.lang.reflect.Method scm (.getMethods sc)]
          (when (Reflector/isAccessibleMatch scm m target) (return scm)))
        (Reflector/getAsMethodOfAccessibleBase sc m target))))

  (method ^:public ^:static isAccessibleMatch ^boolean [^java.lang.reflect.Method lhs
                                                        ^java.lang.reflect.Method rhs target]
    (if (or (or (not (.equals (.getName lhs) (.getName rhs)))
                (not (Modifier/isPublic (.getModifiers (.getDeclaringClass lhs)))))
            (not (Reflector/canAccess lhs target)))
        false
        (let [types1 (.getParameterTypes lhs)
              types2 (.getParameterTypes rhs)]
          (if (not (== (alength types1) (alength types2)))
              false
              (let [^:mutable match true]
                (loop [^int i 0]
                  (if (< i (alength types1))
                      (if (not (.isAssignableFrom (aget types1 i) (aget types2 i)))
                          (set! match false)
                          (recur (unchecked-inc-int i)))
                      nil))
                match)))))

  (method ^:public ^:static invokeConstructor [^Class c ^Object/1 args]
    (try
      (let [ctor (Reflector/selectConstructor c args)]
        (.newInstance ctor (Reflector/boxArgs (.getParameterTypes ctor) args)))
      (catch Exception e (throw (Util/sneakyThrow (Reflector/getCauseOrElse e))))))

  ;; The constructor invokeConstructor calls, split from it for ReflectorCallSite (Arbace). With
  ;; one constructor of the arity it is that one, with more the first congruent one.
  (method ^:static selectConstructor ^Constructor [^Class c ^Object/1 args]
    (let [ctors (Reflector/constructors c (alength args))]
      (cond
        (.isEmpty ctors)
          (throw (IllegalArgumentException. (java-str "No matching ctor found for " c)))
        (== (.size ctors) 1) (cast Constructor (.get ctors 0))
        :else
          (do
            (loop [iterator (.iterator ctors)]
              (when (.hasNext iterator)
                (let [ctor (cast Constructor (.next iterator))
                      params (.getParameterTypes ctor)]
                  (if (Reflector/isCongruent params args)
                      (return ctor)
                      (recur iterator)))))
            (throw (IllegalArgumentException. (java-str "No matching ctor found for " c)))))))

  ;; The public constructors of c taking arity parameters
  (method ^:static constructors ^ArrayList [^Class c ^int arity]
    (let [allctors (.getConstructors c)
          ctors (ArrayList.)]
      (loop [^int i 0]
        (when (< i (alength allctors))
          (let [ctor (aget allctors i)]
            (if (== (alength (.getParameterTypes ctor)) arity)
                (do (.add ctors ctor) (recur (unchecked-inc-int i)))
                (recur (unchecked-inc-int i))))))
      ctors))

  (method ^:public ^:static invokeStaticMethodVariadic [^String className ^String methodName &
                                                        ^Object/1 args]
    (Reflector/invokeStaticMethod className methodName args))

  (method ^:public ^:static invokeStaticMethod [^String className ^String methodName ^Object/1 args]
    (let [c (RT/classForName className)] (Reflector/invokeStaticMethod c methodName args)))

  (method ^:public ^:static invokeStaticMethod [^Class c ^String methodName ^Object/1 args]
    (if (.equals methodName "new")
        (Reflector/invokeConstructor c args)
        (let [methods (Reflector/getMethods c (alength args) methodName true)]
          (Reflector/invokeMatchingMethod methodName methods nil args))))

  (method ^:public ^:static getStaticField [^String className ^String fieldName]
    (let [c (RT/classForName className)] (Reflector/getStaticField c fieldName)))

  (method ^:public ^:static getStaticField [^Class c ^String fieldName]
    (let [f (Reflector/getField c fieldName true)]
      (when (some? f)
        (try
          (return (Reflector/prepRet (.getType f) (.get f nil)))
          (catch IllegalAccessException e (throw (Util/sneakyThrow e)))))
      (throw (IllegalArgumentException. (java-str "No matching field found: " fieldName " for " c)))))

  (method ^:public ^:static setStaticField [^String className ^String fieldName val]
    (let [c (RT/classForName className)] (Reflector/setStaticField c fieldName val)))

  (method ^:public ^:static setStaticField [^Class c ^String fieldName val]
    (let [f (Reflector/getField c fieldName true)]
      (if (some? f)
          (do
            (try
              (.set f nil (Reflector/boxArg (.getType f) val))
              (catch IllegalAccessException e (throw (Util/sneakyThrow e))))
            val)
          (throw (IllegalArgumentException.
                   (java-str "No matching field found: " fieldName " for " c))))))

  (method ^:public ^:static getInstanceField [target ^String fieldName]
    (let [c (.getClass target)
          f (Reflector/getField c fieldName false)]
      (when (some? f)
        (try
          (return (Reflector/prepRet (.getType f) (.get f target)))
          (catch IllegalAccessException e (throw (Util/sneakyThrow e)))))
      (throw (IllegalArgumentException.
               (java-str "No matching field found: " fieldName " for " (.getClass target))))))

  (method ^:public ^:static setInstanceField [target ^String fieldName val]
    (let [c (.getClass target)
          f (Reflector/getField c fieldName false)]
      (if (some? f)
          (do
            (try
              (.set f target (Reflector/boxArg (.getType f) val))
              (catch IllegalAccessException e (throw (Util/sneakyThrow e))))
            val)
          (throw (IllegalArgumentException.
                   (java-str "No matching field found: " fieldName " for " (.getClass target)))))))

  (method ^:public ^:static invokeNoArgInstanceMember [target ^String name]
    (Reflector/invokeNoArgInstanceMember target name false))

  (method ^:public ^:static invokeNoArgInstanceMember [target ^String name ^boolean requireField]
    (let [c (.getClass target)]
      (if requireField
          (let [f (Reflector/getField c name false)]
            (if (some? f)
                (Reflector/getInstanceField target name)
                (throw (IllegalArgumentException.
                         (java-str "No matching field found: " name " for " (.getClass target))))))
          (let [meths (Reflector/getMethods c 0 name false)]
            (if (> (.size meths) 0)
                (Reflector/invokeMatchingMethod name meths target RT/EMPTY_ARRAY)
                (Reflector/getInstanceField target name))))))

  (method ^:public ^:static invokeInstanceMember [target ^String name]
    (let [c (.getClass target)
          f (Reflector/getField c name false)]
      (when (some? f)
        (try
          (return (Reflector/prepRet (.getType f) (.get f target)))
          (catch IllegalAccessException e (throw (Util/sneakyThrow e)))))
      (Reflector/invokeInstanceMethod target name RT/EMPTY_ARRAY)))

  (method ^:public ^:static invokeInstanceMember [^String name target arg1]
    (let [c (.getClass target)
          f (Reflector/getField c name false)]
      (if (some? f)
          (do
            (try
              (.set f target (Reflector/boxArg (.getType f) arg1))
              (catch IllegalAccessException e (throw (Util/sneakyThrow e))))
            arg1)
          (Reflector/invokeInstanceMethod target name (new Object/1 [arg1])))))

  (method ^:public ^:static invokeInstanceMember [^String name target & ^Object/1 args]
    (Reflector/invokeInstanceMethod target name args))

  (method ^:public ^:static getField ^Field [^Class c ^String name ^boolean getStatics]
    (let [allfields (.getFields c)]
      (loop [^int i 0]
        (if (< i (alength allfields))
            (if (and (.equals name (.getName (aget allfields i)))
                     (= (Modifier/isStatic (.getModifiers (aget allfields i))) getStatics))
                (return (aget allfields i))
                (recur (unchecked-inc-int i)))
            nil))
      nil))

  (method ^:public ^:static getMethods ^{:tag (List java.lang.reflect.Method)} [^Class c ^int arity
                                                                                ^String name
                                                                                ^boolean getStatics]
    (let [^:mutable allmethods (.getMethods c)
          methods (ArrayList.)
          bridgeMethods (ArrayList.)]
      (loop [^int i 0]
        (when (< i (alength allmethods))
          (let [method (aget allmethods i)]
            (if (and (and (.equals name (.getName method))
                          (= (Modifier/isStatic (.getModifiers method)) getStatics))
                     (== (alength (.getParameterTypes method)) arity))
                (if (.isBridge method)
                    (do (.add bridgeMethods method) (recur (unchecked-inc-int i)))
                    (do (.add methods method) (recur (unchecked-inc-int i))))
                (recur (unchecked-inc-int i))))))
      (when (.isEmpty methods) (.addAll methods bridgeMethods))
      (when (and (not getStatics) (.isInterface c))
        (set! allmethods (.getMethods Object))
        (loop [^int i 0]
          (if (< i (alength allmethods))
              (if (and (and (.equals name (.getName (aget allmethods i)))
                            (= (Modifier/isStatic (.getModifiers (aget allmethods i))) getStatics))
                       (== (alength (.getParameterTypes (aget allmethods i))) arity))
                  (do (.add methods (aget allmethods i)) (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil)))
      methods))

  (method ^:private ^:static coerceAdapterReturn [ret ^Class targetType]
    (when (.isPrimitive targetType)
      (switch (.getName targetType)
        "boolean" (return (RT/booleanCast ret))
        "long" (return (RT/longCast ret))
        "double" (return (RT/doubleCast ret))
        "int" (return (RT/intCast ret))
        "short" (return (RT/shortCast ret))
        "byte" (return (RT/byteCast ret))
        "float" (return (RT/floatCast ret))))
    ret)

  (method ^:static boxArg [^Class paramType arg]
    (cond
      (and (and (instance? IFn arg) (some? (Compiler$FISupport/maybeFIMethod paramType)))
           (not (.isInstance paramType arg)))
        (Proxy/newProxyInstance (RT/baseLoader)
                                (new Class/1 [paramType])
                                (lambda InvocationHandler [proxy ^java.lang.reflect.Method method
                                                           ^Object/1 methodArgs]
                                  (let [ret (.applyTo (cast IFn arg) (RT/seq methodArgs))]
                                    (Reflector/coerceAdapterReturn ret (.getReturnType method)))))
      (not (.isPrimitive paramType)) (.cast paramType arg)
      (identical? paramType Boolean/TYPE) (.cast Boolean arg)
      (identical? paramType Character/TYPE) (.cast Character arg)
      :else
        (do
          (when (instance? Number arg)
            (let [n (cast Number arg)]
              (cond
                (identical? paramType Integer/TYPE) (return (.intValue n))
                (identical? paramType Float/TYPE) (return (.floatValue n))
                (identical? paramType Double/TYPE) (return (.doubleValue n))
                (identical? paramType Long/TYPE) (return (.longValue n))
                (identical? paramType Short/TYPE) (return (.shortValue n))
                (identical? paramType Byte/TYPE) (return (.byteValue n)))))
          (throw (IllegalArgumentException.
                   (java-str "Unexpected param type, expected: "
                             paramType
                             ", given: "
                             (.getName (.getClass arg))))))))

  (method ^:static boxArgs ^Object/1 [^Class/1 params ^Object/1 args]
    (when-not (== (alength params) 0)
      (let [ret (new Object/1 (alength params))]
        (loop [^int i 0]
          (when (< i (alength params))
            (let [arg (aget args i)
                  paramType (aget params i)]
              (aset ret i (Reflector/boxArg paramType arg))
              (recur (unchecked-inc-int i)))))
        ret)))

  (method ^:public ^:static paramArgTypeMatch ^boolean [^Class paramType ^Class argType]
    (cond
      (nil? argType) (not (.isPrimitive paramType))
      (or (identical? paramType argType) (.isAssignableFrom paramType argType)) true
      (and (some? (Compiler$FISupport/maybeFIMethod paramType)) (.isAssignableFrom IFn argType))
        true
      (identical? paramType Integer/TYPE)
        (or (or (or (or (identical? argType Integer) (identical? argType Long/TYPE))
                    (identical? argType Long))
                (identical? argType Short/TYPE))
            (identical? argType Byte/TYPE))
      (identical? paramType Float/TYPE)
        (or (identical? argType Float) (identical? argType Double/TYPE))
      (identical? paramType Double/TYPE)
        (or (identical? argType Double) (identical? argType Float/TYPE))
      (identical? paramType Long/TYPE)
        (or (or (or (identical? argType Long) (identical? argType Integer/TYPE))
                (identical? argType Short/TYPE))
            (identical? argType Byte/TYPE))
      (identical? paramType Character/TYPE) (identical? argType Character)
      (identical? paramType Short/TYPE) (identical? argType Short)
      (identical? paramType Byte/TYPE) (identical? argType Byte)
      (identical? paramType Boolean/TYPE) (identical? argType Boolean)
      :else false))

  (method ^:static isCongruent ^boolean [^Class/1 params ^Object/1 args]
    (let [^:mutable ret false]
      (if (nil? args)
          (== (alength params) 0)
          (do
            (when (== (alength params) (alength args))
              (set! ret true)
              (loop [^int i 0]
                (when (and ret (< i (alength params)))
                  (let [arg (aget args i)
                        argType (when (some? arg) (.getClass arg))
                        paramType (aget params i)]
                    (set! ret (Reflector/paramArgTypeMatch paramType argType))
                    (recur (unchecked-inc-int i))))))
            ret))))

  (method ^:public ^:static prepRet [^Class c x]
    (cond
      (not (or (.isPrimitive c) (identical? c Boolean))) x
      (instance? Boolean x) (if (.booleanValue (cast Boolean x)) Boolean/TRUE Boolean/FALSE)
      :else x)))
