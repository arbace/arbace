; verdict: invalid CatchSwitchInst handlers must be catchpads
; rule: llvm/lib/IR/Verifier.cpp:5167 catchswitch cs is visited before cp; cp's first non-PHI is the add, so the handler check fires before the catchpad's own check.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %cp] unwind to caller
cp:
 %x = add i32 0, 0
 %p = catchpad within %c [ptr null, i32 0, ptr null]
 catchret from %p to label %ok
}
