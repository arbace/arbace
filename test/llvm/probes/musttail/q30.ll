declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "convergencectrl"() ]
 ret void
}
