(import (rnrs))
(write (let-values (((a b) (values 1 2)) ((c . d) (values 3 4 5))) (list a b c d)))
(write (let*-values (((a b) (values 1 2)) ((c) (values (+ a b)))) (list a b c)))
(newline)
