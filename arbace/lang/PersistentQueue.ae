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
;; Converted from clojure/lang/PersistentQueue.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Collection Iterator NoSuchElementException))

(defclass ^:public PersistentQueue
  :extends Obj
  :implements [IPersistentList Collection Counted IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 8247184423915313132)

  (field ^:public ^:static ^:final ^PersistentQueue EMPTY (PersistentQueue. nil 0 nil nil))

  (field ^:final ^int cnt)

  (field ^:final ^ISeq f)

  (field ^:final ^PersistentVector r)

  (field ^int _hash)

  (field ^int _hasheq)

  (constructor [this ^IPersistentMap meta ^int cnt ^ISeq f ^PersistentVector r]
    (super. meta)
    (set! (.-cnt this) cnt)
    (set! (.-f this) f)
    (set! (.-r this) r))

  (method ^:public equiv ^boolean [this obj]
    (if (not (instance? Sequential obj))
        false
        (let [^:mutable ms (RT/seq obj)]
          (let [^:mutable s (.seq this)]
            (while (some? s)
              (when (or (nil? ms) (not (Util/equiv (.first s) (.first ms)))) (return false))
              (set! s (.next s))
              (set! ms (.next ms))))
          (nil? ms))))

  (method ^:public equals ^boolean [this obj]
    (if (not (instance? Sequential obj))
        false
        (let [^:mutable ms (RT/seq obj)]
          (let [^:mutable s (.seq this)]
            (while (some? s)
              (when (or (nil? ms) (not (Util/equals (.first s) (.first ms)))) (return false))
              (set! s (.next s))
              (set! ms (.next ms))))
          (nil? ms))))

  (method ^:public hashCode ^int [this]
    (let [^:mutable hash (.-_hash this)]
      (when (== hash 0)
        (set! hash 1)
        (loop [s (.seq this)]
          (when (some? s)
            (set! hash
                  (unchecked-add-int (unchecked-multiply-int 31 hash)
                                     (if (nil? (.first s)) 0 (.hashCode (.first s)))))
            (recur (.next s))))
        (set! (.-_hash this) hash))
      hash))

  (method ^:public hasheq ^int [this]
    (let [^:mutable cached (.-_hasheq this)]
      (when (== cached 0) (set! (.-_hasheq this) (set! cached (Murmur3/hashOrdered this))))
      cached))

  (method ^:public peek [this] (RT/first f))

  (method ^:public pop ^PersistentQueue [this]
    (if (nil? f)
        this
        (let [^:mutable f1 (.next f)
              ^:mutable r1 r]
          (when (nil? f1) (set! f1 (RT/seq r)) (set! r1 nil))
          (PersistentQueue. (.meta this) (unchecked-subtract-int cnt 1) f1 r1))))

  (method ^:public count ^int [this] cnt)

  (method ^:public seq ^ISeq [this] (when (some? f) (Seq. f (RT/seq r))))

  (method ^:public cons ^PersistentQueue [this o]
    (if (nil? f)
        (PersistentQueue. (.meta this) (unchecked-add-int cnt 1) (RT/list o) nil)
        (PersistentQueue. (.meta this)
                          (unchecked-add-int cnt 1)
                          f
                          (.cons (if (some? r) r PersistentVector/EMPTY) o))))

  (method ^:public empty ^IPersistentCollection [this]
    (.withMeta EMPTY (.meta this)))

  (method ^:public withMeta ^PersistentQueue [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (PersistentQueue. meta cnt f r)))

  (defclass ^:static Seq
    :extends ASeq

    (field ^:final ^ISeq f)

    (field ^:final ^ISeq rseq)

    (constructor [this ^ISeq f ^ISeq rseq]
      (set! (.-f this) f)
      (set! (.-rseq this) rseq))

    (constructor [this ^IPersistentMap meta ^ISeq f ^ISeq rseq]
      (super. meta)
      (set! (.-f this) f)
      (set! (.-rseq this) rseq))

    (method ^:public first [this] (.first f))

    (method ^:public next ^ISeq [this]
      (let [^:mutable f1 (.next f)
            ^:mutable r1 rseq]
        (when (nil? f1) (when (nil? rseq) (return nil)) (set! f1 rseq) (set! r1 nil))
        (Seq. f1 r1)))

    (method ^:public count ^int [this]
      (unchecked-add-int (RT/count f) (RT/count rseq)))

    (method ^:public withMeta ^Seq [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (Seq. meta f rseq))))

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
    (for-each [o c] (when (.contains this o) (return true)))
    false)

  (method ^:public toArray ^Object/1 [this ^Object/1 a]
    (RT/seqToPassedArray (.seq this) a))

  (method ^:public size ^int [this] (.count this))

  (method ^:public isEmpty ^boolean [this] (== (.count this) 0))

  (method ^:public contains ^boolean [this o]
    (loop [s (.seq this)]
      (if (some? s) (if (Util/equiv (.first s) o) (return true) (recur (.next s))) nil))
    false)

  (method ^:public iterator ^Iterator [this]
    (anon Iterator []
      (field ^:private ^ISeq fseq f)

      (field ^:private ^:final ^Iterator riter (when (some? r) (.iterator r)))

      (method ^:public hasNext ^boolean [this]
        (or (and (some? fseq) (some? (.seq fseq))) (and (some? riter) (.hasNext riter))))

      (method ^:public next [this]
        (cond
          (some? fseq) (let [ret (.first fseq)] (set! fseq (.next fseq)) ret)
          (and (some? riter) (.hasNext riter)) (.next riter)
          :else (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.))))))
