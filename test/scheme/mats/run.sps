;;;; bin/si mats' harness (SM8, SM8b): runs the mats of one Chez .ms file (argv: FILE [SKIPFILE]) on si, in R7RS reading mode.
;;;; Per top-level datum of the file: a (mat NAME CLAUSE ...) is run, its clauses one after the other as top-level forms of a
;;;; new interaction environment (SC121 (c), as Chez does: a definition in one clause is seen by the next) of (chezscheme)
;;;; (si's subset of Chez's library, SC110 (b)) and (si mats), after the file's definitions above it (define,
;;;; define-syntax, define-record-type, define-values, each checked alone first), under si's watchdog (SC111 (b): a
;;;; mat that runs past SI_MAT_MS milliseconds, default 20000, fails); a clause (error? E) must raise, any other clause
;;;; must return #t. A clause that does not expand (an identifier of Chez's own) is not run: the mat is NOTRUN and the
;;;; other clauses' results are counted (CLAUSES), so Chez-specific clauses do not hide the portable ones. Output, one line a result, flushed:
;;;;   BEGIN NAME                 before a mat
;;;;   MAT NAME PASS N            all N clauses ran as they should
;;;;   MAT NAME FAIL N DETAIL     the clause DETAIL ("K: why") failed
;;;;   MAT NAME NOTRUN WHY        the mat did not expand or run: WHY is the expander's message
;;;;   CLAUSE NAME K: WHY         before the MAT line, one a failing clause K of a FAIL or a NOTRUN mat, every one (review 009,
;;;;                              finding 2: the MAT line names only the first, and a NOTRUN mat's failures were only counted)
;;;;   CLAUSES NAME P F N         after a NOTRUN: its clauses' results, P passed, F failed, N did not run
;;;;   MAT NAME SKIP WHY          a form the harness does not run (parameters, mat/cf, a name in SKIPFILE)
;;;;   READ POSITION WHY          a top-level datum the reader refuses (its text is skipped)
;;;;   DEF NAME NOTRUN WHY        a definition that does not expand alone (left out)
;;;;   FILE N-datums M-mats       at the end
(import (rnrs) (rnrs eval) (rnrs mutable-pairs) (si mats) (only (srfi :98) get-environment-variable)
        (only (chezscheme) interaction-environment)
        (only (psyntax system $all) r7rs-write new-interaction-environment library-path))

(define argv (cdr (command-line)))
;; SC170 (b): Chez finds a library in the current directory first (its library-directories default is "."): a mat that writes a
;; library file (testfile-g2.ss) and imports it
(library-path (append (library-path) '(".")))
(define path (car argv))
(define skip
  (if (pair? (cdr argv))
      (guard (e (#t '()))
        (call-with-input-file (cadr argv)
          (lambda (p) (let loop ((x (read p)) (acc '())) (if (eof-object? x) acc (loop (read p) (cons x acc)))))))
      '()))

(define text
  (let* ((p (open-file-input-port path)) (bv (get-bytevector-all p)))
    (close-port p)
    (utf8->string bv)))

;; the top-level segments of the text: (start . end) of each datum, by paren depth
(define (segments s)
  (let ((n (string-length s)))
    (let loop ((i 0) (acc '()))
      (let ((i (skip-ws s i n)))
        (if (>= i n)
            (reverse acc)
            (let ((e (datum-end s i n)))
              (loop e (cons (cons i e) acc))))))))
(define (skip-ws s i n)
  (cond ((>= i n) i)
        ((char-whitespace? (string-ref s i)) (skip-ws s (+ i 1) n))
        ((char=? (string-ref s i) #\;)
         (let loop ((j i)) (if (or (>= j n) (char=? (string-ref s j) #\newline)) (skip-ws s j n) (loop (+ j 1)))))
        ((and (< (+ i 1) n) (char=? (string-ref s i) #\#) (char=? (string-ref s (+ i 1)) #\|))
         (let loop ((j (+ i 2)) (d 1))
           (cond ((>= j n) j)
                 ((and (< (+ j 1) n) (char=? (string-ref s j) #\|) (char=? (string-ref s (+ j 1)) #\#))
                  (if (= d 1) (skip-ws s (+ j 2) n) (loop (+ j 2) (- d 1))))
                 ((and (< (+ j 1) n) (char=? (string-ref s j) #\#) (char=? (string-ref s (+ j 1)) #\|)) (loop (+ j 2) (+ d 1)))
                 (else (loop (+ j 1) d)))))
        (else i)))
(define (delimiter? c) (or (char-whitespace? c) (memv c '(#\( #\) #\[ #\] #\" #\;))))
(define (datum-end s i n)
  (let ((c (string-ref s i)))
    (cond ((memv c '(#\( #\[))
           (let loop ((j i) (d 0))
             (if (>= j n)
                 n
                 (let ((c (string-ref s j)))
                   (cond ((memv c '(#\( #\[)) (loop (+ j 1) (+ d 1)))
                         ((memv c '(#\) #\])) (if (= d 1) (+ j 1) (loop (+ j 1) (- d 1))))
                         ((char=? c #\") (loop (string-end s (+ j 1) n) d))
                         ((char=? c #\;) (loop (skip-ws s j n) d))
                         ((and (char=? c #\#) (< (+ j 1) n) (char=? (string-ref s (+ j 1)) #\|)) (loop (skip-ws s j n) d))
                         ((and (char=? c #\#) (< (+ j 1) n) (char=? (string-ref s (+ j 1)) #\\))
                          ;; a character: #\x then to the next delimiter, at least one character
                          (let cl ((k (+ j 3))) (if (and (< k n) (not (delimiter? (string-ref s k)))) (cl (+ k 1)) (loop k d))))
                         ((char=? c #\|) (loop (bar-end s (+ j 1) n) d))
                         (else (loop (+ j 1) d)))))))
          ((char=? c #\") (string-end s (+ i 1) n))
          (else
           ;; an atom, or a prefix (', `, ,, #, #vu8, #u8, #2%) of a list that follows at once
           (let loop ((j (+ i 1)))
             (cond ((and (< j n) (not (delimiter? (string-ref s j)))) (loop (+ j 1)))
                   ((and (< j n) (memv (string-ref s j) '(#\( #\[ #\")) (or (memv c '(#\' #\` #\,)) (char=? c #\#)))
                    (datum-end s j n))
                   ((and (= j (+ i 1)) (< j n) (memv c '(#\' #\` #\,)) (not (char-whitespace? (string-ref s j))))
                    (datum-end s j n))
                   (else j)))))))
(define (string-end s j n)
  (cond ((>= j n) n)
        ((char=? (string-ref s j) #\\) (string-end s (+ j 2) n))
        ((char=? (string-ref s j) #\") (+ j 1))
        (else (string-end s (+ j 1) n))))
(define (bar-end s j n)
  (cond ((>= j n) n)
        ((char=? (string-ref s j) #\\) (bar-end s (+ j 2) n))
        ((char=? (string-ref s j) #\|) (+ j 1))
        (else (bar-end s (+ j 1) n))))

;; SC180 (b): R6RS's own names with the prefix r6rs: (Chez's tests of its extended procedures, io.ms's r6rs:flush-output-port, name R6RS's)
(define env (environment '(chezscheme) '(si mats) '(prefix (rnrs) r6rs:)))
(define mat-ms
  (let ((v (get-environment-variable "SI_MAT_MS"))) (or (and v (string->number v)) 20000)))
(set-clause-ms! mat-ms)

;; SC140 (b): SI_MAT_VERBOSE set to anything but "" and "0" prints each failing clause, with R7RS's write (labels a cycle)
(define verbose (let ((v (get-environment-variable "SI_MAT_VERBOSE"))) (and v (not (string=? v "")) (not (string=? v "0")))))
(define (message-of e)
  (if (and (condition? e) (message-condition? e) (string=? (condition-message e) "variable ~s is not bound")
           (irritants-condition? e) (pair? (condition-irritants e)) (symbol? (car (condition-irritants e))))
      ;; an interaction environment resolves a free identifier when the clause runs (SC121 (c)): said as the expander says it
      (string-append "unbound identifier " (symbol->string (car (condition-irritants e))))
      (message-of* e)))
(define (message-of* e)
  (let ((s (cond ((and (condition? e) (message-condition? e))
                  (string-append (condition-message e)
                                 (if (and (irritants-condition? e) (pair? (condition-irritants e)))
                                     (string-append " " (show (condition-irritants e)))
                                     "")
                                 (if (and (who-condition? e) (condition-who e)) (string-append " " (show (condition-who e))) "")
                                 (if (syntax-violation? e)
                                     (string-append " " (show (or (syntax-violation-subform e) (syntax-violation-form e))))
                                     "")))
                 ((condition? e) "a condition")
                 (else (string-append "raised " (show e))))))
    (list->string (map (lambda (c) (if (char=? c #\newline) #\space c)) (string->list s)))))
(define (show x)
  (call-with-values open-string-output-port
    ;; R7RS's write: a cyclic irritant is written with labels, not without end
    (lambda (port extract) (r7rs-write x port) (let ((s (extract))) (if (> (string-length s) 160) (substring s 0 160) s)))))

(define (def-form? d)
  (and (pair? d) (memq (car d) '(define define-syntax define-record-type define-values define-structure))))

(define (line) (flush-output-port (current-output-port)))
(define prelude '())   ; the definitions that expand alone, in order, reversed
(define data 0)
(define mats 0)

;; SC121 (c): Chez evaluates a mat's clauses one after the other at the top level, so a definition in one clause is seen by the
;; next; the clauses are evaluated in a new interaction environment with the mat's prelude, each as a top-level form
(define (chez-clause c env2)
  (if (and (pair? c) (memq (car c) '(error? sanitized-error?)) (pair? (cdr c)) (null? (cddr c)))
      (cons 'error (cadr c))
      (if (and (pair? c) (eq? (car c) 'warning?) (pair? (cdr c)) (null? (cddr c)))
          (cons 'warning (lambda () (eval (cadr c) env2)))
          (cons 'true (lambda () (eval c env2))))))

(define (make-mat-env)
  (let ((env2 (new-interaction-environment env)))
    (for-each (lambda (d) (guard (e (#t #f)) (eval d env2))) (reverse prelude))
    env2))

(define (run-one name clauses)
  (set! mats (+ mats 1))
  (newline) (display "BEGIN ") (write name) (newline) (line)
  (cond
    ((member name skip) (newline) (display "MAT ") (write name) (display " SKIP in the skip list") (newline))
    ((and (pair? clauses) (pair? (car clauses)) (eq? (caar clauses) 'parameters))
     (newline) (display "MAT ") (write name) (display " SKIP parameters") (newline))
    (else
     (let ((env2 (timed (lambda () (make-mat-env)))))
       (set-evaluator! (lambda (expr) (eval expr env2)))
       (interaction-environment env2)   ; SC170 (b): (eval x) of a clause evaluates in the mat's environment, as in Chez
       (let loop ((cs clauses) (k 1) (p 0) (f 0) (n 0) (fail #f) (notrun #f))
         (if (null? cs)
             (begin
               (newline) (display "MAT ") (write name)
               (cond ((> n 0) (display " NOTRUN ") (display (message-of notrun)))
                     (fail (display " FAIL ") (display (car fail)) (display " ") (display (cdr fail)))
                     (else (display " PASS ") (display p)))
               (newline)
               (when (> n 0)
                 (line)
                 (newline) (display "CLAUSES ") (write name) (display " ") (display p) (display " ") (display f) (display " ")
                 (display n) (newline)))
             (let ((r (guard (e (#t (list 'notrun 1 e)))
                        (run-mat name (list (chez-clause (car cs) env2))))))
               (case (car r)
                 ((pass) (loop (cdr cs) (+ k 1) (+ p 1) f n fail notrun))
                 ((notrun) (loop (cdr cs) (+ k 1) p f (+ n 1) fail (or notrun (caddr r))))
                 (else
                  (when verbose (display "CLAUSEFAIL ") (write name) (display " ") (display k) (display " ") (r7rs-write (car cs) (current-output-port)) (newline) (line))
                  (let* ((d (caddr r))   ; "1: why" -> "K: why"
                         (kd (string-append (number->string k) (substring d 1 (string-length d)))))
                    (newline) (display "CLAUSE ") (write name) (display " ") (display kd) (newline) (line)
                    (loop (cdr cs) (+ k 1) p (+ f 1) n (or fail (cons k kd)) notrun))))))))))
  (line))

;; the watchdog around a thunk
(define (timed thunk)
  (watchdog mat-ms)
  (let ((v (guard (e (#t (watchdog 0) (raise e))) (thunk))))
    (watchdog 0)
    v))

(define (add-def! d)
  (let ((r (timed (lambda ()
             (guard (e (#t (cons 'notrun e)))
               (eval `(let () ,@(reverse prelude) ,d (if #f #f)) env)
               (cons 'ok #f))))))
    (if (eq? (car r) 'notrun)
        (begin (newline) (display "DEF ") (write (if (pair? (cdr d)) (let ((n (cadr d))) (if (pair? n) (car n) n)) d))
               (display " NOTRUN ") (display (message-of (cdr r))) (newline) (line))
        (set! prelude (cons d prelude)))))

(for-each
 (lambda (seg)
   (set! data (+ data 1))
   (let* ((str (substring text (car seg) (cdr seg)))
          (r (guard (e (#t (cons 'bad e))) (cons 'good (read (open-string-input-port str))))))
     (cond
       ((eq? (car r) 'bad)
        (newline) (display "READ ") (display (car seg)) (display " ") (display (message-of (cdr r))) (newline) (line))
       (else
        (let ((d (cdr r)))
          (cond
            ((and (pair? d) (eq? (car d) 'mat) (pair? (cdr d)))
             (run-one (cadr d) (cddr d)))
            ((and (pair? d) (memq (car d) '(mat/cf)))
             (set! mats (+ mats 1))
             (newline) (display "MAT ") (write (if (pair? (cdr d)) (cdr d) d)) (display " SKIP mat/cf") (newline) (line))
            ((def-form? d) (add-def! d))
            (else #f)))))))
 (segments text))

(newline) (display "FILE ") (display data) (display " ") (display mats) (newline)
