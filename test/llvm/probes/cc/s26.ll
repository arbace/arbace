; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3087 amdgpu_cs_chain with an i1 argument is accepted.
define amdgpu_cs_chain void @f(i1 %x) {
 ret void
}
