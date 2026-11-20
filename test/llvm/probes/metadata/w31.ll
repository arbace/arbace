; verdict: invalid expected string with name of the !prof annotation
; rule: llvm/lib/IR/Verifier.cpp:5262 The first operand of the !prof node is i32 1, not an MDString.
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{i32 1, i32 2}
