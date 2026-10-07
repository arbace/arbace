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
;; Converted from clojure/lang/PersistentTreeSet.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Comparator))

(defclass ^:public PersistentTreeSet
  :extends APersistentSet
  :implements [IObj Reversible Sorted]

  (field ^:public ^:static ^:final ^PersistentTreeSet EMPTY
    (PersistentTreeSet. nil PersistentTreeMap/EMPTY))

  (field ^:final ^IPersistentMap _meta)

  (method ^:public ^:static create ^PersistentTreeSet [^:mutable ^ISeq items]
    (let [^:mutable ret EMPTY]
      (while (some? items)
        (set! ret (cast PersistentTreeSet (.cons ret (.first items))))
        (set! items (.next items)))
      ret))

  (method ^:public ^:static create ^PersistentTreeSet [^Comparator comp ^:mutable ^ISeq items]
    (let [^:mutable ret (PersistentTreeSet. nil (PersistentTreeMap. nil comp))]
      (while (some? items)
        (set! ret (cast PersistentTreeSet (.cons ret (.first items))))
        (set! items (.next items)))
      ret))

  (constructor [this ^IPersistentMap meta ^IPersistentMap impl]
    (super. impl)
    (set! (.-_meta this) meta))

  (method ^:public equals ^boolean [this obj]
    (try (.equals super obj) (catch ClassCastException e false)))

  (method ^:public equiv ^boolean [this obj]
    (try (.equiv super obj) (catch ClassCastException e false)))

  (method ^:public disjoin ^IPersistentSet [this key]
    (if (.contains this key) (PersistentTreeSet. (.meta this) (.without (.-impl this) key)) this))

  (method ^:public cons ^IPersistentSet [this o]
    (if (.contains this o) this (PersistentTreeSet. (.meta this) (.assoc (.-impl this) o o))))

  (method ^:public empty ^IPersistentCollection [this]
    (PersistentTreeSet. (.meta this) (cast PersistentTreeMap (.empty (.-impl this)))))

  (method ^:public rseq ^ISeq [this]
    (APersistentMap$KeySeq/create (.rseq (cast Reversible (.-impl this)))))

  (method ^:public withMeta ^PersistentTreeSet [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (PersistentTreeSet. meta (.-impl this))))

  (method ^:public comparator ^Comparator [this]
    (.comparator (cast Sorted (.-impl this))))

  (method ^:public entryKey [this entry] entry)

  (method ^:public seq ^ISeq [this ^boolean ascending]
    (let [m (cast PersistentTreeMap (.-impl this))] (RT/keys (.seq m ascending))))

  (method ^:public seqFrom ^ISeq [this key ^boolean ascending]
    (let [m (cast PersistentTreeMap (.-impl this))] (RT/keys (.seqFrom m key ascending))))

  (method ^:public meta ^IPersistentMap [this] _meta))
