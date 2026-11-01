(import (rnrs) (rnrs mutable-pairs))
(define p (cons 4 5))
(define-syntax p.car
  (identifier-syntax
    (_ (car p))
    ((set! _ e) (set-car! p e))))
(set! p.car 15)
(write (list p.car p))
(newline)
