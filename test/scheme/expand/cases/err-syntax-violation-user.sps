(import (rnrs))
(define-syntax m (lambda (x) (syntax-case x () ((_ a b) (syntax-violation 'm "bad operands" x #'b)))))
(m 1 2)
