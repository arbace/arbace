; verdict: invalid cannot guarantee tail call due to mismatched calling conv
; rule: llvm/lib/IR/Verifier.cpp:4178 Caller fastcc, call ccc.
declare void @llvm.trap()
define fastcc void @f() {
 musttail call void @llvm.trap()
 ret void
}
