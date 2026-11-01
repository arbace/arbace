;;; tools/scheme/normtests.ss: the recorded cases of si's normalization test (SM6), from Chez Scheme 10.3.0's own
;;; string-normalize-nfd/nfc/nfkd/nfkc.
;;;
;;;   chez --script tools/scheme/normtests.ss > test/scheme/si/norm-chez.tsv
;;;
;;; Two kinds of line (tab separated; HASH is the 32-bit h = (31 h + cp) mod 2^32 over the string's code points, 8 hex digits):
;;;   B START COUNT H1 H2 H3 H4    every code point START .. START+COUNT-1 (surrogates skipped) as a one-character
;;;                                string: the hash of the concatenation of the four forms' strings, for nfd nfc nfkd nfkc
;;;   S CPS H1 H2 H3 H4            a random string (CPS: its code points, hex, dot separated), the hashes of its four forms
;;; The random strings (a fixed generator, seed below) mix starters with decompositions, combining marks of many classes,
;;; Hangul jamo and syllables, compatibility characters, ligatures, supplementary planes and anything else.
(import (rnrs) (only (chezscheme) printf))

(define (hex n) (string-upcase (number->string n 16)))
(define (pad8 n) (let ((s (hex n))) (string-append (make-string (- 8 (string-length s)) #\0) s)))
(define (scalar? n) (not (and (>= n #xD800) (<= n #xDFFF))))
(define (hash-add h s)
  (let loop ((i 0) (h h))
    (if (= i (string-length s)) h
        (loop (+ i 1) (mod (+ (* h 31) (char->integer (string-ref s i))) 4294967296)))))
(define forms (list string-normalize-nfd string-normalize-nfc string-normalize-nfkd string-normalize-nfkc))

;; blocks
(define block 1024)
(let loop ((start 0))
  (when (<= start #x10FFFF)
    (let ((hs (map (lambda (f) 0) forms)) (count 0))
      (let ((hs (let cl ((n start) (hs hs))
                  (if (or (> n #x10FFFF) (>= n (+ start block)))
                      hs
                      (cl (+ n 1)
                          (if (scalar? n)
                              (let ((s (string (integer->char n))))
                                (map (lambda (f h) (hash-add h (f s))) forms hs))
                              hs))))))
        (printf "B\t~a\t~a" (hex start) (min block (- #x110000 start)))
        (for-each (lambda (h) (printf "\t~a" (pad8 h))) hs)
        (newline)))
    (loop (+ start block))))

;; the pools
(define (nonstarter? x)
  (let ((p230 (integer->char #x301)) (p220 (integer->char #x323)))
    (let ((s (string x)))
      (and (string=? (string-normalize-nfd s) s)
           (or (not (string=? (string-normalize-nfd (string p230 x)) (string p230 x)))
               (not (string=? (string-normalize-nfd (string x p230)) (string x p230)))
               (not (string=? (string-normalize-nfd (string x p220)) (string x p220)))
               (not (string=? (string-normalize-nfd (string p220 x)) (string p220 x))))))))
(define decomposing '()) (define marks '()) (define composites '())
(let loop ((n #x10FFFF))
  (when (>= n 0)
    (when (scalar? n)
      (let* ((s (string (integer->char n))))
        (cond ((nonstarter? (integer->char n)) (set! marks (cons n marks)))
              ((not (and (string=? (string-normalize-nfkd s) s))) (set! decomposing (cons n decomposing))))
        (when (and (not (string=? (string-normalize-nfd s) s)) (string=? (string-normalize-nfc s) s))
          (set! composites (cons n composites)))))
    (loop (- n 1))))
(define (vec l) (list->vector l))
(define decv (vec decomposing)) (define markv (vec marks)) (define compv (vec composites))
(define lat (vec (append (string->list "aeiouAEIOUnNcCsSzZyYdDlLgGkKtTrRwWbBpPmMhHjJqQxXfFvV 019") '())))
(define ligs (vec '(#xFB00 #xFB01 #xFB02 #xFB03 #xFB04 #xFB05 #xFB06 #xFDFA #x2126 #x212B #x2160 #x2460 #x3392 #xFF21 #xFF71 #xFE50
                   #x1D15E #x1D15F #x1D160 #x1D1BB #x2F800 #x2F801 #x2FA1D #x1F100 #x1F130 #x1D400 #x1D7CE #x1E900 #x1F1E6 #x0344 #x0F73 #x0F75 #x0958 #x09DC #x0B94 #x0BCA #x0BCB)))

;; a fixed LCG
(define seed 20261030)
(define (rnd n)
  (set! seed (mod (+ (* seed 6364136223846793005) 1442695040888963407) 18446744073709551616))
  (mod (div seed 65536) n))
(define (pick v) (vector-ref v (rnd (vector-length v))))
(define (random-scalar)
  (let ((n (case (rnd 4) ((0) (rnd #x3000)) ((1) (rnd #x10000)) ((2) (rnd #x30000)) (else (rnd #x110000)))))
    (if (scalar? n) n (+ n #x800))))
(define (one)
  (case (rnd 16)
    ((0 1) (char->integer (pick lat)))
    ((2 3 4) (pick decv))
    ((5 6 7 8) (pick markv))
    ((9) (+ #x1100 (rnd 19)))
    ((10) (+ #x1161 (rnd 21)))
    ((11) (+ #x11A8 (rnd 27)))
    ((12) (+ #xAC00 (* 28 (rnd 399)) (if (= 0 (rnd 3)) (rnd 28) 0)))
    ((13) (pick compv))
    ((14) (pick ligs))
    (else (random-scalar))))
(define (random-string)
  (let ((n (+ 1 (rnd 10))))
    (let loop ((i 0) (acc '()))
      (if (= i n) (list->string (map integer->char (reverse acc)))
          (loop (+ i 1) (cons (one) acc))))))
(let loop ((k 0))
  (when (< k 3500)
    (let ((s (random-string)))
      (let ((cps (map char->integer (string->list s))))
        (printf "S\t")
        (let pl ((l cps) (first #t))
          (unless (null? l)
            (unless first (display "."))
            (display (hex (car l)))
            (pl (cdr l) #f)))
        (for-each (lambda (f) (printf "\t~a" (pad8 (hash-add 0 (f s))))) forms)
        (newline)))
    (loop (+ k 1))))
