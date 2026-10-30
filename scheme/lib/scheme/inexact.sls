#!r6rs
;;;; (scheme inexact): R7RS-small; finite?, infinite? and nan? also take complex numbers (R6RS's take reals).
(library (scheme inexact)
  (export acos asin atan cos exp finite? infinite? log nan? sin sqrt tan)
  (import (except (rnrs) finite? infinite? nan?) (prefix (only (rnrs) finite? infinite? nan?) r6:))
  (define (finite? z) (and (r6:finite? (real-part z)) (r6:finite? (imag-part z))))
  (define (infinite? z) (or (r6:infinite? (real-part z)) (r6:infinite? (imag-part z))))
  (define (nan? z) (or (r6:nan? (real-part z)) (r6:nan? (imag-part z)))))
