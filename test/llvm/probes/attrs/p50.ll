; verdict: valid
; rule: - speculatable, readnone (as memory(none)), nounwind, willreturn: no rule.
define void @f() speculatable readnone nounwind willreturn { ret void }
