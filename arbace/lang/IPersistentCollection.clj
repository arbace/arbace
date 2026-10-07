;; Converted from clojure/lang/IPersistentCollection.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:interface IPersistentCollection
  :extends [Seqable]

  (method count ^int [this])

  (method cons ^IPersistentCollection [this o])

  (method empty ^IPersistentCollection [this])

  (method equiv ^boolean [this o]))
