; verdict: invalid cannot guarantee tail call due to mismatched varargs
; rule: llvm/lib/IR/Verifier.cpp:4172 Caller f(i32) is not varargs while the call type void (...) is.
declare void @g(...)
define void @f(i32 %x) {
 musttail call void (...) @g(i32 %x)
 ret void
}
