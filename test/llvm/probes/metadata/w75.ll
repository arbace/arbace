; verdict: invalid scope list must consist of MDNodes
; rule: llvm/lib/IR/Verifier.cpp:5521 The alias.scope list !{i32 1} holds a constant, not an MDNode.
define void @f(ptr %p) {
 %v = load i32, ptr %p, !alias.scope !0
 ret void
}
!0 = !{i32 1}
