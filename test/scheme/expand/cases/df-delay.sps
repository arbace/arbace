(import (rnrs) (rnrs r5rs))
(define p (delay (begin (display "once ") 7)))
(write (list (force p) (force p)))
(newline)
