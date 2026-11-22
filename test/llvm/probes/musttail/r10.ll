; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4143 inalloca(i32) on the last param and the call argument match; the inalloca argument is an Argument, not an alloca.
declare void @g(ptr inalloca(i32))
define void @f(ptr inalloca(i32) %x) {
 musttail call void @g(ptr inalloca(i32) %x)
 ret void
}
