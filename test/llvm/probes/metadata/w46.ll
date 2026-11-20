; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:1931 Warning (2) behavior accepts any value; a single flag with a string ID.
!llvm.module.flags = !{!0}
!0 = !{i32 2, !"a", i32 4}
