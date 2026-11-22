; verdict: invalid Attributes 'byval', 'inalloca', 'preallocated', 'inreg', 'nest', 'byref', and 'sret' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2098 byval counts as one group and inreg (sret/inreg group) as another: count is 2.
define void @f(ptr byval(i32) inreg %p) { ret void }
