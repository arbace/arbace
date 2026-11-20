; verdict: invalid Attributes 'readnone and writeonly' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2128 readnone with writeonly on a ptr parameter.
define void @f(ptr readnone writeonly %p) { ret void }
