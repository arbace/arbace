declare void @g(i32)
define void @f(i32 %x) {
 notail call void @g(i32 5)
 ret void
}
