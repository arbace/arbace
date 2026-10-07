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
;; /* rich Mar 3, 2008 */
;;
;; Converted from clojure/lang/EnumerationSeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException NotSerializableException ObjectOutputStream)
        '(java.util Enumeration))

(defclass ^:public EnumerationSeq
  :extends ASeq

  (field ^:private ^:static ^:final ^long serialVersionUID 5227192199685595994)

  (field ^:final ^Enumeration iter)

  (field ^:final ^State state)

  (defclass ^:static State
    (field ^:volatile val)

    (field ^:volatile _rest))

  (method ^:public ^:static create ^EnumerationSeq [^Enumeration iter]
    (when (.hasMoreElements iter) (EnumerationSeq. iter)))

  (constructor [this ^Enumeration iter]
    (set! (.-iter this) iter)
    (set! state (State.))
    (set! (.-val (.-state this)) state)
    (set! (.-_rest (.-state this)) state))

  (constructor [this ^IPersistentMap meta ^Enumeration iter ^State state]
    (super. meta)
    (set! (.-iter this) iter)
    (set! (.-state this) state))

  (method ^:public first [this]
    (when (identical? (.-val state) state)
      (locking state
        (when (identical? (.-val state) state) (set! (.-val state) (.nextElement iter)))))
    (.-val state))

  (method ^:public next ^ISeq [this]
    (when (identical? (.-_rest state) state)
      (locking state
        (when (identical? (.-_rest state) state)
          (.first this)
          (set! (.-_rest state) (EnumerationSeq/create iter)))))
    (cast ISeq (.-_rest state)))

  (method ^:public withMeta ^EnumerationSeq [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (EnumerationSeq. meta iter state)))

  (method ^:private writeObject :throws [IOException] ^void [this ^ObjectOutputStream out]
    (throw (NotSerializableException. (.getName (.getClass this))))))
