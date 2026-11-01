(import (rnrs))
(define-syntax with-it (lambda (x) (syntax-case x () ((k e body) (with-syntax ((it (datum->syntax #'k 'it))) #'(let ((it e)) body))))))
(write (with-it 42 (+ it 1)))
(newline)
