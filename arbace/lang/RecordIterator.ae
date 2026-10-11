;; /**
;;  * Copyright (c) Rich Hickey. All rights reserved.
;;  * The use and distribution terms for this software are covered by the
;;  * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  * which can be found in the file epl-v10.html at the root of this distribution.
;;  * By using this software in any fashion, you are agreeing to be bound by
;;  * the terms of this license.
;;  * You must not remove this notice, or any other, from this software.
;;  */
;;
;; /* ghadi shayban Sep 24, 2014 */
;;
;; Converted from clojure/lang/RecordIterator.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Iterator))

(defclass ^:public ^:final RecordIterator
  :implements [Iterator]

  (field ^int i 0)

  (field ^:final ^int basecnt)

  (field ^:final ^ILookup rec)

  (field ^:final ^IPersistentVector basefields)

  (field ^:final ^Iterator extmap)

  (constructor ^:public [this ^ILookup rec ^IPersistentVector basefields ^Iterator extmap]
    (set! (.-rec this) rec)
    (set! (.-basefields this) basefields)
    (set! (.-basecnt this) (.count basefields))
    (set! (.-extmap this) extmap))

  (method ^:public hasNext ^boolean [this]
    (if (< i basecnt) true (.hasNext extmap)))

  (method ^:public next [this]
    (if (< i basecnt)
        (let [k (.nth basefields i)]
          (set! i (unchecked-inc-int i))
          (MapEntry/create k (.valAt rec k)))
        (.next extmap)))

  (method ^:public remove ^void [this]
    (throw (UnsupportedOperationException.))))
