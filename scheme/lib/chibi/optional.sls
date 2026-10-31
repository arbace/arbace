#!r6rs
;;;; (chibi optional) for Arbace's Scheme (si), SC150: Chibi Scheme 0.12.0's library (scheme/vendor/chibi/chibi/optional.sld, optional.scm, vendored
;;;; unchanged) with the one macro its cond-expand else branch gets wrong on si, let-optionals*, written as Chibi's own does it: a bare variable
;;;; beside the (var default) pairs is a required position, an identifier at the tail is the rest list. The other definitions are optional.scm's
;;;; (BSD license, Alex Shinn; LICENSE.md). bin/si chibi-tests puts this directory before scheme/vendor/chibi on the library path.
(library (chibi optional)
  (export let-optionals let-optionals*
          opt-lambda opt-lambda*
          define-opt define-opt*
          let-keywords let-keywords*
          keyword-ref keyword-ref*)
  (import (rnrs))

  (define (list-copy l) (if (pair? l) (cons (car l) (list-copy (cdr l))) l))

  (define-syntax let-optionals*
    (syntax-rules ()
      ((_ opt-ls () . body)
       (begin . body))
      ((_ (op . args) vars . body)
       (let ((tmp (op . args)))
         (let-optionals* tmp vars . body)))
      ((_ tmp ((var default) . rest) . body)
       (let* ((tmp2 (if (pair? tmp) (cdr tmp) '()))
              (var (if (pair? tmp) (car tmp) default)))
         (let-optionals* tmp2 rest . body)))
      ((_ tmp (var . rest) . body)
       (let* ((tmp2 (cdr tmp))
              (var (car tmp)))
         (let-optionals* tmp2 rest . body)))
      ((_ tmp tail . body)
       (let ((tail (list-copy tmp))) . body))))

  (define-syntax symbol->keyword*
    (syntax-rules ()
      ((_ sym)
       (string->symbol (string-append (symbol->string sym) ":")))))




(define-syntax let*-to-let
  (syntax-rules ()
    ((let*-to-let letstar ls (vars ...) ((v . d) . rest) . body)
     (let*-to-let letstar ls (vars ... (v tmp (tmp . d))) rest . body))
    ((let*-to-let letstar ls (vars ...) (v . rest) . body)
     (let*-to-let letstar ls (vars ... (v tmp tmp)) rest . body))
    ((let*-to-let letstar ls ((var tmp bind) ...) rest . body)
     (letstar ls (bind ... . rest)
       (let ((var tmp) ...) . body)))))


(define-syntax let-optionals
  (syntax-rules ()
    ((let-optionals ls (var&default ... . rest) body ...)
     (let*-to-let let-optionals* ls () (var&default ... . rest) body ...))))



(define-syntax opt-lambda
  (syntax-rules ()
    ((opt-lambda vars . body)
     (lambda args (let-optionals args vars . body)))))


(define-syntax opt-lambda*
  (syntax-rules ()
    ((opt-lambda* vars . body)
     (lambda args (let-optionals* args vars . body)))))


(define-syntax define-opt
  (syntax-rules ()
    ((define-opt (name . vars) . body)
     (define name (opt-lambda vars . body)))))


(define-syntax define-opt*
  (syntax-rules ()
    ((define-opt* (name . vars) . body)
     (define name (opt-lambda* vars . body)))))

(define (mem-key key ls)
  (and (pair? ls)
       (pair? (cdr ls))
       (if (eq? key (car ls))
           ls
           (mem-key key (cddr ls)))))


(define (keyword-ref ls key . o)
  (cond ((mem-key key ls) => (lambda (cell) (cadr cell)))
        (else (and (pair? o) (car o)))))


(define-syntax keyword-ref*
  (syntax-rules ()
    ((keyword-ref* ls key default)
     (cond ((mem-key key ls) => cadr) (else default)))))

(define (symbol->keyword sym)
  (string->symbol (string-append (symbol->string sym) ":")))

(define-syntax let-key*-to-let
  (syntax-rules ()
    ((let-key*-to-let ls (vars ...) ((v d) . rest) . body)
     (let-key*-to-let ls (vars ... (v tmp ,(symbol->keyword 'v) d)) rest
                      . body))
    ((let-key*-to-let ls (vars ...) ((v k d) . rest) . body)
     (let-key*-to-let ls (vars ... (v tmp k d)) rest . body))
    ((let-key*-to-let ls ((var tmp k d) ...) rest . body)
     (let-keywords* ls ((tmp k d) ... . rest)
       (let ((var tmp) ...) . body)))))


(define-syntax let-keywords
  (syntax-rules ()
    ((let-keywords ls vars . body)
     (let-key*-to-let ls () vars . body))))

(define (remove-keywords ls keywords)
  (let lp ((ls ls) (res '()))
    (if (and (pair? ls) (pair? (cdr ls)))
        (if (memq (car ls) keywords)
            (lp (cddr ls) res)
            (lp (cddr ls) (cons (cadr ls) (cons (car ls) res))))
        (reverse res))))

(define-syntax remove-keywords*
  (syntax-rules ()
    ((remove-keywords* opt-ls (keys ...) ((var key default) . rest))
     (remove-keywords* opt-ls (keys ... key) rest))
    ((remove-keywords* opt-ls (keys ...) ((var default) . rest))
     (remove-keywords* opt-ls (keys ... ,(symbol->keyword* 'var)) rest))
    ((remove-keywords* opt-ls (keys ...) ())
     (remove-keywords opt-ls `(keys ...)))))


(define-syntax let-keywords*
  (syntax-rules ()
    ((let-keywords* opt-ls () . body)
     (begin . body))
    ((let-keywords* (op . args) vars . body)
     (let ((tmp (op . args)))
       (let-keywords* tmp vars . body)))
    ((let-keywords* opt-ls ((var) (vars . x) ...) . body)
     (let-keywords* opt-ls ((var #f) (vars . x) ...) . body))
    ((let-keywords* opt-ls ((var default) (vars . x) ...) . body)
     (let ((var (keyword-ref* opt-ls (symbol->keyword* 'var) default)))
       (let-keywords* opt-ls ((vars . x) ...) . body)))
    ((let-keywords* opt-ls ((var key default) (vars . x) ...) . body)
     (let ((var (keyword-ref* opt-ls `key default)))
       (let-keywords* opt-ls ((vars . x) ...) . body)))
    ((let-keywords* opt-ls ((vars . x) ... tail) . body)
     (let ((tail (remove-keywords* opt-ls () ((vars . x) ...))))
       (let-keywords* opt-ls ((vars . x) ...) . body)))))
)
