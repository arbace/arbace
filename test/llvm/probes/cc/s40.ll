define void @f(ptr %x) {
 %t = call token %x()
 ret void
}
