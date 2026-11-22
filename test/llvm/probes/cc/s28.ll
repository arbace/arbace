; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3087 amdgpu_cs_chain with no arguments is accepted.
define amdgpu_cs_chain void @f() {
 ret void
}
