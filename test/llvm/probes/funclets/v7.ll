; verdict: invalid CatchReturnInst needs to be provided a CatchPad
; rule: llvm/lib/IR/Verifier.cpp:4946 catchret from a cleanuppad; visitFuncletPadInst accepts a catchret user.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 catchret from %p to label %ok
}
