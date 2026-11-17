declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x) #0
 ret void
}
attributes #0 = { noreturn }
