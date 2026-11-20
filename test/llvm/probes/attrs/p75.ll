; verdict: invalid 'allocsize' element size argument must refer to an integer parameter
; rule: llvm/lib/IR/Verifier.cpp:2438 allocsize(0) on a float parameter.
define float @f(float %x) allocsize(0) { ret float 0.0 }
