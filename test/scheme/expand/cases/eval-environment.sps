(import (rnrs) (rnrs eval))
(write (eval '(let ((x 3)) (* x x)) (environment '(rnrs))))
(write (eval '(let-syntax ((m (syntax-rules () ((_ a) (list a a))))) (m 4)) (environment '(rnrs))))
(newline)
