; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3081 x86_intrcc with no arguments passes (arg_empty).
define x86_intrcc void @f() {
 ret void
}
