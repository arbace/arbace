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
;; /* rich Mar 25, 2006 3:44:58 PM */
;;
;; Converted from clojure/lang/Obj.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable))

(defclass ^:public ^:abstract Obj
  :implements [IObj Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID 802029099426284526)

  (field ^:final ^IPersistentMap _meta)

  (constructor ^:public [this ^IPersistentMap meta]
    (set! (.-_meta this) meta))

  (constructor ^:public [this] (set! _meta nil))

  (method ^:public ^:final meta ^IPersistentMap [this] _meta)

  (method ^:public ^:abstract withMeta ^Obj [this ^IPersistentMap meta]))
