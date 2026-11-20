; verdict: valid
; rule: - A personality on a function without EH is fine.
declare void @g(ptr)
define void @f(ptr %x) personality ptr null {
 musttail call void @g(ptr %x)
 ret void
}
