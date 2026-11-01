(import (rnrs))
(define-syntax swap! (syntax-rules () ((_ a b) (let ((tmp a)) (set! a b) (set! b tmp)))))
(let ((tmp 1) (other 2)) (swap! tmp other) (write (list tmp other)))
(newline)
