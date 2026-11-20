; verdict: valid
; rule: - preallocated on a definition's param has no verifier restriction for the C convention (only count/size checks).
define void @f(ptr preallocated(i32) %p) { ret void }
