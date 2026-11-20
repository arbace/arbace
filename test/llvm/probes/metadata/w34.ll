; verdict: invalid !prof branch_weights are not allowed for this instruction
; rule: llvm/lib/IR/Verifier.cpp:5288 ret is not a conditional br, switch, call, indirectbr, select or callbr, so the expected count is 0 and this CheckFailed comes first.
define void @f() {
 ret void, !prof !0
}
!0 = !{!"branch_weights", i32 1}
