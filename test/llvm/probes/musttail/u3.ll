; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Caller returns i32, the llvm.trap call returns void.
declare void @llvm.trap()
define i32 @f() {
 musttail call void @llvm.trap()
 ret i32 0
}
