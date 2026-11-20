; verdict: invalid CatchSwitchInst not the first non-PHI instruction in the block.
; rule: llvm/lib/IR/Verifier.cpp:5143 The catchswitch follows a cleanuppad in the same block.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 %c = catchswitch within %p [label %ok] unwind to caller
}
