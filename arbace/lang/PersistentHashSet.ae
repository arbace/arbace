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
;; Converted from clojure/lang/PersistentHashSet.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util List))

(defclass ^:public PersistentHashSet
  :extends APersistentSet
  :implements [IObj IEditableCollection]

  (field ^:private ^:static ^:final ^long serialVersionUID 6973890746204954938)

  (field ^:public ^:static ^:final ^PersistentHashSet EMPTY
    (PersistentHashSet. nil PersistentHashMap/EMPTY))

  (field ^:final ^IPersistentMap _meta)

  (method ^:public ^:static create ^PersistentHashSet [& ^Object/1 init]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))]
      (loop [^int i 0]
        (when (< i (alength init))
          (set! ret (cast ITransientSet (.conj ret (aget init i))))
          (recur (unchecked-inc-int i))))
      (cast PersistentHashSet (.persistent ret))))

  (method ^:public ^:static create ^PersistentHashSet [^List init]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))]
      (for-each [key init] (set! ret (cast ITransientSet (.conj ret key))))
      (cast PersistentHashSet (.persistent ret))))

  (method ^:public ^:static create ^PersistentHashSet [^:mutable ^ISeq items]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))]
      (while (some? items)
        (set! ret (cast ITransientSet (.conj ret (.first items))))
        (set! items (.next items)))
      (cast PersistentHashSet (.persistent ret))))

  (method ^:public ^:static createWithCheck ^PersistentHashSet [& ^Object/1 init]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))]
      (loop [^int i 0]
        (when (< i (alength init))
          (set! ret (cast ITransientSet (.conj ret (aget init i))))
          (if (not (== (.count ret) (unchecked-add-int i 1)))
              (throw (IllegalArgumentException. (java-str "Duplicate key: " (aget init i))))
              (recur (unchecked-inc-int i)))))
      (cast PersistentHashSet (.persistent ret))))

  (method ^:public ^:static createWithCheck ^PersistentHashSet [^List init]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))
          ^:mutable ^int i 0]
      (for-each [key init]
        (set! ret (cast ITransientSet (.conj ret key)))
        (when-not (== (.count ret) (unchecked-add-int i 1))
          (throw (IllegalArgumentException. (java-str "Duplicate key: " key))))
        (set! i (unchecked-inc-int i)))
      (cast PersistentHashSet (.persistent ret))))

  (method ^:public ^:static createWithCheck ^PersistentHashSet [^:mutable ^ISeq items]
    (let [^:mutable ret (cast ITransientSet (.asTransient EMPTY))]
      (let [^:mutable ^int i 0]
        (while (some? items)
          (set! ret (cast ITransientSet (.conj ret (.first items))))
          (when-not (== (.count ret) (unchecked-add-int i 1))
            (throw (IllegalArgumentException. (java-str "Duplicate key: " (.first items)))))
          (set! items (.next items))
          (set! i (unchecked-inc-int i))))
      (cast PersistentHashSet (.persistent ret))))

  (constructor [this ^IPersistentMap meta ^IPersistentMap impl]
    (super. impl)
    (set! (.-_meta this) meta))

  (method ^:public disjoin ^IPersistentSet [this key]
    (if (.contains this key) (PersistentHashSet. (.meta this) (.without (.-impl this) key)) this))

  (method ^:public cons ^IPersistentSet [this o]
    (if (.contains this o) this (PersistentHashSet. (.meta this) (.assoc (.-impl this) o o))))

  (method ^:public empty ^IPersistentCollection [this]
    (.withMeta EMPTY (.meta this)))

  (method ^:public withMeta ^PersistentHashSet [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (PersistentHashSet. meta (.-impl this))))

  (method ^:public asTransient ^ITransientCollection [this]
    (TransientHashSet. _meta (.asTransient (cast PersistentHashMap (.-impl this)))))

  (method ^:public meta ^IPersistentMap [this] _meta)

  (defclass ^:static ^:final TransientHashSet
    :extends ATransientSet

    (field ^:private ^:final ^IPersistentMap _meta)

    (constructor [this ^IPersistentMap _meta ^ITransientMap impl]
      (super. impl)
      (set! (.-_meta this) _meta))

    (method ^:public persistent ^IPersistentCollection [this]
      (PersistentHashSet. _meta (.persistent (.-impl this))))))
