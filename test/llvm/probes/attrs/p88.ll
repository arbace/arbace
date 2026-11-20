; verdict: valid
; rule: - nosanitize_bounds with nosanitize_coverage: no rule.
define void @f() nosanitize_bounds nosanitize_coverage { ret void }
