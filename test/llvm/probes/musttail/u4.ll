; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Return types are compared before the ret check.
declare void @llvm.trap()
define i32 @f() {
 musttail call void @llvm.trap()
 unreachable
}
