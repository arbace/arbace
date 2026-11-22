; verdict: invalid llvm intrinsics cannot be defined!
; rule: llvm/lib/IR/Verifier.cpp:3217 define of llvm.trap: IsIntrinsic with a body.
define void @llvm.trap() {
  ret void
}
