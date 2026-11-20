; verdict: invalid Attribute 'zeroext' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 zeroext on float: only integer (or byte) types may carry zeroext.
define void @f(float zeroext %p) { ret void }
