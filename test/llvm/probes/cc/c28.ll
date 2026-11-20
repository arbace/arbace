; verdict: invalid inalloca isn't on the last parameter!
; rule: llvm/lib/IR/Verifier.cpp:2343 inalloca on param 0 of 2 is not last; reported at i=0 before anything else.
define void @f(ptr inalloca(i32) %p, ptr byval(i32) %q) { ret void }
