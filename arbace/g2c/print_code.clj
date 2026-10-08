(ns arbace.g2c.print-code
  "g2c's printer, the forms level (doc/go/PRINTER-NOTES.md): Go forms (doc/go/SPEC.md) to Go
  tokens through arbace.g2c.print-emit. Types (§5), declarations (§6), statements and
  expressions (§7), literals (§8), directives and doc comments (§9, §10.4), files (§4.3).

  Every form is classified by its head and position (§4.4); nothing is resolved. Spacing
  follows go/printer (go1.27.1 src/go/printer/nodes.go: binaryExpr, cutoff, walkBinary,
  the depth rules of exprList, controlClause, funcBody, fieldList, valueSpec, keepTypeColumn)
  so that gofmt leaves the output as it is."
  (:require [arbace.g2c.print-emit :as e :refer [tok sp vtab]]
            [arbace.g2c.print-text :as text]
            [arbace.string :as str]))

(declare expr1 expr0 type-expr stmt-list simple-stmt block-body print-stmt stmt-list-one-line)

(def ^:dynamic *go-ns*
  "The namespace qualifiers that name the Go forms' own heads (go/func, go/call, ...)."
  #{"go" "arbace.go"})

(def ^:dynamic *header*
  "True while printing the header of an if, for or switch outside brackets: a composite
  literal of a type name needs parentheses there (§12.2)."
  false)

(def ^:dynamic *generic*
  "True inside a generic declaration: field groups are kept (doc/go/ROUNDTRIP.md, known gaps)."
  false)

(defn- fail [msg form]
  (throw (ex-info (str msg ": " (pr-str form)) {:form form :line (:line (meta form))})))

;; ---------------------------------------------------------------------------------------
;; Classification (§4.4, §13.5)

(defn op
  "The name of a list's head as the forms read it: an unqualified symbol's name,
  \"go/name\" for the Go forms' qualified heads, \"deref\" for @; nil otherwise (a call of a
  package member, of a list)."
  [f]
  (when (seq? f)
    (let [h (first f)]
      (when (symbol? h)
        (let [n (namespace h)]
          (cond
            (nil? n) (name h)
            (*go-ns* n) (str "go/" (name h))
            (and (= (name h) "deref") (#{"arbace.core" "clojure.core"} n)) "deref"
            :else nil))))))

(def stmt-heads
  #{"let" "let-type" "set!" "aset" "inc!" "dec!" ">!" "go" "defer" "return" "break" "continue"
    "goto" "fallthrough" "label" "do" "if" "when" "cond" "switch" "type-switch" "select" "for"
    "while" "range" "go/directive"})

(def binary-ops
  {"*" ["*" 5] "/" ["/" 5] "%" ["%" 5] "<<" ["<<" 5] ">>" [">>" 5] "bit-and" ["&" 5]
   "bit-and-not" ["&^" 5] "+" ["+" 4] "-" ["-" 4] "bit-or" ["|" 4] "bit-xor" ["^" 4]
   "==" ["==" 3] "!=" ["!=" 3] "<" ["<" 3] "<=" ["<=" 3] ">" [">" 3] ">=" [">=" 3]
   "and" ["&&" 2] "or" ["||" 1]})

(def unary-ops {"-" "-" "+" "+" "bit-not" "^" "not" "!" "addr" "&" "deref" "*" "<!" "<-"})

(def assign-ops
  {"+" "+=" "-" "-=" "*" "*=" "/" "/=" "%" "%=" "<<" "<<=" ">>" ">>=" "bit-and" "&="
   "bit-or" "|=" "bit-xor" "^=" "bit-and-not" "&^="})

(def type-heads #{"*" "slice" "array" "map" "chan" "func" "struct" "interface" "|" "tilde" "inst"})

(defn- args [f] (rest f))

(defn binary?
  "A binary operator form: two or more operands (n-ary forms are left-nested chains)."
  [f]
  (and (seq? f) (contains? binary-ops (op f)) (>= (count f) 3)))

(defn- negative-literal? [f] (and (number? f) (text/negative-number? f)))

(defn unary?
  "A unary operator form (Go's UnaryExpr or StarExpr), or a negative literal."
  [f]
  (or (negative-literal? f)
      (and (seq? f) (contains? unary-ops (op f)) (= (count f) 2))))

(defn- unary-op [f] (if (negative-literal? f) "-" (unary-ops (op f))))

(defn- unary-operand [f]
  (if (negative-literal? f)
    (if (instance? Double f) (Math/abs (double f)) (- f))
    (second f)))

(defn- bin-op [f] (first (binary-ops (op f))))
(defn- bin-prec [f] (second (binary-ops (op f))))

(defn- bin-x
  "The left operand: (+ a b c) is (+ (+ a b) c)."
  [f]
  (if (> (count f) 3) (with-meta (apply list (butlast f)) (meta f)) (second f)))

(defn- bin-y [f] (last f))

(defn prec-of
  "The precedence of the Go expression a form prints as: 1-5 binary, 6 unary, 7 primary."
  [f]
  (cond (binary? f) (bin-prec f) (unary? f) 6 :else 7))

(defn- go-sym? [f] (= (op f) "go/id"))

(defn- statement-form? [f]
  (and (seq? f) (contains? stmt-heads (op f))))

(defn expression-form?
  "An expression form other than a call of panic: the implicit return of §6.4."
  [f]
  (and (not (statement-form? f))
       (not (and (seq? f) (= (op f) "panic")))
       (not (keyword? f)) (not (vector? f))))

(defn- line-of [f] (e/target f))

(defn- breaks-of [f] (set (:go/breaks (meta f))))

(defn- child-target
  "The recorded line of child i of a list: its own, or a new line by the list's :go/breaks."
  [parent i child]
  (or (line-of child)
      (when (and (e/lines?) (contains? (breaks-of parent) i)) :break)))

(defn- room? [n] (e/room? n))

(defn- body-offset
  "The index in f of the first of its trailing body forms."
  [f body] (- (count f) (count body)))

(defn- next-line
  "The first recorded line among forms, else *next*."
  [forms]
  (or (some line-of forms) e/*next*))

;; ---------------------------------------------------------------------------------------
;; Names and literals

(defn ident-string
  "The Go spelling of a name: a symbol, pkg/Name, (go/id \"x\")."
  [f]
  (cond
    (symbol? f) (if-let [n (namespace f)] (str n "." (name f)) (name f))
    (go-sym? f) (str (second f))
    :else (fail "not a name" f)))

(defn- label-name [k]
  (if (keyword? k) (name k) (fail "not a label" k)))

(defn- literal [f]
  (cond
    (nil? f) (tok "nil")
    (true? f) (tok "true")
    (false? f) (tok "false")
    (string? f) (let [nl? (str/includes? f "\n")]
                  (if (and nl? (text/raw-string-ok? f)
                           (e/room? (count (filter #(= % \newline) f))))
                    (e/raw-tok (str "`" f "`"))
                    (tok (text/go-string f))))
    (char? f) (tok (text/go-rune (long (int f))))
    (number? f) (tok (text/go-number f))
    :else (fail "not a literal" f)))

;; ---------------------------------------------------------------------------------------
;; Lists of elements: arguments, literal elements, parameters

(defn- element-list
  "Prints xs between open and close, separated by commas, each by (print-one x i); keeps
  the recorded lines of the elements, and puts close on a line of its own after a trailing
  comma when the elements span lines and there is room. parent and offset give the
  elements' indexes in their parent list (for :go/breaks)."
  [open close xs print-one {:keys [parent offset end] :or {offset 1}}]
  (tok open)
  (e/push-open!)
  (let [start (e/line)
        xs (vec xs)
        n (count xs)]
    (dotimes [i n]
      (let [x (nth xs i)
            t (if parent (child-target parent (+ i offset) x) (line-of x))]
        (when (pos? i) (tok ",") (sp))
        (e/break-to! t)
        (binding [e/*next* (next-line (subvec xs (inc i)))]
          (print-one x i))))
    (let [indent (e/pop-open!)
          multi? (> (e/line) start)]
      (when (and (e/lines?) (pos? n)
                 (or (and (integer? end) (> (long end) (e/line)))
                     (and (nil? end) multi? (e/room? 1))))
        (tok ","))
      (e/close-break! end multi? false indent)
      (tok close))))

;; ---------------------------------------------------------------------------------------
;; Types (§5)

(defn- tagged? [x] (and (symbol? x) (contains? (meta x) :tag)))

(defn- param-entries
  "The entries of a parameter, result or field vector: {:name sym-or-nil :type T
  :variadic? bool :meta m :src x}. A symbol with :tag is a name, anything else a type."
  [v]
  (loop [xs (seq v) acc [] variadic false]
    (if-not xs
      acc
      (let [x (first xs)]
        (if (= x '&)
          (recur (next xs) acc true)
          (recur (next xs)
                 (conj acc (if (tagged? x)
                             {:name x :type (:tag (meta x)) :variadic? variadic :meta (meta x) :src x}
                             {:name nil :type x :variadic? variadic :meta (meta x) :src x}))
                 variadic))))))

(defn- groupable? [prev cur]
  (and prev (:name prev) (:name cur)
       (not (:variadic? prev)) (not (:variadic? cur))
       (= (:type prev) (:type cur))
       (not (:doc (:meta cur))) (not (:go/tag (:meta cur)))
       (not (:doc (:meta prev))) (not (:go/tag (:meta prev)))
       (let [t (line-of (:src cur))] (or (nil? t) (= t (line-of (:src prev))) (nil? (line-of (:src prev)))))))

(defn group-entries
  "Consecutive named entries of equal type (and no doc or tag, on one line) as one group
  {:names [...] ...}: Go's `a, b T`. Inside generic declarations the grouping must be the
  source's; this is it when the source grouped what it could (PRINTER-NOTES)."
  [entries]
  (reduce (fn [groups cur]
            (let [prev (peek groups)]
              (if (and prev (groupable? (peek (:entries prev)) cur))
                (conj (pop groups) (-> prev
                                       (update :names conj (:name cur))
                                       (update :entries conj cur)))
                (conj groups (assoc cur :names (if (:name cur) [(:name cur)] []) :entries [cur])))))
          [] entries))

(defn- variadic-elem [t]
  (if (and (seq? t) (= (op t) "slice")) (second t) (fail "variadic parameter not a slice" t)))

(defn- param-list
  "(a, b int, c ...T): the parameters of a vector."
  [v]
  (let [groups (group-entries (param-entries v))]
    (element-list "(" ")" groups
                  (fn [g _]
                    (when (seq (:names g))
                      (doseq [[i nm] (map-indexed vector (:names g))]
                        (when (pos? i) (tok ",") (sp))
                        (tok (ident-string nm)))
                      (sp))
                    (if (:variadic? g)
                      (do (tok "...") (type-expr (variadic-elem (:type g))))
                      (type-expr (:type g))))
                  {})))

(defn- results-entries
  "The results of a signature: a vector's :tag (one unnamed result) or :results."
  [params results]
  (cond
    results (param-entries results)
    (contains? (meta params) :tag) [{:name nil :type (:tag (meta params))}]
    :else []))

(defn- print-results [entries]
  (cond
    (empty? entries) nil
    (and (= 1 (count entries)) (nil? (:name (first entries))))
    (do (sp) (type-expr (:type (first entries))))
    :else (do (sp) (param-list (vec (map (fn [en] (or (:src en) (:type en))) entries))))))

(defn- func-type-sig
  "(params) results of (func [params] [results]): the results vector holds names or types."
  [params results]
  (param-list params)
  (print-results (if results (param-entries results) [])))

(defn- struct-tag [s]
  (if (and (string? s) (not (str/includes? s "\n")) (text/raw-string-ok? s))
    (tok (str "`" s "`"))
    (tok (text/go-string s))))

(defn- doc-lines [doc]
  (let [ls (str/split (str doc) #"\n" -1)
        ls (if (= "" (peek ls)) (pop ls) ls)]
    ls))

(defn write-doc
  "A doc comment (CommentGroup.Text) as // lines."
  [doc indent]
  (when (and doc (not= doc ""))
    (doseq [l (doc-lines doc)]
      (e/comment-line (cond (= l "") "//" (str/starts-with? l "\t") (str "//" l) :else (str "// " l))
                      indent))))

(def ^:private non-directive-keys
  #{:go/tag :go/via :go/breaks :go/end :go/pos :go/apos :go/inst})

(defn directives-of
  "The directive lines of a declared name's metadata (§9.1): :go/<name> true or text (a
  vector for several), sorted by name."
  [m]
  (for [[k v] (sort-by (comp name key) (filter (fn [[k _]] (and (keyword? k) (= (namespace k) "go")
                                                                (not (non-directive-keys k))))
                                               m))
        v (if (vector? v) v [v])]
    (if (true? v) (str "//go:" (name k)) (str "//go:" (name k) " " v))))

(defn pre-lines
  "The number of comment lines before a declaration: doc, a // line between doc and
  directives (gofmt's doc comment format), directives."
  [doc dirs]
  (let [d (if (and doc (not= doc "")) (count (doc-lines doc)) 0)]
    (+ d (if (and (pos? d) (seq dirs)) 1 0) (count dirs))))

(defn write-pre [doc dirs indent]
  (write-doc doc indent)
  (when (and doc (not= doc "") (seq dirs)) (e/comment-line "//" indent))
  (doseq [d dirs] (e/comment-line d indent)))

(defn- interface-method? [x]
  (and (seq? x) (symbol? (first x)) (nil? (namespace (first x)))
       (vector? (second x)) (not= (op x) "func")))

(defn- iface-method-sig
  "(params) results of an interface method or declaration: [params] then :results or the
  vector's tag."
  [params more]
  (let [results (when (= (first more) :results) (second more))]
    (param-list params)
    (print-results (results-entries params results))))

(defn- one-line-fields?
  "go/printer's isOneLineFieldList: one field (group), small, without tag or doc."
  [groups struct?]
  (and (= 1 (count groups))
       (let [g (first groups)]
         (and (not (:go/tag (:meta g))) (not (:doc (:meta g)))
              (if struct?
                (<= (+ (reduce + (map #(count (name %)) (:names g))) 10) 30)
                true)))))

(defn- struct-fields [fs]
  (group-entries (for [f fs]
                   (if (tagged? f)
                     {:name f :type (:tag (meta f)) :meta (meta f) :src f}
                     {:name nil :type f :meta (meta f) :src f}))))

(defn- field-body
  "One struct field (group) or interface element, with its separators."
  [g struct? sep]
  (if struct?
    (do
      (if (seq (:names g))
        (do (doseq [[i nm] (map-indexed vector (:names g))]
              (when (pos? i) (tok ",") (sp))
              (tok (ident-string nm)))
            (sep)
            (type-expr (:type g)))
        (type-expr (:type g)))
      (when-let [t (:go/tag (:meta g))]
        (when (and (seq (:names g)) (= sep vtab)) (sep))
        (sep)
        (struct-tag t)))
    (let [x (:src g)]
      (if (interface-method? x)
        (do (tok (ident-string (first x))) (iface-method-sig (second x) (nnext x)))
        (type-expr x)))))

(defn- field-list
  "struct{...} or interface{...} (go/printer's fieldList)."
  [kw fs struct?]
  (let [groups (if struct?
                 (struct-fields fs)
                 (vec (for [x fs] {:src x :meta (meta (if (interface-method? x) (first x) x))})))
        start (e/line)
        ts (mapv (fn [g] (or (line-of (:src g)) (line-of (first (:names g))))) groups)
        breaks? (and (e/lines?) (some #(and % (> (long %) start)) ts))
        docs? (some (fn [g] (:doc (:meta g))) groups)]
    (cond
      (empty? groups) (tok (str kw "{}"))
      (and (not breaks?) (not docs?) (or (e/lines?) (one-line-fields? groups struct?)))
      (do (tok (str kw "{")) (sp)
          (doseq [[i g] (map-indexed vector groups)]
            (when (pos? i) (tok ";") (sp))
            (field-body g struct? sp))
          (sp) (tok "}"))
      :else
      (let [indent (if (e/bol?) @(:indent e/*p*) (e/cur-indent))
            sep (if (and struct? (= 1 (count groups))) sp vtab)
            last-start (volatile! (e/line))]
        (tok kw) (sp) (tok "{")
        (e/push-open! indent)
        (dotimes [i (count groups)]
          (let [g (nth groups i)
                doc (:doc (:meta g))
                pre (pre-lines doc [])
                t (nth ts i)]
            (binding [e/*next* (next-line (map :src (subvec groups (inc i))))]
              (cond
                (not (e/lines?)) (e/nl (inc indent) (or (zero? i) (> (e/line) @last-start)))
                (and (integer? t) (pos? pre)) (e/stmt-break! (max (- (long t) pre) (inc (e/line))) false ";" (inc indent))
                (pos? pre) (e/stmt-break! :break false ";" (inc indent))
                :else (e/stmt-break! t (zero? i) ";" (inc indent)))
              (when (pos? pre)
                (write-doc doc (inc indent))
                (when (and (e/lines?) (integer? t)) (e/stmt-break! t true nil (inc indent))))
              (vreset! last-start (e/line))
              (field-body g struct? sep))))
        (e/pop-open!)
        (if (e/lines?)
          (when-not (e/close-break! nil true true indent) (sp))
          (e/nl indent true))
        (tok "}")))))

(defn- chan-type [f]
  (let [[_ a b] f
        dir (when (keyword? a) a)
        elem (if dir b a)]
    (case dir
      :recv (do (tok "<-") (tok "chan"))
      :send (do (tok "chan") (tok "<-"))
      nil (tok "chan")
      (fail "bad channel direction" f))
    (sp)
    (if (and (not= dir :recv) (seq? elem) (= (op elem) "chan") (= (second elem) :recv) (nil? dir))
      (do (tok "(") (type-expr elem) (tok ")"))
      (type-expr elem))))

(defn- type-list
  "Type arguments [A, B]."
  [ts]
  (element-list "[" "]" ts (fn [t _] (type-expr t)) {}))

(defn type-expr
  "Prints a type form (§5.1)."
  [f]
  (e/break-to! (line-of f))
  (cond
    (symbol? f) (tok (ident-string f))
    (seq? f)
    (let [o (op f)]
      (case o
        "*" (do (tok "*") (type-expr (second f)))
        "slice" (do (tok "[") (tok "]") (type-expr (second f)))
        "array" (let [[_ n t] f]
                  (tok "[")
                  (if (= n '...) (tok "...") (binding [*header* false] (expr0 n 1)))
                  (tok "]")
                  (type-expr t))
        "map" (let [[_ k v] f]
                (tok "map") (tok "[") (type-expr k) (tok "]") (type-expr v))
        "chan" (chan-type f)
        "func" (let [[_ params results] f]
                 (tok "func") (func-type-sig params results))
        "struct" (field-list "struct" (args f) true)
        "interface" (field-list "interface" (args f) false)
        "|" (doseq [[i t] (map-indexed vector (args f))]
              (when (pos? i) (sp) (tok "|") (sp))
              (type-expr t))
        "tilde" (do (tok "~") (type-expr (second f)))
        "inst" (do (type-expr (second f)) (type-list (nnext f)))
        "go/id" (tok (ident-string f))
        ;; a generic type's instance (G T ...)
        (if (or (symbol? (first f)) (go-sym? (first f)))
          (do (tok (ident-string (first f))) (type-list (rest f)))
          (fail "not a type" f))))
    :else (fail "not a type" f)))

(defn- type-form?
  "A form that prints as a type in an operand position: reserved type heads (other than
  * with two or more operands)."
  [f]
  (and (seq? f)
       (let [o (op f)]
         (and (contains? type-heads o)
              (or (not= o "*") (= 2 (count f)))))))

(defn- begins-with-star-or-paren? [f]
  (or (and (seq? f) (= (op f) "*"))
      (and (seq? f) (= (op f) "|") (begins-with-star-or-paren? (second f)))))

(defn- type-params
  "[P C, Q D] after a name; a single parameter whose constraint begins with * gets a trailing
  comma (§12.2)."
  [tps]
  (let [entries (map (fn [p]
                       {:name p :type (if (contains? (meta p) :tag) (:tag (meta p)) 'any)
                        :meta (meta p) :src p})
                     tps)
        groups (group-entries entries)]
    (tok "[")
    (doseq [[i g] (map-indexed vector groups)]
      (when (pos? i) (tok ",") (sp))
      (doseq [[j nm] (map-indexed vector (:names g))]
        (when (pos? j) (tok ",") (sp))
        (tok (ident-string nm)))
      (sp)
      (type-expr (:type g)))
    (when (and (= 1 (count groups)) (= 1 (count (:names (first groups))))
               (begins-with-star-or-paren? (:type (first groups))))
      (tok ","))
    (tok "]")))

;; ---------------------------------------------------------------------------------------
;; Expressions (§7.5-7.7)

(defn- walk-binary
  "go/printer's walkBinary: [has4 has5 max-problem]."
  [f]
  (let [prec (bin-prec f)
        has4 (= prec 4) has5 (= prec 5)
        l (bin-x f) r (bin-y f)
        [h4 h5 mp] (if (and (binary? l) (>= (bin-prec l) prec)) (walk-binary l) [false false 0])
        [has4 has5 mp] [(or has4 h4) (or has5 h5) mp]
        [rh4 rh5 rmp] (cond
                        (binary? r) (if (> (bin-prec r) prec) (walk-binary r) [false false 0])
                        (and (seq? r) (= (op r) "deref") (= 2 (count r)))
                        [false false (if (= (bin-op f) "/") 5 0)]
                        (unary? r)
                        (let [s (str (bin-op f) (unary-op r))]
                          [false false (cond (#{"/*" "&&" "&^"} s) 5 (#{"++" "--"} s) 4 :else 0)])
                        :else [false false 0])]
    [(or has4 rh4) (or has5 rh5) (max mp rmp)]))

(defn- cutoff [f depth]
  (let [[has4 has5 mp] (walk-binary f)]
    (cond
      (pos? mp) (inc mp)
      (and has4 has5) (if (= depth 1) 5 4)
      (= depth 1) 6
      :else 4)))

(defn- diff-prec [x prec]
  (if (and (binary? x) (= prec (bin-prec x))) 0 1))

(defn- reduce-depth [d] (max 1 (dec d)))

(defn- binary-expr [f prec1 cut depth]
  (let [prec (bin-prec f)]
    (if (< prec prec1)
      (do (tok "(")
          (binding [*header* false] (expr0 f (reduce-depth depth)))
          (tok ")"))
      (let [blank? (< prec cut)
            x (bin-x f) y (bin-y f)]
        (expr1 x prec (+ depth (diff-prec x prec)))
        (when blank? (sp))
        (tok (bin-op f))
        (let [yt (child-target f (dec (count f)) y)]
          (if (and yt (or (= yt :break) (> (long yt) (e/line))))
            (e/break-to! yt)
            (when blank? (sp))))
        (expr1 y (inc prec) (inc depth))))))

(defn- call-args
  "(args) of a call; a final (spread xs) is xs...."
  [f xs depth offset]
  (element-list "(" ")" xs
                (fn [x _]
                  (if (and (seq? x) (= (op x) "spread"))
                    (do (expr0 (second x) depth) (tok "..."))
                    (expr0 x depth)))
                {:parent f :offset offset}))

(defn- callee [h depth]
  (cond
    (and (seq? h) (= (op h) "func")) (do (tok "(") (type-expr h) (tok ")"))
    :else (expr1 h 7 depth)))

(defn- lit-type-needs-parens? [t]
  (or (symbol? t) (go-sym? t)
      (and (seq? t) (not (contains? type-heads (op t))))
      (and (seq? t) (= (op t) "inst"))))

(defn- lit-elements [f]
  "The elements of (lit T ...): [:pos x] [:key k v] [:field kw v]."
  (loop [xs (seq (nnext f)) i 2 acc []]
    (if-not xs
      acc
      (let [x (first xs)]
        (cond
          (keyword? x) (if (next xs)
                         (recur (nnext xs) (+ i 2) (conj acc {:kind :field :key x :value (second xs) :i i}))
                         (fail "a field key without a value" f))
          (vector? x) (if (= 2 (count x))
                        (recur (next xs) (inc i) (conj acc {:kind :key :key (first x) :value (second x) :i i :src x}))
                        (fail "a key-value element is [key value]" x))
          :else (recur (next xs) (inc i) (conj acc {:kind :pos :value x :i i :src x})))))))

(defn- key-size
  "The printed width of a key, for gofmt's alignment decision."
  [el]
  (if (= (:kind el) :field)
    (count (name (:key el)))
    (let [k (:key el)]
      (cond
        (symbol? k) (count (ident-string k))
        (string? k) (count (text/go-string k))
        (number? k) (count (str k))
        :else 10))))

(defn- composite-lit [f depth]
  (let [t (second f)
        wrap? (and *header* (not= t '_) (lit-type-needs-parens? t))]
    (when wrap? (tok "("))
    (binding [*header* false]
      (when-not (= t '_) (type-expr t))
      (let [els (lit-elements f)
            start (e/line)
            multi? (some (fn [el] (let [x (or (:src el) (:value el))
                                         tl (child-target f (:i el) x)]
                                     (and tl (or (= tl :break) (> (long tl) start)))))
                         els)]
        (tok "{")
        (e/push-open!)
        (let [n (count els)]
          (doseq [[i el] (map-indexed vector els)]
            (let [x (or (:src el) (:value el))
                  tl (child-target f (:i el) x)]
              (when (pos? i) (tok ",") (sp))
              (e/break-to! tl)
              (binding [e/*next* (next-line (map #(or (:src %) (:value %)) (drop (inc i) els)))]
                (case (:kind el)
                  :pos (expr0 (:value el) 1)
                  (do (if (= (:kind el) :field) (tok (name (:key el))) (expr0 (:key el) 1))
                      (tok ":")
                      (if (and multi? (> n 1) (e/lines?)) (vtab) (sp))
                      (expr0 (:value el) 1))))))
          (let [indent (e/pop-open!)
                multi (> (e/line) start)]
            (when (and (e/lines?) (pos? n) multi (e/room? 1)) (tok ","))
            (e/close-break! nil multi false indent)
            (tok "}")))))
    (when wrap? (tok ")"))))

(defn- slice-expr [f depth]
  (let [[_ x & idx] f
        idx (vec idx)
        idx (if (< (count idx) 2) (into idx (repeat (- 2 (count idx)) '_)) idx)
        present (filter #(not= % '_) idx)
        blanks? (and (<= depth 1) (> (count present) 1) (some binary? present))]
    (expr1 x 7 1)
    (tok "[")
    (binding [*header* false]
      (doseq [[i ix] (map-indexed vector idx)]
        (when (pos? i)
          (when (and blanks? (not= (nth idx (dec i)) '_)) (sp))
          (tok ":")
          (when (and blanks? (not= ix '_)) (sp)))
        (when (not= ix '_) (expr0 ix (inc depth)))))
    (tok "]")))

(defn- func-lit
  "(fn params results? body*)."
  [f]
  (let [[_ params & more] f
        [results body] (if (= (first more) :results) [(second more) (nnext more)] [nil more])
        res (results-entries params results)]
    (when-not (vector? params) (fail "fn needs a parameter vector" f))
    (tok "func")
    (binding [*header* false]
      (param-list params)
      (print-results res)
      (block-body body {:results? (seq res) :end (:go/end (meta f)) :func? true :sep sp
                        :parent f :offset (body-offset f body)}))))

(defn- new-arg [x]
  ;; new(T) or Go 1.26's new(value): a type unless the operand is clearly an expression
  (if (or (symbol? x) (type-form? x)
          (and (seq? x) (nil? (op x)) (symbol? (first x)))
          (and (seq? x) (op x) (not (contains? stmt-heads (op x)))
               (not (contains? binary-ops (op x))) (not (contains? unary-ops (op x)))
               (not (#{"lit" "conv" "assert" "aget" "subslice" "fn" "inst" "method-expr" "rune"
                       "imaginary" "byte-string" "go/call" "len" "cap" "append" "make" "new"
                       "complex" "real" "imag" "min" "max" "recover"} (op x)))))
    (type-expr x)
    (expr0 x 1)))

(defn expr1
  "go/printer's expr1: prints f as an operand of precedence at least prec1."
  [f prec1 depth]
  (e/break-to! (line-of f))
  (cond
    (binary? f) (binary-expr f prec1 (cutoff f depth) depth)
    (unary? f)
    (if (< 6 prec1)
      (do (tok "(") (binding [*header* false] (expr0 f 1)) (tok ")"))
      (let [o (unary-op f) x (unary-operand f)]
        (tok o)
        (if (= o "*")
          (if (< (prec-of x) 6)
            (do (tok "(") (binding [*header* false] (expr0 x 1)) (tok ")"))
            (expr1 x 6 1))
          (expr1 x 6 depth))))
    (or (symbol? f) (go-sym? f)) (tok (ident-string f))
    (or (nil? f) (true? f) (false? f) (string? f) (char? f) (number? f)) (literal f)
    (seq? f)
    (let [o (op f)]
      (case o
        "lit" (composite-lit f depth)
        "conv" (let [[_ t x] f]
                 (if (and (seq? t) (or (#{"*" "func" "chan"} (op t))))
                   (do (tok "(") (type-expr t) (tok ")"))
                   (type-expr t))
                 (element-list "(" ")" [x] (fn [x _] (binding [*header* false] (expr0 x depth))) {:parent f :offset 2}))
        "assert" (let [[_ t x] f]
                   (expr1 x 7 1) (tok ".") (tok "(") (type-expr t) (tok ")"))
        "aget" (let [[_ x i] f]
                 (expr1 x 7 1) (tok "[") (binding [*header* false] (expr0 i (inc depth))) (tok "]"))
        "subslice" (slice-expr f depth)
        "inst" (let [[_ g & ts] f]
                 (expr1 g 7 depth)
                 (binding [*header* false] (type-list ts)))
        "method-expr" (let [[_ t m] f]
                        (if (and (seq? t) (#{"*" "func" "chan"} (op t)))
                          (do (tok "(") (type-expr t) (tok ")"))
                          (type-expr t))
                        (tok ".") (tok (ident-string m)))
        "fn" (func-lit f)
        "rune" (tok (text/go-rune (long (second f))))
        "imaginary" (let [x (second f)]
                      (tok (str (text/go-number x) "i")))
        "byte-string" (tok (text/go-byte-string (rest f)))
        "go/call" (let [[_ g & xs] f]
                    (tok (ident-string g))
                    (binding [*header* false] (call-args f xs (if (> (count xs) 1) (inc depth) depth) 2)))
        "go/id" (tok (ident-string f))
        "values" (fail "values outside a return, an assignment or a binding" f)
        "spread" (fail "spread outside the arguments of a call" f)
        "zero" (fail "zero outside a var binding" f)
        "make" (let [[_ t & xs] f
                     depth (if (> (count (rest f)) 1) (inc depth) depth)]
                 (tok "make")
                 (binding [*header* false]
                   (element-list "(" ")" (cons t xs)
                                 (fn [x i] (if (zero? i) (type-expr x) (expr0 x depth)))
                                 {:parent f :offset 1})))
        "new" (let [[_ x] f]
                (tok "new")
                (binding [*header* false]
                  (element-list "(" ")" [x] (fn [x _] (new-arg x)) {:parent f :offset 1})))
        (cond
          (contains? stmt-heads o) (fail "a statement in an expression position" f)
          (type-form? f) (type-expr f)
          (and o (str/starts-with? o ".-") (> (count o) 2))
          (do (expr1 (second f) 7 depth) (tok ".") (tok (subs o 2)))
          (and o (str/starts-with? o ".") (> (count o) 1) (not= o "..."))
          (let [[_ recv & xs] f]
            (expr1 recv 7 depth) (tok ".") (tok (subs o 1))
            (binding [*header* false] (call-args f xs (if (> (count xs) 1) (inc depth) depth) 2)))
          :else
          (let [[h & xs] f
                depth (if (> (count xs) 1) (inc depth) depth)]
            (callee h depth)
            (binding [*header* false] (call-args f xs depth 1))))))
    (keyword? f) (fail "a keyword in an expression position" f)
    (vector? f) (fail "a vector in an expression position" f)
    :else (fail "not an expression" f)))

(defn expr0 [f depth] (expr1 f 0 depth))

(defn- expr-list
  "e1, e2, ...: (values ...) or one expression."
  [f depth]
  (let [xs (if (and (seq? f) (= (op f) "values")) (rest f) [f])]
    (doseq [[i x] (map-indexed vector xs)]
      (when (pos? i) (tok ",") (sp))
      (expr0 x depth))))

(defn- n-values [f] (if (and (seq? f) (= (op f) "values")) (count (rest f)) 1))

;; ---------------------------------------------------------------------------------------
;; Statements (§7)

(defn- zero-form? [f] (and (seq? f) (= (op f) "zero")))

(defn- targets [t] (if (and (seq? t) (= (op t) "values")) (vec (rest t)) [t]))

(defn binding-stmt
  "A let binding pair as a Go statement: :=, var, const (§7.3)."
  [target init]
  (let [ts (targets target)
        ms (map meta ts)
        tm (meta target)
        const? (or (:const tm) (some :const ms))
        var? (or (:var tm) (some :var ms))
        typ (some #(when (contains? % :tag) (:tag %)) ms)
        names (fn [] (doseq [[i t] (map-indexed vector ts)]
                       (when (pos? i) (tok ",") (sp))
                       (tok (ident-string t))))]
    (cond
      const? (do (tok "const") (sp) (names)
                 (when typ (sp) (type-expr typ))
                 (sp) (tok "=") (sp) (expr-list init 1))
      (or var? typ) (do (tok "var") (sp) (names)
                        (when typ (sp) (type-expr typ))
                        (when-not (zero-form? init)
                          (sp) (tok "=") (sp) (expr-list init 1)))
      :else (let [depth (if (and (> (count ts) 1) (> (n-values init) 1)) 2 1)]
              (doseq [[i t] (map-indexed vector ts)]
                (when (pos? i) (tok ",") (sp))
                (expr0 t depth))
              (sp) (tok ":=") (sp) (expr-list init depth)))))

(defn- type-binding-stmt [nm t]
  (tok "type") (sp) (tok (ident-string nm)) (sp)
  (when (:alias (meta nm)) (tok "=") (sp))
  (type-expr t))

(defn- assign-stmt [f]
  (let [[_ place a b] f]
    (case (count f)
      3 (let [depth (if (and (> (n-values place) 1) (> (n-values a) 1)) 2 1)]
          (expr-list place depth) (sp) (tok "=") (sp) (expr-list a depth))
      4 (let [aop (assign-ops (if (symbol? a) (name a) (str a)))]
          (when-not aop (fail "not an assignment operator" a))
          (expr0 place 1) (sp) (tok aop) (sp) (expr0 b 1))
      (fail "set! takes 2 or 3 arguments" f))))

(defn simple-stmt
  "A simple statement: an init or post statement, a select communication, an expression
  statement, an assignment, a := binding written as [target init]."
  [f]
  (cond
    (vector? f) (case (count f)
                  2 (binding-stmt (first f) (second f))
                  1 (simple-stmt (first f))
                  (fail "an init vector holds one statement or one binding" f))
    (seq? f)
    (case (op f)
      "set!" (assign-stmt f)
      "aset" (let [[_ a i v] f]
               (expr1 a 7 1) (tok "[") (expr0 i 2) (tok "]") (sp) (tok "=") (sp) (expr0 v 1))
      "inc!" (do (expr0 (second f) 2) (tok "++"))
      "dec!" (do (expr0 (second f) 2) (tok "--"))
      ">!" (let [[_ ch v] f] (expr0 ch 1) (sp) (tok "<-") (sp) (expr0 v 1))
      (expr0 f 1))
    :else (expr0 f 1)))

(defn- clause? [f] (and (seq? f) (#{"case" "default"} (op f))))

(defn- control-clause
  "go/printer's controlClause: init; cond; post of if, switch and for."
  [for? init cond post]
  (binding [*header* true]
    (sp)
    (let [init (when (and init (not= init [])) init)
          post (when (and post (not= post '_)) post)
          cond (when (and (some? cond) (not= cond '_)) cond)]
      (if (and (nil? init) (nil? post))
        (when (some? cond) (expr0 cond 1) (sp))
        (do (when init (simple-stmt init))
            (tok ";") (sp)
            (when (some? cond) (expr0 cond 1))
            (if for?
              (do (tok ";") (sp)
                  (when post (simple-stmt post) (sp)))
              (when (some? cond) (sp))))))))

(defn- branch-forms
  "The statement list of a branch: (do ...) is its forms, anything else one statement."
  [b]
  (if (and (seq? b) (= (op b) "do")) (vec (rest b)) [b]))

(defn- branch-opts
  "block-body's :parent and :offset for branch b, child i of parent."
  [parent i b]
  (if (and (seq? b) (= (op b) "do")) {:parent b :offset 1} {:parent parent :offset i}))

(defn- if-like? [f] (and (seq? f) (#{"if" "when" "cond"} (op f))))

(declare if-stmt)

(defn- else-part [els opts]
  (sp) (tok "else") (sp)
  (if (if-like? els)
    (print-stmt els)
    (block-body (branch-forms els) opts)))

(defn- if-stmt [f]
  (let [o (op f)]
    (case o
      "if" (let [[_ & xs] f
                 [init xs] (if (vector? (first xs)) [(first xs) (rest xs)] [nil xs])
                 [c then & more] xs]
             (when (> (count more) 1) (fail "if takes a condition, then and else" f))
             (tok "if") (control-clause false init c nil)
             (let [ti (if init 3 2)]
               (if (seq more)
                 (let [els (first more)]
                   (block-body (branch-forms then) (merge (branch-opts f ti then)
                                                          {:next (or (line-of els)
                                                                     (when-not (if-like? els)
                                                                       (next-line (branch-forms els))))
                                                           :else-follows? (if-like? els)}))
                   (else-part els (branch-opts f (inc ti) els)))
                 (block-body (branch-forms then) (branch-opts f ti then)))))
      "when" (let [[_ & xs] f
                   [init xs] (if (vector? (first xs)) [(first xs) (rest xs)] [nil xs])
                   [c & body] xs]
               (tok "if") (control-clause false init c nil)
               (block-body (vec body) {:parent f :offset (body-offset f body)}))
      "cond" (let [pairs (partition 2 (rest f))]
               (when (odd? (count (rest f))) (fail "cond takes test-branch pairs" f))
               (loop [[[t b] & more] pairs bi 2]
                 (if (= t :else)
                   (block-body (branch-forms b) (branch-opts f bi b))
                   (do (tok "if") (control-clause false nil t nil)
                       (if (seq more)
                         (let [[nt nb] (first more)
                               nl (if (= nt :else) (next-line (branch-forms nb)) (line-of nt))]
                           (block-body (branch-forms b) (merge (branch-opts f bi b)
                                                               {:next nl :else-follows? (not= nt :else)}))
                           (sp) (tok "else") (sp)
                           (recur more (+ bi 2)))
                         (block-body (branch-forms b) (branch-opts f bi b))))))))))

(defn- clause-list
  "The clauses of a switch, type switch or select, between braces."
  [clauses print-head]
  (let [indent (if (e/bol?) @(:indent e/*p*) (e/cur-indent))]
    (sp) (tok "{")
    (e/push-open! indent)
    (let [clauses (vec clauses)]
      (dotimes [i (count clauses)]
        (let [c (nth clauses i)]
          (when-not (clause? c) (fail "not a case or default clause" c))
          (binding [e/*next* (next-line (subvec clauses (inc i)))]
            (e/stmt-break! (line-of c) false ";" indent)
            (if (= (op c) "default")
              (do (tok "default") (tok ":")
                  (stmt-list (vec (rest c)) {:indent (inc indent) :parent c :offset 1}))
              (let [[_ head & body] c]
                (tok "case") (sp) (print-head head) (tok ":")
                (stmt-list (vec body) {:indent (inc indent) :parent c :offset 2})))))))
    (e/pop-open!)
    (e/close-break! nil true true indent)
    (tok "}")))

(defn- case-exprs [v]
  (when-not (vector? v) (fail "case needs a vector" v))
  (doseq [[i x] (map-indexed vector v)]
    (when (pos? i) (tok ",") (sp))
    (expr0 x 1)))

(defn- case-types [v]
  (when-not (vector? v) (fail "case needs a vector" v))
  (doseq [[i x] (map-indexed vector v)]
    (when (pos? i) (tok ",") (sp))
    (if (nil? x) (tok "nil") (type-expr x))))

(defn- switch-stmt [f]
  (let [xs (rest f)
        [init xs] (if (vector? (first xs)) [(first xs) (rest xs)] [nil xs])
        [tag xs] (if (and (seq xs) (not (clause? (first xs)))) [(first xs) (rest xs)] [nil xs])]
    (tok "switch")
    (binding [*header* true]
      (cond
        (and init (some? tag)) (do (sp) (simple-stmt init) (tok ";") (sp) (expr0 tag 1))
        init (do (sp) (simple-stmt init) (tok ";"))
        (some? tag) (do (sp) (expr0 tag 1))))
    (clause-list xs case-exprs)))

(defn- type-switch-stmt [f]
  (let [[a b & more] (rest f)
        [init guard clauses] (cond
                               (and (vector? a) (some? b) (not (clause? b))) [a b more]
                               :else [nil a (cons b more)])
        clauses (remove nil? clauses)]
    (tok "switch")
    (binding [*header* true]
      (when init (sp) (simple-stmt init) (tok ";"))
      (sp)
      (if (vector? guard)
        (let [[x g] guard]
          (tok (ident-string x)) (sp) (tok ":=") (sp)
          (expr1 g 7 1) (tok ".") (tok "(") (tok "type") (tok ")"))
        (do (expr1 guard 7 1) (tok ".") (tok "(") (tok "type") (tok ")"))))
    (clause-list clauses case-types)))

(defn- comm [c]
  (cond
    (vector? c) (let [[t x] c] (binding-stmt t x))
    :else (simple-stmt c)))

(defn- select-stmt [f]
  (tok "select")
  (if (and (empty? (rest f)) (not (e/lines?)))
    (do (sp) (tok "{") (tok "}"))
    (clause-list (rest f) comm)))

(defn- for-stmt [f]
  (let [[_ init c post & body] f]
    (when-not (vector? init) (fail "for needs an init vector" f))
    (tok "for")
    (control-clause true init c post)
    (block-body (vec body) {:parent f :offset 5})))

(defn- while-stmt [f]
  (let [[_ c & body] f]
    (tok "for")
    (when-not (true? c) (binding [*header* true] (sp) (expr0 c 1)))
    (block-body (vec body) {:parent f :offset 2})))

(defn- range-stmt [f]
  (let [[_ v & body] f
        assign? (:assign (meta f))]
    (when-not (vector? v) (fail "range needs a vector" f))
    (tok "for") (sp)
    (binding [*header* true]
      (let [x (peek v) vars (pop v)]
        (when (seq vars)
          (doseq [[i k] (map-indexed vector vars)]
            (when (pos? i) (tok ",") (sp))
            (expr0 k 1))
          (sp) (tok (if assign? "=" ":=")) (sp))
        (tok "range") (sp) (expr0 x 1)))
    (block-body (vec body) {:parent f :offset 2})))

(defn print-stmt
  "Prints one statement form (not a let: stmt-list flattens those)."
  [f]
  (let [o (op f)]
    (case o
      ("set!" "aset" "inc!" "dec!" ">!") (simple-stmt f)
      "go" (do (tok "go") (sp) (expr0 (second f) 1))
      "defer" (do (tok "defer") (sp) (expr0 (second f) 1))
      "return" (do (tok "return")
                   (when (seq (rest f))
                     (sp)
                     (doseq [[i x] (map-indexed vector (rest f))]
                       (when (pos? i) (tok ",") (sp))
                       (expr0 x 1))))
      "break" (do (tok "break") (when-let [l (second f)] (sp) (tok (label-name l))))
      "continue" (do (tok "continue") (when-let [l (second f)] (sp) (tok (label-name l))))
      "goto" (do (tok "goto") (sp) (tok (label-name (second f))))
      "fallthrough" (tok "fallthrough")
      "do" (block-body (vec (rest f)) {:parent f :offset 1})
      ("if" "when" "cond") (if-stmt f)
      "switch" (switch-stmt f)
      "type-switch" (type-switch-stmt f)
      "select" (select-stmt f)
      "for" (for-stmt f)
      "while" (while-stmt f)
      "range" (range-stmt f)
      ("let" "let-type") (block-body [f] {})
      (simple-stmt f))))

;; Statement lists: a let scopes over the rest of its list, so the list is flattened.

(defn- flatten-stmts
  "The items of a statement list: {:form f} or {:bind [target init] :t line} or
  {:type-bind [name type]}; a let (or let-type) last in a list continues the list with its
  body, one that is not last is a block of its own. parent and offset: the list holding the
  forms and the index of the first (for :go/breaks)."
  [forms parent offset]
  (let [forms (vec forms)
        n (count forms)]
    (into []
          (mapcat
            (fn [i f]
              (cond
                (and (seq? f) (#{"let" "let-type"} (op f)) (= i (dec n)))
                (let [[_ bv & body] f
                      _ (when-not (vector? bv) (fail "let needs a binding vector" f))
                      pairs (partition 2 bv)
                      items (map-indexed
                              (fn [j [t x]]
                                (let [tl (or (line-of t)
                                             (when (zero? j) (line-of f))
                                             (when (seq? x) (line-of x)))]
                                  (if (= (op f) "let")
                                    {:bind [t x] :t tl}
                                    {:type-bind [t x] :t tl})))
                              pairs)]
                  (when (odd? (count bv)) (fail "let needs pairs" f))
                  (concat items (flatten-stmts body f 2)))
                :else [{:form f :t (if parent (child-target parent (+ offset i) f) (line-of f))}]))
            (range n) forms))))

(defn- item-t [it] (:t it))

(defn stmt-list
  "Prints a statement list at indent (opts :indent), each statement on its recorded line.
  :results? makes a final expression the function's return (§6.4). :parent and :offset
  locate the forms in their list (for :go/breaks)."
  [forms {:keys [indent results? parent offset] :as opts}]
  (let [items (flatten-stmts forms parent (or offset 0))
        n (count items)]
    (dotimes [i n]
      (let [it (nth items i)
            nxt (or (some item-t (subvec items (inc i))) e/*next*)]
        (binding [e/*next* nxt]
          (let [f (:form it)
                label? (and (seq? f) (= (op f) "label"))
                directive? (and (seq? f) (= (op f) "go/directive"))]
            (cond
              directive?
              (do (e/stmt-break! (:t it) false ";" indent)
                  (e/comment-line (second f) indent))
              :else
              (do
                (e/stmt-break! (:t it) (zero? i) ";" (if label? (max 0 (dec indent)) indent))
                (e/push-open! indent)
                (cond
                  (:bind it) (apply binding-stmt (:bind it))
                  (:type-bind it) (apply type-binding-stmt (:type-bind it))
                  label? (let [[_ l s] f]
                           (tok (label-name l)) (tok ":")
                           (cond
                             (some? s) (do (e/stmt-break! (line-of s) false nil indent)
                                           (e/push-open! indent)
                                           (if (and (seq? s) (#{"let" "let-type"} (op s)))
                                             (block-body [s] {})
                                             (print-stmt s))
                                           (e/pop-open!))
                             (< i (dec n)) (if (e/lines?) (tok ";") (do (e/nl indent) (tok ";")))))
                  (and results? (= i (dec n)) (expression-form? f))
                  (do (tok "return") (sp) (expr0 f 1))
                  :else (print-stmt f))
                (e/pop-open!)))))))))

(defn block-body
  "{ statements } at one more indentation than the line of the brace. opts: :results?,
  :end (the recorded line of the closing brace), :func? (a function body: gofmt keeps it on
  one line when the source has it so), :sep (whitespace before the brace), :next (the line
  of what follows the brace), :else-follows?."
  [forms {:keys [results? end func? sep next parent offset] :as opts}]
  (let [indent (if (e/bol?) @(:indent e/*p*) (e/cur-indent))
        start (e/line)
        forms (vec forms)
        one-line? (if (e/lines?)
                    (if (integer? end)
                      (= (long end) start)
                      (and func? (every? #(let [t (line-of %)] (or (nil? t) (= t start))) forms)
                           (not (some (breaks-of parent) (range (or offset 0) (+ (or offset 0) (count forms)))))
                           (or (seq forms) (nil? next) (room? 0))))
                    (and func? (empty? forms)))]
    (if one-line? ((or sep sp)) (sp))
    (tok "{")
    (e/push-open! indent)
    (binding [e/*next* (if (integer? end) end (or next e/*next*))]
      (when (seq forms)
        (if one-line?
          (binding [e/*next* nil]
            (sp)
            (stmt-list-one-line forms results? (inc indent)))
          (stmt-list forms {:indent (inc indent) :results? results? :parent parent :offset offset})))
      (e/pop-open!)
      (if one-line?
        (do (when (seq forms) (sp)) (tok "}"))
        (do (when-not (e/close-break! end (> (e/line) start) true indent) (sp))
            (tok "}"))))))

(defn- stmt-list-one-line
  "A function body on one line: statements separated by semicolons (go/printer's funcBody)."
  [forms results? indent]
  (let [items (flatten-stmts forms nil 0)
        n (count items)]
    (dotimes [i n]
      (let [it (nth items i) f (:form it)]
        (when (pos? i) (tok ";") (sp))
        (e/push-open! (dec indent))
        (cond
          (:bind it) (apply binding-stmt (:bind it))
          (:type-bind it) (apply type-binding-stmt (:type-bind it))
          (and results? (= i (dec n)) (expression-form? f)) (do (tok "return") (sp) (expr0 f 1))
          :else (print-stmt f))
        (e/pop-open!)))))

;; ---------------------------------------------------------------------------------------
;; Declarations (§6)

(defn parse-decl-head
  "[doc opts rest] of the arguments after a declaration's name: an optional doc string, then
  :type-params."
  [xs]
  (let [[doc xs] (if (string? (first xs)) [(first xs) (rest xs)] [nil xs])
        [tps xs] (if (= (first xs) :type-params) [(second xs) (nnext xs)] [nil xs])]
    [doc tps xs]))

(defn- generic-type? [t]
  ;; a receiver type naming type parameters: (* (Stack T)) or (Stack T)
  (let [t (if (and (seq? t) (= (op t) "*")) (second t) t)]
    (and (seq? t) (not (contains? type-heads (op t))))))

(defn func-decl
  "go/func and go/method (§6.4)."
  [f method?]
  (let [[_ nm & xs] f
        [doc tps xs] (parse-decl-head xs)
        [params & more] xs
        _ (when-not (vector? params) (fail "a function needs a parameter vector" f))
        [results body] (if (= (first more) :results) [(second more) (nnext more)] [nil more])
        m (meta nm)
        extern? (:extern m)
        [recv params] (if method? [(first params) (with-meta (vec (rest params)) (meta params))] [nil params])
        res (results-entries params results)]
    (binding [*generic* (boolean (or tps (and recv (generic-type? (if (tagged? recv) (:tag (meta recv)) recv)))))]
      (tok "func") (sp)
      (when method?
        (tok "(")
        (if (tagged? recv)
          (do (tok (ident-string recv)) (sp) (type-expr (:tag (meta recv))))
          (type-expr recv))
        (tok ")") (sp))
      (tok (ident-string nm))
      (when tps (type-params tps))
      (param-list params)
      (print-results res)
      (when-not extern?
        (block-body (vec body) {:results? (seq res) :end (:go/end (meta f)) :func? true
                                :parent f :offset (body-offset f body)
                                :sep (if (e/lines?) vtab sp)})))))

(defn- spec-name [t] (if (and (seq? t) (= (op t) "values")) (second t) t))

(defn- value-spec
  "Names, type and values of a const or var spec. keep-type: gofmt's type column."
  [target init group? keep-type]
  (let [ts (targets target)
        typ (some #(when (contains? (meta %) :tag) (:tag (meta %))) ts)
        sep (if group? vtab sp)]
    (doseq [[i t] (map-indexed vector ts)]
      (when (pos? i) (tok ",") (sp))
      (tok (ident-string t)))
    (when (or typ (and group? keep-type)) (sep))
    (when typ (type-expr typ))
    (when (not= init ::none)
      (sep) (tok "=") (sp) (expr-list init 1))))

(defn- keep-type-column
  "go/printer's keepTypeColumn over the specs [target init]."
  [specs]
  (let [n (count specs)
        m (transient (vec (repeat n false)))]
    (loop [i 0 i0 -1 keep false]
      (if (< i n)
        (let [[t init] (nth specs i)
              has-values (not= init ::none)
              typed (some #(contains? (meta %) :tag) (targets t))
              [i0 keep] (if has-values
                          (if (neg? i0) [i false] [i0 keep])
                          (do (when (>= i0 0) (doseq [j (range i0 i)] (when keep (assoc! m j true))))
                              [-1 keep]))
              keep (or keep (boolean typed))]
          (recur (inc i) i0 keep))
        (do (when (>= i0 0) (doseq [j (range i0 n)] (when keep (assoc! m j true))))
            (persistent! m))))))

(defn- spec-t [v]
  (or (line-of v) (line-of (spec-name (first v))) (when (seq? (second v)) (line-of (second v)))))

(defn- group-decl
  "kw ( specs ): print-spec prints one spec vector given its index."
  [kw doc specs print-spec meta-of]
  (tok kw) (sp) (tok "(")
  (let [indent 0]
    (if (empty? specs)
      (tok ")")
      (let [specs (vec specs)
            last-start (volatile! (e/line))
            first-line (e/line)]
        (e/push-open! 0)
        (dotimes [i (count specs)]
          (let [s (nth specs i)
                m (meta-of s)
                dirs (directives-of m)
                pre (pre-lines (:doc m) dirs)
                t (spec-t s)]
            (binding [e/*next* (next-line (subvec specs (inc i)))]
              (cond
                (and (e/lines?) (integer? t))
                (do (if (pos? pre)
                      (e/stmt-break! (max (- (long t) pre) (inc (e/line))) false ";" 1)
                      (e/stmt-break! t (zero? i) ";" 1)))
                (e/lines?) (e/stmt-break! (if (pos? pre) :break nil) (zero? i) ";" 1)
                :else (e/nl 1 (or (zero? i) (> (e/line) @last-start)))))
            (when (pos? pre)
              (when-not (e/bol?) (e/nl 1))
              (write-pre (:doc m) dirs 1)
              (when (and (e/lines?) (integer? t)) (e/stmt-break! t true nil 1)))
            (vreset! last-start (e/line))
            (print-spec s i)))
        (e/pop-open!)
        (if (e/lines?)
          (e/close-break! nil true true indent)
          (e/nl 0 true))
        (tok ")")))))

(defn- const-var-decl [f kw]
  (let [[_ & xs] f
        [doc xs] (if (string? (first xs)) [(first xs) (rest xs)] [nil xs])]
    (if (and (seq xs) (every? vector? xs) (not (and (= 1 (count xs)) false)))
      ;; a group
      (let [specs (mapv (fn [v] [(first v) (if (> (count v) 1) (second v) ::none)]) xs)
            keep (keep-type-column specs)
            group? (> (count specs) 1)]
        (group-decl kw doc xs
                    (fn [v i] (let [[t init] (nth specs i)] (value-spec t init group? (nth keep i))))
                    (fn [v] (meta (spec-name (first v))))))
      (let [[t init] xs]
        (when (and (empty? xs)) (fail "an empty declaration" f))
        (tok kw) (sp)
        (value-spec t (if (> (count xs) 1) init ::none) false false)))))

(defn- type-spec [nm tps t group?]
  (tok (ident-string nm))
  (binding [*generic* (boolean tps)]
    (when tps (type-params tps))
    (if group? (vtab) (sp))
    (when (:alias (meta nm)) (tok "=") (sp))
    (type-expr t)))

(defn- parse-type-spec [xs]
  ;; Name doc? (:type-params [...])? type
  (let [[nm & xs] xs
        [doc tps xs] (parse-decl-head xs)]
    [nm doc tps (first xs)]))

(defn- type-decl [f]
  (let [[_ & xs] f
        [gdoc gxs] (if (string? (first xs)) [(first xs) (rest xs)] [nil xs])]
    (if (or (empty? gxs) (every? vector? gxs))
      (group-decl "type" gdoc gxs
                  (fn [v _] (let [[nm _ tps t] (parse-type-spec (seq v))]
                              (type-spec nm tps t (> (count gxs) 1))))
                  (fn [v] (meta (first v))))
      (let [[nm _ tps t] (parse-type-spec xs)]
        (tok "type") (sp)
        (type-spec nm tps t false)))))

(defn decl-doc-and-directives
  "[doc directives] of a top-level form."
  [f]
  (let [o (op f)
        xs (rest f)]
    (case o
      ("go/func" "go/method") (let [nm (first xs)
                                    [doc] (parse-decl-head (rest xs))]
                                [doc (directives-of (meta nm))])
      "go/type" (if (string? (first xs))
                  [(first xs) []]
                  (if (every? vector? xs)
                    [nil []]
                    (let [[nm doc] (parse-type-spec xs)] [doc (directives-of (meta nm))])))
      ("go/const" "go/var") (if (string? (first xs))
                              [(first xs) []]
                              (if (every? vector? xs)
                                [nil []]
                                (let [nm (spec-name (first xs))]
                                  [(:doc (meta nm)) (directives-of (meta nm))])))
      [nil []])))

(defn top-decl
  "Prints a top-level declaration form, after its doc comment and directives."
  [f]
  (case (op f)
    "go/func" (func-decl f false)
    "go/method" (func-decl f true)
    "go/type" (type-decl f)
    "go/const" (const-var-decl f "const")
    "go/var" (const-var-decl f "var")
    (fail "not a declaration" f)))
