declare void @g(...)
define void @f(i32 %x) {
 musttail call void (...) @g(i32 %x)
 ret void
}
