; verdict: invalid EH pad cannot handle exceptions raised within it
; rule: llvm/lib/IR/Verifier.cpp:4844 The catchswitch unwinds to its own block, so one of the cs block predecessors is the catchswitch itself (FromPad == ToPad).
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %cp] unwind label %cs
cp:
 %p = catchpad within %c [ptr null, i32 0, ptr null]
 catchret from %p to label %ok
}
