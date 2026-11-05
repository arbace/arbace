define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !dereferenceable_or_null !0
 ret ptr %v
}
!0 = !{}
