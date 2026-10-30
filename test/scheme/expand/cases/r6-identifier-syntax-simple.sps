(import (rnrs))
(define-syntax ten (identifier-syntax 10))
(write (+ ten 1))
(newline)
