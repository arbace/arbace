; verdict: valid
; rule: - The ptrtoint of @g is an ordinary instruction before the call; the call is followed by ret.
declare void @g()
define void @f() {
 %p = ptrtoint ptr @g to i64
 musttail call void @g()
 ret void
}
