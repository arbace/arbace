; verdict: invalid Attributes 'byval', 'inalloca', 'preallocated', 'inreg', 'nest', 'byref', and 'sret' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2098 byval and sret both present (nocapture becomes captures(none), readonly, noalias are fine): count is 2.
define void @f(ptr noalias nocapture byval(i32) readonly sret(i32) %p) { ret void }
