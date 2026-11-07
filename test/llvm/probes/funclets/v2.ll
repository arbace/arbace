declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %cp] unwind to caller
cp:
 %p = catchpad within %c [ptr null, i32 0, ptr null]
 catchret from %p to label %ok
}
