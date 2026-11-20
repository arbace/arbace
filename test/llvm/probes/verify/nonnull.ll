; verdict: invalid nonnull applies only to pointer types
; rule: llvm/lib/IR/Verifier.cpp:5827 load i32 with !nonnull: result type is not a pointer.
define void @f(ptr %p) {
  %v = load i32, ptr %p, !nonnull !0
  ret void
}
!0 = !{}
