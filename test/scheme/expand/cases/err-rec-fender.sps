(import (rnrs))
(define-syntax rec
  (lambda (x)
    (syntax-case x ()
      ((_ x e) (identifier? #'x) #'(letrec ((x e)) x)))))
(rec 5 (lambda (x) x))
