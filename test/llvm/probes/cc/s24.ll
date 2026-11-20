; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 amdgpu_ps falls through to the varargs check.
define amdgpu_ps void @f(...) {
 ret void
}
