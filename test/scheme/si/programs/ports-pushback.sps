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
;; N4: fresh-line after an extraction, the length of string ports, nonblocking mode of file ports
(let-values ([(p ex) (open-string-output-port)])
  (put-string p "a") (ex) (fresh-line p) (put-string p "z") (show 'fl (ex)))
(let ([sp (lambda (tag n init)
            (let-values ([(p ex) (open-string-output-port)])
              (put-string p init) (set-port-position! p n) (fresh-line p) (put-string p "Z") (show tag (ex))))])
  (sp 'fl-end 2 "ab")
  (sp 'fl-mid 1 "ab"))
(show 'len (port-has-port-length? (open-string-input-port "ab")) (port-length (open-string-input-port "abc")))
(let ([name "ports-pushback.tmp"])
  ;; the scratch file is deleted even when a line raises (re-review 044, M5)
  (dynamic-wind
    (lambda () #f)
    (lambda ()
      (let ([o (open-output-file name 'replace)]) (put-string o "x") (close-port o))
      (let ([p (open-input-file name)])
        (show 'nb (port-has-port-nonblocking?? p) (port-nonblocking? p))
        (close-port p)))
    (lambda () (if (file-exists? name) (delete-file name)))))
