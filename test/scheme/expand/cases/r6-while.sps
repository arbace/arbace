(import (rnrs))
(define-syntax while
  (syntax-rules ()
    ((_ c b1 b2 ...) (let lp () (when c b1 b2 ... (lp))))))
(let ((i 0) (lp 'mine))
  (while (< i 3) (set! i (+ i 1)))
  (write (list i lp)))
(newline)
