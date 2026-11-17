define void @f() { ret void }
define void @g() { call fastcc void @f()
 ret void }
