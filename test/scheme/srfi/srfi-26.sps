#!r6rs
;;; SRFI 26 (srfi :26 cut): the confidence tests of the reference implementation (check.scm of
;;; https://github.com/scheme-requests-for-implementation/srfi-26 at 2102d4d0), each `(check expr)` turned into a
;;; test, then the examples of the SRFI document and a few cases of our own (hygiene of the slots). Prints FAIL
;;; lines and a last line "srfi-26: P passed, F failed".
(import (rnrs) (srfi :26))

(define passed 0)
(define failed 0)
(define-syntax test
  (syntax-rules ()
    ((_ expr expected)
     (let ((got (guard (e (#t (list 'exception))) expr)))
       (if (equal? got expected)
           (set! passed (+ passed 1))
           (begin (set! failed (+ failed 1))
                  (display "FAIL: ") (write 'expr) (display " => ") (write got)
                  (display ", expected ") (write expected) (newline)))))))

;; check.scm: cut
(test ((cut list)) '())
(test ((cut list <...>)) '())
(test ((cut list 1)) '(1))
(test ((cut list <>) 1) '(1))
(test ((cut list <...>) 1) '(1))
(test ((cut list 1 2)) '(1 2))
(test ((cut list 1 <>) 2) '(1 2))
(test ((cut list 1 <...>) 2) '(1 2))
(test ((cut list 1 <...>) 2 3 4) '(1 2 3 4))
(test ((cut list 1 <> 3 <>) 2 4) '(1 2 3 4))
(test ((cut list 1 <> 3 <...>) 2 4 5 6) '(1 2 3 4 5 6))
(test (let* ((x 'wrong) (y (cut list x))) (set! x 'ok) (y)) '(ok))
(test (let ((a 0)) (map (cut + (begin (set! a (+ a 1)) a) <>) '(1 2)) a) 2)
;; check.scm: cute
(test ((cute list)) '())
(test ((cute list <...>)) '())
(test ((cute list 1)) '(1))
(test ((cute list <>) 1) '(1))
(test ((cute list <...>) 1) '(1))
(test ((cute list 1 2)) '(1 2))
(test ((cute list 1 <>) 2) '(1 2))
(test ((cute list 1 <...>) 2) '(1 2))
(test ((cute list 1 <...>) 2 3 4) '(1 2 3 4))
(test ((cute list 1 <> 3 <>) 2 4) '(1 2 3 4))
(test ((cute list 1 <> 3 <...>) 2 4 5 6) '(1 2 3 4 5 6))
(test (let ((a 0)) (map (cute + (begin (set! a (+ a 1)) a) <>) '(1 2)) a) 1)
;; the SRFI document's examples
(test (map (cut * 2 <>) '(1 2 3 4)) '(2 4 6 8))
(test (let ((x (make-vector 3 1))) (map (cut vector-set! x <> 0) '(0 2)) x) '#(0 1 0))
(test (let ((out '())) (for-each (cut (lambda (v) (set! out (cons v out))) <>) '(1 2)) out) '(2 1))
(test (map (cut <> 1 2 3) (list min max)) '(1 3))
(test (let ((n 0)) (for-each (cut <>) (list (lambda () (set! n (+ n 1))) (lambda () (set! n (+ n 10))))) n) 11)
;; the procedure position may be a slot or an expression
(test ((cut (lambda (x) (* x x)) <>) 7) 49)
(test ((cut <> <> <>) + 1 2) 3)
(test ((cut <> <...>) list 1 2 3) '(1 2 3))
(test ((cute cons (+ 1 2) <>) 'a) '(3 . a))
;; each use makes a fresh procedure of the right arity
(test (let ((f (cut list <> <>))) (guard (e (#t 'arity)) (f 1))) 'arity)
(test (let ((f (cut list 'a <...>))) (list (f) (f 1) (f 1 2))) '((a) (a 1) (a 1 2)))
;; hygiene: the user's x, rest and a variable named like the macro's internals
(test (let ((x 5) (rest 6)) ((cut list x <> rest) 'y)) '(5 y 6))
(test (let ((x 5)) ((cute list x <> <>) 1 2)) '(5 1 2))

(display "srfi-26: ") (display passed) (display " passed, ") (display failed) (display " failed") (newline)
