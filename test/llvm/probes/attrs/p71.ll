; verdict: invalid 'allocsize' element size argument is out of bounds
; rule: llvm/lib/IR/Verifier.cpp:2433 allocsize(2) with only one parameter.
define ptr @f(i32 %x) allocsize(2) { ret ptr null }
