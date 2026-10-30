#!r6rs
;;;; (si mats): the harness library of bin/si mats (SM8): runs one Chez mat (doc/scheme/SCHEME-SURVEY.md, "As built:
;;;; SM8") and a few of Chez's conveniences that the mats use and R6RS lacks (void, iota, list-head, last-pair,
;;;; sort, 1+, 1-, fx1+, fx1-, format via si's). Written for Arbace; not Chez's mat.ss (which needs Chez's
;;;; reset-handler, pretty-print and source annotations).
;;;;
;;;; (run-mat NAME CLAUSES): CLAUSES is a list of (KIND . THUNK), KIND 'true (the clause must return #t), 'error
;;;; (it must raise: its thunk is the clause's datum, evaluated by the evaluator the harness set, as Chez evaluates
;;;; an error? clause on its own, so a macro that fails to expand raises there) or 'warning (it may return). Returns (STATUS COUNT DETAIL): STATUS pass or fail, COUNT the clauses
;;;; run, DETAIL the first failing clause "N: why".
(library (si mats)
  (export run-mat set-evaluator! void iota list-head last-pair sort 1+ 1- fx1+ fx1-
          format printf pretty-print andmap ormap most-positive-fixnum most-negative-fixnum)
  (import (rnrs) (rnrs mutable-pairs) (only (psyntax system $all) format))

  (define (printf fmt . args) (display (apply format fmt args)))
  (define (pretty-print x . port) (apply write x port) (apply newline port))
  (define (andmap f l . ls)
    (let loop ((ls (cons l ls)) (last #t))
      (if (exists null? ls) last
          (let ((v (apply f (map car ls)))) (and v (loop (map cdr ls) v))))))
  (define (ormap f l . ls)
    (let loop ((ls (cons l ls)))
      (if (exists null? ls) #f (or (apply f (map car ls)) (loop (map cdr ls))))))
  (define (most-positive-fixnum) (greatest-fixnum))
  (define (most-negative-fixnum) (least-fixnum))

  (define (void . x) (if #f #f))
  (define (iota n . start)
    (let loop ((i (- n 1)) (acc '()))
      (if (< i 0) acc (loop (- i 1) (cons (+ i (if (pair? start) (car start) 0)) acc)))))
  (define (list-head l n) (if (= n 0) '() (cons (car l) (list-head (cdr l) (- n 1)))))
  (define (last-pair l) (if (pair? (cdr l)) (last-pair (cdr l)) l))
  (define (sort pred seq)
    (if (vector? seq) (vector-sort pred seq) (list-sort pred seq)))
  (define (1+ x) (+ x 1))
  (define (1- x) (- x 1))
  (define (fx1+ x) (+ x 1))
  (define (fx1- x) (- x 1))

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
      (lambda (port extract) (write x port) (let ((s (extract))) (if (> (string-length s) 200) (substring s 0 200) s)))))

  (define evaluator #f)
  (define (set-evaluator! p) (set! evaluator p))
  (define (unbound-id? e)
    (and (condition? e) (message-condition? e) (string=? (condition-message e) "unbound identifier")))

  (define (run-mat name clauses)
    (let loop ((cs clauses) (n 1))
      (if (null? cs)
          (list 'pass (- n 1) "")
          (let* ((kind (caar cs)) (thunk (cdar cs))
                 (r (guard (e (#t (cons 'raised e)))
                      (let ((v (if (eq? kind 'error) (evaluator thunk) (thunk)))) (cons 'value v)))))
            (let ((why
                   (case kind
                     ((true) (cond ((eq? (car r) 'raised) (string-append "error: " (condition-text (cdr r))))
                                   ((eq? (cdr r) #t) #f)
                                   ((not (cdr r)) "returned #f")
                                   (else "returned a non-boolean")))
                     ((error) (cond ((and (eq? (car r) 'raised) (unbound-id? (cdr r)))
                                     (string-append "unbound identifier " (call-with-string (condition-who (cdr r)))))
                                    ((eq? (car r) 'raised) #f)
                                    (else "did not raise")))
                     (else #f))))
              (if why
                  (list 'fail n (string-append (number->string n) ": " why))
                  (loop (cdr cs) (+ n 1)))))))))
