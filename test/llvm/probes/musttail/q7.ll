; verdict: invalid cannot guarantee tail call due to mismatched parameter types
; rule: llvm/lib/IR/Verifier.cpp:4222 Same parameter count, caller i64 vs callee i32.
declare void @g(i32)
define void @f(i64 %x) {
 musttail call void @g(i32 1)
 ret void
}
