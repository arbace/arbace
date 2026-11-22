; verdict: invalid expected either branch_weights or VP profile name
; rule: llvm/lib/IR/Verifier.cpp:5331 The !prof name "bogus" is a string with enough operands but is neither "unknown", "branch_weights" nor "VP", so the final CheckFailed fires.
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"bogus", i32 1, i32 2}
