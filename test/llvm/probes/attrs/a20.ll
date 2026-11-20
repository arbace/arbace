; verdict: invalid Incompatible argument and return types for 'returned' attribute
; rule: llvm/lib/IR/Verifier.cpp:2315 returned on an i64 parameter of a void function: void is not first class so canLosslesslyBitCastTo is false.
define void @f(i64 returned %p) { ret void }
