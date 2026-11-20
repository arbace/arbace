; verdict: invalid Access bit-width not the same as description bit-width
; rule: llvm/lib/IR/Verifier.cpp:8058 The tag's offset is i32 0 while the type node's offsets are i64: the base node's bit width is 64, the access offset's 32.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1, i32 0}
!1 = !{!"int", !2, i64 0}
!2 = !{!"omnipotent char", !3, i64 0}
!3 = !{!"Simple C/C++ TBAA"}
