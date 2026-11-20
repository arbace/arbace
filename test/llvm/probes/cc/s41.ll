; verdict: valid
; rule: - Call of a varargs declaration with no arguments.
declare void @g(...)
define void @f() {
 call void (...) @g()
 ret void
}
