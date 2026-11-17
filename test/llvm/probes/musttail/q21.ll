declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x) [ "funclet"(token none) ]
 ret void
}
