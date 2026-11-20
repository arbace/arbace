; verdict: invalid Attribute 'jumptable' requires 'unnamed_addr'
; rule: llvm/lib/IR/Verifier.cpp:2427 jumptable on a function without unnamed_addr.
define void @f() jumptable { ret void }
