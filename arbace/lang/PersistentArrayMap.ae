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
;; Converted from clojure/lang/PersistentArrayMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Arrays Iterator Map Map$Entry NoSuchElementException))

(defclass ^:public PersistentArrayMap
  :extends APersistentMap
  :implements [IObj IEditableCollection IMapIterable IKVReduce IDrop]

  (field ^:private ^:static ^:final ^long serialVersionUID -2074065891090893601)

  (field ^:final ^Object/1 array)

  (field ^:static ^:final ^int HASHTABLE_THRESHOLD 16)

  (field ^:static ^:final ^int KW_HASHTABLE_THRESHOLD 128)

  (field ^:public ^:static ^:final ^PersistentArrayMap EMPTY (PersistentArrayMap.))

  (field ^:private ^:final ^IPersistentMap _meta)

  (method ^:public ^:static create ^IPersistentMap [^Map other]
    (let [^:mutable ret (.asTransient EMPTY)]
      (for-each [o (.entrySet other)]
        (let [e (cast Map$Entry o)] (set! ret (.assoc ret (.getKey e) (.getValue e)))))
      (.persistent ret)))

  (constructor ^:protected [this]
    (set! (.-array this) (new Object/1 []))
    (set! (.-_meta this) nil))

  (method ^:public withMeta ^PersistentArrayMap [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (PersistentArrayMap. meta array)))

  (method create ^PersistentArrayMap [this & ^Object/1 init]
    (PersistentArrayMap. (.meta this) init))

  (method createHT ^IPersistentMap [this ^Object/1 init]
    (PersistentHashMap/create (.meta this) init))

  (method ^:public ^:static canBePAM ^boolean [^Object/1 init]
    (cond
      (<= (alength init) HASHTABLE_THRESHOLD) true
      (<= (alength init) KW_HASHTABLE_THRESHOLD)
        (do
          (loop [^int i HASHTABLE_THRESHOLD]
            (if (< i (alength init))
                (if (not (instance? Keyword (aget init i)))
                    (return false)
                    (recur (unchecked-add-int i 2)))
                nil))
          true)
      :else false))

  (method ^:public ^:static createWithCheck ^PersistentArrayMap [^Object/1 init]
    (loop [^int i 0]
      (when (< i (alength init))
        (loop [^int j (unchecked-add-int i 2)]
          (if (< j (alength init))
              (if (PersistentArrayMap/equalKey (aget init i) (aget init j))
                  (throw (IllegalArgumentException. (java-str "Duplicate key: " (aget init i))))
                  (recur (unchecked-add-int j 2)))
              nil))
        (recur (unchecked-add-int i 2))))
    (PersistentArrayMap. init))

  (method ^:public ^:static createAsIfByAssoc ^PersistentArrayMap [^Object/1 init]
    (let [^:mutable ^boolean complexPath false
          ^:mutable ^boolean hasTrailing false]
      (set! complexPath (set! hasTrailing (== (bit-and-int (alength init) 1) 1)))
      (loop [^int i 0]
        (when (and (< i (alength init)) (not complexPath))
          (loop [^int j 0]
            (if (< j i)
                (if (PersistentArrayMap/equalKey (aget init i) (aget init j))
                    (set! complexPath true)
                    (recur (unchecked-add-int j 2)))
                nil))
          (recur (unchecked-add-int i 2))))
      (if complexPath
          (PersistentArrayMap/createAsIfByAssocComplexPath init hasTrailing)
          (PersistentArrayMap. init))))

  (method ^:private ^:static growSeedArray ^Object/1 [^Object/1 seed ^IPersistentCollection trailing]
    (let [^:mutable extraKVs (.seq trailing)
          seedCount (unchecked-subtract-int (alength seed) 1)
          result (Arrays/copyOf seed
                                (unchecked-add-int
                                  seedCount
                                  (unchecked-multiply-int (.count trailing) 2)))]
      (let [^:mutable i seedCount]
        (while (some? extraKVs)
          (let [e (cast Map$Entry (.first extraKVs))]
            (aset result i (.getKey e))
            (aset result (unchecked-add-int i 1) (.getValue e)))
          (set! extraKVs (.next extraKVs))
          (set! i (unchecked-add-int i 2))))
      result))

  (method ^:private ^:static createAsIfByAssocComplexPath ^PersistentArrayMap [^:mutable ^Object/1 init
                                                                               ^boolean hasTrailing]
    (when hasTrailing
      (let [trailing (.cons PersistentArrayMap/EMPTY
                            (aget init (unchecked-subtract-int (alength init) 1)))]
        (set! init (PersistentArrayMap/growSeedArray init trailing))))
    (let [^:mutable ^int n 0]
      (loop [^int i 0]
        (when (< i (alength init))
          (let [^:mutable duplicateKey false]
            (loop [^int j 0]
              (if (< j i)
                  (if (PersistentArrayMap/equalKey (aget init i) (aget init j))
                      (set! duplicateKey true)
                      (recur (unchecked-add-int j 2)))
                  nil))
            (if (not duplicateKey)
                (do (set! n (unchecked-add-int n 2)) (recur (unchecked-add-int i 2)))
                (recur (unchecked-add-int i 2))))))
      (when (< n (alength init))
        (let [nodups (new Object/1 n)
              ^:mutable ^int m 0]
          (loop [^int i 0]
            (when (< i (alength init))
              (let [^:mutable duplicateKey false]
                (loop [^int j 0]
                  (if (< j m)
                      (if (PersistentArrayMap/equalKey (aget init i) (aget nodups j))
                          (set! duplicateKey true)
                          (recur (unchecked-add-int j 2)))
                      nil))
                (if (not duplicateKey)
                    (let [^:mutable ^int j 0]
                      (set! j (unchecked-subtract-int (alength init) 2))
                      (while (>= j i)
                        (when (PersistentArrayMap/equalKey (aget init i) (aget init j)) (break))
                        (set! j (unchecked-subtract-int j 2)))
                      (aset nodups m (aget init i))
                      (aset nodups (unchecked-add-int m 1) (aget init (unchecked-add-int j 1)))
                      (set! m (unchecked-add-int m 2))
                      (recur (unchecked-add-int i 2)))
                    (recur (unchecked-add-int i 2))))))
          (when-not (== m n) (throw (IllegalArgumentException. (java-str "Internal error: m=" m))))
          (set! init nodups)))
      (PersistentArrayMap. init)))

  (constructor ^:public [this ^Object/1 init]
    (set! (.-array this) init)
    (set! (.-_meta this) nil))

  (constructor ^:public [this ^IPersistentMap meta ^Object/1 init]
    (set! (.-_meta this) meta)
    (set! (.-array this) init))

  (method ^:public count ^int [this]
    (unchecked-divide-int (alength array) 2))

  (method ^:public containsKey ^boolean [this key]
    (>= (.indexOf this key) 0))

  (method ^:public entryAt ^IMapEntry [this key]
    (let [i (.indexOf this key)]
      (when (>= i 0)
        ^IMapEntry (MapEntry/create (aget array i) (aget array (unchecked-add-int i 1))))))

  (method ^:public assocEx ^IPersistentMap [this key val]
    (let [i (.indexOf this key)
          ^:mutable ^Object/1 newArray nil]
      (if (>= i 0)
          (throw (Util/runtimeException "Key already present"))
          (let [isKW (instance? Keyword key)]
            (when (or (and isKW (>= (alength array) KW_HASHTABLE_THRESHOLD))
                      (and (not isKW) (>= (alength array) HASHTABLE_THRESHOLD)))
              (return (.assocEx (.createHT this array) key val)))
            (set! newArray (new Object/1 (unchecked-add-int (alength array) 2)))
            (when (> (alength array) 0) (System/arraycopy array 0 newArray 2 (alength array)))
            (aset newArray 0 key)
            (aset newArray 1 val)))
      (.create this newArray)))

  (method ^:public assoc ^IPersistentMap [this key val]
    (let [i (.indexOf this key)
          ^:mutable ^Object/1 newArray nil]
      (if (>= i 0)
          (do
            (when (identical? (aget array (unchecked-add-int i 1)) val) (return this))
            (set! newArray (.clone array))
            (aset newArray (unchecked-add-int i 1) val))
          (let [isKW (instance? Keyword key)]
            (when (or (and isKW (>= (alength array) KW_HASHTABLE_THRESHOLD))
                      (and (not isKW) (>= (alength array) HASHTABLE_THRESHOLD)))
              (return (.assoc (.createHT this array) key val)))
            (set! newArray (new Object/1 (unchecked-add-int (alength array) 2)))
            (when (> (alength array) 0) (System/arraycopy array 0 newArray 0 (alength array)))
            (aset newArray (unchecked-subtract-int (alength newArray) 2) key)
            (aset newArray (unchecked-subtract-int (alength newArray) 1) val)))
      (.create this newArray)))

  (method ^:public without ^IPersistentMap [this key]
    (let [i (.indexOf this key)]
      (if (>= i 0)
          (let [newlen (unchecked-subtract-int (alength array) 2)]
            (if (== newlen 0)
                (.empty this)
                (let [newArray (new Object/1 newlen)]
                  (System/arraycopy array 0 newArray 0 i)
                  (System/arraycopy array
                                    (unchecked-add-int i 2)
                                    newArray
                                    i
                                    (unchecked-subtract-int newlen i))
                  (.create this newArray))))
          this)))

  (method ^:public empty ^IPersistentMap [this]
    ^IPersistentMap (.withMeta EMPTY (.meta this)))

  (method ^:public ^:final valAt [this key notFound]
    (let [i (.indexOf this key)] (if (>= i 0) (aget array (unchecked-add-int i 1)) notFound)))

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public capacity ^int [this] (.count this))

  (method ^:private indexOfObject ^int [this key]
    (let [ep (Util/equivPred key)]
      (loop [^int i 0]
        (if (< i (alength array))
            (if (.equiv ep key (aget array i)) (return i) (recur (unchecked-add-int i 2)))
            nil))
      -1))

  (method ^:private indexOf ^int [this key]
    (if (instance? Keyword key)
        (do
          (loop [^int i 0]
            (if (< i (alength array))
                (if (identical? key (aget array i)) (return i) (recur (unchecked-add-int i 2)))
                nil))
          -1)
        (.indexOfObject this key)))

  (method ^:static equalKey ^boolean [k1 k2]
    (if (instance? Keyword k1) (identical? k1 k2) (Util/equiv k1 k2)))

  (method ^:public iterator ^Iterator [this]
    (Iter. array APersistentMap/MAKE_ENTRY))

  (method ^:public keyIterator ^Iterator [this]
    (Iter. array APersistentMap/MAKE_KEY))

  (method ^:public valIterator ^Iterator [this]
    (Iter. array APersistentMap/MAKE_VAL))

  (method ^:public seq ^ISeq [this]
    (when (> (alength array) 0) (Seq. array 0)))

  (method ^:public drop ^Sequential [this ^int n]
    (when (> (alength array) 0) (.drop (cast Seq (.seq this)) n)))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (defclass ^:static Seq
    :extends ASeq
    :implements [Counted IReduce IDrop]

    (field ^:final ^Object/1 array)

    (field ^:final ^int i)

    (constructor [this ^Object/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (constructor ^:public [this ^IPersistentMap meta ^Object/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this]
      (MapEntry/create (aget array i) (aget array (unchecked-add-int i 1))))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 2) (alength array)) (Seq. array (unchecked-add-int i 2))))

    (method ^:public count ^int [this]
      (unchecked-divide-int (unchecked-subtract-int (alength array) i) 2))

    (method ^:public drop ^Sequential [this ^int n]
      (when (< n (.count this)) (Seq. array (unchecked-add-int i (unchecked-multiply-int 2 n)))))

    (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (Seq. meta array i)))

    (method ^:public iterator ^Iterator [this]
      (Iter. array (unchecked-subtract-int i 2) APersistentMap/MAKE_ENTRY))

    (method ^:public reduce [this ^IFn f]
      (if (< i (alength array))
          (let [^:mutable ^Object acc (MapEntry/create
                                        (aget array i)
                                        (aget array (unchecked-add-int i 1)))]
            (loop [^int j (unchecked-add-int i 2)]
              (when (< j (alength array))
                (set! acc
                      (.invoke
                        f
                        acc
                        (MapEntry/create (aget array j) (aget array (unchecked-add-int j 1)))))
                (if (RT/isReduced acc)
                    (return (.deref (cast IDeref acc)))
                    (recur (unchecked-add-int j 2)))))
            acc)
          (.invoke f)))

    (method ^:public reduce [this ^IFn f init]
      (let [^:mutable acc init]
        (loop [^int j i]
          (when (< j (alength array))
            (set! acc
                  (.invoke f
                           acc
                           (MapEntry/create (aget array j) (aget array (unchecked-add-int j 1)))))
            (if (RT/isReduced acc)
                (return (.deref (cast IDeref acc)))
                (recur (unchecked-add-int j 2)))))
        acc)))

  (defclass ^:static Iter
    :implements [Iterator]

    (field ^:final ^IFn f)

    (field ^:final ^Object/1 array)

    (field ^int i)

    (constructor [this ^Object/1 array ^IFn f] (this. array -2 f))

    (constructor [this ^Object/1 array ^int i ^IFn f]
      (set! (.-array this) array)
      (set! (.-i this) i)
      (set! (.-f this) f))

    (method ^:public hasNext ^boolean [this]
      (< i (unchecked-subtract-int (alength array) 2)))

    (method ^:public next [this]
      (try
        (set! i (unchecked-add-int i 2))
        (.invoke f (aget array i) (aget array (unchecked-add-int i 1)))
        (catch IndexOutOfBoundsException e (throw (NoSuchElementException.)))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.))))

  (method ^:public kvreduce [this ^IFn f ^:mutable init]
    (loop [^int i 0]
      (when (< i (alength array))
        (set! init (.invoke f init (aget array i) (aget array (unchecked-add-int i 1))))
        (if (RT/isReduced init)
            (return (.deref (cast IDeref init)))
            (recur (unchecked-add-int i 2)))))
    init)

  (method ^:public asTransient ^ITransientMap [this]
    (TransientArrayMap. _meta array))

  (defclass ^:static ^:final TransientArrayMap
    :extends ATransientMap

    (field ^int len)

    (field ^:final ^Object/1 array)

    (field owner)

    (field ^:private ^:final ^IPersistentMap _meta)

    (constructor ^:private [this ^IPersistentMap meta ^Object/1 array ^int capacity]
      (set! (.-array this) (new Object/1 capacity))
      (set! (.-owner this) (.-array this))
      (System/arraycopy array 0 (.-array this) 0 (alength array))
      (set! (.-len this) (alength array))
      (set! (.-_meta this) meta))

    (constructor ^:public [this ^IPersistentMap meta ^Object/1 array]
      (this. meta array (Math/max HASHTABLE_THRESHOLD (alength array))))

    (method ^:private indexOfObject ^int [this key]
      (loop [^int i 0]
        (if (< i len)
            (if (PersistentArrayMap/equalKey (aget array i) key)
                (return i)
                (recur (unchecked-add-int i 2)))
            nil))
      -1)

    (method ^:private indexOf ^int [this key]
      (if (instance? Keyword key)
          (do
            (loop [^int i 0]
              (if (< i len)
                  (if (identical? key (aget array i)) (return i) (recur (unchecked-add-int i 2)))
                  nil))
            -1)
          (.indexOfObject this key)))

    (field ^:private ^:static ^:final ^double GROW_FACTOR 1.5)

    (method ^:public assoc ^ITransientMap [this key val]
      (.ensureEditable this)
      (let [i (.indexOf this key)]
        (cond
          (>= i 0)
            (when-not (identical? (aget array (unchecked-add-int i 1)) val)
              (aset array (unchecked-add-int i 1) val))
          (< len (alength array))
            (do
              (aset array len key)
              (aset array (unchecked-add-int len 1) val)
              (set! len (unchecked-add-int len 2)))
          (instance? Keyword key)
            (let [^:mutable growCap (unchecked-int
                                      (Math/ceil (unchecked-multiply (alength array) GROW_FACTOR)))]
              (set! growCap (unchecked-add-int growCap (bit-and-int growCap 1)))
              (if (<= growCap KW_HASHTABLE_THRESHOLD)
                  (return (.assoc (TransientArrayMap. _meta array growCap) key val))
                  (return (.assoc (.asTransient (PersistentHashMap/create _meta array)) key val))))
          :else (return (.assoc (.asTransient (PersistentHashMap/create _meta array)) key val)))
        this))

    (method doAssoc ^ITransientMap [this key val] (.assoc this key val))

    (method ^:public without ^ITransientMap [this key]
      (.ensureEditable this)
      (let [i (.indexOf this key)]
        (when (>= i 0)
          (when (>= len 2)
            (aset array i (aget array (unchecked-subtract-int len 2)))
            (aset array (unchecked-add-int i 1) (aget array (unchecked-subtract-int len 1))))
          (set! len (unchecked-subtract-int len 2)))
        this))

    (method doWithout ^ITransientMap [this key] (.without this key))

    (method ^:public valAt [this key notFound]
      (.ensureEditable this)
      (let [i (.indexOf this key)] (if (>= i 0) (aget array (unchecked-add-int i 1)) notFound)))

    (method doValAt [this key notFound] (.valAt this key notFound))

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
      (unchecked-divide-int len 2))

    (method doCount ^int [this] (.count this))

    (method ^:public persistent ^IPersistentMap [this]
      (.ensureEditable this)
      (set! owner nil)
      (let [a (new Object/1 len)]
        (System/arraycopy array 0 a 0 (alength a))
        (PersistentArrayMap. _meta a)))

    (method doPersistent ^IPersistentMap [this] (.persistent this))

    (method ensureEditable ^void [this]
      (when (nil? owner) (throw (IllegalAccessError. "Transient used after persistent! call"))))))
