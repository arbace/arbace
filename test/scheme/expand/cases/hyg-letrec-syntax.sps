(import (rnrs))
(write (letrec-syntax ((my-and (syntax-rules () ((_) #t) ((_ e) e) ((_ e r ...) (if e (my-and r ...) #f)))))
  (let ((if list)) (my-and 1 2 3))))
(newline)
