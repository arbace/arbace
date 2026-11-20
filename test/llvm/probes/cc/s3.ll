; verdict: invalid calling convention does not permit calls
; rule: llvm/lib/IR/Verifier.cpp:3853 Call with amdgpu_ps: isCallableCC false.
declare void @g()
define void @f() {
 call amdgpu_ps void @g()
 ret void
}
