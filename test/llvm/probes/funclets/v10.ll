declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() {
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
