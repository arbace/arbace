; verdict: valid
; rule: - byref(i32) with readonly: only inalloca conflicts with readonly; i32 is sized.
define void @f(ptr byref(i32) readonly %p) { ret void }
