; verdict: valid
; rule: - allockind("free") with allocptr parameter: valid.
define void @f(ptr allocptr %x) allockind("free") "alloc-family"="x" { ret void }
