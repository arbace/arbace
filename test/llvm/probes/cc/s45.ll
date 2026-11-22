; verdict: invalid inalloca isn't on the last argument!
; rule: llvm/lib/IR/Verifier.cpp:4005 inalloca on a vararg argument that is not the call's last argument.
declare void @g(ptr, ...)
define void @f(ptr %p) {
 call void (ptr, ...) @g(ptr %p, ptr inalloca(i32) %p, i32 1)
 ret void
}
