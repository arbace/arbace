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
;; /* rich Jul 31, 2007 */
;;
;; Converted from clojure/lang/Var.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io ObjectStreamException Serializable)
        '(java.util.concurrent.atomic AtomicBoolean))

(defclass ^:public ^:final Var
  :extends ARef
  :implements [IFn IRef Settable Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID 8368961370796295279)

  (defclass ^:static TBox
    (field ^:volatile val)

    (field ^:final ^Thread thread)

    (constructor ^:public [this ^Thread t val]
      (set! (.-thread this) t)
      (set! (.-val this) val)))

  (defclass ^:public ^:static Unbound
    :extends AFn

    (field ^:public ^:final ^Var v)

    (constructor ^:public [this ^Var v] (set! (.-v this) v))

    (method ^:public toString ^String [this] (java-str "Unbound: " v))

    (method ^:public throwArity [this ^int n]
      (throw (IllegalStateException. (java-str "Attempting to call unbound fn: " v)))))

  (defclass ^:static Frame
    (field ^:static ^:final ^Frame TOP (Frame. PersistentHashMap/EMPTY nil))

    (field ^Associative bindings)

    (field ^Frame prev)

    (constructor ^:public [this ^Associative bindings ^Frame prev]
      (set! (.-bindings this) bindings)
      (set! (.-prev this) prev))

    (method ^:protected clone [this] (Frame. (.-bindings this) nil)))

  (field ^:static ^:final ^{:tag (ThreadLocal Frame)} dvals
    (anon (ThreadLocal Frame) []
      (method ^:protected initialValue ^Frame [this] Frame/TOP)))

  (field ^:public ^:static ^:volatile ^int rev 0)

  (field ^:static ^Keyword privateKey (Keyword/intern nil "private"))

  (field ^:static ^IPersistentMap privateMeta
    (PersistentArrayMap. (new Object/1 [privateKey Boolean/TRUE])))

  (field ^:static ^Keyword macroKey (Keyword/intern nil "macro"))

  (field ^:static ^Keyword nameKey (Keyword/intern nil "name"))

  (field ^:static ^Keyword nsKey (Keyword/intern nil "ns"))

  (field ^:volatile root)

  (field ^:volatile ^boolean dynamic false)

  (field ^:final ^:transient ^AtomicBoolean threadBound)

  (field ^:public ^:final ^Symbol sym)

  (field ^:public ^:final ^Namespace ns)

  (method ^:public ^:static getThreadBindingFrame [] (.get dvals))

  (method ^:public ^:static cloneThreadBindingFrame []
    (.clone (cast Frame (.get dvals))))

  (method ^:public ^:static resetThreadBindingFrame ^void [frame]
    (.set dvals (cast Frame frame)))

  (method ^:public setDynamic ^Var [this]
    (set! (.-dynamic this) true)
    this)

  (method ^:public setDynamic ^Var [this ^boolean b]
    (set! (.-dynamic this) b)
    this)

  (method ^:public ^:final isDynamic ^boolean [this] dynamic)

  (method ^:public ^:static intern ^Var [^Namespace ns ^Symbol sym root]
    (Var/intern ns sym root true))

  (method ^:public ^:static intern ^Var [^Namespace ns ^Symbol sym root ^boolean replaceRoot]
    (let [dvout (.intern ns sym)]
      (when (or (not (.hasRoot dvout)) replaceRoot) (.bindRoot dvout root))
      dvout))

  (method ^:public toSymbol ^Symbol [this]
    (Symbol/create (when (some? ns) (.-name (.-name ns))) (.-name sym)))

  (method ^:public toString ^String [this]
    (if (some? ns)
        (java-str "#'" (.-name ns) "/" sym)
        (java-str "#<Var: " (if (some? sym) (.toString sym) "--unnamed--") ">")))

  (method ^:public ^:static find ^Var [^Symbol nsQualifiedSym]
    (when (nil? (.-ns nsQualifiedSym))
      (throw (IllegalArgumentException. "Symbol must be namespace-qualified")))
    (let [ns (Namespace/find (Symbol/intern (.-ns nsQualifiedSym)))]
      (when (nil? ns)
        (throw (IllegalArgumentException. (java-str "No such namespace: " (.-ns nsQualifiedSym)))))
      (.findInternedVar ns (Symbol/intern (.-name nsQualifiedSym)))))

  (method ^:public ^:static intern ^Var [^Symbol nsName ^Symbol sym]
    (let [ns (Namespace/findOrCreate nsName)] (Var/intern ns sym)))

  (method ^:public ^:static internPrivate ^Var [^String nsName ^String sym]
    (let [ns (Namespace/findOrCreate (Symbol/intern nsName))
          ret (Var/intern ns (Symbol/intern sym))]
      (.setMeta ret privateMeta)
      ret))

  (method ^:public ^:static intern ^Var [^Namespace ns ^Symbol sym]
    (.intern ns sym))

  (method ^:public ^:static create ^Var [] (Var. nil nil))

  (method ^:public ^:static create ^Var [root] (Var. nil nil root))

  (constructor [this ^Namespace ns ^Symbol sym]
    (set! (.-ns this) ns)
    (set! (.-sym this) sym)
    (set! (.-threadBound this) (AtomicBoolean. false))
    (set! (.-root this) (Unbound. this))
    (.setMeta this PersistentHashMap/EMPTY))

  (constructor [this ^Namespace ns ^Symbol sym root]
    (this. ns sym)
    (set! (.-root this) root)
    (set! rev (unchecked-inc-int rev)))

  (method ^:public isBound ^boolean [this]
    (or (.hasRoot this)
        (and (.get threadBound) (.containsKey (.-bindings (cast Frame (.get dvals))) this))))

  (method ^:public ^:final get [this]
    (if (not (.get threadBound)) root (.deref this)))

  (method ^:public ^:final deref [this]
    (let [b (.getThreadBinding this)] (if (some? b) (.-val b) root)))

  (method ^:public setValidator ^void [this ^IFn vf]
    (when (.hasRoot this) (.validate this vf root))
    (set! (.-validator this) vf))

  (method ^:public alter [this ^IFn fn ^ISeq args]
    (.set this (.applyTo fn (RT/cons (.deref this) args)))
    this)

  (method ^:public set [this val]
    (.validate this (.getValidator this) val)
    (let [b (.getThreadBinding this)]
      (if (some? b)
          (do
            (when-not (identical? (Thread/currentThread) (.-thread b))
              (throw
                (IllegalStateException.
                  (String/format "Can't set!: %s from non-binding thread" (new Object/1 [sym])))))
            (set! (.-val b) val))
          (throw (IllegalStateException.
                   (String/format "Can't change/establish root binding of: %s with set"
                                  (new Object/1 [sym])))))))

  (method ^:public doSet [this val] (.set this val))

  (method ^:public doReset [this val] (.bindRoot this val) val)

  (method ^:public setMeta ^void [this ^IPersistentMap m]
    (.resetMeta this (.assoc (.assoc m nameKey sym) nsKey ns)))

  (method ^:public setMacro ^void [this]
    (.alterMeta this assoc (RT/list macroKey RT/T)))

  (method ^:public isMacro ^boolean [this]
    (RT/booleanCast (.valAt (.meta this) macroKey)))

  (method ^:public isPublic ^boolean [this]
    (not (RT/booleanCast (.valAt (.meta this) privateKey))))

  (method ^:public ^:final getRawRoot [this] root)

  (method ^:public getTag [this] (.valAt (.meta this) RT/TAG_KEY))

  (method ^:public setTag ^void [this ^Symbol tag]
    (.alterMeta this assoc (RT/list RT/TAG_KEY tag)))

  (method ^:public ^:final hasRoot ^boolean [this]
    (not (instance? Unbound root)))

  (method ^:public ^:synchronized bindRoot ^void [this root]
    (.validate this (.getValidator this) root)
    (let [oldroot (.-root this)]
      (set! (.-root this) root)
      (set! rev (unchecked-inc-int rev))
      (.alterMeta this dissoc (RT/list macroKey))
      (.notifyWatches this oldroot (.-root this))))

  (method ^:synchronized swapRoot ^void [this root]
    (.validate this (.getValidator this) root)
    (let [oldroot (.-root this)]
      (set! (.-root this) root)
      (set! rev (unchecked-inc-int rev))
      (.notifyWatches this oldroot root)))

  (method ^:public ^:synchronized unbindRoot ^void [this]
    (set! (.-root this) (Unbound. this))
    (set! rev (unchecked-inc-int rev)))

  (method ^:public ^:synchronized commuteRoot ^void [this ^IFn fn]
    (let [newRoot (.invoke fn root)]
      (.validate this (.getValidator this) newRoot)
      (let [oldroot root]
        (set! (.-root this) newRoot)
        (set! rev (unchecked-inc-int rev))
        (.notifyWatches this oldroot newRoot))))

  (method ^:public ^:synchronized alterRoot [this ^IFn fn ^ISeq args]
    (let [newRoot (.applyTo fn (RT/cons root args))]
      (.validate this (.getValidator this) newRoot)
      (let [oldroot root]
        (set! (.-root this) newRoot)
        (set! rev (unchecked-inc-int rev))
        (.notifyWatches this oldroot newRoot)
        newRoot)))

  (method ^:public ^:static pushThreadBindings ^void [^Associative bindings]
    (let [f (cast Frame (.get dvals))
          ^:mutable bmap (.-bindings f)]
      (loop [bs (.seq bindings)]
        (when (some? bs)
          (let [e (cast IMapEntry (.first bs))
                v (cast Var (.key e))]
            (when-not (.-dynamic v)
              (throw (IllegalStateException.
                       (String/format "Can't dynamically bind non-dynamic var: %s/%s"
                                      (new Object/1 [(.-ns v) (.-sym v)])))))
            (.validate v (.getValidator v) (.val e))
            (.set (.-threadBound v) true)
            (set! bmap (.assoc bmap v (TBox. (Thread/currentThread) (.val e))))
            (recur (.next bs)))))
      (.set dvals (Frame. bmap f))))

  (method ^:public ^:static popThreadBindings ^void []
    (let [f (.-prev (cast Frame (.get dvals)))]
      (cond
        (nil? f) (throw (IllegalStateException. "Pop without matching push"))
        (identical? f Frame/TOP) (.remove dvals)
        :else (.set dvals f))))

  (method ^:public ^:static getThreadBindings ^Associative []
    (let [f (cast Frame (.get dvals))
          ^:mutable ^IPersistentMap ret PersistentHashMap/EMPTY]
      (loop [bs (.seq (.-bindings f))]
        (when (some? bs)
          (let [e (cast IMapEntry (.first bs))
                v (cast Var (.key e))
                b (cast TBox (.val e))]
            (set! ret (.assoc ret v (.-val b)))
            (recur (.next bs)))))
      ret))

  (method ^:public ^:final getThreadBinding ^TBox [this]
    (when (.get threadBound)
      (let [e (.entryAt (.-bindings (cast Frame (.get dvals))) this)]
        (when (some? e) (return (cast TBox (.val e))))))
    nil)

  (method ^:public ^:final fn ^IFn [this] (cast IFn (.deref this)))

  (method ^:public call [this] (.invoke this))

  (method ^:public run ^void [this] (.invoke this))

  (method ^:public invoke [this] (.invoke (.fn this)))

  (method ^:public invoke [this ^:mutable arg1]
    (.invoke (.fn this) (Util/ret1 arg1 (set! arg1 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2]
    (.invoke (.fn this) (Util/ret1 arg1 (set! arg1 nil)) (Util/ret1 arg2 (set! arg2 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3]
    (.invoke (.fn this)
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4]
    (.invoke (.fn this)
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5]
    (.invoke (.fn this)
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6]
    (.invoke (.fn this)
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7]
    (.invoke (.fn this)
             (Util/ret1 arg1 (set! arg1 nil))
             (Util/ret1 arg2 (set! arg2 nil))
             (Util/ret1 arg3 (set! arg3 nil))
             (Util/ret1 arg4 (set! arg4 nil))
             (Util/ret1 arg5 (set! arg5 nil))
             (Util/ret1 arg6 (set! arg6 nil))
             (Util/ret1 arg7 (set! arg7 nil))))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8]
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
    (.invoke (.fn this)
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
                           ^:mutable ^Object/1 args]
    (.invoke (.fn this)
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
             (cast Object/1 (Util/ret1 args (set! args nil)))))

  (method ^:public applyTo [this ^ISeq arglist]
    (.applyTo (.fn this) arglist))

  (field ^:static ^IFn assoc (anon AFn [] (method ^:public invoke [this m k v] (RT/assoc m k v))))

  (field ^:static ^IFn dissoc (anon AFn [] (method ^:public invoke [this c k] (RT/dissoc c k))))

  (defclass ^:private ^:static Serialized
    :implements [Serializable]

    (constructor ^:public [this ^Symbol nsName ^Symbol sym]
      (set! (.-nsName this) nsName)
      (set! (.-sym this) sym))

    (field ^:private ^Symbol nsName)

    (field ^:private ^Symbol sym)

    (method ^:private readResolve :throws [ObjectStreamException] [this]
      (Var/intern nsName sym)))

  (method ^:private writeReplace :throws [ObjectStreamException] [this]
    (Serialized. (.getName ns) sym)))
