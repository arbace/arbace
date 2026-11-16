declare void @g(ptr swifterror)
define void @f() {
 %p = alloca ptr
 call void @g(ptr swifterror %p)
 ret void
}
