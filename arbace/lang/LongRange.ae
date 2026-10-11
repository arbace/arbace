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
;; Converted from clojure/lang/LongRange.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Iterator NoSuchElementException))

(defclass ^:public LongRange
  :extends ASeq
  :implements [Counted IChunkedSeq IReduce IDrop]

  (field ^:private ^:static ^:final ^long serialVersionUID -1467242400566893909)

  (field ^:final ^long start)

  (field ^:final ^long end)

  (field ^:final ^long step)

  (field ^:final ^int count)

  (constructor ^:private [this ^long start ^long end ^long step ^int count]
    (set! (.-start this) start)
    (set! (.-end this) end)
    (set! (.-step this) step)
    (set! (.-count this) count))

  (constructor ^:private [this ^IPersistentMap meta ^long start ^long end ^long step ^int count]
    (super. meta)
    (set! (.-start this) start)
    (set! (.-end this) end)
    (set! (.-step this) step)
    (set! (.-count this) count))

  (method ^:static rangeCount ^long [^long start ^long end ^long step]
    (unchecked-divide
      (^[long long] Numbers/add
        (^[long long] Numbers/add (^[long long] Numbers/minus end start) step)
        (if (> step 0) -1 1))
      step))

  (method ^:public ^:static create ^ISeq [^long end]
    (if (> end 0)
        (try
          (LongRange. 0 end 1 (Math/toIntExact (LongRange/rangeCount 0 end 1)))
          (catch ArithmeticException e (Range/create end)))
        PersistentList/EMPTY))

  (method ^:public ^:static create ^ISeq [^long start ^long end]
    (if (>= start end)
        PersistentList/EMPTY
        (try
          (LongRange. start end 1 (Math/toIntExact (LongRange/rangeCount start end 1)))
          (catch ArithmeticException e (Range/create start end)))))

  (method ^:public ^:static create ^ISeq [^:final ^long start ^long end ^long step]
    (cond
      (> step 0)
        (if (<= end start)
            PersistentList/EMPTY
            (try
              (LongRange. start end step (Math/toIntExact (LongRange/rangeCount start end step)))
              (catch ArithmeticException e (Range/create start end step))))
      (< step 0)
        (if (>= end start)
            PersistentList/EMPTY
            (try
              (LongRange. start end step (Math/toIntExact (LongRange/rangeCount start end step)))
              (catch ArithmeticException e (Range/create start end step))))
      (== end start) PersistentList/EMPTY
      :else (Repeat/create start)))

  (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
    (if (identical? meta (.-_meta this)) this (LongRange. meta start end step count)))

  (method ^:public first [this] start)

  (method ^:public next ^ISeq [this]
    (when (> count 1)
      (LongRange. (unchecked-add start step) end step (unchecked-subtract-int count 1))))

  (field ^:private ^:static ^:final ^int CHUNK_SIZE 32)

  (method ^:public chunkedFirst ^IChunk [this]
    (LongChunk. start step (Math/min count CHUNK_SIZE)))

  (method ^:public chunkedNext ^ISeq [this] (.seq (.chunkedMore this)))

  (method ^:public chunkedMore ^ISeq [this]
    (if (<= count CHUNK_SIZE)
        PersistentList/EMPTY
        (LongRange/create (unchecked-add start (unchecked-multiply step CHUNK_SIZE)) end step)))

  (method ^:public drop ^Sequential [this ^int n]
    (cond
      (<= n 0) this
      (< n count)
        (LongRange. (unchecked-add start (unchecked-multiply step n))
                    end
                    step
                    (unchecked-subtract-int count n))))

  (method ^:public count ^int [this] count)

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable ^Object acc start
          ^:mutable i (unchecked-add start step)
          ^:mutable n count]
      (while (> n 1)
        (set! acc (.invoke f acc i))
        (when (instance? Reduced acc) (return (.deref (cast Reduced acc))))
        (set! i (unchecked-add i step))
        (set! n (unchecked-dec-int n)))
      acc))

  (method ^:public reduce [this ^IFn f val]
    (let [^:mutable acc val
          ^:mutable n count
          ^:mutable i start]
      (loop []
        (set! acc (.invoke f acc i))
        (when (RT/isReduced acc) (return (.deref (cast Reduced acc))))
        (set! i (unchecked-add i step))
        (set! n (unchecked-dec-int n))
        (when (> n 0) (recur)))
      acc))

  (method ^:public iterator ^Iterator [this] (LongRangeIterator.))

  (defclass LongRangeIterator
    :implements [Iterator]

    (field ^:private ^long next)

    (field ^:private ^int remaining)

    (constructor ^:public [this]
      (set! (.-next this) start)
      (set! (.-remaining this) count))

    (method ^:public hasNext ^boolean [this] (> remaining 0))

    (method ^:public next [this]
      (if (> remaining 0)
          (let [ret next]
            (set! next (unchecked-add next step))
            (set! remaining (unchecked-subtract-int remaining 1))
            ret)
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.))))

  (defclass ^:private ^:static LongChunk
    :implements [IChunk Serializable]

    (field ^:final ^long start)

    (field ^:final ^long step)

    (field ^:final ^int count)

    (constructor ^:public [this ^long start ^long step ^int count]
      (set! (.-start this) start)
      (set! (.-step this) step)
      (set! (.-count this) count))

    (method ^:public first ^long [this] start)

    (method ^:public nth [this ^int i]
      (unchecked-add start (unchecked-multiply i step)))

    (method ^:public nth [this ^int i notFound]
      (if (and (>= i 0) (< i count)) (unchecked-add start (unchecked-multiply i step)) notFound))

    (method ^:public count ^int [this] count)

    (method ^:public dropFirst ^LongChunk [this]
      (when (<= count 1) (throw (IllegalStateException. "dropFirst of empty chunk")))
      (LongChunk. (unchecked-add start step) step (unchecked-subtract-int count 1)))

    (method ^:public reduce [this ^IFn f init]
      (let [^:mutable x start
            ^:mutable ret init]
        (loop [^int i 0]
          (when (< i count)
            (set! ret (.invoke f ret x))
            (when (RT/isReduced ret) (return ret))
            (set! x (unchecked-add x step))
            (recur (unchecked-inc-int i))))
        ret))))
