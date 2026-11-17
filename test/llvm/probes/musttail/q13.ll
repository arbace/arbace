declare void @g(i32 zeroext)
define void @f(i32 %x) {
 musttail call void @g(i32 zeroext %x)
 ret void
}
