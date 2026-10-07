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
;; /* rich Mar 1, 2008 */
;;
;; Converted from clojure/lang/AMapEntry.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:abstract AMapEntry
  :extends APersistentVector
  :implements [IMapEntry]

  (field ^:private ^:static ^:final ^long serialVersionUID -5007980429903443802)

  (method ^:public nth [this ^int i]
    (cond (== i 0) (.key this) (== i 1) (.val this) :else (throw (IndexOutOfBoundsException.))))

  (method ^:private asVector ^IPersistentVector [this]
    (^[Object/1] LazilyPersistentVector/createOwning (.key this) (.val this)))

  (method ^:public assocN ^IPersistentVector [this ^int i val]
    (.assocN (.asVector this) i val))

  (method ^:public count ^int [this] 2)

  (method ^:public seq ^ISeq [this] (.seq (.asVector this)))

  (method ^:public cons ^IPersistentVector [this o]
    (.cons (.asVector this) o))

  (method ^:public empty ^IPersistentCollection [this] nil)

  (method ^:public pop ^IPersistentStack [this]
    (LazilyPersistentVector/createOwning (.key this)))

  (method ^:public setValue [this value]
    (throw (UnsupportedOperationException.))))
