; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 intel_ocl_bicc is in the varargs-restricted group.
define intel_ocl_bicc void @f(...) {
 ret void
}
