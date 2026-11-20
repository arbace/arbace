; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:1931 Error behavior accepts any value and the verifier has no rule for the "uwtable" flag's value (only Module::getUwtableKind reads it, and nothing calls it under -passes=verify).
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"uwtable", !"x"}
