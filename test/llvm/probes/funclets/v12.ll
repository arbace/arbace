; verdict: invalid CleanupReturnInst needs to be provided a CleanupPad
; rule: llvm/lib/IR/Verifier.cpp:5176 cleanupret from a catchpad; the catchpad and its users otherwise pass.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %cp] unwind to caller
cp:
 %p = catchpad within %c [ptr null, i32 0, ptr null]
 cleanupret from %p unwind to caller
}
