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
;; Converted from clojure/lang/ASeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util ArrayList Collection Collections Iterator List ListIterator))

(defclass ^:public ^:abstract ASeq
  :extends Obj
  :implements [ISeq Sequential List Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 4748650717905139299)

  (field ^:transient ^int _hash)

  (field ^:transient ^int _hasheq)

  (method ^:public toString ^String [this] (RT/printString this))

  (method ^:public empty ^IPersistentCollection [this]
    PersistentList/EMPTY)

  (constructor ^:protected [this ^IPersistentMap meta] (super. meta))

  (constructor ^:protected [this])

  (method ^:public equiv ^boolean [this obj]
    (cond
      (not (or (instance? Sequential obj) (instance? List obj))) false
      (and (and (instance? Counted this) (instance? Counted obj))
           (not (== (.count (cast Counted this)) (.count (cast Counted obj)))))
        false
      :else
        (let [^:mutable ms (RT/seq obj)]
          (let [^:mutable s (.seq this)]
            (while (some? s)
              (when (or (nil? ms) (not (Util/equiv (.first s) (.first ms)))) (return false))
              (set! s (.next s))
              (set! ms (.next ms))))
          (nil? ms))))

  (method ^:public equals ^boolean [this obj]
    (cond
      (identical? this obj) true
      (not (or (instance? Sequential obj) (instance? List obj))) false
      :else
        (let [^:mutable ms (RT/seq obj)]
          (let [^:mutable s (.seq this)]
            (while (some? s)
              (when (or (nil? ms) (not (Util/equals (.first s) (.first ms)))) (return false))
              (set! s (.next s))
              (set! ms (.next ms))))
          (nil? ms))))

  (method ^:public hashCode ^int [this]
    (when (== _hash 0)
      (let [^:mutable ^int hash 1]
        (loop [s (.seq this)]
          (when (some? s)
            (set! hash
                  (unchecked-add-int (unchecked-multiply-int 31 hash)
                                     (if (nil? (.first s)) 0 (.hashCode (.first s)))))
            (recur (.next s))))
        (set! (.-_hash this) hash)))
    _hash)

  (method ^:public hasheq ^int [this]
    (when (== _hasheq 0) (set! _hasheq (Murmur3/hashOrdered this)))
    _hasheq)

  (method ^:public count ^int [this]
    (let [^:mutable ^int i 1]
      (let [^:mutable s (.next this)]
        (while (some? s)
          (when (instance? Counted s) (return (unchecked-add-int i (.count s))))
          (set! s (.next s))
          (set! i (unchecked-inc-int i))))
      i))

  (method ^:public ^:final seq ^ISeq [this] this)

  (method ^:public cons ^ISeq [this o] (Cons. o this))

  (method ^:public more ^ISeq [this]
    (let [s (.next this)] (if (nil? s) PersistentList/EMPTY s)))

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

  (method ^:public isEmpty ^boolean [this] (nil? (.seq this)))

  (method ^:public contains ^boolean [this o]
    (loop [s (.seq this)]
      (if (some? s) (if (Util/equiv (.first s) o) (return true) (recur (.next s))) nil))
    false)

  (method ^:public iterator ^Iterator [this] (SeqIterator. this))

  (method ^:private reify ^List [this]
    (Collections/unmodifiableList (ArrayList. this)))

  (method ^:public subList ^List [this ^int fromIndex ^int toIndex]
    (.subList (.reify this) fromIndex toIndex))

  (method ^:public set [this ^int index element]
    (throw (UnsupportedOperationException.)))

  (method ^:public remove [this ^int index]
    (throw (UnsupportedOperationException.)))

  (method ^:public indexOf ^int [this o]
    (let [^:mutable s (.seq this)]
      (let [^:mutable ^int i 0]
        (while (some? s)
          (when (Util/equiv (.first s) o) (return i))
          (set! s (.next s))
          (set! i (unchecked-inc-int i))))
      -1))

  (method ^:public lastIndexOf ^int [this o]
    (.lastIndexOf (.reify this) o))

  (method ^:public listIterator ^ListIterator [this]
    (.listIterator (.reify this)))

  (method ^:public listIterator ^ListIterator [this ^int index]
    (.listIterator (.reify this) index))

  (method ^:public get [this ^int index] (RT/nth this index))

  (method ^:public add ^void [this ^int index element]
    (throw (UnsupportedOperationException.)))

  (method ^:public addAll ^boolean [this ^int index ^Collection c]
    (throw (UnsupportedOperationException.))))
