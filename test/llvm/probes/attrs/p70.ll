; verdict: invalid 'allocsize' element size argument is out of bounds
; rule: llvm/lib/IR/Verifier.cpp:2433 allocsize(0,1) on a function with no parameters.
define void @f() allocsize(0, 1) { ret void }
