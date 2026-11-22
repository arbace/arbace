; verdict: valid
; rule: - nofree, nosync, willreturn: no rule.
define void @f() nofree nosync willreturn { ret void }
