;; Chez's port procedures around unget-u8 and unget-char (re-review 033): pushed-back items, set-port-position!, a custom
;; binary port's get-bytevector-some, fresh-line, port-length; run by Chez and by si, the same answers (SCHEME-SURVEY.md,
;; "As built: re-review 033's fixes")
(import (chezscheme))
(define-syntax try (syntax-rules () [(_ e) (guard (c [#t 'err]) e)]))
(define (show . x) (write x) (newline))
;; N1: get-bytevector-some returns pushed-back bytes first
(let* ([p (open-bytevector-input-port #vu8(1 2 3 4 5))] [a (get-u8 p)] [b (try (unget-u8 p 1))] [c (get-bytevector-some p)] [d (get-u8 p)])
  (show 'n1 a c d))
;; N6: get-bytevector-some on a custom binary port calls read!
(let ([p (make-custom-binary-input-port "x" (lambda (bv s n) (bytevector-u8-set! bv s 0) 1) #f #f #f)])
  (show 'n6 (get-bytevector-some p)))
;; N2: set-port-position! discards pushed-back items
(let* ([p (open-bytevector-input-port #vu8(1 2 3 4 5))] [a (get-u8 p)] [b (try (unget-u8 p 1))] [c (set-port-position! p 3)] [d (get-u8 p)])
  (show 'n2 a d))
(let* ([p (open-string-input-port "abcde")] [a (read-char p)] [b (try (unget-char p #\a))] [c (set-port-position! p 2)] [d (read-char p)])
  (show 'n2t a d))
