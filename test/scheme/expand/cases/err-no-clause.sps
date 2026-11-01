(import (rnrs))
(define-syntax foo (syntax-rules () ((_ a) a)))
(foo)
