; verdict: invalid invalid requirement on flag, flag is not present in module
; rule: llvm/lib/IR/Verifier.cpp:1894 The Require value {!"x", i32 4} is a valid pair and is queued; after all flags are visited no flag "x" is in SeenIDs.
!llvm.module.flags = !{!0}
!0 = !{i32 3, !"a", !{!"x", i32 4}}
