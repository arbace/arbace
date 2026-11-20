; verdict: valid
; rule: - allocsize(0) with an i32 parameter is accepted.
define ptr @f(i32 %x) allocsize(0) { ret ptr null }
