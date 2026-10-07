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
;; /* rich Dec 6, 2007 */
;;
;; Converted from clojure/lang/StringSeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public StringSeq
  :extends ASeq
  :implements [IndexedSeq IDrop IReduceInit]

  (field ^:private ^:static ^:final ^long serialVersionUID 7975525539139301753)

  (field ^:public ^:final ^CharSequence s)

  (field ^:public ^:final ^int i)

  (method ^:public ^:static create ^StringSeq [^CharSequence s]
    (when-not (== (.length s) 0) (StringSeq. nil s 0)))

  (constructor [this ^IPersistentMap meta ^CharSequence s ^int i]
    (super. meta)
    (set! (.-s this) s)
    (set! (.-i this) i))

  (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
    (if (identical? meta (.meta this)) this (StringSeq. meta s i)))

  (method ^:public first [this] (Character/valueOf (.charAt s i)))

  (method ^:public next ^ISeq [this]
    (when (< (unchecked-add-int i 1) (.length s))
      (StringSeq. (.-_meta this) s (unchecked-add-int i 1))))

  (method ^:public index ^int [this] i)

  (method ^:public count ^int [this]
    (unchecked-subtract-int (.length s) i))

  (method ^:public drop ^Sequential [this ^int n]
    (let [ii (unchecked-add-int i n)] (when (< ii (.length s)) (StringSeq. nil s ii))))

  (method ^:public reduce [this ^IFn f start]
    (let [^:mutable acc start]
      (loop [^int ii i]
        (when (< ii (.length s))
          (set! acc (.invoke f acc (.charAt s ii)))
          (if (RT/isReduced acc) (return (.deref (cast IDeref acc))) (recur (unchecked-inc-int ii)))))
      acc)))
