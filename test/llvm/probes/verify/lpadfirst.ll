declare i32 @p(...)
declare void @t()
define void @f() personality ptr @p {
  invoke void @t() to label %ok unwind label %lp
ok:
  ret void
lp:
  %z = add i32 1, 2
  %l = landingpad { ptr, i32 } cleanup
  resume { ptr, i32 } %l
}
