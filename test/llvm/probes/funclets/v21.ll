declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 br label %cp
cp:
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
