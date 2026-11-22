; verdict: invalid cannot guarantee tail call due to mismatched varargs
; rule: llvm/lib/IR/Verifier.cpp:4172 Callee type is varargs, caller is not (the parser accepts the call since the caller is not varargs).
declare void @g(i32, ...)
define void @f(i32 %x) {
 musttail call void (i32, ...) @g(i32 %x)
 ret void
}
