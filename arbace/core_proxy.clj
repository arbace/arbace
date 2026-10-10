;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(in-ns 'arbace.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;; proxy ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import
 '(arbace.asm ClassWriter ClassVisitor Opcodes Type)
 '(java.lang.reflect Modifier Constructor)
 '(java.io Serializable NotSerializableException)
 '(arbace.asm.commons Method GeneratorAdapter)
 '(arbace.lang IProxy Reflector DynamicClassLoader IPersistentMap PersistentHashMap RT))

(defn method-sig [^java.lang.reflect.Method meth]
  [(. meth (getName)) (seq (. meth (getParameterTypes))) (. meth getReturnType)])

(defn- most-specific [rtypes]
  (or (some (fn [t] (when (every? #(isa? t %) rtypes) t)) rtypes)
    (throw (Exception. "Incompatible return types"))))

(defn- group-by-sig
  "Takes a collection of [msig meth] and returns a seq of maps from
   return-types to meths."
  [coll]
  (vals (reduce1 (fn [m [msig meth]]
                  (let [rtype (peek msig)
                        argsig (pop msig)]
                    (assoc m argsig (assoc (m argsig {}) rtype meth))))
          {} coll)))

(defn proxy-name
 {:tag String}
 [^Class super interfaces]
  (let [inames (into1 (sorted-set) (map #(.getName ^Class %) interfaces))]
    (apply str (.replace (str *ns*) \- \_) ".proxy"
      (interleave (repeat "$")
        (concat
          [(.getName super)]
          (map #(subs % (inc (.lastIndexOf ^String % "."))) inames)
          [(Integer/toHexString (hash inames))])))))

(defn- generate-proxy [^Class super interfaces]
  (let [cv (arbace.lang.Compiler/classWriter)
        pname (proxy-name super interfaces)
        cname (.replace pname \. \/) ;(str "arbace/lang/" (gensym "Proxy__"))
        ctype (. Type (getObjectType cname))
        iname (fn [^Class c] (.. Type (getType c) (getInternalName)))
        fmap "__arbaceFnMap"
        totype (fn [^Class c] (. Type (getType c)))
        to-types (fn [cs] (if (pos? (count cs))
                            (into-array (map totype cs))
                            (make-array Type 0)))
        super-type ^Type (totype super)
        imap-type ^Type (totype IPersistentMap)
        ifn-type (totype arbace.lang.IFn)
        obj-type (totype Object)
        sym-type (totype arbace.lang.Symbol)
        rt-type  (totype arbace.lang.RT)
        ex-type  (totype java.lang.UnsupportedOperationException)
        gen-bridge
        (fn [^java.lang.reflect.Method meth ^java.lang.reflect.Method dest]
            (let [pclasses (. meth (getParameterTypes))
                  ptypes (to-types pclasses)
                  rtype ^Type (totype (. meth (getReturnType)))
                  m (new Method (. meth (getName)) rtype ptypes)
                  dtype (totype (.getDeclaringClass dest))
                  dm (new Method (. dest (getName)) (totype (. dest (getReturnType))) (to-types (. dest (getParameterTypes))))
                  gen (new GeneratorAdapter (bit-or (. Opcodes ACC_PUBLIC) (. Opcodes ACC_BRIDGE)) m nil nil cv)]
              (. gen (visitCode))
              (. gen (loadThis))
              (dotimes [i (count ptypes)]
                  (. gen (loadArg i)))
              (if (-> dest .getDeclaringClass .isInterface)
                (. gen (invokeInterface dtype dm))
                (. gen (invokeVirtual dtype dm)))
              (. gen (returnValue))
              (. gen (endMethod))))
        gen-method
        (fn [^java.lang.reflect.Method meth else-gen]
            (let [pclasses (. meth (getParameterTypes))
                  ptypes (to-types pclasses)
                  rtype ^Type (totype (. meth (getReturnType)))
                  m (new Method (. meth (getName)) rtype ptypes)
                  gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)
                  else-label (. gen (newLabel))
                  end-label (. gen (newLabel))
                  decl-type (. Type (getType (. meth (getDeclaringClass))))]
              (. gen (visitCode))
              (if (> (count pclasses) 18)
                (else-gen gen m)
                (do
                  (. gen (loadThis))
                  (. gen (getField ctype fmap imap-type))

                  (. gen (push (. meth (getName))))
                                        ;lookup fn in map
                  (. gen (invokeStatic rt-type (. Method (getMethod "Object get(Object, Object)"))))
                  (. gen (dup))
                  (. gen (ifNull else-label))
                                        ;if found
                  (.checkCast gen ifn-type)
                  (. gen (loadThis))
                                        ;box args
                  (dotimes [i (count ptypes)]
                      (. gen (loadArg i))
                    (. arbace.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses i))))
                                        ;call fn
                  (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                        (into-array (cons obj-type
                                                                          (replicate (count ptypes) obj-type))))))
                                        ;unbox return
                  (. gen (unbox rtype))
                  (when (= (. rtype (getSort)) (. Type VOID))
                    (. gen (pop)))
                  (. gen (goTo end-label))

                                        ;else call supplied alternative generator
                  (. gen (mark else-label))
                  (. gen (pop))

                  (else-gen gen m)

                  (. gen (mark end-label))))
              (. gen (returnValue))
              (. gen (endMethod))))]

                                        ;start class definition
    (. cv (visit arbace.lang.Compiler/JVM_BYTECODE_VERSION (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_SUPER))
                 cname nil (iname super)
                 (into-array (map iname (cons IProxy interfaces)))))
                                        ;add field for fn mappings
    (. cv (visitField (+ (. Opcodes ACC_PRIVATE) (. Opcodes ACC_VOLATILE))
                      fmap (. imap-type (getDescriptor)) nil nil))
                                        ;add ctors matching/calling super's
    ;; in order of their parameter types, so the class is the same in every JVM (Arbace)
    (doseq [^Constructor ctor (sort-by (fn [^Constructor c] (into1 [] (map #(.getName ^Class %) (.getParameterTypes c))))
                                       (. super (getDeclaredConstructors)))]
        (when-not (. Modifier (isPrivate (. ctor (getModifiers))))
          (let [ptypes (to-types (. ctor (getParameterTypes)))
                m (new Method "<init>" (. Type VOID_TYPE) ptypes)
                gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
            (. gen (visitCode))
                                        ;call super ctor
            (. gen (loadThis))
            (. gen (dup))
            (. gen (loadArgs))
            (. gen (invokeConstructor super-type m))

            (. gen (returnValue))
            (. gen (endMethod)))))
                                        ;disable serialization
    (when (some #(isa? % Serializable) (cons super interfaces))
      (let [m (. Method (getMethod "void writeObject(java.io.ObjectOutputStream)"))
            gen (new GeneratorAdapter (. Opcodes ACC_PRIVATE) m nil nil cv)]
        (. gen (visitCode))
        (. gen (loadThis))
        (. gen (loadArgs))
        (. gen (throwException (totype NotSerializableException) pname))
        (. gen (endMethod)))
      (let [m (. Method (getMethod "void readObject(java.io.ObjectInputStream)"))
            gen (new GeneratorAdapter (. Opcodes ACC_PRIVATE) m nil nil cv)]
        (. gen (visitCode))
        (. gen (loadThis))
        (. gen (loadArgs))
        (. gen (throwException (totype NotSerializableException) pname))
        (. gen (endMethod))))
                                        ;add IProxy methods
    (let [m (. Method (getMethod "void __initArbaceFnMappings(arbace.lang.IPersistentMap)"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (loadArgs))
      (. gen (putField ctype fmap imap-type))

      (. gen (returnValue))
      (. gen (endMethod)))
    (let [m (. Method (getMethod "void __updateArbaceFnMappings(arbace.lang.IPersistentMap)"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (dup))
      (. gen (getField ctype fmap imap-type))
      (.checkCast gen (totype arbace.lang.IPersistentCollection))
      (. gen (loadArgs))
      (. gen (invokeInterface (totype arbace.lang.IPersistentCollection)
                              (. Method (getMethod "arbace.lang.IPersistentCollection cons(Object)"))))
      (. gen (checkCast imap-type))
      (. gen (putField ctype fmap imap-type))

      (. gen (returnValue))
      (. gen (endMethod)))
    (let [m (. Method (getMethod "arbace.lang.IPersistentMap __getArbaceFnMappings()"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (getField ctype fmap imap-type))
      (. gen (returnValue))
      (. gen (endMethod)))

                                        ;calc set of supers' non-private instance methods
    (let [[mm considered]
            (loop [mm {} considered #{} c super]
              (if c
                (let [[mm considered]
                      (loop [mm mm
                             considered considered
                             meths (concat
                                    (seq (. c (getDeclaredMethods)))
                                    (seq (. c (getMethods))))]
                        (if (seq meths)
                          (let [^java.lang.reflect.Method meth (first meths)
                                mods (. meth (getModifiers))
                                mk (method-sig meth)]
                            (if (or (considered mk)
                                    (not (or (Modifier/isPublic mods) (Modifier/isProtected mods)))
                                    ;(. Modifier (isPrivate mods))
                                    (. Modifier (isStatic mods))
                                    (. Modifier (isFinal mods))
                                    (= "finalize" (.getName meth)))
                              (recur mm (conj considered mk) (next meths))
                              (recur (assoc mm mk meth) (conj considered mk) (next meths))))
                          [mm considered]))]
                  (recur mm considered (. c (getSuperclass))))
                [mm considered]))
          ifaces-meths (into1 {}
                         (for [^Class iface interfaces meth (. iface (getMethods))
                               :let [msig (method-sig meth)] :when (not (considered msig))]
                           {msig meth}))
          ;; Treat abstract methods as interface methods
          [mm ifaces-meths] (let [abstract? (fn [[_ ^Method meth]]
                                              (Modifier/isAbstract (. meth (getModifiers))))
                                  mm-no-abstract (remove abstract? mm)
                                  abstract-meths (filter abstract? mm)]
                              [mm-no-abstract (concat ifaces-meths abstract-meths)])
          mgroups (group-by-sig (concat mm ifaces-meths))
          rtypes (map #(most-specific (keys %)) mgroups)
          mb (map #(vector (%1 %2) (vals (dissoc %1 %2))) mgroups rtypes)
          bridge? (reduce1 into1 #{} (map second mb))
          ifaces-meths (remove bridge? (vals ifaces-meths))
          mm (remove bridge? (vals mm))
          reflect-Method-keyfn (fn [meth]
                                 (let [[name param-types ^Class return-type] (method-sig meth)]
                                   (-> [name]
                                       (into1 (map #(.getName ^Class %) param-types))
                                       (conj (.getName return-type)))))]
                                        ;add methods matching supers', if no mapping -> call super
      (doseq [[^java.lang.reflect.Method dest bridges] (sort-by (comp reflect-Method-keyfn first) mb)
              ^java.lang.reflect.Method meth (sort-by reflect-Method-keyfn bridges)]
          (gen-bridge meth dest))
      (doseq [^java.lang.reflect.Method meth (sort-by reflect-Method-keyfn mm)]
          (gen-method meth
                      (fn [^GeneratorAdapter gen ^Method m]
                          (. gen (loadThis))
                                        ;push args
                        (. gen (loadArgs))
                                        ;call super
                        (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                                (. super-type (getInternalName))
                                                (. m (getName))
                                                (. m (getDescriptor)))))))

                                        ;add methods matching interfaces', if no mapping -> throw
      (doseq [^java.lang.reflect.Method meth (sort-by reflect-Method-keyfn ifaces-meths)]
                (gen-method meth
                            (fn [^GeneratorAdapter gen ^Method m]
                                (. gen (throwException ex-type (. m (getName))))))))

                                        ;finish class def
    (. cv (visitEnd))
    [cname (. cv toByteArray)]))

(defn- get-super-and-interfaces [bases]
  (if (. ^Class (first bases) (isInterface))
    [Object bases]
    [(first bases) (next bases)]))

(defn get-proxy-class
  "Takes an optional single class followed by zero or more
  interfaces. If not supplied class defaults to Object.  Creates an
  returns an instance of a proxy class derived from the supplied
  classes. The resulting value is cached and used for any subsequent
  requests for the same class set. Returns a Class object."
  {:added "1.0"}
  [& bases]
    (let [[super interfaces] (get-super-and-interfaces bases)
          pname (proxy-name super interfaces)]
      (or (RT/loadClassForName pname)
          (let [[cname bytecode] (generate-proxy super interfaces)]
            (. ^DynamicClassLoader (deref arbace.lang.Compiler/LOADER) (defineClass pname bytecode [super interfaces]))))))

(defn construct-proxy
  "Takes a proxy class and any arguments for its superclass ctor and
  creates and returns an instance of the proxy."
  {:added "1.0"}
  [c & ctor-args]
    (. Reflector (invokeConstructor c (to-array ctor-args))))

(defn init-proxy
  "Takes a proxy instance and a map of strings (which must
  correspond to methods of the proxy superclass/superinterfaces) to
  fns (which must take arguments matching the corresponding method,
  plus an additional (explicit) first arg corresponding to this, and
  sets the proxy's fn map.  Returns the proxy."
  {:added "1.0"}
  [^IProxy proxy mappings]
    (. proxy (__initArbaceFnMappings mappings))
    proxy)

(defn update-proxy
  "Takes a proxy instance and a map of strings (which must
  correspond to methods of the proxy superclass/superinterfaces) to
  fns (which must take arguments matching the corresponding method,
  plus an additional (explicit) first arg corresponding to this, and
  updates (via assoc) the proxy's fn map. nil can be passed instead of
  a fn, in which case the corresponding method will revert to the
  default behavior. Note that this function can be used to update the
  behavior of an existing instance without changing its identity.
  Returns the proxy."
  {:added "1.0"}
  [^IProxy proxy mappings]
    (. proxy (__updateArbaceFnMappings mappings))
    proxy)

(defn proxy-mappings
  "Takes a proxy instance and returns the proxy's fn map."
  {:added "1.0"}
  [^IProxy proxy]
    (. proxy (__getArbaceFnMappings)))

(defmacro proxy
  "class-and-interfaces - a vector of class names

  args - a (possibly empty) vector of arguments to the superclass
  constructor.

  f => (name [params*] body) or
  (name ([params*] body) ([params+] body) ...)

  Expands to code which creates a instance of a proxy class that
  implements the named class/interface(s) by calling the supplied
  fns. A single class, if provided, must be first. If not provided it
  defaults to Object.

  The interfaces names must be valid interface types. If a method fn
  is not provided for a class method, the superclass method will be
  called. If a method fn is not provided for an interface method, an
  UnsupportedOperationException will be thrown should it be
  called. Method fns are closures and can capture the environment in
  which proxy is called. Each method fn takes an additional implicit
  first arg, which is bound to 'this. Note that while method fns can
  be provided to override protected methods, they have no other access
  to protected members, nor to super, as these capabilities cannot be
  proxied."
  {:added "1.0"}
  [class-and-interfaces args & fs]
   (let [bases (map #(or (resolve %) (throw (Exception. (str "Can't resolve: " %))))
                    class-and-interfaces)
         [super interfaces] (get-super-and-interfaces bases)
         compile-effect (when *compile-files*
                          (let [[cname bytecode] (generate-proxy super interfaces)]
                            (arbace.lang.Compiler/writeClassFile cname bytecode)))
         pc-effect (apply get-proxy-class bases)
         pname (proxy-name super interfaces)]
     ;remember the class to prevent it from disappearing before use
     (intern *ns* (symbol pname) pc-effect)
     `(let [;pc# (get-proxy-class ~@class-and-interfaces)
            p# (new ~(symbol pname) ~@args)] ;(construct-proxy pc# ~@args)]
        (init-proxy p#
         ~(loop [fmap {} fs fs]
            (if fs
              (let [[sym & meths] (first fs)
                    meths (if (vector? (first meths))
                            (list meths)
                            meths)
                    meths (map (fn [[params & body]]
                                   (cons (apply vector 'this params) body))
                               meths)]
                (if-not (contains? fmap (name sym))
                (recur (assoc fmap (name sym) (cons `fn meths)) (next fs))
							 (throw (IllegalArgumentException.
										(str "Method '" (name sym) "' redefined")))))
              fmap)))
        p#)))

(defn proxy-call-with-super [call this meth]
 (let [m (proxy-mappings this)]
    (update-proxy this (assoc m meth nil))
    (try
      (call)
      (finally (update-proxy this m)))))

(defmacro proxy-super
  "Use to call a superclass method in the body of a proxy method.
  Note, expansion captures 'this"
  {:added "1.0"}
  [meth & args]
 `(proxy-call-with-super (fn [] (. ~'this ~meth ~@args))  ~'this ~(name meth)))

;; Arbace (hand change 14, doc/VENDOR-NOTES.md): bean finds the readable JavaBean properties by
;; reflection, as java.beans.Introspector does, without java.beans (module java.desktop), so
;; that it works on a runtime of java.base alone

(defn- bean-decapitalize
  "java.beans.Introspector/decapitalize."
  [^String s]
  (if (and (> (count s) 1) (Character/isUpperCase (.charAt s 1)) (Character/isUpperCase (.charAt s 0)))
    s
    (str (Character/toLowerCase (.charAt s 0)) (subs s 1))))

(defn- bean-accessible-method
  "The method of a public, exported class or interface that m (declared by a class that is not
  public) implements, as com.sun.beans.finder.MethodFinder/findAccessibleMethod finds it for
  a method without parameters, or nil."
  ^java.lang.reflect.Method [^java.lang.reflect.Method m]
  (let [c (.getDeclaringClass m)]
    (cond
      (not (.isExported (.getModule c) (.getPackageName c))) nil
      (Modifier/isPublic (.getModifiers c)) m
      (Modifier/isStatic (.getModifiers m)) nil
      :else (let [in (fn [^Class t]
                       (when t
                         (when-let [^java.lang.reflect.Method tm
                                    (try (.getMethod t (.getName m) (make-array Class 0))
                                         (catch NoSuchMethodException _ nil))]
                           (bean-accessible-method tm))))]
              (or (some in (.getInterfaces c)) (in (.getSuperclass c)))))))

(defn- bean-class-methods
  "The methods without parameters java.beans.Introspector takes from class c itself
  (com.sun.beans.introspect.MethodInfo/get): the public ones c declares (for a class that is
  not public, the public interface method each implements, ignored when it comes from a
  superclass, or the method itself when there is none), then the default methods of its
  interfaces, in MethodInfo's order (name, then return type name)."
  [^Class c]
  (let [no-params? (fn [^java.lang.reflect.Method m] (zero? (.getParameterCount m)))
        public? (Modifier/isPublic (.getModifiers c))
        own (remove nil? (map (fn [^java.lang.reflect.Method m]
                    (when (and (no-params? m) (= c (.getDeclaringClass m)))
                      (if public?
                        m
                        (let [a (bean-accessible-method m)]
                          (cond (nil? a) m
                                (.isInterface (.getDeclaringClass a)) a)))))
                  (.getMethods c)))
        ignorable #{AutoCloseable Cloneable java.io.Closeable Comparable}
        defaults (loop [q (vec (.getInterfaces c)) acc []]
                   (if-let [^Class i (peek q)]
                     (if (ignorable i)
                       (recur (pop q) acc)
                       (recur (into1 (pop q) (.getInterfaces i))
                              (into1 acc (filter (fn [^java.lang.reflect.Method m]
                                                  (and (no-params? m)
                                                       (not (Modifier/isAbstract (.getModifiers m)))
                                                       (not (.isBridge m))))
                                                (.getMethods i)))))
                     acc))]
    (sort-by (fn [^java.lang.reflect.Method m] [(.getName m) (.getName (.getReturnType m))])
             (concat own defaults))))

(defn- bean-class-reads
  "{name getter} for class c itself, as com.sun.beans.introspect.PropertyInfo/get chooses the
  read methods: isX() returning boolean first, else among the getX() with a result the last
  in order whose type is assignable to the one chosen so far (not a default method)."
  [^Class c]
  (let [prefixed (fn [^String n ^String p] (when (and (> (count n) (count p)) (.startsWith n p))
                                             (subs n (count p))))
        [is gets] (reduce1
                    (fn [[is gets] ^java.lang.reflect.Method m]
                      (let [n (.getName m) t (.getReturnType m)]
                        (cond
                          (Modifier/isStatic (.getModifiers m)) [is gets]
                          (and (= Boolean/TYPE t) (prefixed n "is"))
                            [(assoc is (prefixed n "is") m) gets]
                          (and (not= Void/TYPE t) (prefixed n "get"))
                            [is (update gets (prefixed n "get") (fnil conj []) m)]
                          :else [is gets])))
                    [{} {}] (bean-class-methods c))
        pick (fn [ms]
               (reduce1 (fn [^java.lang.reflect.Method r ^java.lang.reflect.Method m]
                          (if (and (not (.isDefault m))
                                   (.isAssignableFrom (.getReturnType r) (.getReturnType m)))
                            m r))
                        ms))]
    (into1 (into1 {} (map (fn [[k ms]] [(bean-decapitalize k) (pick ms)]) gets))
           (map (fn [[k m]] [(bean-decapitalize k) m]) is))))

(defn- bean-properties
  "{name getter}: the readable JavaBean properties of class c with a getter without
  parameters, as java.beans.Introspector/getBeanInfo finds them without an explicit BeanInfo:
  each class from Object down to c contributes its own read methods, a later one replacing an
  earlier one unless that one is an isX() of another name. Sorted by name, the Introspector's
  order, which bean's map keeps."
  [^Class c]
  (reduce1 (fn [props ^Class k]
             (reduce1 (fn [props [n ^java.lang.reflect.Method m]]
                        (let [^java.lang.reflect.Method g (props n)]
                          (if (and g (not= (.getName g) (.getName m)) (.startsWith (.getName g) "is"))
                            props
                            (assoc props n m))))
                      props (bean-class-reads k)))
           (sorted-map) (reverse (take-while some? (iterate #(.getSuperclass ^Class %) c)))))

(defn bean
  "Takes a Java object and returns a read-only implementation of the
  map abstraction based upon its JavaBean properties."
  {:added "1.0"}
  [^Object x]
  (let [c (. x (getClass))
	pmap (reduce1 (fn [m [name ^java.lang.reflect.Method method]]
			(assoc m (keyword name) (fn [] (arbace.lang.Reflector/prepRet (.getReturnType method) (. method (invoke x nil))))))
				 {}
				 (bean-properties c))
	v (fn [k] ((pmap k)))
        snapshot (fn []
                   (reduce1 (fn [m e]
                             (assoc m (key e) ((val e))))
                           {} (seq pmap)))
        thisfn (fn thisfn [plseq]
                 (lazy-seq
                   (when-let [pseq (seq plseq)]
                     (cons (arbace.lang.MapEntry/create (first pseq) (v (first pseq)))
                           (thisfn (rest pseq))))))]
    (proxy [arbace.lang.APersistentMap]
           []
      (iterator [] (arbace.lang.SeqIterator. ^java.util.Iterator (thisfn (keys pmap))))
      (containsKey [k] (contains? pmap k))
      (entryAt [k] (when (contains? pmap k) (arbace.lang.MapEntry/create k (v k))))
      (valAt ([k] (when (contains? pmap k) (v k)))
			 ([k default] (if (contains? pmap k) (v k) default)))
      (cons [m] (conj (snapshot) m))
      (count [] (count pmap))
      (assoc [k v] (assoc (snapshot) k v))
      (without [k] (dissoc (snapshot) k))
      (seq [] (thisfn (keys pmap))))))

