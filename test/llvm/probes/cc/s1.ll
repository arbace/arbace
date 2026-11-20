; verdict: invalid calling convention does not permit calls
; rule: llvm/lib/IR/Verifier.cpp:3853 Call with spir_kernel: isCallableCC returns false for this convention.
declare void @g()
define void @f() {
 call spir_kernel void @g()
 ret void
}
