; verdict: invalid Calling convention does not allow sret
; rule: llvm/lib/IR/Verifier.cpp:3097 amdgpu_ps with an sret parameter (void return so the parser accepts it).
define amdgpu_ps void @f(ptr sret(i32) %x) {
 ret void
}
