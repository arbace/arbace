; verdict: valid
; rule: - stack-probe-size is not validated by the verifier.
define void @f() "stack-probe-size"="x" { ret void }
