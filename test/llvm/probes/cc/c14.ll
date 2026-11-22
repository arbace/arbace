; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3081 x86_intrcc with byval first parameter satisfies the check; a second i64 is not restricted.
define x86_intrcc void @f(ptr byval(i32) %p, i64 %x) { ret void }
