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
;; Converted from clojure/lang/PersistentList.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util ArrayList
                    Collection
                    Collections
                    Iterator
                    LinkedList
                    List
                    ListIterator
                    NoSuchElementException))

(defclass ^:public PersistentList
  :extends ASeq
  :implements [IPersistentList IReduce List Counted]

  (field ^:private ^:static ^:final ^long serialVersionUID -8833289659955219995)

  (field ^:private ^:final _first)

  (field ^:private ^:final ^IPersistentList _rest)

  (field ^:private ^:final ^int _count)

  (defclass ^:public ^:static Primordial
    :extends RestFn

    (method ^:public ^:final getRequiredArity ^int [this] 0)

    (method ^:protected ^:final doInvoke [this args]
      (if (instance? ArraySeq args)
          (let [argsarray (.-array (cast ArraySeq args))
                ^:mutable ^IPersistentList ret EMPTY]
            (loop [^int i (unchecked-subtract-int (alength argsarray) 1)]
              (when (>= i (.-i (cast ArraySeq args)))
                (set! ret (cast IPersistentList (.cons ret (aget argsarray i))))
                (recur (unchecked-dec-int i))))
            ret)
          (let [list (LinkedList.)]
            (loop [s (RT/seq args)] (when (some? s) (.add list (.first s)) (recur (.next s))))
            (PersistentList/create list))))

    (method ^:public ^:static invokeStatic [^ISeq args]
      (if (instance? ArraySeq args)
          (let [argsarray (.-array (cast ArraySeq args))
                ^:mutable ^IPersistentList ret EMPTY]
            (loop [^int i (unchecked-subtract-int (alength argsarray) 1)]
              (when (>= i 0)
                (set! ret (cast IPersistentList (.cons ret (aget argsarray i))))
                (recur (unchecked-dec-int i))))
            ret)
          (let [list (LinkedList.)]
            (loop [s (RT/seq args)] (when (some? s) (.add list (.first s)) (recur (.next s))))
            (PersistentList/create list))))

    (method ^:public withMeta ^IObj [this ^IPersistentMap meta]
      (throw (UnsupportedOperationException.)))

    (method ^:public meta ^IPersistentMap [this] nil))

  (field ^:public ^:static ^IFn creator (Primordial.))

  (field ^:public ^:static ^:final ^EmptyList EMPTY (EmptyList. nil))

  (constructor ^:public [this first]
    (set! (.-_first this) first)
    (set! (.-_rest this) nil)
    (set! (.-_count this) 1))

  (constructor [this ^IPersistentMap meta _first ^IPersistentList _rest ^int _count]
    (super. meta)
    (set! (.-_first this) _first)
    (set! (.-_rest this) _rest)
    (set! (.-_count this) _count))

  (method ^:public ^:static create ^IPersistentList [^List init]
    (let [^:mutable ^IPersistentList ret EMPTY]
      (loop [i (.listIterator init (.size init))]
        (when (.hasPrevious i)
          (set! ret (cast IPersistentList (.cons ret (.previous i))))
          (recur i)))
      ret))

  (method ^:public first [this] _first)

  (method ^:public next ^ISeq [this]
    (when-not (== _count 1) (cast ISeq _rest)))

  (method ^:public peek [this] (.first this))

  (method ^:public pop ^IPersistentList [this]
    (if (nil? _rest) (.withMeta EMPTY (.-_meta this)) _rest))

  (method ^:public count ^int [this] _count)

  (method ^:public cons ^PersistentList [this o]
    (PersistentList. (.meta this) o this (unchecked-add-int _count 1)))

  (method ^:public empty ^IPersistentCollection [this]
    (.withMeta EMPTY (.meta this)))

  (method ^:public withMeta ^PersistentList [this ^IPersistentMap meta]
    (if (not (identical? meta (.-_meta this))) (PersistentList. meta _first _rest _count) this))

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable ret (.first this)]
      (loop [s (.next this)]
        (when (some? s)
          (set! ret (.invoke f ret (.first s)))
          (if (RT/isReduced ret) (return (.deref (cast IDeref ret))) (recur (.next s)))))
      ret))

  (method ^:public reduce [this ^IFn f start]
    (let [^:mutable ret (.invoke f start (.first this))]
      (loop [s (.next this)]
        (when (some? s)
          (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
          (set! ret (.invoke f ret (.first s)))
          (recur (.next s))))
      (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

  (defclass ^:static EmptyList
    :extends Obj
    :implements [IPersistentList List ISeq Counted IHashEq]

    (field ^:static ^:final ^int hasheq (Murmur3/hashOrdered Collections/EMPTY_LIST))

    (method ^:public hashCode ^int [this] 1)

    (method ^:public hasheq ^int [this] hasheq)

    (method ^:public toString ^String [this] "()")

    (method ^:public equals ^boolean [this o]
      (and (or (instance? Sequential o) (instance? List o)) (nil? (RT/seq o))))

    (method ^:public equiv ^boolean [this o] (.equals this o))

    (constructor [this ^IPersistentMap meta] (super. meta))

    (method ^:public first [this] nil)

    (method ^:public next ^ISeq [this] nil)

    (method ^:public more ^ISeq [this] this)

    (method ^:public cons ^PersistentList [this o]
      (PersistentList. (.meta this) o nil 1))

    (method ^:public empty ^IPersistentCollection [this] this)

    (method ^:public withMeta ^EmptyList [this ^IPersistentMap meta]
      (if (not (identical? meta (.meta this))) (EmptyList. meta) this))

    (method ^:public peek [this] nil)

    (method ^:public pop ^IPersistentList [this]
      (throw (IllegalStateException. "Can't pop empty list")))

    (method ^:public count ^int [this] 0)

    (method ^:public seq ^ISeq [this] nil)

    (method ^:public size ^int [this] 0)

    (method ^:public isEmpty ^boolean [this] true)

    (method ^:public contains ^boolean [this o] false)

    (method ^:public iterator ^Iterator [this]
      (anon Iterator []
        (method ^:public hasNext ^boolean [this] false)

        (method ^:public next [this] (throw (NoSuchElementException.)))

        (method ^:public remove ^void [this]
          (throw (UnsupportedOperationException.)))))

    (method ^:public toArray ^Object/1 [this] RT/EMPTY_ARRAY)

    (method ^:public add ^boolean [this o]
      (throw (UnsupportedOperationException.)))

    (method ^:public remove ^boolean [this o]
      (throw (UnsupportedOperationException.)))

    (method ^:public addAll ^boolean [this ^Collection collection]
      (throw (UnsupportedOperationException.)))

    (method ^:public clear ^void [this]
      (throw (UnsupportedOperationException.)))

    (method ^:public retainAll ^boolean [this ^Collection collection]
      (throw (UnsupportedOperationException.)))

    (method ^:public removeAll ^boolean [this ^Collection collection]
      (throw (UnsupportedOperationException.)))

    (method ^:public containsAll ^boolean [this ^Collection collection]
      (.isEmpty collection))

    (method ^:public toArray ^Object/1 [this ^Object/1 objects]
      (when (> (alength objects) 0) (aset objects 0 nil))
      objects)

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
      (throw (UnsupportedOperationException.)))))
