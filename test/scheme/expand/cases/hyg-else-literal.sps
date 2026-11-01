(import (rnrs))
(define-syntax my-if (syntax-rules (then else) ((_ c then t else e) (if c t e))))
(write (my-if #f then 1 else 2))
(newline)
