; verdict: invalid invariant.group metadata is only for loads and stores
; rule: llvm/lib/IR/Verifier.cpp:5814 !invariant.group is attached to an add, which is neither a load nor a store; nothing earlier in visitInstruction fails.
define i32 @f(i32 %p) {
 %v = add i32 %p, 1, !invariant.group !0
 ret i32 %v
}
!0 = !{}
