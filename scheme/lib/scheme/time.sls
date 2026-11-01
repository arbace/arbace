#!r6rs
;;;; (scheme time): R7RS-small's clock over si's (SM8b, SC105 (b): current-microseconds of (psyntax system $all),
;;;; go/arbace/scm/clock.ae; SM8's read /proc/uptime). A jiffy is a microsecond since the epoch.
(library (scheme time)
  (export current-jiffy current-second jiffies-per-second)
  (import (rnrs) (only (psyntax system $all) current-microseconds))
  (define (jiffies-per-second) 1000000)
  (define (current-second) (inexact (/ (current-microseconds) 1000000)))
  (define (current-jiffy) (current-microseconds)))
