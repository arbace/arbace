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
;; /* rich Dec 18, 2007 */
;;
;; Converted from clojure/lang/APersistentVector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Collection
                    Iterator
                    List
                    ListIterator
                    NoSuchElementException
                    RandomAccess
                    Spliterator)
        '(java.util.function Consumer))

(defclass ^:public ^:abstract APersistentVector
  :extends AFn
  :implements [IPersistentVector Iterable List RandomAccess Comparable Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 4667575149454420891)

  (field ^int _hash)

  (field ^int _hasheq)

  (method ^:public toString ^String [this] (RT/printString this))

  (method ^:public seq ^ISeq [this]
    (when (> (.count this) 0) (Seq. this 0)))

  (method ^:public rseq ^ISeq [this]
    (when (> (.count this) 0) (RSeq. this (unchecked-subtract-int (.count this) 1))))

  (method ^:static doEquals ^boolean [^IPersistentVector v obj]
    (cond
      (instance? IPersistentVector obj)
        (let [ov (cast IPersistentVector obj)]
          (if (not (== (.count ov) (.count v)))
              false
              (do
                (loop [^int i 0]
                  (if (< i (.count v))
                      (if (not (Util/equals (.nth v i) (.nth ov i)))
                          (return false)
                          (recur (unchecked-inc-int i)))
                      nil))
                true)))
      (instance? List obj)
        (let [ma (cast Collection obj)]
          (if (or (not (== (.size ma) (.count v))) (not (== (.hashCode ma) (.hashCode v))))
              false
              (do
                (loop [i1 (.iterator (cast List v))
                       i2 (.iterator ma)]
                  (if (.hasNext i1)
                      (if (not (Util/equals (.next i1) (.next i2))) (return false) (recur i1 i2))
                      nil))
                true)))
      :else
        (do
          (when-not (instance? Sequential obj) (return false))
          (let [^:mutable ms (RT/seq obj)]
            (let [^:mutable ^int i 0]
              (while (< i (.count v))
                (when (or (nil? ms) (not (Util/equals (.nth v i) (.first ms)))) (return false))
                (set! i (unchecked-inc-int i))
                (set! ms (.next ms))))
            (when (some? ms) (return false)))
          true)))

  (method ^:static doEquiv ^boolean [^IPersistentVector v obj]
    (cond
      (instance? IPersistentVector obj)
        (let [ov (cast IPersistentVector obj)]
          (if (not (== (.count ov) (.count v)))
              false
              (do
                (loop [^int i 0]
                  (if (< i (.count v))
                      (if (not (Util/equiv (.nth v i) (.nth ov i)))
                          (return false)
                          (recur (unchecked-inc-int i)))
                      nil))
                true)))
      (instance? List obj)
        (let [ma (cast Collection obj)]
          (if (and (or (not (instance? IPersistentCollection ma)) (instance? Counted ma))
                   (not (== (.size ma) (.count v))))
              false
              (let [i2 (.iterator ma)]
                (loop [i1 (.iterator (cast List v))]
                  (if (.hasNext i1)
                      (if (or (not (.hasNext i2)) (not (Util/equiv (.next i1) (.next i2))))
                          (return false)
                          (recur i1))
                      nil))
                (not (.hasNext i2)))))
      :else
        (do
          (when-not (instance? Sequential obj) (return false))
          (let [^:mutable ms (RT/seq obj)]
            (let [^:mutable ^int i 0]
              (while (< i (.count v))
                (when (or (nil? ms) (not (Util/equiv (.nth v i) (.first ms)))) (return false))
                (set! i (unchecked-inc-int i))
                (set! ms (.next ms))))
            (when (some? ms) (return false)))
          true)))

  (method ^:public equals ^boolean [this obj]
    (if (identical? obj this) true (APersistentVector/doEquals this obj)))

  (method ^:public equiv ^boolean [this obj]
    (if (identical? obj this) true (APersistentVector/doEquiv this obj)))

  (method ^:public hashCode ^int [this]
    (let [^:mutable hash (.-_hash this)]
      (when (== hash 0)
        (set! hash 1)
        (loop [^int i 0]
          (when (< i (.count this))
            (let [obj (.nth this i)]
              (set! hash
                    (unchecked-add-int (unchecked-multiply-int 31 hash)
                                       (if (nil? obj) 0 (.hashCode obj))))
              (recur (unchecked-inc-int i)))))
        (set! (.-_hash this) hash))
      hash))

  (method ^:public hasheq ^int [this]
    (let [^:mutable hash (.-_hasheq this)]
      (when (== hash 0)
        (let [^:mutable ^int n 0]
          (set! hash 1)
          (set! n 0)
          (while (< n (.count this))
            (set! hash
                  (unchecked-add-int (unchecked-multiply-int 31 hash) (Util/hasheq (.nth this n))))
            (set! n (unchecked-inc-int n)))
          (set! (.-_hasheq this) (set! hash (Murmur3/mixCollHash hash n)))))
      hash))

  (method ^:public get [this ^int index] (.nth this index))

  (method ^:public nth [this ^int i notFound]
    (if (and (>= i 0) (< i (.count this))) (.nth this i) notFound))

  (method ^:public remove [this ^int i]
    (throw (UnsupportedOperationException.)))

  (method ^:public indexOf ^int [this o]
    (loop [^int i 0]
      (if (< i (.count this))
          (if (Util/equiv (.nth this i) o) (return i) (recur (unchecked-inc-int i)))
          nil))
    -1)

  (method ^:public lastIndexOf ^int [this o]
    (loop [^int i (unchecked-subtract-int (.count this) 1)]
      (if (>= i 0) (if (Util/equiv (.nth this i) o) (return i) (recur (unchecked-dec-int i))) nil))
    -1)

  (method ^:public listIterator ^ListIterator [this]
    (.listIterator this 0))

  (method ^:public listIterator ^ListIterator [this ^:final ^int index]
    (anon ListIterator []
      (field ^int nexti index)

      (method ^:public hasNext ^boolean [this]
        (< nexti (.count APersistentVector/this)))

      (method ^:public next [this]
        (if (< nexti (.count APersistentVector/this))
            (.nth APersistentVector/this
                  (let [old-1 nexti] (set! nexti (unchecked-inc-int nexti)) old-1))
            (throw (NoSuchElementException.))))

      (method ^:public hasPrevious ^boolean [this] (> nexti 0))

      (method ^:public previous [this]
        (if (> nexti 0)
            (.nth APersistentVector/this (set! nexti (unchecked-dec-int nexti)))
            (throw (NoSuchElementException.))))

      (method ^:public nextIndex ^int [this] nexti)

      (method ^:public previousIndex ^int [this]
        (unchecked-subtract-int nexti 1))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))

      (method ^:public set ^void [this o]
        (throw (UnsupportedOperationException.)))

      (method ^:public add ^void [this o]
        (throw (UnsupportedOperationException.)))))

  (method rangedIterator ^Iterator [this ^:final ^int start ^:final ^int end]
    (anon Iterator []
      (field ^int i start)

      (method ^:public hasNext ^boolean [this] (< i end))

      (method ^:public next [this]
        (if (< i end)
            (.nth APersistentVector/this (let [old-2 i] (set! i (unchecked-inc-int i)) old-2))
            (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (method rangedSpliterator ^Spliterator [this ^:final ^int start ^:final ^int end]
    (anon Spliterator []
      (field ^int i start)

      (method ^:public characteristics ^int [this]
        (bit-or-int (bit-or-int (bit-or-int Spliterator/IMMUTABLE Spliterator/ORDERED)
                                Spliterator/SIZED)
                    Spliterator/SUBSIZED))

      (method ^:public estimateSize ^long [this]
        (unchecked-subtract-int end i))

      (method ^:public getExactSizeIfKnown ^long [this]
        (unchecked-subtract-int end i))

      (method ^:public tryAdvance ^boolean [this ^Consumer action]
        (if (< i end)
            (do
              (.accept action
                       (.nth APersistentVector/this
                             (let [old-3 i] (set! i (unchecked-inc-int i)) old-3)))
              true)
            false))

      (method ^:public trySplit ^Spliterator [this]
        (let [lo i
              mid (unsigned-bit-shift-right-int (unchecked-add-int lo end) 1)]
          (when-not (>= lo mid) (set! i mid) (.rangedSpliterator APersistentVector/this lo mid))))

      (method ^:public forEachRemaining ^void [this ^Consumer action]
        (loop [^int x i]
          (when (< x end)
            (.accept action (.nth APersistentVector/this x))
            (recur (unchecked-inc-int x))))
        (set! i end))))

  (method ^:public spliterator ^Spliterator [this]
    (.rangedSpliterator this 0 (.count this)))

  (method ^:public subList ^List [this ^int fromIndex ^int toIndex]
    (cast List (RT/subvec this fromIndex toIndex)))

  (method ^:public set [this ^int i o]
    (throw (UnsupportedOperationException.)))

  (method ^:public add ^void [this ^int i o]
    (throw (UnsupportedOperationException.)))

  (method ^:public addAll ^boolean [this ^int i ^Collection c]
    (throw (UnsupportedOperationException.)))

  (method ^:public invoke [this arg1]
    (if (Util/isInteger arg1)
        (.nth this (.intValue (cast Number arg1)))
        (throw (IllegalArgumentException. "Key must be integer"))))

  (method ^:public iterator ^Iterator [this]
    (anon Iterator []
      (field ^int i 0)

      (method ^:public hasNext ^boolean [this]
        (< i (.count APersistentVector/this)))

      (method ^:public next [this]
        (if (< i (.count APersistentVector/this))
            (.nth APersistentVector/this (let [old-4 i] (set! i (unchecked-inc-int i)) old-4))
            (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (method ^:public peek [this]
    (when (> (.count this) 0) (.nth this (unchecked-subtract-int (.count this) 1))))

  (method ^:public containsKey ^boolean [this key]
    (if (not (Util/isInteger key))
        false
        (let [i (.intValue (cast Number key))] (and (>= i 0) (< i (.count this))))))

  (method ^:public entryAt ^IMapEntry [this key]
    (when (Util/isInteger key)
      (let [i (.intValue (cast Number key))]
        (when (and (>= i 0) (< i (.count this)))
          (return ^IMapEntry (MapEntry/create key (.nth this i))))))
    nil)

  (method ^:public assoc ^IPersistentVector [this key val]
    (if (Util/isInteger key)
        (let [i (.intValue (cast Number key))] (.assocN this i val))
        (throw (IllegalArgumentException. "Key must be integer"))))

  (method ^:public valAt [this key notFound]
    (when (Util/isInteger key)
      (let [i (.intValue (cast Number key))]
        (when (and (>= i 0) (< i (.count this))) (return (.nth this i)))))
    notFound)

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public toArray ^Object/1 [this]
    (let [ret (new Object/1 (.count this))]
      (loop [^int i 0]
        (when (< i (.count this)) (aset ret i (.nth this i)) (recur (unchecked-inc-int i))))
      ret))

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

  (method ^:public contains ^boolean [this o]
    (loop [s (.seq this)]
      (if (some? s) (if (Util/equiv (.first s) o) (return true) (recur (.next s))) nil))
    false)

  (method ^:public length ^int [this] (.count this))

  (method ^:public compareTo ^int [this o]
    (let [v (cast IPersistentVector o)]
      (cond
        (< (.count this) (.count v)) -1
        (> (.count this) (.count v)) 1
        :else
          (do
            (loop [^int i 0]
              (when (< i (.count this))
                (let [c (Util/compare (.nth this i) (.nth v i))]
                  (if (not (== c 0)) (return c) (recur (unchecked-inc-int i))))))
            0))))

  (defclass ^:static Seq
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:final ^IPersistentVector v)

    (field ^:final ^int i)

    (constructor ^:public [this ^IPersistentVector v ^int i]
      (set! (.-v this) v)
      (set! (.-i this) i))

    (constructor [this ^IPersistentMap meta ^IPersistentVector v ^int i]
      (super. meta)
      (set! (.-v this) v)
      (set! (.-i this) i))

    (method ^:public first [this] (.nth v i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (.count v)) (Seq. v (unchecked-add-int i 1))))

    (method ^:public index ^int [this] i)

    (method ^:public count ^int [this]
      (unchecked-subtract-int (.count v) i))

    (method ^:public withMeta ^Seq [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (Seq. meta v i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ret (.nth v i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (.count v))
            (set! ret (.invoke f ret (.nth v x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (.nth v i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (.count v))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (.nth v x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret))))

  (defclass ^:public ^:static RSeq
    :extends ASeq
    :implements [IndexedSeq Counted]

    (field ^:final ^IPersistentVector v)

    (field ^:final ^int i)

    (constructor ^:public [this ^IPersistentVector vector ^int i]
      (set! (.-v this) vector)
      (set! (.-i this) i))

    (constructor [this ^IPersistentMap meta ^IPersistentVector v ^int i]
      (super. meta)
      (set! (.-v this) v)
      (set! (.-i this) i))

    (method ^:public first [this] (.nth v i))

    (method ^:public next ^ISeq [this]
      (when (> i 0) (RSeq. v (unchecked-subtract-int i 1))))

    (method ^:public index ^int [this] i)

    (method ^:public count ^int [this] (unchecked-add-int i 1))

    (method ^:public withMeta ^RSeq [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (RSeq. meta v i))))

  (defclass ^:public ^:static SubVector
    :extends APersistentVector
    :implements [IObj IKVReduce]

    (field ^:public ^:final ^IPersistentVector v)

    (field ^:public ^:final ^int start)

    (field ^:public ^:final ^int end)

    (field ^:final ^IPersistentMap _meta)

    (constructor ^:public [this ^IPersistentMap meta ^:mutable ^IPersistentVector v
                           ^:mutable ^int start ^:mutable ^int end]
      (set! (.-_meta this) meta)
      (when (instance? SubVector v)
        (let [sv (cast SubVector v)]
          (set! start (unchecked-add-int start (.-start sv)))
          (set! end (unchecked-add-int end (.-start sv)))
          (set! v (.-v sv))))
      (set! (.-v this) v)
      (set! (.-start this) start)
      (set! (.-end this) end))

    (method ^:public iterator ^Iterator [this]
      (if (instance? APersistentVector v)
          (.rangedIterator (cast APersistentVector v) start end)
          (.iterator super)))

    (method ^:public spliterator ^Spliterator [this]
      (.rangedSpliterator (cast APersistentVector v) start end))

    (method ^:public kvreduce [this ^IFn f ^:mutable init]
      (let [cnt (.count this)]
        (loop [^int i 0]
          (when (< i cnt)
            (set! init (.invoke f init i (.nth v (unchecked-add-int start i))))
            (if (RT/isReduced init)
                (return (.deref (cast IDeref init)))
                (recur (unchecked-inc-int i)))))
        init))

    (method ^:public nth [this ^int i]
      (when (or (>= (unchecked-add-int start i) end) (< i 0)) (throw (IndexOutOfBoundsException.)))
      (.nth v (unchecked-add-int start i)))

    (method ^:public assocN ^IPersistentVector [this ^int i val]
      (cond
        (> (unchecked-add-int start i) end) (throw (IndexOutOfBoundsException.))
        (== (unchecked-add-int start i) end) (return (.cons this val)))
      (SubVector. _meta (.assocN v (unchecked-add-int start i) val) start end))

    (method ^:public count ^int [this] (unchecked-subtract-int end start))

    (method ^:public cons ^IPersistentVector [this o]
      (SubVector. _meta (.assocN v end o) start (unchecked-add-int end 1)))

    (method ^:public empty ^IPersistentCollection [this]
      (.withMeta PersistentVector/EMPTY (.meta this)))

    (method ^:public pop ^IPersistentStack [this]
      (if (== (unchecked-subtract-int end 1) start)
          PersistentVector/EMPTY
          (SubVector. _meta v start (unchecked-subtract-int end 1))))

    (method ^:public withMeta ^SubVector [this ^IPersistentMap meta]
      (if (identical? meta _meta) this (SubVector. meta v start end)))

    (method ^:public meta ^IPersistentMap [this] _meta)))
