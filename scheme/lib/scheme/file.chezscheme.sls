#!r6rs
;;;; (scheme file) for Chez Scheme 10.3.0 (SM8b, SC113 (b)): (scheme file)'s names from Chez's (rnrs).
(library (scheme file)
  (export call-with-input-file call-with-output-file delete-file file-exists? open-binary-input-file
          open-binary-output-file open-input-file open-output-file with-input-from-file with-output-to-file)
  (import (rnrs))
  (define (open-binary-input-file name) (open-file-input-port name))
  (define (open-binary-output-file name) (open-file-output-port name (file-options no-fail))))
