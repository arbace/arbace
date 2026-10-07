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
;; Converted from clojure/lang/Repeat.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public Repeat
  :extends ASeq
  :implements [IReduce IDrop]

  (field ^:private ^:static ^:final ^long serialVersionUID -5140377547192202551)

  (field ^:private ^:static ^:final ^long INFINITE -1)

  (field ^:private ^:final ^long count)

  (field ^:private ^:final val)

  (field ^:private ^:volatile ^ISeq _next)

  (constructor ^:private [this ^long count val]
    (set! (.-count this) count)
    (set! (.-val this) val))

  (constructor ^:private [this ^IPersistentMap meta ^long count val]
    (super. meta)
    (set! (.-count this) count)
    (set! (.-val this) val))

  (method ^:public ^:static create ^Repeat [val] (Repeat. INFINITE val))

  (method ^:public ^:static create ^ISeq [^long count val]
    (if (<= count 0) PersistentList/EMPTY (Repeat. count val)))

  (method ^:public first [this] val)

  (method ^:public next ^ISeq [this]
    (when (nil? _next)
      (cond
        (> count 1) (set! _next (Repeat. (unchecked-subtract count 1) val))
        (== count INFINITE) (set! _next this)))
    _next)

  (method ^:public withMeta ^Repeat [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (Repeat. meta count val)))

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable ret val]
      (if (== count INFINITE)
          (while true
            (set! ret (.invoke f ret val))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret)))))
          (do
            (loop [i 1]
              (when (< i count)
                (set! ret (.invoke f ret val))
                (if (RT/isReduced ret)
                    (return (.deref (cast IDeref ret)))
                    (recur (unchecked-inc i)))))
            ret))))

  (method ^:public reduce [this ^IFn f start]
    (let [^:mutable ret start]
      (if (== count INFINITE)
          (while true
            (set! ret (.invoke f ret val))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret)))))
          (do
            (loop [i 0]
              (when (< i count)
                (set! ret (.invoke f ret val))
                (if (RT/isReduced ret)
                    (return (.deref (cast IDeref ret)))
                    (recur (unchecked-inc i)))))
            ret))))

  (method ^:public drop ^Sequential [this ^int n]
    (if (== count INFINITE)
        this
        (let [droppedCount (unchecked-subtract count n)]
          (when (> droppedCount 0) (Repeat. droppedCount val)))))

  (method ^:public hashCode ^int [this]
    (if (<= count 0) (throw (UnsupportedOperationException.)) (.hashCode super)))

  (method ^:public hasheq ^int [this]
    (if (<= count 0) (throw (UnsupportedOperationException.)) (.hasheq super))))
