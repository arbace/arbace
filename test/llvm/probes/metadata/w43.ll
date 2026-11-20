; verdict: invalid invalid ID operand in module flag (expected metadata string)
; rule: llvm/lib/IR/Verifier.cpp:1926 Behavior 1 is valid; the ID operand is i32 5, not an MDString.
!llvm.module.flags = !{!0}
!0 = !{i32 1, i32 5, i32 4}
