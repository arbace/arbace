;; /**
;;  * Copyright (c) Rich Hickey. All rights reserved.
;;  * The use and distribution terms for this software are covered by the
;;  * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  * which can be found in the file epl-v10.html at the root of this distribution.
;;  * By using this software in any fashion, you are agreeing to be bound by
;;  * the terms of this license.
;;  * You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich 7/16/15 */
;; // proposed by Zach Tellman
;;
;; Converted from clojure/lang/Tuple.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public Tuple
  (field ^:static ^:final ^int MAX_SIZE 6)

  (method ^:public ^:static create ^IPersistentVector []
    PersistentVector/EMPTY)

  (method ^:public ^:static create ^IPersistentVector [v0] (RT/vector v0))

  (method ^:public ^:static create ^IPersistentVector [v0 v1]
    (^[Object/1] RT/vector v0 v1))

  (method ^:public ^:static create ^IPersistentVector [v0 v1 v2]
    (^[Object/1] RT/vector v0 v1 v2))

  (method ^:public ^:static create ^IPersistentVector [v0 v1 v2 v3]
    (^[Object/1] RT/vector v0 v1 v2 v3))

  (method ^:public ^:static create ^IPersistentVector [v0 v1 v2 v3 v4]
    (^[Object/1] RT/vector v0 v1 v2 v3 v4))

  (method ^:public ^:static create ^IPersistentVector [v0 v1 v2 v3 v4 v5]
    (^[Object/1] RT/vector v0 v1 v2 v3 v4 v5)))
