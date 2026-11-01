;;; tools/scheme/normdata.ss: the data of si's Unicode normalization (SM6), dumped from Chez Scheme 10.3.0 itself.
;;;
;;;   chez --script tools/scheme/normdata.ss > RAW
;;;   python3 -I tools/scheme/normdata.py RAW go/arbace/scm/normdata.ae        (attaches the numeric classes, verifies, writes the table)
;;;
;;; For every scalar value (U+0000..U+10FFFF but the surrogates), from Chez's own string-normalize-nfd/nfkd/nfc on
;;; one-character strings, one line each (hex, no prefix):
;;;   D CP X..    the full canonical decomposition, when nfd(CP) is not CP (Hangul syllables left out: algorithmic)
;;;   K CP X..    the full compatibility decomposition, when nfkd(CP) differs from nfd(CP)
;;;   C CP A B    CP is a primary composite (nfd(CP) is not CP and nfc(CP) is CP, Hangul left out): its pair, A the nfc of
;;;               the decomposition but the last character, B the last (checked: nfc of the pair is CP)
;;;   N R CP      CP, not decomposable, has a canonical combining class > 0, of order R (1, 2, ..: the classes sorted
;;;               by Chez's own reordering: the characters of a class reorder against no other of it)
;;; Chez has no accessor for the class: it is derived from nfd's canonical ordering. Of two non-decomposable
;;; characters x y, nfd(x y) = y x exactly when class(x) > class(y). A character with class > 0 is found by
;;; two probes (U+0301, class 230, and U+0323, 220: x reorders against one of them unless its class is 0 or
;;; 230 ... and 230 is caught by U+0323). The classes' true numbers are attached afterwards (normdata.py).
(import (rnrs) (only (chezscheme) printf sort))

(define (str . cs) (apply string cs))
(define (scalar? n) (not (and (>= n #xD800) (<= n #xDFFF))))
(define (hangul? n) (and (>= n #xAC00) (<= n #xD7A3)))
(define (hex n) (string-upcase (number->string n 16)))
(define (cps s) (map char->integer (string->list s)))
(define (emit tag . ns)
  (display tag)
  (for-each (lambda (n) (display " ") (display (hex n))) ns)
  (newline))
(define (emit-list tag n l)
  (apply emit tag n l))

(define p230 (integer->char #x301))
(define p220 (integer->char #x323))

;; non-decomposable x with class > 0
(define (nonstarter? x)
  (let ((s (string x)))
    (and (not (string=? (string-normalize-nfd s) "")) ; always
         (or (not (string=? (string-normalize-nfd (str p230 x)) (str p230 x)))
             (not (string=? (string-normalize-nfd (str x p230)) (str x p230)))
             (not (string=? (string-normalize-nfd (str x p220)) (str x p220)))
             (not (string=? (string-normalize-nfd (str p220 x)) (str p220 x)))))))

(define nonstarters '())

(let loop ((n 0))
  (when (<= n #x10FFFF)
    (when (scalar? n)
      (let* ((c (integer->char n))
             (s (string c))
             (d (string-normalize-nfd s))
             (k (string-normalize-nfkd s))
             (nc (string-normalize-nfc s)))
        (unless (hangul? n)
          (if (not (string=? d s))
              (begin
                (emit-list "D" n (cps d))
                (when (string=? nc s)
                  (let* ((ds (cps d)) (len (length ds)))
                    (when (< len 2) (error 'normdata "composite with a short decomposition" n))
                    (let* ((last (list-ref ds (- len 1)))
                           (pre (string-normalize-nfc (list->string (map integer->char (list-head ds (- len 1)))))))
                      (unless (= (string-length pre) 1) (error 'normdata "prefix does not compose to one" n))
                      (unless (string=? (string-normalize-nfc (str (string-ref pre 0) (integer->char last))) s)
                        (error 'normdata "pair does not compose" n))
                      (emit "C" n (char->integer (string-ref pre 0)) last)))))
              (when (nonstarter? c)
                (set! nonstarters (cons c nonstarters))))
          (unless (string=? k d)
            (emit-list "K" n (cps k))))))
    (loop (+ n 1))))

(define (list-head l n) (if (= n 0) '() (cons (car l) (list-head (cdr l) (- n 1)))))

;; the classes' order: sort by class, then group the equal ones. cmp is -1, 0, 1 by class(x) against class(y)
(define (cmp x y)
  (if (char=? x y)
      0
      (cond
        ((string=? (string-normalize-nfd (str x y)) (str y x)) 1)      ; swapped: class(x) > class(y)
        ((string=? (string-normalize-nfd (str y x)) (str x y)) -1)     ; y x swapped: class(y) > class(x)
        (else 0))))

(let* ((sorted (sort (lambda (x y) (< (cmp x y) 0)) nonstarters)))
  (let loop ((l sorted) (rank 0) (prev #f))
    (unless (null? l)
      (let* ((c (car l))
             (r (if (and prev (= (cmp prev c) 0)) rank (+ rank 1))))
        (emit "N" r (char->integer c))
        (loop (cdr l) r c)))))
