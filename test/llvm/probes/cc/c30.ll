declare void @f(ptr)
define void @g(ptr %p) { musttail call void @f(ptr %p)
 ret void }
