; verdict: valid
; rule: - Caller and callee conventions need not match; the verifier has no such check for non-musttail calls.
define void @f() { ret void }
define void @g() { call fastcc void @f()
 ret void }
