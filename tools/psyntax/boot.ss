;;; Copyright (c) 2006, 2007 Abdulaziz Ghuloum and Kent Dybvig (the structure of psyntax-buildscript.ss); Arbace's
;;; changes the Arbace authors'. Under the MIT License (LICENSE.md, "psyntax").
;;;
;;; The bootstrap of psyntax for Arbace (doc/scheme/SCHEME-SURVEY.md, "As built: SM4"), run by Chez Scheme
;;; 10.3.0. Its structure follows psyntax-buildscript.ss of the portable psyntax (psyntax-r6rs rev4, 2007,
;;; Abdulaziz Ghuloum and Kent Dybvig, under the MIT notice in LICENSE.md): the vendored psyntax, loaded into
;;; Chez as R6RS libraries with a compat library for Chez (tools/psyntax/chez/psyntax/), expands psyntax's own
;;; sources with Arbace's compat and config, and the system's libraries are fabricated as install-library
;;; calls. The libraries' exports are Chez's (library-exports), the reference's view of R6RS.
;;;
;;;   chez --libdirs STAGE --script tools/psyntax/boot.ss SRC OUT
;;;
;;; STAGE holds the psyntax that runs (psyntax/*.ss: the vendored files and the Chez compat); SRC holds the
;;; sources expanded for Arbace (compat.ss, internal.ss, config.ss, library-manager.ss, builders.ss,
;;; expander.ss); OUT is the expanded system, one core form a line, written by Chez's `write` with gensyms as
;;; plain symbols NAME$N (deterministic).

(import (except (rnrs) identifier? generate-temporaries free-identifier=? bound-identifier=? datum->syntax
                syntax-violation syntax->datum make-variable-transformer)
        (prefix (rnrs eval) chez:) (only (chezscheme) command-line-arguments library-exports parameterize
                                 print-gensym print-graph)
        (psyntax compat) (psyntax internal) (psyntax library-manager) (psyntax expander))

(define src-dir (car (command-line-arguments)))
(define out-file (cadr (command-line-arguments)))

(define scheme-library-files
  (map (lambda (f) (string-append src-dir "/" f))
       '("compat.ss" "internal.ss" "config.ss" "library-manager.ss" "builders.ss" "expander.ss")))

;;; The bindings of the system's keywords: those psyntax's expander implements (core-macro-transformer,
;;; macro-transformer and the binding types of chi-body), and the condition types.
(define psyntax-system-macros
  (append
    '((define (define)) (define-syntax (define-syntax)) (module (module)) (begin (begin))
      (import (import)) (export (export)) (set! (set!)) (let-syntax (let-syntax))
      (letrec-syntax (letrec-syntax)) (stale-when (stale-when))
      (foreign-call (core-macro . foreign-call)) (quote (core-macro . quote))
      (syntax-case (core-macro . syntax-case)) (syntax (core-macro . syntax))
      (lambda (core-macro . lambda)) (case-lambda (core-macro . case-lambda))
      (type-descriptor (core-macro . type-descriptor)) (letrec (core-macro . letrec))
      (letrec* (core-macro . letrec*)) (if (core-macro . if))
      (fluid-let-syntax (core-macro . fluid-let-syntax))
      (record-type-descriptor (core-macro . record-type-descriptor))
      (record-constructor-descriptor (core-macro . record-constructor-descriptor)))
    (map (lambda (m) (list m (cons 'macro m)))
         '(define-record-type cond let do or and let* let-values let*-values syntax-rules quasiquote
           quasisyntax with-syntax when unless case identifier-syntax time delay assert endianness guard
           define-enumeration trace-lambda trace-define trace-let trace-define-syntax trace-let-syntax
           trace-letrec-syntax define-condition-type parameterize eol-style error-handling-mode
           buffer-mode file-options ... => _ else unquote unquote-splicing unsyntax unsyntax-splicing
           fields mutable immutable parent protocol sealed opaque nongenerative parent-rtd))
    (map (lambda (c)
           (let ((n (symbol->string c)))
             (list c (cons '$core-rtd (list (string->symbol (string-append n "-rtd"))
                                            (string->symbol (string-append n "-rcd")))))))
         '(&condition &message &warning &serious &error &violation &assertion &irritants &who
           &non-continuable &implementation-restriction &lexical &syntax &undefined &i/o &i/o-read
           &i/o-write &i/o-invalid-position &i/o-filename &i/o-file-protection &i/o-file-is-read-only
           &i/o-file-already-exists &i/o-file-does-not-exist &i/o-port &i/o-decoding &i/o-encoding
           &no-infinities &no-nans))))

;;; Procedures the output of psyntax's macros names beyond R6RS's (bless'd, so looked up in $all): the host's.
(define psyntax-runtime-names '(make-promise make-file-options make-traced-procedure make-traced-macro))

(define (macro-identifier x) (assq x psyntax-system-macros))

;;; The libraries: R6RS's, with Chez's export lists, then psyntax's two system libraries.
(define r6rs-libraries
  '((rnrs) (rnrs base) (rnrs unicode) (rnrs bytevectors) (rnrs lists) (rnrs sorting) (rnrs control)
    (rnrs records syntactic) (rnrs records procedural) (rnrs records inspection) (rnrs exceptions)
    (rnrs conditions) (rnrs io ports) (rnrs io simple) (rnrs files) (rnrs programs)
    (rnrs arithmetic fixnums) (rnrs arithmetic flonums) (rnrs arithmetic bitwise) (rnrs syntax-case)
    (rnrs hashtables) (rnrs enums) (rnrs eval) (rnrs mutable-pairs) (rnrs mutable-strings) (rnrs r5rs)))

;;; What Arbace's host provides to psyntax's compat (scheme/psyntax/compat.ae).
(define bootstrap-exports
  '(void gensym symbol-value set-symbol-value! eval-core label-binding set-label-binding!
    remove-location format read-library-source-file set-primitive-locations!))

(define (sort-symbols ls) (list-sort (lambda (a b) (string<? (symbol->string a) (symbol->string b))) ls))

(define library-legend
  (append
    (map (lambda (n) (list n (sort-symbols (library-exports n)) #t)) r6rs-libraries)
    (list (list '(psyntax system $bootstrap) bootstrap-exports #f))))

(define all-names
  (let f ((ls (apply append (map cadr library-legend))) (seen '()))
    (cond ((null? ls) (reverse seen))
          ((memq (car ls) seen) (f (cdr ls) seen))
          (else (f (cdr ls) (cons (car ls) seen))))))

(define rtd-names
  (apply append psyntax-runtime-names (map (lambda (m) (if (eq? (car (cadr m)) '$core-rtd) (cdr (cadr m)) '()))
                     psyntax-system-macros)))

;;; psyntax's procedures beyond R6RS's names that Arbace's driver calls (arbace.scheme.expand), in a library of
;;; their own, (psyntax arbace), invisible to programs.
(define arbace-extras
  '(core-expand top-level-expander program-expander library-expander boot-library-expand compile-r6rs-top-level
    new-interaction-environment interaction-environment environment-symbols install-library
    current-library-collection find-library-by-name library-exists? installed-libraries
    invoke-library visit-library uninstall-library library-path library-extensions
    current-library-expander library-name library-version library-subst
    syntax-dispatch syntax-error ellipsis-map assertion-error make-compile-time-value
    variable-transformer? variable-transformer-procedure syntax-transpose))

;;; A keyword of the reference that psyntax does not implement would become a procedure: refuse it.
(for-each
  (lambda (x)
    (unless (or (macro-identifier x)
                (guard (e (#t #f)) (chez:eval x (chez:environment '(rnrs) '(rnrs eval) '(rnrs r5rs)
                                                       '(rnrs mutable-pairs) '(rnrs mutable-strings)))
                  #t)
                (memq x bootstrap-exports))
      (error 'boot "a keyword of the reference psyntax has no binding for" x)))
  all-names)

(define (make-collection)
  (let ((set '()))
    (case-lambda (() set) ((x) (set! set (cons x set))))))

(define (lib-exports name)
  (cond ((assoc name library-legend) => cadr) (else '())))

;;; psyntax's own procedures, for the core forms the running psyntax evaluates while it expands.
(let ()
  (define-syntax define-prims
    (syntax-rules ()
      ((_ name* ...)
       (let ((g* (map (lambda (x) (gensym)) '(name* ...)))
             (v* (list name* ...)))
         (for-each set-symbol-value! g* v*)
         (let ((alist (map cons '(name* ...) g*)))
           (current-primitive-locations
             (lambda (x) (cond ((assq x alist) => cdr) (else #f)))))))))
  (define-prims
    syntax-dispatch generate-temporaries datum->syntax syntax->datum identifier? free-identifier=?
    bound-identifier=? syntax-violation syntax-error make-variable-transformer ellipsis-map
    assertion-error gensym))

(define bootstrap-collection
  (let ((ls '()))
    (case-lambda (() ls) ((x) (unless (memq x ls) (set! ls (cons x ls)))))))

;;; The libraries the sources import, installed in the running psyntax: every name a keyword or a
;;; primitive of the host.
(let* ((names (append all-names rtd-names))
       (labels (map (lambda (x) (gensym)) names))
       (export-subst (map cons names labels))
       (export-env (map (lambda (n l) (cons l (cond ((macro-identifier n) => cadr) (else (cons 'core-prim n)))))
                        names labels)))
  (for-each
    (lambda (entry)
      (let* ((name (car entry)) (ids (cadr entry))
             (subst (filter (lambda (p) (memq (car p) ids)) export-subst)))
        (parameterize ((current-library-collection bootstrap-collection))
          (install-library (gensym) name (if (eq? (car name) 'rnrs) '(6) '()) '() '() '()
            subst export-env void void '#f '#f '#f '() #t #f))))
    (cons (list '(psyntax system $all) (append all-names rtd-names) #f) library-legend)))

(define (read-all file)
  (with-input-from-file file
    (lambda () (let f () (let ((x (read))) (if (eof-object? x) '() (cons x (f))))))))

(define (expand-all files)
  (define code* '())
  (define subst '())
  (define env '())
  (for-each
    (lambda (file)
      (for-each
        (lambda (x)
          (let-values (((name code export-subst export-env) (boot-library-expand x)))
            (set! code* (cons code code*))
            (set! subst (append export-subst subst))
            (set! env (append export-env env))))
        (read-all file)))
    files)
  (values (reverse code*) subst env))

;;; The system's data: every name of the legend bound in the new system, a primitive of the host unless
;;; a library of psyntax defines it (then its location, a primloc), and psyntax's keywords.
(define (make-system-data subst env)
  (let ((export-subst (make-collection)) (export-env (make-collection)) (export-primlocs (make-collection)))
    (for-each
      (lambda (x)
        (let ((label (gensym)))
          (export-subst (cons (car x) label))
          (export-env (cons label (cadr x)))))
      psyntax-system-macros)
    (for-each
      (lambda (x)
        (cond
          ((macro-identifier x) (values))
          ((let ((p (assq x subst))) (and p (assq (cdr p) env) p)) =>
           (lambda (p)
             (let ((b (cdr (assq (cdr p) env))))
               (unless (eq? (car b) 'global) (error 'boot "invalid binding" x b))
               (export-subst (cons x (cdr p)))
               (export-env (cons (cdr p) (cons 'core-prim x)))
               (export-primlocs (cons x (cdr b))))))
          (else
           (let ((label (gensym)))
             (export-subst (cons x label))
             (export-env (cons label (cons 'core-prim x)))))))
      (append all-names rtd-names arbace-extras))
    (values (reverse (export-subst)) (reverse (export-env)) (reverse (export-primlocs)))))

(define (build-system-library export-subst export-env primlocs)
  (define (build-library name ids visible?)
    `(install-library ',(gensym) ',name ',(if (eq? (car name) 'rnrs) '(6) '()) '() '() '()
       ',(if ids (filter (lambda (p) (memq (car p) ids)) export-subst) export-subst)
       ',(if ids '() export-env)
       values values '#f '#f '#f '() ',visible? #f))
  (let ((code `(library (psyntax primlocs)
                 (export)
                 (import (only (psyntax library-manager) install-library)
                         (only (psyntax internal) current-primitive-locations)
                         (rnrs))
                 (current-primitive-locations
                   (lambda (x) (cond ((assq x ',primlocs) => cdr) (else #f))))
                 ,(build-library '(psyntax system $all) #f #f)
                 ,@(map (lambda (e) (build-library (car e) (cadr e) (caddr e)))
                        (append library-legend (list (list '(psyntax arbace) arbace-extras #f)))))))
    (let-values (((name code subst env) (boot-library-expand code)))
      code)))

(let-values (((code* subst env)
              (parameterize ((current-library-collection bootstrap-collection))
                (expand-all scheme-library-files))))
  (let-values (((export-subst export-env primlocs) (make-system-data subst env)))
    (let ((sys (parameterize ((current-library-collection bootstrap-collection))
                 (build-system-library export-subst export-env primlocs))))
      (let ((p (open-file-output-port out-file (file-options no-fail) (buffer-mode block)
                                      (native-transcoder))))
        (print-gensym #f)
        (for-each (lambda (x) (write x p) (newline p))
                  (append code* (list sys)))
        (close-port p)))))
