; verdict: invalid CleanupPadInst needs to be in a function with a personality.
; rule: llvm/lib/IR/Verifier.cpp:4957 f has no personality; this is the first check in visitCleanupPadInst.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() {
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
