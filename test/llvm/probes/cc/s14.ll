; verdict: invalid Function takes token but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:3154 declare with a token parameter.
declare void @f(token)
