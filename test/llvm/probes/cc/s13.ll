; verdict: invalid Function declaration shouldn't have a personality routine
; rule: llvm/lib/IR/Verifier.cpp:3212 personality ptr null on a declaration: the parser sets a non-null Constant so hasPersonalityFn is true.
declare void @f() personality ptr null
