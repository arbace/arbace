#!r6rs
;;;; (scheme repl): interaction-environment is the environment of the R7RS libraries a program usually imports.
(library (scheme repl)
  (export interaction-environment)
  (import (rnrs) (rnrs eval))
  (define (interaction-environment)
    (environment '(scheme base) '(scheme char) '(scheme cxr) '(scheme inexact) '(scheme complex)
                 '(scheme lazy) '(scheme write) '(scheme read) '(scheme case-lambda) '(scheme time)
                 '(scheme process-context) '(scheme file))))
