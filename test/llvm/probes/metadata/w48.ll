; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:1947 Behavior 7 is Max, which only needs a ConstantInt; a negative value is allowed for max.
!llvm.module.flags = !{!0}
!0 = !{i32 7, !"a", i32 -4}
