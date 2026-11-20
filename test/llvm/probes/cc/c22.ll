; verdict: valid
; rule: - swifterror on a ptr param of a non-swift function with no uses is accepted.
define void @f(ptr swifterror %e) { ret void }
