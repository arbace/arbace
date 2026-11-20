; verdict: valid
; rule: - String attribute "x", nounwind and uwtable(sync) are all legal function attributes with no verifier rule.
define void @f() "x" nounwind uwtable(sync) { ret void }
