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
;; /* rich Jun 28, 2007 */
;;
;; Converted from clojure/lang/Delay.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util.concurrent.locks Lock ReentrantLock))

(defclass ^:public Delay
  :implements [IDeref IPending]

  (field val)

  (field ^Throwable exception)

  (field ^IFn fn)

  (field ^:volatile ^Lock lock)

  (constructor ^:public [this ^IFn f]
    (set! fn f)
    (set! val nil)
    (set! exception nil)
    (set! lock (ReentrantLock.)))

  (method ^:public ^:static force [x]
    (if (instance? Delay x) (.deref (cast Delay x)) x))

  (method ^:private realize ^void [this]
    (let [l lock]
      (when (some? l)
        (.lock l)
        (try
          (when (some? fn)
            (try (set! val (.invoke fn)) (catch Throwable t (set! exception t)))
            (set! fn nil)
            (set! lock nil))
          (finally (.unlock l))))))

  (method ^:public deref [this]
    (when (some? lock) (.realize this))
    (when (some? exception) (throw (Util/sneakyThrow exception)))
    val)

  (method ^:public isRealized ^boolean [this] (nil? lock)))
