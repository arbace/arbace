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
;; /* rich Jul 26, 2007 */
;;
;; Converted from clojure/lang/LockingTransaction.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util ArrayList HashMap HashSet Map$Entry TreeMap)
        '(java.util.concurrent CountDownLatch TimeUnit)
        '(java.util.concurrent.atomic AtomicInteger AtomicLong))

(defclass ^:public LockingTransaction
  (field ^:public ^:static ^:final ^int RETRY_LIMIT 10000)

  (field ^:public ^:static ^:final ^int LOCK_WAIT_MSECS 100)

  (field ^:public ^:static ^:final ^long BARGE_WAIT_NANOS (unchecked-multiply-int 10 1000000))

  (field ^:static ^:final ^int RUNNING 0)

  (field ^:static ^:final ^int COMMITTING 1)

  (field ^:static ^:final ^int RETRY 2)

  (field ^:static ^:final ^int KILLED 3)

  (field ^:static ^:final ^int COMMITTED 4)

  (field ^:static ^:final ^{:tag (ThreadLocal LockingTransaction)} transaction (ThreadLocal.))

  (defclass ^:static RetryEx
    :extends Error)

  (defclass ^:static AbortException
    :extends Exception)

  (defclass ^:public ^:static Info
    (field ^:final ^AtomicInteger status)

    (field ^:final ^long startPoint)

    (field ^:final ^CountDownLatch latch)

    (constructor ^:public [this ^int status ^long startPoint]
      (set! (.-status this) (AtomicInteger. status))
      (set! (.-startPoint this) startPoint)
      (set! (.-latch this) (CountDownLatch. 1)))

    (method ^:public running ^boolean [this]
      (let [s (.get status)] (or (== s RUNNING) (== s COMMITTING)))))

  (defclass ^:static CFn
    (field ^:final ^IFn fn)

    (field ^:final ^ISeq args)

    (constructor ^:public [this ^IFn fn ^ISeq args]
      (set! (.-fn this) fn)
      (set! (.-args this) args)))

  (field ^:private ^:static ^:final ^AtomicLong lastPoint (AtomicLong.))

  (method getReadPoint ^void [this]
    (set! readPoint (.incrementAndGet lastPoint)))

  (method getCommitPoint ^long [this] (.incrementAndGet lastPoint))

  (method stop ^void [this ^int status]
    (when (some? info)
      (locking info (.set (.-status info) status) (.countDown (.-latch info)))
      (set! info nil)
      (.clear vals)
      (.clear sets)
      (.clear commutes)))

  (field ^Info info)

  (field ^long readPoint)

  (field ^long startPoint)

  (field ^long startTime)

  (field ^:static ^:final ^RetryEx retryex (RetryEx.))

  (field ^:final ^{:tag (ArrayList Agent$Action)} actions (ArrayList.))

  (field ^:final ^{:tag (HashMap Ref Object)} vals (HashMap.))

  (field ^:final ^{:tag (HashSet Ref)} sets (HashSet.))

  (field ^:final ^{:tag (TreeMap Ref (ArrayList CFn))} commutes (TreeMap.))

  (field ^:final ^{:tag (HashSet Ref)} ensures (HashSet.))

  (method tryWriteLock ^void [this ^Ref ref]
    (try
      (when-not (.tryLock (.writeLock (.-lock ref)) LOCK_WAIT_MSECS TimeUnit/MILLISECONDS)
        (throw retryex))
      (catch InterruptedException e (throw retryex))))

  (method lock [this ^Ref ref]
    (.releaseIfEnsured this ref)
    (let [^:mutable unlocked true]
      (try
        (.tryWriteLock this ref)
        (set! unlocked false)
        (when (and (some? (.-tvals ref)) (> (.-point (.-tvals ref)) readPoint)) (throw retryex))
        (let [refinfo (.-tinfo ref)]
          (when (and (and (some? refinfo) (not (identical? refinfo info))) (.running refinfo))
            (when-not (.barge this refinfo)
              (.unlock (.writeLock (.-lock ref)))
              (set! unlocked true)
              (return (.blockAndBail this refinfo))))
          (set! (.-tinfo ref) info)
          (when (some? (.-tvals ref)) (.-val (.-tvals ref))))
        (finally (when-not unlocked (.unlock (.writeLock (.-lock ref))))))))

  (method ^:private blockAndBail [this ^Info refinfo]
    (.stop this RETRY)
    (try
      (.await (.-latch refinfo) LOCK_WAIT_MSECS TimeUnit/MILLISECONDS)
      (catch InterruptedException e))
    (throw retryex))

  (method ^:private releaseIfEnsured ^void [this ^Ref ref]
    (when (.contains ensures ref) (.remove ensures ref) (.unlock (.readLock (.-lock ref)))))

  (method abort :throws [AbortException] ^void [this]
    (.stop this KILLED)
    (throw (AbortException.)))

  (method ^:private bargeTimeElapsed ^boolean [this]
    (> (unchecked-subtract (System/nanoTime) startTime) BARGE_WAIT_NANOS))

  (method ^:private barge ^boolean [this ^Info refinfo]
    (let [^:mutable barged false]
      (when (and (.bargeTimeElapsed this) (< startPoint (.-startPoint refinfo)))
        (set! barged (.compareAndSet (.-status refinfo) RUNNING KILLED))
        (when barged (.countDown (.-latch refinfo))))
      barged))

  (method ^:static getEx ^LockingTransaction []
    (let [t (cast LockingTransaction (.get transaction))]
      (when (or (nil? t) (nil? (.-info t)))
        (throw (IllegalStateException. "No transaction running")))
      t))

  (method ^:public ^:static isRunning ^boolean []
    (some? (LockingTransaction/getRunning)))

  (method ^:static getRunning ^LockingTransaction []
    (let [t (cast LockingTransaction (.get transaction))]
      (when-not (or (nil? t) (nil? (.-info t))) t)))

  (method ^:public ^:static runInTransaction :throws [Exception] [^Callable fn]
    (let [^:mutable t (cast LockingTransaction (.get transaction))
          ^:mutable ^Object ret nil]
      (cond
        (nil? t)
          (do
            (.set transaction (set! t (LockingTransaction.)))
            (try (set! ret (.run t fn)) (finally (.remove transaction))))
        (some? (.-info t)) (set! ret (.call fn))
        :else (set! ret (.run t fn)))
      ret))

  (defclass ^:static Notify
    (field ^:public ^:final ^Ref ref)

    (field ^:public ^:final oldval)

    (field ^:public ^:final newval)

    (constructor [this ^Ref ref oldval newval]
      (set! (.-ref this) ref)
      (set! (.-oldval this) oldval)
      (set! (.-newval this) newval)))

  (method run :throws [Exception] [this ^Callable fn]
    (let [^:mutable done false
          ^:mutable ^Object ret nil
          ^{:tag (ArrayList Ref)} locked (ArrayList.)
          ^{:tag (ArrayList Notify)} notify (ArrayList.)]
      (loop [^int i 0]
        (when (and (not done) (< i RETRY_LIMIT))
          (try
            (.getReadPoint this)
            (when (== i 0) (set! startPoint readPoint) (set! startTime (System/nanoTime)))
            (set! info (Info. RUNNING startPoint))
            (set! ret (.call fn))
            (when (.compareAndSet (.-status info) RUNNING COMMITTING)
              (for-each [^{:tag (Map$Entry Ref (ArrayList CFn))} e (.entrySet commutes)]
                (let [ref (cast Ref (.getKey e))]
                  (when-not (.contains sets ref)
                    (let [wasEnsured (.contains ensures ref)]
                      (.releaseIfEnsured this ref)
                      (.tryWriteLock this ref)
                      (.add locked ref)
                      (when (and (and wasEnsured (some? (.-tvals ref)))
                                 (> (.-point (.-tvals ref)) readPoint))
                        (throw retryex))
                      (let [refinfo (.-tinfo ref)]
                        (when (and (and (some? refinfo) (not (identical? refinfo info)))
                                   (.running refinfo))
                          (when-not (.barge this refinfo) (throw retryex)))
                        (let [val (when (some? (.-tvals ref)) (.-val (.-tvals ref)))]
                          (.put vals ref val)
                          (for-each [^CFn f (cast ArrayList (.getValue e))]
                            (.put vals ref (.applyTo (.-fn f) (RT/cons (.get vals ref) (.-args f)))))))))))
              (for-each [^Ref ref sets] (.tryWriteLock this ref) (.add locked ref))
              (for-each [^{:tag (Map$Entry Ref Object)} e (.entrySet vals)]
                (let [ref (cast Ref (.getKey e))] (.validate ref (.getValidator ref) (.getValue e))))
              (let [commitPoint (.getCommitPoint this)]
                (for-each [^{:tag (Map$Entry Ref Object)} e (.entrySet vals)]
                  (let [ref (cast Ref (.getKey e))
                        oldval (when (some? (.-tvals ref)) (.-val (.-tvals ref)))
                        newval (.getValue e)
                        hcount (.histCount ref)]
                    (cond
                      (nil? (.-tvals ref)) (set! (.-tvals ref) (Ref$TVal. newval commitPoint))
                      (or (and (> (.get (.-faults ref)) 0) (< hcount (.-maxHistory ref)))
                          (< hcount (.-minHistory ref)))
                        (do
                          (set! (.-tvals ref) (Ref$TVal. newval commitPoint (.-tvals ref)))
                          (.set (.-faults ref) 0))
                      :else
                        (do
                          (set! (.-tvals ref) (.-next (.-tvals ref)))
                          (set! (.-val (.-tvals ref)) newval)
                          (set! (.-point (.-tvals ref)) commitPoint)))
                    (when (> (.count (.getWatches ref)) 0)
                      (.add notify (Notify. ref oldval newval)))))
                (set! done true)
                (.set (.-status info) COMMITTED)))
            (catch RetryEx retry)
            (finally
              (loop [^int k (unchecked-subtract-int (.size locked) 1)]
                (when (>= k 0)
                  (.unlock (.writeLock (.-lock (cast Ref (.get locked k)))))
                  (recur (unchecked-dec-int k))))
              (.clear locked)
              (for-each [^Ref r ensures] (.unlock (.readLock (.-lock r))))
              (.clear ensures)
              (.stop this (if done COMMITTED RETRY))
              (try
                (when done
                  (for-each [^Notify n notify] (.notifyWatches (.-ref n) (.-oldval n) (.-newval n)))
                  (for-each [^Agent$Action action actions] (Agent/dispatchAction action)))
                (finally (.clear notify) (.clear actions)))))
          (recur (unchecked-inc-int i))))
      (when-not done
        (throw (Util/runtimeException "Transaction failed after reaching retry limit")))
      ret))

  (method ^:public enqueue ^void [this ^Agent$Action action]
    (.add actions action))

  (method doGet [this ^Ref ref]
    (when-not (.running info) (throw retryex))
    (if (.containsKey vals ref)
        (.get vals ref)
        (do
          (try
            (.lock (.readLock (.-lock ref)))
            (when (nil? (.-tvals ref))
              (throw (IllegalStateException. (java-str (.toString ref) " is unbound."))))
            (let [^:mutable ver (.-tvals ref)]
              (loop []
                (when (<= (.-point ver) readPoint) (return (.-val ver)))
                (when-not (identical? (set! ver (.-prior ver)) (.-tvals ref)) (recur))))
            (finally (.unlock (.readLock (.-lock ref)))))
          (.incrementAndGet (.-faults ref))
          (throw retryex))))

  (method doSet [this ^Ref ref val]
    (when-not (.running info) (throw retryex))
    (when (.containsKey commutes ref) (throw (IllegalStateException. "Can't set after commute")))
    (when-not (.contains sets ref) (.add sets ref) (.lock this ref))
    (.put vals ref val)
    val)

  (method doEnsure ^void [this ^Ref ref]
    (when-not (.running info) (throw retryex))
    (when-not (.contains ensures ref)
      (.lock (.readLock (.-lock ref)))
      (when (and (some? (.-tvals ref)) (> (.-point (.-tvals ref)) readPoint))
        (.unlock (.readLock (.-lock ref)))
        (throw retryex))
      (let [refinfo (.-tinfo ref)]
        (if (and (some? refinfo) (.running refinfo))
            (do
              (.unlock (.readLock (.-lock ref)))
              (when-not (identical? refinfo info) (.blockAndBail this refinfo)))
            (.add ensures ref)))))

  (method doCommute [this ^Ref ref ^IFn fn ^ISeq args]
    (when-not (.running info) (throw retryex))
    (when-not (.containsKey vals ref)
      (let [^:mutable ^Object val nil]
        (try
          (.lock (.readLock (.-lock ref)))
          (set! val (when (some? (.-tvals ref)) (.-val (.-tvals ref))))
          (finally (.unlock (.readLock (.-lock ref)))))
        (.put vals ref val)))
    (let [^:mutable ^{:tag (ArrayList CFn)} fns (cast ArrayList (.get commutes ref))]
      (when (nil? fns) (.put commutes ref (set! fns (ArrayList.))))
      (.add fns (CFn. fn args))
      (let [ret (.applyTo fn (RT/cons (.get vals ref) args))] (.put vals ref ret) ret))))
