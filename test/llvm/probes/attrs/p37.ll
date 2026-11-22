; verdict: invalid Attributes 'noinline and alwaysinline' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2361 noinline and alwaysinline: checked before the optnone rules.
define void @f() alwaysinline optnone noinline { ret void }
