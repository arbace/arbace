(import (rnrs))
(define-syntax my-or (syntax-rules () ((_) #f) ((_ e) e) ((_ e r ...) (let ((t e)) (if t t (my-or r ...))))))
(let ((t 5)) (write (my-or #f t))) (newline)
