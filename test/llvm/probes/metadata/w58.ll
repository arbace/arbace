; verdict: valid
; rule: llvm/lib/IR/AutoUpgrade.cpp:6069 UpgradeDebugInfo reads the non-integer value as version 0, but the module has no debug info, so StripDebugInfo changes nothing and nothing is printed; the verifier has no rule on this flag's value and Error behavior accepts any value.
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"Debug Info Version", !"x"}
