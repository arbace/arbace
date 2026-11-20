; verdict: invalid invalid name for a VFABI variant: x
; rule: llvm/lib/IR/Verifier.cpp:2574 tryDemangleForVFABI requires the _ZGV prefix.
define void @f() "vector-function-abi-variant"="x" { ret void }
