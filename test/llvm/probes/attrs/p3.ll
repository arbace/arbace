; verdict: invalid immarg attribute only applies to intrinsics
; rule: llvm/lib/IR/Verifier.cpp:2296 immarg on declared non-intrinsic; fires before verifyParameterAttrs.
declare void @g(i32 immarg noundef nonnull)
