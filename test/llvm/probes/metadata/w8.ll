; verdict: invalid Intervals are overlapping
; rule: llvm/lib/IR/Verifier.cpp:4510 The second interval [3,4) intersects the first [0,5).
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i32 0, i32 5, i32 3, i32 4}
