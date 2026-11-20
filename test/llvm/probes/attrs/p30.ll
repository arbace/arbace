; verdict: invalid Incompatible argument and return types for 'returned' attribute
; rule: llvm/lib/IR/Verifier.cpp:2315 ptr vs i64 are different non-vector types.
define i64 @f(ptr returned) { ret i64 0 }
