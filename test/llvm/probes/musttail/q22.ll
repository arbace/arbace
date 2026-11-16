declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x) [ "foo"() ]
 ret void
}
