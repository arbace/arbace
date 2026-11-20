; verdict: valid
; rule: - noalias with nest on a ptr parameter: only nest counted in the exclusion group.
define void @f(ptr noalias nest) { ret void }
