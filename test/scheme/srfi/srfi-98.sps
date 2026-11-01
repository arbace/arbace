#!r6rs
;;; SRFI 98 (srfi :98 os-environment-variables): the specification has two procedures and no tests
;;; (https://srfi.schemers.org/srfi-98/); these cases check what it states, on any environment: unknown names give
;;; #f, the alist holds string pairs, each pair agrees with get-environment-variable, PATH (set by every harness
;;; that runs this) is found. The environment is the one the process started with. Prints FAIL lines and a last line
;;; "srfi-98: P passed, F failed".
(import (rnrs) (srfi :98))

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

(define env (get-environment-variables))
(test (list? env) #t)
(test (for-all (lambda (p) (and (pair? p) (string? (car p)) (string? (cdr p)))) env) #t)
(test (for-all (lambda (p) (positive? (string-length (car p)))) env) #t)
(test (get-environment-variable "SRFI98_SURELY_UNSET_NAME") #f)
(test (get-environment-variable "") #f)
(test (get-environment-variable "A=B") #f)
(test (string? (get-environment-variable "PATH")) #t)
(test (not (not (assoc "PATH" env))) #t)
(test (get-environment-variable "PATH") (cdr (assoc "PATH" env)))
;; every name in the alist is found with the first value its entry has
(test (for-all (lambda (p) (string? (get-environment-variable (car p)))) env) #t)
(test (for-all (lambda (p) (equal? (get-environment-variable (car p)) (cdr (assoc (car p) env)))) env) #t)
(test (get-environment-variables) env)          ; stable between calls
(let ((s (get-environment-variable "PATH")))
  (test (and (string? s) (positive? (string-length s))) #t))
(test (guard (e (#t 'raised)) (get-environment-variable 'PATH)) 'raised)

(display "srfi-98: ") (display passed) (display " passed, ") (display failed) (display " failed") (newline)
