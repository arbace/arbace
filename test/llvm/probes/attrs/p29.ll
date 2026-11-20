; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:2314 returned ptr parameter, ptr return: same type, so canLosslesslyBitCastTo holds.
define ptr @f(ptr returned) { ret ptr null }
