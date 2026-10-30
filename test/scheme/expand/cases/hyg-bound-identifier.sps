(import (rnrs))
(define-syntax ids (lambda (x) (syntax-case x () ((_ a b) (if (bound-identifier=? #'a #'b) #''same #''different)))))
(write (list (ids x x) (ids x y)))
(newline)
