; verdict: invalid Attributes 'sret and returned' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2110 sret together with returned on one parameter; the count rule passes (one group).
define void @f(ptr sret(i32) returned %p) { ret void }
