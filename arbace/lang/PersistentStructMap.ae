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
;; /* rich Dec 16, 2007 */
;;
;; Converted from clojure/lang/PersistentStructMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Iterator Map$Entry NoSuchElementException))

(defclass ^:public PersistentStructMap
  :extends APersistentMap
  :implements [IObj]

  (field ^:private ^:static ^:final ^long serialVersionUID -2701411408470234065)

  (defclass ^:public ^:static Def
    :implements [Serializable]

    (field ^:final ^ISeq keys)

    (field ^:final ^IPersistentMap keyslots)

    (constructor [this ^ISeq keys ^IPersistentMap keyslots]
      (set! (.-keys this) keys)
      (set! (.-keyslots this) keyslots)))

  (field ^:final ^Def def)

  (field ^:final ^Object/1 vals)

  (field ^:final ^IPersistentMap ext)

  (field ^:final ^IPersistentMap _meta)

  (method ^:public ^:static createSlotMap ^Def [^ISeq keys]
    (when (nil? keys) (throw (IllegalArgumentException. "Must supply keys")))
    (let [c (RT/count keys)
          v (new Object/1 (unchecked-multiply-int 2 c))
          ^:mutable ^int i 0]
      (let [^:mutable s keys]
        (while (some? s)
          (aset v (unchecked-multiply-int 2 i) (.first s))
          (aset v (unchecked-add-int (unchecked-multiply-int 2 i) 1) i)
          (set! s (.next s))
          (set! i (unchecked-inc-int i))))
      (Def. keys (RT/map v))))

  (method ^:public ^:static create ^PersistentStructMap [^Def def ^:mutable ^ISeq keyvals]
    (let [vals (new Object/1 (.count (.-keyslots def)))
          ^:mutable ^IPersistentMap ext PersistentHashMap/EMPTY]
      (while (some? keyvals)
        (when (nil? (.next keyvals))
          (throw
            (IllegalArgumentException.
              (String/format "No value supplied for key: %s" (new Object/1 [(.first keyvals)])))))
        (let [k (.first keyvals)
              v (RT/second keyvals)
              ^Map$Entry e (.entryAt (.-keyslots def) k)]
          (if (some? e) (aset vals (cast Integer (.getValue e)) v) (set! ext (.assoc ext k v))))
        (set! keyvals (.next (.next keyvals))))
      (PersistentStructMap. nil def vals ext)))

  (method ^:public ^:static construct ^PersistentStructMap [^Def def ^:mutable ^ISeq valseq]
    (let [vals (new Object/1 (.count (.-keyslots def)))
          ^IPersistentMap ext PersistentHashMap/EMPTY]
      (let [^:mutable ^int i 0]
        (while (and (< i (alength vals)) (some? valseq))
          (aset vals i (.first valseq))
          (set! valseq (.next valseq))
          (set! i (unchecked-inc-int i))))
      (when (some? valseq)
        (throw (IllegalArgumentException. "Too many arguments to struct constructor")))
      (PersistentStructMap. nil def vals ext)))

  (method ^:public ^:static getAccessor ^IFn [^:final ^Def def key]
    (let [^Map$Entry e (.entryAt (.-keyslots def) key)]
      (if (some? e)
          (let [^int i (cast Integer (.getValue e))]
            (anon AFn []
              (method ^:public invoke [this arg1]
                (let [m (cast PersistentStructMap arg1)]
                  (when-not (identical? (.-def m) def)
                    (throw (Util/runtimeException "Accessor/struct mismatch")))
                  (aget (.-vals m) i)))))
          (throw (IllegalArgumentException. "Not a key of struct")))))

  (constructor ^:protected [this ^IPersistentMap meta ^Def def ^Object/1 vals ^IPersistentMap ext]
    (set! (.-_meta this) meta)
    (set! (.-ext this) ext)
    (set! (.-def this) def)
    (set! (.-vals this) vals))

  (method ^:protected makeNew ^PersistentStructMap [this ^IPersistentMap meta ^Def def
                                                    ^Object/1 vals ^IPersistentMap ext]
    (PersistentStructMap. meta def vals ext))

  (method ^:public withMeta ^IObj [this ^IPersistentMap meta]
    (if (identical? meta _meta) this (.makeNew this meta def vals ext)))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (method ^:public containsKey ^boolean [this key]
    (or (.containsKey (.-keyslots def) key) (.containsKey ext key)))

  (method ^:public entryAt ^IMapEntry [this key]
    (let [^Map$Entry e (.entryAt (.-keyslots def) key)]
      (if (some? e)
          ^IMapEntry (MapEntry/create (.getKey e) (aget vals (cast Integer (.getValue e))))
          (.entryAt ext key))))

  (method ^:public assoc ^IPersistentMap [this key val]
    (let [^Map$Entry e (.entryAt (.-keyslots def) key)]
      (if (some? e)
          (let [^int i (cast Integer (.getValue e))
                newVals (.clone vals)]
            (aset newVals i val)
            (.makeNew this _meta def newVals ext))
          (.makeNew this _meta def vals (.assoc ext key val)))))

  (method ^:public valAt [this key]
    (let [i (cast Integer (.valAt (.-keyslots def) key))]
      (if (some? i) (aget vals i) (.valAt ext key))))

  (method ^:public valAt [this key notFound]
    (let [i (cast Integer (.valAt (.-keyslots def) key))]
      (if (some? i) (aget vals i) (.valAt ext key notFound))))

  (method ^:public assocEx ^IPersistentMap [this key val]
    (when (.containsKey this key) (throw (Util/runtimeException "Key already present")))
    (.assoc this key val))

  (method ^:public without ^IPersistentMap [this key]
    (let [^Map$Entry e (.entryAt (.-keyslots def) key)]
      (when (some? e) (throw (Util/runtimeException "Can't remove struct key")))
      (let [newExt (.without ext key)]
        (if (identical? newExt ext) this (.makeNew this _meta def vals newExt)))))

  (method ^:public iterator ^Iterator [this]
    (anon Iterator []
      (field ^:private ^ISeq ks (.-keys def))

      (field ^:private ^Iterator extIter (when (some? ext) (.iterator ext)))

      (method ^:public hasNext ^boolean [this]
        (or (and (some? ks) (some? (.seq ks))) (and (some? extIter) (.hasNext extIter))))

      (method ^:public next [this]
        (cond
          (some? ks)
            (let [key (.first ks)] (set! ks (.next ks)) (.entryAt PersistentStructMap/this key))
          (and (some? extIter) (.hasNext extIter)) (.next extIter)
          :else (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (method ^:public count ^int [this]
    (unchecked-add-int (alength vals) (RT/count ext)))

  (method ^:public seq ^ISeq [this] (Seq. nil (.-keys def) vals 0 ext))

  (method ^:public empty ^IPersistentCollection [this]
    (PersistentStructMap/construct def nil))

  (defclass ^:static Seq
    :extends ASeq

    (field ^:final ^int i)

    (field ^:final ^ISeq keys)

    (field ^:final ^Object/1 vals)

    (field ^:final ^IPersistentMap ext)

    (constructor ^:public [this ^IPersistentMap meta ^ISeq keys ^Object/1 vals ^int i
                           ^IPersistentMap ext]
      (super. meta)
      (set! (.-i this) i)
      (set! (.-keys this) keys)
      (set! (.-vals this) vals)
      (set! (.-ext this) ext))

    (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
      (if (not (identical? meta (.-_meta this))) (Seq. meta keys vals i ext) this))

    (method ^:public first [this]
      (MapEntry/create (.first keys) (aget vals i)))

    (method ^:public next ^ISeq [this]
      (if (< (unchecked-add-int i 1) (alength vals))
          (Seq. (.-_meta this) (.next keys) vals (unchecked-add-int i 1) ext)
          (.seq ext)))))
