; verdict: invalid It should have at least one range!
; rule: llvm/lib/IR/Verifier.cpp:4474 !range !{} has 0 operands: the even-count check passes, then NumRanges == 0 fails.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{}
