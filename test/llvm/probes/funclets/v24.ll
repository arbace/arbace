; verdict: invalid EH pad cannot handle exceptions raised within it
; rule: llvm/lib/IR/Verifier.cpp:4844 The invoke inside cp has funclet %p and unwinds to cp, so FromPad == ToPad for the cleanuppad.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 invoke void @g() [ "funclet"(token %p) ] to label %c2 unwind label %cp
c2:
 cleanupret from %p unwind to caller
}
