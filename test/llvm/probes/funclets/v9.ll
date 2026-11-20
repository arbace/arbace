; verdict: invalid CleanupPadInst not the first non-PHI instruction in the block.
; rule: llvm/lib/IR/Verifier.cpp:4962 The second cleanuppad %q follows %p in the same block; %p itself passes.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 %q = cleanuppad within %p []
 cleanupret from %q unwind to caller
}
