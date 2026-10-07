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
;; Converted from clojure/lang/Atom.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util.concurrent.atomic AtomicReference))

(defclass ^:public ^:final Atom
  :extends ARef
  :implements [IAtom2]

  (field ^:final ^AtomicReference state)

  (constructor ^:public [this state]
    (set! (.-state this) (AtomicReference. state)))

  (constructor ^:public [this state ^IPersistentMap meta]
    (super. meta)
    (set! (.-state this) (AtomicReference. state)))

  (method ^:public deref [this] (.get state))

  (method ^:public swap [this ^IFn f]
    (while true
      (let [v (.deref this)
            newv (.invoke f v)]
        (.validate this newv)
        (when (.compareAndSet state v newv) (.notifyWatches this v newv) (return newv)))))

  (method ^:public swap [this ^IFn f arg]
    (while true
      (let [v (.deref this)
            newv (.invoke f v arg)]
        (.validate this newv)
        (when (.compareAndSet state v newv) (.notifyWatches this v newv) (return newv)))))

  (method ^:public swap [this ^IFn f arg1 arg2]
    (while true
      (let [v (.deref this)
            newv (.invoke f v arg1 arg2)]
        (.validate this newv)
        (when (.compareAndSet state v newv) (.notifyWatches this v newv) (return newv)))))

  (method ^:public swap [this ^IFn f x y ^ISeq args]
    (while true
      (let [v (.deref this)
            newv (.applyTo f (RT/listStar v x y args))]
        (.validate this newv)
        (when (.compareAndSet state v newv) (.notifyWatches this v newv) (return newv)))))

  (method ^:public swapVals ^IPersistentVector [this ^IFn f]
    (while true
      (let [oldv (.deref this)
            newv (.invoke f oldv)]
        (.validate this newv)
        (when (.compareAndSet state oldv newv)
          (.notifyWatches this oldv newv)
          (return (^[Object/1] LazilyPersistentVector/createOwning oldv newv))))))

  (method ^:public swapVals ^IPersistentVector [this ^IFn f arg]
    (while true
      (let [oldv (.deref this)
            newv (.invoke f oldv arg)]
        (.validate this newv)
        (when (.compareAndSet state oldv newv)
          (.notifyWatches this oldv newv)
          (return (^[Object/1] LazilyPersistentVector/createOwning oldv newv))))))

  (method ^:public swapVals ^IPersistentVector [this ^IFn f arg1 arg2]
    (while true
      (let [oldv (.deref this)
            newv (.invoke f oldv arg1 arg2)]
        (.validate this newv)
        (when (.compareAndSet state oldv newv)
          (.notifyWatches this oldv newv)
          (return (^[Object/1] LazilyPersistentVector/createOwning oldv newv))))))

  (method ^:public swapVals ^IPersistentVector [this ^IFn f x y ^ISeq args]
    (while true
      (let [oldv (.deref this)
            newv (.applyTo f (RT/listStar oldv x y args))]
        (.validate this newv)
        (when (.compareAndSet state oldv newv)
          (.notifyWatches this oldv newv)
          (return (^[Object/1] LazilyPersistentVector/createOwning oldv newv))))))

  (method ^:public compareAndSet ^boolean [this oldv newv]
    (.validate this newv)
    (let [ret (.compareAndSet state oldv newv)] (when ret (.notifyWatches this oldv newv)) ret))

  (method ^:public reset [this newval]
    (let [oldval (.get state)]
      (.validate this newval)
      (.set state newval)
      (.notifyWatches this oldval newval)
      newval))

  (method ^:public resetVals ^IPersistentVector [this newv]
    (.validate this newv)
    (while true
      (let [oldv (.deref this)]
        (when (.compareAndSet state oldv newv)
          (.notifyWatches this oldv newv)
          (return (^[Object/1] LazilyPersistentVector/createOwning oldv newv)))))))
