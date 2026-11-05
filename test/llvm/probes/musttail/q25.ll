declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "gc-transition"() ]
 ret void
}
