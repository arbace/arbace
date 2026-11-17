declare void @g(i32)
define void @f(i32 %x) personality ptr null {
 invoke void @g(i32 %x) to label %a unwind label %a
a:
 ret void
}
