; verdict: invalid Attributes 'minsize and optnone' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2371 optnone with noinline passes the first check, then minsize is rejected.
define void @f() optnone noinline minsize { ret void }
