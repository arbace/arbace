; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3081 byval first parameter satisfies the x86_intrcc check.
define x86_intrcc void @f(ptr byval(i32) %p, i64 %q) {
 ret void
}
