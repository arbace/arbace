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
;; /* Alex Miller 3/3/15 */
;;
;; Converted from clojure/lang/TransformerIterator.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Iterator LinkedList List NoSuchElementException Queue))

(defclass ^:public TransformerIterator
  :implements [Iterator]

  (field ^:private ^:static ^:final ^Buffer EMPTY (Empty.))

  (field ^:private ^:static ^:final NONE (Object.))

  (field ^:private ^:final ^Iterator sourceIter)

  (field ^:private ^:final ^IFn xf)

  (field ^:private ^:final ^boolean multi)

  (field ^:private ^:volatile ^Buffer buffer EMPTY)

  (field ^:private ^:volatile next NONE)

  (field ^:private ^:volatile ^boolean completed false)

  (constructor ^:private [this ^IFn xform ^Iterator sourceIter ^boolean multi]
    (set! (.-sourceIter this) sourceIter)
    (set! (.-xf this)
          (cast
            IFn
            (.invoke xform
                     (anon AFn []
                       (method ^:public invoke [this] nil)

                       (method ^:public invoke [this acc] acc)

                       (method ^:public invoke [this acc o] (set! buffer (.add buffer o)) acc)))))
    (set! (.-multi this) multi))

  (method ^:public ^:static create ^Iterator [^IFn xform ^Iterator source]
    (TransformerIterator. xform source false))

  (method ^:public ^:static createMulti ^Iterator [^IFn xform ^List sources]
    (let [iters (new Iterator/1 (.size sources))]
      (loop [^int i 0]
        (when (< i (.size sources))
          (aset iters i (cast Iterator (.get sources i)))
          (recur (unchecked-inc-int i))))
      (TransformerIterator. xform (MultiIterator. iters) true)))

  (method ^:private step ^boolean [this]
    (if (not (identical? next NONE))
        true
        (do
          (while (identical? next NONE)
            (if (.isEmpty buffer)
                (cond
                  completed (return false)
                  (.hasNext sourceIter)
                    (let [^:mutable ^Object iter nil]
                      (if multi
                          (set! iter (.applyTo xf (RT/cons nil (.next sourceIter))))
                          (set! iter (.invoke xf nil (.next sourceIter))))
                      (when (RT/isReduced iter) (.invoke xf nil) (set! completed true)))
                  :else (do (.invoke xf nil) (set! completed true)))
                (set! next (.remove buffer))))
          true)))

  (method ^:public hasNext ^boolean [this] (.step this))

  (method ^:public next [this]
    (if (.hasNext this) (let [ret next] (set! next NONE) ret) (throw (NoSuchElementException.))))

  (method ^:public remove ^void [this]
    (throw (UnsupportedOperationException.)))

  (defclass ^:private ^:static ^:interface Buffer
    (method add ^Buffer [this o])

    (method remove [this])

    (method isEmpty ^boolean [this]))

  (defclass ^:private ^:static Empty
    :implements [Buffer]

    (method ^:public add ^Buffer [this o] (Single. o))

    (method ^:public remove [this]
      (throw (IllegalStateException. "Removing object from empty buffer")))

    (method ^:public isEmpty ^boolean [this] true)

    (method ^:public toString ^String [this] "Empty"))

  (defclass ^:private ^:static Single
    :implements [Buffer]

    (field ^:private ^:volatile val)

    (constructor ^:public [this o] (set! (.-val this) o))

    (method ^:public add ^Buffer [this o]
      (if (identical? val NONE) (do (set! val o) this) (Many. val o)))

    (method ^:public remove [this]
      (when (identical? val NONE)
        (throw (IllegalStateException. "Removing object from empty buffer")))
      (let [ret val] (set! val NONE) ret))

    (method ^:public isEmpty ^boolean [this] (identical? val NONE))

    (method ^:public toString ^String [this] (java-str "Single: " val)))

  (defclass ^:private ^:static Many
    :implements [Buffer]

    (field ^:private ^:final ^Queue vals (LinkedList.))

    (constructor ^:public [this o1 o2] (.add vals o1) (.add vals o2))

    (method ^:public add ^Buffer [this o] (.add vals o) this)

    (method ^:public remove [this] (.remove vals))

    (method ^:public isEmpty ^boolean [this] (.isEmpty vals))

    (method ^:public toString ^String [this]
      (java-str "Many: " (.toString vals))))

  (defclass ^:private ^:static MultiIterator
    :implements [Iterator]

    (field ^:private ^:final ^Iterator/1 iters)

    (constructor ^:public [this ^Iterator/1 iters]
      (set! (.-iters this) iters))

    (method ^:public hasNext ^boolean [this]
      (for-each [^Iterator iter iters] (when-not (.hasNext iter) (return false)))
      true)

    (method ^:public next [this]
      (let [nexts (new Object/1 (alength iters))]
        (loop [^int i 0]
          (when (< i (alength iters))
            (aset nexts i (.next (aget iters i)))
            (recur (unchecked-inc-int i))))
        (ArraySeq. nexts 0)))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException.)))))
