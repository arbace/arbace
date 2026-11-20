; verdict: invalid invalid value for 'min' module flag (expected constant non-negative integer)
; rule: llvm/lib/IR/Verifier.cpp:1939 At the end of parsing, UpgradeModuleFlags (llvm/lib/IR/AutoUpgrade.cpp:6525) rewrites a "PIC Level" flag with Error behavior to Min; the verifier then sees a Min flag whose value !"x" is not a ConstantInt.
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"PIC Level", !"x"}
