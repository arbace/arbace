define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !dereferenceable !0
 ret ptr %v
}
!0 = !{i64 8, i64 9}
