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
;; /* rich Jul 5, 2007 */
;;
;; Converted from clojure/lang/PersistentVector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util ArrayList Iterator List NoSuchElementException Spliterator)
        '(java.util.concurrent.atomic AtomicReference)
        '(java.util.function Consumer))

(defclass ^:public PersistentVector
  :extends APersistentVector
  :implements [IObj IEditableCollection IReduce IKVReduce IDrop]

  (field ^:private ^:static ^:final ^long serialVersionUID -7896022351281214157)

  (defclass ^:public ^:static Node
    :implements [Serializable]

    (field ^:public ^:final ^:transient ^{:tag (AtomicReference Thread)} edit)

    (field ^:public ^:final ^Object/1 array)

    (constructor ^:public [this ^{:tag (AtomicReference Thread)} edit ^Object/1 array]
      (set! (.-edit this) edit)
      (set! (.-array this) array))

    (constructor [this ^{:tag (AtomicReference Thread)} edit]
      (set! (.-edit this) edit)
      (set! (.-array this) (new Object/1 32))))

  (field ^:static ^:final ^{:tag (AtomicReference Thread)} NOEDIT (AtomicReference. nil))

  (field ^:public ^:static ^:final ^Node EMPTY_NODE (Node. NOEDIT (new Object/1 32)))

  (field ^:final ^int cnt)

  (field ^:public ^:final ^int shift)

  (field ^:public ^:final ^Node root)

  (field ^:public ^:final ^Object/1 tail)

  (field ^:final ^IPersistentMap _meta)

  (field ^:public ^:static ^:final ^PersistentVector EMPTY
    (PersistentVector. 0 5 EMPTY_NODE (new Object/1 [])))

  (field ^:private ^:static ^:final ^IFn TRANSIENT_VECTOR_CONJ
    (anon AFn []
      (method ^:public invoke [this coll val]
        (.conj (cast ITransientVector coll) val))

      (method ^:public invoke [this coll] coll)))

  (method ^:public ^:static adopt ^PersistentVector [^Object/1 items]
    (PersistentVector. (alength items) 5 EMPTY_NODE items))

  (method ^:public ^:static create ^PersistentVector [^IReduceInit items]
    (let [ret (.asTransient EMPTY)] (.reduce items TRANSIENT_VECTOR_CONJ ret) (.persistent ret)))

  (method ^:public ^:static create ^PersistentVector [^:mutable ^ISeq items]
    (let [arr (new Object/1 32)
          ^:mutable ^int i 0]
      (while (and (some? items) (< i 32))
        (aset arr i (.first items))
        (set! i (unchecked-inc-int i))
        (set! items (.next items)))
      (cond
        (some? items)
          (let [start (PersistentVector. 32 5 EMPTY_NODE arr)
                ^:mutable ret (.asTransient start)]
            (while (some? items) (set! ret (.conj ret (.first items))) (set! items (.next items)))
            (.persistent ret))
        (== i 32) (PersistentVector. 32 5 EMPTY_NODE arr)
        :else
          (let [arr2 (new Object/1 i)]
            (System/arraycopy arr 0 arr2 0 i)
            (PersistentVector. i 5 EMPTY_NODE arr2)))))

  (method ^:public ^:static create ^PersistentVector [^List list]
    (let [size (.size list)]
      (if (<= size 32)
          (PersistentVector. size 5 PersistentVector/EMPTY_NODE (.toArray list))
          (let [^:mutable ret (.asTransient EMPTY)]
            (loop [^int i 0]
              (when (< i size) (set! ret (.conj ret (.get list i))) (recur (unchecked-inc-int i))))
            (.persistent ret)))))

  (method ^:public ^:static create ^PersistentVector [^Iterable items]
    (if (instance? ArrayList items)
        (PersistentVector/create (cast ArrayList items))
        (let [iter (.iterator items)
              ^:mutable ret (.asTransient EMPTY)]
          (while (.hasNext iter) (set! ret (.conj ret (.next iter))))
          (.persistent ret))))

  (method ^:public ^:static create ^PersistentVector [& ^Object/1 items]
    (let [^:mutable ret (.asTransient EMPTY)]
      (for-each [item items] (set! ret (.conj ret item)))
      (.persistent ret)))

  (constructor [this ^int cnt ^int shift ^Node root ^Object/1 tail]
    (set! (.-_meta this) nil)
    (set! (.-cnt this) cnt)
    (set! (.-shift this) shift)
    (set! (.-root this) root)
    (set! (.-tail this) tail))

  (constructor [this ^IPersistentMap meta ^int cnt ^int shift ^Node root ^Object/1 tail]
    (set! (.-_meta this) meta)
    (set! (.-cnt this) cnt)
    (set! (.-shift this) shift)
    (set! (.-root this) root)
    (set! (.-tail this) tail))

  (method ^:public asTransient ^TransientVector [this]
    (TransientVector. this))

  (method ^:final tailoff ^int [this]
    (if (< cnt 32)
        0
        (bit-shift-left-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) 5) 5)))

  (method ^:public arrayFor ^Object/1 [this ^int i]
    (if (and (>= i 0) (< i cnt))
        (if (>= i (.tailoff this))
            tail
            (let [^:mutable node root]
              (loop [^int level shift]
                (when (> level 0)
                  (set! node
                        (cast Node
                              (aget (.-array node)
                                    (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f))))
                  (recur (unchecked-subtract-int level 5))))
              (.-array node)))
        (throw (IndexOutOfBoundsException.))))

  (method ^:public nth [this ^int i]
    (let [node (.arrayFor this i)] (aget node (bit-and-int i 0x01f))))

  (method ^:public nth [this ^int i notFound]
    (if (and (>= i 0) (< i cnt)) (.nth this i) notFound))

  (method ^:public assocN ^PersistentVector [this ^int i val]
    (cond
      (and (>= i 0) (< i cnt))
        (if (>= i (.tailoff this))
            (let [newTail (new Object/1 (alength tail))]
              (System/arraycopy tail 0 newTail 0 (alength tail))
              (aset newTail (bit-and-int i 0x01f) val)
              (PersistentVector. (.meta this) cnt shift root newTail))
            (PersistentVector. (.meta this)
                               cnt
                               shift
                               (PersistentVector/doAssoc shift root i val)
                               tail))
      (== i cnt) (.cons this val)
      :else (throw (IndexOutOfBoundsException.))))

  (method ^:private ^:static doAssoc ^Node [^int level ^Node node ^int i val]
    (let [ret (Node. (.-edit node) (.clone (.-array node)))]
      (if (== level 0)
          (aset (.-array ret) (bit-and-int i 0x01f) val)
          (let [subidx (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f)]
            (aset (.-array ret)
                  subidx
                  (PersistentVector/doAssoc (unchecked-subtract-int level 5)
                                            (cast Node (aget (.-array node) subidx))
                                            i
                                            val))))
      ret))

  (method ^:public count ^int [this] cnt)

  (method ^:public withMeta ^PersistentVector [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (PersistentVector. meta cnt shift root tail)))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (method ^:public cons ^PersistentVector [this val]
    (if (< (unchecked-subtract-int cnt (.tailoff this)) 32)
        (let [newTail (new Object/1 (unchecked-add-int (alength tail) 1))]
          (System/arraycopy tail 0 newTail 0 (alength tail))
          (aset newTail (alength tail) val)
          (PersistentVector. (.meta this) (unchecked-add-int cnt 1) shift root newTail))
        (let [^:mutable ^Node newroot nil
              tailnode (Node. (.-edit root) tail)
              ^:mutable newshift shift]
          (if (> (unsigned-bit-shift-right-int cnt 5) (bit-shift-left-int 1 shift))
              (do
                (set! newroot (Node. (.-edit root)))
                (aset (.-array newroot) 0 root)
                (aset (.-array newroot) 1 (PersistentVector/newPath (.-edit root) shift tailnode))
                (set! newshift (unchecked-add-int newshift 5)))
              (set! newroot (.pushTail this shift root tailnode)))
          (PersistentVector. (.meta this)
                             (unchecked-add-int cnt 1)
                             newshift
                             newroot
                             (new Object/1 [val])))))

  (method ^:private pushTail ^Node [this ^int level ^Node parent ^Node tailnode]
    (let [subidx (bit-and-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) level)
                              0x01f)
          ret (Node. (.-edit parent) (.clone (.-array parent)))
          ^:mutable ^Node nodeToInsert nil]
      (if (== level 5)
          (set! nodeToInsert tailnode)
          (let [child (cast Node (aget (.-array parent) subidx))]
            (set! nodeToInsert
                  (if (some? child)
                      (.pushTail this (unchecked-subtract-int level 5) child tailnode)
                      (PersistentVector/newPath (.-edit root)
                                                (unchecked-subtract-int level 5)
                                                tailnode)))))
      (aset (.-array ret) subidx nodeToInsert)
      ret))

  (method ^:private ^:static newPath ^Node [^{:tag (AtomicReference Thread)} edit ^int level
                                            ^Node node]
    (if (== level 0)
        node
        (let [ret (Node. edit)]
          (aset (.-array ret)
                0
                (PersistentVector/newPath edit (unchecked-subtract-int level 5) node))
          ret)))

  (method ^:public chunkedSeq ^IChunkedSeq [this]
    (when-not (== (.count this) 0) (ChunkedSeq. this 0 0)))

  (method ^:public seq ^ISeq [this] (.chunkedSeq this))

  (method rangedIterator ^Iterator [this ^:final ^int start ^:final ^int end]
    (anon Iterator []
      (field ^int i start)

      (field ^int base (unchecked-subtract-int i (unchecked-remainder-int i 32)))

      (field ^Object/1 array
        (when (< start (.count PersistentVector/this)) (.arrayFor PersistentVector/this i)))

      (method ^:public hasNext ^boolean [this] (< i end))

      (method ^:public next [this]
        (if (< i end)
            (do
              (when (== (unchecked-subtract-int i base) 32)
                (set! array (.arrayFor PersistentVector/this i))
                (set! base (unchecked-add-int base 32)))
              (let [i-1 i] (set! i (unchecked-inc-int i)) (aget array (bit-and-int i-1 0x01f))))
            (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (method ^:public iterator ^Iterator [this]
    (.rangedIterator this 0 (.count this)))

  (method rangedSpliterator ^Spliterator [this ^:final ^int start ^:final ^int end]
    (anon Spliterator []
      (field ^int i start)

      (field ^int base (unchecked-subtract-int i (unchecked-remainder-int i 32)))

      (field ^Object/1 array
        (when (< start (.count PersistentVector/this)) (.arrayFor PersistentVector/this i)))

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
              (when (== (unchecked-subtract-int i base) 32)
                (set! array (.arrayFor PersistentVector/this i))
                (set! base (unchecked-add-int base 32)))
              (.accept action
                       (aget array
                             (bit-and-int (let [old-2 i] (set! i (unchecked-inc-int i)) old-2)
                                          0x01f)))
              true)
            false))

      (method ^:public trySplit ^Spliterator [this]
        (let [lo i
              mid (unsigned-bit-shift-right-int (unchecked-add-int lo end) 1)]
          (when-not (>= lo mid) (set! i mid) (.rangedSpliterator PersistentVector/this lo mid))))

      (method ^:public forEachRemaining ^void [this ^Consumer action]
        (let [^:mutable x i]
          (while (< x end)
            (let [array (.arrayFor PersistentVector/this x)
                  remaining (unchecked-subtract-int end x)
                  offset (bit-and-int x 0x01f)
                  limit (Math/min (alength array) (unchecked-add-int offset remaining))]
              (loop [^int j offset]
                (when (< j limit) (.accept action (aget array j)) (recur (unchecked-inc-int j))))
              (set! x (unchecked-add-int x (unchecked-subtract-int limit offset)))))
          (set! i end)
          (set! (.-array this) nil)))))

  (method ^:public spliterator ^Spliterator [this]
    (.rangedSpliterator this 0 (.count this)))

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable ^Object init nil]
      (if (> cnt 0)
          (do
            (set! init (aget (.arrayFor this 0) 0))
            (let [^:mutable ^int step 0]
              (loop [^int i 0]
                (when (< i cnt)
                  (let [array (.arrayFor this i)]
                    (loop [^int j (if (== i 0) 1 0)]
                      (when (< j (alength array))
                        (set! init (.invoke f init (aget array j)))
                        (if (RT/isReduced init)
                            (return (.deref (cast IDeref init)))
                            (recur (unchecked-inc-int j)))))
                    (set! step (alength array))
                    (recur (unchecked-add-int i step)))))
              init))
          (.invoke f))))

  (method ^:public reduce [this ^IFn f ^:mutable init]
    (let [^:mutable ^int step 0]
      (loop [^int i 0]
        (when (< i cnt)
          (let [array (.arrayFor this i)]
            (loop [^int j 0]
              (when (< j (alength array))
                (set! init (.invoke f init (aget array j)))
                (if (RT/isReduced init)
                    (return (.deref (cast IDeref init)))
                    (recur (unchecked-inc-int j)))))
            (set! step (alength array))
            (recur (unchecked-add-int i step)))))
      init))

  (method ^:public kvreduce [this ^IFn f ^:mutable init]
    (let [^:mutable ^int step 0]
      (loop [^int i 0]
        (when (< i cnt)
          (let [array (.arrayFor this i)]
            (loop [^int j 0]
              (when (< j (alength array))
                (set! init (.invoke f init (unchecked-add-int j i) (aget array j)))
                (if (RT/isReduced init)
                    (return (.deref (cast IDeref init)))
                    (recur (unchecked-inc-int j)))))
            (set! step (alength array))
            (recur (unchecked-add-int i step)))))
      init))

  (method ^:public drop ^Sequential [this ^int n]
    (when (< n cnt)
      (let [offset (unchecked-remainder-int n 32)]
        (ChunkedSeq. this (.arrayFor this n) (unchecked-subtract-int n offset) offset))))

  (defclass ^:public ^:static ^:final ChunkedSeq
    :extends ASeq
    :implements [IChunkedSeq Counted IReduce IDrop]

    (field ^:public ^:final ^PersistentVector vec)

    (field ^:final ^Object/1 node)

    (field ^:final ^int i)

    (field ^:public ^:final ^int offset)

    (constructor ^:public [this ^PersistentVector vec ^int i ^int offset]
      (set! (.-vec this) vec)
      (set! (.-i this) i)
      (set! (.-offset this) offset)
      (set! (.-node this) (.arrayFor vec i)))

    (constructor [this ^IPersistentMap meta ^PersistentVector vec ^Object/1 node ^int i ^int offset]
      (super. meta)
      (set! (.-vec this) vec)
      (set! (.-node this) node)
      (set! (.-i this) i)
      (set! (.-offset this) offset))

    (constructor [this ^PersistentVector vec ^Object/1 node ^int i ^int offset]
      (set! (.-vec this) vec)
      (set! (.-node this) node)
      (set! (.-i this) i)
      (set! (.-offset this) offset))

    (method ^:public chunkedFirst ^IChunk [this] (ArrayChunk. node offset))

    (method ^:public chunkedNext ^ISeq [this]
      (when (< (unchecked-add-int i (alength node)) (.-cnt vec))
        (ChunkedSeq. vec (unchecked-add-int i (alength node)) 0)))

    (method ^:public chunkedMore ^ISeq [this]
      (let [s (.chunkedNext this)] (if (nil? s) PersistentList/EMPTY s)))

    (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
      (if (identical? meta (.-_meta this)) this (ChunkedSeq. meta vec node i offset)))

    (method ^:public first [this] (aget node offset))

    (method ^:public next ^ISeq [this]
      (if (< (unchecked-add-int offset 1) (alength node))
          (ChunkedSeq. vec node i (unchecked-add-int offset 1))
          (.chunkedNext this)))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (.-cnt vec) (unchecked-add-int i offset)))

    (method ^:public iterator ^Iterator [this]
      (.rangedIterator vec (unchecked-add-int i offset) (.-cnt vec)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object acc nil]
        (if (< (unchecked-add-int i offset) (.-cnt vec))
            (do
              (set! acc (aget node offset))
              (loop [^int j (unchecked-add-int offset 1)]
                (when (< j (alength node))
                  (set! acc (.invoke f acc (aget node j)))
                  (if (RT/isReduced acc)
                      (return (.deref (cast IDeref acc)))
                      (recur (unchecked-inc-int j)))))
              (let [^:mutable ^int step 0]
                (loop [^int ii (unchecked-add-int i (alength node))]
                  (when (< ii (.-cnt vec))
                    (let [array (.arrayFor vec ii)]
                      (loop [^int j 0]
                        (when (< j (alength array))
                          (set! acc (.invoke f acc (aget array j)))
                          (if (RT/isReduced acc)
                              (return (.deref (cast IDeref acc)))
                              (recur (unchecked-inc-int j)))))
                      (set! step (alength array))
                      (recur (unchecked-add-int ii step)))))
                acc))
            (.invoke f))))

    (method ^:public reduce [this ^IFn f init]
      (let [^:mutable acc init]
        (loop [^int j offset]
          (when (< j (alength node))
            (set! acc (.invoke f acc (aget node j)))
            (if (RT/isReduced acc)
                (return (.deref (cast IDeref acc)))
                (recur (unchecked-inc-int j)))))
        (let [^:mutable ^int step 0]
          (loop [^int ii (unchecked-add-int i (alength node))]
            (when (< ii (.-cnt vec))
              (let [array (.arrayFor vec ii)]
                (loop [^int j 0]
                  (when (< j (alength array))
                    (set! acc (.invoke f acc (aget array j)))
                    (if (RT/isReduced acc)
                        (return (.deref (cast IDeref acc)))
                        (recur (unchecked-inc-int j)))))
                (set! step (alength array))
                (recur (unchecked-add-int ii step)))))
          acc)))

    (method ^:public drop ^Sequential [this ^int n]
      (let [o (unchecked-add-int offset n)]
        (if (< o (alength node))
            (ChunkedSeq. vec node i o)
            (let [i (unchecked-add-int (.-i this) o)]
              (when (< i (.-cnt vec))
                (let [newOffset (unchecked-remainder-int i 32)]
                  (ChunkedSeq. vec (.arrayFor vec i) (unchecked-subtract-int i newOffset) newOffset))))))))

  (method ^:public empty ^IPersistentCollection [this]
    (.withMeta EMPTY (.meta this)))

  (method ^:public pop ^PersistentVector [this]
    (when (== cnt 0) (throw (IllegalStateException. "Can't pop empty vector")))
    (cond
      (== cnt 1) (.withMeta EMPTY (.meta this))
      (> (unchecked-subtract-int cnt (.tailoff this)) 1)
        (let [newTail (new Object/1 (unchecked-subtract-int (alength tail) 1))]
          (System/arraycopy tail 0 newTail 0 (alength newTail))
          (PersistentVector. (.meta this) (unchecked-subtract-int cnt 1) shift root newTail))
      :else
        (let [newtail (.arrayFor this (unchecked-subtract-int cnt 2))
              ^:mutable newroot (.popTail this shift root)
              ^:mutable newshift shift]
          (when (nil? newroot) (set! newroot EMPTY_NODE))
          (when (and (> shift 5) (nil? (aget (.-array newroot) 1)))
            (set! newroot (cast Node (aget (.-array newroot) 0)))
            (set! newshift (unchecked-subtract-int newshift 5)))
          (PersistentVector. (.meta this) (unchecked-subtract-int cnt 1) newshift newroot newtail))))

  (method ^:private popTail ^Node [this ^int level ^Node node]
    (let [subidx (bit-and-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 2) level)
                              0x01f)]
      (if (> level 5)
          (let [newchild (.popTail this
                                   (unchecked-subtract-int level 5)
                                   (cast Node (aget (.-array node) subidx)))]
            (when-not (and (nil? newchild) (== subidx 0))
              (let [ret (Node. (.-edit root) (.clone (.-array node)))]
                (aset (.-array ret) subidx newchild)
                ret)))
          (when-not (== subidx 0)
            (let [ret (Node. (.-edit root) (.clone (.-array node)))]
              (aset (.-array ret) subidx nil)
              ret)))))

  (defclass ^:static ^:final TransientVector
    :extends AFn
    :implements [ITransientVector ITransientAssociative2 Counted]

    (field ^:volatile ^int cnt)

    (field ^:volatile ^int shift)

    (field ^:volatile ^Node root)

    (field ^:volatile ^Object/1 tail)

    (field ^:private ^:final ^IPersistentMap _meta)

    (constructor [this ^int cnt ^int shift ^Node root ^Object/1 tail ^IPersistentMap _meta]
      (set! (.-cnt this) cnt)
      (set! (.-shift this) shift)
      (set! (.-root this) root)
      (set! (.-tail this) tail)
      (set! (.-_meta this) _meta))

    (constructor [this ^PersistentVector v]
      (this. (.-cnt v)
             (.-shift v)
             (TransientVector/editableRoot (.-root v))
             (TransientVector/editableTail (.-tail v))
             (.-_meta v)))

    (method ^:public count ^int [this] (.ensureEditable this) cnt)

    (method ensureEditable ^Node [this ^Node node]
      (if (identical? (.-edit node) (.-edit root))
          node
          (Node. (.-edit root) (.clone (.-array node)))))

    (method ensureEditable ^void [this]
      (when (nil? (.get (.-edit root)))
        (throw (IllegalAccessError. "Transient used after persistent! call"))))

    (method ^:static editableRoot ^Node [^Node node]
      (Node. (AtomicReference. (Thread/currentThread)) (.clone (.-array node))))

    (method ^:public persistent ^PersistentVector [this]
      (.ensureEditable this)
      (.set (.-edit root) nil)
      (let [trimmedTail (new Object/1 (unchecked-subtract-int cnt (.tailoff this)))]
        (System/arraycopy tail 0 trimmedTail 0 (alength trimmedTail))
        (PersistentVector. _meta cnt shift root trimmedTail)))

    (method ^:static editableTail ^Object/1 [^Object/1 tl]
      (let [ret (new Object/1 32)] (System/arraycopy tl 0 ret 0 (alength tl)) ret))

    (method ^:public conj ^TransientVector [this val]
      (.ensureEditable this)
      (let [i cnt]
        (if (< (unchecked-subtract-int i (.tailoff this)) 32)
            (do (aset tail (bit-and-int i 0x01f) val) (set! cnt (unchecked-inc-int cnt)) this)
            (let [^:mutable ^Node newroot nil
                  tailnode (Node. (.-edit root) tail)]
              (set! tail (new Object/1 32))
              (aset tail 0 val)
              (let [^:mutable newshift shift]
                (if (> (unsigned-bit-shift-right-int cnt 5) (bit-shift-left-int 1 shift))
                    (do
                      (set! newroot (Node. (.-edit root)))
                      (aset (.-array newroot) 0 root)
                      (aset (.-array newroot)
                            1
                            (PersistentVector/newPath (.-edit root) shift tailnode))
                      (set! newshift (unchecked-add-int newshift 5)))
                    (set! newroot (.pushTail this shift root tailnode)))
                (set! root newroot)
                (set! shift newshift)
                (set! cnt (unchecked-inc-int cnt))
                this)))))

    (method ^:private pushTail ^Node [this ^int level ^:mutable ^Node parent ^Node tailnode]
      (set! parent (.ensureEditable this parent))
      (let [subidx (bit-and-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) level)
                                0x01f)
            ret parent
            ^:mutable ^Node nodeToInsert nil]
        (if (== level 5)
            (set! nodeToInsert tailnode)
            (let [child (cast Node (aget (.-array parent) subidx))]
              (set! nodeToInsert
                    (if (some? child)
                        (.pushTail this (unchecked-subtract-int level 5) child tailnode)
                        (PersistentVector/newPath
                          (.-edit root)
                          (unchecked-subtract-int level 5)
                          tailnode)))))
        (aset (.-array ret) subidx nodeToInsert)
        ret))

    (method ^:private ^:final tailoff ^int [this]
      (if (< cnt 32)
          0
          (bit-shift-left-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) 5) 5)))

    (method ^:private arrayFor ^Object/1 [this ^int i]
      (if (and (>= i 0) (< i cnt))
          (if (>= i (.tailoff this))
              tail
              (let [^:mutable node root]
                (loop [^int level shift]
                  (when (> level 0)
                    (set! node
                          (cast Node
                                (aget (.-array node)
                                      (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f))))
                    (recur (unchecked-subtract-int level 5))))
                (.-array node)))
          (throw (IndexOutOfBoundsException.))))

    (method ^:private editableArrayFor ^Object/1 [this ^int i]
      (if (and (>= i 0) (< i cnt))
          (if (>= i (.tailoff this))
              tail
              (let [^:mutable node root]
                (loop [^int level shift]
                  (when (> level 0)
                    (set! node
                          (.ensureEditable this
                                           (cast
                                             Node
                                             (aget
                                               (.-array node)
                                               (bit-and-int
                                                 (unsigned-bit-shift-right-int i level)
                                                 0x01f)))))
                    (recur (unchecked-subtract-int level 5))))
                (.-array node)))
          (throw (IndexOutOfBoundsException.))))

    (method ^:public valAt [this key] (.valAt this key nil))

    (method ^:public valAt [this key notFound]
      (.ensureEditable this)
      (when (Util/isInteger key)
        (let [i (.intValue (cast Number key))]
          (when (and (>= i 0) (< i cnt)) (return (.nth this i)))))
      notFound)

    (field ^:private ^:static ^:final NOT_FOUND (Object.))

    (method ^:public ^:final containsKey ^boolean [this key]
      (not (identical? (.valAt this key NOT_FOUND) NOT_FOUND)))

    (method ^:public ^:final entryAt ^IMapEntry [this key]
      (let [v (.valAt this key NOT_FOUND)]
        (when-not (identical? v NOT_FOUND) (MapEntry/create key v))))

    (method ^:public invoke [this arg1]
      (if (Util/isInteger arg1)
          (.nth this (.intValue (cast Number arg1)))
          (throw (IllegalArgumentException. "Key must be integer"))))

    (method ^:public nth [this ^int i]
      (.ensureEditable this)
      (let [node (.arrayFor this i)] (aget node (bit-and-int i 0x01f))))

    (method ^:public nth [this ^int i notFound]
      (if (and (>= i 0) (< i (.count this))) (.nth this i) notFound))

    (method ^:public assocN ^TransientVector [this ^int i val]
      (.ensureEditable this)
      (cond
        (and (>= i 0) (< i cnt))
          (if (>= i (.tailoff this))
              (do (aset tail (bit-and-int i 0x01f) val) this)
              (do (set! root (.doAssoc this shift root i val)) this))
        (== i cnt) (.conj this val)
        :else (throw (IndexOutOfBoundsException.))))

    (method ^:public assoc ^TransientVector [this key val]
      (if (Util/isInteger key)
          (let [i (.intValue (cast Number key))] (.assocN this i val))
          (throw (IllegalArgumentException. "Key must be integer"))))

    (method ^:private doAssoc ^Node [this ^int level ^:mutable ^Node node ^int i val]
      (set! node (.ensureEditable this node))
      (let [ret node]
        (if (== level 0)
            (aset (.-array ret) (bit-and-int i 0x01f) val)
            (let [subidx (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f)]
              (aset (.-array ret)
                    subidx
                    (.doAssoc this
                              (unchecked-subtract-int level 5)
                              (cast Node (aget (.-array node) subidx))
                              i
                              val))))
        ret))

    (method ^:public pop ^TransientVector [this]
      (.ensureEditable this)
      (when (== cnt 0) (throw (IllegalStateException. "Can't pop empty vector")))
      (if (== cnt 1)
          (do (set! cnt 0) this)
          (let [i (unchecked-subtract-int cnt 1)]
            (if (> (bit-and-int i 0x01f) 0)
                (do (set! cnt (unchecked-dec-int cnt)) this)
                (let [newtail (.editableArrayFor this (unchecked-subtract-int cnt 2))
                      ^:mutable newroot (.popTail this shift root)
                      ^:mutable newshift shift]
                  (when (nil? newroot) (set! newroot (Node. (.-edit root))))
                  (when (and (> shift 5) (nil? (aget (.-array newroot) 1)))
                    (set! newroot (.ensureEditable this (cast Node (aget (.-array newroot) 0))))
                    (set! newshift (unchecked-subtract-int newshift 5)))
                  (set! root newroot)
                  (set! shift newshift)
                  (set! cnt (unchecked-dec-int cnt))
                  (set! tail newtail)
                  this)))))

    (method ^:private popTail ^Node [this ^int level ^:mutable ^Node node]
      (set! node (.ensureEditable this node))
      (let [subidx (bit-and-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 2) level)
                                0x01f)]
        (if (> level 5)
            (let [newchild (.popTail this
                                     (unchecked-subtract-int level 5)
                                     (cast Node (aget (.-array node) subidx)))]
              (when-not (and (nil? newchild) (== subidx 0))
                (let [ret node] (aset (.-array ret) subidx newchild) ret)))
            (when-not (== subidx 0) (let [ret node] (aset (.-array ret) subidx nil) ret)))))))
