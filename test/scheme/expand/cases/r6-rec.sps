(import (rnrs))
(define-syntax rec
  (lambda (x)
    (syntax-case x ()
      ((_ x e)
       (identifier? #'x)
       #'(letrec ((x e)) x)))))
(write (map (rec fact (lambda (n) (if (= n 0) 1 (* n (fact (- n 1)))))) '(1 2 3 4 5)))
(newline)
