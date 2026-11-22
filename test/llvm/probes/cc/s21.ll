; verdict: invalid Calling convention disallows preallocated
; rule: llvm/lib/IR/Verifier.cpp:3104 amdgpu_ps preallocated parameter.
define amdgpu_ps void @f(ptr preallocated(i32) %x) {
 ret void
}
