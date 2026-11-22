; verdict: invalid Attributes 'noinline and alwaysinline' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2361 Function attrs noinline and alwaysinline together.
define void @f() noinline alwaysinline { ret void }
