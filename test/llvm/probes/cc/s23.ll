; verdict: invalid Calling convention disallows stack byref
; rule: llvm/lib/IR/Verifier.cpp:3112 byref ptr in addrspace 0, which equals the default alloca address space.
define amdgpu_ps void @f(ptr byref(i32) %x) {
 ret void
}
