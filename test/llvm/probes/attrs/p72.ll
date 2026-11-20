; verdict: valid
; rule: - allocsize(0,1) with two integer parameters is accepted.
define ptr @f(i32 %x, i64 %y) allocsize(0, 1) { ret ptr null }
