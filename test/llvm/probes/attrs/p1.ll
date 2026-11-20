; verdict: invalid immarg attribute only applies to intrinsics
; rule: llvm/lib/IR/Verifier.cpp:2296 immarg on a non-intrinsic parameter; fires before verifyParameterAttrs and its immarg compatibility rule.
define void @f(ptr immarg nonnull %p) { ret void }
