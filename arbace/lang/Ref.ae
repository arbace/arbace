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
;; /* rich Jul 25, 2007 */
;;
;; Converted from clojure/lang/Ref.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util.concurrent.atomic AtomicInteger AtomicLong)
        '(java.util.concurrent.locks ReentrantReadWriteLock))

(defclass ^:public Ref
  :extends ARef
  :implements [IFn (Comparable Ref) IRef]

  (method ^:public compareTo ^int [this ^Ref ref]
    (cond (== (.-id this) (.-id ref)) 0 (< (.-id this) (.-id ref)) -1 :else 1))

  (method ^:public getMinHistory ^int [this] minHistory)

  (method ^:public setMinHistory ^Ref [this ^int minHistory]
    (set! (.-minHistory this) minHistory)
    this)

  (method ^:public getMaxHistory ^int [this] maxHistory)

  (method ^:public setMaxHistory ^Ref [this ^int maxHistory]
    (set! (.-maxHistory this) maxHistory)
    this)

  (defclass ^:public ^:static TVal
    (field val)

    (field ^long point)

    (field ^TVal prior)

    (field ^TVal next)

    (constructor [this val ^long point ^TVal prior]
      (set! (.-val this) val)
      (set! (.-point this) point)
      (set! (.-prior this) prior)
      (set! (.-next this) (.-next prior))
      (set! (.-next (.-prior this)) this)
      (set! (.-prior (.-next this)) this))

    (constructor [this val ^long point]
      (set! (.-val this) val)
      (set! (.-point this) point)
      (set! (.-next this) this)
      (set! (.-prior this) this)))

  (field ^TVal tvals)

  (field ^:final ^AtomicInteger faults)

  (field ^:final ^ReentrantReadWriteLock lock)

  (field ^LockingTransaction$Info tinfo)

  (field ^:final ^long id)

  (field ^:volatile ^int minHistory 0)

  (field ^:volatile ^int maxHistory 10)

  (field ^:static ^:final ^AtomicLong ids (AtomicLong.))

  (constructor ^:public [this initVal] (this. initVal nil))

  (constructor ^:public [this initVal ^IPersistentMap meta]
    (super. meta)
    (set! (.-id this) (.getAndIncrement ids))
    (set! (.-faults this) (AtomicInteger.))
    (set! (.-lock this) (ReentrantReadWriteLock.))
    (set! tvals (TVal. initVal 0)))

  (method currentVal [this]
    (try
      (.lock (.readLock lock))
      (if (some? tvals)
          (.-val tvals)
          (throw (IllegalStateException. (java-str (.toString this) " is unbound."))))
      (finally (.unlock (.readLock lock)))))

  (method ^:public deref [this]
    (let [t (LockingTransaction/getRunning)] (if (nil? t) (.currentVal this) (.doGet t this))))

  (method ^:public set [this val]
    (.doSet (LockingTransaction/getEx) this val))

  (method ^:public commute [this ^IFn fn ^ISeq args]
    (.doCommute (LockingTransaction/getEx) this fn args))

  (method ^:public alter [this ^IFn fn ^ISeq args]
    (let [t (LockingTransaction/getEx)]
      (.doSet t this (.applyTo fn (RT/cons (.doGet t this) args)))))

  (method ^:public touch ^void [this]
    (.doEnsure (LockingTransaction/getEx) this))

  (method isBound ^boolean [this]
    (try (.lock (.readLock lock)) (some? tvals) (finally (.unlock (.readLock lock)))))

  (method ^:public trimHistory ^void [this]
    (try
      (.lock (.writeLock lock))
      (when (some? tvals) (set! (.-next tvals) tvals) (set! (.-prior tvals) tvals))
      (finally (.unlock (.writeLock lock)))))

  (method ^:public getHistoryCount ^int [this]
    (try (.lock (.writeLock lock)) (.histCount this) (finally (.unlock (.writeLock lock)))))

  (method histCount ^int [this]
    (if (nil? tvals)
        0
        (let [^:mutable ^int count 0]
          (loop [tv (.-next tvals)]
            (when-not (identical? tv tvals)
              (set! count (unchecked-inc-int count))
              (recur (.-next tv))))
          count)))

  (method ^:public ^:final fn ^IFn [this] (cast IFn (.deref this)))

  (method ^:public call [this] (.invoke this))

  (method ^:public run ^void [this] (.invoke this))

  (method ^:public invoke [this] (.invoke (.fn this)))

  (method ^:public invoke [this arg1] (.invoke (.fn this) arg1))

  (method ^:public invoke [this arg1 arg2] (.invoke (.fn this) arg1 arg2))

  (method ^:public invoke [this arg1 arg2 arg3]
    (.invoke (.fn this) arg1 arg2 arg3))

  (method ^:public invoke [this arg1 arg2 arg3 arg4]
    (.invoke (.fn this) arg1 arg2 arg3 arg4))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14]
    (.invoke (.fn this) arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16
             arg17))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16
             arg17
             arg18))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16
             arg17
             arg18
             arg19))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16
             arg17
             arg18
             arg19
             arg20))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20 & ^Object/1 args]
    (.invoke (.fn this)
             arg1
             arg2
             arg3
             arg4
             arg5
             arg6
             arg7
             arg8
             arg9
             arg10
             arg11
             arg12
             arg13
             arg14
             arg15
             arg16
             arg17
             arg18
             arg19
             arg20
             args))

  (method ^:public applyTo [this ^ISeq arglist]
    (AFn/applyToHelper this arglist)))
