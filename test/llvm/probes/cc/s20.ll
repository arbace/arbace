; verdict: invalid Calling convention disallows byval
; rule: llvm/lib/IR/Verifier.cpp:3102 amdgpu_ps byval parameter.
define amdgpu_ps void @f(ptr byval(i32) %x) {
 ret void
}
