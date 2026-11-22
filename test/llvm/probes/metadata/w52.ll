; verdict: invalid invalid value for 'require' module flag (expected metadata pair)
; rule: llvm/lib/IR/Verifier.cpp:1957 Require's value is the string !"x", not an MDNode.
!llvm.module.flags = !{!0}
!0 = !{i32 3, !"a", !"x"}
