; verdict: invalid calling convention does not permit calls
; rule: llvm/lib/IR/Verifier.cpp:3853 Call with amdgpu_cs_chain: isCallableCC false.
declare void @g()
define void @f() {
 call amdgpu_cs_chain void @g()
 ret void
}
