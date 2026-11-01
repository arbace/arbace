;;; tools/schemedump/dump.ss, the oracle of repr/scheme (doc/scheme/SPEC.md, §9): Chez Scheme's reading of
;;; files as data Arbace reads.
;;;
;;;   chez --script tools/schemedump/dump.ss [--mode r6rs|default] FILE...
;;   chez --script tools/schemedump/dump.ss --scan KIND
;;;
;;; For each FILE, one map on one line (the Clojure reader's, not strict EDN), in the order of the arguments:
;;;
;;;   {:file "FILE" :mode :r6rs
;;;    :data [{:off [START END] :form FORM :write "TEXT"} ...]     one per top-level datum, in order
;;;    :error {:message "..." :at N}                                  only when Chez's read stopped on an error
;;;    }
;;;
;;; FORM is the datum spelled as Scheme forms (SPEC §3.1), written here independently of the reader in Arbace
;;; (this file is the second implementation the check compares); TEXT is Chez's `write` of the datum, as a
;;; string (the printer's oracle). START and END are the datum's character offsets in the file (Chez's source
;;; object bfp and efp). The reading is Chez's `read` on a textual port over the file's text, in R6RS mode
;;; (default): the text is read after the directive `#!r6rs`, which switches Chez's reader to R6RS (it
;;; rejects `#!eof`, `{`, `|...|` symbols, ... that Chez's default mode accepts); the seven characters of the
;;; directive are taken off the offsets. `--mode default` reads the text as it stands (Chez's own default
;;; mode, which accepts those extensions). The error's message is Chez's with the "Exception in read: "
;;; prefix and the position suffix taken off; :at is the character offset of the position Chez reports.
;;;
;;; Written in R6RS (rnrs) and the few Chez procedures that give source positions
;;; (get-datum/annotations, make-source-file-descriptor); Chez Scheme 10.3.0 (doc/scheme/SCHEME-SURVEY.md, 2).
(import (rnrs) (only (chezscheme) command-line get-datum/annotations make-source-file-descriptor
                      annotation-source annotation-stripped source-object-bfp source-object-efp
                      display-condition print-subnormal-precision))

;; ---- output helpers (a string port per file, written at once)

(define (put o . xs) (for-each (lambda (x) (cond [(string? x) (put-string o x)] [(char? x) (put-char o x)] [else (put-string o (number->string x))])) xs))

(define (hex4 n) (let ([s (number->string n 16)]) (string-append (make-string (- 4 (string-length s)) #\0) s)))

;; a string in the Clojure reader's syntax: printable ASCII as it stands but " and \, every other
;; character as \uXXXX (a character above U+FFFF as its UTF-16 pair)
(define (put-clj-string o s)
  (put-char o #\")
  (string-for-each
   (lambda (c)
     (let ([n (char->integer c)])
       (cond [(char=? c #\") (put-string o "\\\"")]
             [(char=? c #\\) (put-string o "\\\\")]
             [(and (>= n 32) (< n 127)) (put-char o c)]
             [(< n #x10000) (put-string o "\\u") (put-string o (hex4 n))]
             [else (let ([m (- n #x10000)])
                     (put-string o "\\u") (put-string o (hex4 (+ #xD800 (div m 1024))))
                     (put-string o "\\u") (put-string o (hex4 (+ #xDC00 (mod m 1024)))))])))
   s)
  (put-char o #\"))

;; ---- the forms

;; a symbol is bare when the Clojure reader reads its name back as the same symbol (SPEC §3.1): by rule,
;; validated against the reader by the check
(define (bare-char? c)
  (let ([n (char->integer c)])
    (or (and (>= n 97) (<= n 122)) (and (>= n 65) (<= n 90)) (and (>= n 48) (<= n 57))
        (memv c '(#\! #\$ #\& #\* #\+ #\- #\. #\< #\= #\> #\? #\_)))))
(define (digit? c) (and (char>=? c #\0) (char<=? c #\9)))
(define (bare-symbol? s)
  (let ([n (string-length s)])
    (and (> n 0)
         (let loop ([i 0]) (or (= i n) (and (bare-char? (string-ref s i)) (loop (+ i 1)))))
         (let ([c (string-ref s 0)])
           (and (not (digit? c))
                (or (not (memv c '(#\+ #\- #\.)))
                    (= n 1) (not (digit? (string-ref s 1))))
                (not (and (char=? c #\.) (= n 1)))))
         (not (member s '("nil" "true" "false"))))))

(define (put-char-form o c)
  (let ([n (char->integer c)])
    (cond [(= n 32) (put-string o "\\space")] [(= n 10) (put-string o "\\newline")] [(= n 9) (put-string o "\\tab")]
          [(= n 13) (put-string o "\\return")] [(= n 8) (put-string o "\\backspace")] [(= n 12) (put-string o "\\formfeed")]
          [(and (> n 32) (< n 127)) (put-char o #\\) (put-char o c)]
          [(< n #x10000) (put-string o "\\u") (put-string o (hex4 n))]
          [else (put-string o "(scm/char ") (put o n) (put-string o ")")])))

(define (long-range? n) (and (>= n -9223372036854775808) (<= n 9223372036854775807)))

(define (put-real o x)
  (cond [(and (integer? x) (exact? x))
         (put o (number->string x)) (unless (long-range? x) (put-char o #\N))]
        [(exact? x) (put o (number->string x))]               ; a ratio n/d
        [(or (nan? x) (infinite? x)) (put-string o "(scm/flonum ") (put-clj-string o (number->string x)) (put-string o ")")]
        [else (put o (parameterize ([print-subnormal-precision #f]) (number->string x)))]))

(define (put-form o d)
  (cond
    [(null? d) (put-string o "()")]
    [(pair? d)
     (if (proper-list? d)
         (put-proper-prefix o d)
         (begin (put-string o "(scm/dot ") (put-proper-prefix o d) (put-char o #\space) (put-form o (tail-of d)) (put-char o #\))))]
    [(vector? d) (put-char o #\[) (let loop ([i 0]) (when (< i (vector-length d)) (when (> i 0) (put-char o #\space)) (put-form o (vector-ref d i)) (loop (+ i 1)))) (put-char o #\])]
    [(bytevector? d) (put-string o "(scm/bytevector") (let loop ([i 0]) (when (< i (bytevector-length d)) (put-char o #\space) (put o (bytevector-u8-ref d i)) (loop (+ i 1)))) (put-char o #\))]
    [(boolean? d) (put-string o (if d "true" "false"))]
    [(char? d) (put-char-form o d)]
    [(string? d) (put-clj-string o d)]
    [(symbol? d) (let ([s (symbol->string d)]) (if (bare-symbol? s) (put-string o s) (begin (put-string o "(scm/sym ") (put-clj-string o s) (put-char o #\)))))]
    [(number? d) (if (real? d) (put-real o d)
                     (begin (put-string o "(scm/complex ") (put-real o (real-part d)) (put-char o #\space) (put-real o (imag-part d)) (put-char o #\))))]
    [else (put-string o "(scm/other ") (put-clj-string o (call-with-string-output-port (lambda (p) (write d p)))) (put-char o #\))]))

(define (proper-list? d) (or (null? d) (and (pair? d) (proper-list? (cdr d)))))
(define (put-proper-prefix o d)
  (put-char o #\()
  (let loop ([p d] [first #t])
    (when (pair? p) (unless first (put-char o #\space)) (put-form o (car p)) (loop (cdr p) #f)))
  (put-char o #\)))
(define (tail-of d) (if (pair? d) (tail-of (cdr d)) d))

;; ---- reading

(define (slurp path)
  (call-with-port (open-file-input-port path (file-options) (buffer-mode block) (make-transcoder (utf-8-codec) (eol-style none) (error-handling-mode raise)))
    get-string-all))

(define (starts-with? s p) (and (>= (string-length s) (string-length p)) (string=? (substring s 0 (string-length p)) p)))
(define (string-index s c) (let loop ([i 0]) (cond [(= i (string-length s)) #f] [(char=? (string-ref s i) c) i] [else (loop (+ i 1))])))
(define (string-search s pat) ;; the index of the last occurrence of pat, or #f
  (let ([n (string-length s)] [m (string-length pat)])
    (let loop ([i (- n m)]) (cond [(< i 0) #f] [(string=? (substring s i (+ i m)) pat) i] [else (loop (- i 1))]))))

;; Chez's message for a condition, without "Exception in read: " and the " at char N of PORT" suffix; and N
(define (condition-text e) (call-with-string-output-port (lambda (o) (display-condition e o))))
(define (line-offset src line) ;; the character offset of the start of the 1-based LINE of SRC
  (let loop ([i 0] [l 1])
    (cond [(= l line) i] [(>= i (string-length src)) i]
          [(char=? (string-ref src i) #\newline) (loop (+ i 1) (+ l 1))]
          [else (loop (+ i 1) l)])))
;; (message . offset): Chez's message without the position, and the offset in SRC it names ("at char N of
;; PORT", or "at line L, char C of FILE" with a source file descriptor: C is the column, from 1)
(define (split-message text src)
  (let* ([k (string-index text #\:)]
         [t (if k (substring text (+ k 2) (string-length text)) text)]   ; after "Exception in read: "
         [i (string-search t " at ")])
    (if i
        (let* ([rest (substring t (+ i 4) (string-length t))]
               [lc (starts-with? rest "line ")]
               [numbers (let loop ([j 0] [acc '()] [cur #f])  ; the integers of the position, up to " of "
                          (cond [(or (= j (string-length rest)) (starts-with? (substring rest j (string-length rest)) " of ")) (reverse (if cur (cons cur acc) acc))]
                                [(char-numeric? (string-ref rest j)) (loop (+ j 1) acc (+ (* 10 (or cur 0)) (- (char->integer (string-ref rest j)) 48)))]
                                [else (loop (+ j 1) (if cur (cons cur acc) acc) #f)]))])
          (cons (substring t 0 i)
                (cond [(and lc (= (length numbers) 2)) (+ (line-offset src (car numbers)) (- (cadr numbers) 1))]
                      [(= (length numbers) 1) (car numbers)]
                      [else #f])))
        (cons t #f))))

(define (dump-file path mode out)
  (let* ([text (slurp path)]
         [shift (if (eq? mode 'r6rs) 7 0)]
         [src (if (eq? mode 'r6rs) (string-append "#!r6rs " text) text)]
         [p (open-string-input-port src)]
         [sfd (make-source-file-descriptor path (open-bytevector-input-port (bytevector 0)))]
         [data '()] [err #f])
    (let loop ([fp 0])
      (guard (e [#t (set! err (split-message (condition-text e) src))])
        (let-values ([(ann fp2) (get-datum/annotations p sfd fp)])
          (unless (eof-object? ann)
            (let* ([s (annotation-source ann)] [d (annotation-stripped ann)])
              (set! data (cons (list (- (source-object-bfp s) shift) (- (source-object-efp s) shift) d) data))
              (loop fp2))))))
    (put out "{:file ") (put-clj-string out path) (put out " :mode :" (symbol->string mode) "\n :data [")
    (let loop ([ds (reverse data)] [first #t])
      (unless (null? ds)
        (let ([x (car ds)])
          (put out (if first "" "\n  ") "{:off [" (car x) " " (cadr x) "] :form ")
          (put-form out (caddr x))
          (put out " :write ")
          (put-clj-string out (call-with-string-output-port (lambda (o) (write (caddr x) o))))
          (put out "}")
          (loop (cdr ds) #f))))
    (put out "]")
    (when err
      (put out "\n :error {:message ") (put-clj-string out (car err))
      (when (cdr err) (put out " :at " (max 0 (- (cdr err) shift))))
      (put out "}"))
    (put out "}\n")))

;; ---- scans: the predicates of the lexical syntax and of `write`, over every Unicode scalar value, as ranges
;;   --scan KIND   KIND one of whitespace, ident-initial, ident-subsequent, string-escaped, char-hex,
;;                 symbol-escaped-first, symbol-escaped-rest
;; The output is one map: {:scan KIND :ranges [[LO HI] ...]} of the code points for which the predicate holds.

(define (scalar-values-ranges pred)
  (let loop ([n 0] [start #f] [acc '()])
    (cond [(= n #x110000) (reverse (if start (cons (list start (- n 1)) acc) acc))]
          [(and (>= n #xD800) (<= n #xDFFF)) (loop (+ n 1) #f (if start (cons (list start (- n 1)) acc) acc))]
          [(pred n) (loop (+ n 1) (or start n) acc)]
          [else (loop (+ n 1) #f (if start (cons (list start (- n 1)) acc) acc))])))

(define (read-all-r6rs str)
  (guard (e [#t 'error])
    (let ([p (open-string-input-port (string-append "#!r6rs " str))])
      (let loop ([acc '()])
        (let ([d (read p)]) (if (eof-object? d) (reverse acc) (loop (cons d acc))))))))

(define (written x) (call-with-string-output-port (lambda (o) (write x o))))

(define (scan-predicate kind)
  (case kind
    [(whitespace) (lambda (n) (equal? (read-all-r6rs (string #\a (integer->char n) #\b)) '(a b)))]
    [(ident-initial) (lambda (n) (equal? (read-all-r6rs (string (integer->char n) #\a)) (list (string->symbol (string (integer->char n) #\a)))))]
    [(ident-subsequent) (lambda (n) (equal? (read-all-r6rs (string #\a (integer->char n))) (list (string->symbol (string #\a (integer->char n))))))]
    [(string-escaped) (lambda (n) (not (= (string-length (written (string (integer->char n)))) 3)))]
    [(char-hex) (lambda (n) (let ([s (written (integer->char n))]) (and (> (string-length s) 3) (char=? (string-ref s 2) #\x))))]
    [(symbol-escaped-first) (lambda (n) (not (= (string-length (written (string->symbol (string (integer->char n))))) 1)))]
    [(symbol-escaped-rest) (lambda (n) (not (= (string-length (written (string->symbol (string #\a (integer->char n))))) 2)))]
    [else (error 'scan "unknown scan" kind)]))

(define (scan kind-name)
  (let ([rs (scalar-values-ranges (scan-predicate (string->symbol kind-name)))] [o (current-output-port)])
    (put o "{:scan :" kind-name " :ranges [")
    (let loop ([rs rs] [first #t])
      (unless (null? rs)
        (put o (if first "" " ") "[" (car (car rs)) " " (cadr (car rs)) "]")
        (loop (cdr rs) #f)))
    (put o "]}\n")))

(define (main args)
  (let loop ([as args] [mode 'r6rs] [files '()])
    (cond [(null? as)
           (let ([o (current-output-port)])
             (for-each (lambda (f) (dump-file f mode o)) (reverse files)))]
          [(string=? (car as) "--mode") (loop (cddr as) (string->symbol (cadr as)) files)]
          [(string=? (car as) "--scan") (scan (cadr as))]
          [else (loop (cdr as) mode (cons (car as) files))])))

(main (cdr (command-line)))
