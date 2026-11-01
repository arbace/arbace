#!r6rs
;;; SRFI 39 (srfi :39 parameters): the examples of the SRFI document (https://srfi.schemers.org/srfi-39/srfi-39.html,
;;; the repository has no tests), with with-output-to-string written by hand, then cases of our own checked on Chez
;;; Scheme 10.3.0 (its make-parameter and parameterize behave the same on these). Prints FAIL lines and a last line
;;; "srfi-39: P passed, F failed".
(import (rnrs) (srfi :39))

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
(define-syntax test-error
  (syntax-rules ()
    ((_ expr)
     (let ((got (guard (e (#t 'raised)) expr 'returned)))
       (if (eq? got 'raised)
           (set! passed (+ passed 1))
           (begin (set! failed (+ failed 1))
                  (display "FAIL: ") (write 'expr) (display " => no error") (newline)))))))

(define radix (make-parameter 10))
(define write-shared
  (make-parameter #f
    (lambda (x)
      (if (boolean? x) x (error 'write-shared "only booleans are accepted by write-shared")))))
(test (radix) 10)
(radix 2)
(test (radix) 2)
(test-error (write-shared 0))
(test (write-shared) #f)

(define (object->string x)
  (call-with-string-output-port (lambda (p) (write x p))))
(define prompt
  (make-parameter 123
    (lambda (x) (if (string? x) x (object->string x)))))
(test (prompt) "123")
(prompt ">")
(test (prompt) ">")

(test (radix) 2)
(test (parameterize ((radix 16)) (radix)) 16)
(test (radix) 2)
(define (f n) (number->string n (radix)))
(test (f 10) "1010")
(test (parameterize ((radix 8)) (f 10)) "12")
(test (parameterize ((radix 8) (prompt (f 10))) (prompt)) "1010")

;; own cases
(define p (make-parameter 1 (lambda (x) (* x 10))))
(test (p) 10)
(test (parameterize ((p 2)) (p)) 20)           ; the converter applies to parameterize's value
(test (parameterize ((p 2)) (p 3) (p)) 30)     ; setting inside converts too
(test (p) 10)                                  ; the outer binding is untouched by the inner set
(test (parameterize ((p 2)) (parameterize ((p 3)) (p))) 30)
(test (parameterize ((p 2)) (parameterize ((radix 3)) (list (p) (radix)))) '(20 3))
(test (parameterize () 'empty) 'empty)
(test (parameterize ((radix 7)) (values 1 2) 3) 3)
(test (call-with-values (lambda () (parameterize ((radix 7)) (values 1 2))) list) '(1 2))
;; the binding ends when control leaves, by an exception too
(test (begin (guard (e (#t 'caught)) (parameterize ((radix 5)) (raise 'oops))) (radix)) 2)
;; and on re-entry by a continuation
(test (let ((k #f) (n 0) (seen '()))
        (parameterize ((radix 9))
          (call/cc (lambda (c) (set! k c)))
          (set! seen (cons (radix) seen)))
        (set! seen (cons (radix) seen))
        (set! n (+ n 1))
        (if (< n 2) (k #f))
        (reverse seen))
      '(9 2 9 2))
(test (let ((a (make-parameter 'a)) (b (make-parameter 'b)))
        (parameterize ((a 1) (b (a))) (list (a) (b))))
      '(1 a))
(test (procedure? radix) #t)
(test (let ((q (make-parameter 0))) (q 5) (q)) 5)

(display "srfi-39: ") (display passed) (display " passed, ") (display failed) (display " failed") (newline)
