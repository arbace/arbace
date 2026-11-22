; verdict: invalid EH pad must be jumped to via an unwind edge
; rule: llvm/lib/IR/Verifier.cpp:4837 The cleanuppad's predecessor is a br, not an invoke/cleanupret/catchswitch.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 br label %cp
cp:
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
