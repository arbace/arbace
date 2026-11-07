declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 ret void
}
define void @h() {
 resume {ptr, i32} undef
}
