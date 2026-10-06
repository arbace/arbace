(ns arbace.javalisp.reader
  "A reader for javalisp text that keeps what the Clojure reader drops: the line of every
  token and closing bracket, each line's indentation, and each line's comment.

  It reads the subset javalisp uses: lists, vectors, symbols, keywords, numbers, strings
  and characters, with commas as whitespace and ; comments. Atoms come back as
  {:a text :l line}; lists and vectors as {:L/:V [nodes] :l line :cl line :detached bool},
  where :detached says whitespace precedes the closing bracket. A discarded string
  #_\"...\" (a doc comment) is kept as :pre on the form after it.")

(defn- delimiter? [c]
  (or (Character/isWhitespace (char c)) (#{\( \) \[ \] \{ \} \" \; \,} c)))

(defn read-text
  "Read javalisp `text` into {:forms [nodes] :comments {line text} :indents {line ws}
  :lines n}."
  [^String text]
  (let [n (count text)
        pos (volatile! 0)
        line (volatile! 1)
        line-start? (volatile! true)
        comments (volatile! {})
        indents (volatile! {})
        peek-c (fn [] (when (< @pos n) (.charAt text @pos)))
        next-c (fn []
                 (let [c (.charAt text @pos)]
                   (vswap! pos inc)
                   (when (= c \newline) (vswap! line inc) (vreset! line-start? true))
                   c))
        fail (fn [msg] (throw (ex-info (str "line " @line ": " msg) {:line @line})))
        skip-ws (fn []
                  (loop []
                    (when-let [c (peek-c)]
                      (cond
                        (= c \newline) (do (next-c) (recur))
                        (or (Character/isWhitespace c) (= c \,))
                        (do (if (and @line-start? (not= c \,))
                              (vswap! indents update @line str c)
                              (vreset! line-start? false))
                            (next-c) (recur))
                        (= c \;) (let [e (let [e (.indexOf text "\n" (int @pos))] (if (neg? e) n e))]
                                   (vswap! comments assoc @line (subs text (inc @pos) e))
                                   (vreset! pos e)
                                   (recur))))))
        token (fn [l]
                (let [start @pos]
                  (next-c)
                  (while (and (peek-c) (not (delimiter? (peek-c)))) (next-c))
                  {:a (subs text start @pos) :l l}))]
    (letfn [(read-form []
              (skip-ws)
              (if (and (= \# (peek-c)) (< (inc @pos) n) (= \_ (.charAt text (inc @pos))))
                (do (next-c) (next-c)
                    (let [d (read-form)]
                      (assoc (read-form) :pre (:a d))))
                (let [c (peek-c)
                      l @line]
                  (vreset! line-start? false)
                  (case c
                    nil (fail "unexpected end of input")
                    (\( \[) (read-coll c l)
                    (\) \]) (fail (str "unexpected " c))
                    (\{ \}) (fail "maps and sets are not javalisp")
                    \" (let [start @pos]
                         (next-c)
                         (loop []
                           (let [d (next-c)]
                             (cond (= d \\) (do (next-c) (recur))
                                   (= d \") nil
                                   :else (recur))))
                         {:a (subs text start @pos) :l l})
                    \\ (let [start @pos]
                         (next-c)
                         (next-c)
                         (while (and (peek-c) (not (delimiter? (peek-c)))) (next-c))
                         {:a (subs text start @pos) :l l})
                    (token l)))))
            (read-coll [c l]
              (next-c)
              (let [close (if (= c \() \) \])
                    kids (loop [acc []]
                           (skip-ws)
                           (let [d (peek-c)]
                             (cond
                               (nil? d) (fail (str "unclosed " c))
                               (= d close) acc
                               (#{\) \]} d) (fail (str "unexpected " d))
                               :else (recur (conj acc (read-form))))))
                    cl @line
                    detached (Character/isWhitespace (.charAt text (dec @pos)))]
                (vreset! line-start? false)
                (next-c)
                {(if (= c \() :L :V) kids :l l :cl cl :detached detached}))]
      (let [forms (loop [acc []]
                    (skip-ws)
                    (if (peek-c) (recur (conj acc (read-form))) acc))]
        {:forms forms :comments @comments :indents @indents :lines @line}))))
