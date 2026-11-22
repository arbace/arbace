; verdict: invalid incorrect number of operands in module flag
; rule: llvm/lib/IR/Verifier.cpp:1914 The flag !{i32 1, !"wchar_size"} has 2 operands; the AutoUpgrade passes skip it (they need 3), so visitModuleFlag's first check fires.
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"wchar_size"}
