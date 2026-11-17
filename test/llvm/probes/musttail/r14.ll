declare void @g(ptr)
define void @f(ptr %x) personality ptr null {
 musttail call void @g(ptr %x)
 ret void
}
