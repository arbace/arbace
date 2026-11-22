; verdict: valid
; rule: - sret on the second parameter of a void function is legal (index 0 or 1); parser only checks param 0 against return type.
define void @f(ptr, ptr sret(i32)) { ret void }
