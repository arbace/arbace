; verdict: invalid invalid behavior operand in module flag (unexpected constant)
; rule: llvm/lib/IR/Verifier.cpp:1921 Behavior 9 is a ConstantInt outside Error(1)..Min(8): isValidModFlagBehavior fails, the constant-integer check passes, Check(false) fires.
!llvm.module.flags = !{!0}
!0 = !{i32 9, !"wchar_size", i32 4}
