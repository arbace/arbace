; verdict: valid
; rule: - optforfuzzing with optnone and noinline: no rule forbids optforfuzzing.
define void @f() optforfuzzing optnone noinline { ret void }
