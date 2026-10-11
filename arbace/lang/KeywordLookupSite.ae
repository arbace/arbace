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
;; /* rich Nov 2, 2009 */
;;
;; Converted from clojure/lang/KeywordLookupSite.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:final KeywordLookupSite
  :implements [ILookupSite ILookupThunk]

  (field ^:final ^Keyword k)

  (constructor ^:public [this ^Keyword k] (set! (.-k this) k))

  (method ^:public fault ^ILookupThunk [this target]
    (cond
      (instance? IKeywordLookup target) (.install this target)
      (instance? ILookup target) (.ilookupThunk this (.getClass target))
      :else this))

  (method ^:public get [this target]
    (if (or (instance? IKeywordLookup target) (instance? ILookup target)) this (RT/get target k)))

  (method ^:private ilookupThunk ^ILookupThunk [this ^:final ^Class c]
    (anon ILookupThunk []
      (method ^:public get [this target]
        (if (and (some? target) (identical? (.getClass target) c))
            (.valAt (cast ILookup target) k)
            this))))

  (method ^:private install ^ILookupThunk [this target]
    (let [t (.getLookupThunk (cast IKeywordLookup target) k)]
      (if (some? t) t (.ilookupThunk this (.getClass target))))))
