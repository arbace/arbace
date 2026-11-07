declare void @g(ptr inalloca(i32))
define void @f(ptr inalloca(i32) %x) {
 musttail call void @g(ptr inalloca(i32) %x)
 ret void
}
