(ns arbace.g2c.print-emit
  "The emitter of g2c's printer (doc/go/PRINTER-NOTES.md): writes Go tokens, tracks the
  line, indentation and Go's semicolon rule, and places tokens on the lines the forms record
  (doc/go/SPEC.md §10.1, §12.3).

  The state is one map of volatiles bound to *p* while a file is printed. Text is written
  with tabs for indentation and vertical tabs between aligned cells; print-text/align turns
  it into gofmt's spacing at the end."
  (:require [arbace.g2c.print-text :as text]
            [arbace.string :as str]))

(def ^:dynamic *p* nil)

(def ^:dynamic *next*
  "The line of what follows the construct being printed, when known: the room a closing
  brace or a statement without a recorded line may take (mode :lines)."
  nil)

(defn new-state
  "opts: :lines? (place forms on their recorded lines), :line-directives? (write //line or
  /*line*/ directives where a recorded line cannot be reached by layout)."
  [opts]
  {:sb (StringBuilder.)
   :line (volatile! 1)        ; the line of the cursor, as gc will see it
   :bol (volatile! true)      ; nothing but indentation written on this line yet
   :indent (volatile! 0)      ; the indentation of the next line
   :cur-indent (volatile! 0)  ; the indentation of this line
   :last (volatile! "")       ; the last token
   :semi (volatile! false)    ; a newline after the last token would insert a semicolon
   :ws (volatile! nil)        ; pending whitespace: nil, \space or \u000B
   :opens (volatile! [])      ; indentation of the lines where open brackets were written
   :lines? (boolean (:lines? opts))
   :directives? (:line-directives? opts true)})

(defn lines? [] (:lines? *p*))
(defn line [] @(:line *p*))
(defn bol? [] @(:bol *p*))

(defn target
  "The recorded line of a form (mode :lines): :line metadata on lists, and on symbols and
  vectors that begin a Go line."
  [f]
  (when (lines? )
    (let [l (:line (meta f))]
      (when (integer? l) (long l)))))

(defn room?
  "Whether n more lines fit before *next* (always, when it is unknown or out of mode
  :lines)."
  [n]
  (or (not (lines?)) (nil? *next*) (> (long *next*) (+ (line) (long n)))))

;; ---------------------------------------------------------------------------------------
;; Tokens

(def keywords
  #{"break" "case" "chan" "const" "continue" "default" "defer" "else" "fallthrough" "for"
    "func" "go" "goto" "if" "import" "interface" "map" "package" "range" "return" "select"
    "struct" "switch" "type" "var"})

(defn- semi-after?
  "Go's semicolon rule: a newline after this token ends the statement."
  [^String s]
  (cond
    (#{"break" "continue" "fallthrough" "return" "++" "--" ")" "]" "}"} s) true
    (keywords s) false
    (zero? (.length s)) false
    :else (let [c (.charAt s (dec (.length s)))]
            (or (Character/isLetterOrDigit c) (= c \_) (= c \") (= c \') (= c \`)
                (> (int c) 127)))))

(defn- may-combine?
  "go/printer's mayCombine, extended: the two tokens would scan as other tokens unless
  separated."
  [^String prev ^String next]
  (when (and (pos? (.length prev)) (pos? (.length next)))
    (let [p (.charAt prev (dec (.length prev)))
          n (.charAt next 0)]
      (or (and (= prev "+") (= n \+))
          (and (= prev "-") (= n \-))
          (and (= p \/) (or (= n \*) (= n \/)))
          (and (= prev "<") (or (= n \-) (= n \<)))
          (and (= prev "&") (or (= n \&) (= n \^)))
          (and (Character/isDigit p) (= n \.) (re-matches #"[0-9]+" prev))
          (and (or (Character/isLetterOrDigit p) (= p \_))
               (or (Character/isLetterOrDigit n) (= n \_)))))))

(defn- start-line! []
  (let [p *p*]
    (when @(:bol p)
      (let [n (max 0 (long @(:indent p)))
            ^StringBuilder sb (:sb p)]
        (dotimes [_ n] (.append sb \tab))
        (vreset! (:cur-indent p) n)
        (vreset! (:bol p) false)
        (vreset! (:ws p) nil)))))

(defn tok
  "Writes a token."
  [^String s]
  (let [p *p* ^StringBuilder sb (:sb p)
        at-bol @(:bol p)]
    (start-line!)
    (let [ws @(:ws p)]
      (cond
        at-bol nil
        ws (.append sb (char ws))
        (may-combine? @(:last p) s) (.append sb \space)))
    (vreset! (:ws p) nil)
    (.append sb s)
    (vreset! (:last p) s)
    (vreset! (:semi p) (semi-after? s))
    nil))

(defn raw-tok
  "Writes a token that may span lines (a raw string): its line breaks count, and alignment
  leaves it whole."
  [^String s]
  (tok (text/escape s))
  (vswap! (:line *p*) + (count (filter #(= % \newline) s)))
  (vreset! (:semi *p*) true))

(defn sp
  "A blank before the next token on this line."
  []
  (when-not (bol?)
    (when-not (= @(:ws *p*) (char 11)) (vreset! (:ws *p*) \space))))

(defn vtab
  "An alignment separator before the next token (gofmt's vtab)."
  []
  (when-not (bol?)
    ;; a second separator in a row is an empty cell
    (when (= @(:ws *p*) (char 11)) (.append ^StringBuilder (:sb *p*) (char 11)))
    (vreset! (:ws *p*) (char 11))))

(defn nl
  "Ends the line; the next line is indented by indent. form-feed? ends the alignment
  sections too."
  ([indent] (nl indent false))
  ([indent form-feed?]
   (let [p *p* ^StringBuilder sb (:sb p)]
     (vreset! (:ws p) nil)
     (.append sb (if form-feed? (char 12) (char 10)))
     (vswap! (:line p) inc)
     (vreset! (:bol p) true)
     (vreset! (:indent p) indent)
     nil)))

(defn cur-indent [] @(:cur-indent *p*))

(defn push-open!
  "Records an open bracket (or a statement) whose continuation lines indent one more than
  indent (default: this line's)."
  ([] (push-open! (if (bol?) @(:indent *p*) (cur-indent))))
  ([indent] (vswap! (:opens *p*) conj indent) nil))

(defn pop-open! []
  (let [o (peek @(:opens *p*))]
    (vswap! (:opens *p*) pop)
    o))

(defn cont-indent
  "The indentation of a continuation line."
  []
  (let [o (peek @(:opens *p*))]
    (inc (long (or o 0)))))

(defn comment-line
  "Writes a whole-line comment (doc comment line, directive) at indent and ends the line.
  //line directives start at column 1, as gc requires."
  [^String s indent]
  (let [p *p* ^StringBuilder sb (:sb p)]
    (when-not (bol?) (nl indent))
    (vreset! (:indent p) (if (str/starts-with? s "//line ") 0 indent))
    (start-line!)
    (.append sb (text/escape s))
    (vreset! (:last p) "")
    (vreset! (:semi p) false)
    (nl indent)))

(defn- set-line! [l] (vreset! (:line *p*) l))

(defn line-directive-inline
  "/*line :N:1*/ before the next token: gc puts it on line N (a column keeps the file name)."
  [n]
  (let [p *p* ^StringBuilder sb (:sb p)]
    (start-line!)
    (when-let [ws @(:ws p)] (.append sb (char ws)))
    (.append sb (str "/*line :" n ":1*/"))
    (vreset! (:ws p) nil)
    (set-line! n)))

(defn line-directive-own
  "//line :N:1 on a line of its own, at the start of a line: the line after it is N."
  [n]
  (let [ind @(:indent *p*)]
    (comment-line (str "//line :" n ":1") ind)
    (set-line! n)))

;; ---------------------------------------------------------------------------------------
;; Layout

(defn break-to!
  "Inside an expression, before a form recorded at line t (or :break, a new line): line
  breaks where Go allows them, else a /*line*/ directive."
  [t]
  (when t
    (let [t (if (= t :break) (if (bol?) (line) (inc (line))) t)]
      (cond
        (> t (line))
        (if (and @(:semi *p*) (not (bol?)))
          (when (:directives? *p*) (line-directive-inline t))
          (let [ci (cont-indent)]
            (while (< (line) t) (nl ci))))
        (and (< t (line)) (:directives? *p*))
        (line-directive-inline t)))))

(defn stmt-break!
  "Before a statement, a declaration or another element of a list whose elements may be
  separated by a line break or by sep (\";\" or \",\"): moves to its line t (nil: unknown).
  first? when it follows the opening bracket. indent is the element's indentation. Returns
  true when a line break was made."
  [t first? sep indent]
  (let [newline! (fn [n] (dotimes [_ (max 1 n)] (nl indent)) true)
        same! (fn [] (if first? (sp) (do (when sep (tok sep)) (sp))) false)]
    (cond
      (bol?) (do (vreset! (:indent *p*) indent) false)
      (not (lines?)) (newline! 1)
      (nil? t) (if (room? 1) (newline! 1) (same!))
      (= t :break) (newline! 1)
      (> (long t) (line)) (newline! (- (long t) (line)))
      (= (long t) (line)) (same!)
      :else (do (newline! 1)
                (when (:directives? *p*) (line-directive-own t))
                true))))

(defn close-break!
  "Before a closing bracket: end, its recorded line (or nil); multiline?, whether the
  construct already spans lines; always?, whether gofmt's layout puts it on a line of its
  own. Returns true when the bracket goes on a new line (indented as the line of its
  opening, indent)."
  [end multiline? always? indent]
  (cond
    (and (lines?) (integer? end))
    (cond
      (> (long end) (line)) (do (while (< (line) (long end)) (nl indent)) true)
      (= (long end) (line)) false
      :else (do (when (:directives? *p*) (line-directive-inline end)) false))
    (lines?) (if (and (or multiline? always?) (room? 1)) (do (nl indent) true) false)
    always? (do (nl indent) true)
    :else false))

(defn result
  "The text written, aligned."
  []
  (let [s (.toString ^StringBuilder (:sb *p*))
        s (str/replace s #"[ \x0B]+\n" "\n")]
    (text/align (if (str/ends-with? s "\n") s (str s "\n")))))
