; verdict: invalid Attribute 'nonnull' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 nonnull on an i32 parameter: pointer-only attribute (safe-to-drop set).
define void @f(i32 nonnull %p) { ret void }
