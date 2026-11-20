; verdict: invalid Intervals are contiguous
; rule: llvm/lib/IR/Verifier.cpp:4514 [5,8) does not overlap [0,5) and starts higher, but its lower bound equals the previous upper bound.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i32 0, i32 5, i32 5, i32 8}
