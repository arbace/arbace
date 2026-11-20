; verdict: invalid Attributes 'optsize and optnone' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2368 optnone with noinline passes, then optsize is rejected before minsize.
define void @f() optnone optsize noinline { ret void }
