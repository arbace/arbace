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
;; /* rich 2/28/11 */
;;
;; Converted from clojure/lang/FnLoaderThunk.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException NotSerializableException ObjectInputStream ObjectOutputStream)
        '(java.lang.reflect Constructor))

(defclass ^:public FnLoaderThunk
  :extends RestFn

  (field ^:private ^:static ^:final ^long serialVersionUID 2194257205455463687)

  (field ^:final ^Var v)

  (field ^:final ^ClassLoader loader)

  (field ^:final ^String fnClassName)

  (field ^IFn fn)

  (constructor ^:public [this ^Var v ^String fnClassName]
    (set! (.-v this) v)
    (set! (.-loader this) (cast ClassLoader (.get RT/FN_LOADER_VAR)))
    (set! (.-fnClassName this) fnClassName)
    (set! fn nil))

  (method ^:public invoke [this arg1] (.load this) (.invoke fn arg1))

  (method ^:public invoke [this arg1 arg2]
    (.load this)
    (.invoke fn arg1 arg2))

  (method ^:public invoke [this arg1 arg2 arg3]
    (.load this)
    (.invoke fn arg1 arg2 arg3))

  (method ^:protected doInvoke [this args]
    (.load this)
    (.applyTo fn (cast ISeq args)))

  (method ^:private load ^void [this]
    (when (nil? fn)
      (try
        (set! fn
              (cast IFn
                    (^[Object/1] Constructor/.newInstance
                      (^[Class/1] Class/.getDeclaredConstructor
                        (Class/forName fnClassName true loader)))))
        (catch Exception e (throw (Util/sneakyThrow e))))
      (set! (.-root v) fn)))

  (method ^:public getRequiredArity ^int [this] 0)

  (method ^:public withMeta ^IObj [this ^IPersistentMap meta] this)

  (method ^:public meta ^IPersistentMap [this] nil)

  (method ^:private readObject :throws [IOException] ^void [this ^ObjectInputStream in]
    (throw (NotSerializableException.)))

  (method ^:private writeObject :throws [IOException] ^void [this ^ObjectOutputStream oos]
    (throw (NotSerializableException.))))
