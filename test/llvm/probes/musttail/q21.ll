; verdict: invalid Funclet bundle operands should correspond to a FuncletPadInst
; rule: llvm/lib/IR/Verifier.cpp:4055 The funclet bundle input is token none, not a FuncletPadInst; visitCallBase runs before the musttail check.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x) [ "funclet"(token none) ]
 ret void
}
