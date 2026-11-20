; verdict: valid
; rule: - allockind("free") alone is a valid kind; alloc-family and allocptr are not cross-checked.
define ptr @f(ptr allocptr %x) "alloc-family"="x" allockind("free") { ret ptr null }
