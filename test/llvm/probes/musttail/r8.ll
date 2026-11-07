declare void @g(i32 returned)
define void @f(i32 %x) {
 musttail call void @g(i32 returned %x)
 ret void
}
