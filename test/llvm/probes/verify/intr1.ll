; verdict: invalid intrinsic has incorrect number of args. Expected 0, but got 1
; rule: llvm/lib/IR/Verifier.cpp:3299 Declaration llvm.trap(i32) visited before @f; call-site is not reached first.
declare void @llvm.trap(i32)
define void @f() {
  call void @llvm.trap(i32 1)
  ret void
}
