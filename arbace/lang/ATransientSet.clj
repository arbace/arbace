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
;; /* rich Mar 3, 2008 */
;;
;; Converted from clojure/lang/ATransientSet.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:abstract ATransientSet
  :extends AFn
  :implements [ITransientSet]

  (field ^:volatile ^ITransientMap impl)

  (constructor [this ^ITransientMap impl] (set! (.-impl this) impl))

  (method ^:public count ^int [this] (.count impl))

  (method ^:public conj ^ITransientSet [this val]
    (let [m (.assoc impl val val)] (when-not (identical? m impl) (set! (.-impl this) m)) this))

  (method ^:public contains ^boolean [this key]
    (not (identical? this (.valAt impl key this))))

  (method ^:public disjoin ^ITransientSet [this key]
    (let [m (.without impl key)] (when-not (identical? m impl) (set! (.-impl this) m)) this))

  (method ^:public get [this key] (.valAt impl key))

  (method ^:public invoke [this key notFound] (.valAt impl key notFound))

  (method ^:public invoke [this key] (.valAt impl key))

  (method ^:public valAt [this key] (.valAt impl key))

  (method ^:public valAt [this key notFound] (.valAt impl key notFound)))
