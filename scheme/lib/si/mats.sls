#!chezscheme
;;;; (si mats): the harness library of bin/si mats (SM8; SM8b): runs one Chez mat (doc/scheme/SCHEME-SURVEY.md, "As built:
;;;; SM8", "As built: SM8b") and the helpers the mats take from Chez's mat.ss, written for Arbace from their use in the
;;;; mats (not mat.ss's code): == (eqv?), fl~= and ~= (equal within a relative 1e-14, as the mats compare flonums), nan,
;;;; pi and the +pi ... constants. Chez's own procedures the mats use are (chezscheme)'s (scheme/lib/chezscheme.sls,
;;;; SC110 (b)); SM8's few conveniences here moved there. The watchdog (si's, SC111 (b)) bounds each mat.
;;;;
;;;; (run-mat NAME CLAUSES): CLAUSES is a list of (KIND . THUNK), KIND 'true (the clause must return #t), 'error
;;;; (it must raise: its thunk is the clause's datum, evaluated by the evaluator the harness set, as Chez evaluates
;;;; an error? clause on its own, so a macro that fails to expand raises there) or 'warning (it must raise a &warning
;;;; condition, as Chez's mat.ss asks; review 009, finding 9: it passed whatever it did). Returns (STATUS COUNT DETAIL): STATUS pass or fail, COUNT the clauses
;;;; run, DETAIL the first failing clause "N: why".
(library (si mats)
  (export run-mat set-evaluator! set-clause-ms! watchdog == fl~= ~= nan pi +pi +pi/2 +pi/4 -pi -pi/2 -pi/4 +e -e
          *fuzz* cfl~= equivalent-expansion? windows? embedded? pb? mkfile $record->vector patch-exec-path $cat_flush find-source)
  (import (except (rnrs) with-output-to-file) (rnrs mutable-pairs) (only (psyntax system $all) watchdog r7rs-write)
          (only (chezscheme) cfl-real-part cfl-imag-part gensym? box? unbox pretty-print with-output-to-file machine-type
                csv7:record-type-field-names csv7:record-field-accessor source-directories))

  (define == eqv?)
  (define fl~=
    (case-lambda
      ((x y) (fl~= x y 1e-14))
      ((x y fuzz)
       (cond ((and (fl>=? (flabs x) 2.0) (fl>=? (flabs y) 2.0)) (fl~= (fl/ x 2.0) (fl/ y 2.0) fuzz))
             ((and (fl<? 0.0 (flabs x) 1.0) (fl<? 0.0 (flabs y) 1.0)) (fl~= (fl* x 2.0) (fl* y 2.0) fuzz))
             (else (fl<=? (flabs (fl- x y)) fuzz))))))
  (define (~= x y)
    (or (= x y)
        (and (fl~= (inexact (real-part x)) (inexact (real-part y)))
             (fl~= (inexact (imag-part x)) (inexact (imag-part y))))))
  ;; SMR7: the rest of mat.ss's helpers the mats call, written for Arbace from their use: *fuzz* (fl~='s default tolerance), cfl~= (both
  ;; parts fl~=), equivalent-expansion? (two expansions equal up to a consistent renaming of gensyms, a procedure in either one the
  ;; predicate for the other's datum; on a mismatch it prints both, as mat.ss does), the machine predicates (si is a6le: none), mkfile
  ;; (each expression pretty-printed into a file, replacing it), $record->vector (a record as #(NAME FIELD ...)), $cat_flush
  (define *fuzz* 1e-14)
  (define (cfl~= x y)
    (and (fl~= (cfl-real-part x) (cfl-real-part y))
         (fl~= (cfl-imag-part x) (cfl-imag-part y))))
  (define (equivalent-expansion? x y)
    (let ((alist '()))
      (define (e? x y)
        (cond ((procedure? x) (x y))
              ((procedure? y) (y x))
              ((eqv? x y) #t)
              ((pair? x) (and (pair? y) (e? (car x) (car y)) (e? (cdr x) (cdr y))))
              ((or (and (gensym? x) (symbol? y)) (and (gensym? y) (symbol? x)))
               (let ((a (assq x alist)))
                 (if a (eq? y (cdr a)) (begin (set! alist (cons (cons x y) alist)) #t))))
              ((string? x) (and (string? y) (string=? x y)))
              ((bytevector? x) (and (bytevector? y) (bytevector=? x y)))
              ((vector? x) (and (vector? y) (= (vector-length x) (vector-length y))
                                (let loop ((i 0)) (or (= i (vector-length x)) (and (e? (vector-ref x i) (vector-ref y i)) (loop (+ i 1)))))))
              ((box? x) (and (box? y) (e? (unbox x) (unbox y))))
              (else #f)))
      (or (e? x y)
          (begin (display "failure in equivalent-expansion?:\n") (pretty-print x) (display "is not equivalent to\n") (pretty-print y) #f))))
  (define (windows?) #f)
  (define (embedded?) #f)
  (define (pb?) #f)
  (define (patch-exec-path p) p)
  (define $cat_flush "./cat_flush")
  (define (mkfile filename . exprs)
    (with-output-to-file filename (lambda () (for-each pretty-print exprs)) 'replace))
  (define ($record->vector x)
    (let* ((rtd (record-rtd x)) (names (csv7:record-type-field-names rtd)))
      (list->vector (cons (record-type-name rtd)
                          (let loop ((i 0) (n names))
                            (if (null? n) '() (cons ((csv7:record-field-accessor rtd i) x) (loop (+ i 1) (cdr n)))))))))
  ;; the file a mat names, found in the source directories (the work directory has every corpus file)
  (define (find-source fn)
    (or (exists (lambda (d) (let ((f (if (string=? d ".") fn (string-append d "/" fn)))) (and (file-exists? f) f))) (source-directories))
        (error 'find-source "file not found" fn)))
  (define (nan) (/ 0.0 0.0))
  (define pi (* (asin 1.0) 2))
  (define +pi 3.141592653589793)
  (define +pi/2 1.5707963267948966)
  (define +pi/4 .7853981633974483)
  (define -pi (- +pi))
  (define -pi/2 (- +pi/2))
  (define -pi/4 (- +pi/4))
  (define +e 5e-324)
  (define -e (- +e))

  (define (condition-text e)
    (cond ((and (condition? e) (message-condition? e))
           (string-append (condition-message e)
                          (if (and (irritants-condition? e) (pair? (condition-irritants e)))
                              (string-append " " (call-with-string (condition-irritants e)))
                              "")))
          ((condition? e) "a condition")
          (else (string-append "raised " (call-with-string e)))))
  (define (call-with-string x)
    (call-with-values open-string-output-port
      (lambda (port extract) (r7rs-write x port) (let ((s (extract))) (if (> (string-length s) 200) (substring s 0 200) s)))))

  (define evaluator #f)
  ;; each clause runs under the watchdog: a clause that must raise and loops instead fails, and the next clause
  ;; gets the watchdog again
  (define clause-ms 20000)
  (define (set-clause-ms! ms) (set! clause-ms ms))
  (define (set-evaluator! p) (set! evaluator p))
  (define (watchdog-error? e)
    (and (condition? e) (who-condition? e) (eq? (condition-who e) 'watchdog)))
  (define (unbound-id? e)
    (and (condition? e) (message-condition? e) (member (condition-message e) '("unbound identifier" "variable ~s is not bound"))))

  (define (run-mat name clauses)
    (let loop ((cs clauses) (n 1))
      (if (null? cs)
          (list 'pass (- n 1) "")
          (let* ((kind (caar cs)) (thunk (cdar cs))
                 (r (begin
                      (watchdog clause-ms)
                      (let ((r (guard (e (#t (cons 'raised e)))
                                 (let ((v (if (eq? kind 'error) (evaluator thunk) (thunk)))) (cons 'value v)))))
                        (watchdog 0)
                        r))))
            (if (and (eq? (car r) 'raised) (or (and (not (eq? kind 'error)) (syntax-violation? (cdr r))) (unbound-id? (cdr r))))
              ;; the clause does not expand (SC121 (c)): it is not run
              (list 'notrun n (cdr r))
            (let ((why
                   (case kind
                     ((true) (cond ((eq? (car r) 'raised) (string-append "error: " (condition-text (cdr r))))
                                   ((eq? (cdr r) #t) #f)
                                   ((not (cdr r)) "returned #f")
                                   (else "returned a non-boolean")))
                     ((error) (cond ((and (eq? (car r) 'raised) (watchdog-error? (cdr r))) "looped (the watchdog)")
                                    ((and (eq? (car r) 'raised) (unbound-id? (cdr r)))
                                     (string-append "unbound identifier " (call-with-string (condition-who (cdr r)))))
                                    ((eq? (car r) 'raised) #f)
                                    (else "did not raise")))
                     ((warning) (cond ((and (eq? (car r) 'raised) (warning? (cdr r))) #f)
                                      ((eq? (car r) 'raised) (string-append "error: " (condition-text (cdr r))))
                                      (else "did not warn")))
                     (else #f))))
              (if why
                  (list 'fail n (string-append (number->string n) ": " why))
                  (loop (cdr cs) (+ n 1))))))))))
