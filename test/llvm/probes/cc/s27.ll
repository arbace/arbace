; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3087 amdgpu_cs_chain with an inreg i1 argument is accepted (only whole-wave checks inreg).
define amdgpu_cs_chain void @f(i1 inreg %x) {
 ret void
}
