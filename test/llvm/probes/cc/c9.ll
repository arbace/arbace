; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3124 ptx_kernel with an i32 argument is accepted (only the varargs rule applies to it).
define ptx_kernel void @f(i32 %x) { ret void }
