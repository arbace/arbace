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
;; /* rich Jan 1, 2009 */
;;
;; Converted from clojure/lang/ARef.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Map$Entry))

(defclass ^:public ^:abstract ARef
  :extends AReference
  :implements [IRef]

  (field ^:protected ^:volatile ^IFn validator nil)

  (field ^:private ^:volatile ^IPersistentMap watches PersistentHashMap/EMPTY)

  (constructor ^:public [this] (super.))

  (constructor ^:public [this ^IPersistentMap meta] (super. meta))

  (method validate ^void [this ^IFn vf val]
    (try
      (when (and (some? vf) (not (RT/booleanCast (.invoke vf val))))
        (throw (IllegalStateException. "Invalid reference state")))
      (catch RuntimeException re (throw re))
      (catch Exception e (throw (IllegalStateException. "Invalid reference state" e)))))

  (method validate ^void [this val] (.validate this validator val))

  (method ^:public setValidator ^void [this ^IFn vf]
    (.validate this vf (.deref this))
    (set! validator vf))

  (method ^:public getValidator ^IFn [this] validator)

  (method ^:public getWatches ^IPersistentMap [this] watches)

  (method ^:public ^:synchronized addWatch ^IRef [this key ^IFn callback]
    (set! watches (.assoc watches key callback))
    this)

  (method ^:public ^:synchronized removeWatch ^IRef [this key]
    (set! watches (.without watches key))
    this)

  (method ^:public notifyWatches ^void [this oldval newval]
    (let [ws watches]
      (when (> (.count ws) 0)
        (loop [s (.seq ws)]
          (when (some? s)
            (let [e (cast Map$Entry (.first s))
                  fn (cast IFn (.getValue e))]
              (if (some? fn)
                  (do (.invoke fn (.getKey e) this oldval newval) (recur (.next s)))
                  (recur (.next s))))))))))
