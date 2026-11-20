; verdict: valid
; rule: - minsize with optsize has no verifier rule (only optnone and optdebug are restricted).
define void @f() minsize optsize { ret void }
