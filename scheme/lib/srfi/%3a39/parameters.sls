#!r6rs
;;;; [R6RS port] SRFI 39 (srfi :39 parameters) for Arbace's Scheme (si) and Chez Scheme: the reference implementation
;;;; printed in the "Implementation" section of the SRFI document (https://srfi.schemers.org/srfi-39/srfi-39.html, file
;;;; srfi-39.html of https://github.com/scheme-requests-for-implementation/srfi-39 at commit
;;;; 840e8d7e8b3bc4fbbc0cf8c78ce52dde42211702; the repository has no other reference source), taken from the page as
;;;; printed (HTML tags removed, entities decoded) and wrapped as an R6RS library; nothing else is changed but
;;;; set-car!/set-cdr! coming from (rnrs mutable-pairs).
;;;; Copyright (C) Marc Feeley 2002. All Rights Reserved.
;;;; Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
;;;; documentation files (the "Software"), to deal in the Software without restriction, including without limitation
;;;; the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
;;;; and to permit persons to whom the Software is furnished to do so, subject to the conditions of the MIT license
;;;; (the full text is in LICENSE.md, "SRFI 39"), the above copyright notice and this permission notice included.
(library (srfi :39 parameters)
  (export make-parameter parameterize)
  (import (rnrs) (rnrs mutable-pairs))

(define make-parameter
  (lambda (init . conv)
    (let ((converter
           (if (null? conv) (lambda (x) x) (car conv))))
      (let ((global-cell
             (cons #f (converter init))))
        (letrec ((parameter
                  (lambda new-val
                    (let ((cell (dynamic-lookup parameter global-cell)))
                      (cond ((null? new-val)
                             (cdr cell))
                            ((null? (cdr new-val))
                             (set-cdr! cell (converter (car new-val))))
                            (else ; this case is needed for parameterize
                             (converter (car new-val))))))))
          (set-car! global-cell parameter)
          parameter)))))

(define-syntax parameterize
  (syntax-rules ()
    ((parameterize ((expr1 expr2) ...) body ...)
     (dynamic-bind (list expr1 ...)
                   (list expr2 ...)
                   (lambda () body ...)))))

(define dynamic-bind
  (lambda (parameters values body)
    (let* ((old-local
            (dynamic-env-local-get))
           (new-cells
            (map (lambda (parameter value)
                   (cons parameter (parameter value #f)))
                 parameters
                 values))
           (new-local
            (append new-cells old-local)))
      (dynamic-wind
        (lambda () (dynamic-env-local-set! new-local))
        body
        (lambda () (dynamic-env-local-set! old-local))))))

(define dynamic-lookup
  (lambda (parameter global-cell)
    (or (assq parameter (dynamic-env-local-get))
        global-cell)))

(define dynamic-env-local '())

(define dynamic-env-local-get
  (lambda () dynamic-env-local))

(define dynamic-env-local-set!
  (lambda (new-env) (set! dynamic-env-local new-env)))
)
