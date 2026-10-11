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
;; Converted from clojure/lang/PersistentHashMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util ArrayList Iterator List Map Map$Entry NoSuchElementException)
        '(java.util.concurrent.atomic AtomicReference))

(defclass ^:public PersistentHashMap
  :extends APersistentMap
  :implements [IEditableCollection IObj IMapIterable IKVReduce]

  (field ^:private ^:static ^:final ^long serialVersionUID -8682496769319143320)

  (field ^:final ^int count)

  (field ^:final ^INode root)

  (field ^:final ^boolean hasNull)

  (field ^:final nullValue)

  (field ^:final ^IPersistentMap _meta)

  (field ^:public ^:static ^:final ^PersistentHashMap EMPTY (PersistentHashMap. 0 nil false nil))

  (field ^:private ^:static ^:final NOT_FOUND (Object.))

  (method ^:public ^:static create ^IPersistentMap [^Map other]
    (let [^:mutable ^ITransientMap ret (.asTransient EMPTY)]
      (for-each [o (.entrySet other)]
        (let [e (cast Map$Entry o)] (set! ret (.assoc ret (.getKey e) (.getValue e)))))
      (.persistent ret)))

  (method ^:public ^:static create ^PersistentHashMap [& ^Object/1 init]
    (let [^:mutable ^ITransientMap ret (.asTransient EMPTY)]
      (loop [^int i 0]
        (when (< i (alength init))
          (set! ret (.assoc ret (aget init i) (aget init (unchecked-add-int i 1))))
          (recur (unchecked-add-int i 2))))
      (cast PersistentHashMap (.persistent ret))))

  (method ^:public ^:static createWithCheck ^PersistentHashMap [& ^Object/1 init]
    (let [^:mutable ^ITransientMap ret (.asTransient EMPTY)]
      (loop [^int i 0]
        (when (< i (alength init))
          (set! ret (.assoc ret (aget init i) (aget init (unchecked-add-int i 1))))
          (if (not (== (.count ret) (unchecked-add-int (unchecked-divide-int i 2) 1)))
              (throw (IllegalArgumentException. (java-str "Duplicate key: " (aget init i))))
              (recur (unchecked-add-int i 2)))))
      (cast PersistentHashMap (.persistent ret))))

  (method ^:public ^:static create ^PersistentHashMap [^:mutable ^ISeq items]
    (let [^:mutable ^ITransientMap ret (.asTransient EMPTY)]
      (while (some? items)
        (when (nil? (.next items))
          (throw (IllegalArgumentException.
                   (String/format "No value supplied for key: %s" (new Object/1 [(.first items)])))))
        (set! ret (.assoc ret (.first items) (RT/second items)))
        (set! items (.next (.next items))))
      (cast PersistentHashMap (.persistent ret))))

  (method ^:public ^:static createWithCheck ^PersistentHashMap [^:mutable ^ISeq items]
    (let [^:mutable ^ITransientMap ret (.asTransient EMPTY)]
      (let [^:mutable ^int i 0]
        (while (some? items)
          (when (nil? (.next items))
            (throw
              (IllegalArgumentException.
                (String/format "No value supplied for key: %s" (new Object/1 [(.first items)])))))
          (set! ret (.assoc ret (.first items) (RT/second items)))
          (when-not (== (.count ret) (unchecked-add-int i 1))
            (throw (IllegalArgumentException. (java-str "Duplicate key: " (.first items)))))
          (set! items (.next (.next items)))
          (set! i (unchecked-inc-int i))))
      (cast PersistentHashMap (.persistent ret))))

  (method ^:public ^:static create ^PersistentHashMap [^IPersistentMap meta & ^Object/1 init]
    (.withMeta (PersistentHashMap/create init) meta))

  (constructor [this ^int count ^INode root ^boolean hasNull nullValue]
    (set! (.-count this) count)
    (set! (.-root this) root)
    (set! (.-hasNull this) hasNull)
    (set! (.-nullValue this) nullValue)
    (set! (.-_meta this) nil))

  (constructor ^:public [this ^IPersistentMap meta ^int count ^INode root ^boolean hasNull nullValue]
    (set! (.-_meta this) meta)
    (set! (.-count this) count)
    (set! (.-root this) root)
    (set! (.-hasNull this) hasNull)
    (set! (.-nullValue this) nullValue))

  (method ^:static hash ^int [k] (Util/hasheq k))

  (method ^:public containsKey ^boolean [this key]
    (cond
      (nil? key) hasNull
      (some? root)
        (not (identical? (.find root 0 (PersistentHashMap/hash key) key NOT_FOUND) NOT_FOUND))
      :else false))

  (method ^:public entryAt ^IMapEntry [this key]
    (cond
      (nil? key) (when hasNull ^IMapEntry (MapEntry/create nil nullValue))
      (some? root) (.find root 0 (PersistentHashMap/hash key) key)))

  (method ^:public assoc ^IPersistentMap [this key val]
    (if (nil? key)
        (if (and hasNull (identical? val nullValue))
            this
            (PersistentHashMap. (.meta this)
                                (if hasNull count (unchecked-add-int count 1))
                                root
                                true
                                val))
        (let [addedLeaf (Box. nil)
              newroot (.assoc (if (nil? root) BitmapIndexedNode/EMPTY root)
                              0
                              (PersistentHashMap/hash key)
                              key
                              val
                              addedLeaf)]
          (if (identical? newroot root)
              this
              (PersistentHashMap. (.meta this)
                                  (if (nil? (.-val addedLeaf)) count (unchecked-add-int count 1))
                                  newroot
                                  hasNull
                                  nullValue)))))

  (method ^:public valAt [this key notFound]
    (cond
      (nil? key) (if hasNull nullValue notFound)
      (some? root) (.find root 0 (PersistentHashMap/hash key) key notFound)
      :else notFound))

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public assocEx ^IPersistentMap [this key val]
    (when (.containsKey this key) (throw (Util/runtimeException "Key already present")))
    (.assoc this key val))

  (method ^:public without ^IPersistentMap [this key]
    (cond
      (nil? key)
        (if hasNull
            ^IPersistentMap (PersistentHashMap. (.meta this)
                                                (unchecked-subtract-int count 1)
                                                root
                                                false
                                                nil)
            ^IPersistentMap this)
      (nil? root) this
      :else
        (let [newroot (.without root 0 (PersistentHashMap/hash key) key)]
          (if (identical? newroot root)
              this
              (PersistentHashMap. (.meta this)
                                  (unchecked-subtract-int count 1)
                                  newroot
                                  hasNull
                                  nullValue)))))

  (field ^:static ^:final ^Iterator EMPTY_ITER
    (anon Iterator []
      (method ^:public hasNext ^boolean [this] false)

      (method ^:public next [this] (throw (NoSuchElementException.)))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (method ^:private iterator ^Iterator [this ^:final ^IFn f]
    (let [rootIter (if (nil? root) EMPTY_ITER (.iterator root f))]
      (if hasNull
          (anon Iterator []
            (field ^:private ^boolean seen false)

            (method ^:public hasNext ^boolean [this]
              (if (not seen) true (.hasNext rootIter)))

            (method ^:public next [this]
              (if (not seen) (do (set! seen true) (.invoke f nil nullValue)) (.next rootIter)))

            (method ^:public remove ^void [this]
              (throw (UnsupportedOperationException.))))
          rootIter)))

  (method ^:public iterator ^Iterator [this]
    (.iterator this APersistentMap/MAKE_ENTRY))

  (method ^:public keyIterator ^Iterator [this]
    (.iterator this APersistentMap/MAKE_KEY))

  (method ^:public valIterator ^Iterator [this]
    (.iterator this APersistentMap/MAKE_VAL))

  (method ^:public kvreduce [this ^IFn f ^:mutable init]
    (set! init (if hasNull (.invoke f init nil nullValue) init))
    (cond
      (RT/isReduced init) (.deref (cast IDeref init))
      (some? root)
        (do
          (set! init (.kvreduce root f init))
          (if (RT/isReduced init) (.deref (cast IDeref init)) init))
      :else init))

  (method ^:public fold [this ^long n ^:final ^IFn combinef ^:final ^IFn reducef ^IFn fjinvoke
                         ^:final ^IFn fjtask ^:final ^IFn fjfork ^:final ^IFn fjjoin]
    (let [^Callable top (anon Callable []
                          (method ^:public call :throws [Exception] [this]
                            (let [^:mutable ret (.invoke combinef)]
                              (when (some? root)
                                (set! ret
                                      (.invoke combinef
                                               ret
                                               (.fold root combinef reducef fjtask fjfork fjjoin))))
                              (if hasNull
                                  (.invoke combinef
                                           ret
                                           (.invoke reducef (.invoke combinef) nil nullValue))
                                  ret))))]
      (.invoke fjinvoke top)))

  (method ^:public count ^int [this] count)

  (method ^:public seq ^ISeq [this]
    (let [s (when (some? root) (.nodeSeq root))]
      (if hasNull ^ISeq (Cons. (MapEntry/create nil nullValue) s) s)))

  (method ^:public empty ^IPersistentCollection [this]
    (.withMeta EMPTY (.meta this)))

  (method ^:static mask ^int [^int hash ^int shift]
    (bit-and-int (unsigned-bit-shift-right-int hash shift) 0x01f))

  (method ^:public withMeta ^PersistentHashMap [this ^IPersistentMap meta]
    (if (identical? _meta meta) this (PersistentHashMap. meta count root hasNull nullValue)))

  (method ^:public asTransient ^TransientHashMap [this]
    (TransientHashMap. this))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (defclass ^:static ^:final TransientHashMap
    :extends ATransientMap

    (field ^:final ^{:tag (AtomicReference Thread)} edit)

    (field ^:volatile ^INode root)

    (field ^:volatile ^int count)

    (field ^:volatile ^boolean hasNull)

    (field ^:volatile nullValue)

    (field ^:final ^Box leafFlag (Box. nil))

    (field ^:private ^:final ^IPersistentMap _meta)

    (constructor [this ^PersistentHashMap m]
      (this. (AtomicReference. (Thread/currentThread))
             (.-root m)
             (.-count m)
             (.-hasNull m)
             (.-nullValue m)
             (.-_meta m)))

    (constructor [this ^{:tag (AtomicReference Thread)} edit ^INode root ^int count ^boolean hasNull
                  nullValue ^IPersistentMap _meta]
      (set! (.-edit this) edit)
      (set! (.-root this) root)
      (set! (.-count this) count)
      (set! (.-hasNull this) hasNull)
      (set! (.-nullValue this) nullValue)
      (set! (.-_meta this) _meta))

    (method ^:public assoc ^ITransientMap [this key val]
      (.ensureEditable this)
      (if (nil? key)
          (do
            (when-not (identical? (.-nullValue this) val) (set! (.-nullValue this) val))
            (when-not hasNull
              (set! (.-count this) (unchecked-inc-int (.-count this)))
              (set! (.-hasNull this) true))
            this)
          (do
            (set! (.-val leafFlag) nil)
            (let [n (.assoc (if (nil? root) BitmapIndexedNode/EMPTY root)
                            edit
                            0
                            (PersistentHashMap/hash key)
                            key
                            val
                            leafFlag)]
              (when-not (identical? n (.-root this)) (set! (.-root this) n))
              (when (some? (.-val leafFlag))
                (set! (.-count this) (unchecked-inc-int (.-count this))))
              this))))

    (method doAssoc ^ITransientMap [this key val] (.assoc this key val))

    (method ^:public without ^ITransientMap [this key]
      (.ensureEditable this)
      (cond
        (nil? key)
          (if (not hasNull)
              this
              (do
                (set! hasNull false)
                (set! nullValue nil)
                (set! (.-count this) (unchecked-dec-int (.-count this)))
                this))
        (nil? root) this
        :else
          (do
            (set! (.-val leafFlag) nil)
            (let [n (.without root edit 0 (PersistentHashMap/hash key) key leafFlag)]
              (when-not (identical? n root) (set! (.-root this) n))
              (when (some? (.-val leafFlag))
                (set! (.-count this) (unchecked-dec-int (.-count this))))
              this))))

    (method doWithout ^ITransientMap [this key] (.without this key))

    (method ^:public persistent ^IPersistentMap [this]
      (.ensureEditable this)
      (.set edit nil)
      (PersistentHashMap. _meta count root hasNull nullValue))

    (method doPersistent ^IPersistentMap [this] (.persistent this))

    (method ^:public valAt [this key notFound]
      (.ensureEditable this)
      (cond
        (nil? key) (if hasNull nullValue notFound)
        (nil? root) notFound
        :else (.find root 0 (PersistentHashMap/hash key) key notFound)))

    (method doValAt [this key notFound] (.valAt this key notFound))

    (field ^:private ^:static ^:final NOT_FOUND (Object.))

    (method ^:public containsKey ^boolean [this key]
      (.ensureEditable this)
      (not (identical? (.valAt this key NOT_FOUND) NOT_FOUND)))

    (method ^:public entryAt ^IMapEntry [this key]
      (.ensureEditable this)
      (let [v (.valAt this key NOT_FOUND)]
        (when-not (identical? v NOT_FOUND) (MapEntry/create key v))))

    (method ^:public count ^int [this] (.ensureEditable this) count)

    (method doCount ^int [this] (.count this))

    (method ensureEditable ^void [this]
      (when (nil? (.get edit))
        (throw (IllegalAccessError. "Transient used after persistent! call")))))

  (defclass ^:static ^:interface INode
    :extends [Serializable]

    (method assoc ^INode [this ^int shift ^int hash key val ^Box addedLeaf])

    (method without ^INode [this ^int shift ^int hash key])

    (method find ^IMapEntry [this ^int shift ^int hash key])

    (method find [this ^int shift ^int hash key notFound])

    (method nodeSeq ^ISeq [this])

    (method assoc ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash key val
                          ^Box addedLeaf])

    (method without ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash key
                            ^Box removedLeaf])

    (method ^:public kvreduce [this ^IFn f init])

    (method fold [this ^IFn combinef ^IFn reducef ^IFn fjtask ^IFn fjfork ^IFn fjjoin])

    (method iterator ^Iterator [this ^IFn f]))

  (defclass ^:static ^:final ArrayNode
    :implements [INode]

    (field ^int count)

    (field ^:final ^INode/1 array)

    (field ^:final ^{:tag (AtomicReference Thread)} edit)

    (constructor [this ^{:tag (AtomicReference Thread)} edit ^int count ^INode/1 array]
      (set! (.-array this) array)
      (set! (.-edit this) edit)
      (set! (.-count this) count))

    (method ^:public assoc ^INode [this ^int shift ^int hash key val ^Box addedLeaf]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (if (nil? node)
            (ArrayNode. nil
                        (unchecked-add-int count 1)
                        (PersistentHashMap/cloneAndSet
                          array
                          idx
                          (.assoc BitmapIndexedNode/EMPTY
                                  (unchecked-add-int shift 5)
                                  hash
                                  key
                                  val
                                  addedLeaf)))
            (let [n (.assoc node (unchecked-add-int shift 5) hash key val addedLeaf)]
              (if (identical? n node)
                  this
                  (ArrayNode. nil count (PersistentHashMap/cloneAndSet array idx n)))))))

    (method ^:public without ^INode [this ^int shift ^int hash key]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (if (nil? node)
            this
            (let [n (.without node (unchecked-add-int shift 5) hash key)]
              (cond
                (identical? n node) this
                (nil? n)
                  (if (<= count 8)
                      (.pack this nil idx)
                      (ArrayNode. nil
                                  (unchecked-subtract-int count 1)
                                  (PersistentHashMap/cloneAndSet array idx n)))
                :else (ArrayNode. nil count (PersistentHashMap/cloneAndSet array idx n)))))))

    (method ^:public find ^IMapEntry [this ^int shift ^int hash key]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (when (some? node) (.find node (unchecked-add-int shift 5) hash key))))

    (method ^:public find [this ^int shift ^int hash key notFound]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (if (nil? node) notFound (.find node (unchecked-add-int shift 5) hash key notFound))))

    (method ^:public nodeSeq ^ISeq [this] (Seq/create array))

    (method ^:public iterator ^Iterator [this ^IFn f] (Iter. array f))

    (method ^:public kvreduce [this ^IFn f ^:mutable init]
      (for-each [^INode node array]
        (when (some? node)
          (set! init (.kvreduce node f init))
          (when (RT/isReduced init) (return init))))
      init)

    (method ^:public fold [this ^:final ^IFn combinef ^:final ^IFn reducef ^:final ^IFn fjtask
                           ^:final ^IFn fjfork ^:final ^IFn fjjoin]
      (let [^{:tag (List Callable)} tasks (ArrayList.)]
        (for-each [^INode node array]
          (when (some? node)
            (.add tasks
                  (anon Callable []
                    (method ^:public call :throws [Exception] [this]
                      (.fold node combinef reducef fjtask fjfork fjjoin))))))
        (ArrayNode/foldTasks tasks combinef fjtask fjfork fjjoin)))

    (method ^:public ^:static foldTasks [^{:tag (List Callable)} tasks ^:final ^IFn combinef
                                         ^:final ^IFn fjtask ^:final ^IFn fjfork ^:final ^IFn fjjoin]
      (if (.isEmpty tasks)
          (.invoke combinef)
          (do
            (when (== (.size tasks) 1)
              (let [^Object ret nil]
                (try
                  (return (.call (cast Callable (.get tasks 0))))
                  (catch Exception e (throw (Util/sneakyThrow e))))))
            (let [^{:tag (List Callable)} t1 (.subList
                                               tasks
                                               0
                                               (unchecked-divide-int (.size tasks) 2))
                  ^{:tag (List Callable)} t2 (.subList
                                               tasks
                                               (unchecked-divide-int (.size tasks) 2)
                                               (.size tasks))
                  forked (.invoke fjfork
                                  (.invoke fjtask
                                           (anon Callable []
                                             (method ^:public call :throws [Exception] [this]
                                               (ArrayNode/foldTasks
                                                 t2
                                                 combinef
                                                 fjtask
                                                 fjfork
                                                 fjjoin)))))]
              (.invoke combinef
                       (ArrayNode/foldTasks t1 combinef fjtask fjfork fjjoin)
                       (.invoke fjjoin forked))))))

    (method ^:private ensureEditable ^ArrayNode [this ^{:tag (AtomicReference Thread)} edit]
      (if (identical? (.-edit this) edit) this (ArrayNode. edit count (.clone (.-array this)))))

    (method ^:private editAndSet ^ArrayNode [this ^{:tag (AtomicReference Thread)} edit ^int i
                                             ^INode n]
      (let [editable (.ensureEditable this edit)] (aset (.-array editable) i n) editable))

    (method ^:private pack ^INode [this ^{:tag (AtomicReference Thread)} edit ^int idx]
      (let [newArray (new Object/1 (unchecked-multiply-int 2 (unchecked-subtract-int count 1)))
            ^:mutable ^int j 1
            ^:mutable ^int bitmap 0]
        (loop [^int i 0]
          (if (< i idx)
              (if (some? (aget array i))
                  (do
                    (aset newArray j (aget array i))
                    (set! bitmap (bit-or-int bitmap (bit-shift-left-int 1 i)))
                    (set! j (unchecked-add-int j 2))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil))
        (loop [^int i (unchecked-add-int idx 1)]
          (if (< i (alength array))
              (if (some? (aget array i))
                  (do
                    (aset newArray j (aget array i))
                    (set! bitmap (bit-or-int bitmap (bit-shift-left-int 1 i)))
                    (set! j (unchecked-add-int j 2))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil))
        (BitmapIndexedNode. edit bitmap newArray)))

    (method ^:public assoc ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                   key val ^Box addedLeaf]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (if (nil? node)
            (let [editable (.editAndSet this
                                        edit
                                        idx
                                        (.assoc BitmapIndexedNode/EMPTY
                                                edit
                                                (unchecked-add-int shift 5)
                                                hash
                                                key
                                                val
                                                addedLeaf))]
              (set! (.-count editable) (unchecked-inc-int (.-count editable)))
              editable)
            (let [n (.assoc node edit (unchecked-add-int shift 5) hash key val addedLeaf)]
              (if (identical? n node) this (.editAndSet this edit idx n))))))

    (method ^:public without ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                     key ^Box removedLeaf]
      (let [idx (PersistentHashMap/mask hash shift)
            node (aget array idx)]
        (if (nil? node)
            this
            (let [n (.without node edit (unchecked-add-int shift 5) hash key removedLeaf)]
              (cond
                (identical? n node) this
                (nil? n)
                  (if (<= count 8)
                      (.pack this edit idx)
                      (let [editable (.editAndSet this edit idx n)]
                        (set! (.-count editable) (unchecked-dec-int (.-count editable)))
                        editable))
                :else (.editAndSet this edit idx n))))))

    (defclass ^:static Seq
      :extends ASeq

      (field ^:final ^INode/1 nodes)

      (field ^:final ^int i)

      (field ^:final ^ISeq s)

      (method ^:static create ^ISeq [^INode/1 nodes]
        (Seq/create nil nodes 0 nil))

      (method ^:private ^:static create ^ISeq [^IPersistentMap meta ^INode/1 nodes ^int i ^ISeq s]
        (if (some? s)
            (Seq. meta nodes i s)
            (do
              (loop [^int j i]
                (if (< j (alength nodes))
                    (if (some? (aget nodes j))
                        (let [ns (.nodeSeq (aget nodes j))]
                          (if (some? ns)
                              (return (Seq. meta nodes (unchecked-add-int j 1) ns))
                              (recur (unchecked-inc-int j))))
                        (recur (unchecked-inc-int j)))
                    nil))
              nil)))

      (constructor ^:private [this ^IPersistentMap meta ^INode/1 nodes ^int i ^ISeq s]
        (super. meta)
        (set! (.-nodes this) nodes)
        (set! (.-i this) i)
        (set! (.-s this) s))

      (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
        (if (identical? (.meta this) meta) this (Seq. meta nodes i s)))

      (method ^:public first [this] (.first s))

      (method ^:public next ^ISeq [this] (Seq/create nil nodes i (.next s))))

    (defclass ^:static Iter
      :implements [Iterator]

      (field ^:private ^:final ^INode/1 array)

      (field ^:private ^:final ^IFn f)

      (field ^:private ^int i 0)

      (field ^:private ^Iterator nestedIter)

      (constructor ^:private [this ^INode/1 array ^IFn f]
        (set! (.-array this) array)
        (set! (.-f this) f))

      (method ^:public hasNext ^boolean [this]
        (while true
          (when (some? nestedIter) (if (.hasNext nestedIter) (return true) (set! nestedIter nil)))
          (if (< i (alength array))
              (let [node (aget array (let [old-1 i] (set! i (unchecked-inc-int i)) old-1))]
                (when (some? node) (set! nestedIter (.iterator node f))))
              (return false))))

      (method ^:public next [this]
        (if (.hasNext this) (.next nestedIter) (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (defclass ^:static ^:final BitmapIndexedNode
    :implements [INode]

    (field ^:static ^:final ^BitmapIndexedNode EMPTY (BitmapIndexedNode. nil 0 (new Object/1 0)))

    (field ^int bitmap)

    (field ^Object/1 array)

    (field ^:final ^{:tag (AtomicReference Thread)} edit)

    (method ^:final index ^int [this ^int bit]
      (Integer/bitCount (bit-and-int bitmap (unchecked-subtract-int bit 1))))

    (constructor [this ^{:tag (AtomicReference Thread)} edit ^int bitmap ^Object/1 array]
      (set! (.-bitmap this) bitmap)
      (set! (.-array this) array)
      (set! (.-edit this) edit))

    (method ^:public assoc ^INode [this ^int shift ^int hash key val ^Box addedLeaf]
      (let [bit (PersistentHashMap/bitpos hash shift)
            idx (.index this bit)]
        (if (not (== (bit-and-int bitmap bit) 0))
            (let [keyOrNull (aget array (unchecked-multiply-int 2 idx))
                  valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
              (cond
                (nil? keyOrNull)
                  (let [n (.assoc (cast INode valOrNode)
                                  (unchecked-add-int shift 5)
                                  hash
                                  key
                                  val
                                  addedLeaf)]
                    (if (identical? n valOrNode)
                        this
                        (BitmapIndexedNode. nil
                                            bitmap
                                            (PersistentHashMap/cloneAndSet
                                              array
                                              (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                              n))))
                (Util/equiv key keyOrNull)
                  (if (identical? val valOrNode)
                      this
                      (BitmapIndexedNode. nil
                                          bitmap
                                          (PersistentHashMap/cloneAndSet
                                            array
                                            (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                            val)))
                :else
                  (do
                    (set! (.-val addedLeaf) addedLeaf)
                    (BitmapIndexedNode. nil
                                        bitmap
                                        (PersistentHashMap/cloneAndSet
                                          array
                                          (unchecked-multiply-int 2 idx)
                                          nil
                                          (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                          (PersistentHashMap/createNode
                                            (unchecked-add-int shift 5)
                                            keyOrNull
                                            valOrNode
                                            hash
                                            key
                                            val))))))
            (let [n (Integer/bitCount bitmap)]
              (if (>= n 16)
                  (let [nodes (new INode/1 32)
                        jdx (PersistentHashMap/mask hash shift)]
                    (aset nodes
                          jdx
                          (.assoc EMPTY (unchecked-add-int shift 5) hash key val addedLeaf))
                    (let [^:mutable ^int j 0]
                      (loop [^int i 0]
                        (if (< i 32)
                            (if (not (== (bit-and-int (unsigned-bit-shift-right-int bitmap i) 1) 0))
                                (do
                                  (if (nil? (aget array j))
                                      (aset nodes
                                            i
                                            (cast INode (aget array (unchecked-add-int j 1))))
                                      (aset nodes
                                            i
                                            (.assoc
                                              EMPTY
                                              (unchecked-add-int shift 5)
                                              (PersistentHashMap/hash (aget array j))
                                              (aget array j)
                                              (aget array (unchecked-add-int j 1))
                                              addedLeaf)))
                                  (set! j (unchecked-add-int j 2))
                                  (recur (unchecked-inc-int i)))
                                (recur (unchecked-inc-int i)))
                            nil))
                      (ArrayNode. nil (unchecked-add-int n 1) nodes)))
                  (let [newArray (new Object/1 (unchecked-multiply-int 2 (unchecked-add-int n 1)))]
                    (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 idx))
                    (aset newArray (unchecked-multiply-int 2 idx) key)
                    (set! (.-val addedLeaf) addedLeaf)
                    (aset newArray (unchecked-add-int (unchecked-multiply-int 2 idx) 1) val)
                    (System/arraycopy array
                                      (unchecked-multiply-int 2 idx)
                                      newArray
                                      (unchecked-multiply-int 2 (unchecked-add-int idx 1))
                                      (unchecked-multiply-int 2 (unchecked-subtract-int n idx)))
                    (BitmapIndexedNode. nil (bit-or-int bitmap bit) newArray)))))))

    (method ^:public without ^INode [this ^int shift ^int hash key]
      (let [bit (PersistentHashMap/bitpos hash shift)]
        (if (== (bit-and-int bitmap bit) 0)
            this
            (let [idx (.index this bit)
                  keyOrNull (aget array (unchecked-multiply-int 2 idx))
                  valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
              (cond
                (nil? keyOrNull)
                  (let [n (.without (cast INode valOrNode) (unchecked-add-int shift 5) hash key)]
                    (cond
                      (identical? n valOrNode) this
                      (some? n)
                        (BitmapIndexedNode. nil
                                            bitmap
                                            (PersistentHashMap/cloneAndSet
                                              array
                                              (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                              n))
                      :else
                        (when-not (== bitmap bit)
                          (BitmapIndexedNode. nil
                                              (bit-xor-int bitmap bit)
                                              (PersistentHashMap/removePair array idx)))))
                (Util/equiv key keyOrNull)
                  (when-not (== bitmap bit)
                    (BitmapIndexedNode. nil
                                        (bit-xor-int bitmap bit)
                                        (PersistentHashMap/removePair array idx)))
                :else this)))))

    (method ^:public find ^IMapEntry [this ^int shift ^int hash key]
      (let [bit (PersistentHashMap/bitpos hash shift)]
        (when-not (== (bit-and-int bitmap bit) 0)
          (let [idx (.index this bit)
                keyOrNull (aget array (unchecked-multiply-int 2 idx))
                valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
            (cond
              (nil? keyOrNull) (.find (cast INode valOrNode) (unchecked-add-int shift 5) hash key)
              (Util/equiv key keyOrNull) ^IMapEntry (MapEntry/create keyOrNull valOrNode))))))

    (method ^:public find [this ^int shift ^int hash key notFound]
      (let [bit (PersistentHashMap/bitpos hash shift)]
        (if (== (bit-and-int bitmap bit) 0)
            notFound
            (let [idx (.index this bit)
                  keyOrNull (aget array (unchecked-multiply-int 2 idx))
                  valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
              (cond
                (nil? keyOrNull)
                  (.find (cast INode valOrNode) (unchecked-add-int shift 5) hash key notFound)
                (Util/equiv key keyOrNull) valOrNode
                :else notFound)))))

    (method ^:public nodeSeq ^ISeq [this] (NodeSeq/create array))

    (method ^:public iterator ^Iterator [this ^IFn f] (NodeIter. array f))

    (method ^:public kvreduce [this ^IFn f init]
      (NodeSeq/kvreduce array f init))

    (method ^:public fold [this ^IFn combinef ^IFn reducef ^IFn fjtask ^IFn fjfork ^IFn fjjoin]
      (NodeSeq/kvreduce array reducef (.invoke combinef)))

    (method ^:private ensureEditable ^BitmapIndexedNode [this ^{:tag (AtomicReference Thread)} edit]
      (if (identical? (.-edit this) edit)
          this
          (let [n (Integer/bitCount bitmap)
                newArray (new Object/1
                              (if (>= n 0) (unchecked-multiply-int 2 (unchecked-add-int n 1)) 4))]
            (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 n))
            (BitmapIndexedNode. edit bitmap newArray))))

    (method ^:private editAndSet ^BitmapIndexedNode [this ^{:tag (AtomicReference Thread)} edit
                                                     ^int i a]
      (let [editable (.ensureEditable this edit)] (aset (.-array editable) i a) editable))

    (method ^:private editAndSet ^BitmapIndexedNode [this ^{:tag (AtomicReference Thread)} edit
                                                     ^int i a ^int j b]
      (let [editable (.ensureEditable this edit)]
        (aset (.-array editable) i a)
        (aset (.-array editable) j b)
        editable))

    (method ^:private editAndRemovePair ^BitmapIndexedNode [this
                                                            ^{:tag (AtomicReference Thread)} edit
                                                            ^int bit ^int i]
      (when-not (== bitmap bit)
        (let [editable (.ensureEditable this edit)]
          (set! (.-bitmap editable) (bit-xor-int (.-bitmap editable) bit))
          (System/arraycopy (.-array editable)
                            (unchecked-multiply-int 2 (unchecked-add-int i 1))
                            (.-array editable)
                            (unchecked-multiply-int 2 i)
                            (unchecked-subtract-int
                              (alength (.-array editable))
                              (unchecked-multiply-int 2 (unchecked-add-int i 1))))
          (aset (.-array editable) (unchecked-subtract-int (alength (.-array editable)) 2) nil)
          (aset (.-array editable) (unchecked-subtract-int (alength (.-array editable)) 1) nil)
          editable)))

    (method ^:public assoc ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                   key val ^Box addedLeaf]
      (let [bit (PersistentHashMap/bitpos hash shift)
            idx (.index this bit)]
        (if (not (== (bit-and-int bitmap bit) 0))
            (let [keyOrNull (aget array (unchecked-multiply-int 2 idx))
                  valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
              (cond
                (nil? keyOrNull)
                  (let [n (.assoc (cast INode valOrNode)
                                  edit
                                  (unchecked-add-int shift 5)
                                  hash
                                  key
                                  val
                                  addedLeaf)]
                    (if (identical? n valOrNode)
                        this
                        (.editAndSet this
                                     edit
                                     (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                     n)))
                (Util/equiv key keyOrNull)
                  (if (identical? val valOrNode)
                      this
                      (.editAndSet this
                                   edit
                                   (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                   val))
                :else
                  (do
                    (set! (.-val addedLeaf) addedLeaf)
                    (.editAndSet this
                                 edit
                                 (unchecked-multiply-int 2 idx)
                                 nil
                                 (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                 (PersistentHashMap/createNode
                                   edit
                                   (unchecked-add-int shift 5)
                                   keyOrNull
                                   valOrNode
                                   hash
                                   key
                                   val)))))
            (let [n (Integer/bitCount bitmap)]
              (cond
                (< (unchecked-multiply-int n 2) (alength array))
                  (do
                    (set! (.-val addedLeaf) addedLeaf)
                    (let [editable (.ensureEditable this edit)]
                      (System/arraycopy (.-array editable)
                                        (unchecked-multiply-int 2 idx)
                                        (.-array editable)
                                        (unchecked-multiply-int 2 (unchecked-add-int idx 1))
                                        (unchecked-multiply-int 2 (unchecked-subtract-int n idx)))
                      (aset (.-array editable) (unchecked-multiply-int 2 idx) key)
                      (aset (.-array editable)
                            (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                            val)
                      (set! (.-bitmap editable) (bit-or-int (.-bitmap editable) bit))
                      editable))
                (>= n 16)
                  (let [nodes (new INode/1 32)
                        jdx (PersistentHashMap/mask hash shift)]
                    (aset nodes
                          jdx
                          (.assoc EMPTY edit (unchecked-add-int shift 5) hash key val addedLeaf))
                    (let [^:mutable ^int j 0]
                      (loop [^int i 0]
                        (if (< i 32)
                            (if (not (== (bit-and-int (unsigned-bit-shift-right-int bitmap i) 1) 0))
                                (do
                                  (if (nil? (aget array j))
                                      (aset nodes
                                            i
                                            (cast INode (aget array (unchecked-add-int j 1))))
                                      (aset nodes
                                            i
                                            (.assoc
                                              EMPTY
                                              edit
                                              (unchecked-add-int shift 5)
                                              (PersistentHashMap/hash (aget array j))
                                              (aget array j)
                                              (aget array (unchecked-add-int j 1))
                                              addedLeaf)))
                                  (set! j (unchecked-add-int j 2))
                                  (recur (unchecked-inc-int i)))
                                (recur (unchecked-inc-int i)))
                            nil))
                      (ArrayNode. edit (unchecked-add-int n 1) nodes)))
                :else
                  (let [newArray (new Object/1 (unchecked-multiply-int 2 (unchecked-add-int n 4)))]
                    (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 idx))
                    (aset newArray (unchecked-multiply-int 2 idx) key)
                    (set! (.-val addedLeaf) addedLeaf)
                    (aset newArray (unchecked-add-int (unchecked-multiply-int 2 idx) 1) val)
                    (System/arraycopy array
                                      (unchecked-multiply-int 2 idx)
                                      newArray
                                      (unchecked-multiply-int 2 (unchecked-add-int idx 1))
                                      (unchecked-multiply-int 2 (unchecked-subtract-int n idx)))
                    (let [editable (.ensureEditable this edit)]
                      (set! (.-array editable) newArray)
                      (set! (.-bitmap editable) (bit-or-int (.-bitmap editable) bit))
                      editable)))))))

    (method ^:public without ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                     key ^Box removedLeaf]
      (let [bit (PersistentHashMap/bitpos hash shift)]
        (if (== (bit-and-int bitmap bit) 0)
            this
            (let [idx (.index this bit)
                  keyOrNull (aget array (unchecked-multiply-int 2 idx))
                  valOrNode (aget array (unchecked-add-int (unchecked-multiply-int 2 idx) 1))]
              (cond
                (nil? keyOrNull)
                  (let [n (.without (cast INode valOrNode)
                                    edit
                                    (unchecked-add-int shift 5)
                                    hash
                                    key
                                    removedLeaf)]
                    (cond
                      (identical? n valOrNode) this
                      (some? n)
                        (.editAndSet this
                                     edit
                                     (unchecked-add-int (unchecked-multiply-int 2 idx) 1)
                                     n)
                      :else (when-not (== bitmap bit) (.editAndRemovePair this edit bit idx))))
                (Util/equiv key keyOrNull)
                  (do (set! (.-val removedLeaf) removedLeaf) (.editAndRemovePair this edit bit idx))
                :else this))))))

  (defclass ^:static ^:final HashCollisionNode
    :implements [INode]

    (field ^:final ^int hash)

    (field ^int count)

    (field ^Object/1 array)

    (field ^:final ^{:tag (AtomicReference Thread)} edit)

    (constructor [this ^{:tag (AtomicReference Thread)} edit ^int hash ^int count & ^Object/1 array]
      (set! (.-edit this) edit)
      (set! (.-hash this) hash)
      (set! (.-count this) count)
      (set! (.-array this) array))

    (method ^:public assoc ^INode [this ^int shift ^int hash key val ^Box addedLeaf]
      (if (== hash (.-hash this))
          (let [idx (.findIndex this key)]
            (if (not (== idx -1))
                (if (identical? (aget array (unchecked-add-int idx 1)) val)
                    this
                    (HashCollisionNode. nil
                                        hash
                                        count
                                        (PersistentHashMap/cloneAndSet
                                          array
                                          (unchecked-add-int idx 1)
                                          val)))
                (let [newArray (new Object/1 (unchecked-multiply-int 2 (unchecked-add-int count 1)))]
                  (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 count))
                  (aset newArray (unchecked-multiply-int 2 count) key)
                  (aset newArray (unchecked-add-int (unchecked-multiply-int 2 count) 1) val)
                  (set! (.-val addedLeaf) addedLeaf)
                  (HashCollisionNode. edit hash (unchecked-add-int count 1) newArray))))
          (.assoc (BitmapIndexedNode. nil
                                      (PersistentHashMap/bitpos (.-hash this) shift)
                                      (new Object/1 [nil this]))
                  shift
                  hash
                  key
                  val
                  addedLeaf)))

    (method ^:public without ^INode [this ^int shift ^int hash key]
      (let [idx (.findIndex this key)]
        (if (== idx -1)
            this
            (when-not (== count 1)
              (HashCollisionNode. nil
                                  hash
                                  (unchecked-subtract-int count 1)
                                  (PersistentHashMap/removePair array (unchecked-divide-int idx 2)))))))

    (method ^:public find ^IMapEntry [this ^int shift ^int hash key]
      (let [idx (.findIndex this key)]
        (when-not (< idx 0)
          ^IMapEntry (MapEntry/create (aget array idx) (aget array (unchecked-add-int idx 1))))))

    (method ^:public find [this ^int shift ^int hash key notFound]
      (let [idx (.findIndex this key)]
        (if (< idx 0) notFound (aget array (unchecked-add-int idx 1)))))

    (method ^:public nodeSeq ^ISeq [this] (NodeSeq/create array))

    (method ^:public iterator ^Iterator [this ^IFn f] (NodeIter. array f))

    (method ^:public kvreduce [this ^IFn f init]
      (NodeSeq/kvreduce array f init))

    (method ^:public fold [this ^IFn combinef ^IFn reducef ^IFn fjtask ^IFn fjfork ^IFn fjjoin]
      (NodeSeq/kvreduce array reducef (.invoke combinef)))

    (method ^:public findIndex ^int [this key]
      (loop [^int i 0]
        (if (< i (unchecked-multiply-int 2 count))
            (if (Util/equiv key (aget array i)) (return i) (recur (unchecked-add-int i 2)))
            nil))
      -1)

    (method ^:private ensureEditable ^HashCollisionNode [this ^{:tag (AtomicReference Thread)} edit]
      (if (identical? (.-edit this) edit)
          this
          (let [newArray (new Object/1 (unchecked-multiply-int 2 (unchecked-add-int count 1)))]
            (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 count))
            (HashCollisionNode. edit hash count newArray))))

    (method ^:private ensureEditable ^HashCollisionNode [this ^{:tag (AtomicReference Thread)} edit
                                                         ^int count ^Object/1 array]
      (if (identical? (.-edit this) edit)
          (do (set! (.-array this) array) (set! (.-count this) count) this)
          (HashCollisionNode. edit hash count array)))

    (method ^:private editAndSet ^HashCollisionNode [this ^{:tag (AtomicReference Thread)} edit
                                                     ^int i a]
      (let [editable (.ensureEditable this edit)] (aset (.-array editable) i a) editable))

    (method ^:private editAndSet ^HashCollisionNode [this ^{:tag (AtomicReference Thread)} edit
                                                     ^int i a ^int j b]
      (let [editable (.ensureEditable this edit)]
        (aset (.-array editable) i a)
        (aset (.-array editable) j b)
        editable))

    (method ^:public assoc ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                   key val ^Box addedLeaf]
      (if (== hash (.-hash this))
          (let [idx (.findIndex this key)]
            (cond
              (not (== idx -1))
                (if (identical? (aget array (unchecked-add-int idx 1)) val)
                    this
                    (.editAndSet this edit (unchecked-add-int idx 1) val))
              (> (alength array) (unchecked-multiply-int 2 count))
                (do
                  (set! (.-val addedLeaf) addedLeaf)
                  (let [editable (.editAndSet this
                                              edit
                                              (unchecked-multiply-int 2 count)
                                              key
                                              (unchecked-add-int (unchecked-multiply-int 2 count) 1)
                                              val)]
                    (set! (.-count editable) (unchecked-inc-int (.-count editable)))
                    editable))
              :else
                (let [newArray (new Object/1 (unchecked-add-int (alength array) 2))]
                  (System/arraycopy array 0 newArray 0 (alength array))
                  (aset newArray (alength array) key)
                  (aset newArray (unchecked-add-int (alength array) 1) val)
                  (set! (.-val addedLeaf) addedLeaf)
                  (.ensureEditable this edit (unchecked-add-int count 1) newArray))))
          (.assoc (BitmapIndexedNode. edit
                                      (PersistentHashMap/bitpos (.-hash this) shift)
                                      (new Object/1 [nil this nil nil]))
                  edit
                  shift
                  hash
                  key
                  val
                  addedLeaf)))

    (method ^:public without ^INode [this ^{:tag (AtomicReference Thread)} edit ^int shift ^int hash
                                     key ^Box removedLeaf]
      (let [idx (.findIndex this key)]
        (if (== idx -1)
            this
            (do
              (set! (.-val removedLeaf) removedLeaf)
              (when-not (== count 1)
                (let [editable (.ensureEditable this edit)]
                  (aset (.-array editable)
                        idx
                        (aget (.-array editable)
                              (unchecked-subtract-int (unchecked-multiply-int 2 count) 2)))
                  (aset (.-array editable)
                        (unchecked-add-int idx 1)
                        (aget (.-array editable)
                              (unchecked-subtract-int (unchecked-multiply-int 2 count) 1)))
                  (aset (.-array editable)
                        (unchecked-subtract-int (unchecked-multiply-int 2 count) 2)
                        (aset (.-array editable)
                              (unchecked-subtract-int (unchecked-multiply-int 2 count) 1)
                              nil))
                  (set! (.-count editable) (unchecked-dec-int (.-count editable)))
                  editable)))))))

  (method ^:private ^:static cloneAndSet ^INode/1 [^INode/1 array ^int i ^INode a]
    (let [clone (.clone array)] (aset clone i a) clone))

  (method ^:private ^:static cloneAndSet ^Object/1 [^Object/1 array ^int i a]
    (let [clone (.clone array)] (aset clone i a) clone))

  (method ^:private ^:static cloneAndSet ^Object/1 [^Object/1 array ^int i a ^int j b]
    (let [clone (.clone array)] (aset clone i a) (aset clone j b) clone))

  (method ^:private ^:static removePair ^Object/1 [^Object/1 array ^int i]
    (let [newArray (new Object/1 (unchecked-subtract-int (alength array) 2))]
      (System/arraycopy array 0 newArray 0 (unchecked-multiply-int 2 i))
      (System/arraycopy array
                        (unchecked-multiply-int 2 (unchecked-add-int i 1))
                        newArray
                        (unchecked-multiply-int 2 i)
                        (unchecked-subtract-int (alength newArray) (unchecked-multiply-int 2 i)))
      newArray))

  (method ^:private ^:static createNode ^INode [^int shift key1 val1 ^int key2hash key2 val2]
    (let [key1hash (PersistentHashMap/hash key1)]
      (if (== key1hash key2hash)
          (HashCollisionNode. nil key1hash 2 (new Object/1 [key1 val1 key2 val2]))
          (let [addedLeaf (Box. nil)
                ^{:tag (AtomicReference Thread)} edit (AtomicReference.)]
            (.assoc (.assoc BitmapIndexedNode/EMPTY edit shift key1hash key1 val1 addedLeaf)
                    edit
                    shift
                    key2hash
                    key2
                    val2
                    addedLeaf)))))

  (method ^:private ^:static createNode ^INode [^{:tag (AtomicReference Thread)} edit ^int shift
                                                key1 val1 ^int key2hash key2 val2]
    (let [key1hash (PersistentHashMap/hash key1)]
      (if (== key1hash key2hash)
          (HashCollisionNode. nil key1hash 2 (new Object/1 [key1 val1 key2 val2]))
          (let [addedLeaf (Box. nil)]
            (.assoc (.assoc BitmapIndexedNode/EMPTY edit shift key1hash key1 val1 addedLeaf)
                    edit
                    shift
                    key2hash
                    key2
                    val2
                    addedLeaf)))))

  (method ^:private ^:static bitpos ^int [^int hash ^int shift]
    (bit-shift-left-int 1 (PersistentHashMap/mask hash shift)))

  (defclass ^:static ^:final NodeIter
    :implements [Iterator]

    (field ^:private ^:static ^:final NULL (Object.))

    (field ^:final ^Object/1 array)

    (field ^:final ^IFn f)

    (field ^:private ^int i 0)

    (field ^:private nextEntry NULL)

    (field ^:private ^Iterator nextIter)

    (constructor [this ^Object/1 array ^IFn f]
      (set! (.-array this) array)
      (set! (.-f this) f))

    (method ^:private advance ^boolean [this]
      (while (< i (alength array))
        (let [key (aget array i)
              nodeOrVal (aget array (unchecked-add-int i 1))]
          (set! i (unchecked-add-int i 2))
          (cond
            (some? key) (do (set! nextEntry (.invoke f key nodeOrVal)) (return true))
            (some? nodeOrVal)
              (let [iter (.iterator (cast INode nodeOrVal) f)]
                (when (and (some? iter) (.hasNext iter)) (set! nextIter iter) (return true))))))
      false)

    (method ^:public hasNext ^boolean [this]
      (if (or (not (identical? nextEntry NULL)) (some? nextIter)) true (.advance this)))

    (method ^:public next [this]
      (let [^:mutable ret nextEntry]
        (cond
          (not (identical? ret NULL)) (do (set! nextEntry NULL) ret)
          (some? nextIter)
            (do (set! ret (.next nextIter)) (when-not (.hasNext nextIter) (set! nextIter nil)) ret)
          (.advance this) (.next this)
          :else (throw (NoSuchElementException.)))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.))))

  (defclass ^:static ^:final NodeSeq
    :extends ASeq

    (field ^:final ^Object/1 array)

    (field ^:final ^int i)

    (field ^:final ^ISeq s)

    (constructor [this ^Object/1 array ^int i] (this. nil array i nil))

    (method ^:static create ^ISeq [^Object/1 array]
      (NodeSeq/create array 0 nil))

    (method ^:public ^:static kvreduce [^Object/1 array ^IFn f ^:mutable init]
      (loop [^int i 0]
        (when (< i (alength array))
          (if (some? (aget array i))
              (set! init (.invoke f init (aget array i) (aget array (unchecked-add-int i 1))))
              (let [node (cast INode (aget array (unchecked-add-int i 1)))]
                (when (some? node) (set! init (.kvreduce node f init)))))
          (if (RT/isReduced init) (return init) (recur (unchecked-add-int i 2)))))
      init)

    (method ^:private ^:static create ^ISeq [^Object/1 array ^int i ^ISeq s]
      (if (some? s)
          (NodeSeq. nil array i s)
          (do
            (loop [^int j i]
              (when (< j (alength array))
                (when (some? (aget array j)) (return (NodeSeq. nil array j nil)))
                (let [node (cast INode (aget array (unchecked-add-int j 1)))]
                  (if (some? node)
                      (let [nodeSeq (.nodeSeq node)]
                        (if (some? nodeSeq)
                            (return (NodeSeq. nil array (unchecked-add-int j 2) nodeSeq))
                            (recur (unchecked-add-int j 2))))
                      (recur (unchecked-add-int j 2))))))
            nil)))

    (constructor [this ^IPersistentMap meta ^Object/1 array ^int i ^ISeq s]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i)
      (set! (.-s this) s))

    (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (NodeSeq. meta array i s)))

    (method ^:public first [this]
      (if (some? s)
          (.first s)
          (MapEntry/create (aget array i) (aget array (unchecked-add-int i 1)))))

    (method ^:public next ^ISeq [this]
      (if (some? s)
          (NodeSeq/create array i (.next s))
          (NodeSeq/create array (unchecked-add-int i 2) nil)))))
