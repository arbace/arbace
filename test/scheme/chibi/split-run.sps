;;;; Chibi's own test library run test by test on si (SC142 (b)): argv FILE.sld, a (define-library NAME-test ...) whose
;;;; (define (run-tests) ...) holds the tests. A whole-library run of (chibi optional-test) ends at the expansion of the first test
;;;; that uses a variable beside the (var default) pairs (the fallback of (chibi optional)'s cond-expand does not take it),
;;;; so here the library's declarations are read as data: its imports (but those under a `chibi` feature clause), its
;;;; definitions (the begin forms of the else branches, the inline test macros) and the body of run-tests are evaluated one form
;;;; at a time in one interaction environment; a form that raises prints a "!" for each test it holds, and its error beside the others' marks
;;;; (a dot for a pass, x for a failure: the inline test macros'). Output: the marks, in order, as Chibi's (chibi test) prints them.
(import (rnrs) (rnrs eval) (srfi :98) (only (psyntax system $all) new-interaction-environment r7rs-write))

(define lib (call-with-input-file (cadr (command-line)) get-datum))
(define imports '())
(define defs '())
(define (decls! ds)
  (for-each
   (lambda (d)
     (case (car d)
       ((import) (set! imports (append imports (cdr d))))
       ((begin) (set! defs (append defs (cdr d))))
       ((cond-expand)
        (for-each (lambda (clause) (when (eq? (car clause) 'else) (decls! (cdr clause)))) (cdr d)))
       (else #f)))
   ds))
(decls! (cddr lib))
(define (run-tests-def? f) (and (pair? f) (eq? (car f) 'define) (pair? (cadr f)) (eq? (caadr f) 'run-tests)))
(define env (new-interaction-environment (apply environment imports)))
(define (short x)
  (let ((s (call-with-values open-string-output-port (lambda (p e) (r7rs-write x p) (e)))))
    (if (> (string-length s) 100) (substring s 0 100) s)))
(define (count-tests x)   ; the tests a form holds: its (test ...), (test-assert ...) and (test-error ...) forms
  (cond ((pair? x) (+ (if (memq (car x) '(test test-assert test-error)) 1 0)
                      (let loop ((l x) (n 0)) (cond ((pair? l) (loop (cdr l) (+ n (count-tests (car l))))) (else n)))))
        (else 0)))
(define (run f)
  (guard (e (#t (display (make-string (max 1 (count-tests f)) #\!)) (newline)
                (display "ERROR: ") (display (if (condition? e) (condition-message e) (short e)))
                (display ": ") (display (short f)) (newline)))
    (eval f env)))
(for-each (lambda (f) (unless (run-tests-def? f) (run f))) defs)
(for-each (lambda (f) (run `(let () ,f (if #f #f)))) (cddr (list-ref (filter run-tests-def? defs) 0)))
(newline)
