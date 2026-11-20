; verdict: invalid "warn-stack-size" takes an unsigned integer: x
; rule: llvm/lib/IR/Verifier.cpp:2235 x is not a base-ten unsigned integer.
define void @f() "warn-stack-size"="x" { ret void }
