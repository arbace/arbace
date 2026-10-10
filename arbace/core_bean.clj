;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; Arbace (hand change 15, doc/VENDOR-NOTES.md): bean, moved here from core_proxy.clj, finds the
;; readable JavaBean properties by reflection, as java.beans.Introspector does, without
;; java.beans (module java.desktop), so that it works on a runtime of java.base alone. The JVM
;; and the Go build share this file (the Go build's arbace/lang/go/ns/core_bean.subst.clj only
;; answers bean-exported? without modules); arbace/core.clj loads it after core_proxy.clj.

(in-ns 'arbace.core)

(import '(java.lang.reflect Modifier))


(defn- bean-decapitalize
  "java.beans.Introspector/decapitalize."
  [^String s]
  (if (and (> (count s) 1) (Character/isUpperCase (.charAt s 1)) (Character/isUpperCase (.charAt s 0)))
    s
    (str (Character/toLowerCase (.charAt s 0)) (subs s 1))))

(defn- bean-exported?
  "Whether c's module exports its package (the Go build, which has no modules: always)."
  [^Class c]
  (.isExported (.getModule c) (.getPackageName c)))

(defn- bean-accessible-method
  "The method of a public, exported class or interface that m (declared by a class that is not
  public) implements, as com.sun.beans.finder.MethodFinder/findAccessibleMethod finds it for
  a method without parameters, or nil."
  ^java.lang.reflect.Method [^java.lang.reflect.Method m]
  (let [c (.getDeclaringClass m)]
    (cond
      (not (bean-exported? c)) nil
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

