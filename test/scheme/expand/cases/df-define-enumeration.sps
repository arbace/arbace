(import (rnrs))
(define-enumeration color (black white purple maroon) color-set)
(write (list (color black) (enum-set->list (color-set maroon white))))
(newline)
