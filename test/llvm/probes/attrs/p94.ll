; verdict: valid
; rule: - fn_ret_thunk_extern is a legal function attribute.
define void @f() fn_ret_thunk_extern { ret void }
