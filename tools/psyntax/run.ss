;;; The reference's runs of the expander's tests (doc/scheme/SCHEME-SURVEY.md, "As built: SM4"), on Chez Scheme
;;; 10.3.0: each FILE's leading library forms evaluated, then its program, its output captured; a raised
;;; condition's types, who, message, and &syntax's form and subform as Chez writes them. One map a line, read by
;;; arbace.scheme.expand (bin/si test --record).
;;;
;;;   chez --script tools/psyntax/run.ss FILE...

(import (chezscheme))

(define (read-all file)
  (with-input-from-file file
    (lambda () (let f () (let ((x (read))) (if (eof-object? x) '() (cons x (f))))))))

(define (string-of x) (with-output-to-string (lambda () (write x))))

(define (condition-line c)
  (define (kv k v) (display " ") (display k) (display " ") (write v))
  (display " :error {")
  (if (condition? c)
      (begin
        (display ":kinds [")
        (for-each (lambda (s) (write (symbol->string (record-type-name (record-rtd s)))) (display " "))
                  (simple-conditions c))
        (display "]")
        (when (who-condition? c) (kv ":who" (string-of (condition-who c))))
        (when (message-condition? c) (kv ":message" (condition-message c)))
        (when (syntax-violation? c)
          (kv ":form" (string-of (syntax->datum (syntax-violation-form c))))
          (kv ":subform" (string-of (syntax->datum (syntax-violation-subform c))))))
      (kv ":raised" (string-of c)))
  (display "}"))

(for-each
  (lambda (file)
    (let* ((forms (read-all file))
           (libs (filter (lambda (x) (and (pair? x) (eq? (car x) 'library))) forms))
           (prog (filter (lambda (x) (not (and (pair? x) (eq? (car x) 'library)))) forms))
           (port (open-output-string))
           (c (call/cc
                (lambda (k)
                  (with-exception-handler
                    (lambda (c) (k c))
                    (lambda ()
                      (parameterize ((current-output-port port))
                        (for-each (lambda (l) (eval l)) libs)
                        (eval `(top-level-program ,@prog)))
                      #f))))))
      (display "{:file ") (write (path-root (path-last file)))
      (display " :out ") (write (get-output-string port))
      (when c (condition-line c))
      (display "}") (newline)))
  (command-line-arguments))
