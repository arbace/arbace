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
;; /* rich May 24, 2009 */
;;
;; Converted from clojure/lang/ArrayChunk.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable))

(defclass ^:public ^:final ArrayChunk
  :implements [IChunk Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID -8302142882294545702)

  (field ^:final ^Object/1 array)

  (field ^:final ^int off)

  (field ^:final ^int end)

  (constructor ^:public [this ^Object/1 array]
    (this. array 0 (alength array)))

  (constructor ^:public [this ^Object/1 array ^int off]
    (this. array off (alength array)))

  (constructor ^:public [this ^Object/1 array ^int off ^int end]
    (set! (.-array this) array)
    (set! (.-off this) off)
    (set! (.-end this) end))

  (method ^:public nth [this ^int i]
    (aget array (unchecked-add-int off i)))

  (method ^:public nth [this ^int i notFound]
    (if (and (>= i 0) (< i (.count this))) (.nth this i) notFound))

  (method ^:public count ^int [this] (unchecked-subtract-int end off))

  (method ^:public dropFirst ^IChunk [this]
    (when (== off end) (throw (IllegalStateException. "dropFirst of empty chunk")))
    (ArrayChunk. array (unchecked-add-int off 1) end))

  (method ^:public reduce [this ^IFn f start]
    (let [^:mutable ret (.invoke f start (aget array off))]
      (if (RT/isReduced ret)
          ret
          (do
            (loop [^int x (unchecked-add-int off 1)]
              (when (< x end)
                (set! ret (.invoke f ret (aget array x)))
                (if (RT/isReduced ret) (return ret) (recur (unchecked-inc-int x)))))
            ret)))))
