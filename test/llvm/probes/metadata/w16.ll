; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:5838 !noundef !{} on a load (of any type) satisfies both noundef checks: a load instruction and an empty node.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !noundef !0
 ret i32 %v
}
!0 = !{}
