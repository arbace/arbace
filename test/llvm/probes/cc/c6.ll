; verdict: invalid Calling convention disallows byval
; rule: llvm/lib/IR/Verifier.cpp:3102 amdgpu_kernel byval param (loop is skipped only for SPIR_KERNEL).
define amdgpu_kernel void @f(ptr byval(i32) %x) { ret void }
