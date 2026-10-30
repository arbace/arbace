(import (rnrs))
(define plus (case-lambda (() 0) ((x) x) ((x y) (+ x y)) ((x y . z) (apply plus (+ x y) z))))
(write (list (plus) (plus 1) (plus 1 2) (plus 1 2 3 4)))
(newline)
