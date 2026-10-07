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
;; Converted from clojure/lang/APersistentMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util AbstractCollection AbstractSet Collection Iterator Map Map$Entry Set))

(defclass ^:public ^:abstract APersistentMap
  :extends AFn
  :implements [IPersistentMap Map Iterable Serializable MapEquivalence IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 6736310834519110267)

  (field ^int _hash)

  (field ^int _hasheq)

  (method ^:public toString ^String [this] (RT/printString this))

  (method ^:public cons ^IPersistentCollection [this o]
    (cond
      (instance? Map$Entry o) (let [e (cast Map$Entry o)] (.assoc this (.getKey e) (.getValue e)))
      (instance? IPersistentVector o)
        (let [v (cast IPersistentVector o)]
          (when-not (== (.count v) 2)
            (throw (IllegalArgumentException. "Vector arg to map conj must be a pair")))
          (.assoc this (.nth v 0) (.nth v 1)))
      :else
        (let [^:mutable ^IPersistentMap ret this]
          (loop [es (RT/seq o)]
            (when (some? es)
              (let [e (cast Map$Entry (.first es))]
                (set! ret (.assoc ret (.getKey e) (.getValue e)))
                (recur (.next es)))))
          ret)))

  (method ^:public equals ^boolean [this obj]
    (APersistentMap/mapEquals this obj))

  (method ^:public ^:static mapEquals ^boolean [^IPersistentMap m1 obj]
    (cond
      (identical? m1 obj) true
      (not (instance? Map obj)) false
      :else
        (let [m (cast Map obj)]
          (if (not (== (.size m) (.count m1)))
              false
              (do
                (loop [s (.seq m1)]
                  (when (some? s)
                    (let [e (cast Map$Entry (.first s))
                          found (.containsKey m (.getKey e))]
                      (if (or (not found) (not (Util/equals (.getValue e) (.get m (.getKey e)))))
                          (return false)
                          (recur (.next s))))))
                true)))))

  (method ^:public equiv ^boolean [this obj]
    (cond
      (not (instance? Map obj)) false
      (and (instance? IPersistentMap obj) (not (instance? MapEquivalence obj))) false
      :else
        (let [m (cast Map obj)]
          (if (not (== (.size m) (.size this)))
              false
              (do
                (loop [s (.seq this)]
                  (when (some? s)
                    (let [e (cast Map$Entry (.first s))
                          found (.containsKey m (.getKey e))]
                      (if (or (not found) (not (Util/equiv (.getValue e) (.get m (.getKey e)))))
                          (return false)
                          (recur (.next s))))))
                true)))))

  (method ^:public hashCode ^int [this]
    (let [^:mutable cached (.-_hash this)]
      (when (== cached 0) (set! (.-_hash this) (set! cached (APersistentMap/mapHash this))))
      cached))

  (method ^:public ^:static mapHash ^int [^IPersistentMap m]
    (let [^:mutable ^int hash 0]
      (loop [s (.seq m)]
        (when (some? s)
          (let [e (cast Map$Entry (.first s))]
            (set! hash
                  (unchecked-add-int hash
                                     (bit-xor-int
                                       (if (nil? (.getKey e)) 0 (.hashCode (.getKey e)))
                                       (if (nil? (.getValue e)) 0 (.hashCode (.getValue e))))))
            (recur (.next s)))))
      hash))

  (method ^:public hasheq ^int [this]
    (let [^:mutable cached (.-_hasheq this)]
      (when (== cached 0) (set! (.-_hasheq this) (set! cached (Murmur3/hashUnordered this))))
      cached))

  (method ^:public ^:static mapHasheq ^int [^IPersistentMap m]
    (Murmur3/hashUnordered m))

  (defclass ^:public ^:static KeySeq
    :extends ASeq

    (field ^:final ^ISeq seq)

    (field ^:final ^Iterable iterable)

    (method ^:public ^:static create ^KeySeq [^ISeq seq]
      (when (some? seq) (KeySeq. seq nil)))

    (method ^:public ^:static createFromMap ^KeySeq [^IPersistentMap map]
      (when (some? map) (let [seq (.seq map)] (when (some? seq) (KeySeq. seq map)))))

    (constructor ^:private [this ^ISeq seq ^Iterable iterable]
      (set! (.-seq this) seq)
      (set! (.-iterable this) iterable))

    (constructor ^:private [this ^IPersistentMap meta ^ISeq seq ^Iterable iterable]
      (super. meta)
      (set! (.-seq this) seq)
      (set! (.-iterable this) iterable))

    (method ^:public first [this] (.getKey (cast Map$Entry (.first seq))))

    (method ^:public next ^ISeq [this] (KeySeq/create (.next seq)))

    (method ^:public withMeta ^KeySeq [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (KeySeq. meta seq iterable)))

    (method ^:public iterator ^Iterator [this]
      (cond
        (nil? iterable) (.iterator super)
        (instance? IMapIterable iterable) (.keyIterator (cast IMapIterable iterable))
        :else
          (let [mapIter (.iterator iterable)]
            (anon Iterator []
              (method ^:public hasNext ^boolean [this] (.hasNext mapIter))

              (method ^:public next [this] (.getKey (cast Map$Entry (.next mapIter))))

              (method ^:public remove ^void [this]
                (throw (UnsupportedOperationException.))))))))

  (defclass ^:public ^:static ValSeq
    :extends ASeq

    (field ^:final ^ISeq seq)

    (field ^:final ^Iterable iterable)

    (method ^:public ^:static create ^ValSeq [^ISeq seq]
      (when (some? seq) (ValSeq. seq nil)))

    (method ^:public ^:static createFromMap ^ValSeq [^IPersistentMap map]
      (when (some? map) (let [seq (.seq map)] (when (some? seq) (ValSeq. seq map)))))

    (constructor ^:private [this ^ISeq seq ^Iterable iterable]
      (set! (.-seq this) seq)
      (set! (.-iterable this) iterable))

    (constructor ^:private [this ^IPersistentMap meta ^ISeq seq ^Iterable iterable]
      (super. meta)
      (set! (.-seq this) seq)
      (set! (.-iterable this) iterable))

    (method ^:public first [this] (.getValue (cast Map$Entry (.first seq))))

    (method ^:public next ^ISeq [this] (ValSeq/create (.next seq)))

    (method ^:public withMeta ^ValSeq [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ValSeq. meta seq iterable)))

    (method ^:public iterator ^Iterator [this]
      (cond
        (nil? iterable) (.iterator super)
        (instance? IMapIterable iterable) (.valIterator (cast IMapIterable iterable))
        :else
          (let [mapIter (.iterator iterable)]
            (anon Iterator []
              (method ^:public hasNext ^boolean [this] (.hasNext mapIter))

              (method ^:public next [this]
                (.getValue (cast Map$Entry (.next mapIter))))

              (method ^:public remove ^void [this]
                (throw (UnsupportedOperationException.))))))))

  (field ^:static ^:final ^IFn MAKE_ENTRY
    (anon AFn []
      (method ^:public invoke [this key val] (MapEntry/create key val))))

  (field ^:static ^:final ^IFn MAKE_KEY (anon AFn [] (method ^:public invoke [this key val] key)))

  (field ^:static ^:final ^IFn MAKE_VAL (anon AFn [] (method ^:public invoke [this key val] val)))

  (method ^:public invoke [this arg1] (.valAt this arg1))

  (method ^:public invoke [this arg1 notFound]
    (.valAt this arg1 notFound))

  (method ^:public clear ^void [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public containsValue ^boolean [this value]
    (.contains (.values this) value))

  (method ^:public entrySet ^Set [this]
    (anon AbstractSet []
      (method ^:public iterator ^Iterator [this]
        (.iterator APersistentMap/this))

      (method ^:public size ^int [this] (.count APersistentMap/this))

      (method ^:public hashCode ^int [this] (.hashCode APersistentMap/this))

      (method ^:public contains ^boolean [this o]
        (when (instance? Map$Entry o)
          (let [e (cast Map$Entry o)
                ^Map$Entry found (.entryAt APersistentMap/this (.getKey e))]
            (when (and (some? found) (Util/equals (.getValue found) (.getValue e))) (return true))))
        false)))

  (method ^:public get [this key] (.valAt this key))

  (method ^:public isEmpty ^boolean [this] (== (.count this) 0))

  (method ^:public keySet ^Set [this]
    (anon AbstractSet []
      (method ^:public iterator ^Iterator [this1]
        (let [mi (.iterator APersistentMap/this)]
          (anon Iterator []
            (method ^:public hasNext ^boolean [this] (.hasNext mi))

            (method ^:public next [this]
              (let [e (cast Map$Entry (.next mi))] (.getKey e)))

            (method ^:public remove ^void [this]
              (throw (UnsupportedOperationException.))))))

      (method ^:public size ^int [this] (.count APersistentMap/this))

      (method ^:public contains ^boolean [this o]
        (.containsKey APersistentMap/this o))))

  (method ^:public put [this key value]
    (throw (UnsupportedOperationException.)))

  (method ^:public putAll ^void [this ^Map t]
    (throw (UnsupportedOperationException.)))

  (method ^:public remove [this key]
    (throw (UnsupportedOperationException.)))

  (method ^:public size ^int [this] (.count this))

  (method ^:public values ^Collection [this]
    (anon AbstractCollection []
      (method ^:public iterator ^Iterator [this1]
        (let [mi (.iterator APersistentMap/this)]
          (anon Iterator []
            (method ^:public hasNext ^boolean [this] (.hasNext mi))

            (method ^:public next [this]
              (let [e (cast Map$Entry (.next mi))] (.getValue e)))

            (method ^:public remove ^void [this]
              (throw (UnsupportedOperationException.))))))

      (method ^:public size ^int [this] (.count APersistentMap/this)))))
