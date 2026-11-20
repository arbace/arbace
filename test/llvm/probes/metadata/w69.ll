; verdict: invalid TBAA metadata cannot have 0 operands
; rule: llvm/lib/IR/Verifier.cpp:7965 UpgradeTBAANode leaves an empty node unchanged; the TBAA verifier's first check rejects !tbaa !{}.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{}
