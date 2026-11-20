; verdict: valid
; rule: - no-trapping-math is not a string-bool attribute in this version, so its value is not checked.
define void @f() "no-trapping-math"="x" { ret void }
