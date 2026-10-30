(import (rnrs))
(define-syntax my-let-values1
  (lambda (x)
    (syntax-case x ()
      ((_ (v ...) e b)
       (with-syntax (((t ...) (generate-temporaries #'(v ...))))
         #'(call-with-values (lambda () e) (lambda (t ...) (let ((v t) ...) b))))))))
(write (my-let-values1 (a b) (values 1 2) (list a b)))
(newline)
