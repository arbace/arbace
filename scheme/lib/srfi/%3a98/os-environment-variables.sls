#!r6rs
;;;; SRFI 98 (srfi :98 os-environment-variables) for Arbace's Scheme (si) and Chez Scheme.
;;;; SRFI 98 (https://srfi.schemers.org/srfi-98/, repository https://github.com/scheme-requests-for-implementation/srfi-98
;;;; at commit 8f28224c77be945bf5fd4d5e750831979f23adc4) has no portable reference implementation: its
;;;; "Implementation" section lists one line per Scheme (Gauche, Scheme48, scsh, SCM) binding the two procedures to the
;;;; system's. This library is therefore written for Arbace, not vendored: si has no environment primitive, so the
;;;; process's environment is read from /proc/self/environ (Linux: NUL-terminated NAME=value entries, UTF-8), the
;;;; environment the process was started with (a later setenv is not seen). Where /proc/self/environ cannot be read the
;;;; environment is empty. When si gets a getenv primitive (proposal in the SM6 report), only ENVIRON-ENTRIES changes.
;;;; Copyright (C) Taro Minowa (Higepon) (2008) for the specification, MIT license (LICENSE.md, "SRFI 98").
(library (srfi :98 os-environment-variables)
  (export get-environment-variable get-environment-variables)
  (import (rnrs))

  ;; the raw bytes of the environment, or an empty bytevector
  (define (environ-bytes)
    (guard (e (#t (make-bytevector 0)))
      (let* ((p (open-file-input-port "/proc/self/environ"))
             (bv (get-bytevector-all p)))
        (close-port p)
        (if (bytevector? bv) bv (make-bytevector 0)))))

  ;; the entries as a list of (name . value) in the environment's order; entries with no = or an empty name are dropped
  (define (environ-entries)
    (let* ((bv (environ-bytes)) (n (bytevector-length bv)))
      (let loop ((start 0) (i 0) (acc '()))
        (cond ((= i n)
               (reverse (if (> i start) (cons (entry bv start i) acc) acc)))
              ((= (bytevector-u8-ref bv i) 0)
               (loop (+ i 1) (+ i 1) (cons (entry bv start i) acc)))
              (else (loop start (+ i 1) acc))))
      ))

  ;; (entry bv start end): the pair for bytes [start, end), or #f made into a skipped entry by environ-list
  (define (entry bv start end)
    (let find ((j start))
      (cond ((= j end) #f)
            ((= (bytevector-u8-ref bv j) 61)               ; #\=
             (if (= j start)
                 #f
                 (cons (utf8->string (sub-bytes bv start j))
                       (utf8->string (sub-bytes bv (+ j 1) end)))))
            (else (find (+ j 1))))))

  (define (sub-bytes bv start end)
    (let ((r (make-bytevector (- end start))))
      (bytevector-copy! bv start r 0 (- end start))
      r))

  (define (environ-list)
    (let loop ((l (environ-entries)) (acc '()))
      (cond ((null? l) (reverse acc))
            ((car l) (loop (cdr l) (cons (car l) acc)))
            (else (loop (cdr l) acc)))))

  (define (get-environment-variable name)
    (if (not (string? name))
        (assertion-violation 'get-environment-variable "name must be a string" name)
        (let ((p (assoc name (environ-list))))
          (and p (cdr p)))))

  (define (get-environment-variables)
    (environ-list))
)
