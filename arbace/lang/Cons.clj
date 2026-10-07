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
;; /* rich Mar 25, 2006 11:01:29 AM */
;;
;; Converted from clojure/lang/Cons.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable))

(defclass ^:public ^:final Cons
  :extends ASeq
  :implements [Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID 6682587018567831263)

  (field ^:private ^:final _first)

  (field ^:private ^:final ^ISeq _more)

  (constructor ^:public [this first ^ISeq _more]
    (set! (.-_first this) first)
    (set! (.-_more this) _more))

  (constructor ^:public [this ^IPersistentMap meta _first ^ISeq _more]
    (super. meta)
    (set! (.-_first this) _first)
    (set! (.-_more this) _more))

  (method ^:public first [this] _first)

  (method ^:public next ^ISeq [this] (.seq (.more this)))

  (method ^:public more ^ISeq [this]
    (if (nil? _more) PersistentList/EMPTY _more))

  (method ^:public count ^int [this]
    (unchecked-add-int 1 (RT/count _more)))

  (method ^:public withMeta ^Cons [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (Cons. meta _first _more))))
