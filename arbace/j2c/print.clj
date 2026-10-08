(ns arbace.j2c.print
  "Prints converted forms as Clojure source text: ordered metadata, class references named
  by the file's import table, and a layout in the usual Clojure style."
  (:require [arbace.j2c.forms :as f]
            [arbace.string :as str])
  (:import [arbace.j2c.forms CRef Raw]))

(def width 100)

(def ^:dynamic *resolve*
  "Function from a CRef to the symbol text naming its class (without dims or member)."
  (fn [^CRef r] (:name r)))

;;; Atoms

(def char-names
  {\newline "newline" \space "space" \tab "tab" \backspace "backspace" \formfeed "formfeed"
   \return "return"})

(defn printable? [c]
  (let [ch (char c)
        t (Character/getType ch)]
    (not (or (Character/isISOControl ch) (Character/isWhitespace ch) (Character/isSurrogate ch)
             (Character/isSpaceChar ch)
             (contains? #{(int Character/UNASSIGNED) (int Character/FORMAT)
                          (int Character/PRIVATE_USE)} t)))))

(defn char-text
  "Clojure character literal text for `c`, or nil when the reader cannot read it (a lone
  surrogate)."
  [c]
  (let [ch (char c)]
    (cond
      (char-names ch) (str \\ (char-names ch))
      (Character/isSurrogate ch) nil
      (printable? (int ch)) (str \\ ch)
      :else (format "\\u%04x" (int ch)))))

(defn string-text [^String s]
  (let [sb (StringBuilder. "\"")]
    (doseq [c s]
      (case c
        \" (.append sb "\\\"")
        \\ (.append sb "\\\\")
        \newline (.append sb "\\n")
        \tab (.append sb "\\t")
        \return (.append sb "\\r")
        \backspace (.append sb "\\b")
        \formfeed (.append sb "\\f")
        (if (or (= c \space) (printable? (int c)))
          (.append sb c)
          (.append sb (format "\\u%04x" (int c))))))
    (str (.append sb "\""))))

(defn cref-text [^CRef r]
  (str (*resolve* r)
       (when (pos? (:dims r)) (str "/" (:dims r)))
       (when-let [m (:member r)] (if (= m ".") "." (str "/" m)))))

(declare flat)

(defn- meta-text
  "The metadata prefix of `x`, e.g. \"^:public ^String \"."
  [x]
  (let [items (f/items x)]
    (when (seq items)
      (apply str
             (for [it items]
               (cond
                 (keyword? it) (str "^" it " ")
                 (= :tag (first it)) (let [t (second it)]
                                       (if (and (or (symbol? t) (instance? CRef t)) (empty? (f/items t)))
                                         (str "^" (flat t) " ")
                                         (str "^{:tag " (flat t) "} ")))
                 (= :ann (first it)) (str "^{" (flat (nth it 1)) " " (flat (nth it 2)) "} ")
                 (= :param-tags (first it)) (str "^" (flat (vec (second it))) " ")
                 (= :type-args (first it)) (str "^{:type-args " (flat (vec (second it))) "} ")
                 (= :qualifier (first it)) (str "^{:qualifier " (flat (second it)) "} ")
                 :else (throw (ex-info "bad meta item" {:item it}))))))))

(defn- double-text [^double d]
  (let [s (Double/toString d)]
    (cond
      (Double/isNaN d) "##NaN"
      (Double/isInfinite d) (if (pos? d) "##Inf" "##-Inf")
      :else s)))

(defn- atom-text [x]
  (cond
    (nil? x) "nil"
    (instance? CRef x) (cref-text x)
    (instance? Raw x) (:text x)
    (symbol? x) (str x)
    (keyword? x) (str x)
    (string? x) (string-text x)
    (char? x) (or (char-text x) (format "(unchecked-char 0x%04x)" (int x)))
    (instance? Double x) (double-text x)
    (instance? Float x) (str "(float " (double-text (double x)) ")")
    (integer? x) (str x)
    (true? x) "true"
    (false? x) "false"
    :else (throw (ex-info (str "can't print " (class x)) {:x x}))))

(def ^:dynamic ^java.util.IdentityHashMap *flat-cache* nil)

(declare flat*)

(defn flat
  "`x` printed on one line."
  [x]
  (if (and *flat-cache* (coll? x))
    (or (.get *flat-cache* x)
        (let [s (flat* x)] (.put *flat-cache* x s) s))
    (flat* x)))

(defn- flat*
  [x]
  (str (meta-text x)
       (cond
         (instance? CRef x) (atom-text x)
         (instance? Raw x) (atom-text x)
         (map? x) (str "{" (str/join " " (for [[k v] x] (str (flat k) " " (flat v)))) "}")
         (vector? x) (str "[" (str/join " " (map flat x)) "]")
         (seq? x) (str "(" (str/join " " (map flat x)) ")")
         :else (atom-text x))))

;;; Layout

(def body-heads
  "Heads whose first n arguments stay on the first line, the rest indented by 2."
  {'defclass 1 'method :method 'constructor :method 'field 1 'let 1 'loop 1 'when 1
   'when-not 1 'while 1 'for-each 1 'with-resources 1 'locking 1 'label 1 'letclass 1
   'if-instance 1 'when-instance 1 'anon 2 'lambda :lambda 'try 0 'do 0 'finally 0
   'initializer 0 'static-initializer 0 'constants 0 'catch 2 'java-assert 1 'dotimes 1
   'when-some 1 'binding 1 'defmodule 1})

(def pair-heads #{'cond 'switch})

(defn- head-sym [x]
  (when (and (seq? x) (symbol? (first x)) (not (namespace (first x))))
    (first x)))

(declare pp)

(defn- indent [n] (apply str (repeat n \space)))

(defn- fits? [s col]
  (and (not (str/includes? s "\n")) (<= (+ col (count s)) width)))

(defn- pp-seq-lines
  "Lay out `xs` one per line starting at column `col`; first line is not indented."
  [xs col sep]
  (str/join (str sep (indent col)) (map #(pp % col) xs)))

(defn- method-header-count
  "How many arguments of a method/constructor/lambda form belong to its header: everything up to
  and including the parameter vector."
  [x]
  (let [args (vec (rest x))
        i (loop [i 0]
            (cond
              (>= i (count args)) nil
              (keyword? (args i)) (recur (+ i 2))
              (vector? (args i)) i
              :else (recur (inc i))))]
    (if i (inc i) (count args))))

(defn- class-body? [x] (or (= 'defclass (head-sym x)) (and (seq? x) (:class-body? (meta x)))))

(defn- pp-body-form
  "Head, n header args on the first line, then the body at col+2."
  [x col n]
  (let [m (meta-text x)
        col0 (+ col (count m))
        [hd & args] x
        header (take n args)
        body (drop n args)
        hs (str "(" (flat hd))
        hcol (+ col0 (count hs) 1)
        ;; header args laid out one after another on the first line where they fit
        header-text (loop [hs hs, c (dec hcol), [a & more :as as] header]
                      (if (empty? as)
                        hs
                        (let [t (pp a (inc c))]
                          (recur (str hs " " t)
                                 (if (str/includes? t "\n")
                                   (+ (count (last (str/split-lines t))) 0)
                                   (+ c 1 (count t)))
                                 more))))
        bcol (+ col0 2)
        defclass? (or (= hd 'defclass) (:class-body? (meta x)))
        ;; defclass: record components on the first line, options (keyword value pairs) each
        ;; on its own line, then members
        [comps body] (if (and defclass? (vector? (first body))) [(first body) (rest body)] [nil body])
        header-text (if comps (str header-text " " (pp comps (+ (count (last (str/split-lines header-text))) col0 1))) header-text)
        [opts members] (if defclass?
                         (let [v (vec body)
                               n (loop [i 0] (if (and (< i (count v)) (keyword? (v i))) (recur (+ i 2)) i))]
                           [(partition 2 (subvec v 0 n)) (subvec v n)])
                         [nil body])
        sep-members (if (or defclass? (#{'anon 'constants} hd)) "\n\n" "\n")]
    (str m header-text
         (apply str (for [[k v] opts] (str "\n" (indent bcol) (flat k) " " (pp v (+ bcol (count (flat k)) 1)))))
         (when (seq members)
           (str (if (and defclass? (seq opts)) "\n\n" "\n")
                (indent bcol)
                (str/join (str (if (= 'constants hd) "\n" sep-members) (indent bcol))
                          (map #(pp % bcol) members))))
         ")")))

(defn- pp-pairs [x col]
  (let [m (meta-text x)
        col0 (+ col (count m))
        [hd & args] x
        [pre args] (if (= hd 'switch) [[(first args)] (rest args)] [[] args])
        hs (str "(" (flat hd) (apply str (map #(str " " (pp % (+ col0 (count (flat hd)) 2))) pre)))
        pairs (partition-all 2 args)
        c2 (+ col0 2)]
    (str m hs
         (apply str
                (for [[a b :as p] pairs]
                  (if (= 1 (count p))
                    (str "\n" (indent c2) (pp a c2))
                    (let [one (str (flat a) " " (flat b))]
                      (if (fits? one c2)
                        (str "\n" (indent c2) one)
                        (str "\n" (indent c2) (pp a c2) "\n" (indent (+ c2 2)) (pp b (+ c2 2))))))))
         ")")))

(defn- pp-if [x col]
  (let [m (meta-text x)
        col0 (+ col (count m))
        [_ c & branches] x
        ct (pp c (+ col0 4))]
    (str m "(if " ct
         (apply str (for [b branches] (str "\n" (indent (+ col0 4)) (pp b (+ col0 4)))))
         ")")))

(defn- pp-bindings
  "A binding vector [a 1 b 2], pairs one per line."
  [v col]
  (let [m (meta-text v)
        col0 (+ col (count m) 1)
        pairs (partition-all 2 v)]
    (str m "["
         (str/join (str "\n" (indent col0))
                   (for [[a b :as p] pairs]
                     (let [at (flat a)]
                       (if (= 1 (count p))
                         (pp a col0)
                         (let [bt (pp b (+ col0 (count at) 1))]
                           (if (or (fits? (str at " " bt) col0)
                                   (and (not (str/includes? at "\n"))
                                        (<= (+ col0 (count at) 1 (count (first (str/split-lines bt)))) width)))
                             (str at " " bt)
                             (str at "\n" (indent (+ col0 2)) (pp b (+ col0 2)))))))))
         "]")))

(def binding-heads #{'let 'loop 'for-each 'with-resources 'if-instance 'when-instance 'dotimes
                     'when-some 'binding})

(defn- too-wide? [^String s col]
  (let [lines (str/split-lines s)]
    (or (> (+ col (count (first lines))) width)
        (some #(> (count %) width) (rest lines)))))

(defn- pp-call [x col]
  (let [m (meta-text x)
        col0 (+ col (count m))
        [hd & args] x
        ht (flat hd)
        acol (+ col0 2 (count ht))
        aligned (when (and (seq args) (<= acol 48)
                           (or (fits? (str ht " " (flat (first args))) (inc col0))
                               (<= (count ht) 12)))
                  (str m "(" ht " " (pp-seq-lines args acol "\n") ")"))]
    (cond
      (empty? args) (str m "(" ht ")")
      (and aligned (not (too-wide? aligned col))) aligned
      :else (str m "(" (pp hd (inc col0)) "\n" (indent (+ col0 2)) (pp-seq-lines args (+ col0 2) "\n") ")"))))

(def ^:dynamic ^java.util.IdentityHashMap *pp-cache*
  "Within form-text: form (by identity) -> {col text}. pp-call lays out its arguments twice
  (aligned, then indented when too wide), so without it a chain of nested calls (the JDK's
  generated tables, a string concatenation of hundreds of lines) takes time exponential in its
  depth."
  nil)

(declare pp*)

(defn pp
  "`x` laid out starting at column `col`."
  [x col]
  (if (and *pp-cache* (coll? x))
    (let [^java.util.HashMap m (or (.get *pp-cache* x)
                                   (let [m (java.util.HashMap.)] (.put *pp-cache* x m) m))]
      (or (.get m col)
          (let [s (pp* x col)] (.put m col s) s)))
    (pp* x col)))

(defn- pp*
  [x col]
  (let [s (flat x)]
    (if (and (fits? s col)
             (not (class-body? x))
             (not (and (seq? x) (#{'method 'constructor 'defclass 'anon 'letclass} (head-sym x))
                       (> (count s) 72))))
      s
      (cond
        (and (vector? x) (empty? x)) s
        (vector? x)
        (let [m (meta-text x) c (+ col (count m) 1)]
          (if (every? #(not (coll? %)) x)
            ;; atoms: fill lines
            (let [sb (StringBuilder. (str m "["))]
              (loop [first? true line-len 0 [a & more :as as] (seq x)]
                (when as
                  (let [t (flat a)]
                    (cond
                      first? (do (.append sb t) (recur false (+ c (count t)) more))
                      (<= (+ line-len 1 (count t)) width)
                      (do (.append sb " ") (.append sb t) (recur false (+ line-len 1 (count t)) more))
                      :else (do (.append sb "\n") (.append sb (indent c)) (.append sb t)
                                (recur false (+ c (count t)) more))))))
              (str (.append sb "]")))
            (str m "[" (pp-seq-lines x c "\n") "]")))
        (map? x) s
        (and (seq? x) (seq x))
        (let [h (head-sym x)
              x (if (and (binding-heads h) (vector? (second x)))
                  ;; lay out the bindings vector specially
                  x x)]
          (cond
            (:class-body? (meta x)) (pp-body-form x col 0)
            (= h 'if) (pp-if x col)
            (pair-heads h) (pp-pairs x col)
            (binding-heads h)
            (let [m (meta-text x)
                  col0 (+ col (count m))
                  hs (str "(" h " ")
                  bv (pp-bindings (second x) (+ col0 (count hs)))]
              (str m hs bv
                   (apply str (for [b (drop 2 x)] (str "\n" (indent (+ col0 2)) (pp b (+ col0 2)))))
                   ")"))
            (contains? body-heads h)
            (let [n (body-heads h)
                  n (if (keyword? n) (method-header-count x) n)
                  ;; (anon Inner [args] :outer o ...): the outer instance on the first line
                  n (if (and (= h 'anon) (= :outer (nth x (inc n) nil))) (+ n 2) n)]
              (pp-body-form x col n))
            :else (pp-call x col)))
        :else s))))

(defn form-text [x]
  (binding [*flat-cache* (java.util.IdentityHashMap.)
            *pp-cache* (java.util.IdentityHashMap.)]
    (pp x 0)))
