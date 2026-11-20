; verdict: valid
; rule: - naked function with an unused argument and varargs under the C convention.
define void @f(i32 %x, ...) naked {
 ret void
}
