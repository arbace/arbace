; verdict: invalid EH pad cannot be in entry block.
; rule: llvm/lib/IR/Verifier.cpp:4785 The cleanuppad is in the entry block; personality and first non-PHI checks pass.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
