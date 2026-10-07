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
;; Converted from clojure/lang/TaggedLiteral.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public TaggedLiteral
  :implements [ILookup]

  (field ^:public ^:static ^:final ^Keyword TAG_KW (Keyword/intern "tag"))

  (field ^:public ^:static ^:final ^Keyword FORM_KW (Keyword/intern "form"))

  (field ^:public ^:final ^Symbol tag)

  (field ^:public ^:final form)

  (method ^:public ^:static create ^TaggedLiteral [^Symbol tag form]
    (TaggedLiteral. tag form))

  (constructor ^:private [this ^Symbol tag form]
    (set! (.-tag this) tag)
    (set! (.-form this) form))

  (method ^:public valAt [this key] (.valAt this key nil))

  (method ^:public valAt [this key notFound]
    (cond (.equals FORM_KW key) (.-form this) (.equals TAG_KW key) (.-tag this) :else notFound))

  (method ^:public equals ^boolean [this o]
    (cond
      (identical? this o) true
      (or (nil? o) (not (identical? (.getClass this) (.getClass o)))) false
      :else
        (let [that (cast TaggedLiteral o)]
          (cond
            (if (some? tag) (not (.equals tag (.-tag that))) (some? (.-tag that))) false
            (if (some? form) (not (.equals form (.-form that))) (some? (.-form that))) false
            :else true))))

  (method ^:public hashCode ^int [this]
    (let [^:mutable result (Util/hash tag)]
      (set! result (unchecked-add-int (unchecked-multiply-int 31 result) (Util/hash form)))
      result)))
