; verdict: invalid Calling convention disallows inalloca
; rule: llvm/lib/IR/Verifier.cpp:3106 amdgpu_ps inalloca parameter (last, so the attribute-list check passes first).
define amdgpu_ps void @f(ptr inalloca(i32) %x) {
 ret void
}
