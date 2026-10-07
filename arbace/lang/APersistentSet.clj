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
;; /* rich Mar 3, 2008 */
;;
;; Converted from clojure/lang/APersistentSet.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Collection Iterator Set))

(defclass ^:public ^:abstract APersistentSet
  :extends AFn
  :implements [IPersistentSet Collection Set Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 889908853183699706)

  (field ^int _hash)

  (field ^int _hasheq)

  (field ^:final ^IPersistentMap impl)

  (constructor ^:protected [this ^IPersistentMap impl]
    (set! (.-impl this) impl))

  (method ^:public toString ^String [this] (RT/printString this))

  (method ^:public contains ^boolean [this key] (.containsKey impl key))

  (method ^:public get [this key] (.valAt impl key))

  (method ^:public count ^int [this] (.count impl))

  (method ^:public seq ^ISeq [this] (RT/keys impl))

  (method ^:public invoke [this arg1] (.valAt impl arg1))

  (method ^:public invoke [this arg1 arg2] (.valAt impl arg1 arg2))

  (method ^:public valAt [this key] (.valAt impl key))

  (method ^:public valAt [this key notFound] (.valAt impl key notFound))

  (method ^:public equals ^boolean [this obj]
    (APersistentSet/setEquals this obj))

  (method ^:public ^:static setEquals ^boolean [^IPersistentSet s1 obj]
    (cond
      (identical? s1 obj) true
      (not (instance? Set obj)) false
      :else
        (let [m (cast Set obj)]
          (if (not (== (.size m) (.count s1)))
              false
              (do (for-each [aM m] (when-not (.contains s1 aM) (return false))) true)))))

  (method ^:public equiv ^boolean [this obj]
    (if (not (instance? Set obj))
        false
        (let [m (cast Set obj)]
          (if (not (== (.size m) (.size this)))
              false
              (do (for-each [aM m] (when-not (.contains this aM) (return false))) true)))))

  (method ^:public hashCode ^int [this]
    (let [^:mutable hash (.-_hash this)]
      (when (== hash 0)
        (loop [s (.seq this)]
          (when (some? s)
            (let [e (.first s)]
              (set! hash (unchecked-add-int hash (Util/hash e)))
              (recur (.next s)))))
        (set! (.-_hash this) hash))
      hash))

  (method ^:public hasheq ^int [this]
    (let [^:mutable cached (.-_hasheq this)]
      (when (== cached 0) (set! (.-_hasheq this) (set! cached (Murmur3/hashUnordered this))))
      cached))

  (method ^:public toArray ^Object/1 [this] (RT/seqToArray (.seq this)))

  (method ^:public add ^boolean [this o]
    (throw (UnsupportedOperationException.)))

  (method ^:public remove ^boolean [this o]
    (throw (UnsupportedOperationException.)))

  (method ^:public addAll ^boolean [this ^Collection c]
    (throw (UnsupportedOperationException.)))

  (method ^:public clear ^void [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public retainAll ^boolean [this ^Collection c]
    (throw (UnsupportedOperationException.)))

  (method ^:public removeAll ^boolean [this ^Collection c]
    (throw (UnsupportedOperationException.)))

  (method ^:public containsAll ^boolean [this ^Collection c]
    (for-each [o c] (when-not (.contains this o) (return false)))
    true)

  (method ^:public toArray ^Object/1 [this ^Object/1 a]
    (RT/seqToPassedArray (.seq this) a))

  (method ^:public size ^int [this] (.count this))

  (method ^:public isEmpty ^boolean [this] (== (.count this) 0))

  (method ^:public iterator ^Iterator [this]
    (if (instance? IMapIterable impl)
        (.keyIterator (cast IMapIterable impl))
        (anon Iterator []
          (field ^:private ^:final ^Iterator iter (.iterator impl))

          (method ^:public hasNext ^boolean [this] (.hasNext iter))

          (method ^:public next [this] (.key (cast IMapEntry (.next iter))))

          (method ^:public remove ^void [this]
            (throw (UnsupportedOperationException.)))))))
