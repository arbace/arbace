#!r6rs
;;; SRFI 158 tests: the official tests (srfi-158 repository, chicken-test.scm at
;;; ffd5bac4caf70167d0b57f701a6c43aa07701158), the Chicken test egg replaced by the small harness below; the R7RS
;;; forms the test uses are spelled in R6RS (#vu8, u8-list->bytevector, a string port in place of
;;; with-input-from-string, local truncate/ (positive arguments) and SRFI 1's unfold).
(import (rnrs) (srfi :158))
(define (truncate/ n d) (values (div n d) (mod n d)))
(define (unfold stop? mapper successor seed)
  (if (stop? seed) '() (cons (mapper seed) (unfold stop? mapper successor (successor seed)))))
(define (bytevector . l) (u8-list->bytevector l))
(define cur #f)
(define passed 0)
(define failed 0)
(define (iequal? x y)
  (cond ((and (pair? x) (pair? y)) (and (iequal? (car x) (car y)) (iequal? (cdr x) (cdr y))))
        ((and (vector? x) (vector? y)) (iequal? (vector->list x) (vector->list y)))
        (else (equal? x y))))
(define (report expr got expected ok)
  (if ok (set! passed (+ passed 1))
      (begin (set! failed (+ failed 1))
             (display "FAIL: ") (write expr) (display " => ") (write got)
             (display ", expected ") (write expected) (newline))))
(define (run-test expr thunk expected)
  (let ((got (guard (c (#t (list 'raised (if (message-condition? c) (condition-message c) c)))) (thunk))))
    (report expr got expected (iequal? got expected))))
(define-syntax test
  (syntax-rules ()
    ((_ name expected actual) (run-test 'actual (lambda () actual) expected))
    ((_ expected actual) (run-test 'actual (lambda () actual) expected))))
(define-syntax test-assert
  (syntax-rules ()
    ((_ name expr) (run-test 'expr (lambda () (and expr #t)) #t))
    ((_ expr) (run-test 'expr (lambda () (and expr #t)) #t))))
(define-syntax test-deny
  (syntax-rules ()
    ((_ name expr) (run-test 'expr (lambda () (and (not expr) #t)) #t))
    ((_ expr) (run-test 'expr (lambda () (and (not expr) #t)) #t))))
(define-syntax test-error
  (syntax-rules ()
    ((_ name expr) (test-error expr))
    ((_ expr) (let ((r (guard (c (#t 'raised)) expr 'no-error)))
                (report 'expr r 'raised (eq? r 'raised))))))
(define-syntax test-group
  (syntax-rules ()
    ((_ name expr) expr)
    ((_ name expr-or-defn stuff ...) (let () expr-or-defn (test-group name stuff ...)))))
(define (test-end-report n)
  (display n) (display ": ") (display passed) (display " passed, ")
  (display failed) (display " failed") (newline))


(test-group "generators"
  (test-group "generators/constructors"
    (test '() (generator->list (generator)))
    (test '(1 2 3) (generator->list (generator 1 2 3)))
    (test '(1 2 3 1 2) (generator->list (circular-generator 1 2 3) 5))
    (test '(8 9 10) (generator->list (make-iota-generator 3 8)))
    (test '(8 10 12) (generator->list (make-iota-generator 3 8 2)))
    (test '(3 4 5 6) (generator->list (make-range-generator 3) 4))
    (test '(3 4 5 6 7) (generator->list (make-range-generator 3 8)))
    (test '(3 5 7) (generator->list (make-range-generator 3 8 2)))
    (define g
      (make-coroutine-generator
       (lambda (yield) (let loop ((i 0))
                   (when (< i 3) (yield i) (loop (+ i 1)))))))
    (test '(0 1 2) (generator->list g))
    (test '(1 2 3 4 5) (generator->list (list->generator '(1 2 3 4 5))))
    (test '(1 2 3 4 5) (generator->list (vector->generator '#(1 2 3 4 5))))
    (test '#(0 0 1 2 4)
      (let ((v (make-vector 5 0)))
        (generator->vector! v 2 (generator 1 2 4))
        v))
    (test '(5 4 3 2 1) (generator->list (reverse-vector->generator '#(1 2 3 4 5))))
    (test '(#\a #\b #\c #\d #\e) (generator->list (string->generator "abcde")))
    (test '(10 20 30) (generator->list (bytevector->generator (bytevector 10 20 30))))
    (define (for-each-digit proc n)
      (when (> n 0)
        (let-values (((div rem) (truncate/ n 10)))
          (proc rem)
          (for-each-digit proc div))))
     (test '(5 4 3 2 1) (generator->list
			 (make-for-each-generator for-each-digit
						  12345)))
    (test '(0 2 4 6 8 10) (generator->list
                            (make-unfold-generator
                              (lambda (s) (> s 5))
                              (lambda (s) (* s 2))
                              (lambda (s) (+ s 1))
                              0)))
  ) ; end "generators/constructors"

  (test-group "generators/operators"
    (test '(a b 0 1) (generator->list (gcons* 'a 'b (make-range-generator 0 2))))
    (test '(0 1 2 0 1) (generator->list (gappend (make-range-generator 0 3)
                                                 (make-range-generator 0 2))))
    (test '() (generator->list (gappend)))
    (define g1 (generator 1 2 3))
    (define g2 (generator 4 5 6 7))
    (define (proc . args) (values (apply + args) (apply + args)))
    (test '(15 22 31) (generator->list (gcombine proc 10 g1 g2)))
    (test '(1 3 5 7 9) (generator->list (gfilter
                                           odd?
                                           (make-range-generator 1 11))))
    (test '(2 4 6 8 10) (generator->list (gremove
                                           odd?
                                           (make-range-generator 1 11))))
    (define g (make-range-generator 1 5))
    (test '(1 2 3) (generator->list (gtake g 3)))
    (test '(4) (generator->list g))
    (test '(1 2) (generator->list (gtake (make-range-generator 1 3) 3)))
    (test '(1 2 0) (generator->list (gtake (make-range-generator 1 3) 3 0)))
    (test '(3 4) (generator->list (gdrop (make-range-generator 1 5) 2)))
    (define g (make-range-generator 1 5))
    (define (small? x) (< x 3))
    (test '(1 2) (generator->list (gtake-while small? g)))
    (define g (make-range-generator 1 5))
    (test '(3 4) (generator->list (gdrop-while small? g)))
    (test '() (generator->list (gdrop-while (lambda args #t) (generator 1 2 3))))
    (test '(0.0 1.0 0 2) (generator->list (gdelete 1
                                                   (generator 0.0 1.0 0 1 2))))
    (test '(0.0 0 2) (generator->list (gdelete 1
                                               (generator 0.0 1.0 0 1 2)
                                               =)))
    (test '(a c e) (generator->list (gindex (list->generator '(a b c d e f))
                                            (list->generator '(0 2 4)))))
    (test '(a d e) (generator->list (gselect (list->generator '(a b c d e f))
                                             (list->generator '(#t #f #f #t #t #f)))))
    (test '(1 2 3) (generator->list (gdelete-neighbor-dups
                                      (generator 1 1 2 3 3 3)
                                      =)))
    (test '(1) (generator->list (gdelete-neighbor-dups
				(generator 1 2 3)
				(lambda args #t))))
    (test '(1 2 3 a b c)
          (generator->list
             (gflatten (generator '(1 2 3) '(a b c)))))
    (test '((1 2 3) (4 5 6) (7 8))
          (generator->list (ggroup (generator 1 2 3 4 5 6 7 8) 3)))
    (test '((1 2 3) (4 5 6) (7 8 0))
          (generator->list (ggroup (generator 1 2 3 4 5 6 7 8) 3 0)))
    (test '(1 2 3)
          (generator->list (gmerge < (generator 1 2 3))))
    (test '(1 2 3 4 5 6)
          (generator->list (gmerge < (generator 1 2 3) (generator 4 5 6))))
    (test '(1 2 3 4 4 5 6)
          (generator->list (gmerge <
                                   (generator 1 2 4 6)
                                   (generator)
                                   (generator 3 4 5))))
    (test '(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15)
          (generator->list (gmerge <
                                   (generator 1 10 11)
                                   (generator 2 9 12)
                                   (generator 3 8 13)
                                   (generator 4 7 14)
                                   (generator 5 6 15))))
    ;; check the tie-break rule
    (test '((1 a) (1 e) (1 b) (1 c) (1 d))
          (generator->list (gmerge (lambda (x y) (< (car x) (car y)))
                                   (generator '(1 a) '(1 e))
                                   (generator '(1 b))
                                   (generator '(1 c) '(1 d)))))
    
    (test '(-1 -2 -3 -4 -5)
          (generator->list (gmap - (generator 1 2 3 4 5))))
    (test '(7 9 11 13)
          (generator->list (gmap +
                                 (generator 1 2 3 4 5)
                                 (generator 6 7 8 9))))
    (test '(54 140 264)
          (generator->list (gmap *
                                 (generator 1 2 3 4 5)
                                 (generator 6 7 8)
                                 (generator 9 10 11 12 13))))
    (test '(a c e g i)
          (generator->list
            (gstate-filter
              (lambda (item state) (values (even? state) (+ 1 state)))
              0
              (generator 'a 'b 'c 'd 'e 'f 'g 'h 'i 'j))))
  ) ; end "generators/operators"


  (test-group "generators/consumers"
    ;; no test for plain generator->list (used throughout)
    (test '(1 2 3) (generator->list (generator 1 2 3 4 5) 3))
    (test '(5 4 3 2 1) (generator->reverse-list (generator 1 2 3 4 5)))
    (test '#(1 2 3 4 5) (generator->vector (generator 1 2 3 4 5)))
    (test '#(1 2 3) (generator->vector (generator 1 2 3 4 5) 3))
    (test "abc" (generator->string (generator #\a #\b #\c)))
    (test '(e d c b a . z) ((lambda (th) (set! cur (open-string-input-port "a b c d e")) (th))
                             (lambda () (generator-fold cons 'z (lambda () (read cur))))))

    (define n 0)
    (generator-for-each (lambda values (set! n (apply + values)))
      (generator 1) (generator 2) (generator 3))
    (test 6 n)
    (test '(6 15)
      (generator-map->list (lambda values (apply + values))
        (generator 1 4) (generator 2 5) (generator 3 6)))
    (test 3 (generator-find (lambda (x) (> x 2)) (make-range-generator 1 5)))
    (test #f (generator-find (lambda (x) (> x 10)) (make-range-generator 1 5)))
    (test 2 (generator-count odd? (make-range-generator 1 5)))
    (define g (make-range-generator 2 5))
    (test #t (generator-any odd? g))
    (test '(4) (generator->list g))
    (define g (make-range-generator 2 5))
    (test 3 (generator-any (lambda (x) (and (odd? x) x)) g))
    (define g (make-range-generator 2 5))
    (test #f (generator-every odd? g))
    (test '(3 4) (generator->list g))
    (define g (make-range-generator 2 5))
    (test 4 (generator-every (lambda (x) (and (> x 1) x)) g))
    (test '() (generator->list g))
    (test '(#\a #\b #\c) (generator-unfold (make-for-each-generator string-for-each "abc") unfold))

  ) ; end "generators/consumers"

) ; end "generators"

(test-group "accumulators"
  (test -8
    (let ((a (make-accumulator * 1 -)))
      (a 1)
      (a 2)
      (a 4)
      (a (eof-object))))

  (test 3
    (let ((a (count-accumulator)))
      (a 1)
      (a 2)
      (a 4)
      (a (eof-object))))

   (test '(1 2 4)
     (let ((a (list-accumulator)))
       (a 1)
       (a 2)
       (a 4)
       (a (eof-object))))

(test '(4 2 1)
  (let ((a (reverse-list-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test '#(1 2 4)
  (let ((a (vector-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test '#(0 0 1 2 4)
  (let* ((v (vector 0 0 0 0 0))
         (a (vector-accumulator! v 2)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test '#vu8(0 0 1 2 4)
  (let* ((v (bytevector 0 0 0 0 0))
         (a (bytevector-accumulator! v 2)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test '#(4 2 1)
  (let ((a (reverse-vector-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test "abc"
  (let ((a (string-accumulator)))
    (a #\a)
    (a #\b)
    (a #\c)
    (a (eof-object))))

(test #vu8(1 2 4)
  (let ((a (bytevector-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test 7
  (let ((a (sum-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

(test 8
  (let ((a (product-accumulator)))
    (a 1)
    (a 2)
    (a 4)
    (a (eof-object))))

) ; end "accumulators"
(test-end-report 'srfi-158)
