; verdict: invalid invalid behavior operand in module flag (expected constant integer)
; rule: llvm/lib/IR/Verifier.cpp:1918 The behavior operand is the string "x", not a ConstantInt.
!llvm.module.flags = !{!0}
!0 = !{!"x", !"wchar_size", i32 4}
