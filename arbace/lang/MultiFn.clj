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
;; /* rich Sep 13, 2007 */
;;
;; Converted from clojure/lang/MultiFn.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Map$Entry)
        '(java.util.concurrent.locks ReentrantReadWriteLock))

(defclass ^:public MultiFn
  :extends AFn

  (field ^:public ^:final ^IFn dispatchFn)

  (field ^:public ^:final defaultDispatchVal)

  (field ^:public ^:final ^IRef hierarchy)

  (field ^:final ^String name)

  (field ^:final ^ReentrantReadWriteLock rw)

  (field ^:volatile ^IPersistentMap methodTable)

  (field ^:volatile ^IPersistentMap preferTable)

  (field ^:volatile ^IPersistentMap methodCache)

  (field ^:volatile cachedHierarchy)

  (field ^:static ^:final ^Var assoc (RT/var "arbace.core" "assoc"))

  (field ^:static ^:final ^Var dissoc (RT/var "arbace.core" "dissoc"))

  (field ^:static ^:final ^Var isa (RT/var "arbace.core" "isa?"))

  (field ^:static ^:final ^Var parents (RT/var "arbace.core" "parents"))

  (constructor ^:public [this ^String name ^IFn dispatchFn defaultDispatchVal ^IRef hierarchy]
    (set! (.-rw this) (ReentrantReadWriteLock.))
    (set! (.-name this) name)
    (set! (.-dispatchFn this) dispatchFn)
    (set! (.-defaultDispatchVal this) defaultDispatchVal)
    (set! (.-methodTable this) PersistentHashMap/EMPTY)
    (set! (.-methodCache this) (.getMethodTable this))
    (set! (.-preferTable this) PersistentHashMap/EMPTY)
    (set! (.-hierarchy this) hierarchy)
    (set! cachedHierarchy nil))

  (method ^:public reset ^MultiFn [this]
    (.lock (.writeLock rw))
    (try
      (set! methodTable (set! methodCache (set! preferTable PersistentHashMap/EMPTY)))
      (set! cachedHierarchy nil)
      this
      (finally (.unlock (.writeLock rw)))))

  (method ^:public addMethod ^MultiFn [this dispatchVal ^IFn method]
    (.lock (.writeLock rw))
    (try
      (set! methodTable (.assoc (.getMethodTable this) dispatchVal method))
      (.resetCache this)
      this
      (finally (.unlock (.writeLock rw)))))

  (method ^:public removeMethod ^MultiFn [this dispatchVal]
    (.lock (.writeLock rw))
    (try
      (set! methodTable (.without (.getMethodTable this) dispatchVal))
      (.resetCache this)
      this
      (finally (.unlock (.writeLock rw)))))

  (method ^:public preferMethod ^MultiFn [this dispatchValX dispatchValY]
    (.lock (.writeLock rw))
    (try
      (when (.prefers this (.deref hierarchy) dispatchValY dispatchValX)
        (throw (IllegalStateException.
                 (String/format
                   "Preference conflict in multimethod '%s': %s is already preferred to %s"
                   (new Object/1 [name dispatchValY dispatchValX])))))
      (set! preferTable
            (.assoc (.getPreferTable this)
                    dispatchValX
                    (RT/conj (cast IPersistentCollection
                                   (RT/get (.getPreferTable this)
                                           dispatchValX
                                           PersistentHashSet/EMPTY))
                             dispatchValY)))
      (.resetCache this)
      this
      (finally (.unlock (.writeLock rw)))))

  (method ^:private prefers ^boolean [this hierarchy x y]
    (let [xprefs (cast IPersistentSet (.valAt (.getPreferTable this) x))]
      (if (and (some? xprefs) (.contains xprefs y))
          true
          (do
            (loop [ps (RT/seq (.invoke parents hierarchy y))]
              (if (some? ps)
                  (if (.prefers this hierarchy x (.first ps)) (return true) (recur (.next ps)))
                  nil))
            (loop [ps (RT/seq (.invoke parents hierarchy x))]
              (if (some? ps)
                  (if (.prefers this hierarchy (.first ps) y) (return true) (recur (.next ps)))
                  nil))
            false))))

  (method ^:private isA ^boolean [this hierarchy x y]
    (RT/booleanCast (.invoke isa hierarchy x y)))

  (method ^:private dominates ^boolean [this hierarchy x y]
    (or (.prefers this hierarchy x y) (.isA this hierarchy x y)))

  (method ^:private resetCache ^IPersistentMap [this]
    (.lock (.writeLock rw))
    (try
      (set! methodCache (.getMethodTable this))
      (set! cachedHierarchy (.deref hierarchy))
      methodCache
      (finally (.unlock (.writeLock rw)))))

  (method ^:public getMethod ^IFn [this dispatchVal]
    (when-not (identical? cachedHierarchy (.deref hierarchy)) (.resetCache this))
    (let [targetFn (cast IFn (.valAt methodCache dispatchVal))]
      (if (some? targetFn) targetFn (.findAndCacheBestMethod this dispatchVal))))

  (method ^:private getFn ^IFn [this dispatchVal]
    (let [targetFn (.getMethod this dispatchVal)]
      (when (nil? targetFn)
        (throw (IllegalArgumentException.
                 (String/format "No method in multimethod '%s' for dispatch value: %s"
                                (new Object/1 [name dispatchVal])))))
      targetFn))

  (method ^:private findAndCacheBestMethod ^IFn [this dispatchVal]
    (.lock (.readLock rw))
    (let [^:mutable ^Object bestValue nil
          mt methodTable
          pt preferTable
          ch cachedHierarchy]
      (try
        (let [^:mutable ^Map$Entry bestEntry nil]
          (for-each [o (.getMethodTable this)]
            (let [e (cast Map$Entry o)]
              (when (.isA this ch dispatchVal (.getKey e))
                (when (or (nil? bestEntry) (.dominates this ch (.getKey e) (.getKey bestEntry)))
                  (set! bestEntry e))
                (when-not (.dominates this ch (.getKey bestEntry) (.getKey e))
                  (throw
                    (IllegalArgumentException.
                      (String/format
                        "Multiple methods in multimethod '%s' match dispatch value: %s -> %s and %s, and neither is preferred"
                        (new Object/1 [name dispatchVal (.getKey e) (.getKey bestEntry)]))))))))
          (if (nil? bestEntry)
              (do
                (set! bestValue (.valAt methodTable defaultDispatchVal))
                (when (nil? bestValue) (return nil)))
              (set! bestValue (.getValue bestEntry))))
        (finally (.unlock (.readLock rw))))
      (.lock (.writeLock rw))
      (try
        (if (and (and (and (identical? mt methodTable) (identical? pt preferTable))
                      (identical? ch cachedHierarchy))
                 (identical? cachedHierarchy (.deref hierarchy)))
            (do (set! methodCache (.assoc methodCache dispatchVal bestValue)) (cast IFn bestValue))
            (do (.resetCache this) (.findAndCacheBestMethod this dispatchVal)))
        (finally (.unlock (.writeLock rw))))))

  (method ^:public invoke [this]
    (.invoke (.getFn this (.invoke dispatchFn))))

  (method ^:public invoke [this ^:mutable arg1]
    (.invoke (.getFn this (.invoke dispatchFn arg1)) (Util/ret1 arg1 (set! arg1 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6 arg7))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10]
    (.invoke (.getFn this (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11]
    (.invoke (.getFn this
                     (.invoke dispatchFn arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
                              arg12))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
                              arg13))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
                              arg14))
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))
             (Util/ret1 arg17 (set! arg17 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))
             (Util/ret1 arg17 (set! arg17 nil))
             (Util/ret1 arg18 (set! arg18 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))
             (Util/ret1 arg17 (set! arg17 nil))
             (Util/ret1 arg18 (set! arg18 nil))
             (Util/ret1 arg19 (set! arg19 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19 ^:mutable arg20]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))
             (Util/ret1 arg17 (set! arg17 nil))
             (Util/ret1 arg18 (set! arg18 nil))
             (Util/ret1 arg19 (set! arg19 nil))
             (Util/ret1 arg20 (set! arg20 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19 ^:mutable arg20 &
                           ^Object/1 args]
    (.invoke (.getFn this
                     (.invoke dispatchFn
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
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))
             (Util/ret1 arg8 (set! arg8 nil))
             (Util/ret1 arg9 (set! arg9 nil))
             (Util/ret1 arg10 (set! arg10 nil))
             (Util/ret1 arg11 (set! arg11 nil))
             (Util/ret1 arg12 (set! arg12 nil))
             (Util/ret1 arg13 (set! arg13 nil))
             (Util/ret1 arg14 (set! arg14 nil))
             (Util/ret1 arg15 (set! arg15 nil))
             (Util/ret1 arg16 (set! arg16 nil))
             (Util/ret1 arg17 (set! arg17 nil))
             (Util/ret1 arg18 (set! arg18 nil))
             (Util/ret1 arg19 (set! arg19 nil))
             (Util/ret1 arg20 (set! arg20 nil))
             args))

  (method ^:public getMethodTable ^IPersistentMap [this] methodTable)

  (method ^:public getPreferTable ^IPersistentMap [this] preferTable))
