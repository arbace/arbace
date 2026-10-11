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
;; /* rich Jan 31, 2009 */
;;
;; Converted from clojure/lang/LazySeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException ObjectOutputStream)
        '(java.util ArrayList Collection Iterator List ListIterator)
        '(java.util.concurrent.locks Lock ReentrantLock))

(defclass ^:public ^:final LazySeq
  :extends Obj
  :implements [ISeq Sequential List IPending IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID -7531333024710395876)

  (field ^:private ^:transient ^IFn fn)

  (field ^:private sv)

  (field ^:private ^ISeq s)

  (field ^:private ^:volatile ^Lock lock)

  (constructor ^:public [this ^IFn f]
    (set! fn f)
    (set! lock (ReentrantLock.)))

  (constructor ^:private [this ^IPersistentMap meta ^ISeq seq]
    (super. meta)
    (set! fn nil)
    (set! s seq))

  (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (LazySeq. meta (.seq this))))

  (method ^:private ^:final force ^void [this]
    (when (some? fn) (set! sv (.invoke fn)) (set! fn nil)))

  (method ^:private ^:final sval [this]
    (let [l lock]
      (when (some? l)
        (.lock l)
        (try (when (some? lock) (.force this) (return sv)) (finally (.unlock l))))
      s))

  (method ^:private ^:final unwrap [this ^:mutable ls]
    (while (instance? LazySeq ls) (set! ls (.sval (cast LazySeq ls))))
    ls)

  (method ^:private ^:final realize ^void [this]
    (let [l lock]
      (when (some? l)
        (.lock l)
        (try
          (when (some? lock)
            (.force this)
            (let [^:mutable ls sv]
              (set! sv nil)
              (when (instance? LazySeq ls) (set! ls (.unwrap this ls)))
              (set! s (RT/seq ls))
              (set! lock nil)))
          (finally (.unlock l))))))

  (method ^:public ^:final seq ^ISeq [this]
    (when (some? lock) (.realize this))
    s)

  (method ^:public count ^int [this]
    (let [^:mutable ^int c 0]
      (loop [s (.seq this)] (when (some? s) (set! c (unchecked-inc-int c)) (recur (.next s))))
      c))

  (method ^:public first [this] (.seq this) (when (some? s) (.first s)))

  (method ^:public next ^ISeq [this]
    (.seq this)
    (when (some? s) (.next s)))

  (method ^:public more ^ISeq [this]
    (.seq this)
    (if (nil? s) PersistentList/EMPTY (.more s)))

  (method ^:public cons ^ISeq [this o] (RT/cons o (.seq this)))

  (method ^:public empty ^IPersistentCollection [this]
    PersistentList/EMPTY)

  (method ^:public equiv ^boolean [this o]
    (let [s (.seq this)]
      (if (some? s)
          (.equiv s o)
          (and (or (instance? Sequential o) (instance? List o)) (nil? (RT/seq o))))))

  (method ^:public hashCode ^int [this]
    (let [s (.seq this)] (if (nil? s) 1 (Util/hash s))))

  (method ^:public hasheq ^int [this] (Murmur3/hashOrdered this))

  (method ^:public equals ^boolean [this o]
    (let [s (.seq this)]
      (if (some? s)
          (.equals s o)
          (and (or (instance? Sequential o) (instance? List o)) (nil? (RT/seq o))))))

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

  (method ^:private reify ^List [this] (ArrayList. this))

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
    (throw (UnsupportedOperationException.)))

  (method ^:public isRealized ^boolean [this] (nil? lock))

  (method ^:private writeObject :throws [IOException] ^void [this ^ObjectOutputStream out]
    (let [^:mutable ^ISeq s this] (while (some? s) (set! s (.next s))) (.defaultWriteObject out))))
