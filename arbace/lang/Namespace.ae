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
;; /* rich Jan 23, 2008 */
;;
;; Converted from clojure/lang/Namespace.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io ObjectStreamException Serializable)
        '(java.util.concurrent ConcurrentHashMap)
        '(java.util.concurrent.atomic AtomicReference))

(defclass ^:public Namespace
  :extends AReference
  :implements [Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID -3134444395801383865)

  (field ^:public ^:final ^Symbol name)

  (field ^:final ^:transient ^{:tag (AtomicReference IPersistentMap)} mappings (AtomicReference.))

  (field ^:final ^:transient ^{:tag (AtomicReference IPersistentMap)} aliases (AtomicReference.))

  (field ^:static ^:final ^{:tag (ConcurrentHashMap Symbol Namespace)} namespaces
    (ConcurrentHashMap.))

  (method ^:public toString ^String [this] (.toString name))

  (constructor [this ^Symbol name]
    (super. (.meta name))
    (set! (.-name this) name)
    (.set mappings RT/DEFAULT_IMPORTS)
    (.set aliases (^[Object/1] RT/map)))

  (method ^:public ^:static all ^ISeq [] (RT/seq (.values namespaces)))

  (method ^:public getName ^Symbol [this] name)

  (method ^:public getMappings ^IPersistentMap [this]
    (cast IPersistentMap (.get mappings)))

  (method ^:private isInternedMapping ^boolean [this ^Symbol sym o]
    (and (and (instance? Var o) (identical? (.-ns (cast Var o)) this))
         (.equals (.-sym (cast Var o)) sym)))

  (method ^:public intern ^Var [this ^Symbol sym]
    (when (some? (.-ns sym))
      (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol")))
    (let [^:mutable map (.getMappings this)
          ^:mutable ^Object o nil
          ^:mutable ^Var v nil]
      (while (nil? (set! o (.valAt map sym)))
        (when (nil? v) (set! v (Var. this sym)))
        (let [newMap (.assoc map sym v)]
          (.compareAndSet mappings map newMap)
          (set! map (.getMappings this))))
      (if (.isInternedMapping this sym o)
          (cast Var o)
          (do
            (when (nil? v) (set! v (Var. this sym)))
            (if (.checkReplacement this sym o v)
                (do
                  (while (not (.compareAndSet mappings map (.assoc map sym v)))
                    (set! map (.getMappings this)))
                  v)
                (cast Var o))))))

  (method ^:private checkReplacement ^boolean [this ^Symbol sym old neu]
    (when (instance? Var old)
      (let [ons (.-ns (cast Var old))
            nns (when (instance? Var neu) (.-ns (cast Var neu)))]
        (when (.isInternedMapping this sym old)
          (if (not (identical? nns RT/CLOJURE_NS))
              (do
                (.println (RT/errPrintWriter)
                          (java-str "REJECTED: attempt to replace interned var "
                                    old
                                    " with "
                                    neu
                                    " in "
                                    name
                                    ", you must ns-unmap first"))
                (return false))
              (return false)))))
    (.println (RT/errPrintWriter)
              (java-str "WARNING: "
                        sym
                        " already refers to: "
                        old
                        " in namespace: "
                        name
                        ", being replaced by: "
                        neu))
    true)

  (method reference [this ^Symbol sym val]
    (when (some? (.-ns sym))
      (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol")))
    (let [^:mutable map (.getMappings this)
          ^:mutable ^Object o nil]
      (while (nil? (set! o (.valAt map sym)))
        (let [newMap (.assoc map sym val)]
          (.compareAndSet mappings map newMap)
          (set! map (.getMappings this))))
      (cond
        (identical? o val) o
        (.checkReplacement this sym o val)
          (do
            (while (not (.compareAndSet mappings map (.assoc map sym val)))
              (set! map (.getMappings this)))
            val)
        :else o)))

  (method ^:public ^:static areDifferentInstancesOfSameClassName ^boolean [^Class cls1 ^Class cls2]
    (and (not (identical? cls1 cls2)) (.equals (.getName cls1) (.getName cls2))))

  (method referenceClass ^Class [this ^Symbol sym ^Class val]
    (when (some? (.-ns sym))
      (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol")))
    (let [^:mutable map (.getMappings this)
          ^:mutable c (cast Class (.valAt map sym))]
      (while (or (nil? c) (Namespace/areDifferentInstancesOfSameClassName c val))
        (let [newMap (.assoc map sym val)]
          (.compareAndSet mappings map newMap)
          (set! map (.getMappings this))
          (set! c (cast Class (.valAt map sym)))))
      (if (identical? c val)
          c
          (throw (IllegalStateException.
                   (java-str sym " already refers to: " c " in namespace: " name))))))

  (method ^:public unmap ^void [this ^Symbol sym]
    (when (some? (.-ns sym))
      (throw (IllegalArgumentException. "Can't unintern namespace-qualified symbol")))
    (let [^:mutable map (.getMappings this)]
      (while (.containsKey map sym)
        (let [newMap (.without map sym)]
          (.compareAndSet mappings map newMap)
          (set! map (.getMappings this))))))

  (method ^:public importClass ^Class [this ^Symbol sym ^Class c]
    (.referenceClass this sym c))

  (method ^:public importClass ^Class [this ^Class c]
    (let [n (.getName c)]
      (.importClass this (Symbol/intern (.substring n (unchecked-add-int (.lastIndexOf n \.) 1))) c)))

  (method ^:public refer ^Var [this ^Symbol sym ^Var var]
    (cast Var (.reference this sym var)))

  (method ^:public ^:static findOrCreate ^Namespace [^Symbol name]
    (let [^:mutable ns (cast Namespace (.get namespaces name))]
      (if (some? ns)
          ns
          (let [newns (Namespace. name)]
            (set! ns (cast Namespace (.putIfAbsent namespaces name newns)))
            (if (nil? ns) newns ns)))))

  (method ^:public ^:static remove ^Namespace [^Symbol name]
    (when (.equals name (.-name RT/CLOJURE_NS))
      (throw (IllegalArgumentException. "Cannot remove clojure namespace")))
    (cast Namespace (.remove namespaces name)))

  (method ^:public ^:static find ^Namespace [^Symbol name]
    (cast Namespace (.get namespaces name)))

  (method ^:public getMapping [this ^Symbol name]
    (.valAt (cast IPersistentMap (.get mappings)) name))

  (method ^:public findInternedVar ^Var [this ^Symbol symbol]
    (let [o (.valAt (cast IPersistentMap (.get mappings)) symbol)]
      (when (and (and (some? o) (instance? Var o)) (identical? (.-ns (cast Var o)) this))
        (cast Var o))))

  (method ^:public getAliases ^IPersistentMap [this]
    (cast IPersistentMap (.get aliases)))

  (method ^:public lookupAlias ^Namespace [this ^Symbol alias]
    (let [map (.getAliases this)] (cast Namespace (.valAt map alias))))

  (method ^:public addAlias ^void [this ^Symbol alias ^Namespace ns]
    (when (or (nil? alias) (nil? ns))
      (throw (NullPointerException. "Expecting Symbol + Namespace")))
    (let [^:mutable map (.getAliases this)]
      (while (not (.containsKey map alias))
        (let [newMap (.assoc map alias ns)]
          (.compareAndSet aliases map newMap)
          (set! map (.getAliases this))))
      (when-not (.equals (.valAt map alias) ns)
        (throw (IllegalStateException.
                 (java-str "Alias "
                           alias
                           " already exists in namespace "
                           name
                           ", aliasing "
                           (.valAt map alias)))))))

  (method ^:public removeAlias ^void [this ^Symbol alias]
    (let [^:mutable map (.getAliases this)]
      (while (.containsKey map alias)
        (let [newMap (.without map alias)]
          (.compareAndSet aliases map newMap)
          (set! map (.getAliases this))))))

  (method ^:private readResolve :throws [ObjectStreamException] [this]
    (Namespace/findOrCreate name)))
