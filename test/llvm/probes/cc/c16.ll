define x86_intrcc void @f(ptr byval(i32) %p) { ret void }
define void @g(ptr %p) { call x86_intrcc void @f(ptr byval(i32) %p)
 ret void}
