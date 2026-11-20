; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 spir_kernel passes the void/sret checks, skips the arg loop, then fails the varargs check.
define spir_kernel void @f(...) { ret void }
