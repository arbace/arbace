; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3081 x86_intrcc with byval first parameter satisfies the check; extra parameters are not restricted by the verifier.
define x86_intrcc void @f(ptr byval(i32) %p, i64 %x, i64 %y) { ret void }
