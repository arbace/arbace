declare void @g()
define void @f() {
 %p = ptrtoint ptr @g to i64
 musttail call void @g()
 ret void
}
