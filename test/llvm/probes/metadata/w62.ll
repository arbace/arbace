; verdict: invalid invalid value for llvm.ident metadata entry operand(the operand should be a string)
; rule: llvm/lib/IR/Verifier.cpp:1794 The single operand is i32 1, not an MDString (the two string literals of the message concatenate with no space).
!llvm.ident = !{!0}
!0 = !{i32 1}
