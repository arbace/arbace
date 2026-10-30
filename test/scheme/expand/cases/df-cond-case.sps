(import (rnrs))
(write (list (cond ((assv 2 '((1 . a) (2 . b))) => cdr) (else 'no))
             (case (* 2 3) ((2 3 5 7) 'prime) ((1 4 6 8 9) 'composite) (else 'other))))
(newline)
