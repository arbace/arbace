; verdict: invalid Calling convention requires void return type
; rule: llvm/lib/IR/Verifier.cpp:3089 spir_kernel returning i32.
define spir_kernel i32 @f() { ret i32 0 }
