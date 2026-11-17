declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x), !srcloc !0
 ret void
}
!0 = !{i32 1}
