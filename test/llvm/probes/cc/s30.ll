; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 coldcc is in the varargs-restricted group.
define coldcc void @f(...) {
 ret void
}
