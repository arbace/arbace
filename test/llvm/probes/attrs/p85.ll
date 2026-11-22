; verdict: valid
; rule: - allockind("alloc") without alloc-family: no verifier rule.
define void @f() allockind("alloc") { ret void }
