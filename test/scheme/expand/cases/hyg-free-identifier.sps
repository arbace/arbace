(import (rnrs))
(define-syntax free= (lambda (x) (syntax-case x () ((_ a b) (if (free-identifier=? #'a #'b) #'#t #'#f)))))
(write (list (free= car car) (free= car cdr) (let ((car 1)) (free= car car))))
(newline)
