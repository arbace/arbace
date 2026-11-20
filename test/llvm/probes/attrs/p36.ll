; verdict: valid
; rule: - mustprogress, nounwind, willreturn, noreturn with unreachable body: no rule rejects it.
define void @f() mustprogress nounwind willreturn noreturn { unreachable }
