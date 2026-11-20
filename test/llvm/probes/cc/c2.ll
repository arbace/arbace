; verdict: valid
; rule: - Callee fastcc vs call-site ccc mismatch is not checked by the verifier.
declare fastcc void @f()
define void @g() { call void @f()
 ret void }
