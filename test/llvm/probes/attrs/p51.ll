; verdict: valid
; rule: - noreturn on a function that returns is not checked by the verifier.
define void @f() noreturn { ret void }
