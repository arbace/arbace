(ns arbace.g2c.convert
  "g2c's converter: the helper's typed syntax tree of a Go package (doc/go/HELPER-NOTES.md,
  format :godump 2) to Go forms (doc/go/SPEC.md). Every decision about forms is made here; the
  helper is dumb. See doc/go/CONVERTER-NOTES.md.

  The forms are built in memory as Clojure data: lists, vectors and symbols carry the public
  metadata of the spec (:tag, :val, :inst, :go/via, ...) and the private layout keys of this
  namespace (::l, the Go line a form begins on; ::pos, its node's positions for mode :full).
  Literals are arbace.g2c.lit/Lit (text and value). arbace.g2c.layout writes them as text."
  (:require [arbace.g2c.lit :as lit]
            [arbace.g2c.types :as ty]
            [arbace.string :as str]))

;;; State of a conversion

(def ^:dynamic *dump* nil)
(def ^:dynamic *stats* nil)        ; atom {:nodes {kind n} :heads {head n} :notes {k n}}
(def ^:dynamic *generic* false)    ; inside a generic declaration (field groups matter, §5.5)
(def ^:dynamic *kept-docs* nil)    ; atom #{comment group position} kept as data
(def ^:dynamic *fn-dirs* nil)      ; [[line col text] ...] directives inside function bodies
(def ^:dynamic *placed* nil)       ; atom #{[line col]} directives placed as forms or metadata

(defn note! [k] (when *stats* (swap! *stats* update-in [:notes k] (fnil inc 0))))
(defn count-node! [n] (when *stats* (swap! *stats* update-in [:nodes (first n)] (fnil inc 0))))

(defn fail [msg n]
  (throw (ex-info (str msg ": " (pr-str (if (seq? n) (first n) n))
                       (when-let [p (and (seq? n) (map? (second n)) (some-> n second vals))]
                         (str " at " (first (filter #(and (string? %) (re-matches #"\d+:\d+" %)) p)))))
                  {:node (if (seq? n) (first n) n)})))

;;; Nodes

(defn nt [n] (when (seq? n) (first n)))
(defn nf [n] (second n))
(defn an [n] (meta n))

(defn unparen [n] (if (= :paren-expr (nt n)) (recur (:x (nf n))) n))

(defn parse-pos [^String s]
  (when (and s (re-matches #"\d+:\d+" s))
    (let [i (.indexOf s ":")]
      [(Long/parseLong (subs s 0 i)) (Long/parseLong (subs s (inc i)))])))

(defn pos-line [s] (first (parse-pos s)))

(defn start
  "The position string of a node's start, go/ast's Pos()."
  [n]
  (let [f (nf n)]
    (case (nt n)
      :ident (:name-pos f)
      :basic-lit (:value-pos f)
      :composite-lit (if (:type f) (start (:type f)) (:lbrace f))
      :func-lit (start (:type f))
      :paren-expr (:lparen f)
      (:selector-expr :index-expr :index-list-expr :slice-expr :type-assert-expr :binary-expr)
      (start (:x f))
      :call-expr (start (:fun f))
      :star-expr (:star f)
      :unary-expr (:op-pos f)
      :key-value-expr (start (:key f))
      :array-type (:lbrack f)
      :struct-type (:struct f)
      :func-type (or (:func f) (start (:params f)))
      :interface-type (:interface f)
      :map-type (:map f)
      :chan-type (:begin f)
      :ellipsis (:ellipsis f)
      :field-list (or (:opening f) (some-> (:list f) first start))
      :field (if (seq (:names f)) (start (first (:names f))) (start (:type f)))
      :decl-stmt (start (:decl f))
      :labeled-stmt (start (:label f))
      :expr-stmt (start (:x f))
      :send-stmt (start (:chan f))
      :inc-dec-stmt (start (:x f))
      :assign-stmt (start (first (:lhs f)))
      :go-stmt (:go f)
      :defer-stmt (:defer f)
      :return-stmt (:return f)
      :branch-stmt (:tok-pos f)
      :block-stmt (:lbrace f)
      :if-stmt (:if f)
      (:case-clause :comm-clause) (:case f)
      (:switch-stmt :type-switch-stmt) (:switch f)
      :select-stmt (:select f)
      (:for-stmt :range-stmt) (:for f)
      :empty-stmt (:semicolon f)
      :gen-decl (:tok-pos f)
      :func-decl (start (:type f))
      :import-spec (if (:name f) (start (:name f)) (start (:path f)))
      :value-spec (start (first (:names f)))
      :type-spec (start (:name f))
      (fail "no start for" n))))

(defn line-of [n] (pos-line (start n)))

(defn positions
  "The position fields of a node: {:field [line col]}."
  [n]
  (into (sorted-map)
        (keep (fn [[k v]] (when-let [p (and (string? v) (parse-pos v))] [k p])))
        (nf n)))

(defn max-pos
  "The greatest position [line col] in a node's subtree."
  [n]
  (let [best (volatile! [0 0])
        walk (fn walk [x]
               (cond
                 (seq? x) (when (map? (second x))
                            (doseq [v (vals (second x))] (walk v)))
                 (vector? x) (run! walk x)
                 (string? x) (when-let [p (parse-pos x)]
                               (when (pos? (compare p @best)) (vreset! best p)))))]
    (walk n)
    @best))

;;; Building forms

(defn form
  "A list form beginning on line l, built from node n (whose positions it keeps for :full)."
  [n l & xs]
  (with-meta (apply list xs) (cond-> {::l l} n (assoc ::pos (positions n)))))

(defn at
  "x (a symbol, list or vector) with line l and node n's positions."
  [x n l]
  (if (instance? arbace.lang.IObj x)
    (vary-meta x (fn [m] (cond-> (assoc (or m {}) ::l (or (::l m) l))
                           n (update ::pos #(merge (positions n) %)))))
    x))

(defn line [x] (::l (meta x)))

(defn with-pos
  "x with more positions for mode :full: m maps keys to position strings (nil ones skipped)."
  [x m]
  (let [ps (into {} (keep (fn [[k v]] (when-let [p (parse-pos v)] [k p]))) m)]
    (if (and (seq ps) (instance? arbace.lang.IObj x))
      (vary-meta x update ::pos #(merge (or % (sorted-map)) ps))
      x)))

(defn block-pos [b] (when b {:lbrace (:lbrace (nf b)) :rbrace (:rbrace (nf b))}))

(defn with-line [l x]
  (if (and (instance? arbace.lang.IObj x) (not (line x))) (vary-meta x assoc ::l l) x))

(defn vec-at [l xs] (with-meta (vec xs) {::l l}))

(defn add-meta
  "x with the public metadata m merged in."
  [x m]
  (if (empty? m) x (vary-meta x merge m)))

(defn lit-at
  "A Lit placed on line l (layout records it in its parent)."
  [l ^arbace.g2c.lit.Lit x pos]
  (assoc x :line l :pos pos))

(defn relined
  "x with every form inside on line l and no positions (an implicitly repeated init)."
  [l x]
  (cond
    (lit/lit? x) (assoc x :line l :pos nil)
    (seq? x) (with-meta (apply list (map #(relined l %) x))
               (-> (meta x) (assoc ::l l) (dissoc ::pos)))
    (vector? x) (with-meta (mapv #(relined l %) x) (-> (meta x) (assoc ::l l) (dissoc ::pos)))
    (symbol? x) (vary-meta x #(-> % (assoc ::l l) (dissoc ::pos)))
    :else x))

;;; Names

(def reserved-heads
  "Unqualified heads of Go context (§13.5): a call of a Go name among them is go/call."
  (into #{}
        (map symbol)
        (concat
         ["let" "let-type" "set!" "aset" "inc!" "dec!" ">!" "go" "defer" "return" "break"
          "continue" "goto" "fallthrough" "label" "do" "if" "when" "cond" "switch" "type-switch"
          "select" "case" "default" "for" "while" "range"]
         ["+" "-" "*" "/" "%" "<<" ">>" "==" "!=" "<" "<=" ">" ">=" "and" "or" "not" "bit-and"
          "bit-or" "bit-xor" "bit-not" "bit-and-not" "addr" "<!" "lit" "conv" "assert" "aget"
          "subslice" "spread" "values" "inst" "method-expr" "fn" "zero" "rune" "imaginary"
          "byte-string" "deref"]
         ["append" "cap" "clear" "close" "complex" "copy" "delete" "imag" "len" "make" "max" "min"
          "new" "panic" "print" "println" "real" "recover"]
         ["slice" "array" "map" "chan" "func" "struct" "interface" "|" "tilde"])))

(defn ident-name [n] (:name (nf n)))

(defn obj [n] (or (:use (an n)) (:def (an n))))

(defn targs
  "The type forms of the type arguments of an instance annotation."
  [inst]
  (mapv ty/type-form (:targs inst)))

(defn inst-meta
  "{:inst [...]} for the use of a generic function or method whose type arguments are not
  all in the source (explicit: how many are)."
  [n explicit]
  (let [inst (:inst (an n))
        o (obj n)]
    (when (and inst (= :func (:kind o)) (< explicit (count (:targs inst))))
      {:inst (targs inst)})))

(defn ident
  "An identifier as a symbol, a literal (the universe's true, false, nil) or (go/id \"x\")."
  ([n] (ident n 0))
  ([n explicit]
   (count-node! n)
   (let [name (ident-name n)
         l (line-of n)
         o (obj n)]
     (cond
       (= name "_") (at (symbol "_") n l)
       (#{"true" "false" "nil"} name)
       (if (or (:universe o) (= :nil (:mode (an n))) (and (nil? o) (= :const (:mode (an n)))))
         (lit-at l (lit/lit name ({"true" true "false" false "nil" nil} name)) (parse-pos (start n)))
         (do (note! :go-id) (form n l 'go/id (lit/lit (lit/string-text name) name))))
       :else (-> (symbol name) (at n l) (add-meta (inst-meta n explicit)))))))

(defn label-kw [n] (count-node! n) (keyword (ident-name n)))

(defn pkg-ident? [n] (= :pkg (:kind (:use (an (unparen n))))))

;;; Literals

(defn basic-lit [n]
  (count-node! n)
  (let [{:keys [kind value]} (nf n)
        l (line-of n)
        p (parse-pos (start n))]
    (case kind
      "INT" (lit-at l (lit/int-lit value) p)
      "FLOAT" (lit-at l (lit/float-text-lit value) p)
      "IMAG" (form n l 'imaginary (lit/imag-inner value))
      "CHAR" (let [r (lit/rune-form (lit/rune-value value))]
               (if (lit/lit? r) (lit-at l r p) (at (with-meta r nil) n l)))
      "STRING" (let [r (lit/bytes-form (lit/string-bytes value))]
                 (if (lit/lit? r) (lit-at l r p) (at (with-meta r nil) n l))))))

(defn string-value
  "The Go string value of a STRING basic literal (import paths, struct tags)."
  [n]
  (String. (lit/string-bytes (:value (nf n))) java.nio.charset.StandardCharsets/UTF_8))

;;; Types (from the source, §5)

(declare expr type-form signature field-list-vec)

(defn tagged
  "The symbol sym with the type form t as :tag."
  [sym t]
  (if t (vary-meta sym assoc :tag t) sym))

(defn- group-marks
  "Names of one field after the first get :go/group inside generic declarations (proposed
  amendment: gc's output depends on field groups there)."
  [syms]
  (if *generic*
    (cons (first syms) (map #(vary-meta % assoc :go/grouped true) (rest syms)))
    syms))

(defn mark-ungrouped
  "Inside generic declarations, a name declared apart from the name before it although of an
  equal type gets :go/grouped false, so that the printer does not group them (A1)."
  [elems names-only]
  (if-not *generic*
    elems
    (vec (map-indexed
          (fn [i x]
            (let [prev (when (pos? i) (nth elems (dec i)))]
              (if (and (symbol? x) (symbol? prev) (not (contains? (meta x) :go/grouped))
                       (or (not names-only) (and (:tag (meta x)) (:tag (meta prev))))
                       (= (:tag (meta x)) (:tag (meta prev))))
                (vary-meta x assoc :go/grouped false)
                x)))
          elems))))

(defn doc-of
  "The kept doc text of a comment group position, recording it as kept; nil when empty."
  [pos-str groups]
  (when pos-str
    (when-let [g (get groups pos-str)]
      (when *kept-docs* (swap! *kept-docs* conj pos-str))
      (let [t (:text g)]
        (when (and t (not= t "")) t)))))

(def ^:dynamic *groups* {}) ; comment groups of the file by position

(defn fields
  "The elements of a struct type's fields, or of a parameter/result vector: a tagged name per
  name, a type form for an unnamed field (embedded or unnamed parameter). opts: :struct (tags,
  docs), :variadic (an Ellipsis type is the last parameter)."
  [field-list {:keys [struct?]}]
  (count-node! field-list)
  (mark-ungrouped
   (vec
   (mapcat
    (fn [fld]
      (count-node! fld)
      (let [{:keys [names type tag doc]} (nf fld)
            variadic (= :ellipsis (nt type))
            _ (when variadic (count-node! type))
            _ (when (and struct? tag) (count-node! tag))
            t (if variadic
                (form type (line-of type) 'slice (type-form (:elt (nf type))))
                (type-form type))
            extra (cond-> {}
                    (and struct? tag) (assoc :go/tag (string-value tag))
                    (and struct? doc) (as-> m (if-let [d (doc-of doc *groups*)] (assoc m :doc d) m)))]
        (concat
         (when variadic ['&])
         (if (seq names)
           (group-marks
            (for [nm names]
              (-> (ident nm) (tagged t) (add-meta extra))))
           [(add-meta t extra)]))))
    (:list (nf field-list))))
   true))

(defn interface-elems [field-list]
  (count-node! field-list)
  (vec
   (for [fld (:list (nf field-list))]
     (let [{:keys [names type doc]} (nf fld)
           d (when doc (doc-of doc *groups*))]
       (count-node! fld)
       (if (seq names)
         (let [nm (first names)
               _ (count-node! type)
               [ps & rs] (signature type)]
           (at (apply list (add-meta (ident nm) (when d {:doc d})) ps rs) fld (line-of fld)))
         (add-meta (type-form type) (when d {:doc d})))))))

(defn type-form
  "The type form of a type expression node."
  [n]
  (when-not (= :ident (nt n)) (count-node! n))
  (let [f (nf n)
        l (line-of n)]
    (case (nt n)
      :ident (ident n)
      :paren-expr (type-form (:x f))
      :selector-expr (if (pkg-ident? (:x f))
                       (do (count-node! (unparen (:x f))) (count-node! (:sel f))
                           (at (symbol (ident-name (unparen (:x f))) (ident-name (:sel f))) n l))
                       (fail "type selector" n))
      :star-expr (form n l '* (type-form (:x f)))
      :array-type (cond
                    (nil? (:len f)) (form n l 'slice (type-form (:elt f)))
                    (= :ellipsis (nt (:len f))) (do (count-node! (:len f))
                                                    (form n l 'array (at '... (:len f) l) (type-form (:elt f))))
                    :else (form n l 'array (expr (:len f)) (type-form (:elt f))))
      :map-type (form n l 'map (type-form (:key f)) (type-form (:value f)))
      :chan-type (case (:dir f)
                   :both (form n l 'chan (type-form (:value f)))
                   :send (form n l 'chan :send (type-form (:value f)))
                   :recv (form n l 'chan :recv (type-form (:value f))))
      :func-type (let [ps (fields (:params f) {})
                       rs (when (:results f) (fields (:results f) {}))]
                   (if (seq rs)
                     (form n l 'func (vec-at l ps) (vec-at l rs))
                     (form n l 'func (vec-at l ps))))
      :struct-type (apply form n l 'struct (fields (:fields f) {:struct? true}))
      :interface-type (apply form n l 'interface (interface-elems (:methods f)))
      (:index-expr :index-list-expr)
      (let [g (type-form (:x f))
            args (map type-form (if (= :index-expr (nt n)) [(:index f)] (:indices f)))]
        (if (and (symbol? g) (nil? (namespace g)) (ty/reserved-type-heads g))
          (apply form n l 'inst g args)
          (apply form n l g args)))
      :binary-expr (if (= "|" (:op f))
                     (let [terms (loop [x (:x f) acc (list (type-form (:y f)))]
                                   (when (= :paren-expr (nt x)) (count-node! x))
                                   (let [x (unparen x)]
                                     (if (and (= :binary-expr (nt x)) (= "|" (:op (nf x))))
                                       (recur (do (count-node! x) (:x (nf x))) (cons (type-form (:y (nf x))) acc))
                                       (cons (type-form x) acc))))]
                       (apply form n l '| terms))
                     (fail "binary type" n))
      :unary-expr (if (= "~" (:op f))
                    (form n l 'tilde (type-form (:x f)))
                    (fail "unary type" n))
      :ellipsis (form n l 'slice (type-form (:elt f)))
      (fail "type" n))))

;;; Signatures (§5.3)

(defn signature
  "[params-vector results...] of a func-type: the parameter vector (tagged with a single
  unnamed result) and, for named or several results, :results [...]."
  [ft]
  (let [f (nf ft)
        l (line-of ft)
        ps (-> (vec-at (or (pos-line (:opening (nf (:params f)))) l) (fields (:params f) {}))
               (with-pos (select-keys (nf (:params f)) [:opening :closing])))
        rlist (when (:results f) (:list (nf (:results f))))]
    (cond
      (empty? rlist) (do (when (:results f) (count-node! (:results f))) [ps])
      (and (= 1 (count rlist)) (empty? (:names (nf (first rlist)))))
      (do (count-node! (first rlist)) (count-node! (:results f))
          [(vary-meta ps assoc :tag (type-form (:type (nf (first rlist)))))])
      :else [ps :results (-> (vec-at l (fields (:results f) {}))
                             (with-pos (select-keys (nf (:results f)) [:opening :closing])))])))

(defn type-params [field-list]
  (when field-list (count-node! field-list))
  (when (and field-list (seq (:list (nf field-list))))
    (binding [*generic* true]
     (mark-ungrouped
      (vec
     (mapcat (fn [fld]
               (count-node! fld)
               (let [{:keys [names type]} (nf fld)
                     c (let [u (unparen type)]
                         (if (and (= :ident (nt u)) (= "any" (ident-name u))
                                  (:universe (obj u)))
                           (do (count-node! type) nil)
                           (type-form type)))]
                 (binding [*generic* true]
                   (group-marks (for [nm names] (tagged (ident nm) c))))))
             (:list (nf field-list))))
      false))))

;;; Expressions (§7)

(def binary-ops
  {"+" '+ "-" '- "*" '* "/" '/ "%" '% "<<" '<< ">>" '>> "&" 'bit-and "|" 'bit-or "^" 'bit-xor
   "&^" 'bit-and-not "&&" 'and "||" 'or "==" '== "!=" '!= "<" '< "<=" '<= ">" '> ">=" '>=})

(def nary-ops '#{+ - * / bit-and bit-or bit-xor bit-and-not and or})

(def unary-ops {"-" '- "+" '+ "^" 'bit-not "!" 'not "&" 'addr "<-" '<!})

(def assign-ops
  {"+=" '+ "-=" '- "*=" '* "/=" '/ "%=" '% "<<=" '<< ">>=" '>> "&=" 'bit-and "|=" 'bit-or
   "^=" 'bit-xor "&^=" 'bit-and-not})

(defn untyped-const?
  "Whether a constant expression is untyped (for §8.3's shifts)."
  [n]
  (let [n (unparen n)
        f (nf n)
        a (an n)]
    (and (= :const (:mode a))
         (case (nt n)
           :basic-lit true
           :ident (let [o (obj n)] (and (= :const (:kind o)) (:t o) (ty/untyped? (:t o))))
           :selector-expr (let [o (:use (an (:sel f)))]
                            (and (= :const (:kind o)) (:t o) (ty/untyped? (:t o))))
           :unary-expr (untyped-const? (:x f))
           :binary-expr (if (#{"<<" ">>"} (:op f))
                          (untyped-const? (:x f))
                          (and (untyped-const? (:x f)) (untyped-const? (:y f))))
           :call-expr (and (= :builtin (:mode (an (unparen (:fun f)))))
                           (#{"complex" "real" "imag"} (ident-name (unparen (:fun f))))
                           (every? untyped-const? (:args f)))
           false))))

(defn as-inst
  "A generic type instance (G T...) as (inst G T...): in operands the printer reads as
  expressions too (new, method-expr; PRINTER-NOTES A5)."
  [t]
  (if (and (seq? t) (symbol? (first t)) (not (#{'inst} (first t)))
           (not (and (nil? (namespace (first t))) (ty/reserved-type-heads (first t)))))
    (with-meta (apply list 'inst t) (meta t))
    t))

(defn selection-via [a]
  (when-let [via (:via (:sel a))]
    {:go/via (mapv symbol via)}))

(defn selector-form
  "A selector expression not in callee position."
  [n]
  (let [f (nf n)
        a (an n)
        l (line-of n)
        x (:x f)
        sel (:sel f)
        k (:kind (:sel a))]
    (count-node! n)
    (count-node! sel)
    (when (pkg-ident? x) (count-node! (unparen x)))
    (cond
      (pkg-ident? x) (-> (symbol (ident-name (unparen x)) (ident-name sel))
                         (at n l) (add-meta (inst-meta sel 0))
                         (with-pos {:x-pos (start x) :name-pos (start sel)}))
      (= k :method-expr) (form n l 'method-expr (as-inst (type-form x)) (symbol (ident-name sel)))
      (#{:field :method} k) (-> (form n l (-> (symbol (str ".-" (ident-name sel))) (add-meta (inst-meta sel 0)))
                                      (expr x))
                                (add-meta (selection-via a))
                                (with-pos {:name-pos (start sel)}))
      :else (fail "selector" n))))

(defn call-args [args ellipsis]
  (let [xs (mapv expr args)]
    (if ellipsis
      (conj (pop xs) (form nil (line (peek xs)) 'spread (peek xs)))
      xs)))

(defn builtin-name
  "The head of a builtin call: len, append, ..., or unsafe/Sizeof with the file's name."
  [fun]
  (let [fun (unparen fun)]
    (case (nt fun)
      :ident (symbol (ident-name fun))
      :selector-expr (symbol (ident-name (unparen (:x (nf fun)))) (ident-name (:sel (nf fun))))
      (fail "builtin" fun))))

(defn call [n]
  (let [{:keys [fun args ellipsis]} (nf n)
        l (line-of n)
        u (unparen fun)
        fa (an u)
        ua (nf u)]
    (count-node! n)
    (cond
      (= :type (:mode fa))
      (do (when (not= 1 (count args)) (fail "conversion arity" n))
          (form n l 'conv (type-form fun) (expr (first args))))

      (= :builtin (:mode fa))
      (let [_ (loop [x fun] (when (= :paren-expr (nt x)) (count-node! x) (recur (:x (nf x)))))
            head (builtin-name fun)
            [a0 & more] args
            a0f (when a0 (if (= :type (:mode (an (unparen a0))))
                           (cond-> (type-form a0) (= 'new head) as-inst)
                           (expr a0)))]
        (if (= :selector-expr (nt u))
          (do (count-node! u) (count-node! (:sel (nf u))) (count-node! (unparen (:x (nf u)))))
          (count-node! u))
        (if a0
          (let [xs (into [a0f] (map expr more))
                xs (if ellipsis (conj (pop xs) (form nil (line (peek xs)) 'spread (peek xs))) xs)]
            (apply form n l (at head u (line-of u)) xs))
          (form n l (at head u (line-of u)))))

      (and (= :selector-expr (nt u)) (not (pkg-ident? (:x ua))) (= :method (:kind (:sel fa))))
      (do (count-node! u) (count-node! (:sel ua))
          (-> (apply form n l
                     (-> (symbol (str "." (ident-name (:sel ua)))) (add-meta (inst-meta (:sel ua) 0)))
                     (expr (:x ua))
                     (call-args args ellipsis))
              (add-meta (selection-via fa))
              (with-pos {:dot (:dot ua) :name-pos (start (:sel ua))})))

      (= :ident (nt u))
      (let [callee (ident u)]
        (if (and (symbol? callee) (reserved-heads callee))
          (do (note! :go-call)
              (apply form n l 'go/call callee (call-args args ellipsis)))
          (apply form n l callee (call-args args ellipsis))))

      :else (apply form n l (expr fun) (call-args args ellipsis)))))

(defn instantiation
  "F[T...] in an expression: (inst F T...), F carrying :inst when the arguments are partial."
  [n]
  (let [f (nf n)
        idx (if (= :index-expr (nt n)) [(:index f)] (:indices f))
        x (unparen (:x f))
        fx (case (nt x)
             :ident (ident x (count idx))
             :selector-expr (if (pkg-ident? (:x (nf x)))
                              (-> (do (count-node! x) (count-node! (:sel (nf x)))
                                      (count-node! (unparen (:x (nf x))))
                                      (symbol (ident-name (unparen (:x (nf x)))) (ident-name (:sel (nf x)))))
                                  (at x (line-of x)) (add-meta (inst-meta (:sel (nf x)) (count idx))))
                              (if (= :method-expr (:kind (:sel (an x))))
                                (do (count-node! x) (count-node! (:sel (nf x)))
                                    (form x (line-of x) 'method-expr (as-inst (type-form (:x (nf x))))
                                          (-> (symbol (ident-name (:sel (nf x))))
                                              (add-meta (inst-meta (:sel (nf x)) (count idx))))))
                              (-> (form x (line-of x)
                                        (-> (do (count-node! x) (count-node! (:sel (nf x)))
                                                (symbol (str ".-" (ident-name (:sel (nf x))))))
                                            (add-meta (inst-meta (:sel (nf x)) (count idx))))
                                        (expr (:x (nf x))))
                                  (add-meta (selection-via (an x))))))
             (expr x))]
    (apply form n (line-of n) 'inst fx (map type-form idx))))

(defn composite [n]
  (let [{:keys [type elts]} (nf n)
        l (line-of n)
        t (if type (type-form type) (at (symbol "_") nil l))
        via (into (sorted-map)
                  (keep (fn [e]
                          (when-let [v (:via (:field (an e)))]
                            [(keyword (ident-name (unparen (:key (nf e))))) (mapv symbol v)])))
                  elts)
        els (mapcat (fn [e]
                      (if (= :key-value-expr (nt e))
                        (let [{:keys [key value]} (nf e)]
                          (count-node! e)
                          (if (:field (an e))
                            (let [k (unparen key)]
                              (when-not (= :ident (nt k)) (fail "struct literal key" k))
                              (count-node! k)
                              [(lit-at (line-of k) (lit/lit (str ":" (ident-name k)) (keyword (ident-name k)))
                                       (parse-pos (start k)))
                               (expr value)])
                            [(vec-at (line-of e) [(expr key) (expr value)])]))
                        [(expr e)]))
                    elts)]
    (count-node! n)
    (-> (apply form n l 'lit t els)
        (add-meta (when (seq via) {:go/via via})))))

(declare stmt-list fn-sig-forms generic-decl?)

(defn func-lit [n]
  (count-node! n)
  (let [{:keys [type body]} (nf n)
        l (line-of n)
        has-results (seq (:list (nf (:results (nf type)))))
        _ (count-node! type)
        sig (fn-sig-forms type)
        body-forms (stmt-list body {:tail (boolean has-results)})]
    (-> (apply form n l 'fn (concat sig body-forms))
        (add-meta {:go/end (pos-line (:rbrace (nf body)))})
        (with-pos (merge {:func (:func (nf type))} (block-pos body))))))

(defn expr
  "The form of an expression node."
  [n]
  (let [a (an n)]
    (if (= :type (:mode a))
      (type-form n)
      (let [f (nf n)
            l (line-of n)]
        (case (nt n)
          :ident (ident n)
          :basic-lit (basic-lit n)
          :composite-lit (composite n)
          :func-lit (func-lit n)
          :paren-expr (do (count-node! n) (expr (:x f)))
          :selector-expr (selector-form n)
          :index-expr (do (count-node! n)
                          (if (= :type (:mode (an (unparen (:index f)))))
                            (instantiation n)
                            (form n l 'aget (expr (:x f)) (expr (:index f)))))
          :index-list-expr (do (count-node! n) (instantiation n))
          :slice-expr (let [{:keys [x low high max]} f
                            parts (cond max [low high max] high [low high] low [low] :else [])]
                        (count-node! n)
                        (apply form n l 'subslice (expr x)
                               (map #(if % (expr %) (at (symbol "_") nil l)) parts)))
          :type-assert-expr (do (count-node! n)
                                (when-not (:type f) (fail "x.(type) outside a type switch" n))
                                (form n l 'assert (type-form (:type f)) (expr (:x f))))
          :call-expr (call n)
          :star-expr (do (count-node! n)
                         (vary-meta (form n l 'deref (expr (:x f))) assoc ::deref true))
          :unary-expr (do (count-node! n)
                          (if-let [neg (let [x (unparen (:x f))]
                                         (when (and (= "-" (:op f)) (= :basic-lit (nt x))
                                                    (#{"INT" "FLOAT"} (:kind (nf x))))
                                           (when (= :paren-expr (nt (:x f))) (count-node! (:x f)))
                                           (let [v (basic-lit x)]
                                             (note! :negative-literal)
                                             (assoc v :text (str "-" (:text v)) :value (- (:value v))
                                                    :line l :pos (parse-pos (:op-pos f))))))]
                            neg
                          (if-let [op (unary-ops (:op f))]
                            (form n l op (expr (:x f)))
                            (if (= "~" (:op f)) (type-form n) (fail "unary operator" n)))))
          :binary-expr
          (let [op (or (binary-ops (:op f)) (fail "binary operator" n))]
            (count-node! n)
            (if (nary-ops op)
              (let [logical (#{'and 'or} op)
                    operand (fn [x] (if (and logical (= :paren-expr (nt x)))
                                      (let [fx (expr x)]
                                        (note! :logical-paren)
                                        (if (instance? arbace.lang.IObj fx) (add-meta fx {:go/paren true}) fx))
                                      (expr x)))
                    operands (loop [x (:x f) acc (list (operand (:y f))) nodes [n]]
                               (let [u (unparen x)]
                                 (when (and (not= x u) (not logical) (= op (binary-ops (:op (nf u))))) (count-node! x))
                                 (if (and (= :binary-expr (nt u)) (= op (binary-ops (:op (nf u))))
                                          (not (and logical (not= x u))))
                                   (do (count-node! u)
                                       (recur (:x (nf u)) (cons (operand (:y (nf u))) acc) (conj nodes u)))
                                   (cons (operand x) acc))))]
                (apply form n l op operands))
              (let [fm (form n l op (expr (:x f)) (expr (:y f)))]
                (if (and (#{'<< '>>} op) (not= :const (:mode a)) (untyped-const? (:x f))
                         (:t a) (not (ty/untyped? (:t a))))
                  (do (note! :shift-tag) (add-meta fm {:tag (ty/type-form (:t a))}))
                  fm))))
          :key-value-expr (fail "key-value outside a literal" n)
          :ellipsis (fail "ellipsis outside a parameter list" n)
          (if (#{:array-type :struct-type :func-type :interface-type :map-type :chan-type} (nt n))
            (type-form n)
            (fail "expression" n)))))))

;;; Statements (§7)

(defn targets
  "Binding or assignment targets: one form, or (values ...)."
  [lhs l mk]
  (if (= 1 (count lhs))
    (mk (first lhs))
    (apply form nil l 'values (map mk lhs))))

(defn inits [rhs l]
  (if (= 1 (count rhs))
    (expr (first rhs))
    (apply form nil (line-of (first rhs)) 'values (map expr rhs))))

(defn define-target
  "A := target: a new name, or a reused one marked ^:assign."
  [n]
  (let [s (ident n)]
    (if (and (symbol? s) (not= '_ s) (nil? (:def (an n))) (:use (an n)))
      (vary-meta s assoc :assign true)
      s)))

(defn define-binding
  "[target init] of x, y := e..."
  [st]
  (let [{:keys [lhs rhs]} (nf st)
        l (line-of st)
        t (targets lhs l define-target)]
    [(at t st l) (inits rhs l)]))

(defn const-val
  "{:val v} of a constant name's definition."
  [nm]
  (let [o (:def (an nm))]
    (when (contains? o :val)
      (when-let [v (lit/const-form (:val o) (ty/const-kind (:t o)))]
        {:val v}))))

(defn decl-bindings
  "The let bindings of a local declaration (a decl-stmt), and the kind :let or :let-type."
  [st]
  (let [gd (:decl (nf st))
        {:keys [tok specs]} (nf gd)]
    (count-node! gd)
    (case tok
      "var"
      [:let
       (vec
        (mapcat
         (fn [sp]
           (count-node! sp)
           (let [{:keys [names type values]} (nf sp)
                 l (line-of sp)
                 t (when type (type-form type))
                 mk #(tagged (ident %) t)
                 tg (at (targets names l mk) (when (= sp (first specs)) gd) l)]
             (cond
               (empty? values) [tg (form nil l 'zero (or t (fail "var without type" sp)))]
               t [tg (inits values l)]
               :else [(vary-meta tg assoc :var true) (inits values l)])))
         specs))]
      "const"
      [:let
       (loop [specs specs prev nil acc [] iota 0 specs* specs]
         (if-let [sp (first specs)]
           (let [{:keys [names type values]} (nf sp)
                 _ (count-node! sp)
                 l (line-of sp)
                 [type values implicit] (if (seq values) [type values false]
                                            [(:type prev) (:values prev) true])
                 _ (when implicit (note! :local-const-repeated))
                 t (when type (if implicit
                                (binding [*stats* nil] (relined l (type-form type)))
                                (type-form type)))
                 mk (fn [nm] (-> (ident nm) (tagged t) (vary-meta assoc :const true)
                                 (add-meta (const-val nm))))
                 tg (cond-> (targets names l mk)
                      ;; local const groups stay groups for gc's tree (ROUNDTRIP.md):
                      ;; proposed amendment
                      (and (> (count specs*) 1) (pos? iota)) (vary-meta assoc :go/grouped true)
                      implicit (vary-meta assoc :go/implicit true))
                 init (if implicit
                        (binding [*stats* nil] (relined l (inits values l)))
                        (inits values l))]
             (recur (rest specs) (if implicit prev (nf sp)) (conj acc tg init) (inc iota) specs*))
           (do (when (> (count specs*) 1) (note! :local-const-group))
               (when (> iota 1) (note! :local-const-group-split))
               acc)))]
      "type"
      [:let-type
       (vec
        (mapcat
         (fn [sp]
           (count-node! sp)
           (let [{:keys [name assign type] tparams :type-params} (nf sp)]
             (binding [*generic* (boolean (generic-decl? sp))]
               (let [tps (type-params tparams)
                     nm (cond-> (ident name)
                          assign (vary-meta assoc :alias true)
                          tps (vary-meta assoc :type-params (vec-at (line-of sp) tps)))
                     _ (when tps (note! :local-generic-type))]
                 [nm (type-form type)]))))
         specs))])))

(defn decl-stmt?
  "Whether a statement declares (it becomes a let or let-type)."
  [st]
  (case (nt st)
    :assign-stmt (= ":=" (:tok (nf st)))
    :decl-stmt true
    :labeled-stmt (decl-stmt? (:stmt (nf st)))
    false))

(defn bindings-of [st]
  (case (nt st)
    :assign-stmt (do (count-node! st) [:let (define-binding st)])
    :decl-stmt (do (count-node! st) (decl-bindings st))
    :labeled-stmt (bindings-of (:stmt (nf st)))))

(declare stmt simple-stmt generic-decl?)

(defn dir-form [[l c text]]
  (swap! *placed* conj [l c])
  (note! :directive-statement)
  (with-meta (list 'go/directive (lit/lit (lit/string-text text) text)) {::l l}))

(defn- gap-directives
  "For statements stmts in a block spanning [from to] (positions), the directives in the gaps:
  a vector of n+1 lists, gap k before statement k, gap n after the last."
  [stmts from to]
  (let [dirs (filter (fn [[l c]] (and (pos? (compare [l c] from)) (neg? (compare [l c] to)))) *fn-dirs*)]
    (if (empty? dirs)
      (vec (repeat (inc (count stmts)) nil))
      (let [ranges (mapv (fn [s] [(parse-pos (start s)) (max-pos s)]) stmts)
            inside? (fn [p] (some (fn [[a b]] (and (<= (compare a p) 0) (<= (compare p b) 0))) ranges))
            free (remove (fn [[l c]] (inside? [l c])) dirs)]
        (reduce (fn [gaps [l c :as d]]
                  (let [k (or (first (keep-indexed (fn [i [a _]] (when (pos? (compare a [l c])) i)) ranges))
                              (count stmts))]
                    (update gaps k (fnil conj []) d)))
                (vec (repeat (inc (count stmts)) nil))
                free)))))

(defn stmts-forms
  "The forms of a statement list stmts spanning the positions (from, to). Declarations become
  a let (or let-type) whose body is the rest of the list (§7.3). opts :tail: the list ends a
  function with results, so a final return of one expression is that expression (§6.4)."
  [list from to {:keys [tail]}]
  (let [stmts (vec (remove #(= :empty-stmt (nt %)) list))
        _ (doseq [s list :when (= :empty-stmt (nt s))] (count-node! s))
        gaps (gap-directives stmts from to)
        n (count stmts)]
    (letfn [(build [i]
              (if (>= i n)
                (map dir-form (get gaps n))
                (let [s (stmts i)
                      pre (map dir-form (get gaps i))]
                  (if (decl-stmt? s)
                    (let [kind-of (fn [st] (if (and (= :decl-stmt (nt st))
                                                     (= "type" (:tok (nf (:decl (nf st))))))
                                             :let-type :let))
                          kind (kind-of s)
                          [_ bs] (bindings-of s)
                          ;; L: x := e (PRINTER-NOTES A9): the label on the first target
                          bs (if (= :labeled-stmt (nt s))
                               (do (note! :labeled-declaration) (count-node! s)
                                   (update bs 0 #(vary-meta % assoc :go/label (label-kw (:label (nf s))))))
                               bs)
                          [j bs] (loop [j (inc i) bs bs]
                                   (if (and (< j n) (decl-stmt? (stmts j)) (empty? (get gaps j))
                                            (not= :labeled-stmt (nt (stmts j)))
                                            (= kind (kind-of (stmts j))))
                                     (recur (inc j) (into bs (second (bindings-of (stmts j)))))
                                     [j bs]))
                          l (line-of s)
                          lf (apply form nil l (if (= kind :let) 'let 'let-type)
                                    (vec-at l bs) (build j))]
                      (concat pre
                              [lf]))
                    (concat pre
                            (keep identity [(stmt s (and tail (= i (dec n)) (empty? (get gaps n))))])
                            (build (inc i)))))))]
      (doall (build 0)))))

(defn stmt-list
  "The forms of a block's statements (stmts-forms)."
  [block opts]
  (count-node! block)
  (let [{:keys [list lbrace rbrace]} (nf block)]
    (stmts-forms list (parse-pos lbrace) (parse-pos rbrace) opts)))

(defn block-forms
  "A nested Go block as (do ...)."
  [b]
  (apply form b (line-of b) 'do (stmt-list b {})))

(defn branch
  "A branch of if/cond (§7.2): one statement, or (do ...) for a statement list; else? (an else
  branch): a single if, when or cond is wrapped in do, which an else if is not."
  [b else?]
  (let [fs (stmt-list b {})
        l (line-of b)]
    (if (and (= 1 (count fs))
             (let [f0 (first fs)]
               (not (and (seq? f0) (or (= 'do (first f0))
                                       (and else? (#{'if 'when 'cond} (first f0))))))))
      (first fs)
      (apply form b l 'do fs))))

(defn init-vec
  "The init vector of if, switch, type-switch and for: [target expr] for a :=, [stmt] else."
  [init l]
  (cond
    (nil? init) (vec-at l [])
    (and (= :assign-stmt (nt init)) (= ":=" (:tok (nf init))))
    (do (count-node! init) (vec-at (line-of init) (define-binding init)))
    :else (vec-at (line-of init) [(simple-stmt init)])))

(defn if-form [n]
  (count-node! n)
  (let [{:keys [init body else] test :cond} (nf n)
        l (line-of n)
        chain (loop [acc [[test body]] e else]
                (if (and (= :if-stmt (nt e)) (nil? (:init (nf e))))
                  (recur (conj acc [(:cond (nf e)) (:body (nf e)) e]) (:else (nf e)))
                  [acc e]))
        [tests final] chain]
    (cond
      (nil? else)
      (-> (apply form n l 'when (concat (when init [(init-vec init l)]) [(expr test)] (stmt-list body {})))
          (with-pos (block-pos body)))

      (and (nil? init) (>= (count tests) 2) (not= :if-stmt (nt final)))
      (do (note! :cond)
          (doseq [[_ _ e] (rest tests)] (count-node! e))
          (apply form n l 'cond
                 (concat (mapcat (fn [[c b]] [(expr c) (branch b false)]) tests)
                         (when final
                           [(lit/lit ":else" :else) (branch final true)]))))

      :else
      (-> (apply form n l 'if
                 (concat (when init [(init-vec init l)])
                         [(expr test) (branch body false)
                          (if (= :if-stmt (nt else)) (if-form else) (branch else true))]))
          (with-pos (merge (block-pos body)
                           (when (= :block-stmt (nt else))
                             {:else-lbrace (:lbrace (nf else)) :else-rbrace (:rbrace (nf else))})))))))

(defn clause-bound
  "The end bound of clause i of a switch or select body: the next clause, or the }."
  [body i]
  (let [cl (:list (nf body))]
    (if (< (inc i) (count cl))
      (parse-pos (start (nth cl (inc i))))
      (parse-pos (:rbrace (nf body))))))

(defn case-clause [c to tswitch]
  (count-node! c)
  (let [{:keys [list body colon]} (nf c)
        l (line-of c)
        body-forms (stmts-forms body (parse-pos colon) to {})]
    (if (nil? list)
      (apply form c l 'default body-forms)
      (apply form c l 'case
             (vec-at (line-of (first list))
                     (map (fn [e] (if tswitch
                                    (if (= :nil (:mode (an (unparen e)))) (expr e) (type-form e))
                                    (expr e)))
                          list))
             body-forms))))

(defn clauses [body tswitch]
  (count-node! body)
  (map-indexed (fn [i c] (case-clause c (clause-bound body i) tswitch)) (:list (nf body))))

(defn comm-clause [c to]
  (count-node! c)
  (let [{:keys [comm body colon]} (nf c)
        l (line-of c)
        body-forms (stmts-forms body (parse-pos colon) to {})]
    (if (nil? comm)
      (apply form c l 'default body-forms)
      (let [cf (case (nt comm)
                 :send-stmt (stmt comm false)
                 :expr-stmt (do (count-node! comm) (expr (:x (nf comm))))
                 :assign-stmt (if (= ":=" (:tok (nf comm)))
                                (do (count-node! comm) (vec-at (line-of comm) (define-binding comm)))
                                (stmt comm false))
                 (fail "select case" comm))]
        (apply form c l 'case cf body-forms)))))

(defn assign [n]
  (let [{:keys [lhs rhs tok]} (nf n)
        l (line-of n)]
    (count-node! n)
    (cond
      (= tok "=")
      (if (and (= 1 (count lhs) (count rhs)))
        (let [p (unparen (first lhs))]
          (if (and (= :index-expr (nt p)) (not= :type (:mode (an (unparen (:index (nf p)))))))
            (do (count-node! p)
                (form n l 'aset (expr (:x (nf p))) (expr (:index (nf p))) (expr (first rhs))))
            (form n l 'set! (expr (first lhs)) (expr (first rhs)))))
        (form n l 'set! (targets lhs l expr) (inits rhs l)))
      (assign-ops tok)
      (form n l 'set! (expr (first lhs)) (assign-ops tok) (expr (first rhs)))
      :else (fail (str "assignment " tok) n))))

(defn simple-stmt
  "A simple statement in an init or post position."
  [s]
  (stmt s false))

(declare stmt*)

(defn stmt
  "The form of a statement (not a declaration). tail: the last of a function body with
  results."
  [n tail]
  (let [x (stmt* n tail)]
    (if (#{:for-stmt :range-stmt :switch-stmt :type-switch-stmt :select-stmt} (nt n))
      (with-pos x (block-pos (:body (nf n))))
      x)))

(defn stmt*
  [n tail]
  (let [f (nf n)
        l (line-of n)]
    (case (nt n)
      :expr-stmt (do (count-node! n) (expr (:x f)))
      :send-stmt (do (count-node! n) (form n l '>! (expr (:chan f)) (expr (:value f))))
      :inc-dec-stmt (do (count-node! n) (form n l (if (= "++" (:tok f)) 'inc! 'dec!) (expr (:x f))))
      :assign-stmt (assign n)
      :go-stmt (do (count-node! n) (form n l 'go (expr (:call f))))
      :defer-stmt (do (count-node! n) (form n l 'defer (expr (:call f))))
      :return-stmt (do (count-node! n)
                       (let [rs (:results f)]
                         (if (and tail (= 1 (count rs)))
                           (do (note! :implicit-return) (expr (first rs)))
                           (apply form n l 'return (map expr rs)))))
      :branch-stmt (do (count-node! n)
                       (let [head ({"break" 'break "continue" 'continue "goto" 'goto
                                    "fallthrough" 'fallthrough} (:tok f))]
                         (if (:label f)
                           (form n l head (label-kw (:label f)))
                           (form n l head))))
      :block-stmt (block-forms n)
      :if-stmt (if-form n)
      :switch-stmt (do (count-node! n)
                       (apply form n l 'switch
                              (concat (when (:init f) [(init-vec (:init f) l)])
                                      (when (:tag f) [(expr (:tag f))])
                                      (clauses (:body f) false))))
      :type-switch-stmt
      (do (count-node! n)
          (let [as (:assign f)
                guard (case (nt as)
                        :assign-stmt (let [{:keys [lhs rhs]} (nf as)
                                           ta (unparen (first rhs))]
                                       (count-node! as) (count-node! ta)
                                       (vec-at (line-of as) [(ident (first lhs)) (expr (:x (nf ta)))]))
                        :expr-stmt (let [ta (unparen (:x (nf as)))]
                                     (count-node! as) (count-node! ta)
                                     (expr (:x (nf ta)))))]
            (apply form n l 'type-switch
                   (concat (when (:init f) [(init-vec (:init f) l)])
                           [guard]
                           (clauses (:body f) true)))))
      :select-stmt (do (count-node! n) (count-node! (:body f))
                       (apply form n l 'select
                              (map-indexed (fn [i c] (comm-clause c (clause-bound (:body f) i)))
                                           (:list (nf (:body f))))))
      :for-stmt
      (do (count-node! n)
          (let [{:keys [init post body] test :cond} f
                body-forms (stmt-list body {})]
            (cond
              (and (nil? init) (nil? post) (nil? test))
              (apply form n l 'while (lit/lit "true" true) body-forms)
              (and (nil? init) (nil? post)
                   (not (let [c (unparen test)]
                          (and (= :ident (nt c)) (= "true" (ident-name c)) (:universe (obj c))))))
              (apply form n l 'while (expr test) body-forms)
              :else
              (apply form n l 'for (init-vec init l)
                     (if test (expr test) (at (symbol "_") nil l))
                     (if post (simple-stmt post) (at (symbol "_") nil l))
                     body-forms))))
      :range-stmt
      (do (count-node! n)
          (let [{:keys [key value tok x body]} f
                define (= tok ":=")
                tgt (fn [e] (if define (ident (unparen e)) (expr e)))
                v (vec-at l (concat (when key [(tgt key)]) (when value [(tgt value)]) [(expr x)]))
                fm (apply form n l 'range v (stmt-list body {}))]
            (if (= tok "=") (add-meta fm {:assign true}) fm)))
      :labeled-stmt (do (count-node! n)
                        (let [s (:stmt f)]
                          (if (= :empty-stmt (nt s))
                            (do (count-node! s) (form n l 'label (label-kw (:label f))))
                            (form n l 'label (label-kw (:label f)) (stmt s false)))))
      :empty-stmt (do (count-node! n) nil)
      :decl-stmt (fail "declaration outside a statement list" n)
      (fail "statement" n))))

;;; Functions (§6.4)

(defn fn-sig-forms
  "The signature part of fn: params, results."
  [ft]
  (signature ft))

;;; Directives (§9.1)

(defn directive-name
  "The name of a //go: directive (\"nosplit\"), else nil."
  [^String text]
  (when (str/starts-with? text "//go:")
    (let [body (subs text 5)
          i (str/index-of body " ")]
      (if i (subs body 0 i) body))))

(defn directive-args [^String text]
  (let [body (subs text 5)
        i (str/index-of body " ")]
    (when i (str/triml (subs body (inc i))))))

(def free-standing-names
  "//go: directives that gc applies by name or to the file, never to the next declaration."
  #{"build" "linknamestd" "generate"})

(defn attachable?
  "Whether the directive attaches to a declaration named decl-name that follows it: gc's
  pragmas (noder.go) other than linkname naming another declaration, cgo's and the
  file-level ones."
  [text decl-name]
  (when-let [nm (directive-name text)]
    (and (not (free-standing-names nm))
         (not (str/starts-with? nm "cgo_"))
         (or (not= nm "linkname")
             (= decl-name (first (str/split (or (directive-args text) "") #"\s+")))))))

(defn directive-meta
  "The metadata of attached directives (texts in order): {:go/name true|\"args\"|[...]}."
  [texts]
  (reduce (fn [m t]
            (let [k (keyword "go" (directive-name t))
                  v (or (directive-args t) true)]
              (if (contains? m k)
                (update m k #(if (vector? %) (conj % v) [% v]))
                (assoc m k v))))
          (sorted-map) texts))

(defn free-directive [[l _ text]]
  (note! :directive-free)
  (with-meta (list 'go/directive (lit/lit (lit/string-text text) text)) {::l l}))

;;; Declarations (§6)

(defn generic-decl?
  "A func-decl with type parameters or a receiver of a generic type, or a generic type spec."
  [d]
  (case (nt d)
    :func-decl (let [{:keys [recv type]} (nf d)]
                 (or (seq (:list (nf (:type-params (nf type)))))
                     (when recv
                       (let [t (unparen (:type (nf (first (:list (nf recv))))))
                             t (if (= :star-expr (nt t)) (unparen (:x (nf t))) t)]
                         (#{:index-expr :index-list-expr} (nt t))))))
    :type-spec (seq (:list (nf (:type-params (nf d)))))
    false))

(defn func-decl [d dmeta]
  (count-node! d)
  (binding [*generic* (boolean (generic-decl? d))]
    (let [{:keys [doc recv name type body]} (nf d)
          l (line-of d)
          docstr (doc-of doc *groups*)
          nm (-> (ident name) (add-meta dmeta) (add-meta (when-not body {:extern true})))
          tps (type-params (:type-params (nf type)))
          [ps & rs] (signature type)
          ps (if recv
               (do (with-meta (vec (concat (fields recv {}) ps))
                     (assoc (meta ps) ::l (or (pos-line (:opening (nf recv))) l))))
               ps)
          has-results (seq (:list (nf (:results (nf type)))))
          body-forms (when body (stmt-list body {:tail (boolean has-results)}))]
      (count-node! type)
      (-> (apply form d l (if recv 'go/method 'go/func) nm
                 (concat (when docstr [docstr])
                         (when tps [:type-params (vec-at l tps)])
                         [ps] rs body-forms))
          (add-meta (when body {:go/end (pos-line (:rbrace (nf body)))}))
          (with-pos (merge {:func (:func (nf type))} (block-pos body)))))))

(defn value-spec-target
  "The target of a package-level value spec: a name or (values ...); docs and directive
  metadata on the first name."
  [sp kind extra]
  (let [{:keys [names type]} (nf sp)
        l (line-of sp)
        t (when type (type-form type))
        mk (fn [i nm] (-> (ident nm) (tagged t)
                          (add-meta (when (= kind :const) (const-val nm)))
                          (add-meta (when (zero? i) extra))))]
    (if (= 1 (count names))
      (mk 0 (first names))
      (apply form nil l 'values (map-indexed mk names)))))

(defn gen-decl [d spec-meta]
  (count-node! d)
  (let [{:keys [tok specs lparen doc]} (nf d)
        l (line-of d)
        docstr (doc-of doc *groups*)
        head (symbol "go" tok)]
    (if (= tok "type")
      (let [spec (fn [i sp extra]
                   (count-node! sp)
                   (binding [*generic* (boolean (generic-decl? sp))]
                     (let [{:keys [name assign type doc] tparams :type-params} (nf sp)
                           d2 (doc-of doc *groups*)
                           nm (-> (ident name)
                                  (add-meta (when assign {:alias true}))
                                  (add-meta (spec-meta i))
                                  (add-meta (when d2 {:doc d2}))
                                  (add-meta extra))
                           tps (type-params tparams)]
                       (concat [nm] (when tps [:type-params (vec-at (line-of sp) tps)]) [(type-form type)]))))]
        (if lparen
          (apply form d l head (concat (when docstr [docstr])
                                       (map-indexed (fn [i sp] (vec-at (line-of sp) (spec i sp nil))) specs)))
          (let [[nm & more] (spec 0 (first specs) nil)]
            (apply form d l head nm (concat (when docstr [docstr]) more)))))
      (let [kind (keyword tok)
            spec-vec (fn [i sp]
                       (count-node! sp)
                       (let [{:keys [values doc]} (nf sp)
                             d2 (doc-of doc *groups*)
                             tg (value-spec-target sp kind (merge (spec-meta i) (when d2 {:doc d2})))]
                         (vec-at (line-of sp) (concat [tg] (when (seq values) [(inits values (line-of sp))])))))]
        (if lparen
          (apply form d l head (concat (when docstr [docstr]) (map-indexed spec-vec specs)))
          (let [sp (first specs)
                {:keys [values]} (nf sp)
                _ (count-node! sp)
                tg (value-spec-target sp kind (merge (spec-meta 0) (when docstr {:doc docstr})))]
            (apply form d l head tg (when (seq values) [(inits values (line-of sp))]))))))))

;;; Files (§4.3)

(defn file-directives
  "The dump's directives of file name: [[line col text] ...]."
  [dump name]
  (let [prefix (str name ":")]
    (vec (for [[p text] (:directives dump)
               :when (str/starts-with? p prefix)
               :let [[l c] (parse-pos (subs p (count prefix)))]
               ;; gc reads //line only at the start of a line (cmd/compile/doc.go): elsewhere
               ;; it is an ordinary comment
               :when (not (and (str/starts-with? text "//line ") (not= c 1)))]
           [l c text]))))

(defn decl-name
  "The name a directive before declaration d (or spec) would attach to."
  [d]
  (case (nt d)
    :func-decl (ident-name (:name (nf d)))
    :value-spec (ident-name (first (:names (nf d))))
    :type-spec (ident-name (:name (nf d)))
    nil))

(defn imports-of [decls]
  (doseq [d decls :when (and (= :gen-decl (nt d)) (= "import" (:tok (nf d))))] (count-node! d))
  (vec
   (for [d decls
         :when (and (= :gen-decl (nt d)) (= "import" (:tok (nf d))))
         sp (:specs (nf d))]
     (let [{:keys [name path]} (nf sp)
           l (line-of sp)
           p (string-value path)
           pv (lit/lit (lit/string-text p) p)]
       (count-node! sp) (count-node! path)
       (cond
         (nil? name) (vec-at l [(symbol (:name (:implicit (an sp)))) pv])
         (#{"_" "."} (ident-name name)) (do (count-node! name) (vec-at l [(symbol (ident-name name)) pv]))
         :else (do (count-node! name) (vary-meta (vec-at l [(symbol (ident-name name)) pv]) assoc :alias true)))))))

(defn convert-file
  "The forms of one Go file of the dump: {:name :forms [...] :comments {line [text]}
  :unplaced [...]} (forms in order: in-ns, go/file, declarations and free-standing
  directives)."
  [dump fe ns-sym]
  (let [ast (:ast fe)
        f (nf ast)
        name (:file fe)
        groups (into {} (for [g (:comments fe)] [(first (first (:list g))) g]))
        pkg-pos (parse-pos (:package f))
        pkg-line (first pkg-pos)
        decls (:decls f)
        dirs (file-directives dump name)
        ranges (mapv (fn [d] [(parse-pos (start d)) (max-pos d)]) decls)
        kept (atom #{})
        placed (atom #{})
        header (filter (fn [[l c]] (neg? (compare [l c] pkg-pos))) dirs)
        body-dirs (remove (fn [[l c]] (neg? (compare [l c] pkg-pos))) dirs)
        last-import (last (keep-indexed (fn [i d] (when (and (= :gen-decl (nt d)) (= "import" (:tok (nf d))))
                                                    (second (ranges i))))
                                        decls))
        inside-of (fn [[l c]]
                    (first (keep-indexed (fn [i [a b]] (when (and (<= (compare a [l c]) 0)
                                                                  (<= (compare [l c] b) 0)) i))
                                         ranges)))
        ;; attachment: directive -> [decl-index spec-index] or :free (placed before decl k)
        plan (reduce
              (fn [acc [l c text :as dv]]
                (if-let [k (inside-of [l c])]
                  (let [d (nth decls k)]
                    (if (and (= :gen-decl (nt d)) (:lparen (nf d)))
                      (let [specs (:specs (nf d))
                            si (first (keep-indexed (fn [i sp] (when (pos? (compare (parse-pos (start sp)) [l c])) i)) specs))]
                        (if (and si (attachable? text (decl-name (nth specs si))))
                          (update acc :attach update [k si] (fnil conj []) dv)
                          (update acc :inner conj dv)))
                      (update acc :inner conj dv)))
                  (let [k (first (keep-indexed (fn [i [a _]] (when (pos? (compare a [l c])) i)) ranges))
                        d (when k (nth decls k))
                        target (when d
                                 (cond
                                   (= :func-decl (nt d)) d
                                   (and (= :gen-decl (nt d)) (not (:lparen (nf d)))
                                        (not= "import" (:tok (nf d))))
                                   (first (:specs (nf d)))))]
                    (cond
                      (and target (attachable? text (decl-name target)))
                      (update acc :attach update [k 0] (fnil conj []) dv)
                      (and last-import (neg? (compare [l c] last-import)))
                      (update acc :in-imports conj dv)
                      :else
                      (update acc :free update (or k (count decls)) (fnil conj []) dv)))))
              {:attach {} :free {} :inner [] :in-imports []}
              body-dirs)
        attach-meta (fn [k si]
                      (when-let [ds (get-in plan [:attach [k si]])]
                        (swap! placed into (map (fn [[l c]] [l c]) ds))
                        (note! :directive-attached)
                        (directive-meta (map #(nth % 2) ds))))
        imports (vec (sort-by (fn [x] [(line x) (if (seq? x) 0 1)])
                              (concat (imports-of decls)
                                      (map (fn [dv] (swap! placed conj (vec (take 2 dv))) (free-directive dv))
                                           (:in-imports plan)))))
        doc (doc-of (:doc f) groups)
        file-form (binding [*kept-docs* kept]
                    (apply form nil pkg-line 'go/file (lit/lit (lit/string-text name) name)
                           (concat
                            (when (not= (ident-name (:name f)) (:name dump))
                              [:package (symbol (ident-name (:name f)))])
                            (let [b (filter #(or (str/starts-with? (nth % 2) "//go:build")
                                                 (str/starts-with? (nth % 2) "// +build")) header)]
                              (when (seq b) [:build (vec (map #(lit/lit (lit/string-text (nth % 2)) (nth % 2)) b))]))
                            (let [o (remove #(or (str/starts-with? (nth % 2) "//go:build")
                                                 (str/starts-with? (nth % 2) "// +build")) header)]
                              (when (seq o) [:directives (vec (map #(lit/lit (lit/string-text (nth % 2)) (nth % 2)) o))]))
                            (when-let [v (:go-version fe)] [:lang (lit/lit (lit/string-text v) v)])
                            (when doc [:doc doc])
                            [:imports (vec-at pkg-line imports)])))
        _ (swap! placed into (map (fn [[l c]] [l c]) header))
        decl-forms (binding [*groups* groups
                             *kept-docs* kept
                             *placed* placed
                             *fn-dirs* (:inner plan)]
                     (doall
                      (concat
                       (mapcat
                        (fn [k d]
                          (concat
                           (map (fn [dv] (swap! placed conj (vec (take 2 dv))) (free-directive dv))
                                (get-in plan [:free k]))
                           (case (nt d)
                             :func-decl [(func-decl d (attach-meta k 0))]
                             :gen-decl (if (= "import" (:tok (nf d)))
                                         []
                                         [(gen-decl d (fn [si] (attach-meta k si)))])
                             (fail "declaration" d))))
                        (range) decls)
                       (map (fn [dv] (swap! placed conj (vec (take 2 dv))) (free-directive dv))
                            (get-in plan [:free (count decls)])))))
        unplaced (remove (fn [[l c]] (@placed [l c])) dirs)
        comments (reduce
                  (fn [m g]
                    (if (@kept (first (first (:list g))))
                      m
                      (reduce (fn [m [p text]]
                                (let [[l c] (parse-pos p)]
                                  (if (some (fn [[dl dc]] (and (= dl l) (= dc c))) dirs)
                                    m
                                    (let [lines (str/split-lines (str/replace text "\r" ""))]
                                      (reduce (fn [m [i t]] (update m (+ l i) (fnil conj []) t))
                                              m (map-indexed vector lines))))))
                              m (:list g))))
                  (sorted-map) (:comments fe))]
    (count-node! ast)
    (count-node! (:name f))
    (when (seq unplaced) (note! :directive-unplaced))
    {:name name
     :forms (concat [(with-meta (list 'in-ns (list 'quote ns-sym)) {::l pkg-line ::raw (str "(in-ns '" ns-sym ")")})
                     file-form]
                    decl-forms)
     :comments comments
     :unplaced (vec unplaced)}))

;;; Packages (§4.1, §4.2)

(defn init-order [dump]
  (vec
   (for [{:keys [lhs]} (:init-order dump)]
     (let [one (fn [[nm p]]
                 (if (= nm "_")
                   (let [[_ file l c] (re-matches #"(.*):(\d+):(\d+)" p)]
                     [file (Long/parseLong l) (Long/parseLong c)])
                   (symbol nm)))]
       (if (= 1 (count lhs))
         (one (first lhs))
         (apply list 'values (map one lhs)))))))

(defn- munge-name [s] (str/replace s #"[.-]" "_"))

(defn package-form [dump {:keys [positions]}]
  (let [c (:config dump)
        files (vec (:go-files dump))
        others (vec (sort (mapcat #(get dump %) [:s-files :h-files :syso-files :c-files :cxx-files
                                                 :m-files :f-files :swig-files :swig-cxx-files])))
        embeds (vec (concat (:embed-files dump) (:test-embed-files dump)))]
    (apply list 'go/package (symbol (if (empty? (:name dump))
                                       (munge-name (last (str/split (:package dump) #"/")))
                                       (:name dump)))
           (concat
            [:path (:package dump)
             :config (array-map :goos (:goos c) :goarch (:goarch c)
                                (if (= "amd64" (:goarch c)) :goamd64 :goarm64)
                                (or (:goamd64 c) (:goarm64 c))
                                :toolchain (:toolchain c) :lang (:go-version c) :tags (vec (:tags c))
                                :goexperiment (:goexperiment c) :cgo (:cgo c) :compiler (:compiler c))
             :files files]
            (when (seq others) [:other-files others])
            (when (seq embeds) [:embed-files (vec (map vec embeds))])
            (when (seq (:test-go-files dump)) [:test-files (vec (:test-go-files dump))])
            [:init-order (init-order dump)
             :positions positions]))))

(defn file-stem
  "The .clj path stem of Go file name in package directory dir (a Go source directory): x for
  x.go, or x.go when dir has a subdirectory x (a package of that name would own x.clj:
  proposed amendment)."
  [name src-dir]
  (let [stem (subs name 0 (- (count name) 3))]
    (if (and src-dir (.isDirectory (java.io.File. (str src-dir "/" stem))))
      (do (note! :file-name-collision) name)
      stem)))

(defn convert-package
  "Converts a dump. opts: :positions (:lines or :full), :ns (the namespace symbol, default from
  the import path), :src-dir (the package's source directory, for file-name collisions).
  Returns {:ns sym :package form :files [{:name :stem :forms :comments :unplaced}] :stats}."
  [dump opts]
  (let [stats (atom {})
        ns-sym (or (:ns opts) (ty/ns-name-of (:package dump)))
        underlying (into {} (for [t (:types dump) :when (:underlying t)] [(:t t) (:underlying t)]))]
    (binding [*dump* dump
              *stats* stats
              ty/*table* (:type-table dump)
              ty/*pkg* (:package dump)
              ty/*underlying* underlying]
      (let [files (vec (for [fe (:files dump)]
                         (binding [ty/*imports*
                                   (into {} (for [d (:decls (nf (:ast fe)))
                                                  :when (and (= :gen-decl (nt d)) (= "import" (:tok (nf d))))
                                                  sp (:specs (nf d))
                                                  :let [nm (if-let [n (:name (nf sp))] (ident-name n) (:name (:implicit (an sp))))]
                                                  :when (not (#{"_" "."} nm))]
                                              [(string-value (:path (nf sp))) nm]))]
                           (assoc (convert-file dump fe ns-sym)
                                  :stem (file-stem (:file fe) (:src-dir opts))))))]
        {:ns ns-sym
         :package (package-form dump opts)
         :files files
         :stats @stats}))))
