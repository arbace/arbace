#!r6rs
;;; SRFI 14 (character-set library) as an R6RS library for si.
;;; Ported from the reference implementation srfi-14.scm of
;;; https://github.com/scheme-requests-for-implementation/srfi-14 at d6e98129d1e075c03bfe4dcb30e1b5847727e26e
;;; (provenance-14.edn), under the MIT notice below. Changed from the original: the original is Latin-1 only (char sets are
;;; 256-character strings, "would certainly have to be rewritten for Unicode"), so the representation is rewritten for
;;; Unicode: a char set is a record holding a sorted list of disjoint inclusive code point ranges (surrogates excluded),
;;; the set algebra a sweep over range lists; the procedures keep the original's names, argument orders, optional
;;; arguments, boundary cases and its descending iteration order (char-set-fold, char-set-for-each, cursors start at
;;; the highest character; char-set->list is ascending). The standard sets are computed from (char-general-category)
;;; on first use (Unicode, as the SRFI's text defines them: lower-case Ll, upper-case Lu, title-case Lt, letter L*,
;;; digit Nd, punctuation P*, symbol S*, whitespace Zs/Zl/Zp and U+9..U+D, blank Zs and U+9, iso-control the ranges
;;; U+0..U+1F and U+7F..U+9F, graphic letter+digit+punctuation+symbol). The linear-update ! procedures mutate their
;;; first argument. LET-OPTIONALS*, :OPTIONAL, CHECK-ARG, ERROR become rest-argument parsing and R6RS
;;; assertion-violation; define-record-type in R6RS syntax. The reference's char-set-unfold argument order is
;;; (p f g seed [base]); the SRFI text's prose says (f p g seed), the reference and its tests use p f g, kept.
;;;
;;; Copyright notice (as in the original file)
;;; Copyright (c) 1988-1995 Massachusetts Institute of Technology
;;; 
;;; This material was developed by the Scheme project at the Massachusetts
;;; Institute of Technology, Department of Electrical Engineering and
;;; Computer Science.  Permission to copy and modify this software, to
;;; redistribute either the original software or a modified version, and
;;; to use this software for any purpose is granted, subject to the
;;; following restrictions and understandings.
;;; 
;;; 1. Any copy made of this software must include this copyright notice
;;; in full.
;;; 
;;; 2. Users of this software agree to make their best efforts (a) to
;;; return to the MIT Scheme project any improvements or extensions that
;;; they make, so that these may be included in future releases; and (b)
;;; to inform MIT of noteworthy uses of this software.
;;; 
;;; 3. All materials developed as a consequence of the use of this
;;; software shall duly acknowledge such use, in accordance with the usual
;;; standards of acknowledging credit in academic research.
;;; 
;;; 4. MIT has made no warrantee or representation that the operation of
;;; this software will be error-free, and MIT is under no obligation to
;;; provide any services, by way of maintenance, update, or otherwise.
;;; 
;;; 5. In conjunction with products arising from the use of this material,
;;; there shall be no use of the name of the Massachusetts Institute of
;;; Technology nor of any adaptation thereof in any advertising,
;;; promotional, or sales literature without prior written consent from
;;; MIT in each case.

(library (srfi :14 char-sets)
  (export char-set? char-set= char-set<= char-set-hash
          char-set-cursor char-set-ref char-set-cursor-next end-of-char-set?
          char-set-fold char-set-unfold char-set-unfold! char-set-for-each char-set-map
          char-set-copy char-set list->char-set list->char-set! string->char-set string->char-set!
          char-set-filter char-set-filter! ucs-range->char-set ucs-range->char-set! ->char-set
          char-set-size char-set-count char-set-contains? char-set-every char-set-any
          char-set-adjoin char-set-adjoin! char-set-delete char-set-delete!
          char-set->list char-set->string
          char-set-complement char-set-complement! char-set-union char-set-union!
          char-set-intersection char-set-intersection! char-set-difference char-set-difference!
          char-set-xor char-set-xor! char-set-diff+intersection char-set-diff+intersection!
          char-set:lower-case char-set:upper-case char-set:title-case char-set:letter char-set:digit
          char-set:letter+digit char-set:graphic char-set:printing char-set:whitespace char-set:iso-control
          char-set:punctuation char-set:symbol char-set:hex-digit char-set:blank char-set:ascii
          char-set:empty char-set:full)
  (import (rnrs))

  ;; A char set: R, the sorted list of disjoint, non-adjacent inclusive ranges (lo . hi) of scalar values.
  ;; A promise-like thunk in R computes the standard sets on first use.
  (define-record-type cs (fields (mutable r)) (protocol (lambda (new) (lambda (r) (new r)))))
  (define char-set? cs?)

  (define max-cp #x10FFFF)
  (define universe '((0 . #xD7FF) (#xE000 . #x10FFFF)))

  (define (ranges s)
    (let ((r (cs-r s)))
      (if (procedure? r) (let ((v (r))) (cs-r-set! s v) v) r)))

  (define (check-arg pred val who)
    (if (pred val) val (assertion-violation who "invalid argument" val)))
  (define (cs/check s who)
    (if (cs? s) (ranges s) (assertion-violation who "not a char-set" s)))
  (define (cp c) (char->integer c))

  ;; Range-list algebra.
  (define (norm rs)   ; sort by lo and coalesce
    (let loop ((rs (list-sort (lambda (a b) (< (car a) (car b))) rs)) (acc '()))
      (cond ((null? rs) (reverse acc))
            ((and (pair? acc) (<= (car (car rs)) (+ (cdr (car acc)) 1)))
             (loop (cdr rs)
                   (if (> (cdr (car rs)) (cdr (car acc)))
                       (cons (cons (car (car acc)) (cdr (car rs))) (cdr acc))
                       acc)))
            (else (loop (cdr rs) (cons (car rs) acc))))))
  (define (r-union a b)
    (cond ((null? a) b) ((null? b) a)
          (else (norm (append a b)))))
  (define (r-diff a b)
    (let loop ((a a) (b b) (acc '()))
      (cond ((null? a) (reverse acc))
            ((null? b) (append (reverse acc) a))
            (else
             (let ((lo (car (car a))) (hi (cdr (car a))) (blo (car (car b))) (bhi (cdr (car b))))
               (cond ((< bhi lo) (loop a (cdr b) acc))
                     ((> blo hi) (loop (cdr a) b (cons (car a) acc)))
                     (else
                      (let ((acc (if (> blo lo) (cons (cons lo (- blo 1)) acc) acc)))
                        (if (< bhi hi)
                            (loop (cons (cons (+ bhi 1) hi) (cdr a)) (cdr b) acc)
                            (loop (cdr a) b acc))))))))))
  (define (r-inter a b) (r-diff a (r-diff a b)))
  (define (r-xor a b) (r-union (r-diff a b) (r-diff b a)))
  (define (r-complement a) (r-diff universe a))
  (define (r-clip lo hi)   ; [lo, hi] clipped to the scalar values
    (r-inter (list (cons lo hi)) universe))
  (define (r-contains? rs c)
    (let loop ((rs rs))
      (and (pair? rs)
           (cond ((< c (car (car rs))) #f)
                 ((<= c (cdr (car rs))) #t)
                 (else (loop (cdr rs)))))))
  (define (r-add rs c) (r-union rs (list (cons c c))))
  (define (r-del rs c) (r-diff rs (list (cons c c))))
  (define (r-size rs) (fold-left (lambda (n r) (+ n (- (cdr r) (car r)) 1)) 0 rs))
  ;; descending iteration over the characters
  (define (r-for-each-desc proc rs)
    (for-each (lambda (r)
                (let lp ((i (cdr r)))
                  (when (>= i (car r)) (proc (integer->char i)) (lp (- i 1)))))
              (reverse rs)))

  ;; Optional base set: a fresh copy of its ranges (ranges are never mutated in place, so sharing is safe).
  (define (default-base maybe-base who)
    (if (pair? maybe-base)
        (let ((bcs (car maybe-base)))
          (if (null? (cdr maybe-base))
              (if (cs? bcs) (ranges bcs) (assertion-violation who "base-cs parameter not a char-set" bcs))
              (assertion-violation who "expected final base char set -- too many parameters" maybe-base)))
        '()))

  (define (char-set-copy c) (make-cs (cs/check c 'char-set-copy)))

  (define char-set=
    (case-lambda
      (() #t)
      ((c1 . rest)
       (let ((s1 (cs/check c1 'char-set=)))
         (let lp ((rest rest))
           (or (null? rest)
               (and (equal? s1 (cs/check (car rest) 'char-set=))
                    (lp (cdr rest)))))))))

  (define char-set<=
    (case-lambda
      (() #t)
      ((c1 . rest)
       (let lp ((s1 (cs/check c1 'char-set<=)) (rest rest))
         (or (null? rest)
             (let ((s2 (cs/check (car rest) 'char-set<=)))
               (and (null? (r-diff s1 s2)) (lp s2 (cdr rest)))))))))

  ;; Hash: equal sets have equal canonical range lists, so a fold over the ranges.
  (define (char-set-hash c . maybe-bound)
    (let* ((bound (if (pair? maybe-bound)
                      (check-arg (lambda (n) (and (integer? n) (exact? n) (<= 0 n))) (car maybe-bound) 'char-set-hash)
                      4194304))
           (bound (if (zero? bound) 4194304 bound))
           (s (cs/check c 'char-set-hash)))
      (let lp ((rs s) (h 0))
        (if (null? rs) (mod h bound)
            (lp (cdr rs) (mod (+ (* 37 (+ (* 37 h) (car (car rs)))) (cdr (car rs))) 4194301))))))

  (define (char-set-contains? c ch)
    (r-contains? (cs/check c 'char-set-contains?) (cp (check-arg char? ch 'char-set-contains?))))
  (define (char-set-size c) (r-size (cs/check c 'char-set-size)))
  (define (char-set-count pred c)
    (check-arg procedure? pred 'char-set-count)
    (let ((n 0))
      (r-for-each-desc (lambda (ch) (if (pred ch) (set! n (+ n 1)))) (cs/check c 'char-set-count))
      n))

  ;; Adjoin & delete
  (define (adjoin-all rs chars) (fold-left (lambda (rs ch) (r-add rs (cp ch))) rs chars))
  (define (delete-all rs chars) (fold-left (lambda (rs ch) (r-del rs (cp ch))) rs chars))
  (define (char-set-adjoin c . chars) (make-cs (adjoin-all (cs/check c 'char-set-adjoin) chars)))
  (define (char-set-adjoin! c . chars) (cs-r-set! c (adjoin-all (cs/check c 'char-set-adjoin!) chars)) c)
  (define (char-set-delete c . chars) (make-cs (delete-all (cs/check c 'char-set-delete) chars)))
  (define (char-set-delete! c . chars) (cs-r-set! c (delete-all (cs/check c 'char-set-delete!) chars)) c)

  ;; Cursors: -1 at the end, else a pair (code . descending-ranges) whose first range holds code.
  (define (char-set-cursor c)
    (let ((d (reverse (cs/check c 'char-set-cursor))))
      (if (null? d) -1 (cons (cdr (car d)) d))))
  (define (end-of-char-set? cursor) (not (pair? cursor)))
  (define (char-set-ref c cursor)
    (check-arg pair? cursor 'char-set-ref)
    (integer->char (car cursor)))
  (define (char-set-cursor-next c cursor)
    (check-arg pair? cursor 'char-set-cursor-next)
    (let ((i (car cursor)) (d (cdr cursor)))
      (cond ((> i (car (car d))) (cons (- i 1) d))
            ((null? (cdr d)) -1)
            (else (cons (cdr (car (cdr d))) (cdr d))))))

  ;; for-each map fold unfold every any
  (define (char-set-for-each proc c)
    (check-arg procedure? proc 'char-set-for-each)
    (r-for-each-desc proc (cs/check c 'char-set-for-each)))
  (define (char-set-map proc c)
    (check-arg procedure? proc 'char-set-map)
    (let ((acc '()))
      (r-for-each-desc (lambda (ch) (set! acc (cons (let ((x (cp (proc ch)))) (cons x x)) acc)))
                       (cs/check c 'char-set-map))
      (make-cs (norm acc))))
  (define (char-set-fold kons knil c)
    (check-arg procedure? kons 'char-set-fold)
    (let ((ans knil))
      (r-for-each-desc (lambda (ch) (set! ans (kons ch ans))) (cs/check c 'char-set-fold))
      ans))
  (define (char-set-every pred c)
    (check-arg procedure? pred 'char-set-every)
    (let lp ((cur (char-set-cursor c)))
      (or (end-of-char-set? cur)
          (and (pred (char-set-ref c cur)) (lp (char-set-cursor-next c cur))))))
  (define (char-set-any pred c)
    (check-arg procedure? pred 'char-set-any)
    (let lp ((cur (char-set-cursor c)))
      (and (not (end-of-char-set? cur))
           (or (pred (char-set-ref c cur)) (lp (char-set-cursor-next c cur))))))

  (define (unfold-ranges who p f g seed rs)
    (check-arg procedure? p who) (check-arg procedure? f who) (check-arg procedure? g who)
    (let lp ((seed seed) (acc '()))
      (if (p seed) (r-union rs (norm acc))
          (lp (g seed) (let ((x (cp (f seed)))) (cons (cons x x) acc))))))
  (define (char-set-unfold p f g seed . maybe-base)
    (make-cs (unfold-ranges 'char-set-unfold p f g seed (default-base maybe-base 'char-set-unfold))))
  (define (char-set-unfold! p f g seed base)
    (cs-r-set! base (unfold-ranges 'char-set-unfold! p f g seed (cs/check base 'char-set-unfold!)))
    base)

  ;; list <--> char-set
  (define (chars->ranges chars) (norm (map (lambda (ch) (let ((x (cp ch))) (cons x x))) chars)))
  (define (char-set . chars) (make-cs (chars->ranges chars)))
  (define (list->char-set chars . maybe-base)
    (make-cs (r-union (default-base maybe-base 'list->char-set) (chars->ranges chars))))
  (define (list->char-set! chars base)
    (cs-r-set! base (r-union (cs/check base 'list->char-set!) (chars->ranges chars)))
    base)
  (define (char-set->list c)
    (let ((acc '()))
      (r-for-each-desc (lambda (ch) (set! acc (cons ch acc))) (cs/check c 'char-set->list))
      acc))

  ;; string <--> char-set
  (define (string->char-set str . maybe-base)
    (check-arg string? str 'string->char-set)
    (make-cs (r-union (default-base maybe-base 'string->char-set) (chars->ranges (string->list str)))))
  (define (string->char-set! str base)
    (check-arg string? str 'string->char-set!)
    (cs-r-set! base (r-union (cs/check base 'string->char-set!) (chars->ranges (string->list str))))
    base)
  (define (char-set->string c)
    (list->string (char-set->list c)))

  ;; UCS-range -> char-set: [lower, upper)
  (define (ucs-ranges who lower upper error? rs)
    (check-arg (lambda (x) (and (integer? x) (exact? x) (<= 0 x))) lower who)
    (check-arg (lambda (x) (and (integer? x) (exact? x) (<= lower x))) upper who)
    (let ((new (if (< lower upper) (r-clip lower (- upper 1)) '())))
      (if (and error? (< lower upper) (not (= (r-size new) (- upper lower))))
          (assertion-violation who "requested UCS range contains unavailable characters" lower upper))
      (r-union rs new)))
  (define (ucs-range->char-set lower upper . rest)
    (let ((error? (and (pair? rest) (car rest))))
      (make-cs (ucs-ranges 'ucs-range->char-set lower upper error?
                           (default-base (if (pair? rest) (cdr rest) '()) 'ucs-range->char-set)))))
  (define (ucs-range->char-set! lower upper error? base)
    (cs-r-set! base (ucs-ranges 'ucs-range->char-set! lower upper error? (cs/check base 'ucs-range->char-set!)))
    base)

  ;; predicate -> char-set: the members of DOMAIN satisfying PRED (added to the base)
  (define (filter-ranges who pred domain rs)
    (check-arg procedure? pred who)
    (let ((acc '()))
      (r-for-each-desc (lambda (ch) (if (pred ch) (set! acc (cons (cons (cp ch) (cp ch)) acc))))
                       (cs/check domain who))
      (r-union rs (norm acc))))
  (define (char-set-filter pred domain . maybe-base)
    (make-cs (filter-ranges 'char-set-filter pred domain (default-base maybe-base 'char-set-filter))))
  (define (char-set-filter! pred domain base)
    (cs-r-set! base (filter-ranges 'char-set-filter! pred domain (cs/check base 'char-set-filter!)))
    base)

  (define (->char-set x)
    (cond ((char-set? x) x)
          ((string? x) (string->char-set x))
          ((char? x) (char-set x))
          (else (assertion-violation '->char-set "not a charset, string or char" x))))

  ;; Set algebra
  (define (fold-ranges who op rs csets)
    (fold-left (lambda (rs c) (op rs (cs/check c who))) rs csets))
  (define (char-set-complement c) (make-cs (r-complement (cs/check c 'char-set-complement))))
  (define (char-set-complement! c) (cs-r-set! c (r-complement (cs/check c 'char-set-complement!))) c)
  (define (char-set-union . csets)
    (if (pair? csets)
        (make-cs (fold-ranges 'char-set-union r-union (cs/check (car csets) 'char-set-union) (cdr csets)))
        (char-set)))
  (define (char-set-union! c1 . csets)
    (cs-r-set! c1 (fold-ranges 'char-set-union! r-union (cs/check c1 'char-set-union!) csets)) c1)
  (define (char-set-intersection . csets)
    (if (pair? csets)
        (make-cs (fold-ranges 'char-set-intersection r-inter (cs/check (car csets) 'char-set-intersection) (cdr csets)))
        (make-cs universe)))
  (define (char-set-intersection! c1 . csets)
    (cs-r-set! c1 (fold-ranges 'char-set-intersection! r-inter (cs/check c1 'char-set-intersection!) csets)) c1)
  (define (char-set-difference c1 . csets)
    (make-cs (fold-ranges 'char-set-difference r-diff (cs/check c1 'char-set-difference) csets)))
  (define (char-set-difference! c1 . csets)
    (cs-r-set! c1 (fold-ranges 'char-set-difference! r-diff (cs/check c1 'char-set-difference!) csets)) c1)
  (define (char-set-xor . csets)
    (if (pair? csets)
        (make-cs (fold-ranges 'char-set-xor r-xor (cs/check (car csets) 'char-set-xor) (cdr csets)))
        (char-set)))
  (define (char-set-xor! c1 . csets)
    (cs-r-set! c1 (fold-ranges 'char-set-xor! r-xor (cs/check c1 'char-set-xor!) csets)) c1)
  (define (char-set-diff+intersection c1 . csets)
    (let ((s1 (cs/check c1 'char-set-diff+intersection)))
      (let ((u (fold-ranges 'char-set-diff+intersection r-union '() csets)))
        (values (make-cs (r-diff s1 u)) (make-cs (r-inter s1 u))))))
  (define (char-set-diff+intersection! c1 c2 . csets)
    (let ((s1 (cs/check c1 'char-set-diff+intersection!)) (s2 (cs/check c2 'char-set-diff+intersection!)))
      (let ((u (fold-ranges 'char-set-diff+intersection! r-union s2 csets)))
        (cs-r-set! c1 (r-diff s1 u))
        (cs-r-set! c2 (r-inter s1 u))
        (values c1 c2))))

  ;; System character sets: computed from the general categories on first use, one scan of the scalar values.
  (define cat-table #f)   ; alist category -> ascending range list
  (define (categories)
    (or cat-table
        (let ((h (make-eq-hashtable)))
          (let lp ((i #x10FFFF))   ; descending, so each list ends up ascending after the extensions
            (when (>= i 0)
              (unless (and (>= i #xD800) (< i #xE000))
                (let* ((g (char-general-category (integer->char i))) (l (hashtable-ref h g '())))
                  (hashtable-set! h g
                    (if (and (pair? l) (= (car (car l)) (+ i 1)))
                        (cons (cons i (cdr (car l))) (cdr l))
                        (cons (cons i i) l)))))
              (lp (- i 1))))
          (set! cat-table h)
          h)))
  (define (cats . names)
    (lambda ()
      (let ((h (categories)))
        (fold-left (lambda (rs n) (r-union rs (hashtable-ref h n '()))) '() names))))
  (define (lazy-set thunk) (make-cs thunk))
  (define (lazy-union . sets)
    (lazy-set (lambda () (fold-left (lambda (rs s) (r-union rs (ranges s))) '() sets))))

  (define char-set:empty (char-set))
  (define char-set:full (make-cs universe))
  (define char-set:lower-case (lazy-set (cats 'Ll)))
  (define char-set:upper-case (lazy-set (cats 'Lu)))
  (define char-set:title-case (lazy-set (cats 'Lt)))
  (define char-set:letter (lazy-set (cats 'Lu 'Ll 'Lt 'Lm 'Lo)))
  (define char-set:digit (lazy-set (cats 'Nd)))
  (define char-set:hex-digit (make-cs '((#x30 . #x39) (#x41 . #x46) (#x61 . #x66))))
  (define char-set:letter+digit (lazy-set (cats 'Lu 'Ll 'Lt 'Lm 'Lo 'Nd)))
  (define char-set:punctuation (lazy-set (cats 'Pc 'Pd 'Ps 'Pe 'Pi 'Pf 'Po)))
  (define char-set:symbol (lazy-set (cats 'Sm 'Sc 'Sk 'So)))
  (define char-set:graphic (lazy-union char-set:letter+digit char-set:punctuation char-set:symbol))
  (define char-set:whitespace (lazy-set (lambda () (r-union '((9 . 13)) ((cats 'Zs 'Zl 'Zp))))))
  (define char-set:printing (lazy-union char-set:whitespace char-set:graphic))
  (define char-set:blank (lazy-set (lambda () (r-union '((9 . 9)) ((cats 'Zs))))))
  (define char-set:iso-control (make-cs '((0 . 31) (127 . 159))))
  (define char-set:ascii (make-cs '((0 . 127))))
)
