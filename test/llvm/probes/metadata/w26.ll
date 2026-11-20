; verdict: invalid fpmath requires a floating point result!
; rule: llvm/lib/IR/Verifier.cpp:5780 !fpmath is attached to an i32 add; the FP-result check is the first fpmath check, and fpmath is the first metadata visitInstruction examines.
define i32 @f(i32 %p) {
 %v = add i32 %p, 1, !fpmath !0
 ret i32 %v
}
!0 = !{float 1.0}
