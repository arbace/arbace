; verdict: invalid invalid value for 'append'-type module flag (expected a metadata node)
; rule: llvm/lib/IR/Verifier.cpp:1974 Behavior 6 is AppendUnique; its value !"x" is an MDString, not an MDNode.
!llvm.module.flags = !{!0}
!0 = !{i32 6, !"a", !"x"}
