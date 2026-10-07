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
;; /* rich Nov 17, 2007 */
;;
;; Converted from clojure/lang/Agent.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util.concurrent Executor ExecutorService Executors ThreadFactory)
        '(java.util.concurrent.atomic AtomicLong AtomicReference))

(defclass ^:public Agent
  :extends ARef

  (defclass ^:static ActionQueue
    (field ^:public ^:final ^IPersistentStack q)

    (field ^:public ^:final ^Throwable error)

    (field ^:static ^:final ^ActionQueue EMPTY (ActionQueue. PersistentQueue/EMPTY nil))

    (constructor ^:public [this ^IPersistentStack q ^Throwable error]
      (set! (.-q this) q)
      (set! (.-error this) error)))

  (field ^:static ^:final ^Keyword CONTINUE (Keyword/intern nil "continue"))

  (field ^:static ^:final ^Keyword FAIL (Keyword/intern nil "fail"))

  (field ^:volatile state)

  (field ^{:tag (AtomicReference ActionQueue)} aq (AtomicReference. ActionQueue/EMPTY))

  (field ^:volatile ^Keyword errorMode CONTINUE)

  (field ^:volatile ^IFn errorHandler nil)

  (field ^:private ^:static ^:final ^AtomicLong sendThreadPoolCounter (AtomicLong. 0))

  (field ^:private ^:static ^:final ^AtomicLong sendOffThreadPoolCounter (AtomicLong. 0))

  (field ^:public ^:static ^:volatile ^ExecutorService pooledExecutor
    (Executors/newFixedThreadPool (unchecked-add-int 2 (.availableProcessors (Runtime/getRuntime)))
                                  (Agent/createThreadFactory
                                    "arbace-agent-send-pool-%d"
                                    sendThreadPoolCounter)))

  (field ^:public ^:static ^:volatile ^ExecutorService soloExecutor
    (Executors/newCachedThreadPool
      (Agent/createThreadFactory "arbace-agent-send-off-pool-%d" sendOffThreadPoolCounter)))

  (field ^:static ^:final ^{:tag (ThreadLocal IPersistentVector)} nested (ThreadLocal.))

  (method ^:private ^:static createThreadFactory ^ThreadFactory [^:final ^String format
                                                                 ^:final ^AtomicLong threadPoolCounter]
    (anon ThreadFactory []
      (method ^:public newThread ^Thread [this ^Runnable runnable]
        (let [thread (Thread. runnable)]
          (.setName thread
                    (String/format format (new Object/1 [(.getAndIncrement threadPoolCounter)])))
          thread))))

  (method ^:public ^:static shutdown ^void []
    (.shutdown soloExecutor)
    (.shutdown pooledExecutor))

  (defclass ^:static Action
    :implements [Runnable]

    (field ^:final ^Agent agent)

    (field ^:final ^IFn fn)

    (field ^:final ^ISeq args)

    (field ^:final ^Executor exec)

    (constructor ^:public [this ^Agent agent ^IFn fn ^ISeq args ^Executor exec]
      (set! (.-agent this) agent)
      (set! (.-args this) args)
      (set! (.-fn this) fn)
      (set! (.-exec this) exec))

    (method execute ^void [this]
      (try
        (.execute exec this)
        (catch Throwable error
          (when (some? (.-errorHandler agent))
            (try (.invoke (.-errorHandler agent) agent error) (catch Throwable e))))))

    (method ^:static doRun ^void [^Action action]
      (try
        (.set nested PersistentVector/EMPTY)
        (let [^:mutable ^Throwable error nil]
          (try
            (let [oldval (.-state (.-agent action))
                  newval (.applyTo (.-fn action)
                                   (RT/cons (.-state (.-agent action)) (.-args action)))]
              (.setState (.-agent action) newval)
              (.notifyWatches (.-agent action) oldval newval))
            (catch Throwable e (set! error e)))
          (if (nil? error)
              (Agent/releasePendingSends)
              (do
                (.set nested nil)
                (when (some? (.-errorHandler (.-agent action)))
                  (try
                    (.invoke (.-errorHandler (.-agent action)) (.-agent action) error)
                    (catch Throwable e)))
                (when (identical? (.-errorMode (.-agent action)) CONTINUE) (set! error nil))))
          (let [^:mutable popped false
                ^:mutable ^ActionQueue next nil]
            (while (not popped)
              (let [prior (cast ActionQueue (.get (.-aq (.-agent action))))]
                (set! next (ActionQueue. (.pop (.-q prior)) error))
                (set! popped (.compareAndSet (.-aq (.-agent action)) prior next))))
            (when (and (nil? error) (> (.count (.-q next)) 0))
              (.execute (cast Action (.peek (.-q next)))))))
        (finally (.set nested nil))))

    (method ^:public run ^void [this] (Action/doRun this)))

  (constructor ^:public [this state] (this. state nil))

  (constructor ^:public [this state ^IPersistentMap meta]
    (super. meta)
    (.setState this state))

  (method setState ^boolean [this newState]
    (.validate this newState)
    (let [ret (not (identical? state newState))] (set! state newState) ret))

  (method ^:public deref [this] state)

  (method ^:public getError ^Throwable [this]
    (.-error (cast ActionQueue (.get aq))))

  (method ^:public setErrorMode ^void [this ^Keyword k]
    (set! errorMode k))

  (method ^:public getErrorMode ^Keyword [this] errorMode)

  (method ^:public setErrorHandler ^void [this ^IFn f]
    (set! errorHandler f))

  (method ^:public getErrorHandler ^IFn [this] errorHandler)

  (method ^:public ^:synchronized restart [this newState ^boolean clearActions]
    (when (nil? (.getError this)) (throw (Util/runtimeException "Agent does not need a restart")))
    (.validate this newState)
    (set! state newState)
    (if clearActions
        (.set aq ActionQueue/EMPTY)
        (let [^:mutable restarted false
              ^:mutable ^ActionQueue prior nil]
          (while (not restarted)
            (set! prior (cast ActionQueue (.get aq)))
            (set! restarted (.compareAndSet aq prior (ActionQueue. (.-q prior) nil))))
          (when (> (.count (.-q prior)) 0) (.execute (cast Action (.peek (.-q prior)))))))
    newState)

  (method ^:public dispatch [this ^IFn fn ^ISeq args ^Executor exec]
    (let [error (.getError this)]
      (when (some? error) (throw (Util/runtimeException "Agent is failed, needs restart" error)))
      (let [action (Action. this fn args exec)] (Agent/dispatchAction action) this)))

  (method ^:static dispatchAction ^void [^Action action]
    (let [trans (LockingTransaction/getRunning)]
      (cond
        (some? trans) (.enqueue trans action)
        (some? (.get nested)) (.set nested (.cons (cast IPersistentVector (.get nested)) action))
        :else (.enqueue (.-agent action) action))))

  (method enqueue ^void [this ^Action action]
    (let [^:mutable queued false
          ^:mutable ^ActionQueue prior nil]
      (while (not queued)
        (set! prior (cast ActionQueue (.get aq)))
        (set! queued
              (.compareAndSet aq
                              prior
                              (ActionQueue. (cast IPersistentStack (.cons (.-q prior) action))
                                            (.-error prior)))))
      (when (and (== (.count (.-q prior)) 0) (nil? (.-error prior))) (.execute action))))

  (method ^:public getQueueCount ^int [this]
    (.count (.-q (cast ActionQueue (.get aq)))))

  (method ^:public ^:static releasePendingSends ^int []
    (let [sends (cast IPersistentVector (.get nested))]
      (if (nil? sends)
          0
          (do
            (loop [^int i 0]
              (when (< i (.count sends))
                (let [a (cast Action (.valAt sends i))]
                  (.enqueue (.-agent a) a)
                  (recur (unchecked-inc-int i)))))
            (.set nested PersistentVector/EMPTY)
            (.count sends))))))
