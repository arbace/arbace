; verdict: valid
; rule: - swiftself with nonnull on an unnamed ptr parameter is legal.
define void @f(ptr swiftself nonnull) { ret void }
