declare void @g(i32 inreg)
define void @f(i32 %x) {
 musttail call void @g(i32 inreg %x)
 ret void
}
