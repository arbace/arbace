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
;; /* rich Jun 19, 2007 */
;;
;; Converted from clojure/lang/SeqIterator.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Iterator NoSuchElementException))

(defclass ^:public SeqIterator
  :implements [Iterator]

  (field ^:static ^:final START (Object.))

  (field seq)

  (field next)

  (constructor ^:public [this o] (set! seq START) (set! next o))

  (constructor ^:public [this ^ISeq o] (set! seq START) (set! next o))

  (method ^:public hasNext ^boolean [this]
    (cond
      (identical? seq START) (do (set! seq nil) (set! next (RT/seq next)))
      (identical? seq next) (set! next (RT/next seq)))
    (some? next))

  (method ^:public next :throws [NoSuchElementException] [this]
    (when-not (.hasNext this) (throw (NoSuchElementException.)))
    (set! seq next)
    (RT/first next))

  (method ^:public remove ^void [this]
    (throw (UnsupportedOperationException.))))
