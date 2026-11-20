; verdict: invalid Attribute 'optnone' requires 'noinline'!
; rule: llvm/lib/IR/Verifier.cpp:2365 optnone without noinline.
define void @f() optnone { ret void }
