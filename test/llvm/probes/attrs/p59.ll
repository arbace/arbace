; verdict: invalid "patchable-function-entry" takes an unsigned integer: x
; rule: llvm/lib/IR/Verifier.cpp:2235 x is not a base-ten unsigned integer.
define void @f() "patchable-function-entry"="x" { ret void }
