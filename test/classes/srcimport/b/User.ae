;; Imports a member class of a class compiled from source in the same build, as j2c writes an
;; import of a nested class (import jdk.internal.icu.util.CodePointTrie.Fast16).
(in-ns 'classes.srcimport.b)

(import '(classes.srcimport.a Outer$Inner))

(defclass ^:public User
  (field ^:private ^Outer$Inner held)
  (method ^:public ^:static twice ^int [^int x]
    (let [^Outer$Inner i (Outer$Inner/make x)]
      (unchecked-add-int (.-v i) (.-v i)))))
