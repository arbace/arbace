(import (rnrs))
(define-syntax my-let* (syntax-rules () ((_ () b ...) (let () b ...)) ((_ ((x v) r ...) b ...) (let ((x v)) (my-let* (r ...) b ...)))))
(write (my-let* ((a 1) (b (+ a 1)) (c (* b 2))) (list a b c)))
(newline)
