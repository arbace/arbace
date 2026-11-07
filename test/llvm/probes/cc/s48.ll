declare void @g(ptr swifterror)
define void @f(ptr %p) {
 call void @g(ptr swifterror %p)
 ret void
}
