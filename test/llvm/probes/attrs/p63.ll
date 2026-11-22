; verdict: valid
; rule: - denormal-fp-math is a plain string attribute; the verifier does not parse it.
define void @f() "denormal-fp-math"="a,b" { ret void }
