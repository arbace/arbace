; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:5291 branch_weights with two i32 weights on a conditional br with 2 successors: the count matches and each weight is a ConstantInt.
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"branch_weights", i32 1, i32 2}
