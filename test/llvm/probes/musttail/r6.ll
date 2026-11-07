declare void @g(ptr nest)
define void @f(ptr %x) {
 musttail call void @g(ptr nest %x)
 ret void
}
