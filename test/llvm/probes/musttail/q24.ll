; verdict: invalid Funclet bundle operands should correspond to a FuncletPadInst
; rule: llvm/lib/IR/Verifier.cpp:4055 The funclet bundle has one input (i32 0) which is not a FuncletPadInst.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "funclet"(i32 0) ]
 ret void
}
