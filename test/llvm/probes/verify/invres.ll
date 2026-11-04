declare i32 @g()
declare i32 @p(...)
define i32 @f() personality ptr @p {
  %r = invoke i32 @g() to label %ok unwind label %lp
lp:
  %l = landingpad { ptr, i32 } cleanup
  ret i32 %r
ok:
  ret i32 %r
}
