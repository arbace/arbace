;; SMR1: include and include-ci as expressions of (scheme base) (doc/scheme/MATURITY-REVIEW.md). Run from this directory:
;;   (cd test/scheme/si/include && ../../../../bin/si run include.sps)   prints the three lines of expected.txt
(import (scheme base) (scheme write) (scheme eval))
(include "inc.scm" "inc2.scm")
(include-ci "ci.scm")
(write (list Inc-Val (twice second) ci-x))
(newline)
(define (f)
  (include "inc2.scm")
  (+ second 1))
(write (f)) (newline)
(write (guard (e (#t 'missing)) (eval '(include "nonexistent.scm") (environment '(scheme base) '(scheme eval)))))
(newline)
