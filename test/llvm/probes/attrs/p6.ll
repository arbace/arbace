; verdict: invalid Attributes 'inalloca and readonly' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2104 inalloca with readonly; count rule passes (one group).
define void @f(ptr inalloca(i32) readonly %p) { ret void }
