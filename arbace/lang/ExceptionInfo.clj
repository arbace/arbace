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
;; Converted from clojure/lang/ExceptionInfo.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ExceptionInfo
  :extends RuntimeException
  :implements [IExceptionInfo]

  (field ^:private ^:static ^:final ^long serialVersionUID -1073473305916521986)

  (field ^:public ^:final ^IPersistentMap data)

  (constructor ^:public [this ^String s ^IPersistentMap data]
    (this. s data nil))

  (constructor ^:public [this ^String s ^IPersistentMap data ^Throwable throwable]
    (super. s throwable)
    (set! (.-data this) (if (nil? data) PersistentArrayMap/EMPTY data)))

  (method ^:public getData ^IPersistentMap [this] data)

  (method ^:public toString ^String [this]
    (java-str "arbace.lang.ExceptionInfo: " (.getMessage this) " " (.toString data))))
