; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:2090 sret with inreg is the one allowed combination: they share one counting slot.
define void @f(ptr sret(i32) inreg %p) { ret void }
