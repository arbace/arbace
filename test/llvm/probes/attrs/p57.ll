; verdict: valid
; rule: - uwtable(async) then uwtable(sync): the later one replaces the earlier; no rule.
define void @f() uwtable(async) uwtable(sync) { ret void }
