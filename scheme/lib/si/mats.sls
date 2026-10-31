#!chezscheme
;;;; (si mats): the harness library of bin/si mats (SM8; SM8b): runs one Chez mat (doc/scheme/SCHEME-SURVEY.md, "As built:
;;;; SM8", "As built: SM8b") and the helpers the mats take from Chez's mat.ss, written for Arbace from their use in the
;;;; mats (not mat.ss's code): == (eqv?), fl~= and ~= (equal within a relative 1e-14, as the mats compare flonums), nan,
;;;; pi and the +pi ... constants. Chez's own procedures the mats use are (chezscheme)'s (scheme/lib/chezscheme.sls,
;;;; SC110 (b)); SM8's few conveniences here moved there. The watchdog (si's, SC111 (b)) bounds each mat.
;;;;
;;;; (run-mat NAME CLAUSES): CLAUSES is a list of (KIND . THUNK), KIND 'true (the clause must return #t), 'error
;;;; (it must raise: its thunk is the clause's datum, evaluated by the evaluator the harness set, as Chez evaluates
;;;; an error? clause on its own, so a macro that fails to expand raises there) or 'warning (it may return). Returns (STATUS COUNT DETAIL): STATUS pass or fail, COUNT the clauses
;;;; run, DETAIL the first failing clause "N: why".
(library (si mats)
  (export run-mat set-evaluator! set-clause-ms! watchdog == fl~= ~= nan pi +pi +pi/2 +pi/4 -pi -pi/2 -pi/4 +e -e)
  (import (rnrs) (rnrs mutable-pairs) (only (psyntax system $all) watchdog r7rs-write))

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
                     (else #f))))
              (if why
                  (list 'fail n (string-append (number->string n) ": " why))
                  (loop (cdr cs) (+ n 1))))))))))
