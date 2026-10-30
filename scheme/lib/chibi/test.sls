#!r6rs
;;;; (chibi test) for Arbace's Scheme (si), SM8: the subset of Chibi Scheme's (chibi test) that Chibi's r7rs-tests.scm
;;;; uses (test-begin, test-end, test, test-values, test-assert, test-error, test-exit), written for Arbace from the
;;;; description in that file's header, not vendored. Chibi's own output is a colored line per failure and a
;;;; summary; this one prints a line per test, which bin/si r7rs reads as the per-test result:
;;;;   TEST N PASS|FAIL <expression>          (a failure continues with "  expected: X" and "  got: Y" lines)
;;;;   BEGIN name / END name PASSED FAILED   and at test-exit a SUMMARY line.
;;;; Equality is equal?, with inexact numbers compared to a relative 1e-6 (Chibi uses an approximate comparison
;;;; for inexact numbers too; SC104).
(library (chibi test)
  (export test test-begin test-end test-assert test-error test-values test-exit)
  (import (rnrs) (rnrs mutable-strings))

  (define total 0)
  (define passed 0)
  (define failed 0)
  (define stack '())

  (define (approx? a b)
    (cond ((and (real? a) (real? b) (or (inexact? a) (inexact? b)))
           (or (= a b) (and (not (= a a)) (not (= b b)))
               (<= (abs (- a b)) (* 1e-6 (max 1 (abs a) (abs b))))))
          ((and (number? a) (number? b) (or (inexact? a) (inexact? b)) (not (and (real? a) (real? b))))
           (and (approx? (real-part a) (real-part b)) (approx? (imag-part a) (imag-part b))))
          ((and (pair? a) (pair? b)) (and (approx? (car a) (car b)) (approx? (cdr a) (cdr b))))
          ((and (vector? a) (vector? b) (= (vector-length a) (vector-length b)))
           (let loop ((i 0)) (or (= i (vector-length a)) (and (approx? (vector-ref a i) (vector-ref b i)) (loop (+ i 1))))))
          (else (equal? a b))))

  (define (short-text x)
    (call-with-values open-string-output-port
      (lambda (port extract)
        (write x port)
        (let ((s (extract)))
          (if (> (string-length s) 100) (string-append (substring s 0 100) "...") s)))))

  (define (record! ok? what)
    (set! total (+ total 1))
    (if ok? (set! passed (+ passed 1)) (set! failed (+ failed 1)))
    (display "TEST ") (display total) (display (if ok? " PASS " " FAIL ")) (display (short-text what)) (newline) (flush-output-port (current-output-port)))

  (define (run-test what expected thunk)
    (let ((got (guard (e (#t (cons 'raised e))) (call-with-values thunk list))))
      (cond ((and (pair? got) (eq? (car got) 'raised))
             (record! #f what)
             (display "  expected: ") (write expected) (newline)
             (display "  got exception: ")
             (display (short-text (let ((e (cdr got))) (if (condition? e) (condition-message* e) e)))) (newline))
            ((and (pair? got) (null? (cdr got)) (approx? expected (car got)))
             (record! #t what))
            (else
             (record! #f what)
             (display "  expected: ") (write expected) (newline)
             (display "  got: ") (write (if (and (pair? got) (null? (cdr got))) (car got) (cons 'values got))) (newline)))))

  (define (condition-message* e)
    (if (message-condition? e)
        (list (condition-message e) (if (irritants-condition? e) (condition-irritants e) '()))
        e))

  (define-syntax test
    (syntax-rules ()
      ((_ expected expr) (run-test 'expr expected (lambda () expr)))
      ((_ name expected expr) (run-test 'expr expected (lambda () expr)))))
  (define-syntax test-values
    (syntax-rules ()
      ((_ expected expr)
       (run-test-values 'expr (lambda () expected) (lambda () expr)))))
  (define (run-test-values what exp-thunk thunk)
    (let ((expected (call-with-values exp-thunk list)))
      (let ((got (guard (e (#t (cons 'raised e))) (call-with-values thunk list))))
        (cond ((and (pair? got) (eq? (car got) 'raised) (not (equal? expected got)))
               (record! #f what) (display "  got exception\n"))
              ((approx? expected got) (record! #t what))
              (else (record! #f what)
                    (display "  expected: ") (write expected) (newline)
                    (display "  got: ") (write got) (newline))))))
  (define-syntax test-assert
    (syntax-rules ()
      ((_ expr) (run-test 'expr #t (lambda () (if expr #t #f))))
      ((_ name expr) (run-test 'expr #t (lambda () (if expr #t #f))))))
  (define-syntax test-error
    (syntax-rules ()
      ((_ expr) (run-error 'expr (lambda () expr)))
      ((_ name expr) (run-error 'expr (lambda () expr)))))
  (define (run-error what thunk)
    (let ((r (guard (e (#t 'raised)) (call-with-values thunk list) 'returned)))
      (record! (eq? r 'raised) what)))

  (define (test-begin name . rest)
    (set! stack (cons (list name total passed failed) stack))
    (display "BEGIN ") (display name) (newline))
  (define (test-end . rest)
    (let ((top (car stack)))
      (set! stack (cdr stack))
      (display "END ") (display (car top)) (display " ")
      (display (- passed (caddr top))) (display " ") (display (- failed (cadddr top))) (newline)))
  (define (test-exit . rest)
    (display "SUMMARY ") (display passed) (display " ") (display failed) (newline)
    (exit (if (= failed 0) 0 1))))
