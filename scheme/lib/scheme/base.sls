#!r6rs
;;;; (scheme base): R7RS-small's base library over R6RS's (rnrs) on Arbace's Scheme (si), SM8 (SC1's step;
;;;; doc/scheme/SCHEME-SURVEY.md, "As built: SM8"). Written for Arbace from the R7RS-small report (2013, section 6 and
;;;; appendix A), not vendored. Where R7RS and R6RS name the same thing differently (error, define-record-type, case,
;;;; the range arguments of string and vector procedures, member and assoc with a predicate, bytevector-copy!,
;;;; parameter objects, promises, ports) this library defines the R7RS one; the rest is (rnrs) re-exported.
;;;; si's own string and bytevector port procedures come from (psyntax system $all).
;;;; Not provided: include and include-ci as expressions (define-library's include declarations are read by the
;;;; host, SM8b, SC107 (b)).
(library (scheme base)
  (export
    => ... _ * + - / < <= = > >= abs and append apply assoc assq assv begin binary-port? boolean=? boolean?
    bytevector bytevector-append bytevector-copy bytevector-copy! bytevector-length bytevector-u8-ref
    bytevector-u8-set! bytevector? caar cadr call-with-current-continuation call-with-port call-with-values
    call/cc car case cdar cddr cdr ceiling char->integer char-ready? char<=? char<? char=? char>=? char>? char?
    close-input-port close-output-port close-port complex? cond cond-expand cons current-error-port
    current-input-port current-output-port define define-record-type define-syntax define-values denominator
    do dynamic-wind else eof-object eof-object? eq? equal? eqv? error error-object-irritants error-object-message
    error-object? even? exact exact-integer-sqrt exact-integer? exact? expt features file-error? floor
    floor-quotient floor-remainder floor/ flush-output-port for-each gcd get-output-bytevector get-output-string
    guard if inexact inexact? input-port-open? input-port? integer->char integer? lambda lcm length let let*
    let*-values let-syntax let-values letrec letrec* letrec-syntax list list->string list->vector list-copy
    list-ref list-set! list-tail list? make-bytevector make-list make-parameter make-string make-vector map max
    member memq memv min modulo negative? newline not null? number->string number? numerator odd?
    open-input-bytevector open-input-string open-output-bytevector open-output-string or output-port-open?
    output-port? pair? parameterize peek-char port? peek-u8 positive? procedure? quasiquote quote quotient raise
    raise-continuable rational? rationalize read-bytevector read-bytevector! read-char read-error? read-line
    read-string read-u8 real? remainder reverse round set! set-car! set-cdr! square string string->list
    string->number string->symbol string->utf8 string->vector string-append string-copy string-copy! string-fill!
    string-for-each string-length string-map string-ref string-set! string<=? string<? string=? string>=?
    string>? string? substring symbol->string symbol=? symbol? syntax-error syntax-rules textual-port? truncate
    truncate-quotient truncate-remainder truncate/ u8-ready? unless unquote unquote-splicing utf8->string values
    vector vector->list vector->string vector-append vector-copy vector-copy! vector-fill! vector-for-each
    vector-length vector-map vector-ref vector-set! vector? when with-exception-handler write-bytevector
    write-char write-string write-u8 zero?
    ;; used by (scheme lazy), (scheme r5rs) and the other R7RS libraries
    delay delay-force make-promise promise? force)
  (import (except (rnrs)
                  map for-each string-for-each vector-for-each error define-record-type let-syntax letrec-syntax let*-values case string-copy string->list vector->list string-fill! vector-fill!
                  member assoc bytevector-copy bytevector-copy! utf8->string string->utf8 vector-map close-port close-input-port close-output-port
                  call-with-port exact-integer-sqrt)
          (prefix (only (rnrs) map for-each string-for-each vector-for-each define-record-type let-syntax letrec-syntax let*-values) r6:)
          (rnrs mutable-pairs)
          (only (rnrs r5rs) quotient remainder modulo)
          (except (rnrs mutable-strings) string-fill!)
          (only (rnrs) exact-integer-sqrt)
          (prefix (only (rnrs) vector-map) r6:)
          (prefix (only (rnrs) utf8->string string->utf8 close-port) r6:)
          (srfi :39 parameters)
          (only (psyntax system $all)
                open-bytevector-input-port open-bytevector-output-port
                get-bytevector-n get-bytevector-n! put-bytevector get-u8 lookahead-u8 put-u8
                get-line get-string-n put-string
                current-error-port))

  ;; ---------------------------------------------------------------------------------------- syntax

  ;; syntax-rules is psyntax's, R7RS's since SM8b (SC100 (b): a custom ellipsis, _ and ... as literals)

  ;; R7RS's let-syntax and letrec-syntax and let*-values open a scope (psyntax's splice a body's definitions)
  (define-syntax let-syntax
    (syntax-rules () ((_ binds body ...) (r6:let-syntax binds (let () body ...)))))
  (define-syntax letrec-syntax
    (syntax-rules () ((_ binds body ...) (r6:letrec-syntax binds (let () body ...)))))
  (define-syntax let*-values
    (syntax-rules ()
      ((_ () body ...) (let () body ...))
      ((_ binds body ...) (r6:let*-values binds body ...))))

  (define-syntax case
    (syntax-rules (else =>)
      ((_ key clause ...) (let ((k key)) (%case-clauses k clause ...)))))
  (define-syntax %case-clauses
    (syntax-rules (else =>)
      ((_ k) (if #f #f))
      ((_ k (else => proc)) (proc k))
      ((_ k (else e1 e2 ...)) (begin e1 e2 ...))
      ((_ k ((d ...) => proc) rest ...) (if (memv k '(d ...)) (proc k) (%case-clauses k rest ...)))
      ((_ k ((d ...) e1 e2 ...) rest ...) (if (memv k '(d ...)) (begin e1 e2 ...) (%case-clauses k rest ...)))))

  (define-syntax syntax-error
    (lambda (stx)
      (syntax-case stx ()
        ((_ msg arg ...)
         (syntax-violation 'syntax-error (syntax->datum #'msg) (syntax->datum #'(arg ...)))))))

  (define-syntax define-values
    (syntax-rules ()
      ((_ () expr) (define dummy (call-with-values (lambda () expr) (lambda args #f))))
      ((_ (var) expr) (define var expr))
      ((_ (var0 var1 ... varn) expr)
       (begin (define var0 (call-with-values (lambda () expr) list))
              (define var1 (let ((v (cadr var0))) (set-cdr! var0 (cddr var0)) v)) ...
              (define varn (let ((v (cadr var0))) (set! var0 (car var0)) v))))
      ((_ (var0 var1 ... . varn) expr)
       (begin (define var0 (call-with-values (lambda () expr) list))
              (define var1 (let ((v (cadr var0))) (set-cdr! var0 (cddr var0)) v)) ...
              (define varn (let ((v (cdr var0))) (set! var0 (car var0)) v))))
      ((_ var expr) (define var (call-with-values (lambda () expr) list)))))

  (define-syntax cond-expand
    (lambda (stx)
      (define (feature? f)
        (memq f '(r7rs exact-closed exact-complex ieee-float full-unicode ratios si arbace scheme-si
                  srfi-0 srfi-1 srfi-6 srfi-9 srfi-13 srfi-14 srfi-23 srfi-26 srfi-39 srfi-69 srfi-98
                  srfi-125 srfi-128 srfi-133 srfi-158)))
      (define (lib? l)
        (and (pair? l) (memq (car l) '(scheme srfi chibi rnrs)) #t))
      (define (test req)
        (syntax-case req (and or not library else)
          (id (identifier? #'id) (and (feature? (syntax->datum #'id)) #t))
          ((and r ...) (for-all test #'(r ...)))
          ((or r ...) (exists test #'(r ...)))
          ((not r) (not (test #'r)))
          ((library l) (lib? (syntax->datum #'l)))
          (_ #f)))
      (syntax-case stx (else)
        ((k) #'(begin))
        ((k (else form ...) . rest) #'(begin form ...))
        ((k (req form ...) . rest)
         (if (test #'req) #'(begin form ...) #'(k . rest))))))

  ;; define-record-type: R7RS's (define-record-type name (ctor field ...) pred (field accessor [modifier]) ...),
  ;; R6RS's forms passed on
  (define-syntax define-record-type
    (lambda (stx)
      (define (syntax->list s) (syntax-case s () ((a . d) (cons #'a (syntax->list #'d))) (_ '())))
      (define (r6rs-clause? c)
        (syntax-case c (fields parent protocol sealed opaque nongenerative parent-rtd)
          ((fields . _) #t) ((parent . _) #t) ((protocol . _) #t) ((sealed . _) #t) ((opaque . _) #t)
          ((nongenerative . _) #t) ((parent-rtd . _) #t) (_ #f)))
      (syntax-case stx ()
        ((_ name-spec) #'(r6:define-record-type name-spec))
        ((_ name-spec c1 . rest)
         (r6rs-clause? #'c1)
         #'(r6:define-record-type name-spec c1 . rest))
        ((_ tname ctor pred fspec ...)
         (let* ((fspecs (r6:map (lambda (f) (syntax-case f () ((n a) #'(n a #f)) ((n a m) #'(n a m)) (n #'(n #f #f))))
                             (syntax->list #'(fspec ...))))
                (fnames (r6:map (lambda (f) (syntax-case f () ((n a m) #'n))) fspecs)))
           (define (index-of name lst)
             (let loop ((l lst))
               (cond ((null? l) #f) ((eq? (syntax->datum (car l)) (syntax->datum name)) (car l)) (else (loop (cdr l))))))
           (with-syntax ((ctor-name (syntax-case #'ctor () ((c . _) #'c) (c #'c)))
                         (ctor-args (syntax-case #'ctor () ((_ . args) #'args) (_ fnames)))
                         ((fld ...) (r6:map (lambda (f)
                                           (syntax-case f ()
                                             ((n a #f) #'(immutable n a))
                                             ((n #f m) #'(mutable n #f m))
                                             ((n a m) #'(mutable n a m))))
                                         fspecs))
                         (pred-name #'pred))
             (with-syntax (((pval ...) (r6:map (lambda (n)
                                              (or (index-of n (syntax->list #'ctor-args)) #'#f))
                                            fnames)))
               #'(r6:define-record-type (tname ctor-name pred-name)
                   (fields fld ...)
                   (protocol (lambda (p) (lambda ctor-args (p pval ...))))))))))))
;; ---------------------------------------------------------------------------------------- promises

  (define-record-type (promise %make-promise promise?) (fields (mutable box)))
  (define (promise-done? p) (car (promise-box p)))
  (define (promise-value p) (cdr (promise-box p)))
  (define (promise-update! new old)
    (set-car! (promise-box old) (promise-done? new))
    (set-cdr! (promise-box old) (promise-value new))
    (promise-box-set! new (promise-box old)))
  (define (make-promise v)
    (if (promise? v) v (%make-promise (cons #t v))))
  (define (%delay-force thunk) (%make-promise (cons #f thunk)))
  (define-syntax delay-force (syntax-rules () ((_ e) (%delay-force (lambda () e)))))
  (define-syntax delay (syntax-rules () ((_ e) (delay-force (make-promise e)))))
  (define (force p)
    (if (promise? p)
        (if (promise-done? p)
            (promise-value p)
            (let ((p* ((promise-value p))))
              (unless (promise-done? p) (promise-update! p* p))
              (force p)))
        p))

  ;; ---------------------------------------------------------------------------------------- errors

  (define (error msg . irritants)
    (raise (condition (make-error) (make-message-condition msg) (make-irritants-condition irritants))))
  (define (error-object? x) (and (condition? x) (or (error? x) (message-condition? x)) #t))
  (define (error-object-message x)
    (if (and (condition? x) (message-condition? x)) (condition-message x) ""))
  (define (error-object-irritants x)
    (if (and (condition? x) (irritants-condition? x)) (condition-irritants x) '()))
  (define (file-error? x)
    (and (condition? x)
         (or (i/o-file-does-not-exist-error? x) (i/o-file-protection-error? x) (i/o-file-already-exists-error? x)
             (i/o-filename-error? x))
         #t))
  (define (read-error? x) (and (condition? x) (or (lexical-violation? x) (i/o-read-error? x)) #t))

  ;; ---------------------------------------------------------------------------------------- numbers

  (define (exact-integer? x) (and (integer? x) (exact? x)))
  (define (square x) (* x x))
  (define (floor-quotient n d) (floor (/ n d)))
  (define (floor-remainder n d) (modulo n d))
  (define (floor/ n d) (values (floor-quotient n d) (floor-remainder n d)))
  (define (truncate-quotient n d) (quotient n d))
  (define (truncate-remainder n d) (remainder n d))
  (define (truncate/ n d) (values (quotient n d) (remainder n d)))

  ;; ---------------------------------------------------------------------------------------- lists

  (define (make-list k . fill)
    (let ((f (if (null? fill) #f (car fill))))
      (let loop ((k k) (acc '())) (if (<= k 0) acc (loop (- k 1) (cons f acc))))))
  (define (list-copy l)
    (cond ((pair? l) (cons (car l) (list-copy (cdr l)))) (else l)))
  (define (list-set! l k obj)
    (let loop ((l l) (k k))
      (if (= k 0) (set-car! l obj) (loop (cdr l) (- k 1)))))
  (define member
    (case-lambda
      ((x l) (memp (lambda (y) (equal? x y)) l))
      ((x l =?) (memp (lambda (y) (=? x y)) l))))
  (define assoc
    (case-lambda
      ((x l) (let loop ((l l)) (cond ((null? l) #f) ((equal? x (caar l)) (car l)) (else (loop (cdr l))))))
      ((x l =?) (let loop ((l l)) (cond ((null? l) #f) ((=? x (caar l)) (car l)) (else (loop (cdr l))))))))

  ;; ---------------------------------------------------------------------------------------- ranges

  (define (range-end opt len) (if (and (pair? opt) (pair? (cdr opt))) (cadr opt) len))
  (define (range-start opt) (if (pair? opt) (car opt) 0))

  (define (vector->list v . r)
    (let ((e (range-end r (vector-length v))))
      (let loop ((i (- e 1)) (acc '()))
        (if (< i (range-start r)) acc (loop (- i 1) (cons (vector-ref v i) acc))))))
  (define (string->list s . r)
    (let ((e (range-end r (string-length s))))
      (let loop ((i (- e 1)) (acc '()))
        (if (< i (range-start r)) acc (loop (- i 1) (cons (string-ref s i) acc))))))
  (define (string->vector s . r) (list->vector (apply string->list s r)))
  (define (vector->string v . r) (list->string (apply vector->list v r)))
  (define (vector-copy v . r)
    (let* ((s (range-start r)) (e (range-end r (vector-length v))) (n (- e s)) (out (make-vector n)))
      (let loop ((i 0)) (when (< i n) (vector-set! out i (vector-ref v (+ s i))) (loop (+ i 1))))
      out))
  (define (vector-copy! to at from . r)
    (let* ((s (range-start r)) (e (range-end r (vector-length from))) (n (- e s)))
      (if (and (eq? to from) (< s at))
          (let loop ((i (- n 1))) (when (>= i 0) (vector-set! to (+ at i) (vector-ref from (+ s i))) (loop (- i 1))))
          (let loop ((i 0)) (when (< i n) (vector-set! to (+ at i) (vector-ref from (+ s i))) (loop (+ i 1)))))))
  (define (vector-fill! v x . r)
    (let ((e (range-end r (vector-length v))))
      (let loop ((i (range-start r))) (when (< i e) (vector-set! v i x) (loop (+ i 1))))))
  (define (vector-append . vs) (list->vector (apply append (map vector->list vs))))
  ;; R7RS's map and for-each stop at the shortest list (R6RS's require equal lengths)
  (define (any-ended? ls) (exists (lambda (l) (not (pair? l))) ls))
  (define (map f l . ls)
    (if (null? ls)
        (r6:map f l)
        (let loop ((ls (cons l ls)) (acc '()))
          (if (any-ended? ls)
              (reverse acc)
              (loop (r6:map cdr ls) (cons (apply f (r6:map car ls)) acc))))))
  (define (for-each f l . ls)
    (if (null? ls)
        (r6:for-each f l)
        (let loop ((ls (cons l ls)))
          (unless (any-ended? ls)
            (apply f (r6:map car ls))
            (loop (r6:map cdr ls))))))
  (define (string-for-each f s . ss)
    (apply for-each f (string->list s) (r6:map string->list ss)))
  (define (vector-for-each f v . vs)
    (apply for-each f (vector->list v) (r6:map vector->list vs)))
  (define vector-map
    (case-lambda
      ((f v) (let* ((n (vector-length v)) (out (make-vector n)))
               (let loop ((i 0)) (when (< i n) (vector-set! out i (f (vector-ref v i))) (loop (+ i 1))))
               out))
      ((f v . vs) (list->vector (apply map f (vector->list v) (r6:map vector->list vs))))))
  (define (string-map f s . ss)
    (list->string (apply map f (string->list s) (map string->list ss))))
  (define (string-copy s . r)
    (substring s (range-start r) (range-end r (string-length s))))
  (define (string-copy! to at from . r)
    (let* ((s (range-start r)) (e (range-end r (string-length from))) (n (- e s)))
      (if (and (eq? to from) (< s at))
          (let loop ((i (- n 1))) (when (>= i 0) (string-set! to (+ at i) (string-ref from (+ s i))) (loop (- i 1))))
          (let loop ((i 0)) (when (< i n) (string-set! to (+ at i) (string-ref from (+ s i))) (loop (+ i 1)))))))
  (define (string-fill! s c . r)
    (let ((e (range-end r (string-length s))))
      (let loop ((i (range-start r))) (when (< i e) (string-set! s i c) (loop (+ i 1))))))

  ;; ---------------------------------------------------------------------------------------- bytevectors

  (define (bytevector . bytes) (u8-list->bytevector bytes))
  (define (bytevector-copy bv . r)
    (let* ((s (range-start r)) (e (range-end r (bytevector-length bv))) (n (- e s)) (out (make-bytevector n)))
      (let loop ((i 0)) (when (< i n) (bytevector-u8-set! out i (bytevector-u8-ref bv (+ s i))) (loop (+ i 1))))
      out))
  (define (bytevector-copy! to at from . r)
    (let* ((s (range-start r)) (e (range-end r (bytevector-length from))) (n (- e s)))
      (if (and (eq? to from) (< s at))
          (let loop ((i (- n 1))) (when (>= i 0) (bytevector-u8-set! to (+ at i) (bytevector-u8-ref from (+ s i))) (loop (- i 1))))
          (let loop ((i 0)) (when (< i n) (bytevector-u8-set! to (+ at i) (bytevector-u8-ref from (+ s i))) (loop (+ i 1)))))))
  (define (bytevector-append . bvs)
    (u8-list->bytevector (apply append (map bytevector->u8-list bvs))))
  (define (utf8->string bv . r) (r6:utf8->string (apply bytevector-copy bv r)))
  (define (string->utf8 s . r) (r6:string->utf8 (apply string-copy s r)))

  ;; ---------------------------------------------------------------------------------------- ports

  ;; si's string ports: (open-string-input-port), and an output port with its accumulated text kept here
  (define (open-input-string s) (open-string-input-port s))
  (define string-extractors (make-eq-hashtable))
  (define (open-output-string)
    (call-with-values open-string-output-port
      (lambda (port extract) (hashtable-set! string-extractors port (cons extract "")) port)))
  (define (get-output-string port)
    (let* ((e (hashtable-ref string-extractors port #f))
           (s (string-append (cdr e) ((car e)))))
      (set-cdr! e s)
      s))
  (define closed (make-eq-hashtable))
  (define extractors (make-eq-hashtable))
  (define (close-port p) (hashtable-set! closed p #t) (r6:close-port p))
  (define (close-input-port p) (close-port p))
  (define (close-output-port p) (close-port p))
  (define (input-port-open? p) (and (input-port? p) (not (hashtable-ref closed p #f))))
  (define (output-port-open? p) (and (output-port? p) (not (hashtable-ref closed p #f))))
  (define (call-with-port p proc)
    (call-with-values (lambda () (proc p)) (lambda results (close-port p) (apply values results))))
  (define (open-input-bytevector bv) (open-bytevector-input-port bv))
  (define (open-output-bytevector)
    (call-with-values open-bytevector-output-port
      (lambda (port extract) (hashtable-set! extractors port extract) port)))
  (define (get-output-bytevector port) ((hashtable-ref extractors port #f)))
  (define (char-ready? . p) #t)
  (define (u8-ready? . p) #t)
  (define (in-port o) (if (pair? o) (car o) (current-input-port)))
  (define (out-port o) (if (pair? o) (car o) (current-output-port)))
  (define (read-line . p) (get-line (in-port p)))
  (define (read-string k . p)
    (if (= k 0) "" (get-string-n (in-port p) k)))
  (define (write-string s . r)
    (let* ((p (out-port r)) (r (if (pair? r) (cdr r) r)))
      (put-string p (apply string-copy s r))))
  (define (read-u8 . p) (get-u8 (in-port p)))
  (define (peek-u8 . p) (lookahead-u8 (in-port p)))
  (define (write-u8 b . p) (put-u8 (out-port p) b))
  (define (read-bytevector k . p)
    (if (= k 0) (make-bytevector 0) (get-bytevector-n (in-port p) k)))
  (define (read-bytevector! bv . r)
    (let* ((p (in-port r)) (r (if (pair? r) (cdr r) r))
           (s (range-start r)) (e (range-end r (bytevector-length bv))))
      (if (= s e) 0 (get-bytevector-n! p bv s (- e s)))))
  (define (write-bytevector bv . r)
    (let* ((p (out-port r)) (r (if (pair? r) (cdr r) r)))
      (put-bytevector p bv (range-start r) (- (range-end r (bytevector-length bv)) (range-start r)))))

  (define (features)
    '(r7rs exact-closed exact-complex ieee-float full-unicode ratios si arbace scheme-si))
)
