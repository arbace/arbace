(ns arbace.javalisp.layout
  "Layout nodes and the line-preserving printer.

  A node is one of
    {:a \"text\" :l line}            an atom (token)
    {:L [nodes] :l line :cl line}   a list ( ... )
    {:V [nodes] :l line :cl line}   a vector [ ... ]
  :l is the line the token (or the opening bracket) must be printed on, nil when free.
  A free opening bracket goes on the line of the first anchored token inside it.
  :cl, when present, puts the closing bracket on that line; :detach puts whitespace before it.
  {:a \",\" :glue true} is printed right after the previous token. :pre on any node is a doc
  comment printed before it as a discarded string, #_\"/** ... */\".")

(defn kids [xs]
  (into [] (comp (mapcat #(if (sequential? %) (kids %) [%])) (remove nil?)) xs))

(defn tok
  ([s] {:a s})
  ([s l] {:a s :l l}))

(defn lst [l & xs] {:L (kids xs) :l l})
(defn vect [l & xs] {:V (kids xs) :l l})
(def comma {:a "," :glue true})

(defn atom? [n] (contains? n :a))
(defn children [n] (or (:L n) (:V n)))

(defn first-line
  "The line of the first anchored token of node `n`, or nil."
  [n]
  (or (:l n)
      (some first-line (children n))))

(defn last-line
  "The line of the last anchored token of node `n` (its closing bracket included), or nil."
  [n]
  (or (:cl n)
      (some last-line (rseq (or (children n) [])))
      (:l n)))

(defn- events [n]
  (concat
   (when-let [d (:pre n)] [[:atom (str "#_" (pr-str d)) (first-line n) false]])
   (cond
     (atom? n) [[:atom (:a n) (:l n) (:glue n)]]
     :else (let [[o c] (if (:L n) ["(" ")"] ["[" "]"])]
             (concat [[:open o (first-line n)]]
                     (mapcat events (children n))
                     [[:close c (:cl n) (:detach n)]])))))

(defn render
  "Print `nodes` (top-level forms) so that every anchored token lands on its line.
  opts: :indent (fn [line] leading-whitespace), :comments {line text}, :lines (minimum
  number of lines). Returns {:text .. :violations [..]}: a violation is an anchored token
  that could not be placed because the output had already passed its line."
  [nodes {:keys [indent comments lines] :or {indent (constantly "") comments {}}}]
  (let [out (StringBuilder.)
        cur (volatile! 1)
        line-buf (StringBuilder.)
        violations (transient [])
        after-open (volatile! false)
        finish-line (fn []
                      (let [c (comments @cur)]
                        (cond
                          (and c (pos? (.length line-buf))) (.append line-buf (str " ;" c))
                          c (.append line-buf (str (indent @cur) ";" c))))
                      (.append out (str line-buf))
                      (.append out "\n")
                      (.setLength line-buf 0)
                      (vswap! cur inc))
        goto (fn [l]
               (while (< @cur l) (finish-line)))
        emit (fn [^String text l spacing open?]
               (when l
                 (if (< l @cur)
                   (conj! violations {:token text :line l :at @cur})
                   (goto l)))
               (let [n (.length line-buf)]
                 (cond
                   (zero? n) (.append line-buf (indent @cur))
                   (= spacing :glue) nil
                   (= spacing :space) (.append line-buf " ")
                   :else (when-not (or @after-open
                                       (Character/isWhitespace (.charAt line-buf (dec n))))
                           (.append line-buf " "))))
               (.append line-buf text)
               (vreset! after-open open?))]
    (doseq [n nodes
            [kind text l glue] (events n)]
      (case kind
        :atom (emit text l (if glue :glue :auto) false)
        :open (emit text l :auto true)
        :close (emit text l (if glue :space :glue) false)))
    (goto (max (or lines 0) (reduce max 0 (keys comments))))
    (finish-line)
    {:text (str out) :violations (persistent! violations)}))
