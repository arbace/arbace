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
;; Converted from clojure/lang/ATransientMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Map$Entry))

(defclass ^:public ^:abstract ATransientMap
  :extends AFn
  :implements [ITransientMap ITransientAssociative2]

  (method ^:abstract ensureEditable ^void [this])

  (method ^:abstract doAssoc ^ITransientMap [this key val])

  (method ^:abstract doWithout ^ITransientMap [this key])

  (method ^:abstract doValAt [this key notFound])

  (method ^:abstract doCount ^int [this])

  (method ^:abstract doPersistent ^IPersistentMap [this])

  (method ^:public conj ^ITransientMap [this o]
    (.ensureEditable this)
    (cond
      (instance? Map$Entry o) (let [e (cast Map$Entry o)] (.assoc this (.getKey e) (.getValue e)))
      (instance? IPersistentVector o)
        (let [v (cast IPersistentVector o)]
          (when-not (== (.count v) 2)
            (throw (IllegalArgumentException. "Vector arg to map conj must be a pair")))
          (.assoc this (.nth v 0) (.nth v 1)))
      :else
        (let [^:mutable ^ITransientMap ret this]
          (loop [es (RT/seq o)]
            (when (some? es)
              (let [e (cast Map$Entry (.first es))]
                (set! ret (.assoc ret (.getKey e) (.getValue e)))
                (recur (.next es)))))
          ret)))

  (method ^:public ^:final invoke [this arg1] (.valAt this arg1))

  (method ^:public ^:final invoke [this arg1 notFound]
    (.valAt this arg1 notFound))

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public assoc ^ITransientMap [this key val]
    (.ensureEditable this)
    (.doAssoc this key val))

  (method ^:public without ^ITransientMap [this key]
    (.ensureEditable this)
    (.doWithout this key))

  (method ^:public persistent ^IPersistentMap [this]
    (.ensureEditable this)
    (.doPersistent this))

  (method ^:public valAt [this key notFound]
    (.ensureEditable this)
    (.doValAt this key notFound))

  (field ^:private ^:static ^:final NOT_FOUND (Object.))

  (method ^:public containsKey ^boolean [this key]
    (.ensureEditable this)
    (not (identical? (.valAt this key NOT_FOUND) NOT_FOUND)))

  (method ^:public entryAt ^IMapEntry [this key]
    (.ensureEditable this)
    (let [v (.valAt this key NOT_FOUND)]
      (when-not (identical? v NOT_FOUND) (MapEntry/create key v))))

  (method ^:public count ^int [this]
    (.ensureEditable this)
    (.doCount this)))
