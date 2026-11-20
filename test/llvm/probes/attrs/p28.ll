; verdict: invalid Incompatible argument and return types for 'returned' attribute
; rule: llvm/lib/IR/Verifier.cpp:2315 returned ptr parameter on a void function.
define void @f(ptr returned) { ret void }
