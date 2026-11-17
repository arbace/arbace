;;; Copyright (c) 2006, 2007 Abdulaziz Ghuloum and Kent Dybvig (the structure of psyntax-buildscript.ss); Arbace's
;;; changes the Arbace authors'. Under the MIT License (LICENSE.md, "psyntax").
;;;
;;; The bootstrap of psyntax on si itself (SMR7b, doc/scheme/MATURITY-REVIEW.md, "As built: SMR7"; before, tools/psyntax/boot.ss on
;;; Chez Scheme 10.3.0): boot.ss's program with si's running psyntax (booted from the committed psyntax.pp) in the place of the
;;; vendored psyntax loaded into Chez. It expands psyntax's sources with Arbace's compat and config, fabricates the system's
;;; libraries as install-library calls, the libraries' exports the running system's (environment-symbols: the lists the previous
;;; boot installed, Chez's library-exports at the first), and writes a core form a line, every gensym renamed NAME$N in the order
;;; of its first appearance (deterministic, as Chez's boot named them by its counter).
;;;
;;;   si-light --pp psyntax.pp.ss --lib scheme/lib tools/psyntax/boot-si.sps SRC OUT

(import (rnrs) (rnrs eval) (rnrs mutable-pairs)
        (only (psyntax arbace) environment-symbols boot-library-expand install-library current-library-collection
              library-subst find-library-by-name)
        (only (psyntax system $all) gensym))

;;; Chez's void
(define (void) (if #f #f))

;;; psyntax's parameters are its compat's procedures: set around a body by hand
(define-syntax with-bootstrap-collection
  (syntax-rules ()
    ((_ e1 e2 ...)
     (let ((old (current-library-collection)))
       (dynamic-wind (lambda () (current-library-collection bootstrap-collection))
                     (lambda () e1 e2 ...)
                     (lambda () (current-library-collection old)))))))

;;; every gensym of the forms renamed NAME$N (N from 1, in the order met), as a plain symbol
(define (rename-gensyms forms)
  (let ((table (make-eq-hashtable)) (n 0))
    (define (gensym? x) (and (symbol? x) (not (eq? x (string->symbol (symbol->string x))))))
    (define (walk x)
      (cond ((gensym? x)
             (or (hashtable-ref table x #f)
                 (begin (set! n (+ n 1))
                        (let ((s (string->symbol (string-append (symbol->string x) "$" (number->string n)))))
                          (hashtable-set! table x s) s))))
            ((pair? x) (let ((a (walk (car x)))) (cons a (walk (cdr x)))))
            ((vector? x) (vector-map walk x))
            (else x)))
    (map walk forms)))

(define src-dir (cadr (command-line)))
(define out-file (caddr (command-line)))

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

;;; si's own primitives beyond R6RS's, in $all for the libraries of scheme/lib (SC105 (b), doc/scheme/SCHEME-SURVEY.md,
;;; "As built: SM8b"): Chez's real-time and cpu-time (milliseconds), a microsecond clock (since the epoch), and
;;; R7RS's printer for (scheme write) (SC103 (b)).
(define si-primitives
  '(real-time cpu-time current-microseconds watchdog r7rs-write r7rs-display r7rs-write-shared r7rs-write-simple
    ;; psyntax's own keywords beyond R6RS's, for (chezscheme) (SC110 (b)): Chez's internal modules
    module import
    ;; Chez's port procedures of (chezscheme) (SC180 (b)), go/arbace/scm/prim_io.ae
    unget-u8 unget-char port-has-port-length? port-has-set-port-length!? port-has-port-nonblocking??
    port-has-set-port-nonblocking!? fresh-line port-length set-port-length! port-nonblocking? set-port-nonblocking!))

(define (library-exports name) (environment-symbols (environment name)))

(define (sort-symbols ls) (list-sort (lambda (a b) (string<? (symbol->string a) (symbol->string b))) ls))

;;; R5RS's environments (SC63 (a), SM6): (null-environment 5) and (scheme-report-environment 5) are the libraries
;;; psyntax's expander looks up as (psyntax null-environment-5) and (psyntax scheme-report-environment-5). The null
;;; environment holds R5RS's syntactic keywords (section 7.1, with the auxiliary ones); the report environment
;;; R5RS's bindings: (rnrs base), (rnrs r5rs), the simple I/O of (rnrs io simple), (rnrs mutable-pairs) and
;;; (rnrs mutable-strings): a superset of R5RS's names (the report environment is a view of R6RS's).
(define r5rs-keywords
  '(quote lambda if set! begin cond case and or let let* letrec do delay quasiquote define define-syntax
    let-syntax letrec-syntax syntax-rules else => unquote unquote-splicing ... _))

(define r5rs-libraries '((rnrs base) (rnrs r5rs) (rnrs io simple) (rnrs mutable-pairs) (rnrs mutable-strings)))

(define (union-names ls)
  (let f ((ls (apply append ls)) (seen '()))
    (cond ((null? ls) (sort-symbols seen))
          ((memq (car ls) seen) (f (cdr ls) seen))
          (else (f (cdr ls) (cons (car ls) seen))))))

(define r5rs-null-names
  (let ((have (apply append (map library-exports r5rs-libraries))))
    (filter (lambda (n) (memq n have)) r5rs-keywords)))

(define r5rs-report-names (union-names (map library-exports r5rs-libraries)))

(define library-legend
  (append
    (map (lambda (n) (list n (sort-symbols (library-exports n)) #t)) r6rs-libraries)
    (list (list '(psyntax null-environment-5) r5rs-null-names #f)
          (list '(psyntax scheme-report-environment-5) r5rs-report-names #f)
          (list '(psyntax system $bootstrap) bootstrap-exports #f)
          (list '(psyntax system si) si-primitives #f))))

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
                (guard (e (#t #f)) (eval x (environment '(rnrs) '(rnrs eval) '(rnrs r5rs)
                                                       '(rnrs mutable-pairs) '(rnrs mutable-strings)))
                  #t)
                (memq x bootstrap-exports)
                (memq x si-primitives))
      (error 'boot "a keyword of the reference psyntax has no binding for" x)))
  all-names)

(define (make-collection)
  (let ((set '()))
    (case-lambda (() set) ((x) (set! set (cons x set))))))

(define (lib-exports name)
  (cond ((assoc name library-legend) => cadr) (else '())))

;;; psyntax's own procedures (syntax-dispatch, ...) for the core forms the running psyntax evaluates while it expands: si's
;;; running psyntax (booted from the committed psyntax.pp) has their locations already; Chez's boot set them for its own psyntax.

(define bootstrap-collection
  (let ((ls '()))
    (case-lambda (() ls) ((x) (unless (memq x ls) (set! ls (cons x ls)))))))

;;; The libraries the sources import, installed in the running psyntax: every name a keyword or a
;;; primitive of the host.
(let* ((names (append all-names rtd-names))
       ;; the running system's labels for the names it has: psyntax's bless'd identifiers (scheme-stx) are cached with them
       (real-subst (library-subst (find-library-by-name '(psyntax system $all))))
       (labels (map (lambda (x) (cond ((assq x real-subst) => cdr) (else (gensym)))) names))
       (export-subst (map cons names labels))
       (export-env (map (lambda (n l) (cons l (cond ((macro-identifier n) => cadr) (else (cons 'core-prim n)))))
                        names labels)))
  (for-each
    (lambda (entry)
      (let* ((name (car entry)) (ids (cadr entry))
             (subst (filter (lambda (p) (memq (car p) ids)) export-subst)))
        (with-bootstrap-collection
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
              (with-bootstrap-collection
                (expand-all scheme-library-files))))
  (let-values (((export-subst export-env primlocs) (make-system-data subst env)))
    (let ((sys (with-bootstrap-collection
                 (build-system-library export-subst export-env primlocs))))
      (let ((p (open-file-output-port out-file (file-options no-fail) (buffer-mode block)
                                      (native-transcoder))))
        (let ((forms (rename-gensyms (append code* (list sys)))))
          (for-each (lambda (x) (write x p) (newline p)) forms))
        (close-port p)))))
