define i32 @f() { ret i32 0 }
define void @g() { call i32 @f() zeroext signext
 ret void }
