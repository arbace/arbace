(ns arbace.j2c.forms
  "The data the converter produces: ordinary Clojure forms, plus class references that are
  named only when the file's imports are known, raw tokens, and ordered metadata.")

;; A reference to a class by binary name ("java.util.Map$Entry"), with `dims` array
;; dimensions and an optional `member` suffix: "f" for C/f, ".m" for C/.m, "new", "this",
;; "super". `shadow` holds the simple names that something else claims where the reference
;; occurs (member classes, type variables, locals, fields), so the printer qualifies the
;; reference when its import name is among them.
(defrecord CRef [name dims member shadow])

;; A token printed verbatim, such as 0x9e3779b9.
(defrecord Raw [text])

(defn cref
  ([name] (->CRef name 0 nil #{}))
  ([name dims] (->CRef name dims nil #{})))

(defn member [x m]
  (cond
    (instance? CRef x) (assoc x :member m)
    (symbol? x) (symbol (str x "/" m))
    :else (throw (ex-info "member of non-class" {:x x :m m}))))

(defn raw [s] (->Raw s))

;; Metadata is kept in order under ::m as a vector of items:
;;   :public                  a flag, printed ^:public
;;   [:tag type]              printed ^T or ^{:tag (...)}
;;   [:ann type value]        an annotation, printed ^{T value}
;;   [:param-tags [t ...]]    printed ^[t ...]
(defn m+
  "Add metadata items to form `x` (a symbol, list, vector or CRef)."
  [x & items]
  (let [items (remove nil? items)]
    (if (empty? items)
      x
      (vary-meta x update ::m (fnil into []) items))))

(defn items [x] (::m (meta x)))

(defn tag [x t] (if t (m+ x [:tag t]) x))

(defn flags->items [flags] (map keyword flags))
