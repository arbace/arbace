; verdict: invalid cannot guarantee tail call due to mismatched parameter counts
; rule: llvm/lib/IR/Verifier.cpp:4219 Caller has two params, callee one.
declare void @g(i32)
define void @f(i32 %x, i32 %y) {
 musttail call void @g(i32 %x)
 ret void
}
