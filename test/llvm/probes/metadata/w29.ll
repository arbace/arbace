; verdict: out
; rule: - depends on LLVM's build: branch_weights with the string "x" second breaks hasBranchWeightOrigin's assertion that such a string is "expected" (llvm/lib/IR/ProfDataUtils.cpp:167); an assertions build aborts, a release build reads it as the origin marker and rejects the weights' count
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"branch_weights", !"x", i32 2}
