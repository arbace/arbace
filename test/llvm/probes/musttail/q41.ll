declare void @g(i32)
define void @f(i32 %x) personality ptr null {
 invoke void @g(i32 %x) to label %a unwind label %b
a:
 ret void
b:
 %l = landingpad {ptr,i32} cleanup
 musttail call void @g(i32 %x)
 ret void
}
