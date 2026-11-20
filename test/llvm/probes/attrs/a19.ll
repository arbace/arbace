; verdict: invalid Attributes 'byval', 'inalloca', 'preallocated', 'inreg', 'nest', 'byref', and 'sret' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2098 inreg counts as the sret/inreg group and nest as another: count is 2.
define void @f(ptr inreg nest %p) { ret void }
