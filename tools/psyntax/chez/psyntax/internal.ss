;;; psyntax's internal library for the bootstrap host (Arbace's own; Ikarus's psyntax.internal.ss names
;;; the interface).
(library (psyntax internal)
  (export current-primitive-locations compile-core-expr-to-port expanded->core)
  (import (rnrs) (psyntax compat))
  (define (expanded->core x) x)
  (define (compile-core-expr-to-port x p) (write x p) (newline p)))
