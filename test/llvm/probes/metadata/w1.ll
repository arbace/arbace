; verdict: invalid Unfinished range!
; rule: llvm/lib/IR/Verifier.cpp:4472 The !range node !{i32 0} has one operand, an odd count; visitInstruction accepts !range on a load and verifyRangeLikeMetadata's first check fails.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i32 0}
