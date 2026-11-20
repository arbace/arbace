; verdict: invalid invalid value for 'require' module flag (expected metadata pair)
; rule: llvm/lib/IR/Verifier.cpp:1957 Behavior 3 is Require; its value i32 4 is not an MDNode.
!llvm.module.flags = !{!0}
!0 = !{i32 3, !"a", i32 4}
