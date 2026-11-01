#!r6rs
;;;; (scheme time) for Chez Scheme 10.3.0 (SM8b, SC113 (b)): over Chez's current-time; a jiffy is a microsecond.
(library (scheme time)
  (export current-jiffy current-second jiffies-per-second)
  (import (rnrs) (only (chezscheme) current-time time-second time-nanosecond))
  (define (jiffies-per-second) 1000000)
  (define (now-us) (let ((t (current-time))) (+ (* (time-second t) 1000000) (div (time-nanosecond t) 1000))))
  (define (current-second) (inexact (/ (now-us) 1000000)))
  (define (current-jiffy) (now-us)))
