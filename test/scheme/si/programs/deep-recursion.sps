;; non-tail recursion 10^6 deep: bounded by memory, not Go's stack
(import (rnrs))
(define (sum n) (if (= n 0) 0 (+ n (sum (- n 1)))))
(display (sum 1000000)) (newline)
(define (build n) (if (= n 0) '() (cons n (build (- n 1)))))
(display (length (build 1000000))) (newline)
(display (apply + (map (lambda (x) 1) (build 1000000)))) (newline)
