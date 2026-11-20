; verdict: valid
; rule: - byval(i32) with align 2 on a declaration is legal; no check relates align to the byval type.
declare void @f(ptr byval(i32) align 2 %p)
