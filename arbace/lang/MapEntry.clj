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
;; Converted from clojure/lang/MapEntry.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public MapEntry
  :extends AMapEntry

  (field ^:private ^:static ^:final ^long serialVersionUID -3752414622414469244)

  (field ^:final _key)

  (field ^:final _val)

  (method ^:public ^:static create ^MapEntry [key val]
    (MapEntry. key val))

  (constructor ^:public [this key val]
    (set! (.-_key this) key)
    (set! (.-_val this) val))

  (method ^:public key [this] _key)

  (method ^:public val [this] _val)

  (method ^:public getKey [this] (.key this))

  (method ^:public getValue [this] (.val this)))
