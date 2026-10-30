(import (rnrs))
(define-syntax case-n
  (lambda (x)
    (syntax-case x ()
      ((_ n) #`(list #,(+ (syntax->datum #'n) 1) 'n)))))
(write (case-n 5))
(newline)
