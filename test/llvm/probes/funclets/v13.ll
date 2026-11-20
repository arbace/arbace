; verdict: invalid CatchSwitchInst handlers must be catchpads
; rule: llvm/lib/IR/Verifier.cpp:5167 The handler block ok starts with ret.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %ok] unwind to caller
}
