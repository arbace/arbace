; verdict: valid
; rule: - tail marker on a call to a declaration has no verifier rule; only musttail is checked.
define void @f() {
  tail call fastcc void @g()
  ret void
}
declare void @g()
