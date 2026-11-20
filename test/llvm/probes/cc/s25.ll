; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3087 amdgpu_cs_chain: void return, no sret/byval/etc; no further verifier rule.
define amdgpu_cs_chain void @f(i32 %x) {
 ret void
}
