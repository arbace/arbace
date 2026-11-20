; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3098 spir_kernel skips the byval loop and is void, non-varargs.
define spir_kernel void @f(ptr byval(i32) %x) {
 ret void
}
