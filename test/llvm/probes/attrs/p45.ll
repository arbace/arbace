; verdict: valid
; rule: - sanitize_address with naked: no verifier rule; no arguments so naked's use check is empty.
define void @f() sanitize_address naked { ret void }
