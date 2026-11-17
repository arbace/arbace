declare void @g()
define void @f() {
 musttail call void @g() #0
 ret void
}
attributes #0 = { returned }
