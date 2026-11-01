;;; psyntax's compat library for running the vendored psyntax on Chez Scheme 10.3.0, the bootstrap host
;;; (doc/scheme/SCHEME-SURVEY.md, "As built: SM4"). Arbace's own; not part of the expanded psyntax (that is
;;; scheme/psyntax/compat.ae). The interface is Ikarus's psyntax.compat.ss's export list; the bodies are new.

(library (psyntax compat)
  (export define-record make-parameter parameterize format gensym
          eval-core symbol-value set-symbol-value!
          make-struct-type read-annotated
          annotation? annotation-expression annotation-source
          annotation-stripped
          read-library-source-file
          library-version-mismatch-warning
          library-stale-warning
          file-locator-resolution-error
          label-binding set-label-binding! remove-location
          make-source-position-condition current-primitive-locations)
  (import (rnrs) (rnrs eval) (only (chezscheme) make-parameter parameterize format))

  (define-syntax define-record
    (lambda (stx)
      (define (id ctx . parts)
        (datum->syntax ctx
          (string->symbol
            (apply string-append
              (map (lambda (p) (if (symbol? p) (symbol->string p) p)) parts)))))
      (syntax-case stx ()
        ((_ name (field* ...) printer) #'(define-record name (field* ...)))
        ((_ name (field* ...))
         (let ((n (syntax->datum #'name)) (fs (syntax->datum #'(field* ...))))
           (with-syntax ((rtd (id #'name "rtd:" n))
                         (make (id #'name "make-" n))
                         (pred (id #'name n "?"))
                         ((acc ...) (map (lambda (f) (id #'name n "-" f)) fs))
                         ((mut ...) (map (lambda (f) (id #'name "set-" n "-" f "!")) fs)))
             #'(define-record-type (rtd make pred)
                 (fields (mutable field* acc mut) ...))))))))

  ;;; Deterministic names: the bootstrap's output must not depend on the session.
  (define gensym-count 0)
  (define gensym
    (case-lambda
      (() (gensym "g"))
      ((x)
       (set! gensym-count (+ gensym-count 1))
       (string->symbol
         (string-append (if (symbol? x) (symbol->string x) x) "$" (number->string gensym-count))))))

  ;;; The core forms psyntax builds, run by Chez: (primitive x) is the primitive named x or, for psyntax's
  ;;; own procedures, the location current-primitive-locations names; every free symbol is a top-level
  ;;; location of the store.
  (define store (make-eq-hashtable))
  (define (symbol-value x)
    (let ((v (hashtable-ref store x store)))
      (if (eq? v store) (assertion-violation 'symbol-value "unbound location" x) v)))
  (define (set-symbol-value! x v) (hashtable-set! store x v))
  (define (label-binding x) (hashtable-ref store x #f))
  (define (set-label-binding! x v) (hashtable-set! store x v))
  (define (remove-location x) (hashtable-delete! store x))
  (define current-primitive-locations
    (make-parameter (lambda (x) #f) (lambda (p) (assert (procedure? p)) p)))

  (define (eval-core x)
    (define (bound? x env) (memq x env))
    (define (formals->list f)
      (cond ((pair? f) (cons (car f) (formals->list (cdr f)))) ((null? f) '()) (else (list f))))
    (define (rw x env)
      (cond
        ((symbol? x) (if (bound? x env) x `(',symbol-value ',x)))
        ((pair? x)
         (case (car x)
           ((quote) x)
           ((primitive)
            (let ((loc ((current-primitive-locations) (cadr x))))
              (if loc `(',symbol-value ',loc) `',(eval (cadr x) (environment '(chezscheme))))))
           ((case-lambda)
            `(case-lambda
               ,@(map (lambda (c) (let ((env (append (formals->list (car c)) env)))
                                    (cons (car c) (map (lambda (e) (rw e env)) (cdr c)))))
                      (cdr x))))
           ((lambda)
            (let ((env (append (formals->list (cadr x)) env)))
              `(lambda ,(cadr x) ,@(map (lambda (e) (rw e env)) (cddr x)))))
           ((letrec letrec*)
            (let ((env (append (map car (cadr x)) env)))
              `(,(car x) ,(map (lambda (b) (list (car b) (rw (cadr b) env))) (cadr x))
                ,@(map (lambda (e) (rw e env)) (cddr x)))))
           ((if begin) (cons (car x) (map (lambda (e) (rw e env)) (cdr x))))
           ((set!)
            (if (bound? (cadr x) env)
                `(set! ,(cadr x) ,(rw (caddr x) env))
                `(',set-symbol-value! ',(cadr x) ,(rw (caddr x) env))))
           ((define) `(',set-symbol-value! ',(cadr x) ,(rw (caddr x) env)))
           (else (map (lambda (e) (rw e env)) x))))
        (else x)))
    (eval (rw x '()) (environment '(chezscheme))))

  (define (make-struct-type . args) (error 'make-struct-type "not supported" args))
  (define (read-annotated . args) (apply read args))
  (define (annotation? x) #f)
  (define (annotation-expression x) x)
  (define (annotation-source x) #f)
  (define (annotation-stripped x) x)
  (define (read-library-source-file filename)
    (with-input-from-file filename read))
  (define (library-version-mismatch-warning name depname filename) (values))
  (define (library-stale-warning name filename) (values))
  (define (file-locator-resolution-error libname failed-list pending-list)
    (raise (condition (make-error) (make-who-condition 'expander)
             (make-message-condition "cannot locate library in library-path")
             (make-irritants-condition (list libname)))))
  (define (make-source-position-condition file pos)
    (make-irritants-condition (list file pos))))
