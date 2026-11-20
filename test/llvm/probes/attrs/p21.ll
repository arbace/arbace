; verdict: valid
; rule: - range on a vector of i32 with an i32 range: bit width matches the scalar size and isIntOrIntVectorTy(32) holds.
define void @f(<2 x i32> range(i32 0, 5) %p) { ret void }
