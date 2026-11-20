; verdict: invalid Wrong number of operands
; rule: llvm/lib/IR/Verifier.cpp:5291 One weight for a 2-successor br: the node has fewer than 3 operands so no origin marker is assumed (offset 1), giving 1 weight, not 2.
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"branch_weights", i32 1}
