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
;; Converted from clojure/lang/IteratorSeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException NotSerializableException ObjectOutputStream)
        '(java.util Iterator))

(defclass ^:public IteratorSeq
  :extends ASeq

  (field ^:private ^:static ^:final ^long serialVersionUID -2631916503522522760)

  (field ^:final ^Iterator iter)

  (field ^:final ^State state)

  (defclass ^:static State
    (field ^:volatile val)

    (field ^:volatile _rest))

  (method ^:public ^:static create ^IteratorSeq [^Iterator iter]
    (when (.hasNext iter) (IteratorSeq. iter)))

  (constructor [this ^Iterator iter]
    (set! (.-iter this) iter)
    (set! state (State.))
    (set! (.-val (.-state this)) state)
    (set! (.-_rest (.-state this)) state))

  (constructor [this ^IPersistentMap meta ^Iterator iter ^State state]
    (super. meta)
    (set! (.-iter this) iter)
    (set! (.-state this) state))

  (method ^:public first [this]
    (when (identical? (.-val state) state)
      (locking state (when (identical? (.-val state) state) (set! (.-val state) (.next iter)))))
    (.-val state))

  (method ^:public next ^ISeq [this]
    (when (identical? (.-_rest state) state)
      (locking state
        (when (identical? (.-_rest state) state)
          (.first this)
          (set! (.-_rest state) (IteratorSeq/create iter)))))
    (cast ISeq (.-_rest state)))

  (method ^:public withMeta ^IteratorSeq [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (IteratorSeq. meta iter state)))

  (method ^:private writeObject :throws [IOException] ^void [this ^ObjectOutputStream out]
    (throw (NotSerializableException. (.getName (.getClass this))))))
