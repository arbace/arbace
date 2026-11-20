; verdict: valid
; rule: - No verifier rule excludes dereferenceable with dereferenceable_or_null; both are legal on a ptr parameter.
define void @f(ptr dereferenceable(4) dereferenceable_or_null(4) %p) { ret void }
