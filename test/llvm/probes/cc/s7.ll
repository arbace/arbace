; verdict: valid
; rule: - fastcc call of a matching declaration.
declare void @g()
define void @f() {
 call fastcc void @g()
 ret void
}
