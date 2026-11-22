; verdict: invalid 'allocsize' number of elements argument must refer to an integer parameter
; rule: llvm/lib/IR/Verifier.cpp:2438 Element size param 0 is i32 (ok); param 1 is float.
define ptr @f(i32 %x, float %y) allocsize(0, 1) { ret ptr null }
