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
;;;; Read in Chez's lexical mode (#!chezscheme: si's R7RS mode, which takes 1+ and #%name). format is (si format) (SC122 (b)). Not provided: define-record, define-ftype and the foreign interface,
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
    exclusive-cond define-values interaction-environment transcoder?
    ;; SC180 (b): Chez's port procedures (go/arbace/scm/prim_io.ae)
    unget-u8 unget-char port-has-port-length? port-has-set-port-length!? port-has-port-nonblocking??
    port-has-set-port-nonblocking!? fresh-line port-length set-port-length! port-nonblocking? set-port-nonblocking!
    ;; SMR6: the identifiers the stub experiment found (scheme/lib/chezscheme.sls, doc/scheme/MATURITY-REVIEW.md)
    machine-type threaded? -1+ logior logtest logbit? logbit1 logbit0
    isqrt flonum->fixnum fx+/wraparound fx-/wraparound fx*/wraparound fxsll/wraparound fxlogior fxlogtest
    fxlogbit? fxlogbit1 fxlogbit0 flnonpositive? flnonnegative? conjugate magnitude-squared sinh
    cosh tanh asinh acosh atanh hashtable-values hashtable-cells $hashtable-cells weak-cons ephemeron-cons weak-pair? ephemeron-pair? fxpopcount fxpopcount16 fxpopcount32
    make-weak-eq-hashtable
    make-weak-eqv-hashtable make-ephemeron-eq-hashtable make-ephemeron-eqv-hashtable make-weak-hashtable make-ephemeron-hashtable hashtable-weak? hashtable-ephemeron? eq-hashtable-ref
    eq-hashtable-set! eq-hashtable-contains? eq-hashtable-delete! eq-hashtable-update! eq-hashtable-weak? eq-hashtable-ephemeron? putprop getprop
    remprop merge! immutable-string? mutable-string? immutable-vector? mutable-vector? immutable-bytevector? mutable-bytevector?
    string->immutable-string vector->immutable-vector bytevector->immutable-bytevector string-append-immutable immutable-vector immutable-vector-copy vector-copy vector-append
    immutable-vector-append vector-set/copy immutable-vector-set/copy vector-set-fixnum! string-copy! substring-fill! bytevector->s8-list s8-list->bytevector
    bytevector-s24-ref bytevector-u24-ref bytevector-s40-ref bytevector-u40-ref bytevector-s48-ref bytevector-u48-ref bytevector-s56-ref bytevector-u56-ref
    bytevector-s24-set! bytevector-u24-set! bytevector-s40-set! bytevector-u40-set! bytevector-s48-set! bytevector-u48-set! bytevector-s56-set! bytevector-u56-set!
    fxvector flvector make-fxvector make-flvector fxvector? flvector? list->fxvector list->flvector
    fxvector-length flvector-length fxvector-ref flvector-ref fxvector-set! flvector-set! fxvector->list flvector->list
    fxvector-copy flvector-copy fxvector-fill! flvector-fill! top-level-bound? top-level-value set-top-level-value! define-top-level-value
    scheme-environment load console-output-port console-input-port console-error-port trace-output-port with-input-from-string char-ready?
    directory-separator directory-separator? timer-interrupt-handler keyboard-interrupt-handler collect collect-request-handler collect-maximum-generation collect-trip-bytes
    collect-generation-radix collect-notify bytes-allocated disable-interrupts enable-interrupts optimize-level debug-level enable-unsafe-application
    enable-unsafe-variable-reference enable-type-recovery print-radix print-gensym print-length print-level print-brackets
    print-vector-length make-record-type
    ;; SMR7: Chez's records (si chez-records), the cflonum operations, the compiler's parameters
    define-record type-descriptor csv7:record-field-accessor csv7:record-field-mutator csv7:record-field-mutable?
    csv7:record-type-field-names csv7:record-type-field-decls csv7:record-field-accessible? csv7:record-type-descriptor csv7:record-type-name csv7:record-type-symbol $record-type-descriptor $record-type-field-names
    cflonum? cfl+ cfl- cfl* cfl/ cfl= cfl-real-part cfl-imag-part cfl-conjugate cfl-magnitude-squared fl-make-rectangular gensym?
    enable-cp0 run-cp0 $suppress-primitive-inlining internal-defines-as-letrec* generate-inspector-information generate-procedure-source-information
    generate-interrupt-trap $enable-check-prelex-flags cp0-effort-limit cp0-score-limit cp0-outer-unroll-limit undefined-variable-warnings
    commonization-level enable-arithmetic-left-associative enable-error-source-expression compile-interpret-simple
    car-cas! cdr-cas! vector-cas! list-assuming-immutable? vector-copy! fxvector-copy! flvector-copy! $quotient-remainder subset-mode expt-mod
    with-interrupts-disabled critical-section $fxu< flsingle flbit-field random-seed current-eval case-sensitive gensym-count gensym-prefix
    pretty-line-length pretty-one-line-limit pretty-initial-indent pretty-standard-indent pretty-maximum-lines eval-syntax-expanders-when
    collect-maximum-generation-threshold-factor file-position file-length clear-input-port clear-output-port
    datum syntax->list syntax->vector $string-ref-check? $vector-ref-check? $fxvector-ref-check? $flvector-ref-check? $bytevector-ref-check?
    current-directory source-directories library-directories command-line-arguments reset-handler abort-handler import-notify)
  ;; SC170 (b): Chez's wider forms of eval (the environment optional), dynamic-wind (a leading critical flag), fx+ fx* fx-
  ;; (any number of arguments) and the file procedures' options ('replace, 'truncate) replace (rnrs)'s
  (import (except (rnrs) record? make-record-type-descriptor = < > <= >= char=? char<? char>? char<=? char>=? char-ci=? char-ci<? char-ci>? char-ci<=? char-ci>=? string=? string<? string>? string<=? string>=? string-ci=? string-ci<? string-ci>? string-ci<=? string-ci>=? file-options fx+ fx* fx- for-each dynamic-wind open-output-file call-with-output-file with-output-to-file
                  string->number number->string record-constructor hashtable-keys hashtable-entries)
          (rename (only (rnrs) string->number number->string record-constructor hashtable-keys hashtable-entries) (string->number r6-string->number) (number->string r6-number->string)
                  (record-constructor r6-record-constructor) (hashtable-keys r6-hashtable-keys) (hashtable-entries r6-hashtable-entries))
          (rename (only (rnrs) fx+ fx* fx- for-each dynamic-wind open-output-file call-with-output-file with-output-to-file)
                  (fx+ fx2+) (fx* fx2*) (fx- fx2-) (dynamic-wind r6-dynamic-wind) (for-each r6-for-each) (open-output-file r6-open-output-file)
                  (call-with-output-file r6-call-with-output-file) (with-output-to-file r6-with-output-to-file))
          (prefix (only (rnrs) = < > <= >= char=? char<? char>? char<=? char>=? char-ci=? char-ci<? char-ci>? char-ci<=? char-ci>=? string=? string<? string>? string<=? string>=? string-ci=? string-ci<? string-ci>? string-ci<=? string-ci>=?) r6:)
          (except (rnrs eval) eval) (rename (only (rnrs eval) eval) (eval r6-eval))
          (rnrs mutable-pairs) (rnrs mutable-strings) (rnrs r5rs)
          (srfi :39 parameters)
          (only (psyntax system $all) gensym module import real-time cpu-time make-file-options
                unget-u8 unget-char port-has-port-length? port-has-set-port-length!? port-has-port-nonblocking??
                port-has-set-port-nonblocking!? fresh-line port-length set-port-length! port-nonblocking? set-port-nonblocking!)
          (si format) (si chez-records) (only (srfi :98) get-environment-variable))

  ;; SMR7: Chez's comparisons take one argument too (its type checked, the answer #t), as the mats ask (mats-arity-one); two or more: (rnrs)'s
  (define-syntax define-comparison
    (syntax-rules ()
      ((_ name r6) (define name (case-lambda ((x) (r6 x x) #t) ((x y) (r6 x y)) ((x y . r) (apply r6 x y r)))))))
  (define-comparison = r6:=)
  (define-comparison < r6:<)
  (define-comparison > r6:>)
  (define-comparison <= r6:<=)
  (define-comparison >= r6:>=)
  (define-comparison char=? r6:char=?)
  (define-comparison char<? r6:char<?)
  (define-comparison char>? r6:char>?)
  (define-comparison char<=? r6:char<=?)
  (define-comparison char>=? r6:char>=?)
  (define-comparison char-ci=? r6:char-ci=?)
  (define-comparison char-ci<? r6:char-ci<?)
  (define-comparison char-ci>? r6:char-ci>?)
  (define-comparison char-ci<=? r6:char-ci<=?)
  (define-comparison char-ci>=? r6:char-ci>=?)
  (define-comparison string=? r6:string=?)
  (define-comparison string<? r6:string<?)
  (define-comparison string>? r6:string>?)
  (define-comparison string<=? r6:string<=?)
  (define-comparison string>=? r6:string>=?)
  (define-comparison string-ci=? r6:string-ci=?)
  (define-comparison string-ci<? r6:string-ci<?)
  (define-comparison string-ci>? r6:string-ci>?)
  (define-comparison string-ci<=? r6:string-ci<=?)
  (define-comparison string-ci>=? r6:string-ci>=?)
;; SC170 (b): Chez's file-options names more than R6RS's three (replace, append, compressed, exclusive, ...): the universe is si's
  (define-syntax file-options
    (lambda (x)
      (syntax-case x ()
        ((_ opt ...) (for-all identifier? #'(opt ...)) #'(make-file-options '(opt ...))))))
;; SMR4: Chez's own string->number and number->string (radix 2 to 36, #Nr, # placeholders, a point and an exponent in any radix, a precision
  ;; with any radix) are (rnrs)'s primitives with a mark in the last place the (rnrs) ones do not take
  (define string->number (case-lambda ((s) (r6-string->number s 10 #t)) ((s r) (r6-string->number s r #t))))
  (define number->string
    (case-lambda ((x) (r6-number->string x 10 #f #t)) ((x r) (r6-number->string x r #f #t)) ((x r p) (r6-number->string x r p #t))))
  (define (transcoder? x) (guard (e (#t #f)) (transcoder-codec x) #t))
  (define ienv #f)
  (define interaction-environment
    (case-lambda
      ((e) (set! ienv e))
      (() (or ienv (begin (set! ienv (environment '(chezscheme))) ienv)))))
  (define eval
    (case-lambda
      ((x) (r6-eval x (interaction-environment)))
      ((x e) (r6-eval x e))))
  (define dynamic-wind
    (case-lambda
      ((before thunk after) (r6-dynamic-wind before thunk after))
      ((critical? before thunk after) (r6-dynamic-wind before thunk after))))
  (define fx+ (case-lambda (() 0) ((x) (fx2+ x 0)) ((x y) (fx2+ x y)) ((x y . r) (apply fx+ (fx2+ x y) r))))
  (define fx* (case-lambda (() 1) ((x) (fx2* x 1)) ((x y) (fx2* x y)) ((x y . r) (apply fx* (fx2* x y) r))))
  (define fx- (case-lambda ((x) (fx2- x)) ((x y) (fx2- x y)) ((x y . r) (apply fx- (fx2- x y) r))))
  ;; Chez's for-each returns what its last call returns (the call is a tail call; none: void), which a mat reads
  (define for-each
    (case-lambda
      ((p l) (if (and (pair? l) (list? l))
                 (let loop ((l l) (n (length l)))
                   (unless (pair? l) (assertion-violation 'for-each "input list was altered during operation"))
                   (if (= n 1) (p (car l)) (begin (p (car l)) (loop (cdr l) (- n 1)))))
                 (r6-for-each p l)))
      ((p l1 l2) (if (and (pair? l1) (pair? l2) (list? l1) (list? l2) (= (length l1) (length l2)))
                     (let loop ((l1 l1) (l2 l2) (n (length l1)))
                       (unless (and (pair? l1) (pair? l2)) (assertion-violation 'for-each "input list was altered during operation"))
                       (if (= n 1) (p (car l1) (car l2)) (begin (p (car l1) (car l2)) (loop (cdr l1) (cdr l2) (- n 1)))))
                     (r6-for-each p l1 l2)))
      ((p l . ls) (apply r6-for-each p l ls))))
  (define (replace-existing! who f opts)
    (let ((opts (apply append (map (lambda (o) (if (list? o) o (list o))) opts))))
      (when (and (pair? opts) (or (memq 'replace opts) (memq 'truncate opts)) (file-exists? f)) (delete-file f))))
  (define open-output-file
    (case-lambda
      ((f) (r6-open-output-file f))
      ((f . opts) (replace-existing! 'open-output-file f opts) (r6-open-output-file f))))
  (define call-with-output-file
    (case-lambda
      ((f proc) (r6-call-with-output-file f proc))
      ((f proc . opts) (replace-existing! 'call-with-output-file f opts) (r6-call-with-output-file f proc))))
  (define with-output-to-file
    (case-lambda
      ((f thunk) (r6-with-output-to-file f thunk))
      ((f thunk . opts) (replace-existing! 'with-output-to-file f opts) (r6-with-output-to-file f thunk))))

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
  ;; SMR2: Chez's iota takes the count only, a nonnegative fixnum
  (define (iota n)
    (unless (and (fixnum? n) (>= n 0)) (assertion-violation 'iota "~s is not a nonnegative fixnum" n))
    (let loop ((i (- n 1)) (acc '()))
      (if (< i 0) acc (loop (- i 1) (cons i acc)))))
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
  ;; SC134 (b): Chez's sort and sort! take a list only (a vector is an error: vector-sort), and merge checks its lists
  ;; (length raises on an improper or a circular one, as Chez's merge does)
  (define (sort pred seq) (list-sort pred seq))
  (define sort! sort)
  (define (merge pred a b)
    (length a) (length b)
    (merge-lists pred a b))
  (define (merge-lists pred a b)
    (cond ((null? a) b) ((null? b) a)
          ((pred (car b) (car a)) (cons (car b) (merge-lists pred a (cdr b))))
          (else (cons (car a) (merge-lists pred (cdr a) b)))))
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
  ;; SMR7: the generator's state is Chez's 32-bit seed (random-seed reads and sets it, 1 to 2^32-1); each draw is the splitmix64 of the
  ;; seed, the seed then stepped by a 32-bit linear congruence (not Chez's sequence: the mats check ranges and that a seed repeats a draw)
  (define random-state 1)
  (define (random-draw)
    (let* ((z (bitwise-and (+ (* random-state #x9E3779B97F4A7C15) #x632BE59BD9B4E019) #xFFFFFFFFFFFFFFFF))
           (z (bitwise-and (* (bitwise-xor z (bitwise-arithmetic-shift-right z 30)) #xBF58476D1CE4E5B9) #xFFFFFFFFFFFFFFFF))
           (z (bitwise-and (* (bitwise-xor z (bitwise-arithmetic-shift-right z 27)) #x94D049BB133111EB) #xFFFFFFFFFFFFFFFF))
           (z (bitwise-xor z (bitwise-arithmetic-shift-right z 31))))
      (set! random-state (let ((s (mod (+ (* random-state 1664525) 1013904223) #x100000000))) (if (= s 0) 1 s)))
      z))
  (define (random n)
    (cond ((and (integer? n) (exact? n) (> n 0))
           ;; enough 64-bit draws to cover n, then reduced
           (let loop ((acc 0) (range 1))
             (if (>= range (* n #x10000)) (mod acc n) (loop (+ (* acc #x10000000000000000) (random-draw)) (* range #x10000000000000000)))))
          ((and (real? n) (inexact? n) (> n 0)) (* n (/ (inexact (random-draw)) 18446744073709551616.0)))
          (else (assertion-violation 'random "invalid range" n))))
  (define random-seed
    (case-lambda
      (() random-state)
      ((s) (unless (and (integer? s) (exact? s) (<= 1 s #xFFFFFFFF)) (assertion-violation 'random-seed "invalid seed" s))
           (set! random-state s))))
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
  (define pretty-print (case-lambda ((x) (write x) (newline)) ((x p) (write x p) (newline p))))
  ;; SMR2: Chez's andmap and ormap are for-all and exists (the machine's, with the list checks)
  (define andmap for-all)
  (define ormap exists)
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
  (define fxquotient (case-lambda ((x) (fxquotient* 1 x)) ((x y) (fxquotient* x y)) ((x y . r) (apply fxquotient (fxquotient* x y) r))))
  (define (fxquotient* x y) (let ((q (begin (unless (and (fixnum? x) (fixnum? y)) (assertion-violation 'fxquotient "not a fixnum" x y)) (quotient x y)))) (if (fixnum? q) q (assertion-violation 'fxquotient "result is not a fixnum" q))))
  (define (fxremainder x y) (fx- x (fx* y (fxquotient x y))))
  (define (fxmodulo x y) (modulo x y))
  (define (fxabs x) (if (fx<? x 0) (fx- 0 x) x))
  (define (integer-length x) (bitwise-length x))
  (define (bignum? x) (and (integer? x) (exact? x) (not (fixnum? x))))
  (define (ratnum? x) (and (rational? x) (exact? x) (not (integer? x))))
  (define-record-type (box-type box box?) (fields (mutable v unbox set-box!)))
  ;; append! links the last pair of each non-empty list to the next list (Chez's, destructive: SC170 (b))
  (define (append! . ls)
    (let loop ((ls ls))
      (cond ((null? ls) '())
            ((null? (cdr ls)) (car ls))
            ((not (list? (car ls))) (assertion-violation 'append! "not a proper list" (car ls)))
            (else
             (let ((rest (loop (cdr ls))))
               (if (null? (car ls))
                   rest
                   (begin (set-cdr! (last-pair (car ls)) rest) (car ls))))))))
  ;; SMR7: reverse! reverses the list's own pairs (Chez's: the result is made of the argument's pairs)
  (define (reverse! l)
    (unless (list? l) (assertion-violation 'reverse! "not a proper list" l))
    (let loop ((l l) (acc '()))
      (if (null? l) acc (let ((next (cdr l))) (set-cdr! l acc) (loop next l)))))
  ;; SMR2: append checks its first list as Chez's list-copy does (improper and circular ones raise)
  (define (list-copy l) (append l '()))
  (define (atom? x) (not (pair? x)))
  (define (exact-nonnegative-integer? x) (and (integer? x) (exact? x) (>= x 0)))
  (define (char- a b) (- (char->integer a) (char->integer b)))
  (define (string-truncate! s n)
    (unless (<= 0 n (string-length s)) (assertion-violation 'string-truncate! "invalid length" n))
    (substring s 0 n))
  (define (enumerate l) (iota (length l)))
  (define (fxnonnegative? x) (fx>=? x 0))
  (define (fxnonpositive? x) (fx<=? x 0))
  ;; SMR7: subst shares what it does not change (an unchanged tree is returned itself); subst!, substq!, substv! change the tree's
  ;; own pairs; remq!, remv!, remove! splice the list's own pairs (Chez's: the result is the first pair kept); written from 5_2.ms's mats
  (define (subst-by same? new old tree)
    (let f ((t tree))
      (cond ((same? t old) new)
            ((pair? t) (let ((a (f (car t))) (d (f (cdr t)))) (if (and (eq? a (car t)) (eq? d (cdr t))) t (cons a d))))
            (else t))))
  (define (subst new old tree) (subst-by equal? new old tree))
  (define (substq new old tree) (subst-by eq? new old tree))
  (define (substv new old tree) (subst-by eqv? new old tree))
  (define (subst!-by same? new old tree)
    (let f ((t tree))
      (cond ((same? t old) new)
            ((pair? t) (set-car! t (f (car t))) (set-cdr! t (f (cdr t))) t)
            (else t))))
  (define (subst! new old tree) (subst!-by equal? new old tree))
  (define (substq! new old tree) (subst!-by eq? new old tree))
  (define (substv! new old tree) (subst!-by eqv? new old tree))
  (define (rem!-by who same? x l)
    (unless (list? l) (assertion-violation who "not a proper list" l))
    (let skip ((l l))
      (cond ((null? l) '())
            ((same? (car l) x) (skip (cdr l)))
            (else (let loop ((prev l))
                    (let ((next (cdr prev)))
                      (cond ((null? next) l)
                            ((same? (car next) x) (set-cdr! prev (cdr next)) (loop prev))
                            (else (loop next)))))))))
  (define (remq! x l) (rem!-by 'remq! eq? x l))
  (define (remv! x l) (rem!-by 'remv! eqv? x l))
  (define (remove! x l) (rem!-by 'remove! equal? x l))
  ;; ---- SMR6: Chez's simple identifiers the stub experiment found (doc/scheme/MATURITY-REVIEW.md, "As built: SMR6"); all in Scheme over
  ;; si's primitives (no new primitive, psyntax's boot list untouched), probed against Chez 10.3.0
  (define (machine-type) 'a6le)   ; Chez's name of the machine without threads: si has none
  (define (threaded?) #f)
  (define (-1+ x) (- x 1))
  (define logior bitwise-ior)
  (define (logtest a b) (not (zero? (bitwise-and a b))))
  (define (logbit? i n) (bitwise-bit-set? n i))
  (define (logbit1 i n) (bitwise-ior n (bitwise-arithmetic-shift-left 1 i)))
  (define (logbit0 i n) (bitwise-and n (bitwise-not (bitwise-arithmetic-shift-left 1 i))))
  (define (isqrt n)
    (cond ((and (integer? n) (exact? n) (>= n 0)) (call-with-values (lambda () (exact-integer-sqrt n)) (lambda (s r) s)))
          ((and (flonum? n) (fl>=? n 0.0) (flinteger? n) (not (infinite? n))) (inexact (isqrt (exact n))))
          (else (assertion-violation 'isqrt "not a nonnegative integer" n))))
  (define (flonum->fixnum x)
    (unless (flonum? x) (assertion-violation 'flonum->fixnum "not a flonum" x))
    (let ((n (and (not (nan? x)) (not (infinite? x)) (exact (truncate x)))))
      (if (and n (fixnum? n)) n (assertion-violation 'flonum->fixnum "result would be outside of fixnum range" x))))
  (define (wrap61 n) (- (mod (+ n 1152921504606846976) 2305843009213693952) 1152921504606846976))
  (define (fx-check who . xs) (for-each (lambda (x) (unless (fixnum? x) (assertion-violation who "not a fixnum" x))) xs))
  (define (fx+/wraparound x y) (fx-check 'fx+/wraparound x y) (wrap61 (+ x y)))
  (define fx-/wraparound
    (case-lambda ((x) (fx-check 'fx-/wraparound x) (wrap61 (- x))) ((x y) (fx-check 'fx-/wraparound x y) (wrap61 (- x y)))))
  (define (fx*/wraparound x y) (fx-check 'fx*/wraparound x y) (wrap61 (* x y)))
  (define (fxsll/wraparound x n)
    (fx-check 'fxsll/wraparound x n)
    (unless (and (fx>=? n 0) (fx<? n 61)) (assertion-violation 'fxsll/wraparound "invalid shift count" n))
    (wrap61 (bitwise-arithmetic-shift-left x n)))
  (define fxlogior fxior)
  (define (fxlogtest a b) (not (fxzero? (fxand a b))))
  (define (fxlogbit? i n) (fxbit-set? n i))
  (define (fxlogbit1 i n) (fxior n (fxarithmetic-shift-left 1 i)))
  (define (fxlogbit0 i n) (fxand n (fxnot (fxarithmetic-shift-left 1 i))))
  (define (flnonpositive? x) (fl<=? x 0.0))
  (define (flnonnegative? x) (fl>=? x 0.0))
  (define (conjugate z) (make-rectangular (real-part z) (- (imag-part z))))
  (define (magnitude-squared z) (+ (* (real-part z) (real-part z)) (* (imag-part z) (imag-part z))))
  ;; the hyperbolic functions: a flonum for an exact argument, a signed zero and the infinities kept, a complex answer outside the real range
  (define (hyp-check who x) (unless (number? x) (assertion-violation who "not a number" x)))
  (define (hyp-special? x) (and (flonum? x) (or (nan? x) (infinite? x) (fl=? x 0.0))))
  (define (zero-complex? z) (and (not (real? z)) (flonum? (real-part z)) (fl=? (real-part z) 0.0) (flonum? (imag-part z)) (fl=? (imag-part z) 0.0)))
  (define (sinh x)
    (hyp-check 'sinh x)
    (cond ((eqv? x 0) 0)
          ((real? x)
           (let ((x (inexact x)))
             (cond ((hyp-special? x) x)
                   ((fl<? (flabs x) 1e-5) (fl+ x (fl/ (fl* x (fl* x x)) 6.0)))
                   ((fl>? (flabs x) 20.0) (let ((v (flexp (fl- (flabs x) 0.6931471805599453)))) (if (fl<? x 0.0) (fl- 0.0 v) v)))
                   (else (let ((e (flexp x))) (fl/ (fl- e (fl/ 1.0 e)) 2.0))))))
          ((zero-complex? x) x)
          (else (/ (- (exp x) (exp (- x))) 2))))
  (define (cosh x)
    (hyp-check 'cosh x)
    (cond ((eqv? x 0) 1)
          ((real? x)
           (let ((x (inexact x)))
             (cond ((nan? x) x)
                   ((infinite? x) +inf.0)
                   ((fl>? (flabs x) 20.0) (flexp (fl- (flabs x) 0.6931471805599453)))
                   (else (let ((e (flexp x))) (fl/ (fl+ e (fl/ 1.0 e)) 2.0))))))
          (else (/ (+ (exp x) (exp (- x))) 2))))
  (define (tanh x)
    (hyp-check 'tanh x)
    (cond ((eqv? x 0) 0)
          ((real? x)
           (let ((x (inexact x)))
             (cond ((nan? x) x)
                   ((fl=? x 0.0) x)
                   ((fl>? x 20.0) 1.0)
                   ((fl<? x -20.0) -1.0)
                   ((fl<? (flabs x) 1e-5) (fl- x (fl/ (fl* x (fl* x x)) 3.0)))
                   (else (let ((e (flexp (fl* 2.0 x)))) (fl/ (fl- e 1.0) (fl+ e 1.0)))))))
          ((zero-complex? x) x)
          (else (let* ((re (inexact (real-part x))) (im (inexact (imag-part x))))
                  (if (fl>? (flabs re) 20.0)
                      (make-rectangular (if (fl<? re 0.0) -1.0 1.0) (fl* (fl* 4.0 (flsin (fl* 2.0 im))) (flexp (fl- 0.0 (fl* 2.0 (flabs re))))))
                      (let ((d (fl+ (fl/ (fl+ (flexp (fl* 2.0 re)) (flexp (fl* -2.0 re))) 2.0) (flcos (fl* 2.0 im)))))
                        (make-rectangular (fl/ (sinh (fl* 2.0 re)) d) (fl/ (flsin (fl* 2.0 im)) d))))))))
  (define (asinh x)
    (hyp-check 'asinh x)
    (cond ((eqv? x 0) 0)
          ((real? x)
           (let ((x (inexact x)))
             (cond ((hyp-special? x) x)
                   ((fl<? (flabs x) 1e-5) (fl- x (fl/ (fl* x (fl* x x)) 6.0)))
                   ((fl>? (flabs x) 1e150) (let ((v (fl+ (fllog (flabs x)) 0.6931471805599453))) (if (fl<? x 0.0) (fl- 0.0 v) v)))
                   (else (let ((v (fllog (fl+ (flabs x) (flsqrt (fl+ (fl* x x) 1.0)))))) (if (fl<? x 0.0) (fl- 0.0 v) v))))))
          ((zero-complex? x) x)
          (else (log (+ x (sqrt (+ (* x x) 1)))))))
  (define (acosh x)
    (hyp-check 'acosh x)
    (cond ((eqv? x 1) 0)
          ((and (real? x) (not (nan? x)) (>= x 1))
           (let ((x (inexact x)))
             (cond ((infinite? x) x)
                   ((fl>? x 1e150) (fl+ (fllog x) 0.6931471805599453))
                   (else (fllog (fl+ x (flsqrt (fl- (fl* x x) 1.0))))))))
          ((and (real? x) (nan? x)) x)
          ((and (real? x) (< -1 x 1)) (make-rectangular 0.0 (acos (inexact x))))
          ((and (real? x) (<= x -1)) (let ((x (inexact x))) (make-rectangular (fllog (fl+ (fl- 0.0 x) (flsqrt (fl- (fl* x x) 1.0)))) 3.141592653589793)))
          (else (log (+ x (* (sqrt (+ x 1)) (sqrt (- x 1))))))))
  (define (atanh x)
    (hyp-check 'atanh x)
    (cond ((eqv? x 0) 0)
          ((or (eqv? x 1) (eqv? x -1)) (assertion-violation 'atanh "undefined for" x))
          ((real? x)
           (let ((x (inexact x)))
             (cond ((nan? x) x)
                   ((fl=? x 0.0) x)
                   ((fl=? x 1.0) +inf.0)
                   ((fl=? x -1.0) -inf.0)
                   ((fl<? (flabs x) 1e-5) (fl+ x (fl/ (fl* x (fl* x x)) 3.0)))
                   ((fl<? (flabs x) 1.0) (fl* 0.5 (fllog (fl/ (fl+ 1.0 x) (fl- 1.0 x)))))
                   ((fl>? x 1.0) (make-rectangular (fl* 0.5 (fllog (fl/ (fl+ x 1.0) (fl- x 1.0)))) 1.5707963267948966))
                   (else (make-rectangular (fl* -0.5 (fllog (fl/ (fl- 1.0 x) (fl- -1.0 x)))) 1.5707963267948966)))))
          ((zero-complex? x) x)
          (else (* 1/2 (- (log (+ 1 x)) (log (- 1 x)))))))
  ;; hashtables beyond R6RS's: the values and cells, the weak and ephemeron kinds (strong tables, marked), the eq-hashtable names
  ;; Chez's hashtable-keys, -entries, -values and -cells take an optional size (at most that many entries)
  (define (size-arg who n)
    (unless (and (integer? n) (exact? n) (>= n 0)) (assertion-violation who "invalid size" n))
    n)
  (define (take-vector v n) (if (< n (vector-length v)) (vector-copy v 0 n) v))
  (define hashtable-keys
    (case-lambda ((h) (r6-hashtable-keys h)) ((h n) (take-vector (r6-hashtable-keys h) (size-arg 'hashtable-keys n)))))
  (define hashtable-entries
    (case-lambda
      ((h) (r6-hashtable-entries h))
      ((h n) (let ((n (size-arg 'hashtable-entries n)))
               (call-with-values (lambda () (r6-hashtable-entries h)) (lambda (ks vs) (values (take-vector ks n) (take-vector vs n))))))))
  (define hashtable-values
    (case-lambda
      ((h) (call-with-values (lambda () (r6-hashtable-entries h)) (lambda (ks vs) vs)))
      ((h n) (take-vector (hashtable-values h) (size-arg 'hashtable-values n)))))
  ;; the cells are fresh pairs (a mutation of one does not reach the table)
  (define hashtable-cells
    (case-lambda
      ((h) (call-with-values (lambda () (r6-hashtable-entries h)) (lambda (ks vs) (vector-map cons ks vs))))
      ((h n) (take-vector (hashtable-cells h) (size-arg 'hashtable-cells n)))))
  (define $hashtable-cells hashtable-cells)
  (define table-kinds (make-eq-hashtable))
  (define (mark-table! h kind) (hashtable-set! table-kinds h kind) h)
  (define (make-weak-eq-hashtable . n) (mark-table! (make-eq-hashtable) 'weak))
  (define (make-weak-eqv-hashtable . n) (mark-table! (make-eqv-hashtable) 'weak))
  (define (make-ephemeron-eq-hashtable . n) (mark-table! (make-eq-hashtable) 'ephemeron))
  (define (make-ephemeron-eqv-hashtable . n) (mark-table! (make-eqv-hashtable) 'ephemeron))
  (define (make-weak-hashtable hash eqv . n) (mark-table! (make-hashtable hash eqv) 'weak))
  (define (make-ephemeron-hashtable hash eqv . n) (mark-table! (make-hashtable hash eqv) 'ephemeron))
  (define (hashtable-weak? h) (unless (hashtable? h) (assertion-violation 'hashtable-weak? "not a hashtable" h)) (eq? (hashtable-ref table-kinds h #f) 'weak))
  (define (hashtable-ephemeron? h) (unless (hashtable? h) (assertion-violation 'hashtable-ephemeron? "not a hashtable" h)) (eq? (hashtable-ref table-kinds h #f) 'ephemeron))
  ;; weak and ephemeron pairs are pairs that are marked (nothing is collected here)
  (define pair-kinds (make-eq-hashtable))
  (define (weak-cons a d) (let ((p (cons a d))) (hashtable-set! pair-kinds p 'weak) p))
  (define (ephemeron-cons a d) (let ((p (cons a d))) (hashtable-set! pair-kinds p 'ephemeron) p))
  (define (weak-pair? x) (and (pair? x) (eq? (hashtable-ref pair-kinds x #f) 'weak)))
  (define (ephemeron-pair? x) (and (pair? x) (eq? (hashtable-ref pair-kinds x #f) 'ephemeron)))
  (define (fxpopcount x)
    (unless (and (fixnum? x) (fx>=? x 0)) (assertion-violation 'fxpopcount "not a nonnegative fixnum" x))
    (fxbit-count x))
  (define (fxpopcount16 x)
    (unless (and (fixnum? x) (fx>=? x 0) (fx<=? x #xFFFF)) (assertion-violation 'fxpopcount16 "not a 16-bit fixnum" x))
    (fxbit-count x))
  (define (fxpopcount32 x)
    (unless (and (fixnum? x) (fx>=? x 0) (fx<=? x #xFFFFFFFF)) (assertion-violation 'fxpopcount32 "not a 32-bit fixnum" x))
    (fxbit-count x))
  (define eq-hashtable-ref hashtable-ref)
  (define eq-hashtable-set! hashtable-set!)
  (define eq-hashtable-contains? hashtable-contains?)
  (define eq-hashtable-delete! hashtable-delete!)
  (define eq-hashtable-update! hashtable-update!)
  (define eq-hashtable-weak? hashtable-weak?)
  (define eq-hashtable-ephemeron? hashtable-ephemeron?)
  ;; properties of symbols
  (define props (make-eq-hashtable))
  (define (putprop s k v)
    (unless (symbol? s) (assertion-violation 'putprop "not a symbol" s))
    (let ((a (hashtable-ref props s '())))
      (let ((p (assq k a))) (if p (set-cdr! p v) (hashtable-set! props s (cons (cons k v) a))))))
  (define getprop
    (case-lambda ((s k) (getprop s k #f)) ((s k d) (unless (symbol? s) (assertion-violation 'getprop "not a symbol" s)) (let ((p (assq k (hashtable-ref props s '())))) (if p (cdr p) d)))))
  (define (remprop s k) (unless (symbol? s) (assertion-violation 'remprop "not a symbol" s)) (hashtable-set! props s (remp (lambda (p) (eq? (car p) k)) (hashtable-ref props s '()))))
  (define (merge! pred a b) (merge pred a b))
  ;; immutable strings, vectors and bytevectors: si has one kind of each, the immutable ones are copies that are known (the check of a
  ;; mutation is not made: string-set! on one succeeds)
  (define immutables (make-eq-hashtable))
  (define (mark-immutable! x) (hashtable-set! immutables x #t) x)
  (define (immutable-string? x) (and (string? x) (hashtable-contains? immutables x)))
  (define (mutable-string? x) (and (string? x) (not (hashtable-contains? immutables x))))
  (define (immutable-vector? x) (and (vector? x) (hashtable-contains? immutables x)))
  (define (mutable-vector? x) (and (vector? x) (not (hashtable-contains? immutables x))))
  (define (immutable-bytevector? x) (and (bytevector? x) (hashtable-contains? immutables x)))
  (define (mutable-bytevector? x) (and (bytevector? x) (not (hashtable-contains? immutables x))))
  (define (string->immutable-string s)
    (unless (string? s) (assertion-violation 'string->immutable-string "not a string" s))
    (if (immutable-string? s) s (mark-immutable! (string-copy s))))
  (define (vector->immutable-vector v)
    (unless (vector? v) (assertion-violation 'vector->immutable-vector "not a vector" v))
    (if (immutable-vector? v) v (mark-immutable! (vector-copy v))))
  (define (bytevector->immutable-bytevector b)
    (unless (bytevector? b) (assertion-violation 'bytevector->immutable-bytevector "not a bytevector" b))
    (if (immutable-bytevector? b) b (mark-immutable! (bytevector-copy b))))
  (define (string-append-immutable . ss) (mark-immutable! (apply string-append ss)))
  (define (immutable-vector . xs) (if (null? xs) empty-immutable-vector (mark-immutable! (list->vector xs))))
  (define empty-immutable-vector (mark-immutable! (vector)))
  (define vector-copy
    (case-lambda
      ((v)
       (unless (vector? v) (assertion-violation 'vector-copy "not a vector" v))
       (if (fx=? (vector-length v) 0) v (vector-copy v 0 (vector-length v))))
      ;; Chez 10.3's: the start and the count, not an end
      ((v start n)
       (unless (vector? v) (assertion-violation 'vector-copy "not a vector" v))
       (unless (and (fixnum? start) (fx>=? start 0)) (assertion-violation 'vector-copy "invalid start index" start))
       (unless (and (fixnum? n) (fx>=? n 0)) (assertion-violation 'vector-copy "invalid count" n))
       (unless (fx<=? (fx+ start n) (vector-length v)) (assertion-violation 'vector-copy "index + count is beyond the end" start n v))
       (let ((r (make-vector n)))
         (let loop ((i 0)) (if (fx<? i n) (begin (vector-set! r i (vector-ref v (fx+ start i))) (loop (fx+ i 1))) r))))))
  (define immutable-vector-copy
    (case-lambda
      ((v) (if (and (vector? v) (fx=? (vector-length v) 0)) empty-immutable-vector (mark-immutable! (vector-copy v))))
      ((v start n) (if (and (vector? v) (eqv? n 0)) empty-immutable-vector (mark-immutable! (vector-copy v start n))))))
  (define (vector-append . vs)
    (for-each (lambda (v) (unless (vector? v) (assertion-violation 'vector-append "not a vector" v))) vs)
    (list->vector (apply append (map vector->list vs))))
  (define (immutable-vector-append . vs) (mark-immutable! (apply vector-append vs)))
  (define (vector-set/copy v i x) (let ((r (vector-copy v))) (vector-set! r i x) r))
  (define (immutable-vector-set/copy v i x) (mark-immutable! (vector-set/copy v i x)))
  (define (vector-set-fixnum! v i x)
    (unless (fixnum? x) (assertion-violation 'vector-set-fixnum! "not a fixnum" x))
    (vector-set! v i x))
  (define (string-copy! src s dst d n)
    (unless (string? src) (assertion-violation 'string-copy! "not a string" src))
    (unless (string? dst) (assertion-violation 'string-copy! "not a string" dst))
    (for-each (lambda (i) (unless (and (fixnum? i) (fx>=? i 0)) (assertion-violation 'string-copy! "invalid index or count" i))) (list s d n))
    (unless (and (fx<=? (fx+ s n) (string-length src)) (fx<=? (fx+ d n) (string-length dst))) (assertion-violation 'string-copy! "range is beyond the end" s d n))
    (if (and (eq? src dst) (< s d))
        (do ((i (- n 1) (- i 1))) ((< i 0)) (string-set! dst (+ d i) (string-ref src (+ s i))))
        (do ((i 0 (+ i 1))) ((= i n)) (string-set! dst (+ d i) (string-ref src (+ s i))))))
  (define (substring-fill! s start end c)
    (unless (string? s) (assertion-violation 'substring-fill! "not a string" s))
    (unless (and (fixnum? start) (fixnum? end) (fx<=? 0 start end (string-length s))) (assertion-violation 'substring-fill! "invalid range" start end))
    (unless (char? c) (assertion-violation 'substring-fill! "not a character" c))
    (do ((i start (+ i 1))) ((>= i end)) (string-set! s i c)))
  ;; bytevectors: signed octet lists, and the widths R6RS leaves out
  (define (bytevector->s8-list b) (map (lambda (x) (if (fx>? x 127) (fx- x 256) x)) (bytevector->u8-list b)))
  (define (s8-list->bytevector l)
    (u8-list->bytevector (map (lambda (x) (unless (and (fixnum? x) (fx<=? -128 x 127)) (assertion-violation 's8-list->bytevector "not an s8" x)) (if (fx<? x 0) (fx+ x 256) x)) l)))
  (define (bytevector-s24-ref b i e) (bytevector-sint-ref b i e 3))
  (define (bytevector-u24-ref b i e) (bytevector-uint-ref b i e 3))
  (define (bytevector-s40-ref b i e) (bytevector-sint-ref b i e 5))
  (define (bytevector-u40-ref b i e) (bytevector-uint-ref b i e 5))
  (define (bytevector-s48-ref b i e) (bytevector-sint-ref b i e 6))
  (define (bytevector-u48-ref b i e) (bytevector-uint-ref b i e 6))
  (define (bytevector-s56-ref b i e) (bytevector-sint-ref b i e 7))
  (define (bytevector-u56-ref b i e) (bytevector-uint-ref b i e 7))
  (define (bytevector-s24-set! b i v e) (bytevector-sint-set! b i v e 3))
  (define (bytevector-u24-set! b i v e) (bytevector-uint-set! b i v e 3))
  (define (bytevector-s40-set! b i v e) (bytevector-sint-set! b i v e 5))
  (define (bytevector-u40-set! b i v e) (bytevector-uint-set! b i v e 5))
  (define (bytevector-s48-set! b i v e) (bytevector-sint-set! b i v e 6))
  (define (bytevector-u48-set! b i v e) (bytevector-uint-set! b i v e 6))
  (define (bytevector-s56-set! b i v e) (bytevector-sint-set! b i v e 7))
  (define (bytevector-u56-set! b i v e) (bytevector-uint-set! b i v e 7))
  ;; fixnum and flonum vectors are vectors here (si's reader reads #vfx( and #vfl( as vectors, SMR3): the element type is checked on
  ;; making and setting, fxvector? and flvector? are true of a vector of such elements
  (define (check-all who ok? what l) (for-each (lambda (x) (unless (ok? x) (assertion-violation who what x))) l))
  (define (fxvector . l) (check-all 'fxvector fixnum? "not a fixnum" l) (list->vector l))
  (define (flvector . l) (check-all 'flvector flonum? "not a flonum" l) (list->vector l))
  (define make-fxvector
    (case-lambda ((n) (make-vector n 0)) ((n x) (unless (fixnum? x) (assertion-violation 'make-fxvector "not a fixnum" x)) (make-vector n x))))
  (define make-flvector
    (case-lambda ((n) (make-vector n 0.0)) ((n x) (unless (flonum? x) (assertion-violation 'make-flvector "not a flonum" x)) (make-vector n x))))
  (define (fxvector? x) (and (vector? x) (for-all fixnum? (vector->list x))))
  (define (flvector? x) (and (vector? x) (for-all flonum? (vector->list x))))
  (define (list->fxvector l) (check-all 'list->fxvector fixnum? "not a fixnum" l) (list->vector l))
  (define (list->flvector l) (check-all 'list->flvector flonum? "not a flonum" l) (list->vector l))
  (define fxvector-length vector-length)
  (define flvector-length vector-length)
  (define fxvector-ref vector-ref)
  (define flvector-ref vector-ref)
  (define (fxvector-set! v i x) (unless (fixnum? x) (assertion-violation 'fxvector-set! "not a fixnum" x)) (vector-set! v i x))
  (define (flvector-set! v i x) (unless (flonum? x) (assertion-violation 'flvector-set! "not a flonum" x)) (vector-set! v i x))
  (define fxvector->list vector->list)
  (define flvector->list vector->list)
  (define fxvector-copy vector-copy)
  (define flvector-copy vector-copy)
  (define fxvector-fill! vector-fill!)
  (define flvector-fill! vector-fill!)
  ;; the interaction environment's variables
  (define top-level-bound?
    (case-lambda
      ((s) (top-level-bound? s (interaction-environment)))
      ((s e) (guard (x (#t #f)) (r6-eval s e) #t))))
  (define top-level-value
    (case-lambda ((s) (top-level-value s (interaction-environment))) ((s e) (r6-eval s e))))
  (define set-top-level-value!
    (case-lambda ((s v) (set-top-level-value! s v (interaction-environment))) ((s v e) (r6-eval (list 'set! s (list 'quote v)) e))))
  (define define-top-level-value
    (case-lambda ((s v) (define-top-level-value s v (interaction-environment))) ((s v e) (r6-eval (list 'define s (list 'quote v)) e))))
  (define (scheme-environment) (environment '(chezscheme)))
  ;; load: each datum of a source file evaluated in the interaction environment (the last value returned)
  (define load
    (case-lambda
      ((f) (load f eval))
      ((f ev)
       (let ((p (open-input-file f)))
         (let loop ((x (read p)) (v (if #f #f)))
           (if (eof-object? x) (begin (close-port p) v) (loop (read p) (ev x))))))))
  ;; ports, the system's parameters (no collector, no timer, no compiler here: parameters with Chez's defaults)
  (define console-output-port (make-parameter (current-output-port)))
  (define console-input-port (make-parameter (current-input-port)))
  (define console-error-port (make-parameter (current-error-port)))
  (define trace-output-port (make-parameter (current-output-port)))
  (define (with-input-from-string s thunk)
    (let ((p (open-string-input-port s)) (old (current-input-port)))
      (dynamic-wind (lambda () (current-input-port p)) thunk (lambda () (current-input-port old)))))
  (define (char-ready? . p) #t)
  (define (directory-separator) #\/)
  (define (directory-separator? c) (eqv? c #\/))
  (define timer-interrupt-handler (make-parameter (lambda () (void))))
  (define keyboard-interrupt-handler (make-parameter (lambda () (void))))
  (define (collect . args) (void))
  (define collect-request-handler (make-parameter (lambda () (collect))))
  (define collect-maximum-generation (make-parameter 4))
  (define collect-trip-bytes (make-parameter 8388608))
  (define collect-generation-radix (make-parameter 4))
  (define collect-notify (make-parameter #f))
  (define (bytes-allocated . g) 16491872)
  (define interrupts-disabled 0)
  (define (disable-interrupts) (set! interrupts-disabled (+ interrupts-disabled 1)) (- interrupts-disabled 1))
  (define (enable-interrupts) (set! interrupts-disabled (- interrupts-disabled 1)) interrupts-disabled)
  (define optimize-level (make-parameter 0 (lambda (x) (unless (memv x '(0 1 2 3)) (assertion-violation 'optimize-level "invalid level" x)) x)))
  (define debug-level (make-parameter 1))
  (define enable-unsafe-application (make-parameter #f))
  (define enable-unsafe-variable-reference (make-parameter #f))
  (define enable-type-recovery (make-parameter #t))
  (define print-radix (make-parameter 10))
  (define print-gensym (make-parameter #t))
  (define print-length (make-parameter #f))
  (define print-level (make-parameter #f))
  (define print-brackets (make-parameter #t))
  (define print-vector-length (make-parameter #t))
;; SMR7: Chez's cflonum operations (the User's Guide, 8.5): a cflonum is a flonum or an inexact complex number whose parts are
  ;; flonums; each operation takes cflonums only
  (define (cflonum? x) (or (flonum? x) (and (number? x) (not (real? x)) (inexact? x))))
  (define (cfl-check who xs) (for-each (lambda (x) (unless (cflonum? x) (assertion-violation who "not a cflonum" x))) xs))
  (define (cfl+ . xs) (cfl-check 'cfl+ xs) (apply + 0.0 xs))
  (define (cfl* . xs) (cfl-check 'cfl* xs) (apply * 1.0 xs))
  (define (cfl- x . xs) (cfl-check 'cfl- (cons x xs)) (apply - x xs))
  (define (cfl/ x . xs) (cfl-check 'cfl/ (cons x xs)) (apply / x xs))
  (define (cfl= x . xs) (cfl-check 'cfl= (cons x xs)) (apply = x xs))
  (define (cfl-real-part x) (cfl-check 'cfl-real-part (list x)) (inexact (real-part x)))
  (define (cfl-imag-part x) (cfl-check 'cfl-imag-part (list x)) (inexact (imag-part x)))
  (define (cfl-conjugate x) (cfl-check 'cfl-conjugate (list x)) (if (flonum? x) x (make-rectangular (real-part x) (- (imag-part x)))))
  (define (cfl-magnitude-squared x)
    (cfl-check 'cfl-magnitude-squared (list x))
    (let ((a (inexact (real-part x))) (b (inexact (imag-part x)))) (+ (* a a) (* b b))))
  (define (fl-make-rectangular x y)
    (unless (and (flonum? x) (flonum? y)) (assertion-violation 'fl-make-rectangular "not a flonum" x y))
    (make-rectangular x y))
  ;; a gensym is a symbol that string->symbol of its name does not give back
  (define (gensym? x) (and (symbol? x) (not (eq? x (string->symbol (symbol->string x))))))
  ;; SMR7: the compiler's parameters with Chez's defaults and no effect (si has no cp0 and no code generator), as optimize-level (SMR6)
  (define enable-cp0 (make-parameter #t (lambda (x) (and x #t))))
  (define run-cp0 (make-parameter (lambda (cp0 x) (cp0 (cp0 x)))
                                  (lambda (p) (unless (procedure? p) (assertion-violation 'run-cp0 "not a procedure" p)) p)))
  (define $suppress-primitive-inlining (make-parameter #t (lambda (x) (and x #t))))
  (define internal-defines-as-letrec* (make-parameter #t (lambda (x) (and x #t))))
  (define generate-inspector-information (make-parameter #f (lambda (x) (and x #t))))
  (define generate-procedure-source-information (make-parameter #f (lambda (x) (and x #t))))
  (define generate-interrupt-trap (make-parameter #t (lambda (x) (and x #t))))
  (define $enable-check-prelex-flags (make-parameter #f (lambda (x) (and x #t))))
  (define (nonnegative-fixnum-param who v)
    (make-parameter v (lambda (x) (unless (and (fixnum? x) (fx>=? x 0)) (assertion-violation who "invalid argument" x)) x)))
  (define cp0-effort-limit (nonnegative-fixnum-param 'cp0-effort-limit 200))
  (define cp0-score-limit (nonnegative-fixnum-param 'cp0-score-limit 20))
  (define cp0-outer-unroll-limit (nonnegative-fixnum-param 'cp0-outer-unroll-limit 0))
  (define undefined-variable-warnings (make-parameter #f (lambda (x) (and x #t))))
  (define commonization-level
    (make-parameter 0 (lambda (x) (unless (and (fixnum? x) (fx<=? 0 x 9)) (assertion-violation 'commonization-level "invalid level" x)) x)))
  (define enable-arithmetic-left-associative (make-parameter #f (lambda (x) (and x #t))))
  (define enable-error-source-expression (make-parameter #t (lambda (x) (and x #t))))
  (define compile-interpret-simple (make-parameter #t (lambda (x) (and x #t))))
;; SMR7: the stub experiment's simple names (doc/scheme/MATURITY-REVIEW.md, "As built: SMR7"), each written from its mat
  ;; car-cas!, cdr-cas! and vector-cas!: the compare (eq?) and set of one place; si has no threads, so the swap is never spurious
  (define (car-cas! p old new)
    (unless (pair? p) (assertion-violation 'car-cas! "not a pair" p))
    (and (eq? (car p) old) (begin (set-car! p new) #t)))
  (define (cdr-cas! p old new)
    (unless (pair? p) (assertion-violation 'cdr-cas! "not a pair" p))
    (and (eq? (cdr p) old) (begin (set-cdr! p new) #t)))
  (define (vector-cas! v i old new)
    (unless (and (vector? v) (mutable-vector? v)) (assertion-violation 'vector-cas! "not a mutable vector" v))
    (unless (and (fixnum? i) (fx<=? 0 i) (fx<? i (vector-length v))) (assertion-violation 'vector-cas! "invalid index" i))
    (and (eq? (vector-ref v i) old) (begin (vector-set! v i new) #t)))
  (define (list-assuming-immutable? x) (list? x))
  ;; Chez's vector-copy!: (vector-copy! src src-start dest dest-start n), the ranges checked first, overlapping copies as if through a copy
  (define (copy!-checker who ok?)
    (lambda (src ss dst ds n)
      (unless (ok? src) (assertion-violation who "invalid source" src))
      (unless (ok? dst) (assertion-violation who "invalid destination" dst))
      (for-each (lambda (k) (unless (and (fixnum? k) (fx>=? k 0)) (assertion-violation who "invalid index or count" k))) (list ss ds n))
      (unless (<= (+ ss n) (vector-length src)) (assertion-violation who "source range out of bounds" ss n))
      (unless (<= (+ ds n) (vector-length dst)) (assertion-violation who "destination range out of bounds" ds n))
      (if (and (eq? src dst) (< ss ds))
          (let loop ((k (- n 1))) (when (>= k 0) (vector-set! dst (+ ds k) (vector-ref src (+ ss k))) (loop (- k 1))))
          (let loop ((k 0)) (when (< k n) (vector-set! dst (+ ds k) (vector-ref src (+ ss k))) (loop (+ k 1)))))))
  (define vector-copy! (copy!-checker 'vector-copy! vector?))
  (define fxvector-copy! (copy!-checker 'fxvector-copy! vector?))
  (define flvector-copy! (copy!-checker 'flvector-copy! vector?))
  (define subset-mode (make-parameter #f (lambda (x) (unless (memq x '(#f system)) (assertion-violation 'subset-mode "invalid mode" x)) x)))
  (define ($quotient-remainder x y) (cons (quotient x y) (remainder x y)))
  ;; (expt-mod x y z): x^y mod z by squaring, y a nonnegative exact integer, z a nonzero exact integer
  (define (expt-mod x y z)
    (unless (and (integer? x) (exact? x)) (assertion-violation 'expt-mod "not an exact integer" x))
    (unless (and (integer? y) (exact? y) (>= y 0)) (assertion-violation 'expt-mod "not a nonnegative exact integer" y))
    (unless (and (integer? z) (exact? z) (not (= z 0))) (assertion-violation 'expt-mod "not a nonzero exact integer" z))
    (let loop ((b (modulo x z)) (e y) (r (modulo 1 z)))
      (cond ((= e 0) r)
            ((odd? e) (loop (modulo (* b b) z) (quotient e 2) (modulo (* r b) z)))
            (else (loop (modulo (* b b) z) (quotient e 2) r)))))
  ;; si has no interrupts and no threads: the body is evaluated as it is
  (define-syntax with-interrupts-disabled (syntax-rules () ((_ e1 e2 ...) (let () e1 e2 ...))))
  (define-syntax critical-section (syntax-rules () ((_ e1 e2 ...) (let () e1 e2 ...))))
  ;; the unsigned comparison of two fixnums (their 61-bit two's complement)
  (define ($fxu< x y)
    (unless (and (fixnum? x) (fixnum? y)) (assertion-violation '$fxu< "not a fixnum" x y))
    (let ((u (lambda (v) (if (fx<? v 0) (+ v (expt 2 (fixnum-width))) v)))) (< (u x) (u y))))
  (define (flsingle x)
    (unless (flonum? x) (assertion-violation 'flsingle "not a flonum" x))
    (let ((b (make-bytevector 4))) (bytevector-ieee-single-native-set! b 0 x) (bytevector-ieee-single-native-ref b 0)))
  ;; the bits START to END of a flonum's IEEE double representation, as an unsigned integer
  (define (flbit-field x start end)
    (unless (flonum? x) (assertion-violation 'flbit-field "not a flonum" x))
    (unless (and (fixnum? start) (fixnum? end) (fx<=? 0 start end 64)) (assertion-violation 'flbit-field "invalid range" start end))
    (let ((b (make-bytevector 8))) (bytevector-ieee-double-set! b 0 x (endianness little))
      (bitwise-bit-field (bytevector-u64-ref b 0 (endianness little)) start end)))
  (define current-eval (make-parameter (lambda (x . e) (apply eval x e))
                                       (lambda (p) (unless (procedure? p) (assertion-violation 'current-eval "not a procedure" p)) p)))
  (define case-sensitive (make-parameter #t (lambda (x) (and x #t))))
  (define gensym-count (make-parameter 0 (lambda (x) (unless (and (integer? x) (exact? x) (>= x 0)) (assertion-violation 'gensym-count "invalid count" x)) x)))
  (define gensym-prefix (make-parameter "g"))
  (define pretty-line-length (nonnegative-fixnum-param 'pretty-line-length 75))
  (define pretty-one-line-limit (nonnegative-fixnum-param 'pretty-one-line-limit 60))
  (define pretty-initial-indent (nonnegative-fixnum-param 'pretty-initial-indent 0))
  (define pretty-standard-indent (nonnegative-fixnum-param 'pretty-standard-indent 1))
  (define pretty-maximum-lines (make-parameter #f))
  (define eval-syntax-expanders-when (make-parameter '(compile load eval)))
  (define collect-maximum-generation-threshold-factor (make-parameter 2))
  (define file-position (case-lambda ((p) (port-position p)) ((p pos) (set-port-position! p pos))))
  (define (file-length p) (port-length p))
  (define (clear-input-port . p) (void))
  (define (clear-output-port . p) (void))
;; SMR7: Chez's datum, (datum x) = (syntax->datum #'x), which the mats' own macros use
  (define-syntax datum (syntax-rules () ((_ t) (syntax->datum (syntax t)))))
  ;; a syntax object's list or vector of syntax objects (Chez's syntax->list and syntax->vector)
  (define (syntax->list x)
    (syntax-case x () ((e ...) (syntax (e ...))) (_ (assertion-violation 'syntax->list "invalid argument" x))))
  (define (syntax->vector x)
    (syntax-case x () (#() '#()) (#(e ...) (list->vector (syntax (e ...)))) (_ (assertion-violation 'syntax->vector "invalid argument" x))))
  ;; Chez's index checks: whether (OP x i) would be valid
  (define (ref-check ok? len)
    (lambda (x i) (and (ok? x) (fixnum? i) (fx>=? i 0) (fx<? i (len x)))))
  (define $string-ref-check? (ref-check string? string-length))
  (define $vector-ref-check? (ref-check vector? vector-length))
  (define $fxvector-ref-check? (ref-check vector? vector-length))
  (define $flvector-ref-check? (ref-check vector? vector-length))
  (define $bytevector-ref-check?
    (case-lambda
      ((bits x i) (and (bytevector? x) (fixnum? i) (fx>=? i 0) (fx<=? (fx+ i (fxquotient bits 8)) (bytevector-length x))))
      ((x i) ($bytevector-ref-check? 8 x i))))
  ;; the process's directory as the shell gave it (si has no chdir); the directories Chez searches for sources and libraries
  (define current-directory
    (case-lambda
      (() (or (get-environment-variable "PWD") "."))
      ((d) (assertion-violation 'current-directory "si cannot change the current directory" d))))
  (define source-directories (make-parameter '(".") (lambda (x) (unless (and (list? x) (for-all string? x)) (assertion-violation 'source-directories "invalid list" x)) x)))
  (define library-directories (make-parameter '(("." . "."))))
  (define command-line-arguments (make-parameter '() (lambda (x) (unless (and (list? x) (for-all string? x)) (assertion-violation 'command-line-arguments "invalid list" x)) x)))
  (define reset-handler (make-parameter (lambda () (exit 1)) (lambda (p) (unless (procedure? p) (assertion-violation 'reset-handler "not a procedure" p)) p)))
  (define abort-handler (make-parameter (lambda args (exit 1)) (lambda (p) (unless (procedure? p) (assertion-violation 'abort-handler "not a procedure" p)) p)))
  (define import-notify (make-parameter #f (lambda (x) (and x #t))))
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
