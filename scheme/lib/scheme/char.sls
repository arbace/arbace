#!r6rs
;;;; (scheme char): R7RS-small over (rnrs unicode); digit-value is new.
(library (scheme char)
  (export char-alphabetic? char-ci<=? char-ci<? char-ci=? char-ci>=? char-ci>? char-downcase char-foldcase
          char-lower-case? char-numeric? char-upcase char-upper-case? char-whitespace? digit-value
          string-ci<=? string-ci<? string-ci=? string-ci>=? string-ci>? string-downcase string-foldcase
          string-upcase)
  (import (rnrs) (only (rnrs r5rs) modulo))
  (define (digit-value c)
    (and (char-numeric? c)
         (let loop ((m (char->integer c)))
           ;; decimal digits come in runs of ten: the value is the distance from the start of the contiguous run
           (if (and (> m 0) (not (= m #xE000))
                    (char-numeric? (integer->char (- m 1))))
               (loop (- m 1))
               (modulo (- (char->integer c) m) 10)))))
)
