; verdict: valid
; rule: - swifterror param on ptr with no uses (verifySwiftErrorValue finds none); the verifier does not require swiftcc for it.
define swiftcc void @f(ptr swifterror %e) { ret void }
