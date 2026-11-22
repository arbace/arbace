; verdict: invalid noundef applies only to load instructions
; rule: llvm/lib/IR/Verifier.cpp:5837 !noundef is attached to a store; the load-only check is the first of the noundef block.
define void @f(ptr %p) {
 store i32 0, ptr %p, !noundef !0
 ret void
}
!0 = !{}
