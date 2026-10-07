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
;; /* rich Apr 19, 2008 */
;;
;; Converted from clojure/lang/Util.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException)
        '(java.lang.ref Reference ReferenceQueue)
        '(java.util Collection Map Map$Entry)
        '(java.util.concurrent ConcurrentHashMap))

(defclass ^:public Util
  (method ^:public ^:static equiv ^boolean [k1 k2]
    (cond
      (identical? k1 k2) true
      (some? k1)
        (cond
          (and (instance? Number k1) (instance? Number k2))
            (Numbers/equal (cast Number k1) (cast Number k2))
          (or (instance? IPersistentCollection k1) (instance? IPersistentCollection k2))
            (Util/pcequiv k1 k2)
          :else (.equals k1 k2))
      :else false))

  (defclass ^:public ^:interface EquivPred
    (method equiv ^boolean [this k1 k2]))

  (field ^:static ^EquivPred equivNull
    (anon EquivPred []
      (method ^:public equiv ^boolean [this k1 k2] (nil? k2))))

  (field ^:static ^EquivPred equivEquals
    (anon EquivPred []
      (method ^:public equiv ^boolean [this k1 k2] (.equals k1 k2))))

  (field ^:static ^EquivPred equivNumber
    (anon EquivPred []
      (method ^:public equiv ^boolean [this k1 k2]
        (if (instance? Number k2) (Numbers/equal (cast Number k1) (cast Number k2)) false))))

  (field ^:static ^EquivPred equivColl
    (anon EquivPred []
      (method ^:public equiv ^boolean [this k1 k2]
        (if (or (instance? IPersistentCollection k1) (instance? IPersistentCollection k2))
            (Util/pcequiv k1 k2)
            (.equals k1 k2)))))

  (method ^:public ^:static equivPred ^EquivPred [k1]
    (cond
      (nil? k1) equivNull
      (instance? Number k1) equivNumber
      (or (instance? String k1) (instance? Symbol k1)) equivEquals
      (or (instance? Collection k1) (instance? Map k1)) equivColl
      :else equivEquals))

  (method ^:public ^:static equiv ^boolean [^long k1 ^long k2] (== k1 k2))

  (method ^:public ^:static equiv ^boolean [k1 ^long k2]
    (^[Object Object] Util/equiv k1 (Long/valueOf k2)))

  (method ^:public ^:static equiv ^boolean [^long k1 k2]
    (^[Object Object] Util/equiv (Long/valueOf k1) k2))

  (method ^:public ^:static equiv ^boolean [^double k1 ^double k2]
    (== k1 k2))

  (method ^:public ^:static equiv ^boolean [k1 ^double k2]
    (^[Object Object] Util/equiv k1 (Double/valueOf k2)))

  (method ^:public ^:static equiv ^boolean [^double k1 k2]
    (^[Object Object] Util/equiv (Double/valueOf k1) k2))

  (method ^:public ^:static equiv ^boolean [^boolean k1 ^boolean k2]
    (= k1 k2))

  (method ^:public ^:static equiv ^boolean [k1 ^boolean k2]
    (^[Object Object] Util/equiv k1 (Boolean/valueOf k2)))

  (method ^:public ^:static equiv ^boolean [^boolean k1 k2]
    (^[Object Object] Util/equiv (Boolean/valueOf k1) k2))

  (method ^:public ^:static equiv ^boolean [^char c1 ^char c2] (== c1 c2))

  (method ^:public ^:static pcequiv ^boolean [k1 k2]
    (if (instance? IPersistentCollection k1)
        (.equiv (cast IPersistentCollection k1) k2)
        (.equiv (cast IPersistentCollection k2) k1)))

  (method ^:public ^:static equals ^boolean [k1 k2]
    (if (identical? k1 k2) true (and (some? k1) (.equals k1 k2))))

  (method ^:public ^:static identical ^boolean [k1 k2] (identical? k1 k2))

  (method ^:public ^:static classOf ^Class [x]
    (when (some? x) (.getClass x)))

  (method ^:public ^:static compare ^int [k1 k2]
    (cond
      (identical? k1 k2) 0
      (some? k1)
        (cond
          (nil? k2) 1
          (instance? Number k1) (Numbers/compare (cast Number k1) (cast Number k2))
          :else (.compareTo (cast Comparable k1) k2))
      :else -1))

  (method ^:public ^:static hash ^int [o] (if (nil? o) 0 (.hashCode o)))

  (method ^:public ^:static hasheq ^int [o]
    (cond
      (nil? o) 0
      (instance? IHashEq o) (Util/dohasheq (cast IHashEq o))
      (instance? Number o) (Numbers/hasheq (cast Number o))
      (instance? String o) (Murmur3/hashInt (.hashCode o))
      :else (.hashCode o)))

  (method ^:private ^:static dohasheq ^int [^IHashEq o] (.hasheq o))

  (method ^:public ^:static hashCombine ^int [^:mutable ^int seed ^int hash]
    (set! seed
          (bit-xor-int seed
                       (unchecked-add-int
                         (unchecked-add-int (unchecked-add-int hash (unchecked-int 0x9e3779b9))
                                            (bit-shift-left-int seed 6))
                         (bit-shift-right-int seed 2))))
    seed)

  (method ^:public ^:static isPrimitive ^boolean [^Class c]
    (and (and (some? c) (.isPrimitive c)) (not (identical? c Void/TYPE))))

  (method ^:public ^:static isInteger ^boolean [x]
    (or (or (or (instance? Long x) (instance? Integer x)) (instance? BigInt x))
        (instance? BigInteger x)))

  (method ^:public ^:static ret1 [ret nil_] ret)

  (method ^:public ^:static ret1 ^ISeq [^ISeq ret nil_] ret)

  (method ^:public ^:static clearCache :type-params [K V] ^void [^ReferenceQueue rq
                                                                 ^{:tag (ConcurrentHashMap K (Reference V))} cache]
    (when (some? (.poll rq))
      (while (some? (.poll rq)))
      (for-each [^{:tag (Map$Entry K (Reference V))} e (.entrySet cache)]
        (let [^{:tag (Reference V)} val (cast Reference (.getValue e))]
          (when (and (some? val) (nil? (.get val))) (.remove cache (.getKey e) val))))))

  (method ^:public ^:static runtimeException ^RuntimeException [^String s]
    (RuntimeException. s))

  (method ^:public ^:static runtimeException ^RuntimeException [^String s ^Throwable e]
    (RuntimeException. s e))

  (method ^:public ^:static sneakyThrow ^RuntimeException [^Throwable t]
    (when (nil? t) (throw (NullPointerException.)))
    (Util/sneakyThrow0 t)
    nil)

  (method ^:private ^:static sneakyThrow0 :type-params [(T extends Throwable)] :throws [T] ^void [^Throwable t]
    (throw t))

  (method ^:public ^:static loadWithClass :throws [IOException ClassNotFoundException] [^String scriptbase
                                                                                        ^{:tag (Class ?)} loadFrom]
    (RT/init)
    (Var/pushThreadBindings
      (RT/map (new Object/1 [arbace.lang.Compiler/LOADER (.getClassLoader loadFrom)])))
    (try (.invoke (RT/var "arbace.core" "load") scriptbase) (finally (Var/popThreadBindings))))

  (method ^:static isPosDigit ^boolean [^String s]
    (if (not (== (.length s) 1)) false (let [ch (.charAt s 0)] (and (<= ch \9) (>= ch \1)))))

  (method ^:public ^:static arrayTypeToSymbol ^Symbol [^Class c]
    (let [^:mutable ^int dim 0
          ^:mutable componentClass c]
      (while (.isArray componentClass)
        (when (> (set! dim (unchecked-inc-int dim)) 9) (break))
        (set! componentClass (.getComponentType componentClass)))
      (if (and (<= dim 9) (>= dim 1))
          (Symbol/intern (.getName componentClass) (Integer/toString dim))
          (Symbol/intern nil (.getName c))))))
