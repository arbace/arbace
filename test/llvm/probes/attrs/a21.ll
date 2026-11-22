; verdict: invalid Incompatible argument and return types for 'returned' attribute
; rule: llvm/lib/IR/Verifier.cpp:2315 i64 vs i32 are different non-vector types; canLosslesslyBitCastTo returns false.
define i32 @f(i64 returned %p) { ret i32 0 }
