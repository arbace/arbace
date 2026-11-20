; verdict: valid
; rule: - allocalign with alloc-family string: no rule.
define ptr @f(i32 allocalign %x) "alloc-family"="x" { ret ptr null }
