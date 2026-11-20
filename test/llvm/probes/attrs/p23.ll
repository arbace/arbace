; verdict: valid
; rule: - swifterror with noundef on a ptr parameter has no exclusion rule; argument unused.
define void @f(ptr swifterror noundef %p) { ret void }
