; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 ptx_kernel is in the varargs-restricted group.
define ptx_kernel void @f(...) {
 ret void
}
