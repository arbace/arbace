#!r6rs
;;;; (scheme write): R7RS-small's display, write, write-shared and write-simple: si's R7RS printer (SM8b, SC103 (b):
;;;; go/arbace/scm/write.ae's WriteR7RS, the primitives r7rs-write ... of (psyntax system $all)): datum labels for
;;;; cycles (write, display) and for shared structure (write-shared), none for write-simple; symbols between bars when
;;;; they would not read back; R7RS's string escapes, #u8( bytevectors. (SM8's version printed the labelled
;;;; structures here, over an eq-hashtable walk of every write.)
(library (scheme write)
  (export display write write-shared write-simple)
  (import (except (rnrs) display write)
          (only (psyntax system $all) r7rs-write r7rs-display r7rs-write-shared r7rs-write-simple))
  (define write (case-lambda ((x) (r7rs-write x)) ((x p) (r7rs-write x p))))
  (define display (case-lambda ((x) (r7rs-display x)) ((x p) (r7rs-display x p))))
  (define write-shared (case-lambda ((x) (r7rs-write-shared x)) ((x p) (r7rs-write-shared x p))))
  (define write-simple (case-lambda ((x) (r7rs-write-simple x)) ((x p) (r7rs-write-simple x p)))))
