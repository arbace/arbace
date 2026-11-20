; verdict: invalid cannot guarantee tail call due to mismatched varargs
; rule: llvm/lib/IR/Verifier.cpp:4172 Caller is not varargs, callee type (i64, ...) is.
declare void @g(i64, ...)
define void @f(i32 %x) {
 musttail call void (i64, ...) @g(i64 1)
 unreachable
}
