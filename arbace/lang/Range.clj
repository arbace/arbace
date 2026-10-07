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
;; Converted from clojure/lang/Range.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Iterator NoSuchElementException))

(defclass ^:public Range
  :extends ASeq
  :implements [IChunkedSeq IReduce]

  (field ^:private ^:static ^:final ^long serialVersionUID -71973733672395145)

  (field ^:private ^:static ^:final ^int CHUNK_SIZE 32)

  (field ^:final end)

  (field ^:final start)

  (field ^:final step)

  (field ^:final ^BoundsCheck boundsCheck)

  (field ^:private ^:volatile ^IChunk _chunk)

  (field ^:private ^:volatile ^ISeq _chunkNext)

  (field ^:private ^:volatile ^ISeq _next)

  (defclass ^:private ^:static ^:interface BoundsCheck
    :extends [Serializable]

    (method exceededBounds ^boolean [this val]))

  (method ^:private ^:static positiveStep ^BoundsCheck [^:final end]
    (anon BoundsCheck []
      (method ^:public exceededBounds ^boolean [this val]
        (Numbers/gte val end))))

  (method ^:private ^:static negativeStep ^BoundsCheck [^:final end]
    (anon BoundsCheck []
      (method ^:public exceededBounds ^boolean [this val]
        (Numbers/lte val end))))

  (constructor ^:private [this start end step ^BoundsCheck boundsCheck]
    (set! (.-end this) end)
    (set! (.-start this) start)
    (set! (.-step this) step)
    (set! (.-boundsCheck this) boundsCheck))

  (constructor ^:private [this start end step ^BoundsCheck boundsCheck ^IChunk chunk ^ISeq chunkNext]
    (set! (.-end this) end)
    (set! (.-start this) start)
    (set! (.-step this) step)
    (set! (.-boundsCheck this) boundsCheck)
    (set! (.-_chunk this) chunk)
    (set! (.-_chunkNext this) chunkNext))

  (constructor ^:private [this ^IPersistentMap meta start end step ^BoundsCheck boundsCheck
                          ^IChunk chunk ^ISeq chunkNext]
    (super. meta)
    (set! (.-end this) end)
    (set! (.-start this) start)
    (set! (.-step this) step)
    (set! (.-boundsCheck this) boundsCheck)
    (set! (.-_chunk this) chunk)
    (set! (.-_chunkNext this) chunkNext))

  (method ^:public ^:static create ^ISeq [end]
    (if (Numbers/isPos end) (Range. 0 end 1 (Range/positiveStep end)) PersistentList/EMPTY))

  (method ^:public ^:static create ^ISeq [start end]
    (Range/create start end 1))

  (method ^:public ^:static create ^ISeq [^:final start end step]
    (cond
      (or (or (and (Numbers/isPos step) (Numbers/gt start end))
              (and (Numbers/isNeg step) (Numbers/gt end start)))
          (Numbers/equiv start end))
        PersistentList/EMPTY
      (Numbers/isZero step) (Repeat/create start)
      :else
        (Range. start
                end
                step
                (if (Numbers/isPos step) (Range/positiveStep end) (Range/negativeStep end)))))

  (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
    (if (identical? meta (.-_meta this))
        this
        (Range. meta start end step boundsCheck _chunk _chunkNext)))

  (method ^:public first [this] start)

  (method ^:public forceChunk ^void [this]
    (when-not (some? _chunk)
      (let [arr (new Object/1 CHUNK_SIZE)
            ^:mutable ^int n 0
            ^:mutable val start]
        (while (< n CHUNK_SIZE)
          (aset arr n val)
          (set! n (unchecked-inc-int n))
          (set! val (Numbers/addP val step))
          (when (.exceededBounds boundsCheck val) (set! _chunk (ArrayChunk. arr 0 n)) (return)))
        (if (.exceededBounds boundsCheck val)
            (set! _chunk (ArrayChunk. arr 0 CHUNK_SIZE))
            (do
              (set! _chunk (ArrayChunk. arr 0 CHUNK_SIZE))
              (set! _chunkNext (Range. val end step boundsCheck)))))))

  (method ^:public next ^ISeq [this]
    (if (some? _next)
        _next
        (do
          (.forceChunk this)
          (if (> (.count _chunk) 1)
              (let [smallerChunk (.dropFirst _chunk)]
                (set! _next
                      (Range. (.nth smallerChunk 0) end step boundsCheck smallerChunk _chunkNext))
                _next)
              (.chunkedNext this)))))

  (method ^:public chunkedFirst ^IChunk [this] (.forceChunk this) _chunk)

  (method ^:public chunkedNext ^ISeq [this] (.seq (.chunkedMore this)))

  (method ^:public chunkedMore ^ISeq [this]
    (.forceChunk this)
    (if (nil? _chunkNext) PersistentList/EMPTY _chunkNext))

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable acc start
          ^:mutable i (Numbers/addP start step)]
      (while (not (.exceededBounds boundsCheck i))
        (set! acc (.invoke f acc i))
        (when (RT/isReduced acc) (return (.deref (cast Reduced acc))))
        (set! i (Numbers/addP i step)))
      acc))

  (method ^:public reduce [this ^IFn f val]
    (let [^:mutable acc val
          ^:mutable i start]
      (while (not (.exceededBounds boundsCheck i))
        (set! acc (.invoke f acc i))
        (when (RT/isReduced acc) (return (.deref (cast Reduced acc))))
        (set! i (Numbers/addP i step)))
      acc))

  (method ^:public iterator ^Iterator [this] (RangeIterator.))

  (defclass ^:private RangeIterator
    :implements [Iterator]

    (field ^:private next)

    (constructor ^:public [this] (set! (.-next this) start))

    (method ^:public hasNext ^boolean [this]
      (not (.exceededBounds boundsCheck next)))

    (method ^:public next [this]
      (if (.hasNext this)
          (let [ret next] (set! next (Numbers/addP next step)) ret)
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.)))))
