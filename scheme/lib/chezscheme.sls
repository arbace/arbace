#!chezscheme
;;;; (chezscheme): a subset of Chez Scheme 10.3.0's library on si (SM8b, SC110 (b), SC10 (a); doc/scheme/SCHEME-SURVEY.md,
;;;; "As built: SM8b"): the names of (rnrs), (rnrs eval), (rnrs mutable-pairs), (rnrs mutable-strings) and (rnrs r5rs)
;;;; (the list is Chez's library-exports of them), and the Chez extensions the mats and Chez programs use most, written
;;;; for Arbace from Chez's documentation (The Chez Scheme User's Guide, version 10): the fixnum and flonum comparisons
;;;; of one or more arguments (fx= ... fl>=), fx1+ and relatives, 1+ 1- add1 sub1, void, iota, list-head, last-pair,
;;;; sort and merge, list*, make-list, errorf, warningf, the string ports of Chez's names, bytevector, ash and the
;;;; logical operations, random, nonnegative? and nonpositive?, gensym, rec, printf, fprintf, format (si's: ~a ~s ~d ~%
;;;; ~n ~~), pretty-print (as write), andmap, ormap, the fixnum bounds, eq-hashtable?, the fixnum shifts and logical
;;;; operations, integer-length, bignum? and ratnum?, boxes, append!, reverse!, list-copy, fluid-let,
;;;; psyntax's module and import (Chez's internal modules), real-time and cpu-time (si's clock), and make-parameter and
;;;; parameterize (SRFI 39's).
;;;; Read in Chez's lexical mode (#!chezscheme: si's R7RS mode, which takes 1+ and #%name). Not provided: Chez's format directives beyond si's, define-record, define-ftype and the foreign interface,
;;;; the compiler's parameters (optimize-level, enable-cp0, ...), threads.
(library (chezscheme)
  (export
    &assertion &condition &error &i/o &i/o-decoding &i/o-encoding &i/o-file-already-exists &i/o-file-does-not-exist
    &i/o-file-is-read-only &i/o-file-protection &i/o-filename &i/o-invalid-position &i/o-port &i/o-read &i/o-write
    &implementation-restriction &irritants &lexical &message &no-infinities &no-nans &non-continuable &serious
    &syntax &undefined &violation &warning &who * + - ... / < <= = => > >= _ abs acos and angle append apply asin
    assert assertion-violation assertion-violation? assoc assp assq assv atan begin binary-port? bitwise-and
    bitwise-arithmetic-shift bitwise-arithmetic-shift-left bitwise-arithmetic-shift-right bitwise-bit-count
    bitwise-bit-field bitwise-bit-set? bitwise-copy-bit bitwise-copy-bit-field bitwise-first-bit-set bitwise-if
    bitwise-ior bitwise-length bitwise-not bitwise-reverse-bit-field bitwise-rotate-bit-field bitwise-xor boolean=?
    boolean? bound-identifier=? buffer-mode buffer-mode? bytevector->sint-list bytevector->string bytevector->u8-list
    bytevector->uint-list bytevector-copy bytevector-copy! bytevector-fill! bytevector-ieee-double-native-ref
    bytevector-ieee-double-native-set! bytevector-ieee-double-ref bytevector-ieee-double-set!
    bytevector-ieee-single-native-ref bytevector-ieee-single-native-set! bytevector-ieee-single-ref
    bytevector-ieee-single-set! bytevector-length bytevector-s16-native-ref bytevector-s16-native-set!
    bytevector-s16-ref bytevector-s16-set! bytevector-s32-native-ref bytevector-s32-native-set! bytevector-s32-ref
    bytevector-s32-set! bytevector-s64-native-ref bytevector-s64-native-set! bytevector-s64-ref bytevector-s64-set!
    bytevector-s8-ref bytevector-s8-set! bytevector-sint-ref bytevector-sint-set! bytevector-u16-native-ref
    bytevector-u16-native-set! bytevector-u16-ref bytevector-u16-set! bytevector-u32-native-ref
    bytevector-u32-native-set! bytevector-u32-ref bytevector-u32-set! bytevector-u64-native-ref
    bytevector-u64-native-set! bytevector-u64-ref bytevector-u64-set! bytevector-u8-ref bytevector-u8-set!
    bytevector-uint-ref bytevector-uint-set! bytevector=? bytevector? caaaar caaadr caaar caadar caaddr caadr caar
    cadaar cadadr cadar caddar cadddr caddr cadr call-with-bytevector-output-port call-with-current-continuation
    call-with-input-file call-with-output-file call-with-port call-with-string-output-port call-with-values call/cc
    car case case-lambda cdaaar cdaadr cdaar cdadar cdaddr cdadr cdar cddaar cddadr cddar cdddar cddddr cdddr cddr
    cdr ceiling char->integer char-alphabetic? char-ci<=? char-ci<? char-ci=? char-ci>=? char-ci>? char-downcase
    char-foldcase char-general-category char-lower-case? char-numeric? char-title-case? char-titlecase char-upcase
    char-upper-case? char-whitespace? char<=? char<? char=? char>=? char>? char? close-input-port close-output-port
    close-port command-line complex? cond condition condition-accessor condition-irritants condition-message
    condition-predicate condition-who condition? cons cons* cos current-error-port current-input-port
    current-output-port datum->syntax define define-condition-type define-enumeration define-record-type
    define-syntax delay delete-file denominator display div div-and-mod div0 div0-and-mod0 do dynamic-wind else
    endianness enum-set->list enum-set-complement enum-set-constructor enum-set-difference enum-set-indexer
    enum-set-intersection enum-set-member? enum-set-projection enum-set-subset? enum-set-union enum-set-universe
    enum-set=? environment eof-object eof-object? eol-style eq? equal-hash equal? eqv? error error-handling-mode
    error? eval even? exact exact->inexact exact-integer-sqrt exact? exists exit exp expt fields file-exists?
    file-options filter find finite? fixnum->flonum fixnum-width fixnum? fl* fl+ fl- fl/ fl<=? fl<? fl=? fl>=? fl>?
    flabs flacos flasin flatan flceiling flcos fldenominator fldiv fldiv-and-mod fldiv0 fldiv0-and-mod0 fleven? flexp
    flexpt flfinite? flfloor flinfinite? flinteger? fllog flmax flmin flmod flmod0 flnan? flnegative? flnumerator
    flodd? flonum? floor flpositive? flround flsin flsqrt fltan fltruncate flush-output-port flzero? fold-left
    fold-right for-all for-each force free-identifier=? fx* fx*/carry fx+ fx+/carry fx- fx-/carry fx<=? fx<? fx=?
    fx>=? fx>? fxand fxarithmetic-shift fxarithmetic-shift-left fxarithmetic-shift-right fxbit-count fxbit-field
    fxbit-set? fxcopy-bit fxcopy-bit-field fxdiv fxdiv-and-mod fxdiv0 fxdiv0-and-mod0 fxeven? fxfirst-bit-set fxif
    fxior fxlength fxmax fxmin fxmod fxmod0 fxnegative? fxnot fxodd? fxpositive? fxreverse-bit-field
    fxrotate-bit-field fxxor fxzero? gcd generate-temporaries get-bytevector-all get-bytevector-n get-bytevector-n!
    get-bytevector-some get-char get-datum get-line get-string-all get-string-n get-string-n! get-u8 greatest-fixnum
    guard hashtable-clear! hashtable-contains? hashtable-copy hashtable-delete! hashtable-entries
    hashtable-equivalence-function hashtable-hash-function hashtable-keys hashtable-mutable? hashtable-ref
    hashtable-set! hashtable-size hashtable-update! hashtable? i/o-decoding-error? i/o-encoding-error-char
    i/o-encoding-error? i/o-error-filename i/o-error-port i/o-error-position i/o-error?
    i/o-file-already-exists-error? i/o-file-does-not-exist-error? i/o-file-is-read-only-error?
    i/o-file-protection-error? i/o-filename-error? i/o-invalid-position-error? i/o-port-error? i/o-read-error?
    i/o-write-error? identifier-syntax identifier? if imag-part immutable implementation-restriction-violation?
    inexact inexact->exact inexact? infinite? input-port? integer->char integer-valued? integer? irritants-condition?
    lambda latin-1-codec lcm least-fixnum length let let* let*-values let-syntax let-values letrec letrec*
    letrec-syntax lexical-violation? list list->string list->vector list-ref list-sort list-tail list? log
    lookahead-char lookahead-u8 magnitude make-assertion-violation make-bytevector make-custom-binary-input-port
    make-custom-binary-input/output-port make-custom-binary-output-port make-custom-textual-input-port
    make-custom-textual-input/output-port make-custom-textual-output-port make-enumeration make-eq-hashtable
    make-eqv-hashtable make-error make-hashtable make-i/o-decoding-error make-i/o-encoding-error make-i/o-error
    make-i/o-file-already-exists-error make-i/o-file-does-not-exist-error make-i/o-file-is-read-only-error
    make-i/o-file-protection-error make-i/o-filename-error make-i/o-invalid-position-error make-i/o-port-error
    make-i/o-read-error make-i/o-write-error make-implementation-restriction-violation make-irritants-condition
    make-lexical-violation make-message-condition make-no-infinities-violation make-no-nans-violation
    make-non-continuable-violation make-polar make-record-constructor-descriptor make-record-type-descriptor
    make-rectangular make-serious-condition make-string make-syntax-violation make-transcoder
    make-undefined-violation make-variable-transformer make-vector make-violation make-warning make-who-condition map
    max member memp memq memv message-condition? min mod mod0 modulo mutable nan? native-endianness native-eol-style
    native-transcoder negative? newline no-infinities-violation? no-nans-violation? non-continuable-violation?
    nongenerative not null-environment null? number->string number? numerator odd? opaque open-bytevector-input-port
    open-bytevector-output-port open-file-input-port open-file-input/output-port open-file-output-port
    open-input-file open-output-file open-string-input-port open-string-output-port or output-port-buffer-mode
    output-port? pair? parent parent-rtd partition peek-char port-eof? port-has-port-position?
    port-has-set-port-position!? port-position port-transcoder port? positive? procedure? protocol put-bytevector
    put-char put-datum put-string put-u8 quasiquote quasisyntax quote quotient raise raise-continuable
    rational-valued? rational? rationalize read read-char real->flonum real-part real-valued? real? record-accessor
    record-constructor record-constructor-descriptor record-field-mutable? record-mutator record-predicate record-rtd
    record-type-descriptor record-type-descriptor? record-type-field-names record-type-generative? record-type-name
    record-type-opaque? record-type-parent record-type-sealed? record-type-uid record? remainder remove remp remq
    remv reverse round scheme-report-environment sealed serious-condition? set! set-car! set-cdr! set-port-position!
    simple-conditions sin sint-list->bytevector sqrt standard-error-port standard-input-port standard-output-port
    string string->bytevector string->list string->number string->symbol string->utf16 string->utf32 string->utf8
    string-append string-ci-hash string-ci<=? string-ci<? string-ci=? string-ci>=? string-ci>? string-copy
    string-downcase string-fill! string-foldcase string-for-each string-hash string-length string-normalize-nfc
    string-normalize-nfd string-normalize-nfkc string-normalize-nfkd string-ref string-set! string-titlecase
    string-upcase string<=? string<? string=? string>=? string>? string? substring symbol->string symbol-hash
    symbol=? symbol? syntax syntax->datum syntax-case syntax-rules syntax-violation syntax-violation-form
    syntax-violation-subform syntax-violation? tan textual-port? transcoded-port transcoder-codec
    transcoder-eol-style transcoder-error-handling-mode truncate u8-list->bytevector uint-list->bytevector
    undefined-violation? unless unquote unquote-splicing unsyntax unsyntax-splicing utf-16-codec utf-8-codec
    utf16->string utf32->string utf8->string values vector vector->list vector-fill! vector-for-each vector-length
    vector-map vector-ref vector-set! vector-sort vector-sort! vector? violation? warning? when who-condition?
    with-exception-handler with-input-from-file with-output-to-file with-syntax write write-char zero?
    ;; Chez's own
    fx= fx< fx> fx<= fx>= fl= fl< fl> fl<= fl>= fx1+ fx1- 1+ 1- add1 sub1 void iota list-head last-pair sort sort!
    merge list* make-list errorf warningf with-output-to-string open-input-string open-output-string
    get-output-string bytevector ash logand logor logxor lognot random nonnegative? nonpositive? gensym rec printf
    fprintf format pretty-print andmap ormap most-positive-fixnum most-negative-fixnum eq-hashtable? fxsra fxsll
    fxsrl fxlogand fxlogor fxlogxor fxlognot fxquotient fxremainder fxmodulo fxabs integer-length bignum? ratnum? box
    unbox set-box! box? append! reverse! list-copy fluid-let module import real-time cpu-time make-parameter parameterize
    atom? exact-nonnegative-integer? char- string-truncate! enumerate
    fxnonnegative? fxnonpositive? subst substq substv subst! substq! substv! remq! remv! remove! call/1cc
    exclusive-cond define-values)
  (import (rnrs) (rnrs eval) (rnrs mutable-pairs) (rnrs mutable-strings) (rnrs r5rs)
          (srfi :39 parameters)
          (only (psyntax system $all) format gensym module import real-time cpu-time))

  (define (cmp who ok?)
    (lambda (x . r)
      (if (null? r)
          (begin (ok? x x) #t)
          (apply ok? x r))))
  (define fx= (cmp 'fx= fx=?))
  (define fx< (cmp 'fx< fx<?))
  (define fx> (cmp 'fx> fx>?))
  (define fx<= (cmp 'fx<= fx<=?))
  (define fx>= (cmp 'fx>= fx>=?))
  (define fl= (cmp 'fl= fl=?))
  (define fl< (cmp 'fl< fl<?))
  (define fl> (cmp 'fl> fl>?))
  (define fl<= (cmp 'fl<= fl<=?))
  (define fl>= (cmp 'fl>= fl>=?))
  (define (fx1+ x) (fx+ x 1))
  (define (fx1- x) (fx- x 1))
  (define (1+ x) (+ x 1))
  (define (1- x) (- x 1))
  (define add1 1+)
  (define sub1 1-)
  (define (void . x) (if #f #f))
  (define iota
    (case-lambda
      ((n) (iota n 0 1))
      ((n start) (iota n start 1))
      ((n start step)
       (let loop ((i (- n 1)) (acc '()))
         (if (< i 0) acc (loop (- i 1) (cons (+ start (* i step)) acc)))))))
  (define (list-head l n)
    (unless (and (integer? n) (exact? n) (>= n 0)) (assertion-violation 'list-head "invalid index" n))
    (let loop ((l l) (n n))
      (cond ((= n 0) '())
            ((pair? l) (cons (car l) (loop (cdr l) (- n 1))))
            (else (assertion-violation 'list-head "list too short" l)))))
  (define (last-pair l)
    (unless (pair? l) (assertion-violation 'last-pair "not a pair" l))
    (let loop ((l l) (slow l) (n 0))
      (cond ((not (pair? (cdr l))) l)
            ((and (odd? n) (eq? (cdr l) slow)) (assertion-violation 'last-pair "circular list" l))
            (else (loop (cdr l) (if (odd? n) (cdr slow) slow) (+ n 1))))))
  (define (sort pred seq) (if (vector? seq) (vector-sort pred seq) (list-sort pred seq)))
  (define sort! sort)
  (define (merge pred a b)
    (cond ((null? a) b) ((null? b) a)
          ((pred (car b) (car a)) (cons (car b) (merge pred a (cdr b))))
          (else (cons (car a) (merge pred (cdr a) b)))))
  (define (list* x . r)
    (if (null? r) x (cons x (apply list* r))))
  (define make-list
    (case-lambda ((n) (make-list n (if #f #f)))
                 ((n x) (let loop ((n n) (acc '())) (if (= n 0) acc (loop (- n 1) (cons x acc)))))))
  (define (errorf who fmt . args) (error who (apply format fmt args)))
  (define (warningf who fmt . args)
    (raise-continuable (condition (make-warning) (make-who-condition who) (make-message-condition (apply format fmt args)))))
  (define (open-input-string s) (open-string-input-port s))
  (define string-ports (make-eq-hashtable))
  (define (open-output-string)
    (call-with-values open-string-output-port
      (lambda (p extract) (hashtable-set! string-ports p extract) p)))
  (define (get-output-string p) ((hashtable-ref string-ports p (lambda () (assertion-violation 'get-output-string "not a string output port" p)))))
  (define (with-output-to-string thunk)
    (let ((p (open-output-string)) (old (current-output-port)))
      (dynamic-wind
        (lambda () (current-output-port p))
        thunk
        (lambda () (current-output-port old)))
      (get-output-string p)))
  ;; Chez's bytevector takes octets as s8 or u8 values (-128 to 255)
  (define (bytevector . xs) (u8-list->bytevector (map (lambda (x) (if (and (fixnum? x) (fx<? x 0) (fx>=? x -128)) (fx+ x 256) x)) xs)))
  (define (ash x n) (bitwise-arithmetic-shift x n))
  (define logand bitwise-and)
  (define logor bitwise-ior)
  (define logxor bitwise-xor)
  (define lognot bitwise-not)
  (define random-state 88172645463325252)
  (define (random n)
    ;; xorshift64*, enough for the mats' sampling (not Chez's sequence)
    (set! random-state (bitwise-and (bitwise-xor random-state (bitwise-arithmetic-shift random-state 13)) #xFFFFFFFFFFFFFFFF))
    (set! random-state (bitwise-xor random-state (bitwise-arithmetic-shift random-state -7)))
    (set! random-state (bitwise-and (bitwise-xor random-state (bitwise-arithmetic-shift random-state 17)) #xFFFFFFFFFFFFFFFF))
    (cond ((and (integer? n) (exact? n) (> n 0)) (mod random-state n))
          ((and (real? n) (inexact? n) (> n 0)) (* n (/ (inexact random-state) 18446744073709551616.0)))
          (else (assertion-violation 'random "invalid range" n))))
  (define (nonnegative? x) (>= x 0))
  (define (nonpositive? x) (<= x 0))
  (define-syntax rec
    (syntax-rules () ((_ name e) (letrec ((name e)) name))))
  (define-syntax fluid-let
    (syntax-rules ()
      ((_ () b1 b2 ...) (let () b1 b2 ...))
      ((_ ((x e) ...) b1 b2 ...)
       (let ((new (list e ...)))
         (define (swap!)
           (let ((old (list x ...)))
             (let loop ((vs new) (refs (list (lambda (v) (set! x v)) ...)))
               (unless (null? vs) ((car refs) (car vs)) (loop (cdr vs) (cdr refs))))
             (set! new old)))
         (dynamic-wind swap! (lambda () b1 b2 ...) swap!)))))
  (define (printf fmt . args) (display (apply format fmt args)))
  (define (fprintf port fmt . args) (display (apply format fmt args) port))
  (define pretty-print (case-lambda ((x) (write x) (newline)) ((x p) (write x p) (newline p))))
  (define (andmap f l . ls)
    (let loop ((ls (cons l ls)) (last #t))
      (if (exists null? ls) last
          (let ((v (apply f (map car ls)))) (and v (loop (map cdr ls) v))))))
  (define (ormap f l . ls)
    (let loop ((ls (cons l ls)))
      (if (exists null? ls) #f (or (apply f (map car ls)) (loop (map cdr ls))))))
  (define (most-positive-fixnum) (greatest-fixnum))
  (define (most-negative-fixnum) (least-fixnum))
  (define (eq-hashtable? h) (and (hashtable? h) (eq? (hashtable-equivalence-function h) eq?)))
  (define (fxsra x n) (fxarithmetic-shift-right x n))
  (define (fxsll x n) (fxarithmetic-shift-left x n))
  (define (fxsrl x n) (if (fx>=? x 0) (fxarithmetic-shift-right x n)
                          (bitwise-arithmetic-shift-right (+ x (expt 2 (fixnum-width))) n)))
  (define fxlogand fxand)
  (define fxlogor fxior)
  (define fxlogxor fxxor)
  (define fxlognot fxnot)
  (define (fxquotient x y) (fxquotient* x y))
  (define (fxquotient* x y) (let ((q (quotient x y))) (if (fixnum? q) q (assertion-violation 'fxquotient "result is not a fixnum" q))))
  (define (fxremainder x y) (fx- x (fx* y (fxquotient x y))))
  (define (fxmodulo x y) (modulo x y))
  (define (fxabs x) (if (fx<? x 0) (fx- 0 x) x))
  (define (integer-length x) (bitwise-length x))
  (define (bignum? x) (and (integer? x) (exact? x) (not (fixnum? x))))
  (define (ratnum? x) (and (rational? x) (exact? x) (not (integer? x))))
  (define-record-type (box-type box box?) (fields (mutable v unbox set-box!)))
  (define (append! . ls) (apply append ls))
  (define (reverse! l) (reverse l))
  (define (list-copy l) (if (pair? l) (cons (car l) (list-copy (cdr l))) l))
  (define (atom? x) (not (pair? x)))
  (define (exact-nonnegative-integer? x) (and (integer? x) (exact? x) (>= x 0)))
  (define (char- a b) (- (char->integer a) (char->integer b)))
  (define (string-truncate! s n)
    (unless (<= 0 n (string-length s)) (assertion-violation 'string-truncate! "invalid length" n))
    (substring s 0 n))
  (define (enumerate l) (iota (length l)))
  (define (fxnonnegative? x) (fx>=? x 0))
  (define (fxnonpositive? x) (fx<=? x 0))
  (define (subst-by same? new old tree)
    (let f ((t tree))
      (cond ((same? t old) new)
            ((pair? t) (cons (f (car t)) (f (cdr t))))
            (else t))))
  (define (subst new old tree) (subst-by equal? new old tree))
  (define (substq new old tree) (subst-by eq? new old tree))
  (define (substv new old tree) (subst-by eqv? new old tree))
  (define subst! subst)
  (define substq! substq)
  (define substv! substv)
  (define remq! remq)
  (define remv! remv)
  (define remove! remove)
  (define call/1cc call/cc)
  (define-syntax exclusive-cond (syntax-rules () ((_ c ...) (cond c ...))))
  (define-syntax define-values
    (syntax-rules ()
      ((_ () e) (define dummy (call-with-values (lambda () e) (lambda args #f))))
      ((_ (v ...) e) (define-values-helper (v ...) () e))))
  (define-syntax define-values-helper
    (syntax-rules ()
      ((_ () ((v t) ...) e)
       (begin (define v #f) ... (call-with-values (lambda () e) (lambda (t ...) (set! v t) ...))))
      ((_ (v1 v2 ...) (b ...) e) (define-values-helper (v2 ...) (b ... (v1 tmp)) e)))))
