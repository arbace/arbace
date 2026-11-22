; verdict: valid
; rule: - An indirect musttail call with matching prototype is accepted.
declare void @g(i32)
define void @f(i32 %x) {
 %p = alloca ptr
 store ptr @g, ptr %p
 %q = load ptr, ptr %p
 musttail call void %q(i32 %x)
 ret void
}
