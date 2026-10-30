#!r6rs
;;;; (scheme inexact): R7RS-small; finite?, infinite? and nan? also take complex numbers (R6RS's take reals).
;;;; sqrt (SM8b, SC101 (b)): on the branch cut, a negative real part with a negative zero imaginary part, R7RS's
;;;; (sqrt -1.0-0.0i) is 0.0+1.0i (continuous with quadrant II, as Chibi), where (rnrs)'s, as Chez's and C99's csqrt,
;;;; keeps the zero's sign (0.0-1.0i); si's R6RS sqrt stays Chez's.
(library (scheme inexact)
  (export acos asin atan cos exp finite? infinite? log nan? sin sqrt tan)
  (import (except (rnrs) finite? infinite? nan? sqrt) (prefix (only (rnrs) finite? infinite? nan? sqrt) r6:))
  (define (finite? z) (and (r6:finite? (real-part z)) (r6:finite? (imag-part z))))
  (define (infinite? z) (or (r6:infinite? (real-part z)) (r6:infinite? (imag-part z))))
  (define (nan? z) (or (r6:nan? (real-part z)) (r6:nan? (imag-part z))))
  (define (sqrt z)
    (if (and (not (real? z)) (eqv? (imag-part z) -0.0) (negative? (real-part z)))
        (r6:sqrt (make-rectangular (real-part z) 0.0))
        (r6:sqrt z))))
