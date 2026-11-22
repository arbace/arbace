; verdict: invalid module flag identifiers must be unique (or of 'require' type)
; rule: llvm/lib/IR/Verifier.cpp:1985 Two Error flags named "a": inserting the second into SeenIDs fails.
!llvm.module.flags = !{!0, !1}
!0 = !{i32 1, !"a", i32 4}
!1 = !{i32 1, !"a", i32 5}
