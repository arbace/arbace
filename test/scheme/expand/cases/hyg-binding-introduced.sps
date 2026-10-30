(import (rnrs))
(let-syntax ((m (lambda (x) (syntax-case x () ((_ e) #'(let ((t 1)) e))))))
  (let ((t 2)) (write (m t))))
(newline)
