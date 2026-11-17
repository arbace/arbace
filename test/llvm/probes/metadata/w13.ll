define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !align !0
 ret ptr %v
}
!0 = !{}
