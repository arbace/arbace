; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:1931 Error behavior accepts any value; the verifier does not range-check the "frame-pointer" module flag.
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"frame-pointer", i32 7}
