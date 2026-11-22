; verdict: invalid invalid value for 'append'-type module flag (expected a metadata node)
; rule: llvm/lib/IR/Verifier.cpp:1974 Behavior 5 is Append, whose value must be an MDNode; i32 -4 is a constant.
!llvm.module.flags = !{!0}
!0 = !{i32 5, !"a", i32 -4}
