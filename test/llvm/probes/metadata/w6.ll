; verdict: invalid The lower limit must be an integer!
; rule: llvm/lib/IR/Verifier.cpp:4480 Two MDString operands: an even count, but the lower bound is not a ConstantInt.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{!"a", !"b"}
