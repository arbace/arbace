; verdict: invalid Range must not be empty!
; rule: llvm/lib/IR/Verifier.cpp:4505 [0,0): equal bounds pass the same-value check because 0 is the minimum value, but ConstantRange(0,0) is the empty set.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i32 0, i32 0}
