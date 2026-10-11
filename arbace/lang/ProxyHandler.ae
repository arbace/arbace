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
;; /* rich Oct 4, 2007 */
;;
;; Converted from clojure/lang/ProxyHandler.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.lang.reflect InvocationHandler))

(defclass ^:public ProxyHandler
  :implements [InvocationHandler]

  (field ^:final ^IPersistentMap fns)

  (constructor ^:public [this ^IPersistentMap fns]
    (set! (.-fns this) fns))

  (method ^:public invoke :throws [Throwable] [this proxy ^java.lang.reflect.Method method
                                               ^Object/1 args]
    (let [rt (.getReturnType method)
          fn (cast IFn (.valAt fns (.getName method)))]
      (when (nil? fn)
        (cond
          (identical? rt Void/TYPE) (return nil)
          (.equals (.getName method) "equals") (return (identical? proxy (aget args 0)))
          (.equals (.getName method) "hashCode") (return (System/identityHashCode proxy))
          (.equals (.getName method) "toString")
            (return (java-str "Proxy: " (System/identityHashCode proxy))))
        (throw (UnsupportedOperationException.)))
      (let [ret (.applyTo fn (ArraySeq/create args))]
        (when-not (identical? rt Void/TYPE)
          (when (.isPrimitive rt)
            (cond
              (identical? rt Character/TYPE) (return ret)
              (identical? rt Integer/TYPE) (return (.intValue (cast Number ret)))
              (identical? rt Long/TYPE) (return (.longValue (cast Number ret)))
              (identical? rt Float/TYPE) (return (.floatValue (cast Number ret)))
              (identical? rt Double/TYPE) (return (.doubleValue (cast Number ret)))
              (and (identical? rt Boolean/TYPE) (not (instance? Boolean ret)))
                (return (if (nil? ret) Boolean/FALSE Boolean/TRUE))
              (identical? rt Byte/TYPE) (return (unchecked-byte (.intValue (cast Number ret))))
              (identical? rt Short/TYPE) (return (unchecked-short (.intValue (cast Number ret))))))
          ret)))))
