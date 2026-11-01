#!r6rs
;;;; (scheme write) for Chez Scheme 10.3.0 (SM8b, SC113 (b)): SM8's (scheme write), the labelled printer in Scheme over
;;;; Chez's write (si's is si's R7RS printer since SM8b, SC103 (b)). The rest of this header is SM8's.
;;;; (scheme write): R7RS-small's display, write, write-shared and write-simple over (rnrs io simple), SM8.
;;;; si's own write and display do not stop on a cyclic list or vector, which R7RS's write must, with datum labels
;;;; (#0=(1 . #0#)), and write-shared must label every shared pair and vector. These four therefore look for a cycle
;;;; (write, display) or sharing (write-shared) first and use si's primitive when there is none; otherwise the
;;;; structure is printed here, atoms by the primitive. write-simple is the primitive (it may not stop on a cycle,
;;;; as R7RS allows). The labelled printer does not abbreviate (quote x) as 'x.
(library (scheme write)
  (export display write write-shared write-simple)
  (import (rename (rnrs) (display r6:display) (write r6:write)))

  (define (compound? x) (or (pair? x) (and (vector? x) (> (vector-length x) 0))))

  ;; the compound objects needing a label: those reached again while still being printed (cycles), or with
  ;; shared? every one reached twice
  (define (find-labels x shared?)
    (let ((state (make-eq-hashtable)) (labels (make-eq-hashtable)) (any #f))
      (define (visit x)
        (when (compound? x)
          (let ((s (hashtable-ref state x #f)))
            (cond ((eq? s 'active) (hashtable-set! labels x #t) (set! any #t))
                  ((eq? s 'done) (when shared? (hashtable-set! labels x #t) (set! any #t)))
                  (else
                   (hashtable-set! state x 'active)
                   (if (pair? x)
                       (let loop ((p x))
                         ;; walk the spine iteratively; the car recursively
                         (visit (car p))
                         (let ((d (cdr p)))
                           (cond ((and (pair? d) (not (hashtable-ref state d #f)))
                                  (hashtable-set! state d 'active) (set! spine (cons d spine)) (loop d))
                                 (else (visit d)))))
                       (let loop ((i 0))
                         (when (< i (vector-length x)) (visit (vector-ref x i)) (loop (+ i 1)))))
                   (hashtable-set! state x 'done))))))
      (define spine '())
      (visit x)
      (for-each (lambda (p) (hashtable-set! state p 'done)) spine)
      (and any labels)))

  (define (print-labeled x port labels atom-writer)
    (let ((numbers (make-eq-hashtable)) (next 0))
      (define (label-prefix x)
        ;; #n= on first sight, #n# after: returns #t when x was already printed
        (and (hashtable-ref labels x #f)
             (let ((n (hashtable-ref numbers x #f)))
               (if n
                   (begin (r6:display "#" port) (r6:display n port) (r6:display "#" port) #t)
                   (begin (hashtable-set! numbers x next)
                          (r6:display "#" port) (r6:display next port) (r6:display "=" port)
                          (set! next (+ next 1))
                          #f)))))
      (define (pr x)
        (cond ((compound? x)
               (unless (label-prefix x)
                 (if (pair? x)
                     (begin
                       (r6:display "(" port)
                       (pr (car x))
                       (let loop ((d (cdr x)))
                         (cond ((null? d))
                               ((and (pair? d) (not (hashtable-ref labels d #f)))
                                (r6:display " " port) (pr (car d)) (loop (cdr d)))
                               (else (r6:display " . " port) (pr d))))
                       (r6:display ")" port))
                     (begin
                       (r6:display "#(" port)
                       (let loop ((i 0))
                         (when (< i (vector-length x))
                           (when (> i 0) (r6:display " " port))
                           (pr (vector-ref x i))
                           (loop (+ i 1))))
                       (r6:display ")" port)))))
              (else (atom-writer x port))))
      (pr x)))

;; a symbol R7RS writes between bars: empty, ".", with a character outside the identifier characters, or reading as a number
  (define (plain-char? c)
    (or (char-alphabetic? c) (char-numeric? c) (memv c (string->list "!$%&*/:<=>?^_~+-.@"))
        (and (> (char->integer c) 127) (not (char-whitespace? c)))))
  (define (needs-bars? s)
    (or (= (string-length s) 0) (string=? s ".")
        (not (for-all plain-char? (string->list s)))
        (string->number s)
        (char-numeric? (string-ref s 0))))
  (define (write-symbol x port)
    (let ((s (symbol->string x)))
      (if (needs-bars? s)
          (begin
            (r6:display "|" port)
            (for-each (lambda (c)
                        (cond ((char=? c #\|) (r6:display "\\|" port))
                              ((char=? c #\\) (r6:display "\\\\" port))
                              ((or (char<? c #\space) (char=? c #\x7f))
                               (r6:display "\\x" port) (r6:display (number->string (char->integer c) 16) port) (r6:display ";" port))
                              (else (r6:display c port))))
                      (string->list s))
            (r6:display "|" port))
          (r6:write x port))))
  (define (write-atom x port) (if (symbol? x) (write-symbol x port) (r6:write x port)))

  (define (port-arg o) (if (pair? o) (car o) (current-output-port)))

  (define (write x . o)
    (let ((labels (and (compound? x) (find-labels x #f))) (port (port-arg o)))
      (if labels (print-labeled x port labels write-atom) (write-atom x port))))
  (define (display x . o)
    (let ((labels (and (compound? x) (find-labels x #f))) (port (port-arg o)))
      (if labels (print-labeled x port labels r6:display) (r6:display x port))))
  (define (write-shared x . o)
    (let ((labels (and (compound? x) (find-labels x #t))) (port (port-arg o)))
      (if labels (print-labeled x port labels write-atom) (write-atom x port))))
  (define (write-simple x . o) (r6:write x (port-arg o))))
