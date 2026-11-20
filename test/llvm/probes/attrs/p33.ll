; verdict: valid
; rule: - sret on the first parameter of a void function is legal.
define void @f(ptr sret(i32), ptr) { ret void }
