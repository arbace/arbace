declare void @g(...)
define void @f(...) {
 musttail call void (...) @g(...)
 ret void
}
