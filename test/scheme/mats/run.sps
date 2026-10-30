;;;; bin/si mats' harness (SM8, SM8b): runs the mats of one Chez .ms file (argv: FILE [SKIPFILE]) on si, in R7RS reading mode.
;;;; Per top-level datum of the file: a (mat NAME CLAUSE ...) is run as one body, in an environment of (chezscheme)
;;;; (si's subset of Chez's library, SC110 (b)) and (si mats), after the file's definitions above it (define,
;;;; define-syntax, define-record-type, define-values, each checked alone first), under si's watchdog (SC111 (b): a
;;;; mat that runs past SI_MAT_MS milliseconds, default 20000, fails); a clause (error? E) must raise, any other clause
;;;; must return #t. A mat that does not expand has its clauses run one by one (SC110 (c)), each with the prelude, so
;;;; Chez-specific clauses do not hide the portable ones. Output, one line a result, flushed:
;;;;   BEGIN NAME                 before a mat
;;;;   MAT NAME PASS N            all N clauses ran as they should
;;;;   MAT NAME FAIL N DETAIL     the clause DETAIL ("K: why") failed
;;;;   MAT NAME NOTRUN WHY        the mat did not expand or run: WHY is the expander's message
;;;;   CLAUSES NAME P F N         after a NOTRUN: its clauses run alone, P passed, F failed, N did not run
;;;;   MAT NAME SKIP WHY          a form the harness does not run (parameters, mat/cf, a name in SKIPFILE)
;;;;   READ POSITION WHY          a top-level datum the reader refuses (its text is skipped)
;;;;   DEF NAME NOTRUN WHY        a definition that does not expand alone (left out)
;;;;   FILE N-datums M-mats       at the end
(import (rnrs) (rnrs eval) (rnrs mutable-pairs) (si mats) (only (srfi :98) get-environment-variable)
        (only (psyntax system $all) r7rs-write))

(define argv (cdr (command-line)))
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

(define env (environment '(chezscheme) '(si mats)))
(define mat-ms
  (let ((v (get-environment-variable "SI_MAT_MS"))) (or (and v (string->number v)) 20000)))
(set-clause-ms! mat-ms)

(define (message-of e)
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

(define (chez-clause c)
  (if (and (pair? c) (memq (car c) '(error? sanitized-error?)) (pair? (cdr c)) (null? (cddr c)))
      `(cons 'error ',(cadr c))
      (if (and (pair? c) (eq? (car c) 'warning?) (pair? (cdr c)) (null? (cddr c)))
          `(cons 'warning (lambda () ,(cadr c)))
          `(cons 'true (lambda () ,c)))))

(define (run-one name clauses)
  (set! mats (+ mats 1))
  (display "BEGIN ") (write name) (newline) (line)
  (cond
    ((member name skip) (display "MAT ") (write name) (display " SKIP in the skip list") (newline))
    ((and (pair? clauses) (pair? (car clauses)) (eq? (caar clauses) 'parameters))
     (display "MAT ") (write name) (display " SKIP parameters") (newline))
    (else
     (set-evaluator! (lambda (expr) (eval `(let () ,@(reverse prelude) ,expr) env)))
     (let ((form `(let () ,@(reverse prelude) (run-mat ',name (list ,@(map chez-clause clauses))))))
       (let ((r (timed (lambda () (guard (e (#t (cons 'notrun e))) (cons 'ok (eval form env)))))))
         (display "MAT ") (write name)
         (if (eq? (car r) 'notrun)
             (begin (display " NOTRUN ") (display (message-of (cdr r))))
             (let ((res (cdr r)))
               (if (eq? (car res) 'pass)
                   (begin (display " PASS ") (display (cadr res)))
                   (begin (display " FAIL ") (display (cadr res)) (display " ") (display (caddr res))))))
         (newline)
         (when (eq? (car r) 'notrun) (line) (run-clauses name clauses))))))
  (line))

;; the watchdog around a thunk
(define (timed thunk)
  (watchdog mat-ms)
  (let ((v (guard (e (#t (watchdog 0) (raise e))) (thunk))))
    (watchdog 0)
    v))

;; a mat that does not expand: each clause alone (SC110 (c))
(define (run-clauses name clauses)
  (let loop ((cs clauses) (p 0) (f 0) (n 0))
    (if (null? cs)
        (begin (display "CLAUSES ") (write name) (display " ") (display p) (display " ") (display f) (display " ")
               (display n) (newline))
        (let* ((form `(let () ,@(reverse prelude) (run-mat ',name (list ,(chez-clause (car cs))))))
               (r (timed (lambda () (guard (e (#t (cons 'notrun e))) (cons 'ok (eval form env)))))))
          (cond ((eq? (car r) 'notrun) (loop (cdr cs) p f (+ n 1)))
                ((eq? (cadr r) 'pass) (loop (cdr cs) (+ p 1) f n))
                (else (loop (cdr cs) p (+ f 1) n)))))))

(define (add-def! d)
  (let ((r (timed (lambda ()
             (guard (e (#t (cons 'notrun e)))
               (eval `(let () ,@(reverse prelude) ,d (if #f #f)) env)
               (cons 'ok #f))))))
    (if (eq? (car r) 'notrun)
        (begin (display "DEF ") (write (if (pair? (cdr d)) (let ((n (cadr d))) (if (pair? n) (car n) n)) d))
               (display " NOTRUN ") (display (message-of (cdr r))) (newline) (line))
        (set! prelude (cons d prelude)))))

(for-each
 (lambda (seg)
   (set! data (+ data 1))
   (let* ((str (substring text (car seg) (cdr seg)))
          (r (guard (e (#t (cons 'bad e))) (cons 'good (read (open-string-input-port str))))))
     (cond
       ((eq? (car r) 'bad)
        (display "READ ") (display (car seg)) (display " ") (display (message-of (cdr r))) (newline) (line))
       (else
        (let ((d (cdr r)))
          (cond
            ((and (pair? d) (eq? (car d) 'mat) (pair? (cdr d)))
             (run-one (cadr d) (cddr d)))
            ((and (pair? d) (memq (car d) '(mat/cf)))
             (set! mats (+ mats 1))
             (display "MAT ") (write (if (pair? (cdr d)) (cdr d) d)) (display " SKIP mat/cf") (newline) (line))
            ((def-form? d) (add-def! d))
            (else #f)))))))
 (segments text))

(display "FILE ") (display data) (display " ") (display mats) (newline)
