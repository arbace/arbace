; verdict: valid
; rule: - swiftself on a ptr param without swiftcc is accepted.
define void @f(ptr swiftself %e) { ret void }
