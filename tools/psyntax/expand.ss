;;; The reference's expansion of the expander's tests (doc/scheme/SCHEME-SURVEY.md, "As built: SM4"), on Chez
;;; Scheme 10.3.0: for each FILE that is a program without libraries, Chez's `expand` of (let () BODY ...) in the
;;; environment of its imports, written with gensyms as NAME.N. One map a line (bin/si test --record).
;;;
;;;   chez --script tools/psyntax/expand.ss FILE...

(import (chezscheme))

(define (read-all file)
  (with-input-from-file file
    (lambda () (let f () (let ((x (read))) (if (eof-object? x) '() (cons x (f))))))))

(print-gensym 'pretty/suffix)
(for-each
  (lambda (file)
    (let ((forms (read-all file)))
      (when (and (pair? forms) (pair? (car forms)) (eq? (caar forms) 'import)
                 (not (exists (lambda (x) (and (pair? x) (eq? (car x) 'library))) forms)))
        (let ((x (guard (c (#t #f))
                   (expand `(let () ,@(cdr forms)) (apply environment (cdar forms))))))
          (when x
            (display "{:file ") (write (path-root (path-last file)))
            (display " :expand ") (write (with-output-to-string (lambda () (write x))))
            (display "}") (newline))))))
  (command-line-arguments))
