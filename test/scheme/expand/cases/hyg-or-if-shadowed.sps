(import (rnrs))
(define-syntax my-or (syntax-rules () ((_) #f) ((_ e) e) ((_ e r ...) (let ((t e)) (if t t (my-or r ...))))))
(let ((if (lambda (a b c) 'shadowed))) (write (my-or #f 'b))) (newline)
