; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4505 !{i32 5, i32 0} is one wrapped range [5,0), which ConstantRange treats as neither empty nor full; the bounds are i32 like the loaded value, so every range check passes.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i32 5, i32 0}
