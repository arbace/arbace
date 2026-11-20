; verdict: valid
; rule: - null_pointer_is_valid and the string null-pointer-is-valid are not cross-checked.
define void @f() null_pointer_is_valid "null-pointer-is-valid"="x" { ret void }
