; verdict: invalid Funclet bundle operands should correspond to a FuncletPadInst
; rule: llvm/lib/IR/Verifier.cpp:4055 The ok block call with funclet(token none) is visited before the cp block.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 call void @g() [ "funclet"(token none) ]
 ret void
cp:
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
