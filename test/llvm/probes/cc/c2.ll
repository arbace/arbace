declare fastcc void @f()
define void @g() { call void @f()
 ret void }
