; verdict: valid
; rule: - range(i32 0, 0) is accepted by the parser (0,0 allowed) and the verifier only checks the bit width, which matches i32.
define void @f(i32 range(i32 0, 0) %p) { ret void }
