; verdict: invalid incorrect number of operands in llvm.ident metadata
; rule: llvm/lib/IR/Verifier.cpp:1792 visitModuleIdents requires each llvm.ident entry to have exactly one operand; !{!"x", !"y"} has two.
!llvm.ident = !{!0}
!0 = !{!"x", !"y"}
