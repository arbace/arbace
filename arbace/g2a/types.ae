(ns arbace.g2c.types
  "Type forms (doc/go/SPEC.md §5) from the helper's type table (doc/go/HELPER-NOTES.md,
  \"Types\"): for what the converter writes that the source did not spell, the type arguments
  of :inst (§5.5) and the :tag of an expression (§8.3).

  Named types, aliases and type parameters are written by name: unqualified in their own
  package, by the file's import name when the file imports their package, and otherwise by
  their package's namespace (§4.4).")

(def ^:dynamic *table*
  "The dump's :type-table, a vector."
  nil)

(def ^:dynamic *pkg*
  "The import path of the package converted."
  nil)

(def ^:dynamic *imports*
  "The file's imports: {path name} for the imports with a usable name (not _ or .)."
  {})

(def ^:dynamic *underlying*
  "{type-id underlying-id} of the named types declared in the package (the dump's :types)."
  {})

(defn entry [id] (nth *table* id))

(def reserved-type-heads
  "Type heads (§5.1): a generic type of one of these names is instantiated with inst."
  '#{* slice array map chan func struct interface | tilde inst})

(defn ns-name-of
  "The namespace of a Go import path (§4.1): go. plus the path, / as ., and . and - inside an
  element as _."
  [path]
  (symbol (apply str "go." (interpose "." (map #(.replaceAll ^String % "[.-]" "_")
                                                 (.split ^String path "/"))))))

(defn qualify
  "The symbol naming name of package path, from the file of the current package."
  [path name]
  (cond
    (or (nil? path) (= path *pkg*)) (symbol name)
    (contains? *imports* path) (symbol (get *imports* path) name)
    :else (symbol (str (ns-name-of path)) name)))

(declare type-form)

(defn- params-vec [vars variadic]
  (let [n (count vars)]
    (vec (mapcat (fn [i v]
                   (if (and variadic (= i (dec n)))
                     ['& (type-form (:t v))]
                     [(type-form (:t v))]))
                 (range) vars))))

(defn- method-elem [{:keys [name t]}]
  (let [sig (entry t)
        ps (params-vec (:params sig) (:variadic sig))
        rs (:results sig)]
    (cond
      (empty? rs) (list (symbol name) ps)
      (= 1 (count rs)) (list (symbol name) (with-meta ps {:tag (type-form (:t (first rs)))}))
      :else (list (symbol name) ps :results (mapv (comp type-form :t) rs)))))

(defn type-form
  "The type form of type id."
  [id]
  (let [e (entry id)]
    (case (:kind e)
      :basic (if (:pkg e) (qualify (:pkg e) (:name e)) (symbol (:name e)))
      :pointer (list '* (type-form (:elem e)))
      :slice (list 'slice (type-form (:elem e)))
      :array (list 'array (:len e) (type-form (:elem e)))
      :map (list 'map (type-form (:key e)) (type-form (:elem e)))
      :chan (case (:dir e)
              :both (list 'chan (type-form (:elem e)))
              :send (list 'chan :send (type-form (:elem e)))
              :recv (list 'chan :recv (type-form (:elem e))))
      :func (let [ps (params-vec (:params e) (:variadic e))]
              (if (seq (:results e))
                (list 'func ps (mapv (comp type-form :t) (:results e)))
                (list 'func ps)))
      :struct (apply list 'struct
                     (for [f (:fields e)]
                       (if (:embedded f)
                         (let [t (type-form (:t f))]
                           (if (:tag f) (vary-meta t assoc :go/tag (:tag f)) t))
                         (with-meta (symbol (:name f))
                           (cond-> {:tag (type-form (:t f))}
                             (:tag f) (assoc :go/tag (:tag f)))))))
      :interface (apply list 'interface
                        (concat (map method-elem (:methods e))
                                (map type-form (:embeddeds e))))
      :union (apply list '|
                    (for [t (:terms e)]
                      (if (:tilde t) (list 'tilde (type-form (:t t))) (type-form (:t t)))))
      (:named :alias)
      (let [base (qualify (:pkg e) (:name e))]
        (if (:origin e)
          (if (and (nil? (namespace base)) (reserved-type-heads base))
            (apply list 'inst base (map type-form (:args e)))
            (apply list base (map type-form (:args e))))
          base))
      :type-param (symbol (:name e))
      :tuple (apply list 'values (map (comp type-form :t) (:vars e)))
      (throw (ex-info (str "type entry " id ": " (pr-str e)) {:id id})))))

(defn untyped?
  "Whether type id is an untyped basic type."
  [id]
  (let [e (entry id)]
    (and (= :basic (:kind e)) (.startsWith ^String (:name e) "untyped"))))

(def ^:private basic-kinds
  {"bool" :bool "untyped bool" :bool
   "string" :string "untyped string" :string
   "untyped int" :int "untyped rune" :rune
   "float32" :float "float64" :float "untyped float" :untyped-float
   "complex64" :complex "complex128" :complex "untyped complex" :untyped-complex})

(defn const-kind
  "The kind of constant values of type id (lit/const-form), or nil when it is not known (a
  named type of another package, whose underlying type is in that package's dump)."
  [id]
  (let [e (entry id)]
    (case (:kind e)
      :basic (or (basic-kinds (:name e))
                 (when (#{"int" "int8" "int16" "int32" "int64" "uint" "uint8" "uint16" "uint32"
                          "uint64" "uintptr" "byte" "rune"} (:name e))
                   :int))
      :alias (const-kind (:actual e))
      :named (when-let [u (get *underlying* id)] (const-kind u))
      nil)))
