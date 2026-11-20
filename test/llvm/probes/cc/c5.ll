; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3085 amdgpu_kernel void with an i32 argument: no restriction; AMDGPU module checks need an amdgpu triple.
define amdgpu_kernel void @f(i32 %x) { ret void }
