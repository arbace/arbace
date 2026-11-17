declare void @g(i32)
define void @f(i32 %x) {
 %c = icmp eq i32 %x, 0
 br i1 %c, label %a, label %b
a:
 musttail call void @g(i32 %x)
 ret void
b:
 ret void
}
