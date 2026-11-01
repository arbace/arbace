(import (rnrs))
(write (list (and 1 2 'c '(f g)) (and) (or #f #f) (or (memq 'b '(a b c)) (/ 3 0)) (when (> 3 2) 'greater) (unless (> 3 2) 'less)))
(newline)
