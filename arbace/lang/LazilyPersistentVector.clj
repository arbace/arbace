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
;; /* rich May 14, 2008 */
;;
;; Converted from clojure/lang/LazilyPersistentVector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Collection))

(defclass ^:public LazilyPersistentVector
  (method ^:public ^:static createOwning ^IPersistentVector [& ^Object/1 items]
    (if (<= (alength items) 32)
        (PersistentVector. (alength items) 5 PersistentVector/EMPTY_NODE items)
        (PersistentVector/create items)))

  (method ^:static fcount ^int [c]
    (if (instance? Counted c) (.count (cast Counted c)) (.size (cast Collection c))))

  (method ^:public ^:static create ^IPersistentVector [obj]
    (cond
      (instance? IReduceInit obj) (PersistentVector/create (cast IReduceInit obj))
      (instance? ISeq obj) (PersistentVector/create (RT/seq obj))
      (instance? Iterable obj) (PersistentVector/create (cast Iterable obj))
      :else (LazilyPersistentVector/createOwning (RT/toArray obj)))))
