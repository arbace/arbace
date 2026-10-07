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
;; /* rich May 20, 2006 */
;;
;; Converted from clojure/lang/PersistentTreeMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Comparator
                    EmptyStackException
                    Iterator
                    Map
                    Map$Entry
                    NoSuchElementException
                    Stack))

(defclass ^:public PersistentTreeMap
  :extends APersistentMap
  :implements [IObj Reversible Sorted IKVReduce]

  (field ^:public ^:final ^Comparator comp)

  (field ^:public ^:final ^Node tree)

  (field ^:public ^:final ^int _count)

  (field ^:final ^IPersistentMap _meta)

  (field ^:public ^:static ^:final ^PersistentTreeMap EMPTY (PersistentTreeMap.))

  (method ^:public ^:static create ^IPersistentMap [^Map other]
    (let [^:mutable ^IPersistentMap ret EMPTY]
      (for-each [o (.entrySet other)]
        (let [e (cast Map$Entry o)] (set! ret (.assoc ret (.getKey e) (.getValue e)))))
      ret))

  (constructor ^:public [this] (this. RT/DEFAULT_COMPARATOR))

  (method ^:public withMeta ^PersistentTreeMap [this ^IPersistentMap meta]
    (if (identical? _meta meta) this (PersistentTreeMap. meta comp tree _count)))

  (constructor ^:private [this ^Comparator comp] (this. nil comp))

  (constructor ^:public [this ^IPersistentMap meta ^Comparator comp]
    (set! (.-comp this) comp)
    (set! (.-_meta this) meta)
    (set! tree nil)
    (set! _count 0))

  (constructor [this ^IPersistentMap meta ^Comparator comp ^Node tree ^int _count]
    (set! (.-_meta this) meta)
    (set! (.-comp this) comp)
    (set! (.-tree this) tree)
    (set! (.-_count this) _count))

  (method ^:public ^:static create ^PersistentTreeMap [^:mutable ^ISeq items]
    (let [^:mutable ^IPersistentMap ret EMPTY]
      (while (some? items)
        (when (nil? (.next items))
          (throw (IllegalArgumentException.
                   (String/format "No value supplied for key: %s" (new Object/1 [(.first items)])))))
        (set! ret (.assoc ret (.first items) (RT/second items)))
        (set! items (.next (.next items))))
      (cast PersistentTreeMap ret)))

  (method ^:public ^:static create ^PersistentTreeMap [^Comparator comp ^:mutable ^ISeq items]
    (let [^:mutable ^IPersistentMap ret (PersistentTreeMap. comp)]
      (while (some? items)
        (when (nil? (.next items))
          (throw (IllegalArgumentException.
                   (String/format "No value supplied for key: %s" (new Object/1 [(.first items)])))))
        (set! ret (.assoc ret (.first items) (RT/second items)))
        (set! items (.next (.next items))))
      (cast PersistentTreeMap ret)))

  (method ^:public containsKey ^boolean [this key]
    (some? (.entryAt this key)))

  (method ^:public equals ^boolean [this obj]
    (try (.equals super obj) (catch ClassCastException e false)))

  (method ^:public equiv ^boolean [this obj]
    (try (.equiv super obj) (catch ClassCastException e false)))

  (method ^:public assocEx ^PersistentTreeMap [this key val]
    (let [found (Box. nil)
          t (.add this tree key val found)]
      (when (nil? t) (throw (Util/runtimeException "Key already present")))
      (PersistentTreeMap. comp (.blacken t) (unchecked-add-int _count 1) (.meta this))))

  (method ^:public assoc ^PersistentTreeMap [this key val]
    (let [found (Box. nil)
          t (.add this tree key val found)]
      (if (nil? t)
          (let [foundNode (cast Node (.-val found))]
            (if (identical? (.val foundNode) val)
                this
                (PersistentTreeMap. comp (.replace this tree key val) _count (.meta this))))
          (PersistentTreeMap. comp (.blacken t) (unchecked-add-int _count 1) (.meta this)))))

  (method ^:public without ^PersistentTreeMap [this key]
    (let [found (Box. nil)
          t (.remove this tree key found)]
      (if (nil? t)
          (if (nil? (.-val found)) this (PersistentTreeMap. (.meta this) comp))
          (PersistentTreeMap. comp (.blacken t) (unchecked-subtract-int _count 1) (.meta this)))))

  (method ^:public seq ^ISeq [this]
    (when (> _count 0) (Seq/create tree true _count)))

  (method ^:public empty ^IPersistentCollection [this]
    (PersistentTreeMap. (.meta this) comp))

  (method ^:public rseq ^ISeq [this]
    (when (> _count 0) (Seq/create tree false _count)))

  (method ^:public comparator ^Comparator [this] comp)

  (method ^:public entryKey [this entry] (.key (cast IMapEntry entry)))

  (method ^:public seq ^ISeq [this ^boolean ascending]
    (when (> _count 0) (Seq/create tree ascending _count)))

  (method ^:public seqFrom ^ISeq [this key ^boolean ascending]
    (when (> _count 0)
      (let [^:mutable ^ISeq stack nil
            ^:mutable t tree]
        (while (some? t)
          (let [c (.doCompare this key (.-key t))]
            (cond
              (== c 0) (do (set! stack (RT/cons t stack)) (return (Seq. stack ascending)))
              ascending
                (if (< c 0)
                    (do (set! stack (RT/cons t stack)) (set! t (.left t)))
                    (set! t (.right t)))
              (> c 0) (do (set! stack (RT/cons t stack)) (set! t (.right t)))
              :else (set! t (.left t)))))
        (when (some? stack) (return (Seq. stack ascending)))))
    nil)

  (method ^:public iterator ^NodeIterator [this]
    (NodeIterator. tree true))

  (method ^:public kvreduce [this ^IFn f ^:mutable init]
    (when (some? tree) (set! init (.kvreduce tree f init)))
    (when (RT/isReduced init) (set! init (.deref (cast IDeref init))))
    init)

  (method ^:public reverseIterator ^NodeIterator [this]
    (NodeIterator. tree false))

  (method ^:public keys ^Iterator [this] (.keys this (.iterator this)))

  (method ^:public vals ^Iterator [this] (.vals this (.iterator this)))

  (method ^:public keys ^Iterator [this ^NodeIterator it]
    (KeyIterator. it))

  (method ^:public vals ^Iterator [this ^NodeIterator it]
    (ValIterator. it))

  (method ^:public minKey [this]
    (let [t (.min this)] (when (some? t) (.-key t))))

  (method ^:public min ^Node [this]
    (let [^:mutable t tree] (when (some? t) (while (some? (.left t)) (set! t (.left t)))) t))

  (method ^:public maxKey [this]
    (let [t (.max this)] (when (some? t) (.-key t))))

  (method ^:public max ^Node [this]
    (let [^:mutable t tree] (when (some? t) (while (some? (.right t)) (set! t (.right t)))) t))

  (method ^:public depth ^int [this] (.depth this tree))

  (method depth ^int [this ^Node t]
    (if (nil? t)
        0
        (unchecked-add-int 1 (Math/max (.depth this (.left t)) (.depth this (.right t))))))

  (method ^:public valAt [this key notFound]
    (let [n (.entryAt this key)] (if (some? n) (.val n) notFound)))

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public capacity ^int [this] _count)

  (method ^:public count ^int [this] _count)

  (method ^:public entryAt ^Node [this key]
    (let [^:mutable t tree]
      (while (some? t)
        (let [c (.doCompare this key (.-key t))]
          (cond (== c 0) (return t) (< c 0) (set! t (.left t)) :else (set! t (.right t)))))
      t))

  (method ^:public doCompare ^int [this k1 k2] (.compare comp k1 k2))

  (method add ^Node [this ^Node t key val ^Box found]
    (if (nil? t)
        (do
          (when (and (identical? comp RT/DEFAULT_COMPARATOR)
                     (not (or (or (nil? key) (instance? Number key)) (instance? Comparable key))))
            (throw (ClassCastException.
                     (java-str "Default comparator requires nil, Number, or Comparable: " key))))
          (if (nil? val) (Red. key) (RedVal. key val)))
        (let [c (.doCompare this key (.-key t))]
          (if (== c 0)
              (do (set! (.-val found) t) nil)
              (let [ins (if (< c 0)
                            (.add this (.left t) key val found)
                            (.add this (.right t) key val found))]
                (when (some? ins) (if (< c 0) (.addLeft t ins) (.addRight t ins))))))))

  (method remove ^Node [this ^Node t key ^Box found]
    (when (some? t)
      (let [c (.doCompare this key (.-key t))]
        (if (== c 0)
            (do (set! (.-val found) t) (PersistentTreeMap/append (.left t) (.right t)))
            (let [del (if (< c 0)
                          (.remove this (.left t) key found)
                          (.remove this (.right t) key found))]
              (when-not (and (nil? del) (nil? (.-val found)))
                (cond
                  (< c 0)
                    (if (instance? Black (.left t))
                        (PersistentTreeMap/balanceLeftDel (.-key t) (.val t) del (.right t))
                        (PersistentTreeMap/red (.-key t) (.val t) del (.right t)))
                  (instance? Black (.right t))
                    (PersistentTreeMap/balanceRightDel (.-key t) (.val t) (.left t) del)
                  :else (PersistentTreeMap/red (.-key t) (.val t) (.left t) del))))))))

  (method ^:static append ^Node [^Node left ^Node right]
    (cond
      (nil? left) right
      (nil? right) left
      (instance? Red left)
        (if (instance? Red right)
            (let [app (PersistentTreeMap/append (.right left) (.left right))]
              (if (instance? Red app)
                  (PersistentTreeMap/red (.-key app)
                                         (.val app)
                                         (PersistentTreeMap/red
                                           (.-key left)
                                           (.val left)
                                           (.left left)
                                           (.left app))
                                         (PersistentTreeMap/red
                                           (.-key right)
                                           (.val right)
                                           (.right app)
                                           (.right right)))
                  (PersistentTreeMap/red (.-key left)
                                         (.val left)
                                         (.left left)
                                         (PersistentTreeMap/red
                                           (.-key right)
                                           (.val right)
                                           app
                                           (.right right)))))
            (PersistentTreeMap/red (.-key left)
                                   (.val left)
                                   (.left left)
                                   (PersistentTreeMap/append (.right left) right)))
      (instance? Red right)
        (PersistentTreeMap/red (.-key right)
                               (.val right)
                               (PersistentTreeMap/append left (.left right))
                               (.right right))
      :else
        (let [app (PersistentTreeMap/append (.right left) (.left right))]
          (if (instance? Red app)
              (PersistentTreeMap/red (.-key app)
                                     (.val app)
                                     (PersistentTreeMap/black
                                       (.-key left)
                                       (.val left)
                                       (.left left)
                                       (.left app))
                                     (PersistentTreeMap/black
                                       (.-key right)
                                       (.val right)
                                       (.right app)
                                       (.right right)))
              (PersistentTreeMap/balanceLeftDel (.-key left)
                                                (.val left)
                                                (.left left)
                                                (PersistentTreeMap/black
                                                  (.-key right)
                                                  (.val right)
                                                  app
                                                  (.right right)))))))

  (method ^:static balanceLeftDel ^Node [key val ^Node del ^Node right]
    (cond
      (instance? Red del) (PersistentTreeMap/red key val (.blacken del) right)
      (instance? Black right) (PersistentTreeMap/rightBalance key val del (.redden right))
      (and (instance? Red right) (instance? Black (.left right)))
        (PersistentTreeMap/red (.-key (.left right))
                               (.val (.left right))
                               (PersistentTreeMap/black key val del (.left (.left right)))
                               (PersistentTreeMap/rightBalance
                                 (.-key right)
                                 (.val right)
                                 (.right (.left right))
                                 (.redden (.right right))))
      :else (throw (UnsupportedOperationException. "Invariant violation"))))

  (method ^:static balanceRightDel ^Node [key val ^Node left ^Node del]
    (cond
      (instance? Red del) (PersistentTreeMap/red key val left (.blacken del))
      (instance? Black left) (PersistentTreeMap/leftBalance key val (.redden left) del)
      (and (instance? Red left) (instance? Black (.right left)))
        (PersistentTreeMap/red (.-key (.right left))
                               (.val (.right left))
                               (PersistentTreeMap/leftBalance
                                 (.-key left)
                                 (.val left)
                                 (.redden (.left left))
                                 (.left (.right left)))
                               (PersistentTreeMap/black key val (.right (.right left)) del))
      :else (throw (UnsupportedOperationException. "Invariant violation"))))

  (method ^:static leftBalance ^Node [key val ^Node ins ^Node right]
    (cond
      (and (instance? Red ins) (instance? Red (.left ins)))
        (PersistentTreeMap/red (.-key ins)
                               (.val ins)
                               (.blacken (.left ins))
                               (PersistentTreeMap/black key val (.right ins) right))
      (and (instance? Red ins) (instance? Red (.right ins)))
        (PersistentTreeMap/red (.-key (.right ins))
                               (.val (.right ins))
                               (PersistentTreeMap/black
                                 (.-key ins)
                                 (.val ins)
                                 (.left ins)
                                 (.left (.right ins)))
                               (PersistentTreeMap/black key val (.right (.right ins)) right))
      :else (PersistentTreeMap/black key val ins right)))

  (method ^:static rightBalance ^Node [key val ^Node left ^Node ins]
    (cond
      (and (instance? Red ins) (instance? Red (.right ins)))
        (PersistentTreeMap/red (.-key ins)
                               (.val ins)
                               (PersistentTreeMap/black key val left (.left ins))
                               (.blacken (.right ins)))
      (and (instance? Red ins) (instance? Red (.left ins)))
        (PersistentTreeMap/red (.-key (.left ins))
                               (.val (.left ins))
                               (PersistentTreeMap/black key val left (.left (.left ins)))
                               (PersistentTreeMap/black
                                 (.-key ins)
                                 (.val ins)
                                 (.right (.left ins))
                                 (.right ins)))
      :else (PersistentTreeMap/black key val left ins)))

  (method replace ^Node [this ^Node t key val]
    (let [c (.doCompare this key (.-key t))]
      (.replace t
                (.-key t)
                (if (== c 0) val (.val t))
                (if (< c 0) (.replace this (.left t) key val) (.left t))
                (if (> c 0) (.replace this (.right t) key val) (.right t)))))

  (constructor [this ^Comparator comp ^Node tree ^int count ^IPersistentMap meta]
    (set! (.-_meta this) meta)
    (set! (.-comp this) comp)
    (set! (.-tree this) tree)
    (set! (.-_count this) count))

  (method ^:static red ^Red [key val ^Node left ^Node right]
    (cond
      (and (nil? left) (nil? right)) (if (nil? val) (Red. key) (RedVal. key val))
      (nil? val) (RedBranch. key left right)
      :else (RedBranchVal. key val left right)))

  (method ^:static black ^Black [key val ^Node left ^Node right]
    (cond
      (and (nil? left) (nil? right)) (if (nil? val) (Black. key) (BlackVal. key val))
      (nil? val) (BlackBranch. key left right)
      :else (BlackBranchVal. key val left right)))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (defclass ^:abstract ^:static Node
    :extends AMapEntry

    (field ^:final key)

    (constructor [this key] (set! (.-key this) key))

    (method ^:public key [this] key)

    (method ^:public val [this] nil)

    (method ^:public getKey [this] (.key this))

    (method ^:public getValue [this] (.val this))

    (method left ^Node [this] nil)

    (method right ^Node [this] nil)

    (method ^:abstract addLeft ^Node [this ^Node ins])

    (method ^:abstract addRight ^Node [this ^Node ins])

    (method ^:abstract removeLeft ^Node [this ^Node del])

    (method ^:abstract removeRight ^Node [this ^Node del])

    (method ^:abstract blacken ^Node [this])

    (method ^:abstract redden ^Node [this])

    (method balanceLeft ^Node [this ^Node parent]
      (PersistentTreeMap/black (.-key parent) (.val parent) this (.right parent)))

    (method balanceRight ^Node [this ^Node parent]
      (PersistentTreeMap/black (.-key parent) (.val parent) (.left parent) this))

    (method ^:abstract replace ^Node [this key val ^Node left ^Node right])

    (method ^:public kvreduce [this ^IFn f ^:mutable init]
      (when (some? (.left this))
        (set! init (.kvreduce (.left this) f init))
        (when (RT/isReduced init) (return init)))
      (set! init (.invoke f init (.key this) (.val this)))
      (if (RT/isReduced init)
          init
          (do (when (some? (.right this)) (set! init (.kvreduce (.right this) f init))) init))))

  (defclass ^:static Black
    :extends Node

    (constructor ^:public [this key] (super. key))

    (method addLeft ^Node [this ^Node ins] (.balanceLeft ins this))

    (method addRight ^Node [this ^Node ins] (.balanceRight ins this))

    (method removeLeft ^Node [this ^Node del]
      (PersistentTreeMap/balanceLeftDel (.-key this) (.val this) del (.right this)))

    (method removeRight ^Node [this ^Node del]
      (PersistentTreeMap/balanceRightDel (.-key this) (.val this) (.left this) del))

    (method blacken ^Node [this] this)

    (method redden ^Node [this] (Red. (.-key this)))

    (method replace ^Node [this key val ^Node left ^Node right]
      (PersistentTreeMap/black key val left right)))

  (defclass ^:static BlackVal
    :extends Black

    (field ^:final val)

    (constructor ^:public [this key val]
      (super. key)
      (set! (.-val this) val))

    (method ^:public val [this] val)

    (method redden ^Node [this] (RedVal. (.-key this) val)))

  (defclass ^:static BlackBranch
    :extends Black

    (field ^:final ^Node left)

    (field ^:final ^Node right)

    (constructor ^:public [this key ^Node left ^Node right]
      (super. key)
      (set! (.-left this) left)
      (set! (.-right this) right))

    (method ^:public left ^Node [this] left)

    (method ^:public right ^Node [this] right)

    (method redden ^Node [this] (RedBranch. (.-key this) left right)))

  (defclass ^:static BlackBranchVal
    :extends BlackBranch

    (field ^:final val)

    (constructor ^:public [this key val ^Node left ^Node right]
      (super. key left right)
      (set! (.-val this) val))

    (method ^:public val [this] val)

    (method redden ^Node [this]
      (RedBranchVal. (.-key this) val (.-left this) (.-right this))))

  (defclass ^:static Red
    :extends Node

    (constructor ^:public [this key] (super. key))

    (method addLeft ^Node [this ^Node ins]
      (PersistentTreeMap/red (.-key this) (.val this) ins (.right this)))

    (method addRight ^Node [this ^Node ins]
      (PersistentTreeMap/red (.-key this) (.val this) (.left this) ins))

    (method removeLeft ^Node [this ^Node del]
      (PersistentTreeMap/red (.-key this) (.val this) del (.right this)))

    (method removeRight ^Node [this ^Node del]
      (PersistentTreeMap/red (.-key this) (.val this) (.left this) del))

    (method blacken ^Node [this] (Black. (.-key this)))

    (method redden ^Node [this]
      (throw (UnsupportedOperationException. "Invariant violation")))

    (method replace ^Node [this key val ^Node left ^Node right]
      (PersistentTreeMap/red key val left right)))

  (defclass ^:static RedVal
    :extends Red

    (field ^:final val)

    (constructor ^:public [this key val]
      (super. key)
      (set! (.-val this) val))

    (method ^:public val [this] val)

    (method blacken ^Node [this] (BlackVal. (.-key this) val)))

  (defclass ^:static RedBranch
    :extends Red

    (field ^:final ^Node left)

    (field ^:final ^Node right)

    (constructor ^:public [this key ^Node left ^Node right]
      (super. key)
      (set! (.-left this) left)
      (set! (.-right this) right))

    (method ^:public left ^Node [this] left)

    (method ^:public right ^Node [this] right)

    (method balanceLeft ^Node [this ^Node parent]
      (cond
        (instance? Red left)
          (PersistentTreeMap/red (.-key this)
                                 (.val this)
                                 (.blacken left)
                                 (PersistentTreeMap/black
                                   (.-key parent)
                                   (.val parent)
                                   right
                                   (.right parent)))
        (instance? Red right)
          (PersistentTreeMap/red (.-key right)
                                 (.val right)
                                 (PersistentTreeMap/black
                                   (.-key this)
                                   (.val this)
                                   left
                                   (.left right))
                                 (PersistentTreeMap/black
                                   (.-key parent)
                                   (.val parent)
                                   (.right right)
                                   (.right parent)))
        :else (.balanceLeft super parent)))

    (method balanceRight ^Node [this ^Node parent]
      (cond
        (instance? Red right)
          (PersistentTreeMap/red (.-key this)
                                 (.val this)
                                 (PersistentTreeMap/black
                                   (.-key parent)
                                   (.val parent)
                                   (.left parent)
                                   left)
                                 (.blacken right))
        (instance? Red left)
          (PersistentTreeMap/red (.-key left)
                                 (.val left)
                                 (PersistentTreeMap/black
                                   (.-key parent)
                                   (.val parent)
                                   (.left parent)
                                   (.left left))
                                 (PersistentTreeMap/black
                                   (.-key this)
                                   (.val this)
                                   (.right left)
                                   right))
        :else (.balanceRight super parent)))

    (method blacken ^Node [this] (BlackBranch. (.-key this) left right)))

  (defclass ^:static RedBranchVal
    :extends RedBranch

    (field ^:final val)

    (constructor ^:public [this key val ^Node left ^Node right]
      (super. key left right)
      (set! (.-val this) val))

    (method ^:public val [this] val)

    (method blacken ^Node [this]
      (BlackBranchVal. (.-key this) val (.-left this) (.-right this))))

  (defclass ^:public ^:static Seq
    :extends ASeq

    (field ^:final ^ISeq stack)

    (field ^:final ^boolean asc)

    (field ^:final ^int cnt)

    (constructor ^:public [this ^ISeq stack ^boolean asc]
      (set! (.-stack this) stack)
      (set! (.-asc this) asc)
      (set! (.-cnt this) -1))

    (constructor ^:public [this ^ISeq stack ^boolean asc ^int cnt]
      (set! (.-stack this) stack)
      (set! (.-asc this) asc)
      (set! (.-cnt this) cnt))

    (constructor [this ^IPersistentMap meta ^ISeq stack ^boolean asc ^int cnt]
      (super. meta)
      (set! (.-stack this) stack)
      (set! (.-asc this) asc)
      (set! (.-cnt this) cnt))

    (method ^:static create ^Seq [^Node t ^boolean asc ^int cnt]
      (Seq. (Seq/push t nil asc) asc cnt))

    (method ^:static push ^ISeq [^:mutable ^Node t ^:mutable ^ISeq stack ^boolean asc]
      (while (some? t) (set! stack (RT/cons t stack)) (set! t (if asc (.left t) (.right t))))
      stack)

    (method ^:public first [this] (.first stack))

    (method ^:public next ^ISeq [this]
      (let [t (cast Node (.first stack))
            nextstack (Seq/push (if asc (.right t) (.left t)) (.next stack) asc)]
        (when (some? nextstack) (Seq. nextstack asc (unchecked-subtract-int cnt 1)))))

    (method ^:public count ^int [this] (if (< cnt 0) (.count super) cnt))

    (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (Seq. meta stack asc cnt))))

  (defclass ^:public ^:static NodeIterator
    :implements [Iterator]

    (field ^Stack stack (Stack.))

    (field ^boolean asc)

    (constructor [this ^Node t ^boolean asc]
      (set! (.-asc this) asc)
      (.push this t))

    (method push ^void [this ^:mutable ^Node t]
      (while (some? t) (.push stack t) (set! t (if asc (.left t) (.right t)))))

    (method ^:public hasNext ^boolean [this] (not (.isEmpty stack)))

    (method ^:public next [this]
      (try
        (let [t (cast Node (.pop stack))] (.push this (if asc (.right t) (.left t))) t)
        (catch EmptyStackException e (throw (NoSuchElementException.)))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.))))

  (defclass ^:static KeyIterator
    :implements [Iterator]

    (field ^NodeIterator it)

    (constructor [this ^NodeIterator it] (set! (.-it this) it))

    (method ^:public hasNext ^boolean [this] (.hasNext it))

    (method ^:public next [this] (.-key (cast Node (.next it))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.))))

  (defclass ^:static ValIterator
    :implements [Iterator]

    (field ^NodeIterator it)

    (constructor [this ^NodeIterator it] (set! (.-it this) it))

    (method ^:public hasNext ^boolean [this] (.hasNext it))

    (method ^:public next [this] (.val (cast Node (.next it))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.)))))
