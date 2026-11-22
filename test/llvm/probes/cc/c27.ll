; verdict: invalid inalloca isn't on the last parameter!
; rule: llvm/lib/IR/Verifier.cpp:2343 First of two inalloca params is not last; checked in verifyFunctionAttrs at i=0.
define void @f(ptr inalloca(i32) %p, ptr inalloca(i32) %q) { ret void }
