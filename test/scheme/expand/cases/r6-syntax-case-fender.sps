(import (rnrs))
(define-syntax my-let
  (lambda (x)
    (syntax-case x ()
      ((_ ((i e) ...) b1 b2 ...)
       (for-all identifier? #'(i ...))
       #'((lambda (i ...) b1 b2 ...) e ...)))))
(write (my-let ((a 1) (b 2)) (+ a b)))
(newline)
