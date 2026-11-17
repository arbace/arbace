declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 cleanupret from %p unwind label %cp2
cp2:
 %q = cleanuppad within none []
 cleanupret from %q unwind to caller
}
