(import (rnrs))
(define p (cons 4 5))
(define-syntax p.car (identifier-syntax (car p)))
(set! p.car 15)
