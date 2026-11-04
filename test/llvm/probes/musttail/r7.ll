declare void @g(ptr swiftself)
define swiftcc void @f(ptr %x) {
 musttail call swiftcc void @g(ptr swiftself %x)
 ret void
}
