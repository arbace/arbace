(in-ns 'go.example_com.multi.internal.wc)

(go/file "wc.go"
  :doc "Package wc counts lines, words and bytes.\n"
  :imports [[bufio "bufio"] [fmt "fmt"] [io "io"] [strings "strings"]])

(go/type Counts
  "Counts are the counts of a text.\n"
  (struct ^int Lines ^int Words ^int Bytes ^string Longest))

(go/method String ^string [^Counts c]
  (fmt/Sprintf "%d lines, %d words, %d bytes, longest word %q"
               (.-Lines c) (.-Words c) (.-Bytes c) (.-Longest c)))

(go/func Count
  "Count counts the lines of r, their words and their bytes (each line's newline counted).\n"
  [^io/Reader r] :results [Counts error]
  (let [^Counts c (zero Counts)
        sc (bufio/NewScanner r)]
    (while (.Scan sc)
      (let [line (.Text sc)]
        (inc! (.-Lines c))
        (set! (.-Bytes c) + (+ (len line) 1))
        (range [_ w (strings/Fields line)]
          (inc! (.-Words c))
          (when (> (len w) (len (.-Longest c)))
            (set! (.-Longest c) w)))))
    (return c (.Err sc))))
