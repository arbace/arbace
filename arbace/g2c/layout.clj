(ns arbace.g2c.layout
  "Writes Go forms (arbace.g2c.convert's in-memory forms) as Clojure text (doc/go/SPEC.md §10).

  Mode :lines: every form that begins a Go line is written on that line of the .clj file. A
  first pass (prepare) decides the breaks: a form whose Go line is after everything written
  before it starts a new line; it then carries its line as the reader will give it back, a list
  by itself (the reader's :line), a symbol, vector or @ form by explicit :line metadata, a
  literal in its parent's :go/breaks. Ordinary comments are written as ; comments at the end of
  their Go line, or as ;; lines. Mode :full writes every node's positions as :go/pos and
  :go/apos (§10.2) on the same layout."
  (:require [arbace.g2c.lit :as lit]
            [arbace.string :as str]))

(def ^:private private-ns "arbace.g2c.convert")

(defn- private? [k] (and (keyword? k) (= private-ns (namespace k))))

(defn- iobj? [x] (instance? arbace.lang.IObj x))

(defn- deref-form? [x] (and (seq? x) (:arbace.g2c.convert/deref (meta x))))

(defn form-line
  "The Go line a form begins on, when known."
  [x]
  (cond
    (lit/lit? x) (:line x)
    (iobj? x) (:arbace.g2c.convert/l (meta x))))

(def ^:dynamic *mode* :lines)
(def ^:dynamic *stats* nil)

(defn- note! [k] (when *stats* (swap! *stats* update k (fnil inc 0))))

;;; Pass 1: breaks

(defn- prep
  "Returns [x' cursor] for x written with the cursor (the last line written) at cursor."
  [x cursor]
  (cond
    (or (seq? x) (vector? x))
    (if (:arbace.g2c.convert/raw (meta x))
      [x cursor]
      (let [lines? (= *mode* :lines)
            m (meta x)
            [kids cursor breaks apos]
            (loop [i 0 xs (seq x) cursor cursor acc [] breaks [] apos (sorted-map)]
              (if-let [c (first xs)]
                (let [l (form-line c)
                      brk (and l (> l cursor))
                      c (cond
                          (lit/lit? c) (assoc c :brk brk)
                          (iobj? c)
                          (vary-meta c (fn [cm]
                                         (cond-> (assoc cm ::brk brk)
                                           (and lines? brk (or (not (seq? c)) (deref-form? c)))
                                           (assoc :line l)
                                           (and lines? l (not brk) (< l cursor) (seq? c))
                                           (as-> cm (do (note! :layout-late-list) (assoc cm :line l))))))
                          :else c)
                      cursor (if brk l cursor)
                      [c cursor] (prep c cursor)]
                  (recur (inc i) (next xs) cursor (conj acc c)
                         (if (and brk (lit/lit? c)) (conj breaks i) breaks)
                         (if (and (lit/lit? c) (:pos c)) (assoc apos i (:pos c)) apos)))
                [acc cursor breaks apos]))
            x' (if (seq? x) (apply list kids) kids)
            m (cond-> m
                (and lines? (seq breaks)) (assoc :go/breaks breaks)
                (and (= *mode* :full) (seq apos)) (assoc :go/apos apos))]
        [(with-meta x' m) cursor]))
    :else [x cursor]))

(defn prepare
  "The top-level forms with their breaks, starting at line 1."
  [forms]
  (loop [fs forms cursor 1 acc []]
    (if-let [f (first fs)]
      (let [l (form-line f)
            brk (and l (> l cursor))
            f (vary-meta f assoc ::brk brk)
            cursor (if brk l cursor)
            [f cursor] (prep f cursor)]
        (recur (next fs) cursor (conj acc f)))
      acc)))

;;; Pass 2: text

(declare text)

(def ^:private key-order
  [:line :alias :extern :const :var :assign :go/group :tag :val :inst :go/via :go/tag :doc])

(defn- key-rank [k]
  (let [i (.indexOf ^java.util.List key-order k)]
    (if (neg? i) (count key-order) i)))

(defn- sort-keys [ks]
  (sort-by (fn [k] [(key-rank k) (str k)]) ks))

(defn public-meta
  "The metadata written for x."
  [x]
  (let [m (when (iobj? x) (meta x))
        lines? (= *mode* :lines)
        pm (into {} (remove (fn [[k _]] (or (private? k) (= k ::brk)))) m)
        pm (if lines?
             pm
             (cond-> (dissoc pm :line :go/breaks :go/end)
               (seq (:arbace.g2c.convert/pos m)) (assoc :go/pos (:arbace.g2c.convert/pos m))))]
    pm))

(defn- meta-prefix [x]
  (let [pm (public-meta x)]
    (when (seq pm)
      (let [ks (sort-keys (keys pm))
            flags (filter #(true? (pm %)) ks)
            rest-ks (remove #(true? (pm %)) ks)
            sb (StringBuilder.)]
        (doseq [k flags] (.append sb (str "^" k " ")))
        (cond
          (empty? rest-ks) nil
          (and (= rest-ks [:tag]) (symbol? (:tag pm)) (empty? (public-meta (:tag pm))))
          (.append sb (str "^" (:tag pm) " "))
          :else
          (do (.append sb "^{")
              (.append sb (str/join " " (for [k rest-ks] (str k " " (text (pm k))))))
              (.append sb "} ")))
        (str sb)))))

(defn- atom-text [x]
  (cond
    (nil? x) "nil"
    (lit/lit? x) (:text x)
    (symbol? x) (str x)
    (keyword? x) (str x)
    (string? x) (lit/string-text x)
    (char? x) (lit/char-text (int x))
    (instance? arbace.lang.BigInt x) (str x "N")
    (instance? BigInteger x) (str x "N")
    (integer? x) (str x)
    (boolean? x) (str x)
    (instance? Double x) (str x)
    :else (throw (ex-info (str "can't write " (class x)) {:x x}))))

(defn text
  "x as text on one line (metadata values, the package file)."
  [x]
  (str (meta-prefix x)
       (cond
         (lit/lit? x) (:text x)
         (and (seq? x) (:arbace.g2c.convert/raw (meta x))) (:arbace.g2c.convert/raw (meta x))
         (deref-form? x) (str "@" (text (second x)))
         (seq? x) (str "(" (str/join " " (map text x)) ")")
         (vector? x) (str "[" (str/join " " (map text x)) "]")
         (map? x) (str "{" (str/join " " (for [[k v] x] (str (text k) " " (text v)))) "}")
         :else (atom-text x))))

(defn- comment-text
  "A Go comment line as the text after ; or ;;."
  [^String t]
  (cond
    (str/starts-with? t "//") (subs t 2)
    :else (str " " (str/trim t))))

(defn write-file
  "The text of a Go file's forms (prepared here) with its comments {line [text ...]}."
  [forms comments]
  (let [forms (prepare forms)
        sb (StringBuilder.)
        line (volatile! 1)
        code? (volatile! false)
        depth (volatile! 0)
        flush-comments!
        (fn []
          (when-let [cs (get comments @line)]
            (if @code?
              (.append sb (str " ;" (str/join " ;" (map comment-text cs))))
              (do (.append sb (apply str (repeat (* 2 @depth) " ")))
                  (.append sb (str ";;" (str/join " ;;" (map comment-text cs))))))))
        newline-to!
        (fn [l]
          (while (< @line l)
            (flush-comments!)
            (.append sb "\n")
            (vswap! line inc)
            (vreset! code? false))
          (.append sb (apply str (repeat (* 2 @depth) " "))))
        emit
        (fn emit [x]
          (when (and (or (iobj? x) (lit/lit? x))
                     (if (lit/lit? x) (:brk x) (::brk (meta x))))
            (newline-to! (form-line x)))
          (vreset! code? true)
          (let [p (meta-prefix x)]
            (when p (.append sb p)))
          (cond
            (and (seq? x) (:arbace.g2c.convert/raw (meta x))) (.append sb ^String (:arbace.g2c.convert/raw (meta x)))
            (deref-form? x) (do (.append sb "@") (emit (second x)))
            (or (seq? x) (vector? x))
            (do (.append sb (if (seq? x) "(" "["))
                (vswap! depth inc)
                (doseq [[i c] (map-indexed vector x)]
                  (when (and (pos? i)
                             (not (if (lit/lit? c) (:brk c) (and (iobj? c) (::brk (meta c))))))
                    (.append sb " "))
                  (emit c))
                (vswap! depth dec)
                (.append sb (if (seq? x) ")" "]")))
            :else (.append sb ^String (atom-text x))))]
    (doseq [f forms]
      (if (::brk (meta f))
        (emit f)
        (do (when @code? (.append sb " ")) (emit f))))
    (let [last-comment (if (seq comments) (apply max (keys comments)) 0)]
      (when (> last-comment @line)
        (vreset! depth 0)
        (newline-to! last-comment))
      (flush-comments!)
      (.append sb "\n"))
    (str sb)))

(defn write-package
  "The text of a package file: ns, go/package with one option per line, and the loads."
  [ns-sym pkg-form loads]
  (let [[head nm & opts] pkg-form]
    (str "(ns " ns-sym "\n  (:require [arbace.go :as go]))\n\n"
         "(" head " " nm
         (apply str (for [[k v] (partition 2 opts)] (str "\n  " k " " (text v))))
         ")\n\n"
         (apply str (for [l loads] (str "(load " (lit/string-text l) ")\n"))))))
