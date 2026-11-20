; verdict: valid
; rule: - target-features "+x" starts with '+'; min-legal-vector-width is not validated.
define void @f() "target-features"="+x" "min-legal-vector-width"="x" { ret void }
