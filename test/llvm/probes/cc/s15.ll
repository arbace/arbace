; verdict: invalid Function returns a token but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:3168 declare returning token.
declare token @f()
