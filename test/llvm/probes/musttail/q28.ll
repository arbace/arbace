declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "kcfi"(i32 0) ]
 ret void
}
