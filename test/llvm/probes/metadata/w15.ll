define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !noundef !0
 ret ptr %v
}
!0 = !{i32 1}
