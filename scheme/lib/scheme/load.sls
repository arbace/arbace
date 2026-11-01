#!r6rs
(library (scheme load)
  (export load)
  (import (rnrs) (rnrs eval) (scheme repl))
  (define (load name . env)
    (let ((e (if (pair? env) (car env) (interaction-environment))))
      (call-with-input-file name
        (lambda (p) (let loop ((x (read p))) (unless (eof-object? x) (eval x e) (loop (read p)))))))))
