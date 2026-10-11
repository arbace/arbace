;; A class with a member class, compiled from source with srcimport/b/User.clj, which imports
;; the member class by its binary name (classes.loader-test, source-import-of-member-class).
(in-ns 'classes.srcimport.a)

(defclass ^:public Outer
  (defclass ^:public ^:static Inner
    (field ^:public ^int v)
    (constructor ^:public [this ^int v] (set! (.-v this) v))
    (method ^:public ^:static make ^Inner [^int v] (Inner. v))))
