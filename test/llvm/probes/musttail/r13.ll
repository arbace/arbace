; verdict: invalid cannot guarantee tail call due to mismatched parameter counts
; rule: llvm/lib/IR/Verifier.cpp:4219 h() has no params, the callee f has one.
declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x)
 ret void
}
define void @h() {
 musttail call void @f(ptr null)
 ret void
}
