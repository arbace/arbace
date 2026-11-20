; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4210 Intrinsic callee: parameter type check skipped; caller param has no ABI attributes on either side.
declare void @llvm.trap()
define void @f(i32 %x) {
 musttail call void @llvm.trap()
 ret void
}
