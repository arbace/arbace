; verdict: invalid invalid value for 'append'-type module flag (expected a metadata node)
; rule: llvm/lib/IR/Verifier.cpp:1974 Behavior 5 (Append) with the value i32 2, not an MDNode.
!llvm.module.flags = !{!0}
!0 = !{i32 5, !"a", i32 2}
