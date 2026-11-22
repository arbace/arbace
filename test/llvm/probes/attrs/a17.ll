; verdict: valid
; rule: - swifterror on a ptr parameter is legal; the argument has no users so verifySwiftErrorValue finds nothing.
define void @f(ptr swifterror %p) { ret void }
