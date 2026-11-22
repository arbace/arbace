; verdict: invalid calling convention does not permit calls
; rule: llvm/lib/IR/Verifier.cpp:3853 Call with ptx_kernel: isCallableCC false.
declare void @g()
define void @f() {
 call ptx_kernel void @g()
 ret void
}
