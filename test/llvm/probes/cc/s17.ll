define void @f(ptr %x) naked {
 %y = load i32, ptr %x
 ret void
}
