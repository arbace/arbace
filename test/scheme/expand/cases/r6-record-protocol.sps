(import (rnrs))
(define-record-type pare (fields kar kdr) (protocol (lambda (new) (lambda (a d) (new a (* 2 d))))))
(write (pare-kdr (make-pare 1 2)))
(newline)
