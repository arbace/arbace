; verdict: invalid immarg attribute only applies to intrinsics
; rule: llvm/lib/IR/Verifier.cpp:2296 immarg on a declared non-intrinsic function.
declare void @g(ptr immarg noundef)
