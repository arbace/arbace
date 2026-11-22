; verdict: invalid invariant.load metadata is only for loads and readonly intrinsic calls
; rule: llvm/lib/IR/Verifier.cpp:5820 !invariant.load is on a store, which is neither a load nor a readonly intrinsic call.
define void @f(ptr %p) {
 store i32 0, ptr %p, !invariant.load !0
 ret void
}
!0 = !{}
