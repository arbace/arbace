;; Go-build variant of arbace.lang.Reflector (C2G-SPEC §4.6, §5.11, §10.1, §12): read by c2g
;; only, never by the JVM build. The Go build has no java.lang.invoke (method handles are
;; reworked or cut, §12), no java.util.stream and no java.lang.reflect.Proxy:
;; - canAccess calls Method.canAccess directly (jrt's, over the member tables) instead of
;;   through the MethodHandle the JVM build looks up in its static initializer (kept there
;;   for Java 8, where the method is absent);
;; - instanceMethods collects with a loop instead of a stream;
;; - boxArg adapts a Clojure function to a functional interface with jrt.AdaptFn (the
;;   interface's ClassInfo.FromFn adapter, amendment R13) instead of Proxy.newProxyInstance.
;; Everything else, the method selection included, is the JVM build's.
(in-ns 'arbace.lang)

(c2g/variant Reflector
  (c2g/cut (field ^:private ^:static ^:final ^MethodHandle CAN_ACCESS_PRED))
  (c2g/cut (static-initializer 0))
  (c2g/cut ^:private ^:static isJava8 ^boolean [])

  (method ^:private ^:static canAccess ^boolean [^java.lang.reflect.Method m target]
    (.canAccess m target))

  (method ^:static instanceMethods ^List [target ^Class c ^String methodName ^int arity]
    (let [found (ArrayList.)]
      (for-each [^java.lang.reflect.Method method (Reflector/getMethods c arity methodName false)]
        (let [m (Reflector/toAccessibleSuperMethod method target)]
          (when (some? m) (.add found m))))
      found))

  ;; jrt.AdaptFn(c, f) (C2G-SPEC §11): f, an IFn, as an instance of the functional interface
  ;; c, through c's generated adapter; jrt throws UnsupportedOperationException when c has none
  (c2g/add (method ^:private ^:static ^:native adaptFn [^Class c f]))

  (method ^:static boxArg [^Class paramType arg]
    (cond
      (and (and (instance? IFn arg) (some? (Compiler$FISupport/maybeFIMethod paramType)))
           (not (.isInstance paramType arg)))
        (Reflector/adaptFn paramType arg)
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
                             (.getName (.getClass arg)))))))))
