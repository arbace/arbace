; verdict: valid
; rule: - Simple cleanuppad with cleanupret reached by an invoke unwind edge.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cp
ok:
 ret void
cp:
 %p = cleanuppad within none []
 cleanupret from %p unwind to caller
}
