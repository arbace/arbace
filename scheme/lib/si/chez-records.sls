#!r6rs
;;;; (si chez-records): Chez Scheme's own record interface on si's R6RS records (SMR7; doc/scheme/MATURITY-REVIEW.md, "As built:
;;;; SMR7"), re-exported by (chezscheme). Written for Arbace from The Chez Scheme User's Guide (version 10, section 7.15, "Record
;;;; types": define-record, make-record-type, record-constructor, type-descriptor and the csv7: compatibility procedures) and
;;;; from the behaviour Chez's record.ms mats (v10.3.0) require; not Chez's code.
;;;;
;;;; A field is NAME, (NAME), (CLASS NAME), (TYPE NAME) or (CLASS TYPE NAME): CLASS mutable (the default) or immutable, TYPE
;;;; scheme-object (the default) or one of the foreign types a record field may hold. A typed field checks and converts the
;;;; value its constructor or mutator is given, as Chez does: an integer-N or unsigned-N field takes the exact integers from
;;;; -2^(N-1) to 2^N-1 and keeps their low N bits (read back signed or unsigned), a double-float field a flonum, a
;;;; single-float field a flonum rounded to single precision, a fixnum field a fixnum, a char field a character below 256, a
;;;; wchar field a character, a boolean field any value (kept as #t or #f). The model: each Chez record type is an R6RS
;;;; record type (its fields with their mutability), the types and the constructor's arity kept beside it in a table.
(library (si chez-records)
  (export define-record type-descriptor make-record-type record-constructor
          csv7:record-field-accessor csv7:record-field-mutator csv7:record-field-mutable? csv7:record-type-field-names
          csv7:record-type-field-decls csv7:record-field-accessible? csv7:record-type-descriptor csv7:record-type-name csv7:record-type-symbol $record-type-descriptor
          $record-type-field-names record? make-record-type-descriptor record-field-converter register-chez-record-type! chez-record-parent-arity)
  (import (rename (rnrs) (record-constructor r6-record-constructor) (record? r6-record?) (make-record-type-descriptor r6-make-rtd)))

  ;; rtd -> #(fields arity ctor): fields the list of (class type name) of every field, the parents' first; arity the
  ;; constructor's argument count; ctor the constructor define-record made, or #f
  (define info (make-eq-hashtable))

  (define (register-chez-record-type! rtd own-fields arity ctor)
    (hashtable-set! info rtd (vector (append (parent-fields (record-type-parent rtd)) own-fields) arity ctor))
    rtd)

  ;; the fields of an rtd as (class type name), its parents' first; an R6RS type's fields are scheme-object
  (define (all-fields rtd)
    (cond ((not rtd) '())
          ((hashtable-ref info rtd #f) => (lambda (v) (vector-ref v 0)))
          (else (append (all-fields (record-type-parent rtd))
                        (let ((names (vector->list (record-type-field-names rtd))))
                          (let loop ((names names) (i 0))
                            (if (null? names) '()
                                (cons (list (if (record-field-mutable? rtd i) 'mutable 'immutable) 'scheme-object (car names))
                                      (loop (cdr names) (+ i 1))))))))))
  (define (parent-fields p) (if p (all-fields p) '()))

  ;; the argument count of a parent's constructor: define-record's own, else all the fields
  (define (chez-record-parent-arity rtd)
    (cond ((hashtable-ref info rtd #f) => (lambda (v) (vector-ref v 1)))
          (else (length (all-fields rtd)))))

  (define (bits-of type)
    (case type
      ((integer-8 unsigned-8) 8) ((integer-16 unsigned-16 short unsigned-short) 16) ((integer-24 unsigned-24) 24)
      ((integer-32 unsigned-32 int unsigned unsigned-int) 32) ((integer-40 unsigned-40) 40) ((integer-48 unsigned-48) 48)
      ((integer-56 unsigned-56) 56)
      ((integer-64 unsigned-64 long unsigned-long long-long unsigned-long-long iptr uptr void* size_t ssize_t ptrdiff_t) 64)
      (else #f)))
  (define (signed-type? type)
    (memq type '(integer-8 integer-16 integer-24 integer-32 integer-40 integer-48 integer-56 integer-64 short int long long-long iptr
                 ssize_t ptrdiff_t)))
  (define (field-type? type)
    (or (bits-of type)
        (memq type '(scheme-object ptr double-float double single-float float fixnum boolean char wchar wchar_t))))

  ;; the converter of a field type: a procedure from the value given to the value kept, raising on a value the type refuses
  (define (record-field-converter type)
    (define (bad v) (assertion-violation #f (string-append "invalid value ~s for foreign type " (symbol->string type)) v))
    (let ((n (bits-of type)))
      (cond
        (n (let ((lo (- (expt 2 (- n 1)))) (hi (- (expt 2 n) 1)) (m (expt 2 n)) (half (expt 2 (- n 1))) (signed (signed-type? type)))
             (lambda (v)
               (unless (and (integer? v) (exact? v) (<= lo v hi)) (bad v))
               (let ((u (mod v m))) (if (and signed (>= u half)) (- u m) u)))))
        ((memq type '(scheme-object ptr)) (lambda (v) v))
        ((memq type '(double-float double)) (lambda (v) (if (flonum? v) v (bad v))))
        ((memq type '(single-float float))
         (lambda (v)
           (unless (flonum? v) (bad v))
           (let ((b (make-bytevector 4)))
             (bytevector-ieee-single-native-set! b 0 v)
             (bytevector-ieee-single-native-ref b 0))))
        ((eq? type 'fixnum) (lambda (v) (if (fixnum? v) v (bad v))))
        ((eq? type 'boolean) (lambda (v) (if v #t #f)))
        ((eq? type 'char) (lambda (v) (if (and (char? v) (< (char->integer v) 256)) v (bad v))))
        ((memq type '(wchar wchar_t)) (lambda (v) (if (char? v) v (bad v))))
        (else (assertion-violation 'record-field-converter "invalid field type" type)))))

  ;; make-record-type's field specification, as data: (class type name)
  (define (parse-field who f)
    (define (class? x) (memq x '(mutable immutable)))
    (define (ok-name x) (if (symbol? x) x (assertion-violation who "invalid field specifier" f)))
    (cond ((symbol? f) (list 'mutable 'scheme-object f))
          ((and (pair? f) (list? f))
           (case (length f)
             ((1) (list 'mutable 'scheme-object (ok-name (car f))))
             ((2) (if (class? (car f))
                      (list (car f) 'scheme-object (ok-name (cadr f)))
                      (if (field-type? (car f))
                          (list 'mutable (car f) (ok-name (cadr f)))
                          (assertion-violation who "invalid field specifier" f))))
             ((3) (if (and (class? (car f)) (field-type? (cadr f)))
                      (list (car f) (cadr f) (ok-name (caddr f)))
                      (assertion-violation who "invalid field specifier" f)))
             (else (assertion-violation who "invalid field specifier" f))))
          (else (assertion-violation who "invalid field specifier" f))))

  ;; Chez's make-record-type: [parent] name fields; the name a string (a new type each time) or a gensym (its uid: one type)
  (define make-record-type
    (case-lambda
      ((name fields) (make-record-type #f name fields))
      ((parent name fields)
       (unless (or (string? name) (symbol? name)) (assertion-violation 'make-record-type "invalid record name" name))
       (unless (list? fields) (assertion-violation 'make-record-type "invalid field list" fields))
       (when (and parent (not (record-type-descriptor? parent))) (assertion-violation 'make-record-type "not a record type descriptor" parent))
       (let* ((specs (map (lambda (f) (parse-field 'make-record-type f)) fields))
              (sym (if (string? name) (string->symbol name) (string->symbol (symbol->string name))))
              (uid (and (symbol? name) (gensym-uid name)))
              (rtd (r6-make-rtd sym parent uid #f #f
                     (list->vector (map (lambda (s) (list (car s) (caddr s))) specs)))))
         (unless (hashtable-ref info rtd #f)
           (register-chez-record-type! rtd specs (+ (length specs) (if parent (length (all-fields parent)) 0)) #f))
         rtd))))
  ;; a symbol given as a record name is its uid: its own string (si has no gensym's unique name to take apart)
  (define (gensym-uid s) s)

  ;; Chez 10's make-record-type-descriptor takes, beside R6RS's vector of field specifications, a pair (N . MASK): N anonymous fields
  ;; named field, field i mutable when bit i of MASK is set
  (define (make-record-type-descriptor name parent uid sealed? opaque? fields)
    (r6-make-rtd name parent uid sealed? opaque?
      (if (and (pair? fields) (fixnum? (car fields)) (>= (car fields) 0) (integer? (cdr fields)) (exact? (cdr fields)))
          (let loop ((i (- (car fields) 1)) (acc '()))
            (if (< i 0) (list->vector acc)
                (loop (- i 1) (cons (list (if (bitwise-bit-set? (cdr fields) i) 'mutable 'immutable) 'field) acc))))
          fields)))

  (define (converters rtd)
    (map (lambda (f) (and (not (memq (cadr f) '(scheme-object ptr))) (record-field-converter (cadr f)))) (all-fields rtd)))

  ;; Chez's record-constructor: of a record-constructor descriptor, or of an rtd (its default constructor, every field an
  ;; argument, each typed one converted; a define-record type's own constructor)
  (define (record-constructor x)
    (cond
      ((record-constructor-descriptor? x) (r6-record-constructor x))
      ((record-type-descriptor? x)
       (let ((v (hashtable-ref info x #f)))
         (if (and v (vector-ref v 2))
             (vector-ref v 2)
             (let ((raw (r6-record-constructor (make-record-constructor-descriptor x #f #f))) (cs (converters x)))
               (if (exists (lambda (c) c) cs)
                   (lambda args
                     (unless (= (length args) (length cs)) (assertion-violation 'record-constructor "incorrect number of arguments" args))
                     (apply raw (map (lambda (c a) (if c (c a) a)) cs args)))
                   raw)))))
      (else (assertion-violation 'record-constructor "not a record type descriptor" x))))
  (define (record-constructor-descriptor? x)
    (guard (e (#t #f)) (r6-record-constructor x) #t))

  ;; the field of an rtd named by a symbol or by its index among all the fields: (values owner-rtd local-index spec)
  (define (find-field who rtd f)
    (unless (record-type-descriptor? rtd) (assertion-violation who "not a record type descriptor" rtd))
    (let* ((fields (all-fields rtd))
           (i (cond ((and (fixnum? f) (>= f 0) (< f (length fields))) f)
                    ((symbol? f) (let loop ((l fields) (i 0) (found #f))
                                   (cond ((null? l) found) ((eq? (caddr (car l)) f) (loop (cdr l) (+ i 1) i)) (else (loop (cdr l) (+ i 1) found)))))
                    (else #f))))
      (unless i (assertion-violation who "invalid field specifier" f rtd))
      (let loop ((r rtd))
        (let ((base (length (parent-fields (record-type-parent r)))))
          (if (>= i base) (values r (- i base) (list-ref fields i)) (loop (record-type-parent r)))))))

  (define (csv7:record-field-accessor rtd f)
    (call-with-values (lambda () (find-field 'record-field-accessor rtd f)) (lambda (r k spec) (record-accessor r k))))
  (define (csv7:record-field-mutator rtd f)
    (call-with-values (lambda () (find-field 'record-field-mutator rtd f))
      (lambda (r k spec)
        (unless (eq? (car spec) 'mutable) (assertion-violation 'record-field-mutator "field is immutable" f))
        (let ((m (record-mutator r k)))
          (if (memq (cadr spec) '(scheme-object ptr))
              m
              (let ((c (record-field-converter (cadr spec)))) (lambda (x v) (m x (c v)))))))))
  (define (csv7:record-field-mutable? rtd f)
    (call-with-values (lambda () (find-field 'record-field-mutable? rtd f)) (lambda (r k spec) (eq? (car spec) 'mutable))))
  (define (csv7:record-type-field-names rtd)
    (unless (record-type-descriptor? rtd) (assertion-violation 'record-type-field-names "not a record type descriptor" rtd))
    (map caddr (all-fields rtd)))
  (define $record-type-field-names csv7:record-type-field-names)
  ;; the fields as Chez's declarations, (class type name), and whether a field is accessible (every field of si's records is)
  (define (csv7:record-type-field-decls rtd)
    (unless (record-type-descriptor? rtd) (assertion-violation 'record-type-field-decls "not a record type descriptor" rtd))
    (map (lambda (f) (list (car f) (cadr f) (caddr f))) (all-fields rtd)))
  (define (csv7:record-field-accessible? rtd f)
    (call-with-values (lambda () (find-field 'record-field-accessible? rtd f)) (lambda (r k spec) #t)))
  (define (csv7:record-type-descriptor x) (record-rtd x))
  (define ($record-type-descriptor x) (record-rtd x))
  (define (csv7:record-type-name rtd) (symbol->string (record-type-name rtd)))
  (define (csv7:record-type-symbol rtd) (record-type-name rtd))

  ;; Chez's record?: with an rtd, whether x is an instance of it or of a type below it (an opaque record too)
  (define record?
    (case-lambda
      ((x) (r6-record? x))
      ((x rtd)
       (unless (record-type-descriptor? rtd) (assertion-violation 'record? "not a record type descriptor" rtd))
       ((record-predicate rtd) x))))

  (define-syntax type-descriptor
    (syntax-rules () ((_ name) (record-type-descriptor name))))

  ;; (define-record name [parent] (field ...) [((field init) ...) [(option ...)]]): options (prefix "string"),
  ;; (constructor id), (predicate id), (nongenerative [uid]), (sealed bool), (opaque bool); print-method and
  ;; reader-name are taken and ignored. The constructor takes the parent's arguments then the fields without an init; an
  ;; init sees those fields and the inits before it.
  (define-syntax define-record
    (lambda (x)
      (define (class? s) (memq s '(mutable immutable)))
      (define (type? s)
        (memq s '(scheme-object ptr double-float double single-float float fixnum boolean char wchar wchar_t
                  integer-8 unsigned-8 integer-16 unsigned-16 short unsigned-short integer-24 unsigned-24 integer-32 unsigned-32 int
                  unsigned unsigned-int integer-40 unsigned-40 integer-48 unsigned-48 integer-56 unsigned-56 integer-64 unsigned-64
                  long unsigned-long long-long unsigned-long-long iptr uptr void* size_t ssize_t ptrdiff_t)))
      ;; a field: (class type name-identifier)
      (define (field f)
        (syntax-case f ()
          (n (identifier? #'n) (list 'mutable 'scheme-object #'n))
          ((n) (identifier? #'n) (list 'mutable 'scheme-object #'n))
          ((a n) (and (identifier? #'a) (identifier? #'n) (class? (syntax->datum #'a))) (list (syntax->datum #'a) 'scheme-object #'n))
          ((a n) (and (identifier? #'a) (identifier? #'n) (type? (syntax->datum #'a))) (list 'mutable (syntax->datum #'a) #'n))
          ((c t n) (and (identifier? #'n) (class? (syntax->datum #'c)) (type? (syntax->datum #'t)))
           (list (syntax->datum #'c) (syntax->datum #'t) #'n))
          (_ (syntax-violation 'define-record "invalid field specifier" x f))))
      (define (default d)
        (syntax-case d () ((f e) (cons (field #'f) #'e)) (_ (syntax-violation 'define-record "invalid field specifier" x d))))
      (define (id ctx . parts)
        (datum->syntax ctx (string->symbol (apply string-append
                                                  (map (lambda (p) (if (symbol? p) (symbol->string p) p)) parts)))))
      (define (option-value opts key)
        (let loop ((l opts))
          (cond ((null? l) #f)
                ((and (pair? (car l)) (eq? (syntax->datum (caar l)) key)) (cdar l))
                ((and (identifier? (car l)) (eq? (syntax->datum (car l)) key)) '())
                (else (loop (cdr l))))))
      (define (build name par flds defaults opts)
        (let* ((fs (map field flds))
               (ds (map default defaults))
               (all (append fs (map car ds)))
               (_ (let loop ((ns (map (lambda (f) (syntax->datum (caddr f))) all)))
                    (when (pair? ns)
                      (when (memq (car ns) (cdr ns)) (syntax-violation 'define-record "duplicate field name" x (car ns)))
                      (loop (cdr ns)))))
               (opts (map (lambda (o) (syntax-case o () ((k v ...) (cons #'k #'(v ...))) (k #'k))) opts))
               (nm (symbol->string (syntax->datum name)))
               (prefix (let ((p (option-value opts 'prefix))) (if (and p (pair? p)) (syntax->datum (car p)) (string-append nm "-"))))
               (ctor (let ((c (option-value opts 'constructor))) (if (and c (pair? c)) (car c) (id name "make-" nm))))
               (pred (let ((p (option-value opts 'predicate))) (if (and p (pair? p)) (car p) (id name nm "?"))))
               (ng (option-value opts 'nongenerative))
               (sealed (let ((s (option-value opts 'sealed))) (and s (if (pair? s) (syntax->datum (car s)) #t))))
               (opaque (let ((s (option-value opts 'opaque))) (and s (if (pair? s) (syntax->datum (car s)) #t))))
               (fname (lambda (f) (symbol->string (syntax->datum (caddr f)))))
               (accs (map (lambda (f) (id name prefix (fname f))) all))
               (muts (map (lambda (f) (id name "set-" prefix (fname f) "!")) all))
               (raccs (generate-temporaries all))
               (rmuts (generate-temporaries all))
               (convs (generate-temporaries all))
               (own-ids (map caddr fs))
               (specs (map (lambda (f) (list (car f) (cadr f) (syntax->datum (caddr f)))) all)))
          (with-syntax ((name name)
                        (ctor ctor) (pred pred)
                        ((rctor rpred reg) (generate-temporaries '(1 2 3)))
                        ((fspec ...) (map (lambda (f ra rm) (if (eq? (car f) 'mutable) (list #'mutable (caddr f) ra rm) (list #'immutable (caddr f) ra)))
                                          all raccs rmuts))
                        ((own ...) own-ids)
                        ((conv ...) convs)
                        ((ftype ...) (map (lambda (f) (datum->syntax name (cadr f))) all))
                        ((own-conv ...) (list-head convs (length fs)))
                        ((dflt ...) (map (lambda (d) (caddr (car d))) ds))
                        ((dflt-conv ...) (list-tail convs (length fs)))
                        ((dflt-e ...) (map cdr ds))
                        ((parent-clause ...) (if par (list (list #'parent par)) '()))
                        ((ng-clause ...) (cond ((not ng) '())
                                               ((pair? ng) (list (list #'nongenerative (car ng))))
                                               (else (list (list #'nongenerative)))))
                        ((sealed-clause ...) (if sealed (list (list #'sealed #t)) '()))
                        ((opaque-clause ...) (if opaque (list (list #'opaque #t)) '()))
                        ((acc-def ...) (map (lambda (a ra) (list #'define a ra)) accs raccs))
                        ((mut-def ...) (let loop ((all all) (muts muts) (rmuts rmuts) (convs convs))
                                         (cond ((null? all) '())
                                               ((not (eq? (car (car all)) 'mutable)) (loop (cdr all) (cdr muts) (cdr rmuts) (cdr convs)))
                                               ((memq (cadr (car all)) '(scheme-object ptr))
                                                (cons (list #'define (car muts) (car rmuts)) (loop (cdr all) (cdr muts) (cdr rmuts) (cdr convs))))
                                               (else (cons (with-syntax ((m (car muts)) (rm (car rmuts)) (c (car convs)))
                                                             #'(define (m r v) (rm r (c v))))
                                                           (loop (cdr all) (cdr muts) (cdr rmuts) (cdr convs)))))))
                        (specs (datum->syntax name specs))
                        (nown (length fs)))
            (with-syntax ((proto
                           (if par
                               (with-syntax ((pname par))
                                 #'(lambda (n)
                                     (lambda args
                                       (let ((k (chez-record-parent-arity (record-type-descriptor pname))))
                                         (unless (= (length args) (+ k nown))
                                           (assertion-violation 'ctor "incorrect number of arguments" args))
                                         (let loop ((k k) (pargs '()) (rest args))
                                           (if (= k 0)
                                               (apply (lambda (own ...)
                                                        (let* ((own (own-conv own)) ... (dflt (dflt-conv dflt-e)) ...)
                                                          ((apply n (reverse pargs)) own ... dflt ...)))
                                                      rest)
                                               (loop (- k 1) (cons (car rest) pargs) (cdr rest))))))))
                               #'(lambda (p)
                                   (lambda (own ...)
                                     (let* ((own (own-conv own)) ... (dflt (dflt-conv dflt-e)) ...)
                                       (p own ... dflt ...)))))))
              #'(begin
                  (define conv (record-field-converter 'ftype)) ...
                  (define-record-type (name rctor rpred)
                    parent-clause ... ng-clause ... sealed-clause ... opaque-clause ...
                    (fields fspec ...)
                    (protocol proto))
                  (define ctor rctor)
                  (define pred rpred)
                  acc-def ...
                  mut-def ...
                  (define reg (register-chez-record-type! (record-type-descriptor name) 'specs
                                (+ nown (if (record-type-parent (record-type-descriptor name))
                                            (chez-record-parent-arity (record-type-parent (record-type-descriptor name)))
                                            0))
                                ctor)))))))
      (define (list-head l n) (if (= n 0) '() (cons (car l) (list-head (cdr l) (- n 1)))))
      (syntax-case x ()
        ((_ name (f ...)) (identifier? #'name) (build #'name #f #'(f ...) '() '()))
        ((_ name (f ...) (d ...)) (identifier? #'name) (build #'name #f #'(f ...) #'(d ...) '()))
        ((_ name (f ...) (d ...) (o ...)) (identifier? #'name) (build #'name #f #'(f ...) #'(d ...) #'(o ...)))
        ((_ name parent (f ...)) (and (identifier? #'name) (identifier? #'parent)) (build #'name #'parent #'(f ...) '() '()))
        ((_ name parent (f ...) (d ...)) (and (identifier? #'name) (identifier? #'parent)) (build #'name #'parent #'(f ...) #'(d ...) '()))
        ((_ name parent (f ...) (d ...) (o ...)) (and (identifier? #'name) (identifier? #'parent))
         (build #'name #'parent #'(f ...) #'(d ...) #'(o ...)))))))
