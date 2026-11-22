; verdict: valid
; rule: - preallocated(i32) is a legal parameter attribute and only inalloca excludes readonly.
define void @f(ptr preallocated(i32) readonly %p) { ret void }
