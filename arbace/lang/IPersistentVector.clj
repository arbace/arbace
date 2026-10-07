;; Converted from clojure/lang/IPersistentVector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:interface IPersistentVector
  :extends [Associative Sequential IPersistentStack Reversible Indexed]

  (method length ^int [this])

  (method assocN ^IPersistentVector [this ^int i val])

  (method cons ^IPersistentVector [this o]))
