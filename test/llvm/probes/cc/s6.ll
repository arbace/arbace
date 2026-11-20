; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3853 x86_intrcc is callable per isCallableCC; no other check on the call.
declare void @g()
define void @f() {
 call x86_intrcc void @g()
 ret void
}
