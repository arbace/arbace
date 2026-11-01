(import (rnrs))
(define x 1)
(define-syntax var
  (make-variable-transformer
    (lambda (stx)
      (syntax-case stx (set!)
        (id (identifier? #'id) #'x)
        ((set! id e) #'(set! x (* 2 e)))
        ((id a ...) #'(x a ...))))))
(set! var 5)
(write (list var x))
(newline)
