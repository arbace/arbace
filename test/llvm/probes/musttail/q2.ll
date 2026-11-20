; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 The instruction after the musttail call is an add, not a ret.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 %y = add i32 %x, 1
 ret void
}
