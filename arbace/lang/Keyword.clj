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
;; /* rich Mar 29, 2006 10:39:05 AM */
;;
;; Converted from clojure/lang/Keyword.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io ObjectStreamException Serializable)
        '(java.lang.ref Reference ReferenceQueue WeakReference)
        '(java.util.concurrent ConcurrentHashMap))

(defclass ^:public Keyword
  :implements [IFn Comparable Named Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID -2105088845257724163)

  (field ^:private ^:static ^{:tag (ConcurrentHashMap Symbol (Reference Keyword))} table
    (ConcurrentHashMap.))

  (field ^:static ^:final ^ReferenceQueue rq (ReferenceQueue.))

  (field ^:public ^:final ^Symbol sym)

  (field ^:final ^int hasheq)

  (field ^:transient ^String _str)

  (method ^:public ^:static intern ^Keyword [^:mutable ^Symbol sym]
    (let [^:mutable ^Keyword k nil
          ^:mutable ^{:tag (Reference Keyword)} existingRef (cast Reference (.get table sym))]
      (when (nil? existingRef)
        (Util/clearCache rq table)
        (when (some? (.meta sym)) (set! sym (cast Symbol (.withMeta sym nil))))
        (set! k (Keyword. sym))
        (set! existingRef (cast Reference (.putIfAbsent table sym (WeakReference. k rq)))))
      (if (nil? existingRef)
          k
          (let [existingk (cast Keyword (.get existingRef))]
            (if (some? existingk)
                existingk
                (do (.remove table sym existingRef) (Keyword/intern sym)))))))

  (method ^:public ^:static intern ^Keyword [^String ns ^String name]
    (Keyword/intern (Symbol/intern ns name)))

  (method ^:public ^:static intern ^Keyword [^String nsname]
    (Keyword/intern (Symbol/intern nsname)))

  (constructor ^:private [this ^Symbol sym]
    (set! (.-sym this) sym)
    (set! hasheq (unchecked-add-int (.hasheq sym) (unchecked-int 0x9e3779b9))))

  (method ^:public ^:static find ^Keyword [^Symbol sym]
    (let [^{:tag (Reference Keyword)} ref (cast Reference (.get table sym))]
      (when (some? ref) (cast Keyword (.get ref)))))

  (method ^:public ^:static find ^Keyword [^String ns ^String name]
    (Keyword/find (Symbol/intern ns name)))

  (method ^:public ^:static find ^Keyword [^String nsname]
    (Keyword/find (Symbol/intern nsname)))

  (method ^:public ^:final hashCode ^int [this]
    (unchecked-add-int (.hashCode sym) (unchecked-int 0x9e3779b9)))

  (method ^:public hasheq ^int [this] hasheq)

  (method ^:public toString ^String [this]
    (when (nil? _str) (set! _str (java-str ":" sym)))
    _str)

  (method ^:public ^:deprecated throwArity [this]
    (throw (IllegalArgumentException.
             (java-str "Wrong number of args passed to keyword: " (.toString this)))))

  (method throwArity [this ^int n]
    (throw (ArityException. n (.toString this))))

  (method ^:public call [this] (.throwArity this 0))

  (method ^:public run ^void [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public invoke [this] (.throwArity this 0))

  (method ^:public compareTo ^int [this o]
    (.compareTo sym (.-sym (cast Keyword o))))

  (method ^:public getNamespace ^String [this] (.getNamespace sym))

  (method ^:public getName ^String [this] (.getName sym))

  (method ^:private readResolve :throws [ObjectStreamException] [this]
    (Keyword/intern sym))

  (method ^:public ^:final invoke [this obj]
    (if (instance? ILookup obj) (.valAt (cast ILookup obj) this) (RT/get obj this)))

  (method ^:public ^:final invoke [this obj notFound]
    (if (instance? ILookup obj)
        (.valAt (cast ILookup obj) this notFound)
        (RT/get obj this notFound)))

  (method ^:public invoke [this arg1 arg2 arg3] (.throwArity this 3))

  (method ^:public invoke [this arg1 arg2 arg3 arg4] (.throwArity this 4))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5]
    (.throwArity this 5))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6]
    (.throwArity this 6))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7]
    (.throwArity this 7))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8]
    (.throwArity this 8))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9]
    (.throwArity this 9))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10]
    (.throwArity this 10))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11]
    (.throwArity this 11))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12]
    (.throwArity this 12))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13]
    (.throwArity this 13))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14]
    (.throwArity this 14))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15]
    (.throwArity this 15))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16]
    (.throwArity this 16))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17]
    (.throwArity this 17))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18]
    (.throwArity this 18))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19]
    (.throwArity this 19))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20]
    (.throwArity this 20))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20 & ^Object/1 args]
    (.throwArity this (unchecked-add-int 20 (alength args))))

  (method ^:public applyTo [this ^ISeq arglist]
    (AFn/applyToHelper this arglist)))
