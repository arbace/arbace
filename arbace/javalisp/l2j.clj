(ns arbace.javalisp.l2j
  "javalisp -> Java: print forms read by arbace.javalisp.reader as Java source that keeps
  every line, so javac rebuilds the same tree with the same line numbers."
  (:require [arbace.javalisp.reader :as rd]
            [arbace.javalisp.prec :as prec]
            [clojure.string :as str]))

;;; Java tokens: {:t text :l line :g glue-before :ng no-space-after}

(defn- T
  ([s] {:t s})
  ([s l] {:t s :l l}))

(def ^:private dot {:t "." :g true :ng true})
(def ^:private lparen-call {:t "(" :g true :ng true})
(def ^:private lparen {:t "(" :ng true :fwd true})
(def ^:private rparen {:t ")" :g true})
(def ^:private comma {:t "," :g true})
(def ^:private semi {:t ";" :g true})
(def ^:private lbrack {:t "[" :g true :ng true})
(def ^:private rbrack {:t "]" :g true})
(def ^:private lt {:t "<" :g true :ng true})
(def ^:private gt {:t ">" :g true})

(defn- fail [n msg]
  (throw (ex-info (str "line " (or (:l n) "?") ": " msg) {:node n})))

(defn- atom? [n] (contains? n :a))
(defn- lst? [n] (contains? n :L))
(defn- vec? [n] (contains? n :V))
(defn- text [n] (:a n))
(defn- head [n] (when (lst? n) (text (first (:L n)))))
(defn- items [n] (or (:L n) (:V n)))
(defn- head-line [n] (:l (first (:L n))))

(defn- join [sep xs] (apply concat (interpose [sep] xs)))

(declare clojure-string-value)

(defn- doc-tokens
  "The doc comment a node carries (from #_\"...\"), as a token on the node's line."
  [n]
  (when-let [d (:pre n)]
    [{:t (clojure-string-value d) :l (or (:l n) (some :l (:L n)))}]))

(defn- end-line [n] (if (contains? n :a) (:l n) (:cl n)))

(defn- operands
  "[[operand op-line] ...] of the items of an infix form with operator `op`: op-line is the
  line of the operator before the operand, given by a marker or else where the previous
  operand ends."
  [op xs]
  (loop [xs xs prev nil marker nil acc []]
    (if-let [[a & more] (seq xs)]
      (if (and prev (contains? a :a) (= op (:a a)))
        (recur more prev (:l a) acc)
        (recur more a nil (conj acc [a (when prev (or marker (end-line prev)))])))
      acc)))

;;; Literals and names

(def ^:private named-chars
  {"newline" \newline "space" \space "tab" \tab "backspace" \backspace "formfeed" \formfeed
   "return" \return})

(defn- java-escape [c quote]
  (let [i (int c)]
    (cond
      (= c quote) (str \\ c)
      (= c \\) "\\\\"
      (= c \newline) "\\n"
      (= c \return) "\\r"
      (= c \tab) "\\t"
      (= c \backspace) "\\b"
      (= c \formfeed) "\\f"
      (< i 32) (format "\\%03o" i)
      (or (< i 127) (and (> i 160) (not (Character/isSurrogate c))
                         (not (Character/isISOControl c))
                         (not (Character/isSpaceChar c)))) (str c)
      (= i 127) "\\177"
      :else (format "\\u%04x" i))))

(defn- clojure-string-value
  "The value of Clojure string token `s`."
  [^String s]
  (let [sb (StringBuilder.)
        n (dec (count s))]
    (loop [i 1]
      (when (< i n)
        (let [c (.charAt s i)]
          (if (= c \\)
            (let [e (.charAt s (inc i))]
              (case e
                \t (do (.append sb \tab) (recur (+ i 2)))
                \r (do (.append sb \return) (recur (+ i 2)))
                \n (do (.append sb \newline) (recur (+ i 2)))
                \b (do (.append sb \backspace) (recur (+ i 2)))
                \f (do (.append sb \formfeed) (recur (+ i 2)))
                \\ (do (.append sb \\) (recur (+ i 2)))
                \" (do (.append sb \") (recur (+ i 2)))
                \u (do (.append sb (char (Integer/parseInt (subs s (+ i 2) (+ i 6)) 16))) (recur (+ i 6)))
                (let [m (re-find #"^[0-7]{1,3}" (subs s (inc i)))]
                  (.append sb (char (Integer/parseInt m 8)))
                  (recur (+ i 1 (count m))))))
            (do (.append sb c) (recur (inc i)))))))
    (str sb)))

(defn- char-value
  "The char of Clojure character token `s`."
  [^String s]
  (let [body (subs s 1)]
    (cond
      (named-chars body) (named-chars body)
      (and (= \u (first body)) (= 5 (count body))) (char (Integer/parseInt (subs body 1) 16))
      (and (= \o (first body)) (> (count body) 1)) (char (Integer/parseInt (subs body 1) 8))
      :else (first body))))

(defn- string-literal [s] (str \" (apply str (map #(java-escape % \") (clojure-string-value s))) \"))
(defn- char-literal [c] (str \' (java-escape c \') \'))

(defn- numeral? [^String s] (boolean (re-matches #"[-+]?[0-9].*" s)))

(defn- java-int [^String s]
  (if-let [[_ sign digits] (re-matches #"([-+]?)2r([01]+)" s)]
    (str sign "0b" digits)
    s))

(def modifier-words
  #{"public" "protected" "private" "abstract" "static" "final" "transient" "volatile"
    "synchronized" "native" "strictfp" "default" ":sealed" ":non-sealed"})

(defn- modifier? [n]
  (or (and (atom? n) (modifier-words (text n))) (= ":ann" (head n))))

(defn- name-tokens
  "Tokens for a (possibly dotted) name atom."
  [n]
  (let [l (:l n)
        segs (str/split (text n) #"\.")]
    (join dot (map #(vector (T % l)) segs))))

(def binary-ops
  {"||" ["||" :or] "&&" ["&&" :and] "|" ["|" :bitor] "bit-xor" ["^" :bitxor] "&" ["&" :bitand]
   "==" ["==" :eq] "!=" ["!=" :eq] "<" ["<" :ord] ">" [">" :ord] "<=" ["<=" :ord]
   ">=" [">=" :ord] "<<" ["<<" :shift] ">>" [">>" :shift] ">>>" [">>>" :shift]
   "+" ["+" :add] "-" ["-" :add] "*" ["*" :mul] "/" ["/" :mul] "%" ["%" :mul]})

(def assign-ops
  #{"|=" "&=" "<<=" ">>=" ">>>=" "+=" "-=" "*=" "%="})

(declare expr typ stmt stmts block member members class-decl annotation expr-init expr-pattern
         tparam-tokens enum-constant-tokens* member* stmt* top-level*)

(defn- ex
  "Tokens of expression `n`, parenthesized when context `ctx` needs it (see prec/wrap?)."
  ([n] (ex n prec/free))
  ([n ctx]
   (let [wrap (and ctx (prec/wrap? n ctx))
         ts (expr n (or wrap (not (:in-binary ctx))))]
     (if wrap (concat [lparen] ts [rparen]) ts))))

(defn- condition-parts
  "[paren-line condition more...] of the items after a statement keyword: a `.` marker gives
  the line of the `(` around the condition."
  [xs]
  (if (= "." (text (first xs))) (cons (:l (first xs)) (rest xs)) (cons nil xs)))

(defn- condition-tokens [paren-line c]
  (concat [(assoc lparen :fwd false :l paren-line)] (ex c) [rparen]))

(defn- args-tokens
  ([ns] (args-tokens ns nil))
  ([ns paren-line]
   (concat [(assoc lparen-call :l paren-line)] (join comma (map ex ns)) [rparen])))

(defn- targs-tokens [v]
  (when v (concat [lt] (join comma (map typ (items v))) [gt])))

(defn- numeric-literal? [n]
  (or (and (atom? n) (numeral? (text n)))
      (#{"long" "float"} (head n))))

(defn- string-atom? [n] (and (atom? n) (str/starts-with? (text n) "\"")))

;;; Types

(defn- array-dims?
  "True when list `n` is an array type: a base type followed only by dimension brackets
  (vectors), `...` and annotations."
  [n]
  (let [ds (rest (:L n))]
    (and (some #(or (vec? %) (= "..." (text %))) ds)
         (every? #(or (vec? %) (= "..." (text %)) (= ":ann" (head %))) ds))))

(defn- array-type?
  "True when `n` is an array type in expression position: only empty brackets."
  [n]
  (and (lst? n) (array-dims? n) (every? #(or (not (vec? %)) (empty? (:V %))) (rest (:L n)))))

(defn typ
  "Tokens of type form `n`."
  [n]
  (cond
    (atom? n) (name-tokens n)
    (= "?" (head n)) (let [[q k t] (:L n)] (concat [(T "?" (:l q)) (T (text k))] (typ t)))
    (= "|" (head n)) (join (T "|") (map typ (rest (:L n))))
    (= "&" (head n)) (join (T "&") (map typ (rest (:L n))))
    (= ":annotated" (head n)) (let [xs (rest (:L n))
                                    t (last xs)
                                    anns (mapcat annotation (butlast xs))]
                                (if-let [i (and (atom? t) (str/last-index-of (text t) "."))]
                                  (concat (name-tokens (assoc t :a (subs (text t) 0 i))) [(assoc dot :ng false)] anns
                                          [(T (subs (text t) (inc i)) (:l t))])
                                  (concat anns (typ t))))
    (= ":type" (head n)) (typ (second (:L n)))
    (= ".." (head n)) (let [[_ base & links] (:L n)]
                        (concat (typ base)
                                (loop [ls links dot-line nil acc []]
                                  (if-let [[l & more] (seq ls)]
                                    (if (= "." (text l))
                                      (recur more (:l l) acc)
                                      (recur more nil
                                             (into acc (if (= ":annotated" (head l))
                                                         (let [[_ & as] (:L l)]
                                                           (concat [(assoc dot :l (or dot-line (:l (first (:L (first as))))) :ng false)]
                                                                   (mapcat annotation (butlast as))
                                                                   (name-tokens (last as))))
                                                         (concat [(assoc dot :l (or dot-line (:l l)))] (name-tokens l))))))
                                    acc))))
    (array-dims? n) (let [[base & dims] (:L n)]
                      (concat (typ base)
                              (mapcat (fn [d]
                                        (cond
                                          (= "..." (text d)) [(T "...")]
                                          (= ":ann" (head d)) (annotation d)
                                          :else (concat [(assoc lbrack :l (:l d))] (mapcat ex (items d)) [rbrack])))
                                      dims)))
    (lst? n) (let [[base & targs] (:L n)
                   [l targs] (if (= "." (text (first targs))) [(:l (first targs)) (rest targs)] [nil targs])]
               (concat (typ base) [(assoc lt :l l)] (join comma (map typ targs)) [gt]))
    :else (fail n "bad type")))

;;; Modifiers and annotations

(defn annotation [n]
  (let [[h t & as] (:L n)]
    (concat [(assoc (T "@" (:l h)) :ng true)] (typ t)
            (when (seq as)
              (concat [lparen-call]
                      (join comma (map (fn [a]
                                         (if (= "=" (head a))
                                           (let [[[k] [v l]] (operands "=" (rest (:L a)))]
                                             (concat (name-tokens k) [(T "=" l)] (if (vec? v) (expr-init v) (ex v))))
                                           (if (vec? a) (expr-init a) (ex a))))
                                       as))
                      [rparen])))))

(defn- mods-tokens [ms]
  (mapcat (fn [m]
            (if (atom? m)
              [(T (str/replace (text m) #"^:" "") (:l m))]
              (annotation m)))
          ms))

(defn- split-mods [xs] (split-with modifier? xs))

;;; Variables and parameters

(defn- param-groups
  "Split the items of a parameter vector into [mods type name] groups."
  [xs]
  (loop [xs xs acc []]
    (if (empty? xs)
      acc
      (let [[ms more] (split-mods xs)
            [t nm & more] more]
        (recur more (conj acc [ms t nm]))))))

(defn- param-tokens [[ms t nm]]
  (concat (doc-tokens (first (concat ms [t]))) (mods-tokens ms) (typ t) (name-tokens nm)))

(defn- declarators
  "[[name init] ...] of the items after the type in a (:var ...) form."
  [xs]
  (loop [xs xs acc []]
    (if-let [[nm & more] (seq xs)]
      (if (= "=" (text (first more)))
        (recur (drop 2 more) (conj acc [nm (second more)]))
        (recur more (conj acc [nm nil])))
      acc)))

(defn- var-parts
  "[mods type [[name init] ...]] of a (:var ...) form."
  [n]
  (let [[ms more] (split-mods (rest (:L n)))
        [t & more] more]
    [ms t (declarators more)]))

(defn- declarator-tokens [[nm init]]
  (concat (if (lst? nm)
            (concat (name-tokens (first (:L nm))) (mapcat (fn [v] [(assoc lbrack :l (:l v)) rbrack]) (rest (:L nm))))
            (name-tokens nm))
          (when init (concat [(T "=")] (if (vec? init) (expr-init init) (ex init))))))

(defn- var-tokens
  "Tokens of a variable declaration without the terminating semicolon."
  [n]
  (let [[ms t ds] (var-parts n)]
    (concat (mods-tokens ms) (typ t) (join comma (map declarator-tokens ds)))))

(defn expr-init
  "Tokens of an array initializer vector."
  [v]
  (concat [(T "{" (:l v))] (join comma (map #(if (vec? %) (expr-init %) (ex %)) (items v))) [(T "}")]))

;;; Expressions: (expr n) is [tokens precedence-key]

(defn- call-parts
  "[name-node targs-vector args paren-line] of a call form (name [targs] args...) or
  ([targs] name args...); a `.` marker before the arguments gives the line of their `(`."
  [n]
  (let [[h & more] (if (vec? (first (:L n)))
                     (let [[tv h & more] (:L n)] (cons h (cons tv more)))
                     (:L n))
        [tv more] (if (vec? (first more)) [(first more) (rest more)] [nil more])
        [pl more] (if (= "." (text (first more))) [(:l (first more)) (rest more)] [nil more])]
    [h tv more pl]))

(defn- chain-links
  "Tokens of the links of a (.. base links...) form; a `.` marker gives the next dot's line."
  [ls]
  (loop [ls ls dot-line nil acc []]
    (if-let [[l & more] (seq ls)]
      (if (= "." (text l))
        (recur more (:l l) acc)
        (let [d (assoc dot :l (or dot-line (if (atom? l) (:l l) (head-line l))))]
          (recur more nil
                 (into acc (cond
                             (atom? l) (concat [d] (name-tokens l))
                             (= ":annotated" (head l)) (let [[_ & as] (:L l)]
                                                         (concat [(assoc d :ng false :l (or dot-line (:l (first (:L (first as))))))]
                                                                 (mapcat annotation (butlast as)) (name-tokens (last as))))
                             (= "new" (head l)) (concat [d] (expr l))
                             :else (let [[h tv as pl] (call-parts l)]
                                     (concat [d] (targs-tokens tv) (name-tokens h) (args-tokens as pl))))))))
      acc)))

(defn- new-tokens [n]
  (let [[h & more] (:L n)
        [tv more] (if (vec? (first more)) [(first more) (rest more)] [nil more])
        [t & more] more]
    (if (and (lst? t) (array-dims? t))
      (concat [(T "new" (:l h))] (typ t) (when-let [init (first more)] (expr-init init)))
      (let [body (when (= "class" (head (last more))) (last more))
            as (if body (butlast more) more)]
        (concat [(T "new" (:l h))] (targs-tokens tv) (typ t) (args-tokens as)
                (when body (concat [(T "{" (:l (first (:L body))))]
                                   (members (rest (:L body)) nil)
                                   [(T "}")])))))))

(defn- lambda-tokens [n]
  (let [[h ps body] (:L n)]
    (concat (cond
              (atom? ps) (name-tokens ps)
              (lst? ps) (concat [lparen] (join comma (map name-tokens (:L ps))) [rparen])
              :else (concat [lparen] (join comma (map param-tokens (param-groups (:V ps)))) [rparen]))
            [(T "->" (:l h))]
            (if (= "do" (head body)) (block body) (ex body)))))

(defn- case-tokens
  [c]
  (let [[h & more] (:L c)
        [labels guard more] (if (= "default" (text h))
                              [nil nil more]
                              (let [[v & more] more]
                                (if (= ":when" (head (first more)))
                                  [v (first more) (rest more)]
                                  [v nil more])))
        rule? (= "->" (text (first more)))]
    (concat (if labels
              (concat [(T "case" (:l h))]
                      (join comma (map (fn [lb]
                                         (cond (= "default" (text lb)) [(T "default" (:l lb))]
                                               (#{":var" ":record"} (head lb)) (expr-pattern lb)
                                               (= "_" (text lb)) [(T "_" (:l lb))]
                                               :else (ex lb)))
                                       (items labels))))
              [(T "default" (:l h))])
            (when guard (concat [(T "when" (head-line guard))] (ex (second (:L guard)))))
            (if rule?
              (let [b (second more)]
                (concat [(T "->" (:l (first more)))]
                        (cond (= "do" (head b)) (block b)
                              (= "throw" (head b)) (stmt b)
                              :else (concat (ex b) [semi]))))
              (concat [{:t ":" :g true}] (stmts more))))))

(defn- brace-line
  "The line of the `}` closing list `n` (a block or a switch), whose content starts at
  item `from`."
  [n from]
  (if (:detached n)
    (:cl n)
    (let [open (:l n)
          kids (drop from (:L n))
          last (if (seq kids)
                 (let [k (last kids)] (if (atom? k) (:l k) (:cl k)))
                 open)]
      (if (= open last) open (inc last)))))

(defn- switch-tokens [n]
  (let [[h & more] (:L n)
        [_ sel & cases] (condition-parts more)]
    (concat [(T "switch" (:l h))] (let [[pl c] (condition-parts (rest (:L n)))] (condition-tokens pl c)) [(T "{")]
            (mapcat case-tokens cases)
            [(T "}" (brace-line n 1))])))

(defn expr-pattern [n]
  (case (head n)
    ":var" (param-tokens (first (param-groups (rest (:L n)))))
    ":record" (let [[_ t & ps] (:L n)]
                (concat (typ t) [lparen-call] (join comma (map expr-pattern ps)) [rparen]))
    (if (= "_" (text n)) [(T "_" (:l n))] (typ n))))

(defn expr
  ([n] (expr n true))
  ([n root?]
  (cond
    (atom? n)
    (let [s (text n) l (:l n)]
      (cond
        (str/starts-with? s "\"") [(T (string-literal s) l)]
        (str/starts-with? s "\\") [(T (char-literal (char-value s)) l)]
        (numeral? s) [(T (java-int s) l)]
        :else (name-tokens n)))

    (vec? n) (expr-init n)

    :else
    (let [h (head n) [hn & xs] (:L n) hl (:l hn)
          ctxs (prec/child-contexts n root?)
          sub (fn [i] (ex (nth (:L n) i) (get ctxs i)))
          ;; [[item-index op-line] ...] of the operands of an infix form
          ops (fn [op] (let [os (operands op xs)
                             idx (keep-indexed #(when (some? %2) (inc %1)) (prec/operand-contexts op xs))]
                         (map (fn [[_ l] i] [i l]) os idx)))]
      (cond
        (= h ":paren") (concat [(assoc lparen :l hl)] (ex (first xs) prec/free) [rparen])
        (= h "long") [(T (str (java-int (text (first xs))) "L") (:l (first xs)))]
        (= h "float") [(T (str (text (first xs)) "f") (:l (first xs)))]
        (= h "char") [(T (char-literal (char (Long/decode (text (first xs))))) (:l (first xs)))]
        (= h "..") (concat (let [b (first xs)] (if (array-type? b) (typ b) (sub 1)))
                           (chain-links (rest xs)))
        (= h "new") (new-tokens n)
        (= h "->") (lambda-tokens n)
        (array-type? n) (typ n)
        (= h ":ref") (let [[e & more] xs
                           [tv nm] (if (vec? (first more)) more [nil (first more)])]
                       (concat (if (or (= ":type" (head e)) (= ":annotated" (head e)) (array-type? e))
                                 (typ e)
                                 (sub 1))
                               [{:t "::" :g true :ng true}] (targs-tokens tv) [(T (text nm))]))
        (prec/assign-heads h)
        (let [[[a] [b l]] (ops h)]
          (concat (sub a) [(T (case h "bit-xor=" "^=" "div=" "/=" h) l)] (sub b)))
        (and (#{"+" "-" "!" "bit-not" "++" "--"} h) (= 1 (count xs)))
        (concat [(assoc (T (if (= h "bit-not") "~" h) hl) :ng true)] (sub 1))
        (#{":++" ":--"} h) (concat (sub 1)
                                   [(assoc (T (subs h 1) (if (= "." (text (second xs))) (:l (second xs)) (end-line (first xs))))
                                           :g true)])
        (binary-ops h)
        (let [[op] (binary-ops h)
              [[a] & more] (ops h)]
          (concat (sub a) (mapcat (fn [[i l]] (concat [(T op l)] (sub i))) more)))
        (= h ":cast") (concat [(assoc (T "(" hl) :ng true)] (typ (first xs)) [rparen] (sub 2))
        (= h "instanceof") (let [[[e] [t l]] (ops h)
                                 tn (nth (:L n) t)]
                             (concat (sub e) [(T "instanceof" l)]
                                     (if (#{":var" ":record"} (head tn)) (expr-pattern tn) (typ tn))))
        (= h "?") (let [[[c] [a l] [b]] (ops h)]
                    (concat (sub c) [(T "?" l)] (sub a) [(T ":")] (sub b)))
        (= h ":aget") (let [[[a] & is] (ops ".")]
                        (concat (sub a) (mapcat (fn [[i l]] (concat [(assoc lbrack :l l)] (sub i) [rbrack])) is)))
        (= h "switch") (switch-tokens n)
        (= h ":ann") (annotation n)
        :else (let [[nm tv as pl] (call-parts n)]
                (concat (if tv
                          (let [i (str/last-index-of (text nm) ".")]
                            (cond
                              i (concat (name-tokens (assoc nm :a (subs (text nm) 0 i))) [dot] (targs-tokens tv)
                                        [(T (subs (text nm) (inc i)) (:l nm))])
                              (#{"this" "super"} (text nm))
                              (concat [(assoc lt :g false :fwd true)] (join comma (map typ (items tv))) [gt] (name-tokens nm))
                              :else (fail n "type arguments need a qualified method")))
                          (name-tokens nm))
                        (args-tokens as pl))))))))

;;; Statements

(defn block
  "Tokens of a (do ...) block."
  [n]
  (let [[h & more] (:L n)
        static? (= "static" (text (first more)))]
    (concat (when static? [(T "static" (:l (first more)))])
            [(T "{" (:l h))]
            (stmts (if static? (rest more) more))
            [(T "}" (brace-line n (if static? 2 1)))])))

(defn- body-stmt [n] (stmt n))

(defn stmts [ns] (mapcat stmt ns))

(def class-heads #{"class" "interface" "enum" ":record" ":annotation"})

(defn stmt
  "Tokens of statement form `n`."
  [n]
  (concat (doc-tokens n) (stmt* n)))

(defn- stmt* [n]
  (let [h (head n)
        [hn & xs] (:L n)
        hl (:l hn)]
    (cond
      (and (lst? n) (empty? (:L n))) [(assoc (T ";" (:l n)) :g false)]
      (= h "do") (block n)
      (= h ":var") (concat (var-tokens n) [semi])
      (class-heads h) (class-decl n)
      (= h "if") (let [[pl c a b] (condition-parts xs)]
                   (concat [(T "if" hl)] (condition-tokens pl c) (body-stmt a)
                           (when b (concat [(T "else")] (body-stmt b)))))
      (= h "while") (let [[pl c b] (condition-parts xs)] (concat [(T "while" hl)] (condition-tokens pl c) (body-stmt b)))
      (= h ":do-while") (let [[b & more] xs
                              [pl c] (condition-parts more)]
                          (concat [(T "do" hl)] (body-stmt b) [(T "while")] (condition-tokens pl c) [semi]))
      (= h "for")
      (if (or (vec? (second xs)) (vec? (nth xs 2 nil)))
        (let [[init & more] xs
              [c step body] (if (vec? (first more)) [nil (first more) (second more)] more)
              init-items (:V init)]
          (concat [(T "for" hl) lparen]
                  (if (= ":var" (head (first init-items)))
                    (var-tokens (first init-items))
                    (join comma (map ex init-items)))
                  [semi] (when c (ex c)) [semi]
                  (join comma (map ex (:V step))) [rparen]
                  (body-stmt body)))
        (let [[v body] xs
              vs (:V v)
              [ms more] (split-mods vs)
              [t nm e] more]
          (concat [(T "for" hl) lparen] (mods-tokens ms) (typ t) (name-tokens nm) [(T ":")] (ex e) [rparen]
                  (body-stmt body))))
      (= h ":label") (let [[lb b] xs] (concat [(T (text lb) (:l lb)) {:t ":" :g true}] (body-stmt b)))
      (= h "switch") (switch-tokens n)
      (= h "synchronized") (let [[pl lk b] (condition-parts xs)]
                             (concat [(T "synchronized" hl)] (condition-tokens pl lk) (block b)))
      (= h "try")
      (let [[res more] (if (vec? (first xs)) [(first xs) (rest xs)] [nil xs])
            [b & clauses] more]
        (concat [(T "try" hl)]
                (when res
                  (concat [lparen]
                          (join semi (map #(if (= ":var" (head %)) (var-tokens %) (ex %)) (:V res)))
                          [rparen]))
                (block b)
                (mapcat (fn [c]
                          (let [[ch & cs] (:L c)]
                            (if (= "catch" (text ch))
                              (let [[ms more] (split-mods cs)
                                    [t nm b] more]
                                (concat [(T "catch" (:l ch)) lparen] (mods-tokens ms) (typ t) (name-tokens nm)
                                        [rparen] (block b)))
                              (concat [(T "finally" (:l ch))] (block (first cs))))))
                        clauses)))
      (#{"break" "continue"} h) (concat [(T h hl)] (when-let [lb (first xs)] [(T (text lb) (:l lb))]) [semi])
      (= h ":yield") (concat [(T "yield" hl)] (ex (first xs)) [semi])
      (= h "return") (concat [(T "return" hl)] (when-let [e (first xs)] (ex e)) [semi])
      (= h "throw") (concat [(T "throw" hl)] (ex (first xs)) [semi])
      (= h "assert") (let [[c d] xs]
                       (concat [(T "assert" hl)] (ex c) (when d (concat [(T ":")] (ex d))) [semi]))
      :else (concat (ex n) [semi]))))

;;; Declarations

(defn- method-tokens [n]
  (let [[_ & xs] (:L n)
        [ms xs] (split-mods xs)
        [tps xs] (if (vec? (first xs)) [(first xs) (rest xs)] [nil xs])
        [late xs] (split-with #(= ":ann" (head %)) xs)
        [a b] xs
        [rt nm xs] (cond
                     (vec? b) [nil a (rest xs)]
                     (or (nil? b) (= "do" (head b))) [nil a (rest xs)]
                     :else [a b (drop 2 xs)])
        [ps xs] (if (vec? (first xs)) [(first xs) (rest xs)] [nil xs])
        [dims xs] (split-with #(and (vec? %) (empty? (:V %))) xs)
        [throws xs] (if (= "throws" (head (first xs))) [(first xs) (rest xs)] [nil xs])
        [dflt xs] (if (= "default" (text (first xs))) [(second xs) (drop 2 xs)] [nil xs])
        body (first xs)]
    (concat (mods-tokens ms)
            (when tps (concat [{:t "<" :ng true :fwd true}] (join comma (map tparam-tokens (:V tps))) [gt]))
            (mapcat annotation late)
            (when rt (typ rt))
            (name-tokens nm)
            (when ps (concat [lparen-call] (join comma (map param-tokens (param-groups (:V ps)))) [rparen]))
            (mapcat (fn [d] [(assoc lbrack :l (:l d)) rbrack]) dims)
            (when throws (concat [(T "throws")] (join comma (map typ (rest (:L throws))))))
            (when dflt (concat [(T "default")] (if (vec? dflt) (expr-init dflt) (ex dflt))))
            (if body (block body) [semi]))))

(defn tparam-tokens [p]
  (if (atom? p)
    [(T (text p) (:l p))]
    (let [[anns more] (split-with #(= ":ann" (head %)) (:L p))
          [nm & more] more]
      (concat (mapcat annotation anns) [(T (text nm) (:l nm))]
              (when (seq more) (concat [(T "extends")] (join (T "&") (map typ (rest more)))))))))

(defn- enum-constant-tokens [c]
  (concat (doc-tokens c) (enum-constant-tokens* c)))

(defn- enum-constant-tokens* [c]
  (if (atom? c)
    [(T (text c) (:l c))]
    (let [[anns more] (split-with #(= ":ann" (head %)) (:L c))
          [nm & more] more
          [create-line more] (if (= "." (text (first more))) [(:l (first more)) (rest more)] [(:l nm) more])
          body (when (= "class" (head (last more))) (last more))
          as (if body (butlast more) more)]
      (concat (mapcat annotation anns) [(T (text nm) (:l nm))]
              (when (seq as) (args-tokens as create-line))
              (when body (concat [(T "{" (when (empty? as) create-line))] (members (rest (:L body)) nil) [(T "}")]))))))

(defn- member? [n]
  (or (#{":var" ":method" "do" "class" "interface" "enum" ":record" ":annotation"} (head n))
      (and (lst? n) (empty? (:L n)))))

(defn members [ns enum?]
  (if enum?
    (let [[cs ms] (split-with (complement member?) ns)]
      (concat (join comma (map enum-constant-tokens cs))
              [semi]
              (mapcat member ms)))
    (mapcat member ns)))

(defn member [n]
  (concat (doc-tokens n) (member* n)))

(defn- member* [n]
  (cond
    (= ":method" (head n)) (method-tokens n)
    :else (stmt n)))

(defn class-decl [n]
  (let [[hn & xs] (:L n)
        h (text hn)
        [ms xs] (split-mods xs)
        [nm & xs] xs
        vecs (take-while vec? xs)
        xs (drop (count vecs) xs)
        [tps comps] (if (= h ":record")
                      (if (= 2 (count vecs)) vecs [nil (first vecs)])
                      [(first vecs) nil])
        clauses (take-while #(#{"extends" "implements" ":permits"} (head %)) xs)
        body (drop (count clauses) xs)
        kw (case h ":record" "record" ":annotation" "@interface" h)]
    (concat (mods-tokens ms)
            [(T kw (:l nm)) (T (text nm) (:l nm))]
            (when tps (concat [lt] (join comma (map tparam-tokens (:V tps))) [gt]))
            (when comps (concat [lparen-call]
                                (join comma (map param-tokens (param-groups (:V comps))))
                                [rparen]))
            (mapcat (fn [c]
                      (let [[ch & ts] (:L c)]
                        (concat [(T (str/replace (text ch) #"^:" ""))] (join comma (map typ ts)))))
                    clauses)
            [(T "{")]
            (members body (= h "enum"))
            [(T "}")])))

(defn- module-tokens [n]
  (let [[_ & xs] (:L n)
        [ms xs] (split-mods xs)
        [open? xs] (if (= ":open" (text (first xs))) [true (rest xs)] [false xs])
        [nm & ds] xs]
    (concat (mods-tokens ms) (when open? [(T "open" (:l nm))]) [(T "module" (:l nm))] (name-tokens nm) [(T "{")]
            (mapcat (fn [d]
                      (let [[dh & ys] (:L d)
                            l (:l dh)]
                        (case (text dh)
                          ":requires" (concat [(T "requires" l)]
                                              (map #(T (str/replace (text %) #"^:" "")) (butlast ys))
                                              (name-tokens (last ys)) [semi])
                          (":exports" ":opens") (let [[p & more] ys]
                                                  (concat [(T (subs (text dh) 1) l)] (name-tokens p)
                                                          (when (seq more)
                                                            (concat [(T "to")] (join comma (map name-tokens (rest more)))))
                                                          [semi]))
                          ":uses" (concat [(T "uses" l)] (name-tokens (first ys)) [semi])
                          ":provides" (let [[s _ & is] ys]
                                        (concat [(T "provides" l)] (name-tokens s) [(T "with")]
                                                (join comma (map name-tokens is)) [semi])))))
                    ds)
            [(T "}")])))

(defn- top-level [n]
  (concat (doc-tokens n) (top-level* n)))

(defn- top-level* [n]
  (case (head n)
    "package" (let [[h & xs] (:L n)]
                (concat (mapcat annotation (butlast xs)) [(T "package" (:l (last xs)))] (name-tokens (last xs)) [semi]))
    "import" (let [[h & xs] (:L n)]
               (concat [(T "import" (:l h))]
                       (mapcat #(if (= ":module" (text %)) [(T "module")] (if (= "static" (text %)) [(T "static")] (name-tokens %))) xs)
                       [semi]))
    ":module" (module-tokens n)
    ":method" (method-tokens n)
    (stmt n)))

;;; Rendering

(defn- comment-open-after
  "Whether a block comment is still open after Java comment text `c`, given whether one was
  open before it."
  [open? ^String c]
  (let [n (count c)]
    (loop [i 0 open? open?]
      (cond
        (>= i (dec n)) open?
        open? (if (and (= \* (.charAt c i)) (= \/ (.charAt c (inc i)))) (recur (+ i 2) false) (recur (inc i) true))
        (and (= \/ (.charAt c i)) (= \/ (.charAt c (inc i)))) false
        (and (= \/ (.charAt c i)) (= \* (.charAt c (inc i)))) (recur (+ i 2) true)
        :else (recur (inc i) false)))))

(defn- render [tokens {:keys [indents comments lines]}]
  (let [out (StringBuilder.)
        line-buf (StringBuilder.)
        cur (volatile! 1)
        prev (volatile! nil)
        in-block (volatile! false)
        violations (transient [])
        comment-of (fn [l]
                     (when-let [c (comments l)]
                       (let [[lead c] (if @in-block
                                        (if-let [i (str/index-of c "*/")]
                                          [(subs c 0 (+ i 2)) (str/triml (subs c (+ i 2)))]
                                          [c nil])
                                        [nil c])
                             c (when (seq c) (if (and (nil? lead) (str/starts-with? c ";")) (str "//" (subs c 1)) c))]
                         (vreset! in-block (comment-open-after (boolean (and lead (not (str/includes? lead "*/"))))
                                                               (or c "")))
                         [lead c])))
        finish (fn []
                 (let [[lead c] (comment-of @cur)
                       code (str line-buf)
                       ind (get indents @cur "")
                       code (cond
                              (nil? lead) code
                              (str/blank? code) (str ind lead)
                              :else (str ind lead " " (subs code (count ind))))]
                   (.append out (cond
                                  (nil? c) code
                                  (str/blank? code) (str ind c)
                                  :else (str code " " c)))
                   (.append out "\n")
                   (.setLength line-buf 0)
                   (vreset! prev nil)
                   (vswap! cur inc)))
        goto (fn [l] (while (< @cur l) (finish)))]
    (doseq [[{:keys [t l g ng fwd] :as tk} & more] (take-while seq (iterate rest tokens))]
      (when (and fwd (not l))
        (when-let [nl (some :l more)]
          (when (> nl @cur) (goto nl))))
      (when l
        (if (< l @cur)
          (conj! violations {:token t :line l :at @cur})
          (goto l)))
      (if (zero? (.length line-buf))
        (.append line-buf (get indents @cur ""))
        (when (or (not (or g (:ng @prev)))
                  (and @prev (#{\+ \- \& \| \< \> \=} (last (:t @prev))) (#{\+ \- \& \| \= \>} (first t))))
          (.append line-buf " ")))
      (.append line-buf ^String t)
      (vreset! prev tk))
    (goto (max lines (reduce max 0 (keys comments))))
    (finish)
    {:text (str out) :violations (persistent! violations)}))

(defn translate
  "Translate javalisp `text` to Java. Returns {:text .. :violations [..]}."
  [text]
  (let [{:keys [forms] :as r} (rd/read-text text)
        tokens (doall (mapcat top-level forms))]
    (render tokens (assoc r :lines (count (re-seq #"\n" text))))))
