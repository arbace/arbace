; verdict: invalid !prof branch_weights are not allowed for this instruction
; rule: llvm/lib/IR/Verifier.cpp:5288 An add has no expected weight count (0), so this CheckFailed prints first ('Wrong number of operands' follows).
define i32 @f(i32 %x) {
 %y = add i32 %x, 1, !prof !0
 ret i32 %y
}
!0 = !{!"branch_weights", i32 1}
