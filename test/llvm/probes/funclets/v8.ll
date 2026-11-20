; verdict: invalid A single unwind edge may only enter one EH pad
; rule: llvm/lib/IR/Verifier.cpp:4850 The cleanuppad's parent is itself (a FuncletPadInst so the parent check passes); the invoke edge's FromPad none is neither ToPad nor ToPadParent.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within %p []
 cleanupret from %p unwind to caller
}
