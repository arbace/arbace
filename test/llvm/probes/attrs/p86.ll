; verdict: valid
; rule: - allockind("alloc,aligned") with allocalign i32 parameter and alloc-family: valid.
define ptr @f(i32 allocalign %x) allockind("alloc,aligned") "alloc-family"="x" { ret ptr null }
