; verdict: invalid Attributes 'readnone and readonly' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2122 readnone with readonly on a ptr parameter.
define void @f(ptr readnone readonly %p) { ret void }
