#!r6rs
(library (scheme process-context)
  (export command-line emergency-exit exit get-environment-variable get-environment-variables)
  (import (rnrs) (srfi :98 os-environment-variables))
  (define (emergency-exit . code) (apply exit code)))
